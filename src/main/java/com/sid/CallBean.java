package com.sid;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Call")
public class CallBean {
    private String message;
	private String ip;
	private String port;

    public String getMessage() {
		return message;
    }

    public void setMessage(String message) {
		this.message = message;
    }
	
	public String getIp() {
		return ip;
    }

    public void setIp(String ip) {
		this.ip = ip;
    }
	
	public String getPort() {
		return port;
    }

    public void setPort(String port) {
		this.port = port;
    }
}
