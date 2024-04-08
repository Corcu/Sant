package calypsox.tk.report;

import calypsox.tk.collateral.service.RemoteSantCollateralService;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigExposureGroup;
import com.calypso.tk.report.MarginCallPositionValuationReport;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class SantMCallPositionValCreReport extends MarginCallPositionValuationReport {

    //Init remote services
    //DSConnection.getDefault().getRMIService("baseSantCollateralService",RemoteSantCollateralService .class);

    @Override
    protected List<CollateralConfig> loadMarginCallConfigs(ReportTemplate template) {
        return super.loadMarginCallConfigs(template);
    }

    @Override
    public List<CollateralConfig> loadMarginCallConfigs(int poId, int leId, List<String> contractTypes) {
        //return super.loadMarginCallConfigs(poId, leId, contractTypes);
        List original = null;

        original = loadContractByAccSec();

        if(Util.isEmpty(original)){
            try {
                original = ServiceRegistry.getDefault().getCollateralDataServer().getAllMarginCallConfig();
            } catch (Exception var16) {
                Log.error(this, var16);
            }
        }

        String mccSubtype = (String)this._reportTemplate.get("MARGIN_CALL_CONFIG_SUBTYPE");
        String mccContractGroup = (String)this._reportTemplate.get("MARGIN_CALL_CONFIG_GROUP");
        String configLevel = (String)this._reportTemplate.get("CONFIG_LEVEL");
        List<String> subtypes = new ArrayList();
        if (!Util.isEmpty(mccSubtype)) {
            Util.stringToCollection(subtypes, mccSubtype, ",", false);
        }

        List<String> contractGroups = new ArrayList();
        if (!Util.isEmpty(mccContractGroup)) {
            Util.stringToCollection(contractGroups, mccContractGroup, ",", false);
        }

        List<CollateralConfig> result = new Vector();

        for(int index = original.size() - 1; index >= 0; --index) {
            CollateralConfig mcc = (CollateralConfig)original.get(index);
            if ((!"Exposure Group".equals(configLevel) || mcc instanceof CollateralConfigExposureGroup) && (!"Master".equals(configLevel) || CollateralConfig.class.equals(mcc.getClass()))) {
                int pId = mcc.getPoId();
                LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), pId);
                boolean access = AccessUtil.isAuthorizedProcOrg(po, false);
                if (access && (Util.isEmpty(contractTypes) || contractTypes.contains(mcc.getContractType())) && (Util.isEmpty(subtypes) || subtypes.contains(mcc.getSubtype())) && (Util.isEmpty(contractGroups) || contractGroups.contains(mcc.getContractGroup()))) {
                    result.add(mcc);
                }
            }
        }

        return result;
    }

    private  List<CollateralConfig> loadContractByAccSec(){
        final RemoteSantCollateralService remoteSantColService = DSConnection.getDefault().getRMIService("baseSantCollateralService", RemoteSantCollateralService.class);

        List<CollateralConfig> marginCallConfigByAF = new ArrayList<>();
        HashMap<String,String> aditionalFields = new HashMap<>();
        aditionalFields.put("ACCOUNTING_SECURITY","True");

        HashMap<String,List<CollateralConfig>> contracts = new HashMap<>();
        try {
            marginCallConfigByAF = remoteSantColService.getMarginCallConfigByAdditionalField(aditionalFields);
            //Group contracts by Contract currency
            if( !Util.isEmpty(marginCallConfigByAF)){
                for(CollateralConfig config : marginCallConfigByAF){
                    if(contracts.containsKey(config.getCurrency())){
                        Log.info(this,"More than one STM contract found for currency: " + config.getCurrency());
                        contracts.get(config.getCurrency()).add(config);
                    }else{
                        List<CollateralConfig> stmContracts = new ArrayList<>();
                        stmContracts.add(config);
                        contracts.put(config.getCurrency(),stmContracts);
                    }
                }
            }
        } catch (PersistenceException e) {
            Log.error(this,"Cannot load STM Contracts: " + e );
        }

        return marginCallConfigByAF;
    }


}
