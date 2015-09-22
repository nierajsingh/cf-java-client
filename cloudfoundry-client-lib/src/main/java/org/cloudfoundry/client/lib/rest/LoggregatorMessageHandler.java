package org.cloudfoundry.client.lib.rest;

import javax.websocket.MessageHandler;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.util.MessageSorter;

import com.google.protobuf.InvalidProtocolBufferException;

public class LoggregatorMessageHandler implements MessageHandler.Whole<byte[]> {

	private final LoggregatorMessageParser messageParser;
	private final ApplicationLogListener listener;
	private final MessageSorter messageSorter;

	public LoggregatorMessageHandler(ApplicationLogListener listener) {
		this.listener = listener;
		this.messageParser = new LoggregatorMessageParser();
		this.messageSorter = new MessageSorter(listener);
	}

	public void onMessage(byte[] rawMessage) {
		try {
			messageParser.parseMessage(rawMessage);
			messageSorter.onMessage(messageParser.parseMessage(rawMessage));
		} catch (InvalidProtocolBufferException e) {
			listener.onError(e);
		}
	}
}
