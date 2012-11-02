/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy;

/**
 * This is API for zorka users.
 */
public class SpyLib {

	private SpyInstance instance;


	public SpyLib(SpyInstance instance) {
        this.instance = instance;
	}


    public void add(SpyDefinition sdef) {
        instance.add(sdef);
    }


    public SpyDefinition empty() {
        return SpyDefinition.newInstance();
    }


    public SpyDefinition instrument() {
        return SpyDefinition.instrument().onSubmit().timeDiff(0,1,1);
    }

    // TODO tutaj instrument() z argumentem-kluczem

    // TODO wyrugować SpyDefinition.instrument()
}
