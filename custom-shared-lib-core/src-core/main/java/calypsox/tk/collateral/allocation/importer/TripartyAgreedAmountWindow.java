package calypsox.tk.collateral.allocation.importer;

import calypsox.tk.collateral.allocation.importer.mapper.ExcelExternalAllocationMapper;
import calypsox.tk.collateral.allocation.importer.reader.ExcelExternalAllocationReader;
import calypsox.util.collateral.SantMarginCallEntryUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class TripartyAgreedAmountWindow extends JDialog {

    // for process global percent completed management
    protected Integer total = 0;

    public static MessageFormat stepTitleStyle = new MessageFormat("<b><u>{0}</u></b>");
    public static MessageFormat stepStatusStyle = new MessageFormat("<i>{0}</i>");

    public static final int STEP_TITLE_STYLE = 1;
    public static final int STEP_STATUS_STYLE = 2;

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JTextField filePathArea = null;
    private TextPaneAppender textArea;
    protected MarginCallEntry entry = null;
    protected JProgressBar progressBar = null;
    protected HTMLEditorKit kit = null;
    protected HTMLDocument doc = null;
    protected TAAImportSwingWorker swingWorker = null;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    TripartyAgreedAmountWindow frame = new TripartyAgreedAmountWindow(null);
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
    public TripartyAgreedAmountWindow(MarginCallEntry entry) {
        setSize(new Dimension(600, 450));
        setMinimumSize(new Dimension(600, 450));
        centerOnParent(this, true);
        this.swingWorker = new TAAImportSwingWorker();
        this.entry = entry;
        init();
    }

    protected void init() {
        ActionListener actionListener = new TAAImportActionListener();
        setModal(true);
        // pack();
        setTitle("TripartyAgreedAmountImportWindow");
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

        this.textArea = new TextPaneAppender();
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

    public class TAAImportActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if ("choose".equals(e.getActionCommand())) {
                JFileChooser chooser = new JFileChooser();
                chooser.showOpenDialog(getContentPane());
                File file = chooser.getSelectedFile();
                TripartyAgreedAmountWindow.this.filePathArea.setText(file.getAbsolutePath());

            } else if ("import".equals(e.getActionCommand())) {
                String filePath = TripartyAgreedAmountWindow.this.filePathArea.getText();
                if (Util.isEmpty(filePath)) {
                    AppUtil.displayError(getContentPane(), "Please choose a file before");
                    return;
                }
                if (TripartyAgreedAmountWindow.this.swingWorker.isDone()) {
                    if (!AppUtil.displayQuestion("The import was already performed. \nImport again?", getContentPane())) {
                        return;
                    }
                }
                TripartyAgreedAmountWindow.this.swingWorker = new TAAImportSwingWorker();
                TripartyAgreedAmountWindow.this.swingWorker.execute();

            } else if ("close".equals(e.getActionCommand())) {
                TripartyAgreedAmountWindow.this.swingWorker.cancel(true);
                dispose();
            }

        }
    }

    class TAAImportSwingWorker extends SwingWorker<Integer, String> {

        private static final String TRIPARTY_AGREED_AMOUNT = "Triparty Agreed Amount";

        public TAAImportSwingWorker() {

            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("progress".equals(evt.getPropertyName())) {
                        TripartyAgreedAmountWindow.this.progressBar.setValue((Integer) evt.getNewValue());
                    }
                }
            });
        }

        @Override
        public Integer doInBackground() {
            try {
                TripartyAgreedAmountWindow.this.progressBar.setValue(0);
                TripartyAgreedAmountWindow.this.textArea.setText("");

                String filePath = TripartyAgreedAmountWindow.this.filePathArea.getText();
                ExcelExternalAllocationReader reader = new ExcelExternalAllocationReader(filePath);

                formatAndPublish("Reading triparty agreed amounts from the file...", STEP_TITLE_STYLE);
                List<String> messages = new ArrayList<String>();
                List<ExternalTripartyBean> marginCallTripartyAmountBeans = new ArrayList<ExternalTripartyBean>();
                try {
                    JDate processDate = TripartyAgreedAmountWindow.this.entry.getProcessDate();
                    marginCallTripartyAmountBeans = reader.tripartyAgreedAmountReader(messages);
                    reader.setEntriesTAA(marginCallTripartyAmountBeans, processDate, new ArrayList<String>());
                } catch (Exception e) {
                    Log.error(this, e);
                    publish("<br>");
                    formatAndPublish("Unable to read the file.", STEP_STATUS_STYLE);
                    publish("<br>");
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                    return 0;
                }
                if (!Util.isEmpty(messages)) {
                    for (String message : messages) {
                        publish(message);
                    }
                }
                if (!Util.isEmpty(marginCallTripartyAmountBeans)) {
                    publish("<br>");
                    formatAndPublish(marginCallTripartyAmountBeans.size() + " successfully triparty agreed amounts read.",
                            STEP_STATUS_STYLE);
                } else {
                    publish("<br>");
                    publish("No triparty agreed amount correctly read from the file");
                    publish("<br>");
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                    return 0;
                }

                // start the import
                TripartyAgreedAmountWindow.this.total = marginCallTripartyAmountBeans.size();
                List<ExternalTripartyBean> validatedTAAS = validateTAA(marginCallTripartyAmountBeans);

                if (!Util.isEmpty(validatedTAAS)) {
                    formatAndPublish(validatedTAAS.size() + " valid triparty agreed amount will be imported.",
                            STEP_STATUS_STYLE);
                } else {
                    formatAndPublish("No valid triparty agreed amount found.", STEP_STATUS_STYLE);
                    publish("<br>");
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                    return 0;
                }
                List<ExternalTripartyBean> tripartyAgreedAmounts = importTAA(validatedTAAS);
                if (!Util.isEmpty(tripartyAgreedAmounts)) {
                    formatAndPublish(tripartyAgreedAmounts.size() + " triparty agreed amounts successfully imported.",
                            STEP_STATUS_STYLE);
                } else {
                    formatAndPublish("No triparty agreed amount was imported.", STEP_STATUS_STYLE);
                    publish("<br>");
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                    return 0;
                }
                publish("<br>");
                formatAndPublish("Import finished", STEP_TITLE_STYLE);
            } catch (Exception e) {
                Log.error(this, e);
            }
            return 100;
        }

        private List<ExternalTripartyBean> validateTAA(List<ExternalTripartyBean> marginCallTripartyAmountBeans) {
            publish("<br>");
            formatAndPublish("Validating triparty agreed amount ...", STEP_TITLE_STYLE);
            List<ExternalTripartyBean> validTAAS = new ArrayList<ExternalTripartyBean>();
            if (!Util.isEmpty(marginCallTripartyAmountBeans)) {
                List<String> messages = new ArrayList<String>();
                Integer iteration = 0;
                MarginCallEntry newEntry;
                ExcelExternalAllocationMapper mapper;
                for (ExternalTripartyBean beanTAA : marginCallTripartyAmountBeans) {
                    iteration++;
                    messages.clear();
                    newEntry = beanTAA.getEntry();
                    try {
                        if (newEntry != null) {
                            beanTAA.setMapper(new ExcelExternalAllocationMapper(newEntry, newEntry.getProcessDate()));
                            mapper = beanTAA.getMapper();
                            if (mapper != null) {
                                if (mapper.isValidTripartyAgreedAmount(beanTAA, messages)) {
                                    validTAAS.add(beanTAA);
                                }
                            } else {
                                messages.add("Unable to create mapper for triparty agreed amount in row " + beanTAA.getRowNumber());
                            }
                        } else {
                            messages.add("Unable to find entry for triparty agreed amount in row " + beanTAA.getRowNumber());
                        }
                    } catch (Exception e) {
                        messages.add("Unable to validate triparty agreed amount " + e.getMessage());
                        Log.error(this, e);
                    }
                    // update global process
                    setProgress((int) (getActualPercentageCompleted(iteration.doubleValue()) * 100));
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Log.error(this, e); //sonar
                    }
                    for (String message : messages) {
                        publish(message);
                    }
                }
            }
            publish("<br>");
            // update total variable
            TripartyAgreedAmountWindow.this.total = validTAAS.size();
            return validTAAS;
        }

        private List<ExternalTripartyBean> importTAA(List<ExternalTripartyBean> validatedTAASToImport) {
            publish("<br>");
            formatAndPublish("Importing triparty agreed amount ...", STEP_TITLE_STYLE);

            List<ExternalTripartyBean> importedTAAS = new ArrayList<>();
            if (!Util.isEmpty(validatedTAASToImport)) {
                List<String> messages = new ArrayList<>();
                Integer iteration = 0;
                for (ExternalTripartyBean taa : validatedTAASToImport) {
                    iteration++;
                    messages.clear();

                    MarginCallEntry fullWeightEntry = SantMarginCallEntryUtil.getFullWeightMarginCallEntry(taa.getEntry());
                    if (saveEntry(fullWeightEntry, messages, taa.getNominal())) {
                        importedTAAS.add(taa);
                    }

                    setProgress((int) ((getActualPercentageCompleted(iteration.doubleValue()) + 0.5) * 100));
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Log.error(this, e); //sonar
                    }
                    for (String message : messages) {
                        publish(message);
                    }
                }
            }
            publish("<br>");
            return importedTAAS;
        }

        private boolean saveEntry(MarginCallEntry entry, List<String> messages, double amount) {
            String userAction = Action.S_UPDATE;
            try {
                MarginCallEntryDTO dto = entry.toDTO();
                //dto.setGlobalRequiredMargin(amount);
                dto.addAttribute(TRIPARTY_AGREED_AMOUNT, amount);
                int id = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer().save(dto, userAction, TimeZone.getDefault());
                if (id > 0) return true;
                messages.add("Could not save entry " + entry.getCollateralConfigId() + " with Status " + entry.getStatus());
                return false;
            } catch (CollateralServiceException e) {
                messages.add("Could not save entry " + entry.getCollateralConfigId() + " with Status " + entry.getStatus());
                Log.error(this, e); //sonar
                return false;
            }
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

                default:
                    break;
            }
            publish(messageToPublish);
        }

        @Override
        protected void process(List<String> strings) {
            for (String s : strings) {
                TripartyAgreedAmountWindow.this.textArea.append(s + '\n');
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
                TripartyAgreedAmountWindow.this.kit.insertHTML(TripartyAgreedAmountWindow.this.doc,
                        TripartyAgreedAmountWindow.this.doc.getLength(), chaine, 0, 0, null);
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
}
