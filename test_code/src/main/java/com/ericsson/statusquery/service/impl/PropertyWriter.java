package com.ericsson.statusquery.service.impl;

import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Configuration
public class PropertyWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertyWriter.class);

	@Autowired
	private Environment environment;
	String upgrademaxfilesize = System.getenv("MAXFILESIZE");
	String upgradetotalcapsize = System.getenv("CAPSIZE");
	String retention = System.getenv("RETENTION");
	//String loglevel = System.getenv("LOGLEVEL");
	public void writePropertyToFile() {
		//LOGGER.info("*********************Printing the log config *********************");
		//LOGGER.info("logging.rolling.upgrademaxfilesize = " + upgrademaxfilesize);
		//LOGGER.info("logging.rolling.retention = " + retention);
		//LOGGER.info("logging.rolling.upgradetotalcapsize = " + upgradetotalcapsize);
		//LOGGER.info("logging.level.root = " + loglevel);
		System.setProperty("logging.rolling.upgrademaxfilesize", upgrademaxfilesize);
		System.setProperty("logging.rolling.upgradetotalcapsize", upgradetotalcapsize);
		System.setProperty("logging.rolling.retention", retention);
		//System.setProperty("logging.level.root", loglevel);
	}
	public static void main(String[] args){
		//LOGGER.debug("inside property writer");
		PropertyWriter propertyWriter = new PropertyWriter();
		propertyWriter.writePropertyToFile();
}
}
