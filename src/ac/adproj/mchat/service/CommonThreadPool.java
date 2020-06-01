package ac.adproj.mchat.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Thread pool for scattered tasks.
 * 
 * @author Andy Cheung
 * @since 2020.5.24
 */
public class CommonThreadPool {
    private static int threadNumber = 0;

    private static BlockingQueue<Runnable> bq = new LinkedBlockingQueue<>(16);
    private static String comment = "";
    
    /**
     * Mutex to make sure that the comment variable won't be "invaded" by other thread.
     */
    private static final byte[] mutex = new byte[1];

    private static ThreadFactory threadFactory = r -> {
        synchronized (mutex) {
            threadNumber++;
            
            String threadName = "ScatteredTask - #" + threadNumber + (comment.isEmpty() ? "" : " - " + comment);
            
            Thread t = new Thread(r, threadName);
            
            comment = "";
            
            return t;
        }
    };

    private static ExecutorService threadPool = new ThreadPoolExecutor(3, 8, 1, TimeUnit.MINUTES, bq, threadFactory);
    
    /**
     * Commit task to the thread pool.
     * 
     * @param r Runnable task to run.
     */
    public synchronized static void execute(Runnable r) {
        threadPool.execute(r);
    }
    
    /**
     * Commit task to the thread pool.
     * 
     * @param r Runnable task to run.
     * @param stmt The statement part in the thread name.
     */
    public synchronized static void execute(Runnable r, String stmt) {
        synchronized (mutex) {
            comment = stmt;
        }
        
        threadPool.execute(r);
    }
    
    /**
     * Shutdown the thread pool.
     */
    public static void shutdown() {
        threadPool.shutdownNow();
    }
}
