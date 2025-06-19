package com.ericsson.statusquery.constants;

public class Constants {
	
	public static final String LOGGING_LEVEL_ROOT = "logging.level.root";
	
    // Cassandra

	public static final String CASSANDRA_IP_ADDRESS_SITE_1 = "cassandra.ip.address.local";
	public static final String CASSANDRA_DATACENTER_SITE_1 = "cassandra.datacenter.local";
	public static final String CASSANDRA_KEYSPACE_SITE_1 = "cassandra.keyspace.local";
	public static final String CASSANDRA_PORT_SITE_1 = "cassandra.port.local";
	public static final String CASSANDRA_IP_ADDRESS_SITE_2 = "cassandra.ip.address.remote";
	public static final String CASSANDRA_DATACENTER_SITE_2 = "cassandra.datacenter.remote";
	public static final String CASSANDRA_KEYSPACE_SITE_2 = "cassandra.keyspace.remote";
	public static final String CASSANDRA_PORT_SITE_2 = "cassandra.port.remote";
	public static final String DB_SITE_1 = "POLARIS";
	public static final String DB_SITE_2 = "TITAN";
	public static final String DB_NOT_CONNECTED = "Not Connected";
	public static final String CASSANDRA_NO_OF_REPLICA = "cassandra.no.of.replica";
	public static final String DB_FAILOVER_BOTH = "Both";
	public static final String DB_FAILOVER_TITAN = "Titan";
	public static final String DB_FAILOVER_POLARIS = "Polaris";
	

	// Table Details

	public static final String EDA_GEO_RED_TABLE_NAME = "tab_common_poh";
	public static final String SUB_ID = "subid";
	public static final String MESSAGE_ID = "messageid";
	public static final String CONTRACT_CODE = "contractcode";
	public static final String MSISDN = "msisdn";
	public static final String REGISTRATION_TIMESTAMP = "registrationtimestamp";
	public static final String PRODUCER = "producer";
	public static final String TOPIC = "topic";
	public static final String CONSUMER_TIMESTAMP_NORMAL = "consumetimestampnormal";
	public static final String CONSUMER_ID = "consumerid";
	public static final String LOG_ROOTID_CONSUME_NORMAL = "logrootidconsumenormal";
	public static final String CONSUME_TIMESTAMP_REPLY = "consumetimestampreplay";
	public static final String CONSUMER_ID_REPLY = "consumerIdReplay";
	public static final String LOG_ROOTID_CONSUME_REPLY = "logrootidconsumereplay";
	public static final String INTERIM_NOTIFY_TIMESTAMP = "interimnotifytimestamp";
	public static final String FINAL_NOTIFY_TIMESTAMP = "finalnotifytimestamp";
	public static final String STATUS = "status";
	public static final String TIMERANGE = "timerange";
	public static final String REPROVRULETABVERSION = "reprovruletabversion";
	public static final String RULESRAWDATA= "rulesrawdata";
	public static final String ERROR_DETAILS = "errordetails";
	public static final String FLUSH_INDICATOR = "flush_indicator";
	public static final String FLUSH_MESSAGEID = "flush_messageid";
	public static final String FINAL_STATUS = "finalStatus";
	public static final String FINAL_STATUS_ONGOING = "ongoing";
	public static final String STATUS_FAILED = "failed";
	public static final String STATUS_COMPLETED = "completed";
	public static final String STATUS_SUCCESS = "success";
	public static final String DETAILS = "detail";
	public static final String ACTION = "action";
	public static final String REPLAY_CONSUMER_ID = "replayconsumerid";
	public static final String REPROV_REPLAY_TRIGGER_TIMESTAMP = "reprov_replay_trigger_timestamp";
	public static final String ROOT_LOG_ID = "rootlogid";
	public static final String SCOPE = "scope";
	public static final String TOPIC_PARTITION = "topicpartition";
	public static final String ORIGINAL_MESSAGE_ID = "originalmessageid";
	public static final String OPERATION = "operation";
	public static final String REPROVISIONING_CONSUMER_ID = "reprovisioningconsumerid";
	public static final String CONSUME_TIMESTAMP_REPROVISIONING = "consumetimestampreprovisioning";
	public static final String LOG_ROOT_ID_CONSUMER_REPROVISIONING = "logrootidconsumereprovisioning";
	
	public static final String AUTH_TYPE ="Basic ";
	public static final String AUTH_ERROR="No authentication provided";
	public static final String AUTH_ERROR_1="Invalid authentication credentials";
	public static final String MANAGEMENT_HEALTH_CASSANDRA_ENABLED = "managementHealthCassandraEnabled";
	public static final String MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE = "managementEndpointsWebExposureInclude";
	public static final String MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED = "managementEndpointHealthProbesEnabled";
	public static final String SPRING_JMX_ENABLED = "springJmxEnabled";
	public static final String MANAGEMENT_ENDPOINTS_JMX_EXPOSURE_INCLUDE = "managementEndpointsJmxExposureInclude";
	public static final String MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED = "managementEndpointPrometheusEnabled";
	
	public static final String GET_KAFKA_BROKER_STATUS = "SELECT * from KEYSPACE.tab_kafka_broker_status where namespace = ?";
	public static final String GET_CASSANDRA_HEALTH_STATUS = "SELECT * from KEYSPACE.tab_cassandra_health_status where namespace = ?";
	public static final String GET_NPECLUSTER_HEALTH_STATUS = "SELECT * from KEYSPACE.tab_npecluster_health_status where namespace = ?";
	public static final String GET_ACTIVE_ALARMS_STATUS = "SELECT * from KEYSPACE.tab_active_alarms where namespace = ?";
	public static final String GET_KAFKA_PRODUCER_STATUS = "SELECT * from KEYSPACE.tab_producer_config_version_status where namespace = ?";
	public static final String GET_COMMON_POH_TRANSACTION_STATUS_SUBID = "SELECT contractcode,messageid,action,consumerid,consumetimestampnormal,consumetimestampreplay,errordetails,finalnotifytimestamp,flush_indicator,flush_messageid,interimnotifytimestamp,logrootidconsumenormal,logrootidconsumereplay,msisdn,originalmessageid,producer,registrationtimestamp,replayconsumerid,reprov_replay_trigger_timestamp,scope,status,topic,topicpartition,writetime(registrationtimestamp) from KEYSPACE.tab_common_poh where contractcode = ?";
	public static final String GET_COMMON_POH_TRANSACTION_STATUS_WITH_MESSAGEID_SUBID="SELECT * from KEYSPACE.tab_common_poh where contractcode = ? and messageid = ?";
	public static final String GET_CONTRACTCODE_FOR_MSISDN = "SELECT contractcode from KEYSPACE.tab_common_poh_msisdn where msisdn = ?";
	public static final String GET_EDA_NAMESPACES = "SELECT npeclusters_id from tab_npeclusters_conf";

	public static final String DB_FAILOVER_NONE = "None";

	public static final String POLLING_INTERVAL_TOPIC_CONF = "polling.interval";
	public static final String TIME_ZONE_ID = "time.zone.id";
	
	public static final String NAMESPACE = "namespace";
	public static final String BROKER_NAME = "broker_name";
	public static final String IP_LB = "IP_LB";
	public static final String CASSANDRA_INSTANCE = "cassandra_instance";
	public static final String STATUS_DATA = "status_data";
	public static final String DATA = "data";
	public static final String PRODUCER_NAME = "producer_name";
	public static final String RUNNING_VERSION = "running_version";
	public static final String DB_VERSION = "db_version";
	public static final String REPROVISIONING_REQUESTS = "reprovisioning-requests";
	public static final String KAFKASTATUS = "kafkastatus";
	public static final String CASSANDRASTATUS = "cassandrastatus";
	public static final String NPECLUSTERSTATUS = "npeclusterstatus";
	public static final String ACTIVEALARMS = "activealarms";
	public static final String KAFKAPRODUCERSTATUS = "kafkaproducerstatus";
	public static final String KAFKA_STATUS = "kafka_status";
	public static final String CASSANDRA_STATUS = "cassandra_health_status";
	public static final String NPECLUSTER_STATUS = "npecluster_status";
	public static final String ACTIVE_ALARMS = "active_alarms";
	public static final String KAFKA_PRODUCER_STATUS = "kafka_producer_status";
	public static final String PRODUCER_STATUS_VALUE = "UP";
	
	public static final String KAFKA_LOGGER_LEVEL="kafka.logger.level";
	public static final String CASSANDRA_LOGGER_LEVEL="cassandra.logger.level";
	
	public static final String KAFKA_APP_NAME="org.apache.kafka";
	public static final String CASSANDRA_APP_NAME="com.datastax.oss";
	public static final String ROOT_APP_NAME="logging.level.root";
	public static final String INFO="INFO";
	public static final String OFF="OFF";
	public static final String DEBUG="DEBUG";
	public static final String ERROR="ERROR";
	public static final String RESTRICTED_URI = "/monitor/*";
	public static final String AUTH_HEADER="Authorization";
	
	public static final String KAFKA_NAMESPACE_POLARIS_SITE = "kafka.namespace.polaris.site";
	public static final String KAFKA_NAMESPACE_TITAN_SITE = "kafka.namespace.titan.site";
	public static final String CASSANDRA_NAMESPACE_POLARIS_SITE = "cassandra.namespace.polaris.site";
	public static final String CASSANDRA_NAMESPACE_TITAN_SITE = "cassandra.namespace.titan.site";
	
	public static final String ALARM_URL = "alarm_url";
	public static final String ALARM_SERVICE_NAME = "alarmserviceName";
	public static final String FAULTY_RESOURCE = "faultyResource";
	
	public static final String INTERIM_NOT_TRIGGER = "interimNotification Not Trigger";


	
}

