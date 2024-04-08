package calypsox.camel.processor;


import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.PostingArray;
import com.calypso.tk.util.TaskArray;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author acd
 */

/**
 * Process the message send by MIC, Update BOCre status OK or KO
 * <p>
 * Example:
 * //<PETICION>EJNORMAL</PETICION>
 * //<EVENTO></EVENTO>
 * //<TIPOOPER>CO</TIPOOPER>
 * //<OBB>0030451510046005</OBB>
 * //<VERSOBB><VERSOBB>
 * //  <CONTRATO>003019994510000010</CONTRATO>
 * //  <SUBTIPO>510</SUBTIPO>
 * //  <IDEMPRO>0030</IDEMPRO>
 * //  <IDCENTO>1999</IDCENTO>
 * //  <TERMBTO>12345</TERMBTO>
 * //  <NUMDGO>00000008<NUMDGO>
 * //    <CERTIFIC>OPERACION EJECUTADA CORRECTAMENTE SIN MOVTOS. PROVISIONALES
 * //      <CERTIFIC> <CDTRPBAT>
 * //      </CDTRPBAT><CATRPBAT>
 * //    </CATRPBAT>
 * //    <CODEVERET>OK</CODEVERET>
 * //    <CODDGORC>00</CODDGORC>
 * //    <CODDGORS>00</CODDGORS>
 * //    <CODMSGRUT></CODMSGRUT>
 * //    <RUTINA></RUTINA>
 * //    <CODMSGRET>OB0070</CODMSGRET>
 * //    <MENMSGRET></MENMSGRET>
 * //    <PARMSGRET></PARMSGRET>
 * //    <REFERENCIA>RFERENCIA DE CRUZE PROPUESTA</REFERENCIA>
 */
public class CreMicReturnProcessor implements Processor {
    public static final String CODEVERET = "(?<=<CODEVERET>).*?(?=<\\/CODEVERET>)";
    public static final String MENMSGRET = "(?<=<MENMSGRET>).*?(?=<\\/MENMSGRET>)";
    public static final String REFERENCIA = "(?<=<REFERENCIA>).*?(?=<\\/REFERENCIA>)";
    public static final String CONTRATO = "(?<=<CONTRATO>).*?(?=<\\/CONTRATO>)";
    public static final String OBB = "(?<=<OBB>).*?(?=<\\/OBB>)";

    private static final String FILTER_CRE_ATT_CONTRATO_MIC_BY_OBB_DV = "FilterCreAttContratoMicByOBB";

    private static final String SENT = "SENT";

    @Override
    public void process(Exchange exchange) throws Exception {
        String returnMessage = exchange.getIn().getBody(String.class);
        //Log.system("MIC Response Message: ", returnMessage );

        final String status = getXmlTag(returnMessage, CODEVERET);
        final String comment = getXmlTag(returnMessage, MENMSGRET);
        final String reference = getXmlTag(returnMessage, REFERENCIA);
        final String contract = getXmlTag(returnMessage, CONTRATO);
        final String obb = getXmlTag(returnMessage, OBB);

        try {
            if (!Util.isEmpty(reference) && NumberUtils.isParsable(reference.trim())) {
                final Optional<BOCre> boCre = Optional.ofNullable(loadCre(Long.parseLong(reference.trim())));
                //Check if OK or KO
                boCre.ifPresent(creToSave -> {
                    if (isKO(status)) {
                        generateTaskEx(status, comment, creToSave);
                    }
                    creToSave.addAttribute(BOCreConstantes.SENT, status);
                    if (!Util.isEmpty(comment)) {
                        creToSave.addAttribute("COMMENT:", splitError(comment));
                    }
                    if (!Util.isEmpty(contract) && filterByOBB(obb)) {
                        creToSave.addAttribute(BOCreConstantes.CONTRATO_MIC, contract);
                    }
                    Log.system(this.getClass().getName(), "Saving response from MIC for Cre id: " + creToSave.getId() + " with accountBalance: " + creToSave.getAttributeValue("accountBalance"));
                    saveBOCre(creToSave);
                });

                if (!boCre.isPresent()) {
                    final Optional<BOPosting> boPosting = Optional.ofNullable(loadPosting(Long.parseLong(reference.trim())));
                    boPosting.ifPresent(postingToSave -> {
                        if (isKO(status)) {
                            generateTaskEx(status, comment, postingToSave);
                        }
                        postingToSave.addAttribute("SENT", status);
                        if (!Util.isEmpty(comment)) {
                            postingToSave.addAttribute("COMMENT:", splitError(comment));
                        }
                        saveBOPosting(postingToSave);
                    });
                }
            } else {
                Log.debug(this, "unparseable CreId on <REFERENCIA>");
            }
        } catch (Exception e) {
            Log.error(this, "Error: " + e);
        }
    }

    /**
     * Filter MIC messages by OBB end number. If the OBB ends in some
     * value of the DV ExcludeCreAttContratoMicByOBB
     * the contract will not be reported in the CRE
     * @param obb the obb value in MIC response
     * @return true if obb is empty or not ends with 7591
     */
    private boolean filterByOBB(String obb){
        if(!Util.isEmpty(obb)){
            obb = obb.trim();
            Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(),
                    FILTER_CRE_ATT_CONTRATO_MIC_BY_OBB_DV);
            if(!Util.isEmpty(domainValues)){
                for(String obbNum: domainValues){
                    if(obb.endsWith(obbNum)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * @param message
     * @param regexPattern
     * @return Get tag from xml message
     */
    public String getXmlTag(String message, String regexPattern) {
        Pattern murStartPattern = Pattern.compile(regexPattern);
        Matcher murStartMatcher = murStartPattern.matcher(message);
        if (murStartMatcher.find()) {
            return murStartMatcher.group(0);
        } else {
            Log.error(this,
                    String.format("Could not find field: ", message));
        }

        return "";
    }

    /**
     * @param creId
     * @return BOCre associate to MIC message
     */
    public BOCre loadCre(Long creId) {
        try {
            return DSConnection.getDefault().getRemoteBackOffice().getBOCre(creId);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading BoCre: " + creId + ": " + e);
        }
        return null;
    }

    public boolean saveBOCre(BOCre cre) {
        CreArray array = new CreArray();
        if (null != cre) {
            try {
                array.add(cre);
                DSConnection.getDefault().getRemoteBackOffice().saveCres(array);
                return true;
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error saving Cre: " + e.getCause());
                reprocessCre(array);
            }
        }
        return false;
    }


    /**
     * Wait 0,35 mls, reload and save cre
     *
     * @param array
     * @return
     */
    private boolean reprocessCre(CreArray array) {
        try {
            Thread.sleep(350);
            final BOCre cre = null != array ? array.get(0) : null;
            if (null != cre) {
                Log.debug(this.getClass().getName(), "Reprocessing Cre: " + cre.getId());
                BOCre newCre = loadCre(cre.getId());
                newCre.addAttribute(SENT, cre.getAttributeValue(SENT));
                array.clear();
                array.add(newCre);
            }
            DSConnection.getDefault().getRemoteBackOffice().saveCres(array);
            return true;
        } catch (CalypsoServiceException | InterruptedException e) {
            Log.error(this, "Error saving Cre: " + e.getCause());
        }
        return false;
    }


    private String splitError(String error) {
        String errorMessage = "";
        if (!Util.isEmpty(error)) {
            if (error.length() > 40) {
                errorMessage = error.substring(40);
            } else {
                errorMessage = error;
            }
        }
        return errorMessage;
    }

    private boolean isKO(String status) {
        return "KO".equalsIgnoreCase(status);
    }


    /**
     * Generate task on KO
     *
     * @param status
     * @param comment
     * @param boCre
     */
    private void generateTaskEx(String status, String comment, BOCre boCre) {
        Task taskException = new Task();
        taskException.setStatus(Task.NEW);
        taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        taskException.setEventType("EX_CRE_KO_EXCEPTION");
        taskException.setComment(comment);

        try {
            TaskArray task = new TaskArray();
            task.add(taskException);
            DSConnection.getDefault().getRemoteBackOffice().saveAndPublishTasks(task, 0L, null);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error saving task: " + e.getCause());
        }
    }

    /**
     * @param postingId
     * @return BOPosting associate to MIC message
     */
    public BOPosting loadPosting(Long postingId) {
        try {
            return DSConnection.getDefault().getRemoteBackOffice().getBOPosting(postingId);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading BOPosting: " + postingId + ": " + e);
        }
        return null;
    }

    /**
     * @param posting
     * @return boolean with the result of save
     */
    public boolean saveBOPosting(BOPosting posting) {
        PostingArray array = new PostingArray();
        if (null != posting) {
            try {
                array.add(posting);
                DSConnection.getDefault().getRemoteBackOffice().savePostings(array);
                return true;
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error saving Posting: " + e.getCause().getMessage());
                reprocessPosting(array);
            }
        }
        return false;
    }

    /**
     * Wait 0,35 mls, reload and save posting
     *
     * @param array of postings
     * @return
     */
    private boolean reprocessPosting(PostingArray array) {
        try {
            Thread.sleep(350);
            final BOPosting posting = null != array ? array.get(0) : null;
            if (null != posting) {
                Log.debug(this.getClass().getName(), "Reprocessing Posting: " + posting.getId());
                BOPosting newPosting = loadPosting(posting.getId());
                newPosting.addAttribute(SENT, posting.getAttributeValue(SENT));
                array.clear();
                array.add(newPosting);
            }
            DSConnection.getDefault().getRemoteBackOffice().savePostings(array);
            return true;
        } catch (CalypsoServiceException | InterruptedException e) {
            Log.error(this, "Error saving Posting: " + e.getCause().getMessage());
        }
        return false;
    }


    /**
     * Generate task on KO
     *
     * @param status
     * @param comment
     * @param boPosting
     */
    private void generateTaskEx(String status, String comment, BOPosting boPosting) {
        Task taskException = new Task();
        taskException.setStatus(Task.NEW);
        taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        taskException.setEventType("EX_POSTING_KO_EXCEPTION");
        taskException.setComment(comment);

        try {
            TaskArray task = new TaskArray();
            task.add(taskException);
            DSConnection.getDefault().getRemoteBackOffice().saveAndPublishTasks(task, 0L, null);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error saving task: " + e.getCause());
        }
    }


}



