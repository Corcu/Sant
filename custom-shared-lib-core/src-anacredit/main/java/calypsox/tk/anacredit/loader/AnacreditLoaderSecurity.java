package calypsox.tk.anacredit.loader;

import calypsox.tk.anacredit.formatter.IOperacionesFormatter;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.util.AnacreditFactory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.*;

public abstract class AnacreditLoaderSecurity extends AnacreditLoader {
    private static final String ATTR_CONTRACT_TYPE = "ANACREDIT.Security.ContractTypes";

    protected abstract boolean isValidContract(CollateralConfig config, InventorySecurityPosition position) ;

    @Override
    public List<ReportRow> loadData(String extractionType, List<CollateralConfig> configs, ReportRow[] rows, JDate valDate, PricingEnv pEnv, Vector<String> errors) {
        ArrayList<ReportRow> result = new ArrayList<>();
        if (null != rows) {
            Arrays.asList(rows).stream()
                    .forEach(reportRow -> {
                        createAnacrediItem(extractionType, result, reportRow, valDate, pEnv, errors);
                    });
        }
        return result;
    }

    private void createAnacrediItem(String extractionType, ArrayList<ReportRow> result, ReportRow reportRow, JDate valDate, PricingEnv pEnv, Vector<String> errors) {
        InventorySecurityPosition position  =  reportRow.getProperty("Default");
        if (null!=position){
            CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), position.getMarginCallConfigId());
            if (null == config)   {
                return;
            }
            if (!"CSD".equals(config.getContractType()) && isValidContract(config, position)) {
                IOperacionesFormatter formatter = AnacreditFactory.instance().getOperacionesFormatter(extractionType);
                if (formatter != null) {
                    List<AnacreditOperacionesItem> items = formatter.format(config, reportRow , valDate, pEnv, errors);
                    for (AnacreditOperacionesItem item : items) {
                        ReportRow clone = (ReportRow) reportRow.clone();
                        AnacreditLoader.addRowData(result, clone, item);
                    }
                }
            }
        }
    }

    protected static List<String> getContractTypes(){
        List<String> result = LocalCache.getDomainValues(DSConnection.getDefault(), ATTR_CONTRACT_TYPE);
        return !Util.isEmpty(result) ? result : Arrays.asList("OSLA","ISMA");
    }

    @Override
    public final List<CollateralConfig> selectContractsToReport(Map<Integer, CollateralConfig> contracts, JDate valDate) {
        List<CollateralConfig> result = new ArrayList<>();
        contracts.forEach((key, value) -> {
            result.add(value);
        });
        return result;
    }
}
