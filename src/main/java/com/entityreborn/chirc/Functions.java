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
package com.entityreborn.chirc;

import com.entityreborn.chirc.Events.ConnectionException;
import static com.entityreborn.chirc.Tracking.flatten;
import static com.entityreborn.chirc.Utils.verbose;
import com.entityreborn.socbot.Channel;
import com.entityreborn.socbot.Colors;
import com.entityreborn.socbot.SocBot;
import com.entityreborn.socbot.Styles;
import com.entityreborn.socbot.User;
import com.laytonsmith.PureUtilities.Common.StringUtils;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.StaticLayer;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.exceptions.CRE.*;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Functions {
    public abstract static class IrcFunc extends AbstractFunction {
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{};
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
            verbose("CHIRC", "irc_create:" + flatten(args), t);

            SocBot bot = Tracking.create(args[0].val());

            return CBoolean.get(bot != null);
        }

        public String getName() {
            return "irc_create";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "boolean {id} Create an IRC bot for later use. Returns true"
                    + " if that id didn't exist, and false if it did.";
        }
    }

    @api
    public static class irc_strip_color extends IrcFunc {
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_strip_color:" + flatten(args), t);

            String out = Colors.removeAll(args[0].val());
            out = Styles.removeAll(out);

            return new CString(out, t);
        }

        public String getName() {
            return "irc_strip_color";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "string {line} Remove all IRC formatting from a string.";
        }
    }

    @api
    public static class irc_send_raw extends IrcFunc {
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_send_raw:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String line = args[1].val();

            bot.sendLine(line);

            return CNull.NULL;
        }

        public String getName() {
            return "irc_send_raw";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {id, line} Send a raw IRC line. Consult the IRC RFC for details.";
        }
    }

    @api
    public static class irc_destroy extends IrcFunc {
        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_destroy:" + flatten(args), t);

            Tracking.destroy(args[0].val(), t);

            return CNull.NULL;
        }

        public String getName() {
            return "irc_destroy";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "void {id} Destroy an IRC bot. Disconnects the bot from any"
                    + " connection, and removes it's instance from memory.";
        }
    }

    @api
    public static class irc_connect extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class,
                CRECastException.class, CRERangeException.class};
        }

        public Construct exec(final Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_connect:" + flatten(args), t);

            final SocBot bot = Tracking.get(args[0].val(), t);

            String nick = args[1].val();
            final String host = args[2].val();
            final int port;
            final String password;
            boolean async = true;

            if (args.length >= 4) {
                if (!(args[3] instanceof CArray)
                        || !((CArray) args[3]).inAssociativeMode()) {
                    throw new CRECastException(getName() + " expects an"
                            + " associative array to be sent as the fourth argument", t);
                }

                CArray arr = (CArray) args[3];

                if (arr.containsKey("realname")) {
                    bot.setRealname(arr.get("realname", t).val());
                }

                if (arr.containsKey("username")) {
                    bot.setUsername(arr.get("username", t).val());
                }

                if (arr.containsKey("port")) {
                    long iport = Static.getInt(arr.get("port", t), t);

                    if (iport < 1 || iport > 65535) {
                        throw new CRERangeException(getName() + " expects an"
                                + " integer between 1 and 65535 to be sent as port"
                                + " in the fourth argument", t);
                    }

                    port = (int) iport;
                } else {
                    port = 6667;
                }

                if (arr.containsKey("password")) {
                    password = arr.get("password", t).val();
                } else {
                    password = null;
                }

                if (arr.containsKey("runsync")) {
                    if (!(arr.get("runsync", t) instanceof CBoolean)) {
                        throw new CRECastException(getName() + " expects a"
                                + " boolean to be sent as runsync in the fourth"
                                + " argument", t);
                    }

                    async = !((CBoolean) arr.get("runsync", t)).getBoolean();
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

            return CNull.NULL;
        }

        public String getName() {
            return "irc_connect";
        }

        public Integer[] numArgs() {
            return new Integer[]{3, 4};
        }

        public String docs() {
            return "void {id, nick, host[, array]} Connect to host using nickname nick.";
        }
    }

    @api
    public static class irc_join extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_join:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String channel = args[1].val();

            if (args.length == 3) {
                bot.join(channel, args[2].val());
            } else {
                bot.join(channel);
            }

            return CNull.NULL;
        }

        public String getName() {
            return "irc_join";
        }

        public Integer[] numArgs() {
            return new Integer[]{2, 3};
        }

        public String docs() {
            return "void {id, channel[, password]} Join a channel, optionally using a password.";
        }
    }

    @api
    public static class irc_part extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_part:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String channel = args[1].val();

            if (args.length == 3) {
                bot.part(channel, args[2].val());
            } else {
                bot.part(channel);
            }

            return CNull.NULL;
        }

        public String getName() {
            return "irc_part";
        }

        public Integer[] numArgs() {
            return new Integer[]{2, 3};
        }

        public String docs() {
            return "void {id, channel[, password]} Leave a channel, optionally using a message.";
        }
    }

    @api
    public static class irc_quit extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_quit:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            if (args.length == 2) {
                bot.quit(args[1].val());
            } else {
                bot.quit();
            }

            return CNull.NULL;
        }

        public String getName() {
            return "irc_quit";
        }

        public Integer[] numArgs() {
            return new Integer[]{1, 2};
        }

        public String docs() {
            return "void {id, channel[, password]} Quit the server, optionally using a message.";
        }
    }

    @api
    public static class irc_msg extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_msg:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String channel = args[1].val();
            String message = args[2].val();
            com.entityreborn.socbot.Target target;

            if (com.entityreborn.socbot.Target.Util.isUser(channel, bot)) {
                target = bot.getUser(channel);
            } else {
                target = bot.getChannel(channel);
            }

            if (target != null) {
                target.sendMsg(message);
            }

            return CNull.NULL;
        }

        public String getName() {
            return "irc_msg";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "void {id, target, message} Send a message to target.";
        }
    }

    @api
    public static class irc_action extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_action:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String channel = args[1].val();
            String message = args[2].val();
            com.entityreborn.socbot.Target target;

            if (com.entityreborn.socbot.Target.Util.isUser(channel, bot)) {
                target = bot.getUser(channel);
            } else {
                target = bot.getChannel(channel);
            }

            if (target != null) {
                target.sendCTCP("ACTION", message);
            }

            return CNull.NULL;
        }

        public String getName() {
            return "irc_action";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "void {id, target, message} Send an action to target.";
        }
    }

    @api
    public static class irc_nick extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_nick:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String name = args[1].val();

            bot.setNickname(name);

            return CNull.NULL;
        }

        public String getName() {
            return "irc_nick";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {id, newnick} Try for a new nickname.";
        }
    }

    @api
    public static class irc_channel_info extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_nick:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String chanName = args[1].val();
            Channel chan = bot.getChannel(chanName);

            if (chan == null) {
                throw new CRENotFoundException("Not joined to that channel!", t);
            }

            CArray retn = new CArray(t);

            retn.set("name", chan.getName());
            retn.set("modes", chan.getModes());
            retn.set("topic", chan.getTopic());

            CArray users = new CArray(t);

            for (Entry<User, String> entry : chan.getUserModes().entrySet()) {
                CArray data = new CArray(t);
                data.set("modes", entry.getValue());

                users.set(entry.getKey().getName(), data, t);
            }

            retn.set("users", users, t);

            return retn;
        }

        public String getName() {
            return "irc_channel_info";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {id, channel} Get info on a specific channel.";
        }
    }

    @api
    public static class irc_user_info extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{
                CRENotFoundException.class, CREIOException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_nick:" + flatten(args), t);

            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String userName = args[1].val();
            User user = bot.getUser(userName);

            if (user == null) {
                throw new CRENotFoundException("No idea who that is!", t);
            }

            CArray retn = new CArray(t);

            retn.set("name", user.getName());
            retn.set("modes", user.getModes());
            retn.set("hostmask", user.getHostmask());

            return retn;
        }

        public String getName() {
            return "irc_user_info";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {id, user} Get info on a specific user.";
        }
    }

    @api
    public static class irc_info extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{CRENotFoundException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_info:" + flatten(args), t);

            SocBot bot = Tracking.get(args[0].val(), t);

            CArray retn = new CArray(t);

            CArray channels = new CArray(t);

            for (Channel chan : bot.getChannels()) {
                channels.push(new CString(chan.getName(), t), t);
            }

            retn.set("nickname", bot.getNickname());
            retn.set("channels", channels, t);
            retn.set("connected", CBoolean.get(bot.isConnected()), t);

            return retn;
        }

        public String getName() {
            return "irc_info";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "void {id} Get information about a specific irc connection.";
        }
    }

    @api
    public static class irc_color extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{CREFormatException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_color:" + flatten(args), t);

            String name = args[0].val().toUpperCase();
            Colors color;

            try {
                color = Colors.valueOf(name.replace(" ", ""));
            } catch (IllegalArgumentException e) {
                throw new CREFormatException("Bad foreground color name", t);
            }

            if (args.length == 2) {
                try {
                    String background = args[1].val().toUpperCase();

                    color.setBackground(background.replace(" ", ""));
                } catch (IllegalArgumentException e) {
                    throw new CREFormatException("Bad background color name", t);
                }
            }

            return new CString(color.toColor(), t);
        }

        public String getName() {
            return "irc_color";
        }

        public Integer[] numArgs() {
            return new Integer[]{1, 2};
        }

        public String docs() {
            return "void {color[, background]} Return a colorcode for use in IRC messages. Values: "
                    + StringUtils.Join(Colors.values(), ", ");
        }
    }

    @api
    public static class irc_style extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{CREFormatException.class};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_style:" + flatten(args), t);

            String name = args[0].val();
            Styles style;
            try {
                style = Styles.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CREFormatException("Bad style name", t);
            }

            return new CString(style.toString(), t);
        }

        public String getName() {
            return "irc_style";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "void {style} Return a style for use in IRC messages. Values: "
                    + StringUtils.Join(Styles.values(), ", ") + ". ITALIC and "
                    + "STRIKETHRU don't work on all clients.";
        }
    }

    @api
    public static class irc_mc2irc_colors extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_mc2irc_colors:" + flatten(args), t);

            String line = args[0].val();
            line = line.replaceAll("§0", "\u000301"); // Black
            line = line.replaceAll("§1", "\u000302"); // Dark Blue
            line = line.replaceAll("§2", "\u000303"); // Dark Green
            line = line.replaceAll("§3", "\u000310"); // Dark Aqua
            line = line.replaceAll("§4", "\u000305"); // Dark Red
            line = line.replaceAll("§5", "\u000306"); // Dark Purple
            line = line.replaceAll("§6", "\u000307"); // Gold
            line = line.replaceAll("§7", "\u000315"); // Grey
            line = line.replaceAll("§8", "\u000314"); // Dark Grey
            line = line.replaceAll("§9", "\u000312"); // Blue
            line = line.replaceAll("§a", "\u000309"); // Green
            line = line.replaceAll("§b", "\u000311"); // Aqua
            line = line.replaceAll("§c", "\u000304"); // Red
            line = line.replaceAll("§d", "\u000313"); // Light Purple
            line = line.replaceAll("§e", "\u000308"); // Yellow
            line = line.replaceAll("§f", "\u000300"); // White

            line = line.replaceAll("§.", "");

            return new CString(line, t);
        }

        public String getName() {
            return "irc_mc2irc_colors";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "string {line} Return a string with mc colors converted to"
                    + " irc colors. Unknown colors will be stripped. Does not"
                    + " support styles (yet!).";
        }
    }

    @api
    public static class irc_irc2mc_colors extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_irc2mc_colors:" + flatten(args), t);

            String line = args[0].val();
            String background = "(,(1[0-5]|0?[0-9]))?";
            
            line = line.replaceAll("\u00030?1" + background, "§0");
            line = line.replaceAll("\u00030?2" + background, "§1");
            line = line.replaceAll("\u00030?3" + background, "§2");
            line = line.replaceAll("\u000310" + background, "§3");
            line = line.replaceAll("\u00030?5" + background, "§4");
            line = line.replaceAll("\u00030?6" + background, "§5");
            line = line.replaceAll("\u00030?7" + background, "§6");
            line = line.replaceAll("\u000315" + background, "§7");
            line = line.replaceAll("\u000314" + background, "§8");
            line = line.replaceAll("\u000312" + background, "§9");
            line = line.replaceAll("\u00030?9" + background, "§a");
            line = line.replaceAll("\u000311" + background, "§b");
            line = line.replaceAll("\u00030?4" + background, "§c");
            line = line.replaceAll("\u000313" + background, "§d");
            line = line.replaceAll("\u00030?8" + background, "§e");
            line = line.replaceAll("\u00030?0" + background, "§f");

            line = Colors.removeAll(line);
            line = Styles.removeAll(line);

            return new CString(line, t);
        }

        public String getName() {
            return "irc_irc2mc_colors";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "string {line} Return a string with irc colors converted to"
                    + " mc colors. Unknown colors will be stripped. Does not"
                    + " support styles (yet!).";
        }
    }
    
    @api
    public static class irc_user_meta extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_user_meta:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String userName = args[1].val();
            User user = bot.getUser(userName);

            if (user == null) {
                throw new CRENotFoundException("No idea who that is!", t);
            }
            
            CArray retn = new CArray(t);
            
            for (Map.Entry<String, Object> entry : user.getMetaData().entrySet()) {
                String key = entry.getKey();
                
                if (entry.getValue() instanceof Construct) {
                    Construct value = (Construct)entry.getValue();
                    retn.set(key, value, t);
                } else {
                    String value = entry.getValue().toString();
                    retn.set(key, value);
                }
            }
            
            return retn;
        }

        public String getName() {
            return "irc_user_meta";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "array {id, name} Return an array of metadata for a given user.";
        }
    }
    
    @api
    public static class irc_channel_meta extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_channel_meta:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String chanName = args[1].val();
            Channel channel = bot.getChannel(chanName);

            if (channel == null) {
                throw new CRENotFoundException("No clue about that channel!", t);
            }
            
            CArray retn = new CArray(t);
            
            for (Map.Entry<String, Object> entry : channel.getMetaData().entrySet()) {
                String key = entry.getKey();
                
                if (entry.getValue() instanceof Construct) {
                    Construct value = (Construct)entry.getValue();
                    retn.set(key, value, t);
                } else {
                    String value = entry.getValue().toString();
                    retn.set(key, value);
                }
            }
            
            return retn;
        }

        public String getName() {
            return "irc_channel_meta";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "array {id, name} Return an array of metadata for a given channel.";
        }
    }
    
    @api
    public static class irc_set_user_meta extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_set_user_meta:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String userName = args[1].val();
            User user = bot.getUser(userName);

            if (user == null) {
                throw new CRENotFoundException("No idea who that user is!", t);
            }
            
            String key = args[2].val();
            Construct value = args[3];
            
            user.setMetaData(key, value);
            
            return CNull.NULL;
        }

        public String getName() {
            return "irc_set_user_meta";
        }

        public Integer[] numArgs() {
            return new Integer[]{4};
        }

        public String docs() {
            return "array {id, name, key, value} Set metadata for a given user.";
        }
    }
    
    @api
    public static class irc_set_channel_meta extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_set_channel_meta:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String chanName = args[1].val();
            Channel channel = bot.getChannel(chanName);

            if (channel == null) {
                throw new CRENotFoundException("No idea about that channel!", t);
            }
            
            String key = args[2].val();
            Construct value = args[3];
            
            channel.setMetaData(key, value);
            
            return CNull.NULL;
        }

        public String getName() {
            return "irc_set_channel_meta";
        }

        public Integer[] numArgs() {
            return new Integer[]{4};
        }

        public String docs() {
            return "array {id, name, key, value} Set metadata for a given channel.";
        }
    }
    
    @api
    public static class irc_del_user_meta extends IrcFunc {
        @Override
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{};
        }

        public Construct exec(Target t, Environment environment,
                Construct... args) throws ConfigRuntimeException {
            verbose("CHIRC", "irc_del_user_meta:" + flatten(args), t);
            
            SocBot bot = Tracking.getConnected(args[0].val(), t);

            String userName = args[1].val();
            User user = bot.getUser(userName);

            if (user == null) {
                throw new CRENotFoundException("No idea who that user is!", t);
            }
            
            String key = args[2].val();
            
            Object retn = user.remMetaData(key);
            
            if (retn instanceof Construct) {
                return (Construct)retn;
            } else {
                return new CString(retn.toString(), t);
            }
        }

        public String getName() {
            return "irc_del_user_meta";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "mixed {id, name, key} Deletes metadata for a given user and returns it.";
        }
    }
}
