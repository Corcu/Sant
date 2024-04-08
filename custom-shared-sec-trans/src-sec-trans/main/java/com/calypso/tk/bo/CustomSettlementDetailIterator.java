package com.calypso.tk.bo;

import calypsox.tk.bo.workflow.rule.UpdateCounterPartySDIMessageRule;
import com.calypso.analytics.Util;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.PartySDIInfo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.jfree.util.Log;

import java.util.Vector;
import java.util.stream.Collectors;

/**
 * CustomSettlementDetailIterator Filter the PartySDIInfo so that they did not appear in the SWIFT message by the DV
 * CustomSettlementDetailIteratorFilter
 *
 * @author Ruben Garcia
 */
public class CustomSettlementDetailIterator extends SettlementDetailIterator {

    /**
     * The CustomSettlementDetailIteratorFilter domain name
     * PartyCode;Role;ContactType;Identifier;ProductType
     */
    private static final String FILTER_BY_CTPY_CODE = "CustomSettlementDetailIteratorFilter";

    /**
     * ALL entities ID
     */
    private static final int ALL_ENTITIES = -1;


    @Override
    public void init(BOMessage message, Trade trade, LEContact sender, LEContact receiver, BOTransfer transfer, Vector transferRules, PricingEnv env, DSConnection dsCon) {
        this._message = message;
        this._trade = trade;
        this._sender = sender;
        this._receiver = receiver;
        this._transfer = transfer;
        this._rules = transferRules;
        this._dsCon = dsCon;
        this._rule = transfer.toTradeTransferRule();
        this._count = 0;
        updateCounterpartySDI();
        super.setIteratorSet(super.getSettlementDetails());
        Vector<PartySDIInfoFilter> filters = buildFilters();
        if (!Util.isEmpty(filters) && !Util.isEmpty(getIteratorSet())) {
            Vector<PartySDIInfo> iteratorSet = (Vector<PartySDIInfo>) getIteratorSet();
            this.setIteratorSet(iteratorSet.stream().filter(i -> !filterPartySDIInfo(filters, i)).
                    collect(Collectors.toCollection(Vector::new)));
        }

    }

    private void updateCounterpartySDI() {
        if (this._message != null && this._rule != null && this._trade != null && this._dsCon != null) {
            String ctpySDI = this._message.getAttribute(UpdateCounterPartySDIMessageRule.CTPY_SDI_ID);
            String mxBilateralCtpy = this._trade.getKeywordValue(UpdateCounterPartySDIMessageRule.MX_BILT_CTPY);
            if (!Util.isEmpty(ctpySDI) && !Util.isEmpty(mxBilateralCtpy)) {
                LegalEntity le = BOCache.getLegalEntity(this._dsCon, mxBilateralCtpy);
                if (le != null) {
                    this._rule.setCounterPartySDId(Integer.parseInt(ctpySDI));
                    this._rule.setCounterParty(le.getId());
                }
            }
        }
    }

    /**
     * Check if you have to filter the PartySDIInfo object
     *
     * @param filters the filter to check
     * @param info    the party info
     * @return true ff it meets any filter
     */
    private boolean filterPartySDIInfo(Vector<PartySDIInfoFilter> filters, PartySDIInfo info) {
        if (!Util.isEmpty(filters) && info != null) {
            for (PartySDIInfoFilter f : filters) {
                if (f.accept(info)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Constructs the filter objects from the domain value CustomSettlementDetailIteratorFilter
     *
     * @return vector of filters
     */
    private Vector<PartySDIInfoFilter> buildFilters() {
        Vector<PartySDIInfoFilter> partyFilters = new Vector<>();
        Vector<String> filters = LocalCache.getDomainValues(DSConnection.getDefault(), FILTER_BY_CTPY_CODE);
        if (!Util.isEmpty(filters)) {
            for (String f : filters) {
                String[] splitF = f.split(";");
                if (splitF.length == 5) {
                    if (!"ALL".equals(splitF[0])) {
                        LegalEntity entity = null;
                        try {
                            entity = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(splitF[0]);
                        } catch (CalypsoServiceException e) {
                            Log.error(this, e);
                        }
                        if (entity != null) {
                            partyFilters.add(new PartySDIInfoFilter(entity.getId(), splitF[1], splitF[2], splitF[3], splitF[4]));
                        }
                    } else {
                        partyFilters.add(new PartySDIInfoFilter(ALL_ENTITIES, splitF[1], splitF[2], splitF[3], splitF[4]));
                    }


                }
            }
        }
        return partyFilters;
    }


    /**
     * PartySDIInfoFilter
     *
     * @author Ruben Garcia
     */
    static class PartySDIInfoFilter {
        private final int partyId;
        private final String role;
        private final String contactType;
        private final String identifier;
        private final String productType;

        PartySDIInfoFilter(int partyId, String role, String contactType, String identifier, String productType) {
            this.partyId = partyId;
            this.role = role;
            this.contactType = contactType;
            this.identifier = identifier;
            this.productType = productType;
        }

        /**
         * Check if PartySDIInfo is equals to filter values
         *
         * @param info PartiSDIInfo object
         * @return true if it has the same values
         */
        boolean accept(PartySDIInfo info) {
            if (info != null) {
                if (this.partyId != ALL_ENTITIES) {
                    if (info.getPartyId() != this.partyId) {
                        return false;
                    }
                }
                if (!Util.isEmpty(this.role) && !Util.isEmpty(info.getRole()) &&
                        !this.role.equals(info.getRole())) {
                    return false;
                }
                if (!Util.isEmpty(contactType) && !Util.isEmpty(info.getContactType()) &&
                        !this.contactType.equals(info.getContactType())) {
                    return false;
                }

                if (!Util.isEmpty(this.identifier) && !Util.isEmpty(info.getIdentifier()) &&
                        !this.identifier.equals(info.getIdentifier())) {
                    return false;
                }

                if (!Util.isEmpty(this.productType) && !Util.isEmpty(info.getProductType()) &&
                        !this.productType.equals(info.getProductType())) {
                    return false;
                }

                return true;
            }
            return false;
        }
    }


}
