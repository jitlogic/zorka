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

package com.jitlogic.zorka.rankproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import com.jitlogic.zorka.util.ZorkaUtil;


/**
 * Represents gneeric item for ranking. 
 * 
 * @author Rafal Lewczuk <rle@jitlogic.com>
 *
 */
public class OldRankItem<K,V> implements CompositeData {

	private OldRateAggregate[] rates;
	private Object[] values;
	private CompositeType type;
	private OldRankLister<K,V> lister;
	private long lastGen = 0L;
	
	public OldRankItem(OldRankLister<K, V> lister, CompositeType type, Object[] values, OldRateAggregate[] rates) {
		this.lister = lister;
		this.type = type;
		this.values = values.clone();
		this.rates = rates.clone();
	}
	
	public void updateGen(long lastGen) {
		this.lastGen = lastGen;
	}
	
	public long lastGen() {
		return lastGen;
	}
	
	public OldRateAggregate[] getRates() {
		return rates;
	}
	
	public Object[] getValues() {
		return values;
	}
	
	@SuppressWarnings("unchecked")
	public K getKey() {
		return (K)values[0];
	}
	
	
	
	public CompositeType getCompositeType() {
		return type;
	}
	
	
	public Object get(String key) {
		int idx = lister.attrIndex(key);

		// TODO zwracac null'a czy obsluzyc blad ? 
		
		return idx >= 0 ? values[idx] : null;
	}
	
	
	public Object[] getAll(String[] keys) {
		Object[] rval = new Object[keys.length];
		
		for (int i = 0; i < keys.length; i++) {
			rval[i] = get(keys[i]);
		}

		return rval;
	}
	
	
	public boolean containsKey(String key) {
		return lister.attrIndex(key) != -1;
	}
	
	
	public boolean containsValue(Object value) {
		for (Object obj : values) {
			if (ZorkaUtil.objEquals(obj, value)) {
				return true;
			}
		}

		return false;
	}
	
	
	public Collection<?> values() {
		List<Object> lst = new ArrayList<Object>(values.length);

		for (Object obj : values) {  
			lst.add(obj);
		}

		return lst;
	}
}
