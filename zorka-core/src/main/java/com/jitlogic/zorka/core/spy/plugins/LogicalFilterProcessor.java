/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LogicalFilterProcessor implements SpyProcessor {

    public static final int FILTER_NONE = 0;
    public static final int FILTER_OR = 1;
    public static final int FILTER_AND = 2;

    private int mode;
    private List<SpyProcessor> processors;


    public LogicalFilterProcessor(int mode, SpyProcessor...processors) {
        this(null, mode, processors);
    }


    public LogicalFilterProcessor(LogicalFilterProcessor orig, int mode, SpyProcessor...processors) {
        this.mode = mode;
        this.processors = new ArrayList<SpyProcessor>();

        if (orig != null) {
            this.processors.addAll(orig.processors);
        }

        for (SpyProcessor p : processors) {
            if (p != null) {
                this.processors.add(p);
            }
        }
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Map<String, Object> rec = record;

        for (SpyProcessor sp : processors) {
            rec = sp.process(rec != null ? rec : record);
            if (rec == null && mode != FILTER_OR) {
                return mode == FILTER_NONE ? record : null;
            }
            if (rec != null && mode == FILTER_OR) {
                return rec;
            }
        }

        return rec;
    }


    public LogicalFilterProcessor with(SpyProcessor...processors) {
        return new LogicalFilterProcessor(this, this.mode, processors);
    }

}
