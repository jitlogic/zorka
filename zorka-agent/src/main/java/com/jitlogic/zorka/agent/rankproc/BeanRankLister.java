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
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.jitlogic.zorka.agent.JmxObject;
import com.jitlogic.zorka.agent.JmxResolver;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaUtil;

public class BeanRankLister extends RankLister<String,BeanRankInfo> {
	
	private JmxResolver resolver = new JmxResolver();
	private MBeanServerConnection conn;
	private String query;
	private String attr0 = null;
	private String keyName;
	private String[] attrs = new String[0];
	private String mask = ".*";
	private String nominalAttr, dividerAttr;

    private ObjectInspector inspector = new ObjectInspector();
	
	
	public BeanRankLister(long updateInterval, long rerankInterval,  
		MBeanServerConnection conn, String query,
		String keyName, String attrs[],
		String nominalAttr, String dividerAttr) {
		
		super(updateInterval, rerankInterval, null, null, null);
		
		this.conn = conn;
		this.query = query;
		this.keyName = keyName;
		this.nominalAttr = nominalAttr;
		this.dividerAttr = dividerAttr;
		
		basicAttr = new String[] { "key", "tstamp", nominalAttr, dividerAttr };
		basicDesc = new String[] { "Key", "Timestamp", "Nominal", "Divider" };
		basicType = new OpenType[] { SimpleType.STRING, SimpleType.LONG, 
									 SimpleType.LONG, SimpleType.LONG };
				
		int len = attrs.length;
		
		if (len > 0) {
			attr0 = attrs[0];
		}
		
		if (len > 1) {
			String m = attrs[len-1];
			if (m.contains("*") || m.contains("?")) {
				mask = m; len--;
			}
			
			attrs = new String[len-1];
			
			for (int i = 1; i < len; i++) {
				this.attrs[i-1] = attrs[i]; 
			}
		}
		
	}
	
	
	@Override
	public List<BeanRankInfo> list() {
		Set<ObjectName> names = resolver.queryNames(conn, query);
		
		List<BeanRankInfo> lst = new ArrayList<BeanRankInfo>();
		
		for (ObjectName on : names) {
			try {
				Object obj = attr0 != null ? conn.getAttribute(on, attr0) : new JmxObject(on, conn);
				for (String attr : attrs) obj = inspector.get(obj, attr);
				if (obj == null) { continue; }
				String key = on.getKeyProperty(keyName);
				if (mask != null) {
					for (String k2 : ZorkaUtil.listAttrNames(obj)) {
						if (k2.matches(mask)) {
							lst.add(new BeanRankInfo(key + "." + k2, inspector.get(obj, k2)));
						}
					}
				} else {
					lst.add(new BeanRankInfo(key, obj));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return lst;
	}
	
	
	@Override
	public String getKey(BeanRankInfo info) {
		return info.getName();
	}
	
	
	@Override
	public void updateBasicAttrs(RankItem<String, BeanRankInfo> item, BeanRankInfo info, long tstamp) {
		Object[] v = item.getValues();
		v[0] = info.getName();
		v[1] = tstamp;
		v[2] = inspector.get(info.getValue(), nominalAttr); // TODO coerce to long
		v[3] = inspector.get(info.getValue(), dividerAttr); // TODO coerce to long
	}
	
}
