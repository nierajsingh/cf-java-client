package org.cloudfoundry.client.lib.domain;


import java.util.Date;

public class ApplicationLog implements Comparable<ApplicationLog> {
	public enum MessageType {STDOUT, STDERR}

	private static final long NANOSECONDS_IN_MILLISECOND = 1000000;

	private String appId;
	private String message;
	private Date timestamp;
	private MessageType messageType;
	private String sourceName;
	private String sourceId;
	private long nanosTimestamp;

	public ApplicationLog(String appId, String message, long nanosTimestamp, MessageType messageType, String sourceName, String sourceId) {
		this.appId = appId;
		this.message = message;
		this.timestamp = new Date(nanosTimestamp / NANOSECONDS_IN_MILLISECOND);
		this.nanosTimestamp = nanosTimestamp;
		this.messageType = messageType;
		this.sourceName = sourceName;
		this.sourceId = sourceId;
	}

	public String getAppId() {
		return appId;
	}

	public String getMessage() {
		return message;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getSourceId() {
		return sourceId;
	}

	public int compareTo(ApplicationLog o) {
		return timestamp.compareTo(o.timestamp);
	}

	@Override
	public String toString() {
		return String.format("%s [%s] %s (%s, %s)", appId, timestamp, message, messageType, sourceName);
	}

	public long getNanosTimestamp() {
		return nanosTimestamp;
	}
}