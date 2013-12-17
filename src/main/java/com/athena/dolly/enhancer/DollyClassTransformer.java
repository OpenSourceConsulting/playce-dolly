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
                } else if (cl.subtypeOf(pool.get("org.apache.catalina.Manager"))) {
            		return instumentManager(className, cl);
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
							   "	try { _setAttribute($1, $2); } catch (Exception e) { e.printStackTrace(); }" +
							   "}";
					isEnhanced = true;
				} else if (methods[i].getName().equals("getAttribute")) {
					body =     "{" +
							   "	java.lang.Object obj = com.athena.dolly.enhancer.DollyManager.getInstance().getValue(getId(), $1);" +
							   "	if (obj == null) {" +
							   "		try { obj = _getAttribute($1); } catch (Exception e) { e.printStackTrace(); }" +
							   "	}" +
							   "	return obj;" +
							   "}";
					isEnhanced = true;
				} else if (methods[i].getName().equals("getAttributeNames")) {
					body =     "{" +
							   "	java.util.Enumeration obj = com.athena.dolly.enhancer.DollyManager.getInstance().getValueNames(getId());" +
							   "	if (obj == null) {" +
							   "		try { obj = _getAttributeNames(); } catch (Exception e) { e.printStackTrace(); }" +
							   "	}" +
							   "	return obj;" +
							   "}";
					isEnhanced = true;
				} else if (methods[i].getName().equals("removeAttribute")) {
					body =     "{" +
							   "	com.athena.dolly.enhancer.DollyManager.getInstance().removeValue(getId(), $1);" +
							   "	try { _removeAttribute($1); } catch (Exception e) { e.printStackTrace(); }" +
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
	
	private byte[] instumentManager(String className, CtClass cl) throws Exception {
		byte[] redefinedClassfileBuffer = null;
		
		CtMethod[] methods = cl.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].isEmpty()) {
				continue;
			}
			
			if (methods[i].getName().equals("findSession")) {
				String body = 	"{" +
						   		"	if ($1 == null) {" +
						   		"		return null;" +
						   		"	}" +
						   		"	if (sessions.get($1) == null) {" +
						   		"		return createSession($1);" +
						   		"	} else {" +
						   		"		return (org.apache.catalina.Session) sessions.get($1);" +
						   		"	}" +
						   		"}";

				CtMethod newMethod = CtNewMethod.copy(methods[i], cl, null);
				methods[i].setName("_" + methods[i].getName());
				newMethod.setBody(body);
				cl.addMethod(newMethod);
				
				//newMethod.insertBefore("System.out.println(\"=========== findSession(\" + $1 + \") ===========\");");
				
				if (verbose) {
					System.out.println(className.replace('/', '.') + "." + methods[i].getName() + "() is successfully enhanced.");
				}
			}
			/*
			else if (methods[i].getName().equals("createSession")) {
				methods[i].insertBefore("System.out.println(\"=========== createSession() ===========\");");
			} else if (methods[i].getName().equals("add")) {
				methods[i].insertBefore("System.out.println(\"=========== add(\" + $1 + \") ===========\");");
			}
			*/
		}
		
		redefinedClassfileBuffer = cl.toBytecode();
		
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