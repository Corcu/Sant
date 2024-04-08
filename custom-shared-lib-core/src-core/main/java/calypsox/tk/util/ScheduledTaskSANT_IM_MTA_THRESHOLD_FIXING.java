package calypsox.tk.util;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import calypsox.apps.collateral.MarginCallUtil;
import calypsox.util.collateral.CollateralUtilities;

public class ScheduledTaskSANT_IM_MTA_THRESHOLD_FIXING extends ScheduledTask{
	
	private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
	
	private static final String EUR = "EUR";
	private static final String USD = "USD";
	
	private static final String MTA_EUR = "MTA_EUR";
	private static final String MTA_USD = "MTA_USD";
	private static final String THRESHOLD_EUR = "THRESHOLD_EUR";
	private static final String THRESLHOD_USD = "THRESLHOD_USD";
    private static final String MTA_FIXING_DATE = "MTA_FIXING_DATE";
    private static final String THRESHOLD_FIXING_DATE = "THRESHOLD_FIXING_DATE";
	
	private static final String MTA_THRESHOLD_FIXING = "MTA_THRESHOLD_FIXING";
	private static final String TRUE = "TRUE";

	@Override
	public String getTaskInformation() {
		return "";
	}

	@Override
	public boolean process(DSConnection arg0, PSConnection arg1) {
		
		List<CollateralConfig> contracts = loadContractsFilterByAdditionalField();
		Vector holidays = getHolidays();
		if(Util.isEmpty(holidays)){
			holidays = new Vector();
			holidays.add("SYSTEM");
		}
		JDate date = getValuationDatetime().getJDate(TimeZone.getDefault()).addBusinessDays(-1,holidays); //TODO D || D-1 ?
		Double fxRate = CollateralUtilities.getFXRate(date, EUR, USD);
		List<SaveContract> contractsToSave = new ArrayList<>();
//		List<CollateralConfig> contractsToSave1 = new ArrayList<>();

		if(fxRate!=0.0) {

			for(CollateralConfig contract : contracts) {

				HashMap<String, Double> values = loadAdditionalValues(contract);

				boolean valMTA = validateMTA(values, contract, fxRate);
				boolean valThreshold = validateThreshold(values, contract, fxRate);

				if(valMTA || valThreshold) {
                    contractsToSave.add(new SaveContract(contract,valMTA,valThreshold));
//					contractsToSave.add(contract);
				}
			}

			saveContracts(contractsToSave);

		}else {
			Log.warn(this, "FxRate for: "+date+" FX.EUR.USD = 0.0");
		}

		return true;
	}
	
	
	/**
	 * @return List of Contracts (additionalField : MTA_THRESHOLD_FIXING","TRUE)
	 */
	private List<CollateralConfig> loadContractsFilterByAdditionalField() {
		HashMap<String, String> addtitionalFields = new HashMap<>();
		addtitionalFields.put(MTA_THRESHOLD_FIXING,TRUE);
		//TODO add more filter here IM contracts
		
		return MarginCallUtil.loadContractsFilterByAdditionalField(addtitionalFields);
	}


	/**
	 * 
	 * Validate and calculate if necessary the new value for MTA (Po && Cpty)
	 * 
	 * @param values
	 * @param contract
	 * @param fxRate
	 * @return true if need save contract
	 */
	private boolean validateMTA(HashMap<String, Double> values, CollateralConfig contract, Double fxRate) {
		
		boolean saveContract = false;
		if(values.get(MTA_USD)!=null && values.get(MTA_EUR)!=null) {

			Double mtaValue = (values.get(MTA_USD)/values.get(MTA_EUR));

			Double mtaUSD = values.get(MTA_USD);
			Double mtaEUR = values.get(MTA_EUR);

            //ProccesingOrg Validation
            if(EUR.equalsIgnoreCase(contract.getPoMTACurrency())){
                if(mtaValue<fxRate){
                        Double finalValue = mtaUSD/fxRate;
                        contract.setPoMTAAmount(finalValue);
                        saveContract=true;
                }
            }else if(USD.equalsIgnoreCase(contract.getPoMTACurrency())){
                if(mtaValue>fxRate){
                        Double finalValue = mtaEUR*fxRate;
                        contract.setPoMTAAmount(finalValue);
                        saveContract=true;
                }
            }

            //Counterparty Validation
            if(EUR.equalsIgnoreCase(contract.getLeMTACurrency())){
                if(mtaValue<fxRate){
                        Double finalValue = mtaUSD/fxRate;
                        contract.setLeMTAAmount(finalValue);
                        saveContract=true;
                }
            }else if(USD.equalsIgnoreCase(contract.getLeMTACurrency())){
                if(mtaValue>fxRate){
                        Double finalValue = mtaEUR*fxRate;
                        contract.setLeMTAAmount(finalValue);
                        saveContract=true;
                }
            }

			
//			if(mtaValue<fxRate) {
//				Double finalValue = values.get(MTA_USD)/fxRate;
//				if(EUR.equalsIgnoreCase(contract.getPoMTACurrency())){
//					contract.setPoMTAAmount(finalValue);
//					saveContract=true;
//				}
//				if(EUR.equalsIgnoreCase(contract.getLeMTACurrency())) {
//					contract.setLeMTAAmount(finalValue);
//					saveContract=true;
//				}
//			}
//
//			if(mtaValue>fxRate) {
//				Double finalValue = values.get(MTA_EUR)*fxRate;
//				if(USD.equalsIgnoreCase(contract.getPoMTACurrency())) {
//					contract.setPoMTAAmount(finalValue);
//					saveContract=true;
//				}
//				if(USD.equalsIgnoreCase(contract.getLeMTACurrency())) {
//					contract.setLeMTAAmount(finalValue);
//					saveContract=true;
//				}
//			}
			
		}else {
			Log.warn(this,"Cannot validate MTA for contract: " + contract.getId() + " check additionalFields Values...");
		}
		return saveContract;
	}

    private boolean validateMinThresholdValue(){
        return true;
    }
	
	/**
	 * 
	 * Validate and calculate if necessary the new value for THRESLHOD (Po && Cpty)
	 * 
	 * @param values
	 * @param contract
	 * @param fxRate
	 * @return true if need save contract
	 */
	private boolean validateThreshold(HashMap<String, Double> values, CollateralConfig contract, Double fxRate) { //TODO check values / save contract
		boolean saveContract = false;
		if(values.get(THRESLHOD_USD)!=null && values.get(THRESHOLD_EUR)!=null) {
			
			Double thresholdValue = (values.get(THRESLHOD_USD)/values.get(THRESHOLD_EUR));
            Double thrUSD = values.get(THRESLHOD_USD);
            Double thrEUR = values.get(THRESHOLD_EUR);

            if(EUR.equalsIgnoreCase(contract.getPoNewThresholdCurrency())) {
                if(thresholdValue<fxRate){
                    Double finalValue = values.get(THRESLHOD_USD)/fxRate;
                    contract.setPoNewThresholdAmount(finalValue);
                    saveContract=true;
                }
            }else if(USD.equalsIgnoreCase(contract.getPoNewThresholdCurrency())) {
                if(thresholdValue>fxRate){
                    Double finalValue = values.get(THRESHOLD_EUR)*fxRate;
                    contract.setPoNewThresholdAmount(finalValue);
                    saveContract=true;
                }
            }

            if(EUR.equalsIgnoreCase(contract.getLeNewThresholdCurrency())) {
                if(thresholdValue<fxRate){
                    Double finalValue = values.get(THRESLHOD_USD)/fxRate;
                    contract.setLeNewThresholdAmount(finalValue);
                    saveContract=true;
                }
            }else if(USD.equalsIgnoreCase(contract.getLeNewThresholdCurrency())) {
                if(thresholdValue>fxRate){
                    Double finalValue = values.get(THRESHOLD_EUR)*fxRate;
                    contract.setLeNewThresholdAmount(finalValue);
                    saveContract=true;
                }
            }

//			if(thresholdValue<fxRate) {
//
//				Double finalValue = values.get(THRESLHOD_USD)/fxRate;
//				if(EUR.equalsIgnoreCase(contract.getPoNewThresholdCurrency())) {
//					contract.setPoNewThresholdAmount(finalValue);
//					saveContract=true;
//				}
//				if(EUR.equalsIgnoreCase(contract.getLeNewThresholdCurrency())) {
//					contract.setLeNewThresholdAmount(finalValue);
//					saveContract=true;
//				}
//			}
//
//			if(thresholdValue>fxRate) {
//
//				Double finalValue = values.get(THRESHOLD_EUR)*fxRate;
//				if(USD.equalsIgnoreCase(contract.getPoNewThresholdCurrency())) {
//					contract.setPoNewThresholdAmount(finalValue);
//					saveContract=true;
//				}
//				if(USD.equalsIgnoreCase(contract.getLeNewThresholdCurrency())) {
//					contract.setLeNewThresholdAmount(finalValue);
//					saveContract=true;
//				}
//			}
			
		}else {
			Log.warn(this,"Cannot validate THRESLHOD for contract: " + contract.getId() + " check additionalFields Values...");
		}
		return saveContract;

	}
	
	private void saveContracts(List<SaveContract> contractsToSave) {
 		ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
 		Log.info(this, "Saving contracts...: " + contractsToSave.size());
		try {
			// Multithread execution
			for (final SaveContract save : contractsToSave) {
				exec.submit(new Runnable() {
					@Override
					public void run() {
						try {
                            if(save.getValMTA()){
                                save.getConfig().setAdditionalField(MTA_FIXING_DATE, JDate.getNow().toString());
                            }
                            if(save.getValThreshold()){
                                save.getConfig().setAdditionalField(THRESHOLD_FIXING_DATE, JDate.getNow().toString());
                            }
                            ServiceRegistry.getDefault().getCollateralDataServer().save(save.getConfig());

						} catch (CollateralServiceException e) {
							Log.error(this, "Cannot save contract: " + save.getConfig().getId() + " Error: " + e);
						}
					}
				});
			}
		}finally {
			exec.shutdown();
		}
		
		//Wait for save all contracts
		while(!exec.isTerminated()){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.error(this, "Error waiting executor: " + e);
			}
		}
		
	}
	
	/**
	 * 
	 * Set values for MTA_EUR, MTA_USD, THRESHOLD_EUR and THRESLHOD_USD
	 * 
	 * @param contract
	 * @return 
	 */
	private HashMap<String, Double> loadAdditionalValues(CollateralConfig contract) {
		HashMap<String, Double> values = new HashMap<>();
	
		values.put(MTA_EUR, getDouble(contract.getAdditionalField(MTA_EUR)));
		values.put(MTA_USD, getDouble(contract.getAdditionalField(MTA_USD)));
		values.put(THRESHOLD_EUR, getDouble(contract.getAdditionalField(THRESHOLD_EUR)));
		values.put(THRESLHOD_USD, getDouble(contract.getAdditionalField(THRESLHOD_USD)));
		
		return values;
	}
	
	/**
	 * 
	 * Check Double value
	 * 
	 * @param value
	 * @return
	 * 
	 */
	private Double getDouble(String value) {
		Double result = null;
		if(!Util.isEmpty(value)){
//		    return CollateralUtilities.parseStringAmountToDouble();
			try {
			    if(value.contains(".")){
			        value = value.replace(".","");
                }
                if(value.contains(",")){
                    value = value.replace(",",".");
                }
				result = Util.toDouble(value);
			} catch (NumberFormatException e) {
				Log.error(this, "Cannot cast: " + value + " to double: " + e);
				return result;
			}
		}
		return result;
	}


	private class SaveContract {
        CollateralConfig config;
        Boolean valMTA;
        Boolean valThreshold;


        public SaveContract(CollateralConfig config, Boolean valMTA, Boolean valThreshold) {
            this.config = config;
            this.valMTA = valMTA;
            this.valThreshold = valThreshold;
        }

        public CollateralConfig getConfig() {
            return config;
        }

        public void setConfig(CollateralConfig config) {
            this.config = config;
        }

        public Boolean getValMTA() {
            return valMTA;
        }

        public void setValMTA(Boolean valMTA) {
            this.valMTA = valMTA;
        }

        public Boolean getValThreshold() {
            return valThreshold;
        }

        public void setValThreshold(Boolean valThreshold) {
            this.valThreshold = valThreshold;
        }
    };

}
