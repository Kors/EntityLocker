package locker;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author Kors
 */
public class EntityLocker<T> {

    private final ConcurrentMap<T, ReentrantLock> locks = new ConcurrentHashMap<>();

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
        return locks.computeIfAbsent(id, lock -> new ReentrantLock());
    }

}
