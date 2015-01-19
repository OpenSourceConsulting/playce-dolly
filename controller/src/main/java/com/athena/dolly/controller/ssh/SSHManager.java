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
 * Bong-Jin Kwon	2015. 1. 19.		First Draft.
 */
package com.athena.dolly.controller.ssh;

import com.jcraft.jsch.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <pre>
 * ssh command manager.
 * </pre>
 * @author Bong-Jin Kwon
 * @version 2.0
 */
public class SSHManager {
	private static final Logger LOGGER = Logger.getLogger(SSHManager.class.getName());
	private JSch jsch;
	private String strUserName;
	private String strConnectionIP;
	private int intConnectionPort;
	private String strPassword;
	private Session session;
	private int intTimeOut;

	private void doCommonConstructorActions(String userName, String password, String connectionIP, String knownHostsFileName) {
		jsch = new JSch();

		try {
			if(knownHostsFileName != null && knownHostsFileName.length() > 0){
				jsch.setKnownHosts(knownHostsFileName);
			}
		} catch (JSchException jschX) {
			logError(jschX.getMessage());
		}

		strUserName = userName;
		strPassword = password;
		strConnectionIP = connectionIP;
	}
	
	public SSHManager(String userName, String password, String connectionIP) {
		this(userName, password, connectionIP, "");
	}

	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName) {
		doCommonConstructorActions(userName, password, connectionIP, knownHostsFileName);
		intConnectionPort = 22;
		intTimeOut = 60000;
	}

	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName, int connectionPort) {
		doCommonConstructorActions(userName, password, connectionIP, knownHostsFileName);
		intConnectionPort = connectionPort;
		intTimeOut = 60000;
	}

	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName, int connectionPort, int timeOutMilliseconds) {
		doCommonConstructorActions(userName, password, connectionIP, knownHostsFileName);
		intConnectionPort = connectionPort;
		intTimeOut = timeOutMilliseconds;
	}

	public String connect() {
		String errorMessage = null;

		try {
			session = jsch.getSession(strUserName, strConnectionIP, intConnectionPort);
			session.setPassword(strPassword);
			
			// UNCOMMENT THIS FOR TESTING PURPOSES, BUT DO NOT USE IN PRODUCTION
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(intTimeOut);
		} catch (JSchException jschX) {
			errorMessage = jschX.getMessage();
		}

		return errorMessage;
	}

	private String logError(String errorMessage) {
		if (errorMessage != null) {
			LOGGER.log(Level.SEVERE, "{0}:{1} - {2}", new Object[] { strConnectionIP, intConnectionPort, errorMessage });
		}

		return errorMessage;
	}

	private String logWarning(String warnMessage) {
		if (warnMessage != null) {
			LOGGER.log(Level.WARNING, "{0}:{1} - {2}", new Object[] { strConnectionIP, intConnectionPort, warnMessage });
		}

		return warnMessage;
	}

	public String sendCommand(String command) {
		StringBuilder outputBuffer = new StringBuilder();

		try {
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			channel.connect();
			InputStream commandOutput = channel.getInputStream();
			int readByte = commandOutput.read();

			while (readByte != 0xffffffff) {
				outputBuffer.append((char) readByte);
				readByte = commandOutput.read();
			}

			channel.disconnect();
		} catch (IOException ioX) {
			logWarning(ioX.getMessage());
			return null;
		} catch (JSchException jschX) {
			logWarning(jschX.getMessage());
			return null;
		}

		return outputBuffer.toString();
	}

	public void close() {
		session.disconnect();
	}
	/*
	public static void main(String[] args) {
		SSHManager sshMng = new SSHManager("alm", "opensource", "192.168.0.242");
		
		String errorMsg = sshMng.connect();
		
		if(errorMsg != null){
			System.out.println(errorMsg);
		}else{
			String logs = sshMng.sendCommand("ls -al");
			
			System.out.println(logs);
			
			sshMng.close();
		}
		
	}
*/
}