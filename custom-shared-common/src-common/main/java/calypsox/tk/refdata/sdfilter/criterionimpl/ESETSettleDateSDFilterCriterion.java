package calypsox.tk.refdata.sdfilter.criterionimpl;

import com.calypso.analytics.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.JDate;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;
import org.jfree.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ESETSettleDateSDFilterCriterion extends AbstractSDFilterCriterion {

    private static final String CRIT_NAME = "ESETSettleDate";

    public ESETSettleDateSDFilterCriterion() {
        setName(CRIT_NAME);
        setCategory(SDFilterCategory.MESSAGE);
    }

    @Override
    public List<SDFilterOperatorType> getOperatorTypes() {
        return Arrays.asList(SDFilterOperatorType.DATE_COMPARISON, SDFilterOperatorType.DATE_RANGE, SDFilterOperatorType.TENOR_RANGE);
    }

    @Override
    public Class getValueType() {
        return JDate.class;
    }

    @Override
    public Object getValue(SDFilterInput input) {
        BOMessage message = input.getMessage();
        String settleDate = message.getAttribute("ESET_SettleDate");
        if (!Util.isEmpty(settleDate)) {
            try {
                DateFormat format = new SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH);
                Date date = format.parse(settleDate);
                return JDate.valueOf(date);
            } catch (ParseException e) {
                Log.error(this.getClass().getSimpleName() + "Can't parse the ESET_SettleDate: " + settleDate, e);
                return JDate.getNow();
            }
        }
        return JDate.getNow();
    }
}
