package calypsox.apps.bo;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import calypsox.tk.util.SantImportCAManualConfirmationFile;
import com.calypso.tk.core.Util;


public class SantImportCAManualConfirmationFrame extends JFrame {


    private static final long serialVersionUID = 1L;
    private static final String WINDOW_TITLE = "CA File Uploader";
    private static final String OPEN_FILE_BUTTON_TEXT = "Open";
    protected static final String CHECK_BUTTON_TEXT = "Check";
    protected static final String PROCESS_BUTTON_TEXT = "Process";
    private static final int FILE_NAME_FIELD_SIZE = 100;
    private static final int LOG_ROWS = 20;
    private static final int LOG_COLUMNS = 80;
    private JPanel mainPanel = null;
    protected JTextArea logArea = null;
    protected File inputFile = null;


    public SantImportCAManualConfirmationFrame() {
        setTitle(WINDOW_TITLE);
        mainPanel = buildMainPanel();
        add(mainPanel);
        pack();
    }


    private JPanel buildMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        JPanel filePanel = buildFilePanel();
        JPanel actionPanel = buildActionPanel();
        JPanel logPanel = buildLogPanel();
        panel.add(filePanel);
        panel.add(actionPanel);
        panel.add(logPanel);
        return panel;
    }


    private JPanel buildFilePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        JTextField fileNameField = new JTextField(FILE_NAME_FIELD_SIZE);
        fileNameField.setEditable(false);
        JButton openFileButton = new JButton(OPEN_FILE_BUTTON_TEXT);
        openFileButton.addActionListener(new OpenFileActionListener(fileNameField));
        panel.add(fileNameField);
        panel.add(openFileButton);
        return panel;
    }


    protected JPanel buildActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        JButton checkButton = new JButton(CHECK_BUTTON_TEXT);
        checkButton.addActionListener(new CheckFileActionListener());
        JButton processButton = new JButton(PROCESS_BUTTON_TEXT);
        processButton.addActionListener(new ProcessFileActionListener());
        panel.add(checkButton);
        panel.add(processButton);
        return panel;
    }


    private JPanel buildLogPanel() {
        JPanel panel = new JPanel();
        logArea = new JTextArea(LOG_ROWS, LOG_COLUMNS);
        logArea.setEditable(false);
        panel.add(logArea);
        return panel;
    }


    protected void clearLog() {
        logArea.setText("");
    }


    private class OpenFileActionListener implements ActionListener {

        private JTextField fileNameField = null;

        public OpenFileActionListener(JTextField fileNameField) {
            this.fileNameField = fileNameField;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.showOpenDialog(fileNameField);
            inputFile = fileChooser.getSelectedFile();
            if (this.fileNameField != null && inputFile != null) {
                this.fileNameField.setText(inputFile.getAbsolutePath());
            }
        }
    }


    private class CheckFileActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            clearLog();
            if (inputFile != null && !Util.isEmpty(inputFile.getAbsolutePath())) {
                String filePath = inputFile.getAbsolutePath();
                List<String> errors = new ArrayList<String>();
                boolean fileOK = SantImportCAManualConfirmationFile.getInstance().checkInputFile(filePath, errors);
                if (fileOK) {
                    logArea.append("File checked without errors");
                } else {
                    for (String error : errors) {
                        logArea.append(error);
                        logArea.append("\n");
                    }
                }
            }
        }
    }


    private class ProcessFileActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            clearLog();
            if (inputFile != null && !Util.isEmpty(inputFile.getAbsolutePath())) {
                String filePath = inputFile.getAbsolutePath();
                List<String> errors = new ArrayList<String>();
                boolean fileOK = SantImportCAManualConfirmationFile.getInstance().processInputFile(filePath, errors);
                if (fileOK) {
                    logArea.append("File processed OK.");
                } else {
                    logArea.append("File processed without errors");
                    logArea.append("\n");
                    if (errors.size() > 0) {
                        for (String error : errors) {
                            logArea.append(error);
                            logArea.append("\n");
                        }
                    }
                }
            }
        }
    }


}