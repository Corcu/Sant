package calypsox.tk.util;

import calypsox.tk.event.PSEventBloombergUpdate;
import calypsox.tk.util.ScheduledTaskSANT_BLOOMBERG_TAGGING.ProcessMode;
import calypsox.util.FileUtility;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.ProductCode;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteProduct;
import com.calypso.tk.util.TaskArray;

import java.io.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

// Project: Bloomberg tagging
// Project: Bloomberg tagging. Release 2

/**
 * SantBloombergUtil
 *
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 * @author Jos? Luis F. Luque <joseluis.f.luque@accenture.com>
 * @author Carlos Humberto Cejudo Bermejo <c.cejudo.bermejo@accenture.com>
 */
public class SantBloombergUtil {

    public static final String ALL = "ALL";
    public static final String ISIN = "ISIN";
    public static final String PRODUCTPREFIX = "BBG_";
    private static final int PRODUCT_CODE_NAME_MAX_LENGTH = 32;

    private static final String DOMAIN_NAME_SEC_CODE = "securityCode";

    private static final String STARTOFFILE = "START-OF-FILE";
    private static final String ENDOFFILE = "END-OF-FILE";
    private static final String STARTOFFIELDS = "START-OF-FIELDS";
    private static final String ENDOFFIELDS = "END-OF-FIELDS";
    private static final String STARTOFDATA = "START-OF-DATA";
    private static final String ENDOFDATA = "END-OF-DATA";
    private static final String IDISIN = "ID_ISIN";

    private static final String SECURITY_FLAG_FIELD_NAME = "ID_BB_PRIM_SECURITY_FLAG";
    private static final String SECURITY_FLAG_Y = "Y";

    private static final String COPYPATH = "copy/";
    private static final String FAIL_PATH = "fail/";
    private static final String OK_PATH = "ok/";

    private static final String ISIN_CODES_SEPARATOR = ",";

    private boolean processOk = false;

    private List<String> fields = new Vector<>();
    private List<String> valueLines = new Vector<>();

    private String[] statusList = {"Initial", STARTOFFILE, STARTOFFIELDS,
            ENDOFFIELDS, STARTOFDATA, ENDOFDATA, ENDOFFILE};

    private ArrayList<Task> taskList;

    public SantBloombergUtil() {
        taskList = new ArrayList<>();
    }

    /**
     * Method that orchestrates the file load, product map recovery and
     * comparison methods.
     *
     * @param filePath     - Path where the file is stored
     * @param fileName     - Name of the file to load
     * @param securityCode - Product's security code to process
     * @return true if file is loaded and products are compared and updated,
     * false otherwise
     * @throws IOException
     */
    public boolean processBloombergFileMap(String filePath, String fileName,
                                           JDate date, String productType, ProcessMode processMode) {
        boolean result = false;

        File inputFile = getFile(filePath, fileName, date);
        Map<String, BloombergInfoBean> bloombergFileMap = processBloombergFile(
                inputFile);

        Map<String, Product> productMap = null;
        if (processMode == ProcessMode.FULL) {
            productMap = loadProductMap(productType);
        } else if (processMode == ProcessMode.DIFF) {
            Collection<String> isinCodes = bloombergFileMap.keySet();
            productMap = loadProductMap(isinCodes);
        }

        if (productMap != null && productMap.size() > 0) {
            result = checkProductMap(bloombergFileMap, productMap);
        } else {
            Log.info(this,
                    "Product Map is empty. Check if products in the input file are defined with the correct ISIN codes.");
            result = true;
        }

        if (this.taskList != null && this.taskList.size() > 0) {
            publishTask(taskList);
        }

        return result;
    }

    public boolean processOneOrSeveralProducts(String filePath, String fileName,
                                               JDate date, String isinCodesString) {
        boolean result = false;

        File inputFile = getFile(filePath, fileName, date);
        Map<String, BloombergInfoBean> bloombergFileMap = processBloombergFile(
                inputFile);

        Collection<String> isinCodes = getIsinCodes(isinCodesString);

        Map<String, Product> productMap = loadProductMap(isinCodes);

        if (productMap != null && productMap.size() > 0) {
            result = checkProductMap(bloombergFileMap, productMap);
        }

        if (this.taskList != null && this.taskList.size() > 0) {
            publishTask(taskList);
        }

        return result;
    }

    /**
     * Method that loads the file and call the validation method.
     *
     * @param filePath - Path where the file is stored
     * @param fileName - Name of the file to process
     * @return Map with beans generated from file info
     */
    protected Map<String, BloombergInfoBean> processBloombergFile(
            final File inputFile) {

        Map<String, BloombergInfoBean> bloombergFileMap = new HashMap<String, BloombergInfoBean>();

        FileReader fileReader = null;
        BufferedReader reader = null;
        boolean messageLogged = false;

        try {
            int statusPos = 0;
            fileReader = createFileReader(inputFile);
            reader = createBufferedReader(fileReader);
            fields = new Vector<String>();
            valueLines = new Vector<String>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.equals(STARTOFFILE)
                        && statusList[statusPos].equals("Initial")) {
                    statusPos++;
                } else if (line.equals(STARTOFFIELDS)
                        && statusList[statusPos].equals(STARTOFFILE)) {
                    statusPos++;
                } else if (line.equals(ENDOFFIELDS)
                        && statusList[statusPos].equals(STARTOFFIELDS)) {
                    statusPos++;
                } else if (line.equals(STARTOFDATA)
                        && statusList[statusPos].equals(ENDOFFIELDS)) {
                    statusPos++;
                } else if (line.equals(ENDOFDATA)
                        && statusList[statusPos].equals(STARTOFDATA)) {
                    statusPos++;
                } else if (statusList[statusPos].equals(STARTOFFIELDS)) {
                    if (fields.contains(line)) {
                        Log.error(SantBloombergUtil.class,
                                "Duplicated field name: " + line);
                        processOk = false;
                        messageLogged = true;
                        break;
                    } else {
                        fields.add(line);
                    }
                } else if (statusList[statusPos].equals(STARTOFDATA)) {
                    valueLines.add(line);
                } else if (line.equals(ENDOFFILE)
                        && statusList[statusPos].equals(ENDOFDATA)) {
                    statusPos++;
                    processOk = true;
                } else if (statusList[statusPos].equals(STARTOFFILE)) {
                    continue;
                } else if (statusList[statusPos].equals(ENDOFFIELDS)) {
                    continue;
                } else if (statusList[statusPos].equals(ENDOFDATA)) {
                    continue;
                } else if (Util.isEmpty(line)) {
                    continue;
                } else {
                    Log.error(SantBloombergUtil.class,
                            "Incorrect data in file");
                    processOk = false;
                    messageLogged = true;
                    break;
                }
            }
            if (!processOk) {
                if (!messageLogged) {
                    Log.error(SantBloombergUtil.class,
                            "Incorrect file structure");
                }
            } else if (validateValues()) {
                for (String valueLine : valueLines) {
                    BloombergInfoBean bean = processLine(valueLine);
                    if (isValidSecurityFlag(bean)) {
                        bloombergFileMap.put(bean.get(IDISIN), bean);
                    }
                }
            }
        } catch (Exception e) {
            Log.error(this, "Error while processing Bloomberg input file", e);
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    Log.error(this, "Cannot close input file reader", e);
                }
            }
        }

        return bloombergFileMap;
    }

    /**
     * Method that loads the products to check/update
     *
     * @param securityCode - Code to look for the products
     * @return Map with the products to check/update
     * @throws RemoteException
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Product> loadProductMap(String productType) {
        Map<String, Product> systemProductMap = new HashMap<>();

        String where = getProductWhereClause(productType);

        try {
            Vector<Product> allProducts = DSConnection.getDefault()
                    .getRemoteProduct().getAllProducts(null, where, null);
            for (Product product : allProducts) {
                String isin = product.getSecCode(ISIN);
                if (Util.isEmpty(isin)) {
                    Log.error(SantBloombergUtil.class,
                            "Product doesn't have ISIN. Product Id ="
                                    + product.getId());
                } else {
                    // Check if the Bond already exists in the map. If so it is
                    // duplicate REF_INTERNA
                    if (systemProductMap.get(isin) != null) {
                        Log.error(SantBloombergUtil.class,
                                "Duplicate ISIN for Product ids ="
                                        + systemProductMap.get(isin).getId()
                                        + ", " + product.getId());
                    } else {
                        systemProductMap.put(isin, product);
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve products from database", e);
        }

        return systemProductMap;
    }

    protected Map<String, Product> loadProductMap(
            Collection<String> isinCodes) {
        Map<String, Product> productMap = new HashMap<String, Product>();

        try {
            for (String isinCode : isinCodes) {
                Product product = DSConnection.getDefault().getRemoteProduct()
                        .getProductByCode(ISIN, isinCode);
                // Check if the product is not null before putting the ISIN code
                // in the product map.
                if (product != null) {
                    productMap.put(isinCode, product);
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve products from database", e);
        }

        return productMap;
    }

    protected String getProductWhereClause(String productType) {
        StringBuilder where = new StringBuilder();

        where.append("product_desc.product_family = '");
        where.append(productType);
        where.append('\'');

        return where.toString();
    }

    /**
     * Method that checks the existence of bloomberg references inside the
     * product map, and runs the compare method on those found.
     *
     * @param bloombergFileMap - Bloomberg references
     * @param systemProductMap - Product references
     * @return true if a product is updated, false otherwise
     */
    protected boolean checkProductMap(
            Map<String, BloombergInfoBean> bloombergFileMap,
            Map<String, Product> systemProductMap) {
        int productId = 0;
        boolean result = false;
        Iterator<String> it = systemProductMap.keySet().iterator();
        while (it.hasNext()) {
            String isin = it.next();
            if (!bloombergFileMap.containsKey(isin)) {
                String message = String.format(
                        "Security ISIN=%s does not exist in Bloomberg Bond/Equity bulk file",
                        isin);
                Log.error(this, message);
                addTask(0L, Task.EXCEPTION_EVENT_CLASS, new JDatetime(), 0L,
                        "EX_SANT_BLOOMBERG_TAGGING", "checkProductMap",
                        message);
            } else {
                productId = compareAndSave(bloombergFileMap.get(isin),
                        systemProductMap.get(isin));
                if (productId >= 0) {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Method that creates a bean from a text line
     *
     * @param line - Text line
     * @return Bean generated using the information read from the line text
     */
    protected BloombergInfoBean processLine(String line) {
        BloombergInfoBean bean = new BloombergInfoBean();
        String[] lineTokens = line.split("\\|");
        for (int i = 3; i < lineTokens.length; i++) {
            bean.set(fields.get(i - 3), lineTokens[i]);
        }

        return bean;
    }

    private String addBloombergPrefix(String fieldName) {
        String result = PRODUCTPREFIX + fieldName;
        if (result.length() > PRODUCT_CODE_NAME_MAX_LENGTH) {
            result = fieldName;
        }

        return result;
    }

    /**
     * Method that checks and updates on a product changes received from
     * Bloomberg
     *
     * @param bean    - Information of the product received from Bloomberg
     * @param product - Information of the product stored in the system
     * @return Id of updated product, 0 if product has no changes, -1 if an
     * error happens
     */
    protected int compareAndSave(BloombergInfoBean bean, Product product) {
        boolean valueChanged = false;
        int productId = 0;
        // Check if the product is not null
        if (product != null) {
            for (String field : fields) {
                String bloombergValue = bean.get(field);
                String productValue = product
                        .getSecCode(addBloombergPrefix(field));
                if (!bloombergValue.equals(productValue)) {
                    product.setSecCode(addBloombergPrefix(field),
                            bloombergValue);
                    valueChanged = true;
                }
            }
            if (valueChanged) {
                try {
                    productId = DSConnection.getDefault().getRemoteProduct()
                            .saveProduct(product);
                    Log.info(this, String.format("Product %s updated",
                            product.getSecCode("ISIN")));
                } catch (CalypsoServiceException e) {
                    productId = retrySavingProduct(product);
                }
            }
        }
        return productId;
    }

    private int retrySavingProduct(Product product) {
        int productId = product.getId();

        try {
            Product productFromDB = DSConnection.getDefault().getRemoteProduct()
                    .getProduct(productId);
            Product newProduct = (Product) productFromDB.clone();

            for (String field : fields) {
                String secCodeName = addBloombergPrefix(field);
                String secCodeValue = product.getSecCode(secCodeName);
                newProduct.setSecCode(secCodeName, secCodeValue);
            }

            productId = DSConnection.getDefault().getRemoteProduct()
                    .saveProduct(newProduct);
        } catch (CalypsoServiceException e) {
            Log.error(this,
                    String.format("Error updating product. Product id: %d",
                            product.getId()),
                    e);
            productId = -1;
        } catch (CloneNotSupportedException e) {
            Log.error(this,
                    String.format("Cannot clone product %d", product.getId()),
                    e);
            productId = -1;
        }

        return productId;
    }

    /**
     * Method that validates that the number of specified field names matches
     * the number of values in the data lines
     *
     * @return true if number of fields matches number of values, false
     * otherwise
     */
    protected boolean validateValues() {
        for (String line : valueLines) {
            if ((line.split("\\|").length - 3) != fields.size()) {
                Log.error(SantBloombergUtil.class.getName(),
                        "Incorrect number of field values in line: " + line);
                return false;
            }
        }
        return true;
    }

    /**
     * Create an instance of BufferedReader from an input FileReader instance.
     *
     * @param fileReader
     * @return an instance of BufferedReader
     */
    protected BufferedReader createBufferedReader(final FileReader fileReader) {
        return new BufferedReader(fileReader);

    }

    /**
     * Create an instance of FileReader from an input File instance.
     *
     * @param inputFile
     * @return FileReader
     * @throws FileNotFoundException
     */
    protected FileReader createFileReader(final File inputFile)
            throws FileNotFoundException {
        return new FileReader(inputFile);
    }

    /**
     * Add a task to publish in Task Station.
     *
     * @param objectId
     * @param eventClass
     * @param dateTime
     * @param id
     * @param eventType
     * @param source
     * @param comment
     */
    protected void addTask(long objectId, String eventClass, JDatetime dateTime,
                           long id, String eventType, String source, String comment) {
        Task task = buildTask(objectId, eventClass, dateTime, id, eventType,
                source, comment);
        taskList.add(task);
    }

    /**
     * Publish a task list in Task Station
     *
     * @param tasks - Task list
     */
    protected void publishTask(List<Task> tasks) {
        try {
            DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(
                    new TaskArray(tasks), 0, "SANT_BLOOMBERG_TAGGING");
        } catch (RemoteException e) {
            Log.error(SantBloombergUtil.class, "Failed to publish tasks");
            Log.error(this, e);// Sonar
        }
    }

    /**
     * Build a task
     *
     * @param objectId
     * @param eventClass
     * @param dateTime
     * @param id
     * @param eventType
     * @param source
     * @param comment
     * @return A newly created task
     */
    protected Task buildTask(long objectId, String eventClass,
                             JDatetime dateTime, long id, String eventType, String source,
                             String comment) {
        Task task = new Task();
        task.setObjectLongId(objectId);
        task.setEventClass(eventClass);
        task.setDatetime(dateTime);
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(id);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setSource(source);
        task.setComment(comment);
        return task;
    }

    public void publishBloombergUpdateEvent(String isin, int type) {
        try {
            PSEventBloombergUpdate event = new PSEventBloombergUpdate(isin,
                    type);
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
        } catch (CalypsoServiceException e) {
            Log.error(this,
                    String.format(
                            "Cannot publish PSEventUpdateBloombergUpdate event with ISIN = \"%s\" and type = %d",
                            isin, type),
                    e);
        }
    }

    // METHODS TO MANAGE FILES
    public boolean moveFileToCopyFolder(String filePath, String fileName,
                                        String fileNamePrefix, JDate date) {
        boolean fileMovedOK = false;

        if (!Util.isEmpty(fileName)) {
            String source = filePath + '/' + fileName;
            String destination = filePath + '/' + COPYPATH
                    + getFileNameWithDate(fileNamePrefix, date);
            try {
                FileUtility.moveFile(source, destination);
                fileMovedOK = true;
            } catch (IOException e) {
                fileMovedOK = false;
                String message = String.format(
                        "Could not move file from %s to %s", source,
                        destination);
                Log.error(this, message, e);
            }
        }

        return fileMovedOK;
    }

    /**
     * Returns the name of the first file in the given directory which name
     * starts by the given file name.
     *
     * @param filePath Directory where the file is located
     * @param fileName Beginning of the file name
     * @return The name of a file that begins with the given prefix
     */
    public String getMatchingFileName(String filePath, String fileName) {
        String matchingFileName = null;

        File directory = new File(filePath);
        File[] fileNames = directory.listFiles();

        for (int iFile = 0; matchingFileName == null
                && iFile < fileNames.length; iFile++) {
            File file = fileNames[iFile];
            if (file.isFile() && file.getName().startsWith(fileName)) {
                matchingFileName = file.getName();
            }
        }

        return matchingFileName;
    }

    protected boolean copyFileToResultFolder(String filePath, String fileName,
                                             JDate date, boolean processOK) {
        boolean copyOK = false;

        String source = filePath + '/' + COPYPATH
                + getFileNameWithDate(fileName, date);
        String destination = filePath + '/' + getResultPath(processOK)
                + getFileNameWithDate(fileName, date);

        try {
            FileUtility.copyFile(source, destination);
            copyOK = true;
        } catch (IOException e) {
            String message = String.format("Could not copy file from %s to %s",
                    source, destination);
            Log.error(this, message, e);
        }

        return copyOK;
    }

    protected File getFile(String filePath, String fileName, JDate date) {
        // Look for the file in COPY folder (with date)
        File file = new File(filePath + '/' + COPYPATH
                + getFileNameWithDate(fileName, date));

        return file;
    }

    protected String getFileNameWithDate(String fileName, JDate date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        int dotIndex = fileName.indexOf('.');

        StringBuilder fileNameWithDate = new StringBuilder();

        // File Name
        fileNameWithDate.append(fileName.subSequence(0, dotIndex));

        // Date suffix
        fileNameWithDate.append('_');
        fileNameWithDate.append(sdf.format(date.getDate()));

        // Extension
        fileNameWithDate.append(fileName.substring(dotIndex));

        return fileNameWithDate.toString();
    }

    protected String getResultPath(boolean processOK) {
        if (processOK) {
            return OK_PATH;
        }
        return FAIL_PATH;
    }

    // METHODS TO MANAGE FILES - END

    // METHODS TO ADD MISSING SEC CODES

    public void addMissingSecCodesDomainValues() {
        try {
            if (this.fields != null && !this.fields.isEmpty()) {
                Vector<String> currentSecCodes = DSConnection.getDefault()
                        .getRemoteReferenceData()
                        .getDomainValues(DOMAIN_NAME_SEC_CODE);
                for (String field : this.fields) {
                    String bbgField = addBloombergPrefix(field);

                    // If this field is not present in the "securityCode"
                    // domain, add it.
                    if (!currentSecCodes.contains(bbgField)) {
                        addSecCodeDomainValue(bbgField);
                    }

                    // If this field is not declared as a Product code, save a
                    // new Product Code.
                    ProductCode productCode = DSConnection.getDefault()
                            .getRemoteProduct().getProductCode(bbgField);
                    if (productCode == null) {
                        addSecCodeProductCode(bbgField);
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,
                    "Could not get values from domain " + DOMAIN_NAME_SEC_CODE,
                    e);
        }
    }

    protected void addSecCodeDomainValue(String secCode) {
        try {
            Log.info(this,
                    String.format("Adding missing SecCode \"%s\".", secCode));
            int domainValuesVersion = DSConnection.getDefault()
                    .getRemoteReferenceData().getObjectVersion(
                            DomainValues.class.getSimpleName(), 0, null);
            DSConnection.getDefault().getRemoteReferenceData().addDomainValue(
                    DOMAIN_NAME_SEC_CODE, secCode, "", domainValuesVersion,
                    DSConnection.getDefault().getUser());
            Log.info(this, String.format("SecCode \"%s\" added succesfully.",
                    secCode));
        } catch (CalypsoServiceException e) {
            String message = String.format(
                    "Could not save new SecCode \"%s\" domain value", secCode);
            Log.error(this, message, e);
        }
    }

    protected void addSecCodeProductCode(String secCode) {
        try {
            RemoteProduct remoteProduct = DSConnection.getDefault()
                    .getRemoteProduct();

            ProductCode newProductCode = new ProductCode();
            newProductCode.setCode(secCode);
            newProductCode.setMandatoryB(false);
            newProductCode.setSearchableB(true);
            newProductCode.setType(ProductCode.STRING);
            newProductCode.setUniqueB(false);

            remoteProduct.save(newProductCode);
        } catch (CalypsoServiceException e) {
            String message = String
                    .format("Could not save new Product Code \"%s\"", secCode);
            Log.error(this, message, e);
        }
    }

    // METHODS TO ADD MISSING SEC CODES - END

    public static Collection<String> getIsinCodes(String isinCodesString) {
        Collection<String> isinCodes = new LinkedList<String>();

        if (!Util.isEmpty(isinCodesString)) {
            String[] isinCodesArray = isinCodesString
                    .split(ISIN_CODES_SEPARATOR);
            List<String> isinCodesList = Arrays.asList(isinCodesArray);
            for (String isinCode : isinCodesList) {
                String trimmedIsinCode = isinCode.trim();
                isinCodes.add(trimmedIsinCode);
            }
        }

        return isinCodes;
    }

    private boolean isValidSecurityFlag(BloombergInfoBean bean) {
        boolean validSecurityFlag = true;

        // Field ID_BB_PRIM_SECURITY_FLAG will always be NULL in the Fixed
        // Income file. In the Equities file this field should be "Y", otherwise
        // the line won't be read.
        String securityFlag = bean.get(SECURITY_FLAG_FIELD_NAME);
        if (securityFlag != null && !SECURITY_FLAG_Y.equals(securityFlag)) {
            validSecurityFlag = false;
        }

        return validSecurityFlag;
    }

}
