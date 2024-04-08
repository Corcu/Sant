package calypsox.tk.report.generic.loader;

import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import java.util.*;

public class SantPortfolioRealSettlementTradesLoader extends SantEnableThread<Long, Trade> {

    protected String agreementIds;
    protected JDate valDate;

    public SantPortfolioRealSettlementTradesLoader(boolean enableThreading, ReportTemplate template, String agreementIds,
                                                   JDate valDate) {
        super(template, enableThreading);
        this.agreementIds = agreementIds;
        this.valDate = valDate;
    }

    public void setAgreementIds(String agreementIds) {
        this.agreementIds = agreementIds;
    }

    public void setValDate(JDate valDate) {
        this.valDate = valDate;
    }

    public List<Trade> getDataList() {
        return dataList;
    }

    public void loadData() {

        if (null != this.agreementIds) {
            final Vector<Integer> v = Util.string2IntVector(agreementIds);
            MarginCallDetailEntryDTOReport marginCallDetailEntryReport = new MarginCallDetailEntryDTOReport();
            ArrayList<Long> tradeIds = new ArrayList<>();
            MarginCallDetailEntryDTOReportTemplate marginCallDetailEntryReportTemplate = buildDetailEntriesTemplate();

            for (Integer id : v) {
                try {
                    marginCallDetailEntryReportTemplate.put("MARGIN_CALL_CONFIG_IDS", String.valueOf(id));
                    marginCallDetailEntryReport.setReportTemplate(marginCallDetailEntryReportTemplate);
                    DefaultReportOutput output = (DefaultReportOutput) marginCallDetailEntryReport.load(new Vector());

                    for (ReportRow row : output.getRows()) {
                        MarginCallDetailEntryDTO entryDTO = row.getProperty("Default");
                        tradeIds.add(entryDTO.getTradeId());
                    }

                    this.dataList.addAll(DSConnection.getDefault().getRemoteTrade().getTrades(Util.toLongPrimitive(tradeIds)));

                } catch (CalypsoServiceException e) {
                    Log.error("Cant retrieve trade ids: " + Arrays.toString(tradeIds.toArray()));
                }
            }
        }
    }

    @Override
    protected Map<Long, Trade> getDataMapFromDataList() {
        return super.getDataAsMap();
    }

    private MarginCallDetailEntryDTOReportTemplate buildDetailEntriesTemplate() {
        MarginCallDetailEntryDTOReportTemplate marginCallDetailEntryReportTemplate = new MarginCallDetailEntryDTOReportTemplate();
        marginCallDetailEntryReportTemplate.put("ProcessStartDate", valDate.addBusinessDays(1,Util.string2Vector("SYSTEM")).toString());
        marginCallDetailEntryReportTemplate.put("ProcessEndDate", valDate.addBusinessDays(1,Util.string2Vector("SYSTEM")).toString());
        marginCallDetailEntryReportTemplate.setValDate(valDate.addBusinessDays(1,Util.string2Vector("SYSTEM")));
        return marginCallDetailEntryReportTemplate;
    }
}