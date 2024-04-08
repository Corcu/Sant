package calypsox.tk.export;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.apps.util.AppUtil;
import com.calypso.engine.configuration.EngineDescription;
import com.calypso.engine.configuration.EngineNotFoundException;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.FlowGenerationException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricerConfig;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.corporateaction.CASwiftEventCode;
import com.calypso.tk.product.corporateaction.sql.CABONamedQueryUtil;
import com.calypso.tk.product.flow.CashFlowCompound;
import com.calypso.tk.product.flow.CashFlowCouponCompound;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.product.flow.CompoundPeriod;
import com.calypso.tk.product.util.CashFlowLayout;
import com.calypso.tk.product.util.ResetSampleDate;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteEngineManagerService;
import com.calypso.tk.upload.jaxb.CalypsoCashFlow;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoProduct;
import com.calypso.tk.upload.jaxb.CalypsoUploadDocument;
import com.calypso.tk.upload.jaxb.CashFlows;
import com.calypso.tk.upload.jaxb.Cashflow;
import com.calypso.tk.upload.jaxb.Column;
import com.calypso.tk.upload.jaxb.ProductCode;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.util.DataUploaderUtil;
import com.calypso.tk.util.swiftparser.MT56xMatcherUtil;

import calypsox.engine.BondDefICExportEngine;

public class BondUploaderXMLDataExporter implements AbstractUploaderXMLDataExporter {
	String CASHFLOWLAYOUT_FIELDS_PREFIX = "S_";
	private static PricingEnv PRICING_ENV = null;
	
	private static String DEFAULT_PRICING_ENV_NAME = "DirtyPrice";

	
	@Override
	public String export(Object sourceObject, UploaderXMLDataExporter exporter) {
		Product product = (Product) sourceObject; 
		CalypsoObject object = exporter.exportObject(product, CalypsoProduct.class, "BondProduct");
        if (object != null) {
            exporter.setCalypsoObject(object);
            ((CalypsoProduct)object).setType("Bond");
            
            CalypsoUploadDocument document = new CalypsoUploadDocument();
            
            GatewayUtil.addCalypsoObject(document, object);
//            addFlows(document, object, (Bond)product);      // /!\ TO REMOVE
            addFlowsAllFields(document, object, product);
            
            return DataUploaderUtil.marshallCalypsoObject(document);
        }
        
        return null;
	}
	
	@Override
	public void linkBOMessage(Object sourceObject, BOMessage boMessage) {
		CalypsoProduct product = (CalypsoProduct) sourceObject;
    	List<ProductCode> productCodes = product.getProductCodes().getProductCode();
    	for (int i = 0; i < productCodes.size(); i++) {
    		ProductCode currentCode = productCodes.get(i);
    		if (currentCode.getProductCodeName().equals("ISIN")) {
    			boMessage.setAttribute("ObjectId", currentCode.getProductCodeValue());
    			break;
    		}
    	}
	}

	@Override
	public void fillInfo(Object sourceObject, UploaderXMLDataExporter exporter, BOMessage boMessage) {
		
	}
	
	
	private HashMap<String, Integer> getCashFlowLayoutAllFields(CashFlowLayout cfl) {
		HashMap<String, Integer> fieldsMap = new HashMap<String, Integer>();
		
		Field[] fields = cfl.getClass().getFields();
		for (Field field : fields) {
			String fieldName = field.getName();
		    if (field.getType().equals(String.class) && fieldName.startsWith(CASHFLOWLAYOUT_FIELDS_PREFIX)) {
		    	
		    	field.setAccessible(true);
		    	String fieldValue = null;
		    	try {
		    		fieldValue = (String)field.get(cfl);
		    	} catch (IllegalArgumentException | IllegalAccessException e) {
		    		Log.error("Flows Extraction - get Class Fields", e.toString());
		    		continue;
		    	} 
		    	
		    	Field idField = null;
		    	int fieldId = 0;
		    	try {
					idField = cfl.getClass().getField(fieldName.replaceFirst(CASHFLOWLAYOUT_FIELDS_PREFIX, ""));
					
					idField.setAccessible(true);
			    	try {
			    		fieldId = (int)idField.get(cfl);
			    	} catch (IllegalArgumentException | IllegalAccessException e) {
			    		Log.error("Flows Extraction - get Class Fields", e.toString());
			    		continue;
			    	} 
				} catch (NoSuchFieldException | SecurityException e) {
					Log.error("Flows Extraction - get Class Fields", e.toString());
					continue;
				}
		    	
		    	fieldsMap.put(fieldValue, fieldId);
		    }
		}
		
		return fieldsMap;
	}
	
	private PricingEnv getEnginePricingEnv() {
		if (PRICING_ENV == null) {
			try {
				RemoteEngineManagerService service = (RemoteEngineManagerService) DSConnection.getDefault().getRemoteService(RemoteEngineManagerService.class);
				EngineDescription enginesDescr = service.getEngineDescription(BondDefICExportEngine.ENGINE_NAME);

				String pricingEnvName = enginesDescr.getProperty("PricingEnv");
					
				if (Util.isEmpty(pricingEnvName)) {
					pricingEnvName = DEFAULT_PRICING_ENV_NAME;
				}

				PRICING_ENV = AppUtil.loadPE(pricingEnvName, new JDatetime(JDate.getNow(), TimeZone.getDefault()));
			}
			catch (CalypsoServiceException | EngineNotFoundException e) {
				Log.error("Flows Extraction - get Engine Description", e.toString());
			} 
		}
		
		return PRICING_ENV;
	}
	
	private boolean isCapitalized(CashFlow cf) {
		boolean isCapitalized = false;
		if (cf instanceof CashFlowCompound) {
			isCapitalized = ((CashFlowCompound)cf).getIsCapitalizedIntB();
		}
		return isCapitalized;
	}
	
	private double getPeriodAmount(CashFlow currentCashFlow, CompoundPeriod compoundPeriod) {
		double amount = 0.0D;
		if (this.isCapitalized(currentCashFlow)) {
			amount = compoundPeriod.getCompoundInterestAmount();
		} else {
			amount = compoundPeriod.getInterestPayment() + compoundPeriod.getCompoundInterestAmount();
		}
		amount = currentCashFlow.roundAmount(amount);
		return amount;
	}
	
	private String getCASwiftEventCode(Product bond, JDate paymentDate) {
    	StaticDataFilter sdFilter = new StaticDataFilter(MT56xMatcherUtil.class.getSimpleName());
    	sdFilter.add(StaticDataFilterElement.builder("Under Product Id").operatorType(SDFilterOperatorType.INT_ENUM).values(Arrays.asList(String.valueOf(bond.getLongId()))).build());
    	sdFilter.add(StaticDataFilterElement.builder("Payment Date").operatorType(SDFilterOperatorType.DATE_RANGE).minValue(paymentDate).minInclusive().maxValue(paymentDate).maxInclusive().build());
    	sdFilter.add(StaticDataFilterElement.builder("Deactivated").operatorType(SDFilterOperatorType.IS).isTrueValue(false).build());

    	List<CA> foundCAs = CABONamedQueryUtil.findCA(sdFilter, (JDatetime)null);
    	
    	String result = "9999";
    	int numFoundCAs = foundCAs.size();
    	if (foundCAs == null || numFoundCAs == 0) {
    		Log.error("CAFinder", "Error: One and only one CA should have been found. Found: " + foundCAs.size());
    	}
    	else if (numFoundCAs >= 1) {
    		for (int i = 0; i < numFoundCAs; i++) {
    			CA currentFoundCA = foundCAs.get(i);
    			
    			CASwiftEventCode sec = currentFoundCA.getSwiftEventCode();
    			if (sec != null) {
    				result += sec.name();
        			if (i < (numFoundCAs - 1)) {
        				result += ",";
        			}
    			}
    		}
    	}

    	return result;
	}
	
	
	private void addFlowsAllFields(CalypsoUploadDocument document, CalypsoObject object, Product bond) {
		CalypsoCashFlow ccf = new CalypsoCashFlow();
        CashFlows cfs = new CashFlows();
        List<Cashflow> cfList = cfs.getCashFlow();
        
        try {
        	HashMap<String, Integer> fieldsMap = null;
        	JDate valDate = JDate.getNow();
        	PricingEnv env = getEnginePricingEnv();

        	if (!bond.getCustomFlowsB()) {
        		CashFlowSet cashFlowSet = bond.generateFlows(valDate);
        		bond.calculate(cashFlowSet, env, valDate);
        	}

        	PricerConfig pricerConfig = env.getPricerConfig();
        	Pricer pricer = pricerConfig.getPricerInstance(bond);

        	CashFlowLayout cfl = CashFlowLayout.createCashFlowLayout(bond);
        	cfl.setPricer(pricer);
        	fieldsMap = getCashFlowLayoutAllFields(cfl);

        	cfl.setPrinFlowOwnDisplay(true);
        	CashFlowSet cfSet = bond.getFlows();
        	cfl.processCashFlows(cfSet, !((Bond)bond).getPrePaidB(), true, valDate, (Bond)bond, pricerConfig, env.getQuoteSet());

        	int numRows = cfl.getNoOfRows();
        	for (int i = 0; i < numRows; i++) {
        		Cashflow cf = new Cashflow();
        		List<Column> cols = cf.getColumn();
        		
        		// Add specifically dates of observation of RFR Indices Flows
        		CashFlow currentCashFlow = cfl.getCoupon(i);
        		
        		if (currentCashFlow != null) {
        			if (currentCashFlow instanceof CashFlowCouponCompound) {
        				CashFlowCouponCompound cfcc = (CashFlowCouponCompound)currentCashFlow;

        				ResetSampleDate[] rsd = cfcc.getResetSamples();
        				if (rsd != null && rsd.length >= 1) {
        					ResetSampleDate samplesDatesStart = rsd[0];
        					ResetSampleDate samplesDatesEnd = rsd[rsd.length - 1];

        					if (samplesDatesStart != null && samplesDatesEnd != null &&
        							samplesDatesStart.getSampleDate() != null && samplesDatesEnd.getSampleDate() != null) {
        						addColumn(cols, "Samples Dates Begin", samplesDatesStart.getSampleDate().toString());
        						addColumn(cols, "Samples Dates End", samplesDatesEnd.getSampleDate().toString());
        					}
        				}
        			}
        			else if (currentCashFlow instanceof CashFlowInterest) {
        				CashFlowInterest cfi = (CashFlowInterest)currentCashFlow;
        				
        				double realPeriod = cfi.getDayCount().yearDiff(cfi.getStartDate(), cfi.getEndDate());
        				addColumn(cols, "Real Period", String.valueOf(realPeriod));
        				
        				
        				Vector<CompoundPeriod> cmpPeriods = cfi.getCashflowPeriodDetails();
        				for (int j = 0; j < cmpPeriods.size(); j++) {
        					double totalPeriodAmt = getPeriodAmount(currentCashFlow, cmpPeriods.get(j));
        					
        					addColumn(cols, "Total Period Amt", String.valueOf(totalPeriodAmt));
        				}
        			}
        			
        			addColumn(cols, "Swift Event Code", getCASwiftEventCode(bond, currentCashFlow.getDate()));
        		}
        		
        		// Add all possible keys/values from calculated fields
        		for (Map.Entry<String, Integer> entry : fieldsMap.entrySet()) {
        			String columnValue = "";
        			try {
        				Object columnObject = cfl.getContentByColumnId(entry.getValue(), i);
            			if (columnObject != null) {
            				if (columnObject instanceof Amount) {
            					columnValue = String.valueOf(((Amount)columnObject).get());
            				}
            				else if (columnObject instanceof JDate) {
            					columnValue = ((JDate)columnObject).toString();
            				}
            				else {
            					columnValue = String.valueOf(columnObject);
            				}
            			}
            			addColumn(cols, entry.getKey(), columnValue);
        			}
        			catch (NullPointerException e) {
        				addColumn(cols, entry.getKey(), columnValue); // Add Column with empty value
        			}
        		}
        		cfList.add(cf);
        	}
        } catch (FlowGenerationException e) {
        	Log.error("Flows Extraction - add Flows All Fields", e.toString());
		}
		
		ccf.setCashFlows(cfs);
        GatewayUtil.addCalypsoObject(document, ccf);
	}
	
	
	


//    private void addFlows(CalypsoUploadDocument document, CalypsoObject object, Bond bond) {
//    	CalypsoCashFlow ccf = new CalypsoCashFlow();
//        CashFlows cfs = new CashFlows();
//        List<Cashflow> cfList = cfs.getCashFlow();
//        
//        try {
//        	PricingEnv env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(
//	        DSConnection.getDefault().getDefaultPricingEnv(),
//	        new JDatetime(JDate.getNow(), TimeZone.getDefault()));
//        	
//			CashFlowSet cashFlowSet = bond.generateFlows(JDate.getNow());
//			bond.calculate(cashFlowSet, env, JDate.getNow());
//			
//			for (int i = 0; i < cashFlowSet.size(); i++) {
//				CashFlow cashFlow = cashFlowSet.get(i);
//				
//				cashFlow.getCashFlowDefinition();
//				
//				Cashflow cf = new Cashflow();
//				List<Column> cols = cf.getColumn();
//				addColumn(cols, "Currency", cashFlow.getCurrency());
//				addColumn(cols, "Type", cashFlow.getType());
//				addColumn(cols, "Pmt Begin", cashFlow.getStartDate().toString());
//				addColumn(cols, "Pmt End", cashFlow.getEndDate().toString());
//				addColumn(cols, "Pmt Dt", cashFlow.getDate().toString());
//				addColumn(cols, "Pmt Amount", String.valueOf(cashFlow.getAmount()));
//				
//				if (cashFlow instanceof CashFlowInterest) {
//					CashFlowInterest cfi = (CashFlowInterest)cashFlow;
//					
//					addColumn(cols, "Day Ct", cfi.getDayCount().toString());
//					addColumn(cols, "Manual Amt", String.valueOf(cfi.getManualSetAmtB()));
//					
//					if (bond.getFixedB()) {
//						addColumn(cols, "Rate", String.valueOf(cfi.getRate()));
//					}
//					else {
//						if (cfi.getSpread() > 0.0D) {
//							addColumn(cols, "Spread", String.valueOf(cfi.getSpread()));
//						}
//						if (cfi.getResetDate() != null) {
//							addColumn(cols, "Reset Date", cfi.getResetDate().toString());
//						}
//					}
//				}
//						
//				cfList.add(cf);
//			}
//		} catch (FlowGenerationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (CalypsoServiceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        
//        ccf.setCashFlows(cfs);
//        GatewayUtil.addCalypsoObject(document, ccf);
//	}

	private void addColumn(List<Column> cols, String name, String value) {
		Column col = new Column();
		col.setName(name);
		col.setValue(value);
		cols.add(col);
	}

	@Override
	public String getIdentifier(Object sourceObject) {
		if (sourceObject instanceof Product) {
			Product product = (Product) sourceObject; 
			return product.getSecCode("ISIN");
		}
		return "";
	}
}
