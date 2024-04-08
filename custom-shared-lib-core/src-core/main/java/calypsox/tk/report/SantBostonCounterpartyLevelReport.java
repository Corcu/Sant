package calypsox.tk.report;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.*;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallEntryReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

public class SantBostonCounterpartyLevelReport extends MarginCallEntryReport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String POS = "pos";
	private final String NEG = "neg";

	@Override
	public ReportOutput load(Vector errorMsgs) {

		DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);

		if (output != null) {
			ReportRow[] reportRowArray = output.getRows();//we?ve got an array of rows from the parent?s report
			//final DefaultReportOutput output = new DefaultReportOutput(this);
			ArrayList<ReportRow> rows = new ArrayList<ReportRow>(Arrays.asList(reportRowArray));//We take it from an array.
			ArrayList<ReportRow> reportRow = new ArrayList<ReportRow>();
			ArrayList<MarginCallEntryDTO> entries = new ArrayList<>();

			JDatetime jTime = getValuationDatetime();
			PricingEnv pe = getPricingEnv();
			if(null==pe){
                try {
                    pe = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("OFFICIAL");
                } catch (CalypsoServiceException e) {
                    Log.error(this,"Error: " + e);
                }
            }

			if (!Util.isEmpty(rows)) {
				for (int i = 0; i < rows.size(); i++) {//one loop for every contract, for every row

					MarginCallEntryDTO mcEntryDTO = (MarginCallEntryDTO)rows.get(i)
							.getProperty(ReportRow.DEFAULT);

					mcEntryDTO = fillEntryInfo(mcEntryDTO);

                    MarginCallEntry entry = null;
					if(mcEntryDTO!=null) {

						//get Master Agreement
						try {
							CollateralConfig contract = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(mcEntryDTO.getCollateralConfigId());
							if(null!=contract){
								String masterAgreement = contract.getAdditionalField("MASTER_AGREEMENT");
								if(!Util.isEmpty(masterAgreement)){
									rows.get(i).setProperty(SantBostonCounterpartyLevelReportTemplate.MASTER_AGREEMENT, masterAgreement); //market value for A2.6
								}
							}
						} catch (CollateralServiceException e) {
								Log.error(this,"Cannot get Contract");
						}


						ArrayList <MarginCallDetailEntryDTO> mcDetailEntries = (ArrayList<MarginCallDetailEntryDTO>)mcEntryDTO.getDetailEntries();
						//Double netExp = mcEntry.getNetExposure();//returns NULL

//						Double netExp = mcEntryDTO.getNetExposure();
						//A2.6-->Returning the NPV 
//						if(netExp!=null) {
//							Double netValue = SantBostonUtil.amountCheckToUSD(mcEntryDTO,netExp,jTime,pe);
							 //market value for A2.6
//						}

						if(!Util.isEmpty(mcDetailEntries)) {//if it is not empty or null, call an additional method to sum and control if is pos/neg
							HashMap<String,Double> values = sumMCValues(mcDetailEntries, jTime,pe);
							//NPV_POS-A2.7
							rows.get(i).setProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NPV_POS, values.get(POS));
							//NPV_NEG-A2.8
							rows.get(i).setProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NPV_NEG, values.get(NEG));

							rows.get(i).setProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NPV, values.get(POS) + values.get(NEG));
						}


                        //Nominal (cashPositions + segPositions)
                        PreviousPositionDTO<CashPositionDTO> previousCashPosition = mcEntryDTO.getPreviousCashPosition();
                        PreviousPositionDTO<SecurityPositionDTO> previousSecurityPosition = mcEntryDTO.getPreviousSecurityPosition();

                        //call aux method to sum the value cash + positions
                        HashMap<String,Double> sumPositions = sumCashSeg(previousCashPosition,previousSecurityPosition, mcEntryDTO,jTime,pe);//check if this is what must be sum
                        //Nominal_POS-A2.9
                        rows.get(i).setProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NOMINAL_POS, sumPositions.get(POS));
                        //Nominal_NEG-A2.10
                        rows.get(i).setProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NOMINAL_NEG, sumPositions.get(NEG));

						//Allocation_Value --> The value will be return in USD
						ArrayList<MarginCallAllocationDTO> allocValue = (ArrayList<MarginCallAllocationDTO>)mcEntryDTO.getAllocations();
						if(!Util.isEmpty(allocValue)) {
							//In an additional method I must to sum the allocations
							HashMap <String,Double>sumAllocs = sumAllocations(mcEntryDTO,allocValue,jTime,pe);
							//Allocation_POS-A2.13
							rows.get(i).setProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_ALLOCATION_POS, sumAllocs.get(POS)); //market value for A2.13
							//Allocation_NEG-A2.14
							rows.get(i).setProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_ALLOCATION_NEG, sumAllocs.get(NEG)); //market value for A2.14
						}

					}
				}

			}
			output.setRows(rows.toArray(new ReportRow[rows.size()]));

		}

		return output;

	}

	//	Review this method:
	//	public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgs) {
	//		DefaultReportOutput tmpReportOutput = (DefaultReportOutput) super.load(errorMsgs);
	//		if ((tmpReportOutput == null) || (tmpReportOutput.getRows() == null)) {
	//			return tmpReportOutput;
	//		}
	//		setReportOutput(tmpReportOutput);
	//		return tmpReportOutput;
	//	}


	/*This method sums NPV cheking if the values are negatives or positives*/    
	private HashMap<String,Double> sumMCValues(ArrayList <MarginCallDetailEntryDTO> mcDetailEntries, JDatetime jTime,PricingEnv pe) {//I need one method for every product (NPV,positions,Allocations...)
		HashMap <String,Double> results = new HashMap<String,Double>();
		Double posNum = 0.0;
		Double negNum = 0.0;

		for (MarginCallDetailEntryDTO mcDEntryDTO : mcDetailEntries) {
//			mcDEntryDTO.getCurrency();
			PricerMeasure mcNPV = mcDEntryDTO.getNPV();//returns NULL?//taking the NPV from a detailEntry
			if(mcNPV!=null && mcNPV.getValue()!=0.0) {
				Double npvValue = SantBostonUtil.convertToUSD(mcNPV.getCurrency(),mcNPV.getValue(),jTime,pe);
				if(npvValue<0) {
					negNum+=npvValue;   				
				}
				else {
					posNum+=npvValue;
				}
			}
		}
		results.put(NEG, negNum);
		results.put(POS, posNum);
		return results;

	}
	/*This method sums segPositions and cashPositions cheking if the values are negatives or positives*/
	private HashMap<String,Double> sumCashSeg(PreviousPositionDTO<CashPositionDTO> cashPositions, PreviousPositionDTO<SecurityPositionDTO> segPositions, MarginCallEntryDTO mcEntryDTO,JDatetime jDate,PricingEnv pe) {
		HashMap<String,Double>sumPositions = new HashMap<String,Double>();;
		HashMap<String,Double>sumSegPos=new HashMap<String,Double>();
		HashMap<String,Double>sumCashPos=new HashMap<String,Double>();
		Double cashPos = 0.0;
		Double cashNeg = 0.0;
		Double segPos = 0.0;
		Double segNeg = 0.0;

		if(cashPositions!=null && !Util.isEmpty(cashPositions.getPositions())){
            sumCashPos=sumCashPosNeg(cashPositions.getPositions(), mcEntryDTO, jDate, pe);
            cashPos+=sumCashPos.get(POS);
            cashNeg+=sumCashPos.get(NEG);
        }

        if(segPositions!=null && !Util.isEmpty(segPositions.getPositions())) {
            sumSegPos = sumSegPosNeg(segPositions.getPositions(), mcEntryDTO, jDate, pe);
            segPos += sumSegPos.get(POS);
            segNeg += sumSegPos.get(NEG);
        }

		sumPositions.put(NEG, cashNeg+segNeg);
		sumPositions.put(POS, cashPos+segPos);

		return sumPositions;
	}

	//CASH   
	private HashMap<String,Double> sumCashPosNeg(List<CashPositionDTO>cash,MarginCallEntryDTO mcEntryDTO,JDatetime jTime,PricingEnv pe) {
		HashMap<String,Double>cashValues=new HashMap<String,Double>();
		Double sumPos=0.0;
		Double sumNeg=0.0;
		for (CashPositionDTO cashPos : cash) {
			Double cashValue = SantBostonUtil.amountCheckToUSD(cashPos.getCurrency(),cashPos.getValue(),jTime,pe);//convert to USD
			if(cashValue>=0) {
				sumPos += cashValue;
			}
			else {
				sumNeg+=cashValue;
			}			
		}
		cashValues.put(NEG, sumNeg);
		cashValues.put(POS, sumPos);
		return cashValues; 	
	}

	//SECURITIES	
	private HashMap<String,Double> sumSegPosNeg(List<SecurityPositionDTO>seg, MarginCallEntryDTO mcEntryDTO, JDatetime jTime, PricingEnv pe) {
		HashMap<String,Double>segValues=new HashMap<String,Double>();
		Double sumPos=0.0;
		Double sumNeg=0.0;
		for (SecurityPositionDTO segPos : seg) {
			Double segValue = SantBostonUtil.amountCheckToUSD(segPos.getCurrency(),segPos.getNominal(),jTime,pe);//convert to USD
			if(segValue>=0) {
				sumPos += segValue;
			}
			else {
				sumNeg+=segValue;
			}			
		}
		segValues.put(NEG, sumNeg);
		segValues.put(POS, sumPos);
		return segValues; 	
	}

	/*This method sums all the values in the allocations*/
	private HashMap<String,Double> sumAllocations(MarginCallEntryDTO mcEntryDTO, ArrayList<MarginCallAllocationDTO> allocValue2, JDatetime jTime, PricingEnv pe) {
		HashMap<String,Double>sumAllocs = new HashMap<String,Double>();
		Double sumAllocPos = 0.0;
		Double sumAllocNeg = 0.0;

		for (MarginCallAllocationDTO mcAlloc : allocValue2) {		
			Double allocValue = SantBostonUtil.amountCheckToUSD(mcAlloc.getCurrency(),mcAlloc.getValue(),jTime,pe);//convert to USD
			if(allocValue>=0) {
				sumAllocPos += allocValue;
			}
			else {
				sumAllocNeg += allocValue;
			}
		}
		sumAllocs.put(NEG, sumAllocNeg);
		sumAllocs.put(POS, sumAllocPos);

		return sumAllocs;
	}


	private MarginCallEntryDTO fillEntryInfo(MarginCallEntryDTO entry){
	    MarginCallEntryDTO finalEntry = entry;

	    if(entry!=null){
         try {
                MarginCallEntryDTO resultEntry = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer().loadEntry(entry.getId());
                if(null!=resultEntry){
                   finalEntry = resultEntry;
                }
            } catch (RemoteException e) {
                Log.error(this,"Cannot load entry: " + e);
            }
        }

        return finalEntry;
    }
}


