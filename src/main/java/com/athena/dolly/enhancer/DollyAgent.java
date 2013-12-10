/* 
 * Copyright 2013 The Athena-Peacock Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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