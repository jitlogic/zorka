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

package com.jitlogic.zorka.ranklist;

import com.jitlogic.zorka.agent.rankproc.RateAggregate;


public class RankAttr<K,V> {

	private final String name;
	private final String description;
	private final long horizon;
	private final double multiplier;
	
	private final String nominalAttr;
	private final String dividerAttr;
	
	private final int nominalOffs, dividerOffs;

	
	public RankAttr(RankLister<K,V> lister, String name, String description, long horizon, double multiplier, String nominalAttr, String dividerAttr) {
		this.name = name;
		this.description = description;
		this.horizon = horizon;
		this.multiplier = multiplier;
		this.nominalAttr = nominalAttr;
		this.dividerAttr = dividerAttr;
		//this.nominalOffs = lister.attrIndex(nominalAttr);
		//this.dividerOffs = lister.attrIndex(dividerAttr);
		this.nominalOffs = 0;
		this.dividerOffs = 0;
	}
	
	public RateAggregate newAggregate() {
		return new RateAggregate(horizon, 0.0, multiplier);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public long getHorizon() {
		return horizon;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public String getNominalAttr() {
		return nominalAttr;
	}

	public String getDividerAttr() {
		return dividerAttr;
	}

	public int getNominalOffs() {
		return nominalOffs;
	}

	public int getDividerOffs() {
		return dividerOffs;
	}
	
}
