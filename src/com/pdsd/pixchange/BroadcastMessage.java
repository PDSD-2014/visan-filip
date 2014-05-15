package com.pdsd.pixchange;

import java.net.InetAddress;

public class BroadcastMessage implements IMessage {

	private static final long serialVersionUID = 581876106458303281L;
	protected String info = null;
	protected InetAddress address = null;
	
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
	
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	public InetAddress getAddress() {
		return address;
	}

}
