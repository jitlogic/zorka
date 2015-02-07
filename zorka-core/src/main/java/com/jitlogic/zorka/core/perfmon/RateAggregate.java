/** 
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.perfmon;

import java.util.LinkedList;

import com.jitlogic.zorka.common.util.ZorkaUtil;

/**
 * This is old rate aggregate object.
 */
public class RateAggregate {

    /** To be removed */
	private static class Sample {
		private final long nom, div, time;
		public Sample(long nom, long div, long time) {
			this.nom = nom;
            this.div = div;
            this.time = time;
		}
	}

    /** To be removed */
    private final long horizon;

    /** To be removed */
    private final LinkedList<Sample> samples;

    /** To be removed */
    private final double defVal;

    /** To be removed */
    private final double multiplier;

    /** To be removed */
    private ZorkaUtil util = ZorkaUtil.getInstance();

    /** To be removed */
    public RateAggregate(long horizon, double defVal) {
		this(horizon, defVal, 1.0);
	}

    /** To be removed */
    public RateAggregate(long horizon, double defVal, double multiplier) {
		this.horizon = horizon;
		this.samples = new LinkedList<Sample>();
		this.defVal = defVal;
		this.multiplier = multiplier;
	}


    /** To be removed */
    public void feed(long v) {
		feed(v, 0);
	}


    /** To be removed */
    public void feed(long nom, long div) {
		slide();
		samples.addLast(new Sample(nom, div, util.currentTimeMillis()));
	}


    /** To be removed */
    public double rate() {
		slide();
		
		if (samples.size() == 0) {
			return defVal;
		}
		
		Sample s1 = samples.getFirst(), s2 = samples.getLast();
		long nom = s2.nom - s1.nom, div = s2.div - s1.div;
		
		return multiplier * (div != 0 ? 1.0 * nom / div : nom); 
	}


    /** To be removed */
    public void slide() {
		long tst = util.currentTimeMillis() - horizon;		
		
		for (;;) {
			if (samples.size() == 0) {
				break;
			}
			Sample s = samples.getFirst();
			if (s.time >= tst) {
				break;
			}
			samples.removeFirst();
		}
	}
}
