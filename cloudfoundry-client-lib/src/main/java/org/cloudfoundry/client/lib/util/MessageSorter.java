package org.cloudfoundry.client.lib.util;

import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.domain.ApplicationLog;

/**
 * A wrapper around an ApplicationLogListener that keeps messages in a buffer for a minimum
 * amount of time and sorts them by timestamp, before passing them to the wrapped log listener.
 * <p>
 * This is needed because 'bursty' groups of messages may arrive out of order.
 *
 * @author Kris De Volder
 */
public class MessageSorter implements ApplicationLogListener {

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

	private CompletionEvent completionEvent;

	public class FlushTask extends TimerTask {
		@Override
		public void run() {
			flush();
		}
	}

	private interface CompletionEvent {
		void notifyListener();
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

	public MessageSorter(ApplicationLogListener listener) {
		this.listener = listener;
		this.timer = new Timer("MessageSorter Timer", true);
	}

	public synchronized void onMessage(ApplicationLog msg) {
		if (!isComplete()) {
			queue.add(new Entry(msg));
			ensureFlushTask();
		}
	}

	private void ensureFlushTask() {
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
		synchronized (this) {
			if (queue.isEmpty()) {
				if (isComplete()) {
					//No more work... and no more work should arrive because we already
					//received a 'onComplete' event.
					timer.cancel();
					flushTask = null;
					completionEvent.notifyListener();
				} else {
					//Nothing more to do for now. Cancel flush task until its needed again.
					flushTask.cancel();
					flushTask = null;
				}
			}
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

	/**
	 * @return true if either an onError or onComplete has been observed (meaning that no more data
	 * is expected.
	 */
	private synchronized boolean isComplete() {
		return completionEvent!=null;
	}

	@Override
	public synchronized void onComplete() {
		if (!isComplete()) {
			completionEvent = new CompletionEvent() {
				@Override
				public final void notifyListener() {
					listener.onComplete();
				}
			};
			ensureFlushTask();
		}
	}

	@Override
	public synchronized void onError(final Throwable exception) {
		if (!isComplete()) {
			completionEvent = new CompletionEvent() {
				@Override
				public void notifyListener() {
					listener.onError(exception);
				}
			};
			ensureFlushTask();
		}
	}
}
