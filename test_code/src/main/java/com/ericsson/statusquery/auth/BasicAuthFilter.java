package com.ericsson.statusquery.auth;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import com.ericsson.statusquery.constants.Constants;
import com.ericsson.statusquery.service.impl.AuthenticationService;

//@Component
public class BasicAuthFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(BasicAuthFilter.class);
	private AuthenticationService authService;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.info("Initializing BasicAuthFilter filter... ");
	  	this.authService = new AuthenticationService();
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;

		String authHeader = httpServletRequest.getHeader(Constants.AUTH_HEADER);
		
		if (authHeader == null || !authHeader.startsWith(Constants.AUTH_TYPE)) {
			httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, Constants.AUTH_ERROR);
			return;
		}

		String[] credentials = new String(Base64.getDecoder().decode(authHeader.substring(6))).split(":");
		String username = credentials[0];
		String password = credentials[1];

		logger.debug("Username: {}, Password: {}",username, password);     

		if (!(authService.authenticate(username, password))) {
			logger.info("Authentication Failed...");
			httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, Constants.AUTH_ERROR_1);
			return;
		}

		// Allow the request to proceed
		chain.doFilter(httpServletRequest, httpServletResponse);
	}
}
