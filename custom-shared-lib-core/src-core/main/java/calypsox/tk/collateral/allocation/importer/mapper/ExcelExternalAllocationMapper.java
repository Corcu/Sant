/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.collateral.allocation.importer.mapper;

import calypsox.tk.collateral.allocation.importer.CashExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalTripartyBean;
import calypsox.tk.collateral.allocation.importer.SecurityExternalAllocationBean;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CashAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.SecurityAllocation;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author aela
 */
public class ExcelExternalAllocationMapper extends AbstractExternalAllocationMapper {

    public ExcelExternalAllocationMapper(MarginCallEntry entry, JDate processingDate) {
        super(entry, processingDate);
    }

    @Override
    protected CashAllocation mapCashAllocation(CashExternalAllocationBean allocBean, List<String> messages) {
            CollateralConfig mcc = allocBean.getMcc();
        DSConnection dsCon = DSConnection.getDefault();
        MarginCall mc = new MarginCall();
        mc.setCurrencyCash(allocBean.getCurrency());

        Trade mcTrade = new Trade();
        mcTrade.setProduct(mc);
        mcTrade.setQuantity(allocBean.getNominal());
        mc.setPrincipal(allocBean.getNominal());
        mcTrade.setTraderName(dsCon.getUser());
        mcTrade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
        mcTrade.setTradeCurrency(allocBean.getCurrency());
        mcTrade.setSettleCurrency(allocBean.getCurrency());
        mcTrade.setBook(mcc.getBook((int)allocBean.getNominal(),true));
        // set trade and product dates
        JDate settlementDate = allocBean.getSettlementDate() != null ? JDate.valueOf(allocBean.getSettlementDate())
                : this.getEntry().getProcessDate();
        if (settlementDate != null) {
            mcTrade.setSettleDate(settlementDate);
        } else {
            mcTrade.setSettleDate(JDate.getNow());
        }

        if (this.getEntry() != null) {
            mcTrade.setTradeDate(this.getEntry().getProcessDate().getJDatetime(TimeZone.getDefault()));
        }


        CashAllocation cashAlloc = null;
        try {
            cashAlloc = getAllocationFactory().createCashMargin(mcTrade);
            // MIG_V14
            // cashAlloc.setDirection(getDirection(mcTrade));

            if (cashAlloc != null) {
                cashAlloc.addAttribute("Father ID", allocBean.getFatherId());
                if (allocBean.getSettlementDate() != null) {
                    cashAlloc.setSettlementDate(JDate.valueOf(allocBean.getSettlementDate()));
                }
            }
        } catch (MarketDataException e) {
            messages.add(e.getMessage());
            Log.error(this, e);
        } catch (PricerException e) {
            messages.add(e.getMessage());
            Log.error(this, e);
        }
        return cashAlloc;
    }

    @Override
    protected SecurityAllocation mapSecurityAllocation(SecurityExternalAllocationBean allocBean,
                                                       List<String> messages) {
        // get the allocation security
        CollateralConfig mcc = allocBean.getMcc();
        DSConnection dsCon = DSConnection.getDefault();
        MarginCall mc = new MarginCall();
        mc.setSecurity(allocBean.getSecurity());

        Trade mcTrade = new Trade();
        mcTrade.setProduct(mc);
        mcTrade.setQuantity(allocBean.getNominal() / allocBean.getSecurity().getPrincipal(getEntry().getProcessDate()));
        mcTrade.setTraderName(dsCon.getUser());
        mcTrade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
        mcTrade.setTradeCurrency(mcc.getCurrency());
        mcTrade.setSettleCurrency(mcc.getCurrency());
            mcTrade.setBook(mcc.getBook((int)allocBean.getNominal(),false));
        // mcTrade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER,
        // mcc.getId());
        // mcTrade.addKeyword(TradeInterfaceUtils.TRANS_TRADE_KWD_MTM_DATE,
        // Util.dateToString(this.entry.getValueDate()));
        // set trade and product dates
        JDate settlementDate = allocBean.getSettlementDate() != null ? JDate.valueOf(allocBean.getSettlementDate())
                : this.getEntry().getProcessDate();
        if (settlementDate != null) {
            mcTrade.setSettleDate(settlementDate);
        } else {
            mcTrade.setSettleDate(JDate.getNow());
        }

        if (this.getEntry() != null) {
            mcTrade.setTradeDate(this.getEntry().getProcessDate().getJDatetime(TimeZone.getDefault()));
        }

        SecurityAllocation allocSec = null;
        try {
            allocSec = getAllocationFactory().createSecurityMargin(mcTrade);
            // DNF 25/08/2017 - Check ISIN price calculation. Incidence importer.
            List<String> errorMessages = getAllocationFactory().getPricer().getMessages();
            StringBuffer additionalText = new StringBuffer();
            if (errorMessages != null && !errorMessages.isEmpty()) {
                additionalText.append("Error trying to make pricing allocation for contract ");
                additionalText.append(allocBean.getContractID());
                additionalText.append(" and ISIN: ");
                additionalText.append(allocBean.getIsin());
                messages.add(additionalText.toString());
                for (String errormessage : errorMessages) {
                    messages.add(errormessage);
                }
                allocSec = null;
                messages.add(
                        "Allocation will not be imported. Please, review and correct this issue and try it newly or contact with your application administrator.");
                messages.add("");
            }

            if (allocSec != null) {
                allocSec.addAttribute("Father ID", allocBean.getFatherId());
                if (allocBean.getSettlementDate() != null) {
                    allocSec.setSettlementDate(JDate.valueOf(allocBean.getSettlementDate()));
                }
            }
        } catch (MarketDataException e) {
            messages.add(e.getMessage());
            Log.error(this, e);
        } catch (PricerException e) {
            messages.add(e.getMessage());
            Log.error(this, e);
        }
        return allocSec;
    }

    @Override
    protected boolean isValidCashAllocation(CashExternalAllocationBean allocBean, List<String> messages) {
        // check the CollateralConfig
        // boolean isValid = checkMarginCallConfig(allocBean, messages);
        boolean isValid = checkCollateralConfig(allocBean, messages);

        // check the PO
        isValid = isValid && checkProcessingOrg(allocBean, messages);

        // check the pricing Env
        isValid = isValid && checkPricingEnv(allocBean, messages);

        // check currency
        isValid = isValid && checkCurrency(allocBean, messages);

        // check nominal
        isValid = isValid && checkNominal(allocBean, messages);

        // DNF - 05/09/2017 - Disable check for eligible currencies in the
        // multi-contract importer.
        // check currency
        // isValid = isValid && checkEligibleCurrency(allocBean, messages);

        // check that the status of allocation
        isValid = isValid && checkMarginCallStatus(allocBean, messages);

        return isValid;
    }

    public boolean checkEligibleCurrency(CashExternalAllocationBean allocBean, List<String> messages) {
        boolean isEligibleCurrency = false;
        List<CollateralConfigCurrency> currencies = allocBean.getMcc().getEligibleCurrencies();
        if (!Util.isEmpty(currencies)) {
            for (CollateralConfigCurrency ccy : currencies) {
                if (ccy.getCurrency().equals(allocBean.getCurrency())) {
                    isEligibleCurrency = true;
                    break;
                }
            }
        }
        if (!isEligibleCurrency) {
            messages.add("row " + allocBean.getRowNumber() + " : Currency " + allocBean.getCurrency()
                    + " is not eligible to be used with the contract " + allocBean.getMcc().getName());

        }
        return isEligibleCurrency;
    }

    private boolean checkCurrency(CashExternalAllocationBean allocBean, List<String> messages) {
        CurrencyDefault cd = LocalCache.getCurrencyDefault(allocBean.getCurrency());
        if (cd == null) {
            messages.add("row " + allocBean.getRowNumber() + " : Currency " + allocBean.getCurrency()
                    + " is not valid or not setup in the system");
            return false;
        }
        return true;
    }

    private boolean checkCurrencyTAA(ExternalTripartyBean taa, List<String> messages) {
        CurrencyDefault cd = LocalCache.getCurrencyDefault(taa.getCurrency());
        if (cd == null) {
            messages.add("row " + taa.getRowNumber() + " : Currency " + taa.getCurrency()
                    + " is not valid or not setup in the system");
            return false;
        }
        return true;
    }

    @Override
    protected boolean isValidSecurityAllocation(SecurityExternalAllocationBean allocBean, List<String> messages) {
        // check the CollateralConfig
        // boolean isValid = checkMarginCallConfig(allocBean, messages);
        boolean isValid = checkCollateralConfig(allocBean, messages);

        // check the PO
        isValid = isValid && checkProcessingOrg(allocBean, messages);

        // check the pricing Env
        isValid = isValid && checkPricingEnv(allocBean, messages);

        // check the ISIN
        isValid = isValid && checkISIN(allocBean, messages);

        // checks Security Mat Date
        isValid = isValid && checkSecurityMaturityDate(allocBean, messages);
        // check nominal
        isValid = isValid && checkNominal(allocBean, messages);

        // DNF - 05/09/2017 - Disable check for security ISIN codes for the the
        // multi-contract importer.
        // check that security is accepted by the contract
        // isValid = isValid && checkEligibleSecurity(allocBean, messages);

        // check that the status of allocation
        isValid = isValid && checkMarginCallStatus(allocBean, messages);

        return isValid;
    }

    private boolean checkSecurityMaturityDate(SecurityExternalAllocationBean allocBean, List<String> messages) {
        Product bond = allocBean.getSecurity();
        boolean res = true;
        if (bond instanceof Bond) {
            Date settleDate = allocBean.getSettlementDate();
            if (settleDate != null && bond.getMaturityDate() != null) {
                res = allocBean.getSettlementDate().before(bond.getMaturityDate().getDate());
            }
        }
        if (!res) {
            messages.add("row " + allocBean.getRowNumber() + " : Security with ISIN "
                    + (Util.isEmpty(allocBean.getIsin()) ? "" : allocBean.getIsin()) + " is matured at the selected " +
                    "SettleDate");
        }
        return res;
    }

    @Override
    protected boolean isValidTAA(ExternalTripartyBean taa, List<String> messages) {
        boolean isValid = checkCollateralConfigTaa(taa, messages);
        // check currency
        isValid = isValid && checkCurrencyTAA(taa, messages);

        isValid = isValid && checkValidCurrencyTAA(taa, messages);

        return isValid;
    }

    private boolean checkValidCurrencyTAA(ExternalTripartyBean taa, List<String> messages) {

        MarginCallEntry entry = taa.getEntry();
        String baseCCY = entry.getBaseCurrency();

        if (taa.getCurrency().equals(baseCCY)) {
            return true;
        }
        messages.add("Currency " + taa.getCurrency() + " not valid for entry in row " + taa.getRowNumber()
                + " with currency " + baseCCY);
        return false;
    }

    private boolean checkEligibleSecurity(SecurityExternalAllocationBean allocBean, List<String> messages) {
        List<String> sdfs = allocBean.getMcc().getEligibilityFilterNames();
        boolean isEligibleSecurity = false;
        if (!Util.isEmpty(sdfs)) {
            for (String sdf : sdfs) {
                StaticDataFilter realSDF = BOCache.getStaticDataFilter(DSConnection.getDefault(), sdf);
                if ((realSDF != null) && realSDF.accept(null, allocBean.getSecurity())) {
                    isEligibleSecurity = true;
                    break;
                }
            }
        }

        if (!isEligibleSecurity) {
            messages.add("row " + allocBean.getRowNumber() + " : The security with ISIN code "
                    + (Util.isEmpty(allocBean.getIsin()) ? "" : allocBean.getIsin())
                    + " is not eligible to be used with the contract " + allocBean.getMcc().getName());

        }
        return isEligibleSecurity;
    }

    private boolean checkISIN(SecurityExternalAllocationBean allocBean, List<String> messages) {
        Product sec = null;
        try {
            sec = DSConnection.getDefault().getRemoteProduct().getProductByCode("ISIN", allocBean.getIsin());
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        if (sec == null) {
            messages.add("row " + allocBean.getRowNumber() + " : Unable to found a security with the ISIN code "
                    + (Util.isEmpty(allocBean.getIsin()) ? "" : allocBean.getIsin()));
            return false;
        }
        allocBean.setSecurity(sec);
        return true;
    }

    private boolean checkPricingEnv(ExternalAllocationBean allocBean, List<String> messages) {
        if (getPricingEnv() == null) {
            messages.add("row " + allocBean.getRowNumber() + " : No pricing environment found for the contract  "
                    + allocBean.getCtrShortName());
            return false;
        }
        return true;
    }

    private boolean checkProcessingOrg(ExternalAllocationBean allocBean, List<String> messages) {
        LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), allocBean.getPoShortName());
        if (po == null) {
            messages.add("row " + allocBean.getRowNumber() + " : Invalid ProcessingOrganisation "
                    + (Util.isEmpty(allocBean.getPoShortName()) ? "" : allocBean.getPoShortName()));
            return false;
        }
        return true;
    }

    private boolean checkNominal(ExternalAllocationBean allocBean, List<String> messages) {
        double nominal = allocBean.getNominal();
        if (nominal == 0) {
            messages.add("row " + allocBean.getRowNumber() + " : Invalid Nominal " + allocBean.getNominal() + " (equals zero)");
            return false;
        }
        return true;
    }

    // private boolean checkMarginCallConfig(ExternalAllocationBean allocBean,
    // List<String> messages) {
    // if (getEntry() == null) {
    // messages.add("No marginCall entry selected ");
    // return false;
    // } else {
    // CollateralConfig mcc = getEntry().getCollateralConfig();
    // if (mcc == null) {
    // mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
    // getEntry()
    // .getCollateralConfigId());
    // }
    //
    // if ((mcc == null) || (mcc.getName() == null) ||
    // !mcc.getName().equals(allocBean.getCtrShortName())) {
    // messages.add("row " + allocBean.getRowNumber() + " : Margin call config "
    // + (Util.isEmpty(allocBean.getCtrShortName()) ? "" :
    // allocBean.getCtrShortName())
    // + " is not matching with the selected entry ");
    // return false;
    // }
    // allocBean.setMcc(mcc);
    // return true;
    // }
    // }

    @SuppressWarnings("static-access")
    private boolean checkCollateralConfig(ExternalAllocationBean allocBean, List<String> messages) {
        if (allocBean.getMcc() != null) {
            return true;
        }
        String contractName = allocBean.getCtrShortName();
        int contractId = allocBean.getContractID();
        CollateralConfig marginCallConfig = null;

        if (allocBean.isByName()) {
            try {
                marginCallConfig = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode(null, contractName);
            } catch (CollateralServiceException e) {
                Log.error(this, e); //sonar
                messages.add("row " + allocBean.getRowNumber() + " : Margin call config "
                        + (Util.isEmpty(allocBean.getCtrShortName()) ? "" : allocBean.getCtrShortName()) + " failed");
                return false;
            }
        } else {
            marginCallConfig = CacheCollateralClient.getInstance().getCollateralConfig(DSConnection.getDefault(),
                    contractId);
        }
        allocBean.setMcc(marginCallConfig);
        return true;
    }

    @SuppressWarnings("static-access")
    private boolean checkCollateralConfigTaa(ExternalTripartyBean taa, List<String> messages) {
        if (taa.getMcc() != null) {
            return true;
        }
        String contractName = taa.getCtrShortName();
        int contractId = taa.getContractID();
        CollateralConfig marginCallConfig = null;

        if (taa.isByName()) {
            try {
                marginCallConfig = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode(null, contractName);
            } catch (CollateralServiceException e) {
                messages.add("row " + taa.getRowNumber() + " : Margin call config "
                        + (Util.isEmpty(taa.getCtrShortName()) ? "" : taa.getCtrShortName()) + " failed");
                Log.error(this, e); //sonar
                return false;
            }
        } else {
            marginCallConfig = CacheCollateralClient.getInstance().getCollateralConfig(DSConnection.getDefault(),
                    contractId);
        }
        taa.setMcc(marginCallConfig);
        return true;
    }

    private boolean checkMarginCallStatus(ExternalAllocationBean allocBean, List<String> messages) {
        MarginCallEntry entry = allocBean.getMarginCallEntry();
        String allStatus = CollateralUtilities.getDomainValueComment("contract_status_for_allocation", "all_status");

        if (!Util.isEmpty(allStatus)) {
            String[] status = allStatus.split(";");

            for (String st : status) {
                if (st.equalsIgnoreCase(entry.getStatus()))
                    return true;
            }
        } else {
            return true;
        }
        messages.add("Invalid contract status: " + entry.getStatus() + " for row " + allocBean.getRowNumber());
        return false;
    }

}