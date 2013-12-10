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

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;

/**
 * <pre>
 * 
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class DollyClassTransformer implements ClassFileTransformer {
	
	//private static final String[] IGNORED_PACKAGES = new String[] { "sun/", "com/sun/", "java/", "javax/", "com/apple/" };
	
	private List<String> classList;
	private boolean verbose;

	private Map<ClassLoader, ClassPool> pools = new HashMap<ClassLoader, ClassPool>();
	
	public DollyClassTransformer(List<String> classList, boolean verbose) {
		this.classList = classList;
		this.verbose = verbose;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        CtClass cl = null;
		ClassPool pool = getClassPool(loader);
		
        try {
            if (acceptClass(className)) {
                if (verbose) {
                    System.out.println("[Dolly] Enhancing class : " + className);
                }

                // Transform
                cl = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
                
                if (cl.subtypeOf(pool.get("javax.servlet.http.HttpSession"))) {
                	return instumentHttpSession(className, cl);
                } else {
                	return instrumentPojo(className, cl);
                }
            }
        } catch (Exception e) {
            System.err.println("[Dolly] Warning: could not enhance class " + className + " : " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        } finally {
            if (cl != null) {
                cl.detach();
            }
        }
        
        return classfileBuffer;
	}
	
	private byte[] instrumentPojo(String className, CtClass cl) throws Exception {
		byte[] redefinedClassfileBuffer = null;

		if (cl.isInterface() == false) {
			CtMethod[] methods = cl.getDeclaredMethods();
			boolean isEnhanced = false;
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].isEmpty()) {
					continue;
				}
				
				String body = null;
				isEnhanced = false;
				if (methods[i].getName().startsWith("set")) {
					body =     "{" +
							   "	com.athena.dolly.enhancer.DollyManager.getInstance().setValue(\"" + className.replace('/', '.') + "\", \"" + methods[i].getName().substring(3) + "\", $1);" +
							   "	_" + methods[i].getName() + "($1);" +
							   "}";
					isEnhanced = true;
				} else if (methods[i].getName().startsWith("get")) {
					body =     "{" +
							   "	java.lang.Object obj = com.athena.dolly.enhancer.DollyManager.getInstance().getValue(\"" + className.replace('/', '.') + "\", \"" + methods[i].getName().substring(3) + "\");" +
							   "	if (obj == null) {" +
							   "		obj = _" + methods[i].getName() + "();" +
							   "	}" +
							   "	return obj;" +
							   "}";
					isEnhanced = true;
				} else if (methods[i].getName().startsWith("is")) {
					body =     "{" +
							   "	java.lang.Object obj = com.athena.dolly.enhancer.DollyManager.getInstance().getValue(\"" + className.replace('/', '.') + "\", \"" + methods[i].getName().substring(2) + "\");" +
							   "	if (obj == null) {" +
							   "		obj = _" + methods[i].getName() + "();" +
							   "	}" +
							   "	return obj;" +
							   "}";
					isEnhanced = true;
				}
				
				if (isEnhanced) {
					
					System.out.println(">>> " + body);
					
					CtMethod newMethod = CtNewMethod.copy(methods[i], cl, null);
					methods[i].setName("_" + methods[i].getName());

					System.out.println(">>> " + newMethod);
					
					newMethod.setBody(body);
					System.out.println("   <<< " + newMethod);
					cl.addMethod(newMethod);
					
					if (verbose) {
						System.out.println(className.replace('/', '.') + "." + methods[i].getName() + "() is successfully enhanced.");
					}
				}
			}
			
			redefinedClassfileBuffer = cl.toBytecode();
		}
		
		return redefinedClassfileBuffer;
	}

	private byte[] instumentHttpSession(String className, CtClass cl) throws Exception {
		byte[] redefinedClassfileBuffer = null;

		if (cl.isInterface() == false) {
			CtMethod[] methods = cl.getDeclaredMethods();
			boolean isEnhanced = false;
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].isEmpty()) {
					continue;
				}
				
				String body = null;
				isEnhanced = false;
				if (methods[i].getName().equals("setAttribute")) {
					body =     "{" +
							   "	com.athena.dolly.enhancer.DollyManager.getInstance().setValue(getId(), $1, $2);" +
							   "	_setAttribute($1, $2);" +
							   "}";
					isEnhanced = true;
				} else if (methods[i].getName().equals("getAttribute")) {
					body =     "{" +
							   "	java.lang.Object obj = com.athena.dolly.enhancer.DollyManager.getInstance().getValue(getId(), $1);" +
							   "	if (obj == null) {" +
							   "		obj = _getAttribute($1);" +
							   "	}" +
							   "	return obj;" +
							   "}";
					isEnhanced = true;
				} else if (methods[i].getName().equals("getAttributeNames")) {
					body =     "{" +
							   "	java.util.Enumeration obj = com.athena.dolly.enhancer.DollyManager.getInstance().getValueNames(getId());" +
							   "	if (obj == null) {" +
							   "		obj = _getAttributeNames();" +
							   "	}" +
							   "	return obj;" +
							   "}";
					isEnhanced = true;
				} else if (methods[i].getName().equals("removeAttribute")) {
					body =     "{" +
							   "	com.athena.dolly.enhancer.DollyManager.getInstance().removeValue(getId(), $1);" +
							   "	_removeAttribute($1);" +
							   "}";
					isEnhanced = true;
				}
				
				if (isEnhanced) {
					CtMethod newMethod = CtNewMethod.copy(methods[i], cl, null);
					methods[i].setName("_" + methods[i].getName());
					newMethod.setBody(body);
					cl.addMethod(newMethod);
					
					if (verbose) {
						System.out.println(className.replace('/', '.') + "." + methods[i].getName() + "() is successfully enhanced.");
					}
				}
			}
			
			redefinedClassfileBuffer = cl.toBytecode();
		}
		
		return redefinedClassfileBuffer;
	}

	private ClassPool getClassPool(ClassLoader loader) {
		ClassPool pool = pools.get(loader);
		
		if (pool == null) {
			pool = new ClassPool(true);
			pool.appendClassPath(new LoaderClassPath(loader));
			pools.put(loader, pool);
		}
		
		return pool;
	}

    private boolean acceptClass(String className) {
    	/*
        for (String IGNORED_PACKAGE : IGNORED_PACKAGES) {
            if (className.startsWith(IGNORED_PACKAGE)) {
            	System.err.println(className + " : " + IGNORED_PACKAGE);
                return false;
            }
        }
        
        return true;
        */
        
        for (String targetClass : classList) {
        	if (className.equals(targetClass)) {
        		return true;
        	}
        }
        
        return false;
    }
}
//end of DollyClassTransformer.java