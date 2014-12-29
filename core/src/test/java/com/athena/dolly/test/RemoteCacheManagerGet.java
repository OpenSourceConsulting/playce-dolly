package com.athena.dolly.test;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Properties;


import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.junit.Test;

public class RemoteCacheManagerGet {
	@Test
	public void getTest() throws Exception {
		Properties prop = new Properties();
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("cache2.properties");
		prop.load(in);
		in.close();
		
		ConfigurationBuilder builder = new ConfigurationBuilder();
		RemoteCacheManager cacheManager = new RemoteCacheManager(builder.withProperties(prop).build());
		RemoteCache<String, String> cache = cacheManager.getCache();
		
		assertEquals("Ji-Woong Choi", (String)cache.get("name"));
	}
}
