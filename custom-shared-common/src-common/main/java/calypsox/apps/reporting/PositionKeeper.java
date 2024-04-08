/*
 * Copyright (c) 2000 by Calypso Technology, Inc. 595 Market Street, Suite 1980,
 * San Francisco, CA 94105, U.S.A. All rights reserved. This software is the
 * confidential and proprietary information of Calypso Technology, Inc.
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the license
 * agreement you entered into with Calypso Technology.
 */
package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import com.calypso.apps.engine.LiquidatedPositionWindow;
import com.calypso.apps.marketdata.MarketDataJFrame;
import com.calypso.apps.marketdata.PricerConfigWindow;
import com.calypso.apps.marketdata.PricingEnvWindow;
import com.calypso.apps.marketdata.QuoteJFrame;
import com.calypso.apps.product.ProductUtil;
import com.calypso.apps.refdata.TradeFilterWindow;
import com.calypso.apps.reporting.PositionConfigTabs;
import com.calypso.apps.reporting.PositionKeeperUtil;
import com.calypso.apps.reporting.QuoteListWindow;
import com.calypso.apps.reporting.bo.PosAggregationFilter.AggregationKind;
import com.calypso.apps.reporting.bo.PosAggregationFilterPanel;
import com.calypso.apps.trading.ManualLiquidationJDialog;
import com.calypso.apps.trading.ShowTrade;
import com.calypso.apps.trading.TradeUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComponentListener;
import com.calypso.apps.util.CalypsoLayout;
import com.calypso.apps.util.GUIErrorReporter;
import com.calypso.apps.util.ShowTableWindow;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.apps.util.TableModelUtilAdapter;
import com.calypso.apps.util.TableUtil;
import com.calypso.apps.util.TimerDialog;
import com.calypso.engine.position.LiquidationUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.BookAttribute;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.RoundingMethod;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.ESStarter;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventAggPLPosition;
import com.calypso.tk.event.PSEventMarketDataChange;
import com.calypso.tk.event.PSEventPLPosition;
import com.calypso.tk.event.PSEventQuote;
import com.calypso.tk.event.PSSubscriber;
import com.calypso.tk.infosec.io.ResourceFactory;
import com.calypso.tk.marketdata.MarketDataItem;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricerConfig;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.mo.LiquidatedPosition;
import com.calypso.tk.mo.LiquidationConfig;
import com.calypso.tk.mo.LiquidationInfo;
import com.calypso.tk.mo.PLPosition;
import com.calypso.tk.mo.PLPosition.PLPositionKey;
import com.calypso.tk.mo.PLPositionUtil;
import com.calypso.tk.mo.PositionAggregation;
import com.calypso.tk.mo.PositionAggregationConfig;
import com.calypso.tk.mo.PositionSpec;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.mo.TradeFilterCriterion;
import com.calypso.tk.mo.TradeOpenQuantity;
import com.calypso.tk.mo.TradeOpenQuantity.LiquidableStatus;
import com.calypso.tk.mo.WACMarkCache;
import com.calypso.tk.mo.liquidation.loader.DefaultLiquidatedPositionLoader;
import com.calypso.tk.mo.liquidation.loader.LiquidatedPositionCriteriaBuilder;
import com.calypso.tk.mo.liquidation.loader.PLPositionAggregationParameter;
import com.calypso.tk.mo.liquidation.openquantity.loader.TradeOpenQuantityCriteriaBuilder;
import com.calypso.tk.mo.liquidation.plposition.loader.PLPositionCriteriaBuilder;
import com.calypso.tk.mo.liquidation.plposition.loader.PLPositionLoader;
import com.calypso.tk.mo.liquidation.rebuild.PLPositionRebuildContext;
import com.calypso.tk.mo.liquidation.rebuild.PLPositionRebuildOption;
import com.calypso.tk.mo.liquidation.rebuild.PLPositionRebuildResult;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.CommodityForward;
import com.calypso.tk.product.FX;
import com.calypso.tk.product.Future;
import com.calypso.tk.product.FutureOption;
import com.calypso.tk.product.PortfolioSwapPosition;
import com.calypso.tk.product.Security;
import com.calypso.tk.product.SpotDateCalculatorUtil;
import com.calypso.tk.product.commodities.CommodityUtil;
import com.calypso.tk.product.util.PortfolioSwapUtil;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.BookHierarchy;
import com.calypso.tk.refdata.BookHierarchyNode;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.User;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.report.liqposition.LiquidationAggregationFilterDescriptor;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.RemoteProduct;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.DisplayInBrowser;
import com.calypso.tk.util.ErrorReporter;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.LiquidatedPositionArray;
import com.calypso.tk.util.LogHelper;
import com.calypso.tk.util.LogHelper.Monitor;
import com.calypso.tk.util.OptionUtil;
import com.calypso.tk.util.PLPositionArray;
import com.calypso.tk.util.TimerRunnable;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TradeOpenQuantityArray;
import com.calypso.ui.component.dialog.DualListDialog;
import com.jidesoft.swing.JideSwingUtilities;
import com.calypso.apps.internationalization.rb.CalypsoResourceBundle;

public class PositionKeeper extends javax.swing.JFrame implements PSSubscriber, TimerRunnable {

	static final String ANY_BOOK_NAME = "_ANY_";
	static final int COL_ATTRIBUTE = 0;
	static final int COL_PRODUCT_ID = 1;
	static final int COL_LIQ_CONFIG = 63;
	
	
    private static final int INPUT_WIDTH = 125;

    private static final int GENERIC_HEIGHT = 24;

    private void jbInit() throws Exception {
        setTitle("Position Keeper Window");
        setJMenuBar(menuBar);
        getContentPane().setLayout(new BorderLayout(0, 0));
        setSize(new Dimension(895, 549));
        setVisible(false);
        mainPanel.setLayout(null);
        printButton.setBounds(new Rectangle(259, 15, 70, 24));
        printButton.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.printButton"));
        getContentPane().add(BorderLayout.CENTER, mainPanel);
        JLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JLabel1.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.JLabel1"));
        mainPanel.add(JLabel1);
        JLabel1.setBounds(5, 5, 70, 22);
        valDateText.setEnabled(false);
        mainPanel.add(valDateText);
        valDateText.setBounds(80, 5, 91, 22);
        timeText.setEnabled(false);
        mainPanel.add(timeText);
        timeText.setBounds(174, 5, 91, 22);
        mainPanel.add(portfolioChoice);
        portfolioChoice.setBounds(80, 30, 184, 22);
        JLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JLabel2.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.JLabel2"));
        mainPanel.add(JLabel2);
        JLabel2.setBounds(5, 30, 70, 22);
        JLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JLabel4.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.JLabel4"));
        mainPanel.add(JLabel4);
        JLabel4.setBounds(270, 30, 70, 22);
        mainPanel.add(pricingEnvChoice);
        pricingEnvChoice.setBounds(345, 30, 180, 22);
        buttonPanel.setLayout(null);
        mainPanel.add(buttonPanel);
        buttonPanel.setBounds(new Rectangle(475, 486, 408, 54));
        connectedCheck.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.connectedCheck"));
        connectedCheck.setActionCommand("Connected");
        connectedCheck.setEnabled(false);
        buttonPanel.add(connectedCheck);
        connectedCheck.setBounds(10, 26, 90, 22);
        hideButton.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.hideButton"));
        hideButton.setActionCommand("Hide");
        buttonPanel.add(hideButton);
        hideButton.setBounds(new Rectangle(334, 15, 70, 24));
        loadButton.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.loadButton"));
        loadButton.setActionCommand("Load");
        buttonPanel.add(loadButton);
        loadButton.setBounds(105, 15, 75, 24);
        realTimeChangeCheck.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.realTimeChangeCheck"));
        realTimeChangeCheck.setActionCommand("Real Time");
        buttonPanel.add(realTimeChangeCheck);
        realTimeChangeCheck.setBounds(10, 5, 90, 22);
        clearButton.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.clearButton"));
        clearButton.setActionCommand("clearButton");
        buttonPanel.add(clearButton);
        buttonPanel.add(printButton, null);
        clearButton.setBounds(new Rectangle(185, 15, 70, 24));
        mainPanel.add(JTabbedPane1);
        JTabbedPane1.setBounds(new Rectangle(216, 77, 667, 403));
        treeScrollPane.setOpaque(true);
        mainPanel.add(treeScrollPane);
        treeScrollPane.setBounds(16, 77, 200, 403);
        treeScrollPane.getViewport().add(orgTree);
        orgTree.setBounds(0, 0, 197, 417);
        orgLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        orgLabel.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.orgLabel"));
        mainPanel.add(orgLabel);
        orgLabel.setBounds(565, 5, 70, 22);
        mainPanel.add(orgChoice);
        orgChoice.setBounds(640, 5, 171, 22);
        aggregateLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        aggregateLabel.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.aggregateLabel"));
        mainPanel.add(aggregateLabel);
        aggregateLabel.setBounds(565, 30, 70, 22);
        mainPanel.add(aggregateChoice);
        aggregateChoice.setBounds(640, 30, 171, 22);
        mergeWithFeesCheck.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.mergeWithFeesCheck"));
        mergeWithFeesCheck.setActionCommand("IncludeFees");
        mainPanel.add(mergeWithFeesCheck);
        mergeWithFeesCheck.setBounds(831, 30, 140, 22);
        bySettleDateCheck.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.bySettleDateCheck"));
        bySettleDateCheck.setActionCommand("PositionBySettleDate");
        mainPanel.add(bySettleDateCheck);
        bySettleDateCheck.setBounds(831, 5, 160, 22);
        liquidationKeysLabel.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.liquidationKeysLabel"));
        liquidationKeysLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        liquidationKeysLabel.setBounds(991, 5, 93, 24);
        mainPanel.add(liquidationKeysLabel);
        liquidationKeysPanel.setSize(new Dimension(2 * INPUT_WIDTH, GENERIC_HEIGHT));
        liquidationKeysPanel.setPreferredSize(new Dimension(2 * INPUT_WIDTH, GENERIC_HEIGHT));
        liquidationKeysPanel.setMinimumSize(new Dimension(2 * INPUT_WIDTH, GENERIC_HEIGHT));
        liquidationKeysPanel.setBounds(1088, 5, 250, 24);
        mainPanel.add(liquidationKeysPanel);
        JLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JLabel3.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.JLabel3"));
        JLabel3.setToolTipText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.JLabel30"));
        mainPanel.add(JLabel3);
        JLabel3.setForeground(java.awt.Color.red);
        JLabel3.setBounds(270, 5, 70, 22);
        mainPanel.add(productDescText);
        productDescText.setBounds(345, 5, 180, 22);
        productDescText.setEditable(false);
        productDescButton.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.productDescButton"));
        productDescButton.setActionCommand("...");
        mainPanel.add(productDescButton);
        productDescButton.setBounds(526, 5, 25, 22);
        realTimePanel.setLayout(null);
        mainPanel.add(realTimePanel);
        realTimePanel.setBounds(0, 480, 515, 65);
        quoteCheckBox.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.quoteCheckBox"));
        quoteCheckBox.setActionCommand("Quote");
        realTimePanel.add(quoteCheckBox);
        quoteCheckBox.setBounds(90, 12, 60, 18);
        marketDataCheckBox.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.marketDataCheckBox"));
        marketDataCheckBox.setActionCommand("Market data");
        realTimePanel.add(marketDataCheckBox);
        marketDataCheckBox.setBounds(90, 36, 95, 18);
        JScrollPane1.setOpaque(true);
        realTimePanel.add(JScrollPane1);
        JScrollPane1.setBounds(190, 6, 266, 56);
        JScrollPane1.getViewport().add(realtimeText);
        realtimeText.setBounds(0, 0, 263, 53);
        JLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabel8.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.JLabel8"));
        realTimePanel.add(JLabel8);
        JLabel8.setBounds(5, 4, 80, 20);
        realTimePanel.add(frequencyText);
        frequencyText.setBounds(10, 24, 70, 22);
        JLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabel5.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.JLabel5"));
        realTimePanel.add(JLabel5);
        JLabel5.setBounds(10, 46, 70, 18);
        // $$ menuBar.move(3,523);
        // $$ JMenuBar1.move(0,555);
        zeroPositionLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        zeroPositionLabel.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.zeroPositionLabel"));
        mainPanel.add(zeroPositionLabel);
        zeroPositionLabel.setBounds(535, 55, 100, 22);
        mainPanel.add(zeroPositionChoice);
        zeroPositionChoice.setBounds(640, 55, 171, 22);
        zeroPositionToleranceLabel.setText(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.jbInit.zeroPositionToleranceLabel"));
        zeroPositionToleranceLabel.setBounds(831, 55, 70, 22);
        zeroPositionTolerance.setBounds(900, 55, 50, 22);
        mainPanel.add(zeroPositionTolerance);
        mainPanel.add(zeroPositionToleranceLabel);
        SymWindow aSymWindow = new SymWindow();
        this.addWindowListener(aSymWindow);
        SymAction lSymAction = new SymAction();
        realTimeChangeCheck.addActionListener(lSymAction);
        mergeWithFeesCheck.addActionListener(lSymAction);
        bySettleDateCheck.addActionListener(lSymAction);
        hideButton.addActionListener(lSymAction);
        portfolioChoice.addActionListener(lSymAction);
        printButton.addActionListener(lSymAction);
        valDateText.addActionListener(lSymAction);
        SymItem lSymItem = new SymItem();
        portfolioChoice.addItemListener(lSymItem);
        orgChoice.addItemListener(lSymItem);
        aggregateChoice.addItemListener(lSymItem);
        loadButton.addActionListener(lSymAction);
        SymMouse aSymMouse = new SymMouse();
        orgTree.addMouseListener(aSymMouse);
        pricingEnvChoice.addItemListener(lSymItem);
        pricingEnvChoice.addActionListener(lSymAction);
        JLabel3.addMouseListener(aSymMouse);
        productDescButton.addActionListener(lSymAction);
        frequencyText.addActionListener(lSymAction);
        SymFocus aSymFocus = new SymFocus();
        frequencyText.addFocusListener(aSymFocus);
        quoteCheckBox.addActionListener(lSymAction);
        marketDataCheckBox.addActionListener(lSymAction);
        clearButton.addActionListener(lSymAction);
        zeroPositionChoice.addItemListener(lSymItem);
        zeroPositionTolerance.addActionListener(lSymAction);
    }

    public PositionKeeper() {
        try {
            jbInit();
        } catch (Exception e) {
            Log.error(this, e);
        }
    }

    public PositionKeeper(String title) {
        this();
        setTitle(title);
    }

    class SymWindow extends java.awt.event.WindowAdapter {

        @Override
        public void windowClosing(java.awt.event.WindowEvent event) {
            Object object = event.getSource();
            if (object == PositionKeeper.this)
                PositionKeeper_WindowClosing(event);
        }
    }

    void PositionKeeper_WindowClosing(java.awt.event.WindowEvent event) {
        hideButton_actionPerformed(null);
    }

    // {{DECLARE_CONTROLS
    javax.swing.JPanel mainPanel = new javax.swing.JPanel();

    javax.swing.JLabel JLabel1 = new javax.swing.JLabel();

    javax.swing.JTextField valDateText = new javax.swing.JTextField();

    javax.swing.JTextField timeText = new javax.swing.JTextField();

    com.calypso.apps.util.CalypsoComboBox portfolioChoice = new com.calypso.apps.util.CalypsoComboBox();

    javax.swing.JLabel JLabel2 = new javax.swing.JLabel();

    javax.swing.JLabel JLabel4 = new javax.swing.JLabel();

    javax.swing.JComboBox pricingEnvChoice = new javax.swing.JComboBox();

    javax.swing.JPanel buttonPanel = new javax.swing.JPanel();

    javax.swing.JCheckBox connectedCheck = new javax.swing.JCheckBox();

    javax.swing.JButton hideButton = new javax.swing.JButton();

    javax.swing.JButton loadButton = new javax.swing.JButton();

    javax.swing.JCheckBox realTimeChangeCheck = new javax.swing.JCheckBox();

    javax.swing.JCheckBox mergeWithFeesCheck = new javax.swing.JCheckBox();

    javax.swing.JCheckBox bySettleDateCheck = new javax.swing.JCheckBox();

    JLabel liquidationKeysLabel = new javax.swing.JLabel();

    PosAggregationFilterPanel liquidationKeysPanel = PosAggregationFilterPanel.createAggregationFilterPanel(AggregationKind.LIQUIDATION, "");

    javax.swing.JButton clearButton = new javax.swing.JButton();

    javax.swing.JTabbedPane JTabbedPane1 = new javax.swing.JTabbedPane();

    javax.swing.JScrollPane treeScrollPane = new javax.swing.JScrollPane();

    javax.swing.JTree orgTree = new javax.swing.JTree();

    javax.swing.JLabel orgLabel = new javax.swing.JLabel();

    com.calypso.apps.util.CalypsoComboBox orgChoice = new com.calypso.apps.util.CalypsoComboBox();

    javax.swing.JLabel aggregateLabel = new javax.swing.JLabel();

    javax.swing.JComboBox aggregateChoice = new javax.swing.JComboBox();

    javax.swing.JLabel JLabel3 = new javax.swing.JLabel();

    javax.swing.JTextField productDescText = new javax.swing.JTextField();

    javax.swing.JButton productDescButton = new javax.swing.JButton();

    javax.swing.JPanel realTimePanel = new javax.swing.JPanel();

    javax.swing.JCheckBox quoteCheckBox = new javax.swing.JCheckBox();

    javax.swing.JCheckBox marketDataCheckBox = new javax.swing.JCheckBox();

    javax.swing.JScrollPane JScrollPane1 = new javax.swing.JScrollPane();

    javax.swing.JTextPane realtimeText = new javax.swing.JTextPane();

    javax.swing.JLabel JLabel8 = new javax.swing.JLabel();

    javax.swing.JTextField frequencyText = new javax.swing.JTextField();

    javax.swing.JLabel JLabel5 = new javax.swing.JLabel();

    javax.swing.JMenuBar menuBar = new javax.swing.JMenuBar();

    javax.swing.JButton printButton = new javax.swing.JButton();

    javax.swing.JComboBox zeroPositionChoice = new javax.swing.JComboBox();

    javax.swing.JLabel zeroPositionLabel = new javax.swing.JLabel();

    javax.swing.JLabel zeroPositionToleranceLabel = new javax.swing.JLabel();

    javax.swing.JTextField zeroPositionTolerance = new javax.swing.JTextField();

    // {{DECLARE_MENUS
    JSplitPane _splitter;

    class SymMouse extends java.awt.event.MouseAdapter {

        @Override
        public void mouseClicked(java.awt.event.MouseEvent event) {
            Object object = event.getSource();
            if (object == orgTree)
                orgTree_mouseClicked(event);
            else if (object == JLabel3)
                JLabel3_mouseClicked(event);
        }
    }

    class SymAction implements java.awt.event.ActionListener {

        public void actionPerformed(java.awt.event.ActionEvent event) {
            Object object = event.getSource();
            if (object == realTimeChangeCheck)
                realTimeChangeCheck_actionPerformed(event);
            else if (object == mergeWithFeesCheck)
                mergeWithFeesCheck_actionPerformed(event);
            else if (object == bySettleDateCheck)
                bySettleDateCheck_actionPerformed(event);
            else if (object == hideButton)
                hideButton_actionPerformed(event);
            else if (object == loadButton)
                loadButton_actionPerformed(event);
            else if (object == productDescButton)
                productDescButton_actionPerformed(event);
            else if (object == frequencyText)
                frequencyText_actionPerformed(event);
            if (object == quoteCheckBox)
                quoteCheckBox_actionPerformed(event);
            else if (object == marketDataCheckBox)
                marketDataCheckBox_actionPerformed(event);
            else if (object == clearButton)
                clearButton_actionPerformed(event);
            else if (object == printButton)
                printButton_actionPerformed(event);
            else if (object == valDateText)
                valDateText_actionPerformed(event);
            else if (object == zeroPositionTolerance)
                zeroPositionTolerance_actionPerformed(event);
            else if (object == pricingEnvChoice)
                pricingEnvChoice_actionPerformed(event);
        }
    }

    class SymItem implements java.awt.event.ItemListener {

        public void itemStateChanged(java.awt.event.ItemEvent event) {
            Object object = event.getSource();
            if (object == portfolioChoice)
                portfolioChoice_itemStateChanged(event);
            else if (object == orgChoice)
                orgChoice_itemStateChanged(event);
            else if (object == aggregateChoice)
                aggregateChoice_itemStateChanged(event);
            else if (object == pricingEnvChoice)
                pricingEnvChoice_itemStateChanged(event);
            else if (object == zeroPositionChoice)
                zeroPositionChoice_itemStateChanged(event);
        }
    }

    void updateDomains() {
        try {
            _pkConfigDialog = null;
            String sel = (String) portfolioChoice.getSelectedItem();
            Vector v = AccessUtil.getAllNames(User.TRADE_FILTER);
            AppUtil.set(portfolioChoice, v, true, null);
            if (sel != null)
                portfolioChoice.calypsoSetSelectedItem(sel);
            sel = (String) pricingEnvChoice.getSelectedItem();
            AppUtil.set(pricingEnvChoice, AccessUtil.getAllNames(User.PRICING_ENV), true, null);
            if (sel != null)
                pricingEnvChoice.setSelectedItem(sel);
            sel = (String) orgChoice.getSelectedItem();
            v = AccessUtil.getAllNames(User.BOOK_HIERARCHY);
            if (v == null)
                v = new Vector();
            v.insertElementAt("", 0);
            AppUtil.set(orgChoice, v, true, null);
            if (sel != null)
                orgChoice.calypsoSetSelectedItem(sel);
        } catch (Exception e) {
            Log.error(Log.GUI, e);
        }
    }

    void reconnect() {
        stop();
        start(_esPort, _esHost, _user);
        if (_portfolio != null)
            showTradeFilter(_portfolio.getName());
    }

    void realTimeChangeCheck_actionPerformed(java.awt.event.ActionEvent event) {
        realTimeChange();
    }

    void realTimeChange() {
        _realTime = realTimeChangeCheck.isSelected();
        if (_realTime) {
            handlePendings();
            start(_esPort, _esHost, _user);
            valDateText.setEnabled(false);
            timeText.setEnabled(false);
            getDatetime();
            startTimer();
            enableRealTime(true);
        } else {
            valDateText.setEnabled(true);
            timeText.setEnabled(true);
            stop();
            stopTimer();
            handlePendings();
            enableRealTime(false);
        }
        // BZ 39531: when clicking real-time on and off, re-enable load
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    void hideButton_actionPerformed(java.awt.event.ActionEvent event) {
        closeWS();
    }

    void closeWS() {
        if (!AppUtil.displayQuestion("Are you sure to exit ?", this))
            return;
        stop();
        stopTimer();
        setVisible(false);
        _env = null;
        _previousEnv = null;
        _aggregatedPLPositions = null;
        _books = null;
        _selectedBookHierarchyNode = null;
        _liquidationBooks = null;
        _portfolio = null;
        _user = null;
        _passwd = null;
        _hostName = null;
        _eventClassNames = null;
        _PLPositions = null;
        _orgStructure = null;
        _allPLPositions = null;
        _pkConfigDialog = null;
        _products = null;
        _pendingPositions.removeAllElements();
        dispose();
    }

    void printButton_actionPerformed(java.awt.event.ActionEvent event) {
        if (_products != null) {
            for (int i = 0; i < _products.size(); i++) {
                TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
                TableModelPLPosition model = ppl._plPositionModel;
                model.print(true);
            }
        }
    }

    void valDateText_actionPerformed(ActionEvent event) {
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    void zeroPositionTolerance_actionPerformed(ActionEvent event) {
        if (event == null)
            return;
        if (getPLPositions() == null)
            return;
        setTolerance();
        filter(getAllPLPositions());
        AppUtil.attachIconToButton(loadButton, true);
    }

    void portfolioChoice_itemStateChanged(java.awt.event.ItemEvent event) {
        // loadButton.setForeground(java.awt.Color.yellow);
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    void mergeWithFeesCheck_actionPerformed(java.awt.event.ActionEvent event) {
        realTimeChangeCheck.setSelected(false);
        realTimeChange();
        AppUtil.attachIconToButton(loadButton, true);
    }

    void bySettleDateCheck_actionPerformed(java.awt.event.ActionEvent event) {
        realTimeChangeCheck.setSelected(false);
        realTimeChange();
        AppUtil.attachIconToButton(loadButton, true);
    }

    void orgChoice_itemStateChanged(java.awt.event.ItemEvent event) {
        if (event != null && event.getStateChange() == java.awt.event.ItemEvent.DESELECTED)
            return;
        if (event == null)
            return;
        String previousName = null;
        if (_orgStructure != null)
            previousName = _orgStructure.getName();
        String name = (String) orgChoice.getSelectedItem();
        if (name == null || name.length() == 0) {
            _orgStructure = null;
            _selectedBookHierarchyNode = null;
        }
        _orgStructure = null;
        try {
            _orgStructure = DSConnection.getDefault().getRemoteReferenceData().getBookHierarchy(name);
        } catch (Exception e) {
            Log.error(this, e);
        }
        if (_orgStructure == null) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode("EMPTY");
            DefaultTreeModel treeModel = new DefaultTreeModel(node);
            orgTree.setModel(treeModel);
            _selectedBookHierarchyNode = null;
        } else
            _selectedBookHierarchyNode = _orgStructure.getRootNode();
        AppUtil.show(orgTree, _orgStructure);
        if (previousName == null && _orgStructure == null)
            return;
        if (previousName != null && _orgStructure != null)
            if (previousName.equals(_orgStructure.getName()))
                return;
        name = (String) portfolioChoice.getSelectedItem();
        if (getAllPLPositions() == null)
            showTradeFilter(name);
        else
            filter(getAllPLPositions());
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    void aggregateChoice_itemStateChanged(java.awt.event.ItemEvent event) {
        _attributeName = (String) aggregateChoice.getSelectedItem();
        if (event == null)
            return;
        if (event != null && event.getStateChange() == java.awt.event.ItemEvent.DESELECTED)
            return;
        if (getPLPositions() == null)
            return;
        filter(getAllPLPositions());
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    void zeroPositionChoice_itemStateChanged(java.awt.event.ItemEvent event) {
        _excludeZeroPositions = (int) zeroPositionChoice.getSelectedIndex();
        if (event == null)
            return;
        if (event != null && event.getStateChange() == java.awt.event.ItemEvent.DESELECTED)
            return;
        if (getPLPositions() == null)
            return;
        filter(getAllPLPositions());
        AppUtil.attachIconToButton(loadButton, true);
    }

    public void initDomains() {
        try {
            mainPanel.setSize(getSize());
            _splitter = AppUtil.createHorizontalSplitter(mainPanel, treeScrollPane, JTabbedPane1, POSITION_KEEPER_HIERARCHY);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            initRealTime();
            realTimeChangeCheck.setSelected(true);
            timeText.setText(Util.timeToString(new JDatetime()));
            JDate date = JDate.getNow();
            valDateText.setText(Util.dateToString(date));
            initMenus();
            try {
                Vector v = AccessUtil.getAllNames(User.TRADE_FILTER);
                AppUtil.set(portfolioChoice, v, true, null);
                AppUtil.set(pricingEnvChoice, AccessUtil.getAllNames(User.PRICING_ENV), true, null);
                v = AccessUtil.getAllNames(User.BOOK_HIERARCHY);
                if (v == null)
                    v = new Vector();
                v.insertElementAt("", 0);
                AppUtil.set(orgChoice, v, true, null);
                v = DSConnection.getDefault().getRemoteReferenceData().getBookAttributeNames();
                v.addElement(BookAttribute.LEGAL_ENTITY);
                v.addElement(BookAttribute.BOOK_NAME);
                v.addElement(BookAttribute.ACTIVITY);
                v.addElement(BookAttribute.LOCATION);
                v.addElement(BookAttribute.ACCOUNTING_BOOK);
                AppUtil.set(aggregateChoice, v, true, null);
                aggregateChoice.setSelectedItem(BookAttribute.BOOK_NAME);
                UserDefaults ud = DSConnection.getDefault().getUserDefaults();
                if (ud != null) {
                    if (ud.getPricingEnvName() != null)
                        pricingEnvChoice.setSelectedItem(ud.getPricingEnvName());
                    if (ud.getTradeFilterName() != null)
                        portfolioChoice.calypsoSetSelectedItem(ud.getTradeFilterName());
                    if (ud.getBookHierarchyName() != null)
                        orgChoice.calypsoSetSelectedItem(ud.getBookHierarchyName());
                }
            } catch (Exception e) {
                Log.error(Log.GUI, e);
            }
            realTimeChangeCheck.setEnabled(true);
            AppUtil.addDateListener(valDateText);
            AppUtil.addTimeListener(timeText);
            initTable();
            String name = (String) orgChoice.getSelectedItem();
            _orgStructure = null;
            if (name != null && name.length() > 0) {
                try {
                    _orgStructure = DSConnection.getDefault().getRemoteReferenceData().getBookHierarchy(name);
                } catch (Exception e) {
                    Log.error(this, e);
                }
            }
            if (_orgStructure != null)
                AppUtil.show(orgTree, _orgStructure);
            else {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode("EMPTY");
                DefaultTreeModel treeModel = new DefaultTreeModel(node);
                orgTree.setModel(treeModel);
            }
            // portfolioChoice_itemStateChanged(null);
            checkConnectionButton_Action(null);
            // set zero position choices
            _excludeZeroPositions = 0;
            Vector v = new Vector();
            v.insertElementAt("Include", 0);
            v.insertElementAt("Exclude 0 nominal / 0 P&L", 1);
            v.insertElementAt("Exclude 0 nominal", 2);
            AppUtil.set(zeroPositionChoice, v, false, null);
            setTolerance();
            // load user config
            loadTabs();
            startTimer();
        } catch (Exception e) {
            Log.error(this, e);
        }
        CalypsoComponentListener.setListener(this);
        AppUtil.addCustomListener(this);
    }

    void initMenus() {
        MenuAction maction = new MenuAction();
        JMenu menu;
        JMenuItem item;
        menu = new JMenu(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.menu"), true);
        menuBar.add(menu);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item")));
        item.setActionCommand("ConfigureTabs");
        item.addActionListener(maction);
        menu.addSeparator();
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item0")));
        item.setActionCommand("Load");
        item.addActionListener(maction);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item1")));
        item.setActionCommand("CheckQuotes");
        item.addActionListener(maction);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item2")));
        item.setActionCommand("Reconnect");
        item.addActionListener(maction);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item3")));
        item.setActionCommand("Update");
        item.addActionListener(maction);
        menu.addSeparator();
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item4")));
        item.setActionCommand("Clear");
        item.addActionListener(maction);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item5")));
        item.setActionCommand("RefreshTimeout");
        item.addActionListener(maction);
        menu.addSeparator();
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item6")));
        item.setActionCommand("Close");
        item.addActionListener(maction);
        menu = new JMenu(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.menu0"), true);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item7")));
        item.setActionCommand("PricingEnv");
        item.addActionListener(maction);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item8")));
        item.setActionCommand("Reload");
        item.addActionListener(maction);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item9")));
        item.setActionCommand("Display");
        item.addActionListener(maction);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item10")));
        item.setActionCommand("Check");
        item.addActionListener(maction);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item11")));
        item.setActionCommand("Quotes");
        item.addActionListener(maction);
        menuBar.add(menu);
        menu = new JMenu(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.menu1"), true);
        menu.add(item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.initMenus.item12")));
        item.setActionCommand("Help");
        item.addActionListener(maction);
        menuBar.add(menu);
    }

    void initTable() {
        // initialize layouts
        CalypsoLayout lay = new CalypsoLayout();
        lay.init(mainPanel, 0);
        lay.setAttach(_splitter, CalypsoLayout.RESIZE);
        lay.setAttach(buttonPanel, CalypsoLayout.WIDTH | CalypsoLayout.RIGHT | CalypsoLayout.BOTTOM | CalypsoLayout.HEIGHT);
        lay.setAttach(realTimePanel, CalypsoLayout.LEFT | CalypsoLayout.RIGHT | CalypsoLayout.BOTTOM | CalypsoLayout.HEIGHT);
        // realtime panel
        lay = new CalypsoLayout();
        lay.init(realTimePanel, 0);
        lay.setAttach(JScrollPane1, CalypsoLayout.RESIZE);
        quoteCheckBox.setBackground(realTimePanel.getBackground());
        marketDataCheckBox.setBackground(realTimePanel.getBackground());
        // bottom panel
        lay = new CalypsoLayout();
        lay.init(buttonPanel, 0);
        lay.setAttach(connectedCheck, CalypsoLayout.WIDTH | CalypsoLayout.BOTTOM | CalypsoLayout.HEIGHT);
        lay.setAttach(realTimeChangeCheck, CalypsoLayout.WIDTH | CalypsoLayout.BOTTOM | CalypsoLayout.HEIGHT);
        lay.setAttach(hideButton, CalypsoLayout.RIGHT | CalypsoLayout.WIDTH | CalypsoLayout.BOTTOM | CalypsoLayout.HEIGHT);
        lay.setAttach(loadButton, CalypsoLayout.RIGHT | CalypsoLayout.WIDTH | CalypsoLayout.BOTTOM | CalypsoLayout.HEIGHT);
        connectedCheck.setBackground(buttonPanel.getBackground());
        realTimeChangeCheck.setBackground(buttonPanel.getBackground());
    }

    void checkConnectionButton_Action(java.awt.event.ActionEvent event) {
        boolean v = false;
        try {
            DSConnection.getDefault().getRemoteAccess().checkConnection();
            v = true;
        } catch (Exception e) {
            v = false;
        }
        connectedCheck.setSelected(v);
    }

    public void newPLPosition(PLPosition position) {
        if (_portfolio == null)
            return;
        if (_product != null) {
            if (position.getProductId() != _product.getId())
                return;
        }
        if (!_portfolio.accept(position))
            return;
        /*
			 * * Refresh Open Positions
			 */
        TradeOpenQuantityArray openPositions = null;
        try {
            TradeOpenQuantityCriteriaBuilder criteria = TradeOpenQuantityCriteriaBuilder.create().status().ne(LiquidableStatus.Canceled).posKey(new PLPositionKey(position.getProductId(), position.getBookId(), position.getLiquidationConfig(), position.getPositionAggregationId()));
            openPositions = DSConnection.getDefault().getRemoteLiquidation().getTradeOpenQuantity(criteria);
        } catch (Exception e) {
            Log.error(this, e);
        }
        if (position.getLiquidationInfo() == null) {
            LiquidationInfo liqInfo = BOCache.getLiquidationInfo(DSConnection.getDefault(), position);
            if (liqInfo != null)
                position.setLiquidationInfo(liqInfo);
        }
        position.setOpenPositions(openPositions);
        position.setAsOfDate(getDatetime());
        position.setAveragePrice(position.computeAveragePrice());
        if (mergeWithFeesCheck.isSelected()) {
            if (position.isFeePosition()) {
                PLPosition parentPosition = new PLPosition();
                parentPosition.setBookId(position.getBookId());
                parentPosition.setPositionAggregationId(position.getPositionAggregationId());
                parentPosition.setLiquidationConfig(position.getLiquidationConfig());
                CA ca = (CA) position.getProduct();
                parentPosition.setProduct(ca.getUnderlying());
                int index = _allPLPositions.indexOf(parentPosition);
                if (index < 0) {
                    // _allPLPositions.add(position);
                    return;
                } else {
                    parentPosition = _allPLPositions.elementAt(index);
                    parentPosition.addFeePosition(position);
                    position = parentPosition;
                }
            }
        }
        // (re)load and set liquidation array on new position
        try {
            LiquidatedPositionArray liqArray = PLPositionUtil.loadLiqPos(position, getDatetime(), null, needsLiquidatedPositions(), bySettleDateCheck.isSelected());
            position.setLiqArray(liqArray);
        } catch (Exception e) {
            Log.error(this, e);
        }
        int index = _allPLPositions.indexOf(position);
        if (index < 0)
            _allPLPositions.add(position);
        else
            _allPLPositions.set(index, position);
        if (!accept(position)) {
            // remove rejected position if necessary
            removePLPosition(position, true, -1);
            return;
        }
        index = _PLPositions.indexOf(position);
        if (index < 0) {
            index = _PLPositions.size();
            _PLPositions.add(position);
        } else
            _PLPositions.set(index, position);
        Vector tabs = new Vector();
        PLPositionArray positions = addAggregatedPositions(position, tabs);
        JDatetime datetime = getDatetime();
        if (datetime == null)
            datetime = new JDatetime();
        JDate date = datetime.getJDate();
        PricingEnv env = getPricingEnv();
        addPLPosition(tabs, positions, -1, datetime, date, env, true);
    }

    /**
     * @param p the position to build by aggregation
     * @param positions
     * @param env
     * @param datetime
     * @param pdate
     * @param previous - whether we are dealing with today's position or the previous day position.
     * @return aggregated measures
     */
    private double[] calculatePLPositionValues(PLPosition p, PLPositionArray positions, PricingEnv env, JDatetime datetime, JDate pdate, boolean previous) {
        double unreal = 0.;
        double unrealClean = 0.;
        double pl = 0.;
        double amount = 0.;
        double globalPos = 0.;
        int size = positions.size();
        boolean stateOk = (!previous) || (_previousPLPositionsHash != null);
        if (stateOk) {
            for (int i = 0; i < size; i++) {
                PLPosition tmpPos = positions.elementAt(i);
                if (previous) {
                    tmpPos = (PLPosition) _previousPLPositionsHash.get(tmpPos);
                }
                if (tmpPos == null)
                    continue;
                p.setRealized(p.getRealized() + tmpPos.getRealized());
                p.setBookId(tmpPos.getBookId());
                p.setProduct(tmpPos.getProduct());
                globalPos += tmpPos.getAmount(datetime);
                if (tmpPos.getOpenPositions() != null) {
                    int tmpsize = tmpPos.getOpenPositions().size();
                    for (int j = 0; j < tmpsize; j++) {
                        p.getOpenPositions().add(tmpPos.getOpenPositions().elementAt(j));
                    }
                }
                if (tmpPos.getLiqArray() != null) {
                    int tmpsize = tmpPos.getLiqArray().size();
                    for (int j = 0; j < tmpsize; j++) {
                        p.getLiqArray().add(tmpPos.getLiqArray().elementAt(j));
                    }
                }
                if (tmpPos.getQuantity() != 0.) {
                    double avp = (tmpPos.getAveragePrice() * Math.abs(tmpPos.getQuantity()));
                    double oldavp = (p.getAveragePrice() * Math.abs(p.getQuantity()));
                    double totalQuantity = tmpPos.getQuantity() + p.getQuantity();
                    double avgPrice = Double.NaN;
                    if (totalQuantity != 0.) {
                        avgPrice = (avp + oldavp) / totalQuantity;
                    }
                    p.setAveragePrice(avgPrice);
                    p.setQuantity(p.getQuantity() + tmpPos.getQuantity());
                    if (p.getQuantity() > 0.)
                        p.setAveragePrice(Math.abs(p.getAveragePrice()));
                    else
                        p.setAveragePrice(-1. * Math.abs(p.getAveragePrice()));
                }
                if (tmpPos.getFeePositions() != null) {
                    int tmpSize = tmpPos.getFeePositions().size();
                    for (int j = 0; j < tmpSize; j++) {
                        p.addFeePosition(tmpPos.getFeePositions().elementAt(j));
                    }
                }
                p.setTotalLiqBuyQuantity(tmpPos.getTotalLiqBuyQuantity() + p.getTotalLiqBuyQuantity());
                p.setTotalLiqSellQuantity(tmpPos.getTotalLiqSellQuantity() + p.getTotalLiqSellQuantity());
                p.setTotalLiqBuyAmount(tmpPos.getTotalLiqBuyAmount() + p.getTotalLiqBuyAmount());
                p.setTotalLiqSellAmount(tmpPos.getTotalLiqSellAmount() + p.getTotalLiqSellAmount());
                p.setTotalLiqBuyAccrual(tmpPos.getTotalLiqBuyAccrual() + p.getTotalLiqBuyAccrual());
                p.setTotalLiqSellAccrual(tmpPos.getTotalLiqSellAccrual() + p.getTotalLiqSellAccrual());
                p.setTotalInterest(tmpPos.getTotalInterest() + p.getTotalInterest());
                p.setTotalPrincipal(tmpPos.getTotalPrincipal() + p.getTotalPrincipal());
                amount += tmpPos.computeAmount(datetime);
                double currentUnreal = 0;
                try {
                    if (previous) {
                        Double unReal = (Double) _previousPL.get(tmpPos);
                        currentUnreal = unReal.doubleValue();
                    } else {
                        Product pr = tmpPos.getProduct();
                        if (pr != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(tmpPos), CommodityForward.COMMODITY_FORWARD)) {
                            currentUnreal = CommodityUtil.computeUnrealize(tmpPos, env, datetime);
                        } else {
                            currentUnreal = tmpPos.getUnrealized(env, datetime);
                        }
                    }
                    if (p.getSecurity() instanceof Bond) {
                        unrealClean = tmpPos.getUnrealized(env, datetime, null, true);
                    }
                } catch (Exception e) {
                    Log.error(this, e);
                }
                if (p.getProduct() instanceof PortfolioSwapPosition) {
                    unreal = PortfolioSwapUtil.computeUnrealize(tmpPos, env, datetime);
                } else {
                    unreal += currentUnreal;
                }
                pl += (tmpPos.getRealized() + unreal);
            }
        }
        double principal = p.getProduct().getPrincipal(pdate);
        amount = amount * principal;
        double[] result = new double[6];
        result[0] = unreal;
        result[1] = pl;
        result[2] = amount;
        result[3] = globalPos;
        result[4] = principal;
        result[5] = unrealClean;
        return result;
    }

    // This should be optimized...
    // We should compute it ONCE !
    private boolean includePreviousPL() {
        return _includePreviousPL;
    }

    private void setIncludePreviousPL() {
        Vector cols = getColumnNamesUsed();
        _includePreviousPL = false;
        _useOptimizeLoad = true;
        for (int i = 0; i < cols.size(); i++) {
            String colName = (String) cols.elementAt(i);
            if (colName.indexOf(PositionKeeperUtil.REPOED_POSITION) != -1) {
                _useOptimizeLoad = false;
                break;
            }
        }
        for (int i = 0; i < cols.size(); i++) {
            String colName = (String) cols.elementAt(i);
            if (colName.indexOf("Dly ") != -1) {
                _includePreviousPL = true;
                break;
            }
        }
    }

    private static PLPosition createAggregatedPLPosition(PLPositionArray positions, JDatetime startDate) {
        PLPosition pos = positions.elementAt(0);
        PLPosition p = new PLPosition();
        p.setProduct(pos.getProduct());
        p.setBookId(pos.getBookId());
        p.setPositionAggregationId(pos.getPositionAggregationId());
        p.setPositionLongId(pos.getPositionLongId());
        p.setVersion(pos.getVersion());
        p.setLiquidationConfig(pos.getLiquidationConfig());
        // setting the toqs, etc require a valid key on the position, so we do this last
        // otherwise the key will be invalid
        p.setOpenPositions(new TradeOpenQuantityArray());
        p.setLiqArray(new LiquidatedPositionArray());
        if (LiquidationUtil.isTDWACBasedLiquidation(pos)) {
            // copy transient marks and measures
            List<PLMark> posMarks = pos.getPLMarks();
            if (posMarks != null) {
                List<PLMark> markList = new ArrayList<PLMark>(posMarks.size());
                for (PLMark mark : posMarks) {
                    if (mark == null)
                        continue;
                    try {
                        markList.add((PLMark) mark.clone());
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
                p.setPLMarks(markList);
            }
            Map<String, Double> posWacMeasures = pos.getWACMeasures();
            if (posWacMeasures != null) {
                Map<String, Double> wacMeasures = new HashMap<String, Double>();
                for (String key : posWacMeasures.keySet()) {
                    wacMeasures.put(new String(key), new Double(posWacMeasures.get(key)));
                }
                p.setWACMeasures(wacMeasures);
            }
        }
        p.setStartDate(startDate);
        return p;
    }

    private boolean addPLPosition(Vector tabs, PLPositionArray positions, int index, JDatetime datetime, JDate pdate, PricingEnv env, boolean refresh) {
        JDatetime prevDatetime = getPreviousDateTime(datetime);
        if (tabs.size() == 0)
            return false;
        // CAL-272064 - for some columns we will call the position pricer, and for that we need to ensure the position inception date is initialized.
        JDatetime minStartDate = null;
        for (int i = 0; i < positions.size(); i++) {
            PLPosition pos = positions.get(i);
            JDatetime startDate = pos.getStartDate();
            if (minStartDate == null || minStartDate.after(startDate)) {
                minStartDate = startDate;
            }
        }
        PLPosition pos = positions.elementAt(0);
        PLPosition p = createAggregatedPLPosition(positions, minStartDate);
        try {
            String ccy = p.getProduct().getCurrency();
            int rU = CurrencyUtil.getRoundingUnit(ccy);
            double[] newPLValues = null;
            LogHelper.getCurrentMonitor().resumeTask("calculate PLPositionValues");
            try {
                // double quantity = 0.;
                newPLValues = calculatePLPositionValues(p, positions, env, datetime, pdate, false);
            } finally {
                LogHelper.getCurrentMonitor().pauseTask("calculate PLPositionValues");
            }
            // check if this is a zero aggregated position
            if (isZeroPositionForRemoval(p, true)) {
                removePLPosition(pos, false, index);
                return false;
            }
            double unreal = newPLValues[0];
            double pl = newPLValues[1];
            double amount = newPLValues[2];
            double globalPos = newPLValues[3];
            double principal = newPLValues[4];
            double unrealClean = newPLValues[5];
            double unrealPrevious = .0;
            double plPrevious = .0;
            double unrealCleanPrevious = .0;
            // if calculate daily PL
            PLPosition previousPos = null;
            if (includePreviousPL()) {
                LogHelper.getCurrentMonitor().resumeTask("calculate Previous PLPositionValues");
                try {
                    previousPos = createAggregatedPLPosition(positions, minStartDate);
                    // to rebuild the previous day position, needed for daily fee type values
                    double[] previousPLValues = calculatePLPositionValues(previousPos, positions, env, datetime, pdate, true);
                    unrealPrevious = previousPLValues[0];
                    plPrevious = previousPLValues[1];
                    unrealCleanPrevious = previousPLValues[5];
                } catch (Exception e) {
                    Log.error(this, e);
                } finally {
                    LogHelper.getCurrentMonitor().pauseTask("calculate Previous PLPositionValues");
                }
            }
            // calculate pricer measures
            Pricer pricer;
            PricerMeasure[] a = new PricerMeasure[2];
            a[0] = new PricerMeasure(PricerMeasure.DIRTY_PRICE);
            a[1] = new PricerMeasure(PricerMeasure.YIELD);
            Trade trade = p.toTrade();
            JDatetime positionTime = datetime;
            JDate date = pdate;
            trade.setTradeDate(positionTime);
            JDate spotDate = SpotDateCalculatorUtil.getSpotDate(trade, positionTime);
            trade.setSettleDate(spotDate);
            double value = 0;
            double value2 = 0;
            pricer = env.getPricerConfig().getPricerInstance(trade.getProduct());
            try {
                if (pricer != null)
                    pricer.price(trade, positionTime, env, a);
                else
                    Log.warn(Log.WARN, "Pricer not found for : " + trade.getProduct().getType());
                value = a[0].getValue();
                value2 = a[1].getValue();
            } catch (Exception e) {
            }
            double mktPrice = value;
            double yield = value2;
            double repoedPosition = p.getRepoedPosition(positionTime);
            repoedPosition *= principal;
            String posAttr = getBook(pos.getBookId()).getAttribute(_attributeName);
            // Bug#: 11032
            int rUBaseCcy = CurrencyUtil.getRoundingUnit(env.getBaseCurrency());
            double plBaseCcy = convertCurrency(pl, ccy, env, pdate, p);
            int quoteUsage = env.getParameters().getQuoteUsage(trade.getProductType());
            int instanceToUse = env.getInstance();
            for (int k = 0; k < tabs.size(); k++) {
                TabPLPosition ppl = (TabPLPosition) tabs.elementAt(k);
                TableModelPLPosition model = ppl._plPositionModel;
                Vector cols = ppl._plUtil.getColumnNames();
                QuoteSet set = env.getQuoteSet();
                String setName = env.getQuoteSetName();
                String quoteName = trade.getProduct().getQuoteName();
                double quoteValue;
                QuoteValue qValue = null;
                if (quoteName != null) {
                    qValue = set.getQuote(new QuoteValue(setName, quoteName, pdate, null));
                    if (p.getProduct() != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(p), CommodityForward.COMMODITY_FORWARD)) {
                        try {
                            mktPrice = CommodityUtil.getCommodityForwardLocationAdjMktPrice(p, env, pdate, pdate);
                        } catch (PricerException pe) {
                            Log.error("PositionKeeper", pe);
                            mktPrice = 0.0;
                        }
                    }
                }
                // BZ: 19669
                if (qValue != null) {
                    quoteValue = (quoteUsage >= 0) ? qValue.getValue(quoteUsage) : qValue.getInstanceSide(instanceToUse, QuoteSet.MID);
                } else
                    quoteValue = 0;
                // BZ: 19669
                try {
                    LogHelper.getCurrentMonitor().resumeTask("PositionKeeperUtil.getValueAt");
                    int rowIndex = getRowIndex(index, pos, model);
                    if (rowIndex < 0)
                        return false;
                    model.putClientProperty(rowIndex, PL_POSITION_KEY, p);
                    model.putClientProperty(rowIndex, PL_POSITION_AGG_ATTRIBUTE, posAttr);
                    for (int i = 0; i < cols.size(); i++) model.setValueNoCheck(rowIndex, i, ppl._plUtil.getValueAt(index, i, p, previousPos, posAttr, rU, principal, amount, mktPrice, unreal, unrealPrevious, pl, plPrevious, yield, globalPos, repoedPosition, rUBaseCcy, plBaseCcy, quoteValue, unrealCleanPrevious, unrealClean, datetime, prevDatetime, env));
                } catch (Exception x) {
                    Log.error(Log.GUI, x);
                } finally {
                    LogHelper.getCurrentMonitor().pauseTask("PositionKeeperUtil.getValueAt");
                }
                if (refresh)
                    ppl.callFireTableRowsUpdated(index);
            }
        } catch (Exception e) {
            Log.error(Log.GUI, e);
        }
        // TableUtil.adjust(positionTable);
        return true;
    }

    // Bug#: 11032
    static double convertCurrency(double value, String ccy, PricingEnv env, JDate valDate, PLPosition plPos) {
        String baseCurrency = env.getBaseCurrency();
        if (ccy.equals(baseCurrency))
            return value;
        try {
            QuoteValue qv = env.getFXQuote(ccy, baseCurrency, valDate);
            if (qv != null) {
                if (plPos != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(plPos), CommodityForward.COMMODITY_FORWARD)) {
                    return (value * CommodityUtil.fxAdjustPrice(plPos, env, valDate, valDate, qv, baseCurrency));
                }
                return (value * qv.getMid(env.getParameters().getInstance()));
            } else {
                Log.error("PositionKeeper", ("Can't find FX Rate " + "for " + baseCurrency + "/" + ccy + " on " + valDate));
                return 0;
            }
        } catch (Exception ex) {
            Log.error("PositionKeeper", ex);
        }
        return 0;
    }

    int getRowIndex(int index, PLPosition pos, TableModelPLPosition model) {
        if (index == -1) {
            int pid;
            long posAggId;
            for (int i = 0; i < model.getRowCount(); i++) {
                PLPosition aggPos = (PLPosition) model.getClientProperty(i, PL_POSITION_KEY);
                String attributeName = (String) model.getClientProperty(i, PL_POSITION_AGG_ATTRIBUTE);
                pid = aggPos.getProductId();
                posAggId = aggPos.getPositionAggregationId();
                LiquidationConfig liqConfig = aggPos.getLiquidationConfig();
                Book bk = getBook(pos.getBookId());
                String posAttributeValue = bk.getAttribute(_attributeName);
                if (pid != pos.getProduct().getId()) {
                    continue;
                }
                if (attributeName != null) {
                    if ((posAttributeValue == null) || !attributeName.equals(posAttributeValue)) {
                        continue;
                    }
                } else {
                    if (posAttributeValue != null)
                        continue;
                }
                if (posAggId != pos.getPositionAggregationId()) {
                    continue;
                }
                index = i;
                break;
            }
            if (index == -1) {
                try {
                    index = model.addRow() - 1;
                } catch (Exception e) {
                    Log.error(this, e);
                }
            }
        } else {
            if (index == model.getRowCount()) {
                try {
                    index = model.addRow() - 1;
                } catch (Exception e) {
                    Log.error(this, e);
                }
            }
        }
        // System.out.println("===>>>> found: "+index);
        return index;
    }

    void showTradeFilterButton_Action(java.awt.event.ActionEvent event) {
        TradeFilterWindow w = new TradeFilterWindow();
        w.setVisible(true);
    }

    void showConfigButton_Action(java.awt.event.ActionEvent event) {
        PricerConfigWindow w = new PricerConfigWindow();
        w.setVisible(true);
    }

    public void newEvent(PSEvent event) {
        final PSEvent ev = event;
        Runnable r = new Runnable() {

            public void run() {
                handleEvent(ev);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    void handleEvent(final PSEvent event) {
        if (!_realTime)
            return;
        boolean handleNewQuote = false;
        if (event instanceof PSEventPLPosition && !isUsingLiqAggregation()) {
            synchronized (_pendingLock) {
                if (_refreshTimeout == 0)
                    newPLPosition(((PSEventPLPosition) event).getPLPosition());
                else
                    add2PendingPositions(((PSEventPLPosition) event).getPLPosition());
            }
        } else if (event instanceof PSEventAggPLPosition && isUsingLiqAggregation()) {
            synchronized (_pendingLock) {
                final PSEventAggPLPosition eventAggPos = (PSEventAggPLPosition) event;
                PositionAggregation posAgg = eventAggPos.getPLPosition().getPositionAggregation();
                if (posAgg.getConfigId() == getLiqAggId()) {
                    if (_refreshTimeout == 0)
                        newPLPosition(eventAggPos.getPLPosition());
                    else
                        add2PendingPositions(eventAggPos.getPLPosition());
                }
            }
        } else if (event instanceof PSEventQuote) {
            QuoteValue quote = ((PSEventQuote) event).getQuote();
            PricingEnv env = getPricingEnv();
            QuoteSet qs = env.getQuoteSet();
            QuoteValue[] q = ((PSEventQuote) event).getQuotes();
            if (q != null) {
                for (int i = 0; i < q.length; i++) handleNewQuote |= qs.handleNewQuote(q[i]);
            } else
                handleNewQuote = qs.handleNewQuote(quote);
        }
        handleRealTimeUpdateEvent(event, handleNewQuote);
    }

    public void onDisconnect() {
        connectedCheck.setSelected(false);
        Runnable rs = new Runnable() {

            public void run() {
                addRealTimeText("Disconnected from Event Server ...", 0);
            }
        };
        SwingUtilities.invokeLater(rs);
    }

    public void start(int esPort, String esHost, String user) {
        _user = user;
        _esPort = esPort;
        _esHost = esHost;
        _eventClassNames = new ArrayList<Class>();
        _eventClassNames.add(PSEventPLPosition.class);
        _eventClassNames.add(PSEventAggPLPosition.class);
        _eventClassNames.add(PSEventQuote.class);
        try {
            _ps = ESStarter.startConnection(this, _eventClassNames);
            _ps.start();
        } catch (Exception e) {
            Log.error(Log.GUI, e);
        }
        checkConnectionButton_Action(null);
    }

    public void stop() {
        if (_ps == null)
            return;
        try {
            _ps.stop();
            addRealTimeText("Unsubscribing to all update events...", 4);
            _ps = null;
        } catch (Exception e) {
        }
    }

    interface FRUpdate {

        public void update(TabPLPosition ppl);
    }

    /**
     * Allow the flexibility to call showTradeFilter given the name of the
     * trade filter or the trade filter itself.
     *
     * @param name a <code>String</code> identifying the trade filter.
     */
    protected void showTradeFilter(String name) {
        try {
            _portfolio = BOCache.getTradeFilter(DSConnection.getDefault(), name);
            if (_portfolio == null) {
                AppUtil.displayWarning("Please select or create a portfolio first", SwingUtilities.getWindowAncestor(PositionKeeper.this));
                return;
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        showTradeFilter(_portfolio);
    }

    /*
		 * ================================================================== *
		 * Function showTradeFilter * Purpose Load the Positions and PLPositions
		 * ===================================================================
		 */
    protected void showTradeFilter(TradeFilter tf) {
        final JDatetime datetime = getDatetime();
        if (datetime == null) {
            AppUtil.displayWarning("Please specifiy a valid datetime", this);
            return;
        }
        _allPLPositions = new PLPositionArray();
        _pendingPositions.removeAllElements();
        final TimerDialog[] dialog = new TimerDialog[1];
        /*
			 * * Initialize the Hierarchy Node
			 */
        if (_selectedBookHierarchyNode == null && _orgStructure != null)
            _selectedBookHierarchyNode = _orgStructure.getRootNode();
        _portfolio = null;
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            // _portfolio = BOCache.getTradeFilter(DSConnection.getDefault(), name2);
            _portfolio = tf;
            if (_portfolio == null) {
                AppUtil.displayWarning("Please select or create a portfolio first", SwingUtilities.getWindowAncestor(PositionKeeper.this));
                return;
            }
            /*
				 * if (!_portfolio.isBookSelected()) {
				 * AppUtil.displayWarning("There's no books selected in Trade
				 * Filter " + name2 + "." + "\n" + "Please modify your trade
				 * filter with the selection of books " + "\n" + "in which you
				 * want to see positions.",
				 * SwingUtilities.getWindowAncestor(PositionKeeper.this)); return; }
				 */
            _books = DSConnection.getDefault().getRemoteReferenceData().getTradeFilterBooks(_portfolio);
        } catch (Exception e) {
            Log.error(this, e);
        }
        class FilterRunnable implements Runnable, FRUpdate {

            public void run() {
                WACMarkCache.setUseCache(true);
                try {
                    _portfolio = getUpdatedTradeFilter();
                    if (LiquidationUtil.isSnapshotLiquidationConfig(_portfolio)) {
                        initializeFromSnapshot(datetime);
                    } else {
                        initializeFromPLPositionUtil(datetime);
                    }
                } catch (Exception e) {
                    AppUtil.displayErrorDetails(PositionKeeper.this, "Error loading Position", "Could not load Position", e, "PositionKeeper");
                    Log.error(Log.GUI, e);
                } finally {
                    restoreTradeFilter();
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                WACMarkCache.setUseCache(true);
                filter(getAllPLPositions(), this);
                Runnable doDone = new Runnable() {

                    public void run() {
                        int selected = JTabbedPane1.getSelectedIndex();
                        if (selected != -1 && selected < JTabbedPane1.getTabCount()) {
                            JTabbedPane1.setSelectedIndex(selected);
                        }
                        PricingEnv env = getPricingEnv();
                        if (env != null) {
                            initRealTimeUpdateTrades();
                        }
                    }
                };
                SwingUtilities.invokeLater(doDone);
            }

            public void update(final TabPLPosition ppl) {
                Runnable doProcess = new Runnable() {

                    public void run() {
                        ppl._plPositionModel.notifyOnNewValue(true);
                        ppl.recreateScrollPane();
                    }
                };
                SwingUtilities.invokeLater(doProcess);
            }
        }
        TimerDialog.startNotModal(new FilterRunnable(), "Loading from " + _portfolio.getName(), JideSwingUtilities.getFrame(this), dialog);
    // @#$
    }

    protected void initializeFromPLPositionUtil(JDatetime datetime) throws Exception {
        boolean state = realTimeChangeCheck.isSelected();
        boolean mergeWithFee = mergeWithFeesCheck.isSelected();
        boolean bySettleDate = bySettleDateCheck.isSelected();
        /*
			 * * Rebuild PLPositions in the Past
			 */
        Monitor monitor = LogHelper.startWithCategory("PositionKeeper", "Loading Products");
        ensureProductsLoaded(_portfolio);
        monitor.done();
        _portfolio.setValDate(datetime);
        PLPositionCriteriaBuilder criteria = PLPositionCriteriaBuilder.create(_portfolio);
        monitor = LogHelper.startWithCategory("PositionKeeper", "Loading PLPositions");
        _allPLPositions = DSConnection.getDefault().getRemoteLiquidation().getPLPositionArray(criteria);
        monitor.done(_allPLPositions.size() + " positions loaded.");
        if (state) {
            monitor = LogHelper.startWithCategory("PositionKeeper", "Loading TradeOpenQuantity");
            loadAndSetOpenPositions(_allPLPositions, _portfolio);
            monitor.done();
            WACMarkCache.loadMarksAndEnableCache(_allPLPositions, datetime);
            if (mergeWithFee) {
                PLPositionUtil.mergePositionsWithFees(_allPLPositions);
            }
            for (int i = 0; i < _allPLPositions.size(); i++) {
                PLPosition plpos = _allPLPositions.get(i);
                if (plpos.getLiquidationInfo() == null) {
                    LiquidationInfo liqInfo = BOCache.getLiquidationInfo(DSConnection.getDefault(), plpos);
                    if (liqInfo != null)
                        plpos.setLiquidationInfo(liqInfo);
                }
                plpos.setAsOfDate(datetime);
                plpos.setAveragePrice(plpos.computeAveragePrice());
            }
            // load and set liquidated positions as they're needed to calculate some columns (accruedRealized, cleanRealized, etc.)
            if (needsLiquidatedPositions()) {
                monitor = LogHelper.startWithCategory("PositionKeeper", "Loading LiquidatedPosition");
                loadAllLiquidatedPositions(_allPLPositions, datetime, _portfolio);
                monitor.done();
            }
            // TDWAC always requires a rebuild (if any prem disc measures needed)
            for (int i = 0; i < _allPLPositions.size(); i++) {
                PLPosition plpos = _allPLPositions.get(i);
                if (LiquidationUtil.isTDWACBasedLiquidation(plpos)) {
                    // rebuild to trigger measure derivation
                    PLPositionArray positionArray = new PLPositionArray();
                    positionArray.add(plpos);
						positionArray.setFees(false);
                    PLPositionUtil.rebuildPLPositions(positionArray, datetime, false, null, false);
                }
            }
            if (includePreviousPL()) {
                monitor = LogHelper.startWithCategory("PositionKeeper", "Rebuilding Previous");
                PLPositionArray prevPL = null;
                try {
                    prevPL = (PLPositionArray) _allPLPositions.clone();
						prevPL.setFees(false);
                } catch (Exception e) {
                    Log.error(this, e);
                }
					PLPositionUtil.rebuildPLPositions(prevPL,getPreviousDateTime(datetime), bySettleDate, null, false);
                if (mergeWithFee) {
                    PLPositionUtil.mergePositionsWithFees(prevPL);
                }
                _previousPLPositions = prevPL;
                _previousPLPositionsHash = Util.toHashtable(prevPL.toVector());
                monitor.done();
                monitor = LogHelper.startWithCategory("PositionKeeper", "Computing Previous Values");
                computePreviousValues(_previousPLPositions, _previousPL, getPreviousEnv(), getPreviousDateTime(datetime));
                monitor.done();
            }
        } else {
            // Not Real time, we need to call the PLPositionRebuild, which will take care of loading the PLPosition and LiquidatedPosition
            monitor = LogHelper.startWithCategory("PositionKeeper", "Rebuild PLPosition");
            Vector v = new Vector();
            JDatetime dtprv = getPreviousDateTime(datetime);
            if (includePreviousPL())
                v.addElement(dtprv);
            v.addElement(datetime);
				_allPLPositions.setFees(false);
            Map<JDatetime, PLPositionArray> hash = PLPositionUtil.buildPositionByDates(_allPLPositions, v, (_useOptimizeLoad && !mergeWithFee), false, bySettleDate);
            monitor.done();
            _allPLPositions = hash.get(datetime);
            if (_allPLPositions != null) {
                if (mergeWithFee) {
                    PLPositionUtil.mergePositionsWithFees(_allPLPositions);
                }
                if (includePreviousPL()) {
                    _previousPLPositions = hash.get(dtprv);
                    if (mergeWithFee) {
                        PLPositionUtil.mergePositionsWithFees(_previousPLPositions);
                    }
                    _previousPLPositionsHash = Util.toHashtable(_previousPLPositions.toVector());
                    monitor = LogHelper.startWithCategory("PositionKeeper", "Computing Previous Values");
                    computePreviousValues(_previousPLPositions, _previousPL, getPreviousEnv(), dtprv);
                    monitor.done();
                }
            }
        }
    }

    protected void initializeFromSnapshot(JDatetime datetime) throws Exception {
        boolean realTime = realTimeChangeCheck.isSelected();
        boolean mergeWithFee = mergeWithFeesCheck.isSelected();
        boolean bySettleDate = bySettleDateCheck.isSelected();
        LiquidationConfig liqConfig = LiquidationUtil.getLiquidationConfig(_portfolio);
        OptionUtil<PLPositionRebuildOption> rebuildOptions = OptionUtil.get(PLPositionRebuildOption.class).addIf(PLPositionRebuildOption.SettleDatePosition, bySettleDate).addIf(PLPositionRebuildOption.LoadOnly, realTime).addIf(PLPositionRebuildOption.InMemoryLiquidation, liqConfig.isSimulationSupport()).addIf(PLPositionRebuildOption.MergeFees, mergeWithFee).addIf(PLPositionRebuildOption.LoadAllLiquidatedPositions, needsLiquidatedPositions()).addIf(PLPositionRebuildOption.GetRepoedPositions, needsRepoedPositions()).addIf(PLPositionRebuildOption.AsOf, liqConfig.isUsingSnapshots() && !realTime);
        JDatetime previousPositionDate = getPreviousDateTime(datetime);
        PLPositionRebuildContext positionContext = PLPositionRebuildContext.fromTradeFilter(_portfolio, datetime, rebuildOptions.build());
        PLPositionRebuildContext prevPositionContext = null;
        if (includePreviousPL()) {
            OptionUtil<PLPositionRebuildOption> prevRebuildOptions = OptionUtil.get(PLPositionRebuildOption.class).addIf(PLPositionRebuildOption.SettleDatePosition, bySettleDate).addIf(PLPositionRebuildOption.MergeFees, mergeWithFee).addIf(PLPositionRebuildOption.LoadAllLiquidatedPositions, needsLiquidatedPositions()).addIf(PLPositionRebuildOption.GetRepoedPositions, needsRepoedPositions()).addIf(PLPositionRebuildOption.AsOf, liqConfig.isUsingSnapshots());
            // Need to create another Context for the rebuild.
            prevPositionContext = PLPositionRebuildContext.fromTradeFilter(_portfolio, previousPositionDate, prevRebuildOptions.build());
        }
        /*
			 * * Rebuild PLPositions in the Past
			 */
        Monitor monitor = LogHelper.startWithCategory("PositionKeeper", "Loading Products");
        ensureProductsLoaded(_portfolio);
        monitor.done();
        // Request rebuild position based on the positionContext.
        monitor = LogHelper.startWithCategory("PositionKeeper", "Loading Snapshot Positions");
        PLPositionRebuildResult result = PLPositionLoader.loadAndRebuildPLPosition(positionContext);
        monitor.done();
        if (_allPLPositions == null)
            _allPLPositions = new PLPositionArray();
        if (includePreviousPL()) {
            monitor = LogHelper.startWithCategory("PositionKeeper", "Loading Previous Snapshot Positions");
            result.add(previousPositionDate, PLPositionLoader.loadAndRebuildPLPosition(prevPositionContext));
            monitor.done();
        }
        if (realTime) {
            _allPLPositions = result.getPLPositionsOn(datetime);
            if (includePreviousPL()) {
                _previousPLPositions = result.getPLPositionsOn(previousPositionDate);
                _previousPLPositionsHash = Util.toHashtable(_previousPLPositions.toVector());
                computePreviousValues(_previousPLPositions, _previousPL, getPreviousEnv(), previousPositionDate);
            }
        } else {
            _allPLPositions = result.getPLPositionsOn(datetime);
            if (_allPLPositions != null) {
                if (includePreviousPL()) {
                    _previousPLPositions = result.getPLPositionsOn(previousPositionDate);
                    _previousPLPositionsHash = Util.toHashtable(_previousPLPositions.toVector());
                    computePreviousValues(_previousPLPositions, _previousPL, getPreviousEnv(), previousPositionDate);
                }
            }
        }
    }

    protected boolean needsLiquidatedPositions() {
        // load and set liquidated positions as they're needed to calculate some columns (accruedRealized, cleanRealized, etc.)
        return true;
    }

    protected boolean needsRepoedPositions() {
        return !_useOptimizeLoad;
    }

    private void restoreTradeFilter() {
        String name = (String) portfolioChoice.getSelectedItem();
        _portfolio = BOCache.getTradeFilter(DSConnection.getDefault(), name);
    }

    /**
     * @return a Modified Trade filter which includes Fees information and PositionSpec according to the AggDesc selected.
     */
    private TradeFilter getUpdatedTradeFilter() {
        ErrorReporter reporter = createErrorReporter();
        LiquidationAggregationFilterDescriptor liqAggDesc = (LiquidationAggregationFilterDescriptor) liquidationKeysPanel.getFilterDescriptor();
        TradeFilter tf = PLPositionAggregationParameter.copyForPLPositionLoading(_portfolio, liqAggDesc, reporter);
        if (_product != null) {
            tf.addCriterion(TradeFilter.PRODUCT_ID, Integer.toString(_product.getId()));
        }
        if (mergeWithFeesCheck.isSelected()) {
            LiquidationUtil.addPLFeeProductCriteria(tf, _product, reporter);
        }
        return tf;
    }

    private ErrorReporter createErrorReporter() {
        return new GUIErrorReporter(PositionKeeper.this, "PositionKeeper");
    }

    protected void filter(PLPositionArray positions) {
        filter(positions, null);
    }

    protected void filter(PLPositionArray positions, FRUpdate fswUpdate) {
        if (positions == null) {
            AppUtil.displayWarning("No Positions", this);
            return;
        }
        _PLPositions = new PLPositionArray();
        int size = positions.size();
        for (int i = 0; i < size; i++) {
            PLPosition position = positions.elementAt(i);
            if (accept(position))
                _PLPositions.add(position);
        }
        Monitor monitor = LogHelper.startWithCategory("PositionKeeper", "Displaying Positions");
        showPLPositions(getPLPositions(), fswUpdate);
        monitor.done();
    }

    PLPositionArray getPLPositions() {
        return _PLPositions;
    }

    PLPositionArray getAllPLPositions() {
        return _allPLPositions;
    }

    void timeText_EnterHit(java.awt.event.ActionEvent event) {
        timeText.setText(Util.timeToString(new JDatetime()));
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    JDatetime getDatetime() {
        if (realTimeChangeCheck.isSelected()) {
            timeText.setText(Util.timeToString(new JDatetime()));
            valDateText.setText(Util.dateToString(new JDatetime()));
            return new JDatetime();
        } else {
            try {
                JDatetime datetime = Util.stringToJDatetime(valDateText.getText(), timeText.getText());
                return datetime;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    protected JDatetime getPreviousDateTime(JDatetime datetime) {
        Vector holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
        if (holidays == null) {
            holidays = new Vector();
        }
        TimeZone envTZ = _env.getTimeZone();
        JDate datePrevious = Holiday.getCurrent().addBusinessDays(datetime.getJDate(envTZ), holidays, -1, false);
        return new JDatetime(datePrevious, 23, 59, 0, 0, envTZ);
    }

    void setMarketConfig() {
        String name = (String) pricingEnvChoice.getSelectedItem();
        JDatetime datetime = getDatetime();
        if (datetime == null)
            return;
        JDate today = JDate.valueOf(datetime);
        try {
            _env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(name, datetime);
            JDatetime datetimePrevious = getPreviousDateTime(datetime);
            _previousEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(name, datetimePrevious);
        } catch (Exception e) {
            Log.error(Log.GUI, e);
        }
    }

    PricingEnv getPricingEnv() {
        String name = (String) pricingEnvChoice.getSelectedItem();
        if (name == null)
            return null;
        JDatetime datetime = getDatetime();
        if (datetime == null)
            return null;
        JDate today = JDate.valueOf(datetime);
        if ((_env == null || _previousEnv == null) || !name.equals(_env.getName()) || !today.equals(_env.getDate()))
            setMarketConfig();
        return _env;
    }

    PricingEnv getPreviousEnv() {
        return _previousEnv;
    }

    public void showPLPositions(PLPositionArray v) {
        showPLPositions(v, null);
    }

    public void showPLPositions(PLPositionArray v, FRUpdate fswUpdate) {
        if (v == null)
            v = new PLPositionArray();
        buildAggregatedPLPositions(v);
        PricingEnv env = getPricingEnv();
        QuoteSet qs = null;
        if (env != null)
            qs = env.getQuoteSet();
        boolean saveQuoteSetCacheB = false;
        if (qs != null)
            saveQuoteSetCacheB = qs.getCacheNotFound();
        JDatetime datetime = getDatetime();
        JDate date = datetime.getJDate();
        try {
            if (qs != null)
                qs.setCacheNotFound(true);
            int size = _products.size();
            for (int i = 0; i < size; i++) {
                TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
                TableModelPLPosition model = ppl._plPositionModel;
                Hashtable aggregatedPLPositions = ppl._plPositions;
                model.reinitRows(aggregatedPLPositions.size());
                try {
                    model.notifyOnNewValue(false);
                    Enumeration e = null;
                    e = aggregatedPLPositions.elements();
                    int count = 0;
                    Vector tabs = new Vector();
                    tabs.add(ppl);
                    while (e.hasMoreElements()) {
                        PLPositionArray aggPositions = (PLPositionArray) e.nextElement();
                        if (addPLPosition(tabs, aggPositions, count, datetime, date, env, false) == true) {
                            count++;
                        }
                    }
                    if (null != fswUpdate) {
                        fswUpdate.update(ppl);
                    } else {
                        ppl.recreateScrollPane();
                    }
                } finally {
                    if (null == fswUpdate) {
                        model.notifyOnNewValue(true);
                    }
                }
                ;
            // TableUtil.fastAdjust(model);
            }
            if (null == fswUpdate) {
                int selected = JTabbedPane1.getSelectedIndex();
                if (selected != -1 && selected < JTabbedPane1.getTabCount()) {
                    JTabbedPane1.setSelectedIndex(selected);
                }
                if (env != null) {
                    initRealTimeUpdateTrades();
                }
            }
        } finally {
            if (qs != null)
                qs.setCacheNotFound(saveQuoteSetCacheB);
        }
    }

    void setModelMenu(TableModelUtil toModel, TableModelPLPosition model) {
        JPopupMenu menu = null;
        JMenuItem item;
        PositionMenuAction maction = new PositionMenuAction(toModel.getTable(), model);
        menu = new JPopupMenu();
        menu.add("Position Menu");
        menu.addSeparator();
        item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.setModelMenu.item"));
        item.setActionCommand("showLiquidatedPositions");
        item.addActionListener(maction);
        menu.add(item);
        item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.setModelMenu.item0"));
        item.setActionCommand("showBySettleDate");
        item.addActionListener(maction);
        menu.add(item);
        item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.setModelMenu.item1"));
        item.setActionCommand("showDetailPosition");
        item.addActionListener(maction);
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.setModelMenu.item2"));
        item.setActionCommand("showLiquidationConfig");
        item.addActionListener(maction);
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.setModelMenu.item3"));
        item.setActionCommand("manualLiquidation");
        item.addActionListener(maction);
        menu.add(item);
        menu.addSeparator();
        // Use Configure Tabs Bug#: 12491
        // item = new JMenuItem("Configure Columns");
        // item.setActionCommand("configureColumns");
        // item.addActionListener(maction);
        // menu.add(item);
        // menu.addSeparator();
        item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.setModelMenu.item4"));
        item.setActionCommand("openTrade");
        item.addActionListener(maction);
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem(CalypsoResourceBundle.getResourceKey("PositionKeeper", "com.calypso.apps.reporting.PositionKeeper.setModelMenu.item5"));
        item.setActionCommand("showFeePositions");
        item.addActionListener(maction);
        menu.add(item);
        model.setMenu(menu);
    }

    class TableModelPLPosition extends TableModelUtil {

        PositionKeeperUtil _util = null;

        public TableModelPLPosition(int rows, PositionKeeperUtil util) {
            super(util.getColumnNames().size(), rows);
            _util = util;
            for (int i = 0; i < _util.getColumnNames().size(); i++) setColumnName(i, _util.getHeaderAt(i));
        }

        public void showLiquidatedTrades(JTable table) {
            if (table == null)
                return;
            int row = table.getSelectedRow();
            if (row == -1)
                return;
            dblClicked(row, 0);
        }

        @Override
        public void dblClicked(int row, int col) {
            JDatetime datetime = getDatetime();
            String attributeType = _attributeName;
            PLPosition aggPos = (PLPosition) getClientProperty(row, PL_POSITION_KEY);
            String attributeName = (String) getClientProperty(row, PL_POSITION_AGG_ATTRIBUTE);
            int productId = aggPos.getProductId();
            int liqAggId = aggPos.getPositionAggregationId();
            LiquidationConfig liqConfig = aggPos.getLiquidationConfig();
            if (_books == null)
                return;
            PositionKeeper.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            ArrayList<String> bookNames = new ArrayList<String>();
            ArrayList<Integer> bookIds = new ArrayList<Integer>();
            for (int i = 0; i < _books.size(); i++) {
                Book book = (Book) _books.elementAt(i);
                String bookAttrName = book.getAttribute(attributeType);
                if (bookAttrName != null) {
                    if (bookAttrName.equals(attributeName)) {
                        bookNames.add(book.getName());
                        bookIds.add(book.getId());
                    }
                }
            }
            if (Util.isEmpty(bookIds)) {
                AppUtil.displayWarning("No Books Selected", PositionKeeper.this);
                PositionKeeper.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                return;
            }
            liqAggId = aggPos.getPositionAggregationId();
            boolean realTime = realTimeChangeCheck.isSelected();
            try {
                Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), productId);
                PLPosition plpos = new PLPosition();
                plpos.setProduct(product);
                // BZ 39531: set the liquidation aggregation id since it is part
                // of the key for looking up the position
                plpos.setPositionAggregationId(liqAggId);
                plpos.setLiquidationConfig(liqConfig);
                LiquidatedPositionArray liqPosArray = null;
                TradeOpenQuantityArray openPos = null;
                ;
                if (!realTime) {
                    liqPosArray = new LiquidatedPositionArray();
                    openPos = new TradeOpenQuantityArray();
                    for (Integer bookId : bookIds) {
                        plpos.setBookId(bookId);
                        int index = getPLPositions().indexOf(plpos);
                        if (index < 0)
                            continue;
                        PLPosition found = getPLPositions().elementAt(index);
                        for (int k = 0; k < found.getOpenPositions().size(); k++) {
                            TradeOpenQuantity toq = found.getOpenPositions().elementAt(k);
                            if (toq.getOpenQuantity() != 0 || toq.getOpenRepoQuantity() != 0)
                                openPos.add(toq);
                        }
                        LiquidatedPositionArray array = found.getLiqArray();
                        if (array != null) {
                            for (int k = 0; k < array.size(); k++) liqPosArray.add(array.get(k));
                        }
                    }
                } else {
                    LiquidatedPositionCriteriaBuilder liqPosCriteria = LiquidatedPositionCriteriaBuilder.create().book().eq(bookIds).product().eq(productId).liquidationConfig(liqConfig.getId()).posAggId().eq(liqAggId).deleted(false);
                    liqPosArray = DSConnection.getDefault().getRemoteLiquidation().getLiquidatedPositions(liqPosCriteria);
                    TradeOpenQuantityCriteriaBuilder toqCriteria = TradeOpenQuantityCriteriaBuilder.create().book().eq(bookIds).product().eq(product.getId()).liquidationConfig(liqConfig.getId()).posAggId().eq(liqAggId).status().ne(LiquidableStatus.Canceled).openQuantity().ne(0);
                    try {
                        openPos = DSConnection.getDefault().getRemoteLiquidation().getTradeOpenQuantity(toqCriteria);
                    } catch (Exception e) {
                        Log.error(this, e);
                    }
                }
                if (openPos == null) {
                    AppUtil.displayWarning("No Open Positions", PositionKeeper.this);
                    return;
                }
                LiquidatedPositionWindow ww = new LiquidatedPositionWindow();
                AppUtil.setCalypsoIcon(ww);
                JDatetime liqDatetime = !realTime ? datetime : null;
                ww.setPricingEnv(getPricingEnv());
                ww.setLiquidationDisplayCritereon(bookNames, productId, liqDatetime, liqAggId, liqConfig);
                ww.setPositions(liqPosArray, product);
                ww.setOpenPositions(openPos, product);
                ww.setVisible(true);
                ww.setTitle("Trade Open quantity");
            } catch (Exception e) {
                AppUtil.displayErrorDetails(PositionKeeper.this, "Error loading Position Detail", "Could not Open Pos or Liq Pos", e, "PositionKeeper");
            } finally {
                PositionKeeper.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

        /**
         * @param row
         * @return
         */
        private LiquidationConfig getLiquidationConfig(int row) {
            int pos;
            LiquidationConfig liqConfig = LiquidationConfig.getDEFAULT();
            pos = _util.getColumnPosition(COL_LIQ_CONFIG);
            if (pos != -1)
                liqConfig = (LiquidationConfig) getValueAt(row, pos);
            else {
                PLPosition p = (PLPosition) getClientProperty(row, PL_POSITION_KEY);
                if (p != null) {
                    liqConfig = p.getLiquidationConfig();
                } else {
                    liqConfig = LiquidationConfig.getDEFAULT();
                }
            }
            return liqConfig;
        }

        public void showLiquidationConfig(JTable table) {
            if (table == null)
                return;
            int row = table.getSelectedRow();
            if (row == -1)
                return;
            displayLiquidationConfig(this, row);
        }

        public void showBySettleDate(JTable table) {
            if (table == null)
                return;
            int row = table.getSelectedRow();
            if (row == -1)
                return;
            displayPositionBySettleDate(this, row);
        // Laurent ...
        }

        public void showDetailPosition(JTable table) {
            if (table == null)
                return;
            int row = table.getSelectedRow();
            if (row == -1)
                return;
            displayDetailPosition(this, row);
        }

        public void manualLiquidation(JTable table) {
            if (table == null)
                return;
            int[] rows = table.getSelectedRows();
            if (rows == null || rows.length == 0)
                return;
            performManualLiquidation(this, rows);
        }

        void adjustTooltip() {
            if (getTable() == null)
                return;
            getTable().setToolTipText("Total: (" + getRowCount() + ")");
        }

        public void showTrade(JTable table) {
            if (table == null)
                return;
            int row = table.getSelectedRow();
            if (row == -1)
                return;
            displayTrade(this, row);
        }

        @Override
        protected int compareRowsByColumn(int row1, int row2, int column) {
            String columnName = getColumnName(column);
            if (columnName != null && columnName.equals("Security")) {
                PositionKeeperUtil.SecurityDisplayValue s1 = (PositionKeeperUtil.SecurityDisplayValue) getValueAt(row1, column);
                PositionKeeperUtil.SecurityDisplayValue s2 = (PositionKeeperUtil.SecurityDisplayValue) getValueAt(row2, column);
                if (s1 == null && s2 == null) {
                    return 0;
                } else if (s1 == null) {
                    return 1;
                } else if (s2 == null) {
                    return -1;
                }
                return s1.compareTo(s2);
            }
            return super.compareRowsByColumn(row1, row2, column);
        }

        // 
        public void showLiquidatedFees(JTable table) {
            if (table == null)
                return;
            int row = table.getSelectedRow();
            if (row == -1)
                return;
            int col = 0;
            if (!mergeWithFeesCheck.isSelected()) {
                AppUtil.displayError("Please select Merge Fee CheckBox", PositionKeeper.this);
                return;
            }
            JDatetime datetime = getDatetime();
            String attributeType = _attributeName;
            int pos = _util.getColumnPosition(COL_ATTRIBUTE);
            if (pos == -1)
                return;
            String attributeName = (String) getValueAt(row, pos);
            if (_books == null)
                return;
            PositionKeeper.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Vector selBooks = new Vector();
            for (int i = 0; i < _books.size(); i++) {
                Book book = (Book) _books.elementAt(i);
                String bookAttrName = book.getAttribute(attributeType);
                if (bookAttrName != null) {
                    if (bookAttrName.equals(attributeName))
                        selBooks.addElement(Integer.valueOf(book.getId()));
                }
            }
            String bookStr = Util.vector2String(selBooks);
            if (bookStr == null || bookStr.trim().length() == 0.) {
                AppUtil.displayWarning("No Books Selected", PositionKeeper.this);
                PositionKeeper.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                return;
            }
            PLPosition aggPos = (PLPosition) getClientProperty(row, PL_POSITION_KEY);
            int pid = aggPos.getProductId();
            int liqAggId = aggPos.getPositionAggregationId();
            LiquidationConfig liqConfig = aggPos.getLiquidationConfig();
            String w = "book_id IN (" + bookStr + ")" + " AND product_id = " + pid + " AND liq_agg_id = " + liqAggId + " AND liq_config_id = " + liqConfig.getId() + " AND is_deleted = 0 ";
            LiquidatedPositionArray v = null;
            Product product = null;
            try {
                product = DSConnection.getDefault().getRemoteProduct().getProduct(pid);
                PLPosition plpos = new PLPosition();
                plpos.setProduct(product);
                // BZ 39531: set the liquidation aggregation id since it is part
                // of the key for looking up the position
                plpos.setPositionAggregationId(liqAggId);
                plpos.setLiquidationConfig(liqConfig);
                TradeOpenQuantityArray openPos = new TradeOpenQuantityArray();
                // Retrieve the List Of Positions
                Vector feeTypes = new Vector();
                for (int i = 0; i < selBooks.size(); i++) {
                    plpos.setBookId(((Integer) selBooks.elementAt(i)).intValue());
                    int index = getPLPositions().indexOf(plpos);
                    if (index < 0)
                        continue;
                    PLPosition found = getPLPositions().elementAt(index);
                    PLPositionArray array = found.getFeePositions();
                    if (array == null)
                        continue;
                    else {
                        for (int k = 0; k < array.size(); k++) {
                            PLPosition feePos = array.get(k);
                            Product pr = feePos.getProduct();
                            if (feeTypes.indexOf(pr.getSubType()) < 0) {
                                feeTypes.addElement(pr.getSubType());
                            }
                        }
                    }
                }
                if (Util.isEmpty(feeTypes)) {
                    AppUtil.displayWarning("No Fees attached to this position", PositionKeeper.this);
                    return;
                }
                Vector vv = DualListDialog.chooseList(new Vector(), PositionKeeper.this, feeTypes, new Vector());
                if (vv != null) {
                    for (int i = 0; i < selBooks.size(); i++) {
                        plpos.setBookId(((Integer) selBooks.elementAt(i)).intValue());
                        int index = getPLPositions().indexOf(plpos);
                        if (index < 0)
                            continue;
                        PLPosition found = getPLPositions().elementAt(index);
                        PLPositionArray posarray = found.getFeePositions();
                        if (posarray == null)
                            continue;
                        for (int l = 0; l < posarray.size(); l++) {
                            PLPosition feePos = posarray.get(l);
                            if (vv.indexOf(feePos.getProduct().getSubType()) < 0)
                                continue;
                            for (int k = 0; k < feePos.getOpenPositions().size(); k++) {
                                TradeOpenQuantity toq = feePos.getOpenPositions().elementAt(k);
                                if (toq.getOpenQuantity() != 0 || toq.getOpenRepoQuantity() != 0)
                                    openPos.add(toq);
                            }
                            LiquidatedPositionArray array = feePos.getLiqArray();
                            if (array != null) {
                                if (v == null)
                                    v = new LiquidatedPositionArray();
                                for (int k = 0; k < array.size(); k++) v.add(array.get(k));
                            }
                        }
                    }
                }
                if (openPos == null) {
                    AppUtil.displayWarning("No Open Positions", PositionKeeper.this);
                    return;
                }
                LiquidatedPositionWindow ww = new LiquidatedPositionWindow();
                AppUtil.setCalypsoIcon(ww);
                ww.setPositions(v, product);
                ww.setOpenPositions(openPos, product);
                ww.setVisible(true);
                ww.setTitle("Trade Open quantity");
            } catch (Exception e) {
                Log.error(this, e);
            } finally {
                PositionKeeper.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
        // 
    }

    protected void buildAggregatedPLPositions(PLPositionArray positions) {
        _aggregatedPLPositions = new Hashtable();
        int size = positions.size();
        for (int i = 0; i < size; i++) {
            PLPosition position = positions.elementAt(i);
            addAggregatedPositions(position, null);
        }
        // filter for tab
        size = _products.size();
        for (int i = 0; i < size; i++) {
            TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
            ppl.buildAggregatedPLPositions(_aggregatedPLPositions);
        }
    }

    // Note that if tabs is null the aggregation of each TabPLPosition
    // must be done latter (see buildAggregatedPLPositions)
    protected PLPositionArray addAggregatedPositions(PLPosition position, Vector tabs) {
        if (_aggregatedPLPositions == null)
            return null;
        String key = getBook(position.getBookId()).getAttribute(_attributeName) + position.getProductId() + "_" + position.getLiquidationConfig() + "_" + position.getPositionAggregationId();
        PLPositionArray aggPositions = _aggregatedPLPositions.get(key);
        if (aggPositions == null) {
            aggPositions = new PLPositionArray();
            aggPositions.add(position);
            _aggregatedPLPositions.put(key, aggPositions);
        } else {
            if (aggPositions.indexOf(position) >= 0) {
                aggPositions.remove(position);
            }
            aggPositions.add(position);
        }
        if (tabs != null) {
            // puts into tabs all the TabPLPosition that matches
            for (int i = 0; i < _products.size(); i++) {
                TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
                if (ppl.addAggregatedPositions(key, aggPositions, position))
                    tabs.addElement(ppl);
            }
        }
        return aggPositions;
    }

    private void removePLPosition(PLPosition position, boolean fromPLPositions, int displayIndex) {
        // build position key
        String key = getBook(position.getBookId()).getAttribute(_attributeName) + position.getProductId() + "_" + position.getLiquidationConfig() + "_" + position.getPositionAggregationId();
        // remove from _aggregatedPLPositions
        if (_aggregatedPLPositions != null) {
            PLPositionArray aggPositions = _aggregatedPLPositions.get(key);
            if ((aggPositions != null) && aggPositions.indexOf(position) >= 0) {
                _aggregatedPLPositions.remove(key);
            }
        }
        if (fromPLPositions) {
            // remove from _PLPositions
            if ((_PLPositions != null) && _PLPositions.indexOf(position) >= 0) {
                _PLPositions.remove(position);
            }
        }
        // remove from display
        int size = _products.size();
        for (int i = 0; i < size; i++) {
            TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
            TableModelPLPosition model = ppl._plPositionModel;
            int rowIndex = getRowIndex(displayIndex, position, model);
            ppl.removeAggregatedPosition(key, rowIndex);
        }
    }

    void orgTree_mouseClicked(java.awt.event.MouseEvent event) {
        int lastMouseClkX = event.getX();
        int lastMouseClkY = event.getY();
        if (event.getClickCount() > 1) {
            TreePath path = orgTree.getClosestPathForLocation(lastMouseClkX, lastMouseClkY);
            if (path == null)
                return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) (path.getLastPathComponent());
            if ((node != null) && (node.getUserObject() instanceof BookHierarchyNode)) {
                BookHierarchyNode bnode = (BookHierarchyNode) node.getUserObject();
                _selectedBookHierarchyNode = bnode;
                filter(getAllPLPositions());
            }
        }
    }

    protected int sign(double d) {
        if (d > 0)
            return 1;
        else
            return -1;
    }

    void positionMenuAction(Object source, String action, TableModelPLPosition model, JTable table) {
        if (action.equals("showLiquidatedPositions")) {
            model.showLiquidatedTrades(table);
        } else if (action.equals("showFeePositions")) {
            model.showLiquidatedFees(table);
        } else if (action.equals("showBySettleDate")) {
            model.showBySettleDate(table);
        } else if (action.equals("showDetailPosition")) {
            model.showDetailPosition(table);
        } else if (action.equals("showLiquidationConfig")) {
            model.showLiquidationConfig(table);
        } else if (action.equals("manualLiquidation")) {
            model.manualLiquidation(table);
        } else if (action.equals("configureColumns")) {
        // Use Configure Tabs Bug#: 12491
        // if(model._util.configureColumns(this)) {
        // // update table
        // }
        } else if (action.equals("openTrade")) {
            model.showTrade(table);
        }
    }

    class PositionMenuAction implements java.awt.event.ActionListener {

        TableModelPLPosition _model;

        JTable _table = null;

        public PositionMenuAction(JTable table, TableModelPLPosition model) {
            _model = model;
            _table = table;
        }

        public void actionPerformed(java.awt.event.ActionEvent event) {
            positionMenuAction(event.getSource(), event.getActionCommand(), _model, _table);
        }
    }

    /*
		 * ===================================================================== *
		 * Class TradeExplodeModel * Purpose TableModel for displaying the
		 * TradeExplode given by * Position and Settle Positions
		 * =====================================================================
		 */
    class TradeExplodeModel extends TableModelUtil {

        public TradeExplodeModel(int cols, int rows) {
            super(cols, rows);
        }

        @Override
        public void dblClicked(int row, int col) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            long tradeId = ((Long) getValueAt(row, 0)).longValue();
            try {
                Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
                String productF = trade.getProductFamily();
                ShowTrade w = TradeUtil.getInstance(productF, trade.getProduct().getType());
                w.showTrade(trade);
                ((Frame) w).setVisible(true);
            } catch (Exception e) {
                Log.error(this, e);
            } finally {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /*
		 * * List of Function to accept or reject the Positions based * on the
		 * hierarchy selected
		 */
    protected boolean accept(PLPosition position) {
        if (!_isAllBookAvailable) {
            Book bk = getBook(position.getBookId());
            if (bk != null && _availBooks.size() > 0) {
                if (_availBooks.get(bk.getName()) == null)
                    return false;
            }
        }
        if (_selectedBookHierarchyNode != null)
            return _selectedBookHierarchyNode.fullAccept(getBook(position.getBookId()));
        return (!isZeroPositionForRemoval(position, false));
    }

    private boolean isZeroPositionForRemoval(PLPosition position, boolean checkOnlyIfAggregationOn) {
        // is option checked
        if (_excludeZeroPositions == 0) {
            return false;
        }
        // is the aggregation on versus do we care only if it's on
        if (checkOnlyIfAggregationOn == _attributeName.equals("BookName")) {
            return false;
        }
        boolean zeroPosition = false;
        switch(_excludeZeroPositions) {
            case 1:
                // exclude when 0 nominal AND 0 realized pl
                if (_zeroPositionTolerance != 0.0) {
                    zeroPosition = (Math.abs(position.getQuantity()) <= _zeroPositionTolerance) && (Math.abs(position.getRealized()) <= _zeroPositionTolerance);
                } else {
                    zeroPosition = ((position.getQuantity() == 0.) && (position.getRealized() == 0.));
                }
                break;
            case 2:
                // exclude when 0 nominal
                if (_zeroPositionTolerance != 0.0) {
                    zeroPosition = (Math.abs(position.getQuantity()) <= _zeroPositionTolerance);
                } else {
                    zeroPosition = (position.getQuantity() == 0.);
                }
                break;
        }
        return zeroPosition;
    }

    private void setTolerance() {
        if (_excludeZeroPositions == 0) {
            zeroPositionTolerance.setDisabledTextColor(Color.lightGray);
            zeroPositionTolerance.setEnabled(false);
        } else
            zeroPositionTolerance.setEnabled(true);
        String tolS = zeroPositionTolerance.getText();
        if (Util.isEmpty(tolS)) {
            _zeroPositionTolerance = 0.;
            zeroPositionTolerance.setText("0.0");
            zeroPositionTolerance.setEnabled(false);
        } else
            _zeroPositionTolerance = Util.toDouble(tolS);
    }

    void pricingEnvChoice_itemStateChanged(java.awt.event.ItemEvent event) {
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    // CAL-236947 - itemStateChanged gets called twice by JComboBox.selectedItemChanged, which
    // caused setMarketConfig, with 2 time-consuming remote calls to getPricingEnv, to get called twice.
    // ActionListener only gets called once after the JComboBox selection has changed, so moving
    // setMarketConfig here to improve performance.
    void pricingEnvChoice_actionPerformed(java.awt.event.ActionEvent event) {
        setMarketConfig();
    }

    Book getBook(int bid) {
        return BOCache.getBook(DSConnection.getDefault(), bid);
    }

    void setStaticDataFilter(TabPLPosition p, String name) {
        p._filterName = (name == null ? "" : name);
        p._staticDataFilter = null;
        if (p._filterName != null && p._filterName.length() > 0) {
            try {
                p._staticDataFilter = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter(p._filterName);
            } catch (Exception e) {
                Log.error(this, e);
            }
        }
    }

    public int removeAllPLPositionTabs() {
        int selected = JTabbedPane1.getSelectedIndex();
        for (int i = 0; i < _products.size(); i++) {
            TabPLPosition p = (TabPLPosition) _products.elementAt(i);
            if (JTabbedPane1.indexOfComponent(p._scrollPane) != -1) {
                JTabbedPane1.remove(p._scrollPane);
            }
        }
        _products.removeAllElements();
        return selected;
    }

    // from PositionConfigTabs
    public void apply() {
        loadTabs();
    }

    public void showPositionKeeperConfigDialog() {
        if (_pkConfigDialog == null)
            _pkConfigDialog = new PositionConfigTabs();
        _pkConfigDialog.load(this, PK_NAME, getAllColumnNames());
        _pkConfigDialog.setVisible(true);
    }

    class TabPLPosition {

        public javax.swing.JScrollPane _scrollPane = null;

        public TableModelPLPosition _plPositionModel = null;

        public PositionKeeperUtil _plUtil = null;

        public Hashtable _plPositions = new Hashtable();

        // list of Integers
        Vector _bookIdList = new Vector();

        // list of Strings
        Vector _productTypeList = new Vector();

        // list of Strings
        Vector _contractList = new Vector();

        String _filterName = "";

        StaticDataFilter _staticDataFilter = null;

        String _name;

        int _nbFixedColumns = 0;

        boolean _openTrades = false;

        Vector _models = null;

        public TabPLPosition(String name, int rows, Vector columnNames, int nbFixedColumns, boolean openTrades) {
            _name = name;
            _plUtil = new PositionKeeperUtil();
            _plUtil.setColumnNames(columnNames);
            _plPositionModel = new TableModelPLPosition(rows, _plUtil);
            _nbFixedColumns = nbFixedColumns;
            _openTrades = openTrades;
            if (_nbFixedColumns >= columnNames.size())
                _nbFixedColumns = 0;
        }

        @Override
        public String toString() {
            return _name;
        }

        public void recreateScrollPane() {
            int idx = -1;
            if (_scrollPane != null) {
                idx = JTabbedPane1.indexOfTab(_name);
                JTabbedPane1.remove(_scrollPane);
            }
            _scrollPane = new JScrollPane();
            _models = TableModelUtilAdapter.freezeColumns(_scrollPane, _plPositionModel, _nbFixedColumns, true, true);
            setModelMenu((TableModelUtil) _models.get(_models.size() - 1), _plPositionModel);
            if (idx == -1)
                JTabbedPane1.addTab(_name, _scrollPane);
            else
                JTabbedPane1.insertTab(_name, null, _scrollPane, null, idx);
        }

        public void callFireTableRowsUpdated(int index) {
            if (_models == null)
                return;
            for (int i = 0; i < _models.size(); i++) {
                TableModelUtil model = (TableModelUtil) _models.get(i);
                model.fireTableRowsUpdated(index, index);
            }
        }

        public Vector getBookIdList() {
            return _bookIdList;
        }

        public Vector getProductTypeList() {
            return _productTypeList;
        }

        public Vector getContractList() {
            return _contractList;
        }

        private boolean isPLPositionIn(PLPosition position) {
            // Check Book
            boolean ok = false;
            int bid = position.getBookId();
            for (int i = 0; i < _bookIdList.size(); i++) {
                if (bid == ((Integer) _bookIdList.elementAt(i)).intValue()) {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                return false;
            // Check Product Type
            ok = false;
            String type = position.getProduct().getType();
            if (type == null)
                return false;
            for (int i = 0; i < _productTypeList.size(); i++) {
                if (type.equals(_productTypeList.elementAt(i))) {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                return false;
            // check Contract Type
            if (_contractList != null && _contractList.size() > 0) {
                ok = false;
                Product product = position.getProduct();
                if (product instanceof Future) {
                    String contract = ((Future) product).getName();
                    for (int i = 0; i < _contractList.size(); i++) {
                        if (contract.equals(_contractList.elementAt(i))) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok)
                        return false;
                } else if (product instanceof FutureOption) {
                    FutureOption fo = (FutureOption) product;
                    String contract = fo.getOptionContract().getUnderlying().getName();
                    for (int i = 0; i < _contractList.size(); i++) {
                        if (contract.equals(_contractList.elementAt(i))) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok)
                        return false;
                }
            }
            // check static data filter
            if (_staticDataFilter != null)
                // Need to be optimized...
                if (!_staticDataFilter.accept(position.toTrade()))
                    return false;
            // check open position
            if (_openTrades) {
                TradeOpenQuantityArray array = position.getOpenPositions();
                if (array == null)
                    return false;
                int size = 0;
                for (int kk = 0; kk < array.size(); kk++) {
                    TradeOpenQuantity toq = array.get(kk);
                    if (toq.getOpenQuantity() == 0)
                        continue;
                    size++;
                }
                if (size == 0)
                    return false;
            }
            return true;
        }

        public void buildAggregatedPLPositions(Hashtable all) {
            _plPositions.clear();
            for (Enumeration e = all.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                PLPositionArray value = (PLPositionArray) all.get(key);
                if (value == null || value.size() == 0)
                    continue;
                PLPosition position = value.elementAt(0);
                if (isPLPositionIn(position))
                    _plPositions.put(key, value);
            }
        }

        public boolean addAggregatedPositions(String key, PLPositionArray aggPositions, PLPosition position) {
            PLPositionArray value = (PLPositionArray) _plPositions.get(key);
            if (value != null) {
                _plPositions.put(key, aggPositions);
                return true;
            }
            if (isPLPositionIn(position)) {
                _plPositions.put(key, aggPositions);
                return true;
            }
            return false;
        }

        public void removeAggregatedPosition(String key, int index) {
            // remove aggregated position
            PLPositionArray value = (PLPositionArray) _plPositions.get(key);
            if (value != null) {
                _plPositions.remove(key);
            }
            // remove from underlying model
            if (index > -1) {
                _plPositionModel.removeRow(index);
                _plPositionModel.fireTableRowsDeleted(index, index);
            }
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    class MenuAction implements java.awt.event.ActionListener {

        public void actionPerformed(java.awt.event.ActionEvent event) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            try {
                menuAction(event.getSource(), event.getActionCommand());
            } finally {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    class MenuCheckListener implements java.awt.event.ItemListener {

        public void itemStateChanged(java.awt.event.ItemEvent event) {
            Object object = event.getSource();
            if (event != null && event.getStateChange() == java.awt.event.ItemEvent.DESELECTED)
                return;
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) object;
            menuAction(object, item.getActionCommand());
        }
    }

    void loadButton_actionPerformed(java.awt.event.ActionEvent event) {
        String name = (String) portfolioChoice.getSelectedItem();
        _isAllBookAvailable = isAllBookAvailable();
        if (!_isAllBookAvailable) {
            _availBooks = getBookMap();
        }
        setTolerance();
        if (name != null)
            showTradeFilter(name);
        // loadButton.setForeground(java.awt.Color.black);
        // loadButton.setEnabled(false);
        AppUtil.attachIconToButton(loadButton, false);
    }

    void menuAction(Object source, String action) {
        if (action.equals("ConfigureTabs")) {
            showPositionKeeperConfigDialog();
        } else if (action.equals("Load")) {
            loadButton_actionPerformed(null);
        } else if (action.equals("CheckQuotes")) {
            checkQuotes();
        } else if (action.equals("Reconnect")) {
            reconnect();
        } else if (action.equals("Close")) {
            closeWS();
        } else if (action.equals("Update")) {
            updateDomains();
        } else if (action.equals("Clear")) {
            clearWS();
        } else if (action.equals("RefreshTimeout")) {
            setRefreshTimeout();
        } else if (action.equals("PricingEnv")) {
            PricingEnvWindow w = new PricingEnvWindow();
            w.setVisible(true);
            PricingEnv env = getPricingEnv();
            w.show(env);
        } else if (action.equals("Reload")) {
            reloadPricingEnv();
        } else if (action.equals("Refresh")) {
            refreshPricingEnv();
        } else if (action.equals("Display")) {
            displayPricingEnv();
        } else if (action.equals("Check")) {
            checkQuotes();
        } else if (action.equals("Quotes")) {
            showQuotes();
        } else if (action.equals("Help")) {
            String helpLocation = Defaults.getProperty(Defaults.HELP_LOCATION);
            DisplayInBrowser.displayURL(helpLocation + "Subsystems/Position/Position.htm#cshid=PositionKeeper", true);
        }
    }

    Vector getTrades() {
        Vector trades = new Vector();
        if (_allPLPositions == null)
            return trades;
        for (int i = 0; i < _allPLPositions.size(); i++) {
            PLPosition plPosition = _allPLPositions.elementAt(i);
            Trade trade = plPosition.toTrade();
            trades.add(trade);
        // System.out.println("Quote " +
        // trade.getProduct().getQuoteName());
        }
        return trades;
    }

    void showQuotes() {
        if (_quoteWindow == null) {
            _quoteWindow = new QuoteJFrame();
            AppUtil.setCalypsoIcon(_quoteWindow);
        }
        _quoteWindow.setVisible(true);
        PricingEnv pricingEnv = getPricingEnv();
        if (_env == null) {
            return;
        }
        JDatetime _valDateTime = getValDate();
        JDate valDate = JDate.valueOf(_valDateTime);
        Vector trades = getTrades();
        PricerConfig config = pricingEnv.getPricerConfig();
        Hashtable h = new Hashtable();
        config.getQuotes(_env.getQuoteSetName(), valDate, h);
        _quoteWindow.showQuotes(_env.getQuoteSet(), Util.toVector(h));
    }

    void checkPricingEnv() {
        Vector trades = getTrades();
        PricingEnv pricingEnv = getPricingEnv();
        JDatetime _valDateTime = getValDate();
        JDate valDate = JDate.valueOf(_valDateTime);
        PricerConfig config = pricingEnv.getPricerConfig();
        Hashtable priceh = new Hashtable();
        config.getMissingPricers(trades, pricingEnv, valDate, priceh);
        Hashtable mdataH = new Hashtable();
        config.getMissingMarketDataItems(trades, pricingEnv, valDate, mdataH);
        Hashtable quotesH = new Hashtable();
        config.getMissingQuotes(trades, pricingEnv, valDate, quotesH);
        if (trades != null) {
            config.getMissingPricers(trades, pricingEnv, valDate, priceh);
            config.getMissingQuotes(trades, pricingEnv, valDate, quotesH);
        }
        if ((quotesH.size() == 0) && (mdataH.size() == 0) && (priceh.size() == 0)) {
            AppUtil.displayWarning("Market Data Information complete", this);
            return;
        }
        QuoteListWindow w = new QuoteListWindow();
        w.setVisible(true);
        AppUtil.setCalypsoIcon(w);
        w.setQuotes(Util.toVector(quotesH));
        w.setPricers(Util.toVector(priceh));
        w.setMarketDataItems(Util.toVector(mdataH));
        w.setQuoteSet(pricingEnv.getQuoteSet());
        w.setPricerConfig(pricingEnv.getPricerConfig());
    }

    JDatetime getValDate() {
        return Util.stringToJDatetime(valDateText.getText(), timeText.getText());
    }

    void reloadPricingEnv() {
        _env = null;
        _previousEnv = null;
        getPricingEnv();
    }

    void refreshPricingEnv() {
        PricingEnv env = getPricingEnv();
        try {
            JDatetime _valDateTime = getValDate();
            Vector trades = getTrades();
            Vector errors = env.getPricerConfig().refresh(trades, env, _valDateTime);
            if (errors != null && errors.size() > 0)
                AppUtil.displayWarnings(errors, this);
        } catch (Exception ex) {
            Log.error(this, ex);
        }
    }

    String getDisplayTitle() {
        return "Position Keeper " + portfolioChoice.getSelectedItem() + "/" + pricingEnvChoice.getSelectedItem() + "/" + getValDate();
    }

    void displayPricingEnv() {
        Vector trades = getTrades();
        if (_marketdataFrame == null) {
            _marketdataFrame = new MarketDataJFrame();
            AppUtil.setCalypsoIcon(_marketdataFrame);
        }
        _marketdataFrame.setTitle(getDisplayTitle());
        _marketdataFrame.setVisible(true);
        PricingEnv env = getPricingEnv();
        _marketdataFrame.init(env, getValDate(), trades);
    }

    // temporary
    public static String getDefaultsFileName(String envName) {
        if (envName != null)
            return Defaults.getUserDirName() + File.separator + "calypsouser.pkconfig." + envName;
        else
            return Defaults.getUserDirName() + File.separator + "calypsouser.pkconfig";
    }

    public static Properties getPropertiesFromFile(String fileName) {
        Properties props = new Properties();
        try {
            File file = ResourceFactory.get().newFile(fileName);
            InputStream stream = new FileInputStream(file);
            if (stream != null) {
                props.load(stream);
                try {
                    stream.close();
                } catch (Exception ee) {
                }
            }
        } catch (Exception ae) {
        }
        // Log.error(this, ae);}
        return props;
    }

    static String PK_NAME = "PositionKeeperTab";

    Vector getAllColumnNames() {
        if (_allColumnNames != null)
            return _allColumnNames;
        PositionKeeperUtil util = new PositionKeeperUtil();
        // _allColumnNames = util.getAllColumnNames();
        _allColumnNames = util.getColumnNames();
        List l = BOCache.getPositionAggregationConfigs(DSConnection.getDefault());
        if (l == null || l.size() == 0) {
            _allColumnNames.remove(PositionKeeperUtil.LIQ_AGGREGATION);
            _allColumnNames.remove(PositionKeeperUtil.LIQ_AGG_ID);
            _allColumnNames.remove(PositionKeeperUtil.CUSTODIAN);
            _allColumnNames.remove(PositionKeeperUtil.LONG_SHORT);
            _allColumnNames.remove(PositionKeeperUtil.STRATEGY);
            _allColumnNames.remove(PositionKeeperUtil.TRADER);
            _allColumnNames.remove(PositionKeeperUtil.BUNDLE);
        }
        Map<String, LiquidationConfig> liqConfigs = null;
        try {
            liqConfigs = DSConnection.getDefault().getRemoteReferenceData().getLiquidationConfigsByName();
        } catch (RemoteException e) {
            Log.error(this, "Could not load liquidation configs", e);
        }
        if (liqConfigs == null || liqConfigs.size() <= 1) {
            _allColumnNames.remove(PositionKeeperUtil.LIQ_CONFIG);
        }
        return _allColumnNames;
    }

    // Bug#: 15092
    static Vector _onlySecondary = null;

    public static Vector getOnlySecondaryMarketProduct(boolean clone) {
        if (_onlySecondary != null) {
            if (clone)
                return new Vector(_onlySecondary);
            return _onlySecondary;
        }
        _onlySecondary = LocalCache.cloneDomainValues(DSConnection.getDefault(), "productType");
        if (_onlySecondary == null || _onlySecondary.size() == 0)
            return new Vector();
        Vector all = new Vector(_onlySecondary);
        Product p;
        for (int i = 0; i < all.size(); i++) {
            String name = (String) all.get(i);
            try {
                p = (Product) InstantiateUtil.getInstance("tk.product." + name);
                if (p.isPositionProxy() || (p != null && !p.hasSecondaryMarket()))
                    _onlySecondary.remove(name);
            } catch (Throwable e) {
            }
        }
        // Remove also "Collateral"
        _onlySecondary.remove("Collateral");
        if (clone)
            return new Vector(_onlySecondary);
        return _onlySecondary;
    }

    public void loadTabs() {
        Vector tabs = new Vector();
        Hashtable tabBooks = new Hashtable();
        Hashtable tabProducts = new Hashtable();
        Hashtable tabContracts = new Hashtable();
        Hashtable tabColumns = new Hashtable();
        Hashtable tabFixedColumns = new Hashtable();
        Hashtable tabFilterName = new Hashtable();
        Hashtable tabOpenTrades = new Hashtable();
        PositionConfigTabs.loadTabs(PK_NAME, getAllColumnNames(), tabs, tabBooks, tabProducts, tabContracts, tabColumns, tabFixedColumns, tabFilterName, tabOpenTrades);
        if (tabs.size() == 0) {
            // init defaults
            Vector pt = null;
            Vector contracts = null;
            try {
                // Bug#: 15092
                pt = getOnlySecondaryMarketProduct(false);
                RemoteProduct rp = DSConnection.getDefault().getRemoteProduct();
                Vector cn = rp.getContractNames();
                Vector ocn = rp.getOptionContractNames();
                contracts = new Vector();
                contracts.addAll(cn);
                if (ocn != null) {
                    for (int i = 0; i < ocn.size(); i++) {
                        String name = (String) ocn.elementAt(i);
                        if (!contracts.contains(name))
                            contracts.addElement(name);
                    }
                }
            } catch (Exception e) {
                Log.error(this, e);
                return;
            }
            if (pt == null)
                return;
            Vector bn = new Vector();
            bn.add(ANY_BOOK_NAME);
            tabs.add("All");
            tabBooks.put("All", bn);
            tabProducts.put("All", pt);
            tabContracts.put("All", contracts);
            tabColumns.put("All", getAllColumnNames());
            PositionConfigTabs.saveTabs(PK_NAME, null, getAllColumnNames(), tabs, tabBooks, tabProducts, tabContracts, tabColumns, tabFixedColumns, tabFilterName, tabOpenTrades);
        }
        removeAllPLPositionTabs();
        // Create tabs
        Vector bn = null;
        Vector allColumnList = new Vector();
        for (int i = 0; i < tabs.size(); i++) {
            String tabName = (String) tabs.get(i);
            String filterName = (String) tabFilterName.get(tabName);
            String fixedColumns = (String) tabFixedColumns.get(tabName);
            Vector bookNames = (Vector) tabBooks.get(tabName);
            if (bookNames != null && bookNames.contains(ANY_BOOK_NAME)) {
                if (bn == null) {
                    try {
                        bn = DSConnection.getDefault().getRemoteReferenceData().getBookNames();
                    } catch (Exception e) {
                        Log.error(this, e);
                        return;
                    }
                }
                bookNames = bn;
            }
            Vector productTypes = (Vector) tabProducts.get(tabName);
            Vector contractNames = (Vector) tabContracts.get(tabName);
            Vector columnList = (Vector) tabColumns.get(tabName);
            allColumnList = Util.mergeVectors(allColumnList, columnList);
            String openTrades = (String) tabOpenTrades.get(tabName);
            int nbColumns = 0;
            if (!Util.isEmptyString(fixedColumns)) {
                try {
                    nbColumns = Integer.parseInt(fixedColumns);
                } catch (Throwable x) {
                }
            }
            boolean ot = (openTrades == null ? false : openTrades.equals("true"));
            TabPLPosition p = new TabPLPosition(tabName, 0, columnList, nbColumns, ot);
            setStaticDataFilter(p, filterName);
            if (bookNames != null) {
                for (int j = 0; j < bookNames.size(); j++) {
                    String book = (String) bookNames.elementAt(j);
                    int id = PositionConfigTabs.getBookId(book);
                    p._bookIdList.addElement(Integer.valueOf(id));
                }
            }
            p._productTypeList = productTypes;
            p._contractList = contractNames;
            if (p._productTypeList == null)
                p._productTypeList = new Vector();
            _products.addElement(p);
        }
        if (allColumnList != null && allColumnList.size() > 0) {
            _columnNamesUsed = allColumnList;
            setIncludePreviousPL();
        }
        showPLPositions(getPLPositions());
    }

    private Vector getColumnNamesUsed() {
        if (_columnNamesUsed != null && _columnNamesUsed.size() > 0)
            return _columnNamesUsed;
        Vector columns = new Vector();
        Vector tabs = new Vector();
        Hashtable tabBooks = new Hashtable();
        Hashtable tabProducts = new Hashtable();
        Hashtable tabContracts = new Hashtable();
        Hashtable tabColumns = new Hashtable();
        Hashtable tabFixedColumns = new Hashtable();
        Hashtable tabFilterName = new Hashtable();
        Hashtable tabOpenTrades = new Hashtable();
        PositionConfigTabs.loadTabs(PK_NAME, getAllColumnNames(), tabs, tabBooks, tabProducts, tabContracts, tabColumns, tabFixedColumns, tabFilterName, tabOpenTrades);
        for (int i = 0; i < tabs.size(); i++) {
            String tabName = (String) tabs.get(i);
            Vector columnList = (Vector) tabColumns.get(tabName);
            columns = Util.mergeVectors(columns, columnList);
        }
        _columnNamesUsed = columns;
        return _columnNamesUsed;
    }

    void checkQuotes() {
        if (_allPLPositions == null)
            return;
        Vector trades = new Vector();
        for (int i = 0; i < _allPLPositions.size(); i++) {
            PLPosition plPosition = _allPLPositions.elementAt(i);
            Trade trade = null;
            if (plPosition != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(plPosition), CommodityForward.COMMODITY_FORWARD)) {
                trade = CommodityUtil.getTradeForPricing(plPosition);
            } else {
                trade = plPosition.toTrade();
            }
            trades.addElement(trade);
        // System.out.println("Quote " +
        // trade.getProduct().getQuoteName());
        }
        JDatetime _valDateTime = getValDate();
        JDate valDate = JDate.valueOf(_valDateTime);
        PricingEnv pricingEnv = getPricingEnv();
        PricerConfig config = pricingEnv.getPricerConfig();
        Hashtable priceh = new Hashtable();
        pricingEnv.getQuoteSet().setCacheNotFound(false);
        config.getMissingPricers(trades, pricingEnv, valDate, priceh);
        Hashtable mdataH = new Hashtable();
        config.getMissingMarketDataItems(trades, pricingEnv, valDate, mdataH);
        Hashtable quotesH = new Hashtable();
        config.getMissingQuotes(trades, pricingEnv, valDate, quotesH);
        if ((quotesH.size() == 0) && (mdataH.size() == 0) && (priceh.size() == 0)) {
            AppUtil.displayMessage("Market Data Information complete", this);
            return;
        }
        QuoteListWindow w = new QuoteListWindow();
        w.setVisible(true);
        AppUtil.setCalypsoIcon(w);
        w.setQuotes(Util.toVector(quotesH));
        w.setPricers(Util.toVector(priceh));
        w.setMarketDataItems(Util.toVector(mdataH));
        w.setQuoteSet(pricingEnv.getQuoteSet());
        w.setPricerConfig(pricingEnv.getPricerConfig());
    }

    void displayPositionBySettleDate(TableModelPLPosition posModel, int row) {
        JDatetime datetime = getDatetime();
        String attributeType = this._attributeName;
        PLPosition aggPos = (PLPosition)posModel.getClientProperty(row, "PLPosition");
        String attributeName = (String)posModel.getClientProperty(row, "PLPositionAggAttribute");
        int pid = aggPos.getProductId();
        long liqAggId = aggPos.getPositionAggregationId();
        LiquidationConfig liqConfig = aggPos.getLiquidationConfig();
        if (this._books == null)
          return; 
        setCursor(Cursor.getPredefinedCursor(3));
        try {
          Vector selBooks = new Vector();
          for (int i = 0; i < this._books.size(); i++) {
            Book book = (Book)this._books.elementAt(i);
            String bookAttrName = book.getAttribute(attributeType);
            if (bookAttrName != null && 
              bookAttrName.equals(attributeName)) {
              selBooks.addElement(Integer.valueOf(book.getId()));
            }
          } 
          String bookStr = Util.vector2String(selBooks);
          
          if (bookStr == null || bookStr.trim().length() == 0.0D) {
            AppUtil.displayWarning("No Books Selected", this);
            return;
          } 
          boolean state = this.realTimeChangeCheck.isSelected();
          Vector v = null;
          Product product = null;
          try {
            List<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
            Book abook = new Book();
            product = DSConnection.getDefault().getRemoteProduct().getProduct(pid);
            TradeOpenQuantityArray openPos = new TradeOpenQuantityArray();
            if (state) {
              
              String wopen = "book_id IN (" + Util.collectionToPreparedInString(selBooks, bindVariables) + ") AND product_id = ? AND liq_agg_id = ?";
              bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(pid)));
              bindVariables.add(new CalypsoBindVariable(3000, Long.valueOf(liqAggId)));
              int liqConfigId = liqConfig.getId();
              if (liqConfigId >= 0) {
                wopen = wopen + " AND liq_config_id = ?";
                bindVariables.add(new CalypsoBindVariable(4, Integer.valueOf(liqConfigId)));
              } 
              openPos = DSConnection.getDefault().getRemoteTrade().getTradeOpenQuantity(wopen, "trade_date", bindVariables);
              if (openPos == null) {
                AppUtil.displayWarning("No Open Positions", this);
                return;
              } 
            } else {
              PLPosition plpos = new PLPosition();
              plpos.setProduct(product);
              for (int i = 0; i < selBooks.size(); i++) {
                plpos.setBookId(((Integer)selBooks.elementAt(i)).intValue());
                int index = getPLPositions().indexOf(plpos);
                if (index >= 0) {
                  
                  PLPosition found = getPLPositions().elementAt(index);
                  int size = found.getOpenPositions().size();
                  for (int k = 0; k < size; k++) {
                    TradeOpenQuantity toq = found.getOpenPositions().elementAt(k);
                    if (toq.getOpenQuantity() != 0.0D || toq.getOpenRepoQuantity() != 0.0D)
                      openPos.add(toq); 
                  } 
                } 
              } 
            } 
            showPositionBySettleDate(openPos, product);
          } catch (Exception e) {
            Log.error(this, e);
          } 
        } finally {
          setCursor(Cursor.getPredefinedCursor(0));
        } 
      }

    void showPositionBySettleDate(TradeOpenQuantityArray openPos, Product product) {
        if (product instanceof FX) {
            showFXPositionBySettleDate(openPos, (FX) product);
            return;
        }
        Hashtable h = new Hashtable();
        SettlePos sp = new SettlePos();
        int size = openPos.size();
        for (int i = 0; i < size; i++) {
            TradeOpenQuantity oq = openPos.elementAt(i);
            sp._settleDate = oq.getSettleDate();
            SettlePos pos = (SettlePos) h.get(sp);
            if (pos == null) {
                pos = new SettlePos();
                pos._settleDate = oq.getSettleDate();
                h.put(pos, pos);
            }
            pos._quantity += oq.getQuantity();
            pos._openQuantity += oq.getOpenQuantity();
        }
        ShowTableWindow ww = new ShowTableWindow();
        ww.setTitle("Position By Settle Date for " + product.getDescription());
        AppUtil.setCalypsoIcon(ww);
        JTable table = ww.getTable();
        TableModelUtil model = new TableModelUtil(3, h.size());
        model.setTo(table);
        model.setColumnName(0, "Settle Date");
        model.setColumnName(1, "Open Quantity");
        model.setColumnName(2, "Quantity");
        Enumeration en = h.keys();
        int row = 0;
        while (en.hasMoreElements()) {
            sp = (SettlePos) en.nextElement();
            model.setValueNoCheck(row, 0, sp._settleDate);
            model.setValueNoCheck(row, 1, new SignedAmount(sp._openQuantity));
            model.setValueNoCheck(row, 2, new SignedAmount(sp._quantity));
            row++;
        }
        int[] cols = new int[1];
        cols[0] = 0;
        model.sortByColumn(cols, true);
        model.refresh();
        TableUtil.fastAdjust(table);
        ww.setVisible(true);
    }

    void showFXPositionBySettleDate(TradeOpenQuantityArray openPos, FX product) {
        Hashtable h = new Hashtable();
        FXSettlePos sp = new FXSettlePos();
        CurrencyPair cp = product.getCurrencyPair();
        String pcur = product.getPrincipalCurrency();
        String qcur = product.getPrincipalCurrency();
        int pdig = CurrencyUtil.getRoundingUnit(pcur);
        int qdig = CurrencyUtil.getRoundingUnit(qcur);
        int size = openPos.size();
        for (int i = 0; i < size; i++) {
            TradeOpenQuantity oq = openPos.elementAt(i);
            sp._settleDate = oq.getSettleDate();
            FXSettlePos pos = (FXSettlePos) h.get(sp);
            if (pos == null) {
                pos = new FXSettlePos();
                pos._settleDate = oq.getSettleDate();
                h.put(pos, pos);
            }
            pos._primaryQuantity += oq.getQuantity();
            pos._primaryOpenQuantity += oq.getOpenQuantity();
            double rate = oq.getPrice();
            pos._quotingQuantity += (-oq.getQuantity()) * rate;
            pos._quotingOpenQuantity += (-oq.getOpenQuantity()) * rate;
        }
        ShowTableWindow ww = new ShowTableWindow();
        ww.setTitle("Position By Settle Date for " + product.getDescription());
        AppUtil.setCalypsoIcon(ww);
        JTable table = ww.getTable();
        TableModelUtil model = new TableModelUtil(5, h.size());
        model.setTo(table);
        model.setColumnName(0, "Settle Date");
        model.setColumnName(1, "Open Qty (" + product.getPrincipalCurrency() + ")");
        model.setColumnName(2, "Open Qty (" + product.getQuotingCurrency() + ")");
        model.setColumnName(3, "Qty (" + product.getPrincipalCurrency() + ")");
        model.setColumnName(4, "Qty (" + product.getQuotingCurrency() + ")");
        Enumeration en = h.keys();
        int row = 0;
        while (en.hasMoreElements()) {
            sp = (FXSettlePos) en.nextElement();
            model.setValueNoCheck(row, 0, sp._settleDate);
            double prim = CurrencyUtil.roundAmount(sp._primaryOpenQuantity, pcur);
            model.setValueNoCheck(row, 1, new SignedAmount(prim, pdig));
            double quot = CurrencyUtil.roundAmount(sp._quotingOpenQuantity, qcur);
            model.setValueNoCheck(row, 2, new SignedAmount(quot, qdig));
            prim = CurrencyUtil.roundAmount(sp._primaryQuantity, pcur);
            model.setValueNoCheck(row, 3, new SignedAmount(prim, pdig));
            quot = CurrencyUtil.roundAmount(sp._quotingQuantity, qcur);
            model.setValueNoCheck(row, 4, new SignedAmount(quot, qdig));
            row++;
        }
        int[] cols = new int[1];
        cols[0] = 0;
        model.sortByColumn(cols, true);
        model.refresh();
        TableUtil.fastAdjust(table);
        ww.setVisible(true);
    }

    void loadAndSetOpenPositions(PLPositionArray plpos, TradeFilter tf) throws Exception {
        if (plpos.isEmpty())
            return;
        // Instead of doing a Loop on the Entire scope of position_id, we use the TradeFilter to load the TradeOpenQuantity.
        // If the Trade Filter has a ExcludeInactive position on, we will gether too many Liquidated Positions.
        // TODO - we should try to add a subquery on the position_id which reflects the exclude
        TradeOpenQuantityArray openPos = DSConnection.getDefault().getRemoteLiquidation().getTradeOpenQuantity(TradeOpenQuantityCriteriaBuilder.create(tf).status().ne(LiquidableStatus.Canceled).and(TradeOpenQuantityCriteriaBuilder.create().openQuantity().ne(0).or(TradeOpenQuantityCriteriaBuilder.create().openRepoQuantity().ne(0).or(TradeOpenQuantityCriteriaBuilder.create().productFamily().eq("REALIZED")))));
        if (openPos == null || openPos.size() == 0)
            return;
        Hashtable<PLPosition.PLPositionKey, PLPosition> h = new Hashtable<PLPosition.PLPositionKey, PLPosition>();
        int size = plpos.size();
        for (int i = 0; i < size; i++) {
            PLPosition pl = plpos.elementAt(i);
            h.put(pl.getKey(), pl);
            pl.setOpenPositions(new TradeOpenQuantityArray());
        }
        size = openPos.size();
        for (int i = 0; i < openPos.size(); i++) {
            TradeOpenQuantity oq = openPos.elementAt(i);
            PLPosition pl = h.get(oq.getPLPositionKey());
            if (pl != null) {
                pl.getOpenPositions().add(oq);
            }
        }
        size = plpos.size();
    }

    void clearWS() {
        _allPLPositions = new PLPositionArray();
        _aggregatedPLPositions = new Hashtable();
        _liquidationBooks = new Hashtable();
        _PLPositions = new PLPositionArray();
        showPLPositions(new PLPositionArray());
    }

    private PLPositionArray getAggregatedPosition(TableModelPLPosition posModel, int row) {
        PLPosition plpos = (PLPosition) posModel.getClientProperty(row, PL_POSITION_KEY);
        String attributeName = (String) posModel.getClientProperty(row, PL_POSITION_AGG_ATTRIBUTE);
        int pid = plpos.getProductId();
        long liqAggId = plpos.getPositionAggregationId();
        LiquidationConfig liqConfig = plpos.getLiquidationConfig();
        String key = attributeName + pid + "_" + liqConfig + "_" + liqAggId;
        PLPositionArray aggPositions = _aggregatedPLPositions.get(key);
        if (aggPositions == null || aggPositions.size() < 1) {
            AppUtil.displayError("No Positions found for " + key, this);
        }
        return aggPositions;
    }

    public void displayTrade(TableModelPLPosition posModel, int row) {
        PLPositionArray aggPositions = getAggregatedPosition(posModel, row);
        PLPosition plpos = aggPositions.get(0);
        Trade trade = plpos.toTrade();
        trade.setStatus(Status.S_NONE);
        try {
            String productF = trade.getProductFamily();
            ShowTrade w = TradeUtil.getInstance(productF, trade.getProduct().getType());
            w.showTrade(trade);
            ((Frame) w).setVisible(true);
        } catch (Exception e) {
            Log.error(this, e);
        }
    // finally {
    // setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    // }
    }

    public void displayDetailPosition(TableModelPLPosition posModel, int row) {
        JDatetime datetime = getDatetime();
        int pos = posModel._util.getColumnPosition(COL_PRODUCT_ID);
        if (pos == -1)
            return;
        int pid = ((Integer) posModel.getValueAt(row, pos)).intValue();
        PLPositionArray aggPositions = getAggregatedPosition(posModel, row);
        // to do: code goes here.
        Product product = null;
        try {
            product = DSConnection.getDefault().getRemoteProduct().getProduct(pid);
        } catch (Exception e) {
            Log.error(this, e);
        }
        if (product == null)
            return;
        if (!(product instanceof Security)) {
            AppUtil.displayError("Can only display this position for Security Product", this);
            return;
        }
        ShowTableWindow ww = new ShowTableWindow();
        ww.setVisible(true);
        AppUtil.setCalypsoIcon(ww);
        JTable secPosList = ww.getTable();
        double faceValue = product.getPrincipal();
        ww.setTitle("Detail for Security" + product.getDescription());
        TableModelUtil model = new TableModelUtil(2, 6);
        model.setColumnName(0, "             ");
        model.setColumnName(1, "Position");
        model.setValueAt(0, 0, "Total Security");
        model.setValueAt(1, 0, "Trading");
        model.setValueAt(2, 0, "Security Lent");
        model.setValueAt(3, 0, "Security Borrowed");
        model.setValueAt(4, 0, "Collaterized Out");
        model.setValueAt(5, 0, "Collaterized In");
        JDatetime asOfDate = datetime;
        /*
			 * * Compute PLPositions figures
			 */
        double quantity = 0.;
        double repoedOutQuantity = 0.;
        double repoedInQuantity = 0.;
        double secBorrowedQuantity = 0.;
        double secLentQuantity = 0.;
        double total = 0.;
        for (int i = 0; i < aggPositions.size(); i++) {
            PLPosition plPos = aggPositions.get(i);
            quantity += plPos.getQuantity();
            repoedOutQuantity += plPos.getRepoedOutQuantity(asOfDate);
            repoedInQuantity += plPos.getRepoedInQuantity(asOfDate);
            secBorrowedQuantity += plPos.getSecurityBorrowedQuantity(asOfDate);
            secLentQuantity += plPos.getSecurityLentQuantity(asOfDate);
        }
        total = quantity + repoedOutQuantity + repoedInQuantity + secBorrowedQuantity + secLentQuantity;
        model.setValueAt(0, 1, Util.numberToString(RoundingMethod.roundNearest(total * faceValue, 2)));
        model.setValueAt(1, 1, Util.numberToString(RoundingMethod.roundNearest(quantity * faceValue, 2)));
        model.setValueAt(2, 1, Util.numberToString(RoundingMethod.roundNearest(secLentQuantity * faceValue, 2)));
        model.setValueAt(3, 1, Util.numberToString(RoundingMethod.roundNearest(secBorrowedQuantity * faceValue, 2)));
        model.setValueAt(4, 1, Util.numberToString(RoundingMethod.roundNearest(repoedOutQuantity * faceValue, 2)));
        model.setValueAt(5, 1, Util.numberToString(RoundingMethod.roundNearest(repoedInQuantity * faceValue, 2)));
        model.setTo(secPosList, true);
        model.refresh();
        TableUtil.fastAdjust(secPosList);
        ;
    }

    public void displayLiquidationConfig(TableModelPLPosition posModel, int row) {
        int pos = posModel._util.getColumnPosition(COL_PRODUCT_ID);
        if (pos == -1)
            return;
        int pid = ((Integer) posModel.getValueAt(row, pos)).intValue();
        PLPositionArray aggPositions = getAggregatedPosition(posModel, row);
        // to do: code goes here.
        Product product = null;
        try {
            product = DSConnection.getDefault().getRemoteProduct().getProduct(pid);
        } catch (Exception e) {
            Log.error(this, e);
        }
        if (product == null)
            return;
        PLPosition plpos = aggPositions.get(0);
        final LiquidationConfig liquidationConfig = posModel.getLiquidationConfig(row);
        LiquidationInfo config = BOCache.getLiquidationInfo(DSConnection.getDefault(), plpos);
        String aggrStr = "";
        try {
            if (plpos.getPositionAggregationId() != 0) {
                PositionAggregation aggregation = DSConnection.getDefault().getRemoteReferenceData().getPositionAggregation(// ### needs to be reviewed
                (int) plpos.getPositionAggregationId());
                aggrStr = aggregation.getDisplayString() + "\n";
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        String desc = "Product Type:" + plpos.getProduct().getType() + ".\n" + "Book: " + plpos.getBookId() + ".\nLiqConfig: " + liquidationConfig.getName() + ".\n" + aggrStr + ".\n" + "Liquidation Method: " + config.getLiquidationMethod() + ".\n" + "Comparator Method: " + config.getComparatorMethod() + ".\n" + "Value By Trade: " + config.getValueByTradeB() + ".\n";
        AppUtil.displayMessage(desc, this);
        return;
    }

    public void performManualLiquidation(TableModelPLPosition posModel, int[] rows) {
        String attributeType = _attributeName;
        if (!attributeType.equals("BookName")) {
            AppUtil.displayWarning("Aggregation should be BookName", this);
            return;
        }
        PLPositionArray positions = new PLPositionArray();
        for (int i = 0; i < rows.length; i++) {
            PLPosition position = getPLPosition(posModel, rows[i]);
            if (position != null)
                positions.add(position);
        }
        manualLiquidation(positions);
    }

    protected PLPosition getPLPosition(TableModelPLPosition posModel, int row) {
        int pos = posModel._util.getColumnPosition(COL_PRODUCT_ID);
        if (pos == -1)
            return null;
        int pid = ((Integer) posModel.getValueAt(row, pos)).intValue();
        PLPositionArray aggPositions = getAggregatedPosition(posModel, row);
        // to do: code goes here.
        Product product = null;
        try {
            product = DSConnection.getDefault().getRemoteProduct().getProduct(pid);
        } catch (Exception e) {
            Log.error(this, e);
        }
        if (product == null)
            return null;
        return aggPositions.get(0);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    void add2PendingPositions(PLPosition position) {
        synchronized (_pendingLock) {
            _pendingPositions.addElement(position);
            if (_pendingPositions.size() > MAX_PENDING_POSITIONS) {
                _pendingPositions.removeAllElements();
            }
        }
    }

    void setRefreshTimeout() {
        String value = AppUtil.getUserProperty(REFRESH_STR);
        value = AppUtil.chooseValue("Auto Refresh Frequency(sec)", null, value, true, this);
        if (value == null || value.length() == 0)
            return;
        int value_ = -1;
        try {
            value_ = Integer.parseInt(value);
        } catch (Exception e) {
            Log.error(this, e);
        }
        if (value_ == -1) {
            AppUtil.displayWarning("Invalid value " + value, this);
            return;
        }
        AppUtil.setUserProperty(REFRESH_STR, value);
        startTimer();
    }

    void startTimer() {
        String s = AppUtil.getUserProperty(REFRESH_STR);
        if (s != null)
            _refreshTimeout = Integer.parseInt(s);
        else
            _refreshTimeout = 0;
        if (_timer != null)
            _timer.stop();
        if (_refreshTimeout == 0)
            return;
        _timer = new com.calypso.tk.util.Timer(this, _refreshTimeout * 1000);
        _timer.start();
    }

    void stopTimer() {
        if (_timer != null)
            _timer.stop();
        _timer = null;
    }

    public void timerRun() {
        Runnable r = new Runnable() {

            public void run() {
                handleTimeout();
            }
        };
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (Exception e) {
            Log.error(this, e);
        }
    }

    synchronized void handleTimeout() {
        handlePendings();
    }

    void handlePendings() {
        synchronized (_pendingLock) {
            if (_pendingPositions.size() == 0)
                return;
            for (int i = 0; i < _pendingPositions.size(); i++) {
                newPLPosition((PLPosition) _pendingPositions.elementAt(i));
            }
            _pendingPositions.removeAllElements();
        }
    }

    protected void manualLiquidation(PLPosition plPosition) {
        PLPositionArray positions = new PLPositionArray();
        positions.add(plPosition);
        manualLiquidation(positions);
    }

    protected void manualLiquidation(PLPositionArray positions) {
        for (int i = positions.size() - 1; i >= 0; i--) {
            PLPosition position = positions.get(i);
            LiquidationInfo info = BOCache.getLiquidationInfo(DSConnection.getDefault(), position);
            if (info == null) {
                positions.remove(i);
                continue;
            }
        }
        if (positions.size() == 0)
            return;
        // CAL-153617 we need to clone the positions as manual liquidation always rebuilds them as of the current time.
        boolean realTime = realTimeChangeCheck.isSelected();
        if (!realTime) {
        }
        ManualLiquidationJDialog ml = new ManualLiquidationJDialog(this);
        ml.setVisible(ml.checkValidPositions(positions, new JDatetime(), getPricingEnv()));
    }

    void productDescButton_actionPerformed(java.awt.event.ActionEvent event) {
        Vector v = null;
        Product p = null;
        v = new Vector();
        // Bug#: 15092
        v = getOnlySecondaryMarketProduct(false);
        p = ProductUtil.getProduct(this, v);
        productDescText(p);
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    void JLabel3_mouseClicked(java.awt.event.MouseEvent event) {
        // to do: code goes here.
        if (event.getClickCount() < 2)
            return;
        productDescText(null);
    }

    void productDescText(Product product) {
        _product = null;
        productDescText.setText("");
        if (product == null) {
            // loadButton.setForeground(java.awt.Color.yellow);
            // loadButton.setEnabled(true);
            AppUtil.attachIconToButton(loadButton, true);
            return;
        }
        int id = product.getId();
        try {
            product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), id);
        } catch (Exception e) {
            AppUtil.displayError("Error accessing Product " + id, this);
        }
        if (product == null) {
            AppUtil.displayError("No Product " + id + " found.", this);
            return;
        }
        _product = product;
        productDescText.setText(_product.getDescription());
        // loadButton.setForeground(java.awt.Color.yellow);
        // loadButton.setEnabled(true);
        AppUtil.attachIconToButton(loadButton, true);
    }

    protected void ensureProductsLoaded(TradeFilter filter) throws PersistenceException {
        BOCache.getProductsFromPLPosition(DSConnection.getDefault(), filter, true);
    }

    private boolean filterContainsProductIdOrISIN(TradeFilter filter) {
        TradeFilterCriterion c;
        for (int i = 0; i < filter.getCriterions().size(); i++) {
            c = (TradeFilterCriterion) filter.getCriterions().elementAt(i);
            if (c.getName().equals(TradeFilter.PRODUCT_ID) && c.getIsInB()) {
                return true;
            } else if ((TradeFilter.SEC_CODE + "ISIN").equals(c.getName()) && c.getIsInB()) {
                return true;
            }
        }
        return false;
    }

    protected Product getInstance(String type) {
        if (type.endsWith("Holding"))
            type = "Holding";
        String className = "tk.product." + type;
        try {
            Product instance = (Product) InstantiateUtil.getInstance(className);
            return instance;
        } catch (Exception e) {
            Log.warn("ProductSQL", e);
        }
        return null;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    class SettlePos {

        JDate _settleDate;

        double _quantity;

        double _openQuantity;

        @Override
        public int hashCode() {
            return _settleDate.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return _settleDate.equals(((SettlePos) o)._settleDate);
        }
    }

    class FXSettlePos {

        JDate _settleDate;

        double _primaryQuantity;

        double _primaryOpenQuantity;

        double _quotingQuantity;

        double _quotingOpenQuantity;

        @Override
        public int hashCode() {
            return _settleDate.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return _settleDate.equals(((FXSettlePos) o)._settleDate);
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Real time update...
    void initRealTime() {
        frequencyText.setText("600");
        AppUtil.addNumberListener(frequencyText, 0);
        makeTextPaneWrappable();
        _document = (DefaultStyledDocument) realtimeText.getDocument();
    }

    protected void enableRealTime(boolean enabled) {
        JLabel8.setEnabled(enabled);
        JLabel5.setEnabled(enabled);
        marketDataCheckBox.setEnabled(enabled);
        quoteCheckBox.setEnabled(enabled);
        frequencyText.setEnabled(enabled);
        realtimeText.setEnabled(enabled);
        if (!enabled) {
            marketDataCheckBox.setSelected(false);
            quoteCheckBox.setSelected(false);
        }
    }

    void makeTextPaneWrappable() {
        JScrollPane1.getViewport().remove(realtimeText);
        realtimeText = new JTextPane(new DefaultStyledDocument(_styleContext)) {

            @Override
            public boolean getScrollableTracksViewportWidth() {
                Component parent = getParent();
                ComponentUI ui = getUI();
                return (parent != null ? (ui.getPreferredSize(this).width <= parent.getSize().width) : true);
            }
        };
        JScrollPane1.getViewport().add(realtimeText);
        realtimeText.setEditable(false);
        realtimeText.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() != 2)
                    return;
                if (AppUtil.displayQuestion("Clear real-time text area?", PositionKeeper.this))
                    realtimeText.setText("");
            }
        });
    }

    class SymFocus extends java.awt.event.FocusAdapter {

        @Override
        public void focusLost(java.awt.event.FocusEvent event) {
            Object object = event.getSource();
            if (object == frequencyText)
                frequencyText_focusLost(event);
        }
    }

    void frequencyChanged() {
        try {
            int previous = _frequency;
            String s = frequencyText.getText();
            if (Util.isEmptyString(s))
                _frequency = -1;
            else
                _frequency = Integer.parseInt(frequencyText.getText());
            if (previous == _frequency)
                return;
        } catch (Throwable x) {
            Log.error(Log.GUI, x);
            _frequency = DEFAULT_FREQUENCY;
            frequencyText.setText(Integer.toString(_frequency));
        }
        startRealTimeUpdateTimer();
        addRealTimeText("Change update frequency to " + frequencyText.getText() + " secs.", 4);
    }

    void frequencyText_actionPerformed(java.awt.event.ActionEvent event) {
        frequencyChanged();
    }

    void frequencyText_focusLost(java.awt.event.FocusEvent event) {
        frequencyChanged();
    }

    void marketDataCheckBox_actionPerformed(java.awt.event.ActionEvent event) {
        updateSubscribe(marketDataCheckBox);
    }

    void quoteCheckBox_actionPerformed(java.awt.event.ActionEvent event) {
        updateSubscribe(quoteCheckBox);
    }

    void updateSubscribe(JCheckBox caller) {
        if (_ps == null)
            return;
        boolean startb = (marketDataCheckBox.isSelected() || quoteCheckBox.isSelected());
        if (!startb)
            addRealTimeText("Unsubscribing to all update events...", 4);
        if (_realTimeUpdateTimer != null) {
            try {
                _realTimeUpdateTimer.interrupt();
            } catch (Exception ex) {
            }
            _realTimeUpdateTimer = null;
        }
        if (startb) {
            initRealTimeUpdateTrades();
            startRealTimeUpdateTimer();
            try {
                // we were unsubscribing and re-subscribing
                // during which events would not be delivered, this is
                // same as rebuilding the session which JMS will support
                _ps.stop();
                _eventClassNames.clear();
                // Mandatories
                _eventClassNames.add(PSEventPLPosition.class);
                _eventClassNames.add(PSEventAggPLPosition.class);
                _eventClassNames.add(PSEventQuote.class);
                if (quoteCheckBox.isSelected()) {
                    if (caller == quoteCheckBox) {
                        addRealTimeText("Subscribing to Quote updates...", 4);
                    }
                } else if (caller == quoteCheckBox) {
                    addRealTimeText("Unsubscribing to Quote updates...", 4);
                }
                if (marketDataCheckBox.isSelected()) {
                    _eventClassNames.add(PSEventMarketDataChange.class);
                    if (caller == marketDataCheckBox) {
                        addRealTimeText("Subscribing to MarketData updates...", 4);
                    }
                } else if (caller == marketDataCheckBox) {
                    addRealTimeText("Unsubscribing to MarketData updates...", 4);
                }
                _ps = ESStarter.startConnection(this, _eventClassNames);
                _ps.start();
            } catch (Exception ex) {
                Log.error(Log.EXCEPTION, "Error while rebuilding subscriptions", ex);
            }
        }
    }

    void startRealTimeUpdateTimer() {
        if (_realTimeUpdateTimer != null) {
            try {
                _realTimeUpdateTimer.interrupt();
            } catch (Exception ex) {
            }
            _realTimeUpdateTimer = null;
        }
        String s = frequencyText.getText();
        try {
            if (Util.isEmptyString(s))
                _frequency = -1;
            else
                _frequency = Integer.parseInt(frequencyText.getText());
        } catch (Throwable x) {
            Log.error(Log.GUI, x);
            _frequency = DEFAULT_FREQUENCY;
            frequencyText.setText(Integer.toString(_frequency));
        }
        if (_frequency > 0) {
            TimerRunnable r = new TimerRunnable() {

                public void timerRun() {
                    Runnable rs = new Runnable() {

                        public void run() {
                            doRealTimeUpdate();
                        }
                    };
                    SwingUtilities.invokeLater(rs);
                }
            };
            _realTimeUpdateTimer = new com.calypso.tk.util.Timer(r, _frequency * 1000);
            _realTimeUpdateTimer.start();
        }
    }

    void doRealTimeUpdate() {
        if (_realTimeUpdateRunning) {
            addRealTimeText("Timer schedule Already Running (skip it)\n", 0);
            return;
        }
        try {
            _realTimeUpdateRunning = true;
            // do incremental update
            runIncrementalChanges();
        } catch (Exception ex) {
            Log.error(this, ex);
        } finally {
            _realTimeUpdateRunning = false;
        }
    }

    static Style STYLE_0 = null;

    static Style STYLE_1 = null;

    static Style STYLE_2 = null;

    static Style STYLE_3 = null;

    static Style STYLE_4 = null;

    void addRealTimeText(String info, int style) {
        if (STYLE_0 == null) {
            STYLE_0 = _styleContext.addStyle(null, null);
            StyleConstants.setBold(STYLE_0, true);
            StyleConstants.setFontSize(STYLE_0, 12);
            StyleConstants.setFontFamily(STYLE_0, "Dialog");
            STYLE_1 = _styleContext.addStyle(null, null);
            StyleConstants.setBold(STYLE_1, true);
            StyleConstants.setFontSize(STYLE_1, 12);
            StyleConstants.setFontFamily(STYLE_1, "Dialog");
            StyleConstants.setForeground(STYLE_1, Color.blue);
            STYLE_2 = _styleContext.addStyle(null, null);
            StyleConstants.setBold(STYLE_2, true);
            StyleConstants.setFontSize(STYLE_2, 12);
            StyleConstants.setFontFamily(STYLE_2, "Dialog");
            StyleConstants.setForeground(STYLE_2, Color.magenta);
            STYLE_3 = _styleContext.addStyle(null, null);
            StyleConstants.setBold(STYLE_3, true);
            StyleConstants.setFontSize(STYLE_3, 12);
            StyleConstants.setFontFamily(STYLE_3, "Dialog");
            StyleConstants.setForeground(STYLE_3, Color.orange);
            STYLE_4 = _styleContext.addStyle(null, null);
            StyleConstants.setItalic(STYLE_4, true);
            StyleConstants.setFontSize(STYLE_4, 10);
            StyleConstants.setFontFamily(STYLE_4, "Dialog");
            // gray
            StyleConstants.setForeground(STYLE_4, Color.black);
        }
        try {
            Style st = STYLE_0;
            switch(style) {
                case 1:
                    st = STYLE_1;
                    break;
                case 2:
                    st = STYLE_2;
                    break;
                case 3:
                    st = STYLE_3;
                    break;
                case 4:
                    st = STYLE_4;
                    break;
            }
            int length = _document.getLength();
            if (length >= 100000) {
                // ~100k
                realtimeText.setText("");
                length = 0;
            }
            _document.insertString(length, info + "\n", st);
        } catch (Exception x) {
            Log.error(Log.GUI, x);
        }
    }

    void initRealTimeUpdateTrades() {
        try {
            _frequency = Integer.parseInt(frequencyText.getText());
        } catch (Throwable x) {
            Log.error(Log.GUI, x);
            _frequency = DEFAULT_FREQUENCY;
            frequencyText.setText(Integer.toString(_frequency));
        }
        _quotes2Trades = new HashMap();
        _marketData2Trades = new HashMap();
        _realTimeTrades = new HashMap();
        if (!realTimeChangeCheck.isSelected() || !marketDataCheckBox.isSelected())
            return;
        Monitor monitor = LogHelper.startWithCategory("PositionKeeper", "Init MarketData");
        Vector tmp = new Vector();
        Hashtable h = new Hashtable();
        JDate valDate = _env.getJDate(getValDate());
        TradeArray trades = getTradesFromPosition();
        for (int i = 0; i < trades.size(); i++) {
            Trade trade = trades.get(i);
            _realTimeTrades.put(Long.valueOf(trade.getLongId()), trade);
            tmp.clear();
            tmp.add(trade);
            h.clear();
            _env.getPricerConfig().getQuotes(tmp, _env, valDate, h);
            Enumeration e = h.keys();
            while (e.hasMoreElements()) {
                QuoteValue qv = (QuoteValue) e.nextElement();
                add2Hash(qv, trade, _quotes2Trades);
            }
            h.clear();
            _env.getPricerConfig().getMarketDataItems(tmp, _env, valDate, h);
            e = h.keys();
            while (e.hasMoreElements()) {
                MarketDataItem item = (MarketDataItem) e.nextElement();
                add2Hash(Integer.valueOf(item.getId()), trade, _marketData2Trades);
            }
        }
        monitor.done();
    }

    void add2Hash(Object key, Trade trade, HashMap h) {
        Set v = (Set) h.get(key);
        if (v == null) {
            v = new HashSet();
            h.put(key, v);
        }
        if (!v.contains(trade))
            v.add(trade);
    }

    void handleRealTimeUpdateEvent(PSEvent ev, boolean handleNewQuote) {
        if (ev instanceof PSEventPLPosition) {
            boolean startb = (marketDataCheckBox.isSelected() || quoteCheckBox.isSelected());
            if (startb) {
                // add trade in list if not added
                PLPosition p = ((PSEventPLPosition) ev).getPLPosition();
                Trade trade = p.toTrade();
                synchronized (_realTimeTrades) {
                    if (_dummyTrades.get(p) == null) {
                        int size = _dummyTrades.size();
                        _dummyTrades.put(p, Integer.valueOf(-(size + 1)));
                        trade.setLongId(-(size + 1));
                        _realTimeTrades.put(Long.valueOf(trade.getLongId()), trade);
                    }
                }
            }
            return;
        }
        if (quoteCheckBox.isSelected() && ev instanceof PSEventQuote) {
            synchronized (_realTimeTrades) {
                if (!handleNewQuote)
                    return;
                QuoteValue[] q = ((PSEventQuote) ev).getQuotes();
                if (q != null) {
                    for (int i = 0; i < q.length; i++) {
                        QuoteValue qv = q[i];
                        Set trades = (Set) _quotes2Trades.get(qv);
                        if (trades == null || trades.size() == 0)
                            return;
                        addRealTimeText("Quote Changed " + qv + " Affecting " + trades.size() + " Trades", 2);
                        Iterator iter = trades.iterator();
                        while (iter.hasNext()) {
                            Object k = iter.next();
                            _modifiedTrades.put(k, k);
                        }
                    }
                } else {
                    QuoteValue qv = ((PSEventQuote) ev).getQuote();
                    Set trades = (Set) _quotes2Trades.get(qv);
                    if (trades == null || trades.size() == 0)
                        return;
                    addRealTimeText("Quote Changed " + qv + " Affecting " + trades.size() + " Trades", 2);
                    Iterator iter = trades.iterator();
                    while (iter.hasNext()) {
                        Object k = iter.next();
                        _modifiedTrades.put(k, k);
                    }
                }
                return;
            }
        }
        if (realTimeChangeCheck.isSelected() && marketDataCheckBox.isSelected() && ev instanceof PSEventMarketDataChange) {
            MarketDataItem item = ((PSEventMarketDataChange) ev).getValue();
            synchronized (_realTimeTrades) {
                // replace inside PricingEnv
                Set trades = (Set) _marketData2Trades.get(Integer.valueOf(item.getId()));
                int nbtrades = trades != null ? trades.size() : 0;
                if (trades == null || trades.size() == 0)
                    return;
                MarketDataItem old = _env.getPricerConfig().get(Integer.valueOf(item.getId()));
                if (old == null)
                    return;
                if (item.getDatetime().before(old.getDatetime()))
                    return;
                _env.getPricerConfig().replace(old, item);
                addRealTimeText("MarketDataItem  Changed " + item.getId() + "/" + item.getType() + "/" + item.getName() + "/" + item.getCurrency() + "/" + item.getDatetime() + " Affecting " + trades.size() + " Trades", 3);
                Iterator iter = trades.iterator();
                while (iter.hasNext()) {
                    Object k = iter.next();
                    _modifiedTrades.put(k, k);
                }
                return;
            }
        }
    }

    TradeArray getTradesFromPosition() {
        TradeArray trades = new TradeArray();
        TradeArray tempTrades = new TradeArray();
        if (_allPLPositions == null)
            return trades;
        // Vector tradeIds = new Vector();
        HashSet tradeIds = new HashSet();
        for (int i = 0; i < _allPLPositions.size(); i++) {
            PLPosition plPosition = _allPLPositions.elementAt(i);
            Trade myTrade = plPosition.toTrade();
            myTrade.setLongId(-(i + 1));
            _dummyTrades.put(plPosition, Integer.valueOf(i));
            trades.add(myTrade);
        }
        return trades;
    }

    void runIncrementalChanges() {
        synchronized (_realTimeTrades) {
            if (_modifiedTrades.size() == 0) {
                addRealTimeText("Nothing changed: " + (new JDatetime()) + "...", 4);
                return;
            }
            addRealTimeText("Doing Incremental change at  " + (new JDatetime()) + " For " + _modifiedTrades.size() + " trades", 0);
            // _quotes2Trades.clear();
            // _marketData2Trades.clear();
            Hashtable hbooks = new Hashtable();
            Hashtable keys = new Hashtable();
            StringBuffer buffer = new StringBuffer();
            Iterator iter = _modifiedTrades.keySet().iterator();
            while (iter.hasNext()) {
                Trade trade = (Trade) iter.next();
                hbooks.put(trade.getBook(), trade.getBook());
                buffer.setLength(0);
                buffer.append(trade.getBook().getId()).append(" ").append(trade.getProduct().getId());
                String key = buffer.toString();
                keys.put(key, key);
            }
            int count = 0;
            Vector exceptions = new Vector();
            try {
                Vector books = Util.toVector(hbooks);
                PLPositionArray posV = DSConnection.getDefault().getRemoteTrade().getPLPositions(books, null);
                // To be done: should also update the pending positions array.
                if (posV != null && posV.size() > 0) {
                    for (int i = 0; i < posV.size(); i++) {
                        PLPosition p = posV.get(i);
                        buffer.setLength(0);
                        buffer.append(p.getKey());
                        if (keys.get(buffer.toString()) != null) {
                            count++;
                            newPLPosition(posV.get(i));
                        }
                    }
                }
            } catch (Throwable x) {
                exceptions.add(x);
                Log.error(Log.GUI, x);
            }
            for (int i = 0; i < exceptions.size(); i++) {
                Throwable x = (Throwable) exceptions.get(i);
                addRealTimeText("Exception: " + x.getMessage(), 3);
                addRealTimeText(Log.exceptionToString(x), 4);
            }
            addRealTimeText(Integer.toString(count) + " position(s) updated.", 0);
            _modifiedTrades.clear();
        }
    }

    void clearButton_actionPerformed(java.awt.event.ActionEvent event) {
        clearWS();
    }

    public Vector getPLPositionTables() {
        Vector v = new Vector();
        if (_products != null) {
            for (int i = 0; i < _products.size(); i++) {
                TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
                TableModelPLPosition model = ppl._plPositionModel;
                v.addElement(model);
            }
        }
        return v;
    }

    // Allows to set a single Tab with required column names for application
    // trying to use PositionKeeper
    public void setConfigurableSingleTab(Vector columnNames) {
        Vector tabs = new Vector();
        Hashtable tabBooks = new Hashtable();
        Hashtable tabProducts = new Hashtable();
        Hashtable tabContracts = new Hashtable();
        Hashtable tabColumns = new Hashtable();
        Hashtable tabFixedColumns = new Hashtable();
        Hashtable tabFilterName = new Hashtable();
        Hashtable tabOpenTrades = new Hashtable();
        // init defaults
        Vector pt = null;
        Vector contracts = null;
        try {
            pt = getOnlySecondaryMarketProduct(false);
            contracts = DSConnection.getDefault().getRemoteProduct().getContractNames();
        } catch (Exception e) {
            Log.error(this, e);
            return;
        }
        if (pt == null)
            return;
        Vector bn = new Vector();
        bn.add(ANY_BOOK_NAME);
        tabs.add("All");
        tabBooks.put("All", bn);
        tabProducts.put("All", pt);
        tabContracts.put("All", contracts);
        tabColumns.put("All", columnNames);
        PositionConfigTabs.saveTabs(PK_NAME, null, getAllColumnNames(), tabs, tabBooks, tabProducts, tabContracts, tabColumns, tabFixedColumns, tabFilterName, tabOpenTrades);
        removeAllPLPositionTabs();
        // Create tabs
        for (int i = 0; i < tabs.size(); i++) {
            String tabName = (String) tabs.get(i);
            int nbColumns = 0;
            boolean ot = false;
            TabPLPosition p = new TabPLPosition(tabName, 0, columnNames, columnNames.size(), ot);
            p._productTypeList = pt;
            p._contractList = contracts;
            if (bn != null) {
                for (int j = 0; j < bn.size(); j++) {
                    String book = (String) bn.elementAt(j);
                    int id = PositionConfigTabs.getBookId(book);
                    p._bookIdList.addElement(Integer.valueOf(id));
                }
            }
            _products.addElement(p);
        }
    }

    // Allows to set a single Tab TableModel for application trying to use
    // PositionKeeper
    public void setSingleTabModel(TableModelUtil model) {
        Vector columnNames = new Vector();
        for (int i = 0; i < model.getColumnCount(); i++) {
            columnNames.add(model.getColumnName(i));
        }
        setConfigurableSingleTab(columnNames);
        TabPLPosition tab = (TabPLPosition) _products.get(0);
        TableModelPLPosition plpositions = tab._plPositionModel;
        plpositions.reinitRows(model.getRowCount());
        for (int i = 0; i < model.getRowCount(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                plpositions.setValueNoCheck(model.getValueAt(i, j), i, j);
            }
        }
        tab.recreateScrollPane();
    }

    protected void computePreviousValues(PLPositionArray previousPositions, Hashtable<PLPosition, Double> cache, PricingEnv previousEnv, JDatetime previousDatetime) {
        double unreal = 0.;
        int size = previousPositions.size();
        for (int i = 0; i < size; i++) {
            PLPosition tmpPos = previousPositions.elementAt(i);
            unreal = 0;
            try {
                Product p = tmpPos.getProduct();
                if (p != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(tmpPos), CommodityForward.COMMODITY_FORWARD)) {
                    unreal = CommodityUtil.computeUnrealize(tmpPos, previousEnv, previousDatetime);
                } else {
                    unreal = tmpPos.getUnrealized(previousEnv, previousDatetime);
                }
            } catch (Exception e) {
                Log.error(this, e);
            }
            cache.put(tmpPos, Double.valueOf(unreal));
        }
    }

    public void setPricingEnvName(String envName) {
        pricingEnvChoice.setSelectedItem(envName);
    }

    public void setTradeFilterName(String filterName) {
        portfolioChoice.calypsoSetSelectedItem(filterName);
    }

    private String getPositionAggregationName(int liqAggId) {
        PositionAggregation agg = BOCache.getPositionAggregation(DSConnection.getDefault(), liqAggId);
        if (agg != null) {
            int configId = agg.getConfigId();
            if (configId == 0)
                return null;
            PositionAggregationConfig config = BOCache.getPositionAggregationConfig(DSConnection.getDefault(), configId);
            if (config != null)
                return config.getName();
        }
        return null;
    }

    /**
     * Return a boolean to indicate whether the trade filter uses
     * liquidation aggregation
     *
     * @return a boolean
     */
    private boolean isUsingLiqAggregation() {
        if (_portfolio != null) {
            PositionSpec pSpec = _portfolio.getPositionSpec();
            if (pSpec == null) {
                return false;
            } else {
                if (pSpec.getPositionAggregationConfigId() == 0)
                    return false;
                else
                    return true;
            }
        }
        return false;
    }

    private boolean isUsingLiqAggOrConfig() {
        boolean useLiqAgg = false;
        if (_portfolio != null) {
            PositionSpec pSpec = _portfolio.getPositionSpec();
            if (pSpec != null && (pSpec.getPositionAggregationConfigId() != 0 || pSpec.getLiquidationConfig() != null))
                useLiqAgg = true;
        }
        return useLiqAgg;
    }

    private boolean isUsingLiqConfig() {
        if (_portfolio != null) {
            PositionSpec pSpec = _portfolio.getPositionSpec();
            if (pSpec == null) {
                return false;
            }
            if (pSpec.getLiquidationConfig() == null)
                return false;
            else
                return true;
        }
        return false;
    }

    /**
     * Return liquidation aggregation id from trade filter if trade filter
     * uses poistion spec and the spec uses position aggregation. For all
     * other cases, 0 is returned.
     *
     * @return an int of lquidation aggregation id
     */
    private int getLiqAggId() {
        int liqAggId = 0;
        PositionSpec positionSpec = _portfolio.getPositionSpec();
        if (positionSpec != null)
            liqAggId = positionSpec.getPositionAggregationConfigId();
        return liqAggId;
    }

    private LiquidationConfig getLiqConfig() {
        LiquidationConfig liqConfig = LiquidationConfig.getDEFAULT();
        PositionSpec positionSpec = _portfolio.getPositionSpec();
        if (positionSpec != null)
            liqConfig = positionSpec.getLiquidationConfig();
        return liqConfig;
    }

    private int getLiqConfigId() {
        return getLiqConfig().getId();
    }

    private boolean isAllBookAvailable() {
        if (!AccessUtil.fullAccessB()) {
            User user = AccessUtil.getUser();
            if (user != null) {
                Vector groups = user.getGroups();
                if (groups == null)
                    return false;
                for (int j = 0; j < groups.size(); j++) {
                    String group = (String) groups.elementAt(j);
                    if (AccessUtil.isAllBookAvailable(group))
                        return true;
                }
            }
            return false;
        }
        return true;
    }

    private Hashtable getBookMap() {
        Vector books = AccessUtil.getAllBookNames();
        if (Util.isEmpty(books))
            return new Hashtable();
        return Util.toHashtable(books);
    }

    // bulk-load liquidated positions and set them on the PL positions passed in
    private void loadAllLiquidatedPositions(PLPositionArray plPositions, JDatetime datetime, TradeFilter tf) throws CalypsoServiceException {
        Set<Long> posIds = new HashSet<Long>();
        Map<Long, PLPosition> posIdMap = new TreeMap<Long, PLPosition>();
        // build list of posIds
        for (int i = 0; i < plPositions.size(); i++) {
            PLPosition pl = plPositions.get(i);
            Long posId = Long.valueOf(pl.getPositionLongId());
            posIds.add(posId);
            // add to map for future retrieval
            posIdMap.put(posId, pl);
            // initialize liqArray on each pl
            LiquidatedPositionArray plLiqArray = pl.getLiqArray();
            if (plLiqArray == null)
                pl.setLiqArray(null);
            else
                plLiqArray.clear();
        }
        // Instead of doing a Loop on the Entire scope of position_id, we use the TradeFilter to load the LiquidatedPosition.
        // If the Trade Filter has a ExcludeInactive position on, we will gether too many Liquidated Positions.
        // TODO - we should try to add a subquery on the position_id which reflects the exclude
        List<LiquidatedPosition> liqPos = DefaultLiquidatedPositionLoader.create().load(LiquidatedPositionCriteriaBuilder.create(tf).deleted(false).orderForUndo());
        // set liquidated position on the corresponding PL position
        for (LiquidatedPosition lp : liqPos) {
            // corresponding PL position
            PLPosition pl = posIdMap.get(lp.getPositionLongId());
            if (pl != null) {
                LiquidatedPositionArray plLiqArray = pl.getLiqArray();
                if (plLiqArray != null) {
                    plLiqArray.add(lp);
                }
            }
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    PricingEnv _env;

    PricingEnv _previousEnv;

    String _attributeName;

    int _excludeZeroPositions;

    double _zeroPositionTolerance;

    Hashtable<String, PLPositionArray> _aggregatedPLPositions;

    Vector _books;

    BookHierarchyNode _selectedBookHierarchyNode;

    Hashtable _liquidationBooks;

    protected TradeFilter _portfolio;

    private String _user;

    private String _passwd;

    private String _hostName;

    protected Product _product;

    protected List<Class> _eventClassNames;

    protected PSConnection _ps;

    protected PLPositionArray _PLPositions;

    protected BookHierarchy _orgStructure;

    protected int _esPort;

    protected String _esHost;

    protected PLPositionArray _allPLPositions;

    protected PLPositionArray _previousPLPositions;

    protected Hashtable _previousPLPositionsHash;

    static final String POSITION_KEEPER_HIERARCHY = "PositionKeeperHierarchy";

    static final String PL_POSITION_KEY = "PLPosition";

    static final String PL_POSITION_AGG_ATTRIBUTE = "PLPositionAggAttribute";

    protected boolean _isAllBookAvailable = true;

    protected Hashtable _availBooks = new Hashtable();

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    PositionConfigTabs _pkConfigDialog = null;

    Vector _products = new Vector();

    Vector _allColumnNames = null;

    Vector _columnNamesUsed = null;

    boolean _includePreviousPL = false;

    boolean _useOptimizeLoad = true;

    QuoteJFrame _quoteWindow;

    MarketDataJFrame _marketdataFrame;

    Vector _preloadedTypes = null;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Pending positions
    static final String REFRESH_STR = "PKRefreshTimer";

    static int MAX_PENDING_POSITIONS = 1000;

    com.calypso.tk.util.Timer _timer = null;

    int _refreshTimeout = 0;

    Vector _pendingPositions = new Vector();

    Object _pendingLock = new Object();

    boolean _realTime = true;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Realtime update...
    StyleContext _styleContext = new StyleContext();

    DefaultStyledDocument _document = null;

    com.calypso.tk.util.Timer _realTimeUpdateTimer = null;

    int _frequency = DEFAULT_FREQUENCY;

    boolean _realTimeUpdateRunning = false;

    HashMap _quotes2Trades;

    HashMap _marketData2Trades;

    HashMap<Long, Trade> _realTimeTrades;

    HashMap _modifiedTrades = new HashMap();

    Hashtable<PLPosition, Double> _previousPL = new Hashtable<PLPosition, Double>();

    Hashtable _dummyTrades = new Hashtable();

    static final int DEFAULT_FREQUENCY = 600;
}
