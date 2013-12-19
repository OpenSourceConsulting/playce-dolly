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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * <pre>
 * BCI(Byte Code Instrumentation) 수행 대상 클래스 목록 및 Infinispan 관련 설정이 저장된 파일을 로드하고 파싱한다.
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class DollyConfig {

	protected static final String CONFIG_FILE = "dolly.properties";
	private static final String VERBOSE_PROPERTY = "dolly.verbose";
    private static final String TARGET_CLASS_PROPERTY = "dolly.instrument.target.class";
    
    public static Properties properties;
    
    private List<String> classList = new ArrayList<String>();
    private boolean verbose;
    
	/**
	 * <pre>
	 * 프로퍼티 파일을 로드하고 파싱한다.
	 * </pre>
	 * @return
	 * @throws ConfigurationException
	 */
	public DollyConfig load() throws ConfigurationException {
		properties = loadConfigFile();
        parseConfigFile(properties);
        return this;
    }//end of load()

    /**
     * <pre>
     * System Property 또는 classpath에 존재하는 프로퍼티 파일을 로드한다.
     * </pre>
     * @return
     * @throws ConfigurationException
     */
    private Properties loadConfigFile() throws ConfigurationException {
    	InputStream configResource = null;
    	
        try {
        	String configFile = System.getProperty(CONFIG_FILE);
        	
        	if (configFile != null && !"".equals(configFile)) {
        		configResource = new BufferedInputStream(new FileInputStream(configFile));
        	} else {
        		configResource = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE);
        	}
        	
            if (configResource == null) {
                throw new FileNotFoundException("Could not locate " + CONFIG_FILE + " in the classpath or System Poroperty(-Ddolly.properties=Full Qualified File Name) path.");
            }
            
            Properties config = new Properties();
            config.load(configResource);

            configResource.close();
        	
            return config;
        } catch (IOException e) {
            throw new ConfigurationException("Could not load the configuration file (" + CONFIG_FILE + "). " +
                    "Please make sure it exists at the root of the classpath or System Poroperty(-Ddolly.properties=Full Qualified File Name) path.", e);
        }
    }//end of loadConfigFile()
    
    /**
     * <pre>
     * 프로퍼티 파일을 파싱한다.
     * </pre>
     * @param config
     * @throws ConfigurationException
     */
    private void parseConfigFile(Properties config) throws ConfigurationException {
    	extractTargetClasses(config);
        extractVerbosity(config);
    }//end of parseConfigFile()

    /**
     * <pre>
     * BCI 대상 target class 들을 확인한다.
     * </pre>
     * @param config
     */
    private void extractTargetClasses(Properties config) {
    	String[] classNames = config.getProperty(TARGET_CLASS_PROPERTY, "").split(",");
    	
    	for (String clazzName : classNames) {
    		if (!"".equals(clazzName)) {
    			if (verbose) {
    				System.out.println(clazzName + " will be enhanced.");
    			}
    			classList.add(clazzName.replace(".", "/"));
    		}
    	}
    }//end of extractTargetClasses()

    /**
     * <pre>
     * verbose 여부를 확인한다.
     * </pre>
     * @param config
     */
    private void extractVerbosity(Properties config) {
        this.verbose = Boolean.parseBoolean(config.getProperty(VERBOSE_PROPERTY, "false"));
    }//end of extractVerbosity()

	/**
	 * @return the classList
	 */
	public List<String> getClassList() {
		return classList;
	}

	/**
	 * @return the verbose
	 */
	public boolean isVerbose() {
		return verbose;
	}
}
//end of DollyConfig.java