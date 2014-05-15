package com.pdsd.pixchange;

public class BroadcastReplyMessage implements IMessage {

	@Override
	public int getMessageType() {
		return IMessageTypes.BROADCAST_REPLY_MESSAGE;
	}

}
