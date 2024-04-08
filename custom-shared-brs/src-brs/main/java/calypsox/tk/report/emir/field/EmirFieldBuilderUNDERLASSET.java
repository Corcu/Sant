
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Basket;
import com.calypso.tk.product.PerformanceSwapLeg;

public class EmirFieldBuilderUNDERLASSET implements EmirFieldBuilder {
    @Override
    /*
    "Nueva logica
    Si somos el Pay del swap entonces informamos este campo con el valor ""LEI""
    Si somos el rec del swap entonces revisamos si la contrapartida tiene LEI. Si lo tiene informamos el valor ""LEI"". Si no lo tiene fijamos el valor ""INTERNAL""."
    */

    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        String uType = EmirFieldBuilderUtil.getInstance().getLogicUNDERLYNGASSETTYPE(trade);
        if (EmirSnapshotReduxConstants.ISIN.equalsIgnoreCase(uType)) {
            PerformanceSwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);

            if (pLeg.getLegConfig().equalsIgnoreCase(EmirSnapshotReduxConstants.LEG_SINGLE_ASSET)) {
                rst = pLeg.getReferenceProduct().getSecCode(EmirSnapshotReduxConstants.ISIN);

            } else if (pLeg.getReferenceProduct() instanceof Basket)  {
                Basket basket = (Basket) pLeg.getReferenceProduct();
                rst = getBasketISINS(basket);
            }
        }

        if (Util.isEmpty(rst)) {
            rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        }

        return rst;
    }

    private String getBasketISINS(Basket basket) {
        StringBuilder rst = new StringBuilder();
        if (basket != null && !Util.isEmpty(basket.getBasketComponents())) {
            for (Object obj : basket.getBasketComponents()) {
                if (obj instanceof  Product) {
                    Product component = (Product) obj;
                    if (!Util.isEmpty(component.getSecCode(EmirSnapshotReduxConstants.ISIN))) {
                        rst.append(component.getSecCode(EmirSnapshotReduxConstants.ISIN));
                        rst.append(";");
                    }
                }
            }
        }
        return  rst.toString();
    }


}
