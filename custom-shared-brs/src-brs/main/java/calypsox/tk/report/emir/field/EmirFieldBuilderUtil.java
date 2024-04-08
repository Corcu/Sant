package calypsox.tk.report.emir.field;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.util.LegalEntityAttributesCache;
import calypsox.tk.util.emir.*;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.*;
import com.calypso.tk.product.*;
import com.calypso.tk.product.flow.CashFlowSimple;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalAgreement;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class EmirFieldBuilderUtil {

  private static EmirFieldBuilderUtil instance = null;
  private static final List<String> contactTypes = Arrays.asList("Default","ALL");
  private EmirFieldBuilderUtil() {

  }

  public static EmirFieldBuilderUtil getInstance() {
    if (instance == null) {
      instance = new EmirFieldBuilderUtil();
    }

    return instance;
  }

  public String getLogicActionType(Trade trade) {
    return trade.getKeywordValue(
        EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_ACTION_TYPE);
  }

  public String getUnderlyingIdentificationType(Trade trade) {
    String rst = EmirSnapshotReduxConstants.NA;
    if (trade.getProduct()  instanceof PerformanceSwap) {
      PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();
      PerformanceSwapLeg pLeg =  getBondSecurityLeg(trade);
      if (pLeg != null) {
        if (pLeg.getLegConfig().equals(EmirSnapshotReduxConstants.LEG_SINGLE_ASSET)) {
          if (pLeg.getReferenceProduct() instanceof Basket) {
            rst = EmirSnapshotReduxConstants.BASKET;
          } else   {
            rst = EmirSnapshotReduxConstants.ISIN;
          }
        }
      }
    }
    return rst;
  }

  public String getLogicCollateralized(Trade trade) {

    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    int mc_contract_number = trade.getKeywordAsInt(EmirSnapshotReduxConstants.ATTR_MC_CONTRACT_NUMBER);
    CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mc_contract_number);
    if (config != null) {
       rst = config.getAdditionalField(EmirSnapshotReduxConstants.ATTR_EMIR_COLLATERAL_VALUE);
     }

    if (Util.isEmpty(rst)) {
      rst = EmirSnapshotReduxConstants.UNCOLLATERALIZED;
    }
    return rst;
  }

  public String getLogicConfirmType(Trade trade) {
    String confirmType = EmirSnapshotReduxConstants.NOT_CONFIRMED;
    if (isTradeMatched(trade)) {
        confirmType = EmirSnapshotReduxConstants.NON_ELECTRONIC;

        /*final MessageArray messages = MessagesCache.getInstance()
          .getMessages(trade.getLongId());

      for (int i = 0; i < messages.size(); ++i) {
        final BOMessage boMsg = messages.get(i);
       }
      */
    }
    return confirmType;
  }

  private Boolean isTradeMatched(Trade trade) {

    String matchingStatus = trade.getKeywordValue(SantanderUtil.TRADE_KEYWORD_MATCHING_STATUS);
    if (KeywordConstantsUtil.TRADE_KEYWORD_STATUS_MATCHED
        .equals(matchingStatus)) {
      return true;
    }
    return false;
  }

  public String getLogicCLEARINGSTATUS(Trade trade) {
    Vector<String> clearingHouses =  LocalCache.getDomainValues(DSConnection.getDefault(), EmirSnapshotReduxConstants.DV_EMIR_CLEARING_HOUSE);
    if (!Util.isEmpty(clearingHouses)) {
      if (clearingHouses.contains(trade.getCounterParty().getCode())) {
        return Boolean.toString(Boolean.TRUE).toUpperCase();
      }
    }
    return Boolean.toString(Boolean.FALSE).toUpperCase();
  }

  public boolean isCCP(Trade trade) {
    Vector<String> clearingHouses =  LocalCache.getDomainValues(DSConnection.getDefault(), EmirSnapshotReduxConstants.DV_EMIR_CLEARING_HOUSE);
    if (!Util.isEmpty(clearingHouses)) {
      return clearingHouses.contains(trade.getCounterParty().getCode());
    }
    return false;
  }

  /*
 "Nueva logica
 Se revisa en la pata de financiación
 Si el swap es flotante, e((CashFlowSimple) cf).getRate()nviamos el valor del campo ""1st Rate"" dividido entre 100. Si no tiene valor el campo ""1st Rate"", enviamos cero.
 Si es fijo, se coge el valor del tipo fijo de la pata de financiación y se divide entre 100. (formato ejemplo, se informará 0.03 para un rate con valor 3 en Calypso)"
 */
  public String getlogicFIXRATE(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    double rate = 0.0d;
    JDate eventDate = trade.getUpdatedTime().getJDate(TimeZone.getDefault());
    if (trade.getProduct()  instanceof PerformanceSwap) {
      SwapLeg swapLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
      if (swapLeg != null) {
        if (swapLeg.isFixed()) {
            if (!swapLeg.getFlows().isEmpty()) {
              CashFlow selectedCF = null;
              for (CashFlow cf : swapLeg.getFlows()) {
                // fisrt flow is in future - get it
                if (selectedCF == null
                        && cf.getDate().gte(eventDate)) {
                  selectedCF = cf;
                  break;
                }
                if (selectedCF != null)  {
                    if (selectedCF.getDate().lte(eventDate)
                          && cf.getDate().gte(eventDate)) {
                      break;
                    }
                }
                selectedCF = cf;
              }

              if (selectedCF != null
                      && selectedCF instanceof CashFlowSimple) {
                  rate = ((CashFlowSimple) selectedCF).getRate();
              }
            }
        } else {
          if (swapLeg.getFirstResetRate() != 0.0d) {
            rate = swapLeg.getFirstResetRate();
          }
        }
      }

      if (rate != 0.0) {
          rst = new BigDecimal(Math.abs(rate)/100).setScale(8, BigDecimal.ROUND_HALF_EVEN).toPlainString();
      }
    }
    return rst;
  }

  public SwapLeg getSwapLeg(Trade trade) {
    PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();
    SwapLeg swapLeg = null;
    if (perfSwap.getPrimaryLeg() instanceof SwapLeg)  {
      swapLeg = (SwapLeg) perfSwap.getPrimaryLeg();
    } else {
      swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
    }
    return swapLeg;
  }

  public PerformanceSwapLeg getBondSecurityLeg(Trade trade) {

    try {

        if (trade.getProduct()  instanceof PerformanceSwap) {

          PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();

          if (perfSwap.getPrimaryLeg() instanceof PerformanceSwapLeg ) {
            return (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
          }

          if (perfSwap.getSecondaryLeg()instanceof PerformanceSwapLeg ) {
            return (PerformanceSwapLeg) perfSwap.getSecondaryLeg();
          }

        }
    } catch  (Exception r) {
      r.printStackTrace();
    }
    return null;
  }


  public <T> Vector<T> castVector(final Class<? extends T> clazz,
      final Vector<?> c) {
    final Vector<T> r = new Vector<T>(c.size());
    for (final Object o : c) {
      r.add(clazz.cast(o));
    }
    return r;
  }

  private String getExchangedCurrencyPayValue(final LegalEntity legalEntity) {
    String value = legalEntity.getCode();

    final int leId = legalEntity.getId();
    final LegalEntityAttribute leiAttribute = LegalEntityAttributesCache
        .getInstance().getAttribute(0, leId,
            LegalEntityAttributesCache.ALL_ROLES,
            KeywordConstantsUtil.LE_ATTRIBUTE_LEI);
    if (leiAttribute != null
        && !Util.isEmpty(leiAttribute.getAttributeValue())) {
      value = leiAttribute.getAttributeValue();
    }

    return value;
  }

  // DDR v25

  private String getExchangedCurrencyPayValue(final LegalEntity legalEntity, final Trade trade, final boolean isCpty) {
    // If there was a change LEI and it was generated a CANCEL message.
    final String emirLeiValue = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LEI_VALUE);

    if (!Util.isEmpty(emirLeiValue) && isCpty) {
      return emirLeiValue;
    }

    return getExchangedCurrencyPayValue(legalEntity);
  }

  // DDR v25 - End

  /**
   * Round the amount by a given total length and decimal length
   *
   * @param value
   * @return
   */
  public String roundAmountByLength(final String value,
      final Integer totalLength, final Integer decLength) {

    final String string = value.replace("-", "");
    // Integer amount with sign
    final String integerWithSign = value.split(EmirSnapshotReduxConstants.DOT_CHAR_SPLIT)[0];
    // Integer and decimal amount to calculate lengths
    final String integer = string.split(EmirSnapshotReduxConstants.DOT_CHAR_SPLIT)[0];
    final String decimal = string.contains(EmirSnapshotReduxConstants.DOT_CHAR)
        ? string.split(EmirSnapshotReduxConstants.DOT_CHAR_SPLIT)[1] : "";

        String decValue = EmirSnapshotReduxConstants.EMPTY_SPACE;
        // If integer length plus decimal length is grater than total
        if ((integer.length() + decimal.length()) > totalLength) {
          // We round and trunk decimal amount
          // CAL_711
          decValue = roundDecimalAmount(EmirSnapshotReduxConstants.DOT_CHAR + decimal,
              Math.min(decLength, totalLength - integer.length()));
        } else {
          // We round to the appropriate decimal length
          decValue = roundDecimalAmount(EmirSnapshotReduxConstants.DOT_CHAR + decimal,
              Math.min(decLength, decimal.length()));
        }

        return integerWithSign + decValue;

  }

  /**
   * Round decimal amount to a given decimal digits
   *
   * @param decimal
   * @param decimalDigits
   * @return
   */
  protected String roundDecimalAmount(final String decimal,
      final Integer decimalDigits) {

    if (decimalDigits > 0) {

      final DecimalFormat df = new DecimalFormat();

      df.setMinimumFractionDigits(decimalDigits);
      df.setMaximumFractionDigits(decimalDigits);
      df.setMaximumIntegerDigits(0);
      df.setMinimumIntegerDigits(0);

      df.setGroupingUsed(false);

      // Half up rounding
      df.setRoundingMode(RoundingMode.HALF_UP);

      final DecimalFormatSymbols custom = new DecimalFormatSymbols();
      custom.setDecimalSeparator('.');
      df.setDecimalFormatSymbols(custom);

      return df.format(Double.valueOf(decimal));

    }
    return EmirSnapshotReduxConstants.EMPTY_SPACE;
  }

  public String getLogicExecutionAgentPartyValue2(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    // Look for GESTORA attribute value
    final String gestoraAttrValue = LegalEntityAttributesCache.getInstance()
        .getAttributeValue(trade, EmirSnapshotReduxConstants.LE_ATTRIBUTE_GESTORA,
            true);

    if (!Util.isEmpty(gestoraAttrValue)) {
      // GestoraAttrValue = cpty
      final LegalEntity le = BOCache.getLegalEntity(
          DSConnection.getDefault(), gestoraAttrValue);
      if (le != null) {
        final int leId = le.getId();
        final LegalEntityAttribute leiAttribute = LegalEntityAttributesCache
            .getInstance().getAttribute(0, leId,
                LegalEntityAttributesCache.ALL_ROLES,
                EmirSnapshotReduxConstants.LEI);
        if (leiAttribute != null) {
          rst = leiAttribute.getAttributeValue();
        }
      }
    }

    return rst;
  }

  public String getLogicMasterAgreementDate(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    final LegalAgreement la = MasterLegalAgreementsCache.getInstance()
        .getMasterLegalAgreement(trade);

    if (la != null) {
      final JDate aggrementDate = la.getAgreementDate();

      if (aggrementDate == null) {
        rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
      } else {
        final SimpleDateFormat sdfOld = new SimpleDateFormat("yyyy-MM-dd",
            Locale.getDefault());
        rst = sdfOld.format(
            aggrementDate.getJDatetime(TimeZone.getDefault()));
      }
    }

    return rst;
  }

  public String logicTRADEPARTYPREF2(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    final LegalEntity cpty = trade.getCounterParty();
    if (cpty != null) {
      final int leId = cpty.getId();
      final LegalEntityAttribute leiAttribute = LegalEntityAttributesCache
          .getInstance().getAttribute(0, leId,
              LegalEntityAttributesCache.ALL_ROLES,
              KeywordConstantsUtil.LE_ATTRIBUTE_LEI);
      if (leiAttribute != null
          && !Util.isEmpty(leiAttribute.getAttributeValue())) {
        rst = EmirSnapshotReduxConstants.LEI;
      } else {
        rst = EmirSnapshotReduxConstants.INTERNAL;
      }
    }

    return rst;
  }

  public String getLogicClearingDcoValue(Trade trade) {
    String rst = LegalEntityAttributesCache.getInstance().getAttributeValue(
        trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI, true);

    if (Util.isEmpty(rst)) {
      rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    }

    return rst;
  }

  public String getLogicExecutionVenueMicCode(Trade trade) {

    String result = EmirSnapshotReduxConstants.XXXX;
    String sTotv = trade.getKeywordValue(
            EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_PRODUCT_TOTV);

    if (EmirSnapshotReduxConstants.MAY_TRUE.toLowerCase().equals(sTotv)) {
      String micCode = trade.getKeywordValue(
              EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_MIC_CODE);
      if (Util.isEmpty(micCode)) {
        result = EmirSnapshotReduxConstants.XOFF;
      } else {
        result = micCode;
      }
    }
    return result;
  }

  /**
   * Return Legal Entity attribute from a given type.
   *
   * @param trade
   *            Trade
   * @param attributeType
   *            String
   * @param isCptyAttr
   *            boolean
   * @return String Attribute value from a given type.
   */
  public String getLegalEntityAttribute(final Trade trade,
                                        final String attributeType, final boolean isCptyAttr) {
    int leId = 0;
    final int poId = trade.getBook().getLegalEntity().getId();
    if (isCptyAttr) {
      leId = trade.getCounterParty().getEntityId();
    } else {
      leId = trade.getBook().getLegalEntity().getId();
    }

    String attrValue = null;

    LegalEntityAttribute attr = null;
    try {
      attr = DSConnection.getDefault().getRemoteReferenceData()
              .findAttribute(poId, leId, "ALL", attributeType);
      if (attr == null) {
        attr = DSConnection.getDefault().getRemoteReferenceData()
                .findAttribute(0, leId, "ALL", attributeType);

      }
    } catch (final RemoteException e) {
      Log.error("calypsox.tk.util.TemplateUtil", e);
    }
    if (attr != null) {
      attrValue = attr.getAttributeValue();
    }
    return attrValue;
  }

  public String getLogicINIPAYMENTAMOUNT(Trade trade) {

    String rst =  EmirSnapshotReduxConstants.EMPTY_SPACE;
    if (trade.getFees()!= null)  {
      Fee selectedfee = getPremiumFee(trade);
      if (selectedfee != null) {

        //rst = new BigDecimal(selectedfee.getAmount()).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
        decimalFormat.applyPattern("0.##");
        rst = decimalFormat.format(new Amount(Math.abs(selectedfee.getAmount()),2).get());

      }
    }
    return rst;
  }

  public Fee getPremiumFee(Trade trade) {

    JDate tradeEnteredDate = trade.getEnteredDate().getJDate(TimeZone.getDefault());
    JDate tradeDatePlus = trade.getEnteredDate().getJDate(TimeZone.getDefault()).addBusinessDays(3, Util.string2Vector("SYSTEM"));
    Fee selectedfee = null;
    Vector<Fee> fees = trade.getFees();
    if (!Util.isEmpty(fees)){
      for (Fee fee : fees) {
        if(fee.getType().toUpperCase().contains("PREMIUM") ) {
          if (fee.getKnownDate() != null
                  && fee.getKnownDate().equals(tradeEnteredDate)) {
              selectedfee = fee;
          } else if (fee.getFeeDate().gte(tradeEnteredDate)
                    && fee.getFeeDate().lte(tradeDatePlus)) {
               selectedfee = fee;
            }
        }
      }
    }
    return selectedfee;
  }

  public String getSwiftCode(LegalEntity le, List<String> roles) {
    for(String role : roles) {
      for(String contactType : contactTypes) {
        LEContact contact = BOCache.getContact(DSConnection.getDefault(), role, le, contactType, LEContact.ALL, 0);
        if(contact!=null && contact.getSwift()!=null)
          return contact.getSwift();
      }
    }
    return null;
  }

  public String getTRADEPARTY2THIPARTYVIEWIDTYPELogic(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    String strIntermCode = LegalEntityAttributesCache.getInstance().getAttributeValue(trade, EmirSnapshotReduxConstants.LE_EMIR_THIPARTY, true );
    if (!Util.isEmpty(strIntermCode)) {
      LegalEntity legalEntity = LegalEntitiesCache.getInstance().getLegalEntity(strIntermCode);

      final LegalEntityAttribute leiAttribute = LegalEntityAttributesCache
              .getInstance().getAttribute(0, legalEntity.getId(),
                      LegalEntityAttributesCache.ALL_ROLES,
                      EmirSnapshotReduxConstants.LEI);

      if (leiAttribute != null) {
        rst = EmirSnapshotReduxConstants.LEI;
      }
      else {
        String swiftCode = getSwiftCode(legalEntity, Arrays.asList("ALL"));
        if (!Util.isEmpty(swiftCode)) {
          rst = EmirSnapshotReduxConstants.SWIFTBIC;
        }
      }
    }
    return rst;
  }

  public String getTRADEPARTY2THIPARTYVIEWIDLogic(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    String strIntermCode = LegalEntityAttributesCache.getInstance().getAttributeValue(trade, EmirSnapshotReduxConstants.LE_EMIR_THIPARTY, true );
    if (!Util.isEmpty(strIntermCode)) {
      LegalEntity legalEntity = LegalEntitiesCache.getInstance().getLegalEntity(strIntermCode);

      final LegalEntityAttribute leiAttribute = LegalEntityAttributesCache
              .getInstance().getAttribute(0, legalEntity.getId(),
                      LegalEntityAttributesCache.ALL_ROLES,
                      EmirSnapshotReduxConstants.LEI);

      if (leiAttribute != null) {
        rst = leiAttribute.getAttributeValue();
      }
      else {
        String swiftCode = getSwiftCode(legalEntity, Arrays.asList("ALL"));
        if (!Util.isEmpty(swiftCode)) {
          rst = swiftCode;
        }
      }
    }
    return rst;
  }


  public String getlogicFLOATINGRATE(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    /*
    if (trade.getProduct()  instanceof PerformanceSwap) {
      SwapLeg swapLeg = getSwapLeg(trade);
      double rate = 0.0d;

      if (swapLeg.isFixed()) {
        rate = swapLeg.getFixedRate();
      } else {
        if (swapLeg.getFirstResetRate() != 0.0d) {
          rate = swapLeg.getFirstResetRate();
        }
      }
      rst = new BigDecimal(rate/100).setScale(8, BigDecimal.ROUND_HALF_UP).toPlainString();
    }
  */
    return rst;
    
  }


  public String getLogicEXECUTIONVENUE(Trade trade) {
    String rst = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_PLATFORM);
    if (Util.isEmpty(rst))  {
      rst = EmirSnapshotReduxConstants.OFF_FACILITY;
    }
    return rst.toString();
  }


  public String getLogicUNDERLYNGASSETTYPE(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    PerformanceSwapLeg pLeg = getBondSecurityLeg(trade);
    if (pLeg!= null) {
      if (pLeg.getLegConfig().equalsIgnoreCase(EmirSnapshotReduxConstants.LEG_SINGLE_ASSET)) {
        rst =  EmirSnapshotReduxConstants.ISIN;
      } else if (pLeg.getLegConfig().equalsIgnoreCase(EmirSnapshotReduxConstants.LEG_MANAGED)) {
        rst =  EmirSnapshotReduxConstants.BASKET;
      }
    }
    return rst;
  }

  public String getLogicBENEFICIARYIDPARTY2PREFIX(Trade trade) {
    String rst = EmirSnapshotReduxConstants.LEI;

    final String lei = LegalEntityAttributesCache.getInstance()
            .getAttributeValue(trade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI,
                    true);

    if (Util.isEmpty(lei)) {
      rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
    return rst;
  }

  public String getLogicTRADEPARTY1COLLATPORTFOLIO(Trade trade) {
    String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

    String strCollat = EmirFieldBuilderUtil.getInstance()
            .getLogicCollateralized(trade);

    if (!Util.isEmpty(strCollat)
            &&  (!EmirSnapshotReduxConstants.UNCOLLATERALIZED.equalsIgnoreCase(strCollat)))  {

      rst = EmirSnapshotReduxConstants.YES;

    }

    return rst;
  }

  public String getLogicTRADEPARTY1COUNTERPARTYSIDE(Trade trade) {
    String rst =  EmirSnapshotReduxConstants.SELL;
    if (trade.getQuantity() > 0) {
      rst =    EmirSnapshotReduxConstants.BUY;
    }
    return rst;
  }

  public String getMappedValueCouponFrequency(String emirCouponFreqValue, int idx) {
    String rst = LocalCache.getDomainValueComment(DSConnection.getDefault(), EmirSnapshotReduxConstants.DV_EMIR_FUND_PAYMENT_FREQUENCY, emirCouponFreqValue);
    if (!Util.isEmpty(rst)) {
      String[] split = rst.split(";");
      if (idx < split.length) {
        if (null != split[idx]) {
          return split[idx];
        }
      }
    }
    return EmirSnapshotReduxConstants.EMPTY_SPACE;
  }


  // Check and add UTI temporal
  /**
   *
   * @param trade
   * @return
   */
  public String getUtiTemporal(final Trade trade) {
    String utiTemporal = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_TEMP_UTI_TRADE_ID);
    return (!Util.isEmpty(utiTemporal)) ? utiTemporal : "";
  }


  public boolean hasTemporaryUTI(Trade trade) {
    final String utiTempTradeId = trade.getKeywordValue((EmirSnapshotReduxConstants.TRADE_KEYWORD_TEMP_UTI_TRADE_ID));
      if (!Util.isEmpty(utiTempTradeId)) {
        return true;
      }
      return false;
  }
}