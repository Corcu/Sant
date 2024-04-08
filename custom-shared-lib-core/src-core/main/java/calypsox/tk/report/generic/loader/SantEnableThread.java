/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.calypso.tk.report.ReportTemplate;

public abstract class SantEnableThread<K, V> {

	protected List<V> dataList;
	protected Map<K, V> dataMap;
	private Thread innerThread;
	private boolean enableThreading;
	protected ReportTemplate template;

	public SantEnableThread(ReportTemplate template, boolean enableThreading) {
		this(enableThreading);
		this.template = template;

	}

	public SantEnableThread(boolean enableThreading) {
		this.dataList = new ArrayList<V>();
		this.dataMap = new HashMap<K, V>();
		this.enableThreading = enableThreading;
	}

	public boolean isAlive() {
		if (!this.enableThreading) {
			return false;
		}
		return this.innerThread.isAlive();
	}

	public List<V> getDataAsList() {
		if (!this.dataList.isEmpty() || this.dataMap.isEmpty()) {
			return this.dataList;
		}
		// else
		return new ArrayList<V>(this.dataMap.values());

	}

	public Map<K, V> getDataAsMap() {
		if (!this.dataMap.isEmpty() || this.dataList.isEmpty()) {
			return this.dataMap;
		}
		// else
		return getDataMapFromDataList();
	}

	public void load() {
		if (this.enableThreading) {
			this.innerThread = new Thread(new Runnable() {

				@Override
				public void run() {
					loadData();
				}
			});
			this.innerThread.start();
		} else {
			loadData();
		}
	}
	
	//GSM 10/10/16 - Added to have option Join from this thread
	public void join() throws InterruptedException {
		if (this.enableThreading) {
			this.innerThread.join();
		}
	}
	
	public void join(long time) throws InterruptedException {
		if (this.enableThreading) {
			this.innerThread.join(time);
		}
	}
	

	protected abstract void loadData();

	protected abstract Map<K, V> getDataMapFromDataList();
}