package com.ericsson.statusquery.controller;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.statusquery.exception.StatusQueryServiceException;
import com.ericsson.statusquery.repository.ICassandraRepository;
import com.ericsson.statusquery.service.IStatusQueryService;
import com.ericsson.statusquery.service.impl.AuthenticationService;
@RestController
@RequestMapping("/monitor")
public class StatusQueryController {

	private static final Logger logger = LoggerFactory.getLogger("StatusQueryController");

	@Autowired
	private IStatusQueryService iStatusQueryService;

	@Autowired
	private final AuthenticationService authenticationService;

	@GetMapping("/status")
	public String getStatus() {
		return "ok";
	}

	public StatusQueryController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@GetMapping("/lateststatusreport/query/{status}")
	public ResponseEntity<String> statusDetails(@PathVariable String status) {
		try {
			logger.info("Authenticated Successfully");
			String[] statusArray = status.split(",");
			List<String> responseList = new ArrayList<>();
			for (String individualStatus : statusArray) {
				logger.info("Retrieving health status for {}", individualStatus);
				String response = iStatusQueryService.getStatus(individualStatus, "Authenticated successfully");
				if (response != null) {
					logger.info("Response successfully retrieved");
					responseList.add(response);
				}
			}
			String response = responseList.toString();
			if (response.startsWith("[") && response.endsWith("]")) {
				response = response.substring(1, response.length() - 1);
	        }
			logger.info("Response : {}",response);
			if (!responseList.isEmpty()) {
				return ResponseEntity.ok(response);
			} else {
				logger.info("returning Bad Request");
				return ResponseEntity.badRequest().build();
			}
		} 

		catch (Exception ex) {
			throw new StatusQueryServiceException("Exception Occured in StatusDetails()", ex);
		}
	}

	@GetMapping("/status/individualTransaction/query")
	public ResponseEntity<String> getIndividualTransactionStatusOnContractCode(
			@RequestParam(name = "subId", required = false) String subid,
			@RequestParam(name = "msisdn", required = false) String msisdn,
			@RequestParam(name = "messageid", required = false) String messageid,
			@RequestParam(name = "detail", required = false, defaultValue = "false") boolean detail) {
		try {
			logger.info("Authenticated Successfully");

			if (subid != null) {
				logger.info("Retrieving transaction status for subid : {}", subid);
				String response = iStatusQueryService.getIndividualTransactionStatus(subid, messageid, msisdn, detail,"Authenticated successfully");
				if (response != null) {
					logger.info("Response successfully retrieved for contractcode : {}", subid);
					return ResponseEntity.ok(response);
				} else {
					logger.info("Response recieved is null");
					return ResponseEntity.badRequest().build();
				}
			} else if (msisdn != null) {
				logger.info("Retrieving transaction status for msisdn : {}", msisdn);
				String response = iStatusQueryService.getIndividualTransactionStatus(subid, messageid, msisdn, detail,"Authenticated successfully");
				if (response != null) {
					logger.info("Response successfully retrieved for msisdn : {}", msisdn);
					return ResponseEntity.ok(response);
				} else {
					logger.info("Response recieved is null");
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
				}
			}

			else {
				logger.info("Neither subid nor msisdn provided");
				return ResponseEntity.badRequest().body("Neither subid nor msisdn provided");
			}
		} catch (Exception ex) {
			throw new StatusQueryServiceException("Exception Occured in getIndividualTransactionStatus()", ex);
		}
	}

}
