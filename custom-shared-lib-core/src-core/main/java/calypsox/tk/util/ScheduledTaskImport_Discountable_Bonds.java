/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.product.BondCustomData;
import calypsox.tk.product.EquityCustomData;
import calypsox.tk.util.bean.DiscountableProductBean;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.Map.Entry;

import static calypsox.tk.core.CollateralStaticAttributes.*;
import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * Processes bonds and equities to recover if is elegible by several central bankings or organize
 * markets Each bond is identified by its ISIN Each equity is identified by the word EQ-ISIN-CCY.
 *
 * @author Soma, Elamine Abdelmejid & Guillermo Solano
 * @version 3.0 - Added Discountable for MMOO Meff & Eurex and save into quotes
 * @date 05/09/2014
 */
public class ScheduledTaskImport_Discountable_Bonds extends AbstractProcessFeedScheduledTask {

  // START CALYPCROSS-420 - fperezur
  BufferedReader reader = null;
  // START CALYPCROSS-420 - fperezur

  private static final long serialVersionUID = 3194368663628571947L;

  /** Varibles */
  // BAU - Use different way to import data in order to avoid multiple files
  // problem
  private File file;

  private Map<String, QuoteInfo> quotesMap;

  /** Constants */
  // STs attributes
  private static final String ATT_SEPERATOR = "Separator";

  private static final String ATT_NUMBER_OF_FEEDS = "NumberOfFeeds";
  private static final String CONTROL_LINE = "Control Line";
  protected static final String ATT_SUMMARY_LOG = "Summary Log";
  protected static final String ATT_DETAILED_LOG = "Detailed Log";
  protected static final String ATT_FULL_LOG = "Full Log";

  // type of haircut
  private static final String REGULAR = "Regular";
  private static final String INVERSE = "Inverse";

  // GSM: 25/11/14. Option to save quotes as yesterday or today
  // type of quote date
  private static final String[] QUOTES_DATE = new String[] {"Today", "Yesterday"};

  // direction
  private static final String ECB_DIRECTION = "ECB_Direction";
  private static final String FED_DIRECTION = "FED_Direction";
  private static final String BOE_DIRECTION = "BOE_Direction";
  private static final String SWISS_DIRECTION = "Swiss_Direction";
  private static final String EUREX_DIRECTION = "EUREX_Direction";
  private static final String MEFF_DIRECTION = "MEFF_Direction";

  private static final String CUSTOM_DATA_SOURCE = "CustomDataSource";
  private static final String PRICE_TYPE = "PriceType";

  private static final String EUREX_HAIRCUT = "EUREX_HAIRCUT";
  private static final String MEEF_HAIRCUT = "MEEF_HAIRCUT";
  private static final String EUREX_QUOTE_SET = "Eurex quoteSet";
  private static final String MEFF_QUOTE_SET = "Meff quoteSet";
  private static final String COPY_HC_AS_QUOTES = "Copy HC as Quotes";
  // quote date
  private static final String QUOTE_DATE_SELECTION = "Process Quote Date";
  private static final String ISIN = "ISIN";

  // START CALYPCROSS-38 - mromerod
  /** Attribute that indicates the number of threads to use */
  private static final int NUM_THREADS = 4; // everis

  private static final double NUMBER_THREADS_DOUBLE = 4; // everis P
  /** Bond constant */
  private static final String CT_BOND = "BOND";

  /** Equity constant */
  private static final String CT_EQUITY = "EQUITY";
  // END CALYPCROSS-38 - mromerod

  /*
   * Internal class to keep track of the products haircuts that must be
   * updated in quotes
   */
  class QuoteInfo {
    private boolean disc_eurex;
    private boolean disc_meff;
    private Double hc_eurex;
    private Double hc_meff;
  }

  /** Main process */
  @Override
  public boolean process(final DSConnection conn, final PSConnection connPS) {

    boolean result = true;
    try {
      final String path = getAttribute(FILEPATH);
      final String startFileName = getAttribute(STARTFILENAME);
      JDate fileDate = getValuationDatetime().getJDate(TimeZone.getDefault());
      final JDate valueDate = fileDate.addBusinessDays(-1, getHolidays());

      // BAU - Use different way to import data in order to avoid multiple
      // files problem
      this.file = lookForFile(path, startFileName, fileDate);

      if (this.file != null) {

        // Just after file verifications, this method will make a copy
        // into the
        // ./import/copy/ directory
        FileUtility.copyFileToDirectory(path + this.file.getName(), path + "/copy/");

        // GSM: 04/09/2014. Declared it for later process of quotes
        this.quotesMap = new HashMap<String, QuoteInfo>();

        // file process
        result = processFile(path + this.file.getName());

        // AE: 29/08/14
        if (Boolean.parseBoolean(getAttribute(COPY_HC_AS_QUOTES))) {
          // no matter what happens try to copy the haircuts from
          // bond_custom_data to the quote_value
          // result &= copyHaircutAsQuoteValues();
          // GSM Until BBDD changes de custom tables, this is an
          // alternative

          // GSM: 25/11/14. Option to save quotes as yesterday or
          // today
          final String dateProcess = getAttribute(QUOTE_DATE_SELECTION);
          if (Util.isEmpty(dateProcess) || dateProcess.equals(QUOTES_DATE[1])) {
            Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Saving MMOO Quotes for " + valueDate.toString());
            result &= saveHaircutsQuotesValues(valueDate);
          } else {

            Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Saving MMOO Quotes for " + fileDate.toString());
            result &= saveHaircutsQuotesValues(fileDate);
          }
        }
      } else {
        Log.error(
            LOG_CATEGORY_SCHEDULED_TASK, "No matches found for filename in the path specified.");
        ControlMErrorLogger.addError(
            ErrorCodeEnum.InputFileNotFound,
            "No matches found for filename in the path specified.");
        result = false;
      }

    } catch (Exception exc) {
      Log.error(this, exc); // sonar
    } finally {
      try {
        feedPostProcess();
      } catch (Exception e) {
        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while moving the files to OK/Fail folder");
        Log.error(this, e); // sonar
        ControlMErrorLogger.addError(
            ErrorCodeEnum.UndefinedException, "Error while moving the files to OK/Fail folder");
        result = false;
      }
    }
    return result;
  }

  /**
   * @param file
   * @return ok if file was correctly processed
   */
  private boolean processFile(final String file) {

    Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Starting process of file " + file);
    BufferedReader reader = null;
    try {

      Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Loading all bonds");
      Map<String, Bond> allBondsMap = loadBondsMap();
      // GSM 03/09/14. Same inneficient logic to gather the equities
      Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Loading all equities");
      final Map<String, Equity> allEquitiesMap = loadEquitiesMap();

      Map<String, Bond> bondsWithCustomData = new HashMap<String, Bond>();
      // GSM 03/09/14. Same inneficient logic to gather the equities
      final Map<String, Equity> equitiesWithCustomData = new HashMap<String, Equity>();

      reader = new BufferedReader(new FileReader(file));
      String line = null;
      int lineNumber = -1;
      while ((line = reader.readLine()) != null) {

        lineNumber++;
        if (lineNumber == 0) {
          continue;
        }

        if (line.startsWith("*****")) {
          break;
        }

        final DiscountableProductBean bean = buildBean(line);

        // GSM 05/09/2014: In case the product id cannot be processed
        // correctly, it will be discarded
        if (Util.isEmpty(bean.getProductId())) {
          Log.error(
              LOG_CATEGORY_SCHEDULED_TASK,
              "Line number-" + lineNumber + " : Error processing Product id for line " + line);
          continue;
        }

        // GSM 03/09/14. Discriminate between a bond or an equity
        if (bean.getType().equals(DiscountableProductBean.TYPE.BOND)) {

          Bond bond = allBondsMap.get(bean.getProductId());

          if (bond == null) {
            Log.error(
                LOG_CATEGORY_SCHEDULED_TASK,
                "Line number-"
                    + lineNumber
                    + " : Bond not found with REF_INTERNA"
                    + bean.getProductId());
            continue;
          }

          processBondLine(bond, bean); // tengo la referencia y el
          // bono
          // GSM 04/09/14: capture quote if its marked as Discountable
          // Meff or Eurex
          processProductQuote(bond, bean);

          // START CALYPCROSS-38 - mromerod
          //					try {
          // END CALYPCROSS-38 - mromerod
          bondsWithCustomData.put(bean.getProductId(), bond);
          // START CALYPCROSS-38 - mromerod
          //						getDSConnection().getRemoteProduct().saveBond(bond, true);
          //					} catch (RemoteException re) {
          //						Log.error(LOG_CATEGORY_SCHEDULED_TASK,
          //								"Line number-" + lineNumber + " : Error while saving the Bond with REF_INTERNA="
          //										+ bean.getProductId() + ", Error=" + re.getLocalizedMessage());
          //					}
          // END CALYPCROSS-38 - mromerod
          // end bond type
        } else if (bean.getType().equals(DiscountableProductBean.TYPE.EQUITY)) {

          Equity equity = allEquitiesMap.get(bean.getProductId());

          if (equity == null) {
            Log.error(
                LOG_CATEGORY_SCHEDULED_TASK,
                "Line number-"
                    + lineNumber
                    + " : Equity not found with ISIN & CCY"
                    + bean.getProductId());
            continue;
          }

          processEquityLine(equity, bean); // tengo la referencia y el
          // equity
          // GSM 04/09/14: capture quote
          processProductQuote(equity, bean);
          // START CALYPCROSS-38 - mromerod
          //					try {
          // END CALYPCROSS-38 - mromerod
          equitiesWithCustomData.put(bean.getProductId(), equity);
          // START CALYPCROSS-38 - mromerod
          //						getDSConnection().getRemoteProduct().saveProduct(equity, true); // save
          // equity
          //					} catch (RemoteException re) {
          //						Log.error(LOG_CATEGORY_SCHEDULED_TASK,
          //								"Line number-" + lineNumber + " : Error while saving the Equity with ISIN&CCY="
          //										+ bean.getProductId() + ", Error=" + re.getLocalizedMessage());
          //					}
          // END CALYPCROSS-38 - mromerod
        }
      }
      Log.info(LOG_CATEGORY_SCHEDULED_TASK, file + " processed");

      // START CALYPCROSS-38 - mromerod
      // We prepare the threads to launch by segments
      SegmentThread[] bondThread =
          manageSavingThreads(bondsWithCustomData.values(), null, false, null);

      // We prepare the threads to launch by segments
      SegmentThread[] equityThread =
          manageSavingThreads(null, equitiesWithCustomData.values(), true, null);

      // Remove ECB Attributes for the remaining Bonds.
      //			removeCustomDataFromProductsNotIncluded(allBondsMap, bondsWithCustomData, allEquitiesMap,
      //					equitiesWithCustomData);

      // We execute the previously prepared threads
      joinThreads(bondThread);
      joinThreads(equityThread);

      // Control of the trades we are saving
      StringBuffer bondSaved =
          new StringBuffer("*****All BondIds saved: Total: " + bondsWithCustomData.size());
      for (Bond bond : bondsWithCustomData.values()) {
        bondSaved.append(" BdId: " + bond.getId() + ";");
      }
      System.err.println(bondSaved.toString());
      Log.info(LOG_CATEGORY_SCHEDULED_TASK, bondSaved.toString());

      StringBuffer equitySaved =
          new StringBuffer("*****All EquityIds saved: Total: " + equitiesWithCustomData.size());
      for (Equity equity : equitiesWithCustomData.values()) {
        equitySaved.append("EqId: " + equity.getId() + "; ");
      }
      System.err.println(equitySaved.toString());
      Log.info(LOG_CATEGORY_SCHEDULED_TASK, equitySaved.toString());

      Collection<Bond> bondsWithNOCustomData = allBondsMap.values();
      bondsWithNOCustomData.removeAll(bondsWithCustomData.values());

      // We prepare the threads to launch by segments
      SegmentThread[] removeBondThread =
          manageSavingThreads(bondsWithNOCustomData, null, false, CT_BOND);

      Collection<Equity> equitiesWithNOCustomData = allEquitiesMap.values();
      equitiesWithNOCustomData.removeAll(equitiesWithCustomData.values());

      // We prepare the threads to launch by segments
      SegmentThread[] removeEquityThread =
          manageSavingThreads(null, equitiesWithNOCustomData, true, CT_EQUITY);

      // We execute the previously prepared threads
      joinThreads(removeBondThread);
      joinThreads(removeEquityThread);
      // END CALYPCROSS-38 - mromerod

    } catch (Exception exc) {
      Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
      return false;

    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.getMessage());
          Log.error(this, e); // sonar
        }
      }
    }
    return true;
  }

  /**
   * Saves the quotes for the value date for EUREX_QUOTE_SET & MEFF_QUOTE_SET
   *
   * @param valueDate of the quotes
   * @return true if quotes have been saved
   * @throws RemoteException
   */
  private boolean saveHaircutsQuotesValues(final JDate valueDate) throws RemoteException {

    Map<String, QuoteValue> eurexExistingQuotes =
        getQuotesMap(valueDate, getAttribute(EUREX_QUOTE_SET));
    Map<String, QuoteValue> meffExistingQuotes =
        getQuotesMap(valueDate, getAttribute(MEFF_QUOTE_SET));
    Vector<QuoteValue> newQuotes = new Vector<QuoteValue>(this.quotesMap.size());

    for (Map.Entry<String, QuoteInfo> entry : this.quotesMap.entrySet()) {

      newQuotes.addAll(getQuoteValue(entry, eurexExistingQuotes, meffExistingQuotes, valueDate));
    }

    return getDSConnection().getRemoteMarketData().saveQuoteValues(newQuotes);
  }

  /**
   * Saves in a map the products that must be save a quote later
   *
   * @param p Product
   * @param bean
   */
  private void processProductQuote(final Product p, final DiscountableProductBean bean) {

    QuoteInfo qi = new QuoteInfo();
    if ((qi.disc_eurex = bean.isDiscountableByEurex())
        | (qi.disc_meff = bean.isDiscountableByMeff())) {

      // System.out.println("added -" + bean.getProductId());

      if (getAttribute(EUREX_DIRECTION).equals(REGULAR)) {
        qi.hc_eurex = Double.parseDouble(bean.getHaircutEurex());
      } else {
        qi.hc_eurex = 100.0 + Double.parseDouble(bean.getHaircutEurex());
      }

      if (getAttribute(MEFF_DIRECTION).equals(REGULAR)) {
        qi.hc_meff = Double.parseDouble(bean.getHaircutMeff());
      } else {
        qi.hc_meff = 100.0 - Double.parseDouble(bean.getHaircutMeff());
      }

      if (p instanceof Bond) {

        this.quotesMap.put(((Bond) p).getQuoteName(), qi);

      } else if (p instanceof Equity) {

        this.quotesMap.put(((Equity) p).getQuoteName(), qi);
      }
    }
  }

  /**
   * All products (bonds & equities) that weren't included in the file must have erased their custom
   * data.
   *
   * @param allBondsMap
   * @param bondsWithCustomData
   * @param allEquitiesMap
   * @param equitiesWithCustomData
   */
  private void removeCustomDataFromProductsNotIncluded(
      Map<String, Bond> allBondsMap,
      Map<String, Bond> bondsWithCustomData,
      Map<String, Equity> allEquitiesMap,
      Map<String, Equity> equitiesWithCustomData) {

    Log.info(
        LOG_CATEGORY_SCHEDULED_TASK, "Starting erasing Custom data from Products not processed");

    Collection<Bond> bondsWithNOCustomData = allBondsMap.values();
    bondsWithNOCustomData.removeAll(bondsWithCustomData.values());
    for (Bond bond : bondsWithNOCustomData) {
      removeCustomDataAttributes(bond);
      try {
        getDSConnection().getRemoteProduct().saveBond(bond, true);
      } catch (RemoteException re) {
        Log.error(
            LOG_CATEGORY_SCHEDULED_TASK,
            "Error saving Bond after removing ECB Attribute, ISIN="
                + bond.getSecCode("ISIN")
                + ", Currency="
                + bond.getCurrency()
                + ", Error="
                + re.getLocalizedMessage());
        Log.error(this, re); // sonar
      }
    }

    Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Erased Custom Data Bonds");
    // GSM 03/09/14. erase custom of remaining Equities
    Collection<Equity> equitiesWithNOCustomData = allEquitiesMap.values();
    equitiesWithNOCustomData.removeAll(equitiesWithCustomData.values());
    for (Equity equity : equitiesWithNOCustomData) {
      removeECBAttributes(equity);
      try {
        getDSConnection().getRemoteProduct().saveProduct(equity, true); // save
        // equity
      } catch (RemoteException re) {
        Log.error(
            LOG_CATEGORY_SCHEDULED_TASK,
            "Error saving Bond after removing ECB Attribute, ISIN="
                + equity.getSecCode("ISIN")
                + ", Currency="
                + equity.getCurrency()
                + ", Error="
                + re.getLocalizedMessage());
        Log.error(this, re); // sonar
      }
    }
    Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Erased Custom Data Equities");
  }

  /** ST attributes */
  @Override
  protected List<AttributeDefinition> buildAttributeDefinition() {
    List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
    List<String> quoteSetNames = getQuoteSetNames();
    List<String> directionType =
        new ArrayList<String>(Arrays.asList(new String[] {REGULAR, INVERSE}));
    List<String> quoteTypes = getQuoteTypes();
    // Gets superclass attributes
    attributeList.addAll(super.buildAttributeDefinition());
    attributeList.add(attribute(ATT_SEPERATOR));
    attributeList.add(attribute(ATT_NUMBER_OF_FEEDS));
    attributeList.add(attribute(CONTROL_LINE).booleanType());
    attributeList.add(attribute(ATT_SUMMARY_LOG));
    attributeList.add(attribute(ATT_DETAILED_LOG));
    attributeList.add(attribute(ATT_FULL_LOG));
    attributeList.add(attribute(ECB_DIRECTION).domain(directionType));
    attributeList.add(attribute(FED_DIRECTION).domain(directionType));
    attributeList.add(attribute(BOE_DIRECTION).domain(directionType));
    attributeList.add(attribute(SWISS_DIRECTION).domain(directionType));
    attributeList.add(attribute(EUREX_DIRECTION).domain(directionType));
    attributeList.add(attribute(MEFF_DIRECTION).domain(directionType));
    attributeList.add(attribute(COPY_HC_AS_QUOTES).booleanType());
    attributeList.add(
        attribute(CUSTOM_DATA_SOURCE)
            .domain(
                new ArrayList<String>(Arrays.asList(new String[] {EUREX_HAIRCUT, MEEF_HAIRCUT}))));
    attributeList.add(attribute(EUREX_QUOTE_SET).domain(quoteSetNames));
    attributeList.add(attribute(MEFF_QUOTE_SET).domain(quoteSetNames));
    attributeList.add(attribute(PRICE_TYPE).domain(quoteTypes));
    attributeList.add(attribute(QUOTE_DATE_SELECTION).domain(Arrays.asList(QUOTES_DATE)));

    return attributeList;
  }

  @SuppressWarnings("unchecked")
  private List<String> getQuoteSetNames() {
    try {
      return new ArrayList<String>(
          DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames());
    } catch (RemoteException e) {
      Log.error(this, e);
    }
    return new ArrayList<String>();
  }

  private List<String> getQuoteTypes() {
    List<String> quoteTypes = null;
    try {
      quoteTypes = DSConnection.getDefault().getRemoteReferenceData().getDomainValues("quoteType");
    } catch (RemoteException e) {
      Log.error(this, e);
    }
    if (Util.isEmpty(quoteTypes)) {
      if (quoteTypes == null) {
        quoteTypes = new Vector<String>();
        quoteTypes.add("Price");
      }
    }
    return quoteTypes;
  }

  // /**
  // * ST attributes domains
  // */
  // @SuppressWarnings({ "rawtypes", "unchecked" })
  // @Override
  // public Vector getAttributeDomain(final String attribute, final Hashtable
  // hashtable) {
  // Vector vector = new Vector();
  // Vector<String> quoteSetNames = null;
  // try {
  // quoteSetNames =
  // DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames();
  // } catch (RemoteException e) {
  // Log.error(this, e);
  // }
  //
  // if (CONTROL_LINE.equals(attribute)) {
  // vector.add("true");
  // vector.add("false");
  // } else if (ECB_DIRECTION.equals(attribute) ||
  // FED_DIRECTION.equals(attribute) || BOE_DIRECTION.equals(attribute)
  // || SWISS_DIRECTION.equals(attribute) || EUREX_DIRECTION.equals(attribute)
  // || MEFF_DIRECTION.equals(attribute)) {
  // vector.add(REGULAR);
  // vector.add(INVERSE);
  // }
  //
  // else if (CUSTOM_DATA_SOURCE.equals(attribute)) {
  // vector.add("ALL");
  // vector.add(EUREX_HAIRCUT);
  // vector.add(MEEF_HAIRCUT);
  // }
  //
  // else if (COPY_HC_AS_QUOTES.equals(attribute)) {
  // vector.add("true");
  // vector.add("false");
  // } else if (PRICE_TYPE.equals(attribute)) {
  // Vector<String> quoteTypes = null;
  // try {
  // quoteTypes =
  // DSConnection.getDefault().getRemoteReferenceData().getDomainValues("quoteType");
  // } catch (RemoteException e) {
  // Log.error(this, e);
  // }
  // if (Util.isEmpty(quoteTypes)) {
  // if (quoteTypes == null) {
  // quoteTypes = new Vector<String>();
  // quoteTypes.add("Price");
  // }
  // }
  // vector.addAll(quoteTypes);
  // }
  //
  // else if (MEFF_QUOTE_SET.equals(attribute)) {
  // vector.addAll(quoteSetNames);
  // }
  //
  // else if (EUREX_QUOTE_SET.equals(attribute)) {
  // vector.addAll(quoteSetNames);
  // }
  //
  // else if (attribute.equals(QUOTE_DATE_SELECTION)) {
  //
  // vector.addAll(Arrays.asList(QUOTES_DATE));
  // }
  //
  // return vector;
  // }

  // /**
  // * ST attributes
  // */
  // @Override
  // public Vector<String> getDomainAttributes() {
  // @SuppressWarnings("unchecked")
  // final Vector<String> attr = super.getDomainAttributes();
  // attr.add(ATT_SEPERATOR);
  // attr.add(ATT_NUMBER_OF_FEEDS);
  // attr.add(CONTROL_LINE);
  // attr.add(ATT_SUMMARY_LOG);
  // attr.add(ATT_DETAILED_LOG);
  // attr.add(ATT_FULL_LOG);
  //
  // attr.add(ECB_DIRECTION);
  // attr.add(FED_DIRECTION);
  // attr.add(BOE_DIRECTION);
  // attr.add(SWISS_DIRECTION);
  // attr.add(EUREX_DIRECTION);
  // attr.add(MEFF_DIRECTION);
  // attr.add(COPY_HC_AS_QUOTES);
  // attr.add(CUSTOM_DATA_SOURCE);
  // attr.add(EUREX_QUOTE_SET);
  // attr.add(MEFF_QUOTE_SET);
  // attr.add(PRICE_TYPE);
  // attr.add(QUOTE_DATE_SELECTION);
  //
  // return attr;
  // }
  /** Check ST attributes are valid */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public boolean isValidInput(final Vector messages) {
    boolean retVal = super.isValidInput(messages);

    final String seperator = getAttribute(ATT_SEPERATOR);
    if (Util.isEmpty(seperator)) {
      messages.addElement(ATT_SEPERATOR + " is not specified");
      retVal = false;
    }
    final String noOfFeeds = getAttribute(ATT_NUMBER_OF_FEEDS);

    try {
      if (Util.isEmpty(noOfFeeds)) {
        messages.addElement(ATT_NUMBER_OF_FEEDS + " must be specified.");
      } else if ((Integer.parseInt(noOfFeeds) < 0) || (Integer.parseInt(noOfFeeds) > 6)) {
        messages.addElement(ATT_NUMBER_OF_FEEDS + " must be a number <=6.");
        retVal = false;
      }
    } catch (Exception exc) {
      messages.addElement(ATT_NUMBER_OF_FEEDS + " must be a number<=5.");
      Log.error(this, exc); // sonar
      retVal = false;
    }

    final String controlLine = getAttribute(CONTROL_LINE);
    if (Util.isEmpty(controlLine)) {
      messages.addElement(CONTROL_LINE + " is not specified");
      retVal = false;
    }

    if (Util.isEmpty(getAttribute(ECB_DIRECTION))) {
      messages.addElement(ECB_DIRECTION + " is not specified");
      retVal = false;
    }
    if (Util.isEmpty(getAttribute(FED_DIRECTION))) {
      messages.addElement(FED_DIRECTION + " is not specified");
      retVal = false;
    }
    if (Util.isEmpty(getAttribute(BOE_DIRECTION))) {
      messages.addElement(BOE_DIRECTION + " is not specified");
      retVal = false;
    }
    if (Util.isEmpty(getAttribute(SWISS_DIRECTION))) {
      messages.addElement(SWISS_DIRECTION + " is not specified");
      retVal = false;
    }
    if (Util.isEmpty(getAttribute(EUREX_DIRECTION))) {
      messages.addElement(EUREX_DIRECTION + " is not specified");
      retVal = false;
    }
    if (Util.isEmpty(getAttribute(MEFF_DIRECTION))) {
      messages.addElement(MEFF_DIRECTION + " is not specified");
      retVal = false;
    }
    return retVal;
  }

  /** @return number of fields in each record */
  public int getNumberOfFields() {
    // We process the first two fields and each feed contains two fields
    return 2 + (Integer.parseInt(getAttribute(ATT_NUMBER_OF_FEEDS)) * 2);
  }

  /**
   * Process the bond line
   *
   * @param bond
   * @param bean
   */
  private void processBondLine(Bond bond, DiscountableProductBean bean) {
    int noOfFeed = Integer.parseInt(getAttribute(ATT_NUMBER_OF_FEEDS));
    if (noOfFeed >= 1) {
      bond.setSecCode(BOND_SEC_CODE_DISCOUNTABLE_ECB, bean.isDiscountableECB());
      if ("true".equals(bean.isDiscountableECB())) {
        if (getAttribute(ECB_DIRECTION).equals(REGULAR)) {
          getBondCustomData(bond).setHaircut_ecb(Double.parseDouble(bean.getHaircutECB()));
        } else {
          getBondCustomData(bond).setHaircut_ecb(100.0 - Double.parseDouble(bean.getHaircutECB()));
        }
      } else {
        getBondCustomData(bond).setHaircut_ecb(null);
      }
    }
    if (noOfFeed >= 2) {
      bond.setSecCode(BOND_SEC_CODE_DISCOUNTABLE_FED, bean.isDiscountableFED());
      if ("true".equals(bean.isDiscountableFED())) {
        if (getAttribute(FED_DIRECTION).equals(REGULAR)) {
          getBondCustomData(bond).setHaircut_fed(Double.parseDouble(bean.getHaircutFED()));
        } else {
          getBondCustomData(bond).setHaircut_fed(100.0 - Double.parseDouble(bean.getHaircutFED()));
        }
      } else {
        getBondCustomData(bond).setHaircut_fed(null);
      }
    }

    if (noOfFeed >= 3) {
      bond.setSecCode(BOND_SEC_CODE_DISCOUNTABLE_BOE, bean.isDiscountableBOE());

      if ("true".equals(bean.isDiscountableBOE())) {
        if (getAttribute(BOE_DIRECTION).equals(REGULAR)) {
          getBondCustomData(bond).setHaircut_boe(Double.parseDouble(bean.getHaircutBOE()));
        } else {
          getBondCustomData(bond).setHaircut_boe(100.0 - Double.parseDouble(bean.getHaircutBOE()));
        }
      } else {
        getBondCustomData(bond).setHaircut_boe(null);
      }
    }

    if (noOfFeed >= 4) {
      bond.setSecCode(BOND_SEC_CODE_DISCOUNTABLE_SWISS, bean.isDiscountableSwiss());

      if ("true".equals(bean.isDiscountableSwiss())) {
        if (getAttribute(SWISS_DIRECTION).equals(REGULAR)) {
          getBondCustomData(bond).setHaircut_swiss(Double.parseDouble(bean.getHaircutSwiss()));
        } else {
          getBondCustomData(bond)
              .setHaircut_swiss(100.0 - Double.parseDouble(bean.getHaircutSwiss()));
        }
      } else {
        getBondCustomData(bond).setHaircut_swiss(null);
      }
    }

    if (noOfFeed >= 5) {
      bond.setSecCode(BOND_SEC_CODE_DISCOUNTABLE_EUREX, bean.isDiscountableEurex());

      if ("true".equals(bean.isDiscountableEurex())) {
        if (getAttribute(EUREX_DIRECTION).equals(REGULAR)) {
          getBondCustomData(bond).setHaircut_eurex(Double.parseDouble(bean.getHaircutEurex()));
        } else {
          getBondCustomData(bond)
              .setHaircut_eurex(100.0 + Double.parseDouble(bean.getHaircutEurex()));
        }
      } else {
        getBondCustomData(bond).setHaircut_eurex(null);
      }
    }
    if (noOfFeed >= 6) {
      bond.setSecCode(BOND_SEC_CODE_DISCOUNTABLE_MEFF, bean.isDiscountableMeff());

      if ("true".equals(bean.isDiscountableMeff())) {
        if (getAttribute(MEFF_DIRECTION).equals(REGULAR)) {
          getBondCustomData(bond).setHaircut_meff(Double.parseDouble(bean.getHaircutMeff()));
        } else {
          getBondCustomData(bond)
              .setHaircut_meff(100.0 - Double.parseDouble(bean.getHaircutMeff()));
        }
      } else {
        getBondCustomData(bond).setHaircut_meff(null);
      }
    }
  }

  // GSM 03/09/14 Idem for Equity
  /**
   * Process the equity line
   *
   * @param equity
   * @param bean
   */
  private void processEquityLine(Equity equity, DiscountableProductBean bean) {

    int noOfFeed = Integer.parseInt(getAttribute(ATT_NUMBER_OF_FEEDS));

    if (noOfFeed >= 5) {
      equity.setSecCode(BOND_SEC_CODE_DISCOUNTABLE_EUREX, bean.isDiscountableEurex());

      if ("true".equals(bean.isDiscountableEurex())) {
        if (getAttribute(EUREX_DIRECTION).equals(REGULAR)) {
          getEquityCustomData(equity).setHaircut_eurex(Double.parseDouble(bean.getHaircutEurex()));
        } else {
          getEquityCustomData(equity)
              .setHaircut_eurex(100.0 + Double.parseDouble(bean.getHaircutEurex()));
        }
      } else {
        getEquityCustomData(equity).setHaircut_eurex(null);
      }
    }
    if (noOfFeed >= 6) {
      equity.setSecCode(BOND_SEC_CODE_DISCOUNTABLE_MEFF, bean.isDiscountableMeff());

      if ("true".equals(bean.isDiscountableMeff())) {
        if (getAttribute(MEFF_DIRECTION).equals(REGULAR)) {
          getEquityCustomData(equity).setHaircut_meff(Double.parseDouble(bean.getHaircutMeff()));
        } else {
          getEquityCustomData(equity)
              .setHaircut_meff(100.0 - Double.parseDouble(bean.getHaircutMeff()));
        }
      } else {
        getEquityCustomData(equity).setHaircut_meff(null);
      }
    }
  }

  /**
   * Removes the bond custom data
   *
   * @param bond
   */
  private void removeCustomDataAttributes(final Bond bond) {
    BondCustomData customData = (BondCustomData) bond.getCustomData();
    if (customData != null) {
      customData.setHaircut_boe(null);
      customData.setHaircut_ecb(null);
      customData.setHaircut_fed(null);
      customData.setHaircut_swiss(null);
      customData.setHaircut_eurex(null);
      customData.setHaircut_meff(null);
    }
    if (bond.getSecCodes() != null) {
      bond.getSecCodes().remove(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_BOE);
      bond.getSecCodes().remove(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_ECB);
      bond.getSecCodes().remove(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_FED);
      bond.getSecCodes().remove(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_SWISS);
      bond.getSecCodes().remove(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_EUREX);
      bond.getSecCodes().remove(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_MEFF);
    }
  }

  // GSM 03/09/14
  /**
   * emoves the equity custom data
   *
   * @param equity
   */
  private void removeECBAttributes(Equity equity) {
    EquityCustomData customData = (EquityCustomData) equity.getCustomData();
    if (customData != null) {
      customData.setActive_available_qty(null);
      customData.setExpired_date(null);
      customData.setExpired_date_type(null);
      customData.setFee(null);
      customData.setHaircut_eurex(null);
      customData.setHaircut_meff(null);
    }
    if (equity.getSecCodes() != null) {
      equity.getSecCodes().remove(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_EUREX);
      equity.getSecCodes().remove(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_MEFF);
    }
  }

  /**
   * @param bond
   * @return the custom data
   */
  private BondCustomData getBondCustomData(final Bond bond) {
    BondCustomData customData = (BondCustomData) bond.getCustomData();
    if (customData == null) {
      customData = new BondCustomData();
      customData.setProductId(bond.getId());
      customData.setVersion(bond.getVersion());
      bond.setCustomData(customData);
    }
    return customData;
  }

  // GSM 03/09/14 Idem for Equity
  /**
   * @param equity
   * @return the custom data
   */
  private EquityCustomData getEquityCustomData(final Equity equity) {
    EquityCustomData customData = (EquityCustomData) equity.getCustomData();
    if (customData == null) {
      customData = new EquityCustomData();
      customData.setProductId(equity.getId());
      customData.setVersion(equity.getVersion());
      equity.setCustomData(customData);
    }
    return customData;
  }

  /**
   * @return a map with all the bonds in the system where Map {REF_INTERNA, Bond}
   * @throws RemoteException
   */
  private Map<String, Bond> loadBondsMap() throws RemoteException {
    Map<String, Bond> bondsMap = new HashMap<String, Bond>();
        String from = null;
        String where = " product_desc.product_family='Bond'";
		long TInicio, TFin, tiempo;  /** */
		TInicio = System.currentTimeMillis();  /** */
    @SuppressWarnings("unchecked")
    Vector<Bond> allBonds = getDSConnection().getRemoteProduct().getAllProducts(from, where, null);
    TFin = System.currentTimeMillis();
    /** */
    tiempo = TFin - TInicio;
    /** */
    System.err.println("*******Tiempo total para Bonds: " + tiempo + ". Total: " + allBonds.size());
    /** */
    Log.info(Log.OLD_TRACE, "All bonds have been loaded: " + allBonds.size());
    /** */
    for (Bond bond : allBonds) {
      String refInterna = bond.getSecCode(BOND_SEC_CODE_REF_INTERNA);
      if (Util.isEmpty(refInterna)) {
        Log.error(
            LOG_CATEGORY_SCHEDULED_TASK, "Bond doesn't have REF_INTERNA. Bond Id =" + bond.getId());
      } else {
        // Check if the Bond already exists in the map. If so it is
        // duplicate REF_INTERNA
        if (bondsMap.get(refInterna) != null) {
          Log.error(
              LOG_CATEGORY_SCHEDULED_TASK,
              "Duplicate REF_INTERNA for Bond ids ="
                  + bondsMap.get(refInterna).getId()
                  + ", "
                  + bond.getId());
        } else {
          bondsMap.put(refInterna, bond);
        }
      }
    }
    // Bond b1 = (Bond)
    // getDSConnection().getRemoteProduct().getProductByCode("REF_INTERNA",
    // "C0.RD.LST.12268");
    // bondsMap.put("C0.RD.LST.12268", b1);
    // Bond b2 = (Bond)
    // getDSConnection().getRemoteProduct().getProductByCode("REF_INTERNA",
    // "C0.RD.LST.12349");
    // bondsMap.put("C0.RD.LST.12349", b2);
    // Bond b3 = (Bond)
    // getDSConnection().getRemoteProduct().getProductByCode("REF_INTERNA",
    // "C0.RD.LST.12189");
    // bondsMap.put("C0.RD.LST.12189", b3);
    // Bond b4 = (Bond)
    // getDSConnection().getRemoteProduct().getProductByCode("REF_INTERNA",
    // "C0.RD.LST.12269");
    // bondsMap.put("C0.RD.LST.12269", b4);
    // Bond b5 = (Bond)
    // getDSConnection().getRemoteProduct().getProductByCode("REF_INTERNA",
    // "C0.RD.LST.12272");
    // bondsMap.put("C0.RD.LST.12272", b5);
    System.err.println("*******" + allBonds.size() + " Bonds totales");
    /** */
    return bondsMap;
  }

  /**
   * @return a map with all the equities in the system where Map {ISIN+CCY, Equity}
   * @throws RemoteException
   */
  @SuppressWarnings({"unchecked"})
  private Map<String, Equity> loadEquitiesMap() throws RemoteException {

    final Map<String, Equity> equitiesMap = new HashMap<String, Equity>();
    // START CALYPCROSS-420 - fperezur
        final String from = null;
        final String where = " product_desc.product_family='Equity'";
    // END CALYPCROSS-420 - fperezur
    long TInicio, TFin, tiempo;
    /** */
    TInicio = System.currentTimeMillis();
    /** */
    final Vector<Equity> allEquities =
        getDSConnection().getRemoteProduct().getAllProducts(from, where, null);
    TFin = System.currentTimeMillis();
    /** */
    tiempo = TFin - TInicio;
    /** */
    System.err.println(
        "*******Tiempo total para Equities: " + tiempo + ". Total: " + allEquities.size());
    /** */
    Log.info(Log.OLD_TRACE, "All Equities have been loaded: " + allEquities.size());
    /** */
    for (Equity e : allEquities) {

      final String isin = e.getSecCode(ISIN);
      final String ccy = e.getCurrency();

      if (checkEquity(isin, e)) {

        final String key = isin.trim() + ccy.trim();
        if (!equitiesMap.containsKey(key)) {
          equitiesMap.put(key, e);
        }
      }
    }
    System.err.println("*******" + equitiesMap.size() + " Equities totales");
    /** */
    return equitiesMap;
  }

  /**
   * @param isin
   * @param e Equity
   * @return true if the equity has the isin + currency
   */
  private boolean checkEquity(final String isin, final Equity e) {

    final String ccy = e.getCurrency();

    boolean ok = true;
    if (Util.isEmpty(isin)) {
      Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Equity doesn't have ISIN. Equity Id =" + e.getId());
      ok = false;
    }
    if (Util.isEmpty(ccy)) {
      Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Equity doesn't have CCY. Equity Id =" + e.getId());
      ok = false;
    }
    return ok;
  }

  /**
   * @param line
   * @return the Discountable bean. If product id cannot be processed, it will return null;
   */
  private DiscountableProductBean buildBean(final String line) {
    String[] fields =
        CollateralUtilities.splitMejorado(
            getNumberOfFields(), getAttribute(ATT_SEPERATOR), false, line);
    for (int i = 0; i < fields.length; i++) {
      fields[i] = fields[i].trim();
    }
    return new DiscountableProductBean(fields);
  }

  // BAU - Use different way to import data in order to avoid multiple files
  // problem
  /**
   * @param path
   * @param fileName
   * @param date
   * @return the file to import
   */
  public File lookForFile(String path, String fileName, JDate date) {

    final String fileNameFilter = fileName;
    // name filter
    FilenameFilter filter =
        new FilenameFilter() {
          @Override
          public boolean accept(File directory, String fileName) {
            return fileName.startsWith(fileNameFilter);
          }
        };

    final File directory = new File(path);
    final File[] listFiles = directory.listFiles(filter);

    for (File file : listFiles) {

      final Long dateFileMilis = file.lastModified();
      final Date dateFile = new Date(dateFileMilis);
      final JDate jdateFile = JDate.valueOf(dateFile);

      @SuppressWarnings("unused")
      double aux = JDate.diff(date, jdateFile);

      if (JDate.diff(date, jdateFile) == 0) {
        return file;
      }
    }

    return null;
  }

  // BAU - Use different way to import data in order to avoid multiple files
  // problem
  /** Retrieves the file name */
  @Override
  public String getFileName() {
    return this.file.getName();
  }

  /** Task Info */
  @Override
  public String getTaskInformation() {
    return "Import Discountable Bonds info.";
  }

  // Quick dev to copy HR from custom data to quotes
  // the following methods are copied from the ScheduledTask
  @SuppressWarnings({"unused"})
  private boolean copyHaircutAsQuoteValues() throws Exception {
    Vector<QuoteValue> quotevalues = new Vector<QuoteValue>();
    Collection<Bond> bonds = loadBondsWithCustomData();
    JDate valDate = getValuationDatetime(true).getJDate(TimeZone.getDefault());

    final Map<String, QuoteValue> eurexExistingQuotes =
        getQuotesMap(valDate, getAttribute(EUREX_QUOTE_SET));
    final Map<String, QuoteValue> meffExistingQuotes =
        getQuotesMap(valDate, getAttribute(MEFF_QUOTE_SET));

    for (Bond bond : bonds) {
      Vector<QuoteValue> newQuotes =
          getQuotesFromCustomData(bond, eurexExistingQuotes, meffExistingQuotes);
      if (!Util.isEmpty(newQuotes)) {
        quotevalues.addAll(newQuotes);
      }
    }

    boolean saveQuoteValues = getDSConnection().getRemoteMarketData().saveQuoteValues(quotevalues);
    return saveQuoteValues;
  }

  /**
   * @param entry from where to build the quote
   * @param eurexExistingQuotes list of quotes for the valueDate
   * @param meffExistingQuotes list of quotes for the valueDate
   * @param valueDate
   * @return the quote value to add into Calypso
   */
  private Collection<? extends QuoteValue> getQuoteValue(
      final Entry<String, QuoteInfo> entry,
      final Map<String, QuoteValue> eurexExistingQuotes,
      final Map<String, QuoteValue> meffExistingQuotes,
      final JDate valueDate) {

    Vector<QuoteValue> quotevalues = new Vector<QuoteValue>();
    final QuoteInfo qi = entry.getValue();

    if (qi.disc_eurex) {

      quotevalues.add(
          getQuoteValue(
              qi.hc_eurex,
              eurexExistingQuotes,
              entry.getKey(),
              valueDate,
              getAttribute(EUREX_QUOTE_SET)));
    }

    if (qi.disc_meff) {

      quotevalues.add(
          getQuoteValue(
              qi.hc_meff,
              meffExistingQuotes,
              entry.getKey(),
              valueDate,
              getAttribute(MEFF_QUOTE_SET)));
    }

    return quotevalues;
  }

  /**
   * @param bond
   * @param eurexExistingQuotes
   * @param meffExistingQuotes
   * @return QuoteValue from bond custom data
   * @throws RemoteException
   */
  private Vector<QuoteValue> getQuotesFromCustomData(
      Bond bond,
      Map<String, QuoteValue> eurexExistingQuotes,
      Map<String, QuoteValue> meffExistingQuotes)
      throws RemoteException {

    JDate valDate = getValuationDatetime(true).getJDate(TimeZone.getDefault());
    Vector<QuoteValue> quotevalues = new Vector<QuoteValue>();

    BondCustomData customData = (BondCustomData) bond.getCustomData();
    if (customData != null) {

      Double eurexHaircut = customData.getHaircut_eurex();
      Double meefHaircut = customData.getHaircut_meff();
      boolean addEurex = false;
      boolean addMeff = false;

      if (EUREX_HAIRCUT.equals(getAttribute(CUSTOM_DATA_SOURCE)) && (eurexHaircut != null)) {
        addEurex = true;
      } else if (MEEF_HAIRCUT.equals(getAttribute(CUSTOM_DATA_SOURCE)) && (meefHaircut != null)) {
        addMeff = true;
      } else if ("ALL".equals(getAttribute(CUSTOM_DATA_SOURCE))) {
        if (eurexHaircut != null) {
          addEurex = true;
        }

        if (meefHaircut != null) {
          addMeff = true;
        }
      }

      if (addEurex) {
        quotevalues.add(
            getQuoteValue(
                customData.getHaircut_eurex(),
                eurexExistingQuotes,
                bond,
                valDate,
                getAttribute(EUREX_QUOTE_SET)));
      }

      if (addMeff) {
        quotevalues.add(
            getQuoteValue(
                customData.getHaircut_meff(),
                meffExistingQuotes,
                bond,
                valDate,
                getAttribute(MEFF_QUOTE_SET)));
      }
    }
    return quotevalues;
  }

  /**
   * @param haircutValue
   * @param existingQuotes
   * @param bond
   * @param valDate
   * @param quoteSetName
   * @return the QuoteValue for the bond
   */
  private QuoteValue getQuoteValue(
      Double haircutValue,
      Map<String, QuoteValue> existingQuotes,
      Bond bond,
      JDate valDate,
      String quoteSetName) {
    if (haircutValue != null) {
      // GSM 09/05/16 Mig. v14 - Haircuts based on quote set now as 100
      // Need to devide by 100 as this is one percent based
      // haircutValue = haircutValue / 100;

      QuoteValue qv = existingQuotes.get(bond.getQuoteName());
      if (qv == null) {
        qv = new QuoteValue();
        qv.setName(bond.getQuoteName());
        qv.setDate(valDate);
        qv.setQuoteSetName(quoteSetName);
      }

      // BondCustomData customData = (BondCustomData)
      // bond.getCustomData();

      qv.setQuoteType(getAttribute(PRICE_TYPE));
      qv.setAsk(haircutValue);
      qv.setBid(haircutValue);
      qv.setClose(haircutValue);
      qv.setOpen(haircutValue);
      qv.setHigh(haircutValue);
      qv.setLow(haircutValue);
      qv.setLast(haircutValue);
      return qv;
    }
    return null;
  }

  /**
   * @param haircutValue to save in the quoteValue
   * @param existingQuotes to check if must be created or renewed.
   * @param quoteName to update
   * @param valDate of the quote
   * @param quoteSetName
   * @return a quote value
   */
  private QuoteValue getQuoteValue(
      Double haircutValue,
      final Map<String, QuoteValue> existingQuotes,
      final String quoteName,
      final JDate valDate,
      final String quoteSetName) {
    if (haircutValue != null) {
      // Need to devide by 100 as this is a percentage
      // haircutValue = haircutValue / 100.0;

      QuoteValue qv = existingQuotes.get(quoteName);
      if (qv == null) {
        qv = new QuoteValue();
        qv.setName(quoteName);
        qv.setDate(valDate);
        qv.setQuoteSetName(quoteSetName);
      }

      qv.setQuoteType(getAttribute(PRICE_TYPE));
      qv.setAsk(haircutValue);
      qv.setBid(haircutValue);
      qv.setClose(haircutValue);
      qv.setOpen(haircutValue);
      qv.setHigh(haircutValue);
      qv.setLow(haircutValue);
      qv.setLast(haircutValue);
      return qv;
    }
    return null;
  }

  /**
   * @return al the bonds and their custom data
   * @throws RemoteException
   */
  private Collection<Bond> loadBondsWithCustomData() throws RemoteException {
    // List<Bond> bondsList = new ArrayList<Bond>();
    Map<String, Bond> bondsMap = new HashMap<String, Bond>();

    String from = null;
    String where =
        " product_desc.product_family='Bond' "
            + "and exists (select 1 from bond_custom_data where bond_custom_data.product_id=product_desc.product_id)";
    @SuppressWarnings("unchecked")
    Vector<Bond> allBonds = getDSConnection().getRemoteProduct().getAllProducts(from, where, null);
    for (Bond bond : allBonds) {
      if (bond.getCustomData() != null) {
        // bondsList.add(bond);
        bondsMap.put(bond.getSecCode("ISIN"), bond);
      }
    }

    return bondsMap.values();
  }

  /**
   * @param valDate quote date
   * @param quoteSetName to recover
   * @return all the quotes as a map for a date
   * @throws RemoteException
   */
  @SuppressWarnings("unchecked")
  private Map<String, QuoteValue> getQuotesMap(JDate valDate, String quoteSetName)
      throws RemoteException {
    Map<String, QuoteValue> map = new HashMap<String, QuoteValue>();
    Vector<QuoteValue> quoteValues =
        getDSConnection().getRemoteMarketData().getQuoteValues(valDate, quoteSetName);

    for (QuoteValue quote : quoteValues) {
      map.put(quote.getName(), quote);
    }

    return map;
  }

  // START CALYPCROSS-38 - mromerod
  /**
   * Inner class to manage massive saving Bond threads
   *
   * @author everis
   */
  class SegmentThread extends Thread {

    private Object[] tradeArray;
    private boolean noBond = false;
    private int start;
    private double start_double;
    private int end;
    private double end_double;
    private String remove;

    /**
     * Thread segmentation method
     *
     * @param segment
     * @param tradeArray
     * @param noBond
     */
    public SegmentThread(int segment, Object[] tradeArray, boolean noBond, String remove) {
      this.tradeArray = tradeArray;
      this.noBond = noBond;
      this.remove = remove;

      /*Cambios en la l?gica -> c?lculos con doubles, resultados parseados a int. P
       * this.start = (this.tradeArray.length / NUMBER_THREADS) * segment;
      this.end = Math.min(this.start + this.tradeArray.length/ NUMBER_THREADS, this.tradeArray.length);*/
      this.start_double = (this.tradeArray.length / NUMBER_THREADS_DOUBLE) * segment;
      this.end_double =
          Math.min(
              this.start_double + this.tradeArray.length / NUMBER_THREADS_DOUBLE,
              this.tradeArray.length);
      this.start = (int) this.start_double;
      this.end = (int) this.end_double;
    }

    /**
     * Thread segmentation method
     *
     * @param segment
     * @param tradeArray
     */
    public SegmentThread(int segment, Object[] tradeArray, String remove) {
      this(segment, tradeArray, false, remove);
    }

    /** Thread start method */
    @Override
    public void run() {
      //  Iterate and save
      for (int i = start; i < end; i++) {
        Bond bond = null;
        Equity equity = null;
        try {

          if (noBond) {
            equity = (Equity) tradeArray[i];
          } else {
            bond = (Bond) tradeArray[i];
          }

          if (remove != null && !"".equals(remove)) {
            if (remove.equalsIgnoreCase(CT_BOND)) {
              removeCustomDataAttributes(bond);
            } else if (remove.equalsIgnoreCase(CT_EQUITY)) {
              removeECBAttributes(equity);
            }
          }

          if (noBond) {
            getDSConnection().getRemoteProduct().saveProduct(equity, true); // save equity
          } else {
            getDSConnection().getRemoteProduct().saveBond(bond, true);
          }

        } catch (RemoteException re) {
          Log.error(this, re); // sonar
          if (noBond) {
            if (equity != null) {
              if (remove != null && !"".equals(remove)) {
                Log.error(
                    LOG_CATEGORY_SCHEDULED_TASK,
                    "Error saving Bond after removing ECB Attribute, ISIN="
                        + equity.getSecCode("ISIN")
                        + ", Currency="
                        + equity.getCurrency()
                        + ", Error="
                        + re.getLocalizedMessage());
              } else {
                if (equity.getProduct() != null) {
                  Log.error(
                      LOG_CATEGORY_SCHEDULED_TASK,
                      " : Error while saving the Equity with ISIN&CCY="
                          + equity.getProduct().getId()
                          + ", Error="
                          + re.getLocalizedMessage());
                } else {
                  Log.error(LOG_CATEGORY_SCHEDULED_TASK, " : Error=" + re.getLocalizedMessage());
                }
              }
            }
          } else {
            if (bond != null) {
              if (remove != null && !"".equals(remove)) {
                Log.error(
                    LOG_CATEGORY_SCHEDULED_TASK,
                    "Error saving Bond after removing ECB Attribute, ISIN="
                        + bond.getSecCode("ISIN")
                        + ", Currency="
                        + bond.getCurrency()
                        + ", Error="
                        + re.getLocalizedMessage());
              } else {
                if (bond.getProduct() != null) {
                  Log.error(
                      LOG_CATEGORY_SCHEDULED_TASK,
                      " : Error while saving the Bond with REF_INTERNA="
                          + bond.getProduct().getId()
                          + ", Error="
                          + re.getLocalizedMessage());
                } else {
                  Log.error(LOG_CATEGORY_SCHEDULED_TASK, " : Error=" + re.getLocalizedMessage());
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Manage Saving threads
   *
   * @author everis
   */
  private SegmentThread[] manageSavingThreads(
      Collection<Bond> collectionBond,
      Collection<Equity> collectionEquity,
      boolean noBond,
      String remove) {
    SegmentThread threads[] = new SegmentThread[NUM_THREADS];

    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i] = null;

      if (collectionBond != null && (collectionBond.size() >= NUM_THREADS || i == 0)) {
        threads[i] = new SegmentThread(i, collectionBond.toArray(), noBond, remove);
      }

      if (collectionEquity != null && (collectionEquity.size() >= NUM_THREADS || i == 0)) {
        threads[i] = new SegmentThread(i, collectionEquity.toArray(), noBond, remove);
      }
    }

    for (int i = 0; i < NUM_THREADS; i++) {
      if (threads[i] != null) {
        threads[i].start();
      }
    }

    return threads;
  }

  /**
   * Join Threads
   *
   * @param threads
   */
  private void joinThreads(SegmentThread[] threads) {
    try {
      for (int i = 0; i < NUM_THREADS; i++) {
        if (threads[i] != null) {
          threads[i].join();
        }
      }

    } catch (InterruptedException e) {
      Log.error(this, "Thread interruption building threads");
    }
  }
  // END CALYPCROSS-38 - mromerod

}
