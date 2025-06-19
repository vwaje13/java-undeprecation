package com.ericsson.statusquery.model;

public class AlarmModel {
private String faultName;
private String serviceName;
private String createdAt;
private String faultyResource;
private String severity;
private String description;
private String expiration;
private String additionalInformation;


public String getFaultName() {
	return faultName;
}
public void setFaultName(String faultName) {
	this.faultName = faultName;
}
public String getServiceName() {
	return serviceName;
}
public void setServiceName(String serviceName) {
	this.serviceName = serviceName;
}
public String getCreatedAt() {
	return createdAt;
}
public void setCreatedAt(String createdAt) {
	this.createdAt = createdAt;
}
public String getFaultyResource() {
	return faultyResource;
}
public void setFaultyResource(String faultyResource) {
	this.faultyResource = faultyResource;
}
public String getSeverity() {
	return severity;
}
public void setSeverity(String severity) {
	this.severity = severity;
}
public String getDescription() {
	return description;
}
public void setDescription(String description) {
	this.description = description;
}
public String getExpiration() {
	return expiration;
}
public void setExpiration(String expiration) {
	this.expiration = expiration;
}
public String getAdditionalInformation() {
	return additionalInformation;
}
public void setAdditionalInformation(String additionalInformation) {
	this.additionalInformation = additionalInformation;
}
@Override
public String toString() {
	return "AlarmModel [faultName=" + faultName + ", serviceName=" + serviceName + ", createdAt=" + createdAt
			+ ", faultyResource=" + faultyResource + ", severity=" + severity + ", description=" + description
			+ ", expiration=" + expiration + ", additionalInformation=" + additionalInformation + "]";
}

}
