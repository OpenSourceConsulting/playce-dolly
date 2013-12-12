/* 
 * Athena Peacock Dolly - DataGrid based Clustering 
 * 
 * Copyright (C) 2013 Open Source Consulting, Inc. All rights reserved by Open Source Consulting, Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Revision History
 * Author			Date				Description
 * ---------------	----------------	------------
 * Sang-cheon Park	2013. 12. 5.		First Draft.
 */
package com.athena.dolly.enhancer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * <pre>
 * 
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class DollyAgent implements ClassFileTransformer {
	
	public static final String TRANSFORMER_CLASS = "com.athena.dolly.enhancer.DollyClassTransformer";
    private ClassFileTransformer transformer;
    
    public DollyAgent() {
    	System.out.println("[Dolly] Dolly agent activated.");

        DollyConfig config = null;
        try {
            config = new DollyConfig().load();
        } catch (Exception e) {
            System.err.println("[Dolly] Configuration error : " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }

        boolean verbose = config.isVerbose();
        if (verbose) {
            System.out.println("[Dolly] Target classes :");
            
            List<String> classList = config.getClassList();
            for (String className : classList) {
            	System.out.println("   - " + className);
            }
        }

        try {
            transformer = instanciateTransformer(TRANSFORMER_CLASS, config.getClassList(), verbose);
        } catch (Exception e) {
            System.err.println("[Dolly] Initialization error : " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        instrumentation.addTransformer(new DollyAgent());
    }

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		return transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
	}
	
    private ClassFileTransformer instanciateTransformer(String className, List<String> classList, boolean verbose) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> clazzConstructor = clazz.getConstructor(new Class[]{List.class, Boolean.TYPE});
        return (ClassFileTransformer) clazzConstructor.newInstance(classList, verbose);
    }

}
//end of DollyAgent.java