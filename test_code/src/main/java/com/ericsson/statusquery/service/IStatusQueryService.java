package com.ericsson.statusquery.service;

import java.util.List;
import java.util.Map;

public interface IStatusQueryService {
	
	String getStatus(String tag,String login);
	
	String getIndividualTransactionStatus(String contractCode,String messageid, String msisdn,boolean detail,String login);
	
	String dataInJson(String tag,List<Map<String, Object>> list, boolean isContractCodePresent, String contractCode);

}
