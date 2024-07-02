/*
    Copyright (C) 2011-2020 Andy Cheung

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package ac.adproj.mchat.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.*;

/**
 * Thread pool for scattered tasks.
 *
 * @author Andy Cheung
 * @since 2020.5.24
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonThreadPool {
    /**
     * Mutex to make sure that the comment variable won't be "invaded" by other thread.
     */
    private static final byte[] mutex = new byte[1];
    private static int threadNumber = 0;

    private static final BlockingQueue<Runnable> runnableLinkedBlockingQueue = new LinkedBlockingQueue<>(16);
    private static String comment = "";

    private static final ThreadFactory threadFactory = r -> {
        synchronized (mutex) {
            threadNumber++;

            String threadName = "ScatteredTask - #" + threadNumber + (comment.isEmpty() ? "" : " - " + comment);

            Thread t = new Thread(r, threadName);

            comment = "";

            return t;
        }
    };

    static ExecutorService threadPool = new ThreadPoolExecutor(3, 8, 1, TimeUnit.MINUTES, runnableLinkedBlockingQueue, threadFactory);

    /**
     * Commit task to the thread pool.
     *
     * @param r Runnable task to run.
     */
    public static synchronized void execute(Runnable r) {
        threadPool.execute(r);
    }

    /**
     * Commit task to the thread pool.
     *
     * @param r    Runnable task to run.
     * @param stmt The statement part in the thread name.
     */
    public static synchronized void execute(Runnable r, String stmt) {
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
