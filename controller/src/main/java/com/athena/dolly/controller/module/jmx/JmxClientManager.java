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
 * Sang-cheon Park	2014. 3. 31.		First Draft.
 */
package com.athena.dolly.controller.module.jmx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.athena.dolly.common.cache.DollyConfig;
import com.athena.dolly.common.exception.ConfigurationException;
import com.athena.dolly.controller.module.jmx.vo.MemoryVo;
import com.athena.dolly.controller.module.jmx.vo.OperationgSystemVo;

/**
 * <pre>
 * 
 * </pre>
 * @author Man-Woong Choi
 * @version 1.0
 */
@Component
public class JmxClientManager implements InitializingBean {
	
    private static final Logger logger = LoggerFactory.getLogger(JmxClientManager.class);

	private DollyConfig config;
	private static Map<String, JmxClient> jmxClientMap;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if (DollyConfig.properties == null || config == null) {
    		try {
                config = new DollyConfig().load();
			} catch (ConfigurationException e) {
				logger.error("[Dolly] Configuration error : ", e);
			}
    	}
		
		boolean embedded = config.isEmbedded();
		String[] jmxServers = config.getJmxServers();
		String[] users = config.getUsers();
		String[] passwds = config.getPasswds();
		
		jmxClientMap = new TreeMap<String, JmxClient>();
		
		JmxClient jmxClient = null;
		for (int i = 0; i < jmxServers.length; i++) {
			if (users != null && users.length == jmxServers.length &&
					passwds != null && passwds.length == jmxServers.length) {
				jmxClient = new JmxClient(jmxServers[i], users[i], passwds[i], embedded);
			} else {
				jmxClient = new JmxClient(jmxServers[i], null, null, embedded);
			}
			jmxClientMap.put(i + "", jmxClient);
		}
		
		logger.debug("JMX Client Info : [{}]" + jmxClientMap);
	}

	/**
	 * <pre>
	 * 
	 * </pre>
	 * @param nodeName
	 * @return
	 */
	public static JmxClient getJmxClient(String nodeName) {
		return jmxClientMap.get(nodeName);
	}//end of getJmxClient()
	
	/**
	 * <pre>
	 * 
	 * </pre>
	 * @return
	 */
	public static List<String> getServerList() {
		return new ArrayList<String>(jmxClientMap.keySet());
	}//end of getServerList()
	
	/**
	 * <pre>
	 * 
	 * </pre>
	 * @param nodeName
	 * @return
	 */
	public static boolean isValidNodeName(String nodeName) {
		return jmxClientMap.containsKey(nodeName);
	}//end of isValidNodeName()
	
	public static MemoryVo getMemoryUsage(String nodeName) {
		JmxClient jmxClient = jmxClientMap.get(nodeName);
        
        MemoryVo memory = null;
		try {
			ObjectName objectName=new ObjectName("java.lang:type=Memory");			
			
			MBeanServerConnection connection = jmxClient.getJmxConnector().getMBeanServerConnection();
			CompositeDataSupport heapMemoryUsage = (CompositeDataSupport)connection.getAttribute(objectName, "HeapMemoryUsage");
			
			memory = new MemoryVo();
	        memory.setCommitted((Long)heapMemoryUsage.get("committed"));
	        memory.setInit((Long)heapMemoryUsage.get("init"));
	        memory.setMax((Long)heapMemoryUsage.get("max"));
	        memory.setUsed((Long)heapMemoryUsage.get("used"));
	        
        	logger.debug("nodeName: [{}], memoryUsage: [committed: {}, init:{}, max:{}, used:{}]", new Object[]{nodeName, memory.getCommitted(), memory.getInit(), memory.getMax(), memory.getUsed()});
		} catch (Exception e) {
			logger.error("unhandled exception has errored : ", e);
		}
        
        return memory;
	}
	
	public static OperationgSystemVo getOperatingSystemUsage(String nodeName) {
		JmxClient jmxClient = jmxClientMap.get(nodeName);
        
		OperationgSystemVo osVo = null;
		try {
			ObjectName objectName=new ObjectName("java.lang:type=OperatingSystem");			
			
			MBeanServerConnection connection = jmxClient.getJmxConnector().getMBeanServerConnection();
			
			osVo = new OperationgSystemVo();

			osVo.setName((String) connection.getAttribute(objectName, "Name"));
			osVo.setVersion((String) connection.getAttribute(objectName, "Version"));
			osVo.setArch((String) connection.getAttribute(objectName, "Arch"));
			osVo.setSystemLoadAverage((Double) connection.getAttribute(objectName, "SystemLoadAverage"));
			osVo.setAvailableProcessors((Integer)connection.getAttribute(objectName, "AvailableProcessors"));
			
			osVo.setFreePhysicalMemory((Long) connection.getAttribute(objectName, "FreePhysicalMemorySize"));
			osVo.setFreeSwapSpaceSize((Long) connection.getAttribute(objectName, "FreeSwapSpaceSize"));
			osVo.setProcessCpuTime((Long) connection.getAttribute(objectName, "ProcessCpuTime"));
			osVo.setCommittedVirtualMemorySize((Long) connection.getAttribute(objectName, "CommittedVirtualMemorySize"));
			osVo.setTotalPhysicalMemorySize((Long) connection.getAttribute(objectName, "TotalPhysicalMemorySize"));
			osVo.setTotalSwapSpaceSize((Long) connection.getAttribute(objectName, "TotalSwapSpaceSize"));
			
		} catch (Exception e) {
			logger.error("unhandled exception has errored : ", e);
		}
        
        return osVo;
	}
	
	public static String getCpuUsage(String nodeName) {
		String cpuUsageStr = null;
		JmxClient jmxClient = jmxClientMap.get(nodeName);
		
		try {
			MBeanServerConnection connection = jmxClient.getJmxConnector().getMBeanServerConnection();
	    	
			ObjectName osObjectName = new ObjectName("java.lang:type=OperatingSystem");			
			ObjectName runTimeObjectName = new ObjectName("java.lang:type=Runtime");			

			//before Cpu
			int availableProcessors = (Integer)connection.getAttribute(osObjectName, "AvailableProcessors");
		    long prevUpTime = (Long) connection.getAttribute(runTimeObjectName, "Uptime");
		    long prevProcessCpuTime = (Long) connection.getAttribute(osObjectName, "ProcessCpuTime");
		    
		    try  {
		        Thread.sleep(1000);
		    } catch (Exception ignored) { 
		    	// ignore
		    }
		    
			//after Cpu
		    long upTime = (Long) connection.getAttribute(runTimeObjectName, "Uptime");
		    long processCpuTime = (Long) connection.getAttribute(osObjectName, "ProcessCpuTime");

		    long elapsedCpu = processCpuTime - prevProcessCpuTime;
		    long elapsedTime = upTime - prevUpTime;		    
		    
		    double cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));
		    cpuUsageStr = String.format("%.2f", cpuUsage);
        	logger.debug("nodeName: [{}], cpuUsage: [{}]", nodeName, cpuUsageStr);
		} catch (Exception e) {
			logger.error("unhandled exception has errored : ", e);
		}
		
		return cpuUsageStr;
	}
	
	public static HashMap<String,Object> getObjectNameInfo(ObjectName objName, String nodeName) {
		JmxClient jmxClient = jmxClientMap.get(nodeName);

		try {
			MBeanServerConnection connection = jmxClient.getJmxConnector().getMBeanServerConnection();
		    HashMap<String, Object> infoMap = new HashMap<String,Object>();
			Set<ObjectName> names = new TreeSet<ObjectName>(connection.queryNames(objName, null));
				
			for (ObjectName name : names) {
				logger.info("#######################");
				logger.info("\tObjectName = " + name);

				MBeanInfo info = connection.getMBeanInfo(name);
		        MBeanAttributeInfo[] attributes = info.getAttributes();
		        
		        for (MBeanAttributeInfo attr : attributes) {
		        	logger.info("==========================");
		        	logger.info("attrName = " + attr.getName());
		        	logger.info("attrType = " + attr.getType());
		        	logger.info("connection.getAttribute = " + connection.getAttribute(name, attr.getName()));

		        	infoMap.put(attr.getName(), connection.getAttribute(name, attr.getName()));
				}
			}		    
		    
		    return infoMap;
		} catch (Exception e) {
			logger.error("unhandled exception has errored : ", e);
		}
		
		return null;
	}

	public static HashMap<String,Object> getCacheStatisticsInfo(String nodeName){
		try {
			ObjectName cacheStatisticsInfo = new ObjectName("jboss.infinispan:type=Cache,name=\"default(dist_sync)\",manager=\"clustered\",component=Statistics");	
			HashMap <String, Object> cacheMap = getObjectNameInfo(cacheStatisticsInfo , nodeName);
			return cacheMap;
		} catch (Exception e) {
			logger.error("unhandled exception has errored : ", e);
		}
		
		return null;
	}
	
	public static HashMap<String,Object> getCacheManagerInfo(String nodeName){
		try {
			ObjectName cacheManagerObjectName = new ObjectName("jboss.infinispan:type=CacheManager,name=\"clustered\",component=CacheManager");	
			HashMap <String, Object> cacheManagerMap = getObjectNameInfo(cacheManagerObjectName , nodeName);
			return cacheManagerMap;
		} catch (Exception e) {
			logger.error("unhandled exception has errored : ", e);
		}
		
		return null;
	}

	/**
	 * <pre>
	 * 
	 * </pre>
	 * @return
	 */
	public static List<MemoryVo> getMemoryUsageList() {
		List<MemoryVo> memoryList = new ArrayList<MemoryVo>();
		List<String> nodeList = getServerList();
		
		MemoryVo memory = null;
		for (String nodeName : nodeList) {
			memory = getMemoryUsage(nodeName);
			memoryList.add(memory);
		}
		
		return memoryList;
	}

	/**
	 * <pre>
	 * 
	 * </pre>
	 * @return
	 */
	public static List<String> getCpuUsageList() {
		Map<String, String> cpuMap = new TreeMap<String, String>();
		List<String> cpuList = new ArrayList<String>();
		List<String> nodeList = getServerList();
		
		for (String nodeName : nodeList) {
			new CpuInfo(nodeName, cpuMap).start();
		}

		try {
			Thread.sleep(1500);
		} catch (Exception e) {
			//ignore
		}
		
		for (String nodeName : nodeList) {
			cpuList.add(cpuMap.get(nodeName));
		}
		
		return cpuList;
	}
}
//end of JmxClientManager.java

class CpuInfo extends Thread {
	
	private String nodeName;
	private Map<String, String> cpuMap;
	
	/**
	 * <pre>
	 * 
	 * </pre>
	 */
	public CpuInfo(String nodeName, Map<String, String> cpuMap) {
		this.nodeName = nodeName;
		this.cpuMap = cpuMap;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		cpuMap.put(nodeName, JmxClientManager.getCpuUsage(nodeName));
	}
	
}