package calypsox.tk.report;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

/**
 * @author acd
 */
public class RepoSMMDReportStyle extends TradeReportStyle {

    public static final String STATUS = "Status";
    public static final String PRTRY_TRADEID = "PrtryTradeid";
    public static final String CPTY_NAME = "CptyName";
    public static final String CPTY_LOCATION = "CptyLocation";
    public static final String TRADE_DATE = "TradeDate";
    public static final String SETTLEMENT_DATE = "SettlementDate";
    public static final String MATURITY_DATE = "MaturityDate";
    public static final String TXN_TYPE = "TxnType";
    public static final String NOMINAL = "Nominal";
    public static final String RATE_TYPE = "RateType";
    public static final String DEAL_RATE = "DealRate";
    public static final String BROKERED_DEAL = "BrokeredDeal";
    public static final String COLLATERAL_NOMINAL = "CollateralNominal";
    public static final String ISIN = "ISIN";
    public static final String COLLATERAL_CCY = "CollateralCcy";
    public static final String COLLATERAL_TYPE = "CollateralType";
    public static final String COLLATERAL_STATUS = "CollateralStatus";
    public static final String J_MIN = "CodigoJ";


    public static final String SECTOR = "Sector";
    public static final String SECTOR_1 = "Sector1";
    public static final String SECTOR_2 = "Sector2";
    public static final String SECTOR_3 = "Sector3";
    public static final String SECTOR_4 = "Sector4";
    public static final String SECTOR_5 = "Sector5";
    public static final String SECTOR_6 = "Sector6";
    public static final String SECTOR_7 = "Sector7";
    public static final String SECTOR_8 = "Sector8";
    public static final String SECTOR_9 = "Sector9";
    public static final String SECTOR_10 = "Sector10";
    public static final String SECTOR_11 = "Sector11";
    public static final String SECTOR_12 = "Sector12";
    public static final String SECTOR_13 = "Sector13";
    public static final String SECTOR_14 = "Sector14";

    public static final String HAIRCUT = "Haircut";
    public static final String INDEX = "INDEX";
    public static final String BASIS_POINT_SPREAD = "BasisPointSpread";

    public static final String TIME_EFX = "Time EFX";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        RepoSMMDBean bean = Optional.ofNullable(row.getProperty(RepoSMMDBean.class.getName())).map(o -> (RepoSMMDBean) o).orElse(new RepoSMMDBean());
        Trade trade = row.getProperty(Trade.class.getSimpleName());

        if (PRTRY_TRADEID.equalsIgnoreCase(columnId)) {
            return bean.prtryTradeid;
        }else if (STATUS.equalsIgnoreCase(columnId)) {
            return bean.status;
        }else if (CPTY_NAME.equalsIgnoreCase(columnId)) {
            return bean.cptyName;
        }else if (TRADE_DATE.equalsIgnoreCase(columnId)) {
            return bean.tradeDate;
        }else if (SETTLEMENT_DATE.equalsIgnoreCase(columnId)) {
            return bean.settlementDate;
        }else if (CPTY_LOCATION.equalsIgnoreCase(columnId)) {
            return bean.cptyLocation;
        }else if (TXN_TYPE.equalsIgnoreCase(columnId)) {
            return bean.txnType;
        }else if (NOMINAL.equalsIgnoreCase(columnId)) {
            return bean.cashPrincipal;
        }else if (RATE_TYPE.equalsIgnoreCase(columnId)) {
            return bean.rateType;
        }else if (DEAL_RATE.equalsIgnoreCase(columnId)) {
            return bean.dealRate;
        }else if (BROKERED_DEAL.equalsIgnoreCase(columnId)) {
            return bean.brokeredDeal;
        }else if (COLLATERAL_CCY.equalsIgnoreCase(columnId)) {
            return bean.collateralCCy;
        }else if (COLLATERAL_NOMINAL.equalsIgnoreCase(columnId)) {
            return bean.secValue;
        }else if (COLLATERAL_TYPE.equalsIgnoreCase(columnId)) {
            return null;
        }else if (COLLATERAL_STATUS.equalsIgnoreCase(columnId)) {
            return null;
        }else if (SECTOR.equalsIgnoreCase(columnId)) {
            return null;
       } else if (HAIRCUT.equalsIgnoreCase(columnId)) {
            /*
            To align HC sign with Mx
            Reverse/Give        -> 2.00 (positive)
            Reverse/Receive     -> -2.00 (negative)
            Repo/Receive        -> 2.00 (positive)
            Repo/Give           -> -2.00 (negative)
            */

           String hcDirection = (String) super.getColumnValue(row, "Sec. Hc/Mg Direction", errors);
           if (trade.getQuantity() < 0) //Reverse
               return  "Receive".equals(hcDirection)?"-" + bean.haircutValue:bean.haircutValue;
           else                         //Repo
               return  "Give".equals(hcDirection)?"-" + bean.haircutValue:bean.haircutValue;
        }else if (INDEX.equalsIgnoreCase(columnId)) {
            return bean.index;
        }else if (BASIS_POINT_SPREAD.equalsIgnoreCase(columnId)) {
            return bean.basisPoint;
        }else if (MATURITY_DATE.equalsIgnoreCase(columnId)) {
            return bean.maturityDate;
        }else if (J_MIN.equalsIgnoreCase(columnId)) {
            if(bean.showJMin){
                return super.getColumnValue(row, "CounterParty.Attribute.J_MINORISTA", errors);
            }
            return "";
        }else if("COLL_PRD_CODE.ISIN".equalsIgnoreCase(columnId)){
            if(bean.isPLedge){
                return super.getColumnValue(row, "PRODUCT_CODE.ISIN", errors);
            }else {
                return super.getColumnValue(row, "COLL_PRD_CODE.ISIN", errors);
            }
        } else if (TIME_EFX.equalsIgnoreCase(columnId)) {
            if(Optional.ofNullable(trade).isPresent()){
                SimpleDateFormat sdf;
                String mx_eFx_time = trade.getKeywordValue("Mx EFx Time");
                String mx_computerTime = trade.getKeywordValue("Mx ComputerTime");
                try {
                    if(!Util.isEmpty(mx_eFx_time)){
                        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        final java.util.Date dateObj = sdf.parse(mx_eFx_time);
                        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dateObj);

                    }else if(!Util.isEmpty(mx_computerTime)){
                        String currentDate = JDate.getNow().toString()+"T"+mx_computerTime;
                        sdf = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm:ss");
                        final java.util.Date dateObj = sdf.parse(currentDate);
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        return sdf.format(dateObj);
                    }
                }catch (Exception e){
                    Log.error(this.getClass().getSimpleName(),"Error: " + e + " - Date:" + mx_eFx_time + " - Hour: " + mx_computerTime);
                }
            }
            return "";
        } else {
            return super.getColumnValue(row, columnId, errors);
        }
    }
}
