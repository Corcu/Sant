package calypsox.tk.util.interfaceImporter;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

import calypsox.tk.util.bean.InterfaceTradeBean;
import calypsox.util.TradeImportTracker;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralManagerUtil;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

/**
 * Gathers the trade beans to be sent to the TradeMapper thread.
 *
 * @author aela
 *
 */
public class InterfaceFileProcessor extends ImportExecutor<InterfaceTradeBean, InterfaceTradeBean> {


	//DomainValues
	private static final String CONSUMER_UK_PO = "ConsumerUK.PO";
	private static final String CONSUMER_UK_CPTY = "ConsumerUK.Cpty";
	private static final String CONSUMER_UK_BOOK = "ConsumerUK.Book";

	// class variables
	private final File file;
	private BlockingQueue<InterfaceTradeBean> recodsList = null;
	private final InterfaceFileReader filerProcessor;
	private final Set<String> productExistsDV = new HashSet<String>();
	private final boolean useControlLine;
	private final String CONUK = "CONUK";

	// PDV: exposure trade
	private boolean isPDV = false;
	//SLB:
	private boolean isSLB = false;

	/**
	 * Constructor
	 *
	 * @param f
	 *            file
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param context
	 */
	public InterfaceFileProcessor(File f, BlockingQueue<InterfaceTradeBean> inWorkQueue,
								  BlockingQueue<InterfaceTradeBean> outWorkQueue, ImportContext context, boolean useControlLine) {
		super(inWorkQueue, outWorkQueue, context);
		this.useControlLine = useControlLine;
		this.recodsList = outWorkQueue;
		this.file = f;
		this.filerProcessor = new InterfaceFileReader(context);

	}

	/**
	 * Constructor
	 *
	 * @param f
	 *            file
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param context
	 * @param useControlLine
	 * @param isPDV: true or false, if true means importing Murex PDV file
	 */
	public InterfaceFileProcessor(File f, BlockingQueue<InterfaceTradeBean> inWorkQueue,
								  BlockingQueue<InterfaceTradeBean> outWorkQueue, ImportContext context, boolean useControlLine,
								  boolean isPDV) {
		this(f, inWorkQueue, outWorkQueue, context, useControlLine);
		this.isPDV = isPDV;

		// set PDV allocation flag
		this.filerProcessor.setPDV(true);
	}

	/**
	 * Constructor
	 *
	 * @param f
	 *            file
	 * @param inWorkQueue
	 * @param outWorkQueue
	 * @param context
	 * @param useControlLine
	 * @param isSLB: true or false, if true means importing SLB file
	 */
	public InterfaceFileProcessor(File f, BlockingQueue<InterfaceTradeBean> inWorkQueue,
								  BlockingQueue<InterfaceTradeBean> outWorkQueue, ImportContext context, boolean useControlLine,
								  Boolean isSLB) {
		this(f, inWorkQueue, outWorkQueue, context, useControlLine);
		this.isSLB = isSLB;

		// set isSLB variable
		this.filerProcessor.setIsSLB(true);
	}





	/**
	 * checks on trade bean data .
	 */
	@Override
	public InterfaceTradeBean execute(InterfaceTradeBean item) throws Exception {

		BufferedReader reader = null;
		HashMap<String, String> productSubTypeMapping = this.context.getProductSubTypeMapping();
		HashMap<String, InterfaceTradeBean> nonProcessedYet2LegProducts = this.context.getNonProcessedYet2LegProducts();
		String recordSpliter = this.context.getRecordSpliter();
		TradeImportTracker tradeImportTracker = this.context.getTradeImportTracker();

		int i = 0;
		int nbTrades = 0;
		try {
			reader = new BufferedReader(new FileReader(this.file));
			InterfaceTradeBean tradeBeanLegTwo = null;
			String record = null;
			long start = System.currentTimeMillis();
			InterfaceTradeBean tradeBean = null;
			int lineNb = 0;
			while ((record = reader.readLine()) != null) {
				int tradeNbLegs = 1;
				try {
					lineNb = i + 1;
					Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "***********>end reading line " + lineNb + " in "
							+ (System.currentTimeMillis() - start));
					start = System.currentTimeMillis();

					try {

						tradeBean = this.filerProcessor.readLine(record, recordSpliter, lineNb, this.useControlLine);
					} catch (Exception e) {
						Log.info(this, e); //sonar
						i = i + 1;
						nbTrades++;
						continue;
					}

					if (tradeBean != null) {
						i = i + 1;
						// test if the istrument is not Empty:
						if (Util.isEmpty(tradeBean.getInstrument())) {
							tradeImportTracker.addError(tradeBean, 16, "Required field INSTRUMENT not present.");
							tradeImportTracker.incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
							nbTrades++;
							continue;

						}

						// GSM: check if the the DV are correct.
						if (!subtypeAndLegsMappingExists(tradeBean, tradeImportTracker, tradeNbLegs,
								productSubTypeMapping)) {
							nbTrades++;
							continue;
						}

						if (!isPDV()) {
							/*
							 * GSM: 28/04/2014 - Murex (CollateralExposure.SECURITY_LENDING) is a little bit special. This
							 * part ensures that mandatory fields have been read it.
							 */
							if (!colExposureSecLendingMandatoryfields(tradeBean)) {
								tradeImportTracker.incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
								nbTrades++;
								continue;

							}
						}

						if (CollateralUtilities.isTwoLegsProductType(productSubTypeMapping.get(tradeBean
								.getInstrument()))) {
							InterfaceTradeBean nonCompletedTrade = nonProcessedYet2LegProducts.get(tradeBean
									.getBoReference());
							if (nonCompletedTrade != null) {
								tradeBeanLegTwo = (InterfaceTradeBean) tradeBean.clone();
								tradeBean = nonCompletedTrade;
								nonProcessedYet2LegProducts.remove(tradeBean.getBoReference());
							} else { // keep this trade since we don't have
								// its
								// second leg
								nonProcessedYet2LegProducts.put(tradeBean.getBoReference(), tradeBean);
								continue;
							}
							tradeNbLegs = 2;
							tradeBean.setLineNumber2(i);
							// tradeBean.setLineContent(record);
							tradeBean.setLegTwo(tradeBeanLegTwo);
							// tradeBean.setLineContent2(tradeBeanLegTwo.getLineContent());
							// GSM1
						} else if (CollateralUtilities.isOneLegProductType(productSubTypeMapping.get(tradeBean
								.getInstrument()))) {
							// Check Isin is valid for BOND_FORWARD
							if ("BOND_FORWARD".equals(productSubTypeMapping.get(tradeBean.getInstrument()))) {
								if ("ISIN".equals(tradeBean.getUnderlayingType())) {
									String isin = tradeBean.getUnderlaying();
									if (Util.isEmpty(isin)) {

										tradeImportTracker.addError(tradeBean, 58, "Empty ISIN received");
										tradeImportTracker.incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
										nbTrades++;
										continue;
									}
									Product bond = DSConnection.getDefault().getRemoteProduct()
											.getProductByCode("ISIN", isin);
									if (bond == null) {
										// GSM: 27/06/2013. No bond must be warning
										// tradeImportTracker.addError(tradeBean, 58, "No Bond exists with ISIN=" +
										// isin);
										// tradeImportTracker.incrementKOImports(tradeNbLegs,
										// TradeImportTracker.KO_ERROR);
										// GSM: 27/06/2013. No bond must be warning, no error
										tradeImportTracker.addWarningNoIsin(tradeBean, 58, "No Bond exists with ISIN=",
												isin);
										tradeImportTracker.incrementKOImports(tradeNbLegs,
												TradeImportTracker.KO_WARNING);
										// this.context.getTradeImportTracker().
										nbTrades++;
										continue;
									}

								}
							}
						} else {

							tradeImportTracker.incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
							String instrument = tradeBean.getInstrument();
							tradeImportTracker.addError(tradeBean, 58, "The instrument "
									+ (instrument == null ? "null" : instrument)
									+ " is not well configured as one or two legs product.");
							nbTrades++;
							continue;
						}
						// add the new row to the list of records to be mapped
						nbTrades++;
						String poOriginal = tradeBean.getProcessingOrg();
						String cptyOriginal = tradeBean.getCounterparty();
						String boReference = tradeBean.getBoReference();
						Vector<String> bookVector = CollateralUtilities
								.getDomainValues(CONSUMER_UK_BOOK);
						String book = getFirstValue(bookVector);

						// If the domain values(po-cpty) are full and equal to
						// respective values of file, it clone the tradeBean
						// with the changes in po, cpty, mtm and book
						if (isToClone(poOriginal, cptyOriginal)) {

							InterfaceTradeBean tradeBeanCloned = (InterfaceTradeBean) tradeBean
									.clone();

							tradeBeanCloned.setCounterparty(poOriginal);
							tradeBeanCloned.setProcessingOrg(cptyOriginal);
							tradeBeanCloned.setBoReference(CONUK + boReference);
							String.format(Locale.ENGLISH, "%f",
									(-Double.valueOf(tradeBean.getMtm())));
							tradeBeanCloned.setMtm(String.format(
									Locale.ENGLISH, "%f",
									(-Double.valueOf(tradeBean.getMtm()))));
							// If domainValue book is full, we take this value
							// and it's empty we calculate the new value with
							// the contract associate
							tradeBeanCloned.setPortfolio(null);
							if (null == book) {
								final MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
								mcFilter.setLegalEntity(tradeBean
										.getProcessingOrg());
								mcFilter.setProcessingOrg(tradeBean
										.getCounterparty());
								final List<CollateralConfig> listContracts = CollateralManagerUtil
										.loadCollateralConfigs(mcFilter);
								if (null != listContracts
										&& !listContracts.isEmpty()) {
									CollateralConfig collConfig = listContracts
											.get(0);
									tradeBeanCloned.setPortfolio(collConfig
											.getBook().getName());
								}
							} else {
								tradeBeanCloned.setPortfolio(book);
							}
							this.recodsList.put(tradeBean);
							if(null!=tradeBeanCloned.getPortfolio()) {
								this.recodsList.put(tradeBeanCloned);
							}

						} else {
							this.recodsList.put(tradeBean);
						}
					}
				} catch (Exception e) {
					tradeImportTracker.incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
					tradeImportTracker.addError(tradeBean, 5, " Bad record format");
					nbTrades++;
					Log.error(this, e);
				}
			}
		} catch (Exception ex) {
			Log.error(this, ex);
		} finally {
			// set the number of lines processed
			tradeImportTracker.setNbRowsToBeImported(i);
			if (nonProcessedYet2LegProducts != null) {
				nbTrades = nbTrades + nonProcessedYet2LegProducts.size();
				for (String boRef : nonProcessedYet2LegProducts.keySet()) {
					InterfaceTradeBean orphanTrade = nonProcessedYet2LegProducts.get(boRef);
					tradeImportTracker.addError(orphanTrade, 59, "Two legs trade but just one leg received");
					tradeImportTracker.incrementKOImports(1, TradeImportTracker.KO_ERROR);
				}
			}

			tradeImportTracker.setNbTradesToBeImported(nbTrades);
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while trying close input stream for the CSV file <"
							+ this.file.getName() + "> open previously", e);
				}
			}
		}

		// send the signal to stop everything
		dispose();
		return null;

	}

	/**
	 * Method that allows to get the first String of Vector
	 *
	 * @param vector
	 * @return String
	 */
	private String getFirstValue(Vector<String> vector) {
		String value=null;
		if(null!=vector && !vector.isEmpty()){
			value = vector.get(0);
		}
		return value;
	}

	/*
	 * GSM: 28/04/2014 - Murex (CollateralExposure.SECURITY_LENDING) is a little bit special. This part ensures that
	 * mandatory fields have been read it and that the numbers can be formatted properly
	 */
	private boolean colExposureSecLendingMandatoryfields(InterfaceTradeBean tradeBean) {

		// first check if this trade is colExposure SECURITY_LENDING, if not, just skip
		final String type = this.context.getProductSubTypeMapping().get(tradeBean.getInstrument());
		if (!type.trim().equals(InterfaceTradeMapper.COLLATERAL_EXP_SEC_LENDING)) {
			return true;
		}

		TradeImportTracker tradeImportTracker = this.context.getTradeImportTracker();

		if (Util.isEmpty(tradeBean.getClosingPriceDaily())) {
			tradeImportTracker.addError(tradeBean, 51, "Required field 21: CLOSING_PRICE not present.");
			return false;
		}

		try {
			Double.parseDouble(tradeBean.getClosingPriceDaily());

		} catch (NumberFormatException e) {
			tradeImportTracker.addError(tradeBean, 57, "Required field 21: CLOSING_PRICE has an incorrect format.");
			return false;
		}

		if (Util.isEmpty(tradeBean.getAccruedCoupon())) {
			tradeImportTracker.addError(tradeBean, 52, "Required field 44:  ACCRUED_COUPON not present.");
			return false;
		}

		try {
			Double.parseDouble(tradeBean.getAccruedCoupon());

		} catch (NumberFormatException e) {
			tradeImportTracker.addError(tradeBean, 57, "Required field 44: ACCRUED_COUPON has an incorrect format.");
			return false;
		}

		if (Util.isEmpty(tradeBean.getLotSize())) {
			tradeImportTracker.addError(tradeBean, 54, "Required field 43: LOT_SIZE not present.");
			return false;
		}

		try {
			Double.parseDouble(tradeBean.getLotSize());

		} catch (NumberFormatException e) {
			tradeImportTracker.addError(tradeBean, 57, "Required field 43: LOT_SIZE has an incorrect format.");
			return false;
		}

		if (Util.isEmpty(tradeBean.getHaircut())) {
			tradeImportTracker.addError(tradeBean, 55, "Required field 29:  HAIRCUT not present.");
			return false;
		}

		Double haircut = 0.0d;
		try {
			haircut = Double.parseDouble(tradeBean.getHaircut());

		} catch (NumberFormatException e) {
			tradeImportTracker.addError(tradeBean, 57, "Required field 29: HAIRCUT has an incorrect format.");
			return false;
		}

		if ((haircut != 0.0d) && Util.isEmpty(tradeBean.getHaircutDirection())) {
			tradeImportTracker.addError(tradeBean, 56,
					"Required field 30: HAIRCUT_DIRECTION not present when HAIRCUT is different than 0.");
			return false;

		}

		if ((haircut != 0.0d) && !Util.isEmpty(tradeBean.getHaircutDirection())) {
			if (!tradeBean.getHaircutDirection().contains("GIV") && !tradeBean.getHaircutDirection().contains("REC")) {
				tradeImportTracker.addError(tradeBean, 58,
						"Required field 30: HAIRCUT_DIRECTION has an incorrect format.");
				return false;
			}

		}

		return true;
	}

	/*
	 * Verifies that the product exists in the product submapping (if not is missing in the DV). Also checks that the
	 * product is one or two legs
	 */
	private boolean subtypeAndLegsMappingExists(final InterfaceTradeBean tradeBean,
												final TradeImportTracker tradeImportTracker, final int tradeNbLegs,
												HashMap<String, String> productSubTypeMapping) {

		final int error = 101;

		if ((tradeBean == null) || (tradeBean.getInstrument() == null)) {
			return false;
		}

		final String product = tradeBean.getInstrument().trim();

		if (this.productExistsDV.contains(product)) { // was checked before
			return true;
		}

		if (!productSubTypeMapping.containsKey(product)) {
			tradeImportTracker.incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
			String instrument = tradeBean.getInstrument();
			String bo_system = tradeBean.getBoSystem();
			tradeImportTracker.addError(tradeBean, error, "The instrument "
					+ (instrument == null ? "null" : instrument)
					+ " is not well configured in the DV Mapping for the source system "
					+ (bo_system == null ? "null" : bo_system));
			return false;
		}

		final String mapping = productSubTypeMapping.get(product);

		if (CollateralUtilities.isOneLegProductType(mapping) || CollateralUtilities.isTwoLegsProductType(mapping)) {
			this.productExistsDV.add(product); // all checks passed. added to checked list
			return true;
		}

		tradeImportTracker.incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
		String instrument = tradeBean.getInstrument();
		tradeImportTracker.addError(tradeBean, error, "The instrument " + (instrument == null ? "null" : instrument)
				+ " is not configured as one or either two legs product. Check de DV and set this instrument.");

		return false;
	}

	@Override
	protected void stopProcess() {
		this.context.stopFileReaderProcess();
	}

	public boolean isPDV() {
		return this.isPDV;
	}

	public void setPDV(boolean isPDV) {
		this.isPDV = isPDV;
	}


//	public void setIsSLB(boolean isSLB) {
//		this.isSLB = isSLB;
//	}

	/**
	 * If the domain values(po-cpty) are full and equal to respective values of file, it clone the tradeBean
	 * @param poOriginal
	 * @param cptyOriginal
	 * @return
	 */
	public boolean isToClone(String poOriginal, String cptyOriginal){
		Vector<String> poVector = CollateralUtilities.getDomainValues(CONSUMER_UK_PO);
		Vector<String> cptyVector = CollateralUtilities.getDomainValues(CONSUMER_UK_CPTY);

		String po = getFirstValue(poVector);
		String cpty = getFirstValue(cptyVector);

		boolean result = false;

		if(null!=po && null!=poOriginal && null!=cpty && null!=cptyOriginal){
			if(po.equalsIgnoreCase(poOriginal) && cpty.equalsIgnoreCase(cptyOriginal)){
				result = true;
			}
		}

		return result;
	}

}