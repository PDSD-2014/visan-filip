package com.pdsd.pixchange;

public class ImageMessage implements IMessage{
	
	protected byte[] image = null;
	protected String imageName = null;
	
	public byte[] getImage() {
		return image;
	}
	public void setImage(byte[] image) {
		this.image = image;
	}
	public String getImageName() {
		return imageName;
	}
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}
	
	@Override
	public int getMessageType() {
		return IMessageTypes.IMAGE_MESSAGE;
	}

}
