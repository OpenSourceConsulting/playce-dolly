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
package com.athena.dolly.common.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.athena.dolly.common.cache.client.DollyClient;
import com.athena.dolly.common.cache.client.impl.CouchbaseClient;
import com.athena.dolly.common.cache.client.impl.HotRodClient;
import com.athena.dolly.common.exception.ConfigurationException;
import com.athena.dolly.common.stats.DollyStats;

/**
 * <pre>
 * Infinispan Data Grid Server에 대한 접속, 데이터 입력/조회/삭제 기능을 수행하는 관리 클래스
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class DollyManager {
	
	private static Map<String, AbstractDollyClient> clientMap = new HashMap<String, AbstractDollyClient>();
    private static DollyClient _client;
	private static DollyConfig config;
	
	private static boolean skipConnection = false;
	
	/**
	 * <pre>
	 * get DollyClient by name.
	 * </pre>
	 * @param name
	 * @return
	 */
	public static AbstractDollyClient getClient(String name) {
		
		return clientMap.get(name);
    }
	
	/**
	 * <pre>
	 * create DollyClient.
	 * </pre>
	 * @param name
	 * @param prop
	 * @return
	 */
	public static AbstractDollyClient createClient(String name, Properties prop) {
    	
		AbstractDollyClient client = clientMap.get(name);
    	
    	if (client != null) {
			return client;
		}
    	
    	synchronized (clientMap) {
    		
    		client = clientMap.get(name);
        	
        	if (client != null) {
    			return client;
    		}
    		
    		try {
        		DollyConfig dollyConfig = new DollyConfig().load(prop);
        		
        		if (dollyConfig.isVerbose()) {
            		System.out.println("[Dolly] Properties : ");
            		System.out.println(config.getProperties().toString().replaceAll(", ",  "\n"));
            	}
            	
            	if (dollyConfig.getClientType().equals("infinispan")) {
                	client = new HotRodClient(name, dollyConfig);
            	} else if (dollyConfig.getClientType().equals("couchbase")) {
                	client = new CouchbaseClient();
            	}
            	
    		} catch (ConfigurationException e) {
                System.err.println("[Dolly] Configuration error : " + e.getMessage());
                throw new RuntimeException(e);
    		}
        	
        	if (client == null) {
    			throw new RuntimeException("DollyClient create failed.");
    		}
        	
        	clientMap.put(name, client);
		}
    	
    	
    	
    	return client;
    }
	

    /**
     * <pre>
     * Singleton 형태의 DollyManager로부터 DollyClient 인스턴스를 가져온다.
     * </pre>
     * @return
     */
    public synchronized static DollyClient getClient() {
        if (_client == null) {
        	_client = init();
        }
        
        return _client;
    }//end of getInstance()

    public static boolean isSkipConnection() {
    	return skipConnection;
    }
    
    public synchronized static void setSkipConnection(String name) {
    	if (name == null && !skipConnection) {
    		skipConnection = true;

    		System.out.println("[Dolly] Could not connect to session server. Connection will be blocked by the time server is running.");
    		
    		new ConnectionCheckThread(name).start();
    	} else if(name != null) {
    		AbstractDollyClient client = DollyManager.getClient(name);
    		
    		if(client.isUseable()) {
    			new ConnectionCheckThread(name).start();
    		}
    		
    	}
    }
    
    public static void resetSkipConnection() {
    	skipConnection = false;
    }
    
    /**
     * <pre>
     * 주어진 프로퍼티를 이용하여 Infinispan Data Grid Server에 접속하여 RemoteCache object를 가져온다. 
     * </pre>
     * @return
     */
    private static DollyClient init() {
    	DollyClient client = null;

    	if (config == null) {
    		try {
                config = new DollyConfig().load();
			} catch (ConfigurationException e) {
	            System.err.println("[Dolly] Configuration error : " + e.getMessage());
	            e.printStackTrace();
			}
    	}
    	
    	if (config.isVerbose()) {
    		System.out.println("[Dolly] Properties : ");
    		System.out.println(config.getProperties().toString().replaceAll(", ",  "\n"));
    	}
    	
    	if (config.getClientType().equals("infinispan")) {
        	client = new HotRodClient();
    	} else if (config.getClientType().equals("couchbase")) {
        	client = new CouchbaseClient();
    	}
    	
    	return client;
    }//end of init()
    
    /**
     * <pre>
     * Cache Client 객체를 초기화 한다.
     * </pre>
     * @return
     */
    public static void resetClient() {
    	_client = init();
    }//end of resetClient()

	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		try {
			Map<String, String> value = new HashMap<String, String>();
			value.put("key1", "value1");
			value.put("key2", "value2");
			value.put("key3", "value3");
			
			try {
				DollyManager.getClient().put("cacheKey", value);
			} catch (Exception e) {
			}
			
			value = (Map<String, String>)DollyManager.getClient().get("cacheKey");
			System.out.println("Value => " + value);
			
			DollyManager.getClient().printAllCache();
			
			DollyStats stat = DollyManager.getClient().getStats();
			System.out.println(stat);

			DollyManager.getClient().remove("cacheKey");

			DollyManager.getClient().getKeys(null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DollyManager.getClient().destory();
		}
	}
}

class ConnectionCheckThread extends Thread {
	
	private String name;
	
	public ConnectionCheckThread(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		while (true) {
			try {
				
				if (name == null) {
					DollyManager.getClient().healthCheck();
					DollyManager.resetClient();
					DollyManager.resetSkipConnection();
				} else {
					AbstractDollyClient client = DollyManager.getClient(name);
					client.setUseable(false);
					
					client.healthCheck();
					client.initClient();
				}
				
				System.out.println("client init success.");
				break;
			} catch (Exception e) {
				System.out.println(e.toString());
				
				try {
					Thread.sleep(2000);
				} catch (Exception e1) {
					// ignore..
				}
			}
		}
	}
}
//end of DollyManager.java