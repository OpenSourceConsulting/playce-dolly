package com.athena.dolly.test;

import java.io.InputStream;
import java.util.Properties;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.junit.Test;

public class RemoteCacheManagerPut {
	@Test
	public void putTest() throws Exception { 
		Properties prop = new Properties();
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("cache1.properties");
		prop.load(in);
		in.close();
		
		ConfigurationBuilder builder = new ConfigurationBuilder();
		RemoteCacheManager cacheManager = new RemoteCacheManager(builder.withProperties(prop).build());
		RemoteCache<String, String> cache = cacheManager.getCache();
		
		cache.put("name", "Ji-Woong Choi");
	}
}
