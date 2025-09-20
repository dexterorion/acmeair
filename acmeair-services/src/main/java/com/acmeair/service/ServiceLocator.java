/*******************************************************************************
* Copyright (c) 2013-2015 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package com.acmeair.service;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ServiceLocator {

	public static String REPOSITORY_LOOKUP_KEY = "com.acmeair.repository.type";
	private static String serviceType;
	private static Logger logger = Logger.getLogger(ServiceLocator.class.getName());

	private static AtomicReference<ServiceLocator> singletonServiceLocator = new AtomicReference<ServiceLocator>();

	private ServiceFactory serviceFactory;

	public static ServiceLocator instance() {
		if (singletonServiceLocator.get() == null) {
			synchronized (singletonServiceLocator) {
				if (singletonServiceLocator.get() == null) {
					singletonServiceLocator.set(new ServiceLocator());
				}
			}
		}
		return singletonServiceLocator.get();
	}

	public static void updateService(String serviceName){
		logger.info("Service Locator updating service to : " + serviceName);
		serviceType = serviceName;
		// Update the factory as well
		ServiceLocator locator = instance();
		if (locator.serviceFactory != null) {
			locator.serviceFactory.setServiceType(serviceName);
		}
	}

	private ServiceLocator() {
		serviceFactory = ServiceFactory.getInstance();

		String type = null;

		// Try various ways to get service type
		type = System.getProperty(REPOSITORY_LOOKUP_KEY);
		if (type != null) {
			logger.info("Found repository in jvm property:" + type);
		} else {
			type = System.getenv(REPOSITORY_LOOKUP_KEY);
			if (type != null) {
				logger.info("Found repository in environment property:" + type);
			}
		}

		// Check VCAP_SERVICES for cloud deployment
		if (type == null) {
			String vcapJSONString = System.getenv("VCAP_SERVICES");
			if (vcapJSONString != null) {
				logger.info("Reading VCAP_SERVICES");
				Object jsonObject = JSONValue.parse(vcapJSONString);
				JSONObject json = (JSONObject)jsonObject;
				String key;
				for (Object k: json.keySet()) {
					key = (String ) k;
					if (key.startsWith("mongo")) {
						logger.info("VCAP_SERVICES existed with service:"+key);
						type = "morphia";
						break;
					}
					// Add other service types as needed
				}
			}
		}

		// Default to morphia if nothing specified
		if (type == null) {
			type = "morphia";
			logger.info("No service type specified, defaulting to: " + type);
		}

		serviceType = type;
		serviceFactory.setServiceType(type);
		logger.info("ServiceType is now : " + serviceType);
	}

	public <T> T getService (Class<T> clazz) {
		logger.fine("Looking up service:  "+clazz.getName() + " with service type: " + serviceType);
		return serviceFactory.getService(clazz);
	}
	
	/**
	 * Retrieves the services that are available for use with the description for each service.
	 * @return Map containing a list of services available and a description of each one.
	 */
	public Map<String,String> getServices (){
		TreeMap<String,String> services = new TreeMap<String,String>();
		// For now, just return the current service type
		services.put(serviceType, "MongoDB/Morphia implementation");
		return services;
	}
	
	/**
	 * The type of service implementation that the application is 
	 * currently configured to use.  
	 * 
	 * @return The type of service in use, or "default" if no service has been set. 
	 */
	public String getServiceType (){
		if(serviceType == null){
			return "default";
		}
		return serviceType;
	}
}
