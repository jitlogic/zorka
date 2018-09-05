/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.spy.support.cbor;


import com.jitlogic.zorka.cbor.SimpleValResolver;

import static com.jitlogic.zorka.cbor.TraceDataFormat.TRACE_DROP_TOKEN;

public class TestValResolver implements SimpleValResolver {

    public static enum Tokens {
        TRACE_DROP;
    }

    @Override
    public Object resolve(int sv) {
        switch (sv) {
            case TRACE_DROP_TOKEN:
                return Tokens.TRACE_DROP;
            default:
                return null;
        }
    }
}


