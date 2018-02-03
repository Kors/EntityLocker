package locker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kors
 */
class EntityLockerTest {

    @Test
    @DisplayName("Simple object modification")
    void modifyObject_objectSuccessfullyModified() {
        EntityLocker<String> el = new EntityLocker<>();

        MyTestObj myObj = new MyTestObj();
        MyTestObj modified = el.modifyObject("lock", () -> {
            myObj.val++;
            return myObj;
        });

        assertEquals(myObj, modified);
        assertEquals(1, myObj.val);
    }

    @Test
    @DisplayName("Multithreading object modification")
    void modifyObjectInThreads_objectModifiedCorrectly() throws Exception {
        EntityLocker<String> el = new EntityLocker<>();

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        MyTestObj myObj = new MyTestObj();
        List<Future<MyTestObj>> futures = new ArrayList<>();
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
        for (Future<MyTestObj> future : futures) {
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
        Future<MyTestObj> task1 = CompletableFuture.supplyAsync(
                () -> el.modifyObject(1, () -> {
                    trySleep(1000);
                    myObj.val++;
                    return myObj;
                }),
                threadPool
        );
        trySleep(20);
        Future<MyTestObj> task2 = CompletableFuture.supplyAsync(
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