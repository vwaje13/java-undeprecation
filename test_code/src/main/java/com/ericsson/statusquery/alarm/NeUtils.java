package com.ericsson.statusquery.alarm;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ericsson.statusquery.model.AlarmModel;
import com.ericsson.statusquery.model.KubernetesProperties;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class NeUtils {

    private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	KubernetesProperties kubernetesProperties;

	@Value("${alarm.service.name}")
	private String alarmserviceName;
	//@Value("${spring.application.name}")
	//private String resourceName;
	@Value("${alarm.cacert.bundle.pem}")
    private String cacertPath;

    @Value("${alarm.cert.pem}")
    private String certPath;

    @Value("${alarm.key.pem}")
    private String keyPath;

	@Value("${alarm.url}")
	private String alarmUrl;
	
	@Value("${faulty.resource}")
	private String faultyResource;
	
	private static final Logger logger = LoggerFactory.getLogger(NeUtils.class);

	public boolean sendRestAlarm(String severity, String faultName, String msg) {
		logger.info("NeUtils.java sendRestAlarm()   Initiated");

		String currentTime = ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT );
		String namespace = System.getenv("POD_NAMESPACE");
		AlarmModel alarmModel = new AlarmModel();
		alarmModel.setServiceName(alarmserviceName);
//		alarmModel.setServiceName(kubernetesProperties.getAlarmserviceName());
		alarmModel.setFaultName(faultName);
//		alarmModel.setFaultyResource(kubernetesProperties.getFaultyResource()+","+namespace);
		alarmModel.setFaultyResource(faultyResource+","+namespace);
		alarmModel.setSeverity(severity);
		alarmModel.setCreatedAt(currentTime);

		alarmModel.setDescription(msg);

			logger.info("NeUtils.java sendRestAlarm()   Initiated rest AlarmModel=" + alarmModel.toString());
			logger.debug("NeUtils.java sendRestAlarm()   Triggering rest API rest ");

			try {

				String curlCommand = "curl";
				ObjectMapper objectMapper = new ObjectMapper();

				List<String> curlArguments = Arrays.asList(
						"-i",
//							"https://"+kubernetesProperties.getAlarmUrl()+"/alarm-handler/v1/fault-indications",
						"https://"+alarmUrl+"/alarm-handler/v1/fault-indications",
						"--cacert", cacertPath,
						"--cert", certPath,
						"--key", keyPath,
						"-d", objectMapper.writeValueAsString(alarmModel)
				);

				ProcessBuilder processBuilder = new ProcessBuilder();
				processBuilder.command(curlCommand, "--silent", "--show-error", "--request", "POST");
				processBuilder.command().addAll(curlArguments);
				logger.info("curlArguments {}{}",curlCommand, curlArguments);
				// Start the process
				Process process = processBuilder.start();
				// Read and print the output of the process
				InputStream inputStream = process.getInputStream();
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader reader = new BufferedReader(inputStreamReader);

				String line = null;
				StringBuilder stringBuilder = new StringBuilder();

				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
				}

				int exitCode = process.waitFor();
				logger.debug("Exit Code: " + exitCode);
				if (exitCode == 0 && stringBuilder.toString().contains("204")) {
					logger.info("Alarm send successful");
					logger.info("Response {}", stringBuilder.toString()); // Log the accumulated lines
					return true;
				} else {
					logger.info("Alarm send failed");
					logger.info("Response {}", stringBuilder.toString());
					return false;
				}

			} catch (InterruptedException e) {
			    Thread.currentThread().interrupt(); // Re-interrupting the Thread
			    logger.error("AlarmHandler false");
				logger.error("Exception occurred while raising alarm ", e);
				return false;
			} catch (Exception e) {
				logger.error("AlarmHandler false");
				logger.error("Exception occurred while raising alarm ", e);
				return false;
			}

		}
}
