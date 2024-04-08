package calypsox.tk.util;

import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ScheduledTaskSANT_IMPORT_IM_EVENT_CONTRACT extends ScheduledTask {

    public static final String FILE_PATH = "FILE PATH";
    public static final String FILE_NAME = "FILE NAME";
    public static final String FILE_EXTENSION = "FILE EXTENSION";
    private static final String DATE_FORMAT = "ddMMyyyy";
    private static final String IM_EVENT_NEW_CONTRACT = "IMEventNewContract";
    private static final long serialVersionUID = -53749370477209302L;

    @Override
    public String getTaskInformation() {
        return "Update IM events for new Contracts";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(attribute(FILE_PATH));
        attributes.add(attribute(FILE_NAME));
        attributes.add(attribute(FILE_EXTENSION));

        return attributes;
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        List<Trade> trades = new ArrayList<>();
        String path = getPath();
        if (!Util.isEmpty(path)) {
            List<String> frontids = getFrontIds(path);
            trades = getTrades(frontids);
            if (!Util.isEmpty(trades)) {
                addTradesKeywords(trades);
            }
        }
        return saveTrades(trades);
    }

    private List<String> getFrontIds(String path) {
        List<String> frontids = new ArrayList<>();
        try {
            FileReader file = new FileReader(path);
            if (file != null) {
                BufferedReader inputFileStream = new BufferedReader(file);
                String line;
                while ((line = inputFileStream.readLine()) != null) {
                    frontids.add("'" + line + "'");
                }
                inputFileStream.close();
            }
            file.close();
        } catch (IOException e) {
            Log.error(this, "Cannot read file " + e);
        }
        return frontids;
    }

    @SuppressWarnings("unchecked")
    private List<Trade> getTrades(List<String> frontids) {
        List<Trade> trades = new ArrayList<>();
        StringBuilder where = new StringBuilder("");
        StringBuilder from = new StringBuilder("");
        from.append("trade");
        where.append("trade.EXTERNAL_REFERENCE IN (");
        int idx = 0;
        if (!Util.isEmpty(frontids)) {
            while (idx <= frontids.size()) {

                StringBuilder tradeFrontIds = new StringBuilder("");
                tradeFrontIds.append(where.toString());
                tradeFrontIds.append(Util.collectionToString(
                        frontids.subList(idx, (idx + 999) > frontids.size() ? frontids.size() : idx + 999)));
                tradeFrontIds.append(")");

                try {
                    trades.addAll(DSConnection.getDefault().getRemoteTrade().getTrades(from.toString(),
                            tradeFrontIds.toString(), null, null));
                } catch (RemoteException e) {
                    Log.error("Cannot get trades from DB", e);
                }
                idx += 999;
            }
        }

        return trades;
    }

    private void addTradesKeywords(List<Trade> trades) {
        removeCanceledTrade(trades);
        for (Trade trade : trades) {
            trade.addKeyword(IM_EVENT_NEW_CONTRACT, String.valueOf(true));
        }
    }

    private String getPath() {
        String path = "";
        final SimpleDateFormat sdf1 = new SimpleDateFormat(DATE_FORMAT);
        final String date = sdf1.format(getValuationDatetime());
        if (!Util.isEmpty(getAttribute(FILE_PATH)) && !Util.isEmpty(getAttribute(FILE_NAME))
                && !Util.isEmpty(getAttribute(FILE_EXTENSION))) {
            path = getAttribute(FILE_PATH) + getAttribute(FILE_NAME) + "_" + date + "." + getAttribute(FILE_EXTENSION);
        }

        return path;
    }

    private boolean saveTrades(List<Trade> trades) {
        if (!Util.isEmpty(trades)) {
            try {
                DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(trades));
            } catch (CalypsoServiceException | InvalidClassException e) {
                Log.error(this, "Cannot save trades error: " + e);
                return false;
            }
        }
        return true;
    }

    private void removeCanceledTrade(List<Trade> trades) {
        List<Trade> tradetoRemove = new ArrayList<>();
        for (Trade trade : trades) {
            if (trade.getStatus().toString().equals("CANCELED")) {
                tradetoRemove.add(trade);
            }
        }
        trades.removeAll(tradetoRemove);

    }

}
