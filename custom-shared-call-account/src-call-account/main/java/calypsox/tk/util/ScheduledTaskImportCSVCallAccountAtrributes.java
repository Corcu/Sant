package calypsox.tk.util;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventScheduledTask;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by x379335 on 20/03/2020.
 */
public class ScheduledTaskImportCSVCallAccountAtrributes extends ScheduledTask {

    public static final String CSV_DELIMITER_ATTR = "Csv Delimiter";
    public static final String FILEPATH = "File Path";
    public static final String FILENAME = "File Name";

    public static final String LOG_CATEGORY_SCHEDULED_TASK="ScheduledTask";
    private static final Integer CNST_1=1;

    protected boolean process(DSConnection ds, PSConnection ps) {
        if (Log.isCategoryLogged("ScheduledTask")) {
            Log.debug("ScheduledTask", "Calling Execute ON " + this + " PublishB: " + this._publishB);
        }
        boolean ret = true;
        if (this._publishB) {
            try {
                PSEventScheduledTask ev = new PSEventScheduledTask();
                ev.setScheduledTask(this);
                ps.publish(ev);
                ret = true;

            } catch (Exception var5) {
                Log.error("ScheduledTask", var5);
                ret = false;
            }
        }

        if (this._executeB) {
            try {
                importCallAccountAttributes();
            } catch (CalypsoServiceException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    protected boolean importCallAccountAttributes() throws CalypsoServiceException {

        return processFile(this.getAttribute(FILEPATH) + "/" + this.getAttribute(FILENAME));

    }

    @Override
    public Vector<String> getDomainAttributes() {
        final Vector<String> attr = new Vector<String>();
        attr.add(CSV_DELIMITER_ATTR);
        attr.add(FILEPATH);
        attr.add(FILENAME);
        return attr;
    }

    protected boolean processFile(String file) throws CalypsoServiceException {

        HashMap<Integer, AccountInterests> incomingAttributes = new HashMap<Integer, AccountInterests>();

        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int lineNumber = -1;
            while ((line = reader.readLine()) != null) {

                lineNumber++;
                Log.debug(LOG_CATEGORY_SCHEDULED_TASK, "Processing line " + lineNumber);

                String[] fields = line.split(getDelimiter());

                if (fields.length < 3) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Incorrect line format : " + line);
                } else {

                    String attributeId = fields[0];
                    String attributeIdConfig = fields[1];
                    String date = fields[2];

                    AccountInterests caInterest = new AccountInterests();
                    caInterest.setConfigId(Integer.parseInt(attributeIdConfig));
                    caInterest.setActiveFrom(JDate.valueOf(date));

                    Integer id = Integer.parseInt(attributeId);

                    AccountInterests caIntAttribute = incomingAttributes.get(id);

                    if (caIntAttribute == null) {
                        incomingAttributes.put(id, caInterest);
                    }
                }

            }

        } catch (Exception exc) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.error(this, e); // sonar
                }
            }
        }

        for (Map.Entry<Integer, AccountInterests> entry : incomingAttributes.entrySet()) {
            int accountId = entry.getKey();
            Account acc = null;
            try {
                acc = DSConnection.getDefault().getRemoteAccounting().getAccount(accountId);
            } catch (CalypsoServiceException e) {
                e.printStackTrace();
            }

            Vector<AccountInterests> accountInterests = new Vector<>();
            if (acc != null) {

                if (!callAccountAttributeUnchanged(acc, entry.getValue().getConfigId())) {
                    accountInterests = acc.getAccountInterests();
                    AccountInterests accountInterestOld = acc.getAccountInterests().get(acc.getAccountInterests().size()-CNST_1);

                    AccountInterests accountInterestNew = setAccountInterestNew(accountInterestOld, entry.getValue());
                    accountInterestOld.setActiveTo(entry.getValue().getActiveFrom().substractTenor(1));

                    accountInterests.add(accountInterestNew);
                    acc.setAccountInterests(accountInterests);
                }
                try {
                    DSConnection.getDefault().getRemoteAccounting().save(acc);
                } catch (CalypsoServiceException e) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
                }

            } else {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Call Account " + accountId + " not found.");
            }

        }

        return true;
    }

    protected boolean callAccountAttributeUnchanged(Account acc, Integer configId) throws CalypsoServiceException {
        if (acc == null){
            return false;
        }

        AccountInterests accountInterests = acc.getAccountInterests().get(acc.getAccountInterests().size()-1);
        int configIdAttribute = accountInterests.getConfigId();

        if (configIdAttribute == configId){
            return true;
        }
        return false;
    }

    protected String getDelimiter() {
        String delimiter = this.getAttribute(CSV_DELIMITER_ATTR);
        if (delimiter == null){
            return ";";
        }
        return delimiter;
    }

    protected AccountInterests setAccountInterestNew(AccountInterests caInterestOld, AccountInterests caInterestIn) {
        AccountInterests out = (AccountInterests) caInterestOld.clone();
        out.setId(0);
        out.setActiveFrom(caInterestIn.getActiveFrom());
        out.setConfigId(caInterestIn.getConfigId());
        return out;
    }

    @Override
    public String getTaskInformation() {
        return "Import Call Account Attributes from CSV";
    }

}