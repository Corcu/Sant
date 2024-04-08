package com.isban.api.webservice;

import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.CustomClientCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.DomainValues.DomainValuesRow;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.JavaBeanReportViewer;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.InstantiateUtil;

import calypsox.tk.bo.CustomFallidasfallidasReportClientCacheAdapter;
import calypsox.tk.bo.ReporRowCacheAdapterInterface;
import calypsox.tk.event.PSEventResetCustomBOCache;
import calypsox.webservices.annotations.SgtEndpoint;

/**
 * @author x660030 Webservice to execute some reports of Calypso to get
 *         information.
 */
@SgtEndpoint
@Path("/reportLauncher")
public class ExecutionReportWS {
	private static final String CALYPSO_ENGINE_MANAGER_CONFIG = "calypso.engine.manager.config";
	private static ConcurrentHashMap<String, String> engineServerNameDVHash = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, String> beanClassNameDVHash = new ConcurrentHashMap<>();
	private static final String DVENGINESERVERWS = "DVEngineServerWS";
	private static final String DVBEANOBJECTDTOWS = "DVBeanObjectDTOWS";
	private static final String DVSUBBEANOBJECTDTOWS = "DVSubBeanObjectDTOWS";
	private static final String DVSUBBEANNAME = "DVSubBeanFieldName";

	public ExecutionReportWS() {
		super();
		initializeDVs();
	}

	private void initializeDVs() {
		DSConnection dsConn = getDSConnection();
		if (dsConn != null) {
			try {

				List<DomainValuesRow> dvList = dsConn.getRemoteReferenceData().getDomainValuesRows(DVENGINESERVERWS);
				for (DomainValuesRow domainValuesRow : dvList) {
					String key = domainValuesRow.getValue();
					String value = domainValuesRow.getComment();
					engineServerNameDVHash.put(key, value);
				}

			} catch (Exception e) {
				Log.error(this, "Error loading domain " + DVENGINESERVERWS + " to get Engine server configuration.", e);
			}
		}
	}

	/**
	 * @param reportTypeParam     Type of the Calypso Report to launch.
	 * @param reportTemplateParam Report template name to launch.
	 * @return Response with JSON result of the executed report.
	 */
	@GET
	@Path("/executeReport")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response executeReport(@QueryParam("reportType") String reportTypeParam,
			@QueryParam("reportTemplate") String reportTemplateParam) {
		return executeReportWithFilters(reportTypeParam, reportTemplateParam, null);
	}

	/**
	 * @param reportTypeParam     Type of the Calypso Report to launch.
	 * @param reportTemplateParam Report template name to launch.
	 * @return Response with JSON result of the executed report.
	 */
	@GET
	@Path("/executeReportWithFilters")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response executeReportWithFilters(@QueryParam("reportType") String reportTypeParam,
			@QueryParam("reportTemplate") String reportTemplateParam, InputStream inputJson) {
		Log.info(this, "ExecuteReport from WS. Type:" + reportTypeParam + " Template: " + reportTemplateParam);

		DSConnection dsConn = getDSConnection();
		String reportType = reportTypeParam;
		String reportTemplate = reportTemplateParam;
		String engineServerName = System.getProperty(CALYPSO_ENGINE_MANAGER_CONFIG);
		String engineServerNameDV = getDomainESIfNotLoaded(reportType, reportTemplate, dsConn);
		if (!Util.isEmpty(engineServerName) && !Util.isEmpty(engineServerNameDV)
				&& engineServerNameDV.trim().equals(engineServerName.trim())) {
			try {

				Report rep = createReport(reportType, reportTemplate, dsConn);

				if (setFilters(inputJson, rep)) {
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

					DefaultReportOutput ro = null;
					JavaBeanReportViewer viewer = new JavaBeanReportViewer();
					if (customReportRowFallidasClientCacheAdapter != null) {
						Log.info(this, "ExecuteReport is using BOCache to format the report.");
						ReportRow[] rows = customReportRowFallidasClientCacheAdapter.getReportRows();
						Log.system(this.getClass().getSimpleName(), "Rows returned by the WS: "+rows.length);
						ro = new DefaultReportOutput(rep);
						ro.setRows(rows);
						
					} else {
						try {
							if (Class.forName("tk.bo." + reportType + reportTemplate + "ClientCacheAdapter") != null) {
								Log.error(this,
										"Custom BOCache does not exist. Engine maybe not started. Returning error .");
								return Response.status(Status.SERVICE_UNAVAILABLE).build();
							}
						} catch (ClassNotFoundException e) {
							Log.info(this, "Not BOCache adapter class found. tk.bo." + reportType + reportTemplate
									+ "ClientCacheAdapter");
						} 
						Log.info(this, "Running report " + reportType + " " + reportTemplate);
						@SuppressWarnings("rawtypes")
						Vector errorMsgs = new Vector();
						ro = rep.load(errorMsgs);
						if (ro == null || ro.getNumberOfRows() <= 0) {
							Log.error(this, "Information could not be retrieved. " + errorMsgs.toString());
							return Response.accepted("Information could not be retrieved. " + errorMsgs.toString()).build();
						}
					}
					ro.format(viewer);
					List<Object> array = viewer.getBeanList();
					HashMap<String, List<Object>> result = new HashMap<>();
					result.put(reportTemplate, array);
					Log.info(this, "ExecuteReport ends ok.");
					return Response.ok(result).build();
				} else {
					Log.error(this, "Invalid input report filters. ");
					return Response.status(Status.BAD_REQUEST).build();
				}
			} catch (RemoteException e) {
				Log.error(this, "Error loading report template " + reportTemplate + " of type " + reportType, e);
			}
			finally {
				if(getCacheAdapter(reportType, reportTemplate)!=null) {
					PSEventResetCustomBOCache request = new PSEventResetCustomBOCache(reportType, reportTemplate,false);
					try {
						dsConn.getRemoteTrade().saveAndPublish(request);
					} catch (CalypsoServiceException e) {
						Log.error(this, "Error saving event to reset custom BOCaches.", e);
					}
				}
			}
			Log.error(this, "ExecuteReport ends with errors. Returning server error.");
			return Response.serverError().build();
		} else {
			Log.info(this, "ExecuteReport ends ko because request is not acceptable.");
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
	}
	
	/**
	 * @return Response with JSON result of the executed report.
	 */
	@POST
	@Path("/executeFilteredReport/{reportType}/{reportTemplate}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response executeFilteredReport(@PathParam("reportType") String reportType, @PathParam("reportTemplate") String reportTemplate, InputStream inputJson) {
			
		Log.info(this, "ExecuteReport from WS. Type:" + reportType + " Template: " + reportTemplate);
		DSConnection dsConn = getDSConnection();
		String engineServerName = System.getProperty(CALYPSO_ENGINE_MANAGER_CONFIG);
		String engineServerNameDV = getDomainESIfNotLoaded(reportType, reportTemplate, dsConn);
		if (!Util.isEmpty(engineServerName) && !Util.isEmpty(engineServerNameDV)
				&& engineServerNameDV.trim().equals(engineServerName.trim())) {
			try {

				Report rep = createReport(reportType, reportTemplate, dsConn);

				if (setFilters(inputJson, rep)) {
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

					DefaultReportOutput ro = null;
					JavaBeanReportViewer viewer = new JavaBeanReportViewer();
					if (customReportRowFallidasClientCacheAdapter != null) {
						Log.info(this, "ExecuteReport is using BOCache to format the report.");
						ReportRow[] rows = customReportRowFallidasClientCacheAdapter.getReportRows();
						ro = new DefaultReportOutput(rep);
						ro.setRows(rows);
					} else {
						try {
							if (Class.forName("tk.bo." + reportType + reportTemplate + "ClientCacheAdapter") != null) {
								Log.error(this,
										"Custom BOCache does not exist. Engine maybe not started. Returning error .");
								return Response.status(Status.SERVICE_UNAVAILABLE).build();
							}
						} catch (ClassNotFoundException e) {
							Log.info(this, "Not BOCache adapter class found. tk.bo." + reportType + reportTemplate
									+ "ClientCacheAdapter");
						}
						Log.info(this, "Running report " + reportType + " " + reportTemplate);
						@SuppressWarnings("rawtypes")
						Vector errorMsgs = new Vector();
						ro = rep.load(errorMsgs);
						if (ro == null || ro.getNumberOfRows() <= 0) {
							Log.error(this, "Information could not be retrieved. " + errorMsgs.toString());
							return Response.accepted("Information could not be retrieved. " + errorMsgs.toString()).build();
						}
					}
					ro.format(viewer);
					List<Object> array = viewer.getBeanList();
					HashMap<String, List<Object>> result = new HashMap<>();
					result.put(reportTemplate, array);
					Log.info(this, "ExecuteReport ends ok.");
					return Response.ok(result).build();
				} else {
					Log.error(this, "Invalid input report filters. ");
					return Response.status(Status.BAD_REQUEST).build();
				}
			} catch (RemoteException e) {
				Log.error(this, "Error loading report template " + reportTemplate + " of type " + reportType, e);
			}
			Log.error(this, "ExecuteReport ends with errors. Returning server error.");
			return Response.serverError().build();
		} else {
			Log.info(this, "ExecuteReport ends ko because request is not acceptable.");
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
	}

	private boolean setFilters(InputStream inputJson, Report rep) {
		if (inputJson != null) {
			JsonObject jsobj = getJSonObj(inputJson);
			if (jsobj != null) {
				JsonArray jArr = jsobj.getJsonArray("Filters");
				if (jArr != null) {
					for (int i = 0; i < jArr.size(); i++) {
						String filterName = jArr.getJsonObject(i).getString("Name");
						try {
							JsonArray jArr2 = jArr.getJsonObject(i).getJsonArray("Value");
							JsonString[] values = jArr2.toArray(new JsonString[jArr2.size()]);
							rep.getReportTemplate().put(filterName, Util.arrayToString(values).replaceAll("\"", ""));

						} catch (ClassCastException e) {
							try {
								JsonString jArr2 = jArr.getJsonObject(i).getJsonString("Value");
								rep.getReportTemplate().put(filterName, jArr2.getString());

							} catch (ClassCastException e2) {
								return false;
							}
						}
					}
					return true;
				} else {
					return false;
				}
			}
		}

		return true;
	}

	private JsonObject getJSonObj(InputStream inputJson) {
		JsonReader jsonReader = Json.createReader(inputJson);
		JsonObject output = jsonReader.readObject();
		jsonReader.close();
		return output;
	}

	/**
	 * Reload domainValue if it is not loaded.
	 * 
	 * @param reportType
	 * @param reportTemplate
	 * @param dsConn
	 * @return
	 */
	private String getDomainESIfNotLoaded(String reportType, String reportTemplate, DSConnection dsConn) {

		String engineServerNameDV = "";
		String existDVES = engineServerNameDVHash.get(reportType + "_" + reportTemplate);
		if (Util.isEmpty(existDVES)) {
			try {
				DomainValuesRow row = dsConn.getRemoteReferenceData().getDomainValuesRow(DVENGINESERVERWS,
						reportType + "_" + reportTemplate);
				if (row != null) {
					engineServerNameDV = row.getComment();
					engineServerNameDVHash.put(reportType + "_" + reportTemplate, engineServerNameDV);
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error loading domain value " + reportType + "_" + reportTemplate
						+ " to get Engine server configuration.", e);
			}
		} else {
			engineServerNameDV = existDVES;
		}
		return engineServerNameDV;
	}

	/**
	 * Obtiene el domain value del Bean a usar si existe.
	 * 
	 * @param type
	 * @param templateName
	 * @param dsConn
	 * @return
	 */
	private String getDomainBeanIFNotLoaded(String type, String templateName, String dvName, DSConnection dsConn) {

		String beanClassNameDV = null;
		String existDVbc = beanClassNameDVHash.get(type + "_" + templateName);
		if (Util.isEmpty(existDVbc)) {
			try {
				DomainValuesRow row = dsConn.getRemoteReferenceData().getDomainValuesRow(dvName,
						type + "_" + templateName);
				if (row != null) {
					beanClassNameDV = row.getComment();
					beanClassNameDVHash.put(type + "_" + templateName, beanClassNameDV);
				} else {
					beanClassNameDVHash.put(type + "_" + templateName, "-");
				}
			} catch (CalypsoServiceException e) {
				Log.debug(this, "Could not get domain value " + type + "_" + templateName
						+ " to get bean class name. Default primitive bean will be used.", e);
				beanClassNameDVHash.put(type + "_" + templateName, "-");
			}
		} else if (!existDVbc.equals("-")) {
			beanClassNameDV = existDVbc;
		}
		return beanClassNameDV;
	}

	/**
	 * @param type         Type of report to be created
	 * @param templateName Name of the template to create.
	 * @param sb           Text with error in case of the template does not exist.
	 * @return Instance of the report created.
	 * @throws RemoteException
	 */
	public Report createReport(String type, String templateName, DSConnection dsConn) throws RemoteException {
		Report report;
		JDatetime valDateTime = new JDatetime();
		try {
			String className = "tk.report." + type + "Report";
			report = (Report) InstantiateUtil.getInstance(className, true);
			report.setValuationDatetime(valDateTime);
			report.setUndoDatetime(null);
			report.setForceUndo(false);

		} catch (Exception e) {
			Log.error(this, e);
			report = null;
		}
		if (report != null && !Util.isEmpty(templateName) && dsConn != null) {
			ReportTemplate template = dsConn.getRemoteReferenceData()
					.getReportTemplate(ReportTemplate.getReportName(type), templateName);
			if (template == null) {
				Log.error(this, "Template " + templateName + " Not Found for " + type + " Report");
			} else {

				String strPE = template.get(ReportPanel.PRICING_ENV_NAME);
				if (Util.isEmpty(strPE)) {
					strPE = dsConn.getUserDefaults().getPricingEnvName();
				}
				PricingEnv env = dsConn.getRemoteMarketData().getPricingEnv(strPE);
				String beanClassNameDV = null;
				String subBeanClassNameDV = null;
				String subBeanNameDV = null;
				report.setPricingEnv(env);
				beanClassNameDV = getDomainBeanIFNotLoaded(type, templateName, DVBEANOBJECTDTOWS, dsConn);
				subBeanClassNameDV = getDomainBeanIFNotLoaded(type, templateName, DVSUBBEANOBJECTDTOWS, dsConn);
				subBeanNameDV = getDomainBeanIFNotLoaded(type, templateName, DVSUBBEANNAME, dsConn);
				template.put(JavaBeanReportViewer.BEAN_CLASS_LABEL, beanClassNameDV);
				template.put(JavaBeanReportViewer.SUB_BEAN_CLASS_LABEL, subBeanClassNameDV);
				template.put(JavaBeanReportViewer.SUB_BEAN_FIELD_NAME, subBeanNameDV);
				report.setReportTemplate(template);
				template.setValDate(valDateTime.getJDate(dsConn.getUserDefaults().getTimeZone()));
				template.callBeforeLoad();
			}
		} else {
			report = null;
		}
		return report;
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
}
