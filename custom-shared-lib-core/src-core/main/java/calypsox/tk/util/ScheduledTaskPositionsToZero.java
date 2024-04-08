package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.bean.CSVPositionsBean;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.BOPositionAdjustment;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventorySecurityPositionArray;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * To import check if we don't have updated a particular position, in that case, we will put to zero, and we cancel all
 * the trades marked to do that.
 *
 * @author Jose David Sevillano (josedavid.sevillano@siag.es)
 */
public class ScheduledTaskPositionsToZero extends ScheduledTask {
    private static final String FORMAT_DATE = "Format for the date";
    private static final String DATE = "Date";
    private static final String BOOKS_COLLATERAL = "BOOKS_COLLATERAL";
    private static final long serialVersionUID = 123L;
    private static final String SUBSTITUTABLE = "Y";
    private static final String QTY = "QTY";
    private static final String TRADE_TO_BE_CANCELED = "TRADE_TO_BE_CANCELED";
    private static final String ISIN = "ISIN";
    private static final String SECURITY = "SECURITY";
    private static final String FAILED = "FAILED";
    private static final String ACTUAL = "ACTUAL";
    private static final String SETTLED = "SETTLED";
    private static final String PENDING = "PENDING";
    private static final String THEORETICAL = "THEORETICAL";
    private static final String POSITION_STATUS = "POSITION_STATUS";
    private static final String PLEDGED = "PLEDGED";
    private static final String LOAN = "LOAN";
    private static final String REPO = "REPO";
    private static final String BUYSELL = "BUYSELL";
    private static final String BO_SYSTEM = "BO_SYSTEM";
    private static final String SOURCE_SYSTEM = "Source System";
    private static final String TASK_INFORMATION = "Put positions to zero when we don't have a particular position in the file to import.";
    private DSConnection conn;
    private boolean bResult = false;
    private final Hashtable<String, String> hashAdjusType = new Hashtable<String, String>();
    private final Hashtable<String, String> hashPosType = new Hashtable<String, String>();
    private SimpleDateFormat simpleDateFormat = null;

    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.add(attribute(SOURCE_SYSTEM));
        attributeList.add(attribute(DATE));
        attributeList.add(attribute(FORMAT_DATE));
        return attributeList;
    }
//
//	@Override
//	public Vector<String> getDomainAttributes() {
//		@SuppressWarnings("unchecked")
//		Vector<String> vectorAttr = super.getDomainAttributes();
//		if (null == vectorAttr) {
//			vectorAttr = new Vector<String>();
//		}
//
//		vectorAttr.add(SOURCE_SYSTEM);
//		vectorAttr.add(DATE);
//		vectorAttr.add(FORMAT_DATE);
//		return vectorAttr;
//	}

    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {
        this.conn = conn;

        // We fill the hashtable with the different types for the adjustments.
        this.hashAdjusType.put(BUYSELL, "Buy/Sell");
        this.hashAdjusType.put(REPO, "Repo");
        this.hashAdjusType.put(LOAN, "SecurityLending");
        // hashAdjusType.put(COLLATERAL, "BSB");
        this.hashAdjusType.put(PLEDGED, "Pledge");

        // We fill the hastable with the values for the position type attribute.
        this.hashPosType.put(THEORETICAL, THEORETICAL);
        this.hashPosType.put(SETTLED, ACTUAL);
        this.hashPosType.put(PENDING, FAILED);

        try {
            final InventorySecurityPositionArray arrayToClear = buildDataBean();
            // If we have any value in the array to clean the old positions, we
            // populate the fields and create the movement.
            if (!arrayToClear.isEmpty()) {
                for (int numPosClear = 0; numPosClear < arrayToClear.size(); numPosClear++) {
                    final InventorySecurityPosition invSecPos = arrayToClear.get(numPosClear);
                    final String[] cancelValues = generateData(invSecPos);
                    if (null != cancelValues) {
                        final CSVPositionsBean positionsToCancel = new CSVPositionsBean(cancelValues);
                        insertMovementToZero(positionsToCancel);
                    }
                }
            }

            cancelTradesToBeCanceled();
            this.bResult = true;
        } catch (final Exception e) {
            Log.warn(this, e); //sonar
            this.bResult = false;
        }

        if (this.bResult) {
            ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
        }

        return this.bResult;
    }

    private InventorySecurityPositionArray buildDataBean() throws RemoteException, ParseException {
        final InventorySecurityPositionArray positionArrayToClear = new InventorySecurityPositionArray();
        JDate date;

        final String strDate = getAttribute(DATE);
        String strFormat = getAttribute(FORMAT_DATE);

        if ((null != strDate) && !"".equals(strDate)) {
            if ((null != strFormat) && !"".equals(strFormat)) {
                this.simpleDateFormat = new SimpleDateFormat(strFormat);
                date = JDate.valueOf(strDate);
            } else {
                strFormat = "dd/MM/yyyy";
                this.simpleDateFormat = new SimpleDateFormat(strFormat);
                date = JDate.valueOf(strDate);
            }
        } else {
            date = JDate.getNow();
            strFormat = "dd/MM/yyyy";
            this.simpleDateFormat = new SimpleDateFormat(strFormat);
        }

        final JDate today = date;

        // We get the positions for yesterday and for today in the system, to
        // compare them and to put to zero which don't exist actually in the
        // file.
        final InventorySecurityPositionArray positionArray = getInventorySecPositions(today);
        for (int numPos = 0; numPos < positionArray.size(); numPos++) {
            final InventorySecurityPosition position = positionArray.get(numPos);
            if ((null != position) && (position.getTotal() != 0.0)) {
                JDate positionDate = position.getPositionDate();
                if ((null != positionDate) && positionDate.before(today)) {
                    positionArrayToClear.add(position); // We add the position to the new array.
                }
            }
        }

        return positionArrayToClear;
    }

    private InventorySecurityPositionArray getInventorySecPositions(JDate today) throws RemoteException {
        StringBuilder where = new StringBuilder();
        where.append(" inv_secposition.internal_external = 'INTERNAL' ");
        where.append(" AND inv_secposition.date_type = 'TRADE' ");
        where.append(" AND inv_secposition.position_type = 'THEORETICAL'");
        where.append(" AND inv_secposition.position_date = ");

        where.append(" (");// BEGIN SELECT
        where.append(" select MAX(temp.position_date) from inv_secposition temp ");
        where.append(" WHERE inv_secposition.internal_external = temp.internal_external ");
        where.append(" AND inv_secposition.date_type = temp.date_type ");
        where.append(" AND inv_secposition.position_type = temp.position_type ");
        where.append(" AND inv_secposition.account_id = temp.account_id ");
        where.append(" AND inv_secposition.security_id = temp.security_id ");
        where.append(" AND inv_secposition.agent_id = temp.agent_id ");
        where.append(" AND TRUNC(temp.position_date) <= ").append(Util.date2SQLString(today));
        where.append(" )");// END SELECT

        InventorySecurityPositionArray secPositions = DSConnection.getDefault().getRemoteBO()
                .getInventorySecurityPositions("", where.toString(), null);

        return secPositions;
    }

    @SuppressWarnings("rawtypes")
    private String[] generateData(final InventorySecurityPosition invSecPos) throws RemoteException {
        final String[] cancelValues = new String[14];

        Book bookCancel = invSecPos.getBook();
        // We check if the book in the file is in the DomainValues to exclude
        // it, because it's a Collateral Book.
        final Vector booksCollat = this.conn.getRemoteReferenceData().getDomainValues(BOOKS_COLLATERAL);
        if ((null != booksCollat) && !booksCollat.isEmpty() && (null != bookCancel)) {
            if (booksCollat.contains(bookCancel.getAuthName())) {
                return null;
            }
        }

        // BO_SYSTEM.
        cancelValues[0] = getAttribute(SOURCE_SYSTEM);

        bookCancel = invSecPos.getBook();
        if (null != bookCancel) {
            // Processing Org.
            cancelValues[1] = bookCancel.getLegalEntity().getAuthName();
            // Book.
            cancelValues[2] = bookCancel.getAuthName();
        }

        final Product product = invSecPos.getProduct();
        if (null != product) {
            // ISIN.
            cancelValues[3] = product.getSecCode(ISIN);
            // SEC_DESCR.
            cancelValues[4] = product.getDescription();
        }

        // VALUE_DATE.
        final JDate jdate = JDate.getNow();
        cancelValues[5] = this.simpleDateFormat.format(jdate.getDate(TimeZone.getDefault()));
        // QTY_NOM.
        cancelValues[6] = QTY;
        // VALUE_NOM_QTY.
        cancelValues[7] = String.valueOf(0 - invSecPos.getTotal());
        // CURR.
        cancelValues[8] = invSecPos.getSettleCurrency();
        // POSITION_TYPE.
        cancelValues[9] = invSecPos.getPositionType();
        // ADJUSTMENT_TYPE.
        // TODO TO REVIEW
        cancelValues[10] = BUYSELL;
        // REUSABLE.
        // TODO TO REVIEW
        cancelValues[11] = SUBSTITUTABLE;
        // CUSTODIAN.
        final LegalEntity agent = invSecPos.getAgent();
        if (null != agent) {
            cancelValues[12] = agent.getAuthName();
        }
        // ACCOUNT.
        final Account acc = invSecPos.getAccount();
        if (null != acc) {
            cancelValues[13] = acc.getAuthName();
        }

        return cancelValues;
    }

    private void cancelTradesToBeCanceled() throws RemoteException {
        final String fromClause = "TRADE, TRADE_KEYWORD";
        final String whereClause = "TRADE.TRADE_STATUS != 'CANCELED' AND TRADE.TRADE_ID = TRADE_KEYWORD.TRADE_ID AND TRADE_KEYWORD.KEYWORD_NAME = '"
                + TRADE_TO_BE_CANCELED + "' AND TRADE_KEYWORD.KEYWORD_VALUE = 'true'";

        final TradeArray tradeArray = this.conn.getRemoteTrade().getTrades(fromClause, whereClause, null, false, null);
        if ((null != tradeArray) && !tradeArray.isEmpty()) {
            for (int numTrades = 0; numTrades < tradeArray.size(); numTrades++) {
                final Trade trade = tradeArray.get(numTrades);
                trade.setAction(Action.CANCEL);
                this.conn.getRemoteTrade().save(trade);
            }
        }
    }

    /**
     * Method to do the operation.
     *
     * @param csvPosBean2       CSVPositionsBean with the values for each line read.
     * @param tradeToBeCanceled String parameter that indicates if the Trade is to be canceled in the future or not (TRUE or FALSE).
     * @throws Exception
     * @throws CloneNotSupportedException MODIFIED (Bean)
     */
    private void insertMovementToZero(final CSVPositionsBean csvPosBean2) throws Exception {
        Trade newTrade = null;
        BOPositionAdjustment boPosAdjust = null;

        // Fill the information for the product & trade.
        boPosAdjust = loadProductData(csvPosBean2);
        // We check if the security in the product is a Bond or not.
        if (boPosAdjust.getSecurity() instanceof Bond) {
            newTrade = loadTradeData(boPosAdjust, csvPosBean2);

            // Save the new Trade & Product.
            newTrade.setAction(Action.NEW);
            this.conn.getRemoteTrade().save(newTrade);
        }
    }

    /**
     * To load the information related to a product specified into the CSV file.
     *
     * @param csvPosBean2 CSVPositionsBean with the values for each line read.
     * @return Product loaded or a new one created.
     * @throws RemoteException If it doesn't exist the Legal Entity read from the CSV file in the DataBase.
     *                         <p>
     *                         MODIFIED (Bean)
     */
    private BOPositionAdjustment loadProductData(final CSVPositionsBean csvPosBean2) throws RemoteException {
        final BOPositionAdjustment boPosAux = new BOPositionAdjustment();

        if (null != csvPosBean2) {
            // PO. We retrieve the ID for the PO passed as a parameter.
            final LegalEntity legalEntity = this.conn.getRemoteReferenceData().getLegalEntity(
                    csvPosBean2.getProcessingOrg());
            if (null != legalEntity) {
                boPosAux.setOrdererLeId(legalEntity.getId());
                boPosAux.setOrdererRole(LegalEntity.PROCESSINGORG);
            } else {
                throw new RemoteException("LegalEntity <" + csvPosBean2.getProcessingOrg() + "> doesn't exist");
            }

            // ISIN shouldn't be set for the trades as product secCode
            // boPosAux.setSecCode(ISIN, csvPosBean2.getIsin());

            // Security Description
            final Product product = this.conn.getRemoteProduct().getProductByCode(ISIN, csvPosBean2.getIsin());
            if (null != product) {
                boPosAux.setSecurity(product);
            } else {
                throw new RemoteException("Product <" + csvPosBean2.getIsin()
                        + ">, specified in the file, doesn't exist");
            }

            // We fill all the fields for the BOPositionAdjustment object.
            boPosAux.setFlowType(SECURITY);
            boPosAux.setCurrencyCash(csvPosBean2.getQtyNominalSecCcy());
        }

        return boPosAux;
    }

    /**
     * To load the information for the Trade.
     *
     * @param productAdjustment Product to associate as the security.
     * @param csvPosBean2       CSVPositionsBean with the values for each line read.
     * @param tradeToBeCanceled String parameter that indicates if the Trade is to be canceled in the future or not (TRUE or FALSE).
     * @return New trade created.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Trade loadTradeData(final BOPositionAdjustment productAdjustment, final CSVPositionsBean csvPosBean2)
            throws Exception {
        final Trade localTrade = new Trade();

        if (csvPosBean2 != null) {
            // Portfolio
            final Book paramBook = this.conn.getRemoteReferenceData().getBook(csvPosBean2.getPortfolio());
            localTrade.setBook(paramBook);

            // Value Date
            final int day = Integer.parseInt(csvPosBean2.getValueDate().substring(0, 2));
            final int month = Integer.parseInt(csvPosBean2.getValueDate().substring(3, 5));
            final int year = Integer.parseInt(csvPosBean2.getValueDate().substring(6));
            localTrade.setSettleDate(JDate.valueOf(year, month, day));
            // We put the time to 00.00 hours (86340000 = (23hours * 60min *
            // 60sec * 1000milisec) + (59min * 60sec * 1000milsec)).
            localTrade.setTradeDate(JDate.valueOf(year, month, day).getJDatetime(TimeZone.getDefault()).add(-86340000));

            // We specify the PO as Counterparty, because we don't need this
            // information to import the movement.
            final LegalEntity legalEntity = this.conn.getRemoteReferenceData().getLegalEntity(
                    csvPosBean2.getProcessingOrg());
            localTrade.setCounterParty(legalEntity);
            localTrade.setRole(LegalEntity.PROCESSINGORG);

            // We retrieve the Bond from the BOPositionAdjustment (retrieving
            // the security).
            final Bond bond = (Bond) productAdjustment.getSecurity();

            // Quantity previously calculated.
            localTrade.setQuantity(Double.parseDouble(csvPosBean2.getQtyNominalSec()));

            // Global operations.
            productAdjustment.setPrincipal(bond.getFaceValue());
            productAdjustment.computeNominal(localTrade);

            // Nominal Security Currency
            localTrade.setSettleCurrency(csvPosBean2.getQtyNominalSecCcy());
            localTrade.setTradeCurrency(csvPosBean2.getQtyNominalSecCcy());

            // Position Type
            localTrade.setAdjustmentType(this.hashAdjusType.get(csvPosBean2.getPosType().toUpperCase()));

            // Reusable
            if ((csvPosBean2.getReusable() != "") && (null != csvPosBean2.getReusable())) {
                if (csvPosBean2.getReusable().equals(SUBSTITUTABLE)) {
                    localTrade.setSubstitutableFlag(true);
                } else {
                    localTrade.setSubstitutableFlag(false);
                }
            } else {
                localTrade.setSubstitutableFlag(false);
            }

            // Custodian
            localTrade.setInventoryAgent(csvPosBean2.getCustodian());

            // Account
            localTrade.setAccountNumber(csvPosBean2.getAccount());

            // BO System
            if (localTrade.getKeywords() == null) {
                localTrade.setKeywords(new Hashtable<String, String>());
            }
            if (null != csvPosBean2.getBoSystem()) {
                localTrade.getKeywords().put(BO_SYSTEM, csvPosBean2.getBoSystem());
            }

            // Position Status
            if (null != csvPosBean2.getPosStatus()) {
                localTrade.getKeywords().put(POSITION_STATUS,
                        this.hashPosType.get(csvPosBean2.getPosStatus().toUpperCase()));
            }

            // Keyword to identify if the trade is to be canceled in the future
            // or not.
            localTrade.addKeyword(TRADE_TO_BE_CANCELED, String.valueOf(false));
        }

        // We specify the product for the new trade created.
        localTrade.setProduct(productAdjustment);

        return localTrade;
    }
}
