package com.ericsson.statusquery.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ericsson.statusquery.constants.Constants;
import com.ericsson.statusquery.model.KubernetesProperties;

import ch.qos.logback.classic.Level;

@Component
public class LoggerMapping {
	
	Logger logger= LoggerFactory.getLogger(LoggerMapping.class);

	@Autowired
	KubernetesProperties kubernetesProperties;

	private String kafkaLoggerLevel = "";
	private String cassandraLoggerLevel = "";
	private String rootLoggerLevel = "";
	
	@Scheduled(fixedRate = 5000)
	private void checkLogger() {
		if(logger.isDebugEnabled())
			logger.debug("Schelduled executing...");
//		logger.info("Inside the Logger Scheduler ....  Kafka config map value {} kafka current logger value {} ",
//				kubernetesProperties.getKafkaLoggerLevel() ,getKafkaLoggerLevel());
		
		//KAFKA
		if (!getKafkaLoggerLevel().equals(kubernetesProperties.getKafkaLoggerLevel())) {
			logger.info("Inside the kafka block");
			setKafkaLoggerLevel(kubernetesProperties.getKafkaLoggerLevel());
			if (kubernetesProperties.getKafkaLoggerLevel().equalsIgnoreCase(Constants.INFO))
				logController(Constants.KAFKA_APP_NAME, ch.qos.logback.classic.Level.INFO);
			else if (kubernetesProperties.getKafkaLoggerLevel().equalsIgnoreCase(Constants.OFF))
				logController(Constants.KAFKA_APP_NAME, ch.qos.logback.classic.Level.OFF);
			else if (kubernetesProperties.getKafkaLoggerLevel().equalsIgnoreCase(Constants.DEBUG))
				logController(Constants.KAFKA_APP_NAME, ch.qos.logback.classic.Level.DEBUG);
			else if (kubernetesProperties.getKafkaLoggerLevel().equalsIgnoreCase(Constants.ERROR))
				logController(Constants.KAFKA_APP_NAME, ch.qos.logback.classic.Level.ERROR);
		} 
//		CASSANDRA
		if (!getCassandraLoggerLevel().equals(kubernetesProperties.getCassandraLoggerLevel())) {
			setCassandraLoggerLevel(kubernetesProperties.getCassandraLoggerLevel());
			if (kubernetesProperties.getCassandraLoggerLevel().equalsIgnoreCase(Constants.INFO))
				logController(Constants.CASSANDRA_APP_NAME, ch.qos.logback.classic.Level.INFO);
			else if (kubernetesProperties.getCassandraLoggerLevel().equalsIgnoreCase(Constants.OFF))
				logController(Constants.CASSANDRA_APP_NAME, ch.qos.logback.classic.Level.OFF);
			else if (kubernetesProperties.getCassandraLoggerLevel().equalsIgnoreCase(Constants.DEBUG))
				logController(Constants.CASSANDRA_APP_NAME, ch.qos.logback.classic.Level.DEBUG);
			else if (kubernetesProperties.getCassandraLoggerLevel().equalsIgnoreCase(Constants.ERROR))
				logController(Constants.CASSANDRA_APP_NAME, ch.qos.logback.classic.Level.ERROR);
		}

		if (!getRootLoggerLevel().equals(kubernetesProperties.getRootLoggerLevel())) {
			setRootLoggerLevel(kubernetesProperties.getRootLoggerLevel());
			if (kubernetesProperties.getRootLoggerLevel().equalsIgnoreCase(Constants.INFO))
				logController(Logger.ROOT_LOGGER_NAME,ch.qos.logback.classic.Level.INFO);
			else if (kubernetesProperties.getRootLoggerLevel().equalsIgnoreCase(Constants.OFF))
				logController(Logger.ROOT_LOGGER_NAME,ch.qos.logback.classic.Level.OFF);
			else if (kubernetesProperties.getRootLoggerLevel().equalsIgnoreCase(Constants.DEBUG))
				logController(Logger.ROOT_LOGGER_NAME,ch.qos.logback.classic.Level.DEBUG);
			else if (kubernetesProperties.getRootLoggerLevel().equalsIgnoreCase(Constants.ERROR))
				logController(Logger.ROOT_LOGGER_NAME,ch.qos.logback.classic.Level.ERROR);
		}
	}

	// Enable / Disable 3PP Logger
	private void logController(String appName, Level logLevel) {
//		logger.info("Appname is {} log level {} ",appName , logLevel.toString());
		
		ch.qos.logback.classic.Logger kafkaLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(appName);
		kafkaLogger.setLevel(logLevel);
	}
	
//	private void updateRootLevel(Level logLevel)
//	{
//		ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//        rootLogger.setLevel(logLevel);
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
	public String getRootLoggerLevel() {
		return rootLoggerLevel;
	}

	public void setRootLoggerLevel(String rootLoggerLevel) {
		this.rootLoggerLevel = rootLoggerLevel;
	}

	public void setCassandraLoggerLevel(String cassandraLoggerLevel) {
		this.cassandraLoggerLevel = cassandraLoggerLevel;
	}

}
