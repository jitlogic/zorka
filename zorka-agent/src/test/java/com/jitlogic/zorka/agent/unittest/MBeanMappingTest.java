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

package com.jitlogic.zorka.agent.unittest;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.jitlogic.zorka.agent.mapbean.AttrGetter;
import com.jitlogic.zorka.agent.mapbean.ZorkaMappedMBean;

public class MBeanMappingTest {

	private ZorkaMappedMBean bean;
	
	@Before
	public void setUp() throws Exception {
		bean = new ZorkaMappedMBean("test");
	}
	
	
	@Test
	public void testMapConstantAttr() throws Exception {
		bean.add("test", 1L);
		assertEquals(1L, bean.getAttribute("test"));
	}
	
	
	@Test
	public void testMapGetterAttr() throws Exception {
		bean.add("test", new AttrGetter(this, "class"));
		assertSame(this.getClass(), bean.getAttribute("test"));
	}
	
	
	@Test
	public void testMapGetterMultiAttr() throws Exception {
		bean.add("test", new AttrGetter(this, "class", "name", "length"));
		
	}
	
	
	@Test
	public void testMapGetterWithInterimChange() throws Exception {
		bean.add("test", 1L);
		assertEquals(1L, bean.getAttribute("test"));
		
		bean.add("boo", 2L);
		assertEquals(2L, bean.getAttribute("boo"));
	}
}
