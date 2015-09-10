package org.cloudfoundry.client.lib.util;

import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.util.MessageSorter.FlushTask;
import org.springframework.util.Assert;

/**
 * Keeps messages in a buffer for a minimum amount of time and sorts them by date,
 * before passing them to an log listener.
 * <p>
 * This is needed because 'bursty' groups of message may arrive out of order.
 *
 * @author Kris De Volder
 */
public class MessageSorter {

	/**
	 * Time in millis that messages must be held in the buffer before they
	 * are allowed to be flushed.
	 */
	public final long HOLD_WINDOW = 1000; //messages held for 1 second

	/**
	 * Period in ms at which the 'Flush' task is to be executed. Generally this
	 * number should be smaller or equal to HOLD_WINDOW.
	 */
	public final long FLUSH_PERIOD = 200;

	private final ApplicationLogListener listener;

	private final PriorityQueue<Entry> queue = new PriorityQueue<>();

	private final Timer timer;

	private FlushTask flushTask;

	public class FlushTask extends TimerTask {
		@Override
		public void run() {
			flush();
		}
	}

	private class Entry implements Comparable<Entry> {
		/**
		 * Local system time the Entry was created. Used to ensure messages
		 * stay in the queue for a minimum amount of time.
		 */
		final long arrivalTime;
		final ApplicationLog msg;

		Entry(ApplicationLog msg) {
			arrivalTime = System.currentTimeMillis();
			this.msg = msg;
		}

		@Override
		public int compareTo(Entry other) {
			return Long.compare(this.msg.getNanosTimestamp(), other.msg.getNanosTimestamp());
		}
	}

	public MessageSorter(Timer timer, ApplicationLogListener listener) {
		Assert.isTrue(timer!=null);
		this.listener = listener;
		this.timer = timer;
	}

	public synchronized void onMessage(ApplicationLog msg) {
		queue.add(new Entry(msg));
		if (flushTask == null) {
			timer.scheduleAtFixedRate(flushTask=new FlushTask(), HOLD_WINDOW, FLUSH_PERIOD);
		}
	}

	/**
	 * Flush all leading messages that have been held long enough from the queue.
	 */
	private void flush() {
		Entry e;
		while (null!=(e=fetch())) {
			listener.onMessage(e.msg);
		}
	}

	/**
	 * Fetch the first message in the buffer that is old enough to be flushed.
	 * @return Entry or null if no such message is available.
	 */
	private synchronized Entry fetch() {
		if (!queue.isEmpty()) {
			Entry e = queue.peek();
			if (isOldEnough(e)) {
				queue.remove();
				return e;
			}
		}
		return null;
	}

	private boolean isOldEnough(Entry e) {
		long age = System.currentTimeMillis() - e.arrivalTime;
		return age >= HOLD_WINDOW;
	}


}
