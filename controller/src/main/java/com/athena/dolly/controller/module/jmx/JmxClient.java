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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * 
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
public class JmxClient {
	
    private static final Logger logger = LoggerFactory.getLogger(JmxClient.class);
	
	private static final int RETRY_CNT = 3;
	private static final String URL_PREFIX = "service:jmx:remoting-jmx://";
	private static final String EMBEDDED_URL_PREFIX = "service:jmx:rmi:///jndi/rmi://";

	private String connectionUrl;
	private String user;
	private String passwd;
	private boolean embedded;
	private String nodeName;
	private JMXConnector jmxConnector;
	
	public JmxClient(String connectionUrl, String user, String passwd, boolean embedded) {
		this.connectionUrl = connectionUrl;
		this.user = user;
		this.passwd = passwd;
		this.embedded = embedded;
		
		init();
	}
	
	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}
	
	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	/**
	 * @return the jmxConnector
	 */
	public JMXConnector getJmxConnector() {
		if (jmxConnector == null) {
			init();
		}
		
		return jmxConnector;
	}
	
	/**
	 * <pre>
	 * 
	 * </pre>
	 */
	public void init() {
        Map<String, Object> environment = new HashMap<String, Object>();
        
        if (user != null && passwd != null) {
        	environment.put(JMXConnector.CREDENTIALS, new String[] {user, passwd});
        }
        environment.put("java.naming.factory.url.pkgs", "org.jboss.ejb.client.naming");
        environment.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_STARTTLS", false);
        
		try {
			JMXServiceURL serviceURL = null;
			if (embedded) {
				serviceURL = new JMXServiceURL(EMBEDDED_URL_PREFIX + connectionUrl + "/jmxrmi");
			} else {
				serviceURL = new JMXServiceURL(URL_PREFIX + connectionUrl);
			}

	        int cnt = 0;
	        while(cnt++ < RETRY_CNT) {
		        try {
		        	this.jmxConnector = JMXConnectorFactory.connect(serviceURL, environment);
		        	break;
		        } catch(Exception e) {
		        	logger.error("Unhandled exception has occurred. : ", e);
		        }
	        }
		} catch (MalformedURLException e) {
        	logger.error("MalformedURLException has occurred. : ", e);
		}
	}//end of init()
}
//end of JmxClient.java