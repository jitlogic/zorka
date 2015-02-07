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

package com.jitlogic.zorka.core.test.integ;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.jitlogic.zorka.core.integ.zabbix.ZabbixQueryTranslator;
import org.junit.Test;

import com.jitlogic.zorka.core.integ.zabbix.ZabbixRequestHandler;

import static org.junit.Assert.*;

public class ZabbixAgentUnitTest {

	private String tr(String src) {
		return new ZabbixQueryTranslator().translate(src);
	}
	
	
	@Test
	public void testVarsAndAttrs() {
		assertEquals("var", tr("var"));
		assertEquals("some.var", tr("some__var"));
	}
	
	
	@Test
	public void testFuncs() {
		assertEquals("zorka.version()",   tr("zorka__version[]"));
		assertEquals("some_func(1,2,3)",  tr("some_func[1,2,3]"));
		assertEquals("func2(\"abc\")",    tr("func2[\"abc\"]"));
		assertEquals("func3(\"ab[]cd\")", tr("func3[\"ab[]cd\"]"));
	}
	
	@Test
	public void testBiggerFuncs() {
		assertEquals("zorka.jmx(\"java\", \"java.lang:type=OperatingSystem\", \"Arch\")", 
			tr("zorka__jmx[\"java\", \"java.lang:type=OperatingSystem\", \"Arch\"]"));
	}
	
	private static InputStream mkIS(Object...args) {
		byte buf[] = new byte[2048];
		int pos = 0;
		for (Object arg : args) {
			if (arg instanceof String)
				for (char ch : ((String)arg).toCharArray())
					buf[pos++] = ((byte)ch);
			else if (arg instanceof Integer)
				buf[pos++] = ((byte)(int)(Integer)arg);
			else if (arg instanceof Byte)
				buf[pos++] = ((Byte)arg);
		}
		
		return new ByteArrayInputStream(buf, 0, pos);
	}
	
	
	@Test
	public void testParseBinaryRequests() throws Exception {
		assertEquals("abc", ZabbixRequestHandler.decode(mkIS("abc")));
		InputStream is = mkIS("ZBXD", 1, 0x0c, 0,0,0,0,0,0,0, "system.load", 0x0a); 
		assertEquals("system.load", ZabbixRequestHandler.decode(is));
		// TODO dotestować graniczne przypadki 
	}
}
