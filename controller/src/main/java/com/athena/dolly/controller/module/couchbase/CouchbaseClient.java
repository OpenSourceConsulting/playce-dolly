/* 
 * Copyright (C) 2012-2015 Open Source Consulting, Inc. All rights reserved by Open Source Consulting, Inc.
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
 * Sang-cheon Park	2015. 1. 6.		First Draft.
 */
package com.athena.dolly.controller.module.couchbase;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.athena.dolly.controller.module.DollyClient;
import com.athena.dolly.controller.module.vo.MemoryVo;

/**
 * <pre>
 * Spring RestTemplate을 이용하여 Couchbase 상태 정보를 조회하기 위한 Component 클래스
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class CouchbaseClient extends DollyClient {
	
    private static final Logger logger = LoggerFactory.getLogger(CouchbaseClient.class);
	
	private static final String AUTH_HEADER_KEY = "Authorization";

	private String url;
	private String name;
	private String passwd;
	private String credential;
	
	private String cpu;
	private long totalMem;
	private long freeMem;
	private long timestamp = 0L;
	
	public CouchbaseClient(String url, String name, String passwd) {
		this.url = url;
		this.name = name;
		this.passwd = passwd;
		
		init();
	}

	/**
	 * 유효하지 않은 인증서를 가진 호스트로 HTTPS 호출 시 HandShake Exception 및 인증서 관련 Exception이 발생하기 때문에
	 * SSL 인증서 관련 검증 시 예외를 발생하지 않도록 추가됨.
	 */
	public void init() {
		getCredential();
		System.setProperty("jsse.enableSNIExtension", "false");
	}//end of afterPropertiesSet()
	
	/**
	 * <pre>
	 * HTTP Header에 인증 정보를 포함시킨다.
	 * </pre>
	 * @return
	 */
	private HttpEntity<Object> setHTTPEntity(String body) {
		List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
	    acceptableMediaTypes.add(MediaType.APPLICATION_JSON);
	    
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);
		requestHeaders.setAccept(acceptableMediaTypes);
		
		if (getCredential() != null) {
			requestHeaders.set(AUTH_HEADER_KEY, getCredential());
		}
		
		if (body != null) {
			logger.debug("Content Body => {}", body);
			return new HttpEntity<Object>(body, requestHeaders);
		} else {
			return new HttpEntity<Object>(requestHeaders);
		}
	}//end of addAuth()
	
	/**
	 * <pre>
	 * Couchbase에 전달하기 위한 인증 정보를 생성한다. 
	 * </pre>
	 * @return
	 */
	public String getCredential() {
		if (credential == null && name != null && passwd != null) {
			try {
				String plain = name + ":" + passwd;
				credential = "Basic " + Base64.encodeBase64String(plain.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error("UnsupportedEncodingException has occurred.", e);
			}
		}
		
		return credential;
	}//end of getCredential()
	
	/**
	 * <pre>
	 * API를 호출하고 결과를 반환한다.
	 * </pre>
	 * @param api
	 * @param body
	 * @param method
	 * @return
	 * @throws RestClientException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> submit(String api, String body, HttpMethod method) throws RestClientException, Exception {
		Assert.isTrue(StringUtils.isNotEmpty(api), "api must not be null");

		try {
			RestTemplate rt = new RestTemplate();
			ResponseEntity<?> response = rt.exchange(api, method, setHTTPEntity(body), Map.class);
			
			logger.debug("[Request URL] : {}", api);
			logger.debug("[Response] : {}", response);
			
			return (Map<String, Object>) response.getBody();
		} catch (RestClientException e) {
			logger.error("RestClientException has occurred.", e);
			throw e;
		} catch (Exception e) {
			logger.error("Unhandled Exception has occurred.", e);
			throw e;
		}
	}//end of submit()

	@SuppressWarnings("unchecked")
	public MemoryVo getMemoryUsage() {
		MemoryVo memory = null;
		
		if (System.currentTimeMillis() - timestamp > 2000) {
			try {
				Map<String, Object> response = submit(url + "/default/buckets/" + name, null, HttpMethod.GET);
				
				List<Map<String, Object>> nodeList = (List<Map<String, Object>>) response.get("nodes");
				Map<String, Object> systemStats = (Map<String, Object>) nodeList.get(0).get("systemStats");
				
				this.totalMem = (Long)systemStats.get("mem_total");
				this.freeMem = (Long)systemStats.get("mem_free");
				this.cpu = systemStats.get("cpu_utilization_rate").toString();
				this.timestamp = System.currentTimeMillis();
			} catch (RestClientException e) {
				logger.error("RestClientException has occurred.", e);
			} catch (Exception e) {
				logger.error("Unhandled Exception has occurred.", e);
			}
		}
		
		memory = new MemoryVo();
		memory.setCommitted(this.totalMem);
		memory.setMax(this.totalMem);
		memory.setUsed(this.totalMem - this.freeMem);
		
		return memory;
	}

	@SuppressWarnings("unchecked")
	public String getCpuUsage() {
		if (System.currentTimeMillis() - timestamp > 2000) {
			try {
				Map<String, Object> response = submit(url + "/default/buckets/" + name, null, HttpMethod.GET);
				
				List<Map<String, Object>> nodeList = (List<Map<String, Object>>) response.get("nodes");
				Map<String, Object> systemStats = (Map<String, Object>) nodeList.get(0).get("systemStats");
				
				this.totalMem = (Long)systemStats.get("mem_total");
				this.freeMem = (Long)systemStats.get("mem_free");
				this.cpu = systemStats.get("cpu_utilization_rate").toString();
				this.timestamp = System.currentTimeMillis();
			} catch (RestClientException e) {
				logger.error("RestClientException has occurred.", e);
			} catch (Exception e) {
				logger.error("Unhandled Exception has occurred.", e);
			}
		}
		
		return this.cpu;
	}

	public static void main(String[] args) {
		CouchbaseClient client = new CouchbaseClient("http://127.0.0.1:8091/pools", "dolly", "dolly");
		
		try {
			client.init();
			System.out.println(client.getMemoryUsage());
			System.out.println(client.getCpuUsage());
		} catch (RestClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
//end of CouchbaseClient.java