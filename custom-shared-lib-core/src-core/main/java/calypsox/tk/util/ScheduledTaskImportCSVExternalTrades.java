package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

import calypsox.util.collateral.CollateralUtilities;

public class ScheduledTaskImportCSVExternalTrades extends AbstractProcessFeedScheduledTask {

	private static final long serialVersionUID = 123L;

	// protected static final String FILE = "File";
	protected static final String SEPARATOR_DOMAIN_STRING = "Separator";
	protected static final String TASK_INFORMATION = "Import TRADES from a CSV file.";
	protected static Vector<String> DOMAIN_ATTRIBUTES = new Vector<String>();
	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
	protected static final String BUY_DIRECTION_STRING = "BUY";
	protected static final String SELL_DIRECTION_STRING = "SELL";
	private boolean result = false;
	private String file = "";

	// private static String PRODUCT_FLOW_TYPE = "SantProductType";
	// private static String STRUCURE_ID = "STRUCURE_ID";

	// private final RemoteReferenceData refDataServer = null;

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	/**
	 * ST attributes definition
	 */
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));

		return attributeList;
	}

	// @SuppressWarnings({ "unchecked", "rawtypes" })
	// @Override
	// public Vector<String> getDomainAttributes() {
	// final Vector domainAttributes = super.getDomainAttributes();
	// domainAttributes.add(SEPARATOR_DOMAIN_STRING);
	// return domainAttributes;
	// }

	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		final String path = getAttribute(FILEPATH);
		final String startFileName = getAttribute(STARTFILENAME);

		// We check all the files kept into the path specified in the
		// configuration for the Scheduled Task.
		final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName);

		// We check if the number of matches is 1.
		if (files.size() == 1) {
			this.file = files.get(0);
			final String filePath = path + this.file;

			try {
				if (feedPreProcess(filePath)) {
					// final String separator =
					// getAttribute(SEPARATOR_DOMAIN_STRING);
					// readAndSaveExternalTrades(filePath, separator);
					this.result = hasBadEntries();
				}
			} catch (final Exception e) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString(), e);
				this.result = false;
			}
		} else {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK,
					"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
			this.result = false;
		}

		try {
			feedPostProcess();
		} catch (final Exception e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error in the Post Process", e);
		}

		return this.result;
	}

	//
	// private void readAndSaveExternalTrades(String file, String separator){
	// BufferedReader bufferedReader=null;
	//
	// if(refDataServer==null){
	// refDataServer=getDSConnection().getRemoteReferenceData();
	// }
	//
	// try {
	// bufferedReader = new BufferedReader(new FileReader(file));
	// String line=null;
	// while( (line=bufferedReader.readLine())!=null ){
	// //TODO How to handle last line of the file ********
	//
	// try {
	// Trade tradeToSave = createTrade(line, separator);
	// getDSConnection().getRemoteTrade().save(tradeToSave);
	// } catch (InvalidExternalTradeFeedLineException e) {
	// addBadLine(line, e);
	// }
	// }
	//
	// } catch (FileNotFoundException e) {
	// Log.error(LOG_CATEGORY_SCHEDULED_TASK,
	// "Error while looking for file:"+file,e);
	// } catch (IOException e) {
	// Log.error(LOG_CATEGORY_SCHEDULED_TASK,
	// "Error while reading file:"+file,e);
	// } finally{
	// if(bufferedReader!=null){
	// try {
	// bufferedReader.close();
	// } catch (IOException e) {
	// }
	// }
	// }
	//
	// }
	//
	//
	// /**
	// * This methods parses the line and validates the fields.
	// * And creates the Trade object with Product Type ExternalTrade
	// */
	// private Trade createTrade(String line, String separator) throws
	// InvalidExternalTradeFeedLineException{
	//
	// String[] values = line.split(separator);
	// // String[] values = line.split("\\Q"+separator+"\\E");
	// for(int ii=0;ii<values.length;ii++){
	// values[ii]=values[ii].trim();
	// }
	//
	// //Example line is as below
	// //NUM_FRONT_ID(0),FO_SYSTEM(1),Owner(2),Counterparty(3),Instrument(4),Portfolio(5),ValueDate(6),Trade
	// Date(7),MaturityDate(8),
	// //Direction(9),Underlying(10),UnderlyingType(11),Nominal(12),Nominal-CcY(13),MTM(14),MTM_CCY(15),MTM_DATE(16),Last
	// Modified(17),Trade Version(18),
	// //STRUCURE_ID(19),INDEPENDENT_AMOUNT(20),INDEPENDENT_AMOUNT_CCY(21),INDEPENDENT_AMOUNT_PAY_RECEIVEl(22),
	// //CLOSING_PRICE(23),BO_REFERENCE(24),BO_SYSTEM(25)
	//
	// Trade extTradeToSave=new Trade();
	// extTradeToSave.setAction(Action.NEW);
	// ExternalTrade product=new ExternalTrade();
	// extTradeToSave.setProduct(product);
	//
	// //NUM_FRONT_ID
	// if(Util.isEmpty(values[0]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("NUM_FRONT_ID(External Reference)
	// is missing");
	// } else{
	// extTradeToSave.setExternalReference(values[0]);
	// }
	//
	// //FO_SYSTEM
	// if(Util.isEmpty(values[1]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("FO_SYSTEM(Source System) is
	// missing");
	// } else{
	// product.setSourceSystem(values[1]);
	// }
	//
	// //Owner & Portfolio
	// if(Util.isEmpty(values[2]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("Owner(Processing Org) is
	// missing");
	// } if(Util.isEmpty(values[5]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("Portfolio(Book) is missing");
	// } else{
	// try {
	// String poName=values[2];
	// String bookName=values[5];
	//
	// Book book=refDataServer.getBook(bookName);
	// LegalEntity processingOrg = refDataServer.getLegalEntity(poName);
	// if(!book.getLegalEntity().equals(processingOrg)){
	// throw new
	// InvalidExternalTradeFeedLineException("Portfolio(Book) doesn't belong to
	// Owner(Processing Org) mentioned");
	// }
	// extTradeToSave.setBook(book);
	// } catch (RemoteException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	//
	// //Counterparty
	// if(Util.isEmpty(values[3]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("Counterparty is missing");
	// } else{
	// try {
	// String cptyName=values[3];
	// LegalEntity cpty = refDataServer.getLegalEntity(cptyName);
	// extTradeToSave.setCounterParty(cpty);
	// } catch (RemoteException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	//
	// //Instrument
	// if(Util.isEmpty(values[4]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("Instrument(Product Flow Type) is
	// missing");
	// } else{
	// String instrument=values[4];
	// Vector<String> domainValues =
	// LocalCache.getDomainValues(getDSConnection(), PRODUCT_FLOW_TYPE);
	// if(!domainValues.contains(values[4])){
	// throw new
	// InvalidExternalTradeFeedLineException("Invalid Instrument(Product Flow
	// Type) value:"
	// + instrument);
	// }
	// product.setProductFlowType(instrument);
	// }
	//
	// //ValueDate
	// if(Util.isEmpty(values[6]) ){
	// throw new InvalidExternalTradeFeedLineException("ValueDate is missing");
	// } else{
	// String valDateStr=values[6];
	// try {
	// Date date = dateFormat.parse(valDateStr);
	// product.setStartDate(JDate.valueOf(date));
	// } catch (ParseException e) {
	// new InvalidExternalTradeFeedLineException("Invalid ValueDate : "+
	// valDateStr);
	// }
	// }
	//
	// //Trade Date
	// if(Util.isEmpty(values[7]) ){
	// throw new InvalidExternalTradeFeedLineException("Trade Date is missing");
	// } else{
	// String valDateStr=values[7];
	// try {
	// Date date = dateFormat.parse(valDateStr);
	// extTradeToSave.setTradeDate(new JDatetime(date));
	// } catch (ParseException e) {
	// new InvalidExternalTradeFeedLineException("Invalid Trade Date : "+
	// valDateStr);
	// }
	// }
	//
	// //MaturityDate
	// if(Util.isEmpty(values[8]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("MaturityDate is missing");
	// } else{
	// String valDateStr=values[8];
	// try {
	// Date date = dateFormat.parse(valDateStr);
	// extTradeToSave.setSettleDate(JDate.valueOf(date));
	// } catch (ParseException e) {
	// new InvalidExternalTradeFeedLineException("Invalid MaturityDate : "+
	// valDateStr);
	// }
	// }
	//
	// //Direction(9)
	// if(Util.isEmpty(values[9]) ){
	// throw new InvalidExternalTradeFeedLineException("Direction is missing");
	// } else{
	// String direction=values[9];
	// //TODO
	// }
	//
	// //Underlying(10)
	// if(Util.isEmpty(values[10]) ){
	// throw new InvalidExternalTradeFeedLineException("Underlying is missing");
	// } else{
	// product.setUnderlying(values[10]);
	// }
	//
	// //UnderlyingType(11)
	// if(Util.isEmpty(values[11]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("UnderlyingType is missing");
	// } else{
	// product.setUnderlyingType(values[11]);
	// }
	//
	// //Nominal(12)
	// if(Util.isEmpty(values[12]) ){
	// throw new InvalidExternalTradeFeedLineException("Nominal is missing");
	// } else{
	// double principal = Double.parseDouble(values[12]);
	// product.setPrincipal(principal);
	// }
	//
	// //Nominal-CCY(13)
	// if(Util.isEmpty(values[13]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("Nominal-CCY is missing");
	// } else{
	// product.setCurrencyCash(values[13]);
	// }
	//
	// //TODO Use PLMarks here
	// //MTM(14),MTM_CCY(15),MTM_DATE(16)
	//
	//
	// //Last Modified(17)
	// if(Util.isEmpty(values[17]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("Last Modified is missing");
	// } else{
	// String lastModifDateStr=values[17];
	// try {
	// Date date = dateFormat.parse(lastModifDateStr);
	// product.setLastModifDate(JDate.valueOf(date));
	// } catch (ParseException e) {
	// new InvalidExternalTradeFeedLineException("Invalid Last Modified : "+
	// lastModifDateStr);
	// }
	// }
	//
	// //Trade Version(18)
	// if(Util.isEmpty(values[18]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("Trade Version is missing");
	// } else{
	// product.setTradeVersion(Integer.parseInt(values[18]));
	// }
	//
	// //STRUCURE_ID(19)
	// if(Util.isEmpty(values[19]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("Structure ID is missing");
	// } else{
	// extTradeToSave.addKeyword(STRUCURE_ID, values[19]);
	// }
	//
	// //TODO
	// //INDEPENDENT_AMOUNT(20),INDEPENDENT_AMOUNT_CCY(21),INDEPENDENT_AMOUNT_PAY_RECEIVEl(22)
	// // Fee fee=new Fee();
	// // fee.setType("INDEPENDENT_AMOUNT");
	// // fee.setAmount(Double.parseDouble(values[20]));
	// // fee.setCurrency(values[21]);
	// // fee.setDate(extTradeToSave.getTradeDate().getJDate(TimeZone.getDefault()));
	// // extTradeToSave.getFees().add(fee);
	//
	//
	// //CLOSING_PRICE(23)
	// if(Util.isEmpty(values[23]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("CLOSING_PRICE is missing");
	// } else{
	// product.setClosingPrice( Double.parseDouble(values[23]) );
	// }
	//
	// //BO_REFERENCE(24)
	// if(Util.isEmpty(values[24]) ){
	// throw new
	// InvalidExternalTradeFeedLineException("BO_REFERENCE is missing");
	// } else{
	// product.setExtRef2(values[24]);
	// }
	//
	// //BO_SYSTEM(25)
	// if(Util.isEmpty(values[25]) ){
	// throw new InvalidExternalTradeFeedLineException("BO_SYSTEM is missing");
	// } else{
	// product.setSourceSystem2(values[25]);
	// }
	//
	// return extTradeToSave;
	// }
	//
	//
	// @Override
	// public String getFileName() {
	// return getAttribute(STARTFILENAME);
	// }

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}
}

// class InvalidExternalTradeFeedLineException extends Exception {
//
// private static final long serialVersionUID = 3360675017181427880L;
//
// public InvalidExternalTradeFeedLineException(String message){
// super(message);
// }
//
// }
