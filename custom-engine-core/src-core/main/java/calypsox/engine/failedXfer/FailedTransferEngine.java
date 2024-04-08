package calypsox.engine.failedXfer;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.CustomClientCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.InstantiateUtil;

import calypsox.tk.bo.CustomFallidasfallidasReportClientCacheAdapter;
import calypsox.tk.bo.ReporRowCacheAdapterInterface;
import calypsox.tk.event.PSEventResetCustomBOCache;

/**
 * The Class tk.
 */
public class FailedTransferEngine extends com.calypso.engine.Engine {

	private static final String REPORT_TYPE = "ReportTypeName";
	private static final String REPORT_TEMPLATE = "ReportTemplateName";
	private final ReentrantLock lock = new ReentrantLock();
	String reportType = null;
	String reportTemplate = null;

	/**
	 * @param dsCon
	 * @param hostName
	 * @param esPort
	 */
	public FailedTransferEngine(final DSConnection dsCon, final String hostName, final int esPort) {
		super(dsCon, hostName, esPort);
		Log.debug(this.getClass().getName(), "Arranque FailedTransferEngine ");
	}

	/**
	 * Initialize the engine. Load the report and set data into BOCache.
	 */
	@Override
	protected void init(EngineContext engineContext) {
		
		super.init(engineContext);
		bldDataLoad(engineContext);
	}

	/**
	 * Load the report and set data into BOCache.
	 */
	private void bldDataLoad(EngineContext engineContext) {
		Log.info(this, "Loading cache from report.");
		DSConnection dsConn = getDSConnection();
		if (dsConn != null) {
			reportType = engineContext.getInitParameter(REPORT_TYPE, null);
			reportTemplate = engineContext.getInitParameter(REPORT_TEMPLATE, null);				
			rebuildReport(dsConn);			
		}

	}
	public void rebuildReport(final DSConnection dsConn) {
		try {
			JDatetime valDateTime = new JDatetime();
			Report report = getReportInstance(reportType, valDateTime);
			if (report != null && !Util.isEmpty(reportTemplate)) {

				ReportTemplate template = dsConn.getRemoteReferenceData()
						.getReportTemplate(ReportTemplate.getReportName(reportType), reportTemplate);
				if (template == null) {
					Log.error(this, "Template " + reportTemplate + " Not Found for " + reportType + " Report");
				} else {
					String strPE = template.get(ReportPanel.PRICING_ENV_NAME);
					if (Util.isEmpty(strPE)) {
						strPE = dsConn.getUserDefaults().getPricingEnvName();
					}
					PricingEnv env = dsConn.getRemoteMarketData().getPricingEnv(strPE);
					report.setPricingEnv(env);
					report.setReportTemplate(template);
					template.setValDate(valDateTime.getJDate(dsConn.getUserDefaults().getTimeZone()));
					template.callBeforeLoad();
				}

				@SuppressWarnings("rawtypes")				
				DefaultReportOutput defaultReportOutput = report.load(new Vector());
				Log.system(this.getClass().getName(),
						"Metodo Init FailedTrasnferEngine. Rows:" + defaultReportOutput.getRows().length);
				ReporRowCacheAdapterInterface customReportRowFallidasClientCacheAdapter = getCacheAdapter(
						reportType, reportTemplate);
				loadCacheAndAddToBOCache(customReportRowFallidasClientCacheAdapter, defaultReportOutput);
				Log.info(this, "Load of BOCache ends ok.");
			}else {	
				Log.info(this, "Report does not exists. "+reportType);
			}
		} catch (Exception e) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error loading BOCache from the report.", e);

		}
	}
	public void loadCacheAndAddToBOCache(ReporRowCacheAdapterInterface customReportRowFallidasClientCacheAdapter,
			DefaultReportOutput defaultReportOutput) {
		try {
			Log.debug(this.getClass().getName(), "Calling BOCache from preload.");
			customReportRowFallidasClientCacheAdapter.preloadCache(defaultReportOutput);
			// Anadimos el reporte precargado a las customcache en el arraque del engine
			clearCacheIfExists(reportType, reportTemplate);
			BOCache.addCustomCache((CustomClientCache) customReportRowFallidasClientCacheAdapter);
		} catch (CalypsoServiceException e) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error preloading BOCache from the report. ", e);
		}
	}

	public ReporRowCacheAdapterInterface getCacheAdapter(String reportType, String reportTemplate) {
		ReporRowCacheAdapterInterface customReportRowFallidasClientCacheAdapter = null;

		try {
			customReportRowFallidasClientCacheAdapter = (ReporRowCacheAdapterInterface) Class
					.forName("calypsox.tk.bo." + reportType + reportTemplate + "ClientCacheAdapter").newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			Log.error(this, "Error instantiating cache class.");
		}
		return customReportRowFallidasClientCacheAdapter;
	}

	/**
	 *
	 * Recepcion de un nuevo evento.
	 * 
	 * @param event Evento recibido.
	 */
	@Override
	public boolean process(final PSEvent event) {	
		Log.debug(this.getClass().getName(), "Executing process method.");
		if (event instanceof PSEventResetCustomBOCache) {
			String repTyp = ((PSEventResetCustomBOCache) event).getReportType();
			String repTem = ((PSEventResetCustomBOCache) event).getReportTemplate();
			Boolean forcereset = ((PSEventResetCustomBOCache) event).getForceReset();
			if (repTyp != null && repTyp.equals(reportType) && repTem != null && repTem.equals(reportTemplate)) {
				if(forcereset || needToRebuild(repTyp, repTem)) {
					Log.info(this.getClass().getName(), "Rebuilding custom BOCache.");				
					try {			
						lock.lock();
						Thread.sleep(1000);
						bldDataLoad(this.getEngineContext());					
					} catch (InterruptedException e) {
						Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
								"Error loading BOCache from the report. Thread interrupted.", e);
						Thread.currentThread().interrupt();
					} finally {
						lock.unlock();
					}
					
					Log.info(this.getClass().getName(), "End of rebuild of BOCache.");
				} 
				return processEvent(event.getLongId(), this.getEngineName());
				
			} else {
				Log.info(this.getClass().getName(), "Report not found. Discarding event.");
			}
		} else {			
			try {
				int waitingNum=0;
				while(lock.isLocked() && waitingNum<=90) {					
					waitingNum++;
					Thread.sleep(10000);
				}
			} catch (InterruptedException e) {
				Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
						"Thread interrupted while waiting for rebuilding cache.", e);
				Thread.currentThread().interrupt();
			}
			if (!lock.isLocked()) {
				return processNormalPSEvent(event);
			}else {
				Log.error(this.getClass().getName(), "Engine is locked and timeout is rebased. PSEvent marked with error.");
				return false;
			}
		}
		return false;
	}
	private ReporRowCacheAdapterInterface getCacheSingleton() {
		
		ReporRowCacheAdapterInterface customReportRowFallidasClientCacheAdapter = null;
		// Comprobacion de custom client cache
		// Si vacia se da de alta custom client cache

		if (Util.isEmpty(BOCache.getCustomCaches())) {
			// instacia de la clase (reportType/reportTemplate/clientcacheadpater )
			customReportRowFallidasClientCacheAdapter = getCacheAdapter(reportType, reportTemplate);
			BOCache.addCustomCache((CustomClientCache) customReportRowFallidasClientCacheAdapter);
		}
		// Si custom client cache existe, se busca tipo
		// CustomReportRowFallidasClientCacheAdapter para recuperar la precarga
		else {

			final Set<CustomClientCache> setCaches = BOCache.getCustomCaches();
			final Iterator<CustomClientCache> itCache = setCaches.iterator();
			while (itCache.hasNext()) {
				final CustomClientCache cache = itCache.next();
				if (cache instanceof ReporRowCacheAdapterInterface) {
					customReportRowFallidasClientCacheAdapter = (ReporRowCacheAdapterInterface) cache;
					break;
				}

			}
			// Si customReportRowFallidasClientCacheAdapter es nula se da de alta una nueva
			// cache de tipo custom
			if (customReportRowFallidasClientCacheAdapter == null) {
				customReportRowFallidasClientCacheAdapter = getCacheAdapter(reportType, reportTemplate);
				BOCache.addCustomCache((CustomClientCache) customReportRowFallidasClientCacheAdapter);
			}
		}
		return customReportRowFallidasClientCacheAdapter;
	}
	public boolean processNormalPSEvent(final PSEvent event) {
		ReporRowCacheAdapterInterface cache=getCacheSingleton();
		if(cache!=null) {
			boolean ret=false;			
			try {
				ret=cache.newEvent(event, getEngineName());
				if(ret) {
					return processEvent(event.getLongId(), this.getEngineName());
				}
			} catch (CalypsoServiceException e) {
				Log.error(this.getClass().getName(), "Error processing event.",e);
			}			
		}else {
			Log.error(this.getClass().getName(), "Error getting or creating custom BOCache. PSEvent marked with error");
		}
		return false;
	}
	/**
	 * @param id
	 * @param engineName
	 * @return
	 */
	private boolean processEvent(long id, String engineName) {
		try {
			Log.debug(this.getClass().getName(), "Metodo Process . processEvent");
			this.getDS().getRemoteTrade().eventProcessed(id, engineName);
		} catch (Exception e) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error in Method processEvent saving the event ", e);

			return false;
		}
		return true;
	}

	/**
	 *
	 */
	@Override
	protected void prestop(boolean willTerminate) {
		Log.debug(this.getClass().getName(),
				"Shutdown Request Received, Stopping Engine. Clearing Custom Client Cache");
		if (Util.isEmpty(BOCache.getCustomCaches())) {
			Log.debug("Custom Caches empty", this.getEngineName());
		} else {
			final Set<CustomClientCache> setCaches = BOCache.getCustomCaches();
			final Iterator<CustomClientCache> itCache = setCaches.iterator();
			while (itCache.hasNext()) {
				final CustomClientCache cache = itCache.next();
				if (cache instanceof CustomFallidasfallidasReportClientCacheAdapter) {
					final CustomFallidasfallidasReportClientCacheAdapter cccAdapter = (CustomFallidasfallidasReportClientCacheAdapter) cache;
					cccAdapter.clear();
					itCache.remove();
					break;
				}
			}

			super.prestop(willTerminate);
		}
	}

	public void clearCacheIfExists(String reportType, String reportTemplate) {
		Log.info(this, "Cleaning BOCache.");
		// Comprobacion custom cache vacia, si no esta vacia se limpia
		if (!Util.isEmpty(BOCache.getCustomCaches())) {
			Log.debug(this.getClass().getName(), "Clearing BOCache from FailedTrasnferEngine.");
			final Set<CustomClientCache> setCaches = BOCache.getCustomCaches();
			final Iterator<CustomClientCache> itCache = setCaches.iterator();
			while (itCache.hasNext()) {
				final CustomClientCache cache = itCache.next();
				if (cache instanceof ReporRowCacheAdapterInterface && cache.getClass().getSimpleName().toLowerCase()
						.startsWith((reportType + reportTemplate).toLowerCase())) {
					ReporRowCacheAdapterInterface cccAdapter = (ReporRowCacheAdapterInterface) cache;
					cccAdapter.clear();
					itCache.remove();
				}
			}
		}
	}

	public Report getReportInstance(String reportType, JDatetime valDateTime) {
		Report report = null;
		try {
			String className = "tk.report." + reportType + "Report";
			report = (Report) InstantiateUtil.getInstance(className, true);
			report.setValuationDatetime(valDateTime);
			report.setUndoDatetime(null);
			report.setForceUndo(false);

		} catch (Exception e) {
			Log.error(this, e);
			report = null;
		}
		return report;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public DSConnection getDSConnection() {
		DSConnection dsConn = null;
		try {
			dsConn = DSConnection.getDefault().getReadOnlyConnection();
		} catch (ConnectException e1) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class, "Error getting DSConnection. ", e1);
		}
		if (dsConn == null) {
			dsConn = DSConnection.getDefault();
		}
		return dsConn;
	}
	
	public Boolean needToRebuild(String reportType, String reportTemplate) {
		int sizeReport = 0;
		int sizeCache = 0;
		DSConnection dsConn = getDSConnection();
		try {
			sizeReport = getReportSize(reportType, reportTemplate, dsConn);
			Log.system(this.getClass().getSimpleName(), "Number of rows in the report: "+ Integer.valueOf(sizeReport));
			ReporRowCacheAdapterInterface customReportRowFallidasClientCacheAdapter = getCache(reportType, reportTemplate);

			if (customReportRowFallidasClientCacheAdapter != null) {
				Log.info(this, "Getting number of rows in cache.");
				ReportRow[] rows = customReportRowFallidasClientCacheAdapter.getReportRows();
				if(rows != null) {
					sizeCache = rows.length;
					Log.system(this.getClass().getSimpleName(), "Number of rows in the cache: "+ Integer.valueOf(sizeCache));			
				}
			} else {
				return true;
			}
			
		} catch (Exception e) {
			Log.error(this, "An error has ocurred while geting the report and cache size", e);
		}
		
		return false;
	}
	
	public ReporRowCacheAdapterInterface getCache(String reportType, String reportTemplate) {
		
		
		ReporRowCacheAdapterInterface customReportRowFallidasClientCacheAdapter = null;
		final Set<CustomClientCache> setCaches = BOCache.getCustomCaches();
		final Iterator<CustomClientCache> itCache = setCaches.iterator();
		while (itCache.hasNext()) {
			final CustomClientCache cache = itCache.next();
			if (cache instanceof ReporRowCacheAdapterInterface
					&& cache.getClass().getSimpleName().startsWith(reportType + reportTemplate)) {
				customReportRowFallidasClientCacheAdapter = (ReporRowCacheAdapterInterface) cache;
				break;
			}
		}
		return customReportRowFallidasClientCacheAdapter;
	}
	
	public int getReportSize(String reportType, String reportTemplate, DSConnection dsConn) {
		JDatetime valDateTime = new JDatetime();
		Report report = getReportInstance(reportType, valDateTime);
		try {
			if (report != null && !Util.isEmpty(reportTemplate)) {

				ReportTemplate template = dsConn.getRemoteReferenceData()
						.getReportTemplate(ReportTemplate.getReportName(reportType), reportTemplate);
				if (template == null) {
					Log.error(this, "Template " + reportTemplate + " Not Found for " + reportType + " Report");
				} else {
					String strPE = template.get(ReportPanel.PRICING_ENV_NAME);
					if (Util.isEmpty(strPE)) {
						strPE = dsConn.getUserDefaults().getPricingEnvName();
					}
					PricingEnv env = dsConn.getRemoteMarketData().getPricingEnv(strPE);
					template.setValDate(valDateTime.getJDate(dsConn.getUserDefaults().getTimeZone()));
					template.callBeforeLoad();
					report.setPricingEnv(env);
					report.setReportTemplate(template);
					
					@SuppressWarnings("unchecked")
					Map<String, Integer> map = report.getPotentialSize();
					if(map != null) {
						return map.get("BOTransfer");
					}
					Log.error(this, "Can not obtain the report size");
				}
			}
		} catch(Exception e) {
			Log.error(this, "Can not obtain the report size", e);
		}
		
		return 0;
	}


}