package calypsox.engine.optimizer;

import calypsox.engine.optimizer.messages.OptimJMSQueueAnswer;
import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.collateral.allocation.optimizer.importer.OptimAllocsImportConstants;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimAllocationBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimContractAllocsBean;
import calypsox.tk.interfaces.optimizer.importstatus.Error;
import calypsox.tk.interfaces.optimizer.importstatus.ErrorsList;
import calypsox.tk.interfaces.optimizer.importstatus.ImportStatusList.ImportStatus;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.tk.util.SantCollateralOptimConstants;
import calypsox.tk.util.ScheduledTaskOPTIM_ALLOC_IMPORT;
import calypsox.tk.util.optimizer.OptimizerStatusUtil;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class SantOptimizerAllocationEngine extends SantOptimizerBaseEngine {

    /**
     * @param configName
     * @param dsCon
     * @param hostName
     * @param esPort
     */
    public SantOptimizerAllocationEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }


    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SantOptimizerAllocationEngine";

    public static final String ST_OPTIM_ALLOC_IMPORT = "OPTIM_ALLOC_IMPORT";

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    protected JMSQueueAnswer importMessage(JMSQueueMessage jmsMessage,
                                           List<Task> tasks) throws Exception {
        boolean isProcessingOK = false;
        //
        String fileContent = jmsMessage.getText();
        String fileName = (Util.isEmpty(jmsMessage.getCorrelationId()) ? "Allocations.txt"
                : jmsMessage.getCorrelationId());// jmsMessage.getCorrelationId();
        OptimJMSQueueAnswer importAnswer = new OptimJMSQueueAnswer(fileName);
        isProcessingOK = importAllocationFile(fileContent, fileName,
                new JDatetime(), tasks, importAnswer);

        importAnswer.setCode((isProcessingOK ? JMSQueueAnswer.OK
                : JMSQueueAnswer.KO));
        importAnswer.setImportStatusCode(importAnswer.getCode());

        return importAnswer;
    }

    /**
     * @param nbRows
     * @param tasks
     * @param importAnswer
     * @return
     */
    private boolean importAllocationFile(String fileContent, String fileName,
                                         JDatetime processDate, List<Task> tasks,
                                         OptimJMSQueueAnswer importAnswer) {
        boolean proccesOK = true;
        try {
            // first get the scheduledTask of allocations import
            ScheduledTaskOPTIM_ALLOC_IMPORT st = (ScheduledTaskOPTIM_ALLOC_IMPORT) DSConnection
                    .getDefault().getRemoteBO()
                    .getScheduledTaskByExternalReference(ST_OPTIM_ALLOC_IMPORT);
            if (st == null) {
                tasks.add(buildTask(
                        "No configuration found for the allocation import process",
                        0, OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,
                        "Collateral"));
                importAnswer
                        .addSingleStatus(
                                OptimAllocsImportConstants.ERR_UNABLE_TO_LAUNCH_PROCESS,
                                "No configuration found for the allocation import process");
                return false;

            }

            String path = st
                    .getAttribute(ScheduledTaskOPTIM_ALLOC_IMPORT.FILEPATH);
            String startFileName = fileName;
            // create a file in the file system using the content retrieved from
            // the routing message
            FileOutputStream stream = null;
            PrintStream out = null;
            try {
                stream = new FileOutputStream(new File(path + fileName));
                out = new PrintStream(stream);
                out.print(fileContent);

            } catch (Exception ex) {
                Log.error(this, ex);
                tasks.add(buildTask(
                        "Unable to read the message content as an allocation file",
                        0, OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,
                        "Collateral"));
                importAnswer
                        .addSingleStatus(
                                OptimAllocsImportConstants.ERR_UNABLE_TO_READ_MSG,
                                "Unable to read the message content as an allocation file");

                return false;
            } finally {
                try {
                    if (stream != null)
                        stream.close();
                    if (out != null)
                        out.close();
                } catch (Exception e) {
                    Log.error(this, e);
                }
            }
            // st
            // .getAttribute(ScheduledTaskOPTIM_ALLOC_IMPORT.STARTFILENAME);

            // we add the header and assign the fileWriter to the logs files.
            // We check if the log files does'nt exist in the system. If it?s
            // the case then stop the process.
            String fileToProcess = getAndChekFileToProcess(path, startFileName);
            final String fullFileName = path + fileToProcess;

            if (!Util.isEmpty(fileToProcess)) {

                // Just after file verifications, this method will make a copy
                // into the
                // ./import/copy/ directory
                // FileUtility.copyFileToDirectory(fullFileName, path +
                // "/copy/");
                proccesOK = st.importFileContentMultiThreads(fullFileName,
                        processDate);

                // handle the invalid items
                if (!Util.isEmpty(st.getInvalidItems())) {
                    proccesOK = false;
                    importAnswer.removeSingleStatus();

                    List<MarginCallEntryDTO> failedEntries = new ArrayList<MarginCallEntryDTO>();

                    for (Object invalidItem : st.getInvalidItems()) {
                        if (invalidItem instanceof OptimContractAllocsBean) {
                            // update the status of the margin call
                            // contract as failed optimization

                            OptimContractAllocsBean invalidAllocContract = (OptimContractAllocsBean) invalidItem;
                            Double contractId = st
                                    .getImportContext()
                                    .getContractsNameForId()
                                    .get(invalidAllocContract.getContractName());
                            int mccId = (contractId == null ? 0 : contractId
                                    .intValue());
                            List<String> errors = new ArrayList<String>();

                            MarginCallEntry entry = CollateralManagerUtil
                                    .loadEntry(mccId, st.getImportContext()
                                            .getExecutionContext(), errors);

                            if (entry != null && entry.getId() > 0) {
                                // set the treatment id
                                entry.addAttribute(SantCollateralOptimConstants.OPTIMIZER_LAST_TREATMENT_ID,
                                        st.getImportContext().getExecutionId());
                                failedEntries.add(entry.toDTO());
                            }

                            importAnswer
                                    .addImportStatus(buildImportStatusForContract(invalidAllocContract));
                        } else if (invalidItem instanceof AllocImportErrorBean) {
                            importAnswer
                                    .addImportStatus(buildImportStatusForContract(
                                            (AllocImportErrorBean) invalidItem,
                                            fileName));
                        }
                        // update the status of the margin call
                        // contract as failed optimization
                        if (!Util.isEmpty(failedEntries)) {
                            OptimizerStatusUtil.updateOptimizerStatus(
                                    failedEntries,
                                    OptimizerStatusUtil.FAILED_OPTIMIZATION);
                        }

                    }

                }
                st.getImportContext().getExecutionId();

                // move the allocation file to the corresponding folder
                // depending on the import status
                FileUtility.moveFile(fullFileName, path
                        + (proccesOK ? "ok/" : "fail/") + fileName + "_"
                        + st.getImportContext().getExecutionId());
            } else {
                tasks.add(buildTask(
                        "The specified file does not exist in the import folder",
                        0, OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,
                        "Collateral"));
                importAnswer
                        .addSingleStatus(
                                OptimAllocsImportConstants.ERR_UNABLE_TO_GET_ALLOC_FILE,
                                "The specified file "
                                        + startFileName
                                        + " does not exist in the import folder");
                proccesOK = false;
            }
        } catch (Exception e) {
            tasks.add(buildTask(
                    "Unexpected error while importing allocations : "
                            + e.getMessage(), 0,
                    OptimAllocsImportConstants.TASK_EXCEPTION_TYPE,
                    "Collateral"));
            Log.error(this, e);
            proccesOK = false;
        }
        return proccesOK;

    }

    /**
     * @param contract
     * @return
     */
    private ImportStatus buildImportStatusForContract(
            OptimContractAllocsBean contract) {
        ImportStatus importStatus = new ImportStatus();
        importStatus.setImportKey(contract.getKey());
        importStatus.setImportStatus("KO");
        ErrorsList errors = importStatus.getErrors();

        for (OptimAllocationBean alloc : contract.getAllocations()) {

            for (AllocImportErrorBean errorMessage : alloc.getErrorsList()) {
                // only send back the errors with an error code (the others are
                // to for Calypso users)
                if (Util.isEmpty(errorMessage.getCode())) {
                    continue;
                }

                Error error = new Error();

                error.setDescription(alloc.getKey() + " : "
                        + errorMessage.getValue());
                error.setCode(errorMessage.getCode());

                if (errors == null) {
                    errors = new ErrorsList();
                }

                errors.getErrors().add(error);
            }
        }
        importStatus.setErrors(errors);
        return importStatus;
    }

    /**
     * @param errorBean
     * @param importKey
     * @return
     */
    private ImportStatus buildImportStatusForContract(
            AllocImportErrorBean errorBean, String importKey) {
        ImportStatus importStatus = new ImportStatus();
        importStatus.setImportKey(importKey);
        importStatus.setImportStatus("KO");
        ErrorsList errors = importStatus.getErrors();

        Error error = new Error();

        error.setDescription(errorBean.getValue());
        error.setCode(errorBean.getCode());

        if (errors == null) {
            errors = new ErrorsList();
        }

        errors.getErrors().add(error);

        importStatus.setErrors(errors);
        return importStatus;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.engine.optimizer.SantOptimizerBaseEngine#isAcceptedEvent(com
     * .calypso.tk.event.PSEvent)
     */
    protected boolean isAcceptedEvent(PSEvent psevent) {
        return false;
    }

    /**
     * @param path
     * @param startFileName
     * @return the file name to import if every thing is okay (only one file
     * found as expected and the content of the file is correct)
     */
    public String getAndChekFileToProcess(String path, String startFileName) {
        String fileToProcess = "";
        ArrayList<String> files = CollateralUtilities.getListFiles(path,
                startFileName);
        // We check if the number of matching files is 1.
        if (files.size() == 1) {
            fileToProcess = files.get(0);
        } else {
            Log.error(
                    LOG_CATEGORY_SCHEDULED_TASK,
                    "The number of matches for the filename in the path specified is 0 or greater than 1.");
        }
        return fileToProcess;
    }

    @Override
    protected JMSQueueAnswer importMessage(String message, List<Task> tasks)
            throws Exception {
        return null;
    }
}
