package com.ericsson.statusquery.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AlarmInfo {
		
	public enum ALARM_STATUS {NONE, WARNING, DOWN, MW, SHUTDOWN,THRESHOLD}
	public enum ALARM_SEVERITY {Minor, Major, Critical, Clear}
	
	private static final String ERROR_CODE="errorCode";
	private static final String OID="oid";
	
	private static final String DESCRIPTION="description";
	private static final String DESC_BROKEN="desc_broken";
	private static final String DESC_CLEAR="desc_clear";
	
	private static final Logger logger = LoggerFactory.getLogger(AlarmInfo.class);
	private AlarmInfo() {
	    throw new IllegalStateException("AlarmInfo Creation");
	  }

	public enum FAULTNAME {
		DatabaseDown,
		KafkaProducerServiceDown,
		KafkaServerDown,
		KafkaLatencyHigherthanNormalRate,
		producerDbConnectionDownOrLost
	}
	
	private static final Map<String,String> WARN_ALARM_INFO = new HashMap<>();
		static {
			WARN_ALARM_INFO.put(OID, ".1.1.1.1.30.3");
			WARN_ALARM_INFO.put(ERROR_CODE, "WarningNEThresholdBroken"); 
			WARN_ALARM_INFO.put(DESC_BROKEN, "Warning Threshold: %s of NE: %s is broken on server: %s");
			WARN_ALARM_INFO.put(DESC_CLEAR, "Clear alarm of Warning Threshold of NE: %s is broken on server: %s");
		}
	
		private static final Map<String,String> THRES_ALARM_INFO = new HashMap<>();
		static {
			THRES_ALARM_INFO.put(OID, ".1.1.1.1.30.1"); //sample value to be changed with actual
			THRES_ALARM_INFO.put(ERROR_CODE, "kafkaStorageRatio"); 
			THRES_ALARM_INFO.put(DESC_BROKEN, "EDA eric-data-message-bus-kf Storage Usage Threshold Exceeded");
			THRES_ALARM_INFO.put(DESC_CLEAR, "Clear alarm of EDA eric-data-message-bus-kf Storage Usage Threshold Exceeded");
		}
		
		private static final Map<String,String> DOWN_ALARM_INFO = new HashMap<>();
		static {
			DOWN_ALARM_INFO.put(OID, ".1.1.1.1.30.4"); //sample value to be changed with actual
			DOWN_ALARM_INFO.put(ERROR_CODE, "failedToConnectToExternalCassandra"); 
			DOWN_ALARM_INFO.put(DESC_BROKEN, "EDA eric-act-aapi Failed to Connect to External Cassandra");
			DOWN_ALARM_INFO.put(DESC_CLEAR, "Clear alarm of EDA eric-act-aapi Failed to Connect to External Cassandra");
		}

		private static final Map<String,String> MW_ALARM_INFO = new HashMap<>();
		static {
			MW_ALARM_INFO.put(OID, ".1.1.1.1.30.2");
			MW_ALARM_INFO.put(ERROR_CODE, "NEMWOpen"); 
			MW_ALARM_INFO.put(DESCRIPTION, "MW of NE: %s is %s on cluster: %s");
		}
		
		private static final Map<String,String> AOM_ALARM_INFO = new HashMap<>();
		static {
			AOM_ALARM_INFO.put(OID, ".1.1.1.1.30.5");
			AOM_ALARM_INFO.put(ERROR_CODE, "QShardNEThresholdBroken"); 
			AOM_ALARM_INFO.put(DESC_BROKEN, "QShard Threshold of %s NE (%s) is broken on cluster: %s");
			AOM_ALARM_INFO.put(DESC_CLEAR, "Clear alarm of QShard Threshold of %s NE is broken on cluster: %s");
		}
		
		private static final Map<String,String> ARH_ALARM_INFO = new HashMap<>();
		static {
			ARH_ALARM_INFO.put(OID, ".1.1.1.1.30.6");
			ARH_ALARM_INFO.put(ERROR_CODE, "ARHPartitionWaterlevelThresholdBroken"); 
			ARH_ALARM_INFO.put(DESC_BROKEN, "Partition Waterlevel Threshold of %s is broken on cluster: %s");
			ARH_ALARM_INFO.put(DESC_CLEAR, "Clear alarm of Partition waterlevel Threshold of %s is broken on cluster: %s");
		}		
			
		//Threshold
		public static String getThresAlarmOid()
		{
			return THRES_ALARM_INFO.get(OID);
		}
		
		public static String getThresAlarmErrorCode()
		{
			return THRES_ALARM_INFO.get(ERROR_CODE);
		}
		
		public static String getThresAlarmDescBroken()
		{
			return THRES_ALARM_INFO.get(DESC_BROKEN);
		}
		
		public static String getThresAlarmDescClear()
		{
			return THRES_ALARM_INFO.get(DESC_CLEAR);
		}
		
		
		// Warning
		public static String getWarnThresAlarmOid()
		{
			return WARN_ALARM_INFO.get(OID);
		}
		
		public static String getWarnThresAlarmErrorCode()
		{
			return WARN_ALARM_INFO.get(ERROR_CODE);
		}
		
		public static String getWarnThresAlarmDescBroken()
		{
			return WARN_ALARM_INFO.get(DESC_BROKEN);
		}
		
		public static String getWarnThresAlarmDescClear()
		{
			return WARN_ALARM_INFO.get(DESC_CLEAR);
		}
		
		// MW
		public static String getMwAlarmOid()
		{
			return MW_ALARM_INFO.get(OID);
		}
		
		public static String getMwAlarmErrorCode()
		{
			return MW_ALARM_INFO.get(ERROR_CODE);
		}
		
		public static String getMwAlarmDesc()
		{
			return MW_ALARM_INFO.get(DESCRIPTION);
		}
		
		// UNBAL
		public static String getUnbalThresAlarmOid()
		{
			return DOWN_ALARM_INFO.get(OID);
		}
		
		public static String getUnbalThresAlarmErrorCode()
		{
			return DOWN_ALARM_INFO.get(ERROR_CODE);
		}
		
		public static String getUnbalThresAlarmDescBroken()
		{
			return DOWN_ALARM_INFO.get(DESC_BROKEN);
		}
		
		public static String getUnbalThresAlarmDescClear()
		{
			return DOWN_ALARM_INFO.get(DESC_CLEAR);
		}
		
		// AOM
		public static String getAOMThresAlarmOid()
		{
			return AOM_ALARM_INFO.get(OID);
		}
		
		public static String getAOMThresAlarmErrorCode()
		{
			return AOM_ALARM_INFO.get(ERROR_CODE);
		}
		
		public static String getAOMThresAlarmDescBroken()
		{
			return AOM_ALARM_INFO.get(DESC_BROKEN);
		}
		
		public static String getAOMThresAlarmDescClear()
		{
			return AOM_ALARM_INFO.get(DESC_CLEAR);
		}
		
		//ARH
		public static String getARHThresAlarmOid()
		{
			return ARH_ALARM_INFO.get(OID);
		}
		
		public static String getARHThresAlarmErrorCode()
		{
			return ARH_ALARM_INFO.get(ERROR_CODE);
		}
		
		public static String getARHThresAlarmDescBroken()
		{
			return ARH_ALARM_INFO.get(DESC_BROKEN);
		}
		
		public static String getARHThresAlarmDescClear()
		{
			return ARH_ALARM_INFO.get(DESC_CLEAR);
		}
			
		/*
		 * add an alarm status
		 */
		public static String addAlarmStatus(String addStatus, String currentStatus, String thresDes) {
			String newStatus = "";
			String statusReason = addStatus + ";" + thresDes;
			logger.info("Alarminfo addAlarmStatus()  statusReason="+statusReason);
			logger.info("Alarminfo addAlarmStatus()  addStatus="+addStatus);
			logger.info("Alarminfo addAlarmStatus()  currentStatus="+currentStatus);
			logger.info("Alarminfo addAlarmStatus()  thresDes="+thresDes);
			
			if(Objects.nonNull(currentStatus) && !currentStatus.contains(addStatus)) {
				newStatus = currentStatus.equals(ALARM_STATUS.NONE.name()) ? statusReason : currentStatus + "," + statusReason;
			}	
			logger.info("Alarminfo addAlarmStatus()  newStatus="+newStatus);
			return newStatus;
		}
		
		/*
		 * remove an alarm status
		 */
		public static String removeAlarmStatus(String removeStatus, String currentStatus) {
			String newStatus = "";
			if(Objects.nonNull(currentStatus) && currentStatus.contains(removeStatus)
					&& !currentStatus.equals(ALARM_STATUS.NONE.name())) {
				
				String[] status = currentStatus.split(",");
				List<String> list = Arrays.asList(status);				
				newStatus = list.stream().filter(s-> ! s.contains(removeStatus)).collect(Collectors.joining(","));
									
				if(Objects.isNull(newStatus) || newStatus.isEmpty() ) newStatus = ALARM_STATUS.NONE.name();
			}
			
			// no new status update when it is empty
			return newStatus;
		}
}
