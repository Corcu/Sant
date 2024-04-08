package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;

public class Opt_AcceptableCollateralReport extends ProductReport {

  private static final long serialVersionUID = 123L;

  // START CALYPCROSS-420 - fperezur
  public static final int NUM_THREAD = 4;
  // END CALYPCROSS-420 - fperezur

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public ReportOutput load(final Vector errorMsgsP) {

    try {
      return getReportOutput();
    } catch (RemoteException e) {
      String error = "Error generating Optimization_HaircutDefinitionReport.\n";
      Log.error(this, error, e);
      errorMsgsP.add(error + e.getMessage());
    }

    return null;
  }

  /**
   * Get report output
   *
   * @return
   * @throws RemoteException
   */
  public ReportOutput getReportOutput() throws RemoteException {

    final DefaultReportOutput output = new StandardReportOutput(this);
    final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

    Date iniProcess = new Date();

    Log.info(
        this,
        ">>>>>>>>>> LOG Optimizacion Procesos: Inicio del proceso (" + iniProcess + ") <<<<<<<<<<");

    // load products
    Date iniProducts = new Date();
    List<Product> products = loadAllProducts();

    if (Util.isEmpty(products)) {
      Log.info(this, "No product found.\n");
      return null;
    }

    Date finProducts = new Date();
    Log.info(
        this,
        ">>>>>>>>>> LOG Optimizacion Procesos: Se han obtenido "
            + products.size()
            + " productos en "
            + (finProducts.getTime() - iniProducts.getTime())
            + "milisecs ("
            + finProducts
            + ") <<<<<<<<<<");

    Date iniContracts = new Date();
    // load contracts - GSM 15/07/15. SBNA Multi-PO filter
    Collection<CollateralConfig> contracts = loadOpenContracts(super.getReportTemplate());
    if (Util.isEmpty(contracts)) {
      Log.info(this, "No contract found.\n");
      return null;
    }
    Date finContracts = new Date();
    Log.info(
        this,
        ">>>>>>>>>> LOG Optimizacion Procesos: Se han obtenido "
            + contracts.size()
            + " contratos en "
            + (finContracts.getTime() - iniContracts.getTime())
            + "milisecs ("
            + finContracts
            + ") <<<<<<<<<<");

    Date iniItems = new Date();
    // load items
    List<Opt_AcceptableCollateralItem> acceptCollatItems =
        new ArrayList<Opt_AcceptableCollateralItem>(
            buildItems(contracts, products, getReportTemplate()));
    for (Opt_AcceptableCollateralItem acceptCollatItem : acceptCollatItems) {

      ReportRow row = new ReportRow(acceptCollatItem.getProduct(), ReportRow.PRODUCT);
      row.setProperty(
          Opt_AcceptableCollateralReportTemplate.OPT_ACCEPTABLE_COLLAT_ITEM, acceptCollatItem);
      reportRows.add(row);
    }
    Date finItems = new Date();
    Log.info(
        this,
        ">>>>>>>>>> LOG Optimizacion Procesos: Se han generado "
            + acceptCollatItems.size()
            + " items en "
            + (finItems.getTime() - iniItems.getTime())
            + "milisecs ("
            + finItems
            + ") <<<<<<<<<<");

    // set report rows on output
    output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

    Date finProcess = new Date();

    Log.info(
        this,
        ">>>>>>>>>> LOG Optimizacion Procesos: Fin del proceso. El proceso ha tardado "
            + (finProcess.getTime() - iniProcess.getTime())
            + "milisecs ("
            + finProcess
            + ") <<<<<<<<<<");

    return output;
  }

  /**
   * Load all bonds and equities
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public static List<Product> loadAllProducts() {

    List<Product> allProducts = new Vector<Product>();
    String from = null;
    String whereBond = " product_desc.product_family='Bond'";
    String whereEquity = " product_desc.product_family='Equity'";
    Vector<Product> bondProducts, equityProducts;
    try {
      bondProducts =
          DSConnection.getDefault().getRemoteProduct().getAllProducts(from, whereBond, null);
      equityProducts =
          DSConnection.getDefault().getRemoteProduct().getAllProducts(from, whereEquity, null);
      allProducts.addAll(bondProducts);
      allProducts.addAll(equityProducts);
    } catch (RemoteException e) {
      Log.error(Opt_AcceptableCollateralReport.class, "Cannot get products from DB.\n", e);
    }

    return allProducts;
  }

  /**
   * Load all contracts in the system with status OPEN
   *
   * @param reportTemplate
   * @return
   */
  public static Collection<CollateralConfig> loadOpenContracts(
      final ReportTemplate reportTemplate) {

    ArrayList<Integer> contractsIds = new ArrayList<Integer>();
    Map<Integer, CollateralConfig> contractsMap = new HashMap<Integer, CollateralConfig>();
    String query = "select mrg_call_def from mrgcall_config where agreement_status = 'OPEN'";

    // GSM 24/07/15. SBNA Multi-PO filter
    query = CollateralUtilities.filterPoByQuery(reportTemplate, query);

    try {
      // get contract ids
      contractsIds =
          SantReportingUtil.getSantReportingService(DSConnection.getDefault())
              .getMarginCallConfigIds(query);
      // get contracts
      contractsMap =
          SantReportingUtil.getSantReportingService(DSConnection.getDefault())
              .getMarginCallConfigByIds(contractsIds);
    } catch (RemoteException e) {
      Log.error(Opt_AcceptableCollateralReport.class, "Cannot get contract ids from DB", e);
    } catch (PersistenceException e) {
      Log.error(Opt_AcceptableCollateralReport.class, "Cannot get contracts from DB", e);
    }
    return contractsMap.values();
  }

  // START CALYPCROSS-420 - fperezur
  /**
   * Build data items
   *
   * @param contracts
   * @param products
   * @param reportTemplate
   * @return
   */
  public static ConcurrentLinkedDeque<Opt_AcceptableCollateralItem> buildItems(
      Collection<CollateralConfig> contracts,
      final List<Product> products,
      ReportTemplate reportTemplate) {

    final ConcurrentLinkedDeque<Opt_AcceptableCollateralItem> items =
        new ConcurrentLinkedDeque<Opt_AcceptableCollateralItem>();
    final ConcurrentLinkedDeque<CollateralConfig> contractList =
        new ConcurrentLinkedDeque<CollateralConfig>(contracts);
    final ConcurrentHashMap<StaticDataFilter, Object> setCombined = new ConcurrentHashMap<>();
    ExecutorService exec = Executors.newFixedThreadPool(NUM_THREAD);

    try {
      for (int j = 0; j < NUM_THREAD; j++) {
        exec.execute(
            new Runnable() {
              public void run() {
                CollateralConfig contract;
                while (null != (contract = contractList.poll())) {
                  List<StaticDataFilter> filters = contract.getEligibilityFilters();
                  if (Util.isEmpty(filters)) {
                    continue;
                  } else {
                    for (StaticDataFilter filter : filters) {
                      setCombined.put(filter, true);
                    }
                  }
                }
              }
            });
      }
    } finally {
      exec.shutdown();
      try {
        exec.awaitTermination(240, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        Log.error(
            Opt_AcceptableCollateralReport.class,
            "FAIL: Thread interruption Service: " + e.getMessage(),
            e);
      }
    }

    Set<StaticDataFilter> filterSetList = setCombined.keySet();
    final ConcurrentLinkedDeque<StaticDataFilter> allFilters =
        new ConcurrentLinkedDeque<StaticDataFilter>(filterSetList);
    exec = Executors.newFixedThreadPool(NUM_THREAD);

    try {
      for (int j = 0; j < NUM_THREAD; j++) {
        exec.execute(
            new Runnable() {
              public void run() {
                StaticDataFilter filter;
                while (null != (filter = allFilters.poll())) {
                  List<Product> contractAcceptedProducts = getAcceptedProducts(products, filter);
                  for (Product product : contractAcceptedProducts) {
                    // build item
                    Opt_AcceptableCollateralItem item =
                        new Opt_AcceptableCollateralItem(product, filter.getName());
                    items.add(item);
                  }
                }
              }
            });
      }

    } finally {
      exec.shutdown();
      try {
        exec.awaitTermination(240, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        Log.error(
            Opt_AcceptableCollateralReport.class,
            "FAIL: Thread interruption Service: " + e.getMessage(),
            e);
      }
    }

    return items;
  }
  // END CALYPCROSS-420 - fperezur

  /**
   * Get accepted products for a static data filter
   *
   * @param products
   * @param sdf
   * @return
   */
  private static List<Product> getAcceptedProducts(List<Product> products, StaticDataFilter sdf) {

    List<Product> acceptedProducts = new ArrayList<Product>();

    for (Product product : products) {
      if (sdf.accept(null, product)) {
        acceptedProducts.add(product);
      }
    }

    return acceptedProducts;
  }
}
