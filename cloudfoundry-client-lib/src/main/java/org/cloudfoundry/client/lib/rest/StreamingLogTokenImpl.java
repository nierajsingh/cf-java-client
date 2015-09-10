package org.cloudfoundry.client.lib.rest;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.websocket.Session;

import org.cloudfoundry.client.lib.StreamingLogToken;

public class StreamingLogTokenImpl implements StreamingLogToken {
    private static long keepAliveTime = 25000; // 25 seconds to match the go client

    private Timer keepAliveTimer;

    private Session session;

    public StreamingLogTokenImpl(Session session) {
    }

    public StreamingLogTokenImpl(Session session, Timer timer) {
        this.session = session;
    	this.keepAliveTimer = timer;
        keepAliveTimer.scheduleAtFixedRate(new KeepAliveTimerTask(), keepAliveTime, keepAliveTime);
	}

	public void cancel() {
        keepAliveTimer.cancel();
        try {
            session.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    private class KeepAliveTimerTask extends TimerTask {
        @Override
        public void run() {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText("keep alive");
            } else {
                keepAliveTimer.cancel();
            }
        }
    }

    /**
     * Retrieves the timer associated with this token. The timer's lifetime is
     * tied to the log session, so its suitable for executing periodic tasks
     * that need to be executed as long as the logging session is alive.
     */
    public Timer getTimer() {
    	return keepAliveTimer;
    }

}
