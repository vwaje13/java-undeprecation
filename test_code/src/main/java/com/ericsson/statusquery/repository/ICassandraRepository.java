package com.ericsson.statusquery.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ICassandraRepository {
	
	void initializeDBProperties();
	
	boolean connectCassandraCluster(String ipAddress, int port, String dataCenter, String keySpace);
	
//	String connectDefaultDBSite();
	
	String connectDB();
	
	List<Map<String, Object>> getKafkaBrokerStatus();
	
	List<Map<String, Object>> getCassandraHealthStatus();
	
	List<Map<String, Object>> getNpeClusterHealthStatus();
	
	List<Map<String, Object>> getActiveAlarmsStatus();
	
	List<Map<String, Object>> getKAfkaProducerStatus();
	
	Set<String> getEdaNamespaces();
	
//	String getCommonPodStatusInNS(String namespace);
	
	List<Map<String, Object>> getIndividualTransactionStatus(String contractcode, String messageid,String msisdn,boolean detail);
	
	List<Map<String, Object>> getIndividualTransactionMSISDN(String contractcode, String messageid,String msisdn,boolean detail);
	
	List<String> getContractCodes(String msisdn);
	
	
	

}
