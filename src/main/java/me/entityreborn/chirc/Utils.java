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

import com.laytonsmith.core.CHLog;
import com.laytonsmith.core.constructs.Target;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Utils {
    
    private static String VERSION;
    
    static {
        Package p = Utils.class.getPackage();

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
    
    public static void log(String tag, String line, Target targ) {
        CHLog.GetLogger().i(CHLog.Tags.EXTENSIONS, "[" + tag + "] " + line, targ);
    }
    
    public static void verbose(String tag, String line, Target targ) {
        CHLog.GetLogger().v(CHLog.Tags.EXTENSIONS, "[" + tag + "] " + line, targ);
    }
    
}
