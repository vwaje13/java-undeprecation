package com.ericsson.statusquery.auth;

public class RequestValidationException extends Exception{
        private String errMessage;

        public RequestValidationException() {
            super();
        }

    public RequestValidationException(String errorMessage) {
        super(errorMessage);
    }

    public String getErrMessage() {
            return errMessage;
        }
        public void setErrMessage(String errMessage) {
            this.errMessage = errMessage;
        }

}
