//package com.ericsson.statusquery.repository.impl;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import com.datastax.oss.driver.api.core.CqlSession;
//import com.datastax.oss.driver.api.core.cql.BoundStatement;
//import com.datastax.oss.driver.api.core.cql.PreparedStatement;
//import com.datastax.oss.driver.api.core.cql.ResultSet;
//import com.datastax.oss.driver.api.core.cql.Row;
//import com.ericsson.statusquery.cassandra.CassandraConnector;
//import com.ericsson.statusquery.constants.Constants;
//import com.ericsson.statusquery.model.KubernetesProperties;
//
//@SpringBootTest
//public class CassandraRepositoryImplTest {
//	
//	@Mock
//	private CqlSession session;
//	
//	@Mock
//	private CassandraConnector connector;
//	
//	@Mock
//	KubernetesProperties property;
//	
//	@InjectMocks
//	private CassandraRepositoryImpl cassandraRepository;
//	
//	@Mock
//	private CassandraRepositoryImpl cr;
//	
//	@Test
//    public void testInitializeDBProperties() {
//
//        when(property.getCassandraIpAddressSite1()).thenReturn("127.0.0.1");
//        when(property.getCassandraPortSite1()).thenReturn("9042");
//        when(property.getCassandraDatacenterSite1()).thenReturn("DC1");
//        when(property.getCassandraKeyspaceSite1()).thenReturn("keyspace1");
//        
//        when(property.getCassandraIpAddressSite2()).thenReturn("127.0.0.2");
//        when(property.getCassandraPortSite2()).thenReturn("9043");
//        when(property.getCassandraDatacenterSite2()).thenReturn("DC2");
//        when(property.getCassandraKeyspaceSite2()).thenReturn("keyspace2");
//
//        cassandraRepository.initializeDBProperties();
//
//        assertEquals("127.0.0.1", property.getCassandraIpAddressSite1());
//        assertEquals("9042", property.getCassandraPortSite1());
//        assertEquals("DC1", property.getCassandraDatacenterSite1());
//        assertEquals("keyspace1", property.getCassandraKeyspaceSite1());
//        
//        assertEquals("127.0.0.2", property.getCassandraIpAddressSite2());
//        assertEquals("9043", property.getCassandraPortSite2());
//        assertEquals("DC2", property.getCassandraDatacenterSite2());
//        assertEquals("keyspace2", property.getCassandraKeyspaceSite2());
//    }
//
//
//	@Test
//	void testConnectCassandraCluster() throws Exception{
//		String cassandraIpAddressSiteA = "127.0.0.1";
//		int cassandraPortSiteA = 9042;
//		String cassandraDatacenterSiteA ="datacenter1";
//		String cassandraKeySpaceSiteA = "poh";
//		
//		when(connector.buildSession(cassandraIpAddressSiteA, cassandraPortSiteA, cassandraDatacenterSiteA,cassandraKeySpaceSiteA)).thenReturn(session);
//		when(session.isClosed()).thenReturn(false);
//		boolean connectCassandraCluster = cassandraRepository.connectCassandraCluster(cassandraIpAddressSiteA, cassandraPortSiteA, cassandraDatacenterSiteA, cassandraKeySpaceSiteA);
//		Assertions.assertTrue(connectCassandraCluster);
//	}
//	
//	@Test
//	void testConnectCassandraCluster_failure() throws Exception{
//		String cassandraIpAddressSiteA = "127.0.0.1";
//		int cassandraPortSiteA = 9042;
//		String cassandraDatacenterSiteA ="datacenter1";
//		String cassandraKeySpaceSiteA = "poh";
//		
//		when(connector.buildSession(cassandraIpAddressSiteA, cassandraPortSiteA, cassandraDatacenterSiteA,cassandraKeySpaceSiteA)).thenThrow(new RuntimeException("Connection Failed"));
//		boolean connectCassandraCluster = cassandraRepository.connectCassandraCluster(cassandraIpAddressSiteA, cassandraPortSiteA, cassandraDatacenterSiteA, cassandraKeySpaceSiteA);
//		Assertions.assertFalse(connectCassandraCluster);
//	}
//	
//	
//	
//	@Test
//	void testConnectDB() throws Exception{
//		String cassandraIpAddressSiteA = "127.0.0.1";
//		int cassandraPortSiteA = 9042;
//		String cassandraDatacenterSiteA ="datacenter1";
//		String cassandraKeySpaceSiteA = "poh";
//		
//		doNothing().when(cr).initializeDBProperties();
//		doReturn(true).when(cr).connectCassandraCluster(cassandraIpAddressSiteA, cassandraPortSiteA, cassandraDatacenterSiteA, cassandraKeySpaceSiteA);
//		
//		String connectDB = cassandraRepository.connectDB();
//		Assertions.assertEquals(Constants.DB_SITE_1, connectDB);
//		
//	}
//	
////	@Test
//	void testConnectDB_failure() throws Exception{
//		String cassandraIpAddressSiteA = "127.0.0.1";
//		int cassandraPortSiteA = 9042;
//		String cassandraDatacenterSiteA ="datacenter1";
//		String cassandraKeySpaceSiteA = "poh";
//		
//		doNothing().when(cr).initializeDBProperties();
//		when(cr.connectCassandraCluster(cassandraIpAddressSiteA, cassandraPortSiteA, cassandraDatacenterSiteA, cassandraKeySpaceSiteA)).thenThrow(new RuntimeException("Connection Failed"));
//		
//		String connectDB = cassandraRepository.connectDB();
//		assertEquals(Constants.DB_NOT_CONNECTED, connectDB);
//		
//	}
//	
//	@Test
//	void testFailOverLogic() throws Exception{
//		String cassandraIpAddressSiteB = "127.0.0.1";
//		int cassandraPortSiteB = 9042;
//		String cassandraDatacenterSiteB ="datacenter1";
//		String cassandraKeySpaceSiteB = "poh";
//		
//		doNothing().when(cr).initializeDBProperties();
//		doReturn(true).when(cr).connectCassandraCluster(cassandraIpAddressSiteB, cassandraPortSiteB, cassandraDatacenterSiteB, cassandraKeySpaceSiteB);
//		
//		String connectDB = cassandraRepository.failOverLogic(Constants.DB_NOT_CONNECTED);
//		Assertions.assertEquals(Constants.DB_SITE_2, connectDB);
//	}
//	
////	@Test
//	void testGetKafkaBrokerStatus() throws Exception{
//		String cassandraKeySpaceSiteA = "poh";
//		String query = Constants.GET_KAFKA_BROKER_STATUS.replace("KEYSPACE", cassandraKeySpaceSiteA);
//		ResultSet mockResultSet = mock(ResultSet.class);
//		PreparedStatement  preparedStatement =Mockito.mock(PreparedStatement.class);
//		BoundStatement boundStatement = Mockito.mock(BoundStatement.class);
//		
//		Iterator<Row> mockIterator = getMockResultSetIterator();
//        when(mockResultSet.iterator()).thenReturn(mockIterator);
//        System.out.println("check : "+session.prepare(anyString()));
//        System.exit(0);
//		when(session.prepare(query)).thenReturn(preparedStatement);
//		when(preparedStatement.bind()).thenReturn(boundStatement);
//		when(session.execute(any(BoundStatement.class))).thenReturn(mockResultSet);
//
//        List<Map<String, Object>> result = cassandraRepository.getKafkaBrokerStatus();
//
//        assertEquals(2, result.size());
//
//	}
//	
////	@Test
//	void testGetCassandraHealthStatus() {
//		String cassandraKeySpaceSiteA = "poh";
//		String query = Constants.GET_CASSANDRA_HEALTH_STATUS.replace("KEYSPACE", cassandraKeySpaceSiteA);;
//		ResultSet mockResultSet = mock(ResultSet.class);
//		PreparedStatement  preparedStatement =Mockito.mock(PreparedStatement.class);
//		BoundStatement boundStatement = Mockito.mock(BoundStatement.class);
//		
//		Iterator<Row> mockIterator = getMockResultSetIterator();
//        when(mockResultSet.iterator()).thenReturn(mockIterator);
//
//		when(session.prepare(query)).thenReturn(preparedStatement);
//		when(preparedStatement.bind()).thenReturn(boundStatement);
//		when(session.execute(any(BoundStatement.class))).thenReturn(mockResultSet);
//
//        List<Map<String, Object>> result = cassandraRepository.getCassandraHealthStatus();
//
//        assertEquals(2, result.size());
//	}
//	
//	
//	private Iterator<Row> getMockResultSetIterator() {
//        List<Row> rows = new ArrayList<>();
//        rows.add(getMockRow("namespace1", "broker1", "ip1", 1));
//        rows.add(getMockRow("namespace2", "broker2", "ip2", 0));
//        return rows.iterator();
//    }
//
//    private Row getMockRow(String namespace, String brokerName, String ipLb, int status) {
//        Row mockRow = mock(Row.class);
//        when(mockRow.getString(Constants.NAMESPACE)).thenReturn(namespace);
//        when(mockRow.getString(Constants.BROKER_NAME)).thenReturn(brokerName);
//        when(mockRow.getString(Constants.IP_LB)).thenReturn(ipLb);
//        when(mockRow.getInt(Constants.STATUS)).thenReturn(status);
//        return mockRow;
//    }
//
//
//}
