package com.pdsd.pixchange;

import java.net.InetAddress;

public class ImageMessage implements IMessage{
	
	private static final long serialVersionUID = 8555352848562315298L;
	protected byte[] photo = null;
	protected String photoName = null;
	protected InetAddress ipAddress = null;
	
	public byte[] getPhoto() {
		return photo;
	}
	public void setPhoto(byte[] image) {
		this.photo = image;
	}
	public String getPhotoName() {
		return photoName;
	}
	public void setPhotoName(String imageName) {
		this.photoName = imageName;
	}
	
	@Override
	public int getMessageType() {
		return IMessageTypes.IMAGE_MESSAGE;
	}
	
	public void setIPAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public InetAddress getIPAddress() {
		return ipAddress;
	}

}
