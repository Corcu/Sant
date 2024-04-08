package calypsox.tk.report;


import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import calypsox.tk.report.util.UtilReport;


public class EquityMisPlusCarteraReportStyle extends PositionKeeperReportStyle {


	private static final long serialVersionUID = 1L;

	public static final String ORIGIN = "ORIGIN";
	public static final String PROCESSDATE = "PROCESSDATE";
	public static final String PROCESSDATETIME = "PROCESSDATETIME";
	public static final String ENTITY = "ENTITY";
	public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";
	public static final String BRANCH = "BRANCH";
	public static final String BRANCH_ID = "BRANCH_ID";
	public static final String ISIN = "ISIN";
	public static final String ISINDESC = "ISINDESC";
	public static final String STATUSDEAL = "STATUSDEAL";
	public static final String SOURCESYSTEM = "SOURCESYSTEM";
	public static final String PARTENONID = "PARTENONID";
	public static final String INSTRUMENT = "INSTRUMENT";
	public static final String INSTRUMENT_ID = "INSTRUMENT_ID";
	public static final String INSTRTYPE = "INSTRTYPE";
	public static final String DIRECTION = "DIRECTION";
	public static final String CALYPSO_PRODUCT = "CALYPSO_PRODUCT";
	public static final String ISSUERGLS = "ISSUERGLS";
	public static final String ISSUERCODE = "ISSUERCODE";
	public static final String ISSUERDESC = "ISSUERDESC";
	public static final String ISSUERSECTOR = "ISSUERSECTOR";
	public static final String ISSUERCOUNTRY = "ISSUERCOUNTRY";
	public static final String ISSUERNIF = "ISSUERNIF";
	public static final String CURRENCY = "CURRENCY";
	public static final String QUANTITY = "QUANTITY";
	public static final String NOMINALQUANTITY = "NOMINALQUANTITY";
	public static final String INITIALNOMINALQUANTITY = "INITIALNOMINALQUANTITY";
	public static final String PRODUCT = "PRODUCT";
	public static final String LEGALENTITY = "LEGALENTITY";
	public static final String SYSTEM_BO = "SYSTEM_BO";
	public static final String OURCUSTODIANCODE = "OURCUSTODIANCODE";
	public static final String OURCUSTODIANACCOUNT = "OURCUSTODIANACCOUNT";
	public static final String OURCUSTODIANSWIFT = "OURCUSTODIANSWIFT";
	public static final String OURCUSTODIANGLS = "OURCUSTODIANGLS";
	public static final String OURCUSTODIANDESC = "OURCUSTODIANDESC";
	public static final String OURCUSTODIANCOUNTRY = "OURCUSTODIANCOUNTRY";
	public static final String PRICE = "PRICE";
	public static final String PRECIOMEDIO = "PRECIOMEDIO";
	public static final String AVGPRINCIPAL = "AVGPRINCIPAL";
	public static final String MARKETVALUEMAN = "MARKETVALUEMAN";
	public static final String UNREALIZED_PL = "UNREALIZED PL";
	public static final String REALIZED_PL = "REALIZED PL";
	public static final String MARKETVALUEACC = "MARKETVALUEACC";
	public static final String EQUITY_TYPE = "EQUITY_TYPE";
	public static final String EQUITY_TYPE_DESC = "EQUITY_TYPE_DESC";
	public static final String COMMON = "COMMON";
	public static final String MARKET = "MARKET";
	public static final String POSCORTALARGA = "POSCORTALARGA";
	public static final String HEDGINGTYPE = "HEDGINGTYPE";
	public static final String INDBREAKDOWN = "INDBREAKDOWN";
	//public static final String INDBREAKDOWN_REAL = "INDBREAKDOWN_REAL";
	//public static final String INDBREAKDOWN_GESTION = "INDBREAKDOWN_GESTION";
	public static final String PORTAFOLIO = "PORTAFOLIO";
	public static final String FIXING = "FIXING";
	public static final String GROSSDIVIDENDEUR = "GROSSDIVIDENDEUR";
	public static final String GROSSDIVIDEND = "GROSSDIVIDEND";
	public static final String PROVISION = "PROVISION";
	public static final String FILLER1 = "FILLER1";
	public static final String FILLER2 = "FILLER2";
	public static final String FILLER3 = "FILLER3";
	public static final String FILLER4 = "FILLER4";
	public static final String FILLER5 = "FILLER5";
	//ALM
	public static final String ENTITY_ALM = "ENTITY_ALM";
	public static final String ACCOUNTING_CENTER_ALM = "ACCOUNTING_CENTER_ALM";

	public static final ArrayList<String> emptyColumns = new ArrayList<String>();
	public static final HashMap<String,String> columnToColumn = new HashMap<String,String>();

	static {
		emptyColumns.add(OURCUSTODIANCODE);
		emptyColumns.add(OURCUSTODIANACCOUNT);
		emptyColumns.add(OURCUSTODIANSWIFT);
		emptyColumns.add(OURCUSTODIANGLS);
		emptyColumns.add(OURCUSTODIANDESC);
		emptyColumns.add(OURCUSTODIANCOUNTRY);
		emptyColumns.add(PARTENONID);
		emptyColumns.add(FIXING);
		emptyColumns.add(GROSSDIVIDENDEUR);
		emptyColumns.add(GROSSDIVIDEND);
		emptyColumns.add(PROVISION);
		emptyColumns.add(FILLER2);
		emptyColumns.add(FILLER3);
		emptyColumns.add(FILLER4);
		emptyColumns.add(FILLER5);

	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		if(emptyColumns.contains(columnName))
			return "";
		if(columnToColumn.containsKey(columnName)) {
			return getColumnValue(row,columnToColumn.get(columnName),errors);
		}

		final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
		final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
		final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

		if (columnName.equals(ORIGIN)) {
			return "800018693";
		}

		if (columnName.equals(PROCESSDATE)) {
			return formatDate(valDate);
		}

		if (columnName.equals(PROCESSDATETIME)) {
			return formatDateTime(valDateTime, pricingEnv.getTimeZone());
		}

		if (columnName.equals(ENTITY)) {
			String bookName = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Book");
			String entity = BOCreUtils.getInstance().getEntity(bookName);
			return BOCreUtils.getInstance().getEntityCod(entity, false);
		}

		if (columnName.equals(ACCOUNTING_CENTER)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			String bookName = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Book");
			String entity = BOCreUtils.getInstance().getEntity(bookName);
			if (product!=null){
				return BOCreUtils.getInstance().getCentroContable(product, entity, false);
			}
			return "";
		}

		if (columnName.equals(ENTITY_ALM)) {
			String bookName = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Book");
			String entity = BOCreUtils.getInstance().getEntity(bookName);
			return BOCreUtils.getInstance().getEntityCod(entity, true);
		}

		if (columnName.equals(ACCOUNTING_CENTER_ALM)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			String bookName = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Book");
			String entity = BOCreUtils.getInstance().getEntity(bookName);
			if (product!=null){
				return BOCreUtils.getInstance().getCentroContable(product, entity, true);
			}
			return "";
		}

		if (columnName.equals(BRANCH)) {
			String bookName = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Book");
			return BOCreUtils.getInstance().getEntity(bookName);
		}

		if (columnName.equals(BRANCH_ID)) {
			String bookName = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Book");
			Book book = null;
			LegalEntity le = null;
			try {
				book = BOCache.getBook(DSConnection.getDefault(), bookName);
				if(book != null){
					le = book.getLegalEntity();
					return le!=null ? le.getLongId() : "";
				}
			} catch (Exception e) {
				Log.error(this, "Cannot retrieve book with name = " + bookName, e);
			}
			return "";
		}

		if (columnName.equals(ISINDESC)) {
			return super.getColumnValue(row, "Description", errors);
		}

		if (columnName.equals(STATUSDEAL)) {
			return "VIVA";
		}

		if (columnName.equals(SOURCESYSTEM)) {
			return "MUREX EQ";
		}

		if (columnName.equals(INSTRUMENT)) {
			return "Equity";
		}

		if (columnName.equals(INSTRUMENT_ID)) {
			Product security = null;
			try {
				String common = ((String) super.getColumnValue(row, "Common", errors));
				security = DSConnection.getDefault().getRemoteProduct().getProductByCode("Common", common);
			}
			catch (Exception e) {
				Log.error(this, e);
				return "";
			}
			if (security == null) {
				return "";
			}
			String subType = security.getSecCode("EQUITY_TYPE");
			if(subType ==  null){
				subType = "";
			}

			String internal = "";
			DefaultReportOutput dro = (DefaultReportOutput) row.getProperty("ReportOutput");
			if (dro != null) {
				String templateName = dro.getReport().getReportTemplate().getTemplateName();
				if (!Util.isEmpty(templateName) && templateName.contains("Real")) {
					internal = "N";
				} else if (!Util.isEmpty(templateName) && templateName.contains("Gestion")) {
					internal = "Y";
				}
			}

			String portfolioStrategy = "";
			String bookName = (String) super.getColumnValue(row, "Book", errors);
			if (!Util.isEmpty(bookName)) {
				Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
				AccountingBook acctBook = book.getAccountingBook();
				if (acctBook != null) {
					portfolioStrategy = acctBook.getName();
				}
			}

			//System.out.println("ISIN: " + security.getSecCode("Common") + " - Book: " + portfolioStrategy + " - Subtype: " + subType + " - Internal: " + internal);
			String alias = BOCreUtils.getInstance().generateAlias(security, portfolioStrategy, subType, internal, bookName);
			if(Util.isEmpty(alias)){
				return "";
			}
			else{
				alias = alias.split("_")[1];
			}
			return getTypeByAlias(alias);
		}


		if (columnName.equals(INSTRTYPE)) {
			Product security = null;
			try {
				String common = ((String) super.getColumnValue(row, "Common", errors));
				security = DSConnection.getDefault().getRemoteProduct().getProductByCode("Common", common);
			}
			catch (Exception e) {
				Log.error(this, e);
				return "";
			}
			if (security == null) {
				return "";
			}
			String subType = security.getSecCode("EQUITY_TYPE");
			if(subType ==  null){
				subType = "";
			}

			String internal = "";
			DefaultReportOutput dro = (DefaultReportOutput) row.getProperty("ReportOutput");
			if (dro != null) {
				String templateName = dro.getReport().getReportTemplate().getTemplateName();
				if (!Util.isEmpty(templateName) && templateName.contains("Real")) {
					internal = "N";
				} else if (!Util.isEmpty(templateName) && templateName.contains("Gestion")) {
					internal = "Y";
				}
			}

			String portfolioStrategy = "";
			String bookName = (String) super.getColumnValue(row, "Book", errors);
			if (!Util.isEmpty(bookName)) {
				Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
				AccountingBook acctBook = book.getAccountingBook();
				if (acctBook != null) {
					portfolioStrategy = acctBook.getName();
				}
			}

			String alias = BOCreUtils.getInstance().generateAlias(security, portfolioStrategy, subType, internal, bookName);
			if(Util.isEmpty(alias)){
				return "";
			}
			else{
				alias = alias.split("_")[1];
			}
			return getSubTypeByAlias(alias);
		}

		if(columnName.equals(DIRECTION)) {
			Amount quantityAmount = (Amount)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Quantity");
			if(quantityAmount!=null) {
				if(quantityAmount.get()>=0)
					return "LARGA";
				else
					return "CORTA";
			}
		}

		if (columnName.equals(CALYPSO_PRODUCT)) {
			int productId = (int)super.getColumnValue(row, "Product Id", errors);
			Product	product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), productId);
			return product;
		}

		if (columnName.equals(ISSUERGLS)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				return le != null ? le.getCode() : "";
			}
			return "";
		}

		if (columnName.equals(ISSUERCODE)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				return le != null ? le.getLongId() : "";
			}
			return "";
		}

		if (columnName.equals(ISSUERDESC)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				return le != null ? le.getName() : "";
			}
			return "";
		}

		if (columnName.equals(ISSUERSECTOR)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				if (le != null) {
					final LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, le.getId(), "ALL", "SECTORCONTABLE");
					return attr != null ? attr.getAttributeValue() : "";
				}
			}
			return "";
		}

		if (columnName.equals(ISSUERCOUNTRY)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				if(le!=null) {
					Country country =BOCache.getCountry(DSConnection.getDefault(), le.getCountry());
					if(country!=null)
						return country.getISOCode();
				}
			}
			return "";
		}

		if (columnName.equals(ISSUERNIF)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				return le != null ? le.getExternalRef() : "";
			}
			return "";
		}

		if (columnName.equals(CURRENCY)) {
			return super.getColumnValue(row, "Currency" , errors);
		}

		if (columnName.equals(QUANTITY)) {
			return formatResult(super.getColumnValue(row, "Quantity" , errors));
		}

		if ( columnName.equals(NOMINALQUANTITY)
				|| columnName.equals(INITIALNOMINALQUANTITY)) {
			Amount quantityValue = (Amount) super.getColumnValue(row, "Quantity" , errors);
			Amount averagePrice = (Amount)  super.getColumnValue(row, "Average Price", errors);

			if(quantityValue==null || averagePrice==null)
				return formatResult(0);


			return formatResult(quantityValue.get() * Math.abs(averagePrice.get()));

		}

		if (columnName.equals(PRODUCT)) {
			return "EQ";
		}

		if (columnName.equals(LEGALENTITY)) {
			return "00001";
		}

		if (columnName.equals(SYSTEM_BO)) {
			return "CALYPSO STC";
		}

		if (columnName.equals(EQUITY_TYPE_DESC)) {
			String equityType = (String)super.getColumnValue(row, EQUITY_TYPE, errors);
			if(!Util.isEmpty(equityType)) {
				return LocalCache.getDomainValueComment(DSConnection.getDefault(), "securityCode.EQUITY_TYPE", equityType);
			}
		}

		if (columnName.equals(PRICE)) {
			Amount priceAmount = (Amount)super.getColumnValue(row, "Current Quote", errors);
			return formatResult(priceAmount);
			/*Double quote = row.getProperty(EquityMisPlusCarteraReport.DIRTY_PRICE_STR);

			if(priceAmount!=null) {
				System.out.println(priceAmount.get() +" = " + quote);

				if(quote!=null&&!quote.equals(priceAmount.get())) {
					System.out.println("error");
				}

			}


			return (quote!=null) ? formatResult(quote) : "" ;*/
		}

		if (columnName.equals(PRECIOMEDIO)) {
			return formatResult(super.getColumnValue(row, "Average Price", errors));
		}

		if (columnName.equals(AVGPRINCIPAL)) {
			return "0";
		}

		if (columnName.equals(MARKETVALUEMAN)) {
			//Amount quantityAmount =(Amount) super.getColumnValue(row, "Quantity", errors);

			Amount pvAmount =(Amount) super.getColumnValue(row, "PV", errors);
			return formatResult(pvAmount);


			/*if(quantityAmount!=null) {
				Double quantity = quantityAmount.get();
				Double quote = row.getProperty(EquityMisPlusCarteraReport.DIRTY_PRICE_STR);

				if(quantity!=null && quote!=null) {
					Double old = quantity*quote;
					if(!old.equals(pvAmount.get())) {
						System.out.println("error");
					}
				}

				return (quantity!=null && quote!=null) ? formatResult(quantity*quote) : "" ;
			}
			return "";*/
		}

		if (columnName.equals(UNREALIZED_PL)) {
			return formatResult(super.getColumnValue(row, "Unrealized", errors));
		}

		if (columnName.equals(REALIZED_PL)) {
			return formatResult(super.getColumnValue(row, "Realized", errors));
		}

		if (columnName.equals(MARKETVALUEACC)) {


			Amount realized = (Amount)super.getColumnValue(row, "Realized", errors);
			Amount unrealized = (Amount)super.getColumnValue(row, "Unrealized", errors);

			Double dRealized = 0.0d;
			Double dUnrealized = 0.0d;

			if(realized!=null)
				dRealized = realized.get();

			if(unrealized!=null)
				dUnrealized = unrealized.get();

			return formatResult(dRealized + dUnrealized);

		}

		if (columnName.equals(COMMON)) {
			return super.getColumnValue(row, "Common", errors);
		}

		if (columnName.equals(MARKET)) {
			int productId = (int)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Product Id");
			Product	product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), productId);
			if (product instanceof Equity){
				Equity equity = (Equity) product;
				return equity.getExchange();
			}
			return "";
		}

		if (columnName.equals(POSCORTALARGA)) {
			String strNominal = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Nominal").toString().replace(".","").replace(",",".");
			Double nominal = new Double(strNominal);
			return nominal<0 ? "CORTA" : "LARGA" ;
		}

		if (columnName.equals(PORTAFOLIO)) {
			DefaultReportOutput dro = (DefaultReportOutput) row.getProperty("ReportOutput");
			if (dro == null){
				return "";
			}

			String templateName = dro.getReport().getReportTemplate().getTemplateName();
			if (!Util.isEmpty(templateName) && templateName.contains("Gestion")){
				return super.getColumnValue(row, "Book", errors);
			}
			else if (!Util.isEmpty(templateName) && templateName.contains("Real")){
				Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
				String book = "";
				if (product instanceof Equity) {
					Equity equity = (Equity) product;
					String equityType = equity.getSecCode("SEC_PORTFOLIO");
					if(!Util.isEmpty(equityType) && ("CO2".equalsIgnoreCase(equityType) || "VCO2".equalsIgnoreCase(equityType))){
						book = "ESEUA_MM_1";
					}
					else{
						book = equity.getSecCode("SEC_PORTFOLIO");
					}
				}
				return (!Util.isEmpty(book)) ? book : "ESSR_REPOSMM";
			}
			return "";
		}

		if (columnName.equals(HEDGINGTYPE)) {
			String bookName = (String)super.getColumnValue(row, "Book", errors);
			if(!Util.isEmpty(bookName)) {
				Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
				AccountingBook acctBook = book.getAccountingBook();
				if(acctBook != null) {
					String acctBookName = acctBook.getName();
					if(acctBookName.equals("Negociacion")) {
						return "NE";
					}
					else if(acctBookName.equals("Disponible para la venta")) {
						return "DV";
					}
					else if(acctBookName.equals("Inversion crediticia")) {
						return "IC";
					}
					else if(acctBookName.equals("Inversion a vencimiento")) {
						return "COS";
					}
					else if(acctBookName.equals("Otros a valor razonable")) {
						return "OV";
					}
				}
			}
			return "";
		}

		if (columnName.equals(INDBREAKDOWN)) {
			DefaultReportOutput dro = (DefaultReportOutput) row.getProperty("ReportOutput");
			if (dro == null){
				return "";
			}

			String templateName = dro.getReport().getReportTemplate().getTemplateName();
			if (!Util.isEmpty(templateName) && templateName.contains("Real")){
				return "R";
			}
			else if (!Util.isEmpty(templateName) && templateName.contains("Gestion")){
				return "G";
			}
			return "";
		}

		if (columnName.equals(FILLER1)) {
			String bookName = (String)super.getColumnValue(row, "Book", errors);
			if(!Util.isEmpty(bookName)) {
				Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
				AccountingBook acctBook = book.getAccountingBook();
				if(acctBook != null) {
					String acctBookName = acctBook.getName();
					if(acctBookName.equals("Negociacion")) {
						return "NEG";
					}
					else if(acctBookName.equals("Inversion crediticia")) {
						return "COS";
					}
					else if(acctBookName.equals("Otros a valor razonable")) {
						return "OVR";
					}
				}
			}
			return "NEG";
		}

		/** if (columnName.equals(INDBREAKDOWN_REAL)) {
		 return "R";
		 }

		 if (columnName.equals(INDBREAKDOWN_GESTION)) {
		 return "G";
		 } */

		return formatResult(super.getColumnValue(row, columnName, errors));

	}


	/*@SuppressWarnings("unchecked")
	public Double getQuoteValuesFromISIN(final String quoteName, final JDate quoteDate) throws RemoteException {

		String sql = "quote_name='" + quoteName + "' and quote_date=" + Util.date2SQLString(quoteDate) + " and quote_set_name='DirtyPrice'";
		Vector<QuoteValue> quoteValues = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(sql);
		Map<String, Double> map = new HashMap<String, Double>();
		for (QuoteValue value : quoteValues) {
			return value.getClose();
		}
		return null;
	}*/


	private String formatDate(JDate jDate){
		String date = "";
		if (jDate != null) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
			date = format.format(jDate.getDate());
		}
		return date;
	}

	private String formatDateTime(JDatetime jDatetime, TimeZone tz){
		String date = "";
		if (jDatetime != null) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			date = format.format(new Date(jDatetime.getTime()));
		}
		return date;
	}

	public Object formatResult(Object o) {
		return UtilReport.formatResult(o, '.');
	}

	public String getTypeByAlias(String alias){
		String type = "";
		switch(alias) {
			case "RVCACOAONE":
				type = "408";
				break;
			case "RVCAINAONE":
				type = "408";
				break;
			case "RVCACODSNE":
				type = "408";
				break;
			case "RVCAINDSNE":
				type = "408";
				break;
			case "RVCACOPPNE":
				type = "408";
				break;
			case "RVCAINPPNE":
				type = "408";
				break;
			case "RVCACOPPAM":
				type = "408";
				break;
			case "RVCAINPPAM":
				type = "408";
				break;
			case "RVCACOEGAM":
				type = "408";
				break;
			case "RVCAINEGAM":
				type = "408";
				break;
			case "RVCACOPFAM":
				type = "408";
				break;
			case "RVCAINPFAM":
				type = "408";
				break;
			case "RVCACOCINE":
				type = "408";
				break;
			case "RVCAINCINE":
				type = "408";
				break;
			case "RVCACOADNE":
				type = "408";
				break;
			case "RVCAINADNE":
				type = "408";
				break;
			case "RVCAOPPLIQ":
				type = "408";
				break;
			case "RVCACUSTME":
				type = "408";
				break;
			case "RVCACOD2NE":
				type = "408";
				break;
			case "RVCAIND2NE":
				type = "408";
				break;
			case "RVAUCOAONE":
				type = "984";
				break;
			case "RVAUINAONE":
				type = "984";
				break;
			case "RVAUCODSNE":
				type = "984";
				break;
			case "RVCACINDSNE":
				type = "984";
				break;
			case "RVAUCUSTME":
				type = "984";
				break;
			case "RVNETOME":
				type = "436";
				break;
			case "RVSPLITME":
				type = "436";
				break;
            case "RVCAINETNE":
                type = "408";
                break;
            case "RVCACOETNE":
                type = "408";
                break;
            case "RVCAINV2NE":
                type = "408";
                break;
            case "RVCACOV2NE":
                type = "408";
                break;
            case "RVCAINV2OV":
                type = "408";
                break;
            case "RVCACOV2OV":
                type = "408";
                break;
            default:
				type = "";
				break;
		}
		return type;
	}

	public String getSubTypeByAlias(String alias){
		String subType = "";
		switch(alias) {
			case "RVCACOAONE":
				subType = "522";
				break;
			case "RVCAINAONE":
				subType = "523";
				break;
			case "RVCACODSNE":
				subType = "524";
				break;
			case "RVCAINDSNE":
				subType = "525";
				break;
			case "RVCACOPPNE":
				subType = "526";
				break;
			case "RVCAINPPNE":
				subType = "527";
				break;
			case "RVCACOPPAM":
				subType = "528";
				break;
			case "RVCAINPPAM":
				subType = "529";
				break;
			case "RVCACOEGAM":
				subType = "530";
				break;
			case "RVCAINEGAM":
				subType = "531";
				break;
			case "RVCACOPFAM":
				subType = "532";
				break;
			case "RVCAINPFAM":
				subType = "533";
				break;
			case "RVCACOCINE":
				subType = "534";
				break;
			case "RVCAINCINE":
				subType = "535";
				break;
			case "RVCACOADNE":
				subType = "536";
				break;
			case "RVCAINADNE":
				subType = "537";
				break;
			case "RVCACOD2NE":
				subType = "538";
				break;
			case "RVCAIND2NE":
				subType = "539";
				break;
			case "RVCAOPPLIQ":
				subType = "521";
				break;
			case "RVCACUSTME":
				subType = "520";
				break;
			case "RVAUCOAONE":
				subType = "522";
				break;
			case "RVAUINAONE":
				subType = "523";
				break;
			case "RVAUCODSNE":
				subType = "524";
				break;
			case "RVCACINDSNE":
				subType = "525";
				break;
			case "RVAUCUSTME":
				subType = "520";
				break;
			case "RVNETOME":
				subType = "610";
				break;
			case "RVSPLITME":
				subType = "612";
				break;
            case "RVCAINETNE":
                subType = "569";
                break;
            case "RVCACOETNE":
                subType = "568";
                break;
            case "RVCAINV2NE":
                subType = "603";
                break;
            case "RVCACOV2NE":
                subType = "602";
                break;
            case "RVCAINV2OV":
                subType = "603";
                break;
            case "RVCACOV2OV":
                subType = "602";
                break;
			default:
				subType = "";
				break;
		}
		return subType;
	}


}

