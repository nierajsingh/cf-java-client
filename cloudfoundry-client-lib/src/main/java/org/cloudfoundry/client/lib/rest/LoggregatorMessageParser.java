package org.cloudfoundry.client.lib.rest;

import org.cloudfoundry.client.lib.domain.ApplicationLog;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import loggregator.LogMessages;

public class LoggregatorMessageParser {

	public ApplicationLog parseMessage(byte[] rawMessage) throws InvalidProtocolBufferException {
		LogMessages.Message message = LogMessages.Message.parseFrom(rawMessage);

		return createApplicationLog(message);
	}

	public ApplicationLog parseMessage(String messageString) throws InvalidProtocolBufferException, TextFormat.ParseException {
		LogMessages.Message.Builder builder = LogMessages.Message.newBuilder();
		TextFormat.merge(messageString, builder);
		LogMessages.Message message = builder.build();

		return createApplicationLog(message);
	}

	private ApplicationLog createApplicationLog(LogMessages.Message message) {
		ApplicationLog.MessageType messageType =
				message.getMessageType() == LogMessages.Message.MessageType.OUT ?
						ApplicationLog.MessageType.STDOUT :
						ApplicationLog.MessageType.STDERR;

		return new ApplicationLog(message.getAppId(),
				message.getMessage().toStringUtf8(),
				message.getTimestamp(),
				messageType,
				message.getSourceName(), message.getSourceId());
	}

}
