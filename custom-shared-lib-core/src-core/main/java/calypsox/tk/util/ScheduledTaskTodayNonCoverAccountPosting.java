package calypsox.tk.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.ForexClearFileReader;
import calypsox.util.ForexClearSTUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.util.*;

public class ScheduledTaskTodayNonCoverAccountPosting extends ScheduledTask {

  private static final long serialVersionUID = 123L;
  private ForexClearFileReader file = null;

  // Logs
  protected LogGeneric logGen = new LogGeneric();
  protected String fileName = "";

  // Logs

  /** Devuelve la descripcion de la Scheduled Task */
  public String getTaskInformation() {
    return "This report provides details of non-cover related account postings, such as Interest, fees or coupon payments due";
  }

  /** Devuelve la lista de los atributos de Scheduled Task */
  protected List<AttributeDefinition> buildAttributeDefinition() {
    List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
    attributeList.addAll(super.buildAttributeDefinition());
    attributeList.add(attribute(ForexClearSTUtil.FIELD_SEPARATOR));
    attributeList.add(attribute(ForexClearSTUtil.FILE_NAME));
    attributeList.add(attribute(ForexClearSTUtil.FILE_PATH));
    // Logs
    attributeList.add(attribute(ForexClearSTUtil.SUMMARY_LOG));
    attributeList.add(attribute(ForexClearSTUtil.DETAILED_LOG));
    attributeList.add(attribute(ForexClearSTUtil.FULL_LOG));
    attributeList.add(attribute(ForexClearSTUtil.STATIC_DATA_LOG));
    // Logs
    return attributeList;
  }

  public boolean process(DSConnection ds, PSConnection ps) {
    // Atributos inicializados del ST
    String separator = getAttribute(ForexClearSTUtil.FIELD_SEPARATOR);
    fileName = getAttribute(ForexClearSTUtil.FILE_NAME);
    final String path = getAttribute(ForexClearSTUtil.FILE_PATH);
    final JDate date = this.getValuationDatetime().getJDate(TimeZone.getDefault());
    ArrayList<String> error = new ArrayList<>();

    fileName = ForexClearSTUtil.getFileName(date, fileName);

    ForexClearSTUtil.checkAtributes(separator, path, fileName, date, error);

    if (separator.equalsIgnoreCase("\\t")) {
      separator = "\t";
    }
    if (!error.isEmpty()) {
      for (String msg : error) Log.error(this, msg);
      return false;
    }

    // Logs
    String time = "";
    synchronized (ForexClearSTUtil.timeFormat) {
      time = ForexClearSTUtil.timeFormat.format(date.getDate());
    }
    this.logGen.generateFiles(
        getAttribute(ForexClearSTUtil.DETAILED_LOG),
        getAttribute(ForexClearSTUtil.FULL_LOG),
        getAttribute(ForexClearSTUtil.STATIC_DATA_LOG),
        time);
    try {
      this.logGen.initializeFiles(this.getClass().getSimpleName());
    } catch (IOException e1) {
      this.logGen.incrementError();
      this.logGen.setErrorCreatingLogFile(this.getClass().getSimpleName(), fileName);
      Log.error(this, e1);
    }
    // Logs

    // Lectura del fichero
    file = new ForexClearFileReader(path, fileName, date, separator, error);
    if (!error.isEmpty()) {
      for (String msg : error) Log.error(this, msg);
      // Logs
      this.logGen.incrementError();
      this.logGen.setErrorNumberOfFiles(this.getClass().getSimpleName(), fileName);
      ForexClearSTUtil.returnErrorLog(
          logGen,
          false,
          date,
          fileName,
          path,
          getAttribute(ForexClearSTUtil.SUMMARY_LOG),
          this.getClass().getSimpleName());
      // Logs
      return false;
    }

    // copy
    if (!ForexClearFileReader.copyFile(path, fileName)) {
      Log.error(this, "ERROR: Failed to copy file");
      this.logGen.incrementError();
      this.logGen.setErrorMovingFile(this.getClass().getSimpleName(), fileName);
      ForexClearSTUtil.returnErrorLog(
          logGen,
          false,
          date,
          fileName,
          path,
          getAttribute(ForexClearSTUtil.SUMMARY_LOG),
          this.getClass().getSimpleName());
      return false;
    }

    // PRE: el archivo debe contener alguna fila // CR27 Move to OK (true)
    if (file.getLinesSize() == 0) {
      Log.error(this, "El fichero esta vacio");
      // Logs
      this.logGen.incrementError();
      this.logGen.setErrorNumberOfLines(this.getClass().getSimpleName(), fileName);
      ForexClearSTUtil.returnErrorLog(
          logGen,
          true,
          date,
          fileName,
          path,
          getAttribute(ForexClearSTUtil.SUMMARY_LOG),
          this.getClass().getSimpleName());
      // Logs
      return true;
    }

    ForexClearSTUtil.initLegalEntities(ds, error);
    if (!error.isEmpty()) {
      for (String msg : error) Log.error(this, msg);
      // Logs
      this.logGen.incrementError();
      this.logGen.setErrorRequiredFieldNotPresentNotValid(
          this.getClass().getSimpleName(),
          fileName,
          String.valueOf(this.logGen.getNumberTotal()),
          "0",
          "PO or CTPY",
          "",
          "");
      ForexClearSTUtil.returnErrorLog(
          logGen,
          false,
          date,
          fileName,
          path,
          getAttribute(ForexClearSTUtil.SUMMARY_LOG),
          this.getClass().getSimpleName());
      // Logs
      return false; // cuando el resultado es false se introduce al array
      // de fallos
    }

    ArrayList<Integer> lineArray = filterLines();

    Map<String, CollateralConfig> mapCcByCcy = null;

    if (!lineArray.isEmpty()) { // CR27 Empty, do nothing
      mapCcByCcy = findContracts(ds, error, lineArray);

      if (!error.isEmpty()) {
        for (String msg : error) Log.error(this, msg);
        // Logs
        this.logGen.incrementError();
        this.logGen.setErrorRequiredFieldNotPresentNotValid(
            this.getClass().getSimpleName(),
            fileName,
            String.valueOf(this.logGen.getNumberTotal()),
            "0",
            "CONTRACT",
            "",
            "");
        // Logs
      }
    }

    int position = 0;

    for (int i = 0; i < lineArray.size(); i++) {
      position = lineArray.get(i);
      // Logs
      this.logGen.incrementTotal();
      // Logs
      String currency = this.file.getValue(ForexClearSTUtil.CURRENCY, position);

      // Use the contract by currency
      final CollateralConfig cc = mapCcByCcy.get(currency);

      if (cc != null) {
        Trade trade = this.createTrade(position, cc, currency, error);

        if (trade == null) {
          error.add("ERROR: No se ha podido crear el Simple Transfer , fila (" + i + ")");
          this.logGen.incrementRecordErrors();
          this.logGen.setErrorRequiredFieldNotPresentNotValid(
              this.getClass().getSimpleName(),
              fileName,
              String.valueOf(this.logGen.getNumberTotal()),
              "0",
              "Failed to create SimpleTransfer",
              "",
              String.valueOf(i));
          continue;
        }

        if (!ForexClearSTUtil.checkAndSaveTrade(position, trade, error)) {
          // Logs
          this.logGen.incrementRecordErrors();
          this.logGen.setErrorSavingTrade(
              this.getClass().getSimpleName(),
              fileName,
              String.valueOf(position),
              "",
              String.valueOf(position));
          // Logs
        } else {
          this.logGen.incrementOK();
          this.logGen.setOkLine(
              this.getClass().getSimpleName(),
              fileName,
              position,
              String.valueOf(trade.getLongId()));
        }
      } else {
        error.add("ERROR: No se ha obtenido el CollateralConfig de la divisa (" + currency + ")");
        this.logGen.incrementRecordErrors();
        this.logGen.setErrorRequiredFieldNotPresentNotValid(
            this.getClass().getSimpleName(),
            fileName,
            String.valueOf(this.logGen.getNumberTotal()),
            "0",
            "Failed getting CollateralConfig by currency",
            "",
            currency);
        continue;
      }
    }

    for (String msg : error) Log.error(this, msg);

    // post process
    try {
      ForexClearFileReader.postProcess(error.isEmpty(), date, fileName, path);
    } catch (Exception e1) {
      Log.error(this, e1); // sonar
      this.logGen.incrementError();
      this.logGen.setErrorMovingFile(this.getClass().getSimpleName(), fileName);
    }

    // Logs
    try {
      this.logGen.feedGenericLogProcess(
          fileName,
          getAttribute(ForexClearSTUtil.SUMMARY_LOG),
          this.getClass().getSimpleName(),
          this.logGen.getNumberTotal() - 1);
      this.logGen.feedFullLog(0);
      this.logGen.feedDetailedLog(0);
      this.logGen.closeLogFiles();
    } catch (final IOException e) {
      Log.error(this, e); // sonar
    }
    // Logs

    return error.isEmpty();
  }

  /**
   * findContract.
   *
   * @param ds DSConnection
   * @param error ArrayList<String>
   * @param lineArray ArrayList<Integer>
   * @return CollateralConfig
   */
  private Map<String, CollateralConfig> findContracts(
      DSConnection ds, ArrayList<String> error, ArrayList<Integer> lineArray) {

    Map<String, CollateralConfig> mapCcByCcy = new HashMap<String, CollateralConfig>();

    int linea = 0;
    for (int i = 0; i < lineArray.size(); i++) {
      CollateralConfig cc = null;
      linea = lineArray.get(i);
      String divisa = this.file.getValue(ForexClearSTUtil.CURRENCY, linea);

      // new CCys
      if (ForexClearSTUtil.USD_CURRENCY.equals(divisa)
          && !mapCcByCcy.keySet().contains(ForexClearSTUtil.USD_CURRENCY)) {
        cc =
            ForexClearSTUtil.findContract(
                ds, divisa, ForexClearSTUtil.CSA, ForexClearSTUtil.IM_INTEREST_USD);
        if (cc != null) {
          mapCcByCcy.put(ForexClearSTUtil.USD_CURRENCY, cc);
        }
      } else if (ForexClearSTUtil.EUR_CURRENCY.equals(divisa)
          && !mapCcByCcy.keySet().contains(ForexClearSTUtil.EUR_CURRENCY)) {
        cc =
            ForexClearSTUtil.findContract(
                ds, divisa, ForexClearSTUtil.CSA, ForexClearSTUtil.IM_INTEREST_EUR);
        if (cc != null) {
          mapCcByCcy.put(ForexClearSTUtil.EUR_CURRENCY, cc);
        }
      } else if (ForexClearSTUtil.GBP_CURRENCY.equals(divisa)
          && !mapCcByCcy.keySet().contains(ForexClearSTUtil.GBP_CURRENCY)) {
        cc =
            ForexClearSTUtil.findContract(
                ds, divisa, ForexClearSTUtil.CSA, ForexClearSTUtil.IM_INTEREST_GBP);
        if (cc != null) {
          mapCcByCcy.put(ForexClearSTUtil.GBP_CURRENCY, cc);
        }
      } else {
        if (!ForexClearSTUtil.GBP_CURRENCY.equals(divisa)
            && !ForexClearSTUtil.EUR_CURRENCY.equals(divisa)
            && !ForexClearSTUtil.USD_CURRENCY.equals(divisa)) {
          error.add(
              "ERROR: El valor de la divisa en la linea ("
                  + linea
                  + ") no corresponde con USD/EUR/GBP");
        }
      }
    }

    if (mapCcByCcy.isEmpty()) {
      error.add("ERROR: No se han encontrado contratos con las divisas USD/EUR/GBP ");
    }
    return mapCcByCcy;
  }

  //	/**
  //	 * findContract.
  //	 *
  //	 * @param ds
  //	 *            DSConnection
  //	 * @param error
  //	 *            ArrayList<String>
  //	 * @param lineArray
  //	 *            ArrayList<Integer>
  //	 * @return CollateralConfig
  //	 */
  //	private CollateralConfig findContract(DSConnection ds,
  //			ArrayList<String> error, ArrayList<Integer> lineArray) {
  //		CollateralConfig cc = null;
  //		int linea = 0;
  //		for (int i = 0; i < lineArray.size(); i++) {
  //			linea = lineArray.get(i);
  //			String divisa = this.file
  //					.getValue(ForexClearSTUtil.CURRENCY, linea);
  //			if (ForexClearSTUtil.USD_CURRENCY.equals(divisa)) {
  //				cc = ForexClearSTUtil.findContract(ds, divisa,
  //						ForexClearSTUtil.CSA, ForexClearSTUtil.IM_INTEREST);
  //				break;
  //
  //			} else {
  //				error.add("ERROR: El valor de la divisa en la linea (" + linea
  //						+ ") no es USD");
  //			}
  //
  //		}
  //
  //		if (cc == null) {
  //			error.add("ERROR: No se ha encontrado el contrato con la divisa ");
  //
  //		}
  //		return cc;
  //	}

  private double findPrincipal(int linea, ArrayList<String> error) {
    String debit = this.file.getValue(ForexClearSTUtil.POSTINGDEBIT, linea);
    String credit = this.file.getValue(ForexClearSTUtil.POSTINGCREDIT, linea);
    double debito = 0.0;
    double credito = 0.0;
    if (Util.isEmpty(debit)) {
      error.add("El campo de posting debit esta vacio en la linea (" + linea + ")");
      Log.error(this, "El campo de posting debit esta vacio en la linea (" + linea + ")");
      return 0.0;
    }
    try {
      debito = Double.valueOf(debit);
    } catch (NumberFormatException e) {
      Log.error(this, e);
    }
    if (Util.isEmpty(credit)) {
      error.add("El campo de posting credit esta vacio en la linea (" + linea + ")");
      Log.error(this, "El campo de posting credit esta vacio en la linea (" + linea + ")");
      return 0.0;
    }
    try {
      credito = Double.valueOf(credit);
    } catch (NumberFormatException e) {
      Log.error(this, e);
    }
    if (debito != 0.0 && credito != 0.0) {
      error.add("No pueden estar los dos campos (posting debit y credit) inicializados");
    } else if ((debito == 0.0 && credito == 0.0)) {
      error.add("No pueden estar los dos campos (posting debit y credit) inicializados a 0");
    } else if (debito == 0.0) return credito;
    else return debito;

    return 0.0;
  }

  /**
   * Crear un trade a partir de los datos del fichero y el contrato
   *
   * @param linea int
   * @param cc CollateralConfig
   * @param currency String
   * @param error ArrayList<String>
   * @return Trade
   */
  private Trade createTrade(
      int linea, CollateralConfig cc, String currency, ArrayList<String> error) {

    MarginCall mc = createMarginCall(linea, cc, currency, error);
    if (mc == null) {
      error.add("No se ha podido crear el MArgin Call");
      return null;
    }

    Trade trade = new Trade();
    trade.setProduct(mc);
    trade.setTraderName(ForexClearSTUtil.NONE);
    trade.setSalesPerson(ForexClearSTUtil.NONE);
    trade.setBook(cc.getBook());

    trade.setTradeDate(this.getValuationDatetime());
    trade.setSettleDate(this.getValuationDatetime().getJDate(null));
    trade.setAction(Action.NEW);
    trade.setStatus(Status.S_NONE);
    trade.setCounterParty(ForexClearSTUtil.counterParty);
    ((MarginCall)trade.getProduct()).setOrdererRole("Client");
    ((MarginCall)trade.getProduct()).setOrdererLeId(trade.getCounterParty().getId());
    trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, cc.getId());

    // ForexClear Type depending on currency
    String forexClearType = ForexClearSTUtil.IM_INTEREST;
    if (ForexClearSTUtil.EUR_CURRENCY.equals(currency)) {
      forexClearType = ForexClearSTUtil.IM_INTEREST_EUR;
    } else if (ForexClearSTUtil.GBP_CURRENCY.equals(currency)) {
      forexClearType = ForexClearSTUtil.IM_INTEREST_GBP;
    } else if (ForexClearSTUtil.USD_CURRENCY.equals(currency)) {
      forexClearType = ForexClearSTUtil.IM_INTEREST_USD;
    }

    trade.addKeyword(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE, forexClearType);

    //		trade.addKeyword(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE,
    //				ForexClearSTUtil.IM_INTEREST);

    trade.addKeyword(
        CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT, ForexClearSTUtil.REPORT_22A);

    trade.setTradeCurrency(currency);
    trade.setSettleCurrency(currency);
    trade.setEnteredUser(DSConnection.getDefault().getUser());
    trade.setQuantity(getQuantity(linea, mc.getPrincipal()));
    ((MarginCall) trade.getProduct())
        .setPrincipal(((MarginCall) trade.getProduct()).getPrincipal() * trade.getQuantity());

    return trade;
  }

  // Quantity: if debit/PAY(-) > -1 ; credit/RECEIVE(+) > +1
  private double getQuantity(final int linea, final double principal) {

    final String debit = this.file.getValue(ForexClearSTUtil.POSTINGDEBIT, linea);
    final String credit = this.file.getValue(ForexClearSTUtil.POSTINGCREDIT, linea);

    if (principal == Double.valueOf(debit)) {
      return -1.0;
    } else if (principal == Double.valueOf(credit)) {
      return 1.0;
    }

    return 0.0;
  }

  /**
   * createMarginCall.
   *
   * @param linea int
   * @param cc CollateralConfig
   * @param currency String
   * @param error ArrayList<String>
   * @return MarginCall
   */
  private MarginCall createMarginCall(
      int linea, CollateralConfig cc, String currency, ArrayList<String> error) {
    // Create margin call
    MarginCall mcall = new MarginCall();
    double principal = this.findPrincipal(linea, error);

    if (!Util.isEmpty(error)) {
      for (String msg : error) Log.error(this, msg);
      return null;
    }
    mcall.setPrincipal(principal);
    mcall.setCurrencyCash(currency);
    mcall.setFlowType(ForexClearSTUtil.COLLATERAL);
    mcall.setLinkedLongId(cc.getId());
    mcall.setOrdererLeId(ForexClearSTUtil.processingOrg.getId());

    return mcall;
  }

  /**
   * filterLines.
   *
   * @return ArrayList<Integer>
   */
  private ArrayList<Integer> filterLines() {
    ArrayList<Integer> lineasArray = new ArrayList<Integer>();

    for (int linea = 0; linea < this.file.getLinesSize(); linea++) {
      String descripcion = this.file.getValue(ForexClearSTUtil.POSTINGDESCRIPTION, linea);
      if (!Util.isEmpty(descripcion)
          && (descripcion.equals(ForexClearSTUtil.CSH_COL_FEE)
              || descripcion.equals(ForexClearSTUtil.INTEREST))) {
        String currency = this.file.getValue(ForexClearSTUtil.CURRENCY, linea);
        if (!Util.isEmpty(currency)
            && (currency.equals(ForexClearSTUtil.USD_CURRENCY)
                || currency.equals(ForexClearSTUtil.EUR_CURRENCY) // New ccys
                || currency.equals(ForexClearSTUtil.GBP_CURRENCY))) {
          lineasArray.add(linea);
        }
      }
    }
    return lineasArray;
  }
}
