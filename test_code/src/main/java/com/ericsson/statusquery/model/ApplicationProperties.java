package com.ericsson.statusquery.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {
	
	@Value("${cassandra.ip.address.site1}")
	private String cassandraIpAddressSite1;

	@Value("${cassandra.datacenter.site1}")
	private String cassandraDatacenterSite1;

	@Value("${cassandra.keyspace.site1}")
	private String cassandraKeyspaceSite1;

	@Value("${cassandra.port.site1}")
	private String cassandraPortSite1;

	@Value("${cassandra.ip.address.site2}")
	private String cassandraIpAddressSite2;

	@Value("${cassandra.datacenter.site2}")
	private String cassandraDatacenterSite2;

	@Value("${cassandra.keyspace.site2}")
	private String cassandraKeyspaceSite2;

	@Value("${cassandra.port.site2}")
	private String cassandraPortSite2;

	@Value("${cassandra.no.of.replica}")
	private String cassandraNoOfReplica;
	
		
	public String getCassandraIpAddressSite1() {
		return cassandraIpAddressSite1;
	}

	public String getCassandraDatacenterSite1() {
		return cassandraDatacenterSite1;
	}

	public String getCassandraKeyspaceSite1() {
		return cassandraKeyspaceSite1;
	}

	public String getCassandraPortSite1() {
		return cassandraPortSite1;
	}

	public String getCassandraIpAddressSite2() {
		return cassandraIpAddressSite2;
	}

	public String getCassandraDatacenterSite2() {
		return cassandraDatacenterSite2;
	}

	public String getCassandraKeyspaceSite2() {
		return cassandraKeyspaceSite2;
	}

	public String getCassandraPortSite2() {
		return cassandraPortSite2;
	}

	public String getCassandraNoOfReplica() {
		return cassandraNoOfReplica;
	}
	
}
