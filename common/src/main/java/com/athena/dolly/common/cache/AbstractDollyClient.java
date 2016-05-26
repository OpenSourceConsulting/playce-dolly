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
 * BongJin Kwon		2016. 5. 26.		First Draft.
 */
package com.athena.dolly.common.cache;

import java.net.ConnectException;

import org.infinispan.client.hotrod.exceptions.TransportException;

import com.athena.dolly.common.cache.client.DollyClient;

/**
 * <pre>
 * 
 * </pre>
 * @author Bongjin Kwon
 * @version 1.0
 */
public abstract class AbstractDollyClient implements DollyClient {
	
	/**
	 * client 가 사용 가능한지.
	 */
	private boolean useable;

	/**
	 * <pre>
	 * 
	 * </pre>
	 */
	public AbstractDollyClient() {

	}

	public boolean isUseable() {
		return useable;
	}

	protected void setUseable(boolean useable) {
		this.useable = useable;
	}
	
	protected void handleException(String name, Exception e) {
		
		if (e instanceof TransportException || e instanceof ConnectException) {
			DollyManager.setSkipConnection(name);
		} else if (e instanceof com.couchbase.client.vbucket.ConfigurationException) {
			DollyManager.setSkipConnection(name);
		} else if (e.getMessage().startsWith("Timed out waiting for")) {
			DollyManager.setSkipConnection(name);
		} else {
			e.printStackTrace();
		}
	}
	
}
//end of AbstractDollyClient.java