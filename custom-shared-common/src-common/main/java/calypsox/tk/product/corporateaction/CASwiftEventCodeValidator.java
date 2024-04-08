package calypsox.tk.product.corporateaction;

import calypsox.tk.product.CAProductValidator;
import com.calypso.tk.product.CA;

import java.util.List;

public class CASwiftEventCodeValidator extends com.calypso.tk.product.corporateaction.CASwiftEventCodeValidator {
    CAProductValidator productValidator = new CAProductValidator();

    @Override
    protected List<String> checkValidSwiftEventCode(CA ca) {
        List<String> strings = super.checkValidSwiftEventCode(ca);
        productValidator.addLiquidConfig(ca);
        return strings;
    }
}
