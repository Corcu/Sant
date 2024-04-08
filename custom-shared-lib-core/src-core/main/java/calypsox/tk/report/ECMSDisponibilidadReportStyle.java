/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.HaircutUtil;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InstantiateUtil;
import org.jfree.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static calypsox.tk.report.BOSecurityPositionReportTemplate.*;

public class ECMSDisponibilidadReportStyle
        extends com.calypso.tk.report.BOSecurityPositionReportStyle {

    private static final long serialVersionUID = 2219632537376778702L;


    public static final String CONCILIATION_PRICE_D = "Conciliation Price D";
    public static final String CONCILIATION_PRICE = "Conciliation Price D-1";
    public static final String CONCILIATION_PRICE_2 = "Conciliation Price D-2";
    public static final String INV_PRODUCT = "INV_PRODUCT";

    public static final String REPORT_DATE = "Report Date";

    /*
     * EFECTIVO como precio*nominal (en divisa origen -currency de la equity-)
     * para los IM en titulos
     */
    public static final String MARKET_VALUE = "Market Value";
    /*
     * EFECTIVO_EUR como precio*nominal (en divisa origen) para los IM en
     * titulos con el fixing D-1 a EUR
     */
    public static final String MARKET_VALUE_EUR = "Market Value EUR Fix";
    /*
     * String as FX.EUR.CCY=xx.xx with the fixing of yesterday close
     */
    public static final String FX_FIXING = "EUR Fixing";
    /*
     * Direction, Pay for nominal < 0 , receive >= 0
     */
    public static final String DIRECTION = "Direction";

    /*
     * Security Price
     */
    public static final String PRICE = "Price D-1";

    /*
     * J Issuer
     */
    public static final String JISSUER = "J Issuer";

    /*
     * Row Numbre ID
     */
    public static final String ROWID = "Row Id";

    /*
     * Nominal
     */
    public static final String NOMINAL = BOSecurityPositionReportTemplate.NOMINAL_PROPERTY;

    /**
     * Collateral Config Style
     */
    private CollateralConfigReportStyle collateralConfigReportStyle = null;

    /**
     * Prefix to identify a MarginCallConfig column
     */
    private static String MARGIN_CALL_CONFIG_PREFIX = "MarginCallConfig.";

    // Is MarginCall trade eligible in its contract
    public static final String ELIGIBLE = "Is Position Eligible";

    // Is format double
   	DecimalFormat df = new DecimalFormat("#,##0.00");

    //ECMS COLUMNS
    public static final String PROCESSING_ORG = "ECMS.ProcessingOrg";
    public static final String STATUS = "ECMS.Status";
    public static final String ISIN = "ECMS.ISIN";
    public static final String EMISOR_ISIN = "ECMS.Emisor ISIN";
    public static final String LAST_UPDATE = "ECMS.LastUpdate";
    public static final String CARTERA = "ECMS.Cartera";
    public static final String DESK = "ECMS.Desk";
    public static final String CUENTA_ASOCIADA = "ECMS.Cuenta asociada";
    public static final String TOTAL_ASSETS = "ECMS.Total Assets (Outstanding Nominal)";
    public static final String TOTAL_ASSETS_POR_PO = "ECMS.Total Asset por PO e ISIN";
    public static final String TOTAL_PORTFOLIO = "ECMS.Total Available by Portfolio";
    public static final String FECHA_VENCIMIENTO = "ECMS.Fecha de vencimiento de la emision";
    public static final String DESCRIPCION_ISIN = "ECMS.Descripcion del ISIN";
    public static final String THEORETICAL_VALUE = "ECMS.Theoretical Value";
    public static final String PLEDGED_ECB = "ECMS.Pledged ECB";
    public static final String VALUE_PLEDGED_ECB = "ECMS.Value Pledged ECB";
    public static final String OTHER_PLEDGES = "ECMS.Other Pledges";
    public static final String BEN_OTHER_PLEDGES = "ECMS.Beneficiary Other Pledges";
    public static final String TOTAL_PLEDGED = "ECMS.Total pledged";
    public static final String TOTAL_AVAILABLE = "ECMS.Total available";
    public static final String QUANTITY = "ECMS.Face Value";
    public static final String INTERNAL_PRICE = "ECMS.Internal Price";
	public static final String AGREEGATE_POSITION_BY_STM_BOOKS = "ECMS.Agreegate Position By STM Books";
	public static final String ECMS_LIST_OF_BOOKS_TO_STM_DESK = "ECMS.List Of Books To STM Desk";
	public static final String ECMS_AVAIBLE_LIST_OF_BOOKS_TO_SCF = "ECMS.Avaible List Of Books To SCF";
	public static final String IS_ECMS_ACCOUNT = "ECMS.Is ECMS Account";
	
	private static final String OTHER_PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_OTHER_PLEDGE_ACCOUNTS";
	private static final String ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_PLEDGE_ACCOUNTS";
	private static final String DV_SEPARATOR = ";";



    private static final Map<String, QuoteSet> quotes = new HashMap<>();
    private static final Map<String, QuoteValue> quoteValues = new HashMap<>();

    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnId, Vector errors)
            throws InvalidParameterException {
    	// Is format double
    	df.setGroupingUsed(false);
        ArrayList<String> accountsFinal = new ArrayList<>();
        Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(), "ECMS_PLEDGE_ACCOUNTS");

        for(String account: pledgeAccounts){
            if(!accountsFinal.contains(account)) {
                accountsFinal.add(account);
            }
        }

//
//        String[] accounts = new String[4];
//        accounts[0] = LocalCache.getDomainValues(DSConnection.getDefault(), "SCF_EUROCLEAR_ECMS_PLEDGE_ACCOUNT").get(0);
//        accounts[1] = LocalCache.getDomainValues(DSConnection.getDefault(), "MAD_EUROCLEAR_ECMS_PLEDGE_ACCOUNT").get(0);
//        accounts[2] = LocalCache.getDomainValues(DSConnection.getDefault(), "SCF_IBERCLEAR_ECMS_PLEDGE_ACCOUNT").get(0);
//        accounts[3] = LocalCache.getDomainValues(DSConnection.getDefault(), "MAD_IBERCLEAR_ECMS_PLEDGE_ACCOUNT").get(0);

        String ecmsAccountName = "";
        String ecmsAccountName2 = "";

        Inventory inventory = row.getProperty(ReportRow.INVENTORY);
        Product product = row.getProperty(INV_PRODUCT);
        if(product == null) 
           product = row.getProperty("Product");
        JDate valDate = JDate.valueOf(ReportRow.getValuationDateTime(row));
        QuoteSet quoteSet = new QuoteSet();
    	String poName = inventory.getBook().getLegalEntity().getCode();
		double nominal = getNominal(row, inventory);
        try {
            boolean existProperty = row.getProperties().containsKey(QUOTE_SET_PROPERTY);
            if (existProperty) {
                String quoteSetName = row.getProperty(QUOTE_SET_PROPERTY);
                if (quotes.containsKey(quoteSetName)) {
                    quoteSet = quotes.get(quoteSetName);
                } else {
                    quoteSet = DSConnection.getDefault().getRemoteMarketData().getQuoteSet(row.getProperty(QUOTE_SET_PROPERTY));
                    quotes.put(quoteSetName, quoteSet);
                }
            }
        } catch (CalypsoServiceException e) {
            com.calypso.tk.core.Log.error(this, e.getCause());
        }

        PricingEnv pricingEnv = row.getProperty(PRICING_ENV_PROPERTY);

        if (inventory == null) {
            throw new InvalidParameterException(
                    "Invalid row " + row + ". Cannot locate Inventory object");
        }

        if (SantProductCustomDataReportStyle
                .isProductCustoDataColumn(columnId)) {
            return getProductCustomDataReportStyle().getColumnValue(row,
                    columnId, errors);
        }

        if (PROCESSING_ORG.equals(columnId)) {
            if (Util.isEmpty(poName)) {
                return "";
            } else return poName;
        } else if (STATUS.equals(columnId)) {
            if (Util.isEmpty(poName)) {
                return "";
            } else {

                if (poName.equalsIgnoreCase("BSTE")) {
                    String bste = product.getSecCode("ECMS_Eligibility_BSTE");
                    if (bste != null && bste.equalsIgnoreCase("Y")) return "Y";
                    else return "N";
                }

                if (poName.equalsIgnoreCase("BFOM")) {
                    String bfom = product.getSecCode("ECMS_Eligibility_BFOM");
                    if (bfom != null && bfom.equalsIgnoreCase("Y")) return "Y";
                    else return "N";
                }

            }
        } else if (ISIN.equals(columnId)) {
            if (product.getSecCode("ISIN") != null) {
                return product.getSecCode("ISIN");
            } else return "No ISIN Code";
        } else if (EMISOR_ISIN.equals(columnId)) {
            if (product.getIssuerIds() != null && !product.getIssuerIds().isEmpty()) {
                LegalEntity issuer = LegalEntity.valueOf((Integer) product.getIssuerIds().get(0));
                if (issuer != null) {
                    return issuer.getName();
                } else return "NONE";
            } else return "NONE";

        } else if (LAST_UPDATE.equals(columnId)) {
            JDatetime valDateTime = row.getProperty("CurrentDateTime");
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(valDateTime);
            date = date.replace(" ", "T");
            return date;
        } else if (CURRENCY.equals(columnId)) {
            String ccy = (String) super.getColumnValue(row, "Account.Currency", errors);
            if (Util.isEmpty(ccy)) {
                return "";
            }
        } else if (CARTERA.equalsIgnoreCase(columnId)) {
            return inventory.getBook().getName();
        } else if (DESK.equals(columnId)) {
            if (inventory.getBook() != null) {
                return inventory.getBook().getAttribute("Desk");
            }
        } else if (CUENTA_ASOCIADA.equals(columnId)) {
            return super.getColumnValue(row, "Account", errors);
        } else if (TOTAL_ASSETS.equals(columnId)) {
            double nominalTotalAsset = getNominal(row, inventory);
            if (product instanceof Bond) {
                double principal = row.getProperty("Principal");
                return nominalTotalAsset * principal;
            } else if (product instanceof Equity) {
                return nominalTotalAsset;

            }
        } else if (TOTAL_ASSETS_POR_PO.equals(columnId)) {
            boolean existProperty = row.getProperties().containsKey("TOTAL_ASSETS_POR_PO");
            if (existProperty) {
                double nominalTotalAssets = row.getProperty("TOTAL_ASSETS_POR_PO");
                if (product instanceof Bond) {
                    double principal = row.getProperty("Principal");
                    return nominalTotalAssets * principal;
                } else if (product instanceof Equity) {
                    return nominalTotalAssets;
                }
            } else Double.toString(0.00);
        } else if (TOTAL_PORTFOLIO.equals(columnId)) {
            boolean existProperty = row.getProperties().containsKey("TOTAL_PORTFOLIO");
            if (existProperty) {
                double nominalTotalPort = row.getProperty("TOTAL_PORTFOLIO");
                if (product instanceof Bond) {
                    double principal = row.getProperty("Principal");
                    return nominalTotalPort * principal;
                } else if (product instanceof Equity) {
                    return nominalTotalPort;
                }
            } else return Double.toString(0.00);


        } else if (FECHA_VENCIMIENTO.equals(columnId)) {
            JDate matDate = product.getMaturityDate();
            if (matDate != null) {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(matDate.getDate());
                return date;
            } else return "";
        } else if (DESCRIPCION_ISIN.equals(columnId)) {
            return product.getDescription();
        } else if (THEORETICAL_VALUE.equals(columnId)) {
            Double nom = getNominal(row, inventory);
            if (inventory.getBook() != null && inventory.getBook().getLegalEntity() != null) {
                if (Util.isEmpty(poName)) {
                    return Double.toString(0.00);
                }

                double haircut = 0;

                if (inventory.getAgent() != null) {
                    String issuer = inventory.getAgent().getCode();
                    if (issuer.equalsIgnoreCase("NONE")) {
                        return Double.toString(0.00);
                    }
                    if (issuer != null) {
                        if (poName.equalsIgnoreCase(issuer)) {
                            if (product.getSecCode("ECMS_Haircut_Own_Use") != null)
                                haircut = Double.parseDouble(product.getSecCode("ECMS_Haircut_Own_Use"));
                        } else {
                            if (product.getSecCode("ECMS_Haircut") != null)
                                haircut = Double.parseDouble(product.getSecCode("ECMS_Haircut"));
                        }
                    }
                }

                if (nom != 0.0) {
                    double price = 0.0;
                    JDate startDate = row.getProperty("StartDate");
                    startDate = startDate.addBusinessDays(1, Util.string2Vector("SYSTEM"));
                    if (product instanceof Bond) {
                        //AJUSTA LA DIVISION ENTRE 100
                        QuoteValue qv = getProductQuote(quoteSet, product, startDate, pricingEnv.getName());
                        if (qv != null) {
                            price = qv.getClose();
                            if (price != 0.00) {
                                double principal = row.getProperty("Principal");
                                return nom * (price - (haircut / 100)) * principal;
                            } else return Double.toString(0.00);
                        } else {
                            JDate newDate = startDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                            for (int i = 0; i < 5; i++) {
                                qv = getProductQuote(quoteSet, product, newDate, pricingEnv.getName());
                                if (qv != null) {
                                    price = qv.getClose();
                                    if (price != 0.00) {
                                        double principal = row.getProperty("Principal");
                                        return nom * (price - (haircut / 100)) * principal;
                                    } else return Double.toString(0.00);
                                }
                                newDate = newDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                            }
                        }
                        return nom;
                    } else if (product instanceof Equity) {
                        QuoteValue qv = getProductQuote(quoteSet, product, startDate, "OFFICIAL");
                        if (qv != null) {
                            price = qv.getClose();
                            if (price != 0.00) {
                                return nom * (price - (price * haircut) / 100);
                            } else return Double.toString(0.00);
                        } else {
                            JDate newDate = startDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                            for (int i = 0; i < 5; i++) {
                                qv = getProductQuote(quoteSet, product, newDate, pricingEnv.getName());
                                if (qv != null) {
                                    price = qv.getClose();
                                    if (price != 0.00) {
                                        return nom * (price - (price * haircut) / 100);
                                    } else return Double.toString(0.00);
                                }
                                newDate = newDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                            }
                        }
                        return nom;
                    } else return Double.toString(0.00);

                } else
                    return Double.toString(0.00);
            }
            return nom;
        } else if (PLEDGED_ECB.equals(columnId)) {
        	return Double.toString(getPledgeECB(inventory,row,accountsFinal));

        } else if (VALUE_PLEDGED_ECB.equals(columnId)) {

            Account cuenta = inventory.getAccount();
            for (int acc = 0; acc < accountsFinal.size(); acc++) {
                //EUROCLEAR
                ecmsAccountName = accountsFinal.get(acc).split(";")[3];
                //CUENTA NOMBRE LARGO
                ecmsAccountName2 = accountsFinal.get(acc).split(";")[1];

                if (cuenta != null && (cuenta.getName().equalsIgnoreCase(ecmsAccountName)
                        || cuenta.getName().equalsIgnoreCase(ecmsAccountName2))) {
                    if (row.getProperty("ECB_Contrario") != null) {
                        double totalECB = row.getProperty("ECB_Contrario");
                        if (inventory.getBook() != null && inventory.getBook().getLegalEntity() != null) {
                            if (Util.isEmpty(poName)) {
                                return Double.toString(0.00);
                            }

                            double haircut = 0;

                            if (inventory.getAgent() != null) {
                                String issuer = inventory.getAgent().getCode();
                                if (issuer.equalsIgnoreCase("NONE")) {
                                    return Double.toString(0.00);
                                }
                                if (issuer != null) {
                                    if (poName.equalsIgnoreCase(issuer)) {
                                        if (product.getSecCode("ECMS_Haircut_Own_Use") != null)
                                            haircut = Double.parseDouble(product.getSecCode("ECMS_Haircut_Own_Use"));
                                    } else {
                                        if (product.getSecCode("ECMS_Haircut") != null)
                                            haircut = Double.parseDouble(product.getSecCode("ECMS_Haircut"));
                                    }
                                }
                            }

                            if (totalECB != 0.0) {
                                double price = 0.0;
                                JDate startDate = row.getProperty("StartDate");
                                startDate = startDate.addBusinessDays(1, Util.string2Vector("SYSTEM"));
                                if (product instanceof Bond) {
                                    QuoteValue qv = getProductQuote(quoteSet, product, startDate, pricingEnv.getName());
                                    if (qv != null) {
                                        price = qv.getClose();
                                        if (price != 0.00) {
                                            //SI eres un bono, restamos el haircut
                                            return new Amount(totalECB * (price - (haircut / 100)), 2);
                                        } else return new Amount(totalECB, 2);
                                    } else {
                                        JDate newDate = startDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                                        for (int i = 0; i < 5; i++) {
                                            qv = getProductQuote(quoteSet, product, newDate, pricingEnv.getName());
                                            if (qv != null) {
                                                price = qv.getClose();
                                                if (price != 0.00) {
                                                    return new Amount(totalECB * (price - (haircut / 100)), 2);
                                                } else return 0;
                                            }
                                            newDate = newDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                                        }
                                    }
                                    row.setProperty("VALUE_PLEDGED_ECB", totalECB);
                                    return new Amount(totalECB, 2);
                                } else if (product instanceof Equity) {
                                    QuoteValue qv = getProductQuote(quoteSet, product, startDate, "OFFICIAL");
                                    if (qv != null) {
                                        price = qv.getClose();
                                        if (price != 0.00) {
                                            return new Amount(totalECB * (price - (price * haircut) / 100), 2);
                                        } else return new Amount(totalECB, 2);
                                    } else {
                                        JDate newDate = startDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                                        for (int i = 0; i < 5; i++) {
                                            qv = getProductQuote(quoteSet, product, newDate, pricingEnv.getName());
                                            if (qv != null) {
                                                price = qv.getClose();
                                                if (price != 0.00) {
                                                    return new Amount(totalECB * (price - (price * haircut) / 100), 2);
                                                } else return Double.toString(0.00);
                                            }
                                            newDate = newDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                                        }
                                    }

                                    row.setProperty("VALUE_PLEDGED_ECB", totalECB);
                                    return new Amount(totalECB, 2);
                                } else return Double.toString(0.00);

                            } else
                                return Double.toString(0.00);
                        }
                    }
                }
            }
            return Double.toString(0.00);

        } else if (OTHER_PLEDGES.equals(columnId)) {
            boolean existProperty = row.getProperties().containsKey("OTHER_PLEDGES");
            if (existProperty) {
                double nominalOtherPledges = row.getProperty("OTHER_PLEDGES");
                if (product instanceof Bond) {
                    double principal = row.getProperty("Principal");
                    return Double.toString(Math.abs(nominalOtherPledges * principal));
                } else if (product instanceof Equity) {
                    return Double.toString(Math.abs(nominalOtherPledges));
                }
            }
            return Double.toString(0.00);
        } else if (BEN_OTHER_PLEDGES.equals(columnId)) {
            return "";
        } else if (TOTAL_PLEDGED.equals(columnId)) {
            Account cuenta = inventory.getAccount();
            Double value = 0.0;
            for (int acc = 0; acc < accountsFinal.size(); acc++) {
                value = 0.0;
                //EUROCLEAR
                ecmsAccountName = accountsFinal.get(acc).split(";")[3];
                //CUENTA NOMBRE LARGO
                ecmsAccountName2 = accountsFinal.get(acc).split(";")[1];

                if (cuenta != null && (cuenta.getName().equalsIgnoreCase(ecmsAccountName)
                        || cuenta.getName().equalsIgnoreCase(ecmsAccountName2))) {
                    if (row.getProperty("ECB_Contrario") != null)
                        value += (Double)row.getProperty("ECB_Contrario");

                    boolean existProperty = row.getProperties().containsKey("OTHER_PLEDGES");
                    if (existProperty) {
                        double nominalOtherPledges = row.getProperty("OTHER_PLEDGES");
                        if (product instanceof Bond) {
                            double principal = row.getProperty("Principal");
                            value += (Math.abs(nominalOtherPledges * principal));
                        } else if (product instanceof Equity) {
                            value += (Math.abs(nominalOtherPledges));
                        }
                    }
                    return Double.toString(value);
                }
            }
            return Double.toString(value);

        //} else if (TOTAL_AVAILABLE.equals(columnId)) {
        //    return row.getProperty("TOTAL_AVAILABLE");
        } else if (QUANTITY.equals(columnId)) {
            if (product instanceof Bond) {
                return row.getProperty("FaceValue");
            } else if (product instanceof Equity) {
                return Double.toString(0.00);

            }
        } else if (INTERNAL_PRICE.equals(columnId)) {
            if (product instanceof Bond) {
                Bond b = (Bond) product;
                return b.getDirtyPriceBase();
            } else if (product instanceof Equity) {
                return "Equity";
            }
        } else if (AGREEGATE_POSITION_BY_STM_BOOKS.equals(columnId)) {
	          return row.getProperty("TOTAL_ASSET_STM") == null ? Double.toString(0.00) : row.getProperty("TOTAL_ASSET_STM"); 
								
		} else if (ECMS_LIST_OF_BOOKS_TO_STM_DESK.equals(columnId)) {
			Vector<String> listOfBooksSTMDesk = LocalCache.getDomainValues(DSConnection.getDefault(),
					"ECMSListOfBooksToSTMDesk");
			String bookDesk = "";
			int cont=0;
			if (!listOfBooksSTMDesk.isEmpty()) {
				for (String book : listOfBooksSTMDesk) {
					cont++;
					if(listOfBooksSTMDesk.size() > cont) {
						bookDesk = bookDesk + book + ";";
					} else 
					bookDesk = bookDesk + book;
				}
			} 
				return bookDesk;

		} else if (ECMS_AVAIBLE_LIST_OF_BOOKS_TO_SCF.equals(columnId)) {
			Vector<String> availableListOfBooksToSCF = LocalCache.getDomainValues(DSConnection.getDefault(),
					"ECMSAvailableListOfBooksToSCF");
			String bookSCF = "";
			int cont=0;
			if (!availableListOfBooksToSCF.isEmpty()) {
				for (String book : availableListOfBooksToSCF) {
					cont++;
					if(availableListOfBooksToSCF.size()>cont) {
						bookSCF = bookSCF + book + ";";
					} else
					bookSCF = bookSCF + book;
				}
			} 
				return bookSCF;

		} else if (IS_ECMS_ACCOUNT.equals(columnId)) {
			Vector<String> pledgeAccs = LocalCache.getDomainValues(DSConnection.getDefault(), "ECMS_PLEDGE_ACCOUNTS");
			if(!pledgeAccs.isEmpty()) {
			for (String account : pledgeAccs) {

				accountsFinal.add(account.split(";")[1]);
			}
			if (!accountsFinal.isEmpty() && (inventory.getAccount()!=null && accountsFinal.contains(inventory.getAccount().getName()))) {
				return "Y";
			} else
				return "N";
			} else 
				return "N";
		} else if (TOTAL_AVAILABLE.equals(columnId)) {
			ArrayList<String> pledgeAccountsDV = getEcmsAccounts();
			ArrayList<String> other_pledges = getEcmsAccounts();
			double otherPledgeNominal = getOtherPledgeNominal(product, row);
			double pledgeECBNominal = getPledgeECB(inventory, row, accountsFinal);
            double nominalTotalAsset = getNominal(row, inventory);
            double totalAvailable = 0.0D;

            if(inventory.getAccount()!= null) {
    			if (!pledgeAccountsDV.contains(inventory.getAccount().getName())
    					&& (!other_pledges.contains(inventory.getAccount().getName()))) {
    				
    				if (product instanceof Bond) {
    	                double principal = row.getProperty("Principal");	     
    	                totalAvailable = (nominalTotalAsset * principal) + pledgeECBNominal + otherPledgeNominal;
    	            } else if (product instanceof Equity) {
    	            	totalAvailable = (nominalTotalAsset + pledgeECBNominal + otherPledgeNominal);
    	            
    	            }
    				
    			} else if (pledgeAccountsDV.contains(inventory.getAccount().getName())) {
    				if (product instanceof Bond) {
    	                double principal = row.getProperty("Principal");
    	                totalAvailable =(nominalTotalAsset * principal) + otherPledgeNominal;
    	            } else if (product instanceof Equity) {
    	            	totalAvailable =(nominalTotalAsset  + otherPledgeNominal);
    	          }
    				
    			} else if (other_pledges.contains(inventory.getAccount().getName())) {
    				if (product instanceof Bond) {
    	                double principal = row.getProperty("Principal");
    	                totalAvailable =(nominalTotalAsset * principal) + pledgeECBNominal;
    	            } else if (product instanceof Equity) {
    	            	totalAvailable = (nominalTotalAsset  + pledgeECBNominal);
    	            }
    			  }			
    		   }
    			Object retVal = super.getColumnValue(row, CURRENCY, errors);
    			if(retVal instanceof String) {
    				return new Amount(totalAvailable,retVal.toString());
    			}else {
    				return new Amount(totalAvailable);
    			}
    		}	
	

            // Is MarginCall trade eligible in its contract - End
            String movementType = (String) row
                    .getProperty(BOPositionReportTemplate.MOVE);
            if (!Util.isEmpty(movementType)
                    && (movementType.equals("Balance_HC"))) {

                JDate columnDate = extractDate(columnId);
                if (columnDate != null) {

                    Hashtable positions = (Hashtable) row
                            .getProperty(BOPositionReport.POSITIONS);
                    if (positions == null) {
                        return null;
                    }

                    String s = Util.dateToMString(columnDate);
                    Vector datedPositions = (Vector) positions.get(s);
                    if ((datedPositions == null)
                            || (datedPositions.size() == 0)) {
                        return null;
                    }
                    InventorySecurityPosition invSecPos = (InventorySecurityPosition) datedPositions
                            .get(0);
                    double totalSecurity = invSecPos.getTotalSecurity();
                    Product security = invSecPos.getProduct();
                    Double collateralHaircut = getCollateralHaircut(inventory,
                            row, columnId, errors);
                    Double faceValue = null;
                    if ((security != null) && (security instanceof Bond)) {
                        faceValue = ((Bond) security).getFaceValue();
                    }

                    Double bondClosingPricing = getBodnClosingPrice(row, product, quoteSet, pricingEnv, valDate);

                    // Amount balanceHCAmount = null;
                    if ((faceValue != null) && (collateralHaircut != null)
                            && (bondClosingPricing != null)) {
                        Double balanceHC = totalSecurity * faceValue
                                * (collateralHaircut / 100)
                                * bondClosingPricing;
                        return new Amount(balanceHC);
                    }
                }
            }
              // try BOSecurityPositionReportStyle column as default
        Object valueCol = super.getColumnValue(row, columnId, errors);
        if (valueCol != null)
            return valueCol;

            // no value return and try CollateralConfig column
        else if (

                getMarginCallConfigReportStyle().

                        isMarginCallConfigColumn(
                                MARGIN_CALL_CONFIG_PREFIX, columnId)) {
            // check is collateral Config
            return getMarginCallConfigColumn(row, columnId, errors);
        }

        return valueCol;
    }

    private QuoteValue getProductQuote(QuoteSet quoteSet, Product product, JDate startDate, String name) {
        String key = product.getLongId() + startDate.toString() + name;
        if (quoteValues.containsKey(key)) {
            return quoteValues.get(key);
        } else {
            QuoteValue qv = quoteSet.getProductQuote(product, startDate, name);
            quoteValues.put(key, qv);
            return qv;
        }
    }

    @SuppressWarnings("unused")
    private JDate getValDate(ReportRow row) {
        JDatetime valDatetime = (JDatetime) row
                .getProperty(ReportRow.VALUATION_DATETIME);
        if (valDatetime == null) {
            valDatetime = new JDatetime();
        }
        JDate valDate = null;
        PricingEnv env = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);
        if (env != null) {
            valDate = valDatetime.getJDate(env.getTimeZone());
        } else {
            valDate = valDatetime.getJDate(TimeZone.getDefault());
        }
        return valDate;
    }

    protected Double getCollateralHaircut(Inventory inventory, ReportRow row,
                                          String columnId, @SuppressWarnings("rawtypes") Vector errors) {
        Double mccHairCutStr = 100.0;
        try {
            MarginCallConfig marginCallConfig = BOCache
                    .getMarginCallConfig(DSConnection.getDefault(), inventory.getMarginCallConfigId());
            Product product = inventory.getProduct();
            if ((marginCallConfig != null) && (product != null)) {
                String haircutRuleName = marginCallConfig.getHaircutName();
                if (haircutRuleName != null) {
                    double haircutValue = HaircutUtil.getHaircutValue(haircutRuleName, product, (String) null, null, ((JDate) row.getProperty(END_DATE_PROPERTY)), false, true) * 100;
                    mccHairCutStr -= Math.abs(haircutValue);
                }
                return mccHairCutStr;
            }
        } catch (Exception e) {
            Log.error("Error: ", e);
        }
        return mccHairCutStr;
    }

    private int getHigherRemainingmaturity(String remainingMaturityStr) {
        int higherMaturity = 0;
        if (remainingMaturityStr.indexOf("-") != -1) {
            String higherMaturityStr = remainingMaturityStr
                    .substring(remainingMaturityStr.indexOf("-") + 1);
            if (higherMaturityStr.indexOf("Y") != -1) {
                String temp = higherMaturityStr.substring(0,
                        higherMaturityStr.indexOf("Y"));
                higherMaturity = Integer.parseInt(temp);
            }
        }
        return higherMaturity;
    }

    @Override
    public boolean containsPricingEnvDependentColumns(ReportTemplate template) {
        return true;
    }

    protected SantProductCustomDataReportStyle bondCustomReportStyle = null;

    @Override
    public TreeList getTreeList() {
        if (this._treeList != null) {
            return this._treeList;
        }
        final TreeList treeList = super.getTreeList();
        if (this.bondCustomReportStyle == null) {
            this.bondCustomReportStyle = getProductCustomDataReportStyle();
        }
        if (this.bondCustomReportStyle != null) {
            treeList.add(this.bondCustomReportStyle.getNonInheritedTreeList());
        }

        if (collateralConfigReportStyle == null) {
            collateralConfigReportStyle = getMarginCallConfigReportStyle();
        }

        // add CollateralConfig tree
        if (collateralConfigReportStyle != null) {
            addSubTreeList(treeList, new Vector<String>(),
                    MARGIN_CALL_CONFIG_PREFIX,
                    collateralConfigReportStyle.getTreeList());
        }

        // new columns
        treeList.add(PRICE);
        treeList.add(FX_FIXING);
        treeList.add(MARKET_VALUE);
        treeList.add(MARKET_VALUE_EUR);
        treeList.add(CONCILIATION_PRICE_D);
        treeList.add(CONCILIATION_PRICE);
        treeList.add(CONCILIATION_PRICE_2);
        treeList.add(ROWID);
        treeList.add(JISSUER);

        return treeList;
    }

    protected SantProductCustomDataReportStyle getProductCustomDataReportStyle() {
        try {
            if (this.bondCustomReportStyle == null) {
                String className = "calypsox.tk.report.SantProductCustomDataReportStyle";
                this.bondCustomReportStyle = (SantProductCustomDataReportStyle) InstantiateUtil
                        .getInstance(className, true, true);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return this.bondCustomReportStyle;
    }

    /**
     * @return custom CollateralConfigReportStyle
     */
    private CollateralConfigReportStyle getMarginCallConfigReportStyle() {
        try {
            if (this.collateralConfigReportStyle == null) {
                String className = "calypsox.tk.report.CollateralConfigReportStyle";

                this.collateralConfigReportStyle = (calypsox.tk.report.CollateralConfigReportStyle) InstantiateUtil
                        .getInstance(className, true, true);

            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return this.collateralConfigReportStyle;
    }

    /**
     * @param row
     * @param columnName
     * @param errors
     * @return value of Collateral Config if is a MarginCAllConfig Column
     */
    private Object getMarginCallConfigColumn(ReportRow row, String columnName,
                                             @SuppressWarnings("rawtypes") Vector errors) {

        // Somehow super method isMarginCallConfigColumn returns null.
        // Implemented logic here
        String name = getMarginCallConfigReportStyle()
                .getRealColumnName(MARGIN_CALL_CONFIG_PREFIX, columnName);
        return getMarginCallConfigReportStyle().getColumnValue(row, name,
                errors);
    }

    private Double getNominal(final ReportRow row, Inventory inventory) {

        if (row.getProperty(NOMINAL_PROPERTY) != null) {
            return row.getProperty(NOMINAL_PROPERTY);
        }

        if (row.getProperty("NOMINAL_PROPERTY_INFO") == null) {
            Map positions = row.getProperty(BOPositionReport.POSITIONS);
            BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
            if (inventory == null || context == null) {
                com.calypso.tk.core.Log.error(this, "Inventory/context not available for row " + row.toString());
                row.setProperty("NOMINAL_PROPERTY_INFO", "NOMINAL_notFound");
                return null;
            }
            if (inventory instanceof InventorySecurityPosition) {
                Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions.get(context.endDate);
                if (Util.isEmpty(datedPositions)) {
                    row.setProperty("NOMINAL_PROPERTY_INFO", "NOMINAL_notFound");
                    return null;
                }
                row.setProperty(NOMINAL_PROPERTY, InventorySecurityPosition.getTotalSecurity(datedPositions, BOSecurityPositionReport.BALANCE));
                return row.getProperty(NOMINAL_PROPERTY);
            }
            row.setProperty("NOMINAL_PROPERTY_INFO", "NOMINAL_notFound");
            return null;
        }
        return null;
    }

    private Double getCleanPrice(ReportRow row, Product product, QuoteSet quoteSet, PricingEnv pricingEnv, JDate valDate) {

        if (row.getProperty("CleanPrice") != null) {
            return row.getProperty("CleanPrice");
        }

        if (row.getProperty("CleanPriceINFO") == null) {
            QuoteValue productQuote = quoteSet.getProductQuote(product, valDate, pricingEnv.getName());
            if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) {
                Double closePrice = productQuote.getClose();
                closePrice *= 100;
                row.setProperty("CleanPrice", closePrice);
                return closePrice;
            }
            row.setProperty("CleanPriceINFO", "CleanPrice_notFound");
            return null;
        }
        return null;
    }

    private Double getBodnClosingPrice(ReportRow row, Product product, QuoteSet quoteSet, PricingEnv pricingEnv, JDate valDate) {
        if (row.getProperty("Bodn_Closing_Price") != null) {
            return row.getProperty("Bodn_Closing_Price");
        }
        if (row.getProperty("BodnClosingPriceINFO") == null) {
            Double cleanPrice = getCleanPrice(row, product, quoteSet, pricingEnv, valDate);
            if (cleanPrice != null) {
                Double closePrice = (cleanPrice / 100);
                if (product instanceof Bond) {
                    row.setProperty("Bodn_Closing_Price", closePrice);
                    return closePrice;
                }
                row.setProperty("BodnClosingPriceINFO", "BodnClosingPrice_notFound");
                return null;
            }
            row.setProperty("BodnClosingPriceINFO", "BodnClosingPrice_notFound");
            return null;
        }
        return null;
    }
    private double getOtherPledgeNominal(Product product, ReportRow row) {
		boolean existProperty = row.getProperties().containsKey("OTHER_PLEDGES");
		if (existProperty) {
			double nominalOtherPledges = row.getProperty("OTHER_PLEDGES");
			if (product instanceof Bond) {
				double principal = row.getProperty("Principal");
				return Math.abs(nominalOtherPledges * principal);
			} else if (product instanceof Equity) {
				return Math.abs(nominalOtherPledges);
			}
		}
		return 0.00;
	}
    private double getPledgeECB(Inventory inventory, ReportRow row, ArrayList<String> accountsFinal) {
    	Account cuenta = inventory.getAccount();
        for (int acc = 0; acc < accountsFinal.size(); acc++) {
            String ecmsAccountName = accountsFinal.get(acc).split(";")[3];
            String custodioAccountNamen = accountsFinal.get(acc).split(";")[1];
            if (cuenta != null && (cuenta.getName().equalsIgnoreCase(ecmsAccountName) 
            		|| (cuenta.getName().equalsIgnoreCase(custodioAccountNamen)))){
            	if (row.getProperty("ECB_Contrario") != null)
                    return row.getProperty("ECB_Contrario");
            }  
        }
        return 0.00;	    
   
    }
    
    private ArrayList<String> getEcmsAccounts() {
		ArrayList<String> accountsFinal = new ArrayList<>();
		Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(), ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT);
		
		for(String account: pledgeAccounts){
           
            accountsFinal.add(account.split(DV_SEPARATOR)[1]);  
        }

		return accountsFinal;
	}
    
    private ArrayList<String> getEcmsOherAccounts() {
		ArrayList<String> accountsFinal = new ArrayList<>();
		Vector<String> other_pledges = LocalCache.getDomainValues(DSConnection.getDefault(), OTHER_PLEDGE_DOMAIN_VALUE_ACCOUNT);
				
		for(String account: other_pledges){
	           
            accountsFinal.add(account.split(DV_SEPARATOR)[1]);  
        }

		return accountsFinal;
	}
    
}
