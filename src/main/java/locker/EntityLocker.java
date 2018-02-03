package locker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author Kors
 */
public class EntityLocker<T> {

    private final Map<T, ReentrantLock> locks = new HashMap<>();

    public <R> Optional<R> modifyObject(T id, Supplier<R> action) {
        return modifyObject(id, action, 10);
    }

    public <R> Optional<R> modifyObject(T id, Supplier<R> action, long timeout) {
        ReentrantLock lock = getLock(id);
        try {
            lock.tryLock(timeout, TimeUnit.SECONDS);
            return Optional.ofNullable(action.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
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
