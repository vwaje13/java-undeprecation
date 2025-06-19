package com.ericsson.statusquery.exception;

public class StatusQueryServiceException extends RuntimeException{
	
private static final long serialVersionUID = 1L;
	
	private int errorCode;
	private String errorMsg;
	
	public int getErrorCode() {
		return errorCode;
	}
	public String getErrorMessage() {
		return errorMsg;
	}
	
	public StatusQueryServiceException(String ex){
		super(ex);
	}
	public StatusQueryServiceException(String message, Exception ex) {
		super(message, ex);
	}
	public StatusQueryServiceException(int errorCode, String errorMessage) {
		super(errorMessage, new Throwable());
	}

}
