package calypsox.tk.report;

import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.collateral.MarginCallDetailEntryFacade;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import org.apache.commons.lang.StringUtils;

import java.rmi.RemoteException;
import java.util.*;

public class SantContractBasketReport extends Report {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -6089330498152017144L;

    public static final String TYPE = "SantContractBasket";
    private static final String PRODUCT_CODE_ISIN="PRODUCT_CODE.ISIN";

    /**
     *
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput load(Vector errors) {

        final StandardReportOutput output = new StandardReportOutput(this);

        Vector v = new Vector();
        v.add("TARGET");
        getReportTemplate().setHolidays(v);

        List<ReportRow> rowsList = new ArrayList<ReportRow>();

        List<Integer> idContracts = new ArrayList<Integer>();

        Map<String, String> params = new HashMap<>();
        params.put("MC_GC_POOLING", "SI");
        params.put("MC_TRIPARTY", "SI");

        Map<Integer, CollateralConfig> idContractacuerdo = getFilteredCollateralContractsIds(params, idContracts);
        List<AcuerdoIsinItem> acuerdoIsins = getCestaIsins(idContractacuerdo, errors);


        for (AcuerdoIsinItem key : acuerdoIsins) {

            ReportRow row = new ReportRow(key, TYPE);
            rowsList.add(row);
        }



        output.setRows(rowsList.toArray(new ReportRow[0]));
        return output;
    }

    private List<AcuerdoIsinItem> getCestaIsins(Map<Integer, CollateralConfig> idContractAcuerdo, Vector errors){

        List<AcuerdoIsinItem> acuerdoIsinItems = new ArrayList<>();

        Set<Integer> idContracts = idContractAcuerdo.keySet();
        List<Integer> listIdContracts = new ArrayList<Integer>(idContracts);

        // It gets the entries related to the contracts marked as MC_TRIPARTY or GC_POOLING
        List<MarginCallEntryDTO> marginCallEntryDTOS = null;
        try {
            marginCallEntryDTOS = CollateralManagerUtil.loadMarginCallEntriesDTO(listIdContracts, getValDate());
        } catch (CollateralServiceException e) {
            e.printStackTrace();
        }

        // It gets the trades related to the entries
        for(MarginCallEntryDTO entry:marginCallEntryDTOS){
            for(MarginCallDetailEntryDTO detailEntry: entry.getDetailEntries()){
                try {
                    Trade trade = getDSConnection().getRemoteTrade().getTrade(detailEntry.getTradeId());
                    String isin = "";
                    com.calypso.tk.report.MarginCallDetailReportStyle reportStyle = new MarginCallDetailReportStyle();
                    if(detailEntry instanceof MarginCallDetailEntryFacade) {
                        isin = (String) reportStyle.getColumnValue((MarginCallDetailEntryFacade) detailEntry, PRODUCT_CODE_ISIN, null);
                    }
                    if(StringUtils.isNotBlank(isin)) {
                        CollateralConfig acuerdo = idContractAcuerdo.get(entry.getCollateralConfigId());
                        String ccy = acuerdo.getCurrency();
                        String cpty = acuerdo.getLegalEntity().getCode();
                        String acuerdoNombre = acuerdo.getName();

                        AcuerdoIsinItem key = new AcuerdoIsinItem(isin, acuerdoNombre, ccy, cpty);

                        if (!acuerdoIsinItems.contains(key)) {
                            acuerdoIsinItems.add(key);
                        }
                    }
                } catch (CalypsoServiceException e) {
                    errors.add(e.getMessage());
                    Log.error(this, e);
                }
            }
        }
        return acuerdoIsinItems;
    }



    /**
     * @param params
     * @param ids
     * @return
     */
    private Map<Integer, CollateralConfig> getFilteredCollateralContractsIds(Map<String, String> params,
                                                                             List<Integer> ids) {

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
        List<CollateralConfig> listContracts = null;
        Map<Integer, CollateralConfig> idContractAcuerdo = new HashMap<Integer, CollateralConfig>();
        // retrieve the contract
        try {

            listContracts = srvReg.getCollateralDataServer().getAllMarginCallConfig();

        } catch (RemoteException e) {
            // DB error, should not happen
            Log.error(this, e.getLocalizedMessage());
            Log.error(this, e); //sonar
            return null;
        }

        for (CollateralConfig collateralConfig : listContracts) {
            boolean attOK = false;

            for(String attribCode:params.keySet()){
                String attribValue = params.get(attribCode);

                if(!attOK) { // It checks if the contract has already been added to the list

                    final String field = collateralConfig.getAdditionalField(attribCode);

                    if (attribValue.equals(field)) {
                        ids.add(collateralConfig.getId());
                        idContractAcuerdo.put(collateralConfig.getId(),
                                collateralConfig);

                        attOK = true;

                    }
                }

            }



        }
        return idContractAcuerdo;
    }

}
