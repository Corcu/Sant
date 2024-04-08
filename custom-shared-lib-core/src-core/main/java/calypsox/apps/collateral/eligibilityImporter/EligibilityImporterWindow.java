package calypsox.apps.collateral.eligibilityImporter;

import calypsox.util.collateral.SantMarginCallEntryUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.collateral.util.TaskWorker;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EligibilityImporterWindow extends JDialog {

    // for process global percent completed management
    protected Integer total = 0;

    private static MessageFormat stepTitleStyle = new MessageFormat("<b><u>{0}</u></b>");
    private static MessageFormat stepStatusStyle = new MessageFormat("<i>{0}</i>");
    private static MessageFormat stepErrorStyle = new MessageFormat("<p><i color=\"RED\">{0}</i></p>");

    public static final int STEP_TITLE_STYLE = 1;
    public static final int STEP_STATUS_STYLE = 2;
    public static final int STEP_ERROR_STYLE = 3;

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JTextField filePathArea = null;
    private EligibilityImporterWindow.TextPaneAppender textArea;
    protected MarginCallEntry entry = null;
    protected JProgressBar progressBar = null;
    protected HTMLEditorKit kit = null;
    protected HTMLDocument doc = null;
    protected EligibilityImporterWindow.EligImportSwingWorker swingWorker = null;
    protected PropertyChangeListener propListener = null;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    EligibilityImporterWindow frame = new EligibilityImporterWindow();
                    frame.setVisible(true);
                } catch (Exception e) {
                    Log.error(this, e); //sonar
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public EligibilityImporterWindow() {
        setSize(new Dimension(600, 450));
        setMinimumSize(new Dimension(600, 450));
        centerOnParent(this, true);
        this.swingWorker = new EligibilityImporterWindow.EligImportSwingWorker();
        propListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    EligibilityImporterWindow.this.progressBar.setValue((Integer) evt.getNewValue());
                } else if (TaskWorker.PROPERTY_MESSAGE.equals(evt.getPropertyName())) {
                    textArea.append(evt.getNewValue().toString());
                }
            }
        };
        init();
    }


    protected void init() {
        ActionListener actionListener = new EligibilityImporterWindow.EligImportActionListener();
        setModal(true);
        // pack();
        setTitle("EligibilityImportWindow");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        final JPanel panel = new JPanel();
        // .setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        getContentPane().add(panel);
        // UIManager.getBorder("TitledBorder.border")
        panel.setBorder(new TitledBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("")
                .getBorder(), BorderFactory.createEmptyBorder(0, 0, 5, 0)), "File to import :", TitledBorder.LEADING,
                TitledBorder.TOP, null, new Color(51, 153, 255)));
        // panel.setBorder(BorderFactory.createTitledBorder());

        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        this.filePathArea = new JTextField();
        this.filePathArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, this.filePathArea.getPreferredSize().height));
        panel.add(this.filePathArea);
        this.filePathArea.setColumns(10);
        // this.filePathArea.setMinimumSize(new Dimension(45, 23));
        // this.filePathArea.setMaximumSize(new Dimension(45, 23));
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnNewButton = new JButton("...");
        panel.add(btnNewButton);
        // btnNewButton.setBounds(384, 23, 45, 23);
        btnNewButton.setActionCommand("choose");
        btnNewButton.addActionListener(actionListener);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton btnImport = new JButton("Import");
        panel.add(btnImport);
        // btnImport.setBounds(439, 23, 89, 23);
        btnImport.setActionCommand("import");
        btnImport.addActionListener(actionListener);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

        this.textArea = new EligibilityImporterWindow.TextPaneAppender();
        this.kit = new HTMLEditorKit();
        this.doc = new HTMLDocument();
        this.textArea.setEditorKit(this.kit);
        this.textArea.setDocument(this.doc);
        this.textArea.setEditable(false);
        this.textArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        // this.textArea.setBounds(10, 101, 525, 267);

        JPanel panel_2 = new JPanel();
        getContentPane().add(panel_2);
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
        // getContentPane().setFocusTraversalPolicy(
        // new FocusTraversalOnArray(new Component[] { panel, this.filePathArea, panelButtons, btnNewButton,
        // btnImport, this.progressBar, scrollPane, btnClose }));
        this.progressBar = new JProgressBar();
        panel_2.add(this.progressBar);
        panel_2.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        // this.progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE,
        // this.progressBar.getPreferredSize().height));
        JScrollPane scrollPane = new JScrollPane(this.textArea);
        scrollPane.setBorder(new TitledBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("")
                .getBorder(), BorderFactory.createEmptyBorder(0, 10, 5, 10)), "Import detail :", TitledBorder.LEADING,
                TitledBorder.TOP, null, new Color(51, 153, 255)));
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        JPanel panel_1 = new JPanel();
        getContentPane().add(panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
        panel_1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel_1.add(Box.createHorizontalGlue());
        JButton btnClose = new JButton("Close");
        panel_1.add(btnClose);
        panel_1.add(Box.createRigidArea(new Dimension(10, 0)));

        btnClose.setHorizontalAlignment(SwingConstants.RIGHT);
        btnClose.setActionCommand("close");
        btnClose.addActionListener(actionListener);

    }

    public class EligImportActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if ("choose".equals(e.getActionCommand())) {
                JFileChooser chooser = new JFileChooser();
                chooser.showOpenDialog(getContentPane());
                File file = chooser.getSelectedFile();
                EligibilityImporterWindow.this.filePathArea.setText(file.getAbsolutePath());

            } else if ("import".equals(e.getActionCommand())) {
                String filePath = EligibilityImporterWindow.this.filePathArea.getText();
                if (Util.isEmpty(filePath)) {
                    AppUtil.displayError(getContentPane(), "Please choose a file before");
                    return;
                }
                if (EligibilityImporterWindow.this.swingWorker.isDone()) {
                    if (!AppUtil.displayQuestion("The import was already performed. \nImport again?", getContentPane())) {
                        return;
                    }
                }
                EligibilityImporterWindow.this.swingWorker = new EligibilityImporterWindow.EligImportSwingWorker();
                EligibilityImporterWindow.this.swingWorker.execute();

            } else if ("close".equals(e.getActionCommand())) {
                EligibilityImporterWindow.this.swingWorker.cancel(true);
                dispose();
            }

        }
    }

    class EligImportSwingWorker extends SwingWorker<Integer, String> {

        public EligImportSwingWorker() {

            addPropertyChangeListener(propListener);
        }

        private void formatAndPublish(String string, int stepTitleStyleCode) {
            String messageToPublish = string;
            switch (stepTitleStyleCode) {
                case STEP_TITLE_STYLE:
                    messageToPublish = stepTitleStyle.format(new String[]{string});
                    break;
                case STEP_STATUS_STYLE:
                    messageToPublish = stepStatusStyle.format(new String[]{string});
                    break;
                case STEP_ERROR_STYLE:
                    messageToPublish = stepErrorStyle.format(new String[]{string});
                    break;
                default:
                    break;
            }
            publish(messageToPublish);
        }
        @Override
        public Integer doInBackground() {
                EligibilityImporterWindow.this.progressBar.setValue(0);
                EligibilityImporterWindow.this.textArea.setText("");

                String filePath = EligibilityImporterWindow.this.filePathArea.getText();
                BufferedReader reader;
                List<String> messages = new ArrayList<>();
                try {
                    formatAndPublish("Starting the import...", STEP_TITLE_STYLE);
                    reader = new BufferedReader(new FileReader(filePath));
                    String line;
                    int lineNumber = 0;
                    while ((line = reader.readLine()) != null) {
                        if (lineNumber == 0){
                            lineNumber++;
                            continue;
                        }
                        lineNumber++;
                        String[] fields = line.split(";");

                        EligibilityImporterBean bean = new EligibilityImporterBean();
                        try {
                            generateBean(fields, bean, lineNumber);
                        }catch (ArrayIndexOutOfBoundsException e){
                            publish("Unable to import row " + (lineNumber-1) +". Please check all fields are filled up");
                            continue;
                        }
                        if (validateBean(bean, messages)) {
                            doThings(bean, messages);
                        }
                        if (!Util.isEmpty(messages)) {
                            for (String message : messages) {
                                publish(message);
                            }
                            messages.clear();
                        }
                    }
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            return 100;
        }


        @Override
        protected void process(List<String> strings) {
            for (String s : strings) {
                EligibilityImporterWindow.this.textArea.append(s + '\n');
            }
        }

        @Override
        protected void done() {
            try {
                setProgress(100);
            } catch (Exception e) {
                Log.error(this, e);
            }
        }
    }

    class TextPaneAppender extends JTextPane {
        private static final long serialVersionUID = 1L;

        public void append(String chaine) {
            try {
                EligibilityImporterWindow.this.kit.insertHTML(EligibilityImporterWindow.this.doc,
                        EligibilityImporterWindow.this.doc.getLength(), chaine, 0, 0, null);
            } catch (Exception e) {
                Log.error(this, e);
            }
        }
    }

    // Center on parent ( absolute true/false (exact center or 25% upper left) )
    public void centerOnParent(final Window child, final boolean absolute) {
        child.pack();
        boolean useChildsOwner = child.getOwner() != null ? ((child.getOwner() instanceof JFrame) || (child.getOwner() instanceof JDialog))
                : false;
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension parentSize = useChildsOwner ? child.getOwner().getSize() : screenSize;
        final Point parentLocationOnScreen = useChildsOwner ? child.getOwner().getLocationOnScreen() : new Point(0, 0);
        final Dimension childSize = child.getSize();
        childSize.width = Math.min(childSize.width, screenSize.width);
        childSize.height = Math.min(childSize.height, screenSize.height);
        child.setSize(childSize);
        int x;
        int y;
        if ((child.getOwner() != null) && child.getOwner().isShowing()) {
            x = (parentSize.width - childSize.width) / 2;
            y = (parentSize.height - childSize.height) / 2;
            x += parentLocationOnScreen.x;
            y += parentLocationOnScreen.y;
        } else {
            x = (screenSize.width - childSize.width) / 2;
            y = (screenSize.height - childSize.height) / 2;
        }
        if (!absolute) {
            x /= 2;
            y /= 2;
        }
        child.setLocation(x, y);
    }

    // v4.2
    // method to get process global percent completed at the moment
    public double getActualPercentageCompleted(double iteration) {
        return (iteration / this.total) / 2.0;
    }

    private void generateBean(String[] fields, EligibilityImporterBean bean, int lineNumber) throws ArrayIndexOutOfBoundsException{

        String contract = fields[0];
        try {
            int contractId = Integer.parseInt(contract);
            bean.setContractID(contractId);
        } catch (Exception e) {
            bean.setContractName(contract);
        }
        bean.setSdfName(Collections.singletonList(fields[1]));
        bean.setSide(fields[2]);
        bean.setRowNumber(lineNumber);
    }

    private boolean validateBean(EligibilityImporterBean bean, List<String> messages) {
        if (validateMcc(bean, messages) & validateSDF(bean, messages) & validateSide(bean,messages)) {
            return true;
        } else {
            messages.add("Unable to import row " + (bean.getRowNumber()-1));
            return false;
        }
    }

    private boolean validateMcc(EligibilityImporterBean bean, List<String> messages) {
        if (bean.getMarginCallConfig() != null) {
            return true;
        }
        String contractName = bean.getContractName();
        int contractId = bean.getContractID();
        CollateralConfig marginCallConfig = null;

        if (contractId!=0 || contractName!=null) {
            if (bean.isByName()) {
                try {
                    marginCallConfig = ServiceRegistry.getDefault().getCollateralDataServer()
                            .getMarginCallConfigByCode(null, contractName);
                    if (marginCallConfig == null) {
                        messages.add("row " + (bean.getRowNumber() - 1) + " : Margin Call Config "
                                + (Util.isEmpty(bean.getContractName()) ? "" : bean.getContractName()) + " doesn't exist");
                        return false;
                    }
                } catch (CollateralServiceException e) {
                    Log.error(this, e); //sonar
                    messages.add("row " + (bean.getRowNumber() - 1) + " : Margin Call Config "
                            + (Util.isEmpty(bean.getContractName()) ? "" : bean.getContractName()) + " not found");
                    return false;
                }
            } else {
                try {
                    marginCallConfig = CacheCollateralClient.getInstance().getCollateralConfig(DSConnection.getDefault(),
                            contractId);
                    if (marginCallConfig == null) {
                        messages.add("row " + (bean.getRowNumber() - 1) + " : Margin Call Config with id " + bean.getContractID() + " doesn't exist");
                        return false;
                    }
                } catch (Exception e) {
                    Log.error(this, e); //sonar
                    messages.add("row " + (bean.getRowNumber() - 1) + " : Margin Call Config with id " + bean.getContractID() + " not found");
                    return false;
                }

            }
            bean.setMarginCallConfig(marginCallConfig);
            return true;
        } else {
            messages.add("row " + (bean.getRowNumber() - 1) + " : the 'Contract' field is mandatory");
            return false;
        }
    }

    private boolean validateSDF(EligibilityImporterBean bean, List<String> messages) {
            if (bean.getSdfName() != null && bean.getSdfName().get(0) != null) {
                StaticDataFilter sdf = BOCache.getStaticDataFilter(DSConnection.getDefault(), bean.getSdfName().get(0));
                if (null != sdf) {
                    boolean isProductTypeFiltered = false;
                    for (StaticDataFilterElement sdfElement : sdf.getElements()) {
                        if (sdfElement.getName().equalsIgnoreCase("Product Type")){
                            isProductTypeFiltered = true;
                        }
                    }
                    if (isProductTypeFiltered) {
                        bean.setStaticDataFilter(sdf);
                        return true;
                    } else {
                        messages.add("row " + (bean.getRowNumber() - 1) + " : Static Data Filter " + (Util.isEmpty(bean.getSdfName().get(0)) ? "" : bean.getSdfName().get(0)) + " doesn't have a Product Type Filter");
                        return false;
                    }

                } else {
                    messages.add("row " + (bean.getRowNumber() - 1) + " : Static Data Filter " + (Util.isEmpty(bean.getSdfName().get(0)) ? "" : bean.getSdfName().get(0)) + " doesn't exist");
                    return false;
                }
            }else{
                messages.add("row " + (bean.getRowNumber() - 1) + " : the 'Eligible Collateral' field is mandatory");
                return false;
            }
    }

    private boolean validateSide(EligibilityImporterBean bean, List<String> messages) {
        if (bean.getSide()!=null && (bean.getSide().equalsIgnoreCase("PO") || bean.getSide().equalsIgnoreCase("LE"))){
            return true;
        } else {
            messages.add("row " + (bean.getRowNumber()-1) + ": the PO/LE " + bean.getSide() + " has not the right format");
            return false;
        }
    }

    private void doThings(EligibilityImporterBean bean, List<String> messages) {
        if (bean.getSide().equalsIgnoreCase("PO")) {
            bean.getMarginCallConfig().setEligibilityFilterNames(bean.getSdfName());
        } else {
            bean.getMarginCallConfig().setLeEligibilityFilterNames(bean.getSdfName());
        }
        try {
            ServiceRegistry.getDefault().getCollateralDataServer().save(bean.getMarginCallConfig());
            messages.add("Row " + (bean.getRowNumber()-1) + " has been succesfully imported");
        } catch (Exception e) {
            messages.add("row " + (bean.getRowNumber()-1) + " : Margin Call Config " + bean.getMarginCallConfig().getName() + " can not be saved");
        }
    }

}
