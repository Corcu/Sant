package calypsox.tk.bo;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.SWIFTFileReader;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.mo.TradeFilterCriterion;
import com.calypso.tk.product.secfinance.triparty.TripartyAllocationRecord;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.SecFinanceTripartyServiceUtil;
import com.calypso.tk.util.TradeArray;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReverseTripartyContractsFileReader extends SWIFTFileReader {
	
	protected static final SimpleDateFormat prepDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	protected static final SimpleDateFormat shortDateTimeFormat = new SimpleDateFormat("ddMMyyyyHHmm");
	
	protected static final List<String> tripartyAgentRoles = Arrays.asList("TripartyAgent", "TripartyEurexGCAgent","ALL","Agent");
	protected static final List<String> ProcessingOrgRoles = Arrays.asList("ProcessingOrg","ALL");
	protected static final List<String> contactTypes = Arrays.asList("Default","ALL");

	public static final String FILTER_PO_NAME = "PO Name";
	public static final String FILTER_LE_NAME = "LE Name";
	public static final String FILTER_CONTRACT_TYPES = "Contract Types";
	public static final String FILTER_CONTRACT_GROUPS = "Contract Groups";
	public static final String FILTER_CONTRACT_FILTER = "Contract Filter";
	public static final String FILTER_CONTRACTS_IDS = "Contracts Ids";
	public static final String FILTER_STATUS = "Status";
	public static final String FILTER_PROCESSING_TYPES = "Processing Types";
	public static final String FILTER_DIRECTION = "Direction";
	public static final String REPO_TRIPARTY = "isRepoTriparty";
	
	
	public static final String DATETIME = "DateTime";
	
	public static final String DEFAULT_BIC = "";
	public static final String OFFSETPOS="OFFSETPOS";
	
	
	public String getSwiftCode(LegalEntity le, List<String> roles) {
		
		for(String role : roles) {
			for(String contactType : contactTypes) {
				LEContact contact = BOCache.getContact(DSConnection.getDefault(), role, le, contactType, LEContact.ALL, 0);
				if(contact!=null && contact.getSwift()!=null)
					return formatSwiftCode(contact.getSwift());
			}
		}
		return formatSwiftCode(DEFAULT_BIC);
	}
	
	public String getAttributeAsString (Map<String, ?> attributes, String attributeName) {
		if(attributes.containsKey(attributeName))
			return ""+attributes.get(attributeName);
		return null;
	}
	public boolean getAttributeAsBoolean (Map<String, ?> attributes, String attributeName) {
		if(attributes.containsKey(attributeName))
			return Boolean.parseBoolean(String.valueOf(attributes.get(attributeName)));
		return false;
	}
	
	@Override
	public Vector<ExternalFile> readExternalFile(Map<String, ?> attributes) {
		Vector<ExternalFile> result = new Vector<ExternalFile>();
		boolean isRepoTriparty = getAttributeAsBoolean(attributes, REPO_TRIPARTY);
		if(isRepoTriparty){
			result = processRepoTriparty(attributes);
		}else {
			result = processTripartyMarginCallContract(attributes);
		}

		return result;
	}


	protected Vector<ExternalFile> processRepoTriparty(Map<String, ?> attributes){
		Vector<ExternalFile> result = new Vector<ExternalFile>();
		String allContractIds = getAttributeAsString(attributes, FILTER_CONTRACTS_IDS);

		TradeFilter filter = new TradeFilter();
		TradeFilterCriterion test = new TradeFilterCriterion();
		test.setName("TRADE_ID_LIST");
		test.setValues(new Vector<>(Util.stringToList(allContractIds)));
		filter.addCriterion(test);

		try {
			final TradeArray trades = DSConnection.getDefault().getRemoteTrade().getTrades(filter, null);
			for(Trade trade : trades.getTrades()){
				ArrayList<ExternalFile> externalFiles = getExternalFilesFromRepoTrade(attributes, trade);
				result.addAll(externalFiles);
				for(ExternalFile externalFile : externalFiles ) {
					if(externalFile!=null) {
						StringBuilder strBuilder = new StringBuilder();
						for(String line : externalFile.getRecords()) {
							strBuilder.append(line+"\r\n");
						}
						Log.info(LOG_CATEGORY, strBuilder.toString());
					}
				}
			}
		} catch (CalypsoServiceException e) {
			Log.error(this,"Error: " + e);
		}
		return result;
	}

	/**
	 * @param attributes
	 * @return
	 */
	private Vector<ExternalFile> processTripartyMarginCallContract(Map<String, ?> attributes){
			Vector<ExternalFile> result = new Vector<ExternalFile>();

			String poNames = getAttributeAsString(attributes,FILTER_PO_NAME);
			String leNames = getAttributeAsString(attributes, FILTER_LE_NAME);
			String contractTypes = getAttributeAsString(attributes, FILTER_CONTRACT_TYPES);
			String contractGroups = getAttributeAsString(attributes, FILTER_CONTRACT_GROUPS);
			String contractFilter = getAttributeAsString(attributes, FILTER_CONTRACT_FILTER);
			String allContractIds = getAttributeAsString(attributes, FILTER_CONTRACTS_IDS);
			String status = getAttributeAsString(attributes, FILTER_STATUS);
			String processingTypes = getAttributeAsString(attributes, FILTER_PROCESSING_TYPES);
			String direction = getAttributeAsString(attributes, FILTER_DIRECTION);

			MarginCallConfigFilter filter = new MarginCallConfigFilter();
			filter.setProcessingOrg(poNames);
			filter.setLegalEntity(leNames);
			filter.setContractTypes(Util.string2Vector(contractTypes));
			filter.setContractGroups(Util.string2Vector(contractGroups));
			filter.setContractFilters(contractFilter);

			List<Integer> allContracts =  new ArrayList<Integer>();
			for(String contractStr : Util.string2Vector(allContractIds))
				allContracts.add(Integer.parseInt(contractStr));

			filter.setContractIds(allContracts);
			filter.setStatuses(Util.string2Vector(status));
			filter.setProcessingTypes(Util.string2Vector(processingTypes));
			filter.setEntryDirections(Util.string2Vector(direction));

			try {
				for(CollateralConfig config :ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigs(filter,ServiceRegistry.getDefaultContext()) ) {
					ArrayList<ExternalFile> externalFiles = getExternalFilesFromContract(attributes, config);
					result.addAll(externalFiles);
					for(ExternalFile externalFile : externalFiles ) {
						if(externalFile!=null) {
							StringBuilder strBuilder = new StringBuilder();
							for(String line : externalFile.getRecords()) {
								strBuilder.append(line+"\r\n");
							}
							Log.info(LOG_CATEGORY, strBuilder.toString());
						}
					}
				}
			} catch (CollateralServiceException e) {
				Log.error(LOG_CATEGORY, e);
			}

		return result;
	}
	
	public static String encodeString(String string) {
		return encodeInteger(Integer.parseInt(string));
	}
	
	public static String encodeInteger(int integer) {
		       return Integer.toString(integer, 36).toUpperCase();
	}
	
	public String formatSwiftCode(String bic) {
		return SwiftUtil.formatBIC(bic);
	}

	
	public String getPREPDate(String dateTime) {
		if(dateTime!=null) {
			return prepDateFormat.format(JDatetime.valueOf(dateTime));
		}
		return prepDateFormat.format(JDate.getNow().getDate());
	}
	
	public String getShortDateTime(String dateTime) {
		if(dateTime!=null) {
			return shortDateTimeFormat.format(JDatetime.valueOf(dateTime));
		}
		return shortDateTimeFormat.format(JDate.getNow().getDate());
	}

	protected Collection<TripartyAllocationRecord> getAllExistingAllocations(Trade trade) {
		String whereClause="triparty_allocation_records.trade_id = ?";
		ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
		bindVariables.add(new CalypsoBindVariable(3000, trade.getLongId()));
		try {
			return SecFinanceTripartyServiceUtil.getSecFinanceTripartyServer().getTripartyAllocationRecords(whereClause, null, bindVariables);
		} catch (CalypsoServiceException e) {
			Log.error(LOG_CATEGORY, e);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	protected Collection<TripartyAllocationRecord> getAllExistingAllocations(CollateralConfig config) {
		String whereClause="triparty_allocation_records.margin_call_contract = ?";
		ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
		bindVariables.add(new CalypsoBindVariable(4, config.getId()));
		try {
			return SecFinanceTripartyServiceUtil.getSecFinanceTripartyServer().getTripartyAllocationRecords(whereClause, null, bindVariables);
		} catch (CalypsoServiceException e) {
			Log.error(LOG_CATEGORY, e);
		}
		return null;
	}
	
	protected Collection<TripartyAllocationRecord> getNewTripartyAllocationRecords(Collection<TripartyAllocationRecord> existingAllocationRecords) {
		HashMap<String, TripartyAllocationRecord> uniqueNewTripartyAllocationRecords = new HashMap<String, TripartyAllocationRecord>();
		for(TripartyAllocationRecord record : existingAllocationRecords) {
			TripartyAllocationRecord newAllocationRecord = new TripartyAllocationRecord();
			String key="";

			newAllocationRecord.setAgentId(record.getAgentId());
			key+=newAllocationRecord.getAgentId();
			newAllocationRecord.setGenericIdentifier(record.getGenericIdentifier());
			key+=newAllocationRecord.getGenericIdentifier();
			newAllocationRecord.setTransactionCcy(record.getTransactionCcy());
			key+=newAllocationRecord.getTransactionCcy();
			newAllocationRecord.setRepoType(record.getRepoType());
			key+=newAllocationRecord.getRepoType(); 
			newAllocationRecord.setBookId(record.getBookId());
			newAllocationRecord.setMargingCallContractId(record.getMargingCallContractId());
			key+=newAllocationRecord.getMargingCallContractId(); 
			uniqueNewTripartyAllocationRecords.put(key, newAllocationRecord);
		}
		return uniqueNewTripartyAllocationRecords.values();
	}

	protected Collection<TripartyAllocationRecord> getNewRepoTripartyAllocationRecords(Collection<TripartyAllocationRecord> existingAllocationRecords) {
		HashMap<String, TripartyAllocationRecord> uniqueNewTripartyAllocationRecords = new HashMap<String, TripartyAllocationRecord>();
		for(TripartyAllocationRecord record : existingAllocationRecords) {
			TripartyAllocationRecord newAllocationRecord = new TripartyAllocationRecord();
			String key="";

			newAllocationRecord.setAgentId(record.getAgentId());
			key+=newAllocationRecord.getAgentId();
			newAllocationRecord.setGenericIdentifier(record.getGenericIdentifier());
			key+=newAllocationRecord.getGenericIdentifier();
			newAllocationRecord.setTransactionCcy(record.getTransactionCcy());
			key+=newAllocationRecord.getTransactionCcy();
			newAllocationRecord.setRepoType(record.getRepoType());
			key+=newAllocationRecord.getRepoType();
			newAllocationRecord.setBookId(record.getBookId());
			newAllocationRecord.setTradeLongId(record.getTradeLongId());
			key+=newAllocationRecord.getTradeLongId();

			uniqueNewTripartyAllocationRecords.put(key, newAllocationRecord);
		}
		return uniqueNewTripartyAllocationRecords.values();
	}
	
	
	protected Vector<String> getFileLinesFromNewAllocation(String dateTimeStr, TripartyAllocationRecord allocationRecord) {
		
		Book book= BOCache.getBook(DSConnection.getDefault(), allocationRecord.getBookId());
		final long tradeLongId = allocationRecord.getTradeLongId();

		LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(),book.getProcessingOrgBasedId());
		LegalEntity agent =  BOCache.getLegalEntity(DSConnection.getDefault(),allocationRecord.getAgentId());
		
		String poSwiftCode = getSwiftCode(po, ProcessingOrgRoles);
		String agentSwiftCode = getSwiftCode(agent, tripartyAgentRoles);
		String ccy = allocationRecord.getTransactionCcy();


		Vector<String> fileLines = new Vector<String>();
		fileLines.add("{1:F01"+poSwiftCode+"0000000000}{2:O5690000000000"+agentSwiftCode+"00000000000000000000N}{3:{108:XXXX000000000000}}{4:");
		fileLines.add(":16R:GENL");
		fileLines.add(":28E:1/ONLY"); // changed
		fileLines.add(":13A::STAT//XXX");
		fileLines.add(":20C::SEME//"+OFFSETPOS);
		fileLines.add(":23G:NEWM");
		fileLines.add(":98C::PREP//"+getPREPDate(dateTimeStr));
		fileLines.add(":22H::REPR//"+allocationRecord.getRepoType());
		fileLines.add(":22F::STBA//EOSP");
		fileLines.add(":22F::SFRE//INDA");
		fileLines.add(":16R:COLLPRTY");
		fileLines.add(":95R::XXXX/XXXX/XXXX");
		fileLines.add(":16S:COLLPRTY");
		fileLines.add(":16S:GENL:16R:COLLPRTY");
		fileLines.add(":95R::XXXX/XXXX/XXXX");
		fileLines.add(":16S:COLLPRTY");
		fileLines.add(":16S:GENL:16R:COLLPRTY");
		fileLines.add(":95R::XXXX/XXXX/XXXX");
		fileLines.add(":16S:COLLPRTY:16R:SUMM");
		fileLines.add(":19A::TEXA//"+ccy+"0,");
		fileLines.add(":19A::TCOR//"+ccy+"0,");
		fileLines.add(":19A::COVA//"+ccy+"0,");
		fileLines.add(":19A::MARG//"+ccy+"0,");
		fileLines.add(":92A::MARG//0,00");
		fileLines.add(":98C::VALN//"+getPREPDate(dateTimeStr));
		fileLines.add(":16S:SUMM");
		fileLines.add(":16R:SUME");
		fileLines.add(":22F::COLA//SLOA");
		fileLines.add(":19A::TEXA//"+ccy+"0,");
		fileLines.add(":19A::TCOR//"+ccy+"0,");
		fileLines.add(":19A::COVA//"+ccy+"0,");
		fileLines.add(":19A::MARG//"+ccy+"0,");
		fileLines.add(":19A::TVOC//"+ccy+"0,");
		fileLines.add(":19A::TVRC//"+ccy+"0,");
		fileLines.add(":92A::MARG//0,00");
		fileLines.add(":16R:SUMC");
		fileLines.add(":13B::ELIG//AMIAFULL");
		fileLines.add(":95R::XXXX/XXXX/XXXX");
		fileLines.add(":95R::XXXX/XXXX/XXXX");
		fileLines.add(":19A::TEXA//"+ccy+"0,");
		fileLines.add(":19A::TCOR//"+ccy+"0,");
		fileLines.add(":19A::COVA//"+ccy+"0,");
		fileLines.add(":19A::MARG//"+ccy+"0,");
		fileLines.add(":19A::TVOC//"+ccy+"0,");
		fileLines.add(":19A::TVRC//"+ccy+"0,");
		fileLines.add(":92A::MARG//0,");
		fileLines.add(":16R:TRANSDET");
		if(tradeLongId>0){
			fileLines.add(":20C::CLTR//"+tradeLongId);
		}else {
			fileLines.add(":20C::CLTR//"+encodeInteger(allocationRecord.getMargingCallContractId())+"--"+OFFSETPOS);
		}
		fileLines.add(":20C::TCTR//"+getShortDateTime(dateTimeStr));
		fileLines.add(":98B::TERM//OPEN");
		fileLines.add(":98C::EXRQ//"+getPREPDate(dateTimeStr));
		fileLines.add(":19A::TEXA//"+ccy+"0,");
		fileLines.add(":19A::TCOR//"+ccy+"0,");
		fileLines.add(":19A::COVA//"+ccy+"0,");
		fileLines.add(":19A::MARG//"+ccy+"0,");
		fileLines.add(":19A::TCFA//"+ccy+"0,");
		fileLines.add(":92A::MARG//0,");
		fileLines.add(":92A::PRIC//0,");
		fileLines.add(":25D::TREX//INTD");
		fileLines.add(":16S:TRANSDET");
		fileLines.add(":16S:SUMC");
		fileLines.add(":16S:SUME");
		fileLines.add("-}");
		
		return fileLines;
		
	}
	
	
	protected ArrayList<ExternalFile> getExternalFilesFromContract(Map<String, ?> attributes, CollateralConfig config) {
		
		ArrayList<ExternalFile> files = new ArrayList<ExternalFile>();
		
		Object dateTime = attributes.get(DATETIME);
		String dateTimeStr = null;
		if(dateTime!=null) 
			dateTimeStr=(String)dateTime;	
			Collection<TripartyAllocationRecord> allocations = getAllExistingAllocations(config);;
			for(TripartyAllocationRecord newAllocation : getNewTripartyAllocationRecords(allocations) ) {
				Vector<String> fileLines = getFileLinesFromNewAllocation(dateTimeStr, newAllocation);
				ExternalFile newFile = this.createExternalFile("", null, fileLines,false);
				files.add(newFile);
			}
			
			return files;
	}

	protected ArrayList<ExternalFile> getExternalFilesFromRepoTrade(Map<String, ?> attributes, Trade trade) {
		ArrayList<ExternalFile> files = new ArrayList<ExternalFile>();
		Object dateTime = attributes.get(DATETIME);
		String dateTimeStr = null;
		if(dateTime!=null)
			dateTimeStr=(String)dateTime;
		Collection<TripartyAllocationRecord> allocations = getAllExistingAllocations(trade);;
		for(TripartyAllocationRecord newAllocation : getNewRepoTripartyAllocationRecords(allocations) ) {
			Vector<String> fileLines = getFileLinesFromNewAllocation(dateTimeStr, newAllocation);
			ExternalFile newFile = this.createExternalFile("", null, fileLines,false);
			files.add(newFile);
		}

		return files;
	}
	
}

