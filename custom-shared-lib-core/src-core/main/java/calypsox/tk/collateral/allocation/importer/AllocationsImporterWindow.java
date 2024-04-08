/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.collateral.allocation.importer;

import calypsox.tk.collateral.allocation.importer.mapper.ExcelExternalAllocationMapper;
import calypsox.tk.collateral.allocation.importer.reader.ExcelExternalAllocationReader;
import calypsox.util.collateral.SantMarginCallEntryUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.collateral.util.TaskWorker;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralContext;
import com.calypso.tk.refdata.ContextUserAction;

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

public class AllocationsImporterWindow extends JDialog {

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
    private TextPaneAppender textArea;
    protected MarginCallEntry entry = null;
    protected JProgressBar progressBar = null;
    protected HTMLEditorKit kit = null;
    protected HTMLDocument doc = null;
    protected AllocImportSwingWorker swingWorker = null;
    protected PropertyChangeListener propListener = null;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    AllocationsImporterWindow frame = new AllocationsImporterWindow(null);
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
    public AllocationsImporterWindow(MarginCallEntry entry) {
        setSize(new Dimension(600, 450));
        setMinimumSize(new Dimension(600, 450));
        centerOnParent(this, true);
        this.swingWorker = new AllocImportSwingWorker();
        this.entry = entry;
        propListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    AllocationsImporterWindow.this.progressBar.setValue((Integer) evt.getNewValue());
                } else if (TaskWorker.PROPERTY_MESSAGE.equals(evt.getPropertyName())) {
                    textArea.append(evt.getNewValue().toString());
                }
            }
        };
        init();
    }


    protected void init() {
        ActionListener actionListener = new AllocImportActionListener();
        setModal(true);
        // pack();
        setTitle("AllocationImportWindow");
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

    public class AllocImportActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if ("choose".equals(e.getActionCommand())) {
                JFileChooser chooser = new JFileChooser();
                chooser.showOpenDialog(getContentPane());
                File file = chooser.getSelectedFile();
                AllocationsImporterWindow.this.filePathArea.setText(file.getAbsolutePath());

            } else if ("import".equals(e.getActionCommand())) {
                String filePath = AllocationsImporterWindow.this.filePathArea.getText();
                if (Util.isEmpty(filePath)) {
                    AppUtil.displayError(getContentPane(), "Please choose a file before");
                    return;
                }
                if (AllocationsImporterWindow.this.swingWorker.isDone()) {
                    if (!AppUtil.displayQuestion("The import was already performed. \nImport again?", getContentPane())) {
                        return;
                    }
                }
                AllocationsImporterWindow.this.swingWorker = new AllocImportSwingWorker();
                AllocationsImporterWindow.this.swingWorker.execute();

            } else if ("close".equals(e.getActionCommand())) {
                AllocationsImporterWindow.this.swingWorker.cancel(true);
                dispose();
            }

        }
    }

    class AllocImportSwingWorker extends SwingWorker<Integer, String> {

        public AllocImportSwingWorker() {

            addPropertyChangeListener(propListener);
        }


        @Override
        public Integer doInBackground() {
            try {
                AllocationsImporterWindow.this.progressBar.setValue(0);
                AllocationsImporterWindow.this.textArea.setText("");

                String filePath = AllocationsImporterWindow.this.filePathArea.getText();
                ExcelExternalAllocationReader reader = new ExcelExternalAllocationReader(filePath);

                formatAndPublish("Reading allocations from the file...", STEP_TITLE_STYLE);
                List<String> messages = new ArrayList<String>();
                List<ExternalAllocationBean> marginCallAllocationsBeans = new ArrayList<ExternalAllocationBean>();
                try {
                    JDate processDate = AllocationsImporterWindow.this.entry.getProcessDate();
                    marginCallAllocationsBeans = reader.readAllocations(messages);
                    reader.setEntries(marginCallAllocationsBeans, processDate, new ArrayList<String>());
                } catch (Exception e) {
                    Log.error(this, e);
                    publish("<br>");
                    formatAndPublish("Unable to read the file: " + e.getMessage(), STEP_STATUS_STYLE);
                    publish("<br>");
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                    return 0;
                }
                if (!Util.isEmpty(messages)) {
                    for (String message : messages) {
                        publish(message);
                    }
                }
                if (!Util.isEmpty(marginCallAllocationsBeans)) {
                    publish("<br>");
                    formatAndPublish(marginCallAllocationsBeans.size() + " successfully allocations read.",
                            STEP_STATUS_STYLE);
                } else {
                    publish("<br>");
                    publish("No allocation correctly read from the file");
                    publish("<br>");
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                    return 0;
                }

                // start the import
                AllocationsImporterWindow.this.total = marginCallAllocationsBeans.size();
                List<ExternalAllocationBean> validatedAllocations = newValidateAllocations(marginCallAllocationsBeans);

                if (!Util.isEmpty(validatedAllocations)) {
                    formatAndPublish(validatedAllocations.size() + " valid allocations will be imported.",
                            STEP_STATUS_STYLE);
                } else {
                    formatAndPublish("No valid allocation found.", STEP_STATUS_STYLE);
                    publish("<br>");
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                    return 0;
                }
                List<MarginCallAllocation> marginCallAllocations = newImportAllocations(validatedAllocations);
                if (!Util.isEmpty(marginCallAllocations)) {
                    formatAndPublish(marginCallAllocations.size() + " allocations successfully imported.",
                            STEP_STATUS_STYLE);
                } else {
                    formatAndPublish("No allocation was imported.", STEP_STATUS_STYLE);
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

        private List<ExternalAllocationBean> newValidateAllocations(List<ExternalAllocationBean> marginCallAllocationsBeans) {
            publish("<br>");
            formatAndPublish("Validating allocations ...", STEP_TITLE_STYLE);
            List<ExternalAllocationBean> validAllocations = new ArrayList<ExternalAllocationBean>();
            if (!Util.isEmpty(marginCallAllocationsBeans)) {
                List<String> messages = new ArrayList<String>();
                Integer iteration = 0;
                MarginCallEntry newEntry;
                ExcelExternalAllocationMapper mapper;
                for (ExternalAllocationBean allocBean : marginCallAllocationsBeans) {
                    iteration++;
                    messages.clear();
                    newEntry = allocBean.getMarginCallEntry();
                    try {
                        if (newEntry != null) {
                            allocBean.setMapper(new ExcelExternalAllocationMapper(newEntry, newEntry.getProcessDate()));
                            mapper = allocBean.getMapper();
                            if (mapper != null) {
                                if (mapper.isValidAllocation(allocBean, messages)
                                        && (!(allocBean instanceof CashExternalAllocationBean)
                                        || mapper.checkEligibleCurrency((CashExternalAllocationBean) allocBean, messages)))        // check that the currency is eligible if the allocation type is cash, since the check has been disabled in mapper.isValidAllocation()
                                    validAllocations.add(allocBean);
                            } else {
                                messages.add("Unable to create mapper for allocation in row " + allocBean.getRowNumber());
                            }
                        } else {
                            messages.add("Unable to find entry for allocation in row " + allocBean.getRowNumber());
                        }
                    } catch (Exception e) {
                        messages.add("Unable to validate allocation " + e.getMessage());
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
            AllocationsImporterWindow.this.total = validAllocations.size();
            return validAllocations;
        }

        private List<MarginCallAllocation> newImportAllocations(List<ExternalAllocationBean> validatedAllocationsToImport) {
            publish("<br>");
            formatAndPublish("Importing allocations ...", STEP_TITLE_STYLE);

            List<MarginCallAllocation> importedAllocations = new ArrayList<>();
            List<MarginCallEntry> entries = new ArrayList<>();
            if (!Util.isEmpty(validatedAllocationsToImport)) {
                List<String> messages = new ArrayList<>();
                Integer iteration = 0;
                MarginCallEntry tempEntry = null;
                for (ExternalAllocationBean allocBean : validatedAllocationsToImport) {
                    iteration++;
                    messages.clear();
                    try {
                        ExcelExternalAllocationMapper mapper = allocBean.getMapper();
                        if (mapper != null) {
                            MarginCallAllocation allocation = mapper.mapAllocation(allocBean, messages);
                            if (allocation != null) {
                                tempEntry = SantMarginCallEntryUtil.getFullWeightMarginCallEntry(getEntry(entries, allocBean.getMarginCallEntry()));
                                if (addMCEntry(allocation, tempEntry)) {
                                    importedAllocations.add(allocation);
                                }
                            }
                        } else {
                            messages.add("Unable to find mapper for allocation in row " + allocBean.getRowNumber());
                        }
                    } catch (Exception e) {
                        messages.add("Unable to import allocation " + e.getMessage());
                        Log.error(this, e);
                    }
                    setProgress((int) ((getActualPercentageCompleted(iteration.doubleValue()) + 0.5) * 100));
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Log.error(this, e); //sonar
                    }
                    for (String message : messages) {
                        formatAndPublish(message, STEP_ERROR_STYLE);
                    }
                }

                messages.clear();
                //TODO Search for a saving validation
                saveEntries(entries, messages);
                int cont = 0;
                if (cont > 0) {
                    messages.add(cont + " contracts were modified");

                }
                for (String message : messages) {
                    publish(message);
                }
            }
            publish("<br>");
            return importedAllocations;
        }

        private MarginCallEntry getEntry(List<MarginCallEntry> entries, MarginCallEntry oldEntry) {
            int oldContractID = oldEntry.getCollateralConfig().getId();
            JDate oldProcessDate = oldEntry.getProcessDate();
            int contractID = 0;
            JDate processDate = null;
            for (MarginCallEntry listEntry : entries) {
                contractID = listEntry.getCollateralConfig().getId();
                processDate = listEntry.getProcessDate();
                if (contractID == oldContractID && processDate.equals(oldProcessDate)) {
                    return listEntry;
                }
            }
            entries.add(oldEntry);
            return oldEntry;
        }

        private boolean addMCEntry(MarginCallAllocation alloc, MarginCallEntry entry) {
            if (!isCancelled() && alloc != null && entry != null) {
                entry.addAllocation(alloc);
                return true;
            }
            return false;
        }

        private void saveEntries(List<MarginCallEntry> entries, List<String> messages) {
            ExecutionContext context = buildMarginCallEntriesExecutionContext(entries);
            CollateralTaskWorker saveWorker = CollateralTaskWorker.getInstance(CollateralTaskWorker.TASK_SAVE, context, setActionAndFilterEntries(entries, context, messages));
            saveWorker.addPropertyChangeListener(propListener);
            saveWorker.process();
        }

        private List<MarginCallEntry> setActionAndFilterEntries(List<MarginCallEntry> entries, ExecutionContext context, List<String> messages) {
            List<MarginCallEntry> filteredEntries = new ArrayList<>();
            if (context != null) {
                for (MarginCallEntry mcEntry : entries) {
                    ContextUserAction allocAction = getAllocateAction(mcEntry, context);
                    if (allocAction != null) {
                        mcEntry.setAction(allocAction.getWorkflowAction());
                        filteredEntries.add(mcEntry);
                    } else {
                        messages.add("Could not find any allocate action for contract " + mcEntry.getCollateralConfigId() + " on process date " + mcEntry.getProcessDate() + " and status " + mcEntry.getStatus());
                    }
                }
            }
            return filteredEntries;
        }

        private ContextUserAction getAllocateAction(MarginCallEntry mcEntry, ExecutionContext context) {
            ContextUserAction allocAction = new ContextUserAction();
            if (context != null) {
                CollateralContext collContext = context.getCollateralContext();
                allocAction = collContext.getUserAction(mcEntry, ContextUserAction.ACTION_ALLOCATE);
            }
            return allocAction;
        }

        private ExecutionContext buildMarginCallEntriesExecutionContext(List<MarginCallEntry> entries) {
            ExecutionContext context = null;
            if (!Util.isEmpty(entries) && entries.get(0) != null) {
                CollateralContext collContext = entries.get(0).getCollateralContext();
                if (collContext == null) {
                    collContext = ServiceRegistry.getDefaultContext();
                }
                context = new ExecutionContext(collContext, ServiceRegistry.getDefaultExposureContext());
            }
            return context;
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
        protected void process(List<String> strings) {
            for (String s : strings) {
                AllocationsImporterWindow.this.textArea.append(s + '\n');
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
                AllocationsImporterWindow.this.kit.insertHTML(AllocationsImporterWindow.this.doc,
                        AllocationsImporterWindow.this.doc.getLength(), chaine, 0, 0, null);
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
