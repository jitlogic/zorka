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

public class Token {


    private final static String[] prefixes = { "U", "W", "S", "O", "L", "C", "K", "P" };

    private int type;
    private String content;

    public Token(int type, String content) {
        this.type = type >= 0 ? type : 0;
        this.content = content != null ? content : "";
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public int hashCode() {
        return content.hashCode() + type;
    }

    public boolean equals(Object obj) {
        return obj instanceof Token &&
                ((Token)obj).type == type &&
                ((Token)obj).content.equals(content);
    }

    public String toString() {
        return prefixes[type] + "(" + content + ")";
    }
}
