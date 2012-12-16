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
package com.jitlogic.zorka.logproc;

import com.jitlogic.zorka.util.ZorkaLogLevel;

import java.util.regex.Pattern;

public class LogFilter implements LogProcessor {

    public final static int FILTER_CLASS = 1;
    public final static int FILTER_METHOD = 2;
    public final static int FILTER_MESSAGE = 3;
    public final static int FILTER_EXCEPTION = 4;

    private int type;
    private Pattern pattern;


    public LogFilter(int type, String filter) {
        this.type = type;
        this.pattern = filter.startsWith("~") ? Pattern.compile(filter.substring(1)) :
            Pattern.compile("^" + Pattern.compile(
                filter.replace(".", "\\.").replace("**", ".+").replace("*", "[a-zA-Z0-9_]+")) + "$");
    }


    public LogRecord process(LogRecord rec) {
        String s = "";

        switch (type) {
            case FILTER_CLASS:
                s = rec.getOriginClass(); break;
            case FILTER_METHOD:
                s = rec.getOriginMethod(); break;
            case FILTER_MESSAGE:
                s = rec.getMessage();
            case FILTER_EXCEPTION:
                s = rec.getException() != null ? rec.getException().getClass().getName() : "";
        }

        return pattern.matcher(s).matches() ? rec : null;
    }
}
