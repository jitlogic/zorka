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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.test.spy.support;

import com.jitlogic.zorka.core.spy.SpySubmitter;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.ArrayList;
import java.util.List;

public class TestSubmitter implements SpySubmitter {

    public static class SubmitEntry {
        public final int stage;
        public final int id;
        public final int submitFlags;
        private final Object[] vals;

        public SubmitEntry(int stage, int id, int submitFlags, Object[] vals) {
            this.stage = stage;
            this.id = id;
            this.submitFlags = submitFlags;
            this.vals = vals != null ? ZorkaUtil.copyArray(vals) : null;
        }

        public Object get(int idx) {
            return vals[idx];
        }

        public int size() {
            return vals != null ? vals.length : 0;
        }

        public boolean nullVals() {
            return this.vals == null;
        }
    }

    private List<SubmitEntry> entries = new ArrayList<SubmitEntry>();

    public void submit(int stage, int id, int submitFlags, Object[] vals) {
        entries.add(new SubmitEntry(stage, id, submitFlags, vals));
    }

    public SubmitEntry get(int idx) {
        return entries.get(idx);
    }

    public int size() {
        return entries.size();
    }
}
