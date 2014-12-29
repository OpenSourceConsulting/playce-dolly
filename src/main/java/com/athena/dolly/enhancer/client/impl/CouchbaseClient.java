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
package com.athena.dolly.enhancer.client.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.athena.dolly.enhancer.ConfigurationException;
import com.athena.dolly.enhancer.DollyConfig;
import com.athena.dolly.enhancer.client.DollyClient;
import com.athena.dolly.stats.DollyStats;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewDesign;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.google.gson.Gson;

/**
 * <pre>
 * Couchbase용 Dolly Client
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class CouchbaseClient implements DollyClient {

	private DollyConfig config;
	private com.couchbase.client.CouchbaseClient client;
	
	final Gson gson = new Gson();
	
	/**
	 * <pre>
	 * 주어진 프로퍼티를 이용하여 CouchbaseClient 객체를 생성한다. 
	 * </pre>
	 */
	public CouchbaseClient() {
		if (DollyConfig.properties == null || config == null) {
			try {
	            config = new DollyConfig().load();
			} catch (ConfigurationException e) {
	            System.err.println("[Dolly] Configuration error : " + e.getMessage());
	            e.printStackTrace();
			}
		}
		
		String[] uris = config.getCouchbaseUris().split(";");
		List<URI> uriList = new LinkedList<URI>();
		for (String uri : uris) {
			uriList.add(URI.create(uri));
		}
		
		try {
			client = new com.couchbase.client.CouchbaseClient(uriList, config.getCouchbaseBucketName(), config.getCouchbaseBucketPasswd());
		} catch (IOException e) {
			System.err.println("Error connecting to Couchbase: " + e.getMessage());
		}
	}//end of Default Contructor()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#get(java.lang.String)
	 */
	public Object get(String cacheKey) {
		Object obj = null;
		
		String document = (String) client.get(cacheKey);
		
		if (document != null) {
			obj = gson.fromJson(document, Object.class);
		}
		
		client.touch(cacheKey, config.getTimeout() * 60);
		
		return obj;
	}//end of get()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#get(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public Object get(String cacheKey, String dataKey) {
    	Map<String, Object> attribute = (Map<String, Object>)get(cacheKey);
		
		if (attribute == null) {
			return null;
		} else {
			return attribute.get(dataKey);
		}
	}//end of get()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#put(java.lang.String, java.lang.Object)
	 */
	public synchronized void put(String cacheKey, Object value) {
		client.set(cacheKey, config.getTimeout() * 60, gson.toJson(value));
	}//end of put()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#put(java.lang.String, java.lang.String, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public synchronized void put(String cacheKey, String dataKey, Object value) {
		if (dataKey != null) {
	    	Map<String, Object> attribute = (Map<String, Object>)get(cacheKey);
			
			if (attribute == null) {
				attribute = new ConcurrentHashMap<String, Object>();
			}
			
			attribute.put(dataKey, value);
			client.set(cacheKey, config.getTimeout() * 60, gson.toJson(attribute));
		}
	}//end of put()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#remove(java.lang.String)
	 */
	public synchronized Object remove(String cacheKey) {
		return client.delete(cacheKey);
	}//end of remove()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#remove(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public synchronized void remove(String cacheKey, String dataKey) {
		if (dataKey != null) {
	    	Map<String, Object> attribute = (Map<String, Object>)get(cacheKey);
			
			if (attribute == null) {
				attribute = new ConcurrentHashMap<String, Object>();
			}
			
			attribute.remove(dataKey);
			client.set(cacheKey, config.getTimeout() * 60, gson.toJson(attribute));
		}
	}//end of remove()
    
    /* (non-Javadoc)
     * @see com.athena.dolly.enhancer.client.DollyClient#getValueNames(java.lang.String)
     */
    @SuppressWarnings("unchecked")
	public Enumeration<String> getValueNames(String cacheKey) {
    	Map<String, Object> attribute = (Map<String, Object>)get(cacheKey);
		
		if (attribute == null) {
			return null;
		} else {
			return Collections.enumeration(attribute.keySet());
		}
    }//end of getValueNames()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#getKeys()
	 */
	public List<String> getKeys() {
		DesignDocument designDoc = getDesignDocument(config.getCouchbaseBucketName());

        boolean found = false;
        for (ViewDesign view : designDoc.getViews()) {
            if (view.getName().equals("get_all")) {
                found = true;
                break;
            }
        }
        
        if (!found) {
            ViewDesign design = new ViewDesign("get_all", "function (doc, meta) {\n" +
                    "  emit(meta.id, 1);\n" +
                    "}", "_count");
            designDoc.getViews().add(design);
            client.createDesignDoc(designDoc);
        }
        
		View view = client.getView(config.getCouchbaseBucketName(), "get_all");
		Query query = new Query().setReduce(false).setIncludeDocs(false);
		
		ViewResponse response = client.query(view, query);

		List<String> keyList = new ArrayList<String>();
		for (ViewRow row : response) {
			keyList.add(row.getKey());
		}
		
		return keyList;
	}//end of getKeys()

    /* (non-Javadoc)
     * @see com.athena.dolly.enhancer.client.DollyClient#destory()
     */
    public void destory() {
		client.shutdown(10, TimeUnit.SECONDS);
    }//end of destory()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#getStats()
	 */
	public DollyStats getStats() {
		Map<SocketAddress, Map<String, String>> statMap = client.getStats();
		
		List<SocketAddress> socketList = new ArrayList<SocketAddress>(statMap.keySet());
		
		Map<String, String> stat = null;
		for (SocketAddress addr : socketList) {
			stat = statMap.get(addr);
			
			System.out.println(addr + " : " + stat);
		}
		
		return new DollyStats();
	}//end of getStats()

	/* (non-Javadoc)
	 * @see com.athena.dolly.enhancer.client.DollyClient#printAllCache()
	 */
	public void printAllCache() {
		DesignDocument designDoc = getDesignDocument(config.getCouchbaseBucketName());

        boolean found = false;
        for (ViewDesign view : designDoc.getViews()) {
            if (view.getName().equals("get_all")) {
                found = true;
                break;
            }
        }
        
        if (!found) {
            ViewDesign design = new ViewDesign("get_all", "function (doc, meta) {\n" +
                    "  emit(meta.id, 1);\n" +
                    "}", "_count");
            designDoc.getViews().add(design);
            client.createDesignDoc(designDoc);
        }
        
		View view = client.getView(config.getCouchbaseBucketName(), "get_all");
		Query query = new Query().setReduce(false).setIncludeDocs(true).setLimit(20);
		//int docsPerPage = 25;
		
		ViewResponse response = client.query(view, query);
		System.out.println("TotalRow => " + response.getTotalRows());
		
		Gson gson = new Gson();
		for (ViewRow row : response) {
			Object obj = gson.fromJson((String) row.getDocument(), Object.class);
			System.out.println(row.getKey() + ":" + obj);
		}
	}//end of printAllCache()
	
	/**
	 * <pre>
	 * 지정된 Design Documents를 조회하고 존재하지 않을 경우 생성하여 반환한다.
	 * </pre>
	 * @param name
	 * @return
	 */
	private DesignDocument getDesignDocument(String name) {
		try {
			return client.getDesignDoc(name);
		} catch (com.couchbase.client.protocol.views.InvalidViewException e) {
			return new DesignDocument(name);
		}
	}//end of getDesignDocument()
}
//end of CouchbaseClient.java