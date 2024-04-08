package com.isban.api.webservice;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.DomainValues.DomainValuesRow;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.JavaBeanReportViewer;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.ui.component.condition.ConditionTree;
import com.calypso.ui.component.condition.ConditionTreeNode;

import calypsox.tk.bo.CustomFallidasfallidasReportClientCacheAdapter;
import calypsox.tk.report.restservices.AbstractReportDataWS;
import calypsox.webservices.annotations.SgtEndpoint;

/**
 * @author x957355
 *  Webservice to execute some reports of Calypso to get
 *         information.
 */
@SgtEndpoint
@Path("/reportDigitalFront")
public class ReportDigitalFrontWS {
	private static final String CALYPSO_ENGINE_MANAGER_CONFIG = "calypso.engine.manager.config";
	private static ConcurrentHashMap<String, String> engineServerNameDVHash = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, String> beanClassNameDVHash = new ConcurrentHashMap<>();
	private static final String DVENGINESERVERWS = "DVReportDigitalFrontWS";
	private static final String DVBEANOBJECTDTOWS = "DVBeanObjectDTOWS";
	private static final String DVSUBBEANOBJECTDTOWS = "DVSubBeanObjectDTOWS";
	private static final String DVSUBBEANNAME = "DVSubBeanFieldName";
	private static final String XFER_ATT = "XferAttributes";

	public ReportDigitalFrontWS() {
		super();
		initializeDVs();
//		exec();
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
	 * @return Response with JSON result of the executed report.
	 */
	@POST
	@Path("/executeFilteredReport/{reportType}/{reportTemplate}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response executeFilteredReport(@PathParam("reportType") String reportType,
			@PathParam("reportTemplate") String reportTemplate, InputStream inputJson) {

		Log.info(this, "ExecuteReport from WS. Type:" + reportType + " Template: " + reportTemplate);

		DSConnection dsConn = getDSConnection();

		String engineServerName = System.getProperty(CALYPSO_ENGINE_MANAGER_CONFIG);
		String engineServerNameDV = getDomainESIfNotLoaded(reportType, reportTemplate, dsConn);
		AbstractReportDataWS data;

		List<String> orderByOptions = new ArrayList<>();
		List<String> groupOptions = new ArrayList<>();
		List<Object> pagination = new ArrayList<>();

		if (!Util.isEmpty(engineServerName) && !Util.isEmpty(engineServerNameDV)
				&& engineServerNameDV.trim().equals(engineServerName.trim())) {
			try {

				Report rep = createReport(reportType, reportTemplate, dsConn);
				JsonObject jsobj = getJSonObj(inputJson);
				getOrderBy(jsobj, orderByOptions);
				getGroupBy(jsobj, groupOptions);

				if (setFilters(jsobj, rep)) {

					DefaultReportOutput ro = null;
					JavaBeanReportViewer viewer = new JavaBeanReportViewer();
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
					Vector<String> errorMsgs = new Vector<String>();

					data = getDataClassInstance(reportType, reportTemplate);
					if (data == null) {
						Log.error(this, "The report data class is not found");
						return Response.serverError().entity("The request cannot be answered").build();
					}
					ro = data.executeReport(rep, orderByOptions, groupOptions, getPageNumber(jsobj), getPageSize(jsobj),
							pagination, errorMsgs);
					if (ro == null || ro.getNumberOfRows() <= 0) {
						Log.error(this, "Information could not be retrieved. " + errorMsgs.toString());
						return Response.accepted("Information could not be retrieved. " + errorMsgs.toString()).build();
					}
					ro.format(viewer);
					List<Object> array = viewer.getBeanList();
					HashMap<String, List<Object>> result = new HashMap<>();

					if (!pagination.isEmpty()) {
						result.put("Pagination", pagination);
					}
					result.put(reportTemplate, array);

					Log.info(this, "ExecuteReport ends ok.");
					if (!errorMsgs.isEmpty()) {
						return Response.status(Status.BAD_REQUEST).entity(errorMsgs).build();

					}
					return Response.ok(result).build();
				} else {
					Log.error(this, "Invalid input report filters. ");
					return Response.status(Status.BAD_REQUEST).build();
				}
			} catch (RemoteException e) {
				Log.error(this, "Error loading report template " + reportTemplate + " of type " + reportType, e);
			} catch (InterruptedException e) {
				Log.error(this, e);
				return Response.serverError().build();
			} catch (TimeoutException e) {
				Log.error(this, "Too many concurrent request. Timeout exceded", e);
				return Response.status(Status.CONFLICT).entity("Too many concurrent request. Try again in a few minutes.").build();
			}
			Log.error(this, "ExecuteReport ends with errors. Returning server error.");
			return Response.serverError().build();
		} else {
			Log.info(this, "ExecuteReport ends ko because request is not acceptable.");
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
	}
	
	@POST
	@Path("/exportReport/{reportType}/{exportReport}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response exportFilteredReport(@PathParam("reportType") String reportType,
			@PathParam("exportReport") String exportReport, InputStream inputJson) {

		Log.info(this, "ExecuteReport from WS. Type:" + reportType + " Template: " + exportReport);

		DSConnection dsConn = getDSConnection();

		String engineServerName = System.getProperty(CALYPSO_ENGINE_MANAGER_CONFIG);
		String engineServerNameDV = getDomainESIfNotLoaded(reportType, exportReport, dsConn);
		AbstractReportDataWS data;


		if (!Util.isEmpty(engineServerName) && !Util.isEmpty(engineServerNameDV)
				&& engineServerNameDV.trim().equals(engineServerName.trim())) {
			try {

				Report rep = createReport(reportType, exportReport, dsConn);
				JsonObject jsobj = getJSonObj(inputJson);

				if (setFilters(jsobj, rep)) {

					DefaultReportOutput ro = null;
					JavaBeanReportViewer viewer = new JavaBeanReportViewer();
					
					Log.info(this, "Running report " + reportType + " " + exportReport);
					Vector<String> errorMsgs = new Vector<String>();

					data = getDataClassInstance(reportType, exportReport);
					if (data == null) {
						Log.error(this, "The report data class is not found");
						return Response.serverError().entity("The request cannot be answered").build();
					}
					ro = data.exportReport(rep, errorMsgs);
					if (ro == null || ro.getNumberOfRows() <= 0) {
						Log.error(this, "Information could not be retrieved. " + errorMsgs.toString());
						return Response.accepted("Information could not be retrieved. " + errorMsgs.toString()).build();
					}
					ro.format(viewer);
					List<Object> array = viewer.getBeanList();
					HashMap<String, List<Object>> result = new HashMap<>();

					
					result.put(exportReport, array);

					Log.info(this, "ExecuteReport ends ok.");
					if (!errorMsgs.isEmpty()) {
						return Response.status(Status.BAD_REQUEST).entity(errorMsgs).build();

					}
					return Response.ok(result).build();
				} else {
					Log.error(this, "Invalid input report filters. ");
					return Response.status(Status.BAD_REQUEST).build();
				}
			} catch (RemoteException e) {
				Log.error(this, "Error loading report template " + exportReport + " of type " + reportType, e);
			} catch (InterruptedException e) {
				Log.error(this, e);
				return Response.serverError().build();
			} catch (TimeoutException e) {
				Log.error(this, "Too many concurrent request. Timeout exceded", e);
				return Response.status(Status.CONFLICT).entity("Too many concurrent request. Try again in a few minutes.").build();
			}
			Log.error(this, "ExecuteReport ends with errors. Returning server error.");
			return Response.serverError().build();
		} else {
			Log.info(this, "ExecuteReport ends ko because request is not acceptable.");
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
	}

	private boolean setFilters(JsonObject jsobj, Report rep) {

		if (jsobj != null) {
			JsonArray jArr = jsobj.getJsonArray("Filters");
			if (jArr != null) {
				for (int i = 0; i < jArr.size(); i++) {
					String filterName = jArr.getJsonObject(i).getString("Name");
					if (filterName.equals("KeywordConditions")) {
						JsonArray jArr2 = jArr.getJsonObject(i).getJsonArray("Value");
						getKeywordCondition(jArr2, rep);
					} else if(filterName.equals(XFER_ATT)) {
						JsonArray jArr2 = jArr.getJsonObject(i).getJsonArray("Value");
						getXferAttributes(jArr2, rep);
					} else {
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

				}
				return true;
			} else {
				return false;
			}
		}

		return true;
	}

	private boolean getGroupBy(JsonObject jsob, List<String> groupBy) {

		JsonArray jArr = jsob.getJsonArray("GroupBy");
		if (jArr != null) {
			for (int i = 0; i < jArr.size(); i++) {
				String filterName = jArr.getJsonObject(i).getString("Name");
				groupBy.add(filterName);

			}
		}

		return true;
	}

	private boolean getOrderBy(JsonObject jsob, List<String> orderBy) {

		JsonArray jArr = jsob.getJsonArray("OrderBy");
		if (jArr != null) {
			for (int i = 0; i < jArr.size(); i++) {
				String filterName = jArr.getJsonObject(i).getString("Name");
				orderBy.add(filterName);

			}
		}

		return true;
	}

	private int getPageNumber(JsonObject jsobj) {

		if (jsobj != null) {
			JsonArray jArr = jsobj.getJsonArray("PageNumber");
			if (jArr != null) {
				for (int i = 0; i < jArr.size(); i++) {
					String filterName = jArr.getJsonObject(i).getString("Name");
					try {
						return Integer.parseInt(filterName) == 0 ? 1 : Integer.parseInt(filterName);
					} catch (Exception e) {
						Log.error(this, e);
					}

				}
			}
		}

		return 1;
	}

	private int getPageSize(JsonObject jsobj) {

		if (jsobj != null) {
			JsonArray jArr = jsobj.getJsonArray("PageSize");
			if (jArr != null) {
				for (int i = 0; i < jArr.size(); i++) {
					String filterName = jArr.getJsonObject(i).getString("Name");
					try {
						return Integer.parseInt(filterName) == 0 ? 1 : Integer.parseInt(filterName);
					} catch (Exception e) {
						Log.error(this, e);
					}

				}
			}
		}
		return 1;

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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private AbstractReportDataWS getDataClassInstance(String reportType, String reportTemplate) {

		AbstractReportDataWS data = null;
		String className = "tk.report.restservices." + reportType + "ReportDataWS";
		try {

			Class[] type = { String.class, String.class };
			Class classDefinition = InstantiateUtil.getClass(className, true);
			Constructor cons = classDefinition.getDeclaredConstructor(type);
			data = (AbstractReportDataWS) cons.newInstance(reportType, reportTemplate);

			return data;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException
				| IllegalArgumentException | InvocationTargetException e) {
			Log.error(this, e);
			return data;
		}
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

	/**
	 * Create a ConditionTree from the request filters to be able to filter by TradeKeyword
	 * @param jArr array containing the filter information
	 * @param rep report
	 */
	private void getKeywordCondition(JsonArray jArr, Report rep) {

		//This information will allways be the same
		List<ConditionTreeNode> list = new ArrayList<ConditionTreeNode>();
		ConditionTree tree = new ConditionTree();
		ConditionTreeNode node = tree.getRoot();
		node.setOperatorId("ConditionRoot");
		ConditionTreeNode childNode = new ConditionTreeNode();
		node.setOperands(new Object[] { childNode });
		childNode.setOperatorId("&&");
		
		//Request data provided
		if(jArr != null && !jArr.isEmpty()) {
			for (int i = 0; i < jArr.size(); i++) {
				try {
					ConditionTreeNode comparatorNode = new ConditionTreeNode();
					String operator = jArr.getJsonObject(i).getString("Operator");
					String property = jArr.getJsonObject(i).getString("Property");
					JsonArray jArr2 = jArr.getJsonObject(i).getJsonArray("Operand");
					if(jArr2 != null && !jArr2.isEmpty()) {
						List<String> operandList = jArr2.stream().map(s -> s.toString().replaceAll("\"", "")).collect(Collectors.toList());
						String[] operands = operandList.toArray(new String[operandList.size()]);
						comparatorNode.setOperands(operands);
						comparatorNode.setOperatorId(operator);
						comparatorNode.setPropertyId(property);
					} else {
						comparatorNode.setOperands(null);
						comparatorNode.setOperatorId(operator);
						comparatorNode.setPropertyId(property);
					}
					
					list.add(comparatorNode);
				} catch(ClassCastException e) {
					Log.error(this,"Error geting keyword filters from body request",e);
					return;
				}
				
			}
			childNode.setOperands(list.toArray()); //set the conditions to the object
		}
		ReportTemplate template = rep.getReportTemplate();
		template.put("KeywordConditions", tree);
		rep.setReportTemplate(template); //Needs to refresh the Keyword information
	}
	
	/**
	 * Set xfer Attributes filter included in the request body
	 * @param jArr
	 * @param rep
	 */
	private void getXferAttributes(JsonArray jArr, Report rep) {

		ReportTemplate template = rep.getReportTemplate();
		Attributes att = rep.getReportTemplate().getAttributes(XFER_ATT);
		// Request data provided
		if (jArr != null && !jArr.isEmpty()) {
			for (int i = 0; i < jArr.size(); i++) {
				try {
					String name = jArr.getJsonObject(i).getString("Name");
					JsonArray value = jArr.getJsonObject(i).getJsonArray("Value");
					String val = Util.arrayToString(value.toArray()).replaceAll("\"", "").replaceAll(",", "|");
					att.put(name, val);

				} catch (ClassCastException e) {
					Log.error(this, "Error geting keyword filters from body request", e);
					return;
				}

			}
			template.put(XFER_ATT, att); // Set the attributtes
			rep.setReportTemplate(template);
		}
	}
//
//	public void exec() {
//
////			DSConnection dscon = ConnectionUtil.connect("bonos", "bonos", "Navigator", "DEV71");
//
//			String text = "{\n" + "  \"PageNumber\": [\n" + "    {\n" + "      \"Name\": \"1\"\n" + "    }\n" + "  ],\n"
//					+ "  \"PageSize\": [\n" + "    {\n" + "      \"Name\": \"9\"\n" + "    }\n" + "  ],\n"
//					+ "  \"Filters\": [\n" + "        {\n" + "            \"Name\": \"MCE_ID\",\n"
//					+ "            \"Value\": [\"10073507\"]\n" + "        }\n" + "    ]\n" + "}";
//			InputStream inputJson = new StringBufferInputStream(text);
//
//		executeFilteredReport("MarginCallDetail", "MarginCallUnderlyingsDP", inputJson);
//
//
//	}
}
