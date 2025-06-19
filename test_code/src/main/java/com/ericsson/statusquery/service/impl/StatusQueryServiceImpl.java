package com.ericsson.statusquery.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.statusquery.constants.Constants;
import com.ericsson.statusquery.exception.StatusQueryServiceException;
import com.ericsson.statusquery.repository.ICassandraRepository;
import com.ericsson.statusquery.service.IStatusQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Component
public class StatusQueryServiceImpl implements IStatusQueryService {

	private static final Logger logger = LoggerFactory.getLogger("StatusQueryServiceImpl");

	@Autowired
	private ICassandraRepository iCassandraRepository;

	@Override
	public String getStatus(String tag, String login) {
		try {
			iCassandraRepository.connectDB();
			if (Constants.KAFKASTATUS.equalsIgnoreCase(tag)) {
				return this.dataInJson(Constants.KAFKA_STATUS, iCassandraRepository.getKafkaBrokerStatus(), false, "");
			} else if (Constants.CASSANDRASTATUS.equalsIgnoreCase(tag)) {
				return this.dataInJson(Constants.CASSANDRA_STATUS, iCassandraRepository.getCassandraHealthStatus(),
						false, "");
			} else if (Constants.NPECLUSTERSTATUS.equalsIgnoreCase(tag)) {
				return this.dataInJson(Constants.NPECLUSTER_STATUS, iCassandraRepository.getNpeClusterHealthStatus(),
						false, "");
			} else if (Constants.ACTIVEALARMS.equalsIgnoreCase(tag)) {
				return this.dataInJson(Constants.ACTIVE_ALARMS, iCassandraRepository.getActiveAlarmsStatus(), false,
						"");
			} else if (Constants.KAFKAPRODUCERSTATUS.equalsIgnoreCase(tag)) {
				return this.dataInJson(Constants.KAFKA_PRODUCER_STATUS, iCassandraRepository.getKAfkaProducerStatus(),
						false, "");
			} else if ("all".equalsIgnoreCase(tag)) {
				JSONObject combinedStatus = new JSONObject();
				combinedStatus.put(Constants.KAFKA_STATUS, iCassandraRepository.getKafkaBrokerStatus());
				combinedStatus.put(Constants.CASSANDRA_STATUS, iCassandraRepository.getCassandraHealthStatus());
				combinedStatus.put(Constants.NPECLUSTER_STATUS, iCassandraRepository.getNpeClusterHealthStatus());
				combinedStatus.put(Constants.ACTIVE_ALARMS, iCassandraRepository.getActiveAlarmsStatus());
				combinedStatus.put(Constants.KAFKA_PRODUCER_STATUS, iCassandraRepository.getKAfkaProducerStatus());
				return combinedStatus.toString();
			}
			return "Invalid request";
		} catch (Exception ex) {
			throw new StatusQueryServiceException("Exception Occured in getKafkaStatus()", ex);
		}
	}

	@Override
	public String dataInJson(String tag, List<Map<String, Object>> list, boolean isContractCodePresent,
			String contractCode) {
		try {
			Map<String, Object> jsonMap = new LinkedHashMap<>();
			if (isContractCodePresent) {
				jsonMap.put(Constants.SUB_ID, contractCode);
			}

			jsonMap.put(tag, list);
			// logger.info("Status details map : {}",jsonMap);

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			String responseString = objectMapper.writeValueAsString(jsonMap);
			// String jsonString = writeValueAsString.replaceAll("\\\\[ntr]+", "");
			// String cleanJsonString = jsonString.replace("\\", "");
			String prettyJsonString = responseString.replace("\\\"", "");
			logger.info("Final json data is ready");
			return prettyJsonString;
		} catch (Exception ex) {
			throw new StatusQueryServiceException("Exception Occured in dataInJson()", ex);
		}
	}

	@Override
	public String getIndividualTransactionStatus(String contractCode, String messageid, String msisdn, boolean detail,
			String login) {
		try {
			iCassandraRepository.connectDB();
			if (contractCode != null) {
				logger.info("retrieving bss-requests for contractcode : {} and messageid : {}",contractCode,messageid);
				return this.dataInJson("bss-requests",
						iCassandraRepository.getIndividualTransactionStatus(contractCode, messageid, msisdn, detail),
						true, contractCode);
			} else {
				List<String> contractCodesList = iCassandraRepository.getContractCodes(msisdn);
				if(!contractCodesList.isEmpty()) {
					logger.info("retrieving bss-requests for msisdn : {} and messageid : {}",msisdn,messageid);
					return this.dataInJson("bss-requests",
							iCassandraRepository.getIndividualTransactionStatus(contractCodesList.get(0), messageid, msisdn, detail), true, contractCodesList.get(0));
//					return this.dataInJson("requests", iCassandraRepository.getIndividualTransactionMSISDN(
//							contractCodesList.get(0), messageid, msisdn, detail), true, contractCodesList.get(0));
				}else {
					return "MSISDN not found";
				}
				

			}

		} catch (Exception ex) {
			throw new StatusQueryServiceException("Exception Occured in getIndividualTransactionStatus()", ex);
		}
	}

}
