package calypsox.tk.collateral.marginCall.mapper;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public abstract class AbstractExternalMarginCallMapper implements ExternalMarginCallMapper {

  private static final String LOAN = "Loan";
  private static final String TRADE_KEYWORD_SLB_MX = "SLB_MX";
  private static final String S_TRUE = "true";
  protected ExternalMarginCallImportContext context = null;

  /** */
  public AbstractExternalMarginCallMapper(ExternalMarginCallImportContext context) {
    this.context = context;
  }

  @Override
  public Trade mapMarginCallTrade(
      ExternalMarginCallBean mcBean, List<MarginCallImportErrorBean> messages) throws Exception {

    // The validation, also it creates the MarginCall object and sets it to
    // ExternalMarginCallBean
    if (!isValidMarginCall(mcBean, null, messages)) {
      return null;
    }

    // Create the Trade
    Trade trade = createTrade(mcBean);
    return trade;
  }

  /**
   * Create a new trade.
   *
   * @param mcBean
   * @return
   */
  private Trade createTrade(final ExternalMarginCallBean mcBean) {
    Trade trade = null;

    final CollateralConfig cc = mcBean.getCollateralConfig();

    // Get the MarginCall
    final MarginCall mc = mcBean.getMarginCall();

    final String cp = mcBean.getCounterparty();
    final LegalEntity counterparty = BOCache.getLegalEntity(DSConnection.getDefault(), cp);

    final String direction = mcBean.getCollateralDirection();

    final String currency = mcBean.getAmountCcy();

    final Date tradeDate = mcBean.getTradeDate();
    final JDate jDate = JDate.valueOf(tradeDate);
    final JDatetime jTradeDate = jDate.getJDatetime(TimeZone.getDefault());

    final Date valueDate = mcBean.getValueDate();
    final JDate jValueDate = JDate.valueOf(valueDate);

    final String closingPrice = mcBean.getClosingPrice();

    final String boSystem = mcBean.getAttributes().get(PDVUtil.COLLAT_FO_SYSTEM_FIELD);
    final String frontId = mcBean.getAttributes().get(PDVUtil.COLLAT_NUM_FRONT_ID_FIELD);
    final String collatId = mcBean.getAttributes().get(PDVUtil.COLLAT_COLLAT_ID_FIELD);

    if (mc.getPrincipal() != 0.0) {
      trade = new Trade();
      trade.setAction(Action.NEW);
      trade.setStatus(Status.S_NONE);
      trade.setProduct(mc);
      trade.setCounterParty(counterparty);
      trade.setBook(cc.getBook());
      trade.setTradeCurrency(currency);
      trade.setSettleCurrency(currency);

      trade.setTradeDate(jTradeDate);
      trade.setSettleDate(jValueDate);
      trade.setTraderName("NONE");
      trade.setSalesPerson("NONE");

      int quantity = 0;
      quantity = (LOAN.equalsIgnoreCase(direction)) ? -1 : 1;
      trade.setQuantity(quantity);

      // Price - DirtyPrice
      double dirtyPrice = 0.0;
      Product product = mc.getSecurity();
      try {
        dirtyPrice = CollateralUtilities.parseStringAmountToDouble(closingPrice);
      } catch (NumberFormatException e) {
        Log.error(this, e);
        return null;
      }

      if (dirtyPrice != 0.0 && product instanceof Bond) {
        trade.setTradePrice(dirtyPrice / 100);
      } else {
        // If is not Bond or dirtyPrice = 0.0
        trade.setTradePrice(dirtyPrice);
      }

      // Add keywords
      trade.addKeyword(TRADE_KEYWORD_SLB_MX, S_TRUE);
      trade.addKeywordAsLong(CollateralStaticAttributes.MC_CONTRACT_NUMBER, mc.getLinkedLongId());
      trade.addKeyword("COLLAT_ID", collatId);
      trade.addKeyword("NUM_FRONT_ID", frontId);
      trade.addKeyword("FO_SYSTEM", boSystem);
    }

    return trade;
  }
}
