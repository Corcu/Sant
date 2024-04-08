package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.marketdata.HaircutProxy;
import com.calypso.tk.marketdata.HaircutProxyFactory;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

public class SantHaircutByIsinReportStyle extends MarginCallReportStyle {

    private static final long serialVersionUID = 123L;

    public static final String ISSUER_SHORT_NAME = "Issuer Short Name";
    public static final String ISIN = "ISIN";
    public static final String PRODUCT_TYPE = "Product Type";
    public static final String ISSUE_DATE = "Issue Date";
    public static final String MATURITY_DATE = "Maturity Date";
    public static final String NEXT_COUPON_DATE = "Next Coupon Date";
    public static final String HAIRCUT = "Haircut";


    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        CollateralConfig agreement = row.getProperty(ReportRow.MARGIN_CALL_CONFIG);

        LegalEntity issuer = row.getProperty(SantHaircutByIsinReportTemplate.ISSUER);

        Product product = row.getProperty(SantHaircutByIsinReportTemplate.PRODUCT);

        JDate valDate = row.getProperty(SantHaircutByIsinReportTemplate.VAL_DATE);

        if (columnName.equals(ISIN)) {
            return product.getSecCode(ISIN);
        } else if (columnName.equals(PRODUCT_TYPE)) {
            return product.getType();
        } else if (columnName.equals(ISSUER_SHORT_NAME)) {
            return issuer.getCode();
        } else if (columnName.equals(ISSUE_DATE)) {
            return getIssueDate(product);
        } else if (columnName.equals(MATURITY_DATE)) {
            return product.getMaturityDate();
        } else if (columnName.equals(NEXT_COUPON_DATE)) {
            return getNextCouponDate(product, valDate);
        } else if (columnName.equals(HAIRCUT)) {
            return getProductHaircut(agreement, product, valDate);
        } else {
            return super.getColumnValue(row, columnName, errors);
        }
    }

    private JDate getIssueDate(Product product) {
        JDate issueDate = null;
        if (product instanceof Bond) {
            Bond bond = (Bond) product;
            issueDate = bond.getIssueDate();
        }
        return issueDate;
    }

    private JDate getNextCouponDate(Product product, JDate valDate) {
        JDate nextCouponDate = null;
        if (product instanceof Bond) {
            Bond bond = (Bond) product;
            nextCouponDate = bond.getNextCouponDate(valDate);
        }
        return nextCouponDate;
    }

    private double getProductHaircut(CollateralConfig agreement, Product product, JDate valDate) {

        CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
        HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
        HaircutProxy haircutProxy = fact.getProxy(agreement.getPoHaircutName());

        // get haircut value for security
        //JRL 20/04/2016 Migration 14.4
        return Math.abs(haircutProxy.getHaircut(agreement.getCurrency(), new CollateralCandidate(product), valDate, true, agreement, "Pay")) * 100;

    }

    @Override
    public TreeList getTreeList() {

        if (this._treeList != null) {
            return this._treeList;
        }
        final TreeList treeList = super.getTreeList();
        return treeList;

    }
}
