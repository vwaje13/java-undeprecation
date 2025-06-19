package com.ericsson.statusquery.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.yetus.audience.InterfaceAudience.Private;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UtilsTest {
	
	@Autowired
	private Utils util;
	
	@Test
    public void testReadableTimestamp() {
        String epochTimestamp = "1707895109942";
        String expectedDateTime = "2024-02-13 23:18:29.942";
        
        String actualDateTime = util.readableTimestamp(epochTimestamp);
        
        assertEquals(expectedDateTime, actualDateTime);
    }

}
