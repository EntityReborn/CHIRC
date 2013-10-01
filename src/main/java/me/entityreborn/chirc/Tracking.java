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

import com.laytonsmith.annotations.shutdown;
import com.laytonsmith.annotations.startup;
import com.laytonsmith.core.CHLog;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import java.util.HashMap;
import java.util.Map;
import me.entityreborn.socbot.api.SocBot;
import me.entityreborn.socbot.core.Core;
import me.entityreborn.socbot.events.EventManager;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Tracking {
    private static final Map<String, SocBot> bots = new HashMap<String, SocBot>();
    private static final Events events = new Events();
    
    private static final String VERSION = "0.0.0";
    
    @startup
    public static void startup() {
        log("CHIRC v." + VERSION + " loaded.", Target.UNKNOWN);
    }
    
    @shutdown
    public static void shutdown() {
        log("CHIRC v." + VERSION + " stopping...", Target.UNKNOWN);
        
        for (SocBot bot : bots.values()) {
            bot.disconnect(true);
        }
        
        bots.clear();
        
        log("CHIRC v." + VERSION + " stopped", Target.UNKNOWN);
    }
    
    public static void log(String line, Target targ) {
        CHLog.GetLogger().i(CHLog.Tags.EXTENSIONS, "[CHIRC] " + line, targ);
    }
    
    public static void verbose(String line, Target targ) {
        CHLog.GetLogger().v(CHLog.Tags.EXTENSIONS, "[CHIRC] " + line, targ);
    }
    
    public static String flatten(Construct... args) {
        String retn = "";
        
        for (int i = 0; i < args.length; i++) {
            retn += args[i].val();
            
            if (i != args.length - 1) {
                retn += ", ";
            }
        }
        
        return retn;
    }
    
    public static SocBot create(String id) {
        if (bots.containsKey(id.toLowerCase())) {
            return null;
        }
        
        log("Creating irc bot with id " + id, Target.UNKNOWN);
        
        SocBot bot = new Core(id.toLowerCase());
        
        EventManager.registerEvents(events, bot);
        
        bots.put(id.toLowerCase(), bot);
        
        return bot;
    }
    
    public static SocBot get(String id, Target t) {
        SocBot bot = bots.get(id.toLowerCase());
        
        if (bot == null) {
            throw new ConfigRuntimeException("That id doesn't exist!",
                    ExceptionType.NotFoundException, t);
        }
        
        return bot;
    }
    
    public static SocBot getConnected(String id, Target t) {
        SocBot bot = get(id, t);
        
        if (!bot.isConnected()) {
            throw new ConfigRuntimeException("This bot is not connected!",
                        ExceptionType.IOException, t);
        }
        
        return bot;
    }
    
    public static void destroy(String id, Target t) {
        SocBot bot = bots.remove(id.toLowerCase());
        
        if (bot != null) {
            log("Destroying bot with id " + id, Target.UNKNOWN);
            
            bot.disconnect(true);
        } else {
            throw new ConfigRuntimeException("That id doesn't exist!",
                    ExceptionType.NotFoundException, t);
        }
    }
}
