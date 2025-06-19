package com.ericsson.statusquery.auth;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ericsson.statusquery.constants.Constants;

@Configuration
public class BasicAuthFilterConfig {

	@Bean
	public FilterRegistrationBean<BasicAuthFilter> basicAuthFilter() {

		FilterRegistrationBean<BasicAuthFilter> filter = new FilterRegistrationBean<>();
		filter.setFilter(new BasicAuthFilter());


		// provide endpoints which needs to be restricted.
		// All Endpoints would be restricted if unspecified
		filter.addUrlPatterns(Constants.RESTRICTED_URI);
		return filter;
	}

}
