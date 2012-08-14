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

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class ZorkaSpy implements ClassFileTransformer {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

	private List<MethodTemplate> templates = new ArrayList<MethodTemplate>();
	
	
	public boolean lookup(String className) {
		for (MethodTemplate template : templates)
			if (template.match(className))
				return true;
		return false;
	}
	
	
	public DataCollector lookup(String className, String methodName, String signature, String[] interfaces) {
		for (MethodTemplate template : templates) {
			if (template.match(className, methodName, signature)) {
				return template.mkCollector(className, methodName, signature);
			}
		}
		//for (InstrumentationFilter filter : filters)
		return null; // TODO 
	}

	
	public void addTemplate(MethodTemplate template) {
		templates.add(template);
	}
	
	
	
	public byte[] instrument(String className, byte[] buf, ClassReader cr) throws IOException {
		
		if (!lookup(className)) return buf;

        log.info("Instrumenting class: " + className);

		ClassWriter cw = new ClassWriter(cr, 0);
		ClassVisitor ca = new ClassInstrumentator(className, this, cw);
		
		cr.accept(ca, 0);
		return cw.toByteArray();
	}
	
	
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		String cn = className.replace("/", ".").replace("$", ".");
		try {
			return instrument(cn, classfileBuffer, new ClassReader(classfileBuffer));
		} catch (IOException e) {
			System.err.println("ERROR: Class " + cn + " failed to instrument: " + e.getMessage());
			e.printStackTrace(); // TODO poprawne logowanie bledow  
			return classfileBuffer;
		}
	}

}
