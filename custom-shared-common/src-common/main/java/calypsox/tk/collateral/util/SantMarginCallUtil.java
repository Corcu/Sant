/**
 *
 */
package calypsox.tk.collateral.util;

import calypsox.util.collateral.SantMarginCallEntryUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.collateral.BilateralEntry;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.marketdata.PricingEnvProxy;
import com.calypso.tk.collateral.marketdata.impl.DefaultPricingEnvProxy;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.impl.CollateralPricingConfig;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * @author aela
 *
 */
public class SantMarginCallUtil {
    /**
     * @param comment
     */
    public static MarginCallEntryDTO getMarginCallEntryDTO(BOMessage message, DSConnection dsCon) throws Exception {
        MarginCallEntryDTO dto = null;

        String attribute = message.getAttribute(MarginCallEntry.MESSAGE_ATTRIBUTE_ENTRY_ID);
        if (!Util.isEmpty(attribute)) {
            int marginCallEntryId = Integer.valueOf(attribute);
            dto = ServiceRegistry.getDefault(dsCon).getCollateralServer().loadEntry(marginCallEntryId);
        }
        if (dto == null) {
            attribute = message.getAttribute("CommentId");
            if (!Util.isEmpty(attribute)) {
                int marginCallEntryId = Integer.valueOf(attribute);
                dto = ServiceRegistry.getDefault(dsCon).getCollateralServer().loadEntry(marginCallEntryId);
            }
        }
        return dto;
    }


    /**
     * @param dto
     * @param config
     * @return
     */
    public static MarginCallEntry getMarginCallEntry(MarginCallEntryDTO dto, CollateralConfig config, boolean getHeavyWeight) {
        MarginCallEntry entry = null;
        if ((dto != null) && (config != null)) {
            //MIG V16
  /*          PricingEnv pricingEnv = DSConnection.getDefault().getRemoteMarketData()
                    .getPricingEnv(config.getPricingEnvName(), dto.getValueDatetime());*/
            PricingEnvProxy pricingEnvProxy = new DefaultPricingEnvProxy(new CollateralPricingConfig());
            entry = new BilateralEntry(ServiceRegistry.getDefaultContext(),
                    ServiceRegistry.getDefaultExposureContext(), config, pricingEnvProxy, dto);
            if (getHeavyWeight) {
                SantMarginCallEntryUtil.fillSingleLightWeightMarginCallEntry(entry);
            }
        }
        return entry;
    }

    /* *//**
     * Get a marginCallEntry instance using the given dto and contract config and pricingEnv
     * MIG V16 aalonsop: This two methods doesnt make sense, none of them uses the PricingEnv parameters, so they r basically the same.
     * That why this is commented out
     * @param entryId
     * @param dsCon
     * @return
     *//*
    public static MarginCallEntry getMarginCallEntry(MarginCallEntryDTO dto, CollateralConfig config, PricingEnv pricingEnv) throws RemoteException {
        PricingEnvProxy pricingEnvProxy = new DefaultPricingEnvProxy(new CollateralPricingConfig());
        return new BilateralEntry(ServiceRegistry.getDefaultContext(),
                ServiceRegistry.getDefaultExposureContext(), config, pricingEnvProxy, dto);
    }*/

    /**
     *
     * @param entryId
     * @param dsCon
     * @return
     * @throws CollateralServiceException
     */
    public static MarginCallEntryDTO getMarginCallEntryDTO(int entryId, DSConnection dsCon) throws CollateralServiceException {
        return ServiceRegistry.getDefault(dsCon).getCollateralServer().loadEntry(entryId);
    }

    /**
     * @param entry
     * @return true if the margin call global required margin is <= 0
     */
    public static boolean isPayMarginCall(MarginCallEntryDTO entry) {
        return (entry.getGlobalRequiredMargin() <= 0);
    }

    /**
     * @param entry
     * @return true if the margin call global required margin is > 0
     */
    public static boolean isReceiveMarginCall(MarginCallEntryDTO entry) {
        return (entry.getGlobalRequiredMargin() > 0);
    }

    /**
     * @param entry
     * @return true if the margin call global required margin is <= 0
     */
    public static boolean isPayMarginCall(MarginCallEntry entry) {
        return (entry.getGlobalRequiredMargin() <= 0.0);
    }

    /**
     * @param entry
     * @return true if the margin call global required margin is > 0
     */
    public static boolean isReceiveMarginCall(MarginCallEntry entry) {
        return (entry.getGlobalRequiredMargin() > 0.0);
    }

    /**
     * @param le
     * @return true if the legal entity has an agent role
     */
    public static boolean isPOValuationAgent(LegalEntity le) {
        return le.hasRole(LegalEntity.AGENT);
    }

    /**
     * @param mcc
     *            a margin call config
     * @return true if the PO of the margin call acts as its agent
     */
    public static boolean isPOValuationAgent(CollateralConfig mcc) {
        return mcc.getValuationAgentId() == mcc.getProcessingOrg().getId();
    }

    /**
     * @param templateName
     * @param processDate
     * @param valDate
     * @param pricingEnvName
     * @param errors
     * @return
     * @throws RemoteException
     */
    @SuppressWarnings("rawtypes")
    public static ReportOutput loadEntriesFromTemplate(String templateName, JDatetime processDate, JDatetime valDate,
                                                       String pricingEnvName, Vector errors) throws RemoteException {
        return loadEntriesFromTemplate(templateName, processDate, valDate, null, pricingEnvName, null, errors);
    }

    /**
     * @param templateName
     * @param processDate
     * @param valDate
     * @param undoDate
     * @param pricingEnvName
     * @param filterName
     * @param errors
     * @return
     */
    public static ReportOutput loadEntriesFromTemplate(String templateName, JDatetime processDate, JDatetime valDate,
                                                       JDatetime undoDate, String pricingEnvName, String filterName, Vector errors) throws RemoteException {
        MarginCallReport marginCallReport = new MarginCallReport();
        DSConnection ds = DSConnection.getDefault();

        marginCallReport.setPricingEnv(ds.getRemoteMarketData().getPricingEnv(pricingEnvName));
        // MarginCallReportTemplate
        MarginCallReportTemplate template = (MarginCallReportTemplate) ds.getRemoteReferenceData().getReportTemplate(
                "MarginCall", templateName);
        if (template != null) {
            //template.put(MarginCallReportTemplate.VAL_DATE, valDate);
            template.put(MarginCallReportTemplate.PROCESS_DATE, processDate);
        }
        marginCallReport.setReportTemplate(template);
        marginCallReport.setFilterSet(filterName);
        marginCallReport.setUndoDatetime(undoDate);

        // load the report
        return marginCallReport.load(errors);
    }

}
