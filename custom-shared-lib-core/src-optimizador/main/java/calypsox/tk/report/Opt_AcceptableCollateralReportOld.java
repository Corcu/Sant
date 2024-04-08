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

public class Opt_AcceptableCollateralReportOld extends ProductReport {

  private static final long serialVersionUID = 123L;

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

    // load products
    long tIn, tOut, tiempo;
    /** */
    tIn = System.currentTimeMillis();
    /** */
    List<Product> products = loadAllProducts();
    tOut = System.currentTimeMillis();
    /** */
    tiempo = tOut - tIn;
    /** */
    System.err.println(products.size() + " productos en " + tiempo + " milisecs");
    /** */
    if (Util.isEmpty(products)) {
      Log.info(this, "No product found.\n");
      return null;
    }

    // load contracts - GSM 15/07/15. SBNA Multi-PO filter
    tIn = System.currentTimeMillis();
    /** */
    Collection<CollateralConfig> contracts = loadOpenContracts(super.getReportTemplate());
    tOut = System.currentTimeMillis();
    /** */
    tiempo = tOut - tIn;
    /** */
    System.err.println(contracts.size() + " contratos en " + tiempo + " milisecs");
    /** */
    if (Util.isEmpty(contracts)) {
      Log.info(this, "No contract found.\n");
      return null;
    }

    // load items
    tIn = System.currentTimeMillis();
    /** */
    List<Opt_AcceptableCollateralItem> acceptCollatItems =
        buildItems(contracts, products, getReportTemplate());
    tOut = System.currentTimeMillis();
    /** */
    tiempo = tOut - tIn;
    /** */
    System.err.println(acceptCollatItems.size() + " items en " + tiempo + " milisecs");
    /** */
    for (Opt_AcceptableCollateralItem acceptCollatItem : acceptCollatItems) {

      ReportRow row = new ReportRow(acceptCollatItem.getProduct(), ReportRow.PRODUCT);
      row.setProperty(
          Opt_AcceptableCollateralReportTemplate.OPT_ACCEPTABLE_COLLAT_ITEM, acceptCollatItem);
      reportRows.add(row);
    }

    // set report rows on output
    output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

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
    String whereBond = " product_desc.product_family='Bond' and product_desc.product_id > 14536238";
    String whereEquity =
        " product_desc.product_family='Equity' and product_desc.product_id > 14536238";
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
    String query =
        "select mrg_call_def from mrgcall_config where agreement_status = 'OPEN' and mrg_call_def > 14906413";

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

  /**
   * Build data items
   *
   * @param contracts
   * @param products
   * @param reportTemplate
   * @return
   */
  public static List<Opt_AcceptableCollateralItem> buildItems(
      Collection<CollateralConfig> contracts,
      List<Product> products,
      ReportTemplate reportTemplate) {

    List<Opt_AcceptableCollateralItem> items = new ArrayList<Opt_AcceptableCollateralItem>();
    List<String> secFiltersProcessed = new ArrayList<String>();

    for (CollateralConfig contract : contracts) {

      //			// GSM 15/07/15. SBNA Multi-PO filter
      //			if (CollateralUtilities.filterPoByTemplate(reportTemplate, contract)) {
      //				return null;
      //			}

      // get eligible security filters
      List<StaticDataFilter> filters = contract.getEligibilityFilters();
      if (Util.isEmpty(filters)) {
        continue;
      }

      // get accepted products for each filter
      for (StaticDataFilter filter : filters) {

        // check filter already processed
        if (secFiltersProcessed.contains(filter.getName())) {
          continue;
        }
        List<Product> contractAcceptedProducts = getAcceptedProducts(products, filter);
        for (Product product : contractAcceptedProducts) {
          // build item
          Opt_AcceptableCollateralItem item =
              new Opt_AcceptableCollateralItem(product, filter.getName());
          items.add(item);
        }
        // add filter to list
        secFiltersProcessed.add(filter.getName());
      }
    }

    return items;
  }

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
