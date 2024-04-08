package calypsox.tk.util;

import java.util.ArrayList;
import java.util.List;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

// Project: Bloomberg tagging
// Project: Bloomberg tagging. Release 2

/**
 * ScheduledTaskSANT_BLOOMBERG_TAGGING
 * 
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 * @author Jos? Luis F. Luque <joseluis.f.luque@accenture.com>
 * @author Carlos Humberto Cejudo Bermejo <c.cejudo.bermejo@accenture.com>
 * 
 */
public class ScheduledTaskSANT_BLOOMBERG_TAGGING extends ScheduledTask {

    protected static final String TASK_INFORMATION = "Scheduled task to update Bloomberg bond/equity security codes.";
    protected static final String PROCESSMODEDESCRIPTION = "Scheduled Task SANT_BLOOMBERG_TAGGING process mode";

    protected static final String PROCESS_MODE_DOMAIN = "SANT_BLOOMBERG_TAGGING.processMode";
    protected static final String PRODUCT_TYPE_DOMAIN = "SANT_BLOOMBERG_TAGGING.productType";
    protected static final String PRODUCT_TYPE_DESCRIPTION = "Product type that will be processed by this Scheduled Task";

    protected SantBloombergUtil util = null;

    public static final String FILEPATH = "FilePath";
    public static final String FILENAME_FULL = "FileName Full";
    public static final String FILENAME_DIFF = "FileName Diff";
    public static final String SECURITY_CODES = "Security Codes To Process";
    public static final String PROCESS_MODE = "Process Mode";
    public static final String PRODUCT_TYPE = "Product Type";

    private static final String ALL_ISINS = "ALL";

    public enum ProcessMode {
        FULL("Full"), DIFF("Diff");

    	protected String value;

        ProcessMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public ProcessMode getProcessMode(String value) {
            for (ProcessMode processMode : ProcessMode.values()) {
                if (processMode.getValue().equals(value)) {
                    return processMode;
                }
            }
            return null;
        }

    }

    public ScheduledTaskSANT_BLOOMBERG_TAGGING() {

    }

    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.add(attribute(FILEPATH));
        attributeList.add(attribute(FILENAME_FULL));
        attributeList.add(attribute(FILENAME_DIFF));
        attributeList.add(attribute(SECURITY_CODES));
        attributeList
                .add(attribute(PROCESS_MODE).domainName(PROCESS_MODE_DOMAIN)
                        .mandatory().description(PROCESSMODEDESCRIPTION));
        attributeList
                .add(attribute(PRODUCT_TYPE).domainName(PRODUCT_TYPE_DOMAIN)
                        .description(PRODUCT_TYPE_DESCRIPTION));

        return attributeList;
    }

    @Override
    public boolean process(DSConnection dsCon, PSConnection psCon) {
        boolean processOK = false;

        String filePath = getAttribute(FILEPATH);
        String fullFileNamePrefix = getAttribute(FILENAME_FULL);
        String diffFileNamePrefix = getAttribute(FILENAME_DIFF);
        String processModeAttribute = getAttribute(PROCESS_MODE);
        String productType = getAttribute(PRODUCT_TYPE);
        String securityCodes = getAttribute(SECURITY_CODES);

        SantBloombergUtil util = new SantBloombergUtil();
        JDate date = getCurrentDate();

        if (Util.isEmpty(securityCodes)
                || ALL_ISINS.equalsIgnoreCase(securityCodes)) {
            // Daily process from files
            // Move both files to COPY
            // Look for files that match the prefix
            String fullFileName = util.getMatchingFileName(filePath,
                    fullFileNamePrefix);
            util.moveFileToCopyFolder(filePath, fullFileName,
                    fullFileNamePrefix, date);
            String diffFileName = util.getMatchingFileName(filePath,
                    diffFileNamePrefix);
            util.moveFileToCopyFolder(filePath, diffFileName,
                    diffFileNamePrefix, date);

            // Process file according to processing mode
            final ProcessMode processMode = getProcessMode(
                    processModeAttribute);
            final String fileName = getFileName(fullFileNamePrefix,
                    diffFileNamePrefix, processMode);
            processOK = util.processBloombergFileMap(filePath, fileName, date,
                    productType, processMode);

            // Copy processed file to OK or FAIL
            util.copyFileToResultFolder(filePath, fileName, getCurrentDate(),
                    processOK);

            if (processOK) {
                // Add new SecCodes so they are available in product
                // windows and reports
                Log.info(this, "Adding missing SecCodes");
                util.addMissingSecCodesDomainValues();

                // Update all haircuts
                Log.info(this, "Updating haircut values");
                SantBloombergHaircutUtil.getInstance().updateAllHaircutQuotes(
                        productType, this.getCurrentDate());
            }
        } else {
            // Process only one or several products
            util.processOneOrSeveralProducts(filePath, fullFileNamePrefix, date,
                    securityCodes);
        }

        // Return
        return processOK;
    }

    protected String getFileName(String fullFileName, String diffFileName,
            ProcessMode processMode) {
        String fileName = null;

        if (processMode == ProcessMode.FULL) {
            fileName = fullFileName;
        } else if (processMode == ProcessMode.DIFF) {
            fileName = diffFileName;
        }

        return fileName;
    }

    protected ProcessMode getProcessMode(String value) {
        ProcessMode processMode = null;

        ProcessMode[] values = ProcessMode.values();
        for (int iMode = 0; iMode < values.length; iMode++) {
            if (values[iMode].getValue().equalsIgnoreCase(value)) {
                processMode = values[iMode];
            }
        }

        return processMode;
    }

}
