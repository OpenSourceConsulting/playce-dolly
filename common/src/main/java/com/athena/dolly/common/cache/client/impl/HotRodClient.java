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
 * Sang-cheon Park	2014. 12. 23.		First Draft.
 */
package com.athena.dolly.common.cache.client.impl;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.ServerStatistics;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.exceptions.TransportException;

import com.athena.dolly.common.cache.DollyConfig;
import com.athena.dolly.common.cache.DollyManager;
import com.athena.dolly.common.cache.SessionKey;
import com.athena.dolly.common.cache.client.DollyClient;
import com.athena.dolly.common.exception.ConfigurationException;
import com.athena.dolly.common.stats.DollyStats;

/**
 * <pre>
 * Infinispan용 Dolly Client
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class HotRodClient implements DollyClient {

	/**
	 * 동일 cacheKey(세션 ID)에 대한 read-modify-write 원자성만 보장하기 위한 락 스트라이프 수.
	 * 인스턴스 전역 synchronized 대신 키 해시로 분산된 락을 사용하여 서로 다른 세션은 병렬로 처리한다.
	 */
	private static final int LOCK_STRIPES = 256;

	private DollyConfig config;
	private RemoteCache<String, Object> cache;

	/** cacheKey 해시 기반 락 스트라이프. 생성 시 한 번만 채우고 이후 불변. */
	private final Object[] locks;

    /**
     * <pre>
     * 주어진 프로퍼티를 이용하여 Infinispan Data Grid Server에 접속하여 RemoteCache object를 가져온다.
     * </pre>
     */
	public HotRodClient() {
		if (DollyConfig.properties == null || config == null) {
			try {
	            config = new DollyConfig().load();
			} catch (ConfigurationException e) {
	            System.err.println("[Dolly] Configuration error : " + e.getMessage());
	            e.printStackTrace();
			}
		}

		Object[] stripes = new Object[LOCK_STRIPES];
		for (int i = 0; i < LOCK_STRIPES; i++) {
			stripes[i] = new Object();
		}
		this.locks = stripes;

		// https://issues.jboss.org/browse/ISPN-4468
		ConfigurationBuilder builder = new ConfigurationBuilder();
	    cache = new RemoteCacheManager(builder.withProperties(DollyConfig.properties).build()).getCache();
	}//end of Default Contructor()

	/**
	 * <pre>
	 * 주어진 cacheKey 에 해당하는 락 스트라이프를 반환한다.
	 * 같은 키는 항상 같은 락에 매핑되어 read-modify-write 원자성이 보장되고,
	 * 다른 키는 (해시 충돌이 없는 한) 서로 다른 락을 사용하여 병렬 처리된다.
	 * </pre>
	 */
	private Object lockFor(String cacheKey) {
		int hash = (cacheKey == null) ? 0 : cacheKey.hashCode();
		return locks[(hash & 0x7fffffff) % LOCK_STRIPES];
	}//end of lockFor()

	public Object get(String cacheKey) {
		Object obj = null;

		if (!DollyManager.isSkipConnection()) {
			try {
				obj = cache.get(cacheKey);
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}			
		}
		return obj;
	}//end of get()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#get(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public Object get(String cacheKey, String dataKey) {
		if (!DollyManager.isSkipConnection()) {
			try {
				Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheKey);
				
				if (attribute == null) {
					return null;
				} else {
					return attribute.get(dataKey);
				}
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}//end of get()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#put(java.lang.String, java.lang.Object)
	 */
	public void put(String cacheKey, Object value) throws Exception {
		if (!DollyManager.isSkipConnection()) {
			try {
				synchronized (lockFor(cacheKey)) {
					cache.put(cacheKey, value, -1, TimeUnit.SECONDS, config.getTimeout() * 60, TimeUnit.SECONDS);
				}
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
	}//end of put()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#put(java.lang.String, java.lang.String, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public void put(String cacheKey, String dataKey, Object value) throws Exception {
		if (!DollyManager.isSkipConnection()) {
			try {
				if (dataKey != null) {
					if (config.getSessionKeyList().size() < 1 || config.getSessionKeyList().contains(dataKey) || dataKey.equals("jvmRoute")) {
						synchronized (lockFor(cacheKey)) {
					    		Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheKey);

							if (attribute == null) {
								attribute = new ConcurrentHashMap<String, Object>();
							}

							if (value != null) {
								attribute.put(dataKey, value);
								cache.put(cacheKey, attribute, -1, TimeUnit.SECONDS, config.getTimeout() * 60, TimeUnit.SECONDS);
							}
						}
					} else {
						if (config.isVerbose()) {
							System.out.println("[Dolly] \"" + dataKey + "\" is not a member of \"dolly.session.key.list\".");
						}
					}
				}
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage() != null && e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
	}//end of put()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#remove(java.lang.String)
	 */
	public void remove(String cacheKey) throws Exception {
		if (!DollyManager.isSkipConnection()) {
			try {
				synchronized (lockFor(cacheKey)) {
					cache.remove(cacheKey);
				}
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
	}//end of remove()

    /* (non-Javadoc)
     * @see com.athena.dolly.enhancer.client.DollyClient#remove(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
	public void remove(String cacheKey, String dataKey) throws Exception {
		if (!DollyManager.isSkipConnection()) {
			try {
				synchronized (lockFor(cacheKey)) {
					Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheKey);

					if (attribute != null) {
						attribute.remove(dataKey);
					}

					cache.put(cacheKey, attribute, -1, TimeUnit.SECONDS, config.getTimeout() * 60, TimeUnit.SECONDS);
				}
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
    }//end of remove()
    
    /* (non-Javadoc)
     * @see com.athena.dolly.enhancer.client.DollyClient#getValueNames(java.lang.String)
     */
    @SuppressWarnings("unchecked")
	public Enumeration<String> getValueNames(String cacheKey) {
		if (!DollyManager.isSkipConnection()) {
			try {
		    		Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheKey);
				
				if (attribute == null) {
					return null;
				} else {
					return Collections.enumeration(attribute.keySet());
				}
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
		
		return null;
    }//end of getValueNames()

	/* (non-Javadoc)
	 * @see com.athena.dolly.common.cache.client.DollyClient#getValueNames(java.lang.String, java.util.Enumeration)
	 */
    @SuppressWarnings("unchecked")
	public Enumeration<String> getValueNames(String cacheKey, Enumeration<String> obj) {
		if (!DollyManager.isSkipConnection()) {
			try {
		    		Map<String, Object> attribute = (Map<String, Object>)cache.get(cacheKey);
				
				if (attribute == null) {
					return obj;
				} else {
					List<String> keyList1 = new ArrayList<String>(attribute.keySet());
					List<String> keyList2 = Collections.list(obj);
					
					for (String key : keyList2) {
						if (!keyList1.contains(key)) {
							keyList1.add(key);
						}
					}
					
					//return Collections.enumeration(attribute.keySet());
					return Collections.enumeration(keyList1);
				}
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
		
		return obj;
	}

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#getKeys()
	 */
	public List<SessionKey> getKeys(String viewName) {
		List<SessionKey> keyList = new ArrayList<SessionKey>();

		if (!DollyManager.isSkipConnection()) {
			try {
				Enumeration<String> cacheKeys = Collections.enumeration(cache.keySet());
				SessionKey key = null;
			    while (cacheKeys.hasMoreElements()) {
			    	key = new SessionKey();
			    	key.setKey(cacheKeys.nextElement());
			    	keyList.add(key);
			    }
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
	    
	    return keyList;
	}//end of getKeys()

    /* (non-Javadoc)
     * @see com.athena.dolly.enhancer.client.DollyClient#destory()
     */
    public void destory() {
	    cache.stop();
    }//end of destory()
    
	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#getStats()
	 */
	public DollyStats getStats() {
		DollyStats stats = new DollyStats();

		if (!DollyManager.isSkipConnection()) {
			try {
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
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
		
		return stats;
	}//end of getStats()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#printAllCache()
	 */
	public void printAllCache() {
		if (!DollyManager.isSkipConnection()) {
			try {
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
			} catch (Exception e) {
				if (e instanceof TransportException || e instanceof ConnectException) {
					DollyManager.setSkipConnection();
				} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
					DollyManager.setSkipConnection();
				} else if (e.getMessage().startsWith("Timed out waiting for")) {
					DollyManager.setSkipConnection();
				} else {
					e.printStackTrace();
				}
			}
		}
    }//end of printAllCache()
	
	/* (non-Javadoc)
	 * @see com.athena.dolly.common.cache.client.DollyClient#healthCheck()
	 */
	public void healthCheck() {
		cache.get("healthCheck");
	}//end of healthCheck()
}
//end of HotRodClient.java