package calypsox.tk.report;

import calypsox.tk.util.log.TimeLog;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallDetailEntryDTOReportTemplate;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.filter.AttributeFilter;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ExportMarginCallDetailEntryReport extends com.calypso.tk.report.MarginCallDetailEntryReport {

    private boolean _countOnly;

    public ExportMarginCallDetailEntryReport() {
        Log.system("calypsox.tk.report.ExportMarginCallDetailEntryReport",
                "Instanciando custom ExportMarginCallDetailEntryReport");
    }

    @Override
    public ReportOutput load(Vector errorMsgs) {
        return TimeLog.timeLog("TOTAL calypsox.tk.report.ExportMarginCallDetailEntryReport.load(Vector errorMsgs)",
                ()-> doLoad(errorMsgs));
    }

    public ReportOutput doLoad(Vector errorMsgs) {
        DefaultReportOutput output = new DefaultReportOutput(this);
        MarginCallDetailEntryDTOReportTemplate template = (MarginCallDetailEntryDTOReportTemplate)this.getReportTemplate();
        List<MarginCallDetailEntryDTO> detailEntries = detailEntries = template.getDetailEntries();
        if (Util.isEmpty(detailEntries)) {
            detailEntries = TimeLog.timeLog("calypsox.tk.report.ExportMarginCallDetailEntryReport.loadDetailEntries()",
                    ()->this.loadDetailEntries());
            Log.system("calypsox.tk.report.ExportMarginCallDetailEntryReport",
                    "loadEntries().size = " + detailEntries.size());

            List<MarginCallDetailEntryDTO> finalDetailEntries = detailEntries;
            detailEntries = TimeLog.timeLog("calypsox.tk.report.ExportMarginCallDetailEntryReport.filterEntriesAttributes(detailEntries)",
                    ()->this.filterEntriesAttributes(finalDetailEntries));
            Log.system("calypsox.tk.report.ExportMarginCallDetailEntryReport",
                    "this.filterEntriesAttributes(detailEntries).size = " + detailEntries.size());
        }

        boolean includeCollaterals = true;
        Boolean b = (Boolean)this._reportTemplate.get("INCLUDE_COLLATERALS");
        if (b != null) {
            includeCollaterals = b;
        }

        List<MarginCallDetailEntryDTO> finalDetailEntries = detailEntries;
        List<MarginCallDetailEntryDTO> allEntries = new ArrayList();
        List<MarginCallDetailEntryDTO> accepted = TimeLog.timeLog(
                "calypsox.tk.report.ExportMarginCallDetailEntryReport.filter(detailEntries)",
                ()->this.filter(finalDetailEntries));
        Log.system("calypsox.tk.report.ExportMarginCallDetailEntryReport",
                "this.filter(detailEntries).size = " + accepted.size());
        if (!Util.isEmpty(accepted)) {
            Iterator var9 = accepted.iterator();

            while(var9.hasNext()) {
                MarginCallDetailEntryDTO detailEntryDTO = (MarginCallDetailEntryDTO)var9.next();
                if (includeCollaterals) {
                    allEntries.add(detailEntryDTO);
                    List<MarginCallDetailEntryDTO> collaterals = detailEntryDTO.getCollateralDetailEntries();
                    if (!Util.isEmpty(collaterals)) {
                        allEntries.addAll(collaterals);
                    }
                } else if (!detailEntryDTO.isCollateralTrade()) {
                    allEntries.add(detailEntryDTO);
                }
            }
        }

//        if (this._countOnly) {
//            this.addPotentialSize(MarginCallDetailEntry.class.getName(), allEntries.size());
//        }

        ReportRow[] rows = new ReportRow[allEntries.size()];

        for(int i = 0; i < rows.length; ++i) {
            MarginCallDetailEntryDTO dto = (MarginCallDetailEntryDTO)allEntries.get(i);
            rows[i] = new ReportRow(dto, "Default");
            rows[i].setProperty("ValuationDatetime", this.getValuationDatetime());
        }

        Log.system("calypsox.tk.report.ExportMarginCallDetailEntryReport",
                "rows.size = " + rows.length);

        output.setRows(rows);
        return output;
    }

    private List<MarginCallDetailEntryDTO> filterEntriesAttributes(List<MarginCallDetailEntryDTO> detailEntries) {
        AttributeFilter<MarginCallDetailEntryDTO> filter = AttributeFilter.getInstance(MarginCallDetailEntryDTO.class);
        filter.setAttributes(this.getAttributeName(), this.getAttributeValue());
        filter.setCollateralServiceRegistry(this.getServiceRegistry());
        MarginCallDetailEntryDTOReportTemplate template = (MarginCallDetailEntryDTOReportTemplate)this.getReportTemplate();
        String filterName = (String)template.get("COLLATERAL_CONTRACT_FILTER");
        StaticDataFilter sdFilter = BOCache.getStaticDataFilter(this.getDSConnection(), filterName);
        if (sdFilter != null) {
            Iterator iter = detailEntries.iterator();

            while(iter.hasNext()) {
                MarginCallDetailEntryDTO dto = (MarginCallDetailEntryDTO)iter.next();
                CollateralConfig config = CacheCollateralClient.getCollateralConfig(this.getDSConnection(), dto.getMarginCallConfigId());
                ReportRow configRow = new ReportRow(config, "MarginCallConfig");
                if (!sdFilter.accept(configRow)) {
                    iter.remove();
                }
            }
        }

        return filter.filter(detailEntries);
    }

    private List<MarginCallDetailEntryDTO> loadDetailEntries() {
        List<MarginCallDetailEntryDTO> result = null;
        String where = this.buildQuery();
        List<String> from = this.buildFrom(where);
        Boolean includeDetailArchived = (Boolean)this._reportTemplate.get("ARCHIVED_DETAIL_ENTRY");

        Log.system("calypsox.tk.report.ExportMarginCallDetailEntryReport",
                "\nQUERY FROM = " + from +
                        "\nQUERY WHERE = " + where);
        try {
            result = this.getServiceRegistry().getDashBoardServer().loadMarginCallDetailEntries(where, from);

            if (includeDetailArchived != null && includeDetailArchived) {
                from = this.modifyAlias(from);
                result.addAll(this.getServiceRegistry().getDashBoardServer().loadMarginCallDetailEntries(where, "mrgcall_detail_entries_hist", from));
            }
        } catch (RemoteException var6) {
            Log.error(this, var6);
        }

        return result;
    }

    private String buildQuery() {
        if (this._reportTemplate == null) {
            return null;
        } else {
            String entryWhere = this.buildQuery(this._reportTemplate);
            StringBuilder where = new StringBuilder(32);
            if (!Util.isEmpty(entryWhere)) {
                where.append(entryWhere)
                        .append(" AND margin_call_entries.MCC_ID = margin_call_detail_entries.MCC_ID ")
                        .append(" AND margin_call_entries.MCC_ID = mrgcall_config.MRG_CALL_DEF ")
                        .append(" AND margin_call_detail_entries.mc_entry_id = margin_call_entries.id ");
            }

            return where.toString();
        }
    }

    private List<MarginCallDetailEntryDTO> filter(List<MarginCallDetailEntryDTO> details) {
        List<MarginCallDetailEntryDTO> result = new ArrayList();
        List<String> currency = this.getFilter("CURRENCY");
        List<String> pricingStatus = this.getFilter("PRICING_STATUS");
        List<String> productType = this.getFilter("PRODUCT_TYPE");
        if (!Util.isEmpty(details)) {
            Iterator var6 = details.iterator();

            while(var6.hasNext()) {
                MarginCallDetailEntryDTO detail = (MarginCallDetailEntryDTO)var6.next();
                if (this.isAuthorized(detail) && this.accept(detail.getStatus(), pricingStatus) && this.accept(detail.getProductType(), productType) && this.accept(detail.getCurrency(), currency)) {
                    result.add(detail);
                }
            }
        }

        return result;
    }



    private boolean accept(String value, List<String> accepted) {
        return !Util.isEmpty(accepted) ? accepted.contains(value) : true;
    }

    private List<String> getFilter(String field) {
        List<String> result = null;
        String s = (String)this._reportTemplate.get(field);
        if (!Util.isEmpty(s)) {
            List<String> list = new ArrayList();
            result = (List)Util.stringToCollection(list, s, ",", false);
        }

        return result;
    }

    @Override
    public Map getPotentialSize() {
        Vector errors = new Vector();

        try {
            this._potentialSize = new HashMap();
            this._countOnly = true;
            this.load(errors);
        } finally {
            this._countOnly = false;
        }

        return this._potentialSize;
    }

}
