package com.ericsson.statusquery.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Config;

@Repository
public class UserDataLoaderService {
	private static final Logger logger = LoggerFactory.getLogger(UserDataLoaderService.class);
	private final Map<String, String> userData = new HashMap<>();
	private long lastModifiedTimestamp = 0;
	String namespace = System.getenv("POD_NAMESPACE");
	String secretName=System.getenv("SECRET_NAME_SQ");
	
	

	public UserDataLoaderService() {
		super();
		loadUserData();
	}

	// @PostConstruct
	public void loadUserData() {
		try {
			ApiClient client = Config.defaultClient();
			Configuration.setDefaultApiClient(client);
			String decodedUserData = "";
			// Create a CoreV1Api instance
			CoreV1Api api = new CoreV1Api();
			String absolutePath = "";
			// Retrieve the secret
			V1Secret secret = api.readNamespacedSecret(secretName, namespace, null, null, null);

			// Get the data from the secret
			byte[] userDataBytes = secret.getData().get("user-data.id");
			String userData = new String(userDataBytes);
			logger.info("Printing user-data.id {}", userData);
//			try {
			logger.info("User data written to file user-data.id");
			Path tempDir = Files.createTempDirectory("auth-temp");	//NOSONAR
			Path filePath = Paths.get(tempDir.toString(), "user-data.id");	//NOSONAR
			Files.write(filePath, userData.getBytes());
			// System.out.println("File written successfully.");
			byte[] bytes = Files.readAllBytes(filePath);
			String readContent = new String(bytes);
			// System.out.println("Read content:" +readContent.toString());
			loadDataFromFile(readContent);

//			} 
//			catch (IOException e) {
//				logger.error("Failed to write user data to file", e);
//			}
		} catch (IOException | ApiException e) {
			logger.debug("Failed to write user data to file", e);
		}

	}

	private void loadDataFromFile(String userDataContent) throws IOException {
		// logger.info("User data written to file user-data.id in load data file"
		// +userDataContent);
		Map<String, String> tempUserData = new HashMap<>();
		String[] userDataLines = userDataContent.split("\\r?\\n");
		for (String line : userDataLines) {
			String[] parts = line.split("==");
			if (parts.length == 2) {
				tempUserData.put(parts[0].trim(), parts[1].trim());
			}
		}
		this.userData.putAll(tempUserData);
		// logger.info("successfully written on userdata map" +userDataContent);
	}

	public Map<String, String> getUserData() {
		// logger.info("successfully return on getUserData()"+this.userData);
		return this.userData;
	}
}
