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
package com.athena.dolly.controller.tomcat.instance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.athena.dolly.controller.tomcat.instance.domain.TomcatInstance;
import com.athena.dolly.controller.tomcat.instance.domain.TomcatInstanceRepository;

/**
 * <pre>
 *
 * </pre>
 * @author Bong-Jin Kwon
 * @version 2.0
 */
@Service
public class TomcatInstanceService {

	@Autowired
	private TomcatInstanceRepository repo;
	
	public TomcatInstanceService() {
		// TODO Auto-generated constructor stub
	}
	
	public Page<TomcatInstance> getList(Pageable pageable){
		return repo.findAll(pageable);
	}
	
	/**
	 * insert or update
	 * @param inst
	 */
	public void save(TomcatInstance inst){
		repo.save(inst);
	}
	
	public TomcatInstance getOne(Long id){
		return repo.findOne(id);
	}
	
	public void delete(Long id){
		repo.delete(id);
	}

}
