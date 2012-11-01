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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.spy.transformers.SpyTransformer;

import java.util.List;

public class SpyUtil {

    public static int max(int x, int y) {
        return x > y ? x : y;
    }

    public static SpyRecord transform(int stage, SpyDefinition sdef, SpyRecord record) {
        List<SpyTransformer> transformers = sdef.getTransformers(stage);

        for (SpyTransformer transformer : transformers) {
            if (null == (record = transformer.transform(record))) {
                break;
            }
        }

        return record;
    }

}
