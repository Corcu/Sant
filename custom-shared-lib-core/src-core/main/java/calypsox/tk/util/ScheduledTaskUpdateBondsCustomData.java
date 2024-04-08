package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.DateRoll;
import com.calypso.tk.core.DateRule;
import com.calypso.tk.core.DayCount;
import com.calypso.tk.core.Frequency;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PeriodRule;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

public class ScheduledTaskUpdateBondsCustomData extends ScheduledTask {

	private static final long serialVersionUID = 1L;

	private static final String FILE_PATH = "File Path";
	private static final String FILE_NAME = "File name";
	private static final String SEPARATOR = "Separator";
	private static final String DATE_FORMAT = "Date Format";

	private static final String DEFAULT_SEC_CODE = "ISIN";
	private Stack<ProccesBond> threads = new Stack<>();
	private int bondprecesed = 0;
	private String dateformat = "yyyyMMdd";
	private String split = ";";

	protected HashMap<String, String> methods = new HashMap<>();

	@Override
	public String getTaskInformation() {

		return "Update Bonds Custom Data";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getDomainAttributes() {
		final Vector<String> domainAttributes = new Vector<String>();
		domainAttributes.add(FILE_PATH);
		domainAttributes.add(FILE_NAME);
		domainAttributes.add(DATE_FORMAT);
		domainAttributes.add(SEPARATOR);
		return domainAttributes;
	}

	@Override
	protected boolean process(DSConnection ds, PSConnection arg1) {

		initValues();

		// Colums and Poss
		HashMap<Integer, String> columPoss = new HashMap<>();
		List<HashMap<String, String>> bonds = new ArrayList<>();
		setLines(columPoss, bonds);

		int i = 0;
		while (i < bonds.size()) {
			if (threads.size() < 10) {
				if (!Util.isEmpty(bonds.get(i))) {
					ProccesBond procces = new ProccesBond(bonds.get(i));
					procces.start();
					threads.add(procces);
				}
				i++;
			} else {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Log.error("Error procces bonds", e);
				}
			}
		}

		while (!threads.isEmpty()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Log.error(this, e);
			}
		}

		Log.info(this, "Total bond processed: " + bondprecesed);

		Log.info(this, "end");

		return super.process(ds, arg1);
	}

	/**
	 * @param columPoss
	 * @param bonds
	 */
	private void setLines(HashMap<Integer, String> columPoss, List<HashMap<String, String>> bonds) {
		try {
			final FileReader reader = new FileReader(getFilePath());
			final BufferedReader inputFileStream = new BufferedReader(reader);
			String line;
			boolean firts = true;
			while ((line = inputFileStream.readLine()) != null) {
				String[] lines = line.split(split);
				if (!firts) {
					bonds.add(createLine(columPoss, line));
				} else {
					firts = false;
					createColums(columPoss, lines);
				}
			}
			inputFileStream.close();
			reader.close();
		} catch (IOException e) {
			Log.error(this, e);
		}
	}

	/**
	 * @param columPoss
	 * @param line
	 * @return
	 */
	private HashMap<String, String> createLine(HashMap<Integer, String> columPoss, String line) {
		HashMap<String, String> lin = new HashMap<>();
		if (line.contains(split)) {
			String[] finalLine = line.split(split);
			for (int i = 0; i < finalLine.length; i++) {
				if (columPoss.containsKey(i) && !Util.isEmpty(finalLine[i])) {
					lin.put(columPoss.get(i), finalLine[i]);
				}
			}
		}

		return lin;
	}

	/**
	 * @param columPoss
	 * @param lines
	 */
	private void createColums(HashMap<Integer, String> columPoss, String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			columPoss.put(i, lines[i]);
		}
	}

	private String getFilePath() {
		if (!Util.isEmpty(getAttribute(FILE_PATH)) && !Util.isEmpty(getAttribute(FILE_NAME))) {
			return getAttribute(FILE_PATH) + getAttribute(FILE_NAME);
		}
		return "/calypso_interfaces/datauploader/file/bond.csv";
	}

	public void executeSt(DSConnection con) {
		process(con, null);
	}
	
	private void initValues() {
		if (!Util.isEmpty(getAttribute(DATE_FORMAT))) {
			dateformat = getAttribute(DATE_FORMAT);
		}
		if (!Util.isEmpty(getAttribute(SEPARATOR))) {
			split = getAttribute(SEPARATOR);
		}
	}

	private class ProccesBond extends Thread {

		protected Product product;
		protected HashMap<String, String> values;
		protected String isin;
		protected Hashtable<String, String> codes = new Hashtable<>();

		public ProccesBond(HashMap<String, String> values) {
			this.values = values;
		}

		@Override
		public void run() {	
			setCodes();
			// load bond
			try {
				if (!Util.isEmpty(isin)) {
					product = DSConnection.getDefault().getRemoteProduct().getProductByCode(DEFAULT_SEC_CODE, this.isin);
					procProduct(product);
					threads.remove(this);
				}
			} catch (Exception e) {
				threads.remove(this);
				Log.error(this, this.isin + " - " + e);
			}
		}

		private void procProduct(Product product) {
			if (product != null) {
				if (product instanceof Bond) {
					Bond bond = (Bond) product;
					for (Map.Entry<String, String> entry : values.entrySet()) {
						doingBadMagic(this.isin, bond, entry, this.codes);
					}
					try {
						getDSConnection().getRemoteProduct().saveBond(bond, true);
						Log.info(this, this.isin + "SAVED");
					} catch (Exception e) {
						Log.error(this, isin + " - NOT UPDATED - Cannot save:  " + e);
					}
				}
				bondprecesed++;
			} else {
				Log.error(this, "Cannot find " + this.isin);
			}

		}

		private void setCodes() {
			if (values.containsKey("ProductCode.ISIN")) {
				this.isin = values.get("ProductCode.ISIN");
			} else if (values.containsKey("SecCodeValue")) {
				this.isin = values.get("SecCodeValue");
			}
			if (values.containsKey("SecCodeName")) {
				codes.put(values.get("SecCodeName"), values.get("SecCodeValue"));
			}

		}

	}

	@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
	public void doingBadMagic(String isin, Bond bond, Map.Entry<String, String> entry,
			Hashtable<String, String> codes) {
		SimpleDateFormat formatter = new SimpleDateFormat(this.dateformat);

		switch (entry.getKey()) {
		case "Action":
			Log.info(this, "Calypo Action " + entry.getValue());
			break;
		case "BondDefaultName":
			break;
		case "SecCodeName":
			Hashtable<String, String> secCodes = new Hashtable<>();
			if (!Util.isEmpty(bond.getSecCodes())) {
				secCodes = bond.getSecCodes();
			}
			secCodes.putAll(codes);
			bond.setSecCodes(secCodes);
			break;
		case "ProductCode.ISIN":
			break;
		case "Name":
			bond.setName(entry.getValue());
			break;
		case "Country":
			bond.setCountry(entry.getValue());
			break;
		case "Currency":
			bond.setCurrency(entry.getValue());
			break;
		case "Comment":
			bond.setComment(entry.getValue());
			break;
		case "IssueDate":
			try {
				JDate jdate = JDate.valueOf(formatter.parse(entry.getValue()));
				bond.setIssueDate(jdate);
			} catch (ParseException e) {
				Log.error(this, isin + "-IssueDate fail " + entry.getKey() + ": " + e);
			}
			break;
		case "DatedDate":
			try {
				JDate jdate = JDate.valueOf(formatter.parse(entry.getValue()));
				bond.setStartDate(jdate);
			} catch (ParseException e) {
				Log.error(this, isin + "-DatedDate fail " + entry.getKey() + ": " + e);
			}
			break;
		case "MaturityDate":
			try {
				JDate jdate = JDate.valueOf(formatter.parse(entry.getValue()));
				bond.setMaturityDate(jdate);
			} catch (ParseException e) {
				Log.error(this, isin + "-MaturityDate fail " + entry.getKey() + ": " + e);
			}
			break;
		case "Tenor":
			//??
			break;
		case "Issuer":
			try {
				LegalEntity entity = getDSConnection().getRemoteReferenceData().getLegalEntity(entry.getValue());
				if (entity != null) {
					bond.setIssuerId(entity.getId());
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, isin + "-Cannot find LE" + entry.getValue() + " : " + e);
			}
			break;
		case "TotalIssued":
			if (Double.valueOf(entry.getValue()) != null) {
				bond.setTotalIssued(Double.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-TotalIssued fail");
			}
			break;
		case "IssuePrice":
			if (Double.valueOf(entry.getValue()) != null) {
				bond.setIssuePrice(Double.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-IssuePrice fail");
			}
			break;
		case "IssueYield":
			if (Double.valueOf(entry.getValue()) != null) {
				bond.setIssueYield(Double.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-IssueYield fail");
			}
			break;
		case "RedemptionPrice":
			if (Double.valueOf(entry.getValue()) != null) {
				bond.setRedemptionPrice(Double.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-RedemptionPrice fail");
			}
			break;
		case "RedemptionCurrency":
			bond.setRedemptionCurrency(entry.getValue());
			break;
		case "FaceValue":
			if (Double.valueOf(entry.getValue()) != null) {
				bond.setFaceValue(Double.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-FaceValue fail");
			}
			break;
		case "Status":
			bond.setBondStatus(entry.getValue());
			break;
		case "MinPurchaseAmount":
			if (Double.valueOf(entry.getValue()) != null) {
				bond.setMinPurchaseAmt(Double.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-Status fail");
			}
			break;
		case "CouponRate":
			//??
			break;
		case "CouponCurrency":
			bond.setCouponCurrency(entry.getValue());
			break;
		case "CouponDayCount":
			if (DayCount.valueOf(entry.getValue()) != null) {
				bond.setCouponDayCount(DayCount.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-CouponDayCount fail");
			}
			break;
		case "CouponHolidays":
			Vector holidays = new Vector<>();
			holidays.add(entry.getValue());
			bond.setCouponHolidays(holidays);
			break;
		case "CouponRollDay":
			if (DateRoll.valueOf(entry.getValue()) != null) {
				bond.setCouponDateRoll(DateRoll.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-CouponRollDay fail");
			}
			break;
		case "CouponPaymentLag":
			// ??
			break;
		case "CouponPaymentBusB":
			// ??
			break;
		case "CouponPaymentRule":
			if (PeriodRule.valueOf(entry.getValue()) != null) {
				bond.setCouponPeriodRule(PeriodRule.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-CouponPaymentRule fail");
			}
			break;
		case "CouponDateRoll":
			// correcto ??
			if (DateRoll.valueOf(entry.getValue()) != null) {
				bond.setCouponDateRoll(DateRoll.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-CouponDateRoll fail: " + entry.getValue());
			}
			break;
		case "CouponFrequency":
			// correcto??
			if (Frequency.valueOf(entry.getValue()) != null) {
				bond.setCouponFrequency(Frequency.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-CouponFrequency fail");
			}
			break;
		case "CouponPrepaidB":
			// ??
			break;
		case "CouponDiscountMethod":
			// ??
			break;
		case "CouponAccrualDayCount":
			if (DayCount.valueOf(entry.getValue()) != null) {
				bond.setAccrualDaycount(DayCount.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-CouponAccrualDayCount fail");
			}
			break;
		case "CouponUseInStubsB":
			// ??
			break;
		case "CouponUseDateRuleB":
			// ??
			break;
		case "CouponDateRule":
			if (DateRule.valueOf(entry.getValue()) != null) {
				bond.setCouponDateRule(DateRule.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-CouponDateRule fail");
			}
			break;
		case "StubStartDate":
			try {
				JDate jdate = JDate.valueOf(formatter.parse(entry.getValue()));
				bond.setFirstCouponDate(jdate);
			} catch (ParseException e) {
				Log.error(this, isin + "-StubStartDate fail " + entry.getKey() + ": " + e);
			}
			
			break;
		case "StubEndDate":
			try {
				JDate jdate = JDate.valueOf(formatter.parse(entry.getValue()));
				bond.setPenultimateCouponDate(jdate);
			} catch (ParseException e) {
				Log.error(this, isin + "-StubStartDate fail " + entry.getKey() + ": " + e);
			}
			break;
		case "SettleDays":
			if (Integer.valueOf(entry.getValue()) != null) {
				bond.setSettleDays(Integer.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-SettleDays fail");
			}
			break;
		case "AccrualDays":
			// ??
			break;
		case "ExDividendDays":
			if (Integer.valueOf(entry.getValue()) != null) {
				bond.setExdividendDays(Integer.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-ExDividendDays fail");
			}
			// ??
			break;
		case "ExDividendBusB":
			if (Boolean.valueOf(entry.getValue()) != null) {
				bond.setExdividendDayBusB(Boolean.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-ExDividendBusB fail");
			}
			break;
		case "RecordDays":
			if (Integer.valueOf(entry.getValue()) != null) {
				bond.setRecordDays(Integer.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-RecordDays fail");
			}
			break;
		case "AccrualDigits":
			// ??
			break;
		case "AccrualRoundingMethod":
			bond.setAccrualRoundingMethod(entry.getValue());
			break;
		case "PriceDecimals":
			if (Integer.valueOf(entry.getValue()) != null) {
				bond.setPriceDecimals(Integer.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-PriceDecimals fail");
			}
			break;
		case "PriceRoundingMethod":
			bond.setPriceRoundingMethod(entry.getValue());
			break;
		case "YieldDecimals":
			if (Integer.valueOf(entry.getValue()) != null) {
				bond.setYieldDecimals(Integer.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-YieldDecimals fail");
			}
			break;
		case "YieldRoundingMethod":
			bond.setYieldRoundingMethod(entry.getValue());
			break;
		case "NominalDecimals":
			if (Integer.valueOf(entry.getValue()) != null) {
				bond.setNominalDecimals(Integer.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-NominalDecimals fail");
			}
			break;
		case "CouponRateDecimals":
			// ??
			break;
		case "CouponRateRoundingMethod":
			bond.setCouponRateRoundingMethod(entry.getValue());
			break;
		case "CapitilizationDecimals":
			if(Integer.valueOf(entry.getValue())!=null) {
				bond.setCapitalizationFactorRounding(Integer.valueOf(entry.getValue()));
			}
			break;
		case "CapitalizationRoundingMethod":
			bond.setCapitalizationFactorRoundingMethod(entry.getValue());
			break;
		case "AnnounceDate":
			try {
				JDate jdate = JDate.valueOf(formatter.parse(entry.getValue()));
				bond.setAnnounceDate(jdate);
			} catch (ParseException e) {
				Log.error(this, isin + "-AnnounceDate fail " + entry.getKey() + ": " + e);
			}
			break;
		case "AuctionDate":
			try {
				JDate jdate = JDate.valueOf(formatter.parse(entry.getValue()));
				bond.setAuctionDate(jdate);
			} catch (ParseException e) {
				Log.error(this, isin + "-AuctionDate fail" + entry.getKey() + ": " + e);
			}
			break;
		case "DefaultDate":
			try {
				JDate jdate = JDate.valueOf(formatter.parse(entry.getValue()));
				bond.setDefaultDate(jdate);
			} catch (ParseException e) {
				Log.error(this, isin + "-DefaultDate fail " + entry.getKey() + ": " + e);
			}

			break;
		case "ApplyWithholdingTaxB":
			if (Boolean.valueOf(entry.getValue()) != null) {
				bond.setApplyWithholdingTaxB(Boolean.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-ApplyWithholdingTaxB fail");
			}
			break;
		case "WithholdingTax":
			if (Double.valueOf(entry.getValue()) != null) {
				bond.setWithholdingTax(Double.valueOf(entry.getValue()));
			} else {
				Log.warn(this, isin + "-WithholdingTax fail");
			}
			break;
		case "TickSize":
			bond.setTickSize(entry.getValue());
			break;
		case "YieldMethod":
			bond.setYieldMethod(entry.getValue());
			break;
		case "QuoteType":
			bond.setQuoteType(entry.getValue());
			break;
		case "IssuePayingAgent":
			// ??
			// bond.setIssuePayingAgentId(123);
			break;
		case "CalculatorAgent":
			// ??
			// bond.setCalculatorAgentId(123);
			break;
		case "Trustee":
			// ??
			// bond.setTrusteeId(1234);
			break;
		case "BenchmarkName":
			bond.setBenchmarkName(entry.getValue());
			break;
		case "CommissionPaidB":
			// ??
			break;
		case "FutureContractExchange":
			// FutureContract futcontract = new FutureContract();
			break;
		case "FutureContractCurrency":
			// ??
			break;
		case "FutureContractName":
			// ??
			break;
		case "BenchmarkSecCodeName":
			// ??
			break;
		case "BenchmarkSecCodeValue":
			// ??
			break;
		}
	}

}
