package calypsox.balances;

import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.util.List;
import java.util.Vector;


/**
 * @author jriquell
 */
public class ProcessBalances {
    public static final String TYPE_CASH = "CASH";
    public static final String TYPE_SECURYTY = "SECURITY";
    private SantImportBalancesTableModel balancesTableModel;
    private String contract_name;
    private String type;
    private String nominal;
    private String isin_code;
    private String currency;
    private String value_date;


    public ProcessBalances(SantImportBalancesTableModel balancesTableModel) {
        this.balancesTableModel = balancesTableModel;
    }

    public SantImportBalancesTableModel start() {
        long idTrade;
        try {
            for (int i = 0; i < balancesTableModel.getRowCount(); i++) {
                this.contract_name = (String) balancesTableModel.getValueAt(i, SantImportBalancesTableModel.CONTRACT_NAME_COL_NUM);
                this.type = (String) balancesTableModel.getValueAt(i, SantImportBalancesTableModel.TYPE_COL_NUM);
                this.nominal = String.valueOf(balancesTableModel.getValueAt(i, SantImportBalancesTableModel.NOMINAL_COL_NUM));
                this.isin_code = (String) balancesTableModel.getValueAt(i, SantImportBalancesTableModel.ISIN_COL_NUM);
                this.currency = (String) balancesTableModel.getValueAt(i, SantImportBalancesTableModel.CURRENCY_COL_NUM);
                this.value_date = balancesTableModel.getValueAt(i, SantImportBalancesTableModel.VALUE_DATE_COL_NUM).toString();

                idTrade = 0;
                CollateralConfig contract = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigByCode(null, contract_name);
                Product produ = new MarginCall();
                Trade trade = new Trade();
                MarginCall mc = (MarginCall) produ;
                if (isCashType(type)) {
                    mc.setFlowType(TYPE_CASH);
                    mc.setCurrencyCash(currency);
                    mc.setPrincipal(Double.parseDouble(nominal));

                } else {
                    mc.setFlowType(TYPE_SECURYTY);
                    String where = "product_desc.PRODUCT_ID = product_sec_code.PRODUCT_ID "
                            + "AND product_sec_code.SEC_CODE = 'ISIN' "
                            + "AND product_sec_code.CODE_VALUE_UCASE = '" + "?" + "'";
                    List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(isin_code);
                    Vector<Product> bonds = DSConnection.getDefault().getRemoteProduct().getProducts("Bond", "product_sec_code", where, true, bindVariables);
                    Bond bond = (Bond) bonds.get(0);
                    mc.setSecurity(bond);
                    mc.setPrincipal(bond.getPrincipal());
                    trade.setAccrual(0.0);
                    trade.setQuantity(Double.parseDouble(nominal) / bond.getPrincipal());
                    trade.setTradePrice(1.0);
                }
                mc.setOrdererLeId(contract.getPoId());
                mc.setMarginCallId(contract.getId());
                trade.setCounterParty(contract.getLegalEntity());
                trade.setEnteredUser(DSConnection.getDefault().getUser());
                trade.setModifiedUser(DSConnection.getDefault().getUser());
                trade.setProduct(mc);
                trade.setBook(contract.getBook());
                trade.setAction(Action.NEW);
                trade.setStatus(new Status());
                trade.setTradeDate(new JDatetime());
                trade.setSettleDate(Util.stringToJDate(value_date));
                trade.setTradeCurrency(currency);
                idTrade = DSConnection.getDefault().getRemoteTrade().save(trade);
                if (idTrade != 0) {
                    this.balancesTableModel.setValueAt(i, SantImportBalancesTableModel.ID_TRADE_NUM, idTrade);
                }
            }

        } catch (CalypsoServiceException | CollateralServiceException e) {
            Log.error(this, e); //sonar purpose
        }

        return balancesTableModel;
    }


    private boolean isCashType(String type) {
        return type.equalsIgnoreCase(TYPE_CASH);
    }
}
