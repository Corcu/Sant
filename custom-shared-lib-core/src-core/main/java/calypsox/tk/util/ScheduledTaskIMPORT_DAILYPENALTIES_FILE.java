package calypsox.tk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.DomainValues.DomainValuesRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.SwiftParserUtil;

public class ScheduledTaskIMPORT_DAILYPENALTIES_FILE extends ScheduledTask {

	private static final String CYO = "CYO";
	private static final String SECURITY = "SECURITY";
	private static final String DV_DAILY_PENALTY_FIELDS = "DailyPenaltyFields";
	private static final String EXT_SEPARATOR = ".";
	private static final String FILEPATH_SEPARATOR = "/";
	private static final String OUTPUT_DIR_ATTR = "OutputDir";
	private static final String INPUT_DIR_ATTR = "InputDir";
	private static final String FILE_NAME_ATTR = "File Name";
	private static final String DATEFORMAT_FILENAME = "yyyyMMdd_HHmmss";
	private static final String ID = "id-";
	private static final String KEY = "key-";
	private List<DomainValuesRow> dvFields = null;
	private static final String DAILY_PENALTY = "CSDRContingencyDailyPenalty";
	private static final String BLANK = "BLANK";

	private static final String INC_RECON = "INC_RECON";
	private static final String SWIFT = "SWIFT";

	private ArrayList<String> penaltyRefList = new ArrayList<String>();
	private ArrayList<String> penaltyComRefList = new ArrayList<String>();
	private ArrayList<Integer> linesDataXLSX = new ArrayList<Integer>();

//private static final String LOCAL_PATH = "C:\\Users\\x334424\\OneDrive - Santander Office 365\\Documents\\Fichero_Contingencia\\CSDRManualPenaltyCreationDetails.xlsx";

	protected final List<ScheduledTask.AttributeDefinition> buildAttributeDefinition() {
		return Arrays.asList(new ScheduledTask.AttributeDefinition[] {
				attribute(FILE_NAME_ATTR).description("The incoming text file name").mandatory(),
				attribute(INPUT_DIR_ATTR).description("The directory where incoming message text file is taken")
						.mandatory(),
				attribute(OUTPUT_DIR_ATTR).description("The directory where incoming message text file is saved")
						.mandatory() });
	}

	@Override
	public boolean process(DSConnection ds, PSConnection ps) {
		boolean retValue = true;
		String path = getAttribute(INPUT_DIR_ATTR);
		if (!path.endsWith(FILEPATH_SEPARATOR)) {
			path += FILEPATH_SEPARATOR;
		}
		path += getAttribute(FILE_NAME_ATTR);
		String finalPath = getAttribute(OUTPUT_DIR_ATTR);
		if (!finalPath.endsWith(FILEPATH_SEPARATOR)) {
			finalPath += FILEPATH_SEPARATOR;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT_FILENAME);
		String name = getAttribute(FILE_NAME_ATTR);
		String ext = name.substring(name.lastIndexOf(EXT_SEPARATOR));

		finalPath += name.replaceAll(EXT_SEPARATOR + ext, "") + "_" + sdf.format(getValuationDatetime()) + ext;
		try {
			dvFields = ds.getRemoteReferenceData().getDomainValuesRows(DV_DAILY_PENALTY_FIELDS);
			if (!Util.isEmpty(dvFields)) {
				List<String> keyHeaders = new ArrayList<String>();
				for (DomainValuesRow domainValuesRow : dvFields) {
					if (domainValuesRow.getValue().startsWith(KEY) || domainValuesRow.getValue().startsWith(ID)) {
						keyHeaders.add(domainValuesRow.getValue().replaceFirst(KEY, "").replaceFirst(ID, ""));
					} else {
						keyHeaders.add(domainValuesRow.getValue());
					}
				}
				Map<String, LinkedList<HashMap<String, Object>>> dataXLSX = readExcelFile(path, keyHeaders);
				retValue = createPenaltyTrade(dataXLSX, ds);
				try {
					copyAndRenameFile(path, finalPath);
					deleteOriginalFile(path);
				} catch (IOException e) {
					Log.error(this, "Error saving excel file");
					retValue = false;
				} catch (Exception e) {
					Log.error(this, "Error deleting original file");
					retValue = false;
				}
			} else {
				retValue = false;
			}
		} catch (CalypsoServiceException e1) {
			Log.error(this, e1);
			retValue = false;
		}
		return retValue && super.process(ds, ps);
	}

	private void deleteOriginalFile(String path) {
		File file = new File(path);

		// Delete file
		if (file.delete()) {
			Log.info("Delete file form original path was successful", path);
		} else {
			Log.error(this, "Cannot delete file from original path");
		}
	}

	public static void copyAndRenameFile(String rutaOrigen, String rutaDestinoConNuevoNombre) throws IOException {
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		try {
			inputStream = new FileInputStream(rutaOrigen);
			outputStream = new FileOutputStream(rutaDestinoConNuevoNombre);
			FileChannel inputChannel = inputStream.getChannel();
			FileChannel outputChannel = outputStream.getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}

	private boolean createPenaltyTrade(Map<String, LinkedList<HashMap<String, Object>>> dataXLSX, DSConnection ds) {
		int line = 0;
		for (Entry<String, LinkedList<HashMap<String, Object>>> entry : dataXLSX.entrySet()) {
			if (entry.getValue().element().size() != 0) { // No headers
				Iterator<HashMap<String, Object>> rowIterator = entry.getValue().iterator();
				while (rowIterator.hasNext()) {
					HashMap<String, Object> row = rowIterator.next();
					int lineData = linesDataXLSX.get(line);
					String penaltyRef = (String) row.get("PenaltyRef");
					String penaltyComRef = (String) row.get("PenaltyComRef");
					if (penaltyRefList.contains(penaltyRef)) { // Exist another penalty in excel with same penaltyRef
						Log.error(this, "Line " + lineData + " PenaltyRef already exists in excel");
						line++;
					} else if (penaltyComRefList.contains(penaltyComRef)) {
						Log.error(this, "Line " + lineData + " PenaltyComRef already exists in excel");
						line++;
					} else { // No problem
						penaltyRefList.add(penaltyRef);
						penaltyComRefList.add(penaltyComRef);
						line++;
						BOMessage msg = new BOMessage();
						long idMessage = setAttributesValuesMsgAndGetId(msg, row, ds); // Set attributes from file
						setRequiredMsgAttributes(msg); // Set required attributes
						try {
							BOMessage msgMT54x = DSConnection.getDefault().getRemoteBO().getMessage(idMessage);
							// Assign AgentId
							String agentStr = msg.getAttribute("Agent");
							int agentId = SwiftParserUtil.findUniqueLegalEntityUsingBIC(agentStr, "Agent");
							if (agentId > 0) {
								msg.setAttribute("AgentId", Integer.toString(agentId));
							}
							if (msgMT54x != null && (msgMT54x.getTemplateName().equals("MT540")
									|| msgMT54x.getTemplateName().equals("MT541")
									|| msgMT54x.getTemplateName().equals("MT542")
									|| msgMT54x.getTemplateName().equals("MT543"))) { // MT54x exists
								Long xferId = msgMT54x.getTransferLongId(); // Transfer id
								BOTransfer xfer = DSConnection.getDefault().getRemoteBackOffice().getBOTransfer(xferId);
								if (xfer != null) { // Transfer exists
									msg.setStatus(Status.S_NONE);
									// Table values
									msg.setTransferLongId(xferId);
									if (xfer.getTransferType().equals(SECURITY)) { // Transfer type SECURITY
										// Trade
										Trade trade = DSConnection.getDefault().getRemoteTrade()
												.getTrade(xfer.getTradeLongId());
										if (trade != null) { // Trade exists
											msg.setTradeLongId(xfer.getTradeLongId());
											msg.setAttribute(DAILY_PENALTY, "Y");
											msg.setTemplateName("MT537");
											// SENDER: Obtain MT54x data and asociate the msg
											String mt54xSenderAddressCode = msgMT54x.getSenderAddressCode();
											msg.setReceiverAddressCode(mt54xSenderAddressCode);
											int mt54xSenderContactId = msgMT54x.getSenderContactId();
											msg.setReceiverContactId(mt54xSenderContactId);
											String mt54xSenderContactType = msgMT54x.getSenderContactType();
											msg.setReceiverContactType(mt54xSenderContactType);
											int mt54xSenderId = msgMT54x.getSenderId();
											msg.setReceiverId(mt54xSenderId);
											String mt54xSenderRole = msgMT54x.getSenderRole();
											msg.setReceiverRole(mt54xSenderRole);
											// RECEIVER: Obtain MT54x data and asociate the msg
											String mt54xReceiverAddressCode = msgMT54x.getReceiverAddressCode();
											msg.setSenderAddressCode(mt54xReceiverAddressCode);
											int mt54xReceiverContactId = msgMT54x.getReceiverContactId();
											msg.setSenderContactId(mt54xReceiverContactId);
											String mt54xReceiverContactType = msgMT54x.getReceiverContactType();
											msg.setSenderContactType(mt54xReceiverContactType);
											int mt54xReceiverId = msgMT54x.getReceiverId();
											msg.setSenderId(mt54xReceiverId);
											String mt54xReceiverRole = msgMT54x.getReceiverRole();
											msg.setSenderRole(mt54xReceiverRole);
											// Inform the rest of the fields. MessageType
											msg.setMessageType(INC_RECON);
											msg.setProductFamily(msgMT54x.getProductFamily());
											msg.setProductType(msgMT54x.getProductType());
											// Action
											msg.setAction(Action.NEW);
											// SubAction
											msg.setSubAction(Action.NONE);
											// ProductType
											msg.setProductType("ALL");
											// Gateway
											msg.setGateway(SWIFT);
											// Address Method
											msg.setAddressMethod(SWIFT);
											// Creation Date
											JDate date = JDate.getNow(); // Today Date
											TimeZone hourZone = TimeZone.getDefault(); // Hour Zone
											msg.setCreationDate(JDatetime.currentTimeValueOf(date, hourZone));
											Object prepDateObj = row.get("PreparationDate");
											Object compDateObj = row.get("ComputationDate");
											JDate prepDate = null;
											if (prepDateObj instanceof Date) {
												prepDate = JDate.valueOf((Date) prepDateObj);
											}
											JDate compDate = null;
											if (compDateObj instanceof Date) {
												compDate = JDate.valueOf((Date) compDateObj);
											}
											msg.setSettleDate(com.calypso.tk.core.DateUtil.max(prepDate, compDate));
											try {
												long newId = ds.getRemoteBO().save(msg, 0, "MessageEngine"); // Save
																												// message
												Log.info(this, "Message saved: " + newId);
											} catch (CalypsoServiceException e) {
												e.printStackTrace();
												Log.error(this, "Problem saving message");
											}
										} else // Trade does not exists
										{
											Log.error(this, "Trade doesnt exists");
										}
									} else // Transfer is not type SECURITY
									{
										Log.error(this, "Transfer is not a SECURITY");
									}
								} else { // Transfer does not exists
									Log.error(this, "Transfer does not exists");
								}
							} else { // Message is not MT54x
								Log.error(this, "Message is not MT54x");
							}
						} catch (CalypsoServiceException e) {
							Log.error(this, e);
						}
					}
				}
			}
		}
		return true;
	}

	private void setRequiredMsgAttributes(BOMessage msg) {
		// Set mandatory msg attributes for CreatePenaltyTradeRule
		msg.setAttribute("Frequency_Indicator", "DAIL");
		msg.setAttribute("Message_Function", "PENA");
		msg.setAttribute("PO Account", "NONREF");
		msg.setAttribute("PenaltyCode", "CURR");
		msg.setAttribute("Penalty_Reason", "ACTV//NEWP");
		msg.setAttribute("Penalty_Status", "PNST//ACTV");
		msg.setAttribute("AutomaticTrade", "false");
		msg.setAttribute("Transfer Type", "PENALTY");
	}

	public Map<String, LinkedList<HashMap<String, Object>>> readExcelFile(String excelAbosluteFileName,
			List<String> keyHeaders) {
		HashMap<String, LinkedList<HashMap<String, Object>>> map = null;
		Workbook wb = null;
		try {
			wb = WorkbookFactory.create(new FileInputStream(excelAbosluteFileName));
			if (wb != null) {
				Sheet sheet = wb.getSheetAt(0);

				if (sheet == null) {
					Log.error(this, "Error in the process to import excel sheet in file: " + excelAbosluteFileName);
					return map;
				}
				map = getRowsAsList(sheet, keyHeaders);
			}
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
			Log.error("This file can not read: " + excelAbosluteFileName, e);
		}
		return map;
	}

	/**
	 * Get the data from the excel rows and formated it. It will obtain the map
	 * using a series of fields as a key, in this way you can create the position
	 * for the different products that affect the same account
	 * 
	 * @param sheet excel sheet to read
	 * @return data from the excel sheet
	 */
	private HashMap<String, LinkedList<HashMap<String, Object>>> getRowsAsList(Sheet sheet, List<String> keyHeaders) {
		HashMap<String, LinkedList<HashMap<String, Object>>> map = new HashMap<>();
		int numRows = sheet.getPhysicalNumberOfRows();
		ArrayList<String> header = new ArrayList<>();
		if (numRows <= 1) { // Empty file
			return map;
		}
		for (int row = 0; row < numRows; ++row) {
			boolean existPenaltyRef = false;
			boolean existPenaltyComRef = false;
			StringBuilder key = new StringBuilder();
			if (row == 0) { // Get the header names
				Row xssfRow = sheet.getRow(row);
				for (int cell = 0; cell < xssfRow.getLastCellNum(); ++cell) {
					header.add(xssfRow.getCell(cell).toString().trim().replace("\n", " ").replace("\r", " "));
				}
			} else { // Get the information
				Row xssfRow = sheet.getRow(row);
				if (xssfRow != null) {
					LinkedList<HashMap<String, Object>> data = new LinkedList<>();
					HashMap<String, Object> rowValues = new HashMap<>();
					ArrayList<Integer> failedRows = new ArrayList<Integer>();
					boolean allBlanks = true;
					int prob = checkBlankCellsAndPenaltyAttributes(xssfRow, failedRows, existPenaltyRef,
							existPenaltyComRef);
					if (prob == 1) { // PenaltyComRef exists
						existPenaltyComRef = true;
					} else if (prob == 2) { // PenaltyRef exist
						existPenaltyRef = true;
					}
					for (int cell = 0; cell < xssfRow.getLastCellNum(); ++cell) {
						if (!failedRows.contains(xssfRow.getRowNum())) { // GOOD LINE
							Cell xssfCell = null;
							Object cellValue = "";
							xssfCell = xssfRow.getCell(cell);
							if (xssfCell.getCellTypeEnum() == CellType.NUMERIC) {
								if (DateUtil.isCellDateFormatted(xssfCell)) {
									cellValue = xssfCell.getDateCellValue();
								} else {
									cellValue = xssfCell.getNumericCellValue();
								}
							} else {
								cellValue = xssfCell.getStringCellValue();
							}
							rowValues.put(header.get(cell), cellValue); // Inserts the column name and the column value
						} else { // WRONG LINE
							Cell xssfCell = xssfRow.getCell(cell);
							if (!xssfCell.getCellTypeEnum().toString().equals(BLANK)) {
								allBlanks = false;
							}
						}
					}
					for (int i = 0; i < keyHeaders.size(); i++) { // Obtain the key conformed by the first five fields
						if (i != 0) {
							key.append("-");
						}
						key.append(rowValues.get(keyHeaders.get(i)));
					}
					/*
					if (map.containsKey(key.toString())) {
						map.get(key.toString()).add(rowValues);
					} */
						if (!failedRows.contains(xssfRow.getRowNum())) { // GOOD LINE
							if(!rowValues.isEmpty()) {
								data.add(rowValues);
								map.put(key.toString(), data);
								linesDataXLSX.add(xssfRow.getRowNum()+1 );	// Add numer of line
							}
						} else { // WRONG LINE
							if (sheet.getLastRowNum() == 1 && allBlanks == false) { // There are only one line
								Log.error(this, "Line " + (xssfRow.getRowNum()+1) + " has blanks cells");
							} else if (sheet.getLastRowNum() != 1) {
								if (existPenaltyRef) { // Problem penaltyRef
									Log.error(this, "Line " + (xssfRow.getRowNum()+1)  + " PenaltyRef already exists");
								}
								else if (existPenaltyComRef) { // Problem penaltyComRef
									Log.error(this, "Line " + (xssfRow.getRowNum()+1)  + " PenaltyComRef already exists");
								} else {
									Log.error(this, "Line " + (xssfRow.getRowNum()+1)  + " has blanks cells");
								}
							}
						}
				}
			}
		}
		return map;
	}

	private int checkBlankCellsAndPenaltyAttributes(Row xssfRow, ArrayList<Integer> failedRows, boolean existPenaltyRef,
			boolean existPenaltyComRef) {
		String penaltyComRef = null;
		String penaltyRef = null;
		for (int cell = 0; cell < xssfRow.getLastCellNum(); ++cell) {
			Cell xssfCell = xssfRow.getCell(cell);
			if (failedRows.contains(xssfRow.getRowNum())) {
				break;
			}
			if (xssfCell == null || xssfCell.getCellTypeEnum().toString().equals(BLANK)) {
				// There are cells without values
				failedRows.add(xssfCell.getRowIndex()); // Line number
				return 0;
			} else if (cell == 8 || cell == 10) { // PenaltyComRef OR PenaltyRef

				if (cell == 8) {
					penaltyComRef = xssfCell.getStringCellValue();
				} else {
					penaltyRef = xssfCell.getStringCellValue();
				}

				if (penaltyComRef != null && penaltyRef != null) { // In column of PenaltyRef

					long[] idsTrades;
					try {
						idsTrades = DSConnection.getDefault().getRemoteTrade()
								.getTradeIdsByKeywordNameAndValue("PenaltyRef", penaltyRef);

						if (idsTrades.length != 0) { // Operation with this penaltyRef
							failedRows.add(xssfCell.getRowIndex()); // Line number
							existPenaltyRef = true;
							return 2;
						}
					} catch (CalypsoServiceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					long[] idsTrades2;
					boolean condition = false;
					try {
						idsTrades2 = DSConnection.getDefault().getRemoteTrade()
								.getTradeIdsByKeywordNameAndValue("PenaltyComRef", penaltyComRef);
						
						for(int i = 0; i < idsTrades2.length; i++) {
							
							Trade t = DSConnection.getDefault().getRemoteTrade().getTrade(idsTrades2[i]);
							
							String penaltyRefTrade = t.getKeywordValue("PenaltyRef");
							
							if(penaltyRefTrade==null) {		// Has PenaltyRef equals blank
								condition = true;
							}
							
							
						}
						
						

						if (idsTrades2.length != 0 && condition) { // Operation with this penaltyComRef and penaltyRef equals blank
							failedRows.add(xssfCell.getRowIndex()); // Line number
							existPenaltyComRef = true;
							return 1;
						}
					} catch (CalypsoServiceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return 0;
	}

	private Long setAttributesValuesMsgAndGetId(BOMessage msg, HashMap<String, Object> row, DSConnection ds) {
		Long ret = null;
		for (DomainValuesRow domainValuesRow : dvFields) {
			String colName = domainValuesRow.getComment();
			Object value = row.get(colName);
			if (domainValuesRow.getValue().startsWith(ID)) {
				String cellStringValue = value.toString();
				if (cellStringValue.startsWith(CYO)) {
					cellStringValue = cellStringValue.substring(3); // Delete three firsts caracters (CYO)
				}
				ret = Long.valueOf(cellStringValue);
			}
			if (value != null) {
				if (value instanceof Date) {
					msg.setAttribute(domainValuesRow.getValue().replaceFirst(KEY, "").replaceFirst(ID, ""),
							Util.idateToString(JDate.valueOf((Date) value)));
				} else if (value instanceof Double) {
					msg.setAttribute(domainValuesRow.getValue().replaceFirst(KEY, "").replaceFirst(ID, ""),
							Util.inumberToString((Double) value));
				} else {
					msg.setAttribute(domainValuesRow.getValue().replaceFirst(KEY, "").replaceFirst(ID, ""),
							value.toString());
				}
			}
		}
		return ret;
	}

	@Override
	public String getTaskInformation() {
		return "Penalties from file";
	}
}
