package com.ericsson.statusquery.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import com.ericsson.statusquery.constants.Constants;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.util.Config;

@Configuration
public class KubernetesProperties {

	private static final Logger logger = LoggerFactory.getLogger(KubernetesProperties.class);

	private static Map<String, String> confmap = null;

	@Value("${configmap.name}")
	private String configMapName;

	private String namespace;

	// Database Configuration
	private String cassandraIpAddressSite1;
	private String cassandraDatacenterSite1;
	private String cassandraKeyspaceSite1;
	private String cassandraPortSite1;
	private String cassandraIpAddressSite2;
	private String cassandraDatacenterSite2;
	private String cassandraKeyspaceSite2;
	private String cassandraPortSite2;
	private String cassandraNoOfReplica;
	private String clusterName;
	
//	private String managementHealthCassandraEnabled;
//	private String managementEndpointsWebExposureInclude;
//	private String managementEndpointHealthProbesEnabled;
//	private String springJmxEnabled;
//	private String managementEndpointsJmxExposureInclude;
//	private String managementEndpointPrometheusEnabled;
	
	private String timeZoneId;
	
//	private String switchBack = "true";
//	// kubernetes properties
	private String podId;
	private String nodeId;
	private String value = "";
	private ApiClient client;
	private CoreV1Api api;
	private V1ConfigMapList configList;
	private V1PodList podList;
	private V1NodeList nodeList;
	private V1ServiceList serviceList;
	private V1ConfigMap configmap;

	private String loggingLevelRoot;
	
	// Logger
	private String kafkaLoggerLevel;
	private String cassandraLoggerLevel;
	private String rootLoggerLevel;

	private String kafkaNamespacePolarisSite;
	private String kafkaNamespaceTitanSite;
	private String cassandraNamespacePolarisSite;
	private String cassandraNamespaceTitanSite;
	
//	private String alarmserviceName;
//	private String alarmUrl;
//	private String faultyResource;


	@Autowired
	public KubernetesProperties() {

		try {
			// for lab and local
			client = Config.defaultClient();
			api = new CoreV1Api(client);
		} catch (IOException e) {

			logger.error("Exception Occurred...", e);
		}

	}

	@PostConstruct
	private void initializeProperties() {
		namespace = System.getenv("POD_NAMESPACE");
		podId = System.getenv("POD_NAME");
		getAllPropertiesFromConfigMap();
	}

	// After every 1 min fetching configMap Data from mount Volume
	@Scheduled(fixedRate = 60000L, initialDelay = 0)
	public void getAllPropertiesFromConfigMap() {
		try {
			getPods();
			Map<String, String> confmap = this.getConfig(configMapName, namespace);

			confmap.entrySet().forEach(entry -> {
				value = entry.getValue();
			});

			HashMap<String, String> map = new HashMap<String, String>();

			StringTokenizer token = new StringTokenizer(value, " ");
			// StringTokenizer token = new StringTokenizer(value, "\n");
			while (token.hasMoreTokens()) {
				String[] str = token.nextToken().split("=");
//				logger.info("{},{}",str[0],str[1]);
				map.put(str[0], str[1]);
			}

			map.entrySet().forEach(entry -> {
				
				if (Constants.KAFKA_LOGGER_LEVEL.equals(entry.getKey())) {
					this.kafkaLoggerLevel = entry.getValue();

				}
				if (Constants.CASSANDRA_LOGGER_LEVEL.equals(entry.getKey())) {
					this.cassandraLoggerLevel = entry.getValue();

				}
				if (Constants.LOGGING_LEVEL_ROOT.equals(entry.getKey())) {
					this.rootLoggerLevel = entry.getValue();

				}

				if (logger.isDebugEnabled()) {
					logger.debug(" {} = {} ", entry.getKey(), entry.getValue());
				}

				if (Constants.CASSANDRA_IP_ADDRESS_SITE_1.equals(entry.getKey())) {
					this.cassandraIpAddressSite1 = entry.getValue();

				}

				if (Constants.CASSANDRA_DATACENTER_SITE_1.equals(entry.getKey())) {
					this.cassandraDatacenterSite1 = entry.getValue();

				}
				if (Constants.CASSANDRA_KEYSPACE_SITE_1.equals(entry.getKey())) {
					this.cassandraKeyspaceSite1 = entry.getValue();

				}

				if (Constants.CASSANDRA_PORT_SITE_1.equals(entry.getKey())) {
					this.cassandraPortSite1 = entry.getValue();

				}

				if (Constants.CASSANDRA_IP_ADDRESS_SITE_2.equals(entry.getKey())) {
					this.cassandraIpAddressSite2 = entry.getValue();

				}

				if (Constants.CASSANDRA_DATACENTER_SITE_2.equals(entry.getKey())) {
					this.cassandraDatacenterSite2 = entry.getValue();

				}

				if (Constants.CASSANDRA_KEYSPACE_SITE_2.equals(entry.getKey())) {
					this.cassandraKeyspaceSite2 = entry.getValue();

				}
				if (Constants.CASSANDRA_PORT_SITE_2.equals(entry.getKey())) {
					this.cassandraPortSite2 = entry.getValue();

				}
				if (Constants.CASSANDRA_NO_OF_REPLICA.equals(entry.getKey())) {
					this.cassandraNoOfReplica = entry.getValue();

				}
				
//				if (Constants.MANAGEMENT_HEALTH_CASSANDRA_ENABLED.equals(entry.getKey())) {
//					this.managementHealthCassandraEnabled = entry.getValue();
//
//				}
//				
//				if (Constants.MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE.equals(entry.getKey())) {
//					this.managementEndpointsWebExposureInclude = entry.getValue();
//
//				}
//				
//				if (Constants.MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED.equals(entry.getKey())) {
//					this.managementEndpointHealthProbesEnabled = entry.getValue();
//
//				}
//				
//				if (Constants.SPRING_JMX_ENABLED.equals(entry.getKey())) {
//					this.springJmxEnabled = entry.getValue();
//
//				}
//				
//				if (Constants.MANAGEMENT_ENDPOINTS_JMX_EXPOSURE_INCLUDE.equals(entry.getKey())) {
//					this.managementEndpointsJmxExposureInclude = entry.getValue();
//
//				}
//				
//				if (Constants.MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED.equals(entry.getKey())) {
//					this.managementEndpointPrometheusEnabled = entry.getValue();
//
//				}
				
//				if (Constants.POLLING_INTERVAL_TOPIC_CONF.equals(entry.getKey())) {
//					this.pollingIntervalTopicConf = entry.getValue();
//				}

				if (Constants.LOGGING_LEVEL_ROOT.equals(entry.getKey())) {
					this.loggingLevelRoot = entry.getValue();
				}
				
				if (Constants.TIME_ZONE_ID.equals(entry.getKey())) {
					this.timeZoneId = entry.getValue();
				}
				
				if (Constants.CASSANDRA_NAMESPACE_POLARIS_SITE.equals(entry.getKey())) {
					cassandraNamespacePolarisSite = entry.getValue();
				}
				if (Constants.CASSANDRA_NAMESPACE_TITAN_SITE.equals(entry.getKey())) {
					cassandraNamespaceTitanSite = entry.getValue();
				}
				if (Constants.KAFKA_NAMESPACE_POLARIS_SITE.equals(entry.getKey())) {
					kafkaNamespacePolarisSite = entry.getValue();
				}
				if (Constants.KAFKA_NAMESPACE_TITAN_SITE.equals(entry.getKey())) {
					kafkaNamespaceTitanSite = entry.getValue();
				}
//				if (Constants.ALARM_SERVICE_NAME.equals(entry.getKey())) {
//					this.alarmserviceName = entry.getValue();
//				}
//				
//				if (Constants.ALARM_URL.equals(entry.getKey())) {
//					this.alarmUrl = entry.getValue();
//				}
//				if (Constants.FAULTY_RESOURCE.equals(entry.getKey())) {
//					this.faultyResource = entry.getValue();
//
//				}
			});

		} catch (IOException | ApiException e) {
			logger.error("Error in reading ConfigMap {}", e);
		} catch (InterruptedException ex) {
			logger.warn("Interrupted!", ex);
			Thread.currentThread().interrupt();
		}
	}

	public void getNodes() throws ApiException {
		// Get the Node name where the pod is running
		String nodeName = api.readNamespacedPod(this.podId, namespace, null, null, null).getSpec().getNodeName();
		try {
			nodeList = getNodeList();

			V1Node node = nodeList.getItems().stream().filter(n -> n.getMetadata().getName().equals(nodeName))
					.findFirst().orElse(null);

			if (node != null) {
				// Retrieve the worker node ID
				this.nodeId = node.getMetadata().getName();
				System.out.println("Worker Node ID: " + nodeId);
			} else {
				System.out.println("Node not found.");
			}
		} catch (ApiException e) {
			logger.error("error in reading Node Id, {} ", e.getMessage());

		}

	}

	public V1NodeList getNodeList() throws ApiException {

		nodeList = api.listNode(null, null, null, null, null, null, null, null, null, null);
		return nodeList;
	}

	public void getPods() throws ApiException {
		podList = getPodList(namespace);
	}

	public V1PodList getPodList(String namespace) throws ApiException {

		podList = api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null);
		return podList;
	}

	public void getService() throws ApiException {
		serviceList = getServiceList(namespace);

		this.getServiceList(namespace).getItems().stream().forEach((service) -> {
			// logger.info("service {}", service.getMetadata());

		});
	}

	public V1ServiceList getServiceList(String namespace) throws ApiException {
		serviceList = api.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null, null);
		return serviceList;
	}

	public V1ConfigMapList getConfigMapList(String namespace) throws ApiException {

		configList = api.listNamespacedConfigMap(namespace, null, null, null, null, null, null, null, null, null, null);

		return configList;
	}

	public V1ConfigMap replaceNamespacedConfigMap(String configmapname, String namespace, V1ConfigMap body)
			throws ApiException {

		configmap = api.replaceNamespacedConfigMap(configmapname, namespace, body, null, null, null);

		return configmap;
	}

	public Map<String, String> getConfig(String configMapName, String namespace)
			throws InterruptedException, IOException, ApiException {

		this.getConfigMapList(namespace).getItems().stream().forEach((config) -> {
			if (config.getMetadata().getName().contentEquals(configMapName)) {
				confmap = config.getData();

			}

		});

		return confmap;

	}

	public void updateConfigMap(KubernetesProperties prop) throws InterruptedException, IOException, ApiException {

		if (prop.getKafkaLoggerLevel() != null) {
			this.kafkaLoggerLevel = prop.getKafkaLoggerLevel();
		}
		if (prop.getCassandraLoggerLevel() != null) {
			this.cassandraLoggerLevel = prop.getCassandraLoggerLevel();
		}
		if (prop.getRootLoggerLevel() != null) {
			this.rootLoggerLevel = prop.getRootLoggerLevel();
		}

		if (prop.getCassandraIpAddressSite1() != null) {
			this.cassandraIpAddressSite1 = prop.getCassandraIpAddressSite1();
		}
		if (prop.getCassandraDatacenterSite1() != null) {
			this.cassandraDatacenterSite1 = prop.getCassandraDatacenterSite1();
		}
		if (prop.getCassandraKeyspaceSite1() != null) {
			this.cassandraKeyspaceSite1 = prop.getCassandraKeyspaceSite1();
		}
		if (prop.getCassandraPortSite1() != null) {
			this.cassandraPortSite1 = prop.getCassandraPortSite1();
		}
		if (prop.getCassandraIpAddressSite2() != null) {
			this.cassandraIpAddressSite2 = prop.getCassandraIpAddressSite2();
		}
		if (prop.getCassandraDatacenterSite2() != null) {
			this.cassandraDatacenterSite2 = prop.getCassandraDatacenterSite2();
		}
		if (prop.getCassandraKeyspaceSite2() != null) {
			this.cassandraKeyspaceSite2 = prop.getCassandraKeyspaceSite2();
		}
		if (prop.getCassandraPortSite2() != null) {
			this.cassandraPortSite2 = prop.getCassandraPortSite2();
		}
		if (prop.getCassandraNoOfReplica() != null) {
			this.cassandraNoOfReplica = prop.getCassandraNoOfReplica();
		}
//		if (prop.getManagementHealthCassandraEnabled() != null) {
//			this.managementHealthCassandraEnabled = prop.getManagementHealthCassandraEnabled();
//		}
//		if (prop.getManagementEndpointsWebExposureInclude() != null) {
//			this.managementEndpointsWebExposureInclude = prop.getManagementEndpointsWebExposureInclude();
//		}
//		if (prop.getSpringJmxEnabled() != null) {
//			this.springJmxEnabled = prop.getSpringJmxEnabled();
//		}
//		if (prop.getManagementEndpointsJmxExposureInclude() != null) {
//			this.managementEndpointsJmxExposureInclude = prop.getManagementEndpointsJmxExposureInclude();
//		}
//		if (prop.getManagementEndpointPrometheusEnabled() != null) {
//			this.managementEndpointPrometheusEnabled = prop.getManagementEndpointPrometheusEnabled();
//		}
//		if (prop.getPollingIntervalTopicConf() != null) {
//			this.pollingIntervalTopicConf = prop.getPollingIntervalTopicConf();
//		}
		if (prop.getTimeZoneId() != null) {
			this.timeZoneId = prop.getTimeZoneId();
		}
		if (prop.getLoggingLevelRoot() != null) {
			this.loggingLevelRoot = prop.getLoggingLevelRoot();
		}

		if (prop.getClusterName() != null) {
			this.clusterName = prop.getClusterName();
		}
		if (prop.getKafkaNamespacePolarisSite() != null) {
			this.kafkaNamespacePolarisSite = prop.getKafkaNamespacePolarisSite();
		}
		if (prop.getKafkaNamespaceTitanSite() != null) {
			this.kafkaNamespaceTitanSite = prop.getKafkaNamespaceTitanSite();
		}

		if (prop.getCassandraNamespacePolarisSite() != null) {
			this.cassandraNamespacePolarisSite = prop.getCassandraNamespacePolarisSite();
		}
		if (prop.getCassandraNamespaceTitanSite() != null) {
			this.cassandraNamespaceTitanSite = prop.getCassandraNamespaceTitanSite();
		}
//		if (prop.getAlarmUrl() != null) {
//			this.alarmUrl = prop.getAlarmUrl();
//		}
//		if (prop.getAlarmserviceName() != null) {
//			this.alarmserviceName = prop.getAlarmserviceName();
//		}
//		if (prop.getFaultyResource() != null) {
//			this.faultyResource = prop.getFaultyResource();
//		}

		
		StringBuilder stringdata = new StringBuilder();
		
		if (!Objects.isNull(this.kafkaLoggerLevel)) {
			stringdata.append(Constants.KAFKA_LOGGER_LEVEL + "=" + this.kafkaLoggerLevel + "\n");
		}
		if (!Objects.isNull(this.cassandraLoggerLevel)) {
			stringdata.append(Constants.CASSANDRA_LOGGER_LEVEL + "=" + this.cassandraLoggerLevel + "\n");
		}
		if (!Objects.isNull(this.rootLoggerLevel)) {
			stringdata.append(Constants.LOGGING_LEVEL_ROOT + "=" + this.rootLoggerLevel + "\n");
		}

		if (!Objects.isNull(this.cassandraIpAddressSite1)) {
			stringdata.append(Constants.CASSANDRA_IP_ADDRESS_SITE_1 + "=" + this.cassandraIpAddressSite1 + "\n");

		}
		if (!Objects.isNull(this.cassandraDatacenterSite1)) {
			stringdata.append(Constants.CASSANDRA_DATACENTER_SITE_1 + "=" + this.cassandraDatacenterSite1 + "\n");

		}
		if (!Objects.isNull(this.cassandraKeyspaceSite1)) {
			stringdata.append(Constants.CASSANDRA_KEYSPACE_SITE_1 + "=" + this.cassandraKeyspaceSite1 + "\n");

		}
		if (!Objects.isNull(this.cassandraPortSite1)) {
			stringdata.append(Constants.CASSANDRA_PORT_SITE_1 + "=" + this.cassandraPortSite1 + "\n");

		}
		if (!Objects.isNull(this.cassandraIpAddressSite2)) {
			stringdata.append(Constants.CASSANDRA_IP_ADDRESS_SITE_2 + "=" + this.cassandraIpAddressSite2 + "\n");

		}
		if (!Objects.isNull(this.cassandraDatacenterSite2)) {
			stringdata.append(Constants.CASSANDRA_DATACENTER_SITE_2 + "=" + this.cassandraDatacenterSite2 + "\n");

		}
		if (!Objects.isNull(this.cassandraKeyspaceSite2)) {
			stringdata.append(Constants.CASSANDRA_KEYSPACE_SITE_2 + "=" + this.cassandraKeyspaceSite2 + "\n");

		}
		if (!Objects.isNull(this.cassandraPortSite2)) {
			stringdata.append(Constants.CASSANDRA_PORT_SITE_2 + "=" + this.cassandraPortSite2 + "\n");

		}

		if (!Objects.isNull(this.cassandraNoOfReplica)) {
			stringdata.append(Constants.CASSANDRA_NO_OF_REPLICA + "=" + this.cassandraNoOfReplica + "\n");

		}
				
//		if (!Objects.isNull(this.managementEndpointHealthProbesEnabled)) {
//			stringdata.append(Constants.MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED + "=" + this.managementEndpointHealthProbesEnabled + "\n");
//
//		}
//		
//		if (!Objects.isNull(this.managementEndpointPrometheusEnabled)) {
//			stringdata.append(Constants.MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED + "=" + this.managementEndpointPrometheusEnabled + "\n");
//
//		}
//		
//		if (!Objects.isNull(this.managementEndpointsJmxExposureInclude)) {
//			stringdata.append(Constants.MANAGEMENT_ENDPOINTS_JMX_EXPOSURE_INCLUDE + "=" + this.managementEndpointsJmxExposureInclude + "\n");
//
//		}
//		
//		if (!Objects.isNull(this.managementEndpointsWebExposureInclude)) {
//			stringdata.append(Constants.MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE + "=" + this.managementEndpointsWebExposureInclude + "\n");
//
//		}
//		
//		if (!Objects.isNull(this.managementHealthCassandraEnabled)) {
//			stringdata.append(Constants.MANAGEMENT_HEALTH_CASSANDRA_ENABLED + "=" + this.managementHealthCassandraEnabled + "\n");
//
//		}
//		if (!Objects.isNull(this.pollingIntervalTopicConf)) {
//			stringdata.append(Constants.POLLING_INTERVAL_TOPIC_CONF + "=" + this.pollingIntervalTopicConf + "\n");
//
//		}
		if (!Objects.isNull(this.timeZoneId)) {
			stringdata.append(Constants.TIME_ZONE_ID + "=" + this.timeZoneId + "\n");

		}
		
//		if (!Objects.isNull(this.springJmxEnabled)) {
//			stringdata.append(Constants.SPRING_JMX_ENABLED + "=" + this.springJmxEnabled + "\n");
//
//		}

		if (!Objects.isNull(this.loggingLevelRoot)) {
			stringdata.append(Constants.LOGGING_LEVEL_ROOT + "=" + this.loggingLevelRoot + "\n");

		}
		
		if (!Objects.isNull(this.kafkaNamespacePolarisSite)) {
			stringdata.append(Constants.KAFKA_NAMESPACE_POLARIS_SITE + "=" + this.kafkaNamespacePolarisSite + "\n");

		}
		
		if (!Objects.isNull(this.kafkaNamespaceTitanSite)) {
			stringdata.append(Constants.KAFKA_NAMESPACE_TITAN_SITE + "=" + this.kafkaNamespaceTitanSite + "\n");

		}
		
		if (!Objects.isNull(this.cassandraNamespacePolarisSite)) {
			stringdata.append(Constants.CASSANDRA_NAMESPACE_POLARIS_SITE + "=" + this.cassandraNamespacePolarisSite + "\n");

		}
		
		if (!Objects.isNull(this.cassandraNamespaceTitanSite)) {
			stringdata.append(Constants.CASSANDRA_NAMESPACE_TITAN_SITE + "=" + this.cassandraNamespaceTitanSite + "\n");

		}
		
//		if (!Objects.isNull(this.alarmserviceName)) {
//			stringdata.append(Constants.ALARM_SERVICE_NAME + "=" + this.alarmserviceName + "\n");
//
//		}
//		
//		if (!Objects.isNull(this.alarmUrl)) {
//			stringdata.append(Constants.ALARM_URL + "=" + this.alarmUrl + "\n");
//
//		}
//		if (!Objects.isNull(this.faultyResource)) {
//			stringdata.append(Constants.FAULTY_RESOURCE + "=" + this.faultyResource + "\n");
//		}

		
		

		Map<String, String> data = new HashMap<String, String>();
		data.put("application.properties", stringdata.toString());

		V1ObjectMeta buildable = new V1ObjectMeta();
		buildable.setName(prop.getConfigMapName());

		V1ConfigMap body = new V1ConfigMap();
		body.setMetadata(buildable);
		body.setData(data);

		this.replaceNamespacedConfigMap(prop.getConfigMapName(), prop.getNamespace(), body);

	}

	public String getConfigMapName() {
		return configMapName;
	}

	public void setConfigMapName(String configMapName) {
		this.configMapName = configMapName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

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

	public void setCassandraIpAddressSite1(String cassandraIpAddressSite1) {
		this.cassandraIpAddressSite1 = cassandraIpAddressSite1;
	}

	public void setCassandraDatacenterSite1(String cassandraDatacenterSite1) {
		this.cassandraDatacenterSite1 = cassandraDatacenterSite1;
	}

	public void setCassandraKeyspaceSite1(String cassandraKeyspaceSite1) {
		this.cassandraKeyspaceSite1 = cassandraKeyspaceSite1;
	}

	public void setCassandraPortSite1(String cassandraPortSite1) {
		this.cassandraPortSite1 = cassandraPortSite1;
	}

	public void setCassandraIpAddressSite2(String cassandraIpAddressSite2) {
		this.cassandraIpAddressSite2 = cassandraIpAddressSite2;
	}

	public void setCassandraDatacenterSite2(String cassandraDatacenterSite2) {
		this.cassandraDatacenterSite2 = cassandraDatacenterSite2;
	}

	public void setCassandraKeyspaceSite2(String cassandraKeyspaceSite2) {
		this.cassandraKeyspaceSite2 = cassandraKeyspaceSite2;
	}

	public void setCassandraPortSite2(String cassandraPortSite2) {
		this.cassandraPortSite2 = cassandraPortSite2;
	}

	public void setCassandraNoOfReplica(String cassandraNoOfReplica) {
		this.cassandraNoOfReplica = cassandraNoOfReplica;
	}

	public String getLoggingLevelRoot() {
		return loggingLevelRoot;
	}

	public void setLoggingLevelRoot(String loggingLevelRoot) {
		this.loggingLevelRoot = loggingLevelRoot;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getPodId() {
		return podId;
	}

	public void setPodId(String podId) {
		this.podId = podId;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

//	public String getManagementHealthCassandraEnabled() {
//		return managementHealthCassandraEnabled;
//	}
//
//	public void setManagementHealthCassandraEnabled(String managementHealthCassandraEnabled) {
//		this.managementHealthCassandraEnabled = managementHealthCassandraEnabled;
//	}
//
//	public String getManagementEndpointsWebExposureInclude() {
//		return managementEndpointsWebExposureInclude;
//	}
//
//	public void setManagementEndpointsWebExposureInclude(String managementEndpointsWebExposureInclude) {
//		this.managementEndpointsWebExposureInclude = managementEndpointsWebExposureInclude;
//	}
//
//	public String getManagementEndpointHealthProbesEnabled() {
//		return managementEndpointHealthProbesEnabled;
//	}
//
//	public void setManagementEndpointHealthProbesEnabled(String managementEndpointHealthProbesEnabled) {
//		this.managementEndpointHealthProbesEnabled = managementEndpointHealthProbesEnabled;
//	}
//
//	public String getSpringJmxEnabled() {
//		return springJmxEnabled;
//	}
//
//	public void setSpringJmxEnabled(String springJmxEnabled) {
//		this.springJmxEnabled = springJmxEnabled;
//	}
//
//	public String getManagementEndpointsJmxExposureInclude() {
//		return managementEndpointsJmxExposureInclude;
//	}
//
//	public void setManagementEndpointsJmxExposureInclude(String managementEndpointsJmxExposureInclude) {
//		this.managementEndpointsJmxExposureInclude = managementEndpointsJmxExposureInclude;
//	}
//
//	public String getManagementEndpointPrometheusEnabled() {
//		return managementEndpointPrometheusEnabled;
//	}
//
//	public void setManagementEndpointPrometheusEnabled(String managementEndpointPrometheusEnabled) {
//		this.managementEndpointPrometheusEnabled = managementEndpointPrometheusEnabled;
//	}
	
//	public String getPollingIntervalTopicConf() {
//		return pollingIntervalTopicConf;
//	}
//
//	public void setPollingIntervalTopicConf(String pollingIntervalTopicConf) {
//		this.pollingIntervalTopicConf = pollingIntervalTopicConf;
//	}

	public String getKafkaLoggerLevel() {
		return kafkaLoggerLevel;
	}

	public void setKafkaLoggerLevel(String kafkaLoggerLevel) {
		this.kafkaLoggerLevel = kafkaLoggerLevel;
	}

	public String getCassandraLoggerLevel() {
		return cassandraLoggerLevel;
	}

	public void setCassandraLoggerLevel(String cassandraLoggerLevel) {
		this.cassandraLoggerLevel = cassandraLoggerLevel;
	}

	public String getRootLoggerLevel() {
		return rootLoggerLevel;
	}

	public void setRootLoggerLevel(String rootLoggerLevel) {
		this.rootLoggerLevel = rootLoggerLevel;
	}

	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	public String getKafkaNamespacePolarisSite() {
		return kafkaNamespacePolarisSite;
	}

	public void setKafkaNamespacePolarisSite(String kafkaNamespacePolarisSite) {
		this.kafkaNamespacePolarisSite = kafkaNamespacePolarisSite;
	}

	public String getKafkaNamespaceTitanSite() {
		return kafkaNamespaceTitanSite;
	}

	public void setKafkaNamespaceTitanSite(String kafkaNamespaceTitanSite) {
		this.kafkaNamespaceTitanSite = kafkaNamespaceTitanSite;
	}

	public String getCassandraNamespacePolarisSite() {
		return cassandraNamespacePolarisSite;
	}

	public void setCassandraNamespacePolarisSite(String cassandraNamespacePolarisSite) {
		this.cassandraNamespacePolarisSite = cassandraNamespacePolarisSite;
	}

	public String getCassandraNamespaceTitanSite() {
		return cassandraNamespaceTitanSite;
	}

	public void setCassandraNamespaceTitanSite(String cassandraNamespaceTitanSite) {
		this.cassandraNamespaceTitanSite = cassandraNamespaceTitanSite;
	}

//	public String getAlarmserviceName() {
//		return alarmserviceName;
//	}
//
//	public void setAlarmserviceName(String alarmserviceName) {
//		this.alarmserviceName = alarmserviceName;
//	}
//
//	public String getAlarmUrl() {
//		return alarmUrl;
//	}
//
//	public void setAlarmUrl(String alarmUrl) {
//		this.alarmUrl = alarmUrl;
//	}
//
//	public String getFaultyResource() {
//		return faultyResource;
//	}
//
//	public void setFaultyResource(String faultyResource) {
//		this.faultyResource = faultyResource;
//	}
	
	
	
}
