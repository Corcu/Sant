package calypsox.tk.csdr;

import com.calypso.analytics.Util;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.FdnCurrencyDefault;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;

public enum CSDRSecurityCategory {

    LIQUID(4, 4, new String[]{"LISH"}),
    OTHERS(7, 7, new String[]{"CBON", "GOMB", "ISLH", "OTHR"}),
    SME(15, 7, new String[]{"GBON", "NBON"});

    int initialKickoffDays;
    int daysAddedPerStep;
    String[] valueMap;

    String currentCategory;

    String applicableRatesDomainName = "CSDRApplicableRates";

    CSDRSecurityCategory(int initialKickoffDays, int daysAddedPerStep, String[] valueMap) {
        this.initialKickoffDays = initialKickoffDays;
        this.daysAddedPerStep = daysAddedPerStep;
        this.valueMap = valueMap;
    }

    //HOTFIX, NEED TO CLEANUP THIS
    public CSDRPenaltyPeriod getTargetCSDRPeriod(BOTransfer xfer, JDate targetDate){
        CSDRPenaltyPeriod targetPeriod=CSDRPenaltyPeriod.COMPENSATION;

        CSDRPenaltyPeriod periodToCheck=getTargetCSDRPeriod(xfer,targetDate,CSDRPenaltyPeriod.EXTENSION,CSDRPenaltyPeriod.BUYIN);
        if(periodToCheck==null){
            periodToCheck = getTargetCSDRPeriod(xfer, targetDate, CSDRPenaltyPeriod.BUYIN, CSDRPenaltyPeriod.DEFERRAL);
        }
        if(periodToCheck==null){
            periodToCheck = getTargetCSDRPeriod(xfer, targetDate, CSDRPenaltyPeriod.BUYIN, CSDRPenaltyPeriod.DEFERRAL);
        }
        if(periodToCheck==null) {
            periodToCheck= getTargetCSDRPeriod(xfer, targetDate, CSDRPenaltyPeriod.DEFERRAL, CSDRPenaltyPeriod.COMPENSATION);
        }
        if(periodToCheck!=null){
            targetPeriod=periodToCheck;
        }
        return targetPeriod;
    }

    public CSDRPenaltyPeriod getTargetCSDRPeriod(BOTransfer xfer, JDate targetDate,CSDRPenaltyPeriod firstPeriod,CSDRPenaltyPeriod followingPeriod){
        CSDRPenaltyPeriod targetPeriod=null;
        JDate firstPeriodDate=getAdjustedCSDRDate(xfer,firstPeriod);
        JDate followingPeriodDate=getAdjustedCSDRDate(xfer,followingPeriod);
        if(targetDate.gte(firstPeriodDate)&&targetDate.before(followingPeriodDate)){
            targetPeriod=firstPeriod;
        }
        return targetPeriod;
    }

    public JDate getAdjustedCSDRDate(BOTransfer xfer, CSDRPenaltyPeriod periodAction) {
        Vector<String> holidays = getHolidaysFromTransfer(xfer);
        return getAdjustedCSDRDate(xfer, periodAction, holidays);
    }

    public JDate getAdjustedCSDRDate(BOTransfer xfer, CSDRPenaltyPeriod periodAction, Vector<String> holidays) {
        JDate kickoffTime = null;
        if (CSDRPenaltyPeriod.EXTENSION.equals(periodAction)) {
            kickoffTime = calculateJDatetime(xfer, holidays, 0);
        } else if (CSDRPenaltyPeriod.BUYIN.equals(periodAction)) {
            kickoffTime = calculateJDatetime(xfer, holidays, this.initialKickoffDays);
        } else if (CSDRPenaltyPeriod.DEFERRAL.equals(periodAction)) {
            kickoffTime = calculateJDatetime(xfer, holidays, this.initialKickoffDays + daysAddedPerStep);
        } else if (CSDRPenaltyPeriod.COMPENSATION.equals(periodAction)) {
            kickoffTime = calculateJDatetime(xfer, holidays, this.initialKickoffDays + (daysAddedPerStep * 2));
        }
        return kickoffTime;
    }

    private JDate calculateJDatetime(BOTransfer xfer, Vector<String> holidays, int addedDays) {
        return Holiday.getCurrent().addBusinessDays(xfer.getValueDate(), holidays, addedDays, true);
    }


    public static CSDRSecurityCategory lookup(String csdrCategory) {
        String mappedCat = Optional.ofNullable(csdrCategory)
                .map(String::toUpperCase).orElse("");
        CSDRSecurityCategory enumValue = null;
        for (CSDRSecurityCategory currentEnum : CSDRSecurityCategory.values()) {
            if (Arrays.asList(currentEnum.valueMap).contains(mappedCat)) {
                enumValue = currentEnum;
                enumValue.currentCategory = csdrCategory;
                break;
            }
        }
        return enumValue;
    }

    public double getBPSRate() {
        double bpsRate = 0.0d;
        String bpsRateStr = LocalCache.getDomainValueComment(DSConnection.getDefault(), applicableRatesDomainName, this.currentCategory);
        if (!Util.isEmpty(bpsRateStr)) {
            try {
                bpsRate = Double.parseDouble(bpsRateStr);
            } catch (NumberFormatException exc) {
                Log.debug(this, exc.getCause());
            }
        }
        return bpsRate;
    }

    private Vector<String> getHolidaysFromTransfer(BOTransfer xfer) {
        return Optional.ofNullable(xfer).map(BOTransfer::getSettlementCurrency)
                .map(LocalCache::getCurrencyDefault).map(FdnCurrencyDefault::getDefaultHolidays)
                .orElse(getSystemHolidays());
    }

    private Vector<String> getSystemHolidays() {
        Vector<String> holidays = new Vector<>();
        holidays.add("SYSTEM");
        return holidays;
    }
}
