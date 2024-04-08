/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.util.TradeCollateralizationService;

import static calypsox.tk.core.CollateralStaticAttributes.MC_CONTRACT_NUMBER;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.CollateralizedTradesReportLogic;
import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantCollateralConfigUtil;

/**
 * This class contains all the required logic for the service Trade TradeCollateralizationServiceEngine. Allows to:
 * build a fake trade, map between INPUT/OUTPUT MAPS and Beans, find suitable MCContracts for a trade and/or build the
 * output message for the trade collateralization degree.
 *
 * @author Guillermo Solano
 * @version 1.1
 * @date 09/01/2014
 */
public class TradeCollateralizationLogic extends TradeCollateralizationConstants {

	private static String CLASSNAME = "TradeCollateralizationLogic";

    /**
     * if true, it will response a descriptive error reasonfor the trade when exception occur.
     */
    private final static boolean DESCRIPTIVE_ERROR_RESPONSE = false;

    /*************************************************/
    /** TRADE COLLATERALIZATION SIMULATION METHODS */
    /***********************************************/

    // 1? CALL

    /**
     * @param inputMap to be checked
     * @return returns null if the input map passes the validation. The String with the error if it didn't pass it.
     * Mandatory fields, correct date format and ALIAS_BOOK in DB are checked.
     * @throws TradeCollateralizationException
     */
    public static String validateInputTradeMap(Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap, final TradeCollateralizationInputBean inputBean)
            throws TradeCollateralizationException {
        StringBuilder sb = new StringBuilder();
        
        // check that we have all the mandatory fields in the map
        for (DFA_INPUT_SIMULATED_FIELDS input : DFA_INPUT_SIMULATED_FIELDS.values()) {
            if (input.isMandatory()) {
                if ((inputMap.get(input) == null) || inputMap.get(input).isEmpty()) {
                    sb.append(RESPONSES.ERR_INPUT_FIELD_MISSING.getResponseValue());

                    if (DESCRIPTIVE_ERROR_RESPONSE) {
                        sb.append(". ").append(RESPONSES.ERR_INPUT_FIELD_MISSING.getDescription()).append(" Missing Mandatory Field: ");
                        sb.append(input.getFieldName()).append(". \n");
                    }
                    
                    Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Error found : Field is mandatory and missing : " + input.getFieldName());
                    
                    return sb.toString(); // errors found
                }
            }
        }

        // check that the format of dates are correct
        sb.append(datesFormatAreCorrect(inputMap));

        if (sb.length() > 0) { // errors found
        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Dates format are incorrect");
            return sb.toString();
        }

        // check that the PO exists
        final LegalEntity poLe = BOCache.getLegalEntity(DSConnection.getDefault(), inputMap.get(DFA_INPUT_SIMULATED_FIELDS.PROCESSING_ORG));
        if (poLe == null) {
            sb.append(RESPONSES.ERR_PO_NOT_FOUND.getResponseValue());
            if (DESCRIPTIVE_ERROR_RESPONSE) {
                sb.append(RESPONSES.ERR_PO_NOT_FOUND.getDescription()).append(".");
            }
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Error found : " + RESPONSES.ERR_PO_NOT_FOUND.getDescription());
            return sb.toString(); // errors found
        }

        String expoProduct = EMPTY;
        // check the mapping of the product
        try {
        	expoProduct = getProductMapped(inputBean, sb, inputBean.isPhoenix());
        } catch (TradeCollateralizationException e) {
        	// is missing the DV UPIColExpProductsMapping
        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, e.getLocalizedMessage());
        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
        	
        	return sb.toString(); // errors found
        }

        if (expoProduct.equals(EMPTY)) {
        	sb.append(RESPONSES.ERR_PRODUCT_NOT_FOUND.getResponseValue());
        	if (DESCRIPTIVE_ERROR_RESPONSE) {
        		sb.append(RESPONSES.ERR_PRODUCT_NOT_FOUND.getDescription()).append(".");
        	}

        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Error found : " + RESPONSES.ERR_PRODUCT_NOT_FOUND.getDescription());

        	return sb.toString(); // errors found
        }
        else {
        	inputBean.setProductTypeMapped(expoProduct);
        }

        // everything OK, no errors
        return null;
    }

    // 2? CALL

    /**
     * Generates a fake trade if all the input fields are satisfied
     *
     * @param inputMap a map with all the input parameters based on the enum DFA_INPUT_FIELDS
     * @return a fake trade
     * @throws ParseException
     * @throws Exception      and FormatException
     */
    public static Trade buildTradeFromInputMap(Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap, final TradeCollateralizationInputBean inputBean) {
        Trade trade = new Trade();

        Log.debug(TradeCollateralizationConstants.ENGINE_NAME, "1. buildTradeFromInputMap -> Start Trade creation");

        // BO reference - externalReference
        trade.addKeyword(BO_REFERENCE_KEYWORD, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.BO_EXTERNAL_REFERENCE));
        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : External Reference : " + inputMap.get(DFA_INPUT_SIMULATED_FIELDS.BO_EXTERNAL_REFERENCE));

        // BO System
        trade.addKeyword(BO_SYSTEM_KEYWORD, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.BO_SOURCE_SYSTEM));
        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Source System : " + inputMap.get(DFA_INPUT_SIMULATED_FIELDS.BO_SOURCE_SYSTEM));

        // CPTY - counterParty
        trade.setCounterParty(BOCache.getLegalEntity(DSConnection.getDefault(), inputMap.get(DFA_INPUT_SIMULATED_FIELDS.COUNTERPARTY)));
        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Counterparty : " + inputMap.get(DFA_INPUT_SIMULATED_FIELDS.COUNTERPARTY));
 
        Product product = null;
        Boolean isPhoenixPureProduct = false;
        if (inputBean != null && inputBean.isPhoenix()) {
        	switch (inputBean.getProductTypeMapped()) {
        	case "Repo":
        		product = new Repo();
        		isPhoenixPureProduct = true;
        		Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Trade created is a " + inputBean.getProductTypeMapped());
        		break;
        	case "SecLending":
        		product = new SecLending();
        		((SecLending)product).setSecLendingType(SecLending.SUBTYPE_SEC_VS_CASH);
        		isPhoenixPureProduct = true;
        		Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Trade created is a " + inputBean.getProductTypeMapped());
        		break;
        	case "PerformanceSwap":
        		product = new PerformanceSwap();
        		isPhoenixPureProduct = true;
        		Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Trade created is a " + inputBean.getProductTypeMapped());
        		break;
        	}
        	trade.setProduct(product);
        }
        
        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Phoenix Pure Product : " + isPhoenixPureProduct);
        
        if (!isPhoenixPureProduct) {
        	// creation of the specific product of the trade
            product = new CollateralExposure();
            trade.setProduct(product);
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Not a Phoenix Pure Product, CollateralExposure created.");
            
            String productMapped = inputBean.getProductTypeMapped();
            if (Util.isEmpty(productMapped)) {
            	// product type - productType mapped, either UPI or Calypso catalogue
            	productMapped = getProductMapped(inputBean, inputBean.isPhoenix());
            }
        	product.setSubType(productMapped);
        	((CollateralExposure)product).setUnderlyingType(productMapped);
        	
        	Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : CollateralExposure Subtype / Underlying Type : " + productMapped);
        }

        // insert available days
        try {
            // start date. OPTIONAL
            if (inputMap.containsKey(DFA_INPUT_SIMULATED_FIELDS.START_DATE)) {
                Date startDate = dateFormat.parse(inputMap.get(DFA_INPUT_SIMULATED_FIELDS.START_DATE));
                
                setProductDate(product, JDate.valueOf(startDate), "start");
                Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Start Date : " + inputMap.get(DFA_INPUT_SIMULATED_FIELDS.START_DATE));
            }
            else {
            	Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : No Start Date specified.");
            }

            // maturity - endDate. OPTIONAL
            if (inputMap.containsKey(DFA_INPUT_SIMULATED_FIELDS.END_DATE)) {
                Date matDate = dateFormat.parse(inputMap.get(DFA_INPUT_SIMULATED_FIELDS.END_DATE));
                JDate jMaturityDate = JDate.valueOf(matDate);
                setProductDate(product, jMaturityDate, "maturity");
                setProductDate(product, jMaturityDate, "end");
                
                if (product instanceof PerformanceSwap) {
                	trade.setSettleDate(jMaturityDate);
                }
                
                Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : End Date : " + inputMap.get(DFA_INPUT_SIMULATED_FIELDS.END_DATE));
            }
            else {
            	Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : No End Date specified.");
            }

            // value date - valueDate
            if (inputMap.containsKey(DFA_INPUT_SIMULATED_FIELDS.VALUE_DATE)) {
                Date valDate = dateFormat.parse(inputMap.get(DFA_INPUT_SIMULATED_FIELDS.VALUE_DATE));
                JDatetime jValueDateTime = new JDatetime(valDate);
                trade.setTradeDate(jValueDateTime);
                
                Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Value Date (Trade Date) : " + inputMap.get(DFA_INPUT_SIMULATED_FIELDS.VALUE_DATE));
                
                if ((inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE) != null)
                		&& !inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE).isEmpty()) { // read valuation date
                	Date mccValDate = dateFormat.parse(inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE));
                	if (valDate.after(mccValDate)) {
                		Log.warn(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : WARNING TradeDate > MCC Valuation Date : Most Probably Calypso will reject acceptation of Trade in MC Contract !!");
                	}
                }
            }
            else {
            	Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : No Value Date specified.");
            	return null;
            }
        } catch (ParseException e) {// Never to occur
            return null;
        }

        // The deal Currency - currency
        String ccy = inputMap.get(DFA_INPUT_SIMULATED_FIELDS.CURRENCY);
        setProductCurrency(product, ccy);
        trade.setSettleCurrency(ccy);
        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Currency : " + ccy);
        
        Log.debug(TradeCollateralizationConstants.ENGINE_NAME, "2. buildTradeFromInputMap -> Trade generated");
        
        if (trade.getCounterParty() == null) {
        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Mandatory data CounterParty is missing, aborting.");
        	return null;
        }
        else if (trade.getTradeDate() == null) {
        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Mandatory data TradeDate is missing, aborting.");
        	return null;
        }
        return trade;
    }

    private static void setProductCurrency(Product product, String currency) {
		if (product instanceof SecLending) {
			((SecLending)product).setCurrency(currency);
		}
		else if (product instanceof CollateralExposure) {
			((CollateralExposure)product).setCurrency(currency);
		}
	}

	private static void setProductDate(Product product, JDate jDate, String dateType) {
		switch (dateType) {
		case "start":
			if (product instanceof Repo) {
				if (((Repo)product).getCash() == null) {
					Cash cash = new Cash();
					((Repo)product).setCash(cash);
				}
				((Repo)product).setStartDate(jDate);
			}
			else if (product instanceof SecLending) {
				((SecLending)product).setStartDate(jDate);
			}
			else if (product instanceof CollateralExposure) {
				((CollateralExposure)product).setStartDate(jDate);
			}
			break;
			
		case "maturity":
			if (product instanceof Repo) {
				if (((Repo)product).getCash() == null) {
					Cash cash = new Cash();
					((Repo)product).setCash(cash);
				}
				((Repo)product).setMaturityDate(jDate);
			}
			else if (product instanceof SecLending) {
				((SecLending)product).setMaturityDate(jDate);
			}
			else if (product instanceof PerformanceSwap) {
				((PerformanceSwap)product).setMaturityDate(jDate);
			}
			else if (product instanceof CollateralExposure) {
				((CollateralExposure)product).setMaturityDate(jDate);
			}
			break;
			
		case "end":
			if (product instanceof Repo) {
				if (((Repo)product).getCash() == null) {
					Cash cash = new Cash();
					((Repo)product).setCash(cash);
				}
				((Repo)product).setEndDate(jDate);
			}
			else if (product instanceof SecLending) {
				((SecLending)product).setEndDate(jDate);
			}
			else if (product instanceof CollateralExposure) {
				((CollateralExposure)product).setEndDate(jDate);
			}
			break;
		}
	}
	
	private static JDate getProductDate(Product product, String dateType) {
		JDate date = null;
		switch (dateType) {
		case "start":
			if (product instanceof Repo) {
				date = ((Repo)product).getStartDate();
			}
			else if (product instanceof SecLending) {
				date = ((SecLending)product).getStartDate();
			}
			else if (product instanceof CollateralExposure) {
				date = ((CollateralExposure)product).getStartDate();
			}
			break;
			
		case "maturity":
			if (product instanceof Repo) {
				date = ((Repo)product).getMaturityDate();
			}
			else if (product instanceof SecLending) {
				date = ((SecLending)product).getMaturityDate();
			}
			else if (product instanceof PerformanceSwap) {
				date = ((PerformanceSwap)product).getMaturityDate();
			}
			else if (product instanceof CollateralExposure) {
				date = ((CollateralExposure)product).getMaturityDate();
			}
			break;
			
		case "end":
			if (product instanceof Repo) {
				date = ((Repo)product).getEndDate();
			}
			else if (product instanceof SecLending) {
				date = ((SecLending)product).getEndDate();
			}
			else if (product instanceof CollateralExposure) {
				date = ((CollateralExposure)product).getEndDate();
			}
			break;
		}
		
		return date;
	}

	// 3? CALL

    /**
     * Builds an output message with the appropiate Collateralization value for the Trade.
     *
     * @param inputMap
     * @param simulatedTrade
     * @param inputBean
     * @return
     */
    public static Map<DFA_OUTPUT_FIELDS, String> tradeCollateralizationDegreeAndBuildOutput(
            final Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap, final Trade simulatedTrade,
            final TradeCollateralizationInputBean inputBean) {
        String messageValue = "";
        boolean tradeMatchesMrgContract = false;
         
        int foundContractId = -1;
        if (inputBean.getProductTypeMapped().equals("Repo")) {
        	TradeArray realTradesArray;
			try {
				realTradesArray = DSConnection.getDefault().getRemoteTrade().getTradesByExternalRef(inputBean.getFOExternalReference());
				if (realTradesArray.size() >= 1) {
					Trade realTrade = realTradesArray.get(0);
					Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Found Real Trade with ExternalRef " + inputBean.getFOExternalReference());
					
					String workFlowSubtypeKW = realTrade.getKeywordValue("WorkflowSubType");
					Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - WorkflowSubType = " + workFlowSubtypeKW);
					if (Util.isEmpty(workFlowSubtypeKW) || !workFlowSubtypeKW.equalsIgnoreCase("RepoMurex")) {
						String mccIdKW = realTrade.getKeywordValue(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
						if (!Util.isEmpty(mccIdKW)) {
							foundContractId = Integer.valueOf(mccIdKW);
						}
					}
					else if (realTrade.getProduct() instanceof Repo) { // WorkflowSubType KW = RepoMurex - check if this really is a Repo
						Repo repo = (Repo) realTrade.getProduct();
						String isGCPoolingKW = realTrade.getKeywordValue("isGCPooling");
						if (!repo.isTriparty() || (!Util.isEmpty(isGCPoolingKW) && "true".equalsIgnoreCase(isGCPoolingKW))) {
							Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - isTriparty = " + repo.isTriparty());
							Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - isGCPooling KW = " + isGCPoolingKW == null ? Boolean.toString(false) : isGCPoolingKW);

							String mccIdKW = realTrade.getKeywordValue(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
							if (!Util.isEmpty(mccIdKW)) {
								foundContractId = Integer.valueOf(mccIdKW);
							}
						}
						else {
							Map<DFA_OUTPUT_FIELDS, String> outMap = buildDummyOutputMap("", inputMap, inputBean);

							String tripartyAgent = realTrade.getKeywordValue("TripartyAgent");
							Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Repo Triparty no GCPooling - TripartyAgent = " + tripartyAgent);
							
							if (!Util.isEmpty(tripartyAgent)) {
								outMap.put(DFA_OUTPUT_FIELDS.TRIPARTY_AGENT, tripartyAgent);
							}
							// Leave Collateralization Degree empty
							outMap.put(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL, "");

							return outMap; // We already have our output so return it.
						}
					}
				}
				else {
					Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Did not find Real Trade with ExternalRef " + inputBean.getFOExternalReference());
				}
			} catch (CalypsoServiceException e) {
				Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Error getting Trades by External Ref : " + e.toString());
			}
        }
        
        CollateralConfig foundCollateralConfig = null;
        if (foundContractId > 0) {
        	Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Found contractID from real Trade : " + foundContractId);
			
        	foundCollateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), foundContractId);
        	// If EMIR_COLLATERAL_VALUE field is empty try to force reload of contract
        	if (Util.isEmpty(foundCollateralConfig.getAdditionalField("EMIR_COLLATERAL_VALUE"))) {
        		Log.warn(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - EMIR_COLLATERAL_VALUE field empty, try force reload of contract : " + foundContractId);
        		foundCollateralConfig = getFreshCollateralConfig(foundContractId, inputBean);
        	}
        }
        
        if (foundCollateralConfig == null) {
        	Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - No real contract found - Will search for suitables ones.");
        	
	        // first retrieve the contract. If is correct, it should bring back only one contract for the trade
	        final List<CollateralConfig> suitableMrgContractList = findSuitableCollateralContractsForTrade(simulatedTrade, inputMap);
	        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Suitable MC Contracts found : " + suitableMrgContractList);
	        
	        if (!inputBean.isPhoenix()) {
	        	Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Not Phoenix : we get contract and test it if only one has been found, otherwise (>1 contract or no contract) we have an error.");
	        	
	        	// no contract match or there more than one contract match for the trade or PO of the input doesn't belong to
	        	// the contract-> No collateralizable
	        	messageValue = validateCollateralContract(inputMap, suitableMrgContractList);

	        	if (messageValue != null) { // return NO
	        		return buildDummyOutputMap(messageValue, inputMap, inputBean);
	        	}

	        	// At this point we have one match between the trade and ONE suitable Mrg contract
	        	foundCollateralConfig = suitableMrgContractList.get(0);
	        	
	        	if (foundCollateralConfig != null) {
			        // just add a book of the contract to ensure the trade is accepted by the SDF
			        simulatedTrade.setBook(foundCollateralConfig.getBook());
		
			        if (simulatedTrade.getBook() == null) {
			        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Book is null, aborting.");
			        	return buildDummyOutputMap(String.valueOf(RESPONSES.ERR_EXCEPTION_OCCURRED.getResponseValue()), inputMap, inputBean);
			        }
			        
			        // checks if the simulated trade matches the contract
			        tradeMatchesMrgContract = checkIfMCContractAcceptsTrade(inputMap, simulatedTrade, foundCollateralConfig);
		        }
	        }
	        else {	        		        	
	        	// Filter all found suitable Contracts found
	        	if (!Util.isEmpty(suitableMrgContractList) && suitableMrgContractList.size() > 0) {
	        		Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Phoenix : we first filter all found contracts");

	        		for (int i = suitableMrgContractList.size() - 1; i >= 0; i--) {
	        			CollateralConfig currentCollateralConfig = suitableMrgContractList.get(i);
	        			messageValue = checkPOBelongsToContract(inputMap, currentCollateralConfig);
	        			if (messageValue != null) {
	        				Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Contract elimininated because PO does not beglon to contract, contract ID = " + currentCollateralConfig.getId());
	        				suitableMrgContractList.remove(i);
	        				continue;
	        			}
	        			
	        			if (currentCollateralConfig.getBook() == null) {
	        				Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Contract elimininated because Book is null, contract ID = " + currentCollateralConfig.getId());
	        				suitableMrgContractList.remove(i);
	        				continue;
	        			}

	        			// just add a book of the contract to ensure the trade is accepted by the SDF
	        			simulatedTrade.setBook(currentCollateralConfig.getBook());

	        			if (!checkIfMCContractAcceptsTrade(inputMap, simulatedTrade, currentCollateralConfig)) {
	        				Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Contract elimininated because it does not accept Trade, contract ID = " + currentCollateralConfig.getId());
	        				suitableMrgContractList.remove(i);
	        				continue;
	        			}
	        			
	        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Accepted Contract with ID = " + currentCollateralConfig.getId());
	        		}
	        		
	        		if (suitableMrgContractList.size() > 1) {
	        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - After filtering, more than one contracts remain, we order them and get max ID, with priotity on Triparty Contracts");

	        			int highestID = 0;
	        			// First search for Triparty contracts
	        			for (int i = 0; i < suitableMrgContractList.size(); i++) {
	        				CollateralConfig currentCollateralConfig = suitableMrgContractList.get(i);
	        				if (currentCollateralConfig.isTriParty() && currentCollateralConfig.getId() > highestID) {
	        					Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Found Contract Triparty with ID = " + currentCollateralConfig.getId());
	        					foundCollateralConfig = currentCollateralConfig;
	        					highestID = currentCollateralConfig.getId();
	        				}
	        			}
	        			
	        			// Then search for other contract types
	        			if (foundCollateralConfig == null) {
	        				highestID = 0;
		        			for (int i = 0; i < suitableMrgContractList.size(); i++) {
		        				CollateralConfig currentCollateralConfig = suitableMrgContractList.get(i);
		        				if (currentCollateralConfig.getId() > highestID) {
		        					Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Found Contract not Triparty with ID = " + currentCollateralConfig.getId());
		        					foundCollateralConfig = currentCollateralConfig;
		        					highestID = currentCollateralConfig.getId();
		        				}
		        			}
	        			}
	        		}
	        		else if (suitableMrgContractList.size() == 1) {
	        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - After filtering, only one contract remains : we choose this one.");
	        			foundCollateralConfig = suitableMrgContractList.get(0);
	        		}
	        		else {
	        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - After filtering, no suitable contract remain.");
	        		}
	        	}
	        	
	        	if (foundCollateralConfig != null) {
	        		Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Final chosen Contract ID = " + foundCollateralConfig.getId());
	        		
		        	if (foundCollateralConfig != null && Util.isEmpty(foundCollateralConfig.getAdditionalField("EMIR_COLLATERAL_VALUE"))) {
			        	Log.warn(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - EMIR_COLLATERAL_VALUE field empty, try force reload of contract : " + foundContractId);
		        		foundCollateralConfig = getFreshCollateralConfig(foundCollateralConfig.getId(), inputBean);
		        	}
		        	
		        	tradeMatchesMrgContract = true;
	        	}
	        	else {
	        		return buildDummyOutputMap(messageValue, inputMap, inputBean);
	        	}
	        }
        }
        else {
        	tradeMatchesMrgContract = true;
        }

        // read the input UPI mapping for the product
        final String productMapped = inputMap.get(DFA_INPUT_SIMULATED_FIELDS.PRODUCT_TYPE);
        
        // if there is a match, an output with the appropiate Collateralization degree is sent.
        return buildOutputMapFromTradeAndContract(simulatedTrade, foundCollateralConfig, tradeMatchesMrgContract, productMapped, inputMap, inputBean);
    }

    private static CollateralConfig getFreshCollateralConfig(int foundContractId, TradeCollateralizationInputBean inputBean) {
		try {
			return ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(foundContractId);
		} catch (CollateralServiceException e) {
			Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Error retrieving fresh Collateral Config.");
		}
		
		return null;
	}

    // Taken from PDV
    public static boolean isTripartyNoGcPooling(Trade trade) {
		boolean ret=false;
		if (trade.getProduct() instanceof Repo){
			Repo repo = (Repo) trade.getProduct();
			String isGCPooling = trade.getKeywordValue("isGCPooling");
			if (repo.isTriparty() && (Util.isEmpty(isGCPooling) || "false".equalsIgnoreCase(isGCPooling) || trade.getMirrorBook() != null)) {
				ret = true;
			}
		}
		return ret;
	}

	/**
     * generates the input map from the Bean for a simulated trade
     */
    public static Map<DFA_INPUT_SIMULATED_FIELDS, String> getMap(final TradeCollateralizationInputBean inputBean) {
        final Map<DFA_INPUT_SIMULATED_FIELDS, String> inMap = new HashMap<DFA_INPUT_SIMULATED_FIELDS, String>(
                DFA_INPUT_SIMULATED_FIELDS.values().length);

        if (!Util.isEmpty(inputBean.getBOExternalReference())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.BO_EXTERNAL_REFERENCE, inputBean.getBOExternalReference()); // 1
        }
        if (!Util.isEmpty(inputBean.getBOSourceSystem())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.BO_SOURCE_SYSTEM, inputBean.getBOSourceSystem()); // 2
        }

        if (!Util.isEmpty(inputBean.getProcessingOrg())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.PROCESSING_ORG, inputBean.getProcessingOrg()); // 3
        }

        if (!Util.isEmpty(inputBean.getCounterParty())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.COUNTERPARTY, inputBean.getCounterParty()); // 4
        }

        if (!Util.isEmpty(inputBean.getProductType())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.PRODUCT_TYPE, inputBean.getProductType()); // 5
        }

        if (!Util.isEmpty(inputBean.getStartDate())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.START_DATE, inputBean.getStartDate()); // 6
        }

        if (!Util.isEmpty(inputBean.getEndDate())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.END_DATE, inputBean.getEndDate()); // 7
        }

        if (!Util.isEmpty(inputBean.getValueDate())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.VALUE_DATE, inputBean.getValueDate()); // 8
        }

        if (!Util.isEmpty(inputBean.getCurrency())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.CURRENCY, inputBean.getCurrency()); // 9
        }

        if (!Util.isEmpty(inputBean.getProcessingDate())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.MCC_PROCESSING_DATE, inputBean.getProcessingDate()); // 10
        }

        if (!Util.isEmpty(inputBean.getValuationDate())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE, inputBean.getValuationDate()); // 11
        }
         
        // For Phoenix
        if (!Util.isEmpty(inputBean.getFOExternalReference())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.FO_EXTERNAL_REFERENCE, inputBean.getFOExternalReference()); 
        } 
        if (!Util.isEmpty(inputBean.getFOSourceSystem())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.FO_SOURCE_SYSTEM, inputBean.getFOSourceSystem()); 
        }
        if (!Util.isEmpty(inputBean.getInstrument())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.INSTRUMENT, inputBean.getInstrument()); 
        } 
        if (!Util.isEmpty(inputBean.getTipology())) {
            inMap.put(DFA_INPUT_SIMULATED_FIELDS.TIPOLOGY, inputBean.getTipology()); 
        }
        
        inMap.put(DFA_INPUT_SIMULATED_FIELDS.ID, String.valueOf(inputBean.getId()));
        
        return inMap;
    }

    /**
     * builds the TradeCollateralizationOutputBean from the output map
     */
    public static TradeCollateralizationOutputBean getOutputBean(final Map<DFA_OUTPUT_FIELDS, String> outputMap) {
        TradeCollateralizationOutputBean bean = new TradeCollateralizationOutputBean();

        bean.setIsCollateralizedDeal(outputMap.get(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL)); // 1
        bean.setBOExternalReference(outputMap.get(DFA_OUTPUT_FIELDS.BO_EXTERNAL_REFERENCE)); // 2
        bean.setBOSourceSystem(outputMap.get(DFA_OUTPUT_FIELDS.BO_SOURCE_SYSTEM)); // 3
        bean.setValueDate(outputMap.get(DFA_OUTPUT_FIELDS.VALUE_DATE)); // 4
        bean.setCollateralName(outputMap.get(DFA_OUTPUT_FIELDS.COLLATERAL_NAME)); // 5
        bean.setCollateralType(outputMap.get(DFA_OUTPUT_FIELDS.COLLATERAL_TYPE)); // 6
        bean.setProductType(outputMap.get(DFA_OUTPUT_FIELDS.PRODUCT_TYPE)); // 7
        bean.setContractDirection(outputMap.get(DFA_OUTPUT_FIELDS.CONTRACT_DIRECTION)); // 8
        bean.setCollateralStartDate(outputMap.get(DFA_OUTPUT_FIELDS.COLLATERAL_START_DATE)); // 9
        bean.setCollateralEndDate(outputMap.get(DFA_OUTPUT_FIELDS.COLLATERAL_END_DATE)); // 10
        
        // For Phoenix
        bean.setFOExternalReference(outputMap.get(DFA_OUTPUT_FIELDS.FO_EXTERNAL_REFERENCE)); 
        bean.setFOSourceSystem((outputMap.get(DFA_OUTPUT_FIELDS.FO_SOURCE_SYSTEM)));
        bean.setContractId((outputMap.get(DFA_OUTPUT_FIELDS.CONTRACT_ID)));
        bean.setContractName((outputMap.get(DFA_OUTPUT_FIELDS.CONTRACT_NAME)));
        bean.setIsTriparty(outputMap.get(DFA_OUTPUT_FIELDS.IS_TRIPARTY));
        bean.setTripartyAgent(outputMap.get(DFA_OUTPUT_FIELDS.TRIPARTY_AGENT));
        bean.setCollateralPortfolioCode(outputMap.get(DFA_OUTPUT_FIELDS.COLLATERAL_PORTFOLIO_CODE));

        return bean;
    }

    /***********************************************************/
    /** TRADE BO & SYSTEM REFERENCE COLLATERALIZATION METHODS */
    /*********************************************************/

    /**
     * builds the TradeCollateralizationOutputBean ...
     */
    public static TradeCollateralizationOutputBean getExistingTradeOutputBean(final Trade trade, final TradeCollateralizationInputBean inputBean) {
        final CollateralConfig contract = getContractFromTrade(trade);

        try {
            // if contract not found or input date after maturity trade date -> 3-Uncollateralized
            if ((contract == null) || tradeIsMature(trade, inputBean)) {
                return getUncollateralizedOutputBean(inputBean);
            }
        } catch (TradeCollateralizationException e) {

            final TradeCollateralizationOutputBean outBadFormat = getUncollateralizedOutputBean(inputBean);
            outBadFormat.setIsCollateralizedDeal(RESPONSES.ERR_INPUT_FORMAT_INCORRECT.getResponseValue());
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
        }
        // reach this point, contract found (as expected) 
        // find the UPI mapping for the product subtype of the trade
        String productMapped = inputBean.getProductTypeMapped();
        if (Util.isEmpty(productMapped)) {
        	productMapped= getProductMapped(trade, inputBean.isPhoenix());
        }

        // get the output map with the data and the correct collateralization degree
        final Map<DFA_OUTPUT_FIELDS, String> outMap = buildOutputMapFromTradeAndContract(trade, contract, true,
                productMapped, null ,inputBean);

        // return outputBean to generate the result
        return getOutputBean(outMap);
    }

    /**
     * checks date of requested real trade input is less than the recovered trade mature day
     *
     * @param trade
     * @param inputBean
     * @return
     * @throws TradeCollateralizationException
     */
    private static boolean tradeIsMature(Trade trade, TradeCollateralizationInputBean inputBean) throws TradeCollateralizationException {
        if (inputBean.getValueDate().isEmpty()) {
            return false;
        }

        final String stringDate = inputBean.getValueDate();

        // bad format
        if (!datePattern.matcher(stringDate).matches()) {

            String errorDesc = RESPONSES.ERR_INPUT_FORMAT_INCORRECT.getResponseValue() + "";
            if (DESCRIPTIVE_ERROR_RESPONSE) {
                errorDesc += ". " + " Field: " + stringDate + " has an incorrect format. It must have the pattern "
                        + dateFormat.toPattern() + "\n";
            }

            throw new TradeCollateralizationException(errorDesc);
        }

        Date testDate;

        try {
            testDate = dateFormat.parse(stringDate);
        } catch (ParseException e) {

            String errorDesc = RESPONSES.ERR_INPUT_FORMAT_INCORRECT.getResponseValue() + "";
            if (DESCRIPTIVE_ERROR_RESPONSE) {
                errorDesc += ". " + " Field: " + stringDate + " has an incorrect format. It must have the pattern "
                        + dateFormat.toPattern() + "\n";
            }

            throw new TradeCollateralizationException(errorDesc);
        }
        JDate valuation = JDate.valueOf(testDate);

        // check is higher that maturity day of the trade
        return valuation.after(trade.getMaturityDate());

    }

    /**
     * builds the TradeCollateralizationOutputBean for an null Trade (try to recover try from the system using BO
     * Reference and BO system but wasn't found. Returns UNCOLLATERALIZED degree.
     *
     * @return the output bean to be formatted
     */
    public static TradeCollateralizationOutputBean getUncollateralizedOutputBean(final TradeCollateralizationInputBean inputBean) {
        final TradeCollateralizationOutputBean bean = new TradeCollateralizationOutputBean();

        bean.setIsCollateralizedDeal(RESPONSES.UNCOLLATERALIZED.getResponseValue());
        bean.setBOExternalReference(inputBean.getBOExternalReference());
        bean.setBOSourceSystem(inputBean.getBOSourceSystem());

        return bean;
    }

    // //////////////////////////////////////////////////////////////////////////////
    // /////////////////// PRIVATE METHODS EXISTING TRADE //////////////////////////
    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Try to recover contract from Trade contract keyword as a keyword. If fails, tries to read the contract from the
     * internal id. If fails again (it shouldn't) it will look for a match contract for the trade.
     *
     * @param the trade from which the contract has to be recovered
     * @return the collateral contract
     */
    private static CollateralConfig getContractFromTrade(Trade trade) {
        if (trade == null) {
            return null;
        }

        // tries to recover the contract from: 1? the internal reference; if fails, 2? from trade keyword
        CollateralConfig contract = getContractFromTradeKeyOrIntRef(trade);

        // if contract still wasn't found, it will trade to match the trade
        if (contract == null) {
            contract = findContractMatchForTrade(trade);
        }

        return contract;
    }

    /**
     * Try to recover contract from Trade contract keyword as a keyword. If fails, tries to read the contract from the
     * internal id.
     *
     * @param the trade from which the contract has to be recovered
     * @return the collateral contract
     */
    private static CollateralConfig getContractFromTradeKeyOrIntRef(final Trade trade) {
        CollateralConfig contract = null;
        int contractId = -1;

        try {
            // first try from internal reference
            contractId = Integer.valueOf(trade.getInternalReference());
        } catch (Exception e) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
            // do nothing
        }

        try {
            // try from Keyword
            if (contractId <= 0) {
                contractId = Integer.valueOf(trade.getKeywordValue(MC_CONTRACT_NUMBER));
            }
        } catch (Exception e2) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, e2); //sonar
            return null; // impossible to recover Contract from trade ID
        }

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();

        try {
            contract = srvReg.getCollateralDataServer().getMarginCallConfig(contractId);
        } catch (RemoteException e) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
            return null;
        }

        return contract;
    }

    /**
     * If contract id keyword and internal reference fails, gather the contract from the trade.
     */
    private static CollateralConfig findContractMatchForTrade(final Trade trade) {
        CollateralConfig contract = null;

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();

        try {
            contract = srvReg.getCollateralDataServer().getMarginCallConfig(trade);
        } catch (Exception e) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
            return null;
        }

        return contract;
    }

    // ///////////////////////////////////////////////////////////////////////////////
    // /////////////////// PRIVATE METHODS SIMULATED TRADE //////////////////////////
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * Does the mapping conversion between the UPI catalogue and Calypso. In case a is not found, it will try to
     * validate the input as a Calypso Product.
     *
     * @param product to map
     * @param errors  attached (if any)
     * @return the product in calypso
     * @throws TradeCollateralizationException
     */
    private static String getProductMapped(final TradeCollateralizationInputBean inputBean, final StringBuilder errors, boolean isPhoenix) throws TradeCollateralizationException {
        String mapped = EMPTY;

        if (!isPhoenix) {
        	final HashMap<String, String> productsMappingUPI = CollateralUtilities.initMappingFromDomainValues(DSConnection.getDefault(), UPI_CATALOGUE_DV_NAME);

        	// ensure the UPI mapping is in the system
        	if (productsMappingUPI.isEmpty()) {
        		final String message = "Missing values in DV " + UPI_CATALOGUE_DV_NAME
        				+ CollateralUtilities.PRODUCT_MAPPING_DOMAIN_VALUE + ". Add the UPI Mapping to use this service.";

        		TradeCollateralizationException excMissingDV = new TradeCollateralizationException();
        		excMissingDV.setLogMessage(message);
        		errors.append(RESPONSES.ERR_UPI_DV_MAPPING_MISSING.getResponseValue()).append(". ");

        		if (DESCRIPTIVE_ERROR_RESPONSE) {
        			errors.append(RESPONSES.ERR_UPI_DV_MAPPING_MISSING.getDescription()).append("|");
        		}
        		throw excMissingDV;
        	}

        	try {
        		// GSM: 09/01/2014. Max DV mapped product is 64 chars. For names with more letter, it will be trimmed to
        		// first 64 characters.
        		final String productName = CollateralUtilities.trimStringForSize(inputBean.getProductType(), 64);

        		// retrieve UPI mapped product
        		if (productsMappingUPI.containsKey(productName)) {
        			mapped = productsMappingUPI.get(productName);

        			// is not here, try CALYPSO Catalogue
        		} else if (CollateralUtilities.isOneLegProductType(productName)
        				|| CollateralUtilities.isTwoLegsProductType(productName)) {
        			mapped = productName;
        		}
        	} catch (NumberFormatException e) {
        		Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
        	}
        }
        else {
        	final HashMap<String, String> productsMappingPhoenix = CollateralUtilities.initMappingFromDomainValues(DSConnection.getDefault(), PHOENIX_CATALOGUE_DV_NAME);
        	
        	if (productsMappingPhoenix.isEmpty()) {
        		final String message = "Missing values in DVs. Add the Mappings to use this service.";

        		TradeCollateralizationException excMissingDV = new TradeCollateralizationException();
        		excMissingDV.setLogMessage(message);
        		errors.append(RESPONSES.ERR_UPI_DV_MAPPING_MISSING.getResponseValue()).append(". ");

        		if (DESCRIPTIVE_ERROR_RESPONSE) {
        			errors.append(RESPONSES.ERR_UPI_DV_MAPPING_MISSING.getDescription()).append("|");
        		}
        		throw excMissingDV;
        	}
        	
        	try {
        		StringBuilder key = new StringBuilder();
        		
        		// 1. Search for key with everything
        		key.append(inputBean.getInstrument());
        		key.append("-");
        		key.append(inputBean.getProductType());
        		key.append("-");
        		key.append(inputBean.getTipology());
        		String keyFull = key.toString();
        		
        		// 2. Search for key with only Instrument and ProductType
        		key.setLength(0);
        		key.append(inputBean.getInstrument());
        		key.append("-");
        		key.append(inputBean.getProductType());
        		key.append("-");
        		String keyInstrumentProductType = key.toString();
        		
        		// 3. Search for key with only Instrument
        		key.setLength(0);
        		key.append(inputBean.getInstrument());
        		key.append("-");
        		key.append("-");
        		String keyInstrument = key.toString();
        		
        		// 4. Search for key with only ProductType and Tipology
        		key.setLength(0);
        		key.append("-");
        		key.append(inputBean.getProductType());
        		key.append("-");
        		key.append(inputBean.getTipology());
        		String keyProductTypeTypology = key.toString();
        		
        		// 5. Search for key with only ProductType
        		key.setLength(0);
        		key.append("-");
        		key.append(inputBean.getProductType());
        		key.append("-");
        		String keyProductType = key.toString();
        		
        		Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Searching for mapping in DomainValues, in this order : Full, Instrument+ProductType, Instrument, ProductType+Typology, ProductType");
        		
        		// retrieve mapped product, first with full key
        		if (productsMappingPhoenix.containsKey(keyFull)) {
        			mapped = productsMappingPhoenix.get(keyFull);
        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " -  > Found mapping with FULL key : " + keyFull);
        		}
        		else if (productsMappingPhoenix.containsKey(keyInstrumentProductType)) {
        			mapped = productsMappingPhoenix.get(keyInstrumentProductType);
        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " -  > Found mapping with Instrument and ProductType : " + keyInstrumentProductType);
        		}
        		else if (productsMappingPhoenix.containsKey(keyInstrument)) {
        			mapped = productsMappingPhoenix.get(keyInstrument);
        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " -  > Found mapping with Instrument : " + keyInstrument);
        		}
        		else if (productsMappingPhoenix.containsKey(keyProductTypeTypology)) {
        			mapped = productsMappingPhoenix.get(keyProductTypeTypology);
        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " -  > Found mapping with ProductType and Typology : " + keyProductTypeTypology);
        		}
        		else if (productsMappingPhoenix.containsKey(keyProductType)) {
        			mapped = productsMappingPhoenix.get(keyProductType);
        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " -  > Found mapping with ProductType : " + keyProductType);
        		}
        		
        		if (mapped.equals(EMPTY)) {
        			StringBuilder sb = new StringBuilder(" > Could not find mapping with any key (in order) : ");
        			sb.append(keyFull);
        			sb.append(", ");
        			sb.append(keyInstrumentProductType);
        			sb.append(", ");
        			sb.append(keyInstrument);
        			sb.append(", ");
        			sb.append(keyProductTypeTypology);
        			sb.append(", ");
        			sb.append(keyProductType);
        			Log.warn(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - " + sb.toString());
        		}
        		else {
        			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " -  > Mapping found : " + mapped);
        		}
        	} catch (NumberFormatException e) {
        		Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
        	}
        }

        return mapped;
    }

    /**
     * Does the mapping conversion between the UPI catalogue and Calypso. In case a is not found, it will try to
     * validate the input as a Calypso Product.
     *
     * @param product to map
     * @param errors  attached (if any)
     * @return the product in calypso
     * @throws TradeCollateralizationException
     */
    private static String getProductMapped(final Trade trade, boolean isPhoenix) {
        final String product = trade.getProductSubType();
        String mapped = EMPTY;

        try {
        	TradeCollateralizationInputBean dummyInputBean = new TradeCollateralizationInputBean();
        	dummyInputBean.setProductType(product);
            mapped = getProductMapped(dummyInputBean, new StringBuilder(), isPhoenix);
        } catch (TradeCollateralizationException e) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
            return "UNKONWN";
        }

        return mapped;
    }

    /**
     * Does the mapping conversion between the UPI catalogue and Calypso. In case a is not found, it will try to
     * validate the input as a Calypso Product.
     *
     * @param product to map
     * @param errors  attached (if any)
     * @return the product in calypso
     * @throws TradeCollateralizationException
     */
    private static String getProductMapped(final TradeCollateralizationInputBean inputBean, boolean isPhoenix) {
        String mapped = EMPTY;

        try {
            mapped = getProductMapped(inputBean, new StringBuilder(), isPhoenix);
        } catch (TradeCollateralizationException e) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, e); //sonar
            return "UNKONWN";
        }

        return mapped;
    }

    /* Just sends and empty message with the IS_COLLATERALIZED_DEAL with the message */
    public static Map<DFA_OUTPUT_FIELDS, String> buildDummyOutputMap(final String messageValue,
                                                                     final Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap, final TradeCollateralizationInputBean inputBean) {
        final Map<DFA_OUTPUT_FIELDS, String> outMap = new HashMap<DFA_OUTPUT_FIELDS, String>(
                DFA_OUTPUT_FIELDS.values().length);

        for (DFA_OUTPUT_FIELDS f : DFA_OUTPUT_FIELDS.values()) {
            outMap.put(f, EMPTY);
        }
        
        if (!inputBean.isPhoenix()) {
        	outMap.put(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL, messageValue);
        }
        else {
        	outMap.put(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL, RESPONSES.UNCOLLATERALIZED.getName());
        }
        outMap.put(DFA_OUTPUT_FIELDS.BO_EXTERNAL_REFERENCE, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.BO_EXTERNAL_REFERENCE));
        outMap.put(DFA_OUTPUT_FIELDS.BO_SOURCE_SYSTEM, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.BO_SOURCE_SYSTEM));
        
        // For Phoenix
        outMap.put(DFA_OUTPUT_FIELDS.FO_EXTERNAL_REFERENCE, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.FO_EXTERNAL_REFERENCE)); 
        outMap.put(DFA_OUTPUT_FIELDS.FO_SOURCE_SYSTEM, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.FO_SOURCE_SYSTEM)); 

        StringBuilder sb = new StringBuilder("\n");
        for (Entry<TradeCollateralizationConstants.DFA_OUTPUT_FIELDS, String> entry : outMap.entrySet()) {
        	sb.append(entry.getKey().getFieldName());
        	sb.append("=");
        	sb.append(entry.getValue());
        	sb.append("\n");
        }
        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - outMap : " + sb.toString());
        
        return outMap;
    }

    /*
     * if the fields of the input map (must be date) have the correct format. If not the case, returns the error
     * description
     */
    private static StringBuilder datesFormatAreCorrect(final Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap) {
        StringBuilder result = new StringBuilder();

        final ArrayList<DFA_INPUT_SIMULATED_FIELDS> dates = new ArrayList<DFA_INPUT_SIMULATED_FIELDS>();
        // first we retrive the date types

        for (DFA_INPUT_SIMULATED_FIELDS check : DFA_INPUT_SIMULATED_FIELDS.values()) {
            if (check.isDate()) {
                dates.add(check);
            }
        }
        // check correct format of dates: only the ones on the map
        for (DFA_INPUT_SIMULATED_FIELDS checkDates : dates) {
            if (inputMap.containsKey(checkDates) && !inputMap.get(checkDates).isEmpty()
                    && !datePattern.matcher(inputMap.get(checkDates)).matches()) {
                result.append(RESPONSES.ERR_INPUT_FORMAT_INCORRECT.getResponseValue()).append(". ");
                result.append(" Field: ").append(checkDates.getFieldName())
                        .append(" has an incorrect format. It must have the pattern ");
                result.append(dateFormat.toPattern()).append("\n");
            }
        }
        return result;
    }

    /*
     * In case a MC contract is found (only one and is suitable) it will be returned
     *
     * @param trade trade to be checked if it matches a suitable MC contract
     *
     * @return List of suitable Contracts. If everything is ok, it should appear one contract in the list.
     */
    private static List<CollateralConfig> findSuitableCollateralContractsForTrade(final Trade trade,
                                                                                  final Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap) {
        ArrayList<CollateralConfig> eligibleCollateralConfigs = null;
        final LegalEntity poLe = BOCache.getLegalEntity(DSConnection.getDefault(),
                inputMap.get(DFA_INPUT_SIMULATED_FIELDS.PROCESSING_ORG));
        final int PO_id = poLe.getId();

        Log.debug(TradeCollateralizationConstants.ENGINE_NAME, "1. getMrgCallContractForTrade -> Trade BO id = "
                + getTradeBOKeyword(trade) + ". Trade BO System" + getTradeSystemKeyword(trade));

        if (trade == null) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME,
                    "No trade received as parameter in getMrgCallContractForTrade method.");
            return null;
        }
        
        // start
        try {
            eligibleCollateralConfigs = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getEligibleCollateralConfigs(trade, PO_id);

            if (Log.isDebug()) {
            	localGetEligibleCollateralConfigs(trade, PO_id);
            }
            
        } catch (final Exception e) {
            Log.error(TradeCollateralizationLogic.class, "Error finding MCContract for the trade BO: " + getTradeBOKeyword(trade), e);
            return null;
        }
        Log.debug(TradeCollateralizationConstants.ENGINE_NAME, "2. getMrgCallContractForTrade -> end");

        return eligibleCollateralConfigs;
    }
    

    private static void localGetEligibleCollateralConfigs(Trade trade, int po_id) {
    	
    	final ArrayList<CollateralConfig> eligibleContracts = new ArrayList<CollateralConfig>();

    	// final int po_id = //trade.getBook().getLegalEntity().getId();
    	final int le_id = trade.getCounterParty().getId();

    	final String query = "select distinct mcle1.mcc_id from mrgcall_config_le mcle1, mrgcall_config_le mcle2, MRGCALL_CONFIG mc "
    			+ " where mcle1.mcc_id=mcle2.mcc_id and mc.mrg_call_def=mcle1.mcc_id " + "AND ( (mcle1.le_id = " + po_id
    			+ " and mcle1.le_role='ProcessingOrg') or mc.PROCESS_ORG_ID=" + po_id + ") " + "and ( mcle2.le_id="
    			+ le_id + " OR mc.LEGAL_ENTITY_ID= " + le_id + ")" + " union " + " select mrg_call_def  "
    			+ " from MRGCALL_CONFIG mc " + " where mc.PROCESS_ORG_ID=" + po_id + " and LEGAL_ENTITY_ID=" + le_id;

    	try {
    		RemoteSantReportingService reportingService = SantReportingUtil.getSantReportingService(DSConnection.getDefault());
    		Vector<?> result = reportingService.executeSelectSQL(query);
			
			if (result.size() > 2) {
				Vector<Integer> contractsFound = new Vector<Integer>();
				
                for (int i = 2; i < result.size(); i++) {
                    if (result.get(i) instanceof Vector) {
                        Vector<?> rawRow = (Vector<?>) result.get(i);
                        
                        for (Object value : rawRow) {
                            if (value instanceof String) {
                                contractsFound.add(Integer.valueOf((String)value));
                            }
                        }
                    }
                }
            
                for (int i = 0; i < contractsFound.size(); i++) {
                	final int mccID = (int)contractsFound.get(i);
                	CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccID);

                	// to avoid null pointer if book is empty
                	trade.setBook(marginCallConfig.getBook());
                	
                	// final CollateralConfig marginCallConfig =
                	// MarginCallConfigSQL.get(mccID, con);
                	if (marginCallConfig.getAgreementStatus().equals("CLOSED")) {
                		Log.debug(TradeCollateralizationConstants.ENGINE_NAME, "Contract ignored: Has agreement status CLOSED - " + mccID);
                	}
                	else if (!marginCallConfig.accept(trade, DSConnection.getDefault())) {
                		Log.debug(TradeCollateralizationConstants.ENGINE_NAME, "Contract ignored: Config does not accept Trade - " + mccID);
                	}
                	else {
                		eligibleContracts.add(marginCallConfig);
                	}
                }
			}
			
			Log.debug(TradeCollateralizationConstants.ENGINE_NAME, "List of suitable contracts : " + eligibleContracts.toString());
		} catch (CalypsoServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
	}

	/*
     * just returns the BOKeyword if the trade is not null
     */
    private static String getTradeBOKeyword(Trade trade) {
        return (trade != null ? trade.getKeywordValue(BO_REFERENCE_KEYWORD) : "Trade is NULL");
    }

    /*
     * returns the BO system if the trade is not null
     */
    private static String getTradeSystemKeyword(Trade trade) {
        return (trade != null ? trade.getKeywordValue(BO_SYSTEM_KEYWORD) : "Trade is NULL");
    }

    /*
     * @return a String with the result value in case the trade is not collateralizble, there more than one contract
     * match for the trade or the the PO of the input doesn't belong to the contract
     */
    private static String validateCollateralContract(Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap,
                                                     List<CollateralConfig> eligibleMarginCallConfigs) {
        StringBuilder result = new StringBuilder();
        CollateralConfig marginCallConfig = null;

        if (Util.isEmpty(eligibleMarginCallConfigs) || (eligibleMarginCallConfigs.size() > 1)) { // -> Not
            // Collateralizable
            // MC Contract NOT FOUND OR MORE than ONE MC contract FOUND
            result.append(RESPONSES.UNCOLLATERALIZED.getResponseValue());
            if (DESCRIPTIVE_ERROR_RESPONSE) {
                result.append(". ").append(RESPONSES.UNCOLLATERALIZED.getDescription());
            }
            
            StringBuilder sb = new StringBuilder();
            int numberofConfigsFound = (Util.isEmpty(eligibleMarginCallConfigs))? 0 : eligibleMarginCallConfigs.size();
            sb.append("Eligible MC Contracts number = ").append(numberofConfigsFound);
            sb.append(" - ").append(RESPONSES.UNCOLLATERALIZED.getDescription());
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract error : " + sb.toString());
        }
        // check if result is not empty
        if (result.length() > 0) {
            return result.toString();
        }

        // At this point the ONLY one MCContract candidate is retrieved and we have only one match
        marginCallConfig = eligibleMarginCallConfigs.get(0);

        /*
         * we check that the PO belongs to the contract. Otherwise we consider contract does NOT accept the trade.// PO
         * - processingOrg
         */
        return checkPOBelongsToContract(inputMap, marginCallConfig);
    }
    
    private static String checkPOBelongsToContract(Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap, CollateralConfig marginCallConfig) {
    	StringBuilder result = new StringBuilder();
    	
    	final String po = inputMap.get(DFA_INPUT_SIMULATED_FIELDS.PROCESSING_ORG);
        if (!marginCallConfig.getProcessingOrg().getAuthName().contains(po)
                && !marginCallConfig.getProcessingOrg().getName().contains(po)
                && !isAndAdditionalPOofMCContract(marginCallConfig, po)) {

            result.append(RESPONSES.ERR_PO_DIFFERENT_CONTRACT.getResponseValue());
            if (DESCRIPTIVE_ERROR_RESPONSE) {
                result.append(RESPONSES.ERR_PO_DIFFERENT_CONTRACT.getDescription()).append(". ");
                result.append("PO: ").append(po).append(" for the Mrg Contract: ").append(marginCallConfig.getName());
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("PO: ").append(po).append(" for the Mrg Contract: ").append(marginCallConfig.getName());
            sb.append(" - ").append(RESPONSES.ERR_PO_DIFFERENT_CONTRACT.getDescription());
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract error : " + sb.toString());
        }
        
        // check if result is not empty
        if (result.length() > 0) {
            return result.toString();
        }

        return null;
    }

    /*
     * Checks if the PO is a branch of the MCContract
     */
    private static boolean isAndAdditionalPOofMCContract(CollateralConfig marginCallConfig, String po) {
        for (LegalEntity le : marginCallConfig.getAdditionalPO()) {
            if (le.getAuthName().contains(po) || le.getName().contains(po)) {
                return true;
            }
        }
        return false;
    }

    /*
     * @param inputMap a map with all the input parameters obtained from the enum DFA_INPUT_FIELDS. To check MC process
     * and/or valuation dates if were given
     *
     * @param marginCallConfig is the suitable MC contract obtained previous
     *
     * @param trade trade to be checked if it matches the MC contract as parameter
     *
     * @return true if the trade is collateralized
     */
    private static boolean checkIfMCContractAcceptsTrade(final Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap,
                                                         final Trade trade, final CollateralConfig marginCallConfig) {
        JDatetime mCCvalDatetime = null;
        JDate mCCprocessDate = null;
        Date temp = null;
        
        TimeZone tz = marginCallConfig.getValuationTimeZone();
        if (tz == null) {
        	tz = TimeZone.getDefault();
        }

        if (!checkTradeAndMCContractNotNull(trade, marginCallConfig)) {
            return false;
        }
        /*
         * Finally we read the process and valuation day of the Margin Call detail. Logic Description: processing
         * received, valuation is null -> valuation is previous working day for processing. Valuation received,
         * processing is null -> processing is next previous working for valuation. Both are null -> Then processing is
         * today if is a working day, if not next working day & valuation is previous working day.
         */
        try {
            if ((inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE) != null)
                    && !inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE).isEmpty()) { // read valuation date
                temp = dateFormat.parse(inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE));
                mCCvalDatetime = new JDatetime(temp);
                Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - Input : MCC Valuation Date : " + inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE));
            }
            else {
            	Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - Input : MCC Valuation Date : null or empty");
            }
            
            if ((inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_PROCESSING_DATE) != null)
                    && !inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_PROCESSING_DATE).isEmpty()) { // read processing
                // date
                temp = dateFormat.parse(inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_PROCESSING_DATE));
                mCCprocessDate = JDate.valueOf(temp);
                Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - Input : MCC Processing Date : " + inputMap.get(DFA_INPUT_SIMULATED_FIELDS.MCC_PROCESSING_DATE));
            }
            else {
            	Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - Input : MCC Processing Date : null or empty");
            }

        } catch (ParseException e) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, "Not possible to parse MCC input dates.", e);
            return false;
        }

        /* At this point we retrieve the process and valuation date of the MC if they were given */

        if ((mCCprocessDate == null) && (mCCvalDatetime != null)) {// received valuation, we calculate process
            mCCprocessDate = getNextWorkingDay(JDate.valueOf(mCCvalDatetime), marginCallConfig);
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : MCC Process Date changed to (because Process Date null and Valuation Date not null) : " + mCCprocessDate);
        } 
        else if ((mCCprocessDate != null) && (mCCvalDatetime == null)) {// received process, we calculate valuation
            mCCvalDatetime = new JDatetime(getPreviousWorkingDay(mCCprocessDate, marginCallConfig), tz);
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : MCC Valuation Date changed to (because Process Date not null and Valuation Date null) : " + mCCvalDatetime);
        } 
        else if ((mCCprocessDate == null) && (mCCvalDatetime == null)) { // neither of them, we consider current date
            // as process
            // take today day if process date, if not take following day
            mCCprocessDate = JDate.getNow();
            mCCprocessDate.addBusinessDays(0, marginCallConfig.getBook().getHolidays());
            // and the valuation day as previous working day of the process date
            mCCvalDatetime = new JDatetime(getPreviousWorkingDay(mCCprocessDate, marginCallConfig), tz);
            
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : MCC Process Date changed to (because dates in input are null) : " + mCCprocessDate);
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : MCC Valuation Date changed to (because dates in input are null) : " + mCCvalDatetime);
        }

        // we check if process/valuation day are working day
        if (mCCprocessDate.isWeekEndDay() || JDate.valueOf(mCCvalDatetime).isWeekEndDay()) {
            mCCprocessDate = getNextWorkingDay(mCCprocessDate, marginCallConfig);
            mCCvalDatetime = new JDatetime(getPreviousWorkingDay(mCCprocessDate, marginCallConfig), tz);
            
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : MCC Process Date changed to (because isWeekEndDay) : " + mCCprocessDate);
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : MCC Valuation Date changed to (because isWeekEndDay) : " + mCCvalDatetime);
        }

        // if process day equals today, it might discriminate the operation as not collateralized. This is a small fix
        if (trade.getTradeDate() != null && trade.getTradeDate().getJDate(tz).equals((mCCprocessDate))) {
            mCCvalDatetime = new JDatetime(mCCprocessDate, tz);
            mCCprocessDate = getNextWorkingDay(mCCprocessDate, marginCallConfig);
            
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : MCC Process Date changed to (because Trade Date = Process Date) : " + mCCprocessDate);
            Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : MCC Valuation Date changed to (because Trade Date = Process Date) : " + mCCvalDatetime);
        }
        
        // Main SDF to check that the trade is accepted by the contract
        // accept(Trade trade, JDatetime valDatetime, JDate processDate, DSConnection ds)
        boolean res = false;
        
        // First try, with a catch on NullPointer as sometimes Calypso requires some date but Phoenix does not always send it.
        // Date is not always needed so we cannot simply check for its presence in code.
        try {
        	res = marginCallConfig.accept(trade, mCCvalDatetime, mCCprocessDate, DSConnection.getDefault());
        }
        catch (NullPointerException e) {
        	// Some fields like StartDate are NOT mandatory but can sometimes cause a NullPointer in Calypso Code if they are not present...
        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - Controlled NullPointerException Error in accept() method of Calypso.");
        	Log.error(TradeCollateralizationConstants.ENGINE_NAME, e);
        	
        	if (trade.getProduct() instanceof Repo && ((SecFinance)trade.getProduct()).getStartDate() == null) {
        		if (((Repo)trade.getProduct()).getCash() == null) {
					Cash cash = new Cash();
					((Repo)trade.getProduct()).setCash(cash);
				}
        		((Repo)trade.getProduct()).setStartDate(JDate.getNow());
        		Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - Forcing StartDate of Repo to : " + JDate.getNow());
        	}
        	
        	// Second try : we tried adding Start Date. Other date may still be missing...
        	// As said before : Calypso accept() method may or may not need a date, that may or may not be sent by Phoenix...
        	try {
        		res = marginCallConfig.accept(trade, mCCvalDatetime, mCCprocessDate, DSConnection.getDefault());
        	}
        	catch (NullPointerException e2) {
        		Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - Controlled NullPointerException Error in accept() method of Calypso (after trying to add StartDate for Repos).");
        		Log.error(TradeCollateralizationConstants.ENGINE_NAME, e2);
        	}
        }
        
        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputMap.get(DFA_INPUT_SIMULATED_FIELDS.ID) + " - MC Contract validation : Calypso accept() method result : " + res);
        
        return res;
    }

    /*
     * builds the final output map
     */
    private static Map<DFA_OUTPUT_FIELDS, String> buildOutputMapFromTradeAndContract(final Trade trade,
                                                                                     final CollateralConfig marginCallConfig, final boolean isCollateralizable, final String mappedProduct,
                                                                                     Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap, TradeCollateralizationInputBean inputBean) {
        final Map<DFA_OUTPUT_FIELDS, String> outMap = new HashMap<DFA_OUTPUT_FIELDS, String>(
                DFA_OUTPUT_FIELDS.values().length);
        String collateralizationGrade = null;

        if (!checkTradeAndMCContractNotNull(trade, marginCallConfig)) {
            return buildDummyOutputMap(null, inputMap, inputBean);
        }

        for (DFA_OUTPUT_FIELDS f : DFA_OUTPUT_FIELDS.values()) {
            outMap.put(f, EMPTY);
        }

        /* After checking the match with a contract, the grade is generated here */
        if (isCollateralizable) { // Trade IS COLLATERALIZABLE, get the degree
            collateralizationGrade = buildCollaterizedDegreeResponse(marginCallConfig, trade, inputBean);
        } else { // Trade NOT COLLATERALIZABLE
            collateralizationGrade = RESPONSES.UNCOLLATERALIZED.getResponseValue().toString();
        }

        outMap.put(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL, collateralizationGrade); // 1
        if (!Util.isEmpty(trade.getKeywordValue(BO_REFERENCE_KEYWORD))) {
        	outMap.put(DFA_OUTPUT_FIELDS.BO_EXTERNAL_REFERENCE, trade.getKeywordValue(BO_REFERENCE_KEYWORD)); // 2
        }
        if (!Util.isEmpty(trade.getKeywordValue(BO_SYSTEM_KEYWORD))) {
        	outMap.put(DFA_OUTPUT_FIELDS.BO_SOURCE_SYSTEM, trade.getKeywordValue(BO_SYSTEM_KEYWORD)); // 3
        }

        if (trade.getTradeDate() != null) {
            outMap.put(DFA_OUTPUT_FIELDS.VALUE_DATE, dateFormat.format(trade.getTradeDate()));// 4
        }

        if (isCollateralizable) { // Trade NOT COLLATERALIZABLE
            outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_NAME, marginCallConfig.getName()); // 5
            outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_TYPE, marginCallConfig.getContractType()); // 6
        } 

        if (trade.getProduct() != null) {
            outMap.put(DFA_OUTPUT_FIELDS.PRODUCT_TYPE, mappedProduct); // 7
        }

        if (isCollateralizable) { // Trade NOT COLLATERALIZABLE
            outMap.put(DFA_OUTPUT_FIELDS.CONTRACT_DIRECTION, SantCollateralConfigUtil.getContractDirectionV14Value(marginCallConfig)); // 8

            if ((marginCallConfig.getStartingDate() != null)) {
                outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_START_DATE,
                        dateFormat.format(marginCallConfig.getStartingDate().getTime())); // 9
            }

            if ((marginCallConfig.getClosingDate() != null)) {
                outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_END_DATE,
                        dateFormat.format(marginCallConfig.getClosingDate().getTime())); // 10
            }
            
            // For Phoenix
            if (inputBean.isPhoenix()) {
            	if (!Util.isEmpty(marginCallConfig.getAdditionalField("EMIR_COLLATERAL_VALUE"))) {
            		outMap.put(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL, marginCallConfig.getAdditionalField("EMIR_COLLATERAL_VALUE"));
            	}
            	else {
            		outMap.put(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL, RESPONSES.UNCOLLATERALIZED.getName());
            	}
            	
            	outMap.put(DFA_OUTPUT_FIELDS.CONTRACT_ID, String.valueOf(marginCallConfig.getId()));
            	outMap.put(DFA_OUTPUT_FIELDS.CONTRACT_NAME, String.valueOf(marginCallConfig.getName()));
            	outMap.put(DFA_OUTPUT_FIELDS.IS_TRIPARTY, Boolean.toString(marginCallConfig.isTriParty()));
            	String tripartyAgent = marginCallConfig.getTripartyAgent();
            	if (!Util.isEmpty(tripartyAgent)) {
            		outMap.put(DFA_OUTPUT_FIELDS.TRIPARTY_AGENT, tripartyAgent);
            	}

            	String imGlobalId = marginCallConfig.getAdditionalField("IM_GLOBAL_ID");
            	if (!Util.isEmpty(imGlobalId)) {
            		outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_PORTFOLIO_CODE, imGlobalId);
            	}
            	else {
            		outMap.put(DFA_OUTPUT_FIELDS.COLLATERAL_PORTFOLIO_CODE, String.valueOf(marginCallConfig.getId()));
            	}
            }
        }
        else if (inputBean.isPhoenix()) {
        	collateralizationGrade = RESPONSES.UNCOLLATERALIZED.getName();
        	outMap.put(DFA_OUTPUT_FIELDS.IS_COLLATERALIZED_DEAL, collateralizationGrade);
        }
        
        if (inputBean.isPhoenix()) {
        	outMap.put(DFA_OUTPUT_FIELDS.FO_EXTERNAL_REFERENCE, inputBean.getFOExternalReference()); 
            outMap.put(DFA_OUTPUT_FIELDS.FO_SOURCE_SYSTEM, inputBean.getFOSourceSystem()); 
            
        }
        
        return outMap;
    }

    /*
     * When a trade has matched a MCContract and is considered collateralized, this methods returns the value of the
     * appropiate collateralization category type (0,1 or 2)
     */
    private static String buildCollaterizedDegreeResponse(final CollateralConfig contract, final Trade trade,
                                                          final TradeCollateralizationInputBean inputBean) {
    	String result = "";

        if (!checkTradeAndMCContractNotNull(trade, contract)) { // error
            result = (RESPONSES.ERR_EXCEPTION_OCCURRED.getResponseValue() + ": " + RESPONSES.ERR_EXCEPTION_OCCURRED.getDescription());
        }
        else {
        	RESPONSES response;
	        /* Perform the collateralizetion degree generation */
	        if (contract != null && contract.getContractDirection().equals(CollateralConfig.NET_UNILATERAL)) {
	            // degree 0 - UNILATERAL
	        	response = RESPONSES.ONE_WAY_COLLATERALIZED;
	
	        } else { // must be BILATERAL
	            final JDate tradeValuation = getTradeValuationDate(inputBean);
	
	            if (CollateralizedTradesReportLogic.checkBilateralHasThreshold(contract, tradeValuation)) {
	                // degree 1 - PARTIAL
	            	response = RESPONSES.PARTIAL_COLLATERALIZED;
	
	            } else {
	                // degree 2 - FULL
	            	response = RESPONSES.FULL_COLLATERALIZED;
	            }
	        }
	        
	        if (!inputBean.isPhoenix()) {
	        	result = String.valueOf(response.getResponseValue());
	        }
	        else {
	        	result = response.getName();
	        }
        }

        return result;
    }

    // returns valuation date of the trade. If not found, returns today
    private static JDate getTradeValuationDate(TradeCollateralizationInputBean inputBean) {
        JDate value = null;

        if ((inputBean.getValueDate() != null) && !inputBean.getValueDate().isEmpty()) {
            value = JDate.valueOf(inputBean.getValueDate());
        } else {
            value = JDate.getNow();
        }

        return value;
    }

    /**
     * To cache contracts it has to be done for each day, as in the online DFA they can request for another day and the
     * contract might have changed
     *
     * @param trade    from which date is read
     * @param contract to get the contract String unique ID
     * @return
     */
    private static String generateDFAKey(Trade trade, CollateralConfig contract) {
        if (!checkTradeAndMCContractNotNull(trade, contract)) {
            return EMPTY;
        }

        final JDate date = getMatrixValuationDay(trade);

        return contract.getKey() + date.toString();
    }

    /*
     * Just checks that trade and MCContract have been passed as parameters
     */
    private static boolean checkTradeAndMCContractNotNull(Trade trade, CollateralConfig contract) {
        if (trade == null) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME,
                    "No trade received as parameter in buildOutputMapFromTradeAndContract method.");
            return false;
        }
        if (contract == null) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME,
                    "No marginCallConfig received as parameter in buildOutputMapFromTradeAndContract method.");
            return false;
        }
        return true;
    }

    /**
     * returns the previous workind day
     */
    public static JDate getPreviousWorkingDay(JDate from, CollateralConfig marginCallConfig) {
        return getNextOrPreviousWorkingDay(from, marginCallConfig, false);
    }

    /**
     * returns the next workind day
     */
    private static JDate getNextWorkingDay(JDate from, CollateralConfig marginCallConfig) {
        return getNextOrPreviousWorkingDay(from, marginCallConfig, true);
    }

    /**
     * returns previous or next workind day based on the boolean param
     */
    private static JDate getNextOrPreviousWorkingDay(JDate from, CollateralConfig marginCallConfig, boolean NextOrPrev) {
        int what = 0;
        JDate day = null;

        if (NextOrPrev) { // based on boolean: true -> next day. False -> previous day
            what = 1;
        } else {
            what = -1;
        }
        day = from.addBusinessDays(what, marginCallConfig.getBook().getHolidays());
        // not really necessary...
        while (day.isWeekEndDay()) {
            day = day.addBusinessDays(what, marginCallConfig.getBook().getHolidays());
        }
        return day;
    }

    /**
     * Ensures to take the valuation matrix date to be used by the trade if the contract has a threshold
     *
     * @param trade
     * @return
     */
    private static JDate getMatrixValuationDay(Trade trade) {
        JDate valuation = null;

        if ((trade != null) && (trade.getTradeDate() != null)) {
            valuation = JDate.valueOf(trade.getTradeDate());

        } else {
            valuation = JDate.getNow();
        }

        return valuation;
    }
}
