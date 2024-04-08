package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.core.WorkflowUtil;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.util.*;

public class SantSetRIGTradeRule implements WfTradeRule {
    /**
     * The fixed description information returned by getDescription method.
     */
    public static final String ARG_DESCRIPTION = "Set the RIG the reference that relates Intragroup operations";

    private static final String RIG_PREFIX = "G";
    private static final String TRADE_KEYWORD_BONDFORWARD = "BondForward";


    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.WfTradeRule#getDescription()
     */
    @Override
    public String getDescription() {
        return ARG_DESCRIPTION;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.WfTradeRule#check(com.calypso.tk.bo. TaskWorkflowConfig,
     * com.calypso.tk.core.Trade, com.calypso.tk.core.Trade, java.util.Vector,
     * com.calypso.tk.service.DSConnection, java.util.Vector, com.calypso.tk.bo.Task,
     * java.lang.Object, java.util.Vector)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade newTrade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon, final Vector events) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.WfTradeRule#update(com.calypso.tk.bo. TaskWorkflowConfig,
     * com.calypso.tk.core.Trade, com.calypso.tk.core.Trade, java.util.Vector,
     * com.calypso.tk.service.DSConnection, java.util.Vector, com.calypso.tk.bo.Task,
     * java.lang.Object, java.util.Vector)
     */
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade newTrade, final Trade oldTrade, final Vector messages,
                          final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon, final Vector events) {

        Log.debug(this, "SantSetRIGDealHubTradeRule.update Start");

        // change CAL_INT_019
        // If the rig comes from murex we don't do anything
        final String rigValue = newTrade.getKeywordValue(KeywordConstantsUtil.KEYWORD_RIG);
        List<String> isIntragroup = Arrays.asList("S", "YES");
        if (rigValue == null) {

            try {

                if (Optional.ofNullable(newTrade.getMirrorBook()).isPresent()) {
                    return true;
                }

                LegalEntityAttribute intragroup = BOCache.getLegalEntityAttributes(dsCon, newTrade.getCounterParty().getId()).stream()
                        .filter(s -> s.getAttributeType().equalsIgnoreCase("INTRAGROUP")).findFirst().orElse(new LegalEntityAttribute());

                if (Util.isEmpty(intragroup.getAttributeValue()) || !isIntragroup.contains(intragroup.getAttributeValue())) {
                    Log.debug(this,
                            "Trade not INTRAGRUPO --> SantSetRIGTradeRule.update End");
                    return true;

                }

                Log.debug(this, "Generate RIG by Calypso");

                final String rigReference = generateRIG(newTrade);
                newTrade.addKeyword(TradeInterfaceUtils.TRADE_KWD_RIG_CODE, rigReference);

            } catch (final Exception e) {
                Log.error(this, "SantSetRIGTradeRule Exception", e);
                final BOException ev = new BOException(newTrade.getLongId(), "SantSetRIGTradeRule", "Error :" + e.getMessage());
                ev.setType(BOException.EXCEPTION);
                excps.addElement(ev);
                return false;
            }
        }
        Log.debug(this, "SantSetRIGDealHubTradeRule.update End");
        return true;
    }


    /**
     * Generate RIG reference by checking trade keywords and Murex references.
     *
     * @param trade the trade
     * @return the string
     */
    public String generateRIG(final Trade trade) {
        StringBuilder res = new StringBuilder(RIG_PREFIX);
        res.append("CAL");
        if (trade.getProduct() instanceof Bond){
            res.append(WorkflowUtil.XLATED_PRODUCT_CODE.get(trade.getProductType() + isBondForward(trade)));
        } else
            res.append(WorkflowUtil.XLATED_PRODUCT_CODE.get(trade.getProductType()));
        res.append(trade.getLongId());

        return res.toString();
    }

    /**
     *
     * @param trade
     * @return
     */
    private boolean isBondForward(Trade trade){
        return Optional.ofNullable(trade).map(t->t.getKeywordValue(TRADE_KEYWORD_BONDFORWARD))
                .map(Boolean::parseBoolean).orElse(false);
    }

    /*   *//**
     * Define is Calypso is reponsable of generation the RIG reference.
     *
     * @param trade the trade
     * @param dsCon the ds con
     * @return true, if is calypso generating rig
     * @throws Exception the exception
     *//*
        protected boolean isCalypsoGeneratingRIG ( final Trade trade, final DSConnection dsCon) throws Exception {
            boolean res = false;

            final LegalEntity po = trade.getBook().getLegalEntity();
            final LegalEntity cPty = trade.getCounterParty();
            final String tradeLogInfo = "Trade: " + trade.getLongId() + " - ";

            // 1. Check if PO and CPTY are in RIG Table
            // 2. If not, as normal.
            // 3. If yes, poCargabal or cptyCargabal or both, is 00001000 (then,
            // parse to integer).
            // 4. Compare
            // 4.1. If both are 00001000, use the RIG table order to select the
            // priority.

            boolean isPoCargabalValue = true;
            boolean isCptyCargabalValue = true;

            int poCargabal = Integer.valueOf(S_CARGABAL_VALUE);
            int cPtyCargabal = Integer.valueOf(S_CARGABAL_VALUE);

            final int legEntityId = trade.getBook().getLegalEntity().getId();
            final int counterpartyId = trade.getCounterParty().getId();

            final int poOrder = getGlcsOrder(po);
            Log.debug(this, tradeLogInfo + " PO table value: " + poOrder);
            final int cPtyOrder = getGlcsOrder(cPty);
            Log.debug(this, tradeLogInfo + "Cpty table value: " + cPtyOrder);

            if (poOrder < 0) {
                isPoCargabalValue = false;
                poCargabal = getCargabalByAttribute(dsCon, legEntityId, po, trade);
                Log.debug(this, tradeLogInfo + "PO cargabal: " + poCargabal);
            }

            if (cPtyOrder < 0) {
                isCptyCargabalValue = false;
                cPtyCargabal = getCargabalByAttribute(dsCon, counterpartyId, cPty, trade);
                Log.debug(this, tradeLogInfo + "Cpty cargabal: " + cPtyCargabal);
            }

            if (poCargabal < cPtyCargabal) {
                res = true;
            } else if (poCargabal > cPtyCargabal) {
                res = false;
            } else {// poCargabal == cPtyCargabal

                int poPrioridad = 0;
                int cPtyPrioridad = 0;

                if (isPoCargabalValue && isCptyCargabalValue) {
                    poPrioridad = poOrder;
                    cPtyPrioridad = cPtyOrder;
                    Log.debug(this, tradeLogInfo + "Both in table.");
                } else {
                    poPrioridad = getPriorityByAttribute(dsCon, legEntityId, po, trade);
                    cPtyPrioridad = getPriorityByAttribute(dsCon, counterpartyId, cPty, trade);
                    Log.debug(this, tradeLogInfo + "One o both out of table.");
                }
                res = (poPrioridad <= cPtyPrioridad) ? true : false;
                Log.debug(this, tradeLogInfo + "Both cptys in table. PoPriority: " + poPrioridad + ". CptyPriority: "
                        + cPtyPrioridad);
            }
            Log.debug(this, tradeLogInfo + "Generation of RIG" + String.valueOf(res));
            return res;
        }*/

    /*
     */
/**
 * Gets the number from attribute.
 *
 * @param attributeV the attribute v
 * @param leAttribute the le attribute
 * @param leName the le name
 * @return the number from attribute
 * @throws Exception the exception
 *//*

        @SuppressWarnings({"rawtypes", "unchecked"})
        protected int getNumberFromAttribute ( final Vector attributeV, final String leAttribute, final String leName,
        final Trade newTrade)
            throws Exception {
            int res = 0;
            final LegalEntity po = newTrade.getBook().getLegalEntity();

            try {
                res = Integer.valueOf(SantanderUtil.getInstance().getLEAttributeValue(attributeV, leAttribute, po));
            } catch (final Throwable t) {
                final Exception ex = new Exception("Unable to convert the Legal Entity (" + leName + ") attribute " + leAttribute
                        + " to a number", t);
                throw ex;
            }

            return res;
        }
*/
    /*
     *//**
     * Get Priority from LegalEntity Attributes
     *
     * @param dsCon
     * @param id
     * @param legalEntity
     * @return
     * @throws Exception
     *//*
        @SuppressWarnings("rawtypes")
        protected int getPriorityByAttribute ( final DSConnection dsCon, final int id, final LegalEntity legalEntity,
        Trade newTrade)
            throws Exception {
            final Vector attributes = BOCache.getLegalEntityAttributes(dsCon, id);
            final int cargabalValue = getNumberFromAttribute(attributes, KeywordConstantsUtil.LE_ATTRIBUTE_PRIORIDAD,
                    legalEntity.getCode(), newTrade);

            return cargabalValue;
        }*/

    /*  *//**
     * Get Cargabal value from LegalEntity Attributes
     *
     * @param dsCon
     * @param id
     * @param legalEntity
     * @return
     * @throws Exception
     *//*
        @SuppressWarnings("rawtypes")
        protected int getCargabalByAttribute ( final DSConnection dsCon, final int id, final LegalEntity legalEntity,
        Trade trade)
            throws Exception {
            final Vector attributes = BOCache.getLegalEntityAttributes(dsCon, id);
            final int cargabalValue = getNumberFromAttribute(attributes, KeywordConstantsUtil.LE_ATTRIBUTE_CARGABAL,
                    legalEntity.getCode(), trade);

            return cargabalValue;
        }*/

    /*  *//**
     * Get order from DomainValue GLCS table
     *
     * @param legalEntity
     * @return
     *//*
        private int getGlcsOrder ( final LegalEntity legalEntity){

            Integer order = new Integer(-1);
            final String leCode = legalEntity.getCode();
            final String sOrder = LocalCache.getDomainValueComment(DSConnection.getDefault(), RIG_GLCS_TABLE, leCode);

            if ((sOrder != null) && !sOrder.isEmpty()) {
                order = Integer.valueOf(sOrder);
            }

            return order.intValue();
        }*/


}
