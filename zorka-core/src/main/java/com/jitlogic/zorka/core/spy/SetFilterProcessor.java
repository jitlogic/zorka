/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.spy;

import java.util.Map;
import java.util.Set;


public class SetFilterProcessor implements SpyProcessor {

    private String srcField;
    private boolean invert;
    private Set<?> candidates;

    public SetFilterProcessor(String srcField, boolean invert, Set<?> candidates) {
        this.srcField = srcField;
        this.invert = invert;
        this.candidates = candidates;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        return candidates.contains(record.get(srcField)) ^ invert ? record : null;
    }
}
