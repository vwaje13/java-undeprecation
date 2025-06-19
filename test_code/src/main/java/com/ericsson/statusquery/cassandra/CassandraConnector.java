package com.ericsson.statusquery.cassandra;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.internal.core.auth.PlainTextAuthProvider;
import com.datastax.oss.driver.internal.core.ssl.DefaultSslEngineFactory;
import com.ericsson.statusquery.exception.StatusQueryServiceException;

@Component
public class CassandraConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(CassandraConnector.class);

	@Value("${username}")
	private String username;

	@Value("${truststorefile}")
	private String truststorefile;

	@Value("${truststorepass}")
	private String truststorepass;

	@Value("${keystorefile}")
	private String keystorefile;

	@Value("${keystorepass}")
	private String keystorepass;

	private CqlSession session;
	private boolean isUsingExternalCert;
	
	public CassandraConnector(boolean isUsingExternalCert) {
        this.isUsingExternalCert = isUsingExternalCert;
    }
	
	public CassandraConnector() {
        
    }

	public void connect(final String node, final Integer port, final String dataCenter) {
		CqlSessionBuilder builder = CqlSession.builder();
		builder.addContactPoint(new InetSocketAddress(node, port));

		builder.withLocalDatacenter(dataCenter);

		session = builder.build();
	}

	public CqlSession buildSession(String contactPoints, int port, String datacenter, String keyspace) {
		try {
			session = CqlSession.builder().addContactPoints(parseContactPoints(contactPoints, port))
					.withKeyspace(keyspace).withLocalDatacenter(datacenter).withConfigLoader(getConfigLoader()).build();
		} catch (Exception e) {
			if(LOGGER.isDebugEnabled())
			LOGGER.error("Exception building cassandra session - ", e);
			
			LOGGER.info(" DB is Down, unable to connect");
			
			throw new StatusQueryServiceException(" DB is Down, unable to connect", e);
		}

		return session;
	}

	private DriverConfigLoader getConfigLoader() {
		    String username = "wcdbcd_admin"; // get from application properties, inject as @Value
	        //String password = "CeYEzAtzLbOAgJDY";
	        String truststorefile = null;
	        String keystorefile = null;
	        if (isUsingExternalCert) {
	            LOGGER.info("use external cert");
	            truststorefile = "truststore_external.jks";
	            keystorefile = "keystore.p12"; // get from application properties, inject as @Value
	        } 
//	        else {
//	        	LOGGER.info("use siptls cert");
//	           // truststorefile = "truststore_siptls.jks";
//	        	truststorefile="kafkatruststore.jks";
//	        	keystorefile = "kafkakeystore.jks";
//	        }
	        String truststorepass = "pohpoh"; // get from application properties, inject as @Value	        
	        String keystorepass = "pohpoh"; // get from application properties, inject as @Value

		
		
	        Class<?> sslEngineFactory;
	        if (isUsingExternalCert) {
	            sslEngineFactory = SNISslEngineFactory.class;
	        } else {
	            sslEngineFactory = DefaultSslEngineFactory.class;
	        }
		
		
		ProgrammaticDriverConfigLoaderBuilder configBuilder = DriverConfigLoader.programmaticBuilder();
		configBuilder.withClass(DefaultDriverOption.AUTH_PROVIDER_CLASS, PlainTextAuthProvider.class)
				.withString(DefaultDriverOption.AUTH_PROVIDER_USER_NAME, username)
				.withString(DefaultDriverOption.AUTH_PROVIDER_PASSWORD, System.getenv("WCDBCD_ADMIN_PASSWORD"))
				//.withString(DefaultDriverOption.AUTH_PROVIDER_PASSWORD, password)
				//.withClass(DefaultDriverOption.SSL_ENGINE_FACTORY_CLASS, DefaultSslEngineFactory.class)
				.withBoolean(DefaultDriverOption.SSL_HOSTNAME_VALIDATION, false)
				.withString(DefaultDriverOption.SSL_TRUSTSTORE_PATH, truststorefile)
				.withString(DefaultDriverOption.SSL_TRUSTSTORE_PASSWORD, truststorepass)
				.withString(DefaultDriverOption.SSL_KEYSTORE_PATH, keystorefile)
				.withString(DefaultDriverOption.SSL_KEYSTORE_PASSWORD, keystorepass)
		        .withClass(DefaultDriverOption.SSL_ENGINE_FACTORY_CLASS, sslEngineFactory);

		return configBuilder.build();
	}

	private Collection<InetSocketAddress> parseContactPoints(String contactPointString, int port) {
		StringTokenizer tokenizer = new StringTokenizer(contactPointString, ";");
		List<InetSocketAddress> contactPoints = new ArrayList<>();
		while (tokenizer.hasMoreElements()) {
			String contactPoint = (String) tokenizer.nextElement();
			contactPoints.add(new InetSocketAddress(contactPoint, port));
		}
		return contactPoints;
	}

	public CqlSession getSession() {
		return this.session;
	}

	public void close() {
		session.close();
	}

}
