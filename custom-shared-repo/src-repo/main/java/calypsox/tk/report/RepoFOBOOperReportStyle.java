package calypsox.tk.report;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Vector;

public class RepoFOBOOperReportStyle extends TradeReportStyle {
    public static final String FHCONCILIA = "FHCONCILIA";
    public static final String CONTRACTID = "CONTRACTID";
    public static final String ROOTCONTRACT = "ROOTCONTRACT";
    public static final String EXTERNAL = "EXTERNALNUMBER";
    public static final String FOLDER = "FOLDER";
    public static final String CONTRAPARTIDA = "CONTRAPARTIDA";
    public static final String MIRRORFOLDER = "MIRRORFOLDER";
    public static final String DIRECTION = "DIRECTION";
    public static final String INTERNA = "INTERNA";
    public static final String ISINCURR = "ISINCURR";
    public static final String STLCURR = "STLCURR";
    public static final String QUANTITY = "QUANTITY";
    public static final String CLEANPRICE = "CLEANPRICE";
    public static final String DIRTYPRICE = "DIRTYPRICE";
    public static final String NOMINAL = "NOMINAL";
    public static final String SETTLEMENT_TYPE = "SETTLEMENT_TYPE";
    public static final String OPEN_REPO = "OPEN_REPO";
    public static final String COUPONTYPE = "COUPONTYPE";
    public static final String INDIREFE = "INDIREFE";
    public static final String RATECOUPON = "RATECOUPON";
    public static final String EFECTIVO_INICIAL = "EFECTIVO_INICIAL";
    public static final String EFECTIVO_FINAL = "EFECTIVO_FINAL";
    public static final String NOM_NO_INDX = "NOM_NO_INDX";
    public static final String MARCA_INDX = "MARCA_INDX";
    public static final String FHMATUR = "FHMATUR";
    public static final String ACTION = "ACTION";
    public static final String ACTION_EFF_DATE = "ACTION_EFF_DATE";


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        RepoFOBOOperBean bean = Optional.ofNullable(row.getProperty(RepoFOBOOperBean.class.getName())).map(o -> (RepoFOBOOperBean) o).orElse(new RepoFOBOOperBean());

        if (FHCONCILIA.equalsIgnoreCase(columnId)) {
            return ReportRow.getValuationDateTime(row);
        }else if (CONTRACTID.equalsIgnoreCase(columnId)) {
            return bean.getContractId();
        }else if (ROOTCONTRACT.equalsIgnoreCase(columnId)) {
            return bean.getRootContractId();
        }else if (EXTERNAL.equalsIgnoreCase(columnId)) {
            return bean.getExternalNumber();
        }else if (FOLDER.equalsIgnoreCase(columnId)) {
            return bean.getBook();
        }else if (CONTRAPARTIDA.equalsIgnoreCase(columnId)) {
            return bean.getCounterParty();
        }else if (MIRRORFOLDER.equalsIgnoreCase(columnId)) {
            return bean.getMirrorBook();
        }else if (DIRECTION.equalsIgnoreCase(columnId)) {
            return bean.getDirection();
        }else if (INTERNA.equalsIgnoreCase(columnId)) {
            return bean.getInternalOper();
        }else if (ISINCURR.equalsIgnoreCase(columnId)) {
            return bean.getIsinCcy();
        }else if (STLCURR.equalsIgnoreCase(columnId)) {
            return bean.getCashCcy();
        }else if (QUANTITY.equalsIgnoreCase(columnId)) {
            //return super.getProductColumnValue(row,"Sec. Quantity" , errors);
            return bean.getQuantity();
        }else if (CLEANPRICE.equalsIgnoreCase(columnId)) {
            return bean.getCleanPrice();
        }else if (DIRTYPRICE.equalsIgnoreCase(columnId)) {
            return bean.getDirtyPrice();
        }else if (NOMINAL.equalsIgnoreCase(columnId)) {
            return bean.getNominal();
        }else if (SETTLEMENT_TYPE.equalsIgnoreCase(columnId)) {
            return bean.getSettlementType();
        }else if (OPEN_REPO.equalsIgnoreCase(columnId)) {
            return bean.getOpen();
        }else if (COUPONTYPE.equalsIgnoreCase(columnId)) {
            return bean.getCouponType();
        }else if (INDIREFE.equalsIgnoreCase(columnId)) {
            return bean.getRefernceIndex();
        }else if (RATECOUPON.equalsIgnoreCase(columnId)) {
            return bean.getRateCoupon();
        }else if (EFECTIVO_INICIAL.equalsIgnoreCase(columnId)) {
            return bean.getEffInicial();
        }else if (EFECTIVO_FINAL.equalsIgnoreCase(columnId)) {
            return bean.getEffFinal();
        }else if (NOM_NO_INDX.equalsIgnoreCase(columnId)) {
            return bean.getNomNoIndx();
        }else if (MARCA_INDX.equalsIgnoreCase(columnId)) {
            return bean.getMarcaIdx();
        }else if (ACTION.equalsIgnoreCase(columnId)) {
            return bean.getAction();
        }else if (ACTION_EFF_DATE.equalsIgnoreCase(columnId)) {
            return bean.getActionEffDate();
        }else {
            return super.getColumnValue(row, columnId, errors);
        }
    }

}
