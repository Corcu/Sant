package calypsox.tk.util;


import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DMOPositionUtil {

    public static final String LOG_CATEGORY_SCHEDULED_TASK = "ScheduledTask";
    //   static String direction = "";

    public static Vector<DMOPosition> addUN01Row(TradeFilter tf, StaticDataFilter sdf, DSConnection dsConnection, JDatetime valDateTime) throws Exception {
        TradeArray tradesFromTF = getTradesWithTradeFilter(tf, valDateTime);
        TradeArray filteredTrades = new TradeArray();
        if (null != sdf) {
            filteredTrades = filterTrades(sdf, tradesFromTF);
        } else {
            filteredTrades = tradesFromTF;
        }

        Vector<DMOPosition> UN01Rows = new Vector<>();

        for (int i = 0; i < filteredTrades.size(); i++) {

            try {
                double finalAmount = getUnsettledNominal(filteredTrades.get(i), valDateTime.getJDate());
                if (finalAmount != 0.0) {
                    //  finalAmount = finalAmount / 1000;
                    DMOPosition un01Row = new DMOPosition();
                    un01Row.setPosition_Type("UN01");
                    un01Row.setCpty_full_name(filteredTrades.get(i).getCounterParty().getName());
                    /* This is incorrect  need to consider reversals  */
                    ///  un01Row.setDirection(direction);


                    un01Row.setDirection(finalAmount < 0 ? "S" : "P");

                    Bond prod = (Bond) filteredTrades.get(i).getProduct();
                    String isin = prod.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN);
                    un01Row.setProduct_Code(isin);
                    JDate settleDate = filteredTrades.get(i).getSettleDate();
                    String settleDateStr = "";
                    if (settleDate != null) {
                        SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy", Locale.UK);
                        settleDateStr = format.format(settleDate.getDate());
                    }
                    un01Row.setTrade_settle_date(settleDateStr);
                    /*
                    String fiAmount = String.valueOf(finalAmount);
                    if (fiAmount.contains(".")) {
                        fiAmount = fiAmount.substring(0, fiAmount.indexOf("."));
                    }
                    un01Row.setNominal_in_1000(fiAmount); */


                    if (Double.valueOf(RoundingMethod.R_NEAREST.round(Math.abs(finalAmount) / 1000D, 0)).longValue() == 0) {
                        continue;
                    }
                    un01Row.setNominal_in_1000(Long.toString(Double.valueOf(RoundingMethod.R_NEAREST.round(Math.abs(finalAmount) / 1000D, 0)).longValue()));
                    UN01Rows.add(un01Row);

                }

            } catch (Exception e) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while handling Trade ID : " + filteredTrades.get(i) + "With Error " + e.getMessage());
                throw e;
            }


        }
        return UN01Rows;

    }

    public static TradeArray getTradesWithTradeFilter(TradeFilter tf, JDatetime valDatetime) throws CalypsoServiceException {

        return  DSConnection.getDefault().getRemoteTrade().getTrades(tf, valDatetime);

    }

    public static TradeArray filterTrades(StaticDataFilter sdf, TradeArray trades) throws CalypsoServiceException {
        TradeArray result = new TradeArray();
        for (int i = 0; i < trades.size(); i++) {
            if (sdf.accept(trades.get(i))) {
                result.add(trades.get(i));
            }
        }
        return result;
    }

    public static double getUnsettledNominal(Trade trade, JDate valDate) {
        double finalAmount = 0.0;
        if (trade != null) {
            try {


                /*
                * this won't work for trade netting as both trade and underling transfer will have the same trade id

                final TransferArray transfers = DSConnection.getDefault().getRemoteBO()
                        .getTransfers(null, "trade_id = " + trade.getLongId(), null);
                 **/
                final TransferArray transfers = DSConnection.getDefault().getRemoteBO()
                        .getBOTransfers(trade.getLongId(), false);

                finalAmount = transfers.stream().filter(DMOPositionUtil::filterTransfer).mapToDouble(t -> "PAY".equals(t.getPayReceive()) ? -Math.abs(t.getNominalAmount()) : Math.abs(t.getNominalAmount())).sum();
                /*

                for (int i = 0; i < transfers.size(); i++) {
                    double amount = 0.0;
                    if (filterTransfer(transfers.get(i))) {
                        continue;
                    }

                    String payRec = getPayReceive(transfers.get(i).getPayReceive());


                    if (transfers.get(i).getValueDate().lte(valDate)) {
                        if (transfers.get(i).getTransferType().equalsIgnoreCase("SECURITY")) {
                            amount = transfers.get(i).getNominalAmount();
                            direction = payRec;

                        }
                        finalAmount = finalAmount + amount;
                    }
                }

                return finalAmount;
                */

            } catch (CalypsoServiceException exc) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);

            }
        }
        return finalAmount;
    }

    public static Boolean filterTransfer(BOTransfer transfer) {

        return !Status.isCanceled(transfer.getStatus()) && !Status.S_SETTLED.equals(transfer.getStatus()) && "SECURITY".equals(transfer.getTransferType());
        /*
        if (transfers.getStatus().toString().equalsIgnoreCase("CANCELED") ||
                transfers.getStatus().toString().equalsIgnoreCase("SETTLED") ||
                transfers.getStatus().toString().equalsIgnoreCase("SPLIT")) {
            return true;
        }
        return false; */
    }
    protected boolean processFile(String inputFile, String outputFile, Vector<DMOPosition> UN01Rows, JDate valDate) throws IOException {
        //  JDate valDate = valDateTime.getJDate();
        String validateString = "";
        if (valDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy", Locale.UK);
            validateString = format.format(valDate.getDate());
        }

        BufferedReader reader = null;
        OutputStream outStream = new FileOutputStream(outputFile, true);
        String delimiter = ",";
        try {

            reader = new BufferedReader(new FileReader(inputFile));
            String line;
            int lineNumber = -1;
            while ((line = reader.readLine()) != null) {

                lineNumber++;
                Log.debug(LOG_CATEGORY_SCHEDULED_TASK, "Processing line " + lineNumber);

                String[] fields = line.split(",");

                String positionType = fields[0];

                if (positionType.equalsIgnoreCase("DT01")) {

                    if (!validateString.equalsIgnoreCase(fields[1])) {
                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Valuation date check failed. Valuation date is file :" + validateString + "Valuation date is file" + fields[1]);
                        return false;
                    }

                    outStream.write(fields[0].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[1].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[2].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[3].getBytes());
                    outStream.write("\r\n".getBytes());
                }

                if (positionType.equalsIgnoreCase("PO01")) {
                    outStream.write(fields[0].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[1].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[2].getBytes());
                    outStream.write("\r\n".getBytes());
                }

                if (positionType.equalsIgnoreCase("PO02")) {
                    outStream.write(fields[0].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[1].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[2].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[3].getBytes());
                    outStream.write("\r\n".getBytes());
                }

                if (positionType.equalsIgnoreCase("TR01")) {
                    for (int i = 0; i < UN01Rows.size(); i++) {
                        DMOPosition un01Row = UN01Rows.get(i);
                        outStream.write(un01Row.getPosition_Type().getBytes());
                        outStream.write(delimiter.getBytes());
                        outStream.write(un01Row.getCpty_full_name().getBytes());
                        outStream.write(delimiter.getBytes());
                        outStream.write(un01Row.getDirection().getBytes());
                        outStream.write(delimiter.getBytes());
                        outStream.write(un01Row.getProduct_Code().getBytes());
                        outStream.write(delimiter.getBytes());
                        outStream.write(un01Row.getTrade_settle_date().getBytes());
                        outStream.write(delimiter.getBytes());
                        outStream.write(un01Row.getNominal_in_1000().getBytes());
                        outStream.write("\r\n".getBytes());
                    }
                    outStream.write(fields[0].getBytes());
                    outStream.write(delimiter.getBytes());
                    int noOfRows = Integer.parseInt(fields[1]);
                    noOfRows = noOfRows + UN01Rows.size();
                    outStream.write(String.valueOf(noOfRows).getBytes());
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

            outStream.close();
        }

        return true;
    }

    public void handleInputFileErrorData(String inputFile , String fromAttr , String toAttr , JDate valDate , String failReason) {
        try {
            ArrayList<String> files = new ArrayList<>();
            files.add(inputFile);
            String subject = "Information About DMO Report  : Invalid Input File";
            boolean proccesOK = true;
            proccesOK = areFilesGenerated(files);

            List<String> to = getEmails(toAttr);
            String from = fromAttr;
            try {
                if (!Util.isEmpty(to) && proccesOK) {
                    String body = getTextBody(valDate , failReason);
                    CollateralUtilities.sendEmail(to, subject, body, from, files);
                }
            } catch (Exception e) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }
        } catch (Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
        }
    }

    public List<String> getEmails(String toattr) {

        List<String> to = null;

        if (Util.isEmpty(to)) {
            to = new ArrayList<String>();
        }

        String emails = toattr;
        if (!Util.isEmpty(emails)) {
            to.addAll(Arrays.asList(emails.split(";")));
        }
        return to;
    }

    private boolean areFilesGenerated(ArrayList<String> files) {
        if (!files.isEmpty()) {
            for (String namefile : files) {
                try {
                    File f = new File(namefile);
                } catch (Exception e) {
                    Log.error(this, e); //sonar
                    return false;
                }

            }
        } else {
            return false;
        }

        return true;
    }

    public String getTextBody(JDate valDate , String failReason) {

        String month = String.valueOf(valDate.getMonth());
        if (month.length() < 2) {
            month = "0".concat(month);
        }

        String day = String.valueOf(valDate.getDayOfMonth());
        if (day.length() < 2) {
            day = "0".concat(day);
        }
        StringBuffer body = new StringBuffer();

        String date = String.valueOf(valDate.getYear()) +
                month + day;
        body.append("<br>Hi Team,");
        body.append("<br>We have received invalid input file for date "+ date + ".The validation failed because :");
        body.append("<br>"+ failReason);
        body.append("<br>");
        body.append("<br>Please find attached the input file for review");
        body.append("<br><br>Thank You.");
        return body.toString();
    }
}
