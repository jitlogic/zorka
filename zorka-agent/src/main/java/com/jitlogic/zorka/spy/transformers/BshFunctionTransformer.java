/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy.transformers;

import bsh.This;
import com.jitlogic.zorka.spy.InstrumentationContext;

public class BshFunctionTransformer implements SpyTransformer {

    public BshFunctionTransformer(This ns, String funcName) {

    }

    public Object[] transform(Object... args) {
        return new Object[0];  // TODO
    }

    public BshFunctionTransformer parametrize(InstrumentationContext ctx) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
