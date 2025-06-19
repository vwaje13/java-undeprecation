package com.ericsson.statusquery.auth;

public class StausqueryException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private int errorCode;
	private String errorMsg;
	
	public int getErrorCode() {
		return errorCode;
	}
	public String getErrorMessage() {
		return errorMsg;
	}
	
	public StausqueryException(){
		super();
	}
	
	public StausqueryException(String ex){
		super(ex);
	}
	
	public StausqueryException(String ex, Throwable t){
		super(ex,t);
	}
	
	public StausqueryException(int errorCode, String errorMessage) {
		super(errorMessage, new Throwable());
		this.errorCode = errorCode;
		this.errorMsg = errorMessage;
	}
}
