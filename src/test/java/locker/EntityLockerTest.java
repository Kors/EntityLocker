package locker;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kors
 */
class EntityLockerTest {

    @Test
    @DisplayName("One lock for each new id")
    void modifyObjects_locksCreated() throws Exception {
        EntityLocker<Double> el = new EntityLocker<>();

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        List<Future<Optional<Double>>> futures = new ArrayList<>();
        for (double d = 0.1; d < 10; d += 0.1) {
            final Double key = d;
            for (int i = 0; i < 3; i++) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> el.modifyObject(key, () -> key),
                        threadPool
                ));
            }
        }
        for (Future<Optional<Double>> future : futures) {
            future.get();
        }

        Field locksMapField = el.getClass().getDeclaredField("locks");
        locksMapField.setAccessible(true);
        assertEquals(100,
                ((ConcurrentMap) locksMapField.get(el)).size());
    }

    @Test
    @DisplayName("Simple object modification")
    void modifyObject_objectSuccessfullyModified() {
        EntityLocker<String> el = new EntityLocker<>();

        MyTestObj myObj = new MyTestObj();
        Optional<MyTestObj> modified = el.modifyObject("lock", () -> {
            myObj.val++;
            return myObj;
        });

        assertTrue(modified.isPresent());
        assertEquals(myObj, modified.get());
        assertEquals(1, myObj.val);
    }

    @Test
    @DisplayName("Multithreading object modification")
    void modifyObjectInThreads_objectModifiedCorrectly() throws Exception {
        EntityLocker<String> el = new EntityLocker<>();

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        MyTestObj myObj = new MyTestObj();
        List<Future<Optional<MyTestObj>>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> el.modifyObject("lock", () -> {
                        int val = myObj.val;
                        trySleep(10);
                        myObj.val = val + 1;
                        return myObj;
                    }),
                    threadPool
            ));
        }
        for (Future<Optional<MyTestObj>> future : futures) {
            future.get();
        }

        assertEquals(100, myObj.val);
    }

    private void trySleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //    EntityLocker should allow concurrent execution of protected code on different entities
    @Test
    @DisplayName("Threads with different objects could run independently")
    void modifyDifferentObjects_objectsSuccessfullyModified() throws Exception {
        EntityLocker<Integer> el = new EntityLocker<>();

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        MyTestObj myObj = new MyTestObj();
        MyTestObj differentObj = new MyTestObj();
        Future<Optional<MyTestObj>> task1 = CompletableFuture.supplyAsync(
                () -> el.modifyObject(1, () -> {
                    trySleep(1000);
                    myObj.val++;
                    return myObj;
                }),
                threadPool
        );
        trySleep(20);
        Future<Optional<MyTestObj>> task2 = CompletableFuture.supplyAsync(
                () -> el.modifyObject(2, () -> {
                    int val = differentObj.val;
                    differentObj.val = val + 1;
                    return differentObj;
                }),
                threadPool
        );
        task2.get();
        assertTrue(!task1.isDone()); // no guarantee that it's true
        task1.get();
        assertTrue(task1.isDone());
    }


    class MyTestObj {
        int val = 0;
    }
}