package calypsox.tk.report;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.MarginCallDetailEntry;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallPosition;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class SecLendingPositionUtil {

    public static String PE_MEXICO = "PE-MEXICO";
    public static String MEXICO_SOURCESYSTEM = "MEXBS_CALYPSOCSA";

    /**
     * Generate {@link SecLendingSecurityPositionBean} from {@link MarginCallDetailEntry}
     * @param entry
     * @return
     */
    public static List<MarginCallPosition> getSecVsSecPositions(MarginCallEntry entry){
        ConcurrentLinkedQueue<MarginCallPosition> tempSecVsSecPositions = new ConcurrentLinkedQueue<>();
        List<MarginCallDetailEntry> secVsSecPos = getSecVsSecDetailEntry(entry);

        secVsSecPos.forEach(sec -> {
            Trade trade = getTrade(sec.getTradeId());
            List<Collateral> rightCollaterals = getRightCollaterals(trade);
            rightCollaterals.forEach(collateral -> {
                SecLendingSecurityPositionBean bean = new SecLendingSecurityPositionBean(entry);
                bean.init(trade,collateral);
                tempSecVsSecPositions.add(bean);
            });
        });
        return new ArrayList<>(tempSecVsSecPositions);
    }



    /**
     * Generate {@link SecLendingInventorySecurityBean} from {@link Trade}
     * @param marginCall
     * @return
     */
    public static List<InventorySecurityPosition> getSecVsSecInventorySecurityPosition(CollateralConfig marginCall, JDate valDate){
        ConcurrentLinkedQueue<InventorySecurityPosition> productPosition = new ConcurrentLinkedQueue<>();
        List<MarginCallDetailEntryDTO> marginCallDetailEntryDTOS = loadMarginCallDetailEntry(marginCall, valDate);

        if(!Util.isEmpty(marginCallDetailEntryDTOS)){
           marginCallDetailEntryDTOS.stream().parallel().forEach(dto ->{
               Trade trade = getTrade(dto.getTradeId());
               List<Collateral> rightCollaterals = getRightCollaterals(trade);
               rightCollaterals.forEach(collateral -> {
                   SecLendingInventorySecurityBean bean = new SecLendingInventorySecurityBean();
                   bean.init(trade,collateral,valDate);
                   productPosition.add(bean);
               });
           });
        }
        return new ArrayList<>(productPosition);
    }

    public static List<InventorySecurityPosition> getSecVsSecInventorySecurityPosition(MarginCallEntry marginCallEntry,JDate valDate){
        ConcurrentLinkedQueue<InventorySecurityPosition> productPosition = new ConcurrentLinkedQueue<>();
        if(Optional.ofNullable(marginCallEntry).isPresent()){
            List<MarginCallDetailEntry> secVsSecDetailEntry = getSecVsSecDetailEntry(marginCallEntry);
            secVsSecDetailEntry.stream().parallel().forEach(dto ->{
                Trade trade = getTrade(dto.getTradeId());
                List<Collateral> rightCollaterals = getRightCollaterals(trade);
                rightCollaterals.forEach(collateral -> {
                    SecLendingInventorySecurityBean bean = new SecLendingInventorySecurityBean();
                    bean.init(trade,collateral,valDate);
                    productPosition.add(bean);
                });
            });
        }
        return new ArrayList<>(productPosition);
    }


    private static List<MarginCallDetailEntryDTO> loadMarginCallDetailEntry(CollateralConfig marginCall, JDate valDate){
        StringBuilder sqlWhere = new StringBuilder();
        sqlWhere.append(" PRODUCT_DESC.PRODUCT_ID =  MARGIN_CALL_DETAIL_ENTRIES.PRODUCT_ID ");
        sqlWhere.append(" AND MARGIN_CALL_DETAIL_ENTRIES.MCC_ID="+marginCall.getId());
        sqlWhere.append(" AND MARGIN_CALL_DETAIL_ENTRIES.PRODUCT_TYPE='SecLending' ");
        sqlWhere.append(" AND PRODUCT_DESC.PRODUCT_SUB_TYPE = 'Sec Vs Sec' ");
        sqlWhere.append(" AND TRUNC(MARGIN_CALL_DETAIL_ENTRIES.PROCESS_DATETIME) = " + Util.date2SQLString(valDate));

        try {
            return ServiceRegistry.getDefault().getDashBoardServer().loadMarginCallDetailEntries(sqlWhere.toString(), Arrays.asList("MARGIN_CALL_DETAIL_ENTRIES", "PRODUCT_DESC"));
        } catch (CollateralServiceException e) {
            Log.error(SecLendingPositionUtil.class.getSimpleName(),"Error loading MarginCallDetailEntry for contract: " + marginCall.getId() + ": " + e.getCause());
        }
        return new ArrayList<>();
    }

    private static List<MarginCallDetailEntry> getSecVsSecDetailEntry(MarginCallEntry marginCallEntry){
        return marginCallEntry.getDetailEntries().stream()
                .filter(detail -> "Sec Vs Sec".equalsIgnoreCase((String) detail.getTradeDefinition().getValue("Product Subtype")))
                .collect(Collectors.toList());
    }

    private static List<Collateral> getRightCollaterals(Trade trade){
        SecLending secLendingProduct = Optional.ofNullable(trade).map(Trade::getProduct).filter(SecLending.class::isInstance).map(SecLending.class::cast).orElse(null);
        Vector<Collateral> rightCollaterals = Optional.ofNullable(secLendingProduct).map(SecLending::getRightCollaterals).orElse(new Vector<>());
        return new ArrayList<>(rightCollaterals);
    }

    /**
     * @param mcId
     * @return
     *         SELECT * FROM trade, product_desc,trade_keyword
     *         WHERE product_desc.product_id  = trade.product_id
     *         AND trade_keyword.trade_id = trade.trade_id
     *         AND trade.trade_status IN ('VERIFIED')
     *         AND product_desc.product_type IN ('SecLending')
     *         AND product_desc.product_sub_type LIKE 'Sec Vs Sec'
     *         AND trade_keyword.keyword_name = 'MARGIN_CALL_CONFIG_ID'
     *         AND trade_keyword.keyword_value = 44548801;
     */
    private static TradeArray loadSecVsSecTrades(int mcId){
        StringBuilder where = new StringBuilder();
        where.append(" product_desc.product_id  = trade.product_id ");
        where.append(" AND trade_keyword.trade_id = trade.trade_id");
        where.append(" AND trade.trade_status IN ('VERIFIED','PARTENON')"); //TODO only VERIFIED
        where.append(" AND product_desc.product_type IN ('SecLending')");
        where.append(" AND product_desc.product_sub_type LIKE 'Sec Vs Sec'");
        where.append(" AND trade_keyword.keyword_name = 'MARGIN_CALL_CONFIG_ID'");
        where.append(" AND trade_keyword.keyword_value = " + mcId);

        try {
            return DSConnection.getDefault().getRemoteTrade().getTrades("product_desc, trade_keyword", where.toString(), "trade.TRADE_ID DESC", null);
        } catch (CalypsoServiceException e) {
            Log.error(SecLendingPositionUtil.class.getSimpleName(),"Error loading trade: " + e.getCause());
        }
        return new TradeArray();
    }

    private static Trade getTrade(long tradeId){
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);// 41266002
        } catch (CalypsoServiceException e) {
            Log.error(SecLendingPositionUtil.class.getSimpleName(),"Error loading trade: " + e.getCause());
        }
        return null;
    }
}