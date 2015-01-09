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
package com.athena.dolly.controller.tomcat.instance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.athena.dolly.controller.tomcat.instance.domain.TomcatInstance;
import com.athena.dolly.controller.tomcat.instance.service.TomcatInstanceService;
import com.athena.dolly.controller.web.common.model.ExtjsGridParam;

/**
 * <pre>
 *
 * </pre>
 * @author Bong-Jin Kwon
 * @version 2.0
 */
@RestController
@RequestMapping("/tomcat")
public class TomcatInsanceController {

	@Autowired
	private TomcatInstanceService service;
	
	public TomcatInsanceController() {
		// TODO Auto-generated constructor stub
	}
	
	@RequestMapping(value="/instance/_all", method=RequestMethod.GET)
	public Page<TomcatInstance> getList(ExtjsGridParam gridParam){
		
		//service.save(new TomcatInstance("inst11", "1234"));
		//service.save(new TomcatInstance("inst22", "2222"));
		
		return service.getList(new PageRequest(gridParam.getPage()-1, gridParam.getLimit()));
	}
	
	@RequestMapping(value="/instance", method={RequestMethod.POST, RequestMethod.PUT})
	public void save(TomcatInstance inst){
		service.save(inst);
	}
	
	@RequestMapping(value="/instance/{instId}", method=RequestMethod.GET)
	public TomcatInstance get(@PathVariable Long instId){
		return service.getOne(instId);
	}
	
	@RequestMapping(value="/instance/{instId}", method=RequestMethod.DELETE)
	public void delete(@PathVariable Long instId){
		service.delete(instId);
	}

}
