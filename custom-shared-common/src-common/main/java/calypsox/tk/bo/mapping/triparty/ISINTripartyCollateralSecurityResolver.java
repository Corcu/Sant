package calypsox.tk.bo.mapping.triparty;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.SecurityMatchingUtil;
import com.calypso.tk.bo.mapping.triparty.DefaultTripartyCollateralSecurityResolver;
import com.calypso.tk.bo.mapping.triparty.TripartyCollateralSecurityResolver;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.secfinance.triparty.TripartyCollateralAllocation;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.service.DSConnection;

public class ISINTripartyCollateralSecurityResolver implements TripartyCollateralSecurityResolver {
    @Override
    public Product findSecurityProduct(String codeName, String codeValue, TripartyCollateralAllocation tripartyMarginDetails) {
        //triparty core search product
        Product product =  Util.isEmpty(codeValue) ? null :
                SecurityMatchingUtil.getExchangeTradedProduct(codeName, codeValue, (LegalEntity)null, Country.valueOfISO(codeValue.substring(0, 2)),
                        tripartyMarginDetails.getSecurityCurrency());

        //if null, get any product with this codeName=codeValue
        if (null!=product){
            return product;
        } else{
            try {
                return DSConnection.getDefault().getRemoteProduct().getProductByCode(codeName,codeValue);
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
            return null;
        }
    }
}
