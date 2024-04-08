package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.BOSecurityPositionReport;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InventoryPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;
import org.apache.commons.lang.ArrayUtils;

import java.rmi.RemoteException;
import java.util.*;

public class SantFiPositionReport extends BOSecurityPositionReport {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 6196228029054271524L;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(final Vector vector) {

        JDate processDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                "PROCESS_DATE", "PROCESS_PLUS", "PROCESS_TENOR");

        Log.info(SantFiPositionReport.class, "processDate=" + processDate);

        this._reportTemplate.put("PROCESS_DATE", Util.dateToString(processDate));
        this._reportTemplate.put("StartDate", Util.dateToString(processDate));
        this._reportTemplate.put("EndDate", Util.dateToString(processDate));
        // this._valuationDateTime=processDate.getJDatetime();
        // v14 Migration - GSM ALL not allowed any more
        this._reportTemplate.put(BOPositionReportTemplate.POSITION_TYPE, "Not settled,Actual,Theoretical");

        try {
            // Check if the process date is empty. Cannot be empty
            computeProcessStartDate(vector);

            StandardReportOutput output = new StandardReportOutput(this);
            //AAP MIG14.4 This crap needs to be refactored
            if (vector.isEmpty()) {
                ReportRow[] rows = ((DefaultReportOutput) super.load(new Vector<Object>())).getRows();
                // AAP
                List<ReportRow> rowsList = new ArrayList<ReportRow>();
                for (ReportRow row : rows) {
                    InventorySecurityPosition pos = (InventorySecurityPosition) row.getProperty("Default");
                    if (pos != null) {
                        InventorySecurityPosition pos1 = getInventorySecPositions(pos);
                        if (pos1 != null) {
                            double quantity = pos1.getTotalSecurity();
                            row.setProperty("FRITANGA", quantity);
                        } else
                            row.setProperty("FRITANGA", new Double(0));
                    } else
                        row.setProperty("FRITANGA", new Double(0));

                    rowsList.add(row);
                }
                output.setRows(rowsList.toArray(new ReportRow[0]));

                SantFiPositionReportStyle.setProcessDate(processDate);
                return output;
            }

            return null;
        } catch (final Exception exception) {
            Log.error(this, exception.getMessage());
            Log.error(this, exception); //sonar
            vector.add(exception.getMessage());
            return null;
        }
    }

    @Override
    public boolean filterPosition(Inventory inventoryposition) {
        if (!super.filterPosition(inventoryposition)) {
            return false;
        }
        if (inventoryposition.getPositionType().toUpperCase().equals("ACTUAL")
                || inventoryposition.getPositionType().toUpperCase().equals("FAILED")) {
            return true;
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected InventoryPositionArray load(final String s, final String s1, final Vector vector, List<CalypsoBindVariable> bindVariables) {
        final InventoryPositionArray inventoryPositions = super.load(s, s1, vector, null);
        final Vector<InventorySecurityPosition> positions = inventoryPositions.toVector();
        final InventoryPositionArray inventoryPositionsFiltered = new InventorySecurityPositionArray();

        JDate fromDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                "NextPaymentStartRange", "NextPaymentStartRangePlus", "NextPaymentStartRangeTenor");

        final JDate toDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                "NextPaymentEndRange", "NextPaymentEndRangePlus", "NextPaymentEndRangeTenor");

        final String buySell = (String) this._reportTemplate.get("Buy/Sell");

        for (final InventorySecurityPosition position : positions) {
            Bond bond = null;
            boolean toAdd = false;

            if (position.getProduct() instanceof Bond) {
                bond = (Bond) position.getProduct();
            }

            if (bond != null) {

                JDate nextPaymentDate = null;
                if (fromDate != null) {
                    nextPaymentDate = bond.getNextCouponDate(JDate.getNow());
                } else {
                    fromDate = JDate.getNow();
                    nextPaymentDate = bond.getNextCouponDate(JDate.getNow());
                }

                if (nextPaymentDate == null) {
                    if (isValidDirection(buySell, position)) {
                        toAdd = true;
                    }
                } else {
                    if ((fromDate == null) && (toDate == null)) {
                        if (isValidDirection(buySell, position)) {
                            toAdd = true;
                        }
                    } else if (toDate == null) {
                        if ((nextPaymentDate.after(fromDate) || nextPaymentDate.equals(fromDate))) {
                            if (isValidDirection(buySell, position)) {
                                toAdd = true;
                            }
                        }
                    } else if (fromDate == null) {
                        if ((nextPaymentDate.before(toDate) || nextPaymentDate.equals(toDate))) {
                            if (isValidDirection(buySell, position)) {
                                toAdd = true;
                            }
                        }
                    } else {
                        if (((nextPaymentDate.before(toDate) || nextPaymentDate.equals(toDate)))
                                && (nextPaymentDate.after(fromDate) || nextPaymentDate.equals(fromDate))) {
                            if (isValidDirection(buySell, position)) {
                                toAdd = true;
                            }
                        }
                    }
                }
            }

            if (toAdd) {
                JDate positionDate = JDate.valueOf((String) this._reportTemplate.get("PROCESS_DATE"));
                position.setPositionDate(positionDate);
                inventoryPositionsFiltered.addElement(position);
            }
        }

        return inventoryPositionsFiltered;
    }

    private boolean isValidDirection(String direction, InventorySecurityPosition position) {

        if (Util.isEmpty(direction)) {
            return true;
        }
        if ("Buy".equals(direction) && (position.getTotal() >= 0)) {
            return true;
        } else if ("Sell".equals(direction) && (position.getTotal() < 0)) {
            return true;
        }

        return false;
    }

    public InventorySecurityPosition getInventorySecPositions(InventorySecurityPosition positionOld)
            throws RemoteException {
        StringBuilder where = new StringBuilder();
        StringBuilder from = new StringBuilder();
        where.append(" inv_secposition.internal_external = '" + positionOld.getInternalExternal() + "' ");
        where.append(" AND inv_secposition.date_type = '" + positionOld.getDateType() + "' ");
        where.append(" AND inv_secposition.position_type = '" + positionOld.getPositionType() + "'");
        // where.append(" AND inv_secposition.config_id = '" +
        // positionOld.getConfigId() + "'");
        where.append(" AND inv_secposition.security_id = product_desc.product_id");
        where.append(" AND product_desc.product_family = 'Bond'");
        where.append(" AND inv_secposition.security_id IN (" + positionOld.getSecurityId() + ")");
        where.append(" AND inv_secposition.position_date = ");
        where.append(" (");// BEGIN SELECT
        where.append(" select MAX(temp.position_date) from inv_secposition temp ");
        where.append(" WHERE inv_secposition.internal_external = temp.internal_external ");
        where.append(" AND inv_secposition.date_type = temp.date_type ");
        where.append(" AND inv_secposition.position_type = temp.position_type ");
        where.append(" AND inv_secposition.account_id = temp.account_id ");
        where.append(" AND inv_secposition.security_id = temp.security_id ");
        where.append(" AND inv_secposition.agent_id = temp.agent_id ");
        where.append(" AND inv_secposition.book_id = temp.book_id ");
        where.append(" AND TRUNC(temp.position_date) <= ")
                .append(com.calypso.tk.core.Util.date2SQLString(positionOld.getPositionDate()));
        where.append(" )");// END SELECT

        from.append("product_desc");

        InventorySecurityPositionArray secPositions = DSConnection.getDefault().getRemoteBackOffice()
                .getInventorySecurityPositions(from.toString(), where.toString(), null);
        Vector<InventorySecurityPosition> positionV = secPositions.toVector();
        for (InventorySecurityPosition position : positionV) {
            if (position.getMarginCallConfigId() == positionOld.getMarginCallConfigId())
                // &&
                // (position.getPositionDate().equals(positionOld.getPositionDate())))
                return position;
        }
        return null;
        // return secPositions;
        // if (secPositions != null) {
        // for (int i = 0; i < secPositions.size(); i++) {
        // InventorySecurityPosition position = secPositions.get(i);
        // if (position != null) {
        //
        // String positionId = position.getSecurityId() + ";" +
        // position.getBookId() + ";"
        // + position.getAgentId() + ";" + position.getAccountId();
        // this.hashPositions.put(positionId, position);
        // }
        //
        // }
        // }
    }


        @Override
        protected boolean buildWhere(final StringBuffer stringbuffer, final StringBuffer stringbuffer1, final String s, Set<Integer> booksids,Set<Integer> configIds, List<CalypsoBindVariable> bindVariables)
            throws Exception {
        super.buildWhere(stringbuffer, stringbuffer1, "product_desc", null,configIds, null);
        final ReportTemplate template = getReportTemplate();

        JDate maturityStartRange = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                "MaturityStartRange", "MaturityStartRangePlus", "MaturityStartRangeTenor");
        JDate maturityEndRange = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                "MaturityEndRange", "MaturityEndRangePlus", "MaturityEndRangeTenor");
        if ((maturityStartRange != null) || (maturityEndRange != null)) {
            stringbuffer.append(" AND ");
            stringbuffer.append(" security_id IN (");
            stringbuffer
                    .append("SELECT product_id FROM product_desc WHERE und_security_id = inv_secposition.security_id ");
        }

        if (maturityStartRange != null) {
            stringbuffer.append("  AND maturity_date >=");
            stringbuffer.append(Util.date2SQLString(maturityStartRange));
        }

        if (maturityEndRange != null) {
            stringbuffer.append("  AND maturity_date <=");
            stringbuffer.append(Util.date2SQLString(maturityEndRange));
        }

        if ((maturityStartRange != null) || (maturityEndRange != null)) {
            stringbuffer.append(" ) ");
        }

        if (!Util.isEmpty((String) template.get("Bonds"))) {
            stringbuffer.append(" AND ");
            stringbuffer.append(" security_id IN (");
            stringbuffer.append("SELECT product_id FROM product_sec_code WHERE sec_code = 'ISIN' AND code_value in(");

            final String[] isins = ((String) template.get("Bonds")).split(",");

            final StringBuffer isinCodes = new StringBuffer("");
            for (final String isin : isins) {
                isinCodes.append("'").append(isin).append("'").append(",");
            }

            final String finalIsinCodes = isinCodes.substring(0, isinCodes.length() - 1);
            stringbuffer.append(finalIsinCodes);
            stringbuffer.append(" ) ");
            stringbuffer.append(" ) ");
        }

        return true;

    }

    private void computeProcessStartDate(Vector<String> errorMsgs) {
        JDate processStartDate = null;
        processStartDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                "PROCESS_DATE", "PROCESS_PLUS", "PROCESS_TENOR");

        if (processStartDate == null) {
            errorMsgs.add("Process Start Date cannot be empty.");
        }
    }

    @SuppressWarnings({"rawtypes", "unused"})
    protected static JDate adjustDate(final ReportTemplate reporttemplate, JDate jdate, final String s, final String s1) {
        if (Util.isEmpty(s1)) {
            return jdate;
        }
        Vector vector = null;
        boolean flag = false;
        if (reporttemplate != null) {
            vector = reporttemplate.getHolidays();
            flag = reporttemplate.getBusDays();
        }
        final Vector vector1 = LocalCache.getDomainValues(DSConnection.getDefault(), "DateRuleReportTemplate");
        if (!Util.isEmpty(vector1) && vector1.contains(s1)) {
            final DateRule daterule = BOCache.getDateRule(DSConnection.getDefault(), s1);
            if (daterule != null) {
                if (s.equals("-")) {
                    jdate = daterule.previous(jdate);
                } else {
                    jdate = daterule.next(jdate);
                }
                return jdate;
            }
        }
        final Tenor tenor = new Tenor(s1);
        if (vector != null) {
            if ((s != null) && s.equals("-")) {
                if (!flag) {
                    return Holiday.getCurrent().addCalendarDays(jdate, vector, -1 * tenor.getCode());
                } else {
                    return Holiday.getCurrent().addBusinessDays(jdate, vector, -1 * tenor.getCode());
                }
            }
            if (!flag) {
                return Holiday.getCurrent().addCalendarDays(jdate, vector, tenor.getCode());
            } else {
                return Holiday.getCurrent().addBusinessDays(jdate, vector, tenor.getCode());
            }
        }
        if (s1.trim().length() > 0) {
            if ((s != null) && s.equals("-")) {
                jdate = jdate.substractTenor(tenor);
            } else {
                jdate = jdate.addTenor(tenor);
            }
        }
        return jdate;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.report.BOPositionReport#extractLegalEntityIds() v14
     * Mig - for whatever reason, error core in Util.string2IntVector(s);
     */
    @Override
    protected int[] extractLegalEntityIds() {
        String s = (String) _reportTemplate.get("AGENT_ID");
        if (Util.isEmpty(s)) {
            return ArrayUtils.EMPTY_INT_ARRAY;
        }
        LegalEntity le = BOCache.getLegalEntity(getDSConnection(), s);
        Vector<Integer> list = new Vector<Integer>();// Util.string2IntVector(s);
        if (le != null) {
            list = new Vector<Integer>();
            list.add(Integer.valueOf(le.getId()));
        } else if (s.indexOf(",") > 0) {
            list = Util.string2IntVector(s);
        }
        return ArrayUtils.toPrimitive((Integer[]) list.toArray(new Integer[0]));
    }
}
