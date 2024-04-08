package com.isban.api.webservice;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.calypso.jaxb.xml.CustomCollateralAllocations;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.CalypsoEntities;
import com.calypso.tk.publish.jaxb.CalypsoEntity;
import com.calypso.tk.publish.jaxb.CollateralAllocation;
import com.calypso.tk.publish.jaxb.Error;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.DomainValues.DomainValuesRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.DataUploaderUtil;
import com.calypso.tk.util.TransferArray;

import calypsox.tk.bo.netting.BODigitalPlatformManualNettingHandler;
import calypsox.webservices.annotations.PATCH;
import calypsox.webservices.annotations.SgtEndpoint;

/**
 * @author x660030 Webservice to execute some reports of Calypso to get
 *         information.
 */
@SgtEndpoint
@Path("/dataUploader")
public class DataUploaderWS {
	private static final String SEPARATOR = ",";
	private static final String DECIMALFORMAT = "#0.0#";
	private static final String CALYPSO_ENGINE_MANAGER_CONFIG = "calypso.engine.manager.config";
	private static ConcurrentHashMap<String, String> engineServerNameDVHash = new ConcurrentHashMap<>();
	private static final String DVENGINESERVERWS = "DVEngineServerWSCDUF";

	public DataUploaderWS() {
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
	@POST
	@Path("/upload/{dataUploaderType}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response uploadCDUF(@PathParam("dataUploaderType") String dataUploaderType, InputStream inputJson) {
		Log.info(this, "Importing dataUploader JSON object of type " + dataUploaderType + ".");

		DSConnection dsConn = getDSConnection();

		String engineServerName = System.getProperty(CALYPSO_ENGINE_MANAGER_CONFIG);
		String engineServerNameDV = getDomainESIfNotLoaded(dataUploaderType, dsConn);
		if (!Util.isEmpty(engineServerName) && !Util.isEmpty(engineServerNameDV)
				&& engineServerNameDV.trim().equals(engineServerName.trim())) {
			if (dataUploaderType.equals("NETTING")) {
				CalypsoAcknowledgement ca = new CalypsoAcknowledgement();
				if (nettingTransfers(inputJson, dataUploaderType, ca)) {
					if (ca.getRejected() > 0) {
						return Response.accepted(ca).build();
					} else {
						return Response.ok(ca).build();
					}
				} else {
					Log.error(this, "Bad Netting arguments");
					return Response.status(Status.BAD_REQUEST).entity("Bad Netting arguments").build();
				}
			}
			try {
				StringBuilder csvmessage = getHeaderAndRow(inputJson, dataUploaderType);
				CalypsoAcknowledgement ca = null;
				if (csvmessage != null) {
					if ("CollateralAllocation".equalsIgnoreCase(dataUploaderType)) {
						ca = uploadCorporateAction(dataUploaderType, dsConn, csvmessage);
					} else {
						ca = DataUploaderUtil.uploadCSV(csvmessage.toString(), dataUploaderType, SEPARATOR, null, null);
					}
				}
				if (ca != null) {
					Log.info(this, ca.toString());
					if (ca.getRejected() > 0) {
						return Response.accepted(ca).build();
					} else {
						return Response.ok(ca).build();
					}
				}

			} catch (Exception e) {
				Log.error(this, "Error importing CDUF object " + dataUploaderType, e);
			}

			Log.error(this, "ExecuteReport ends with errors. Returning server error.");
			return Response.serverError().build();
		} else {
			Log.info(this, "ExecuteReport ends ko because request is not acceptable.");
			return Response.status(Status.NOT_ACCEPTABLE).build();
		}
	}

	@PATCH
	@Path("/update/{dataUploaderType}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response updateCDUF(@PathParam("dataUploaderType") String dataUploaderType, InputStream inputJson) {
		return uploadCDUF(dataUploaderType, inputJson);
	}

	private StringBuilder getHeaderAndRow(InputStream inputJson, String type) {
		StringBuilder resul = new StringBuilder();

		if (inputJson != null) {
			JsonObject jsobj = getJSonObj(inputJson);
			if (jsobj != null) {
				JsonArray jArr = jsobj.getJsonArray(type);
				if (jArr != null) {
					for (int i = 0; i < jArr.size(); i++) {
						StringBuilder row = new StringBuilder();
						Set<String> headerList = jArr.getJsonObject(i).keySet();
						if (resul.length() == 0) {
							resul.append(String.join(SEPARATOR, headerList));
						}
						for (String headField : headerList) {

							if (jArr.getJsonObject(i).getValueType().equals(JsonValue.ValueType.NUMBER)) {
								double rowValue = jArr.getJsonObject(i).getJsonNumber(headField).doubleValue();
								if (row.length() > 0) {
									row.append(SEPARATOR);
								}
								DecimalFormat df = new DecimalFormat(DECIMALFORMAT);
								row.append(df.format(rowValue));
							} else {

								String rowValue = jArr.getJsonObject(i).getJsonString(headField).getString();
								rowValue = rowValue.replace(",", ";comma;");
								if (row.length() > 0) {
									row.append(SEPARATOR);
								}
								row.append(rowValue);

							}
						}

						resul.append("\n");
						resul.append(row);
					}
					return resul;
				}
			}
		}
		return null;

	}

	private boolean nettingTransfers(InputStream inputJson, String type, CalypsoAcknowledgement ca) {
		boolean resul = false;
		if (inputJson != null) {
			JsonObject jsobj = getJSonObj(inputJson);
			if (jsobj != null) {
				JsonArray jArr = jsobj.getJsonArray(type);
				if (jArr != null) {
					long[] arrXferId = new long[jArr.size()];
					for (int i = 0; i < jArr.size(); i++) {

						Set<String> headerList = jArr.getJsonObject(i).keySet();
						for (String headField : headerList) {
							if (headField.equals("Action")) {
								String rowValue = jArr.getJsonObject(i).getJsonString(headField).getString();
								if (rowValue == null || !rowValue.equals("ASSIGN")) {
									return false;
								}
							} else if (headField.equals("EntityType")) {
								String rowValue = jArr.getJsonObject(i).getJsonString(headField).getString();
								if (rowValue == null || !rowValue.equals("BOTransfer")) {
									return false;
								}
							} else if (headField.equals("AssignTransfer.NettingType")) {
								String rowValue = jArr.getJsonObject(i).getJsonString(headField).getString();
								if (rowValue == null || !rowValue.equals("CA_PairOffCash")) {
									return false;
								}
							} else if (headField.equals("EntityId")) {
								long rowValue = jArr.getJsonObject(i).getJsonNumber(headField).longValue();
								arrXferId[i] = rowValue;
							}
						}
					}
					ArrayList<String> errors = new ArrayList<String>();
					DSConnection dsConn = getDSConnection();
					try {
						TransferArray transfers = null;
						transfers = dsConn.getRemoteBackOffice().getTransfers(arrXferId);
						BODigitalPlatformManualNettingHandler handler = new BODigitalPlatformManualNettingHandler();
						Long transferId = handler.applyActionAssignCommand(transfers, "CA_PairOffCash", errors);
						CalypsoEntity ce = new CalypsoEntity();
						CalypsoEntities ces = new CalypsoEntities();
						if (transferId != 0) {
							ca.setRejected(0);
							ca.setReceived(arrXferId.length);
							ca.setUploaded(arrXferId.length);
							ces.setRejected(0);
							ces.setReceived(arrXferId.length);
							ces.setUploaded(arrXferId.length);
							ce.setStatus("SUCCESS");
							resul = true;
						} else {
							ca.setRejected(arrXferId.length);
							ca.setReceived(arrXferId.length);
							ca.setUploaded(0);
							ces.setRejected(arrXferId.length);
							ces.setReceived(arrXferId.length);
							ces.setUploaded(0);
							ce.setStatus("ERROR");
							resul = true;
						}
						for (String error : errors) {
							Error err = new Error();
							err.setMessage(error);
							ce.getError().add(err);
						}
						ce.setEntityId(transferId);
						ce.setAction("ASSIGN");
						ce.setType("BOTransfer");

						ces.getCalypsoEntity().add(ce);
						ca.setCalypsoEntities(ces);
					} catch (CalypsoServiceException e) {
						Log.error(this, "Error getting transfers.");
					}
				}
			}
		}
		return resul;

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
	private String getDomainESIfNotLoaded(String duType, DSConnection dsConn) {

		String engineServerNameDV = "";
		String existDVES = engineServerNameDVHash.get(duType);
		if (Util.isEmpty(existDVES)) {
			try {
				DomainValuesRow row = dsConn.getRemoteReferenceData().getDomainValuesRow(DVENGINESERVERWS, duType);
				if (row != null) {
					engineServerNameDV = row.getComment();
					engineServerNameDVHash.put(duType, engineServerNameDV);
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error loading domain value " + duType + " to get Engine server configuration.", e);
			}
		} else {
			engineServerNameDV = existDVES;
		}
		return engineServerNameDV;
	}

	public DSConnection getDSConnection() {
		DSConnection dsConn = null;
		dsConn = DSConnection.getDefault();
		return dsConn;
	}

	private CalypsoAcknowledgement uploadCorporateAction(String dataUploaderType, DSConnection dsConn,
			StringBuilder csvmessage) throws CollateralServiceException, Exception {

		List<Integer> allocationErr = new ArrayList<>();
		csvmessage = parseAllocationInput(csvmessage, dsConn, allocationErr);
		CalypsoAcknowledgement ca = null;
		if (csvmessage != null)
			ca = DataUploaderUtil.uploadCSV(csvmessage.toString(), dataUploaderType, SEPARATOR, null, null);
		// Parse custom Allocation errors
		if (!allocationErr.isEmpty()) {
			List<CollateralAllocation> caList = new ArrayList<>();
			;
			if (ca == null) {
				ca = new CalypsoAcknowledgement();
			} else {
				caList.addAll(ca.getCollateralAllocations().getCollateralAllocation());
			}
			allocationErr.forEach(a -> {
				CollateralAllocation newCA = new CollateralAllocation();
				newCA.setAction("ALLOCATE");
				newCA.setStatus("Rejected");
				newCA.setId(a);
				caList.add(newCA);
			});
			CustomCollateralAllocations customCa = new CustomCollateralAllocations();
			customCa.setCollateralAllocation(caList);
			ca.setCollateralAllocations(customCa);
		}
		return ca;
	}

	private StringBuilder parseAllocationInput(StringBuilder csvmessage, DSConnection conn, List<Integer> allocationErr)
			throws CollateralServiceException {

		String[] lines = csvmessage.toString().split("\n");
		boolean content = false;
		if (lines[0] != null) {
			// Parse headers
			List<String> headers = new ArrayList<>(Arrays.asList(lines[0].split(",")));
			if (!headers.contains("ContractName") && headers.contains("Id")) {
				StringBuilder newCsvMessage = new StringBuilder();
				int idPos = headers.indexOf("Id");
				headers.set(idPos, "ContractName");
				newCsvMessage = listToStringBuilder(headers);

				// Parse and convert Amounts
				for (int x = 1; x < lines.length; x++) {
					newCsvMessage.append("\n");
					List<String> line = new ArrayList<>(Arrays.asList(lines[x].split(",")));
					int id = Integer.parseInt(line.get(idPos));
					MarginCallEntryDTO mc = ServiceRegistry.getDefault(conn).getCollateralServer().loadEntry(id);
					if (mc != null) {
						CollateralConfig cc = CacheCollateralClient.getCollateralConfig(conn,
								mc.getCollateralConfigId());
						String contractName = cc != null ? cc.getName() : null;
						line.set(idPos, contractName);
						newCsvMessage.append(listToStringBuilder(line));
						content = true;
					} else {
						allocationErr.add(id);
					}
				}
				return content ? newCsvMessage : null;
			}
		}
		return csvmessage;
	}

	private StringBuilder listToStringBuilder(List<String> headers) {
		StringBuilder newCsvLine = new StringBuilder();
		headers.stream().forEach(s -> {
			newCsvLine.append(s);
			newCsvLine.append(",");
		});
		return newCsvLine.deleteCharAt(newCsvLine.length() - 1);
	}
}
