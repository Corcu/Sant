package calypsox.tk.bo.cremapping;

import calypsox.tk.bo.cremapping.event.*;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.SimpleTransfer;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;

/**
 * @author acd
 */
public class BOCreMappingFactory {

    private static BOCreMappingFactory instance = new BOCreMappingFactory();
    
    
    private static final String SEC = "SECURITY";
    private static final String CASH = "COLLATERAL";


    public synchronized static final BOCreMappingFactory getFactory() {

        if (instance == null) {
            instance = new BOCreMappingFactory();
        }

        return instance;
    }

    /**
     *
     * Only to JUnit. You can use that to replace the instance with a mockito
     * instance
     */
    public static void setInstance(final BOCreMappingFactory mockInstance) {
        instance = mockInstance;
        throw new UnsupportedOperationException(
                "This should be used just for unit testing!!! If this is your case, use TestUtil.getMockedBOCreMappingFactory() and remember to use TestUtil.setNullBOCreMappingFactory() after!!!");
    }

    /**
     * @param boCre
     * @param trade
     * @return Custom BOCre event class (COT, COT_REV, CST, CST_VERIFIED, ACCRUAL)
     */
    public SantBOCre getCreType(final BOCre boCre, Trade trade) {
        final String creEventType = boCre.getEventType();
        final String originalEventType = boCre.getOriginalEventType();

        //Netting
        if(BOCreConstantes.CST_NET.equalsIgnoreCase(creEventType)){
            if(null!=trade){
                if(trade.getProduct() instanceof Repo){
                    return new BOCreRepoCST_NET(boCre,trade);
                }else if(trade.getProduct() instanceof SecLending){
                    return new BOCreSecLendingCST_NETTING(boCre, trade);
                }else if(trade.getProduct() instanceof Equity){
                    return new BOCreEquityCST_NET(boCre, trade);
                }else if(trade.getProduct() instanceof CA){
                    return new BOCreCADividendCST_NET(boCre, trade);
                }
            }
            if(checkPairOffNettedTransfer(boCre)){
                return new BOCreSecLendingCST_NETTING(boCre, trade);
            }
            return new BOCreMarginCallCST_NETTING(boCre, trade);

        }else if(BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)){
            if(checkPairOffNettedTransfer(boCre) && trade.getProduct() instanceof CustomerTransfer){
                return new BOCreCustomerXferPairOffCST_UNNET(boCre, trade);
            }
        }

        if(BOCreConstantes.MARGIN_SECURITY_POSITION.equalsIgnoreCase(creEventType)){
            return new BOCreMarginCallSEC_POS(boCre, trade);
        }

        if (BOCreConstantes.POSITION_SHORT.equalsIgnoreCase(creEventType) ||
                BOCreConstantes.POSITION_LONG.equalsIgnoreCase(creEventType)) {
            return new BOCreEquityPOSITION_SHORTLONG(boCre, trade);
        }

        if (BOCreConstantes.UNREALIZED_PL.equalsIgnoreCase(creEventType)) {
            return new BOCreEquityUNREALIZED_PL(boCre, trade);
        }

        if ( null != trade && trade.getProduct() != null ) {
            if (trade.getProduct() instanceof MarginCall) {
                if (BOCreConstantes.COT.equalsIgnoreCase(creEventType)) {
                    return new BOCreMarginCall(boCre, trade);
                } else if (BOCreConstantes.COT_REV.equalsIgnoreCase(creEventType)) {
                    return new BOCreMarginCall(boCre, trade);
                } else if (BOCreConstantes.CST.equalsIgnoreCase(creEventType)) {
                    return new BOCreMarginCallCST(boCre, trade);
                } else if (BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)) {
                    return new BOCreMarginCallCST_UNNET(boCre, trade);
                } else if (BOCreConstantes.INTEREST.equalsIgnoreCase(creEventType)) {
                    return new BOCreMarginCallINTEREST(boCre, trade);
                }
            } else if (trade.getProduct() instanceof CustomerTransfer) {
                if (BOCreConstantes.CST.equalsIgnoreCase(creEventType)) {
                    return new BOCreCustomerTransferCST(boCre, trade);
                } else if (BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)) {
                    return new BOCreCustomerTransferCST_UNNET(boCre, trade);
                }
            } else if (trade.getProduct() instanceof SecLending) {
                if (BOCreConstantes.COT.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLending(boCre, trade);
                } else if (BOCreConstantes.COT_REV.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLending(boCre, trade);
                } else if (BOCreConstantes.PRINCIPAL.equalsIgnoreCase(creEventType)) {

                    return new BOCreSecLendingPRINCIPAL(boCre, trade);
                } else if (BOCreConstantes.MTM_FULL.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLendingMTM_FULL(boCre, trade);
                } else if (BOCreConstantes.CST_FAILED.equalsIgnoreCase(creEventType)
                        || BOCreConstantes.CST.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLendingCST(boCre, trade);
                }else if (BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLendingCST_UNNET(boCre, trade);
                } else if (BOCreConstantes.FEE_ACCRUAL.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLendingFEE_ACCRUAL(boCre, trade);
                } else if (BOCreConstantes.ADJUSTMENT_FEE.equalsIgnoreCase(creEventType) ||
                        BOCreConstantes.ADJUSTMENT_RF_FEE.equalsIgnoreCase(creEventType) ||
                        BOCreConstantes.ADJUSTMENT_RV_FEE.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLendingADJUST_FEE(boCre, trade);
                } else if (BOCreConstantes.SECLENDING_FEE.equalsIgnoreCase(creEventType) ||
                        BOCreConstantes.SECLENDING_FEE_FAILED.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLendingSECLENDING_FEE(boCre, trade);
                }else if (BOCreConstantes.COLLATERAL.equalsIgnoreCase(creEventType)) {
                    return new BOCreSecLendingCOLLATERAL(boCre, trade);
                }
            } else if (trade.getProduct() instanceof InterestBearing) {
                if (BOCreConstantes.ACCRUAL.equalsIgnoreCase(creEventType)) {
                    return new BOCreInterestBearingACCRUAL(boCre, trade);

                } else if (BOCreConstantes.CST_VERIFIED.equalsIgnoreCase(creEventType)) {
                    return new BOCreInterestBearingCST_VERIFIED(boCre, trade);
                }
            } else if (trade.getProduct() instanceof CA) {
                String subtype = null != trade.getProduct() ? trade.getProduct().getSubType() : "";
                if ("AMORTIZATION".equalsIgnoreCase(subtype) || "INTEREST".equalsIgnoreCase(subtype) || "REDEMPTION".equalsIgnoreCase(subtype)) {
                    if(BOCreConstantes.CST_SETTLED.equalsIgnoreCase(creEventType) ||
                            BOCreConstantes.CST_VERIFIED.equalsIgnoreCase(creEventType)){
                        return new BOCreCACuponAmortCST(boCre, trade);
                    } else if(BOCreConstantes.CST_FAILED.equalsIgnoreCase(creEventType)){
                        return new BOCreCACuponAmortCST_FAILED(boCre, trade);
                    } else if(BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendCST_UNNET(boCre, trade);
                    } else if(BOCreConstantes.NET_WTHTAX.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendNET_WTHTAX(boCre, trade);
                    } else if(BOCreConstantes.RECLAIM_TAX.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendRECLAIM_TAX(boCre, trade);
                    } else if(BOCreConstantes.WRITE_OFF.equals(creEventType)){
                        return new BOCreCADividendWRITE_OFF(boCre, trade);
                    }
                } else if ("DIVIDEND".equalsIgnoreCase(subtype)) {
                    if(BOCreConstantes.CST_SETTLED.equalsIgnoreCase(creEventType) ||
                            BOCreConstantes.CST_FAILED.equalsIgnoreCase(creEventType)){
                        return "RECLAIM_TAX".equalsIgnoreCase(boCre.getDescription()) ?
                                new BOCreCADividendCST_RECLAIM_TAX(boCre, trade) : new BOCreCADividendCST(boCre, trade);
                    } else if(BOCreConstantes.FULL_DIVIDEND_XD.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendFULL_DIVIDEND_XD(boCre, trade);
                    } else if(BOCreConstantes.DIVIDEND.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendDIVIDEND(boCre, trade);
                    } else if(BOCreConstantes.NET_WTHTAX.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendNET_WTHTAX(boCre, trade);
                    } else if(BOCreConstantes.RECLAIM_TAX.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendRECLAIM_TAX(boCre, trade);
                    } else if(BOCreConstantes.DIV_CLAIM_PL.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendDIV_CLAIM_PL(boCre, trade);
                    } else if(BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)){
                        return new BOCreCADividendCST_UNNET(boCre, trade);
                    } else if(BOCreConstantes.WRITE_OFF.equals(creEventType)){
                        return new BOCreCADividendWRITE_OFF(boCre, trade);
                    }
                }
            } else if (trade.getProduct() instanceof Equity) {
                if (BOCreConstantes.COT.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityCOT(boCre, trade);
                } else if (BOCreConstantes.COT_REV.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityCOT_REV(boCre, trade);
                } else if (BOCreConstantes.NOM_FULL.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityNOM_FULL(boCre, trade);
                } else if (BOCreConstantes.NOM_FULL_REV.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityNOM_FULL_REV(boCre, trade);
                } else if (BOCreConstantes.REALIZED_PL.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityREALIZED_PL(boCre, trade);
                } else if (BOCreConstantes.MTM_FULL.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityPOSITION(boCre, trade);
                } else if (BOCreConstantes.CST_SETTLED.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityCST_SETTLED(boCre, trade);
                } else if (BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityCST_UNNET(boCre, trade);
                } else if (BOCreConstantes.CST_FAILED.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityCST_FAILED(boCre, trade);
                } else if (BOCreConstantes.ADDITIONAL_FEE.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityADDITIONAL_FEE(boCre, trade);
                } else if (BOCreConstantes.WRITE_OFF.equalsIgnoreCase(creEventType)) {
                    return new BOCreEquityWRITE_OFF(boCre, trade);
                }
            }else if (trade.getProduct() instanceof Repo) {
                if (BOCreConstantes.COT.equalsIgnoreCase(creEventType) ||
                        BOCreConstantes.COT_REV.equalsIgnoreCase(creEventType)) {
                    return new BOCreRepo(boCre,trade);
                }else if(BOCreConstantes.NOMINAL.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoNOMINAL(boCre,trade);
                }else if(BOCreConstantes.NOMINAL_REV.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoNOMINAL_REV(boCre,trade);
                }else if(BOCreConstantes.PRINCIPAL.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoPRINCIPAL(boCre,trade);
                }else if(BOCreConstantes.ACCRUAL.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoACCRUAL(boCre,trade);
                }else if(BOCreConstantes.MTM_NET.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoMTM_NET(boCre,trade);
                }else if(BOCreConstantes.CST.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoCST(boCre,trade);
                }else if(BOCreConstantes.CST_FAILED.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoCST_FAILED(boCre,trade);
                }else if(BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoCST_UNNET(boCre,trade);
                }else if(BOCreConstantes.INTEREST.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoINTEREST(boCre,trade);
     //           }else if(BOCreConstantes.WRITE_OFF.equalsIgnoreCase(creEventType)){
      //              return new BOCreRepoWRITE_OFF(boCre,trade);
                }else if(BOCreConstantes.RL_PRINCIPAL.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoRL_PRINCIPAL(boCre,trade);
                }else if(BOCreConstantes.RL_INTEREST.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoRL_INTEREST(boCre,trade);
                }else if(BOCreConstantes.PRINCIPAL_START.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoPRINCIPAL_START(boCre,trade);
                }else if(BOCreConstantes.CST_S_SETTLED.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoCST_S_SETTLED(boCre,trade);
                }else if(BOCreConstantes.REPO_CST_NET_S_SETTLED.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoCST_NET_S_SETTLED(boCre,trade);
                }else if(BOCreConstantes.COLL_INTEREST.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoCOLL_INTEREST(boCre,trade);
                }else if(BOCreConstantes.INDEMNITY.equalsIgnoreCase(creEventType)){
                    return new BOCreRepoINDEMNITY(boCre,trade);
                }
            } else if (trade.getProduct() instanceof Bond) {
                if (BOCreConstantes.CST_S_SETTLED.equalsIgnoreCase(creEventType)) {
                    return new BOCreBondCST_S_SETTLED(boCre, trade);
                } else if (BOCreConstantes.CST_FAILED.equalsIgnoreCase(creEventType)) {
                    return new BOCreBondCST_FAILED(boCre, trade);
                } else if (BOCreConstantes.CST_NET_S_SETTLED.equalsIgnoreCase(creEventType)) {
                    return new BOCreBondCST_NET_S_SETTLED(boCre, trade);
                } else if (BOCreConstantes.CST_UNNET.equalsIgnoreCase(creEventType)) {
                    return new BOCreBondCST_UNNET(boCre, trade);
                }
			}
        }

        if (BOCreConstantes.WRITE_OFF.equalsIgnoreCase(creEventType)) {
            return new BOCreWRITE_OFF(boCre, trade);
        }

        return null;
    }

    /**
     * @param boCre
     * @param trade
     * @return Custom BOCre event class (COT, COT_REV, MTM_NET)
     */
    public SantBOCre getCreTypeKafka(final BOCre boCre, Trade trade) {
        final String creEventType = boCre.getEventType();

        if (trade.getProduct() instanceof Bond) {
            if (BOCreConstantes.COT.equalsIgnoreCase(creEventType) || BOCreConstantes.COT_REV.equalsIgnoreCase(creEventType)) {
                return new BOCreBondCOT(boCre,trade);
            } else if(BOCreConstantes.MTM_NET.equalsIgnoreCase(creEventType)){
                return new BOCreBondMTM_NET(boCre,trade);
            } else if(BOCreConstantes.NOM_FWD.equalsIgnoreCase(creEventType)){
                return new BOCreBondNOM_FWD(boCre,trade);
            } else if(BOCreConstantes.FWD_CASH_FIXING.equalsIgnoreCase(creEventType)
                    || BOCreConstantes.FWD_CASH_FIXING_REAL.equalsIgnoreCase(creEventType)){
                return new BOCreBondFWD_CASH_FIXING(boCre,trade);
            } else if (BOCreConstantes.ALLOCATED.equalsIgnoreCase(creEventType)) {
                return new BOCreBondALLOCATED(boCre, trade);
            }
        }

        return null;
    }

    public SantBOCre getCreTypeCCCT(final BOCre boCre, Trade trade){
        final String creEventType = boCre.getEventType();
        if (trade.getProduct() instanceof Bond){
            if (BOCreConstantes.BOOKING.equalsIgnoreCase(creEventType)) {
                return new BOCreFXBOOKING(boCre, trade);
            } else if (BOCreConstantes.MATURITY.equalsIgnoreCase(creEventType)) {
                return new BOCreFXMATURITY(boCre, trade);
            }
        }
        return null;
    }

    /**
     * @param boPosting
     * @param trade
     * @return Custom BOPosting event class (CST)
     */
    public SantBOCre getCreType(BOPosting boPosting, Trade trade) {
        if (boPosting!=null){
            return new BOPostingCash(boPosting, trade);
        }
        return null;
    }

    private boolean checkPairOffNettedTransfer(BOCre cre){
        BOTransfer nettedTransfer = BOCreUtils.getInstance().getNettedTransfer(cre);
        return null!=nettedTransfer && "PairOff".equalsIgnoreCase(nettedTransfer.getNettingType());
    }

}