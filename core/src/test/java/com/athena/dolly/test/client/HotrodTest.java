/* 
 * Copyright (C) 2012-2014 Open Source Consulting, Inc. All rights reserved by Open Source Consulting, Inc.
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
 * Sang-cheon Park	2014. 12. 12.		First Draft.
 */
package com.athena.dolly.test.client;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.ServerStatistics;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

/**
 * <pre>
 * 
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class HotrodTest {

	private RemoteCache<String, Object> cache;
	
	public HotrodTest() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		Properties properties = new Properties();

		// infinispan-server-7.0.2.Final
		//properties.put("infinispan.client.hotrod.server_list", "121.138.109.61:11222");
		
		// startServer_standalone()
		//properties.put("infinispan.client.hotrod.server_list", "127.0.0.1:11222");
		
		// startServer_cluster1(), startServer_cluster2()
		//properties.put("infinispan.client.hotrod.server_list", "127.0.0.1:11222;127.0.0.1:11322;");
		properties.put("infinispan.client.hotrod.server_list", "192.168.0.11:11222;192.168.0.77:11222;");
		
		//properties.put("infinispan.client.hotrod.transport_factory", "org.infinispan.client.hotrod.impl.transport.tcp.TcpTransportFactory");
		//properties.put("infinispan.client.hotrod.marshaller", "org.infinispan.commons.marshall.jboss.GenericJBossMarshaller");
		//properties.put("infinispan.client.hotrod.async_executor_factory", "org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory");

		RemoteCacheManager manager = new RemoteCacheManager(builder.withProperties(properties).build());
		
		cache = manager.getCache();
	}
	
	public synchronized void setValue(String cacheKey, Object value) {
		cache.put(cacheKey, value);
		
		// Set Expiration & Eviction like this
		//cache.put(cacheKey, value, 60, TimeUnit.SECONDS); // do expire after 60 seconds later.
		//cache.put(cacheKey, value, -1, TimeUnit.SECONDS, 5 * 60, TimeUnit.SECONDS); // no expiration & do evict after 5 minutes later.
    }
	
	public synchronized void replaceValue(String cacheKey, Object value) {
		cache.replace(cacheKey, value);
    }
	
	public Object getValue(String cacheKey) {
    	return cache.get(cacheKey);
    }
	
	public synchronized void removeValue(String cacheKey) {
		cache.remove(cacheKey);
    }
	
	public synchronized void clear() {
		cache.clear();
    }
	
	public void printStats() {
	    System.out.println("ProtocolVersion : " + cache.getProtocolVersion());
	    System.out.println("Version : " + cache.getVersion());
	    System.out.println("isEmpty : " + cache.isEmpty());
	    System.out.println("Size : " + cache.size());
	    
		ServerStatistics statistics = cache.stats();
		System.out.println("TimeSinceStart : " + statistics.getStatistic(ServerStatistics.TIME_SINCE_START));
		System.out.println("CurrentNumberOfEntries : " + statistics.getStatistic(ServerStatistics.CURRENT_NR_OF_ENTRIES));
		System.out.println("TotalNumberOfEntries : " + statistics.getStatistic(ServerStatistics.TOTAL_NR_OF_ENTRIES));
		System.out.println("Stores : " + statistics.getStatistic(ServerStatistics.STORES));
		System.out.println("Retrievals : " + statistics.getStatistic(ServerStatistics.RETRIEVALS));
		System.out.println("Hits : " + statistics.getStatistic(ServerStatistics.HITS));
		System.out.println("Misses : " + statistics.getStatistic(ServerStatistics.MISSES));
		System.out.println("RemoveHits : " + statistics.getStatistic(ServerStatistics.REMOVE_HITS));
		System.out.println("RemoveMisses : " + statistics.getStatistic(ServerStatistics.REMOVE_MISSES));
	}
    
	public void printAllCache() {
	    Enumeration<String> cacheKeys = Collections.enumeration(cache.getBulk().keySet());
	    String cacheKey = null;
    	Object data = null;
	    int i = 1;
	    while (cacheKeys.hasMoreElements()) {
	    	cacheKey = cacheKeys.nextElement();
    		System.out.println("================== Element index [" + i + "] ==================");
    		System.out.println("Cache Key : " + cacheKey);
	    	
	    	data = cache.get(cacheKey);
	    	
	    	if (data != null) {
    			System.out.println("Cache Data : " + data.toString());
	    	} else {
	    		System.out.println("Cache Data is NULL.");
	    	}
    		
    		System.out.println("");
    		i++;  
	    }
    }
	
	/**
	 * <pre>
	 * 
	 * </pre>
	 * @param args
	 */
	public static void main(String[] args) {
		HotrodTest test = new HotrodTest();
		
		test.setValue("test-key1", "test-value11");
		test.setValue("test-key2", "test-value22");
		test.setValue("test-key3", "test-value33");

		test.printStats();
		test.printAllCache();

		System.out.println("\n========================================================\n");
		
		test.setValue("test-key1", "111111111111");
		test.setValue("test-key2", "222222222222");
		test.setValue("test-key3", "333333333333");
		
		try {
			SampleDto1 sample1 = null;
			for (int i = 0; i < 2; i++) {
				sample1 = new SampleDto1();
				sample1.setUserId("osci_" + i);
				sample1.setEmail("support" + i + "@osck.kr");
				test.setValue("sampleDto1-" + i, sample1);
			}
	
			test.printStats();
			test.printAllCache();

			
			sample1 = (SampleDto1) test.getValue("sampleDto1-1");
			System.out.println(sample1.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("\n========================================================\n");

		//test.removeValue("test-key1");
		//test.removeValue("test-key2");
		//test.removeValue("test-key3");

		try {
			SampleDto2 sample2 = null;
			for (int i = 0; i < 2; i++) {
				sample2 = new SampleDto2();
				sample2.setUserId("osci_" + i);
				sample2.setEmail("support" + i + "@osck.kr");
				test.setValue("sampleDto2-" + i, sample2);
			}
	
			test.printStats();
			test.printAllCache();
			
			sample2 = (SampleDto2) test.getValue("sampleDto2-1");
			System.out.println(sample2.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//test.clear();
	}

}
//end of HotrodTest.java