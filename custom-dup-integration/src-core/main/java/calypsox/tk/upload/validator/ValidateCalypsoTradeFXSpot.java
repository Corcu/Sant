package calypsox.tk.upload.validator;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.Product;
import static calypsox.tk.upload.validator.ValidatorUtils.createCurrencyPair;
import static calypsox.tk.upload.validator.ValidatorUtils.existCurrencyPair;

import java.util.Vector;

public class ValidateCalypsoTradeFXSpot extends com.calypso.tk.upload.validator.ValidateCalypsoTradeFXSpot {

    @Override
    public void validateProduct(Product product, Vector<BOException> errors, long tradeId) {
        String primCcy = product.getFX().getPrimaryCurrency();
        String secCcy = product.getFX().getSecondaryCurrency();
        if (!existCurrencyPair(primCcy, secCcy)) {
            createCurrencyPair(primCcy, secCcy, DSConnection.getDefault());
        }
        super.validateProduct(product, errors, tradeId);
    }


}
