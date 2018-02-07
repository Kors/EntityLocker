package locker;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Reusable utility class that provides synchronization mechanism similar to row-level DB locking.
 *
 * @param <T> type of id
 * @author Kors
 */
public class EntityLocker<T> {

    private final ConcurrentMap<T, CountLocker> locks = new ConcurrentHashMap<>();

    /**
     * Method execute Supplier and return result, synchronized by id.
     * Default time waiting for lock - 10 seconds.
     *
     * @see EntityLocker<>.modifyObject(T id, Supplier<R> action, long timeout)
     */
    public <R> Optional<R> modifyObject(T id, Supplier<R> action) {
        return modifyObject(id, action, 10);
    }

    /**
     * Method execute Supplier and return result, synchronized by id.
     * Allow the caller to specify timeout for locking an entity.
     *
     * @param id      id for synchronization
     * @param action  operations that will be done synchronized
     * @param timeout max time limit to wait lock
     */
    public <R> Optional<R> modifyObject(T id, Supplier<R> action, long timeout) {
        CountLocker cl = getCountLocker(id);
        ReentrantLock lock = cl.lock;
        try {
            lock.tryLock(timeout, TimeUnit.SECONDS);
            return Optional.ofNullable(action.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
            cl.releaseLocker();
            locks.computeIfPresent(id, (key, val) -> val.isInUse() ? val : null);
        }
    }

    private CountLocker getCountLocker(T id) {
        return locks.compute(id, (k, cl) -> cl == null ? new CountLocker() : cl.useCountLocker());
    }

    private class CountLocker {
        private volatile int inUseCounter = 1;
        final ReentrantLock lock = new ReentrantLock();

        synchronized CountLocker useCountLocker() {
            inUseCounter++;
            return this;
        }

        synchronized void releaseLocker() {
            inUseCounter--;
        }

        boolean isInUse() {
            return inUseCounter > 0;
        }
    }
}
