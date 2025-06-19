//package com.ericsson.statusquery.service.impl;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.when;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.junit.Before;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import com.ericsson.statusquery.constants.Constants;
//import com.ericsson.statusquery.repository.impl.CassandraRepositoryImpl;
//
//@SpringBootTest
//public class StatusQueryServiceImplTest {
//	
//	@Mock
//	private CassandraRepositoryImpl cassandraRepository;
//	
//	@InjectMocks
//	private StatusQueryServiceImpl statusQueryServiceImpl;
//	
//	@Mock
//	private StatusQueryServiceImpl sqs;
//	
//	public List<Map<String, Object>> resultSetList = new ArrayList<>();
//	
//	@Before
//	public void setUp() {
//
//        Map<String, Object> row1 = new HashMap<>();
//        row1.put(Constants.NAMESPACE, "namespace1");
//        row1.put(Constants.BROKER_NAME, "broker1");
//        row1.put(Constants.IP_LB, "192.168.0.1");
//        row1.put(Constants.STATUS, 1);
//        resultSetList.add(row1);
//
//        Map<String, Object> row2 = new HashMap<>();
//        row2.put(Constants.NAMESPACE, "namespace2");
//        row2.put(Constants.BROKER_NAME, "broker2");
//        row2.put(Constants.IP_LB, "192.168.0.2");
//        row2.put(Constants.STATUS, 0);
//        resultSetList.add(row2);
//    }
//
//	@Test
//	void testGetStatus() {
//		
//		when(cassandraRepository.getKafkaBrokerStatus()).thenReturn(resultSetList);
//		when(sqs.dataInJson(Constants.KAFKA_STATUS, resultSetList, false, "")).thenReturn("kafka_status");
//
//        String result = statusQueryServiceImpl.getStatus(Constants.KAFKASTATUS,"Authenticated successfully");
//
//        assertEquals("kafka_status", result);
//	}


//	void testGetStatus_kafka() {
//		
//		when(cassandraRepository.getKafkaBrokerStatus()).thenReturn(resultSetList);
//		when(sqs.dataInJson(Constants.KAFKA_STATUS, null, false, "")).thenReturn("{\r\n"
//				+ "  \"kafka_status\" : [ ]\r\n"
//				+ "}");
//		when(cassandraRepository.connectDB()).thenReturn(Constants.DB_SITE_1);
//        String result = statusQueryServiceImpl.getStatus(Constants.KAFKASTATUS);
//        assertEquals("{\r\n"
//        		+ "  \"kafka_status\" : [ ]\r\n"
//        		+ "}", result);
//	}
//	

//	@Test
//	void testGetStatus_cassandra() {
//		
//		when(cassandraRepository.getKafkaBrokerStatus()).thenReturn(resultSetList);
//		when(sqs.dataInJson(Constants.CASSANDRA_STATUS, null, false, "")).thenReturn("{\r\n"
//				+ "  \"cassandra_health_status\" : [ ]\r\n"
//				+ "}");
//		when(cassandraRepository.connectDB()).thenReturn(Constants.DB_SITE_1);
//        String result = statusQueryServiceImpl.getStatus(Constants.CASSANDRASTATUS);
//        assertEquals("{\r\n"
//        		+ "  \"cassandra_health_status\" : [ ]\r\n"
//        		+ "}", result);
//	}
//	
//	@Test
//	void testGetStatus_npecluster() {
//		
//		when(cassandraRepository.getKafkaBrokerStatus()).thenReturn(resultSetList);
//		when(sqs.dataInJson(Constants.NPECLUSTER_STATUS, null, false, "")).thenReturn("{\r\n"
//				+ "  \"npecluster_status\" : [ ]\r\n"
//				+ "}");
//		when(cassandraRepository.connectDB()).thenReturn(Constants.DB_SITE_1);
//        String result = statusQueryServiceImpl.getStatus(Constants.NPECLUSTERSTATUS);
//        assertEquals("{\r\n"
//        		+ "  \"npecluster_status\" : [ ]\r\n"
//        		+ "}", result);
//	}
//	
//	@Test
//	void testGetStatus_activealarms() {
//		
//		when(cassandraRepository.getKafkaBrokerStatus()).thenReturn(resultSetList);
//		when(sqs.dataInJson(Constants.ACTIVE_ALARMS, null, false, "")).thenReturn("{\r\n"
//				+ "  \"active_alarms\" : [ ]\r\n"
//				+ "}");
//		when(cassandraRepository.connectDB()).thenReturn(Constants.DB_SITE_1);
//        String result = statusQueryServiceImpl.getStatus(Constants.ACTIVEALARMS);
//        assertEquals("{\r\n"
//        		+ "  \"active_alarms\" : [ ]\r\n"
//        		+ "}", result);
//	}
//	
//	@Test
//	void testGetStatus_kafkaproducer() {
//		
//		when(cassandraRepository.getKafkaBrokerStatus()).thenReturn(resultSetList);
//		when(sqs.dataInJson(Constants.KAFKA_PRODUCER_STATUS, null, false, "")).thenReturn("{\r\n"
//				+ "  \"kafka_producer_status\" : [ ]\r\n"
//				+ "}");
//		when(cassandraRepository.connectDB()).thenReturn(Constants.DB_SITE_1);
//        String result = statusQueryServiceImpl.getStatus(Constants.KAFKAPRODUCERSTATUS);
//        assertEquals("{\r\n"
//        		+ "  \"kafka_producer_status\" : [ ]\r\n"
//        		+ "}", result);
//	}
//
//	@Test
//	void testDataInJson_withContractCode() {
//
//        List<Map<String, Object>> list = new ArrayList<>();
//        Map<String, Object> map = new HashMap<>();
//        map.put("key1", "value1");
//        list.add(map);
//        boolean isContractCodePresent = true;
//        String contractCode = "XYZ123";
//
//        String jsonResult = statusQueryServiceImpl.dataInJson("tag", list, isContractCodePresent, contractCode);
//        assertEquals("{\r\n"
//        		+ "  \"subid\" : \"XYZ123\",\r\n"
//        		+ "  \"tag\" : [ {\r\n"
//        		+ "    \"key1\" : \"value1\"\r\n"
//        		+ "  } ]\r\n"
//        		+ "}", jsonResult);
//	}
//	
//	@Test
//	void testDataInJson_withoutContractCode() {
//
//        List<Map<String, Object>> list = new ArrayList<>();
//        Map<String, Object> map = new HashMap<>();
//        map.put("key1", "value1");
//        list.add(map);
//        boolean isContractCodePresent = false;
//        String contractCode = "XYZ123";
//
//        String jsonResult = statusQueryServiceImpl.dataInJson("tag", list, isContractCodePresent, contractCode);
//        assertEquals("{\r\n"
//        		+ "  \"tag\" : [ {\r\n"
//        		+ "    \"key1\" : \"value1\"\r\n"
//        		+ "  } ]\r\n"
//        		+ "}", jsonResult);
//	}
//
////	@Test
//	void testGetIndividualTransactionStatus() {
//
//	}
//
//}
