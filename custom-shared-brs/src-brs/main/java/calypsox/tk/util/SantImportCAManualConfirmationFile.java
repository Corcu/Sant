package calypsox.tk.util;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;


public class SantImportCAManualConfirmationFile {


    private static SantImportCAManualConfirmationFile instance = null;
    private final List<String> errorFileLines = new ArrayList<String>();
    private long maxDocumentSize = 9223372036854775807L;


    private SantImportCAManualConfirmationFile() {
        // Do nothing
    }


    public static SantImportCAManualConfirmationFile getInstance() {
        if (SantImportCAManualConfirmationFile.instance == null) {
            SantImportCAManualConfirmationFile.instance = new SantImportCAManualConfirmationFile();
        }
        return SantImportCAManualConfirmationFile.instance;
    }


    public static void setInstance(SantImportCAManualConfirmationFile mockInstance) {
        SantImportCAManualConfirmationFile.instance = mockInstance;
    }


    public boolean checkInputFile(String filePath, List<String> errors) {
        if(filePath.toLowerCase().endsWith(".pdf")){
            return true;
        }
        else{
            errors.add("File imported is not a PDF format.");
            return false;
        }
    }


    public boolean processInputFile(String filePath, List<String> errors) {
        boolean fileOk = true;
        DSConnection dsCon = DSConnection.getDefault();
        if (!checkInputFile(filePath, errors)) {
            return false;
        }
        BOMessage message = new BOMessage();
        message.setMessageType("CA_INPUT_MANUAL_CONFIRM");
        message.setStatus(Status.S_NONE);
        message.setAction(Action.NEW);
        message.setSubAction(Action.NONE);
        message.setLongId(0);
        message.setMessageClass(0);
        message.setProductType("CA");
        message.setProductFamily("CA");
        message.setEventType("INPUT_CA_MANUAL_CONFIRM");
        message.setLanguage("English");
        message.setCreationDate(new JDatetime());
        message.setTradeUpdateDatetime(new JDatetime());
        message.setMatchingB(false);
        message.setFormatType(null);
        message.setExternalB(true);
        message.setGateway("FileSystem");
        message.setAddressMethod("MAIL");
        message.setFormatType("TEXT");
        message.setTemplateName("EmptyTemplate.htm");
        message.setSenderContactId(0);
        message.setReceiverContactId(0);

        try {
            final long messageId = DSConnection.getDefault().getRemoteBO().save(message, 0, null);
            Log.info(this, "Message saved with id: " + messageId);
            // Generic Comment
            BOMessage savedMessage = null;
            if(messageId > 0){
                try {
                    savedMessage = DSConnection.getDefault().getRemoteBackOffice().getMessage(messageId);
                } catch (CalypsoServiceException e) {
                    Log.error(this.getClass().getName(), "Error getting BOMessage.", e);
                }
            }
            GenericComment comment = new GenericComment(savedMessage);
            File currentDropedFile = new File(filePath);
            byte[] bytes = this.getFileBytes(currentDropedFile);
            if ((long)bytes.length > maxDocumentSize) {
                Log.error(this, "Document is too large. Max size is " + maxDocumentSize);
            }
            comment.setComment("Upload Generic Comment");
            comment.setType("CA Manual Confirmation");
            comment.setDocument(bytes, "pdf", false);
            comment.setEnteredUser(DSConnection.getDefault().getUser());
            comment.setEnteredDateTime(new JDatetime());
            DSConnection.getDefault().getRemoteBO().saveGenericComment(comment, comment.getId() != 0);

        } catch (final CalypsoServiceException e) {
            final String errorMessage = String.format("Could not save %s message", "CA_INPUT_MANUAL_CONFIRM");
            Log.error(this, errorMessage, e);
        }

        return fileOk;
    }


    private void addErrorFileLines(final String errorFileLine) {
        errorFileLines.add(errorFileLine);
    }


    protected byte[] getFileBytes(File file) {
        if (file != null) {
            try {
                FileInputStream inputStream = new FileInputStream(file);
                Throwable var3 = null;
                byte[] var5;
                try {
                    byte[] bytes = new byte[(int)file.length()];
                    inputStream.read(bytes, 0, (int)file.length());
                    var5 = bytes;
                } catch (Throwable var16) {
                    var3 = var16;
                    throw var16;
                } finally {
                    if (inputStream != null) {
                        if (var3 != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable var15) {
                                var3.addSuppressed(var15);
                            }
                        } else {
                            inputStream.close();
                        }
                    }
                }
                return var5;
            } catch (FileNotFoundException var18) {
                Log.error(this, var18.getMessage());
            } catch (IOException var19) {
                Log.error(this, var19.getMessage());
            }
        }
        return null;
    }


}