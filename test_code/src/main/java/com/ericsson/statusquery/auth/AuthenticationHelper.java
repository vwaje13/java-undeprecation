package com.ericsson.statusquery.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthenticationHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHelper.class);
	private static final Map<String, AuthenticationCache> AUTH_CACHE_MAP 
	= Collections.synchronizedMap(new HashMap<String, AuthenticationCache>());
	private AuthenticationHelper(){}

	public static boolean authenticate(final String username, final String password) throws Exception {

		AuthenticationCache existingCache = AUTH_CACHE_MAP.get(username);
		if(Objects.isNull(existingCache)){
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("Inside non-existingCache with userName: {}",username);
			}			
			authenticateAndCache(username, password);
		}else{
			LOGGER.debug("Validating with userName: {}",username);
			boolean isPassMatching = existingCache.getPassword().equals(password);
			if(!isPassMatching){
				LOGGER.debug("Incorrect password, authentication failed..");
				throw new StausqueryException(1005, "Authentication failed");
			}				
		}
		return true;
	}

	public static void authenticateAndCache(final String username, final String password) throws Exception {
		AuthenticationCache authCache = new AuthenticationCache(username, password, System.currentTimeMillis());

		if(!(Objects.isNull(authCache)))
			AUTH_CACHE_MAP.put(username, authCache);

	}
}
