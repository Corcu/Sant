package calypsox.tk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import calypsox.tk.report.exception.SantException;
import calypsox.tk.report.exception.SantExceptionType;

import com.calypso.tk.core.Log;

/**
 * Manage asynchronous multithreading
 * 
 * Example of how to use it:
 * 
 * FutureTaskThreadPool<Void> threadPool = new FutureTaskThreadPool<Void>(10);
 * 
 * for (int i = 0; i < n ; i++){ Callable<Void> myCallableTask = new
 * MyCallableTask(); threadPool.add(myCallableTask); }
 * threadPool.waitForAllTasksToBeCompleted();
 * 
 * @param <T>
 *            the generic type
 */
public class FutureTaskThreadPool<T extends Throwable> {

    private final List<FutureTask<T>> tasks;
    private final ExecutorService executor;
    private final List<T> exceptions = new ArrayList<T>();

    /**
     * Instantiates a new future task thread pool.
     * 
     * @param nbThreads
     *            the nb threads
     */
    public FutureTaskThreadPool(final int nbThreads) {
        this.tasks = new ArrayList<FutureTask<T>>();
        this.executor = Executors.newFixedThreadPool(nbThreads);
    }

    /**
     * Wait for all tasks to be completed and shutdown executor.
     */
    public void waitForAllTasksToBeCompleted() {
        // CAL_ACC_1313
        waitForAllTasksToBeCompleted(true);
    }

    // CAL_ACC_1313
    /**
     * Wait for all tasks to be completed.
     * 
     * @param shutdown
     *            If <code>true</code> the executor will shutdown so no more
     *            tasks can be added after all present tasks are finished. Set
     *            to <code>false</code> if you want to wait for the present
     *            tasks to end but want to add more tasks in the future.
     */
    @SuppressWarnings("unchecked")
    public void waitForAllTasksToBeCompleted(final boolean shutdown) {
        for (int i = 0; i < this.tasks.size(); i++) {
            try {
                final T exception = this.tasks.get(i).get();

                if (exception != null) {
                    this.exceptions.add(exception);
                }
                this.tasks.remove(i);
                i--;
            } catch (final Exception e) {
                Log.error(this, e);
                this.exceptions.add((T) new SantException(
                        SantExceptionType.TECHNICAL_EXCEPTION, this.getClass()
                                .getName(), e.getMessage(), 0, 0, null, 0, e));
            }
        }
        // CAL_ACC_1313
        if (shutdown) {
            this.executor.shutdown();
        }

    }

    /**
     * Adds the task.
     * 
     * @param callable
     *            the callable
     */
    public void addTask(final Callable<T> callable) {
        final FutureTask<T> future = new FutureTask<T>(callable);
        this.tasks.add(future);
        this.executor.execute(future);
    }

    /**
     * Gets the exceptions.
     * 
     * @return the exceptions
     */
    public List<T> getExceptions() {
        return this.exceptions;
    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        this.executor.shutdown();
    }
}
