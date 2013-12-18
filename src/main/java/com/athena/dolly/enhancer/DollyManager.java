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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.ServerStatistics;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import com.athena.dolly.stats.DollyStats;

/**
 * <pre>
 * 
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class DollyManager {

    private static DollyManager _instance;
	private static RemoteCache<String, Object> cache;
	private static DollyConfig config;

    public synchronized static DollyManager getInstance() {
        if (_instance == null) {
        	init();
            _instance = new DollyManager();
        }
        
        return _instance;
    }
    
    private static void init() {
    	if (DollyConfig.properties == null || config == null) {
    		try {
                config = new DollyConfig().load();
			} catch (ConfigurationException e) {
	            System.err.println("[Dolly] Configuration error : " + e.getMessage());
	            e.printStackTrace();
			}
    	}
    	
    	if (config.isVerbose()) {
    		System.out.println("[Dolly] infinispan.client.hotrod.server_list=" + DollyConfig.properties.getProperty("infinispan.client.hotrod.server_list"));
    	}
    	
		ConfigurationBuilder builder = new ConfigurationBuilder();
	    cache = new RemoteCacheManager(builder.withProperties(DollyConfig.properties).build()).getCache();
    }
    
    public void destory() {
	    cache.stop();
    }
    
	@SuppressWarnings("unchecked")
	public synchronized void setValue(String cacheName, String key, Object value) {
    	Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheName);
		
		if (attribute == null) {
			attribute = new ConcurrentHashMap<String, Object>();
		}
		
		attribute.put(key, value);
		cache.put(cacheName, attribute);
    }
    
	public Object getValue(String cacheName) {
    	return cache.get(cacheName);
    }
    
    @SuppressWarnings("unchecked")
	public Object getValue(String cacheName, String key) {
    	Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheName);
		
		if (attribute == null) {
			return null;
		} else {
			return attribute.get(key);
		}
    }
    
    @SuppressWarnings("unchecked")
	public Enumeration<String> getValueNames(String cacheName) {
    	Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheName);
		
		if (attribute == null) {
			return null;
		} else {
			return Collections.enumeration(attribute.keySet());
		}
    }
    
    @SuppressWarnings("unchecked")
	public synchronized void removeValue(String cacheName, String key) {
    	Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheName);
		
		if (attribute != null) {
			attribute.remove(key);
		}
    }
    
	public void printAllCache() {
	    System.out.println("ProtocolVersion : " + cache.getProtocolVersion());
	    System.out.println("Version : " + cache.getVersion());
	    System.out.println("isEmpty : " + cache.isEmpty());
	    System.out.println("Size : " + cache.size());
	    
	    Enumeration<String> cacheKeys = Collections.enumeration(cache.keySet());
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
	
	public DollyStats getStats() {
		DollyStats stats = new DollyStats();
		
		stats.setProtocolVersion(cache.getProtocolVersion());
		stats.setVersion(cache.getVersion());
		stats.setName(cache.getName());
		stats.setIsEmpty(cache.isEmpty());
		stats.setSize(cache.size());
		
		ServerStatistics statistics = cache.stats();
		stats.setTimeSinceStart(statistics.getStatistic(ServerStatistics.TIME_SINCE_START));
		stats.setCurrentNumberOfEntries(statistics.getStatistic(ServerStatistics.CURRENT_NR_OF_ENTRIES));
		stats.setTotalNumberOfEntries(statistics.getStatistic(ServerStatistics.TOTAL_NR_OF_ENTRIES));
		stats.setStores(statistics.getStatistic(ServerStatistics.STORES));
		stats.setRetrievals(statistics.getStatistic(ServerStatistics.RETRIEVALS));
		stats.setHits(statistics.getStatistic(ServerStatistics.HITS));
		stats.setMisses(statistics.getStatistic(ServerStatistics.MISSES));
		stats.setRemoveHits(statistics.getStatistic(ServerStatistics.REMOVE_HITS));
		stats.setRemoveMisses(statistics.getStatistic(ServerStatistics.REMOVE_MISSES));
		
		stats.setCacheKeys(new ArrayList<String>(cache.keySet()));
		
		return stats;
	}
}
//end of DollyManager.java