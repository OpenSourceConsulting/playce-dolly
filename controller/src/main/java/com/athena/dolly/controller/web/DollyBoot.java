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
 * Bong-Jin Kwon	2015. 1. 6.			First Draft.
 */
package com.athena.dolly.controller.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main class with Spring Boot
 * 
 * - required java option : -Dspring.config.name=dolly -Dspring.profiles.active=[local|dev|prd]
 * 
 * @author BongJin Kwon
 *
 */
@Configuration
@RestController
@EnableAutoConfiguration
@ComponentScan(basePackages={"com.athena.dolly.controller"})
//@PropertySource(value={"classpath:dolly.properties","classpath:dolly-${spring.profiles.active:local}.properties"})
public class DollyBoot {

	public static void main(String[] args) {
		SpringApplication.run(DollyBoot.class, args);
	}

}
