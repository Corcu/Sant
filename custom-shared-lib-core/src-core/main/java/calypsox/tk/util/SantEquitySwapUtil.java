package calypsox.tk.util;

import calypsox.tk.refdata.CustomLeveragePercentage;
import calypsox.tk.refdata.service.RemoteCustomLeverageService;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * calypsox.tk.util
 *
 * @author x865229
 * date 15/12/2022
 */
public class SantEquitySwapUtil {

    public static final String ISIN = "ISIN";

    //private static final Pattern UNDERLYING_PATTERN = Pattern.compile(".*?\\((\\w{12})\\)");
    private static final Pattern ISIN_ONLY_UNDERLYING_PATTERN = Pattern.compile("(\\w{12})");

    public static LegInfo extractIsinLeg(Trade trade) {
        if (trade.getProduct() instanceof CollateralExposure) {
            CollateralExposure ce = (CollateralExposure) trade.getProduct();
            for (int i = 1; i <= 2; i++) {
                String underlying = (String) ce.getAttribute("UNDERLYING_" + i);
                if (!Util.isEmpty(underlying)) {
                    Matcher matcher = ISIN_ONLY_UNDERLYING_PATTERN.matcher(underlying);
                    if (matcher.matches()) {
                        LegInfo legInfo = extractLeg(trade, i);
                        if (legInfo != null) {
                            legInfo.setIsin(matcher.group(1));
                            return legInfo;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static LegInfo extractLeg(Trade trade, int legNum) {
        if (trade.getProduct() instanceof CollateralExposure) {
            CollateralExposure ce = (CollateralExposure) trade.getProduct();
            LegInfo legInfo = new LegInfo();
            legInfo.setLegNum(legNum);
            legInfo.setDirection((String) ce.getAttribute("DIRECTION_" + legNum));
            legInfo.setUnderlyingType((String) ce.getAttribute("UNDERLYING_TYPE_" + legNum));
            legInfo.setUnderlying((String) ce.getAttribute("UNDERLYING_" + legNum));
            legInfo.setNominal((Amount) ce.getAttribute("NOMINAL_" + legNum));
            legInfo.setCcy((String) ce.getAttribute("CCY_" + legNum));
            legInfo.setMtm((Amount) ce.getAttribute("MTM_" + legNum));
            legInfo.setMtmCcy((String) ce.getAttribute("MTM_CCY_" + legNum));
            legInfo.setClosingPrice((Amount) ce.getAttribute("CLOSING_PRICE_" + legNum));
            return legInfo;
        }
        return null;
    }

    /**
     * Returns the product associated with the ISIN of the leg. If there are several products, the one with ccy BRL
     *
     * @param legInfo the leg info
     * @return the leg product
     */
    public static Product getLegInfoBRLProduct(LegInfo legInfo) {
        if (legInfo != null && !Util.isEmpty(legInfo.getIsin())) {
            return BOCache.getExchangeTradedProductByKey(DSConnection.getDefault(),
                    ISIN + "#Ccy=BRL", legInfo.getIsin());
        }
        return null;
    }

    /**
     * Return the leverage percentage by ISIN leg info and legal entity
     *
     * @param isinLeg the isin leg
     * @param leId    the legal entity id
     * @return the leverage
     */
    public static double getPercentage(LegInfo isinLeg, int leId) {
        if (isinLeg != null) {
            Product product = getLegInfoBRLProduct(isinLeg);
            if (product != null) {
                RemoteCustomLeverageService service = DSConnection.getDefault().getService(RemoteCustomLeverageService.class);
                CustomLeveragePercentage custom = null;
                try {
                    custom = service.loadByProduct(product.getId());
                } catch (RemoteException e) {
                    Log.error("SantEquitySwapUtil", e);
                }
                if (custom != null && !Util.isEmpty(custom.getItems())) {
                    List<CustomLeveragePercentage.CustomLeveragePercentageItem> items = custom.getItems().stream()
                            .filter(item -> item.getLegalEntityId() == leId
                                    || item.getLegalEntityId() == CustomLeveragePercentage.CustomLeveragePercentageItem.ALL_ID)
                            .sorted((f1, f2) -> Integer.compare(f2.getLegalEntityId(), f1.getLegalEntityId())).collect(Collectors.toList());
                    if (!Util.isEmpty(items)) {
                        return items.get(0).getPercentage();
                    }
                }
            }
        }
        return 0d;
    }

    public static class LegInfo {
        int legNum;
        String direction;
        Amount nominal;
        String ccy;
        Amount mtm;
        String mtmCcy;
        Amount closingPrice;
        String underlyingType;
        String underlying;
        String isin;

        public int getLegNum() {
            return legNum;
        }

        public void setLegNum(int legNum) {
            this.legNum = legNum;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public Amount getNominal() {
            return nominal;
        }

        public void setNominal(Amount nominal) {
            this.nominal = nominal;
        }

        public String getCcy() {
            return ccy;
        }

        public void setCcy(String ccy) {
            this.ccy = ccy;
        }

        public Amount getMtm() {
            return mtm;
        }

        public void setMtm(Amount mtm) {
            this.mtm = mtm;
        }

        public String getMtmCcy() {
            return mtmCcy;
        }

        public void setMtmCcy(String mtmCcy) {
            this.mtmCcy = mtmCcy;
        }

        public Amount getClosingPrice() {
            return closingPrice;
        }

        public void setClosingPrice(Amount closingPrice) {
            this.closingPrice = closingPrice;
        }

        public String getUnderlyingType() {
            return underlyingType;
        }

        public void setUnderlyingType(String underlyingType) {
            this.underlyingType = underlyingType;
        }

        public String getUnderlying() {
            return underlying;
        }

        public void setUnderlying(String underlying) {
            this.underlying = underlying;
        }

        public String getIsin() {
            return isin;
        }

        public void setIsin(String isin) {
            this.isin = isin;
        }
    }

}
