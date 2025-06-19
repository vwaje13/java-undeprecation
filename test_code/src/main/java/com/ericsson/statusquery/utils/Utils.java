package com.ericsson.statusquery.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import com.ericsson.statusquery.exception.StatusQueryServiceException;
import com.ericsson.statusquery.model.KubernetesProperties;


@Component
@Import(KubernetesProperties.class)
public class Utils {
	
	private static final Logger logger = LoggerFactory.getLogger("Utils");
	
	@Autowired
	private  KubernetesProperties property;
	
	public Utils() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String readableTimestamp(String epochTimestamp) {
		try {
			logger.debug("Time Zone Id in readableTimestamp : {}",property.getTimeZoneId());
			if(epochTimestamp != null && !epochTimestamp.equalsIgnoreCase("")) {
				long epochTime = Long.parseLong(epochTimestamp);
		        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochTime), ZoneId.of(property.getTimeZoneId()));
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss.SSS");
		        return dateTime.format(formatter);
			}
			
		}catch(NumberFormatException | StatusQueryServiceException ex) {
			
			logger.debug("Error occured while conversion or invalid timestamp", ex);
		}
		return epochTimestamp;
    }
	
	public String convertTimestampInJson(String jsonString) {
		try {
			logger.debug("Time Zone Id in convertTimestampInJson :  {}",property.getTimeZoneId());
			JSONObject json = new JSONObject(jsonString);
			if(json.has("eventTime")) {
				String eventTimeUTC = json.getString("eventTime");
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS zzz");
		        ZonedDateTime zonedDateTime = ZonedDateTime.parse(eventTimeUTC, formatter);
		        ZonedDateTime pstDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of(property.getTimeZoneId()));
		        DateTimeFormatter pstFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		        String eventTimePST = pstDateTime.format(pstFormatter);
		        json.put("eventTime", eventTimePST);
		        return json.toString();
			}
	        
		}catch(NumberFormatException | StatusQueryServiceException ex) {
			logger.debug("Error occured while conversion or invalid timestamp", ex);
		}
		return jsonString;
    }

}
