package locker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author Kors
 */
public class EntityLocker<T> {

	private final Map<T, ReentrantLock> locks = new HashMap<>();

	public <R> R modifyObject(T id, Supplier<R> action) {
		synchronized (getLock(id)) {
			return action.get();
		}
	}

	private ReentrantLock getLock(T id) {
		synchronized (locks) {
			if (locks.containsKey(id))
				return locks.get(id);
			else {
				ReentrantLock newLock = new ReentrantLock();
				locks.put(id, newLock);
				return newLock;
			}
		}
	}

}
