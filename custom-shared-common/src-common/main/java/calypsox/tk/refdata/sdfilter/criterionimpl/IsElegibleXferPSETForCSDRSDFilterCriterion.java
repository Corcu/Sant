package calypsox.tk.refdata.sdfilter.criterionimpl;

import calypsox.tk.csdr.CSDRXferPSETHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class IsElegibleXferPSETForCSDRSDFilterCriterion extends AbstractSDFilterCriterion<Boolean> {

    private static final String CRIT_NAME = "IsElegibleXferPSETForCSDR";

    public IsElegibleXferPSETForCSDRSDFilterCriterion() {
        setName(CRIT_NAME);
        setCategory(SDFilterCategory.TRANSFER);
        setTradeNeeded(false);
    }

    @Override
    public List<SDFilterOperatorType> getOperatorTypes() {
        return Collections.singletonList(SDFilterOperatorType.IS);
    }

    public SDFilterCategory getCategory() {
        return SDFilterCategory.TRANSFER;
    }

    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public Boolean getValue(SDFilterInput sdFilterInput) {
        final BOTransfer xfer = sdFilterInput.getTransfer();
        return Optional.ofNullable(xfer).map(t->new CSDRXferPSETHandler().isElegiblePSET(xfer,null)).orElse(false);
    }
}
