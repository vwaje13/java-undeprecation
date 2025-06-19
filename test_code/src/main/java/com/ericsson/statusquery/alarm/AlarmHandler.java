package com.ericsson.statusquery.alarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlarmHandler {


	@Autowired
    private NeUtils  neUtils;

//	@Value("${alarmfaultName}")
//	private String alarmfaultName;
//	
//	@Value("${alarmserviceName}")
//	private String alarmserviceName;
	
	private static final Logger logger = LoggerFactory.getLogger(AlarmHandler.class);

	
	public boolean raiseAlarm( String faultName, String severity, String msg)
	{  
		boolean status;
		
		status = raise(faultName, msg,severity);

		logger.info("Alarm Handler raiseAlarm: EXiting with status="+status);
		return status;
	}

	public boolean raise(String faultName, String alarmMsg,String severity)
	{		// For eda2 severity=Minor
		return sendAlarm(severity, faultName, alarmMsg);
	}
	
	public boolean clear(String eventId, String alarmMsg)
	{
		//For eda2 severity=Clear
		return sendAlarm("Clear", eventId, alarmMsg);
	}
	
	public boolean sendAlarm(String severity, String faultName, String msg)
	{  
		logger.info("AlarmHandler sendAlarm()   faultName="+faultName);
		try {
        	
        	boolean resp = neUtils.sendRestAlarm(severity,faultName, msg);
   		 	
            logger.debug("Alarm sent response: [{}]", resp);
            if(!resp) {
            	logger.debug("Error: Alarm sent failed: [{}]", resp);
            	return false;
            }
            
            return true;

		}
		catch(Exception e) {
			if(logger.isDebugEnabled())
			logger.error("Alarm sent exception: {}", e.getMessage());
		}
		
		return false;
	}
}
