package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.jfree.util.Log;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Class with the necessary logic to retrieve the different values for the Repos
 * report.
 *
 * @author Jose David Sevillano (josedavid.sevillano@siag.es)
 */
public class ReposTradeLogic {
    private static final String NPV_BASE = "NPV_BASE";
    private static final String CLOSING_PRICE = "CLOSING_PRICE";
    private static final String INDEPENDENT_AMOUNT = "INDEPENDENT_AMOUNT";

    private static final String LENDER = "Lender";
    private static final String BORROWER = "Borrower";
    private static final String REPO = "Repo";
    private static final String REVERSE = "Reverse";
    private static final String BLANK = "";
    private static final String PRODUCT_CLASS = "Repo/Reverse";
    private static final String STRUCTURE_ID = "STRUCTURE_ID";
    private static final String ISIN = "ISIN";
    private final Hashtable<String, CurrencyDefault> currencies = LocalCache
            .getCurrencyDefaults();

    public ReposTradeLogic() {
    }

    private ReposTradeItem getReposTradeItem(Vector<String> errors) {
        return null;
    }

    public String getMtmDate(PLMark mark, JDate date) {

        if (null != mark) {
            return mark.getValDate().toString();

        }

        return date.toString();
    }

    /**
     * Retrieve the mark-to-Market value.
     *
     * @param collMarks Collection with the PLMarks obtained from the database.
     * @return The double value for the Mark-to-Market.
     */
    public String getMtmValue(PLMark mark) {
        if (mark != null) {
            PLMarkValue markValue = mark.getPLMarkValueByName(NPV_BASE);
            if (markValue != null) {
                return formatAmount(Util.numberToString(markValue
                        .getMarkValue()));
            }
        }
        return "0";
    }

    /**
     * Retrieve the Mark-to-Market currency code.
     *
     * @param collMarks Collection with the PLMarks obtained from the database.
     * @return The currency code for the Mark-to-Market.
     */
    public String getMtmCurr(PLMark mark) {

        if (mark != null) {
            PLMarkValue markValue = mark.getPLMarkValueByName(NPV_BASE);
            if (markValue != null) {
                return this.currencies.get(markValue.getCurrency())
                        .getDescription().toUpperCase();
            }
        }

        return BLANK;
    }

    /**
     * Return the string "Repo/Reverse".
     *
     * @return Repo/Reverse.
     */
    public String getProductClass() {
        return PRODUCT_CLASS;
    }

    /**
     * Retrieve the direction for the repo, buy or sell.
     *
     * @param repo The Repo object to obtain the direction.
     * @return Borrower if reverse, else Lender.
     */
    public String getDirectionRepo(Repo repo) {
        if (null != repo) {
            String direction = repo.getDirection(Repo.REPO,repo.getSign());
            if (REVERSE.equals(direction)) {
                return BORROWER;
            } else if (REPO.equals(direction)) {
                return LENDER;
            }
        }

        return "";
    }

    public String getDirectionRepoSt(CollateralExposure ce, Trade t) {
        if (null != ce) {
            String direction = ce.getDirection(t);
            return direction;
        }

        return "";
    }

    /**
     * Retrieve the value for the cash of the repo.
     *
     * @param repo The Repo object to obtain the cash.
     * @return Double value with the cash of the repo.
     */
    public String getCashRepo(Repo repo) {
        if (null != repo) {
            Cash repoCash = repo.getCash();
            if (null != repoCash) {
                return formatAmount(Util.numberToString(repo.getCash()
                        .getPrincipal()));
            }
        }

        return BLANK;
    }

    public String getCashRepoSt(CollateralExposure ce) {
        if (null != ce) {
            return formatAmount(Util.numberToString(ce.getPrincipal()));
        }

        return BLANK;
    }

    /**
     * Retrieve the currency for the cash of the repo.
     *
     * @param repo The Repo object to obtain the currency for the cash.
     * @return String with the code for the currency of the cash.
     */
    public String getTradeCurr2Repo(Repo repo) {
        if (null != repo) {
            Cash repoCash = repo.getCash();
            if ((null != repoCash) && (repoCash.getCurrency() != null)) {
                if (!repo.getCash().getCurrency().equals(BLANK)) {
                    return this.currencies.get(repo.getCash().getCurrency())
                            .getDescription().toUpperCase();
                }
            }
        }

        return BLANK;
    }

    public String getTradeCurr2ValueRepo(Repo repo) {
        if (null != repo) {
            Cash repoCash = repo.getCash();
            if ((null != repoCash) && (repoCash.getCurrency() != null)) {
                return repo.getCash().getCurrency();
            }
        }

        return BLANK;
    }

    public String getTradeCurr2ValueRepoSt(CollateralExposure ce) {
        if (null != ce) {
            if (ce.getCurrency() != null) {
                return ce.getCurrency();
            }

        }

        return BLANK;
    }

    public String getTradeCurr2RepoSt(CollateralExposure ce) {
        if (null != ce) {
            if (ce.getCurrency() != null) {
                return ce.getCurrency();
            }
        }

        return BLANK;
    }

    // new
    public String getCollatAgree(CollateralConfig mcc) {

        return mcc.getName();

    }

    public String getCollatAgreeType(final Trade t) {
        return t.getKeywordValue("CONTRACT_TYPE");
    }

    public String getOwner(CollateralConfig mcc) {

        if (mcc.getProcessingOrg() != null) {
            return mcc.getProcessingOrg().getName();
        }

        return BLANK;
    }

    public String getTradeDate(final Trade t) {
        if ((t != null) && (t.getTradeDate() != null)) {
            return t.getTradeDate().getJDate(TimeZone.getDefault()).toString();
        }
        return BLANK;
    }

    public String getMatDate(final Trade t) {
        if ((t != null) && (t.getProduct() != null)
                && (t.getProduct().getMaturityDate() != null)) {
            return t.getProduct().getMaturityDate().toString();

        }
        return BLANK;
    }

    public double getDirtyPrice(Repo repo) {
        if (repo != null) {
            Collateral col = repo.getFirstCollateral();
            if (col != null) {
                return col.getCollateralPrice();
            }
        }
        return 0.00;
    }

    public String getRateRepo(Repo repo, CollateralConfig mcc, JDate date, PricingEnv pricingEnv) {

        return formatAmount(Util.numberToString(CollateralUtilities.getFXRatebyQuoteSet(
                date, mcc.getCurrency(), getTradeCurr2ValueRepo(repo), pricingEnv)));

    }

    public String getRateRepoSt(CollateralExposure ce, CollateralConfig mcc,
                                JDate date, PricingEnv pricingEnv) {

        return formatAmount(Util.numberToString(CollateralUtilities.getFXRatebyQuoteSet(
                date, getTradeCurr2ValueRepoSt(ce), mcc.getCurrency(), pricingEnv)));

    }

    public String getValAgent(CollateralConfig mcc) {

        return mcc.getAdditionalField("CALC_AGENT");

    }

    public double getClosingPrice(Repo repo, PLMark mark) {
        // MIGRATION V14.4 18/01/2015
        if (mark != null) {
            PLMarkValue markValue = mark.getPLMarkValueByName(CLOSING_PRICE);
            if (markValue != null) {
                return markValue.getMarkValue();
            }
        }
        return 0.00;
    }

    public String getIntRate(Repo repo) {
        if (repo != null) {
            return formatAmount(Util.numberToString(repo.getFixedRate() * 100));
        }
        return BLANK;

    }

    public String getNominal(Repo repo) {
        if (repo != null) {
            Collateral col = repo.getFirstCollateral();
            if (col != null) {
                return formatAmount(Util.numberToString(col.getNominal()));
            }
        }
        return BLANK;

    }

    public String getHaircut(Repo repo) {
        if (repo != null) {
            Collateral col = repo.getFirstCollateral();
            if (col != null) {
                return Util.numberToString(Math.abs(col.getHaircut() * 100));
            }
        }
        return BLANK;

    }

    public String getStructure(Trade t) {
        if (t != null) {
            return t.getKeywordValue(STRUCTURE_ID);
        }
        return BLANK;

    }

    public String getUnderlyingRepo(Repo repo) {
        if (repo != null) {
            if ((repo.getSecurity() != null)
                    && (repo.getSecurity() instanceof Bond)) {
                Bond bond = (Bond) repo.getSecurity();
                return bond.getComment() + " " + bond.getSecCode(ISIN);
            }
        }
        return BLANK;

    }

    public String getUnderlyingRepoSt(final CollateralExposure ce) {
        String value = BLANK;
        if ((ce.getSubType() != null) && (!ce.getSubType().equals(BLANK))) {
            if (CollateralUtilities.isTwoLegsProductType(ce.getSubType())) {
                if (null != ce.getAttribute("UNDERLYING_1")) {
                    value = ce.getAttribute("UNDERLYING_1").toString();
                }
            }
        }
        return value;
    }

    public CollateralConfig getMCContract(int mccId) {
        CollateralConfig mcc = null;
        if (mccId > 0) {
            try {
                mcc = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfig(mccId);
            } catch (final CollateralServiceException exc) {
                Log.error(ReposTradeLogic.class.getName(), exc);
            }
        }
        return mcc;
    }

    public String formatAmount(String amount) {
        if ((amount != null) && (!amount.equals(BLANK))) {
            return amount.replace('.', '*').replace(',', '.').replace('*', ',');
        }
        return amount;

    }

    public String getBaseCcy(CollateralConfig mcc) {

        String ccy = mcc.getCurrency();

        if ((ccy != null) && (!ccy.equals(BLANK))) {
            return this.currencies.get(ccy).getDescription().toUpperCase();
        }

        return BLANK;
    }

    public String getCounterparty(CollateralConfig mcc) {
        if (!mcc.getLegalEntity().getName().equals(BLANK)) {
            return mcc.getLegalEntity().getName();
        }
        return BLANK;

    }

    public String getTradeID(Trade t) {
        if (t != null) {
            String id = t.getKeywordValue("BO_REFERENCE");
            if (id != null) {
                return id;
            }
        }
        return BLANK;
    }

    public String getIndependentAmount(PLMark mark) {
        String result = "0";
        if (mark != null) {
            PLMarkValue markValue = mark
                    .getPLMarkValueByName(INDEPENDENT_AMOUNT);
            if (markValue != null) {
                result = formatAmount(Util.numberToString(markValue
                        .getMarkValue()));
            }
        }

        return result;
    }

    // COL_OUT_016

    /**
     * Method used to add the rows in the report generated. Changed by Carlos
     * Cejudo: No DSConnection is needed anymore, all data is passed through the
     * PLMarksMap.
     *
     * @param trade      Trade associated with the Repo object.
     * @param plMarksMap Map containing the PLMarks for each trade.
     * @param errorMsgs  Vector with the different errors occurred.
     * @return Vector with the rows added.
     */
    public Vector<ReposTradeItem> getReportRows(Trade trade,
                                                JDate valDate, Vector<String> errorMsgs, DSConnection dsConn,
                                                PricingEnv pricingEnv) {
        Vector<ReposTradeItem> reportRows = new Vector<>();
        ReposTradeItem rowCreated = null;

        rowCreated = getReposRowsTradeItem(trade, valDate,
                errorMsgs, dsConn, pricingEnv);
        if (null != rowCreated) { // If the result row is equals to NULL, we
            // don't add this row to the report.
            reportRows.add(rowCreated);
        }

        return reportRows;
    }

    // COL_OUT_016

    /**
     * Method that retrieve row by row from Calypso, to insert in the vector
     * with the result to show. Changed by Carlos Cejudo: No DSConnection is
     * needed anymore, all data is passed through the PLMarksMap.
     *
     * @param trade      Trade associated with the Repo object.
     * @param plMarksMap Map containing the PLMarks for each trade.
     * @param errors     Vector with the different errors occurred.
     * @return The row retrieved from the system, with the necessary
     * information.
     */

    private ReposTradeItem getReposRowsTradeItem(Trade trade, JDate valDate,
                                                 Vector<String> errors, DSConnection dsConn, PricingEnv pricingEnv) {
        ReposTradeItem reposTradeItem = new ReposTradeItem();
        int mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");
        CollateralConfig mcc = getMCContract(mccId);
        //System.out.println("Obtenido el contrato: " + (mcc != null ? mcc.getId() : " null ") + " para el trade:" + trade.getLongId());
        try {
            if (mcc != null) {
                PLMark plMark = CollateralUtilities.retrievePLMark(
                        trade.getLongId(), dsConn, pricingEnv.getName(), valDate);

                // COMMON FIELDS
                reposTradeItem.setCollatAgree(getCollatAgree(mcc));
                reposTradeItem.setCollatAgreeType(getCollatAgreeType(trade));
                reposTradeItem.setCpty(getCounterparty(mcc));
                reposTradeItem.setTradeID(getTradeID(trade));
                // frontID (extRef) - COL
                reposTradeItem.setMtmDate(getMtmDate(plMark, valDate));
                reposTradeItem.setStructure(getStructure(trade));
                reposTradeItem.setTradeDate(getTradeDate(trade));
                // valueDate (product->startDate) - COL
                reposTradeItem.setMatDate(getMatDate(trade));
                reposTradeItem.setValAgent(getValAgent(mcc));
                // portfolio - COL
                reposTradeItem.setOwner(getOwner(mcc)); // para owner y
                // dealOwner
                // instrument - COL
                reposTradeItem.setIndAmount(getIndependentAmount(plMark));
                reposTradeItem.setMtmValue(getMtmValue(plMark));
                reposTradeItem.setMtmCurr(getBaseCcy(mcc));

                // REPO
                if (trade.getProduct() instanceof Repo) {
                    Repo repo = (Repo) trade.getProduct();

                    reposTradeItem.setUnderlying(getUnderlyingRepo(repo));
                    reposTradeItem
                            .setClosingPrice(getClosingPrice(repo, plMark));
                    reposTradeItem.setTradeCurr2(getTradeCurr2Repo(repo));
                    reposTradeItem.setCash(getCashRepo(repo));
                    reposTradeItem.setRate(getRateRepo(repo, mcc, valDate, pricingEnv));
                    // 3
                    reposTradeItem.setDirection(getDirectionRepo(repo));
                    reposTradeItem.setNominal(getNominal(repo));
                    reposTradeItem.setIntRate(getIntRate(repo));
                    reposTradeItem.setDirtyPrice(getDirtyPrice(repo));
                    reposTradeItem.setHaircut(getHaircut(repo));

                    // REPO ESTRUCTURADO
                } else if (trade.getProduct() instanceof CollateralExposure) {
                    CollateralExposure ce = (CollateralExposure) trade
                            .getProduct();

                    reposTradeItem.setUnderlying(getUnderlyingRepoSt(ce));
                    reposTradeItem.setClosingPrice(0.00);
                    reposTradeItem.setTradeCurr2(getTradeCurr2RepoSt(ce));
                    reposTradeItem.setCash(getCashRepoSt(ce));
                    reposTradeItem.setRate(getRateRepoSt(ce, mcc, valDate, pricingEnv));
                    // 3
                    reposTradeItem.setDirection(getDirectionRepoSt(ce, trade)); // duda
                    // de
                    // que
                    // sacar
                    // buy/sell
                    // o
                    // lender/borrower
                    reposTradeItem.setNominal(BLANK);
                    reposTradeItem.setIntRate(BLANK);
                    reposTradeItem.setDirtyPrice(0.00);
                    reposTradeItem.setHaircut(BLANK);
                }

            }

        } catch (RemoteException re) {
            Log.error(ReposTradeLogic.class.getName(), re);
        }

        return reposTradeItem;
    }
}
