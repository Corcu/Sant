/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.inventory;

import calypsox.util.SantReportingUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InventorySecurityPositionArray;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.*;

public class ImportInventarioTercerosWindow extends JFrame {

    private static final long serialVersionUID = 123L;

    private static final String WINDOW_TITLE = "Import Inventario Terceros";
    public static final String PO_INVENTARIO_TERCEROS = "POInventarioTerceros";
    public static final String FIELD_SEPERATOR = ";";
    public static final String LAST_INV_IMPORTED_DATE = "LAST_INV_IMPORTED_DATE";
    public static final String FULL_LOG_PATH = "/calypso_interfaces/inv_terceros/logs/";
    private static final String NONE1 = "NONE1";
    private static final String NONE = "NONE";
    private static final String PO = "ProcessingOrg";
    private final static String BUY_SELL = "Buy/Sell";
    private final static String SECURITY = "SECURITY";
    private final static String ACTUAL_POS_KWD = "ACTUAL_POS";
    private final static String FALSE = "false";

    // interface
    protected javax.swing.JLabel valDateLabel = new javax.swing.JLabel();
    protected JTextField valDateText = new JTextField();
    protected javax.swing.JLabel portfolioLabel = new javax.swing.JLabel();
    protected CalypsoComboBox portfolioCombo = new CalypsoComboBox();
    protected javax.swing.JLabel lastImportedLabel = new javax.swing.JLabel();
    protected javax.swing.JLabel lastImportedDateLabel = new javax.swing.JLabel();

    protected JLabel fileNameLabel = new JLabel();
    protected JTextField fileNameText = new JTextField();
    protected JButton fileChooserButton = new JButton();
    protected JFileChooser _fileChooser = null;
    protected JButton loadButton = new JButton();
    protected JButton closeButton = new JButton();

    // process
    protected Vector<String> thirdPartyBooks = null;
    protected Map<String, Product> productsMap = null;
    private final Hashtable<Integer, InventorySecurityPosition> invTerceroSecPositions = new Hashtable<Integer, InventorySecurityPosition>();

    public ImportInventarioTercerosWindow() {
        setTitle(WINDOW_TITLE);
        setSize(700, 495);
        setResizable(true);

        setLayout(null);

        this.valDateLabel.setText("Value Date");
        this.valDateLabel.setBounds(50, 50, 80, 24);
        add(this.valDateLabel);

        this.valDateText.setBounds(140, 50, 150, 24);
        add(this.valDateText);
        AppUtil.addDateListener(this.valDateText);

        // row 2
        this.portfolioLabel.setText("Portfoio");
        this.portfolioLabel.setBounds(50, 80, 80, 24);
        add(this.portfolioLabel);

        add(this.portfolioCombo);
        this.portfolioCombo.setBounds(140, 80, 150, 24);

        this.lastImportedLabel.setText("Last Imported on:");
        add(this.lastImportedLabel);
        this.lastImportedLabel.setBounds(350, 80, 100, 24);

        this.lastImportedDateLabel.setText("");
        this.lastImportedDateLabel.setBorder(new LineBorder(Color.black));
        add(this.lastImportedDateLabel);
        this.lastImportedDateLabel.setBounds(460, 80, 100, 24);

        // row 3
        this.fileNameLabel.setText("File Name:");
        this.fileNameLabel.setBounds(50, 110, 80, 24);
        add(this.fileNameLabel);
        this.fileNameText.setText("");
        this.fileNameText.setBounds(140, 110, 150, 24);
        add(this.fileNameText);

        this.fileChooserButton.setText("...");
        this.fileChooserButton.setBounds(290, 110, 30, 24);
        add(this.fileChooserButton);

        // row 4
        this.loadButton.setText("Load");
        this.loadButton.setBounds(140, 170, 80, 24);
        add(this.loadButton);

        this.closeButton.setText("Close");
        this.closeButton.setBounds(230, 170, 80, 24);
        add(this.closeButton);

        initDomainsAndListners();

    }

    private void initDomainsAndListners() {

        final SymAction lSymAction = new SymAction();
        this.loadButton.addActionListener(lSymAction);
        this.fileChooserButton.addActionListener(lSymAction);
        this.portfolioCombo.addItemListener(lSymAction);
        this.closeButton.addActionListener(lSymAction);

        try {
            AppUtil.set(this.portfolioCombo, getThirdPartyInventoryBooks());
        } catch (Exception e) {
            Log.error(this, e); //sonar
            AppUtil.displayError(this, e.getMessage());
            return;
        }
    }

    class SymAction implements ActionListener, ItemListener {
        @Override
        public void actionPerformed(final java.awt.event.ActionEvent event) {
            final Object object = event.getSource();
            final Cursor origCursor = getCursor();
            try {

                if (object == ImportInventarioTercerosWindow.this.loadButton) {
                    loadButton_actionPerformed();
                }

                if (object == ImportInventarioTercerosWindow.this.closeButton) {
                    closeButton_actionPerformed();
                }
                if (object == ImportInventarioTercerosWindow.this.fileChooserButton) {
                    fileChooserButton_ActionPerformed();
                }

            } finally {
                setCursor(origCursor);
            }
        }

        @Override
        public void itemStateChanged(ItemEvent event) {
            final Object object = event.getSource();
            final Cursor origCursor = getCursor();
            try {

                if (object == ImportInventarioTercerosWindow.this.portfolioCombo) {
                    String bookName = (String) ImportInventarioTercerosWindow.this.portfolioCombo.getSelectedItem();
                    Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
                    // Fix v4.2 (set value always, included when no value)
                    ImportInventarioTercerosWindow.this.lastImportedDateLabel.setText(book
                            .getAttribute(LAST_INV_IMPORTED_DATE));
                }

            } finally {
                setCursor(origCursor);
            }

        }
    }

    void closeButton_actionPerformed() {
        setVisible(false);
        dispose();
    }

    protected void fileChooserButton_ActionPerformed() {
        final String cwd = System.getProperty("user.dir");
        if (this._fileChooser == null) {
            this._fileChooser = AppUtil.getFileChooser("csv", "");
            try {
                final File f = new File(cwd);
                this._fileChooser.setCurrentDirectory(f);
            } catch (final Exception e) {
                Log.error(this, e); //sonar
            }
        }
        final int returnVal = this._fileChooser.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File dataFile = this._fileChooser.getSelectedFile();

        this.fileNameText.setText(dataFile.getAbsolutePath());
    }

    void loadButton_actionPerformed() {

        List<String> errorMsgs = new ArrayList<String>();

        try {

            // check input parameters
            if (!isValidInput(errorMsgs)) {
                AppUtil.displayError(this, errorMsgs);
                return;
            }

            // get input parameters
            String bookName = (String) this.portfolioCombo.getSelectedItem();
            Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
            JDate valDate = Util.stringToJDate(this.valDateText.getText());
            String fileName = this.fileNameText.getText();

            // get new positions from file
            List<InventarioTercerosBean> beans = readFile(fileName, errorMsgs);

            // get actual positions in a map
            feedInvTerceroSecPositionsMap(book, valDate);

            // update positions
            updatePositions(beans, book, valDate, errorMsgs);

            // set positions not recieved to zero
            setPositionsNotReceivedToZero(book, valDate, errorMsgs);

            // update last_update_attribute on book
            Book bookClone = (Book) book.clone();
            bookClone.setAttribute(LAST_INV_IMPORTED_DATE, valDate.toString());
            DSConnection.getDefault().getRemoteReferenceData().save(bookClone);

            // display result on screen
            if (errorMsgs.size() > 0) {
                AppUtil.displayError(this, "Import process with errors:\n" + errorMsgs);
            } else {
                AppUtil.displayMessage("Import process finished with no errors.", this);
            }

            // generate log file
            try {
                SantReportingUtil.getSantReportingService(DSConnection.getDefault()).generateLogFromDS(FULL_LOG_PATH,
                        errorMsgs);
            } catch (RemoteException e) {
                Log.error(this, e); //sonar
                AppUtil.displayError(this, e.getMessage());
            }

        } catch (Exception exc) {
            Log.error(this, exc); //sonar
            AppUtil.displayError(this, exc.getMessage());
            return;
        }
    }

    // --- FILE MANAGEMENT METHODS --- //

    /* Check load positions window parameters */
    private boolean isValidInput(List<String> errorMsgs) {
        boolean isValid = true;
        if (Util.isEmpty(this.fileNameText.getText())) {
            isValid = false;
            errorMsgs.add("Please select a File Name to import.");
        }
        if (Util.isEmpty((String) this.portfolioCombo.getSelectedItem())) {
            isValid = false;
            errorMsgs.add("Please select a Portfolio.");
        }
        if (Util.isEmpty(this.valDateText.getText())) {
            isValid = false;
            errorMsgs.add("Please input a ValDate.");
        }

        return isValid;
    }

    /* Read incoming file and save positions info as beans */
    @SuppressWarnings("resource")
    private List<InventarioTercerosBean> readFile(String fileName, List<String> msgs) throws Exception {

        List<InventarioTercerosBean> beans = new ArrayList<InventarioTercerosBean>();

        BufferedReader reader = null;

        reader = new BufferedReader(new FileReader(fileName));
        String line = null;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {

            lineNumber++;

            String[] fields = line.split(FIELD_SEPERATOR);
            if (fields.length != 5) {
                msgs.add("Line " + lineNumber + ": Invalid line. Number of fields should be 5\n");
                continue;
            }
            // check field QTY/NOM
            if (!fields[2].equals("QTY") && !fields[2].equals("NOM")) {
                msgs.add("Line " + lineNumber + ": Invalid line. QTY/NOM field invalid\n");
                continue;
            }

            InventarioTercerosBean bean = new InventarioTercerosBean(fields, lineNumber);
            beans.add(bean);

        }

        return beans;
    }

    // --- POSIITONS MANAGEMENT METHODS --- //

    /* Update book positions for valDate using info recieved in beans */
    private void updatePositions(List<InventarioTercerosBean> beans, Book book, JDate valDate, List<String> errorMsgs) {
        for (InventarioTercerosBean bean : beans) {
            Trade secXfer = buildSecurityTransfer(bean, book, valDate, errorMsgs);
            if (secXfer != null) {
                try {
                    long tradeId = DSConnection.getDefault().getRemoteTrade().save(secXfer);
                    Log.info(this, "Trade saved with id=" + tradeId);
                    // remove this updated position from map
                    this.invTerceroSecPositions.remove(secXfer.getProduct().getUnderlyingSecurityId());
                } catch (RemoteException e) {
                    Log.error(this, "Error saving trade", e);
                    errorMsgs.add("Line " + bean.getLine() + ": Cannot save the trade\n");
                }
            }
        }
    }

    /* Set positions not recieved in incoming file to zero */
    private void setPositionsNotReceivedToZero(Book book, JDate valDate, List<String> errorMsgs) throws RemoteException {
        for (int securityId : this.invTerceroSecPositions.keySet()) {
            Trade toZeroSecXfer = buildToZeroSecurityTransfer(securityId, book, valDate, errorMsgs);
            if (toZeroSecXfer != null) {
                try {
                    long tradeId = DSConnection.getDefault().getRemoteTrade().save(toZeroSecXfer);
                    Log.info(this, "Trade saved with id=" + tradeId);
                } catch (RemoteException e) {
                    Log.error(this, "Error saving toZero trade", e);
                }
            }
        }
    }

    /* Build security transfer trade in orde to update position */
    public Trade buildSecurityTransfer(InventarioTercerosBean bean, Book book, JDate positionDate,
                                       List<String> errorMsgs) {

        // get security
        Product security = getSecurity(bean);
        if (security == null) {
            errorMsgs.add("Line " + bean.getLine() + ": Security not found with SEC_CODE_TYPE = "
                    + bean.getSecCodeType() + " and SEC_CODE = " + bean.getSecCode() + " and CCY = " + bean.getCcy()
                    + "\n");
            return null;
        }

        // check maturity
        if (isBondProduct(security)) {
            if (isMaturedBond((Bond) security, positionDate)) {
                errorMsgs.add("Line " + bean.getLine() + ": Bond with SEC_CODE_TYPE = " + bean.getSecCodeType()
                        + " and SEC_CODE = " + bean.getSecCode() + " is matured\n");
                return null;
            }
        }

        // create simple transfer product
        SimpleTransfer product = createSimpleTransferProduct(book, security);

        // create trade
        Trade trade = createSimpleTransferTrade(bean, product, book, positionDate);

        return trade;
    }

    /* Build security transfer trade in orde to update position to zero */
    public Trade buildToZeroSecurityTransfer(int securityId, Book book, JDate positionDate, List<String> errorMsgs) {

        // get security
        Product security = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), securityId);

        if (security == null) {
            return null;
        }

        // create simple transfer product
        SimpleTransfer product = createSimpleTransferProduct(book, security);

        // create trade
        Trade trade = createToZeroSimpleTransferTrade(product, book, positionDate);

        return trade;
    }

    /* Create simpleTransfer product to attach on trade */
    private SimpleTransfer createSimpleTransferProduct(Book book, Product security) {

        // create simple transfer
        SimpleTransfer product = new SimpleTransfer();
        product.setFlowType(SECURITY);
        product.setOrdererRole(PO);
        product.setOrdererLeId(book.getLegalEntity().getId());
        product.setSecurity(security);
        product.setPrincipal(security.getPrincipal());

        return product;
    }

    /* Create trade, attach simpleTransfer product and set trade qty after calculate position delta */
    private Trade createSimpleTransferTrade(InventarioTercerosBean bean, SimpleTransfer product, Book book,
                                            JDate positionDate) {

        // create trade
        Trade trade = new Trade();
        setSimpleTransferTradeAtributes(trade, product, book, positionDate);

        // attach product to trade
        trade.setProduct(product);

        if (bean.isSecValueTypeQty()) {
            double positionDelta = getPositionDelta(product.getSecurity(), bean.getSecValue(), true);
            trade.setQuantity(positionDelta);
        } else {
            double positionDelta = getPositionDelta(product.getSecurity(), bean.getSecValue(), false);
            trade.setQuantity(positionDelta);
        }

        return trade;
    }

    /* Create to zero trade */
    private Trade createToZeroSimpleTransferTrade(SimpleTransfer product, Book book, JDate positionDate) {

        // create trade
        Trade trade = new Trade();
        setSimpleTransferTradeAtributes(trade, product, book, positionDate);

        // attach product to trade
        trade.setProduct(product);

        // get delta and set as trade qty
        double positionDelta = getPositionDelta(product.getSecurity(), 0.0, true);
        trade.setQuantity(positionDelta);

        return trade;
    }

    /* Set trade attributes */
    private void setSimpleTransferTradeAtributes(Trade trade, SimpleTransfer product, Book book, JDate positionDate) {
        trade.setAccountNumber(NONE1);
        trade.setInventoryAgent(NONE1);
        trade.addKeyword("BO_SYSTEM", "INV_TERCEROS");
        trade.setTraderName(NONE);
        trade.setSalesPerson(NONE);
        trade.setBook(book);
        trade.setTradeDate(new JDatetime(positionDate, TimeZone.getDefault()));
        trade.setSettleDate(positionDate);
        trade.setAction(Action.NEW);
        trade.setCounterParty(book.getLegalEntity());
        trade.setRole(PO);
        trade.setAdjustmentType(BUY_SELL);
        trade.setTradeCurrency(product.getSecurity().getCurrency());
        trade.setSettleCurrency(product.getSecurity().getCurrency());
        trade.setEnteredUser(DSConnection.getDefault().getUser());
        trade.addKeyword(ACTUAL_POS_KWD, FALSE);
    }

    /* Get position delta */
    private double getPositionDelta(Product security, double newPositionValue, boolean isQty) {
        double actualQty = 0.0;
        if (!isNewPosition(security)) {
            InventorySecurityPosition invSecPosition = this.invTerceroSecPositions.get(security.getId());
            actualQty = invSecPosition.getTotal();
        }
        if (isBondProduct(security)) {
            if (isQty) {
                return newPositionValue - actualQty;
            } else {
                Bond bond = (Bond) security;
                return (newPositionValue / bond.getFaceValue()) - actualQty;
            }
        } else {
            return newPositionValue - actualQty;
        }
    }

    /* Check if incoming position change is related to a new position or not */
    private boolean isNewPosition(Product security) {
        return this.invTerceroSecPositions.get(security.getId()) == null;
    }

    /* Check if product is a bond or not */
    private boolean isBondProduct(Product product) {
        return (product instanceof Bond);
    }

    /* Check bond maturity date */
    private boolean isMaturedBond(Bond bond, JDate date) {
        return (bond.isAfterMaturityDate(date) || (JDate.diff(bond.getMaturityDate(), date) == 0));
    }

    /* Get a map with all book retrieved positions */
    private void feedInvTerceroSecPositionsMap(Book book, JDate valueDate) {

        InventorySecurityPositionArray invSecPositionsArray = null;
        try {
            invSecPositionsArray = DSConnection
                    .getDefault()
                    .getRemoteBackOffice()
                    .getInventorySecurityPositions("product_desc",
                            getInvTerceroQueryWhereClause(book.getId(), valueDate), null);
        } catch (RemoteException e) {
            Log.error(this, "Error retrieving Inventario Tercero from book: " + book.getName() + "security positions",
                    e);
            return;
        }

        if (invSecPositionsArray != null) {
            for (int i = 0; i < invSecPositionsArray.size(); i++) {
                InventorySecurityPosition secPosition = invSecPositionsArray.get(i);
                if (secPosition != null) {
                    this.invTerceroSecPositions.put(secPosition.getSecurityId(), secPosition);
                }
            }
        }

    }

    /* Make map with all book retrieved positions query */
    private String getInvTerceroQueryWhereClause(int bookId, JDate valueDate) {

        StringBuilder whereClause = new StringBuilder();
        whereClause.append(" inv_secposition.internal_external = 'INTERNAL' ");
        whereClause.append(" AND inv_secposition.date_type = 'TRADE' ");
        whereClause.append(" AND inv_secposition.position_type = 'THEORETICAL'");
        whereClause.append(" AND inv_secposition.book_id = ");
        whereClause.append(bookId);
        whereClause.append(" AND inv_secposition.security_id = product_desc.product_id");
        whereClause.append(" AND");
        // whereClause.append(" product_desc.product_family = 'Bond'");
        whereClause.append(" (product_desc.product_family = 'Bond' OR product_desc.product_family = 'Equity')");
        whereClause.append(" AND inv_secposition.position_date = ");
        whereClause.append(" (");// BEGIN SELECT
        whereClause.append(" select MAX(temp.position_date) from inv_secposition temp ");
        whereClause.append(" WHERE inv_secposition.internal_external = temp.internal_external ");
        whereClause.append(" AND inv_secposition.date_type = temp.date_type ");
        whereClause.append(" AND inv_secposition.position_type = temp.position_type ");
        whereClause.append(" AND inv_secposition.security_id = temp.security_id ");
        whereClause.append(" AND inv_secposition.book_id = temp.book_id ");
        whereClause.append(" AND TRUNC(temp.position_date) <= ").append(
                com.calypso.tk.core.Util.date2SQLString(valueDate));
        whereClause.append(" )");// END SELECT

        return whereClause.toString();
    }

    /* Get security using products map */
    private Product getSecurity(InventarioTercerosBean bean) {
        try {
            return loadBondsMap().get(getKey(bean));
        } catch (RemoteException e) {
            Log.error(this, "Error creating key-product map", e);
        }
        return null;
    }

    /* Get product key */
    private String getKey(InventarioTercerosBean bean) {
        return getKey(bean.getSecCodeType(), bean.getSecCode(), bean.getCcy());
    }

    /* Create key */
    private String getKey(String secCodeType, String secCodeValue, String ccy) {
        return secCodeType + "-" + secCodeValue + "-" + ccy;
    }

    // --- HELPER METHODS --- //

    /* Get ThirdParty inventory books */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Vector getThirdPartyInventoryBooks() throws Exception {

        if (this.thirdPartyBooks != null) {
            return this.thirdPartyBooks;
        }

        Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), PO_INVENTARIO_TERCEROS);
        if (Util.isEmpty(domainValues)) {
            throw new Exception("No PO specified under domain " + PO_INVENTARIO_TERCEROS);
        }

        String poName = domainValues.get(0);
        LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), poName);

        if (po == null) {
            throw new Exception("PO doesn't exist with name " + poName);
        }

        Vector<Book> books = DSConnection.getDefault().getRemoteReferenceData()
                .getBooks(null, "legal_entity_id=" + po.getId(), null);

        this.thirdPartyBooks = new Vector<String>();
        this.thirdPartyBooks.add("");
        for (Book book : books) {
            this.thirdPartyBooks.add(book.getName());
        }

        return this.thirdPartyBooks;
    }

    /* Get all equities & bonds map using ref_type - ref_value - ccy as key */
    @SuppressWarnings({"unchecked"})
    private Map<String, Product> loadBondsMap() throws RemoteException {

        if (this.productsMap != null) {
            return this.productsMap;
        }

        this.productsMap = new HashMap<>();
        Vector<Product> allProducts = new Vector<>();
        String whereBond = " product_desc.product_family='Bond'";
        String whereEquity = " product_desc.product_family='Equity'";
        Vector<Product> bondProducts = DSConnection.getDefault().getRemoteProduct().getAllProducts(null, whereBond, null);
        Vector<Product> equityProducts = DSConnection.getDefault().getRemoteProduct().getAllProducts(null, whereEquity, null);
        allProducts.addAll(bondProducts);
        allProducts.addAll(equityProducts);

        for (Product product : allProducts) {

            String ccy = product.getCurrency();
            // ISIN
            String isin = product.getSecCode(BOND_SEC_CODE_ISIN);
            if (!Util.isEmpty(isin) && !Util.isEmpty(ccy)) {
                String key = getKey(BOND_SEC_CODE_ISIN, isin, ccy);
                if (this.productsMap.get(key) != null) {
                    Log.error(ImportInventarioTercerosWindow.class, "Duplicate ISIN for Bond ids ="
                            + this.productsMap.get(key).getId() + ", " + product.getId());
                } else {
                    this.productsMap.put(key, product);
                }
            }
            // CUSIP
            String cusip = product.getSecCode(BOND_SEC_CODE_CUSIP);
            if (!Util.isEmpty(cusip) && !Util.isEmpty(ccy)) {
                String key = getKey(BOND_SEC_CODE_CUSIP, cusip, ccy);
                if (this.productsMap.get(key) != null) {
                    Log.error(ImportInventarioTercerosWindow.class, "Duplicate CUSIP for Bond ids ="
                            + this.productsMap.get(key).getId() + ", " + product.getId());
                } else {
                    this.productsMap.put(key, product);
                }
            }
            // SEDOL
            String sedol = product.getSecCode(BOND_SEC_CODE_SEDOL);
            if (!Util.isEmpty(sedol) && !Util.isEmpty(ccy)) {
                String key = getKey(BOND_SEC_CODE_SEDOL, sedol, ccy);
                if (this.productsMap.get(key) != null) {
                    Log.error(ImportInventarioTercerosWindow.class, "Duplicate SEDOL for Bond ids ="
                            + this.productsMap.get(key).getId() + ", " + product.getId());
                } else {
                    this.productsMap.put(key, product);
                }
            }

        }
        return this.productsMap;
    }

}
