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

import com.entityreborn.socbot.Numerics;
import com.entityreborn.socbot.Packet;
import com.entityreborn.socbot.SocBot;
import com.entityreborn.socbot.events.CTCPEvent;
import com.entityreborn.socbot.events.ConnectedEvent;
import com.entityreborn.socbot.events.DisconnectedEvent;
import com.entityreborn.socbot.events.ErrorEvent;
import com.entityreborn.socbot.events.JoinEvent;
import com.entityreborn.socbot.events.NickEvent;
import com.entityreborn.socbot.events.NickInUseEvent;
import com.entityreborn.socbot.events.NumericEvent;
import com.entityreborn.socbot.events.PacketReceivedEvent;
import com.entityreborn.socbot.events.PartEvent;
import com.entityreborn.socbot.events.PrivmsgEvent;
import com.entityreborn.socbot.events.QuitEvent;
import com.entityreborn.socbot.events.WelcomeEvent;
import com.entityreborn.socbot.eventsystem.EventHandler;
import com.entityreborn.socbot.eventsystem.Listener;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CInt;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Events implements Listener {
    public void fireEvent(final String name, final BindableEvent evt) {
        EventUtils.TriggerListener(Driver.EXTENSION, name, evt);
    }

    @EventHandler
    public void handleDisconnect(DisconnectedEvent e) {
        final Disconnected event = new Disconnected(e);
        fireEvent("irc_disconnected", event);
    }

    @EventHandler
    public void handleError(ErrorEvent e) {
        final Error event = new Error(e);
        fireEvent("irc_error", event);
    }

    @EventHandler
    public void handleNick(NickEvent e) {
        final Nick event = new Nick(e);
        fireEvent("irc_nick_changed", event);
    }
    
    @EventHandler
    public void handleNickInUse(NickInUseEvent e) {
        final NickInUse event = new NickInUse(e);
        fireEvent("irc_nick_in_use", event);
    }
    
    @EventHandler
    public void handleNumeric(NumericEvent e) {
        final Numeric event = new Numeric(e);
        fireEvent("irc_numeric", event);
    }

    @EventHandler
    public void handlePacketRecv(PacketReceivedEvent e) {
        final RecvLine event = new RecvLine(e);
        fireEvent("irc_recv_raw", event);
    }

    @EventHandler
    public void handleConnect(ConnectedEvent e) {
        final Connected event = new Connected(e);
        fireEvent("irc_connected", event);
    }

    @EventHandler
    public void handlePrivMsg(PrivmsgEvent e) {
        final PrivMsg event = new PrivMsg(e);
        fireEvent("irc_msg", event);
    }

    @EventHandler
    public void handleCTCP(CTCPEvent e) {
        if ("ACTION".equalsIgnoreCase(e.getType())) {
            final Action event = new Action(e);
            fireEvent("irc_action", event);
        }
    }

    @EventHandler
    public void handleWelcome(WelcomeEvent e) {
        final Welcome event = new Welcome(e);
        fireEvent("irc_welcomed", event);
    }

    @EventHandler
    public void handleJoined(JoinEvent e) {
        final Join event = new Join(e);
        fireEvent("irc_joined", event);
    }

    @EventHandler
    public void handleQuit(QuitEvent e) {
        final Quit event = new Quit(e);
        fireEvent("irc_quit", event);
    }

    @EventHandler
    public void handleParted(PartEvent e) {
        final Part event = new Part(e);
        fireEvent("irc_parted", event);
    }

    private static class Error implements BindableEvent {
        private final ErrorEvent event;

        public Object _GetObject() {
            return this;
        }

        public Error(ErrorEvent evt) {
            event = evt;
        }

        public SocBot getBot() {
            return event.getBot();
        }

        public String getMessage() {
            return event.getMessage();
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

    private static class Connected implements BindableEvent {
        private final ConnectedEvent event;

        public Connected(ConnectedEvent event) {
            this.event = event;
        }

        public Object _GetObject() {
            return this;
        }

        public SocBot getBot() {
            return event.getBot();
        }

        public String getServer() {
            return event.getServer();
        }

        public int getPort() {
            return event.getPort();
        }
    }

    private static class Nick implements BindableEvent {
        private final NickEvent event;

        public Nick(NickEvent event) {
            this.event = event;
        }

        public Object _GetObject() {
            return this;
        }

        public SocBot getBot() {
            return event.getBot();
        }

        public String getOld() {
            return event.getUser().getLastNick();
        }

        public String getNew() {
            return event.getNewNick();
        }
    }
    
    private static class NickInUse implements BindableEvent {
        private final NickInUseEvent event;

        public NickInUse(NickInUseEvent event) {
            this.event = event;
        }

        public Object _GetObject() {
            return this;
        }

        public SocBot getBot() {
            return event.getBot();
        }

        public String getNick() {
            return event.getNick();
        }
    }
    
    private static class Numeric implements BindableEvent {
        private final NumericEvent event;

        public Numeric(NumericEvent event) {
            this.event = event;
        }

        public Object _GetObject() {
            return this;
        }

        public SocBot getBot() {
            return event.getBot();
        }

        public Numerics.Numeric getNumeric() {
            return event.getNumeric();
        }
        
        public String getMessage() {
            return event.getMessage();
        }
        
        public List<String> getArgs() {
            return event.getPacket().getArgs();
        }
    }

    protected static class ConnectionException implements BindableEvent {
        private final IOException exception;
        private final SocBot bot;

        public ConnectionException(IOException e, SocBot b) {
            exception = e;
            bot = b;
        }

        public Object _GetObject() {
            return this;
        }

        public SocBot getBot() {
            return bot;
        }

        public String className() {
            return exception.getClass().getSimpleName();
        }

        public String getMessage() {
            return exception.getMessage();
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

    private static class Quit implements BindableEvent {
        private final QuitEvent event;

        public Quit(QuitEvent e) {
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

        public String getQuitMessage() {
            return event.getQuitMessage();
        }
    }

    private static class RecvLine implements BindableEvent {
        private final PacketReceivedEvent event;

        public RecvLine(PacketReceivedEvent e) {
            event = e;
        }

        public Object _GetObject() {
            return this;
        }

        public SocBot getBot() {
            return event.getBot();
        }

        public Packet getPacket() {
            return event.getPacket();
        }
    }

    private static class Part implements BindableEvent {
        private final PartEvent event;

        public Part(PartEvent e) {
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
                Disconnected msg = (Disconnected) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("wasClean", new CBoolean(msg.wasClean(), Target.UNKNOWN));
            }

            return retn;
        }
    }

    @api
    public static class irc_nick extends IrcEvent {
        public String getName() {
            return "irc_nick_changed";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof Nick) {
                Nick msg = (Nick) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("oldnick", new CBoolean(msg.getOld(), Target.UNKNOWN));
                retn.put("newnick", new CBoolean(msg.getNew(), Target.UNKNOWN));
            }

            return retn;
        }
    }
    
    @api
    public static class irc_nick_in_use extends IrcEvent {
        public String getName() {
            return "irc_nick_in_use";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof NickInUse) {
                NickInUse msg = (NickInUse) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("nick", new CBoolean(msg.getNick(), Target.UNKNOWN));
            }

            return retn;
        }
    }
    
    @api
    public static class irc_numeric extends IrcEvent {
        public String getName() {
            return "irc_numeric";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof Numeric) {
                Numeric msg = (Numeric) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("numeric", new CBoolean(msg.getNumeric().getName(), Target.UNKNOWN));
                retn.put("numericid", new CInt(msg.getNumeric().getCode(), Target.UNKNOWN));
                retn.put("args", Construct.GetConstruct(msg.getArgs()));
                retn.put("message", new CString(msg.getMessage(), Target.UNKNOWN));
            }

            return retn;
        }
    }

    @api
    public static class irc_connected extends IrcEvent {
        public String getName() {
            return "irc_connected";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof Connected) {
                Connected msg = (Connected) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("server", new CString(msg.getServer(), Target.UNKNOWN));
                retn.put("port", new CInt(msg.getPort(), Target.UNKNOWN));
            }

            return retn;
        }
    }

    @api
    public static class irc_connection_exception extends IrcEvent {
        public String getName() {
            return "irc_connection_exception";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof ConnectionException) {
                ConnectionException msg = (ConnectionException) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("message", new CBoolean(msg.getMessage(), Target.UNKNOWN));
                retn.put("exceptionclass", new CString(msg.className(), Target.UNKNOWN));
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
                PrivMsg msg = (PrivMsg) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("who", new CString(msg.getWho(), Target.UNKNOWN));
                retn.put("target", new CString(msg.getTarget(), Target.UNKNOWN));
                retn.put("message", new CString(msg.getMessage(), Target.UNKNOWN));
            }

            return retn;
        }
    }

    @api
    public static class irc_error extends IrcEvent {
        public String getName() {
            return "irc_error";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof Error) {
                Error msg = (Error) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("message", new CString(msg.getMessage(), Target.UNKNOWN));
            }

            return retn;
        }
    }

    @api
    public static class irc_recv_raw extends IrcEvent {
        public String getName() {
            return "irc_recv_raw";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof RecvLine) {
                RecvLine msg = (RecvLine) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("line", new CString(msg.getPacket().getOriginalLine(), Target.UNKNOWN));
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
                Action msg = (Action) e;

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
                Welcome msg = (Welcome) e;
                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
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
                Join msg = (Join) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("who", new CString(msg.getWho(), Target.UNKNOWN));
                retn.put("channel", new CString(msg.getChannel(), Target.UNKNOWN));
            }

            return retn;
        }
    }

    @api
    public static class irc_quit extends IrcEvent {
        public String getName() {
            return "irc_quit";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof Quit) {
                Quit msg = (Quit) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("who", new CString(msg.getWho(), Target.UNKNOWN));
                retn.put("message", new CString(msg.getQuitMessage(), Target.UNKNOWN));
            }

            return retn;
        }
    }

    @api
    public static class irc_parted extends IrcEvent {
        public String getName() {
            return "irc_parted";
        }

        public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
            Map<String, Construct> retn = new HashMap<String, Construct>();

            if (e instanceof Part) {
                Part msg = (Part) e;

                retn.put("id", new CString(msg.getBot().getID(), Target.UNKNOWN));
                retn.put("who", new CString(msg.getWho(), Target.UNKNOWN));
                retn.put("channel", new CString(msg.getChannel(), Target.UNKNOWN));
            }

            return retn;
        }
    }
}
