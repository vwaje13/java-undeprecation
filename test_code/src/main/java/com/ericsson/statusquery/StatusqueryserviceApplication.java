package com.ericsson.statusquery;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.ericsson.statusquery.service.impl.PropertyWriter;


@SpringBootApplication
public class StatusqueryserviceApplication {

	public static void main(String[] args) {
		PropertyWriter.main(args);
		SpringApplication.run(StatusqueryserviceApplication.class, args);
	}

}
