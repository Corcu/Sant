/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.startup;

import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.apps.startup.MainEntryStartUp;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.*;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Vector;

public class MainEntryStartUpCompleteMCCValidation implements MainEntryStartUp {
    private static final String LOGCAT = "MCC_VALIDATION";
    private static RemoteSantCollateralService remoteSantColService = (RemoteSantCollateralService) DSConnection
            .getDefault().getRMIService("baseSantCollateralService", RemoteSantCollateralService.class);
    public static final String ADJUSTEMENT_SDF_PREFIX = "ADJ_";
    public static final String ADD_FIELD_IA_CCY = "CONTRACT_IA_CCY";

    @SuppressWarnings({"rawtypes", "unused"})
    @Override
    public void onStartUp() {
        MCCModificationListener eventListener = new MCCModificationListener();
        // events we are interested in
        Class[] subscriptionList = new Class[]{PSEventDomainChange.class,};
        Log.system(LOGCAT, "Connecting to routing bus...");
        try {
            PSConnection ps = ESStarter.startConnection(eventListener, subscriptionList);
        } catch (ConnectException e) {
            Log.error(this, e);
        }

    }

    /**
     * MySubscriber class will be the call back point for all incoming events. newEvent will be invoked when an event
     * matching the subscription list is recieved.
     */
    private static class MCCModificationListener implements PSSubscriber {
        @SuppressWarnings("rawtypes")
        @Override
        public void newEvent(PSEvent event) {
            if (!(event instanceof PSEventDomainChange)) {
                return;
            }
            PSEventDomainChange domainChangeEvent = ((PSEventDomainChange) event);
            boolean lockAcquired = false;
            if ((event != null) && (PSEventDomainChange.MARGIN_CALL_CONFIG == domainChangeEvent.getType())) {
                CollateralConfig mcc = null;
                try {
                    mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                            domainChangeEvent.getValueId());
                    if (mcc == null) {
                        return;
                    }
                    Vector audit = DSConnection
                            .getDefault()
                            .getRemoteTrade()
                            .getAudit(
                                    "entity_id=" + mcc.getId()
                                            + " and entity_class_name = 'CollateralConfig' and version_num="
                                            + mcc.getVersion(), "bo_audit.version_num", null);

                    if (Util.isEmpty(audit)) {
                        return;
                    }
                    String lastModifyer = ((AuditValue) audit.get(0)).getUserName();
                    // to avoid concurrent handling of an event, we check that the contract was modified by the same
                    // user
                    if (!DSConnection.getDefault().getUser().equals(lastModifyer)) {
                        return;
                    }
                    // acquire a lock on the contract
                    lockAcquired = acquireLockOnContract(mcc);
                    if (!lockAcquired) {
                        System.out.println("Cannot acquire a lock for " + mcc.getId()
                                + " quit the handling of this event");
                        return;
                    }

                    // handle the case where the contract is created manually and the filter adjustment doesn't have the
                    // contract id yet
                    StaticDataFilter sdf = BOCache.getStaticDataFilter(DSConnection.getDefault(),
                            mcc.getProdStaticDataFilterName());
                    if (sdf != null) {
                        StaticDataFilter adjSDF = null;
                        Vector errors = new Vector();
                        @SuppressWarnings("unchecked")
                        Vector<StaticDataFilter> sdfElements = sdf.getLinkedStaticDatafilters(errors);
                        if (!Util.isEmpty(sdfElements)) {
                            for (StaticDataFilter tmpSDF : sdfElements) {
                                String filterName = tmpSDF.getName();
                                if (filterName.startsWith(ADJUSTEMENT_SDF_PREFIX)) {
                                    adjSDF = tmpSDF;
                                    break;
                                }
                            }

                            if (adjSDF != null) {

                                Vector<StaticDataFilterElement> elements = adjSDF.getElements();
                                if (!Util.isEmpty(elements)) {
                                    for (StaticDataFilterElement sdfElement : elements) {
                                        if (("KEYWORD." + CollateralStaticAttributes.MC_CONTRACT_NUMBER)
                                                .equals(sdfElement.getName())) {
                                            if (Util.isEmpty(sdfElement.getValues())
                                                    || sdfElement.getValues().contains("0")) {
                                                Vector<String> v = new Vector<String>();
                                                v.add("" + mcc.getId());
                                                sdfElement.setValues(v);
                                                DSConnection.getDefault().getRemoteReferenceData().save(adjSDF);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }

                    // get the generic comment related to this contract
                    Double ia = getMCCIndependentAmount(mcc);
                    Trade iaTrade = null;
                    try {
                        TradeArray iaTrades = DSConnection
                                .getDefault()
                                .getRemoteTrade()
                                .getTrades(
                                        "product_collateral_exposure",
                                        "trade.product_id=product_collateral_exposure.product_id and product_collateral_exposure.mcc_id="
                                                + mcc.getId()
                                                + " and underlying_type='CONTRACT_IA' and trade.trade_status<>'CANCELED' ",
                                        "trade.trade_id", null);

                        if (!Util.isEmpty(iaTrades) && (iaTrades.size() > 0)) {
                            iaTrade = iaTrades.get(0);
                        }

                    } catch (Exception e) {
                        Log.error(this, e);
                    }
                    Trade oldTrade = null;
                    if (iaTrade != null) {
                        oldTrade = (Trade) iaTrade.clone();
                    }

                    // create a independent amout trade for the current contract
                    try {
                        createUpdateIAExposureTrade(mcc, ia, iaTrade, oldTrade);
                    } catch (Exception e) {
                        Log.error(this, e);
                    }
                } catch (Exception e) {
                    Log.error(this, e);
                } finally {
                    if (lockAcquired) {
                        releaseLockOnContract(mcc);
                    }
                }
            }
        }

        private static boolean acquireLockOnContract(CollateralConfig mcc) {
            if (mcc == null) {
                return false;
            }
            int counter = 0;
            boolean lockAcquired = false;
            while (!lockAcquired && (counter < 5)) {
                counter++;
                try {
                    remoteSantColService.acquireLockOnContract(mcc.getId());
                    lockAcquired = true;
                } catch (RemoteException e) {
                    Log.debug(MainEntryStartUpCompleteMCCValidation.class, "Contract " + mcc.getId()
                            + " already locked");
                    Log.error(MainEntryStartUpCompleteMCCValidation.class, e); //sonar
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Log.error(MainEntryStartUpCompleteMCCValidation.class, ie);
                    }
                }
            }
            return lockAcquired;
        }

        private static void releaseLockOnContract(CollateralConfig mcc) {
            if (mcc == null) {
                return;
            }
            try {
                remoteSantColService.releaseLockOnContract(mcc.getId());
            } catch (RemoteException e) {
                Log.error(MainEntryStartUpCompleteMCCValidation.class, e.getMessage());
                Log.error(MainEntryStartUpCompleteMCCValidation.class, e); //sonar
            }
        }

        @Override
        public void onDisconnect() {
            Log.system(LOGCAT, "Event bus has disconnected!");
        }
    }

    /**
     * @param mcc
     * @param ia
     * @param trade
     * @return
     * @throws RemoteException
     */
    private static boolean createUpdateIAExposureTrade(CollateralConfig mcc, Double ia, Trade trade, Trade oldTrade)
            throws Exception {
        Calendar cal = Calendar.getInstance(mcc.getValuationTimeZone());
        cal.setTimeInMillis(mcc.getStartingDate().getTime());
        cal.set(Calendar.AM_PM, Calendar.PM);
        cal.set(Calendar.HOUR, 11);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);

        JDatetime tradeDateTime = new JDatetime(cal.getTime());

        DSConnection dsCon = DSConnection.getDefault();
        CollateralExposure product = null;
        if (trade == null) {
            if (ia == 0.0) {
                // nothing to create,
                return true;
            }
            trade = new Trade();
            trade.setAction(Action.NEW);
            product = new CollateralExposure();
            trade.setProduct(product);
        } else {
            if (ia == 0.0) {
                trade.setAction(Action.CANCEL);
            } else {
                // Double oldIa = ((CollateralExposure) trade.getProduct()).getPrincipal() * trade.getQuantity();
                // // nothing changed on the trade so runaway.
                // if ((oldIa != null) && oldIa.equals(ia) ) {
                // return true;
                // }
                trade.setAction(Action.valueOf("UNPRICE"));
            }
            product = (CollateralExposure) trade.getProduct();
        }

        // set trade properties
        String currency = mcc.getCurrency();
        if (!Util.isEmpty(mcc.getAdditionalField(ADD_FIELD_IA_CCY))) {
            currency = mcc.getAdditionalField(ADD_FIELD_IA_CCY).toUpperCase();
        }
        trade.setTraderName(dsCon.getUser());
        trade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
        trade.setTradeCurrency(currency);
        trade.setSettleCurrency(currency);
        trade.setBook(mcc.getBook());
        trade.addKeyword("BO_REFERENCE", "IA");
        trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, mcc.getId());

        // set trade and product dates
        trade.setSettleDate(mcc.getStartingDate().getJDate(TimeZone.getDefault()));
        trade.setTradeDate(tradeDateTime);
        product.setEnteredDatetime(new JDatetime());
        product.setStartDate(mcc.getStartingDate().getJDate(TimeZone.getDefault()));
        // product.setMaturityDate(entry.getProcessDate());
        // set the end date for the product
        // product.setEndDate(entry.getProcessDate());
        product.setDirection((ia >= 0 ? "Buy" : "Sell"), trade);
        // set the product properties
        product.setPrincipal(Math.abs(ia));
        product.setSubType("CONTRACT_IA");
        product.setUnderlyingType("CONTRACT_IA");
        product.setCurrency(currency);
        // link this trade to the entry contract
        product.setMccId(mcc.getId());
        product.addAttribute("CONTRACT_ID", "" + mcc.getId());
        dsCon.getRemoteTrade().save(trade);
        return true;
    }

    private static Double getMCCIndependentAmount(CollateralConfig mcc) {
        // get the IA from the contract directly.
        Double ia = 0.0;
        String ctrIA = mcc.getAdditionalField("CONTRACT_INDEPENDENT_AMOUNT");
        if (Util.isEmpty(ctrIA)) {
            ia = 0.0;
        } else {

            try {
                ia = Double.valueOf(ctrIA);
            } catch (Exception e) {
                Log.error(MainEntryStartUpCompleteMCCValidation.class, e);
                ia = 0.0;
            }
        }
        return ia;
    }

}
