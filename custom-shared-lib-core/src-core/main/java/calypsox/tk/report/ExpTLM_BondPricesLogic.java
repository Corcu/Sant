package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.ProductDesc;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteProduct;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Class with the necessary logic to retrieve the different values for the BondPrices report.
 *
 * @author David Porras Mart?nez
 */
public class ExpTLM_BondPricesLogic {

    private static final String LADO = "S";
    private static String fecha;
    private static String feed_name;
    private static final int NUM_MAX_DECIMALS = 10;

    public ExpTLM_BondPricesLogic() {
    }

    /**
     * Retrieve the date.
     *
     * @param
     * @return String with the date in dd/mm/yyyy fomat.
     */
    public String getFecha() {
        return fecha;
    }

    /**
     * Retrieve the feed of the bond.
     *
     * @param
     * @return String with the feed.
     */
    public String getFeed() {
        return feed_name;
    }

    /**
     * Retrieve the lado of the bond.
     *
     * @param
     * @return String with the lado.
     */
    public String getLado() {
        return LADO;
    }

    /**
     * Retrieve the price of the bond.
     *
     * @param qv QuoteValue associated with the bond.
     * @return String with the close price in the correct format
     */
    public String getPrice(final QuoteValue qv) {

        try {
            return Util.numberToString(CollateralUtilities.truncDecimal(NUM_MAX_DECIMALS, qv.getClose() * 100))
                    .replace(".", "");
        } catch (NumberFormatException e) {
            Log.error("Close quote is NaN", e);
            return "";
        }

    }

    /**
     * Retrieve the ISIN of the bond.
     *
     * @param qv QuoteValue corresponding to the bond, dsConn DSConnection
     * @return String with the ISIN.
     */
    @SuppressWarnings("unchecked")
    public String getIsin(final QuoteValue qv, final DSConnection dsConn) {

        String isin = "";

        // 1. get product desc item from quote name
        // 2. obtain the bond id
        // 3. use it for obtain the bond from product table
        final RemoteProduct rp = dsConn.getRemoteProduct();
        try {
            String clausule = "quote_name =";
            clausule = clausule.concat(ioSQL.string2SQLString(qv.getName()));
            final Vector<ProductDesc> v = rp.getAllProductDesc(clausule, null);
            if (!Util.isEmpty(v)) {
                final Bond bond = (Bond) rp.getProduct(v.get(0).getId());
                isin = bond.getSecCode("ISIN");
            }
        } catch (final RemoteException e) {
            Log.error(this, e); //sonar
        }
        return isin;
    }

    /**
     * Method used to add the rows in the report generated.
     *
     * @param bond      Bond.
     * @param dsConn    Database connection.
     * @param errorMsgs Vector with the different errors occurred.
     * @return Vector with the rows added.
     */
    public static Vector<ExpTLM_BondPricesItem> getReportRows(final QuoteValue qv, final String date,
                                                              final String feed, final String price_type, final DSConnection dsConn, final Vector<String> errorMsgs) {

        final Vector<ExpTLM_BondPricesItem> reportRows = new Vector<ExpTLM_BondPricesItem>();
        final ExpTLM_BondPricesLogic verifiedRow = new ExpTLM_BondPricesLogic();
        ExpTLM_BondPricesItem rowCreated = null;
        fecha = date;
        feed_name = feed;

        rowCreated = verifiedRow.getExpTLM_BondPricesItem(qv, dsConn, errorMsgs);
        if (null != rowCreated) { // If the result row is equals to NULL, we
            // don't add this row to the report.
            reportRows.add(rowCreated);
        }

        return reportRows;
    }

    /**
     * Method that retrieve row by row from Calypso, to insert in the vector with the result to show.
     *
     * @param bond   Bond.
     * @param dsConn Database connection.
     * @param errors Vector with the different errors occurred.
     * @return The row retrieved from the system, with the necessary information.
     * @throws RemoteException
     */
    private ExpTLM_BondPricesItem getExpTLM_BondPricesItem(final QuoteValue qv, final DSConnection dsConn,
                                                           final Vector<String> errors) {

        final ExpTLM_BondPricesItem expTLM_BondPricesItem = new ExpTLM_BondPricesItem();

        if (qv != null) {
            // Set values
            // If we don't have ISIN, we return NULL.
            String isin = getIsin(qv, dsConn);
            if (Util.isEmpty(isin)) {
                return null;
            }
            expTLM_BondPricesItem.setFecha(getFecha());
            expTLM_BondPricesItem.setFeed(getFeed());
            expTLM_BondPricesItem.setLado(getLado());
            expTLM_BondPricesItem.setIsin(isin);
            expTLM_BondPricesItem.setPrice(getPrice(qv));

            return expTLM_BondPricesItem;
        }

        return null;

    }

}
