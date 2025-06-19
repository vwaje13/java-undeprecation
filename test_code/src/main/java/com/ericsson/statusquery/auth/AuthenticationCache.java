package com.ericsson.statusquery.auth;

public class AuthenticationCache {
	private String user;
	private String password;

	public AuthenticationCache(String user, String password, long time) {
		super();
		this.user = user;
		this.password = password;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}


}
