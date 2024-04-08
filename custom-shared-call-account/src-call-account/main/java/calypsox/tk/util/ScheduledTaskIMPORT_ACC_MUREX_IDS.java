package calypsox.tk.util;

import calypsox.tk.product.CustomerTransferSDIHandler;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aalonsop
 */
public class ScheduledTaskIMPORT_ACC_MUREX_IDS extends ScheduledTask {

    private static final String DESCRIPTION = "This ST must be launched for Calypso's and Murex's CallAccount linking. \n" +
            "From a given CSV containing contractId, murexId and currency columns, this ST looks for the appropriate " +
            "contract account to set the murexId on it.";

    private static final String FILE_PATH_ATTR_NAME = "Full File Path";

    private static final String SET_ACCOUNT_BOOK = "Modify Call Account Book";

    private static final String SET_MCCONTRACT_BOOKS = "Modify mcContract books";

    private static final String SET_ACC_MUREX_ID = "Modify Account's Murex Id";

    private static final String FILE_SEPARATOR = ";";

    private static final String ACC_MUREX_ID_ATTR_NAME = "MurexID";

    private static final long serialVersionUID = 5915421319662219807L;

    private boolean setCallAccountBook = false;
    private boolean setMarginCallContractBooks = false;
    private boolean setAccountMurexId = false;

    /**
     * Default column index values
     */
    private int contractIdColumnPosition = 0;
    private int cashBookColumnPosition = 1;
    private int currencyColumnPosition = 2;
    private int murexIdColumnPosition = 3;

    @Override
    public String getTaskInformation() {
        return DESCRIPTION;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(FILE_PATH_ATTR_NAME).description("Complete File Path"));
        attributeList.add(attribute(SET_ACCOUNT_BOOK).description("Update account's book").booleanType());
        attributeList.add(attribute(SET_MCCONTRACT_BOOKS).description("Update contract's books").booleanType());
        attributeList.add(attribute(SET_ACC_MUREX_ID).description("Update account's murexId").booleanType());
        return attributeList;
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        String filePath = this.getAttribute(FILE_PATH_ATTR_NAME);
        setCallAccountBook = Boolean.valueOf(this.getAttribute(SET_ACCOUNT_BOOK));
        setMarginCallContractBooks = Boolean.valueOf(this.getAttribute(SET_MCCONTRACT_BOOKS));
        setAccountMurexId = Boolean.valueOf(this.getAttribute(SET_ACC_MUREX_ID));
        if (!Util.isEmpty(filePath)) {
            readAndMapFile(Paths.get(filePath)).forEach(this::processLine);
        } else {
            Log.error(this, "File path is empty!!!! Please fill " + FILE_PATH_ATTR_NAME + " scheduledTask's attribute");
        }
        return true;
    }

    /**
     * @param line
     */
    private void processLine(LineWrapper line) {
        Account account = null;
        try {
            account = (Account) new CustomerTransferSDIHandler().findAndFilterContractRelatedAccounts(line.contractId, line.accCurrency).orElseGet(Account::new).cloneIfImmutable();
            Book book = BOCache.getBook(DSConnection.getDefault(), line.mcContractCashBook);
            setMurexIdAndSaveAccount(account, book, line);
            setAndSaveMarginCallContractBooks(line, book);
        } catch (CloneNotSupportedException exc) {
            Log.error(this.getClass().getSimpleName(), exc.getMessage());
        }
    }

    /**
     * @param accountToSave
     * @param murexCallAccId
     */
    private void setMurexIdAndSaveAccount(Account accountToSave, Book book, LineWrapper line) {
        if (accountToSave.getLongId() > 0L) {
            setAccountMurexId(accountToSave, line);
            setAccountCallAccBook(accountToSave, book);
            try {
                DSConnection.getDefault().getRemoteAccounting().save(accountToSave);
                Log.info(this.getClass().getSimpleName(), "Account " + accountToSave.getName() + " [" + accountToSave.getId() + "] saved");
            } catch (CalypsoServiceException exc) {
                Log.error(this.getClass().getSimpleName(), "Error while saving account", exc.getCause());
            }
        }
    }

    /**
     * @param accountToSave
     * @param line
     */
    private void setAccountMurexId(Account accountToSave, LineWrapper line) {
        if (setAccountMurexId) {
            accountToSave.setAccountProperty(ACC_MUREX_ID_ATTR_NAME, String.valueOf(line.murexAccId));
        }
    }

    /**
     * @param accountToSave
     * @param line
     */
    private void setAccountCallAccBook(Account accountToSave, Book book) {
        if (setCallAccountBook && book != null) {
            accountToSave.setCallBookId(book.getId());
        }
    }

    /**
     * @param line
     * @param book
     */
    private void setAndSaveMarginCallContractBooks(LineWrapper line, Book book) {
        if (setMarginCallContractBooks && book != null) {
            try {
                CollateralConfig cc = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(line.contractId);
                if (cc != null) {
                    cc.setDefaultBook("BOOK_CASH_IN", book);
                    cc.setDefaultBook("BOOK_CASH_OUT", book);
                    ServiceRegistry.getDefault().getCollateralDataServer().save(cc);
                    Log.info(this.getClass().getSimpleName(), "MarginCall Contract " + cc.getName() + " saved");
                }
            } catch (CollateralServiceException exc) {
                Log.error(this, "Error while processing MarginCall contract: " + line.contractId);
            }
        }
    }

    /**
     * @return
     */
    private List<LineWrapper> readAndMapFile(Path path) {
        List<LineWrapper> mappedLines = Collections.emptyList();
        try (Stream<String> fileLines = Files.lines(path)) {
            mappedLines = fileLines.map(this::mapLine).filter(LineWrapper::isValid).collect(Collectors.toList());
        } catch (IOException exc) {
            Log.error(this, exc.getMessage(), exc);
        }
        return mappedLines;
    }

    /**
     * @param line
     * @return
     */
    private LineWrapper mapLine(String line) {
        LineWrapper newMap = new LineWrapper();
        if (!Util.isEmpty(line)) {
            String[] splittedLine = line.split(FILE_SEPARATOR);
            newMap.fillLineWrapper(splittedLine);
        }
        return newMap;
    }

    /**
     * Represents one file line
     */
    private class LineWrapper {

        private static final int COLUMN_NUMBER = 4;
        private int contractId;
        private int murexAccId;
        private String accCurrency;
        private String mcContractCashBook;

        private LineWrapper() {
            //EMPTY
        }

        /**
         * @param lineArray
         */
        private void fillLineWrapper(String[] lineArray) {
            this.contractId = parseInt(parseColumnLine(lineArray, contractIdColumnPosition));
            this.murexAccId = parseInt(parseColumnLine(lineArray, murexIdColumnPosition));
            this.mcContractCashBook = parseColumnLine(lineArray, cashBookColumnPosition);
            this.accCurrency = parseColumnLine(lineArray, currencyColumnPosition);
        }

        /**
         * @param numericColumnValue
         * @return
         */
        private int parseInt(String numericColumnValue) {
            int res = 0;
            try {
                res = Integer.parseInt(numericColumnValue);
            } catch (NumberFormatException exc) {
                Log.debug(LineWrapper.class.getSimpleName(), "Column with value " + numericColumnValue + " is not a number");
            }
            return res;
        }

        /**
         * @param lineArray
         * @param positionToParse
         * @return
         */
        private String parseColumnLine(String[] lineArray, int positionToParse) {
            String res = "";
            if (!Util.isEmpty(lineArray) && lineArray.length == COLUMN_NUMBER) {
                res = lineArray[positionToParse];
            }
            return res;
        }

        /**
         * @return True if is a valid registry
         */
        private boolean isValid() {
            return !(contractId == 0 || murexAccId == 0 || "".equals(accCurrency) || "".equals(mcContractCashBook));
        }
    }
}
