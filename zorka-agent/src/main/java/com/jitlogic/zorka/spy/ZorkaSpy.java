package com.jitlogic.zorka.spy;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class ZorkaSpy implements ClassFileTransformer {

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
