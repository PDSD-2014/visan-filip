package com.pdsd.pixchange;

import java.net.InetAddress;

public class BroadcastMessage implements IMessage {

	private static final long serialVersionUID = 581876106458303281L;
	protected String info = null;
	protected InetAddress ipAddress = null;
	//hash MAC address + seq number
	protected String pictureName = null;
	
	@Override
	public int getMessageType() {
		// Broadcast message type
		return IMessageTypes.BROADCAST_MESSAGE;
	}
	
	public void setInfo(String info) {
		this.info = info;
	}
	
	public String getInfo() {
		return info;
	}
	
	public void setIPAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public InetAddress getIPAddress() {
		return ipAddress;
	}
	
	public String getPictureName() {
		return pictureName;
	}
	
	public void setPictureName(String pictureName) {
		this.pictureName = pictureName;
	}

}
