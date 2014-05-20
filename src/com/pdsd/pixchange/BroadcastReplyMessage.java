package com.pdsd.pixchange;

import java.net.InetAddress;

public class BroadcastReplyMessage implements IMessage {


	private static final long serialVersionUID = 1L;
	protected String macAddress = null;
	protected InetAddress ipAddress = null;
	
	
	@Override
	public int getMessageType() {
		return IMessageTypes.BROADCAST_REPLY_MESSAGE;
	}
	
	public String getMACAddress() {
		return macAddress;
	}
	
	public void setMACAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	public void setIPAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public InetAddress getIPAddress() {
		return ipAddress;
	}
}
