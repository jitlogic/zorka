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

package com.jitlogic.zorka.agent.rankproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jitlogic.zorka.agent.ZorkaError;
import com.jitlogic.zorka.agent.ZorkaService;
import com.jitlogic.zorka.agent.ZorkaUtil;
import com.jitlogic.zorka.agent.rateproc.RateAggregate;

/**
 * 
 * Objects implementing this interface are responsible for feeding 
 * associated rank lists with data, supplying common view data etc.
 *  
 * Note that RankLister can supply multiple 
 * 
 * @author RLE <rafal.lewczuk@jitlogic.com>
 *
 * @param <T>
 */
public abstract class RankLister<K,T> implements Runnable, ZorkaService {

	private static Logger log = LoggerFactory.getLogger(RankLister.class);

	protected static final int ATTR_ID            = 0;
	protected static final int ATTR_TSTAMP        = 1;

	private long updateInterval, rerankInterval;
	private long lastUpdate = 0L, lastRerank = 0L;
	protected long lastGen = 0L;

	protected List<RankAttr<K,T>> extAttrs = new ArrayList<RankAttr<K,T>>();
	protected Map<String,RankList<K,T>> rankLists = new HashMap<String, RankList<K,T>>();

	protected Map<K,RankItem<K,T>> items = new HashMap<K,RankItem<K,T>>();
	
	protected CompositeType type = null;
	protected TabularType tabularType = null;

	protected String[] basicAttr;
	protected String[] basicDesc;
	protected OpenType[] basicType;
	
	protected int STD_LEN;
	
	private volatile boolean running = false;
	
	private Thread thread = null;
	
	
	public abstract List<T> list();  // TODO refactor list() to return List<T> instead of T[]
	public abstract K getKey(T info);
	public abstract void updateBasicAttrs(RankItem<K,T> item, T info, long tstamp);

	public RankLister(long updateInterval, long rerankInterval, String[] basicAttr, String[] basicDesc, OpenType[] basicType) {
		this.updateInterval = updateInterval;
		this.rerankInterval = rerankInterval;
		this.basicAttr = basicAttr;
		this.basicDesc = basicDesc;
		this.basicType = basicType;
		STD_LEN = basicAttr.length;
	}

	
	public int attrIndex(String attr) {
		for (int i = 0; i < basicAttr.length; i++)
			if (basicAttr[i].equals(attr))
				return i;
		
		for (int i = 0; i < extAttrs.size(); i++)
			if (extAttrs.get(i).name.equals(attr))
				return STD_LEN+i;
			
		return -1;
	}
	
	
	public OpenType attrType(String attr) {
		int idx = attrIndex(attr);
		if (idx >= 0) 
			return idx < STD_LEN ? basicType[idx] : SimpleType.DOUBLE;
		else
			return null;
	}
	

	public Comparator<RankItem<K,T>> comparator(String attr) {
		final int idx = attrIndex(attr);
		
		if (idx < 0)
			throw new ZorkaError("Thread lister has no such attribute: '" + attr + "'");
		
		OpenType type = attrType(attr);
		
		if (type == null)
			throw new ZorkaError("Attribute '" + attr + "' has no data type(?)");
		
		if (SimpleType.DOUBLE.equals(type)) {
			return new Comparator<RankItem<K,T>>() {
				public int compare(RankItem<K,T> o1, RankItem<K,T> o2) {
					return (Double)o1.getValues()[idx] > (Double)o2.getValues()[idx] ? -1 : 1;
				}
			};
		} else if (SimpleType.LONG.equals(type)) {
			return new Comparator<RankItem<K,T>>() {
				public int compare(RankItem<K,T> o1, RankItem<K,T> o2) {
					return (Long)o1.getValues()[idx] > (Long)o2.getValues()[idx] ? -1 : 1;
				}
			};			
		} else if (SimpleType.INTEGER.equals(type)) {
			return new Comparator<RankItem<K,T>>() {
				public int compare(RankItem<K,T> o1, RankItem<K,T> o2) {
					return (Integer)o1.getValues()[idx] > (Integer)o2.getValues()[idx] ? -1 : 1;
				}
			};			
		} else if (SimpleType.STRING.equals(type)) {
			return new Comparator<RankItem<K,T>>() {
				public int compare(RankItem<K,T> o1, RankItem<K,T> o2) {
					return -1*((String)o1.getValues()[idx]).compareTo((String)o2.getValues()[idx]);
				}
			};						
		} else if (SimpleType.SHORT.equals(type)) {
			return new Comparator<RankItem<K,T>>() {
				public int compare(RankItem<K,T> o1, RankItem<K,T> o2) {
					return (Short)o1.getValues()[idx] > (Short)o2.getValues()[idx] ? -1 : 1;
				}
			};						
		}
		
		throw new ZorkaError("Illegal data type for attribute " + attr + ": " + type); 
	}

	
	private RankItem<K,T> extendItem(RankItem<K,T> item) {
		Object[] oldVals = item.getValues();
		Object[] newVals = new Object[STD_LEN+extAttrs.size()];
		
		for (int i = 0; i < oldVals.length; i++) 
			newVals[i] = oldVals[i];
		
		RateAggregate[] oldRates = item.getRates();
		RateAggregate[] newRates = new RateAggregate[extAttrs.size()];
		
		for (int i = 0; i < oldRates.length; i++)
			newRates[i] = oldRates[i];
		
		for (int i = oldRates.length; i < newRates.length; i++) {
			newRates[i] = extAttrs.get(i).newAggregate();
			newVals[i] = newRates[i].rate();
		}
		
		return new RankItem<K,T>(this, type, newVals, newRates);
	} // extendItem()
	
	
	protected void makeCompositeType() {
		int size = STD_LEN + extAttrs.size();
		
		String[] newAttr = new String[size];
		String[] newDesc = new String[size];
		OpenType[] newType = new OpenType[size];
		
		for (int i = 0; i < STD_LEN; i++) {
			newAttr[i] = basicAttr[i];
			newDesc[i] = basicDesc[i];
			newType[i] = basicType[i];	
		}
		
		for (int i = 0; i < extAttrs.size(); i++) {
			RankAttr<K,T> ra = extAttrs.get(i);
			newAttr[STD_LEN+i] = ra.name;
			newDesc[STD_LEN+i] = ra.description;
			newType[STD_LEN+i] = SimpleType.DOUBLE;
		}
		
		try {
			type = new CompositeType("com.jitlogic.zorka.threadproc.ThreadItem", 
				"Thread statistic.", newAttr, newDesc, newType);
		} catch (OpenDataException e) {
			throw new ZorkaError("Error creating CompositeType for thread lister", e);
		} 
		
		String[] index = { "id" };
		try {
			tabularType = new TabularType("ThreadList", "Thread Ranking List", type, index);
		} catch (OpenDataException e) {
			throw new ZorkaError("Error creating TabularType for thread lister", e);
		}

	} // makeCompositeType()

	
	protected RankItem<K,T> makeItem(T info, long tstamp) {
		
		Object[] vals = new Object[STD_LEN+extAttrs.size()];
		RateAggregate[] rates = new RateAggregate[extAttrs.size()];
		
		for (int i = 0; i < extAttrs.size(); i++)
			rates[i] = extAttrs.get(i).newAggregate();
		
		RankItem<K,T> item = new RankItem<K,T>(this, type, vals, rates);
		
		updateBasicAttrs(item, info, tstamp);
		updateExtAttrs(item, info, tstamp);
		
		return item;
	} // makeItem()
	

	public void newAttr(String name, String description, long horizon, double multiplier, String nominalAttr, String dividerAttr) {
		if (attrIndex(name) >= 0)
			throw new ZorkaError("Attribute '" + name + "' already exists.");
		
		RankAttr<K,T> attr = new RankAttr<K,T>(this, name, description, horizon, multiplier, nominalAttr, dividerAttr);
		extAttrs.add(attr);
		
		makeCompositeType();
		
		// Rebuild all items 
		Map<K,RankItem<K,T>> newItems = new HashMap<K,RankItem<K,T>>();
		for (Entry<K,RankItem<K,T>> entry : items.entrySet())
			newItems.put(entry.getKey(), extendItem(entry.getValue()));
		
		// Update TabularType for all associated rank lists
		for (RankList<K,T> rlist : rankLists.values()) {
			rlist.updateType(tabularType);
		}
		
		items = newItems;
	}
	

	public RankList<K,T> newList(String listName, String attrName, int size) {
		
		if (rankLists.containsKey(listName))
			throw new ZorkaError("List '" + listName + "' already exists.");
		
		if (-1 == attrIndex(attrName))
			throw new ZorkaError("Attribute '" + attrName + "' not defined.");
		
		RankList<K,T> rlist = new RankList<K,T>(listName, attrName, size, tabularType);
		rankLists.put(listName,  rlist);
		return rlist; 
	}	
	

	public void rerank(long tstamp) {
		
		if (items.size() == 0) return;
		
		@SuppressWarnings("unchecked")
		RankItem<K,T>[] itab = new RankItem[items.size()]; 
		
		int i = 0; 
		
		for (RankItem<K,T> item : items.values())
			itab[i++] = item;

		for (RankList<K,T> rlist : rankLists.values()) {
			Comparator<RankItem<K,T>> cmp = comparator(rlist.getAttrName());
			Arrays.sort(itab, cmp);
			rlist.refresh(itab);
		}
	} // rerank()
	

	public void run() {
		while (running) {
			try {
				long tstamp = ZorkaUtil.currentTimeMillis();
				runCycle(tstamp);
			
				long ts1 = updateInterval-tstamp+lastUpdate;
				long ts2 = rerankInterval-tstamp+lastRerank;
				long ts = ts1 < ts2 ? ts1 : ts2;
				if (ts > 0) Thread.sleep(ts);
			} catch (InterruptedException e) { 
				
			} catch (Throwable e) {
				log.error("Error executing ThreadLister cycle", e);
			}
		} // while (running)
		thread = null;
	} // run()
	
	
	public void runCycle(long tstamp) {
		lastGen++;
		if (tstamp-lastUpdate >= updateInterval) {
			update(tstamp);
			lastUpdate = tstamp;
		}
		if (tstamp-lastRerank >= rerankInterval) {
			rerank(tstamp);
			lastUpdate = tstamp;
		}		
	}
		
		
	protected void updateExtAttrs(RankItem<K,T> item, T info, long tstamp) {
		RateAggregate[] rates = item.getRates();
		Object[] vals = item.getValues();
		
		for (int i = 0; i < extAttrs.size(); i++) {
			RankAttr<K,T> attr = extAttrs.get(i);
			rates[i].feed((Long)vals[attr.nominalOffs], (Long)vals[attr.dividerOffs]);
			vals[STD_LEN+i] = rates[i].rate();
		}
	}
	
	
	protected void updateItem(RankItem<K,T> item, T info, long tstamp) {
		updateBasicAttrs(item, info, tstamp);
		updateExtAttrs(item, info, tstamp);
		item.updateGen(lastGen);
	} // updateItem()
	

	public void update(long tstamp) {
		List<T> infos = list();
		
		for (T info : infos) {
			K key = getKey(info);
			if (items.containsKey(key)) {
				updateItem(items.get(key), info, tstamp);
			} else {
				items.put(key, makeItem(info, tstamp));
			}
		} 		
	} // update()
	
	/* ZorkaService methods */
	
	public void svcStart() {
		thread = new Thread(this);
		thread.setName("Zorka-ThreadLister("+updateInterval+","+rerankInterval+")");
		running = true;
		thread.start();
	}


	public void svcStop() {
		running = false;
	}


	public void svcClear() {
	}


	public void svcReload() {
	}

}
