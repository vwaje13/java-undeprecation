package com.ericsson.statusquery.cassandra;

import java.util.Arrays;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.datastax.oss.driver.internal.core.ssl.DefaultSslEngineFactory;

import edu.umd.cs.findbugs.annotations.NonNull;

public class SNISslEngineFactory extends DefaultSslEngineFactory{
	
	public SNISslEngineFactory(DriverContext driverContext) {
	    super(driverContext);
	  }

	@Override
	  public SSLEngine newSslEngine(@NonNull EndPoint remoteEndpoint) {
	    SSLEngine engine = super.newSslEngine(remoteEndpoint);
	    SSLParameters sslParameters = engine.getSSLParameters();
	    sslParameters.setServerNames(Arrays.asList(new SNIHostName("external")));
	    engine.setSSLParameters(sslParameters);
	    return engine;
	  }
	
}
