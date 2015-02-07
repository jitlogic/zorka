/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.util;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.net.URL;
import java.util.regex.Pattern;

public class OverlayClassLoader extends ClassLoader {

    private static final ZorkaLog log = ZorkaLogger.getLog(OverlayClassLoader.class);

    private ClassLoader overlay;
    private Pattern pattern;

    public OverlayClassLoader(ClassLoader parent, String pattern, ClassLoader overlay) {
        super(parent);
        this.pattern = Pattern.compile("^"+pattern.replace("**", ".+").replace("*", "[^\\.]+"));
        this.overlay = overlay;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (pattern.matcher(name).matches()) {
            log.info(ZorkaLogger.ZAG_TRACE, "Loading class " + name + " via overlay.");
            return overlay.loadClass(name);
        } else {
            return super.loadClass(name, resolve);
        }
    }

    @Override
    public URL getResource(String path) {
        if (pattern.matcher(path).matches()) {
            return overlay.getResource(path);
        } else {
            return super.getResource(path);
        }
    }

}
