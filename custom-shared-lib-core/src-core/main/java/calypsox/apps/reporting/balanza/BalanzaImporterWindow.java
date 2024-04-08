/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting.balanza;

import calypsox.tk.util.bean.ExternalBalanzaBean;
import com.calypso.apps.util.AppUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.collateral.util.TaskWorker;
import com.calypso.tk.core.*;
import com.calypso.tk.core.Action;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BalanzaImporterWindow extends JFrame {

    // for process global percent completed management
    protected Integer total = 0;

    private static MessageFormat stepTitleStyle = new MessageFormat("<b><u>{0}</u></b>");
    private static MessageFormat stepStatusStyle = new MessageFormat("<i>{0}</i>");
    private static MessageFormat stepErrorStyle = new MessageFormat("<p><i color=\"RED\">{0}</i></p>");
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
    private static String parsedPeriod;

    public static final int STEP_TITLE_STYLE = 1;
    public static final int STEP_STATUS_STYLE = 2;
    public static final int STEP_ERROR_STYLE = 3;

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JTextField filePathArea = null;
    private JTextField periodTextArea = null;

    private TextPaneAppender textArea;
    private static JDate valuationDate;

    protected JProgressBar progressBar = null;
    protected HTMLEditorKit kit = null;
    protected HTMLDocument doc = null;
    protected BalanzaImportSwingWorker swingWorker = null;
    protected PropertyChangeListener propListener = null;


    /**
     * Create the frame.
     */
    public BalanzaImporterWindow() {
        setSize(new Dimension(600, 450));
        setMinimumSize(new Dimension(600, 450));
        centerOnParent(this, true);
        this.swingWorker = new BalanzaImportSwingWorker();
        //this.entry = entry;
        propListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    BalanzaImporterWindow.this.progressBar.setValue((Integer) evt.getNewValue());
                } else if (TaskWorker.PROPERTY_MESSAGE.equals(evt.getPropertyName())) {
                    textArea.append(evt.getNewValue().toString());
                }
            }
        };
        init();
    }


    protected void init() {
        ActionListener actionListener = new BalanzaImportActionListener();
        setTitle("BalanzaExternalImporterWindow");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));


        this.periodTextArea = new JTextField();

        final JPanel panelPeriod = new JPanel();
        // .setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelPeriod.setBorder(new TitledBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("")
                .getBorder(), BorderFactory.createEmptyBorder(0, 0, 5, 0)), "Enter Period of the Import (YYYYMM) :", TitledBorder.LEADING,
                TitledBorder.TOP, null, new Color(51, 153, 255)));
        //panelPeriod.add(Box.createRigidArea(new Dimension(10, 0)));
        panelPeriod.setLayout(new BoxLayout(panelPeriod, BoxLayout.X_AXIS));
        panelPeriod.add(Box.createRigidArea(new Dimension(10, 0)));

        this.periodTextArea = new JTextField();
        this.periodTextArea.setMaximumSize(new Dimension(150, this.periodTextArea.getPreferredSize().height));

        this.periodTextArea.setColumns(1);
        panelPeriod.add(this.periodTextArea);
        panelPeriod.add(Box.createRigidArea(new Dimension(400, 0)));
        getContentPane().add(panelPeriod);


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

    public class BalanzaImportActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if ("choose".equals(e.getActionCommand())) {
                JFileChooser chooser = new JFileChooser();
                chooser.showOpenDialog(getContentPane());
                File file = chooser.getSelectedFile();
                BalanzaImporterWindow.this.filePathArea.setText(file.getAbsolutePath());

            } else if ("import".equals(e.getActionCommand())) {
                String filePath = BalanzaImporterWindow.this.filePathArea.getText();
                if (Util.isEmpty(filePath)) {
                    AppUtil.displayError(getContentPane(), "Please choose a file before");
                    return;
                }
                if (BalanzaImporterWindow.this.swingWorker.isDone()) {
                    if (!AppUtil.displayQuestion("The import was already performed. \nImport again?", getContentPane())) {
                        return;
                    }
                }
                BalanzaImporterWindow.this.swingWorker = new BalanzaImportSwingWorker();
                BalanzaImporterWindow.this.swingWorker.execute();

            } else if ("close".equals(e.getActionCommand())) {
                BalanzaImporterWindow.this.swingWorker.cancel(true);
                dispose();
            }

        }
    }

    class BalanzaImportSwingWorker extends SwingWorker<Integer, String> {

        private static final String BALANZA_EXTERNAL = "BALANZA_EXTERNAL";
        private static final String GATEWAY_FILE_SYSTEM = "FileSystem";

        public BalanzaImportSwingWorker() {
            addPropertyChangeListener(propListener);
        }


        @Override
        public Integer doInBackground() {
            try {
                BalanzaImporterWindow.this.progressBar.setValue(0);
                BalanzaImporterWindow.this.textArea.setText("");

                if (Util.isEmpty(periodTextArea.getText())) {
                    formatAndPublish("Period is Invalid.", STEP_ERROR_STYLE);
                    return 0;
                }
                try {
                    sdf.setLenient(false);
                    Date date = sdf.parse(periodTextArea.getText());
                    parsedPeriod = sdf.format(date);
                } catch (Exception e) {
                    formatAndPublish("Period is Invalid.", STEP_ERROR_STYLE);
                    return 0;
                }

                String filePath = BalanzaImporterWindow.this.filePathArea.getText();
                CSVExternalBalanzaReader reader = new CSVExternalBalanzaReader( JDate.getNow(), filePath);
                formatAndPublish("Reading lines from the file...", STEP_TITLE_STYLE);
                List<String> messages = new ArrayList<String>();
                List<ExternalBalanzaBean> balanzaBeans = new ArrayList<ExternalBalanzaBean>();
                try {
                    balanzaBeans = reader.readLines(messages);
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
                if (!Util.isEmpty(balanzaBeans)) {
                    publish("<br>");
                    formatAndPublish(balanzaBeans.size() + " successfully lines read.",
                            STEP_STATUS_STYLE);
                } else {
                    publish("<br>");
                    publish("No lines correctly read from the file");
                    publish("<br>");
                    formatAndPublish("Import finished", STEP_TITLE_STYLE);
                    return 0;
                }

                publish("<br>");
                formatAndPublish("validating entered records... ", STEP_STATUS_STYLE);

                // start the import
                BalanzaImporterWindow.this.total = balanzaBeans.size();

                String periodo = validatePeriod(balanzaBeans, messages);
                if (Util.isEmpty(periodo)) {
                    formatAndPublish("Import canceled.", STEP_TITLE_STYLE);
                    return 0;
                };


                publish("<br>");
                formatAndPublish("Found " + BalanzaImporterWindow.this.total + " valid records for the period : " + periodo, STEP_STATUS_STYLE);

                int result = JOptionPane.showConfirmDialog((Component) null,
                        "Ready to upload file to File System? (Period is " + periodo + ") " +
                                "\n Valid records in file : " + balanzaBeans.size(),
                            "alert", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result != 0) {
                    formatAndPublish("Operation canceled by user.", STEP_STATUS_STYLE);
                    return 0;
                }

                publish("<br>");
                publish("Preparing Document...");

                BalanzaImporterWindow.this.total = balanzaBeans.size();
                prepareAndSaveDocument(balanzaBeans, messages);


                publish("<br>");
                formatAndPublish("Import finished", STEP_TITLE_STYLE);
            } catch (Exception e) {
                Log.error(this, e);
                formatAndPublish("Error preparing Document. Check log files. Message :" + e.getLocalizedMessage(), STEP_ERROR_STYLE);
                return 100;
            }
            return 100;
        }

        private Integer prepareAndSaveDocument(List<ExternalBalanzaBean> balanzaBeans, List<String> messages) throws RemoteException {
            try {

                StringBuilder records = transformToHost(balanzaBeans);

                if (Util.isEmpty(records.toString())) {
                    formatAndPublish("Error generating host format. File is empty. Process aborted.", STEP_ERROR_STYLE);
                    return 100;
                }
                BOMessage message = new BOMessage();
                message.setMessageType(BALANZA_EXTERNAL);
                message.setAddressMethod(BALANZA_EXTERNAL);
                message.setTransferLongId(0);
                message.setTradeLongId(0);

                LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), "BSTE");
                message.setLegalEntityId(le.getId());
                message.setAction(Action.NEW);
                message.setSubAction(Action.NONE);
                message.setProductType("ALL");
                message.setLinkedLongId(0);
                message.setStatus(Status.S_NONE);
                message.setFormatType("TEXT");
                message.setCreationDate(new JDatetime());
                message.setSenderId(le.getId());
                message.setReceiverId(le.getId());
                message.setSenderRole("ProcessingOrg");
                message.setReceiverRole("ProcessingOrg");
                message.setGateway(GATEWAY_FILE_SYSTEM);
                message.setAttribute("Period", parsedPeriod);
                LEContact contact = BOCache.getContact(DSConnection.getDefault(), "ProcessingOrg", le, "Default", "ALL", le.getId());

                message.setReceiverContactId(contact.getId());
                message.setSenderContactId(contact.getId());
                AdviceDocument adviceDocument = new AdviceDocument(message, new JDatetime(new Date()));
                adviceDocument.setDocument(new StringBuffer(records));
                long[] longs = DSConnection.getDefault().getRemoteBO().saveMessageDocument(message, adviceDocument, 0, "GenericSenderEngine", "Balanza External Window");

                publish ("Balanza de Pagos External File Message id = " + longs[0] + ", created succesfully");

            } catch (Exception e) {
                publish ("Error generating message : " + e.getMessage() + " Process aborted.");
                Log.error("CALYPSOX", e);
                return 100;
            }
            return 0;
        }

        private String validatePeriod(List<ExternalBalanzaBean> balanzaBeans, List<String> messages) {
            SimpleDateFormat periodFormatter  = new SimpleDateFormat("yyyyMM");
            String periodo = periodTextArea.getText().trim();

            double i = 0;
            for (ExternalBalanzaBean bean: balanzaBeans) {
                 if (!periodo.equals(periodFormatter.format(bean.getPeriodo()))) {
                    publish("<br>");
                    formatAndPublish("Check failed. Found different periods in file : " + periodo + " and " + periodFormatter.format(bean.getPeriodo()), STEP_ERROR_STYLE);
                    return null;
                }
                 bean.setPeriodoStr( periodFormatter.format(bean.getPeriodo()));
                 i++;
                setProgress((int) ((getActualPercentageCompleted(i) + 0.5) * 100));
            }
            return periodo;
        }

        private int generateHostFormat(JDate valDate, List<ExternalBalanzaBean> balanzaBeans)  throws  Exception {
            try {
                StringBuilder records = transformToHost(balanzaBeans);
                if (!Util.isEmpty(records.toString())) {
                    JFileChooser chooser = new JFileChooser();

                    FileNameExtensionFilter filter = new FileNameExtensionFilter("csv", "csv", "csv");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showSaveDialog(getContentPane());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File fileOut = chooser.getSelectedFile();
                        FileUtils.writeStringToFile(fileOut, records.toString(), Charset.forName("UTF-8"));
                        publish("<br>");
                        formatAndPublish("File generated successfully.", STEP_TITLE_STYLE);
                        return 0;
                    }
                    publish("<br>");
                    formatAndPublish("Operation Canceled.", STEP_TITLE_STYLE);
                }
                return 100;
            } catch (Exception e) {
                publish("<br>");
                formatAndPublish("Error generating host format.", STEP_ERROR_STYLE);
                formatAndPublish("message is : " + e.getMessage(), STEP_ERROR_STYLE);
                return -1;
            }
        }


        private StringBuilder transformToHost(List<ExternalBalanzaBean> balanzaItems) throws RemoteException {

            BalanzaDePagosBuilder
                    builder = new BalanzaDePagosBuilder();
            StringBuilder records = new StringBuilder();
            LegalEntity bste = BOCache.getLegalEntity(DSConnection.getDefault(), "BSTE");
            LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), bste.getId(), bste.getId(), "ALL", "TAXID");
            String nifPo = "";
            if (attr != null && !com.calypso.tk.core.Util.isEmpty(attr.getAttributeValue())) {
                nifPo = attr.getAttributeValue();
            }
            try {
                int i = 0;
                for (ExternalBalanzaBean bean : balanzaItems) {
                    records.append(builder.buildLine(nifPo, bean));
                    setProgress((int) ((getActualPercentageCompleted(i++) + 0.5) * 100));
                }


            } catch (Exception e) {
                Log.error(this, e);
                throw new RemoteException(e.getMessage());
            }
            return records;
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
                BalanzaImporterWindow.this.textArea.append(s + '\n');
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
                BalanzaImporterWindow.this.kit.insertHTML(BalanzaImporterWindow.this.doc,
                        BalanzaImporterWindow.this.doc.getLength(), chaine, 0, 0, null);
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
