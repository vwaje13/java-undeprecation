package com.ericsson.statusquery.service.impl;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final UserDataLoaderService userDataLoaderService;
    
    public AuthenticationService() {
		super();
		userDataLoaderService = new UserDataLoaderService();
	}
    
    public boolean authenticate(String username, String password) {
    	
    	logger.info("Fetching User Data for authentication");
    	try {
    		Map<String, String> userData = userDataLoaderService.getUserData();
            String storedPassword = userData.get(username);
            return storedPassword != null && storedPassword.equals(password);
    	} catch(Exception e) {
    		logger.info("Exception occurred in authentication...",e);
    		return false;
    	}
        
    }
    
}
