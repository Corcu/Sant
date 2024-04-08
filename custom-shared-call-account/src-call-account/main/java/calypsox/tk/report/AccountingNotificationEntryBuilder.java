package calypsox.tk.report;

import calypsox.apps.reporting.util.loader.MarginCallConfigLightLoader;
import calypsox.tk.core.MarginCallConfigLight;
import calypsox.tk.report.generic.loader.interestnotif.SantInterestNotificationEntryBuilder;
import calypsox.tk.report.loader.CallAccountLoader;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.InterestBearingEntry;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.BOPositionUtil;
import com.calypso.tk.util.TradeArray;

import java.util.*;
import java.util.stream.Collectors;

public class AccountingNotificationEntryBuilder extends SantInterestNotificationEntryBuilder {

    protected final List<AccountingNotificationEntry> santInterestEntries = new ArrayList<AccountingNotificationEntry>();
    protected final HashMap<String, AccountingNotificationEntry> agrupation = new HashMap<>();
    private static final String PRESTADO = "PRESTADO";
    private static final String TOMADO = "TOMADO";


    public List<AccountingNotificationEntry> getAccountingEntries() {
        return this.santInterestEntries;
    }

    @Override
    public void build(TradeArray trades, JDate processStartDate, JDate processEndDate, JDate valDate, ReportTemplate template) {
        Map<Integer, MarginCallConfigLight> mccLights = (Map<Integer, MarginCallConfigLight>) template
                .get(SantInterestNotificationReportTemplate.CONTRACT_MAP);
        Map<Integer, Account> accounts = (Map<Integer, Account>) template
                .get(SantInterestNotificationReportTemplate.ACCOUNT_MAP);
        // final String rateIndexes = (String) template.get(SantInterestNotificationReportTemplate.RATE_INDEX);
        // final Vector<String> rateIndexV = Util.string2Vector(rateIndexes);
        final String contractTypes = (String) template.get(SantInterestNotificationReportTemplate.AGREEMENT_TYPE);
        final Vector<String> contractTypeV = Util.string2Vector(contractTypes);
        final String poNames = (String) template.get(SantInterestNotificationReportTemplate.OWNER_AGR);
        final Vector<String> poNameV = Util.string2Vector(poNames);
        final String currencies = (String) template.get(SantInterestNotificationReportTemplate.CURRENCY);
        final Vector<String> currencyV = Util.string2Vector(currencies);

        CallAccountLoader loader = new CallAccountLoader();
        loader.load();
        accounts = loader.get();

        final MarginCallConfigLightLoader mccLightLoader = new MarginCallConfigLightLoader();
        mccLightLoader.load();
        mccLights = mccLightLoader.get();

        boolean calculateMovements=false;
        if(template.getVisibleColumns().contains(SantInterestNotificationReportStyle.MOVEMENT))
        	calculateMovements=true;
        	
        // AdHoc Interest Bearing entries aggregated by account
        Map<Account, List<InterestBearingEntryWrapper>> interestBearingAdHocEntriesByAccount = new HashMap<Account, List<InterestBearingEntryWrapper>>();
        // Generic Interest Bearing aggregated by trade
        Map<Trade, List<InterestBearingEntryWrapper>> interestBearingByTrade = new HashMap<Trade, List<InterestBearingEntryWrapper>>();

        for (int i = 0; i < trades.size(); i++) {
            final Trade trade = trades.get(i);
            buildInterestBearingentryMap(interestBearingAdHocEntriesByAccount, interestBearingByTrade, trade,
                    mccLights, accounts, contractTypeV, poNameV, currencyV);
        }

        // Process generic
        for (Map.Entry<Trade, List<InterestBearingEntryWrapper>> entry : interestBearingByTrade.entrySet()) {
                    buildAccountingCallAccountEntries(processStartDate, processEndDate, valDate, entry.getValue(), calculateMovements);
        }

        List<AccountingNotificationEntry> collect = agrupation.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
        this.santInterestEntries.addAll(collect);

        // Process AdHoc
        for (Map.Entry<Account, List<InterestBearingEntryWrapper>> entry : interestBearingAdHocEntriesByAccount.entrySet()) {
            this.santInterestEntries
                    .addAll(buildAccountingCallAccountEntries(processStartDate, processEndDate, valDate, entry.getValue(), calculateMovements));
        }
    }

    protected Collection<AccountingNotificationEntry> buildAccountingCallAccountEntries(final JDate processStartDate,
                                                                                        final JDate processEndDate, final JDate valDate, List<InterestBearingEntryWrapper> ibEntriesWrapperList, boolean calculateMovements) {

        final List<AccountingNotificationEntry> localEntries = new ArrayList<AccountingNotificationEntry>();

        double previousPositionAmount = 0.0d;
        double totalAccrual = 0.0;

        Map<JDate, InterestPosEntryWrapped> mergedWrappedEntries = new HashMap<JDate, InterestPosEntryWrapped>();
        if (processStartDate.getMonth()==12){
            for (InterestBearingEntryWrapper ibWrapper : ibEntriesWrapperList) {
                mergeWrappedEntryLastAdjustment(mergedWrappedEntries, ibWrapper, processEndDate, valDate);
            }
        } else {
            for (InterestBearingEntryWrapper ibWrapper : ibEntriesWrapperList) {
                mergeWrappedEntry(mergedWrappedEntries, ibWrapper, processEndDate, valDate);
            }
        }

        final SortedSet<JDate> sortedEntriesDates = new TreeSet<JDate>(mergedWrappedEntries.keySet());

        boolean firstRow = true;
        double movementAmount = 0;

        //TODO agrupación por sentido y dirección

        for (final JDate d : sortedEntriesDates) {
            if (d.after(processEndDate)) {
                continue;
            }
            final InterestPosEntryWrapped entry = mergedWrappedEntries.get(d);

            final InterestBearingEntryWrapper interestEntryWrapped = entry.getInterest();
            final InterestBearingEntryWrapper positionEntryWrapped = entry.getPos();
            final InterestBearingEntryWrapper adjustmentEntryWrapped = entry.getAdjustment();

            // Previous
            final InterestPosEntryWrapped previousEntry = mergedWrappedEntries.get(d.addDays(-1));

            InterestBearingEntryWrapper partialSettleEntry = null;
            if (previousEntry != null) {
                partialSettleEntry = previousEntry.getPartialSettle();
            }
            double interestAmount = 0.0d;
            double positionAmount = 0.0d;
            double adjustmentAmount = 0.0d;
            if (interestEntryWrapped != null) {
                interestAmount = interestEntryWrapped.getEntry().getAmount()
                        + interestEntryWrapped.getEntry().getAdjustment();
            }
            if (positionEntryWrapped != null) {
                positionAmount = positionEntryWrapped.getEntry().getAmount();
            }
            if (adjustmentEntryWrapped != null) {
                adjustmentAmount = adjustmentEntryWrapped.getEntry().getAmount();
            }
            if (partialSettleEntry != null) {
                totalAccrual = 0.0d;
            }

            if (d.before(processStartDate)) {
                totalAccrual += interestAmount;
                totalAccrual += adjustmentAmount;
                continue;
            }

            if ((interestEntryWrapped == null) || (positionEntryWrapped == null)) {
                continue;
            }

            Account account = interestEntryWrapped.getAccount();
            Trade trade = interestEntryWrapped.getTrade();
            MarginCallConfigLight mccLight = interestEntryWrapped.getMccLight();
            
            if(calculateMovements) {
            	if (firstRow) {
	                @SuppressWarnings("deprecation") final InventoryCashPosition invCashPosition = BOPositionUtil.getCashPosition(account.getCurrency(),
	                        BOPositionUtil.CLIENT, BOPositionUtil.ACTUAL, BOPositionUtil.SETTLE_DATE, d, 0,
	                        account.getId(), DSConnection.getDefault(), null);
	                if (invCashPosition != null) {
	                    if (invCashPosition.getPositionDate().equals(d)) {
	                        movementAmount = invCashPosition.getDailyChange();
	                    } else {
	                        movementAmount = 0;
	                    }
	                } else {
	                    movementAmount = 0;
	                }
	            } else {
	                movementAmount = positionAmount - previousPositionAmount;
	            }
        	}
            AccountInterestConfig accontInterestConfig = getAccontInterestConfig(interestEntryWrapped.getEntry());
            String indexName = getEntryIndexName(accontInterestConfig,interestEntryWrapped.getEntry());

            String key = "";
            String direction = "";
            if(trade.getProduct() instanceof InterestBearing){
                double principal = positionEntryWrapped.getEntry().getAmount();
                double quantity = trade.getQuantity();
                if(principal<0.0){
                    direction = PRESTADO;
                }else{
                    direction = TOMADO;
                }
            }
            String bookName = interestEntryWrapped.getTrade().getBook().getName();
            key = direction +":"+ bookName +":"+ account.getName() +":"+ account.getCurrency();

            Double interest = interestEntryWrapped.getEntry().getAmount() + interestEntryWrapped.getEntry().getAdjustment();

            JDate endDateActualMonth = JDate.valueOf("01/" + String.valueOf(processEndDate.getMonth() + 1) + "/" + String.valueOf(processEndDate.getYear()));
            JDate actualMonth = JDate.valueOf("01/"+String.valueOf(processEndDate.getMonth())+"/" + String.valueOf(processEndDate.getYear()));
            JDate previusMonth = actualMonth.addBusinessDays(-1, Util.string2Vector("SYSTEM"));


            if(agrupation.containsKey(key)){
                AccountingNotificationEntry accEntry = agrupation.get(key);
                accEntry.setInteretanual(accEntry.getInteretanual()+interest);

                if(null!=adjustmentEntryWrapped){
                    double amount = adjustmentEntryWrapped.getEntry().getAmount();
                    accEntry.setAdjustement(accEntry.getAdjustement()+amount);
                }

                if(interest<0){
                    accEntry.setAnnualNegativeInterest(accEntry.getAnnualNegativeInterest()+interest);
                }else{
                    accEntry.setAnnualPositiveInterest(accEntry.getAnnualPositiveInterest()+interest);
                }
                if(accEntry.getTrade().getLongId()<trade.getLongId()){
                    accEntry.setTrade(trade);
                    /*accEntry.setCurrentLiveBalance(positionAmount);*/
                }

                if(accEntry.getTrade().getLongId()==trade.getLongId() && d.equals(processEndDate)){
                    accEntry.setCurrentLiveBalance(positionAmount);
                }

                if(d.after(previusMonth) && d.before(endDateActualMonth) && d.after(accEntry.getLastInterestDate())){
                    if(accEntry.getTrade().getLongId()==trade.getLongId()){
                        totalAccrual = totalAccrual + interestAmount + adjustmentAmount;
                        accEntry.setUnliquidatedAccumulatedPeriodic(totalAccrual);
                    }else{
                        if(accEntry.getTrade().getLongId()<trade.getLongId()){
                            totalAccrual = interestAmount + adjustmentAmount;
                            accEntry.setUnliquidatedAccumulatedPeriodic(totalAccrual);
                        }
                    }
                }

                agrupation.put(key, accEntry);

            }else{
                final AccountingNotificationEntry santEntry = new AccountingNotificationEntry();
                // GSM: Call account name fix
                santEntry.setAdHoc(interestEntryWrapped.isAdHoc());
                santEntry.setContractName(mccLight.getDescription());
                santEntry.setPoName(mccLight.getPoName());
                santEntry.setContractId(mccLight.getId());
                santEntry.setDate(interestEntryWrapped.getEntry().getEntryDate());
                santEntry.setMovement(movementAmount);
                santEntry.setRate(interestEntryWrapped.getEntry().getRate());
                santEntry.setSpread(interestEntryWrapped.getEntry().getSpread());
                santEntry.setAdjustedRate(interestEntryWrapped.getEntry().getRate()
                        + interestEntryWrapped.getEntry().getSpread());
                santEntry.setDailyAccrual(interestEntryWrapped.getEntry().getAmount() + interestEntryWrapped.getEntry().getAdjustment());

                santEntry.setWatchInterest(account.getAccountProperty("WatchInterest"));

                previousPositionAmount = positionAmount;
                firstRow = false;

                //AccountingNotification new columns
                santEntry.setInteretanual(interestEntryWrapped.getEntry().getAmount() + interestEntryWrapped.getEntry().getAdjustment());
                santEntry.setProccesDate(processEndDate);
                santEntry.setTradeId(trade.getLongId());
                santEntry.setIndexName(indexName);
                santEntry.setCallAccountName(account.getExternalName()); // getName()); old
                santEntry.setCallAccountId(account.getId());
                santEntry.setPrincipal(positionEntryWrapped.getEntry().getAmount());
                santEntry.setCurrency(account.getCurrency());
                santEntry.setBook(interestEntryWrapped.getTrade().getBook());
                santEntry.setMurexID(getMurexID(account));
                santEntry.setInterstBearingDirection(direction);
                santEntry.setCounterparty(trade.getCounterParty());
                santEntry.setCalc(getCompound(accontInterestConfig));
                santEntry.setProduct(mccLight.getContractType());
                santEntry.setKey(santEntry.getCallAccountId()+santEntry.getInterstBearingDirection()+santEntry.getBook()+santEntry.getCurrency());

                CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccLight.getId());
                if (collateralConfig != null) {
                    santEntry.setCouponType(collateralConfig.getAdditionalField(AccountingNotificationReportStyle.COUPON_TYPE));
                    santEntry.setGuaranteeType(collateralConfig.getAdditionalField(AccountingNotificationReportStyle.GUARANTEE_TYPE));
                }


                if(interest<0){
                    santEntry.setAnnualNegativeInterest(interest);
                }else{
                    santEntry.setAnnualPositiveInterest(interest);
                }
                if(d.after(previusMonth) && d.before(endDateActualMonth)){
                    totalAccrual += interestAmount;
                    totalAccrual += adjustmentAmount;
                    santEntry.setUnliquidatedAccumulatedPeriodic(totalAccrual);
                }
                santEntry.setLastInterestDate(d);
                santEntry.setTrade(trade);
                if(d.equals(processEndDate)){
                    santEntry.setCurrentLiveBalance(positionAmount);
                }
                if(null!=adjustmentEntryWrapped){
                    double amount = adjustmentEntryWrapped.getEntry().getAmount();
                    santEntry.setAdjustement(amount);
                }
                agrupation.put(key,santEntry);
            }
        }

        return localEntries;
    }

    private void AccountingCallAccountEntry (Account account,Trade trade){

    }

    private String getMurexID(Account account){
        String murexID = account.getAccountProperty("MurexID");
        return null!=murexID ? murexID : "";
    }


    private AccountInterestConfig getAccontInterestConfig(InterestBearingEntry entry){
        return BOCache.getAccountInterestConfig(DSConnection.getDefault(),
                entry.getConfigId());
    }

    private String getCompound(AccountInterestConfig config){
        if(null!=config){
            if(config.isCompound()){
                return "COMP";
            }else{
                return "SIM";
            }
        }
        return "";
    }

    protected String getEntryIndexName(AccountInterestConfig config,InterestBearingEntry entry) {

        if (null!=config && (config.getRanges() != null)) {
            for (int i = 0; i < config.getRanges().size(); i++) {
                AccountInterestConfigRange range = (AccountInterestConfigRange) config.getRanges().get(i);
                if ((range == null) || range.isFixed() || (range.getId() != entry.getRangeId())) {
                    continue;
                }
                if (range.getRateIndex() == null) {
                    continue;
                }
                return range.getRateIndex().toStringNoSource();
            }
        }
        return null;
    }

    protected void mergeWrappedEntry(Map<JDate, InterestPosEntryWrapped> mergedWrappedEntries,
                                     InterestBearingEntryWrapper ibWrapper, JDate processEndDate, JDate valDate) {
        String entryType = ibWrapper.getEntry().getEntryType();
        JDate entryDate = ibWrapper.getEntry().getEntryDate();
        JDate valueDate = ibWrapper.getEntry().getValueDate();

        InterestPosEntryWrapped intPosWrapped = null;

        if (InterestBearingEntry.INTEREST.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setInterest(ibWrapper);
        } else if (InterestBearingEntry.POSITION.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setPos(ibWrapper);
        } else if (InterestBearingEntry.ADJUSTMENT.equals(entryType) && valDate.getYear()==valueDate.getYear()) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setAdjustment(ibWrapper);
        } else if (InterestBearingEntry.PARTIAL_SETTLE.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setPartialSettle(ibWrapper);
        }
    }

    protected void mergeWrappedEntryLastAdjustment(Map<JDate, InterestPosEntryWrapped> mergedWrappedEntries,
                                     InterestBearingEntryWrapper ibWrapper, JDate processEndDate, JDate valDate) {

        String entryType = ibWrapper.getEntry().getEntryType();
        JDate entryDate = ibWrapper.getEntry().getEntryDate();
        JDate valueDate = ibWrapper.getEntry().getValueDate();

        InterestPosEntryWrapped intPosWrapped = null;

        if (InterestBearingEntry.INTEREST.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setInterest(ibWrapper);
        } else if (InterestBearingEntry.POSITION.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setPos(ibWrapper);
        } else if (InterestBearingEntry.ADJUSTMENT.equals(entryType) && valDate.before(valueDate)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setAdjustment(ibWrapper);
        } else if (InterestBearingEntry.PARTIAL_SETTLE.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setPartialSettle(ibWrapper);
        }
    }


    /**
     * @param value
     */
    private void setAdjustment(AccountingNotificationEntry entry , Double value){
        if(null!=entry){
            if(PRESTADO.equalsIgnoreCase(entry.getInterstBearingDirection())){

            }else if(TOMADO.equalsIgnoreCase(entry.getInterstBearingDirection())){

            }
        }
    }

}
