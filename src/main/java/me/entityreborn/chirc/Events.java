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
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.events.AbstractEvent;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.exceptions.EventException;
import com.laytonsmith.core.exceptions.PrefilterNonMatchException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.entityreborn.socbot.api.SocBot;
import me.entityreborn.socbot.api.events.CTCPEvent;
import me.entityreborn.socbot.api.events.DisconnectedEvent;
import me.entityreborn.socbot.api.events.JoinEvent;
import me.entityreborn.socbot.api.events.PrivmsgEvent;
import me.entityreborn.socbot.api.events.WelcomeEvent;
import me.entityreborn.socbot.events.EventHandler;
import me.entityreborn.socbot.events.Listener;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Events implements Listener {
    @EventHandler
    public void handleDisconnect(DisconnectedEvent e) {
        final Disconnected event = new Disconnected(e);
        try {
            StaticLayer.GetConvertor().runOnMainThreadAndWait(new Callable<Object>() {
                public Object call() {
                    EventUtils.TriggerListener(Driver.EXTENSION, "irc_disconnected", event);
                    return null;
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(Events.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @EventHandler
    public void handlePrivMsg(PrivmsgEvent e) {
        final PrivMsg event = new PrivMsg(e);
        try {
            StaticLayer.GetConvertor().runOnMainThreadAndWait(new Callable<Object>() {
                public Object call() {
                    EventUtils.TriggerListener(Driver.EXTENSION, "irc_msg", event);
                    return null;
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(Events.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @EventHandler
    public void handleCTCP(CTCPEvent e) {
        if ("ACTION".equalsIgnoreCase(e.getType())) {
            final Action event = new Action(e);
            try {
                StaticLayer.GetConvertor().runOnMainThreadAndWait(new Callable<Object>() {
                    public Object call() {
                        EventUtils.TriggerListener(Driver.EXTENSION, "irc_action", event);
                        return null;
                    }
                });
            } catch (Exception ex) {
                Logger.getLogger(Events.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @EventHandler
    public void handleWelcome(WelcomeEvent e) {
        final Welcome event = new Welcome(e);
        try {
            StaticLayer.GetConvertor().runOnMainThreadAndWait(new Callable<Object>() {
                public Object call() {
                    EventUtils.TriggerListener(Driver.EXTENSION, "irc_welcomed", event);
                    return null;
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(Events.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @EventHandler
    public void handleJoined(JoinEvent e) {
        final Join event = new Join(e);
        try {
            StaticLayer.GetConvertor().runOnMainThreadAndWait(new Callable<Object>() {
                public Object call() {
                    EventUtils.TriggerListener(Driver.EXTENSION, "irc_joined", event);
                    return null;
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(Events.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static class Disconnected implements BindableEvent {
        private final DisconnectedEvent event;

        public Disconnected(DisconnectedEvent event) {
            this.event = event;
        }
        
        public Object _GetObject() {
            return this;
        }
        
        public SocBot getBot() {
            return event.getBot();
        }
        
        public boolean wasClean() {
            return event.wasClean();
        }
    }
    
    private static class Welcome implements BindableEvent {
        private final WelcomeEvent event;

        public Welcome(WelcomeEvent e) {
            event = e;
        }
        
        public Object _GetObject() {
            return this;
        }
        
        public SocBot getBot() {
            return event.getBot();
        }
    }
    
    private static class Join implements BindableEvent {
        private final JoinEvent event;

        public Join(JoinEvent e) {
            event = e;
        }
        
        public Object _GetObject() {
            return this;
        }
        
        public SocBot getBot() {
            return event.getBot();
        }
        
        public String getWho() {
            return event.getUser().getName();
        }
        
        public String getChannel() {
            return event.getChannel().getName();
        }
    }
    
    private static class PrivMsg implements BindableEvent {
        private final PrivmsgEvent event;

        public PrivMsg(PrivmsgEvent e) {
            event = e;
        }
        
        public Object _GetObject() {
            return this;
        }
        
        public SocBot getBot() {
            return event.getBot();
        }
        
        public String getWho() {
            return event.getSender().getName();
        }
        
        public String getTarget() {
            return event.getTarget().getName();
        }
        
        public String getMessage() {
            return event.getMessage();
        }
    }
    
    private static class Action implements BindableEvent {
        private final CTCPEvent event;

        public Action(CTCPEvent e) {
            event = e;
        }
        
        public Object _GetObject() {
            return this;
        }
        
        public SocBot getBot() {
            return event.getBot();
        }
        
        public String getWho() {
            return event.getSender().getName();
        }
        
        public String getTarget() {
            return event.getTarget().getName();
        }
        
        public String getMessage() {
            return event.getMessage();
        }
    }
    
    private abstract static class IrcEvent extends AbstractEvent {
        public String docs() {
            return ""; //TBA
        }

        public boolean matches(Map<String, Construct> prefilter, BindableEvent e) throws PrefilterNonMatchException {
            return true;
        }

        public BindableEvent convert(CArray manualObject) {
            return null;
        }
        
        public Driver driver() {
            return Driver.EXTENSION;
        }

        public boolean modifyEvent(String key, Construct value, BindableEvent event) throws ConfigRuntimeException {
            return false;
        }

        public Version since() {
            return CHVersion.V3_3_1;
        }
    }
    
    @api
    public static class irc_disconnected extends IrcEvent {

        public String getName() {
            return "irc_disconnected";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();
            
            if (e instanceof Disconnected) {
                Disconnected msg = (Disconnected)e;
                
                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("wasClean", new CBoolean(msg.wasClean(), Target.UNKNOWN));
            }
            
            return retn;
        }
    }
    
    @api
    public static class irc_msg extends IrcEvent {

        public String getName() {
            return "irc_msg";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();
            
            if (e instanceof PrivMsg) {
                PrivMsg msg = (PrivMsg)e;
                
                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("who", new CString(msg.getWho(), Target.UNKNOWN));
                retn.put("target", new CString(msg.getTarget(), Target.UNKNOWN));
                retn.put("message", new CString(msg.getMessage(), Target.UNKNOWN));
            }
            
            return retn;
        }
    }
    
    @api
    public static class irc_action extends IrcEvent {

        public String getName() {
            return "irc_action";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();
            
            if (e instanceof Action) {
                Action msg = (Action)e;
                
                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("who", new CString(msg.getWho(), Target.UNKNOWN));
                retn.put("target", new CString(msg.getTarget(), Target.UNKNOWN));
                retn.put("message", new CString(msg.getMessage(), Target.UNKNOWN));
            }
            
            return retn;
        }
    }
    
    @api
    public static class irc_welcomed extends IrcEvent {

        public String getName() {
            return "irc_welcomed";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();
            
            if (e instanceof Welcome) {
                Welcome msg = (Welcome)e;
            }
            
            return retn;
        }
    }
    
    @api
    public static class irc_joined extends IrcEvent {

        public String getName() {
            return "irc_joined";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();
            
            if (e instanceof Join) {
                Join msg = (Join)e;
                
                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("who", new CString(msg.getWho(), Target.UNKNOWN));
                retn.put("channel", new CString(msg.getChannel(), Target.UNKNOWN));
            }
            
            return retn;
        }
    }
}
