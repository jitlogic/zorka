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
package com.jitlogic.zorka.normproc;

public class XqlToken {

    public final static int UNKNOWN     = 0;
    public final static int WHITESPACE  = 1;
    public final static int SYMBOL      = 2;
    public final static int OPERATOR    = 3;
    public final static int LITERAL     = 4;
    public final static int COMMENT     = 5;
    public final static int KEYWORD     = 6;
    public final static int PLACEHOLDER = 7;

    private final static String[] prefixes = { "U", "W", "S", "O", "L", "C", "K", "P" };

    private int type;
    private String content;

    public XqlToken(int type, String content) {
        this.type = type >= 0 ? type : UNKNOWN;
        this.content = content != null ? content : "";
    }

    public int getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int hashCode() {
        return content.hashCode() + type;
    }

    public boolean equals(Object obj) {
        return obj instanceof XqlToken &&
                ((XqlToken)obj).type == type &&
                ((XqlToken)obj).content.equals(content);
    }

    public String toString() {
        return prefixes[type] + "(" + content + ")";
    }
}
