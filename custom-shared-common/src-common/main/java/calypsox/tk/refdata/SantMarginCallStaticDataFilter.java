package calypsox.tk.refdata;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTransferNetHandler;
import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.util.product.BOTransferUtil;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.*;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.*;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

/**
 * Custom Static DataFilter for margin call
 *
 * @author aela
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SantMarginCallStaticDataFilter extends CollateralStaticDataFilter {

    private static final String IS_RECOUPONING = "Is Recoupponing mrg call";
    private static final String EMIR_DIRECTION = "Emir direction";
    private static final String IS_SEND_STATEMENT = "Is send statmenet";
    private static final String MC_CONTRACT_TYPE = "Sant Contract Type";

    private static final String IS_PO_VALUATION_AGENT = "valuation agent is PO";
    private static final String IS_PORTFOLIO_NOTIF_AUTO = "Is portfolio notif auto";
    private static final String IS_VALIDATION_AUTO = "Is validation auto";
    private static final String MESSAGE_EVENT_TYPE = "Message event type";
    private static final String SANT_MRG_CALL_ID = "Sant Margin call contract Id";
    private static final String TRADE_KYW_MRG_CALL_CTR_ID = "MC_CONTRACT_NUMBER";
    private static final String LAST_STATUS_BEFORE_SUBSTITUTION_ATTR = "Entry status before subst";
    private static final String LAST_ACTION = "Last Action";
    private static final String NB_PENDING_ALLOCS = "Not executed allocations number";
    public static final String IS_COLLATERALIZABLE_TRADE = "Is Collateralizable Trade";

    // BAU 5.4 - Add new attribute "IS_SEND_EMIR". This attibute show in
    // Collateral Manager
    // the contracts must be sended to SEND_EMIR
    private static final String IS_SEND_EMIR = "IS_SEND_EMIR";

    // START OA Filter messages to send to K+ 05/11/2013
    // BAU 5.2.0 - Add new filters in order to segregate security in bond &
    // equity.
    private static final String MC_BLOCK_MESSAGE_PAY_CASH = "Sant BLOCK_MESSAGE_PAY_CASH";
    private static final String MC_BLOCK_MESSAGE_PAY_SECURITIES = "Sant BLOCK_MESSAGE_PAY_SECURITIES";
    private static final String MC_BLOCK_MESSAGE_PAY_SECURITIES_EQTY =
            "Sant BLOCK_MESSAGE_PAY_SECURITIES_EQTY";
    private static final String MC_BLOCK_MESSAGE_RECEIVE_CASH = "Sant BLOCK_MESSAGE_RECEIVE_CASH";
    private static final String MC_BLOCK_MESSAGE_RECEIVE_SECURITIES =
            "Sant BLOCK_MESSAGE_RECEIVE_SECURITIES";
    private static final String MC_BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY =
            "Sant BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY";

    private static final String MC_SLB_CIRCUIT_PO = "Sant SLB_CIRCUIT_PO";

    // START eLab - Calypso Col - Filters - 07/06/2016
    private static final String SANT_MRG_CALL_CONTRACT_TYPE = "Sant Margin call contract type";
    private static final String SANT_MRG_CALL_CSD_TYPE = "Sant Margin call CSD type";
    private static final String IM_CSD_TYPE = "IM_CSD_TYPE";

    private static final String SANT_THIRDPARTY_SWIFT = "Sant Margin Call Thirdparty Swift";
    private static final String THIRDPARTY_SWIFT = "THIRDPARTY_SWIFT";

    // END eLab - Calypso Col - Filters - 07/06/2016

    // END OA Filter messages to send to K+ 05/11/2013

    //Call Account acd
    private static final String STM_FILTER = "Sant STM Filter";
    private static final String SETTLEMENT_FILTER = "Sant SETTLEMENT Filter";
    private static final String ACCOUNTING_FILTER = "Sant ACCOUNTING Filter";
    private static final String MEDUSA_FILTER = "Sant MEDUSA Filter";
    private static final String CALL_ACCOUNT_CIRCUIT_FILTER = "Sant CALL ACCOUNT Filter";
    private static final String SANT_SETTLEMENT_SEC = "Sant SETTLEMENT SEC DATE Filter";


    //Dates Filters
    private static final String CALL_ACCOUNT_CIRCUIT_DATE_FILTER = "Sant CALL ACCOUNT DATE Filter";
    private static final String ACCOUNTING_DATE_FILTER = "Sant ACCOUNTING DATE Filter";
    private static final String ACCOUNTING_SECURITY_FILTER = "Sant ACCOUNTING SECURITY Filter";
    private static final String SANT_IS_TRIPARTY = "Sant TRIPARTY Filter";
    private static final String SANT_TRIPARTY_AGENT = "Sant TRIPARTY AGENT Filter";
    private static final String SANT_SL_MIGRATION_DATE = "Sant SL MIGRATION DATE Filter";

    private static final String MEDUSA_DATE_FILTER = "Sant MEDUSA DATE Filter";
    private static final String SETTLEMENT_DATE_FILTER = "Sant SETTLEMENT DATE Filter";
    private static final String DELIVERY_NOTICE_GENERATION_FILTER = "Sant DELIVERY NOTICE GENERATION Filter";

    private static final String STM = "STM";
    private static final String SETTLEMENT = "SETTLEMENT";
    private static final String ACCOUNTING = "ACCOUNTING";
    private static final String ACCOUNTING_SECURITY = "ACCOUNTING_SECURITY";
    private static final String MEDUSA = "MEDUSA";
    private static final String CALL_ACCOUNT_CIRCUIT = "NEW_CALL_ACCOUNT_CIRCUIT";
    private static final String DELIVERY_NOTICE_GENERATION = "DELIVERY_NOTICE_GENERATION";

    private static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";

    private static final String NEW_CALL_ACCOUNT_CIRCUIT_DATE = "NEW_CALL_ACCOUNT_CIRCUIT_DATE";

    public static final String TODAY = "Today";
    public static final String PROCESS_DATE = "Process Date";

    private static final String SETTLEMENT_SEC = "SETTLEMENT_SEC";

    private static final String TRANSFER_SEC_TYPE="Transfer Security Type";
    private static final String EXPORT_BOND_DATE_ADDFIELD="Additional Field-POSITION_EXPORT_BOND_DATE";



    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.refdata.StaticDataFilterInterface#getDomainValues(com.
     * calypso.tk.service.DSConnection, java.util.Vector)
     */
    @Override
    public void getDomainValues(DSConnection arg0, Vector vect) {
        vect.addElement(IS_RECOUPONING);
        vect.addElement(EMIR_DIRECTION);
        vect.addElement(IS_SEND_STATEMENT);
        vect.addElement(IS_PO_VALUATION_AGENT);
        vect.addElement(IS_PORTFOLIO_NOTIF_AUTO);
        vect.addElement(IS_VALIDATION_AUTO);
        vect.addElement(MESSAGE_EVENT_TYPE);
        vect.addElement(MC_CONTRACT_TYPE);
        vect.addElement(SANT_MRG_CALL_ID);
        vect.addElement(LAST_STATUS_BEFORE_SUBSTITUTION_ATTR);
        vect.addElement(LAST_ACTION);
        vect.addElement(NB_PENDING_ALLOCS);
        vect.addElement(IS_COLLATERALIZABLE_TRADE);
        vect.addElement(IS_SEND_EMIR);
        // START OA Filter messages to send to K+ 05/11/2013
        // BAU 5.2.0 - Add new filters in order to segregate security in bond &
        // equity.
        vect.add(MC_BLOCK_MESSAGE_PAY_CASH);
        vect.add(MC_BLOCK_MESSAGE_PAY_SECURITIES);
        vect.add(MC_BLOCK_MESSAGE_PAY_SECURITIES_EQTY);
        vect.add(MC_BLOCK_MESSAGE_RECEIVE_CASH);
        vect.add(MC_BLOCK_MESSAGE_RECEIVE_SECURITIES);
        vect.add(MC_BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY);
        // END OA Filter messages to send to K+ 05/11/2013
        // START eLab - Calypso Col - Filters - 07/06/2016
        vect.addElement(SANT_MRG_CALL_CONTRACT_TYPE);
        vect.addElement(SANT_MRG_CALL_CSD_TYPE);
        // END eLab - Calypso Col - Filters - 07/06/2016
        vect.addElement(SANT_THIRDPARTY_SWIFT);
        vect.addElement(MC_SLB_CIRCUIT_PO);

        //Call Account Filters
        vect.addElement(STM_FILTER);
        vect.addElement(SETTLEMENT_FILTER);
        vect.addElement(ACCOUNTING_FILTER);
        vect.addElement(MEDUSA_FILTER);
        vect.addElement(CALL_ACCOUNT_CIRCUIT_FILTER);

        vect.addElement(CALL_ACCOUNT_CIRCUIT_DATE_FILTER);
        vect.addElement(ACCOUNTING_DATE_FILTER);
        vect.addElement(MEDUSA_DATE_FILTER);
        vect.addElement(SETTLEMENT_DATE_FILTER);
        vect.addElement(SANT_SL_MIGRATION_DATE);

        vect.addElement(TODAY);
        vect.addElement(PROCESS_DATE);
        vect.addElement(DELIVERY_NOTICE_GENERATION_FILTER);
        vect.addElement(ACCOUNTING_SECURITY_FILTER);
        vect.addElement(SANT_SETTLEMENT_SEC);
        vect.addElement(SANT_IS_TRIPARTY);
        vect.addElement(SANT_TRIPARTY_AGENT);
        vect.addElement(TRANSFER_SEC_TYPE);
        vect.addElement(EXPORT_BOND_DATE_ADDFIELD);

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.refdata.StaticDataFilterInterface#fillTreeList(com.calypso
     * .tk.service.DSConnection, com.calypso.apps.util.TreeList)
     */
    @Override
    public boolean fillTreeList(DSConnection ds, TreeList treeList) {

        Vector nodes = new Vector();
        nodes.addElement("SantManrginCall");

        treeList.add(nodes, IS_RECOUPONING);
        treeList.add(nodes, EMIR_DIRECTION);
        treeList.add(nodes, IS_SEND_STATEMENT);
        treeList.add(nodes, IS_PO_VALUATION_AGENT);
        treeList.add(nodes, IS_PORTFOLIO_NOTIF_AUTO);
        treeList.add(nodes, IS_VALIDATION_AUTO);
        treeList.add(nodes, MESSAGE_EVENT_TYPE);
        treeList.add(nodes, MC_CONTRACT_TYPE);
        treeList.add(nodes, SANT_MRG_CALL_ID);
        treeList.add(nodes, LAST_STATUS_BEFORE_SUBSTITUTION_ATTR);
        treeList.add(nodes, LAST_ACTION);
        treeList.add(nodes, NB_PENDING_ALLOCS);
        treeList.add(nodes, IS_COLLATERALIZABLE_TRADE);
        treeList.add(nodes, IS_SEND_EMIR);
        // START OA Filter messages to send to K+ 05/11/2013
        // BAU 5.2.0 - Add new filters in order to segregate security in bond &
        // equity.
        treeList.add(nodes, MC_BLOCK_MESSAGE_PAY_CASH);
        treeList.add(nodes, MC_BLOCK_MESSAGE_PAY_SECURITIES);
        treeList.add(nodes, MC_BLOCK_MESSAGE_PAY_SECURITIES_EQTY);
        treeList.add(nodes, MC_BLOCK_MESSAGE_RECEIVE_CASH);
        treeList.add(nodes, MC_BLOCK_MESSAGE_RECEIVE_SECURITIES);
        treeList.add(nodes, MC_BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY);
        // END OA Filter messages to send to K+ 05/11/2013
        // START eLab - Calypso Col - Filters - 07/06/2016
        treeList.add(nodes, SANT_MRG_CALL_CONTRACT_TYPE);
        treeList.add(nodes, SANT_MRG_CALL_CSD_TYPE);
        // END eLab - Calypso Col - Filters - 07/06/2016
        treeList.add(nodes, SANT_THIRDPARTY_SWIFT);
        treeList.add(nodes, MC_SLB_CIRCUIT_PO);

        //Call Account
        treeList.add(nodes, STM_FILTER);
        treeList.add(nodes, SETTLEMENT_FILTER);
        treeList.add(nodes, ACCOUNTING_FILTER);
        treeList.add(nodes, MEDUSA_FILTER);
        treeList.add(nodes, CALL_ACCOUNT_CIRCUIT_FILTER);
        treeList.add(nodes, CALL_ACCOUNT_CIRCUIT_DATE_FILTER);

        treeList.add(nodes, ACCOUNTING_DATE_FILTER);
        treeList.add(nodes, MEDUSA_DATE_FILTER);
        treeList.add(nodes, SETTLEMENT_DATE_FILTER);
        treeList.add(nodes,SANT_SL_MIGRATION_DATE);

        treeList.add(nodes, TODAY);
        treeList.add(nodes, PROCESS_DATE);
        treeList.add(nodes,DELIVERY_NOTICE_GENERATION_FILTER);
        treeList.add(nodes,ACCOUNTING_SECURITY_FILTER);
        treeList.add(nodes,SANT_SETTLEMENT_SEC);
        treeList.add(nodes,SANT_IS_TRIPARTY);
        treeList.add(nodes,SANT_TRIPARTY_AGENT);
        treeList.add(nodes,TRANSFER_SEC_TYPE);
        treeList.add(nodes,EXPORT_BOND_DATE_ADDFIELD);
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.refdata.StaticDataFilterInterface#getTypeDomain(java.lang
     * .String)
     */
    @Override
    public Vector getTypeDomain(String name) {
        Vector<String> vect = new Vector<String>();
        if (name.equals(IS_RECOUPONING)) {
            vect.addElement(StaticDataFilterElement.S_IS);
        } else if (name.equals(EMIR_DIRECTION)) {
            vect.addElement(StaticDataFilterElement.S_LIKE);
        } else if (name.equals(IS_SEND_STATEMENT)) {
            vect.addElement(StaticDataFilterElement.S_IS);
        } else if (name.equals(IS_PO_VALUATION_AGENT)) {
            vect.addElement(StaticDataFilterElement.S_IS);
        } else if (name.equals(IS_PORTFOLIO_NOTIF_AUTO)) {
            vect.addElement(StaticDataFilterElement.S_IS);
        } else if (name.equals(IS_COLLATERALIZABLE_TRADE)) {
            vect.addElement(StaticDataFilterElement.S_IS);
        } else if (name.equals(IS_VALIDATION_AUTO)) {
            vect.addElement(StaticDataFilterElement.S_IS);
        } else if (name.equals(MESSAGE_EVENT_TYPE)) {
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
        } else if (name.equals(MC_CONTRACT_TYPE)) {
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
        } else if (name.equals(SANT_MRG_CALL_ID)) {
            vect.addElement(StaticDataFilterElement.S_INT_ENUM);
            vect.addElement(StaticDataFilterElement.S_NOT_IN_INT_ENUM);
        } else if (name.equals(LAST_STATUS_BEFORE_SUBSTITUTION_ATTR)) {
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
        } else if (name.equals(LAST_ACTION)) {
                vect.addElement(StaticDataFilterElement.S_IN);
                vect.addElement(StaticDataFilterElement.S_NOT_IN);                
        } else if (name.equals(NB_PENDING_ALLOCS)) {
            vect.addElement(StaticDataFilterElement.S_INT_ENUM);
            vect.addElement(StaticDataFilterElement.S_NOT_IN_INT_ENUM);
            // BAU 5.4 - Add new attribute "IS_SEND_EMIR". This attibute show in
            // Collateral Manager
            // the contracts must be sended to SEND_EMIR
        } else if (name.equals(IS_SEND_EMIR)) {
            vect.addElement(StaticDataFilterElement.S_IN);
        }
        // START OA Filter messages to send to K+ 05/11/2013
        // BAU 5.2.0 - Add new filters in order to segregate security in bond &
        // equity.
        else if (name.equals(MC_BLOCK_MESSAGE_PAY_CASH)
                || name.equals(MC_BLOCK_MESSAGE_PAY_SECURITIES)
                || name.equals(MC_BLOCK_MESSAGE_RECEIVE_CASH)
                || name.equals(MC_BLOCK_MESSAGE_RECEIVE_SECURITIES)
                || name.equals(MC_BLOCK_MESSAGE_PAY_SECURITIES_EQTY)
                || name.equals(MC_BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY)
                || name.equals(MC_SLB_CIRCUIT_PO)
                || name.equals(ACCOUNTING_SECURITY_FILTER)) {
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
        }

        // END OA Filter messages to send to K+ 05/11/2013
        // START eLab - Calypso Col - Filters - 07/06/2016
        else if (name.equals(SANT_MRG_CALL_CONTRACT_TYPE)) {
            vect.addElement(StaticDataFilterElement.S_IN);
        } else if (name.equals(SANT_MRG_CALL_CSD_TYPE)) {
            vect.addElement(StaticDataFilterElement.S_IN);
        }
        // END eLab - Calypso Col - Filters - 07/06/2016
        else if (name.equals(SANT_THIRDPARTY_SWIFT)) {
            vect.addElement(StaticDataFilterElement.S_IN);
        }else if (name.equals(DELIVERY_NOTICE_GENERATION_FILTER)) {
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
        } else if (name.equals(STM_FILTER) //Call Account
                || name.equals(SETTLEMENT_FILTER)
                || name.equals(ACCOUNTING_FILTER)
                || name.equals(MEDUSA_FILTER)
                || name.equals(CALL_ACCOUNT_CIRCUIT_FILTER)) {
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
        } else if (name.equals(CALL_ACCOUNT_CIRCUIT_DATE_FILTER)
                || name.equals(MEDUSA_DATE_FILTER)
                || name.equals(SETTLEMENT_DATE_FILTER)
                || name.equals(ACCOUNTING_DATE_FILTER)
                || name.equals(SANT_SETTLEMENT_SEC)
                || name.equals(SANT_SL_MIGRATION_DATE)) {
            vect.addElement(StaticDataFilterElement.S_DATE_COMPARISON);
            vect.addElement(StaticDataFilterElement.S_IS_NULL);
            vect.addElement(StaticDataFilterElement.S_IS_NOT_NULL);
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
        }else if(name.equalsIgnoreCase(SANT_IS_TRIPARTY)){
            vect.addElement(StaticDataFilterElement.S_IS);
        }else if(name.equalsIgnoreCase(SANT_TRIPARTY_AGENT)){
            vect.addElement(StaticDataFilterElement.S_IS_NULL);
            vect.addElement(StaticDataFilterElement.S_IS_NOT_NULL);
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
            vect.addElement(StaticDataFilterElement.S_LIKE);
        }else if(name.equalsIgnoreCase(TRANSFER_SEC_TYPE)){
            vect.addElement(StaticDataFilterElement.S_IN);
            vect.addElement(StaticDataFilterElement.S_NOT_IN);
            vect.addElement(StaticDataFilterElement.S_LIKE);
        }else if(name.equalsIgnoreCase(EXPORT_BOND_DATE_ADDFIELD)){
            vect.addElement(StaticDataFilterElement.S_LIKE);
        }

        return vect;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.refdata.StaticDataFilterInterface#getDomain(com.calypso
     * .tk.service.DSConnection, java.lang.String)
     */
    @Override
    public Vector getDomain(DSConnection dsCon, String domain) {
        Vector vect = new Vector();
        if (MESSAGE_EVENT_TYPE.equals(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "eventType");
        } else if (MC_CONTRACT_TYPE.equals(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "legalAgreementType");
        } else if (LAST_STATUS_BEFORE_SUBSTITUTION_ATTR.equals(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "CollateralStatus");
        }else if (LAST_ACTION.equals(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "CollateralAction");
        }
        // START OA Filter messages to send to K+ 05/11/2013
        // BAU 5.2.0 - Add new filters in order to segregate security in bond &
        // equity.
        else if (domain.equals(MC_BLOCK_MESSAGE_PAY_CASH)
                || domain.equals(MC_BLOCK_MESSAGE_PAY_SECURITIES)
                || domain.equals(MC_BLOCK_MESSAGE_RECEIVE_CASH)
                || domain.equals(MC_BLOCK_MESSAGE_RECEIVE_SECURITIES)
                || domain.equals(MC_BLOCK_MESSAGE_PAY_SECURITIES_EQTY)
                || domain.equals(MC_BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY)) {
            vect = new Vector(CollateralConfig.getAttributeValues(domain.substring(5)));
        }
        // END OA Filter messages to send to K+ 05/11/2013
        // START eLab - Calypso Col - Filters - 07/06/2016
        else if (SANT_MRG_CALL_CONTRACT_TYPE.equals(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "legalAgreementType");
        } else if (SANT_MRG_CALL_CSD_TYPE.equals(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.IM_CSD_TYPE");
        }
        // END eLab - Calypso Col - Filters - 07/06/2016
        else if (SANT_THIRDPARTY_SWIFT.equals(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.THIRDPARTY_SWIFT");
        } else if (MC_SLB_CIRCUIT_PO.equalsIgnoreCase(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.SLB_CIRCUIT_PO");
        } else if (STM_FILTER.equalsIgnoreCase(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.STM");
        } else if (SETTLEMENT_FILTER.equalsIgnoreCase(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.SETTLEMENT");
        } else if (ACCOUNTING_FILTER.equalsIgnoreCase(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.ACCOUNTING");
        } else if (MEDUSA_FILTER.equalsIgnoreCase(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.MEDUSA");
        } else if (CALL_ACCOUNT_CIRCUIT_FILTER.equalsIgnoreCase(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.NEW_CALL_ACCOUNT_CIRCUIT");
        }else if (DELIVERY_NOTICE_GENERATION_FILTER.equalsIgnoreCase(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.DELIVERY_NOTICE_GENERATION");
        } else if (CALL_ACCOUNT_CIRCUIT_DATE_FILTER.equalsIgnoreCase(domain)
                || MEDUSA_DATE_FILTER.equalsIgnoreCase(domain)
                || SETTLEMENT_DATE_FILTER.equalsIgnoreCase(domain)
                || ACCOUNTING_DATE_FILTER.equalsIgnoreCase(domain)
                || SANT_SETTLEMENT_SEC.equalsIgnoreCase(domain)
                || SANT_SL_MIGRATION_DATE.equalsIgnoreCase(domain)) {
            vect = LocalCache.getDomainValues(dsCon, "Accounting_Dates_Filter");
        }else if(ACCOUNTING_SECURITY_FILTER.equalsIgnoreCase(domain)){
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.ACCOUNTING_SECURITY");
        }else if(SANT_TRIPARTY_AGENT.equalsIgnoreCase(domain)){
            vect = LocalCache.getDomainValues(dsCon, "mccAdditionalField.TRIPARTY_AGENT");
        }else if(TRANSFER_SEC_TYPE.equalsIgnoreCase(domain)){
            return LocalCache.getDomainValues(dsCon, "productType");
        }

        return vect;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.refdata.StaticDataFilterInterface#getValue(com.calypso
     * .tk.core.Trade, com.calypso.tk.core.LegalEntity, java.lang.String,
     * com.calypso.tk.core.Product, com.calypso.tk.bo.BOTransfer,
     * com.calypso.tk.bo.BOMessage, com.calypso.tk.bo.TradeTransferRule,
     * com.calypso.tk.report.ReportRow, com.calypso.tk.bo.Task,
     * com.calypso.tk.refdata.Account, com.calypso.tk.core.CashFlow,
     * com.calypso.tk.core.HedgeRelationship, java.lang.String,
     * com.calypso.tk.refdata.StaticDataFilterElement)
     */
    @SuppressWarnings({"static-access", "deprecation"})
    @Override
    public Object getValue(
            Trade trade,
            LegalEntity le,
            String role,
            Product product,
            BOTransfer transfer,
            BOMessage message,
            TradeTransferRule rule,
            ReportRow reportRow,
            Task task,
            Account glAccount,
            CashFlow cashflow,
            HedgeRelationship relationship,
            String filterElement,
            StaticDataFilterElement element) {
        CollateralConfig mcc = null;
        MarginCallEntry entry = null;
        if (reportRow != null) {
            Object defaultObject = reportRow.getProperty("Default");
            if (defaultObject instanceof MarginCallEntry) {
                entry = (MarginCallEntry) defaultObject;
            }
            defaultObject = reportRow.getProperty("MarginCallConfig");
            if (defaultObject instanceof CollateralConfig) {
                mcc = (CollateralConfig) defaultObject;
            }
            defaultObject = reportRow.getProperty("MarginCallEntry");
            if (mcc == null && defaultObject instanceof MarginCallEntry) {
                entry = (MarginCallEntry) defaultObject;
                mcc = entry.getCollateralConfig();
            }
        }

        if (message != null) {
            try {
                mcc =
                        CacheCollateralClient.getInstance()
                                .getCollateralConfig(DSConnection.getDefault(), message.getStatementId());

                String mceId = message.getAttribute("marginCallEntryId");
                if (!Util.isEmpty(mceId) && (mcc != null)) {
                    MarginCallEntryDTO entryDTO =
                            ServiceRegistry.getDefault().getCollateralServer().loadEntry(Integer.valueOf(mceId));
                    if (entryDTO != null) {
                        entry = SantMarginCallUtil.getMarginCallEntry(entryDTO, mcc, false);
                    }
                }
            } catch (RemoteException e) {
                Log.error(this, e);
                mcc = null;
            }
        }

        if ((mcc == null) && (trade != null) && (trade.getProduct() instanceof MarginCall)) {
            mcc =
                    CacheCollateralClient.getCollateralConfig(
                            DSConnection.getDefault(), ((MarginCall) trade.getProduct()).getMarginCallId());
        }


        if ((mcc == null) && (trade != null) && ((trade.getProduct() instanceof InterestBearing) || (trade.getProduct() instanceof CustomerTransfer))) {
            mcc = CacheCollateralClient.getCollateralConfig(
                    DSConnection.getDefault(), trade.getKeywordAsInt(MC_CONTRACT_NUMBER));
        }

        if (java.util.Objects.isNull(mcc)) {
            mcc = BOTransferUtil.getCollateralConfig(Optional.ofNullable(transfer));
        }
        // else {
        // filterElement = StaticDataFilterElement.TRADE_MARGIN_CALL_CONTRACT;
        // }

        // get the margin call config
        if (filterElement.equals(IS_RECOUPONING)) {
            String isRecoup =
                    (String)
                            super.getValue(
                                    trade,
                                    le,
                                    role,
                                    product,
                                    transfer,
                                    message,
                                    rule,
                                    reportRow,
                                    task,
                                    glAccount,
                                    cashflow,
                                    relationship,
                                    "ADDITIONAL_FIELD.MC_RECOUPONING",
                                    element);

            // return Boolean.parseBoolean(isRecoup);
            return "YES".equals(isRecoup);
        } else if (filterElement.equals(EMIR_DIRECTION)) {
            String emirDirection =
                    (String)
                            super.getValue(
                                    trade,
                                    le,
                                    role,
                                    product,
                                    transfer,
                                    message,
                                    rule,
                                    reportRow,
                                    task,
                                    glAccount,
                                    cashflow,
                                    relationship,
                                    "ADDITIONAL_FIELD.EMIR_DIRECTION",
                                    element);

            if ("BILATERAL".equals(emirDirection)) {
                return "BILATERAL";
            } else {
                return "UNILATERAL";
            }
        } else if (filterElement.equals(IS_SEND_STATEMENT)) {
            if (mcc != null) {
                return mcc.sendStatement();
            }
        } else if (filterElement.equals(IS_PO_VALUATION_AGENT)) {
            if (mcc != null) {
                return (CollateralConfig.PARTY_A.equals(mcc.getValuationAgentType())
                        || CollateralConfig.BOTH.equals(mcc.getValuationAgentType()));
            }
        } else if (filterElement.equals(IS_PORTFOLIO_NOTIF_AUTO)) {
            String isPrtfAuto =
                    (String)
                            super.getValue(
                                    trade,
                                    le,
                                    role,
                                    product,
                                    transfer,
                                    message,
                                    rule,
                                    reportRow,
                                    task,
                                    glAccount,
                                    cashflow,
                                    relationship,
                                    "ADDITIONAL_FIELD.MC_PORTFOLIO_NOTIF_AUTO",
                                    element);
            return "YES".equals(isPrtfAuto);
            // return Boolean.parseBoolean(isPrtfAuto);

        } else if (filterElement.equals(IS_COLLATERALIZABLE_TRADE)) {
            ContextAttribute unColTrades = null;
            if (DSConnection.getDefault().isServer()) {
                CollateralContext ctx = null;
                try {
                    ctx = ServiceRegistry.getDefault().getCollateralDataServer().loadDefaultContext();
                } catch (RemoteException e) {
                    Log.error(this, e);
                }
                if (ctx != null) {
                    unColTrades = ctx.getAttribute("UNCOLLATERALIZABLE_TRADES");
                }

            } else {
                unColTrades = ServiceRegistry.getDefaultContext().getAttribute("UNCOLLATERALIZABLE_TRADES");
            }
            String unColTradesList = null;
            if (unColTrades != null) {
                unColTradesList = unColTrades.getValue();
            }

            if (Util.isEmpty(unColTradesList)) {
                return true;
            }

            if (trade == null) {
                return true;
            }
            
            if (Util.isEmpty(trade.getProductSubType())) {
            	return true;
            }
            
            return !unColTradesList.contains(trade.getProductSubType());

        } else if (filterElement.equals(IS_VALIDATION_AUTO)) {
            String isValidationAuto =
                    (String)
                            super.getValue(
                                    trade,
                                    le,
                                    role,
                                    product,
                                    transfer,
                                    message,
                                    rule,
                                    reportRow,
                                    task,
                                    glAccount,
                                    cashflow,
                                    relationship,
                                    "ADDITIONAL_FIELD.MC_VALIDATION",
                                    element);
            // return Boolean.parseBoolean(isValidationAuto);
            return "AUTOMATICO".equals(isValidationAuto);
        } else if (filterElement.equals(MESSAGE_EVENT_TYPE)) {
            if (message != null) {
                return message.getEventType();
            } else {
                return "";
            }
        } else if (filterElement.equals(MC_CONTRACT_TYPE)) {
            if (mcc != null) {
                return mcc.getContractType();
            } else {
                return "";
            }
        } else if (filterElement.equals(SANT_MRG_CALL_ID)) {
            if (mcc != null) {
                return mcc.getId();
            }
            if (trade != null) {
                return trade.getKeywordValue(TRADE_KYW_MRG_CALL_CTR_ID);
            }

            return "";
        } else if (filterElement.equals(LAST_STATUS_BEFORE_SUBSTITUTION_ATTR)) {
            if (entry != null) {
                return entry.getAttribute("LAST_STATUS_BEFORE_SUBSTITUTION");
            }
        } else if (filterElement.equals(LAST_ACTION)) {
                if (entry != null) {
                    return entry.getEntityState().getAction().toString();
                }
        } else if (filterElement.equals(NB_PENDING_ALLOCS)) {
            if (entry != null) {
                List<MarginCallAllocation> allocs = entry.getPendingMarginAllocations();
                return (Util.isEmpty(allocs) ? 0 : allocs.size());
            }
        } else if (filterElement.equals(IS_SEND_EMIR)) {
            if (entry != null) {
                return entry.getAttribute("IS_SEND_EMIR");
            }
        }
        // START OA Filter messages to send to K+ 05/11/2013
        // BAU 5.2.0 - Add new filters in order to segregate security in bond &
        // equity.
        else if (filterElement.equals(MC_BLOCK_MESSAGE_PAY_CASH)
                || filterElement.equals(MC_BLOCK_MESSAGE_PAY_SECURITIES)
                || filterElement.equals(MC_BLOCK_MESSAGE_RECEIVE_CASH)
                || filterElement.equals(MC_BLOCK_MESSAGE_RECEIVE_SECURITIES)
                || filterElement.equals(MC_BLOCK_MESSAGE_PAY_SECURITIES_EQTY)
                || filterElement.equals(MC_BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY)
                || filterElement.equals(MC_SLB_CIRCUIT_PO)) {
            if (mcc != null) {
                return mcc.getAdditionalField(filterElement.substring(5));
            }
        }
        // END OA Filter messages to send to K+ 05/11/2013
        // START eLab - Calypso Col - Filters - 07/06/2016
        else if (filterElement.equals(SANT_MRG_CALL_CONTRACT_TYPE)) {
           return getMcContractType(transfer,mcc);
        } else if (filterElement.equals(SANT_MRG_CALL_CSD_TYPE)) {
            return getCSDType(transfer,mcc);
        } else if (filterElement.equals(SANT_THIRDPARTY_SWIFT)) {
            if (mcc != null) {
                return mcc.getAdditionalField(THIRDPARTY_SWIFT);
            } else {
                return "";
            }
        } else if (filterElement.equals(STM_FILTER)) {
            return getAdditionalField(mcc, STM);
        } else if (filterElement.equals(SETTLEMENT_FILTER)) {
            return getAdditionalField(mcc, SETTLEMENT);
        } else if (filterElement.equals(ACCOUNTING_FILTER)) {
            return getAdditionalField(mcc, ACCOUNTING);
        } else if (filterElement.equals(MEDUSA_FILTER)) {
            return getAdditionalField(mcc, MEDUSA);
        } else if (filterElement.equals(CALL_ACCOUNT_CIRCUIT_FILTER)) {
            return getAdditionalField(mcc, CALL_ACCOUNT_CIRCUIT);
        } else if (filterElement.equals(CALL_ACCOUNT_CIRCUIT_DATE_FILTER)) {
            String additionalField = (String) getAdditionalField(mcc, NEW_CALL_ACCOUNT_CIRCUIT_DATE);
            return JDate.valueOf(additionalField);
        } else if (filterElement.equals(MEDUSA_DATE_FILTER)) {
            String additionalField = (String) getAdditionalField(mcc, MEDUSA);
            return JDate.valueOf(additionalField);
        } else if (filterElement.equals(SETTLEMENT_DATE_FILTER)) {
            String additionalField = (String) getAdditionalField(mcc, SETTLEMENT);
            return JDate.valueOf(additionalField);
        } else if (filterElement.equals(ACCOUNTING_DATE_FILTER)) {
            String additionalField = (String) getAdditionalField(mcc, ACCOUNTING);
            return JDate.valueOf(additionalField);
        } else if (filterElement.equals(SANT_SL_MIGRATION_DATE)) {
            if(null!=trade){
                return trade.getKeywordAsJDate("SL_MIG");
            }
            return null;
        } else if (filterElement.equals(TODAY)) {
            return JDate.getNow();
        } else if (filterElement.equals(PROCESS_DATE)) { //TODO
            return JDate.getNow();
        }else if (filterElement.equals(DELIVERY_NOTICE_GENERATION_FILTER)) {
            return getAdditionalField(mcc, DELIVERY_NOTICE_GENERATION);
        }else if (filterElement.equals(ACCOUNTING_SECURITY_FILTER)) {
            return getAdditionalField(mcc, ACCOUNTING_SECURITY);
        }else if (filterElement.equals(SANT_SETTLEMENT_SEC)) {
            String additionalField = (String) getAdditionalField(mcc, SETTLEMENT_SEC);
            return JDate.valueOf(additionalField);
        }else if (filterElement.equals(SANT_IS_TRIPARTY)) {
            return isTripartyContract(mcc);
        }else if (filterElement.equals(SANT_TRIPARTY_AGENT)) {
            return null!=mcc ? mcc.getTripartyAgent() : "";
        }else if (filterElement.equals(TRANSFER_SEC_TYPE)){
            return getUndTransferSecurityType(transfer);
        }else if(filterElement.equals(EXPORT_BOND_DATE_ADDFIELD)){
            return getExportBondDateAddField(transfer,mcc);
        }
        // END eLab - Calypso Col - Filters - 07/06/2016

        return false;
    }

    private Object getAdditionalField(CollateralConfig mcc, String additionalField) {
        if (mcc != null) {
            return mcc.getAdditionalField(additionalField);
        } else {
            return "";
        }
    }

    private Boolean isTripartyContract(CollateralConfig mcc) {
        return null!=mcc && mcc.isTriParty();
    }


    private CollateralConfig getCollateralConfigInNet(BOTransfer transfer, CollateralConfig mcc){
        if(mcc==null&&transfer!=null&&transfer.getTradeLongId()==0) {
            BOTransfer undTransfer =new FIFlowTransferNetHandler(transfer).getFirstUndTransfer();
            mcc=BOTransferUtil.getCollateralConfig(Optional.ofNullable(undTransfer));
        }
        return mcc;
    }

    private String getMcContractType(BOTransfer transfer, CollateralConfig mcc){
        String contractType="";
        mcc=getCollateralConfigInNet(transfer,mcc);
        if (mcc != null) {
            contractType=mcc.getContractType();
        }
        return contractType;
    }

    private String getExportBondDateAddField(BOTransfer transfer, CollateralConfig mcc){
        String exportBondDate="";
        mcc=getCollateralConfigInNet(transfer,mcc);
        if (mcc != null) {
            exportBondDate=mcc.getAdditionalField("POSITION_EXPORT_BOND_DATE");
        }
        return exportBondDate;
    }
    private String getCSDType(BOTransfer transfer, CollateralConfig mcc){
        String csdType="";
        mcc=getCollateralConfigInNet(transfer,mcc);
        if (mcc != null) {
            csdType=mcc.getAdditionalField(IM_CSD_TYPE);
        }
        return csdType;
    }

    private String getUndTransferSecurityType(BOTransfer transfer){
        String secType="";
        if(transfer!=null){
            Product product=getProductFromTransfer(transfer.getProductId());
            if(product!=null){
                secType=product.getType();
            }
        }
        return secType;
    }

    private Product getProductFromTransfer(int productId){
        Product product=null;
        try {
            product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), productId);
            if (product == null) {
                product = DSConnection.getDefault().getRemoteProduct().getProduct(productId);
            }
            if (product != null) {
                product.condenseFlows();
            }
        } catch (CalypsoServiceException exc) {
            Log.error(SantMarginCallStaticDataFilter.class.getSimpleName(), "Could not get product " + productId, exc);
        }
        return product;
    }
    /*
     * (non-Javadoc)
     *
     * @see
     * com.calypso.tk.refdata.StaticDataFilterInterface#isTradeNeeded(java.lang
     * .String)
     */
    @Override
    public boolean isTradeNeeded(String arg0) {
        return false;
    }
}

