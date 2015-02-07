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

package com.jitlogic.zorka.viewer;

import java.io.File;
import java.util.regex.Pattern;

public class ViewerUtil {

    private static final Pattern RE_DOT = Pattern.compile("\\.");

    public static String shortClassName(String className) {
        String[] segs = RE_DOT.split(className != null ? className : "");
        return segs[segs.length-1];
    }

    public static File usableDir(File path) {

        while (!path.isDirectory()) {
            File newPath = new File(path.getParent());
            if (newPath.equals(path)) {
                return new File(System.getProperty("user.home"));
            } else {
                path = newPath;
            }
        }

        return path;
    }
}
