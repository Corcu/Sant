package calypsox.tk.product;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.event.PSEventBloombergUpdate;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.product.ShowProduct;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.FlowGenerationException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.DefaultProductValidator;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Vector;

//Project: Bloomberg tagging

public class EquityProductValidator extends DefaultProductValidator {
	final static String[] BAD_MARKETS = {"NONE","NONE1"};

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean isValidInput(Product product, ShowProduct window,
            Vector messages) {
        if (product instanceof Equity) {
            Equity equity = (Equity) product;
            boolean isValidEquity = super.isValidInput(product, window,
                    messages);
            
            isValidEquity &= isValidProduct(equity, messages);
            String isin = product.getSecCode(
                    CollateralStaticAttributes.BOND_SEC_CODE_ISIN);
            boolean isValidIsinCcyPair = isValidIsinCurrencyPair(product,
                    messages);

            if (isValidEquity && isValidIsinCcyPair) {

                if (!CollateralUtilities.isValidISINValue(isin, messages)){
                    return false;
                }

                try {
                    createPsEventBloombergUpdate(isin);
                } catch (CalypsoServiceException cse) {
                    Log.error(this,
                            "Couldn't publish the MarginCallQef Events: "
                                    + cse.getMessage());
                    Log.error(this, cse); //sonar purpose
                }
            }

            try {
                equity.generateFlows(JDate.getNow());

                if (CollateralUtilities.checkIsinCode(isin)) {
                    equity.setSecCode("CUSIP", CollateralUtilities.getCusipFromIsin(isin));
                }

            } catch (FlowGenerationException e) {
            	Log.error(this, e); //sonar purpose
                messages.add(e.getMessage());
                return false;
            }

            return isValidEquity && isValidIsinCcyPair;

        }
        return false;
    }

    private boolean isValidProduct(Equity product, Vector messages) {
    	boolean isValid = true;
    	
    	String common = product.getSecCode("Common");
    	if (Util.isEmpty(common)) {
    		messages.add("Equity cannot be saved if field Common is missing.");
    		isValid = false;
    	}
    	
    	LegalEntity market = product.getMarketPlace();
    	if (market.getAuthName() == null || Arrays.asList(BAD_MARKETS).contains(market.getAuthName())) {
    		messages.add("Equity cannot be saved if Market is not set.");
    		isValid = false;
    	}
    	
    	return isValid;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean isValidIsinCurrencyPair(Product inProduct, Vector messages) {
    	if (!(inProduct instanceof Equity)) {
    		return true;
    	}
    	Equity inEquity = (Equity)inProduct;
        String isin = inProduct.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN);

        if (Util.isEmpty(isin)) {
            return true;
        }

        // get products matching by ISIN - BAU DPM 12/05/15 - Fix ISIN with
        // spaces
        Vector<Product> matchingProducts = getMatchingProductsByIsin(isin.trim());

        // if already exist products, check currency
        if (!Util.isEmpty(matchingProducts)) {
            for (Product product : matchingProducts) {
            	if (!(product instanceof Equity)) {
            		continue;
            	}
            	Equity existingEquity = (Equity)product;
                if (inEquity.getCurrency().equals(existingEquity.getCurrency()) && inEquity.getMarketPlaceId() == existingEquity.getMarketPlaceId()) {
                    // Block only if this is a new Equity
                    if (inEquity.getId() != existingEquity.getId()) {
                        messages.add("An Equity already exists in the system that has same ISIN ("
                                        + isin + "), Currency ("
                                        + inEquity.getCurrency() + "), and Market Place (" + inEquity.getMarketPlace().getAuthName() + ")");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Vector<Product> getMatchingProductsByIsin(String isin) {

        Vector<Product> matchingProducts = null;

        try {
            matchingProducts = DSConnection.getDefault().getRemoteProduct()
                    .getProductsByCode(
                            CollateralStaticAttributes.BOND_SEC_CODE_ISIN,
                            isin);
        } catch (RemoteException e) {
            Log.error(this,
                    "Cannot get any equity related to ISIN = " + isin + ".\n",
                    e);
        }

        return matchingProducts;

    }

    /**
     * createPsEventBloombergUpdate.
     * 
     * @param tituloId
     *            String
     * @throws CalypsoServiceException
     */
    private void createPsEventBloombergUpdate(String tituloId)
            throws CalypsoServiceException {
        PSEventBloombergUpdate event = new PSEventBloombergUpdate(tituloId, 1);

        DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
    }


    public boolean isValidInput(Trade trade, Vector messages) {
        if(trade.getProduct() instanceof Equity){
            Double splitBookPrice = trade.getSplitBookPrice();
            trade.setSplitBookPrice(1.0d);
            Boolean rtn = super.isValidInput(trade, messages);
            trade.setSplitBookPrice(splitBookPrice);
            return rtn;
        }
        else{
            return super.isValidInput(trade, messages);
        }
    }


}
