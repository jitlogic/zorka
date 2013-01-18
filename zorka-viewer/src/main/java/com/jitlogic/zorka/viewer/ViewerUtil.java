/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import org.objectweb.asm.Type;

public class ViewerUtil {

    public static String nanoSeconds(long ns) {
        double t = 1.0 * ns / 1000000;
        String u = "ms";
        if (t > 1000.0) {
            t /= 1000.0;
            u = "s";
        }
        return String.format(t > 10 ? "%.0f" : "%.2f", t) + u;
    }

    public static String percent(long nom, long div) {
        double t = 100.0 * nom / div;
        return String.format(t >= 10.0 ? "%.0f" : "%.2f", t);
    }

    public static String methodString(String className, String methodName, String methodSignature) {

        if (className == null || methodName == null || methodSignature == null) {
            return "?";
        }

        Type[] argTypes = Type.getArgumentTypes(methodSignature);
        String[] classComponents = className.split("\\.");

        StringBuilder sb = new StringBuilder();

        sb.append(classComponents[classComponents.length-1]);
        sb.append(".");
        sb.append(methodName);
        sb.append("(");
        for (int i = 0; i < argTypes.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(argTypes[i].getClassName());
        }
        sb.append(")");
        return sb.toString();
    }

}
