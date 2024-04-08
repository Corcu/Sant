package calypsox.engine.optimizer.messages;

import calypsox.tk.interfaces.optimizer.importstatus.Error;
import calypsox.tk.interfaces.optimizer.importstatus.ErrorsList;
import calypsox.tk.interfaces.optimizer.importstatus.ImportStatusList;
import calypsox.tk.interfaces.optimizer.importstatus.ImportStatusList.ImportStatus;
import calypsox.tk.util.JMSQueueAnswer;
import org.jfree.util.Log;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author aela
 */
public class OptimJMSQueueAnswer extends JMSQueueAnswer {

    protected ImportStatusList importStatusList = new ImportStatusList();

    protected ImportStatus singleImportStatus = null;

    protected String importKey = "";

    @Override
    public String toString() {
        return generateXML();
    }

    public OptimJMSQueueAnswer(String importKey) {
        super();
        singleImportStatus = new ImportStatus();

        // set import status
        singleImportStatus.setImportStatus(getCode());
        singleImportStatus.setImportKey(importKey);

        importStatusList.getImportStatuses().add(singleImportStatus);

        this.importKey = importKey;
    }

    public OptimJMSQueueAnswer() {
        this(null);
    }

    public void removeSingleStatus() {
        importStatusList.getImportStatuses().remove(singleImportStatus);
    }

    /**
     * @return
     */
    protected String generateXML() {
        try {
            JAXBContext jc = JAXBContext.newInstance(ImportStatusList.class);
            Marshaller marshaller = jc.createMarshaller();
            OutputStream os = new ByteArrayOutputStream();
            marshaller.marshal(importStatusList, os);

            os.close();
            return os.toString();
        } catch (JAXBException e) {
            Log.error(this, e);
        } catch (IOException e) {
            Log.error(this, e);
        }
        return "";
    }

    /**
     * @param errroCode
     * @param errorMessage
     */
    public void addSingleStatus(String errroCode, String errorMessage) {
        addSingleStatus(importKey, errroCode, errorMessage);
    }

    /**
     * @param errroCode
     * @param errorMessage
     */
    public void addSingleStatus(String importKey, String errroCode,
                                String errorMessage) {

        // set import status
        singleImportStatus.setImportStatus(getCode());
        singleImportStatus.setImportKey(importKey);
        ErrorsList errors = singleImportStatus.getErrors();

        calypsox.tk.interfaces.optimizer.importstatus.Error error = new Error();

        error.setDescription(errorMessage);
        error.setCode(errroCode);

        if (errors == null) {
            errors = new ErrorsList();
        }
        errors.getErrors().add(error);
        singleImportStatus.setErrors(errors);

        //importStatusList.getImportStatuses().add(singleImportStatus);

    }

    /**
     * @return the importKey
     */
    public String getImportKey() {
        return importKey;
    }

    /**
     * @param importKey the importKey to set
     */
    public void setImportKey(String importKey) {
        this.importKey = importKey;
    }

    public void setImportStatusCode(String statusCode) {
        if (singleImportStatus != null) {
            singleImportStatus.setImportStatus(statusCode);
        }
    }

    /**
     * @param importStatus
     */
    public void addImportStatus(ImportStatus importStatus) {
        importStatusList.getImportStatuses().add(importStatus);
    }

}
