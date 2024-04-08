/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util.importer;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessExecutorThreadFactory implements ThreadFactory {

	protected static AtomicInteger poolNumber = new AtomicInteger(1);
	protected final ThreadGroup group;
	protected final AtomicInteger threadNumber = new AtomicInteger(1);
	protected final String namePrefix;

	public ProcessExecutorThreadFactory(String poolThreadsName) {
		SecurityManager s = System.getSecurityManager();
		this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		this.namePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-" + poolThreadsName;
	}

	@Override
	public Thread newThread(Runnable r) {
		return new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0);
	}
}
