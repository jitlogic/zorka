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

package com.jitlogic.zorka.core.test.agent;

import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.jitlogic.zorka.core.mbeans.AttrGetter;
import com.jitlogic.zorka.core.mbeans.ZorkaMappedMBean;

public class MBeanMappingUnitTest extends ZorkaFixture {

	private ZorkaMappedMBean bean;


	@Before
	public void setUp() throws Exception {
        bean = new ZorkaMappedMBean("test");
	}


	@Test
	public void testMapConstantAttr() throws Exception {
		bean.put("test", 1L);
		assertEquals(1L, bean.getAttribute("test"));
	}
	
	
	@Test
	public void testMapGetterAttr() throws Exception {
		bean.put("test", new AttrGetter(this, "class"));
		assertSame(this.getClass(), bean.getAttribute("test"));
	}
	
	
	@Test
	public void testMapGetterMultiAttr() throws Exception {
		bean.put("test", new AttrGetter(this, "class", "name", "length"));
		
	}
	
	
	@Test
	public void testMapGetterWithInterimChange() throws Exception {
		bean.put("test", 1L);
		assertEquals(1L, bean.getAttribute("test"));
		
		bean.put("boo", 2L);
		assertEquals(2L, bean.getAttribute("boo"));
	}
}
