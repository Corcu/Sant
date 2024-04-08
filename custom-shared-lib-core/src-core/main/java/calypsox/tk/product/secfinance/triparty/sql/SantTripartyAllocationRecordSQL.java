package calypsox.tk.product.secfinance.triparty.sql;

import com.calypso.tk.core.*;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.product.secfinance.triparty.TripartyAllocationData;
import com.calypso.tk.product.secfinance.triparty.TripartyAllocationRecord;
import com.calypso.tk.product.secfinance.triparty.TripartyPersistenceReport;
import com.calypso.tk.product.secfinance.triparty.sql.TripartyAllocationRecordSQL;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SantTripartyAllocationRecordSQL extends TripartyAllocationRecordSQL {

    public static TripartyPersistenceReport santSaveNewAllocationRecords(List<TripartyAllocationData> newAllocationRecords, @Nullable List<Trade> existingAllocationTradesToTerminate, String tripartyAgent) throws PersistenceException {
        for (int i = 0; i < newAllocationRecords.size(); i++) {
            TripartyAllocationData data = newAllocationRecords.get(i);
            if (data != null && data.getPledgeTrade() != null && data.getPledgeTrade().getProduct() instanceof Pledge) {
                final String processingOrg = Optional.ofNullable(data.getPledgeTrade().getBook()).map(Book::getLegalEntity).map(LegalEntity::getCode).orElse("");

                String dvName = "TripartyPledgeBook";
                if("BDSD".equalsIgnoreCase(processingOrg)){
                    dvName = "SLBTripartyPledgeBook";
                }

                Product pledgeSecurity = ((Pledge) data.getPledgeTrade().getProduct()).getSecurity();
                DSConnection dsCon = DSConnection.getDefault();
                String bookName = LocalCache.getDomainValueComment(dsCon,dvName, pledgeSecurity.getProductFamily());
                try {
                    Trade parentTrade = dsCon.getRemoteTrade().getTrade(Integer.parseInt(data.getPledgeTrade().getInternalReference()));
                    Book pledgeBook = dsCon.getRemoteReferenceData().getBook(bookName);
                    if(pledgeBook != null){
                        data.getPledgeTrade().setBook(pledgeBook);
                        String tripartyEligibilitySet = parentTrade.getKeywordValue("TripartyEligibilitySet");

                        boolean tripartyEligibilitySetB = !com.calypso.infra.util.Util.isEmpty(tripartyEligibilitySet);
                        if (tripartyEligibilitySetB) {
                            data.getPledgeTrade().addKeyword("TripartyEligibilitySet",tripartyEligibilitySet);
                        }

                        data.getPledgeTrade().addKeyword("ParentProductType", parentTrade.getProductType());
                        String tripartyAgentKw = LegalEntity.valueOf(data.getAgentId()).getAuthName();
                        data.getPledgeTrade().addKeyword("TripartyAgent", tripartyAgentKw);

                        newAllocationRecords.set(i,data);
                    }else{
                        Log.error(TripartyAllocationRecordSQL.class, "Did not find " + bookName + " to assign to the pledge trade");
                    }
                } catch (CalypsoServiceException | NullPointerException | NumberFormatException e) {
                    Log.error(TripartyAllocationRecordSQL.class, e);
                }
            }
        }

        TripartyPersistenceReport report = TripartyAllocationRecordSQL.save(newAllocationRecords, existingAllocationTradesToTerminate, tripartyAgent);

        if(!Util.isEmpty(newAllocationRecords)){
            for (TripartyAllocationData data : newAllocationRecords) {
                if( ((TripartyAllocationRecord)data).getMargingCallContractId() == 0
                        && !"N/A".equals(data.getIsin())){
                    List existedAllocationRecordWithSameStatementAndIsin = null;
                    try {
                        existedAllocationRecordWithSameStatementAndIsin = getExistedAllocationRecordWithSameStatementAndIsin(data, tripartyAgent,true);
                    } catch (PersistenceException var26) {
                        Log.error(TripartyAllocationRecordSQL.class, var26.getMessage());
                    }
                    if (!existedAllocationRecordWithSameStatementAndIsin.isEmpty()) {
                        TripartyAllocationRecord existedAllocationRecordToUpdate = (TripartyAllocationRecord) existedAllocationRecordWithSameStatementAndIsin.get(0);
                        Trade existedPledgeTrade = existedAllocationRecordToUpdate.getPledgeTrade();
                        if (updateKeywords((TripartyAllocationRecord)data,existedPledgeTrade)) {
                            data.setHasExistedAllocationRecordToUpdate(true);
                        }
                    }
                }
            }
        }
        return report;
    }

    /**
     * @param newData
     * @param existedPledgeTrade
     * @return
     */
    public static boolean updateKeywords(TripartyAllocationRecord newData,Trade existedPledgeTrade){
        try {
            Trade newPledgeTrade = existedPledgeTrade.clone();
            newPledgeTrade.setAction(Action.AMEND);
            newPledgeTrade.setUpdatedTime(new JDatetime());
            String user = existedPledgeTrade.getEnteredUser();
            if (!Util.isEmpty(user)) {
                newPledgeTrade.setEnteredUser(user);
            }
            if(updateNewPledgeTradeKeywords(newData,existedPledgeTrade,newPledgeTrade)){
                DSConnection.getDefault().getRemoteTrade().save(newPledgeTrade);
                return true;
            }
        }catch (Exception e){
            Log.error(SantTripartyAllocationRecordSQL.class.getSimpleName(),"Error: " + e);
        }
        return false;
    }


    /**
     * @param newData
     * @param existedPledgeTrade
     * @param newPledgeTrade
     * @return
     */
    private static boolean updateNewPledgeTradeKeywords(TripartyAllocationRecord newData,Trade existedPledgeTrade, Trade newPledgeTrade){
        boolean needSave = false;
        Locale locale = new Locale("es", "ES");
        final double oldKeywordValue = Util.stringToNumber(existedPledgeTrade.getKeywordValue("90A::MRKT"), locale);
        final Double collValPrice = newData.getCollValPrice();
        final double oldCovaKeywordValue = Util.stringToNumber(existedPledgeTrade.getKeywordValue("19A::COVA"), locale);
        final Double collateralHeld = newData.getCollateralHeld();
        final double oldVafcKeywordValue = Util.stringToNumber(existedPledgeTrade.getKeywordValue("92A::VAFC"), locale);
        final Double valuationFactor = newData.getValuationFactor();
        final double oldMktpKeywordValue = Util.stringToNumber(existedPledgeTrade.getKeywordValue("19A::MKTP"), locale);
        final Double marketValue = newData.getMarketValue();
        final double oldExchKeywordValue = Util.stringToNumber(existedPledgeTrade.getKeywordValue("92B::EXCH"), locale);
        final Double fxRate = newData.getFxRate();
        final String oldTripartyAgentValue = existedPledgeTrade.getKeywordValue("TripartyAgent");
        final String tripartyAgent = LegalEntity.valueOf(newData.getAgentId()).getAuthName();

        //Log to verify TripartyAgent value
        Log.info("SantTripartyAllocationRecordSQL", String.format("Filling TrypartyAgent with value %s on trade %s", newPledgeTrade.getKeywordValue("TripartyAgent"), newPledgeTrade.getLongId()));

        if(!Util.isEqualStrings(oldTripartyAgentValue, tripartyAgent)){
            newPledgeTrade.addKeyword("TripartyAgent",tripartyAgent);
            needSave = true;
        }

        if(Util.isEmpty(newPledgeTrade.getKeywordValue("TripartyAgent"))){
            newPledgeTrade.addKeyword("TripartyAgent",tripartyAgent);
            needSave = true;
        }

        if(!Util.isEqual(oldKeywordValue,collValPrice,0.0)){
            newPledgeTrade.addKeyword("90A::MRKT",Util.numberToString(collValPrice, locale));
            needSave = true;
        }
        if(!Util.isEqual(oldCovaKeywordValue,collateralHeld,0.0)){
            newPledgeTrade.addKeyword("19A::COVA",Util.numberToString(collateralHeld, locale));
            needSave = true;
        }

        if(!Util.isEqual(oldVafcKeywordValue,valuationFactor,0.0)){
            newPledgeTrade.addKeyword("92A::VAFC",Util.numberToString(valuationFactor, locale));
            needSave = true;
        }

        if(!Util.isEqual(oldMktpKeywordValue,marketValue,0.0)){
            newPledgeTrade.addKeyword("19A::MKTP",Util.numberToString(marketValue, locale));
            needSave = true;
        }

        if(!Util.isEqual(oldExchKeywordValue,fxRate,0.0)){
            newPledgeTrade.addKeyword("92B::EXCH",Util.numberToString(fxRate, locale));
            needSave = true;
        }

        //Log to verify TripartyAgent value
        Log.info("SantTripartyAllocationRecordSQL", String.format("Checking TrypartyAgent with value %s on trade %s", newPledgeTrade.getKeywordValue("TripartyAgent"), newPledgeTrade.getLongId()));

        return needSave;
    }

    private static List<TripartyAllocationRecord> getExistedAllocationRecordWithSameStatementAndIsin(TripartyAllocationData data, String tripartyAgent, boolean executeSql) throws PersistenceException {
        String statementIdentification = (String)data.getAttributeValue("statement_number");
        if (executeSql) {
            StringBuilder builder = new StringBuilder();
            List<CalypsoBindVariable> bindVariables = new ArrayList();
            String transactionCcy = null;
            TripartyAllocationRecord tar;
            if (data.getTradeLongId() == 0L) {
                builder.append("triparty_allocation_records.margin_call_contract = ? ");
                bindVariables.add(new CalypsoBindVariable(4, ((TripartyAllocationRecord)data).getMargingCallContractId()));
                tar = (TripartyAllocationRecord)data;
                transactionCcy = tar.getTransactionCcy();
            } else {
                builder.append("triparty_allocation_records.trade_id = ? ");
                bindVariables.add(new CalypsoBindVariable(3000, data.getTradeLongId()));
            }

            if (data instanceof TripartyAllocationRecord) {
                tar = (TripartyAllocationRecord)data;
                String genericIdentifier = tar.getGenericIdentifier();
                if (!Util.isEmpty(genericIdentifier) && !"N/A".equals(genericIdentifier)) {
                    builder.append("  and ");
                    builder.append("triparty_allocation_records.generic_identifier = ? ");
                    bindVariables.add(new CalypsoBindVariable(12, genericIdentifier));
                }
            }

            builder.append("  and ");
            builder.append("triparty_allocation_records.transaction_currency = ? ");
            builder.append("  and ");
            builder.append("triparty_allocation_records.isin = ? ");
            builder.append("  and ");
            builder.append("triparty_allocation_records.coll_date = ?");
            builder.append("  and ");
            builder.append("  triparty_allocation_records.pledge_id in ");
            builder.append(fromSamePageReference());
            if (transactionCcy != null) {
                builder.append(" and triparty_allocation_records.transaction_currency = ? ");
            }

            bindVariables.add(new CalypsoBindVariable(12, data.getTransactionCcy()));
            bindVariables.add(new CalypsoBindVariable(12, data.getIsin()));
            bindVariables.add(new CalypsoBindVariable(3002, data.getCollateralDate()));
            bindVariables.add(new CalypsoBindVariable(12, "statement_number"));
            bindVariables.add(new CalypsoBindVariable(12, statementIdentification));
            if (transactionCcy != null) {
                bindVariables.add(new CalypsoBindVariable(12, transactionCcy));
            }

            return load(builder.toString(), tripartyAgent, bindVariables);
        } else {
            return new ArrayList();
        }
    }

    private static String fromSamePageReference() {
        StringBuilder builder = new StringBuilder();
        builder.append("  (select distinct triparty_allocation_attributes.entity_id from triparty_allocation_attributes ");
        builder.append("  where ");
        builder.append("  triparty_allocation_attributes.attr_name = ? ").append(" and triparty_allocation_attributes.attr_value  =  ? )");
        return builder.toString();
    }
}
