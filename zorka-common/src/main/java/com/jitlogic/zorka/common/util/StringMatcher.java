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
package com.jitlogic.zorka.common.util;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class StringMatcher {

    private Set<String> strIncludes = new HashSet<String>();
    private Set<String> strExcludes = new HashSet<String>();

    private List<Pattern> reIncludes = new ArrayList<Pattern>();
    private List<Pattern> reExcludes = new ArrayList<Pattern>();

    public StringMatcher(List<String> includes, List<String> excludes) {

        if (includes != null) {
            for (String inc : includes) {
                if (inc.startsWith("~")) {
                    reIncludes.add(Pattern.compile(inc.substring(1)));
                } else {
                    strIncludes.add(inc);
                }
            }
        }

        if (excludes != null) {
            for (String exc : excludes) {
                if (exc.startsWith("~")) {
                    reExcludes.add(Pattern.compile(exc.substring(1)));
                } else {
                    strExcludes.add(exc);
                }
            }
        }
    }


    public boolean matches(String s) {

        if (strExcludes.contains(s)) {
            return false;
        }

        for (Pattern p : reExcludes) {
            if (p.matcher(s).matches()) {
                return false;
            }
        }

        if (strIncludes.size() > 0 || reIncludes.size() > 0) {

            if (strIncludes.contains(s)) {
                return true;
            }

            for (Pattern p : reIncludes) {
                if (p.matcher(s).matches()) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }


    public boolean isInclusive() {
        return strIncludes.size() == 0 && reIncludes.size() == 0;
    }

}
