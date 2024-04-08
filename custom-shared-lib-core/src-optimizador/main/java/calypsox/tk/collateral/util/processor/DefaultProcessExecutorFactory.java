/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.util.processor;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * @author aela
 *
 * @param <IN>
 * @param <OUT>
 * @param <LOG>
 */
public  class DefaultProcessExecutorFactory<IN, OUT, LOG> implements ProcessExecutorFactory{
	
	/*
	getExternalAlloationReaderFactory() {
		
	}
	
	getExternalAlloationMapperFactory() {
		
	}
	
	getExternalAlloationPersistorFactory() {
		
	}
	*/
	
	public  ProcessExecutor<IN, OUT, LOG> createExecutor(
			BlockingQueue<IN> inWorkQueue, BlockingQueue<OUT> outWorkQueue,
			BlockingQueue<LOG> loggingQueue, ProcessContext context, List<ProcessExecutorLauncher> prodcuersLauncher) {
		return null;
	}

	public  String getName(){
		return null;
	}
	
	public static <IN, OUT, LOG> DefaultProcessExecutorFactory<IN, OUT, LOG> getInstance(){
		return null;
	}
}
