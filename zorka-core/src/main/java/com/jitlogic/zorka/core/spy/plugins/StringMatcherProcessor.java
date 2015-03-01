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
package com.jitlogic.zorka.core.spy.plugins;


import com.jitlogic.zorka.common.util.StringMatcher;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.List;
import java.util.Map;

public class StringMatcherProcessor implements SpyProcessor {

    private String srcField;
    private StringMatcher matcher;


    public StringMatcherProcessor(String srcField, List<String> includes, List<String> excludes) {
        this.srcField = srcField;
        matcher = new StringMatcher(includes, excludes);
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object v = record.get(srcField);

        if (v instanceof String) {
            return matcher.matches(v.toString()) ? record : null;
        }

        return matcher.isInclusive() ? record : null;
    }
}
