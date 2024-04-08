/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.MarginCallAllocationFacade;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.ProductDescriptionGenerator;
import com.calypso.tk.core.ProductDescriptionGeneratorUtil;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondMMDiscount;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOPositionReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import calypsox.util.collateral.CollateralUtilities;

public class SantCollateralPositionReportStyle extends BOSecurityPositionReportStyle {

    private final BOSecurityPositionReportStyle secReportStyle = new BOSecurityPositionReportStyle();
    private final BOCashPositionReportStyle cashReportStyle = new BOCashPositionReportStyle();

    private static final String COUNTERPARTY = "CounterParty";
    private static final String CTR_NAME = "Collateral agree";
    private static final String CTR_BASE_CCY = "Coll. Base CCY";
    private static final String ELIGIBLE_COL = "Eligible Collateral";
    public static final String FACE_AMOUNT = "Face Amount";
    public static final String NET_MTM_CCY_AGREE = "Net MTM CCY Agree";
    public static final String NET_MTM_CCY_MOVEMENT = "Net MTM CCY movement";
    protected static final String POS_STATUS = "Status";

    private static final long serialVersionUID = 1L;

    public static final String[] DEFAULT_COLUMN_NAMES = new String[]{BOPositionReportStyle.PO, COUNTERPARTY,
            CTR_NAME, BOSecurityPositionReportStyle.MCC_CONTRACT_TYPE, CTR_BASE_CCY, ELIGIBLE_COL, POS_STATUS};

    private final HashMap<String, PricingEnv> pricingEnvsCache = new HashMap<>();

    @SuppressWarnings({"rawtypes"})
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        Inventory pos = row.getProperty(ReportRow.INVENTORY);

        if (pos == null) {
            return null;
        }

        //GSM v14 fix - now the MC id is not in getConfigId()
        MarginCallConfig mcConfig = (pos.getMarginCallConfigId() == 0) ? null : BOCache.getMarginCallConfig(
                DSConnection.getDefault(), pos.getMarginCallConfigId());

        if (mcConfig == null) {
            return null;
        }

        if (COUNTERPARTY.equals(columnName)) {
            return mcConfig.getLegalEntity().getName();
        } else if (CTR_BASE_CCY.equals(columnName)) {
            return mcConfig.getCurrency();
        } else if (CTR_NAME.equals(columnName)) {
            return mcConfig.getName() + " - (" + mcConfig.getCurrency() + ")";
        } else if (POS_STATUS.equals(columnName)) {
            //v14 Mig - AAP, correction in mapping position status
            if (BOSecurityPositionReport.FAILED.equals(BOSecurityPositionReport.getPositionTypeMapping(pos.getPositionType()))) {
                return "In Transit";
            } else if (BOSecurityPositionReport.ACTUAL.equals(BOSecurityPositionReport.getPositionTypeMapping(pos.getPositionType()))) {
                return "Held";
            }
        }

        if (pos instanceof InventoryCashPosition) {
            InventoryCashPosition cashPos = (InventoryCashPosition) pos;
            if (ELIGIBLE_COL.equals(columnName)) {
                return this.cashReportStyle.getColumnValue(row, BOCashPositionReportStyle.CURRENCY, errors);
            } else if (FACE_AMOUNT.equals(columnName)) {
                return cashPos.getTotal();
            }
            if (columnName.contains(NET_MTM_CCY_AGREE)) {
                // Convert to MC Base Currency
                return calculateNET_MTM_CCY_AGREE(row, columnName, errors, mcConfig, this.cashReportStyle);

            } else {
                return this.cashReportStyle.getColumnValue(row, columnName, errors);
            }

        } else if (pos instanceof InventorySecurityPosition) {
            InventorySecurityPosition secPos = (InventorySecurityPosition) pos;
            if (ELIGIBLE_COL.equals(columnName)) {
            	Object bond = row.getProperty(INV_PRODUCT);
            	if (bond != null && bond instanceof BondMMDiscount) {
            		Bond bondDummy = new Bond();
            		ProductDescriptionGenerator gen = ProductDescriptionGeneratorUtil.getGenerator(bondDummy);
            		return gen == null ? null : gen.getDescription((Bond)bond);
            	}

            	return this.secReportStyle.getColumnValue(row, BOSecurityPositionReportStyle.PRODUCT_DESCRIPTION,
            			errors);

            } else if (FACE_AMOUNT.equals(columnName)) {
                return this.secReportStyle.getColumnValue(row, columnName, errors);
            } else if (columnName.contains(NET_MTM_CCY_AGREE)) {
                // Convert to MC Base Currency
                return calculateNET_MTM_CCY_AGREE(row, columnName, errors, mcConfig, this.cashReportStyle);
            } else if (columnName.contains(NET_MTM_CCY_MOVEMENT)) {
                // get the nominal amount
                JDate columnDate = extractDate(columnName);
                // We always need to use Pricing Env of D-1
                JDate prevColumnDate = columnDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                PricingEnv pricingEnv = getPricingEnvFromCache(mcConfig, prevColumnDate);

                if (columnDate != null) {

                    HashMap positions = row.getProperty(BOPositionReport.POSITIONS);
                    if (positions == null) {
                        return null;
                    }

                    Vector datedPositions = (Vector) positions.get(columnDate);
                    if (Util.isEmpty(datedPositions)) {
                        return null;
                    }
                    InventorySecurityPosition invSecPos = (InventorySecurityPosition) datedPositions.get(0);

                    Amount totalSecurityAmount = (Amount) this.secReportStyle.getColumnValue(row, columnName, errors);
                    Product security = invSecPos.getProduct();
                    Double collateralHaircut = getCollateralHaircut(pos, row, columnName, errors);

                    Double bondClosingPricing = getSecurityQuoteValue(secPos, pricingEnv, prevColumnDate);
                    Log.info("SantCollateralPositionReportStyle", "Haircut=" + collateralHaircut + "; SecurityId="
                            + security.getId() + "; ISIN=" + security.getSecCode("ISIN"));

                    if ((collateralHaircut != null) && (bondClosingPricing != null)) {
                        Double balanceHC = totalSecurityAmount.get() * (collateralHaircut / 100) * bondClosingPricing;
                        return new Amount(balanceHC, 2);
                    }
                }
            } else {
                return this.secReportStyle.getColumnValue(row, columnName, errors);
            }

        }

        return null;

    }

    @SuppressWarnings({"static-access", "unchecked"})
    private Object calculateNET_MTM_CCY_AGREE(final ReportRow row, final String columnName,
                                              @SuppressWarnings("rawtypes") final Vector errors, MarginCallConfig mcConfig, BOPositionReportStyle style) {
        // Convert to MC Base Currency
        String ccy = (String) style.getColumnValue(row, style.CURRENCY, errors);
        JDate columnDate = extractDate(columnName);
        String dateString = Util.dateToMString(columnDate);
        Amount movement = (Amount) getColumnValue(row, dateString + "_" + NET_MTM_CCY_MOVEMENT, errors);
        if (!Util.isEmpty(ccy) && (movement != null)) {
            if (ccy.equals(mcConfig.getCurrency())) {
                return movement;
            } else {
                // We always need to use Pricing Env of D-1
                JDate prevColumnDate = columnDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                PricingEnv pricingEnv = getPricingEnvFromCache(mcConfig, prevColumnDate);
                try {
                    double convertCurrency = CollateralUtilities.convertCurrency(ccy, movement.get(),
                            mcConfig.getCurrency(), prevColumnDate, pricingEnv);
                    return new Amount(convertCurrency, 2);

                } catch (MarketDataException e) {
                    errors.add(e.getMessage());
                    Log.error(this, e); //sonar
                }
            }
        }
        return null;
    }

    /**
     * Tells if an allocation is in transit or held according to its settlement date.
     *
     * @param reportDate     The date on which the report is launched
     * @param settlementDate The settlement date of the collateral position
     * @return {@link SantCollateralPositionReport#ALLOCATION_STATUS_HELD} if the allocation is settled or
     * {@link SantCollateralPositionReport#ALLOCATION_STATUS_IN_TRANSIT} if the allocation is not settled when
     * the report is launched.
     */
    @SuppressWarnings("unused")
    private String getAllocationStatus(final JDate reportDate, final MarginCallAllocationFacade allocation) {
        final JDate settlementDate = allocation.getSettlementDate();

        String status = SantCollateralPositionReport.ALLOCATION_STATUS_HELD;

        if (settlementDate.after(reportDate)) {
            status = SantCollateralPositionReport.ALLOCATION_STATUS_IN_TRANSIT;
        }

        return status;
    }

    @Override
    public TreeList getTreeList() {

        TreeList treelist = super.getTreeList();
        //
        treelist.add(BOPositionReportStyle.PRODUCT_DESCRIPTION);
        treelist.add(BOSecurityPositionReportStyle.CLEANPRICE_QUOTE);
        treelist.add(BOSecurityPositionReportStyle.CLEANPRICE_VALUE);
        treelist.add(BOSecurityPositionReportStyle.MCC_CONTRACT_NAME);
        treelist.add(BOSecurityPositionReportStyle.MCC_HAIRCUT_TYPE);
        treelist.add(BOSecurityPositionReportStyle.MCC_CONTRACT_TYPE);
        treelist.add(BOPositionReportStyle.CONFIG_ID);
        treelist.add(BOPositionReportStyle.PO);
        treelist.add(CTR_NAME);
        treelist.add(CTR_BASE_CCY);
        treelist.add(COUNTERPARTY);
        treelist.add(ELIGIBLE_COL);
        treelist.add(FACE_AMOUNT);
        treelist.add(NET_MTM_CCY_MOVEMENT);
        treelist.add(POS_STATUS);

        return treelist;
    }

    private Double getSecurityQuoteValue(InventorySecurityPosition inventory, PricingEnv env, JDate date) {
        if (env == null) {
            return null;
        }
        Double closePrice = null;
        QuoteSet quoteSet = env.getQuoteSet();
        int securityId = inventory.getSecurityId();
        Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), securityId);
        if (product == null) {
            return null;
        }

        QuoteValue productQuote = quoteSet.getProductQuote(product, date, env.getName());
        if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) {
            closePrice = productQuote.getClose();

            // row.setProperty("CleanPrice", closePrice * 100);
            // double total = inventory.getTotal();
            // if ((closePrice != null) && !Double.isNaN(total) && (product instanceof Bond)) {
            //
            // // This is used in the report Style to calculate Balance_HC
            // row.setProperty("Bodn_Closing_Price", closePrice);
            //
            // Bond bond = (Bond) product;
            // double cleanPriceValue = total * closePrice * bond.getFaceValue();
            // row.setProperty("CleanPrice_Value", cleanPriceValue);
            // }
        }
        return closePrice;

    }

    public PricingEnv getPricingEnvFromCache(MarginCallConfig marginCallConfig, JDate date) {

        PricingEnv pricingEnv = null;
        String envName = marginCallConfig.getPricingEnvName() + "_" + Util.dateToMString(date);

        if (this.pricingEnvsCache.get(envName) == null) {
            try {
                pricingEnv = DSConnection.getDefault().getRemoteMarketData()
                        .getPricingEnv(marginCallConfig.getPricingEnvName(), new JDatetime(date, TimeZone.getDefault()));
            } catch (RemoteException e) {
                Log.error(this, e);
                return null;
            }
            this.pricingEnvsCache.put(envName, pricingEnv);

        } else {
            pricingEnv = this.pricingEnvsCache.get(envName);
        }

        return pricingEnv;
    }

}
