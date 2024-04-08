package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerRepo;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.PricerMeasureUtility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RepoNewEndCreFeeAccrualTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "NEW FEE_ACCRUAL Cre in case of Repo EndDate is today";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (trade == null) {
            return true;
        }
        CreArray cresToSave = new CreArray();
        Repo repo = (Repo) trade.getProduct();
        JDate endDate = getEndDate(repo);

        if (trade.getAction().equals(Action.RERATE) || (trade.getAction().equals(Action.AMEND) && anyCashChange(trade,oldTrade))) {
            if(!existAccrualToday(trade,JDate.getNow())){
                BOCre cre = getOldACCRUALCre(trade,endDate);
                if (null != cre) {
                    if(isActionPredate(trade,oldTrade)){
                        if (endDate.before(JDate.getNow())) {
                            BOCre creToReversal = getOldACCRUALCre(trade,JDate.getNow());
                            cresToSave.add(createNewACCRUAl(trade, creToReversal, endDate, BOCre.REVERSAL,true));

                            BOCre creNew = createNewACCRUAl(trade, cre, endDate, BOCre.NEW,true);
                            creNew.addAttribute("EndDateChange","true");
                            cresToSave.add(creNew);
                        } else {
                            cresToSave.add(createNewACCRUAl(trade, cre, endDate, BOCre.REVERSAL,false));
                            BOCre creNew = createNewACCRUAl(trade, cre, endDate, BOCre.NEW,false);
                            cresToSave.add(creNew);
                        }
                    }else if(JDate.getNow().after(endDate)){
                        BOCre creNew = createNewACCRUAl(trade, cre, endDate, BOCre.NEW,false);
                        cresToSave.add(creNew);
                    }
                }
            }

        } else if (trade.getAction().equals(Action.TERMINATE) && isActionPredate(trade, oldTrade)){
            BOCre cre = getOldACCRUALCre(trade,endDate);
            if (null != cre){
                if (endDate.before(JDate.getNow())) {
                    BOCre creNew = createNewACCRUAl(trade, cre, endDate, BOCre.NEW,true);
                    creNew.addAttribute("EndDateChange","true");
                    cresToSave.add(creNew);
                }else {
                    BOCre creNew = createNewACCRUAl(trade, cre, endDate, BOCre.NEW,false);
                    cresToSave.add(creNew);
                }
            }

        }
        else if (null != endDate && endDate == JDate.getNow()) {
            BOCre cre = getOldACCRUALCre(trade,null);

            if (null != cre && cre.getEffectiveDate().getMonth() == endDate.getMonth() && (cre.getEffectiveDate() != (endDate))) {
                JDate effectiveDate = cre.getEffectiveDate().addBusinessDays(1, Util.string2Vector("SYSTEM"));

                BOCre creNew = createNewACCRUAl(trade, cre, effectiveDate, BOCre.NEW,false);
                cresToSave.add(creNew);
            }
        }

        if (!cresToSave.isEmpty()) {
            try {
                DSConnection.getDefault().getRemoteBO().saveCres(cresToSave); //TODO Comprobar si se genera duplicado
                publishEvents(cresToSave);
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error saving Cre: " + e.getClass());
            }
        }
        return true;
    }

    protected Double calculatePricerMeasure(String pmName, Trade trade, PricingEnv env, JDatetime dateTime) {
        double pricerMeasureAmt = 0.0D;
        Pricer pricer = new PricerRepo();
        PricerMeasure measure = PricerMeasureUtility.makeMeasure(pmName);
        try {
            pricer.price(trade, dateTime, env, new PricerMeasure[]{measure});
            pricerMeasureAmt = Optional.ofNullable(measure).map(PricerMeasure::getValue).orElse(0.0D);
        } catch (PricerException exc) {
            Log.warn(this,exc.getCause());
        }
        return pricerMeasureAmt;
    }

    /**
     * Unicamnete cunado hay cambio de EndDate
     * @param newTrade
     * @param oldTrade
     * @return
     */
    private boolean isActionPredate(Trade newTrade, Trade oldTrade){
        JDate newRepoEndDate = getEndDate(Optional.ofNullable(newTrade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).orElse(null));
        JDate oldRepoEndDate = getEndDate(Optional.ofNullable(oldTrade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).orElse(null));
        return null!=newRepoEndDate && newRepoEndDate.before(oldRepoEndDate) && newRepoEndDate.lte(JDate.getNow());
    }

    /**
     * Check any controlled change on Repo Cash side
     *
     * @param newTrade
     * @param oldTrade
     * @return
     */
    private boolean anyCashChange(Trade newTrade, Trade oldTrade){
        boolean result = false;
        String change = "";
        final Optional<Cash> optionalNewCash = Optional.ofNullable(newTrade.getProduct()).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getCash);
        final Optional<Cash> optionalOldCash = Optional.ofNullable(oldTrade.getProduct()).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getCash);
        if(optionalNewCash.isPresent() && optionalOldCash.isPresent()){
            final Cash newCash = optionalNewCash.get();
            final Cash oldCash = optionalOldCash.get();
            final RateIndex newRateIndex = newCash.getRateIndex();
            final RateIndex oldRateIndex = oldCash.getRateIndex();

            if(checkChangeValue(newCash.getPrincipal(),oldCash.getPrincipal())){
                change = "Principal";
                result = true;
            }

            if(null!=newCash.getStartDate() && null!=oldCash.getStartDate() && !newCash.getStartDate().equals(oldCash.getStartDate())){
                change = "StartDate";
                result = true;
            }
            if(checkChangeValue(newCash.getFixedRate(),oldCash.getFixedRate())){
                change = "FixedRate";
                result = true;
            }
            final String newRateIndexValue = Optional.ofNullable(newRateIndex).map(RateIndex::toString).orElse("");
            final String oldRateIndexValue = Optional.ofNullable(oldRateIndex).map(RateIndex::toString).orElse("");
            //Any change on RateIndex Values (name, currency, tenor and source)
            if(!newRateIndexValue.equalsIgnoreCase(oldRateIndexValue)){
                change = "RateIndex";
                result = true;
            }
            final String newDateRoll = Optional.ofNullable(newRateIndex).map(RateIndex::getDateRoll).map(DateRoll::toString).orElse("");
            final String oldDateRoll = Optional.ofNullable(oldRateIndex).map(RateIndex::getDateRoll).map(DateRoll::toString).orElse("");
            if(!newDateRoll.equalsIgnoreCase(oldDateRoll)){
                change = "DateRoll";
                result = true;
            }

            final String newDayCount = Optional.ofNullable(newRateIndex).map(RateIndex::getDayCount).map(DayCount::toString).orElse("");
            final String oldDayCount = Optional.ofNullable(oldRateIndex).map(RateIndex::getDayCount).map(DayCount::toString).orElse("");
            if(!newDayCount.equalsIgnoreCase(oldDayCount)){
                change = "DayCount";
                result = true;
            }

            final String newCompoundFrequency = Optional.of(newCash).map(Cash::getCompoundFrequency).map(Frequency::toString).orElse("");
            final String oldCompoundFrequency = Optional.of(oldCash).map(Cash::getCompoundFrequency).map(Frequency::toString).orElse("");
            if(!newCompoundFrequency.equalsIgnoreCase(oldCompoundFrequency)){
                change = "CompoundFrequency";
                result = true;
            }

            final String newPaymentFrequency = Optional.of(newCash).map(Cash::getPaymentFrequency).map(Frequency::toString).orElse("");
            final String oldPaymentFrequency = Optional.of(oldCash).map(Cash::getPaymentFrequency).map(Frequency::toString).orElse("");
            if(!newPaymentFrequency.equalsIgnoreCase(oldPaymentFrequency)){
                change = "PaymentFrequency";
                result = true;
            }

            final String newRateType = Optional.of(newCash).map(Cash::getRateType).orElse("");
            final String oldRateType = Optional.of(oldCash).map(Cash::getRateType).orElse("");
            if(!newRateType.equalsIgnoreCase(oldRateType)){
                change = "RateType";
                result = true;
            }

            if(checkChangeValue(newCash.getSpread(),oldCash.getSpread())){
                change = "Spread";
                result = true;
            }

            if(result){
                Log.info(this, "[Repo: "+newTrade.getLongId()+"] Detected change: " + change);
            }

        }

        return result;
    }

    private boolean checkChangeValue(double firstValue, double secondValue){
        return !Util.isEqual(firstValue,secondValue,0.00);
    }

    private boolean isFxRateCashChange(Trade newTrade, Trade oldTrade){
        final Double newFxRate = Optional.ofNullable(newTrade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getCash).map(Cash::getFixedRate).orElse(0.0);
        final Double oldFxRate = Optional.ofNullable(oldTrade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getCash).map(Cash::getFixedRate).orElse(0.0);
        return Util.isEqual(newFxRate,oldFxRate,0.0);
    }

    private JDate getEndDate(Repo repo){
        JDate endDate = null;
        if(null!=repo){
            if (!repo.getMaturityType().equalsIgnoreCase("OPEN")) {
                endDate = repo.getEndDate();
            } else if (repo.getSecurity() instanceof Bond) {
                Bond bond = (Bond) repo.getSecurity();
                endDate = bond.getEndDate();
            }
        }
        return endDate;
    }

    /**
     * @param trade
     * @return
     */
    private BOCre getOldACCRUALCre(Trade trade, JDate effectiveDate){
        BOCre oldCre = null;
        try {
            String whereClause = buildWhereClause(trade, BOCre.NEW,effectiveDate);
            CreArray array = DSConnection.getDefault().getRemoteBO().getBOCres(null, whereClause, null);
            if (array != null && !array.isEmpty()) {
                oldCre = Arrays.stream(array.getCres()).max(Comparator.comparing(s -> s.getEffectiveDate().getJDatetime())).orElse(null);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve CREs from Trade: " + e.toString());
        }
        return oldCre;
    }

    private boolean existAccrualToday(Trade trade, JDate effectiveDate){
        try {
            String whereClause = buildWhereCreation(trade, effectiveDate);
            CreArray array = DSConnection.getDefault().getRemoteBO().getBOCres(null, whereClause, null);
            if (array != null && !array.isEmpty()) {
                final BOCre boCre = Arrays.stream(array.getCres()).max(Comparator.comparing(BOCre::getCreationDate)).orElse(null);
                return Optional.ofNullable(boCre).isPresent();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve CREs from Trade: " + e.toString());
        }
        return false;
    }

    /**
     * @param cre
     * @param effectiveDate
     * @return
     */
    private BOCre createNewACCRUAl(Trade trade, BOCre cre, JDate effectiveDate, String CreType,boolean predated){
        BOCre creNew = null;
        try {
            creNew = (BOCre) cre.clone();

            if(predated){
                creNew.setAmount(0, 0.0D);
            }else if (effectiveDate.equals(JDate.getNow())) { // On End Date force amount 1 to 0
                creNew.setAmount(0, 0.0D);
            } else if (effectiveDate.getDayOfWeek() == JDate.MONDAY) { // Check if monday
                JDatetime dateTime = effectiveDate.addDays(-1).getJDatetime(TimeZone.getTimeZone("Europe/Madrid"));
                PricingEnv env = PricingEnv.loadPE("OFFICIAL_ACCOUNTING", dateTime);
                Double accrualAmount = calculatePricerMeasure("ACCRUAL_FIRST", trade, env, dateTime);
                creNew.setAmount(0, accrualAmount);
            }

            creNew.setId(0L);
            creNew.setEffectiveDate(effectiveDate);
            creNew.setCreationDate(new JDatetime());
            creNew.setCreType(CreType);
            creNew.setSentDate(null);
            creNew.setStatus(BOCre.NEW);
            creNew.setSentStatus(null);
            creNew.setBookingDate(effectiveDate);
            if("REVERSAL".equalsIgnoreCase(CreType)){
                creNew.setLinkedId(cre.getId());
            }else {
                creNew.setVersion(0);
            }
        } catch (CloneNotSupportedException e) {
            Log.error(this,"Error" + e);
        }
        return creNew;
    }

    private BOCre createNewReversalACCRUAl(BOCre cre, JDate effectiveDate){
        BOCre creNew = null;
        try {
            creNew = (BOCre) cre.clone();
            creNew.setId(0L);
            creNew.setVersion(0);
            creNew.setEffectiveDate(effectiveDate);
            creNew.setCreationDate(JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault()));
            creNew.setCreType(BOCre.REVERSAL);
            creNew.setSentDate(null);
            creNew.setStatus(BOCre.NEW);
            creNew.setSentStatus(null);
            creNew.setBookingDate(effectiveDate);
        } catch (CloneNotSupportedException e) {
            Log.error(this,"Error" + e);
        }
        return creNew;
    }




    public static boolean isPredateRepo(Trade trade){
        final StringBuilder where = new StringBuilder();
        where.append("entity_id = " + trade.getLongId());
        where.append(" AND ");
        where.append("entity_class_name = 'Trade'");
        where.append(" AND ");
        where.append("trunc(modif_date) = " + Util.date2SQLString(JDate.getNow()));
        where.append(" AND ");
        where.append("entity_field_name LIKE 'Product.CASH._endDate'");
        try {
            final Vector<AuditValue> auditConf = DSConnection.getDefault().getRemoteTrade().getAudit(where.toString(), "entity_id ASC, version_num DESC",null);
            if(!Util.isEmpty(auditConf)){
                String date = Optional.ofNullable(auditConf.get(0)).map(AuditValue::getField).map(FieldModification::getNewValue).orElse("");
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
                JDate newEndDate = JDate.valueOf(dateFormat.parse(date));
                return null!=newEndDate && newEndDate.before(JDate.getNow());
            }
        } catch (CalypsoServiceException | ParseException e) {
            Log.error(RepoNewEndCreFeeAccrualTradeRule.class.getName(),"Error" + e);
        }
        return false;
    }


    /**
     * Publish event for CreOnlineSenderEngine
     *
     * @param cresToSave
     */
    private void publishEvents(CreArray cresToSave) {
        if (null != cresToSave && !cresToSave.isEmpty()){
            for (BOCre cre : cresToSave){
                PSEventCre creEvent = new PSEventCre();
                creEvent.setBoCre(cre);
                try {
                    DSConnection.getDefault().getRemoteTrade().saveAndPublish(creEvent);
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Error publish the event: " + e);
                }
            }
        }
    }

    private boolean isPreviousDayHoliday(Trade trade, JDate endDate) {
        JDate previousDay = endDate.addDays(-1);
        if (((Repo) trade.getProduct()).getMaturityType().equalsIgnoreCase("TERM")) {
            return !CollateralUtilities.isBusinessDay(previousDay, Util.string2Vector("SYSTEM"));
        }
        return false;
    }

    private double priceRepo(Trade trade) {
        PricerRepo pricerRepo = new PricerRepo();
        PricerMeasure pm = null;
        JDatetime valDatetime = JDate.getNow().addBusinessDays(-1, null).getJDatetime(TimeZone.getDefault());
        PricingEnv official_accounting = PricingEnv.loadPE("OFFICIAL_ACCOUNTING", valDatetime);
        official_accounting.setTimeZone(TimeZone.getDefault());
        try {
            List<String> kws = removeAndKeepKeywords(trade);
            pm = pricerRepo.price(trade, valDatetime,official_accounting, PricerMeasure.ACCRUAL_FIRST);
            addKeywords(trade, kws);
        } catch (PricerException e) {
            Log.error("Can not do price on trade: " + trade.getLongId(), e);
        }
        return Optional.ofNullable(pm.getValue()).orElse(0.0);
    }

    private List<String> removeAndKeepKeywords(Trade trade) {
        List<String> saveValues = Arrays.asList(trade.getKeywordValue("TerminationDate"), trade.getKeywordValue("TerminationTradeDate"), trade.getKeywordValue("TerminationPayIntFlow"));
        trade.removeKeyword("TerminationDate");
        trade.removeKeyword("TerminationTradeDate");
        trade.removeKeyword("TerminationPayIntFlow");
        return saveValues;
    }

    private void addKeywords(Trade trade, List<String> savedValues) {
        trade.addKeyword("TerminationDate", savedValues.get(0));
        trade.addKeyword("TerminationTradeDate", savedValues.get(1));
        trade.addKeyword("TerminationPayIntFlow", savedValues.get(2));
    }

    private String buildWhereClause(Trade trade, String creType, JDate effectiveDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(" bo_cre_type = '");
        sb.append(BOCreConstantes.ACCRUAL);
        sb.append("'");
        sb.append(" AND trade_id = ");
        sb.append(trade.getLongId());
        sb.append(" AND cre_type = '");
        sb.append(creType);
        sb.append("'");
        sb.append(" AND cre_status = 'NEW'");
        if(null!=effectiveDate){
            sb.append(" AND trunc(effective_date) < " + Util.date2SQLString(effectiveDate));
        }
        return sb.toString();
    }

    private String buildWhereCreation(Trade trade, JDate effectiveDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(" bo_cre_type = '");
        sb.append(BOCreConstantes.ACCRUAL);
        sb.append("'");
        sb.append(" AND trade_id = ");
        sb.append(trade.getLongId());
        sb.append(" AND cre_type IN ('NEW') ");
        sb.append(" AND cre_status = 'NEW'");
        if(null!=effectiveDate){
            sb.append(" AND trunc(creation_date) = " + Util.date2SQLString(effectiveDate));
        }
        return sb.toString();
    }
}
