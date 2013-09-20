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
    private static final Map<SocBot, String> ids = new HashMap<SocBot, String>();
    private static final Events events = new Events();
    
    private static String VERSION;

    static {
        Package p = Tracking.class.getPackage();

        if (p == null) {
            p = Package.getPackage("me.entityreborn.chirc");
        }

        VERSION = "(unknown)";

        if (p != null) {
            String v = p.getImplementationVersion();

            if (v != null) {
                VERSION = v;
            }
        }
    }

    public static String getVersion() {
        return VERSION;
    }
    
    @startup
    public static void startup() {
        System.out.println("CHIRC v." + VERSION + " loaded.");
    }
    
    @shutdown
    public static void shutdown() {
        System.out.println("CHIRC v." + VERSION + " stopping...");
        
        for (SocBot bot : bots.values()) {
            bot.disconnect();
        }
        
        bots.clear();
        ids.clear();
        
        System.out.println("CHIRC v." + VERSION + " stopped");
    }
    
    public static SocBot create(String id) {
        if (bots.containsKey(id)) {
            return null;
        }
        
        SocBot bot = new Core();
        
        EventManager.registerEvents(events, bot);
        
        bots.put(id.toLowerCase(), bot);
        ids.put(bot, id.toLowerCase());
        
        return bot;
    }
    
    public static SocBot get(String id) {
        return bots.get(id.toLowerCase());
    }
    
    public static String getId(SocBot bot) {
        return ids.get(bot);
    }
    
    public static void destroy(String id) {
        SocBot bot = bots.remove(id.toLowerCase());
        
        if (bot != null) {
            bot.disconnect();
            ids.remove(bot);
        }
    }
    
    public static void main(String[] strings) {
        System.out.println(VERSION);
    }
}
