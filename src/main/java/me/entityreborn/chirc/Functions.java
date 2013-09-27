/*
 * The MIT License
 *
 * Copyright 2013 Jason Unger <entityreborn@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.entityreborn.chirc;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.StaticLayer;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.entityreborn.chirc.Events.ConnectionException;
import static me.entityreborn.chirc.Tracking.verbose;
import static me.entityreborn.chirc.Tracking.flatten;
import me.entityreborn.socbot.api.Channel;
import me.entityreborn.socbot.api.SocBot;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Functions {
    public abstract static class IrcFunc extends AbstractFunction {
        public ExceptionType[] thrown() {
            return new ExceptionType[] {};
        }
        
        public boolean isRestricted() {
            return true;
        }

        public Boolean runAsync() {
            return false;
        }

        public Version since() {
            return CHVersion.V3_3_1;
        }
    }
    
    @api
    public static class irc_create extends IrcFunc {
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_create:" + flatten(args), t);
            
            SocBot bot = Tracking.create(args[0].val());
            
            return new CBoolean(bot != null, t);
        }

        public String getName() {
            return "irc_create";
        }

        public Integer[] numArgs() {
            return new Integer[] {1};
        }

        public String docs() {
            return "boolean {id} Create an IRC bot for later use. Returns true"
                    + " if that id didn't exist, and false if it did.";
        }
    }
    
    @api
    public static class irc_send_raw extends IrcFunc {
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_send_raw:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);
            
            String line = args[1].val();
            
            bot.sendLine(line);
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_send_raw";
        }

        public Integer[] numArgs() {
            return new Integer[] {2};
        }

        public String docs() {
            return "void {id, line} Send a raw IRC line. Consult the IRC RFC for details.";
        }
    }
    
    @api
    public static class irc_destroy extends IrcFunc {
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_destroy:" + flatten(args), t);
            
            Tracking.destroy(args[0].val(), t);
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_destroy";
        }

        public Integer[] numArgs() {
            return new Integer[] {1};
        }

        public String docs() {
            return "void {id} Destroy an IRC bot. Disconnects the bot from any"
                    + " connection, and removes it's instance from memory.";
        }
    }
    
    @api
    public static class irc_connect extends IrcFunc {
        @Override
        public ExceptionType[] thrown() {
            return new ExceptionType[] {
                ExceptionType.NotFoundException, ExceptionType.IOException, 
                ExceptionType.CastException, ExceptionType.RangeException};
        }
        
        public Construct exec(final Target t, Environment environment
                , Construct... args) throws ConfigRuntimeException {
            verbose("irc_connect:" + flatten(args), t);
            
            final SocBot bot = Tracking.get(args[0].val(), t);
            
            String nick = args[1].val();
            final String host = args[2].val();
            final int port;
            final String password;
            boolean async = true;
            
            if (args.length >= 4) {
                if (!(args[3] instanceof CArray) || 
                        !((CArray)args[3]).inAssociativeMode()) {
                    throw new ConfigRuntimeException(getName() + " expects an"
                            + " associative array to be sent as the fourth argument", 
                            ExceptionType.CastException, t);
                }
                
                CArray arr = (CArray)args[3];
                
                if (arr.containsKey("realname")) {
                    bot.setRealname(arr.get("realname").val());
                }
                
                if (arr.containsKey("username")) {
                    bot.setUsername(arr.get("username").val());
                }
                
                if (arr.containsKey("port")) {
                    long iport = Static.getInt(arr.get("port"), t);
                    
                    if (iport < 1 || iport > 65535) {
                        throw new ConfigRuntimeException(getName() + " expects an"
                                + " integer between 1 and 65535 to be sent as port"
                                + " in the fourth argument", ExceptionType.RangeException, t);
                    }
                    
                    port = (int)iport;
                } else {
                    port = 6667;
                }
                
                if (arr.containsKey("password")) {
                    password = arr.get("password").val();
                } else {
                    password = null;
                }
                
                if (arr.containsKey("runsync")) {
                    if (!(arr.get("runsync") instanceof CBoolean)) {
                        throw new ConfigRuntimeException(getName() + " expects a"
                                + " boolean to be sent as runsync in the fourth"
                                + " argument", ExceptionType.CastException, t);
                    }
                    
                    async = !((CBoolean)arr.get("runsync")).getBoolean();
                }
            } else {
                port = 6667;
                password = null;
            }
            
            bot.setNickname(nick);
            
            final Runnable doConnect = new Runnable() {
                public void run() {
                    try {
                        bot.connect(host, port, password);
                    } catch (IOException e) {
                        final ConnectionException event = new ConnectionException(e, bot);
                        
                        try {
                            StaticLayer.GetConvertor().runOnMainThreadAndWait(new Callable<Object>() {
                                public Object call() {
                                    EventUtils.TriggerListener(Driver.EXTENSION, "irc_connection_exception", event);
                                    return null;
                                }
                            });
                        } catch (Exception ex) {
                            Logger.getLogger(Events.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            };
            
            if (async) {
                Thread th = new Thread() {
                    @Override
                    public void run() {
                        doConnect.run();
                    }
                };
            
                th.start();
            } else {
                doConnect.run();
            }
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_connect";
        }

        public Integer[] numArgs() {
            return new Integer[] {3, 4};
        }

        public String docs() {
            return "void {id, nick, host[, array]} Connect to host using nickname nick.";
        }
    }
    
    @api
    public static class irc_join extends IrcFunc {
        @Override
        public ExceptionType[] thrown() {
            return new ExceptionType[] {
                ExceptionType.NotFoundException, ExceptionType.IOException};
        }
        
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_join:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);
            
            String channel = args[1].val();
            
            if (args.length == 3) {
                bot.join(channel, args[2].val());
            } else {
                bot.join(channel);
            }
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_join";
        }

        public Integer[] numArgs() {
            return new Integer[] {2, 3};
        }

        public String docs() {
            return "void {id, channel[, password]} Join a channel, optionally using a password.";
        }
    }
    
    @api
    public static class irc_part extends IrcFunc {
        @Override
        public ExceptionType[] thrown() {
            return new ExceptionType[] {
                ExceptionType.NotFoundException, ExceptionType.IOException};
        }
        
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_part:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);
            
            String channel = args[1].val();
            
            if (args.length == 3) {
                bot.part(channel, args[2].val());
            } else {
                bot.part(channel);
            }
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_part";
        }

        public Integer[] numArgs() {
            return new Integer[] {2, 3};
        }

        public String docs() {
            return "void {id, channel[, password]} Leave a channel, optionally using a message.";
        }
    }
    
    @api
    public static class irc_quit extends IrcFunc {
        @Override
        public ExceptionType[] thrown() {
            return new ExceptionType[] {
                ExceptionType.NotFoundException, ExceptionType.IOException};
        }
        
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_quit:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);
            
            if (args.length == 2) {
                bot.quit(args[1].val());
            } else {
                bot.quit();
            }
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_quit";
        }

        public Integer[] numArgs() {
            return new Integer[] {1, 2};
        }

        public String docs() {
            return "void {id, channel[, password]} Quit the server, optionally using a message.";
        }
    }
    
    @api
    public static class irc_msg extends IrcFunc {
        @Override
        public ExceptionType[] thrown() {
            return new ExceptionType[] {
                ExceptionType.NotFoundException, ExceptionType.IOException};
        }
        
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_msg:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);
            
            String channel = args[1].val();
            String message = args[2].val();
            me.entityreborn.socbot.api.Target target;
            
            if (me.entityreborn.socbot.api.Target.Util.isUser(channel, bot)) {
                target = bot.getUser(channel);
            } else {
                target = bot.getChannel(channel);
            }
            
            if (target != null) {
                target.sendMsg(message);
            }
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_msg";
        }

        public Integer[] numArgs() {
            return new Integer[] {3};
        }

        public String docs() {
            return "void {id, target, message} Send a message to target.";
        }
    }
    
    @api
    public static class irc_action extends IrcFunc {
        @Override
        public ExceptionType[] thrown() {
            return new ExceptionType[] {
                ExceptionType.NotFoundException, ExceptionType.IOException};
        }
        
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_action:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);
            
            String channel = args[1].val();
            String message = args[2].val();
            me.entityreborn.socbot.api.Target target;
            
            if (me.entityreborn.socbot.api.Target.Util.isUser(channel, bot)) {
                target = bot.getUser(channel);
            } else {
                target = bot.getChannel(channel);
            }
            
            if (target != null) {
                target.sendCTCP("ACTION", message);
            }
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_action";
        }

        public Integer[] numArgs() {
            return new Integer[] {3};
        }

        public String docs() {
            return "void {id, target, message} Send an action to target.";
        }
    }
    
    @api
    public static class irc_nick extends IrcFunc {
        @Override
        public ExceptionType[] thrown() {
            return new ExceptionType[] {
                ExceptionType.NotFoundException, ExceptionType.IOException};
        }
        
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_nick:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);
            
            String name = args[1].val();
            
            bot.setNickname(name);
            
            return new CVoid(t);
        }

        public String getName() {
            return "irc_nick";
        }

        public Integer[] numArgs() {
            return new Integer[] {2};
        }

        public String docs() {
            return "void {id, newnick} Try for a new nickname.";
        }
    }
    
    @api
    public static class irc_info extends IrcFunc {
        @Override
        public ExceptionType[] thrown() {
            return new ExceptionType[] {ExceptionType.NotFoundException};
        }
        
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("irc_info:" + flatten(args), t);
            
            SocBot bot = Tracking.get(args[0].val(), t);
            
            CArray retn = new CArray(t);
            
            CArray channels = new CArray(t);
            
            for (Channel chan : bot.getChannels()) {
                channels.push(new CString(chan.getName(), t));
            }
            
            retn.set("nickname", bot.getNickname());
            retn.set("channels", channels, t);
            retn.set("connected", new CBoolean(bot.isConnected(), t), t);
            
            return retn;
        }

        public String getName() {
            return "irc_info";
        }

        public Integer[] numArgs() {
            return new Integer[] {1};
        }

        public String docs() {
            return "void {id} Get information about a specific irc connection.";
        }
    }
}
