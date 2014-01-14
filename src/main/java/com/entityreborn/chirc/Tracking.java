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

import com.entityreborn.socbot.SocBot;
import com.entityreborn.socbot.eventsystem.EventManager;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.extensions.AbstractExtension;
import com.laytonsmith.core.extensions.MSExtension;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import java.util.HashMap;
import java.util.Map;
import static com.entityreborn.chirc.Utils.getVersion;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
@MSExtension("CHIRC")
public class Tracking extends AbstractExtension {
    private static final Map<String, SocBot> bots = new HashMap<String, SocBot>();
    private static final Events events = new Events();
    
    @Override
    public void onStartup() {
        Utils.log("CHIRC", "v." + getVersion() + " loaded.", Target.UNKNOWN);
    }
    
    public void onShutdown() {
        Utils.log("CHIRC", "v." + getVersion() + " stopping...", Target.UNKNOWN);
        
        for (SocBot bot : bots.values()) {
            bot.disconnect(true);
        }
        
        bots.clear();
        
        Utils.log("CHIRC", "v." + getVersion() + " stopped", Target.UNKNOWN);
                
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
        
        Utils.verbose("CHIRC", "Creating irc bot with id " + id, Target.UNKNOWN);
        
        SocBot bot = new SocBot(id.toLowerCase());
        
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
            Utils.verbose("CHIRC", "Destroying bot with id " + id, t);
            
            bot.disconnect(true);
        } else {
            throw new ConfigRuntimeException("That id doesn't exist!",
                    ExceptionType.NotFoundException, t);
        }
    }
}
