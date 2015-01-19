/* 
 * Athena Peacock Dolly - DataGrid based Clustering 
 * 
 * Copyright (C) 2014 Open Source Consulting, Inc. All rights reserved by Open Source Consulting, Inc.
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
 * Bong-Jin Kwon	2015. 1. 9.		First Draft.
 */
package com.athena.dolly.controller.tomcat.instance.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;

/**
 * <pre>
 *
 * </pre>
 * @author Bong-Jin Kwon
 * @version 2.0
 */
@Entity
@DynamicUpdate
public class TomcatInstance {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column(nullable = false)
	private String instanceName;
	
	@Column(nullable = false)
	private String ipAddr;
	
	@Column(nullable = false)
	private String catalinaHome;
	
	@Column(nullable = false)
	private String catalinaBase;
	
	@Column(nullable = false)
	private String envScriptFile;

	@Column(nullable = false)
	private String startScriptFile;

	@Column(nullable = false)
	private String stopScriptFile;
	
	@Column(nullable = false)
	private int sshPort;
	
	@Column(nullable = false)
	private String sshUsername;
	
	@Column(nullable = false)
	private String sshPassword;
	
	private int httpPort;
	
	private int applications; // application 개수
	
	private int state;
	
	public TomcatInstance() {}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getIpAddr() {
		return ipAddr;
	}

	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}

	public String getCatalinaHome() {
		return catalinaHome;
	}

	public void setCatalinaHome(String catalinaHome) {
		this.catalinaHome = catalinaHome;
	}

	public String getCatalinaBase() {
		return catalinaBase;
	}

	public void setCatalinaBase(String catalinaBase) {
		this.catalinaBase = catalinaBase;
	}

	public String getEnvScriptFile() {
		return envScriptFile;
	}

	public void setEnvScriptFile(String envScriptFile) {
		this.envScriptFile = envScriptFile;
	}

	public String getStartScriptFile() {
		return startScriptFile;
	}

	public void setStartScriptFile(String startScriptFile) {
		this.startScriptFile = startScriptFile;
	}

	public String getStopScriptFile() {
		return stopScriptFile;
	}

	public void setStopScriptFile(String stopScriptFile) {
		this.stopScriptFile = stopScriptFile;
	}

	public int getSshPort() {
		return sshPort;
	}

	public void setSshPort(int sshPort) {
		this.sshPort = sshPort;
	}

	public String getSshUsername() {
		return sshUsername;
	}

	public void setSshUsername(String sshUsername) {
		this.sshUsername = sshUsername;
	}

	public String getSshPassword() {
		return sshPassword;
	}

	public void setSshPassword(String sshPassword) {
		this.sshPassword = sshPassword;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}

	public int getApplications() {
		return applications;
	}

	public void setApplications(int applications) {
		this.applications = applications;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
	
}
