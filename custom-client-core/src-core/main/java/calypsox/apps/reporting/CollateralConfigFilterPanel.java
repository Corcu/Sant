package calypsox.apps.reporting;

import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.LegalEntityTextPanel;
import com.calypso.tk.collateral.impl.MarginCallConfigHelper;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.sql.CollateralConfigDesc;
import com.calypso.tk.report.ReportTemplate;
import com.jidesoft.swing.JideSwingUtilities;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * @author Unknown
 */

public class CollateralConfigFilterPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JLabel contractTypeLabel = null;
    private JTextField contractTypeTextField;
    private JButton contractTypeButton;
    private JLabel contractGroupLabel = null;
    private JTextField contractGroupTextField;
    private JButton contractGroupButton;
    private JLabel contractLabel = null;
    private JTextField contractTextField;
    private JButton contractButton;
    private JLabel statusLabel = null;
    private JTextField statusTextField;
    private JButton statusButton;
    private JCheckBox hideInactiveEGBox = null;
    private LegalEntityTextPanel processingOrgPanel;
    private LegalEntityTextPanel counterPartyPanel;
    private DefaultActionListener defaultActionListener = null;

    private JCheckBox getHideInactiveEGBox() {
        if (this.hideInactiveEGBox == null) {// 193
            this.hideInactiveEGBox = new JCheckBox("Hide Inactive Exposure Group");// 194
            this.hideInactiveEGBox.setBounds(314, 276, 250, 24);// 195
        }

        return this.hideInactiveEGBox;// 197
    }


    private List<String> getRepartitionChoices() {
        List<String> result = new ArrayList();// 253
        result.add("");// 254
        result.add("<=");// 255
        result.add("<");// 256
        return result;// 257
    }

    public CollateralConfigFilterPanel() {
        this.setLayout((LayoutManager)null);// 261

        this.add(this.getContractTypeLabel());// 272
        this.add(this.getContractTypeTextField());// 273
        this.add(this.getContractTypeButton());// 274
        this.add(this.getContractLabel());// 283
        this.add(this.getContractTextField());// 284
        this.add(this.getContractButton());// 285
        this.add(this.getContractGroupLabel());// 291
        this.add(this.getContractGroupTextField());// 292
        this.add(this.getContractGroupButton());// 293
        this.add(this.getStatusLabel());// 303
        this.add(this.getStatusTextField());// 304
        this.add(this.getStatusButton());// 305
        this.add(this.getProcessingOrgPanel());// 307
        this.add(this.getCounterPartyPanel());// 308
        this.add(this.getHideInactiveEGBox());// 317
        this.setBorder(new TitledBorder(new EtchedBorder(1, (Color)null, (Color)null), "MarginCall Contract", 4, 2, (Font)null, (Color)null));// 319
        this.initDomain();// 323
    }// 324

    private void initDomain() {

    }


    private JLabel getContractTypeLabel() {
        if (this.contractTypeLabel == null) {// 447
            this.contractTypeLabel = new JLabel("Contract Types :");// 448
            this.contractTypeLabel.setBounds(314, 106, 109, 24);// 449
        }

        return this.contractTypeLabel;// 451
    }

    private JTextField getContractTypeTextField() {
        if (this.contractTypeTextField == null) {// 455
            this.contractTypeTextField = new JTextField();// 456
            this.contractTypeTextField.setEditable(false);// 457
            this.contractTypeTextField.setBounds(426, 106, 120, 24);// 458
        }

        return this.contractTypeTextField;// 460
    }

    private JButton getContractTypeButton() {
        if (this.contractTypeButton == null) {// 464
            this.contractTypeButton = new JButton();// 465
            this.contractTypeButton.setText("...");// 466
            this.contractTypeButton.setBounds(545, 106, 32, 24);// 467
            this.contractTypeButton.setActionCommand("ACTION_SELECT_CONTRACT_TYPE");// 468
            this.contractTypeButton.addActionListener(this.getDefaultActionListener());// 469
        }

        return this.contractTypeButton;// 471
    }

    private JLabel getContractLabel() {
        if (this.contractLabel == null) {// 477
            this.contractLabel = new JLabel("Contract :");// 478
            this.contractLabel.setBounds(314, 78, 109, 24);// 479
        }

        return this.contractLabel;// 481
    }

    private JTextField getContractTextField() {
        if (this.contractTextField == null) {// 485
            this.contractTextField = new JTextField();// 486
            this.contractTextField.setEditable(false);// 487
            this.contractTextField.setBounds(426, 78, 120, 24);// 488
        }

        return this.contractTextField;// 490
    }

    private JButton getContractButton() {
        if (this.contractButton == null) {// 494
            this.contractButton = new JButton();// 495
            this.contractButton.setText("...");// 496
            this.contractButton.setBounds(545, 78, 32, 24);// 497
            this.contractButton.setActionCommand("ACTION_SELECT_CONTRACT");// 498
            this.contractButton.addActionListener(this.getDefaultActionListener());// 499
        }

        return this.contractButton;// 501
    }

    private JLabel getContractGroupLabel() {
        if (this.contractGroupLabel == null) {// 538
            this.contractGroupLabel = new JLabel("Contract Groups :");// 539
            this.contractGroupLabel.setBounds(314, 192, 109, 24);// 540
        }

        return this.contractGroupLabel;// 542
    }

    private JTextField getContractGroupTextField() {
        if (this.contractGroupTextField == null) {// 546
            this.contractGroupTextField = new JTextField();// 547
            this.contractGroupTextField.setEditable(false);// 548
            this.contractGroupTextField.setBounds(426, 192, 120, 24);// 549
        }

        return this.contractGroupTextField;// 551
    }

    private JButton getContractGroupButton() {
        if (this.contractGroupButton == null) {// 555
            this.contractGroupButton = new JButton();// 556
            this.contractGroupButton.setText("...");// 557
            this.contractGroupButton.setBounds(545, 192, 32, 24);// 558
            this.contractGroupButton.setActionCommand("ACTION_SELECT_CONTRACT_GROUP");// 559
            this.contractGroupButton.addActionListener(this.getDefaultActionListener());// 560
        }

        return this.contractGroupButton;// 562
    }



    private JLabel getStatusLabel() {
        if (this.statusLabel == null) {// 627
            this.statusLabel = new JLabel("Status :");// 628
            this.statusLabel.setBounds(314, 163, 109, 24);// 629
        }

        return this.statusLabel;// 631
    }

    private JTextField getStatusTextField() {
        if (this.statusTextField == null) {// 635
            this.statusTextField = new JTextField();// 636
            this.statusTextField.setEditable(false);// 637
            this.statusTextField.setBounds(426, 163, 120, 24);// 638
        }

        return this.statusTextField;// 640
    }

    private JButton getStatusButton() {
        if (this.statusButton == null) {// 644
            this.statusButton = new JButton();// 645
            this.statusButton.setText("...");// 646
            this.statusButton.setBounds(545, 163, 32, 24);// 647
            this.statusButton.setActionCommand("ACTION_SELECT_STATUS");// 648
            this.statusButton.addActionListener(this.getDefaultActionListener());// 649
        }

        return this.statusButton;// 651
    }





    private JComboBox getDisputeComboBox(int x, int y) {
        JComboBox result = new JComboBox();// 904
        result.setEditable(false);// 905
        result.setBounds(x, y, 70, 24);// 906
        AppUtil.set(result, this.getRepartitionChoices());// 907
        return result;// 908
    }

    private LegalEntityTextPanel getProcessingOrgPanel() {
        if (this.processingOrgPanel == null) {// 915
            this.processingOrgPanel = new LegalEntityTextPanel();// 916
            this.processingOrgPanel.setBounds(new Rectangle(10, 78, 282, 24));// 917
            this.processingOrgPanel.setRole("ProcessingOrg", "Processing Org", false, true);// 918
            this.processingOrgPanel.allowMultiple(true);// 920
            this.processingOrgPanel.setEditable(true);// 921
        }

        return this.processingOrgPanel;// 923
    }

    private LegalEntityTextPanel getCounterPartyPanel() {
        if (this.counterPartyPanel == null) {// 927
            this.counterPartyPanel = new LegalEntityTextPanel();// 928
            this.counterPartyPanel.setBounds(new Rectangle(10, 106, 282, 24));// 929
            this.counterPartyPanel.setRole(null, "CP role: ALL", true, true);// 930
            this.counterPartyPanel.allowMultiple(true);// 931
            this.counterPartyPanel.setEditable(true);// 932
        }

        return this.counterPartyPanel;// 934
    }

    public void setTemplate(ReportTemplate template) {
        String s = null;// 948
        s = (String)template.get("MARGIN_CALL_CONFIG_TYPE");// 950 951
        if (Util.isEmpty(s)) {// 952
            s = "";// 953
        }

        this.getContractTypeTextField().setText(s);// 955
        s = null;// 957
        s = (String)template.get("MARGIN_CALL_CONFIG_SUBTYPE");// 958 959
        if (Util.isEmpty(s)) {// 960
            s = "";// 961
        }

        this.getContractGroupTextField().setText(s);// 980
        s = (String)template.get("MARGIN_CALL_CONFIG_IDS");// 982 983
        if (Util.isEmpty(s)) {// 984
            s = "";// 985
        }

        this.getContractTextField().setText(s);// 987
        Vector poIds = template.get("PROCESSING_ORG_IDS");// 989 990
        this.getProcessingOrgPanel().setLEIds(this.toIntVector(poIds));// 991
        Vector leIds = template.get("LEGAL_ENTITY_IDS");// 993 994
        this.getCounterPartyPanel().setLEIds(this.toIntVector(leIds));// 995
        s = (String)template.get("ENTRY_STATUS");// 997 998
        if (Util.isEmpty(s)) {// 999
            s = "";// 1000
        }

        this.getStatusTextField().setText(s);// 1002
        s = (String)template.get("ENTRY_DISPUTE_STATUS");// 1004 1005
        if (Util.isEmpty(s)) {// 1006
            s = "";// 1007
        }


        Boolean b = (Boolean)template.get("HIDE_INACTIVE_EG");// 1143
        if (b != null) {// 1144
            this.getHideInactiveEGBox().setSelected(b);// 1145
        }

        s = (String)template.get("COLLATERALIZATION_STATUS");// 1148 1149
        if (Util.isEmpty(s)) {// 1150
            s = "";// 1151
        }

    }// 1154

    private Vector<Integer> toIntVector(Vector input) {
        Vector<Integer> result = new Vector();// 1157
        if (!Util.isEmpty(input)) {// 1159
            Iterator var3 = input.iterator();

            while(var3.hasNext()) {
                Object o = var3.next();// 1160
                if (o instanceof Integer) {// 1161
                    result.add((Integer)o);// 1162
                } else if (o instanceof String) {// 1163
                    result.add(Integer.valueOf((String)o));// 1164
                }
            }
        }

        return result;// 1169
    }

    public void getTemplate(ReportTemplate template) {
        template.put("MARGIN_CALL_CONFIG_TYPE", this.getContractTypeTextField().getText());// 1179 1180
        template.put("MARGIN_CALL_CONFIG_IDS", this.getContractTextField().getText());// 1182 1183
        template.put("MARGIN_CALL_CONFIG_GROUP", this.getContractGroupTextField().getText());// 1189 1190
        if (!Util.isEmpty(this.getProcessingOrgPanel().getLE())) {// 1192
            template.put("PROCESSING_ORG_IDS", this.getProcessingOrgPanel().getLEIds());// 1193 1194
        } else {
            template.remove("PROCESSING_ORG_IDS");// 1196
        }

        if (!Util.isEmpty(this.getCounterPartyPanel().getLE())) {// 1199
            template.put("LEGAL_ENTITY_IDS", this.getCounterPartyPanel().getLEIds());// 1200 1201
        } else {
            template.remove("LEGAL_ENTITY_IDS");// 1203
        }

        template.put("ENTRY_STATUS", this.getStatusTextField().getText());// 1206 1207
        template.put("HIDE_INACTIVE_EG", this.getHideInactiveEGBox().isSelected());// 1274
    }

    private void selectContractType() {
        Vector<String> all = new Vector();
        all.addAll(CollateralConfig.getContractTypes());
        Vector<String> sels = Util.string2Vector(this.getContractTypeTextField().getText());
        sels = AppUtil.chooseList(JideSwingUtilities.getFrame(this), all, sels, "Select Contract Types");
        if (sels != null) {
            this.getContractTypeTextField().setText(Util.collectionToString(sels));
        }
    }

    private void selectContractGroup() {
        Vector<String> all = new Vector();// 1333
        all.addAll(CollateralConfig.getContractGroups());// 1334
        Vector<String> sels = Util.string2Vector(this.getContractGroupTextField().getText());// 1335 1336
        sels = AppUtil.chooseList(JideSwingUtilities.getFrame(this), all, sels, "Select Contract Groups");// 1337
        if (sels != null) {// 1339
            this.getContractGroupTextField().setText(Util.collectionToString(sels));// 1341
        }
    }// 1340 1342

    private void selectContract() {
        final List<CollateralConfigDesc> allContracts = MarginCallConfigHelper.getAllCollateralConfigDesc(true);// 1386 1387
        SwingUtilities.invokeLater(new Runnable() {// 1389
            public void run() {
                List<CollateralConfigDesc> selected = MarginCallConfigHelper.selectMarginCallContractDesc(JideSwingUtilities.getFrame(CollateralConfigFilterPanel.this), allContracts, CollateralConfigFilterPanel.this.getSelectedConfigs());// 1394 1395 1396
                Vector<Integer> newIds = new Vector();// 1398
                if (!Util.isEmpty(selected)) {// 1399
                    for(int i = 0; i < selected.size(); ++i) {// 1400
                        CollateralConfigDesc config = (CollateralConfigDesc)selected.get(i);// 1402
                        if (config != null) {// 1403
                            newIds.add(config.getId());// 1404
                        }
                    }
                }

                if (!Util.isEmpty(newIds)) {// 1409
                    getContractTextField().setText(Util.collectionToString(newIds));// 1410 1411
                } else {
                    getContractTextField().setText("");// 1413
                }

            }// 1415
        });
    }// 1418

    private List<CollateralConfigDesc> getSelectedConfigs() {
        List<CollateralConfigDesc> result = new ArrayList();// 1421
        String s = this.getContractTextField().getText();// 1423
        if (!Util.isEmpty(s)) {// 1424
            List<String> list = new ArrayList();// 1425
            List<String> selectedAsString = (List)Util.stringToCollection(list, s, ",", false);// 1426 1427
            List<Integer> ids = new ArrayList();// 1429
            Iterator var6 = selectedAsString.iterator();

            while(var6.hasNext()) {// 1430
                String string = (String)var6.next();

                try {
                    ids.add(Integer.parseInt(string));// 1432
                } catch (NumberFormatException var11) {// 1433
                }
            }

            List<CollateralConfigDesc> allContracts = MarginCallConfigHelper.getAllCollateralConfigDesc(true);// 1443
            Iterator var13 = allContracts.iterator();

            while(var13.hasNext()) {
                CollateralConfigDesc contract = (CollateralConfigDesc)var13.next();// 1444
                Iterator var9 = ids.iterator();// 1445

                while(var9.hasNext()) {
                    Integer mccId = (Integer)var9.next();
                    if (contract.getId() == mccId) {// 1446
                        result.add(contract);// 1447
                    }
                }
            }
        }

        return result;// 1453
    }

    private void selectStatus() {
        Vector<String> all = new Vector();
        all.add("OPEN");
        Vector<String> sels = Util.string2Vector(this.getStatusTextField().getText());// 1459
        sels = AppUtil.chooseList(JideSwingUtilities.getFrame(this), all, sels, "Select Status");// 1460
        if (sels != null) {// 1462
            this.getStatusTextField().setText(Util.collectionToString(sels));// 1464
        }
    }// 1463 1465

    private CollateralConfigFilterPanel.DefaultActionListener getDefaultActionListener() {
        if (this.defaultActionListener == null) {// 1501
            this.defaultActionListener = new CollateralConfigFilterPanel.DefaultActionListener();// 1502
        }

        return this.defaultActionListener;// 1504
    }

    private class DefaultActionListener implements ActionListener {
        private DefaultActionListener() {
        }// 1507

        public void actionPerformed(ActionEvent e) {
            String action = e.getActionCommand();// 1510
            if ("ACTION_SELECT_CONTRACT_TYPE".equals(action)) {// 1511
                CollateralConfigFilterPanel.this.selectContractType();// 1512
            } else if ("ACTION_SELECT_CONTRACT".equals(action)) {// 1513
                CollateralConfigFilterPanel.this.selectContract();// 1514
            } else if ("ACTION_SELECT_STATUS".equals(action)) {// 1515
                CollateralConfigFilterPanel.this.selectStatus();// 1516
            }else if ("ACTION_SELECT_CONTRACT_GROUP".equals(action)) {// 1529
                CollateralConfigFilterPanel.this.selectContractGroup();// 1530
            }
        }// 1534
    }

}


