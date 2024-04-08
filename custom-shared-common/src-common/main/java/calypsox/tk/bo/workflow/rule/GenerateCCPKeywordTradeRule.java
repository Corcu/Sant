package calypsox.tk.bo.workflow.rule;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

/**
 * GenerateCCPKeywordTradeRule Add the BIC code of the contact of the legal entity to the trade keyword CCP
 *
 * @author Ruben Garcia
 */
public class GenerateCCPKeywordTradeRule implements WfTradeRule {

    /**
     * Name of the domain value where the Ctpy relation and the comment with
     * contact data are stored Role;ContactType
     */
    private static final String GENERATE_CCP_KW_CTPY_CODES = "GenerateCCPKWCtpyCodes";

    /**
     * Mx BANKCODESP trade KW name
     */
    private static final String MX_BANKCODESP = "Mx BANKCODESP";

    /**
     * Mx Electplatf trade KW name
     */
    private static final String MX_Electplatf = "Mx Electplatf";

    /**
     * MurexBilateralCounterparty trade KW name
     */
    private static final String MurexBilateralCounterparty = "MurexBilateralCounterparty";

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Trade Rule that updates the CCP trade keyword with the BIC value of the contact";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (trade != null && trade.getCounterParty() != null && !Util.isEmpty(trade.getCounterParty().getCode())
                && !Util.isEmpty(trade.getProductType()) && isBilateral(trade)) {
            String poCode = "ALL";

            if (trade.getBook() != null && trade.getBook().getLegalEntity() != null && !Util.isEmpty(trade.getBook().getLegalEntity().getCode())) {
                poCode = trade.getBook().getLegalEntity().getCode();
            }


            String productType = trade.getProductType();
            String config = LocalCache.getDomainValueComment(dsCon, GENERATE_CCP_KW_CTPY_CODES, trade.getCounterParty().getCode() + ";" + productType + ";" + poCode);

            if (Util.isEmpty(config)) {
                productType = "ALL";
                config = LocalCache.getDomainValueComment(dsCon, GENERATE_CCP_KW_CTPY_CODES, trade.getCounterParty().getCode() + ";" + productType + ";" + poCode);
            }

            if (Util.isEmpty(config)) {
                productType = trade.getProductType();
                poCode = "ALL";
                config = LocalCache.getDomainValueComment(dsCon, GENERATE_CCP_KW_CTPY_CODES, trade.getCounterParty().getCode() + ";" + productType + ";" + poCode);
            }

            if (Util.isEmpty(config)) {
                productType = "ALL";
                poCode = "ALL";
                config = LocalCache.getDomainValueComment(dsCon, GENERATE_CCP_KW_CTPY_CODES, trade.getCounterParty().getCode() + ";" + productType + ";" + poCode);
            }


            if (!Util.isEmpty(config)) {
                String[] splitConfig = config.split(";");
                if (splitConfig.length == 2) {
                    String role = splitConfig[0];
                    String contactType = splitConfig[1];
                    LegalEntity le = BOCache.getLegalEntity(dsCon, BOCache.getLegalEntityId(dsCon, trade.getCounterParty().getCode()));
                    int poId = -1;
                    if (!"ALL".equals(poCode)) {
                        poId = BOCache.getLegalEntityId(dsCon, poCode);
                    }
                    if (le != null) {
                        LEContact lec = BOCache.getContact(dsCon, role, le, contactType, productType, poId, JDate.getNow(), trade, null);
                        if (lec != null) {
                            String swiftCode = lec.getSwift();
                            if (!Util.isEmpty(swiftCode)) {
                                trade.addKeyword("CCP", swiftCode);
                            }
                        }
                    }


                }
            }else{
                //If it is not CounterParty in the configuration, we do not consider it CCP
                trade.addKeyword("CCP", "");
            }
        }else if(!isBilateral(trade)){
            trade.addKeyword("CCP", "");
        }
        return true;
    }

    /**
     * Check if is Bilateral Trade
     * Mx Bankcodesp and MurexBilateralCounterparty not empty or BTEC electplatf
     *
     * @param trade the current trade
     * @return true if is bilateral trade
     */
    private boolean isBilateral(Trade trade) {
        if (trade != null) {
            String bankcodesp = trade.getKeywordValue(MX_BANKCODESP);
            String electplatf = trade.getKeywordValue(MX_Electplatf);
            String bilateralCtpy = trade.getKeywordValue(MurexBilateralCounterparty);
            if (!Util.isEmpty(bankcodesp) && !Util.isEmpty(bilateralCtpy)) {
                return true;
            }
            return Util.isEmpty(bankcodesp) && Util.isEmpty(bilateralCtpy) && !Util.isEmpty(electplatf) && "BTEC".equalsIgnoreCase(electplatf);
        }
        return false;
    }
}
