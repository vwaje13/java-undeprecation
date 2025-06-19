package com.ericsson.statusquery.auth;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public enum RestImplError {
    REQUEST_RERSERVED_LINEINFO_FORMATERROR(
            20020,
            " should not be empty in lineInfo object"),
    REQUEST_BLACKLIST_INVALIDURL(
            20020,
            " Provisioning URI is invalid."),
    /** account Info **/
    REQUEST_RERSERVED_ACCOUNTNFO_FORMATERROR(
            20020,
            " should not be empty in accountInfo object"),
    /** customer Info **/
    REQUEST_RERSERVED_CUSTOMERINFO_FORMATERROR(
            20020,
            " should not be empty in customerInfo object"),
    REQUEST_SCOPE_FORMATERROR(20020,"scope should contain only PGW (and/or) NAP (and/or) CDB (and/or) VMAS (and/or) IPM (and/or) IAM (and/or) DDI (and/or) WSGPNB (and/or) OPM. Multiple NEs should be separated by comma(,) in between them. "),

	REQUEST_SUBSTATUSREASONCODETYPE_EMPTY(
           20020, "subStatusReasonCode parameter values should NOT be empty or null in lineInfo object for IOTCC"), 
	
	
	REQUEST_BRANDANDSUBBRAND_FORMATERROR(
            20023, "The Subscriber is not belonging to this source %1$s "),
			
	REQUEST_SUBSCRIBER_NOT_BELONGS_FORMATERROR(
			20023,"The Subscriber is not belonging to "),	
	 REQUEST_WHITEBLACKLIST_LINEINFO_PARAMETERERROR(
	            20020,
	            " currentReason should not be empty in lineInfo object"),
	    
		
;
	
	
	
    private int errorCode;
    private String errorMsg;
    private String originalErrorMsg;

    RestImplError(int errorCode, String errorDesc) {
        this.errorCode = errorCode;
        this.errorMsg = errorDesc;
        this.originalErrorMsg = this.errorMsg;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String msg) {
        this.originalErrorMsg = this.errorMsg;

        errorMsg = msg;
    }

    @Override
    public String toString() {
        return "RestImplError{" + "errorCode=" + errorCode + ", errorMsg='"
                + errorMsg + '\'' + '}';
    }
}
