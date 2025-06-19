package com.ericsson.statusquery.repository.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.NoNodeAvailableException;
import com.datastax.oss.driver.api.core.connection.ConnectionInitException;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.ericsson.statusquery.alarm.AlarmHandler;
import com.ericsson.statusquery.cassandra.CassandraConnector;
import com.ericsson.statusquery.constants.AlarmInfo;
import com.ericsson.statusquery.constants.Constants;
import com.ericsson.statusquery.exception.StatusQueryServiceException;
import com.ericsson.statusquery.model.KubernetesProperties;
import com.ericsson.statusquery.repository.ICassandraRepository;
import com.ericsson.statusquery.utils.Utils;

@Component
@Import(KubernetesProperties.class)
@EnableScheduling
public class CassandraRepositoryImpl implements ICassandraRepository {

	private static final Logger logger = LoggerFactory.getLogger("CassandraRepositoryImpl");

	private CqlSession session/* = null */;
	private CassandraConnector connector = new CassandraConnector(true);

	// @Autowired
	// private ApplicationProperties property;

	@Autowired
	private KubernetesProperties property;

	@Autowired
	private Utils util;

	@Autowired
	AlarmHandler alarmHandler;

	private String cassandraIpAddressSiteA = "";
	private int cassandraPortSiteA = 0;
	private String cassandraDatacenterSiteA = "";
	private String cassandraKeySpaceSiteA = "";

	private String cassandraIpAddressSiteB = "";
	private int cassandraPortSiteB = 0;
	private String cassandraDatacenterSiteB = "";
	private String cassandraKeySpaceSiteB = "";

	private boolean isDBConnected;

	public String currentDbSite;
	private boolean schedulerEnabled = false;
	private boolean enableSchedulerNoneDBFailover = false;
	private boolean enableSchedulerFetchFailoverAfterNONE = false;

	public boolean isDBConnected() {
		return isDBConnected;
	}

	public void setDBConnected(boolean isDBConnected) {
		this.isDBConnected = isDBConnected;
	}

	public String getCurrentDbSite() {
		return currentDbSite;
	}

	public void setCurrentDbSite(String currentDbSite) {
		this.currentDbSite = currentDbSite;
	}

	@Override
	public boolean connectCassandraCluster(String ipAddress, int port, String dataCenter, String keySpace)
			throws StatusQueryServiceException {
		boolean isConnected = false;
		try {
			// For local environment
			logger.debug("Inside session builder method...");
			// For lab environment
			session = connector.buildSession(ipAddress, port, dataCenter, keySpace);

			if (session != null && !session.isClosed()) {
				logger.info("Connected to DB {}", ipAddress);
				setDBConnected(true);
				isConnected = true;
			}
			return isConnected;

		} catch (Exception e) {
			setDBConnected(false);
			if (logger.isDebugEnabled())
				logger.error("Exception Occured while connecting to DB", e.getMessage(), e);

			logger.info("Raising alarm for DB down site :: {}", ipAddress);
			// alarm when db down
			alarmHandler.raiseAlarm(AlarmInfo.FAULTNAME.DatabaseDown.name(), AlarmInfo.ALARM_SEVERITY.Critical.name(),
					"Cassandra DB site" + ipAddress + "is down");
			throw new StatusQueryServiceException(" DB is Down, unable to connect", e);
		}

	}

	@Override
	public String connectDB() throws StatusQueryServiceException {

		currentDbSite = Constants.DB_NOT_CONNECTED;

		initializeDBProperties();

		try {

			this.connectCassandraCluster(cassandraIpAddressSiteA, cassandraPortSiteA, cassandraDatacenterSiteA,
					cassandraKeySpaceSiteA);
			logger.info("Connected to Local DB");
			currentDbSite = Constants.DB_SITE_1;
			return currentDbSite;
		} catch (Exception ex) {
			if (logger.isDebugEnabled())
				logger.error("Not able to connect to Local DB", ex);
			currentDbSite = failOverLogic(currentDbSite);
			logger.info("Failed to connect DB");
			return currentDbSite;
		}
	}

	public String failOverLogic(String currentDbSite) throws StatusQueryServiceException {
		try {
			this.connectCassandraCluster(cassandraIpAddressSiteB, cassandraPortSiteB, cassandraDatacenterSiteB,
					cassandraKeySpaceSiteB);
			logger.info("inside failover logic {}", currentDbSite);
			logger.info("Connected to Remote DB");
			this.currentDbSite = Constants.DB_SITE_2;
			// alarmHandler.clear(AlarmInfo.FAULTNAME.producerDbConnectionDownOrLost.name(),
			// "Clear Cassandra Down Alarm"); //
			logger.info("Cassandra Down Alarm is cleared");
			return this.currentDbSite;
		} catch (Exception exe) {
			// throw new StatusQueryServiceException("Titan DB is Down, unable to connect",
			// exe);
			if (logger.isDebugEnabled())
				logger.error("Remote DB is Down, unable to connect", exe);
		}
		return currentDbSite;

	}

	@Override
	public void initializeDBProperties() {
		if (property.getCassandraIpAddressSite1() != null) {
			cassandraIpAddressSiteA = property.getCassandraIpAddressSite1();
		}

		if (property.getCassandraPortSite1() != null) {
			cassandraPortSiteA = Integer.parseInt(property.getCassandraPortSite1());
		}

		if (property.getCassandraDatacenterSite1() != null) {
			cassandraDatacenterSiteA = property.getCassandraDatacenterSite1();
		}

		if (property.getCassandraKeyspaceSite1() != null) {
			cassandraKeySpaceSiteA = property.getCassandraKeyspaceSite1();
		}

		if (property.getCassandraIpAddressSite2() != null) {
			cassandraIpAddressSiteB = property.getCassandraIpAddressSite2();
		}

		if (property.getCassandraPortSite2() != null) {
			cassandraPortSiteB = Integer.parseInt(property.getCassandraPortSite2());
		}

		if (property.getCassandraDatacenterSite2() != null) {
			cassandraDatacenterSiteB = property.getCassandraDatacenterSite2();
		}

		if (property.getCassandraKeyspaceSite2() != null) {
			cassandraKeySpaceSiteB = property.getCassandraKeyspaceSite2();
		}

		logger.debug("Ip Address SIte A :: {}", cassandraIpAddressSiteA);
		logger.debug("Port no Site A :: {}", cassandraPortSiteA);
		logger.debug("Data Center Site A :: {}", cassandraDatacenterSiteA);
		logger.debug("Key Space Site A {}::", cassandraKeySpaceSiteA);

		logger.debug("Ip Address SIte B :: {}", cassandraIpAddressSiteB);
		logger.debug("Port no Site B :: {}", cassandraPortSiteB);
		logger.debug("Data Center Site B :: {}", cassandraDatacenterSiteB);
		logger.debug("Key Space Site B :: {}", cassandraKeySpaceSiteB);

	}

	@Override
	public List<Map<String, Object>> getKafkaBrokerStatus() {
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		List<String> namespaceList = new ArrayList<>();
		try {
			namespaceList.add(property.getKafkaNamespacePolarisSite());
			namespaceList.add(property.getKafkaNamespaceTitanSite());

			for (String namespace : namespaceList) {
				String query = Constants.GET_KAFKA_BROKER_STATUS.replace("KEYSPACE", cassandraKeySpaceSiteA);
				PreparedStatement preparedStatement = session.prepare(query);
				BoundStatement boundStatement = preparedStatement.bind(namespace);
				ResultSet resultSet = session.execute(boundStatement);

				if (resultSet != null) {
					for (Row row : resultSet) {
						Map<String, Object> rowMap = new LinkedHashMap<>();
						rowMap.put(Constants.NAMESPACE, row.getString(Constants.NAMESPACE));
						rowMap.put(Constants.BROKER_NAME, row.getString(Constants.BROKER_NAME));
						rowMap.put(Constants.IP_LB, row.getString(Constants.IP_LB));
						rowMap.put(Constants.STATUS, row.getInt(Constants.STATUS));

						resultSetList.add(rowMap);
					}
				}
			}
			logger.debug("Data retrieved successfully from tab_kafka_broker_status");
			return resultSetList;

		} catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return resultSetList;
	}

	@Override
	public List<Map<String, Object>> getCassandraHealthStatus() {
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		List<String> namespaceList = new ArrayList<>();
		try {
			namespaceList.add(property.getCassandraNamespacePolarisSite());
			namespaceList.add(property.getCassandraNamespaceTitanSite());

			for (String namespace : namespaceList) {
				String query = Constants.GET_CASSANDRA_HEALTH_STATUS.replace("KEYSPACE", cassandraKeySpaceSiteA);
				PreparedStatement preparedStatement = session.prepare(query);
				BoundStatement boundStatement = preparedStatement.bind(namespace);
				ResultSet resultSet = session.execute(boundStatement);

				if (resultSet != null) {
					for (Row row : resultSet) {
						Map<String, Object> rowMap = new LinkedHashMap<>();
						rowMap.put(Constants.NAMESPACE, row.getString(Constants.NAMESPACE));
						rowMap.put(Constants.CASSANDRA_INSTANCE, row.getString(Constants.CASSANDRA_INSTANCE));
						rowMap.put(Constants.IP_LB, row.getString(Constants.IP_LB));
						rowMap.put(Constants.STATUS, row.getInt(Constants.STATUS));

						resultSetList.add(rowMap);
					}
				}
			}
			logger.debug("Data retrieved successfully from tab_cassandra_health_status");
			return resultSetList;
		} catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return resultSetList;
	}

	@Override
	public List<Map<String, Object>> getNpeClusterHealthStatus() {
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		Set<String> namespaceList = new HashSet<>();
		try {
			namespaceList = this.getEdaNamespaces();

			for (String namespace : namespaceList) {
				String query = Constants.GET_NPECLUSTER_HEALTH_STATUS.replace("KEYSPACE", cassandraKeySpaceSiteA);
				PreparedStatement preparedStatement = session.prepare(query);
				BoundStatement boundStatement = preparedStatement.bind(namespace);
				ResultSet resultSet = session.execute(boundStatement);

				if (resultSet != null) {
					for (Row row : resultSet) {
						Map<String, Object> rowMap = new LinkedHashMap<>();
						rowMap.put(Constants.NAMESPACE, row.getString(Constants.NAMESPACE));
						rowMap.put(Constants.STATUS_DATA, row.getString(Constants.STATUS_DATA));

						resultSetList.add(rowMap);
					}
				}
			}
			logger.debug("Data retrieved successfully from tab_npecluster_health_status");
			return resultSetList;
		} catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return resultSetList;
	}

	@Override
	public List<Map<String, Object>> getActiveAlarmsStatus() {
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		Set<String> namespaceList = new HashSet<>();
		try {
			namespaceList = this.getEdaNamespaces();
			namespaceList.add(property.getCassandraNamespacePolarisSite());
			namespaceList.add(property.getCassandraNamespaceTitanSite());
			namespaceList.add(property.getKafkaNamespacePolarisSite());
			namespaceList.add(property.getKafkaNamespaceTitanSite());

			for (String namespace : namespaceList) {
				String query = Constants.GET_ACTIVE_ALARMS_STATUS.replace("KEYSPACE", cassandraKeySpaceSiteA);
				PreparedStatement preparedStatement = session.prepare(query);
				BoundStatement boundStatement = preparedStatement.bind(namespace);
				ResultSet resultSet = session.execute(boundStatement);

				if (resultSet != null) {
					for (Row row : resultSet) {
						Map<String, Object> rowMap = new LinkedHashMap<>();
						rowMap.put(Constants.NAMESPACE, row.getString(Constants.NAMESPACE));
//						logger.info("Data received from cassandra for query is: {}", row.getString(Constants.DATA));
						//String data = util.convertTimestampInJson(row.getString(Constants.DATA));
						String dataStr = row.getString(Constants.DATA);
					    Object data = util.convertTimestampInJson(dataStr);
//						logger.info("Data received from the util after conversion is: {}", data);
						rowMap.put(Constants.DATA, data);
						resultSetList.add(rowMap);
					}
				}
			}

			logger.debug("Data retrieved successfully from tab_active_alarms");
			return resultSetList;
		} catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return resultSetList;
	}

	@Override
	public List<Map<String, Object>> getKAfkaProducerStatus() {
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		Set<String> namespaceList = new HashSet<>();
		try {
			namespaceList.add(property.getCassandraNamespacePolarisSite());
			namespaceList.add(property.getCassandraNamespaceTitanSite());
			namespaceList.add(property.getKafkaNamespacePolarisSite());
			namespaceList.add(property.getKafkaNamespaceTitanSite());

			for (String namespace : namespaceList) {
				String query = Constants.GET_KAFKA_PRODUCER_STATUS.replace("KEYSPACE", cassandraKeySpaceSiteA);
				PreparedStatement preparedStatement = session.prepare(query);
				BoundStatement boundStatement = preparedStatement.bind(namespace);
				ResultSet resultSet = session.execute(boundStatement);

				if (resultSet != null) {
					for (Row row : resultSet) {
						Map<String, Object> rowMap = new LinkedHashMap<>();
						rowMap.put(Constants.NAMESPACE, row.getString(Constants.NAMESPACE));
						rowMap.put(Constants.PRODUCER_NAME, row.getString(Constants.PRODUCER_NAME));
						rowMap.put(Constants.RUNNING_VERSION, row.getString(Constants.RUNNING_VERSION));
						rowMap.put(Constants.DB_VERSION, row.getString(Constants.DB_VERSION));
						rowMap.put(Constants.STATUS, Constants.PRODUCER_STATUS_VALUE);
						// rowMap.put(Constants.STATUS,
						// this.getCommonPodStatusInNS(row.getString(Constants.NAMESPACE)));
						resultSetList.add(rowMap);
					}
				}
			}
			logger.debug("Data retrieved successfully from tab_producer_health_status");
			return resultSetList;
		} catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return resultSetList;
	}

	@Override
	public Set<String> getEdaNamespaces() {
		Set<String> namespaceList = new HashSet<>();

		try {
			String query = Constants.GET_EDA_NAMESPACES.replace("KEYSPACE", cassandraKeySpaceSiteA);
			PreparedStatement preparedStatement = session.prepare(query);
			BoundStatement boundStatement = preparedStatement.bind();
			ResultSet resultSet = session.execute(boundStatement);

			if (resultSet != null) {
				for (Row row : resultSet) {
					namespaceList.add(row.getString("npeclusters_id"));
				}
			}
			logger.debug("Data retrieved successfully from tab_npeclusters_conf");
		} catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return namespaceList;
	}

//	@Override
//	public List<Map<String, Object>> getIndividualTransactionStatus(String contractcode, String messageid,
//			String msisdn, boolean detail) {
//		List<Map<String, Object>> resultSetList = new ArrayList<>();
//		Map<String, List<Map<String, Object>>> reprovMap = new LinkedHashMap<>();
//		Map<String, Map<String, Object>> parentMap = new LinkedHashMap<>();
//		Map<String, String> finalStatusMap = new LinkedHashMap<>();
//		ResultSet resultSet = null;
//
//		try {
//			if (messageid != null && !messageid.endsWith("*")) {
//				String query = Constants.GET_COMMON_POH_TRANSACTION_STATUS_WITH_MESSAGEID_SUBID.replace("KEYSPACE",
//						cassandraKeySpaceSiteA);
//				PreparedStatement preparedStatement = session.prepare(query);
//				BoundStatement boundStatement = preparedStatement.bind(contractcode, messageid);
//				resultSet = session.execute(boundStatement);
//
//			} else {
//				if (contractcode != null) {
//					String query = Constants.GET_COMMON_POH_TRANSACTION_STATUS_SUBID.replace("KEYSPACE",
//							cassandraKeySpaceSiteA);
//					PreparedStatement preparedStatement = session.prepare(query);
//					BoundStatement boundStatement = preparedStatement.bind(contractcode);
//					resultSet = session.execute(boundStatement);
//				}
//
//			}
//
//			if (resultSet != null) {
//				
//				//Testing ravi
//				for (Row row : resultSet) {
//		        	
//	                Map<String, Object> rowMap = mapRowToResult(row, detail);
//	                resultSetList.add(rowMap);
//	        }
//	        logger.info("testing map list : {}",resultSetList);
//				//end
//
//				if (messageid != null && messageid.endsWith("*")) {
//					logger.info("messageid is ending with *");
//					for (Row row : resultSet) {
//
//						if (row.getString(Constants.MESSAGE_ID)
//								.contains(messageid.substring(0, messageid.length() - 1))) {
//							if (logger.isDebugEnabled()) {
//								logger.debug("contains msgid : {}", messageid.substring(0, messageid.length() - 1));
//							}
//
//							String messageId = row.getString(Constants.MESSAGE_ID);
//
//							if (messageId.contains("REPROV")) {
//								logger.info("Reprov msgids are present");
//								String parentId = messageId.substring(0, messageId.indexOf("-REPROV"));
//								List<Map<String, Object>> reprovList = reprovMap.get(parentId);
//								if (reprovList == null) {
//									reprovList = new ArrayList<Map<String, Object>>();
//									reprovMap.put(parentId, reprovList);
//								}
//								Map<String, Object> reprovRowMap = new LinkedHashMap<>();
//								reprovRowMap.put(Constants.MESSAGE_ID, messageId);
//								reprovRowMap.put(Constants.MSISDN, row.getString(Constants.MSISDN));
//								reprovRowMap.put(Constants.PRODUCER, row.getString(Constants.PRODUCER));
//								reprovRowMap.put(Constants.REGISTRATION_TIMESTAMP,
//										util.readableTimestamp(row.getString(Constants.REGISTRATION_TIMESTAMP)));
//								reprovRowMap.put(Constants.TOPIC, row.getString(Constants.TOPIC));
//								reprovRowMap.put(Constants.CONSUMER_ID, row.getString(Constants.CONSUMER_ID));
//								reprovRowMap.put(Constants.CONSUMER_TIMESTAMP_NORMAL,
//										util.readableTimestamp(row.getString(Constants.CONSUMER_TIMESTAMP_NORMAL)));
//								reprovRowMap.put(Constants.LOG_ROOTID_CONSUME_NORMAL,
//										row.getString(Constants.LOG_ROOTID_CONSUME_NORMAL));
//								if (row.getString(Constants.INTERIM_NOTIFY_TIMESTAMP) != null) {
//									reprovRowMap.put(Constants.INTERIM_NOTIFY_TIMESTAMP,
//											util.readableTimestamp(row.getString(Constants.INTERIM_NOTIFY_TIMESTAMP)));
//								} else {
//									reprovRowMap.put(Constants.INTERIM_NOTIFY_TIMESTAMP, Constants.INTERIM_NOT_TRIGGER);
//								}
//
//								reprovRowMap.put(Constants.FINAL_NOTIFY_TIMESTAMP,
//										util.readableTimestamp(row.getString(Constants.FINAL_NOTIFY_TIMESTAMP)));
//								reprovRowMap.put(Constants.STATUS, row.getString(Constants.STATUS));
//								reprovRowMap.put(Constants.ACTION, row.getString(Constants.ACTION));
//								reprovRowMap.put(Constants.FLUSH_INDICATOR, row.getString(Constants.FLUSH_INDICATOR));
//								reprovRowMap.put(Constants.FLUSH_MESSAGEID, row.getString(Constants.FLUSH_MESSAGEID));
//								if (detail) {
//									reprovRowMap.put(Constants.DETAILS, row.getString(Constants.ERROR_DETAILS));
//								}
//								reprovList.add(reprovRowMap);
//							} else {
//								Map<String, Object> rowMap = new LinkedHashMap<>();
//								rowMap.put(Constants.MESSAGE_ID, messageId);
//								rowMap.put(Constants.MSISDN, row.getString(Constants.MSISDN));
//								rowMap.put(Constants.PRODUCER, row.getString(Constants.PRODUCER));
//								rowMap.put(Constants.REGISTRATION_TIMESTAMP,
//										util.readableTimestamp(row.getString(Constants.REGISTRATION_TIMESTAMP)));
//								rowMap.put(Constants.TOPIC, row.getString(Constants.TOPIC));
//								rowMap.put(Constants.CONSUMER_ID, row.getString(Constants.CONSUMER_ID));
//								rowMap.put(Constants.CONSUMER_TIMESTAMP_NORMAL,
//										util.readableTimestamp(row.getString(Constants.CONSUMER_TIMESTAMP_NORMAL)));
//								rowMap.put(Constants.LOG_ROOTID_CONSUME_NORMAL,
//										row.getString(Constants.LOG_ROOTID_CONSUME_NORMAL));
//								if (row.getString(Constants.INTERIM_NOTIFY_TIMESTAMP) != null) {
//									rowMap.put(Constants.INTERIM_NOTIFY_TIMESTAMP,
//											util.readableTimestamp(row.getString(Constants.INTERIM_NOTIFY_TIMESTAMP)));
//								} else {
//									rowMap.put(Constants.INTERIM_NOTIFY_TIMESTAMP, Constants.INTERIM_NOT_TRIGGER);
//								}
//
//								rowMap.put(Constants.FINAL_NOTIFY_TIMESTAMP,
//										util.readableTimestamp(row.getString(Constants.FINAL_NOTIFY_TIMESTAMP)));
//								rowMap.put(Constants.STATUS, row.getString(Constants.STATUS));
//								rowMap.put(Constants.ACTION, row.getString(Constants.ACTION));
//								rowMap.put(Constants.FLUSH_INDICATOR, row.getString(Constants.FLUSH_INDICATOR));
//								rowMap.put(Constants.FLUSH_MESSAGEID, row.getString(Constants.FLUSH_MESSAGEID));
//								if (detail) {
//									rowMap.put(Constants.DETAILS, row.getString(Constants.ERROR_DETAILS));
//								}
//								parentMap.put(messageId, rowMap);
//							}
//						}
//					}
//					// for final status
//					for (String parentId : reprovMap.keySet()) {
//						List<Map<String, Object>> reprovList = reprovMap.get(parentId);
//						boolean allHaveTimestamp = true;
//						// boolean allFinalized = true;
//						String lastStatus = null;
//
//						for (Map<String, Object> reprov : reprovList) {
//							String finalNotifyTimestamp = (String) reprov.get(Constants.FINAL_NOTIFY_TIMESTAMP);
//							if (finalNotifyTimestamp == null || finalNotifyTimestamp.isEmpty()) {
//								allHaveTimestamp = false;
//							}
//							lastStatus = (String) reprov.get(Constants.STATUS);
//						}
//
//						String finalStatus;
//						if (allHaveTimestamp) {
//							finalStatus = lastStatus != null && lastStatus.equalsIgnoreCase(Constants.STATUS_COMPLETED)
//									? Constants.STATUS_SUCCESS
//									: Constants.STATUS_FAILED;
//						} else {
//							finalStatus = Constants.FINAL_STATUS_ONGOING;
//						}
//						logger.info("finalStatus : {}",finalStatus);
//						finalStatusMap.put(parentId, finalStatus);
//					}
//
//					for (Map.Entry<String, Map<String, Object>> entry : parentMap.entrySet()) {
//						String parentId = entry.getKey();
//						Map<String, Object> parentMessage = entry.getValue();
//						parentMessage.put(Constants.FINAL_STATUS, finalStatusMap.get(parentId));
//						logger.info("parentMessage :{}",parentMessage);
//						resultSetList.add(parentMessage);
//						if (reprovMap.containsKey(parentId)) {
//							Map<String, Object> reprovWrapper = new LinkedHashMap<>();
//							reprovWrapper.put(Constants.REPROVISIONING_REQUESTS, reprovMap.get(parentId));
//							resultSetList.add(reprovWrapper);
//						}
//					}
//
//					logger.info("Data retrieved successfully from tab_common_poh");
//					return resultSetList;
//
//				}
//
//				else {
//					logger.info("query doesn't contain *");
//					for (Row row : resultSet) {
//						Map<String, Object> rowMap = new LinkedHashMap<>();
//						rowMap.put(Constants.MESSAGE_ID, row.getString(Constants.MESSAGE_ID));
//						rowMap.put(Constants.MSISDN, row.getString(Constants.MSISDN));
//						rowMap.put(Constants.PRODUCER, row.getString(Constants.PRODUCER));
//						rowMap.put(Constants.REGISTRATION_TIMESTAMP,
//								util.readableTimestamp(row.getString(Constants.REGISTRATION_TIMESTAMP)));
//						rowMap.put(Constants.TOPIC, row.getString(Constants.TOPIC));
//						rowMap.put(Constants.CONSUMER_ID, row.getString(Constants.CONSUMER_ID));
//						rowMap.put(Constants.CONSUMER_TIMESTAMP_NORMAL,
//								util.readableTimestamp(row.getString(Constants.CONSUMER_TIMESTAMP_NORMAL)));
//						rowMap.put(Constants.LOG_ROOTID_CONSUME_NORMAL,
//								row.getString(Constants.LOG_ROOTID_CONSUME_NORMAL));
//						if (row.getString(Constants.INTERIM_NOTIFY_TIMESTAMP) != null) {
//							rowMap.put(Constants.INTERIM_NOTIFY_TIMESTAMP,
//									util.readableTimestamp(row.getString(Constants.INTERIM_NOTIFY_TIMESTAMP)));
//						} else {
//							rowMap.put(Constants.INTERIM_NOTIFY_TIMESTAMP, Constants.INTERIM_NOT_TRIGGER);
//						}
//
//						rowMap.put(Constants.FINAL_NOTIFY_TIMESTAMP,
//								util.readableTimestamp(row.getString(Constants.FINAL_NOTIFY_TIMESTAMP)));
//						rowMap.put(Constants.STATUS, row.getString(Constants.STATUS));
//						rowMap.put(Constants.ACTION, row.getString(Constants.ACTION));
//						rowMap.put(Constants.FLUSH_INDICATOR, row.getString(Constants.FLUSH_INDICATOR));
//						rowMap.put(Constants.FLUSH_MESSAGEID, row.getString(Constants.FLUSH_MESSAGEID));
//						if (detail) {
//							rowMap.put(Constants.DETAILS, row.getString(Constants.ERROR_DETAILS));
//						}
//						resultSetList.add(rowMap);
//					}
//
//					logger.info("Data retrieved successfully from tab_common_poh");
//					return resultSetList;
//				}
//
//			} else {
//				logger.info("No data exist for given contract code");
//				return resultSetList;
//			}
//		} catch (ConnectionInitException | NoNodeAvailableException e) {
//			logger.info("Raising alarm as Cassandra server is down");
//			failOverLogic(currentDbSite);
//			enableCassandraAutoRecoverScheduler();
//		}
//		return resultSetList;
//	}
	
	@Override
	public List<Map<String, Object>> getIndividualTransactionStatus(String contractcode, String messageid,String msisdn, boolean detail){
		
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		List<Map<String, Object>> groupedList = new ArrayList<>();
		ResultSet resultSet = null;
		
		try {
			String query = Constants.GET_COMMON_POH_TRANSACTION_STATUS_SUBID.replace("KEYSPACE",cassandraKeySpaceSiteA);
			PreparedStatement preparedStatement = session.prepare(query);
			BoundStatement boundStatement = preparedStatement.bind(contractcode);
			resultSet = session.execute(boundStatement);
			
			if (resultSet != null) {
				for (Row row : resultSet) {
	                Map<String, Object> rowMap = mapRowToResult(row, detail);
	                resultSetList.add(rowMap);
				}
			}else {
				logger.info("No data exist for given contract code");
				return groupedList;
			}
			logger.debug("list : {}",resultSetList);
			resultSetList.sort(Comparator.comparing(m -> Long.parseLong((String) m.get("writetime(registrationtimestamp)"))));
			logger.info("resultset list is sorted on the basis of writetime(registrationTimestamp)");
			groupedList = getGroupedList(resultSetList, messageid,detail);
		}catch (ConnectionInitException | NoNodeAvailableException  e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return groupedList;
		
	}
	
    public static List<Map<String, Object>> getGroupedList(List<Map<String, Object>> resultSetList, String messageid, boolean detail){
    	
    	Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
    	List<Map<String, Object>> groupedResultList = new ArrayList<>();
    	try {
        	for (Map<String, Object> row : resultSetList) {
                String originalMessageId = (String) row.get("originalmessageid");

                // If the key is not present, initialize a new list
                if (!groupedData.containsKey(originalMessageId)) {
                    groupedData.put(originalMessageId, new ArrayList<Map<String, Object>>());
                }

                // Add the row to the appropriate list
                groupedData.get(originalMessageId).add(row);
            }
        	
        	logger.info("found {} original messagid for given contractcode ", groupedData.size());
            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
            	
            	String parentId = entry.getKey();
            	if(messageid != null) {
                	if(messageid.equalsIgnoreCase(parentId)) {
                		logger.info("queried msgid matched with originalmsgid");
                		Map<String, Object> parentMap = getParentMap(entry,detail);
                		groupedResultList.add(parentMap);
                		return groupedResultList;
                	}
                	else if(messageid.endsWith("*")) {
                		List<Map<String, Object>> childRequests = entry.getValue();
                		for(Map<String, Object> childMap : childRequests) {
                			String internalMsgId = (String) childMap.get("messageid");
                			if(internalMsgId.contains(messageid.substring(0, messageid.length() - 1))) {
                				logger.info("internal msgid : {} contains queried msgid, adding to the grouped list of wildcard",internalMsgId);
                				Map<String, Object> parentMap = getParentMap(entry,detail);
                        		groupedResultList.add(parentMap);
                        		break;
                			}
                		}
                	}
                	else {
                		List<Map<String, Object>> childRequests = entry.getValue();
                		for(Map<String, Object> childMap : childRequests) {
                			String internalMsgId = (String) childMap.get("messageid");
                			if(internalMsgId.equalsIgnoreCase(messageid)) {
                				logger.info("queried msgid matched with internal msgid");
                				Map<String, Object> parentMap = getParentMap(entry,detail);
                        		groupedResultList.add(parentMap);
                        		//defect-8712 for internal msgid 
                        		if(detail) {
                        			for (Map<String, Object> entryInternal : groupedResultList) {
                                        List<Map<String, Object>> filteredRequests = new ArrayList<>();

                                        List<Map<String, Object>> requestsList = (List<Map<String, Object>>) entryInternal.get("requests");
                                        for (Map<String, Object> req : requestsList) {
                                            if (messageid.equals(req.get("messageid"))) {
                                                filteredRequests.add(req);
                                            }
                                        }

                                        // Update the entry with filtered requests
                                        entryInternal.put("requests", filteredRequests);
                                    }
                        		}
                        		//end
                                logger.info("returning filtered internal msgid requests only");
                        		return groupedResultList;
                			}
                			else {
//                				System.out.println("queried msgid is not valid");
                			}
                		}
                	}
                }
            	else {
            		Map<String, Object> parentMap = getParentMap(entry,detail);
            		groupedResultList.add(parentMap);
            	}
                
            }
    	}catch(Exception e) {
    		throw new StatusQueryServiceException("exception in getGroupedList", e);
    	}

		return groupedResultList;      
    }
    
    public static Map<String, Object> getParentMap(Map.Entry<String, List<Map<String, Object>>> entry, boolean detail){
    	Map<String, Object> parentMap = new LinkedHashMap<>();
    	
    	try {
            String parentId = entry.getKey();
            logger.info("Fetching details for original messageid : {}", parentId);
            List<Map<String, Object>> childRequests = entry.getValue();
            List<String> parentIdDetails = getParentIdDetails(childRequests);
            
            parentMap.put("originalmessageid", parentId);
            parentMap.put("msisdn", parentIdDetails.get(0));
            parentMap.put("operation", parentIdDetails.get(1));
            if(!detail) {
            	parentMap.put("registrationtimestamp", parentIdDetails.get(6));
            }
            parentMap.put("CoreStaus", parentIdDetails.get(2));
            parentMap.put("NonCoreStatus", parentIdDetails.get(3));
            parentMap.put("OverallStatus", parentIdDetails.get(4));
            if(!"".equals(parentIdDetails.get(5))) {
        		parentMap.put("errordetails", parentIdDetails.get(5));
            }
            if(detail) {
            	parentMap.put("requests", cleanChildRequests(entry.getValue()));
            }
            
    	}
    	catch(Exception e) {
    		throw new StatusQueryServiceException("exception in getParentMap", e);
    	}
		return parentMap;
    }
    
    public static List<String> getParentIdDetails(List<Map<String, Object>> childRequests) {
    	List<String> parentDetails = new ArrayList<>();
    	try {
    		Map<String, Object> map = childRequests.get(childRequests.size()-1);
        	
        	String msidn = (String) map.get("msisdn");
        	String operation = (String) map.get("operation");
        	String status = (String) map.get("status");
        	String interimts = (String) map.get("interimnotifytimestamp");
        	String registrationts = (String) map.get("registrationtimestamp");
        	String overAllStatus = "";
        	String nonCoreStatus = "";
        	String coreStatus = "";
        	String errordetails = (String) map.get("errordetails");
        	//EDA2NPE-8898
        	if (errordetails == null) {
        	    errordetails = "";
        	}
        	
        	logger.info("Computing details for status : {} on the basis of latest internal messageid : {}" , status, (String) map.get("messageid"));
        	
        	if("reject".equalsIgnoreCase(status)) {
        		coreStatus= "";
        		nonCoreStatus= "";
        		overAllStatus = "rejected";
        	}
        	else if("produced".equalsIgnoreCase(status)) {
        		coreStatus= "";
        		nonCoreStatus= "";
        		overAllStatus = "produced";
        	}
        	else if("In-Progress".equalsIgnoreCase(status)) {
        		coreStatus= "in-progress";
        		nonCoreStatus= "";
        		overAllStatus = "in-progress";
        	}
        	else if("completed".equalsIgnoreCase(status)) {
        		coreStatus= "completed";
        		nonCoreStatus= "completed";
        		overAllStatus = "completed";
        	}
        	else if("Final Failure".equalsIgnoreCase(status) && errordetails.matches(".*\\b(PGW|CS|NPC)\\b.*") && "interimNotification Not Trigger".equalsIgnoreCase(interimts)) {
        		coreStatus= "failure";
        		nonCoreStatus= "";
        		overAllStatus = "failure";
        	}
        	else if("Final Failure".equalsIgnoreCase(status) && !(errordetails.matches(".*\\b(PGW|CS|NPC)\\b.*")) && interimts != null) {
        		coreStatus= "completed";
        		nonCoreStatus= "partial-failure";
        		overAllStatus = "partial-success";
        	}
        	else if("Core Success".equalsIgnoreCase(status) || !StringUtils.isBlank(interimts)) {
        		coreStatus= "completed";
        		nonCoreStatus= "in-progress";
        		overAllStatus = "in-progress";
        	}
        	else {
        		logger.info("status not matched");
        	}
        	
        	if(!("completed").equalsIgnoreCase(status) || !("in-progress").equalsIgnoreCase(status)) {
        		errordetails = (String) map.get("errordetails");
        	}
        	else{
        		errordetails = "";
        	}
        	
        	parentDetails.add(msidn);
        	parentDetails.add(operation);
        	parentDetails.add(coreStatus);
        	parentDetails.add(nonCoreStatus);
        	parentDetails.add(overAllStatus);
        	parentDetails.add(errordetails);
        	parentDetails.add(registrationts);
        	
        	logger.info("parent details : {} ", parentDetails);
        	
    	}
    	catch(Exception e) {
    		throw new StatusQueryServiceException("exception in getParentMap", e);
    	}
    	return parentDetails;
    }

	private Map<String, Object> mapRowToResult(Row row, boolean detail) {
		Map<String, Object> rowMap = new LinkedHashMap<>();
		
		try {
			rowMap.put(Constants.CONTRACT_CODE,row.getString(Constants.CONTRACT_CODE));
		    rowMap.put(Constants.MESSAGE_ID, row.getString(Constants.MESSAGE_ID));
		    rowMap.put(Constants.ACTION, row.getString(Constants.ACTION));
		    rowMap.put(Constants.CONSUMER_ID, row.getString(Constants.CONSUMER_ID));
		    rowMap.put(Constants.CONSUMER_TIMESTAMP_NORMAL, util.readableTimestamp(row.getString(Constants.CONSUMER_TIMESTAMP_NORMAL)));
		    rowMap.put(Constants.CONSUME_TIMESTAMP_REPLY, util.readableTimestamp(row.getString(Constants.CONSUME_TIMESTAMP_REPLY)));
		    rowMap.put(Constants.ERROR_DETAILS, row.getString(Constants.ERROR_DETAILS));
		    rowMap.put(Constants.FINAL_NOTIFY_TIMESTAMP, util.readableTimestamp(row.getString(Constants.FINAL_NOTIFY_TIMESTAMP)));
		    rowMap.put(Constants.FLUSH_INDICATOR, row.getString(Constants.FLUSH_INDICATOR));
		    rowMap.put(Constants.FLUSH_MESSAGEID, row.getString(Constants.FLUSH_MESSAGEID));
		    if (row.getString(Constants.INTERIM_NOTIFY_TIMESTAMP) != null) {
		        rowMap.put(Constants.INTERIM_NOTIFY_TIMESTAMP, util.readableTimestamp(row.getString(Constants.INTERIM_NOTIFY_TIMESTAMP)));
		    } else {
		        rowMap.put(Constants.INTERIM_NOTIFY_TIMESTAMP, Constants.INTERIM_NOT_TRIGGER);
		    }
		    rowMap.put(Constants.LOG_ROOTID_CONSUME_NORMAL,row.getString(Constants.LOG_ROOTID_CONSUME_NORMAL));
		    rowMap.put(Constants.LOG_ROOTID_CONSUME_REPLY,row.getString(Constants.LOG_ROOTID_CONSUME_REPLY));
		    rowMap.put(Constants.MSISDN, row.getString(Constants.MSISDN));
		    rowMap.put(Constants.PRODUCER, row.getString(Constants.PRODUCER));
		    rowMap.put(Constants.REGISTRATION_TIMESTAMP, util.readableTimestamp(row.getString(Constants.REGISTRATION_TIMESTAMP)));
		    rowMap.put(Constants.REPLAY_CONSUMER_ID,row.getString(Constants.REPLAY_CONSUMER_ID));
		    rowMap.put(Constants.REPROV_REPLAY_TRIGGER_TIMESTAMP,row.getString(Constants.REPROV_REPLAY_TRIGGER_TIMESTAMP));
//		    rowMap.put(Constants.ROOT_LOG_ID,row.getString(Constants.ROOT_LOG_ID));
		    rowMap.put(Constants.SCOPE,row.getString(Constants.SCOPE));
		    rowMap.put(Constants.STATUS, row.getString(Constants.STATUS));
		    rowMap.put(Constants.TOPIC, row.getString(Constants.TOPIC));
		    rowMap.put(Constants.TOPIC_PARTITION, row.getString(Constants.TOPIC_PARTITION));
		    rowMap.put(Constants.ORIGINAL_MESSAGE_ID, row.getString(Constants.ORIGINAL_MESSAGE_ID));
//		    rowMap.put(Constants.OPERATION, row.getString(Constants.OPERATION));
//		    rowMap.put(Constants.REPROVISIONING_CONSUMER_ID, row.getString(Constants.REPROVISIONING_CONSUMER_ID));
//		    rowMap.put(Constants.CONSUME_TIMESTAMP_REPROVISIONING, row.getString(Constants.CONSUME_TIMESTAMP_REPROVISIONING));
//		    rowMap.put(Constants.LOG_ROOT_ID_CONSUMER_REPROVISIONING, row.getString(Constants.LOG_ROOT_ID_CONSUMER_REPROVISIONING));
		    rowMap.put("writetime(registrationtimestamp)", String.valueOf(row.getLong("writetime(registrationtimestamp)")));
		    return rowMap;
		}catch (Exception e) {
			logger.error("exception in mapRowToResult()");
		}
		return rowMap;
	}
	
    public static List<Map<String, Object>> cleanChildRequests(List<Map<String, Object>> childRequests) {
        for (Map<String, Object> request : childRequests) {
            request.remove("writetime(registrationtimestamp)");
        }
        return childRequests;
    }

	@Override
	public List<String> getContractCodes(String msisdn) {
		List<String> msisdnList = new ArrayList<>();
		try {
			String query = Constants.GET_CONTRACTCODE_FOR_MSISDN.replace("KEYSPACE", cassandraKeySpaceSiteA);
			PreparedStatement preparedStatement = session.prepare(query);
			BoundStatement boundStatement = preparedStatement.bind(msisdn);
			ResultSet resultSet = session.execute(boundStatement);

			if (resultSet.iterator().hasNext()) {
				for (Row row : resultSet) {
					msisdnList.add(row.getString(Constants.CONTRACT_CODE));
				}
				logger.info("Contract code for given msisdn successfully retrieved");
				return msisdnList;
			} else {
				logger.info("No contract code found for given msisdn");
				return msisdnList;
			}
		} catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			// alarmUtils.sendRestAlarm(AlarmInfo.ALARM_SEVERITY.Critical.name(),
			// AlarmInfo.FAULTNAME.DatabaseDown.name(),"Cassandra DB site is down");
			// alarmHandler.raiseAlarm(AlarmInfo.FAULTNAME.DatabaseDown.name(),
			// AlarmInfo.ALARM_SEVERITY.Critical.name(), "Cassandra DB site is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return msisdnList;
	}
	
	@Override
	public List<Map<String, Object>> getIndividualTransactionMSISDN(String contractcode, String messageid,String msisdn, boolean detail) {
		
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		try {
			if (messageid != null) {
				logger.info("fetching data for msisdn with msgid");
	            return fetchDataWithMessageId(contractcode, messageid, msisdn, detail);
	        } else {
	        	logger.info("fetching data for only msisdn");
	            return fetchDataWithoutMessageId(contractcode, msisdn, detail);
	        }
		} catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return resultSetList;
	}
	
	private List<Map<String, Object>> fetchDataWithMessageId(String contractcode, String messageid, String msisdn, boolean detail) {
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		try {
		    String query = Constants.GET_COMMON_POH_TRANSACTION_STATUS_WITH_MESSAGEID_SUBID.replace("KEYSPACE", cassandraKeySpaceSiteA);
		    PreparedStatement preparedStatement = session.prepare(query);
		    BoundStatement boundStatement = preparedStatement.bind(contractcode, messageid);
		    ResultSet resultSet = session.execute(boundStatement);
		    logger.debug(query);
		    if (resultSet.iterator().hasNext()) {
		        for (Row row : resultSet) {
		        	if (msisdn.equalsIgnoreCase(row.getString(Constants.MSISDN))) {
		                Map<String, Object> rowMap = mapRowToResult(row, detail);
		                resultSetList.add(rowMap);
		            }
		        }
		        logger.info("Data retrieved successfully from tab_common_poh");
		    } else {
		        logger.info("No data exist for given contract code");
		    }
		    return resultSetList;
	    }catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return resultSetList;
		
	}

	private List<Map<String, Object>> fetchDataWithoutMessageId(String contractcode, String msisdn, boolean detail) {
		List<Map<String, Object>> resultSetList = new ArrayList<>();
		try {
			String query = Constants.GET_COMMON_POH_TRANSACTION_STATUS_SUBID.replace("KEYSPACE", cassandraKeySpaceSiteA);
		    PreparedStatement preparedStatement = session.prepare(query);
		    BoundStatement boundStatement = preparedStatement.bind(contractcode);
		    ResultSet resultSet = session.execute(boundStatement);
		    logger.debug(query);
		    if (resultSet.iterator().hasNext()) {
		        for (Row row : resultSet) {
		            if (msisdn.equalsIgnoreCase(row.getString(Constants.MSISDN))) {
		                Map<String, Object> rowMap = mapRowToResult(row, detail);
		                resultSetList.add(rowMap);
		            }
		        }
		        logger.info("Data retrieved successfully from tab_common_poh");
		    } else {
		        logger.info("No data exist for given contract code");
		    }
		    return resultSetList;
	    }catch (ConnectionInitException | NoNodeAvailableException e) {
			logger.info("Raising alarm as Cassandra server is down");
			failOverLogic(currentDbSite);
			enableCassandraAutoRecoverScheduler();
		}
		return resultSetList;
	}


	public void closeSession() {
		session.close();
	}

	public CqlSession getSession() {
		return this.session;
	}

	public CassandraConnector getConnector() {

		return this.connector;
	}

	public void enableCassandraAutoRecoverScheduler() {
		schedulerEnabled = true;
	}

	public void disableCassandraAutoRecoverScheduler() {
		schedulerEnabled = false;
	}

	public boolean isEnableSchedulerNoneDBFailover() {
		return enableSchedulerNoneDBFailover;
	}

	public void setEnableSchedulerNoneDBFailover(boolean enableSchedulerNoneDBFailover) {
		this.enableSchedulerNoneDBFailover = enableSchedulerNoneDBFailover;
	}

	public boolean isEnableSchedulerFetchFailoverAfterNONE() {
		return enableSchedulerFetchFailoverAfterNONE;
	}

	public void setEnableSchedulerFetchFailoverAfterNONE(boolean enableSchedulerFetchFailoverAfterNONE) {
		this.enableSchedulerFetchFailoverAfterNONE = enableSchedulerFetchFailoverAfterNONE;
	}

	

}
