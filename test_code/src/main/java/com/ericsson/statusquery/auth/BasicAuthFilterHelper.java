package com.ericsson.statusquery.auth;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BasicAuthFilterHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthFilterHelper.class);

	public Optional<Boolean> getUserDetailsAndAuthenticate(final String header) throws RequestValidationException {
		Optional<Boolean> optionalSessionContext = Optional.empty();
		Optional<Map.Entry<String, String>> userPass = parseUserAndPassPairFromAuthHeader(header);
		if (userPass.isPresent()) {
			optionalSessionContext = userPass.map(e -> {
				try {
					return authenticate(e.getKey(), e.getValue()/* , reqCtx */);
				} catch (Exception ex) {
					LOGGER.info("Exception while basic authenticate username and password");
					throw new RuntimeException(ex);
				}
			}).orElse(optionalSessionContext);
		}
		return optionalSessionContext;

	}

	private Optional<Map.Entry<String, String>> parseUserAndPassPairFromAuthHeader(String header) {
		String userPass = decodeUsernamePassword(header);
		String[] authParts = userPass.split(":");
		if (2 == authParts.length) {
			return Optional.of(new AbstractMap.SimpleEntry<>(authParts[0], authParts[1]));
		} else {
			LOGGER.debug("Authentication Failed");
		}
		return Optional.empty();
	}

	private Optional<Boolean> authenticate(final String username, final String password) throws Exception {
		Optional<Boolean> loginStatus = Optional.empty();
		try {
			boolean authenticateResp = AuthenticationHelper.authenticate(username, password);
			if (authenticateResp) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Authentication Successfull");
				loginStatus = Optional.of(true);
			} else {
				LOGGER.info("Authentication Failed");
			}
		} catch (Exception e) {
			LOGGER.debug("Exception while checking Authentication", e);
		}
		return loginStatus;
	}

	private String decodeUsernamePassword(final String header) {
		String base64UserPass = header.trim().substring(SecurityContext.BASIC_AUTH.length());
		byte[] decoded = Base64.getDecoder().decode(base64UserPass.trim());
		return new String(decoded, StandardCharsets.UTF_8);
	}

	public boolean authenticateUser(final ContainerRequestContext reqCtx) throws Exception {
		String authHeader = reqCtx.getHeaderString(HttpHeaders.AUTHORIZATION);
		String decodedAuthString = decodeUsernamePassword(authHeader);
		String[] authParts = decodedAuthString.split(":");
		if (authParts.length == 2) {
			return AuthenticationHelper.authenticate(authParts[0], authParts[1]);
		}
		return false;
	}
}
