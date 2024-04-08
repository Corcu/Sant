package com.calypso.tk.upload.jaxb;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * @author aalonsop
 */
public class StrikePriceScheduledAdapter {

    private final StrikePriceSchedules strikePriceSchedules;

    public StrikePriceScheduledAdapter(){
        this.strikePriceSchedules=new StrikePriceSchedules();
    }

    public void addStrikePriceSchedule(CouponStrikeSchedule strikeSchedule){
        List<CouponStrikeSchedule> strikeScheduleList=this.strikePriceSchedules.getStrikeSchedule();
        strikeScheduleList.add(strikeSchedule);
        //this.strikePriceSchedules.strikeSchedule=strikeScheduleList;
    }

    public void convertAndAddStrikePriceSchedule(XMLGregorianCalendar schedDate, Double schedRate){
        CouponStrikeSchedule strikeSchedule=new CouponStrikeSchedule();
        strikeSchedule.setDate(schedDate);
        strikeSchedule.setRate(schedRate*100.0D);
        addStrikePriceSchedule(strikeSchedule);
    }


    public StrikePriceSchedules getStrikePriceSchedules(){
        return this.strikePriceSchedules;
    }
}
