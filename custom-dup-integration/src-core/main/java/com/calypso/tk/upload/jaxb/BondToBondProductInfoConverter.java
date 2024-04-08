package com.calypso.tk.upload.jaxb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.BooleanUtils;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.DateRoll;
import com.calypso.tk.core.DateRule;
import com.calypso.tk.core.DateUtil;
import com.calypso.tk.core.DayCount;
import com.calypso.tk.core.Frequency;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.RoundingMethod;
import com.calypso.tk.core.Tenor;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Bond.NotionalGuaranteedEnum;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.product.BondConvertible;
import com.calypso.tk.product.ComparatorPutCallDate;
import com.calypso.tk.product.ConversionReset;
import com.calypso.tk.product.LotteryWinnerRedemption;
import com.calypso.tk.product.PoolFactorEntry;
import com.calypso.tk.product.PutCallDate;
import com.calypso.tk.product.util.ComparatorCouponDate;
import com.calypso.tk.product.util.CouponDate;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.util.BondUtil;
import com.calypso.tk.upload.util.CommonConstants;
import com.calypso.tk.upload.util.UploaderProductUtil;
import com.calypso.tk.upload.util.UploaderTradeUtil;
import com.calypso.tk.upload.util.ValidationUtil;
import com.calypso.tk.util.DataUploaderUtil;
import com.calypso.tk.util.ReflectUtil;
import com.calypso.uploader.calypso.mapping.converter.RateIndexDefToRateIndexConverter;
import com.github.dozermapper.core.DozerConverter;

public class BondToBondProductInfoConverter extends DozerConverter<com.calypso.tk.product.Bond, BondProductInfo> {

	public BondToBondProductInfoConverter() {
        super(com.calypso.tk.product.Bond.class, BondProductInfo.class);
    }

	@Override
	public Bond convertFrom(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null) {
			return bond;
		}
		
		manageAmortization(bondJAXB, bond);
		manageFloater(bondJAXB, bond);
		manageConventioned(bondJAXB, bond);
		manageTradesFlat(bondJAXB, bond);
		manageSpreadAndCouponSchedule(bondJAXB, bond);
		manageOddFirstAndLastCoupon(bondJAXB, bond);
		manageCouponDateRule(bondJAXB, bond);
		manageAssimilationProduct(bondJAXB, bond);
		manageCouponReset(bondJAXB, bond);
		manageCouponType(bondJAXB, bond);
		manageCallSchedule(bondJAXB, bond);
		manageABS(bondJAXB, bond);
		manageConvertible(bondJAXB, bond);
		manageLotteryWinner(bondJAXB, bond);
		manageInflation(bondJAXB, bond);
		managePaymentInKind(bondJAXB, bond);
		manageExDividendSchedule(bondJAXB, bond);
		
		return bond;
	}

	@Override
	public BondProductInfo convertTo(Bond bond, BondProductInfo bondJAXB) {
		if (bond == null) {
			return bondJAXB;
		}
		
		manageAmortization(bond, bondJAXB);
		manageFloater(bond, bondJAXB);
		manageConventioned(bond, bondJAXB);
		manageTradesFlat(bond, bondJAXB);
		manageSpreadAndCouponSchedule(bond, bondJAXB);
		manageCouponDateRule(bond, bondJAXB);
		manageAssimilationProduct(bond, bondJAXB);
		manageCouponReset(bond, bondJAXB);
		manageCouponType(bond, bondJAXB);
		manageCallSchedule(bond, bondJAXB);
		manageABS(bond, bondJAXB);
		manageConvertible(bond, bondJAXB);
		manageLotteryWinner(bond, bondJAXB);
		manageInflation(bond, bondJAXB);
		managePaymentInKind(bond, bondJAXB);
		manageExDividendSchedule(bond, bondJAXB);

		return bondJAXB;
	}

	private void manageAmortization(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bondJAXB.getAmortization() != null && bondJAXB.getAmortization().getAmortizationType() != null) {
			String notionalType = !Util.isEmpty(bondJAXB.getNotionalType()) ? bondJAXB.getNotionalType() : "Notional";
			if (bondJAXB.getAmortization().getAmortizationType().equalsIgnoreCase("Amortizing")) {
				bond.setAmortizingFaceValueB(false);
			} else if (bondJAXB.getAmortization().getAmortizationType().equalsIgnoreCase("Sinking")) {
				bond.setAmortizingFaceValueB(true);
			}
			this.setAmortizationSchedule(bond, bondJAXB, notionalType);
		} else {
			bond.setAmortizingB(false);
			bond.setAmortStructure((String)null);
			bond.setAmortSchedule(new Vector());
			bond.setAmortAmount(0.0D);
			bond.setAmortRate(0.0D);
		}
	}

	private void setAmortizationSchedule(Bond bond, BondProductInfo bondJAXB, String notionalType) {
		bond.setAmortizingB(true);
		bond.setAmortStructure(bondJAXB.getAmortization().getAmortizationSubType());

		if (bond.getAmortStructure().equals("Step down")) {
			bond.setAmortAmount((double)bondJAXB.getAmortization().getAmount());
		} else {
			bond.setAmortAmount(0.0D);
		}
		if (bond.getAmortStructure().equals("Schedule") && !Util.isEmpty(bondJAXB.getAmortization().getAmortizationSchedule())) {
			Vector schedule = bond.getAmortSchedule();
			this.setAmortizingNotionalSchedule(bond, schedule, bondJAXB.getAmortization().getAmortizationSchedule(), notionalType, bondJAXB.getFaceValue());
			bond.setAmortSchedule(schedule);
		}

		bond.setAmortRate(bond.getCoupon());
	}
	
	private void setAmortizingNotionalSchedule(Bond bond, Vector schedule, List<AmortizationSchedule> list, String notionalType, Double faceValueJaxb) {
		if (schedule != null) {
			schedule.removeAllElements();
		} else {
			schedule = new Vector();
		}

		int noOfRows = list.size();

		double faceValue = 1.0D;
		boolean isQuantity = false;
		if ("Quantity".equalsIgnoreCase(notionalType)) {
			isQuantity = true;
			if (!ValidationUtil.isNonZeroNumber(faceValueJaxb)) {
				faceValue = faceValueJaxb;
			}
		}

		for(int i = 0; i < noOfRows; ++i) {
			AmortizationSchedule aSchedule = (AmortizationSchedule)list.get(i);
			JDate date = UploaderTradeUtil.getDate(aSchedule.getAmortizationDate(), "yyyy-MM-dd");
			if (date != null) {
				Double amt = UploaderProductUtil.getAmortizationAmount(aSchedule);
				if (!ValidationUtil.isNotNull(amt)) {
					amt = 0.0D;
				}

				if (isQuantity) {
					amt = amt * faceValue;
				}

				NotionalDate nd = new NotionalDate(date, amt);
				schedule.addElement(nd);
			}
		}

		bond.setNotionalType(notionalType);
	}
	
	private void manageAmortization(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getAmortizingB()) {
			String notionalType = bond.getNotionalType();
			bondJAXB.setNotionalType(notionalType);
			
			Double faceValue = 1.0d;
			boolean isQuantity = false;
			if ("Quantity".equalsIgnoreCase(notionalType)) {
				isQuantity = true;
				Double bondFaceValue = bond.getFaceValue();
				if (ValidationUtil.isNonZeroNumber(bondFaceValue)) {
					faceValue = bondFaceValue;
				}
			}
			
			Amortization amort = bondJAXB.getAmortization();
			if (bondJAXB.getAmortization() == null) {
				amort = new Amortization();
			}

			if (bond.getAmortizingFaceValueB()) {
				amort.setAmortizationType("Sinking");
			}
			else {
				amort.setAmortizationType("Amortizing");
			}
			
			amort.setAmortizationSubType(bond.getAmortStructure());
			amort.setAmount((int)bond.getAmortAmount());
			
			if (bond.getAmortStructure().equals("Schedule") && bond.getAmortSchedule() != null && bond.getAmortSchedule().size() > 0) {
				Vector<NotionalDate> schedule = bond.getAmortSchedule();
				List<AmortizationSchedule> listAmortSched = new ArrayList<AmortizationSchedule>();
				
				for (int i = 0; i < schedule.size(); i++) {
					NotionalDate currentNotionalDate = schedule.get(i);
					AmortizationSchedule amortSched = new AmortizationSchedule();
					amortSched.setAmortizationAmount(currentNotionalDate.getNotionalAmt() / faceValue);
					if (currentNotionalDate.getStartDate() != null) {
						amortSched.setAmortizationDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(currentNotionalDate.getStartDate()));
					}
					listAmortSched.add(amortSched);
				}
				
				amort.amortizationSchedule = listAmortSched;
			}
			
			bondJAXB.setAmortization(amort);
		}
	}
	
	private void manageFloater(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bondJAXB.getFloater() != null && bondJAXB.getFloater().isFloaterB()) {
			   bond.setFloaterB(true);
			   bond.setOptionType(bondJAXB.getFloater().getType());
			   bond.setCapStrike(Util.rateToNumber(bondJAXB.getFloater().getCap()));
			   bond.setFloorStrike(Util.rateToNumber(bondJAXB.getFloater().getFloor()));
			} else {
			   bond.setFloaterB(false);
			   bond.setOptionType("None");
			   bond.setCapStrike(0.0D);
			   bond.setFloorStrike(0.0D);
			}
	}
	
	private void manageFloater(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getFloaterB()) {
			Floater floater = bondJAXB.getFloater();
			if (floater == null) {
				floater = new Floater();
			}
			
			floater.setFloaterB(bond.getFloaterB());
			floater.setCap(bond.getCapStrike());
			floater.setFloor(bond.getFloorStrike());
			floater.setType(bond.getOptionType());
			
			bondJAXB.setFloater(floater);
		}
	}
	
	private void manageConventioned(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bondJAXB.getReconvention() != null) {
			if (bondJAXB.getReconvention().isReconventionB()) {
				bond.setReconventioningDate(UploaderTradeUtil.getDate(bondJAXB.getReconvention().getDate(), "yyyy-MM-dd"));
				bond.setReconventioningDayCount(DayCount.valueOf(bondJAXB.getReconvention().getDayCount()));
			} else {
				bond.setReconventioningDate((JDate)null);
				bond.setReconventioningDayCount(bond.getDaycount());
			}      
		}
	}
	
	private void manageConventioned(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getReconventioningDate() != null) {
			Reconvention reconvention = bondJAXB.getReconvention();
			if (reconvention == null) {
				reconvention = new Reconvention();
			}

			reconvention.setReconventionB(true);
			if (bond.getReconventioningDate() != null) {
				reconvention.setDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(bond.getReconventioningDate()));
			}
			reconvention.setDayCount(bond.getReconventioningDayCount().toString());

			bondJAXB.setReconvention(reconvention);
		}
	}
	
	
	private void manageTradesFlat(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bondJAXB.getTradesFlat() != null) {
			boolean isTradeFlat = bondJAXB.getTradesFlat().isTradeFlatB();
			if (isTradeFlat) {
				bond.setTradingFlatAsOf(UploaderTradeUtil.getDate(bondJAXB.getTradesFlat().getDate(), "yyyy-MM-dd"));
			} else {
				bond.setTradingFlatAsOf((JDate)null);
			}     
		}
	}
	
	private void manageTradesFlat(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getTradingFlatAsOf() != null) {
			TradesFlat tradesFlat = bondJAXB.getTradesFlat();
			if (tradesFlat == null) {
				tradesFlat = new TradesFlat();
			}
			
			tradesFlat.setTradeFlatB(true);
			if (bond.getTradingFlatAsOf() != null) {
				tradesFlat.setDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(bond.getTradingFlatAsOf()));
			}

			bondJAXB.setTradesFlat(tradesFlat);
		}
	}

	private void manageSpreadAndCouponSchedule(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		Vector schedule;

		if (!Util.isEmpty(bondJAXB.getCouponType()) && (bondJAXB.getCouponType().equalsIgnoreCase("Floating Rate") || (bondJAXB.getFlipper() != null && bondJAXB.getFlipper().isFlipperB()))) {
			if (bondJAXB.isVariableFloatingSpreadB() != null && bondJAXB.isVariableFloatingSpreadB()) {
				if (bondJAXB.getCouponSpreadSchedules() != null && !Util.isEmpty(bondJAXB.getCouponSpreadSchedules().getCouponSpreadSchedule())) {
					schedule = bond.getSpreadSchedule();
					this.setFloatingSpreadSchedule(schedule, bondJAXB.getCouponSpreadSchedules().getCouponSpreadSchedule(), bondJAXB.getCouponType());
					bond.setSpreadSchedule(schedule);
				} else {
					bond.setSpreadSchedule(new Vector());
				}
			} else {
				bond.setSpreadSchedule(new Vector());
			}
		}

		if (!Util.isEmpty(bondJAXB.getCouponType()) && bondJAXB.getCouponType().equalsIgnoreCase("VARIABLE")) {
			if (bondJAXB.getCouponSpreadSchedules() != null && !com.calypso.infra.util.Util.isEmpty(bondJAXB.getCouponSpreadSchedules().getCouponSpreadSchedule())) {
				schedule = bond.getCouponSchedule();
				this.setFloatingSpreadSchedule(schedule, bondJAXB.getCouponSpreadSchedules().getCouponSpreadSchedule(), bondJAXB.getCouponType());
				bond.setCouponSchedule(schedule);
			} else {
				bond.setCouponSchedule(new Vector());
			}
		}
	}
	
	private void setFloatingSpreadSchedule(Vector schedule, List<CouponSpreadSchedule> list, String couponType) {
		if (schedule != null) {
			schedule.removeAllElements();
		} else {
			schedule = new Vector();
		}

		int noOfRows = list.size();

		for(int i = 0; i < noOfRows; ++i) {
			CouponSpreadSchedule cpSchedule = (CouponSpreadSchedule)list.get(i);
			JDate date = UploaderTradeUtil.getDate(cpSchedule.getDate(), "yyyy-MM-dd");
			if (date != null) {
				double rate = 0.0D;
				if (couponType.equalsIgnoreCase("Floating Rate")) {
					rate = Util.stringToSpread(Util.numberToString(cpSchedule.getSpread()));
				} else if (couponType.equalsIgnoreCase("VARIABLE")) {
					rate = Util.stringToRate(Util.numberToString(cpSchedule.getCoupon()));
				}
				CouponDate nd = new CouponDate(date, rate);
				schedule.addElement(nd);
			}
		}

		if (couponType.equalsIgnoreCase("VARIABLE")) {
			Collections.sort(schedule, new ComparatorCouponDate());
		}
	}

	private void manageSpreadAndCouponSchedule(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getSpreadSchedule() != null || bond.getCouponSchedule() != null) {
			List<CouponSpreadSchedule> cssList = null;
			CouponSpreadSchedules csss = bondJAXB.getCouponSpreadSchedules();
			if (csss == null) {
				csss = new CouponSpreadSchedules();
			}
			cssList = csss.getCouponSpreadSchedule();
			
			addBondJAXBSchedule(cssList, bond.getSpreadSchedule(), bond);
			addBondJAXBSchedule(cssList, bond.getCouponSchedule(), bond);
			
			bondJAXB.setCouponSpreadSchedules(csss);
		}
	}

	private void addBondJAXBSchedule(List<CouponSpreadSchedule> cssList, Vector<CouponDate> ss, Bond bond) {
		if (ss != null && cssList != null) {
			for (int i = 0; i < ss.size(); i++) {
				CouponDate currentCD = ss.get(i);

				CouponSpreadSchedule css = new CouponSpreadSchedule();
				if (bond.getFixedB()) {
					css.setCoupon(Util.rateToNumber(currentCD.getCouponRate()));
				}
				else {
					css.setSpread(Util.spreadToNumber(currentCD.getCouponRate()));
				}
				if (currentCD.getEndDate() != null) {
					css.setDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(currentCD.getEndDate()));
				}
				cssList.add(css);
			}
		}
	}
	
	private void manageOddFirstAndLastCoupon(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getFirstCouponDate() != null) {
			bond.setOddFirstCouponB(true);
		} else {
			bond.setOddFirstCouponB(false);
		}

		if (bond.getPenultimateCouponDate() != null) {
			bond.setOddLastCouponB(true);
		} else {
			bond.setOddLastCouponB(false);
		}
	}
	
	private void manageCouponDateRule(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		String couponRateRoundingMethod = bondJAXB.getCouponDateRule();
		if (!Util.isEmpty(couponRateRoundingMethod)) {
			bond.setCouponDateRule(UploaderProductUtil.getDateRule(couponRateRoundingMethod));
		} else {
			bond.setCouponDateRule((DateRule)null);
		}
	}
	
	private void manageCouponDateRule(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getCouponDateRule() != null) {
			bondJAXB.setCouponDateRule(bond.getCouponDateRule().getAuthName());
			bondJAXB.setCouponUseDateRuleB(true);
		}
		else {
			bondJAXB.setCouponUseDateRuleB(false);
		}
	}
	
	private void manageAssimilationProduct(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (!Util.isEmpty(bondJAXB.getAssimilationSecCodeName()) && !Util.isEmpty(bondJAXB.getAssimilationSecCodeValue())) {
			com.calypso.tk.core.Product product = ValidationUtil.getProductByCode(bondJAXB.getAssimilationSecCodeName(), bondJAXB.getAssimilationSecCodeValue());
			if (product != null) {
				bond.setAssimilationProductId(product.getId());
			}
		}
	}

	private void manageAssimilationProduct(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getAssimilationProductId() == 0) {
			return;
		}

		com.calypso.tk.core.Product product;
		try {
			product = DSConnection.getDefault().getRemoteProduct().getProduct(bond.getAssimilationProductId());
			if (product != null) {
				bondJAXB.setAssimilationSecCodeName("ISIN");
				bondJAXB.setAssimilationSecCodeValue(product.getSecCode("ISIN"));
			}
		} catch (CalypsoServiceException e) {
		}
	}
	
	private void manageCouponReset(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}

		if (bondJAXB.getCouponReset() != null) {
			boolean isResetAverage = BooleanUtils.isTrue(bondJAXB.getCouponReset().isResetB());
			if (isResetAverage) {
				if (!Util.isEmpty(bondJAXB.getCouponReset().getResetFrequency())) {
					String resetAverageSampleFrequencyString = bondJAXB.getCouponReset().getResetFrequency();
					Frequency resetAverageFrequency = Frequency.get(resetAverageSampleFrequencyString);
					bond.setResetFrequency(resetAverageFrequency);
				} else {
					bond.setResetFrequency((Frequency)null);
				}

				int dayOfWeekInt = 0;
				if ("WK".equals(bondJAXB.getCouponReset().getResetFrequency())) {
					String dayOfWeekString = bondJAXB.getCouponReset().getDayOfWeek();
					try {
						if (!Util.isEmpty(dayOfWeekString)) {
							dayOfWeekInt = DateUtil.getWeekDayCode(dayOfWeekString);
						}
					} catch (Exception var6) {
						Log.error(this, var6);
					}            }

				bond.setResetSamplingDayOfWeek(dayOfWeekInt);

				if (!Util.isEmpty(bondJAXB.getCouponReset().getResetMethod())) {
					bond.setResetSamplingMethod(bondJAXB.getCouponReset().getResetMethod());
				}

				if (ValidationUtil.isNonZeroNumber(bondJAXB.getCouponReset().getCutOffLag())) {
					bond.setResetSamplingCutOffLag(bondJAXB.getCouponReset().getCutOffLag());
				}

				bond.setResetSamplingCutOffLagB(BooleanUtils.isTrue(bondJAXB.getCouponReset().isOffsetBusDayB()));
			} else {
				bond.setResetSamplingMethod((String)null);
				bond.setResetSamplingCutOffLag(0);
				bond.setResetSamplingCutOffLagB(false);
				bond.setResetSamplingDayOfWeek(0);
				bond.setResetFrequency((Frequency)null);
			}      
		}
	}

	private void manageCouponReset(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getResetFrequency() != null) {
			CouponReset couponReset = bondJAXB.getCouponReset();
			if (couponReset == null) {
				couponReset = new CouponReset();
			}
			
			couponReset.setCutOffLag(bond.getResetSamplingCutOffLag());
			couponReset.setDayOfWeek(DateUtil.getWeekDayString(bond.getResetSamplingDayOfWeek()));
			couponReset.setOffsetBusDayB(bond.getResetSamplingCutOffLagB());
			couponReset.setResetB(true);
			couponReset.setResetFrequency(bond.getResetFrequency().toString());
			couponReset.setResetMethod(bond.getResetSamplingMethod());
			
			bondJAXB.setCouponReset(couponReset);
		}
	}
	
	private void manageCouponType(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		String bondCouponType = bondJAXB.getCouponType();

		if (Util.isEmpty(bondCouponType)) {
		   bondCouponType = "FIXED";
		}

		boolean isFloat = bondCouponType.equalsIgnoreCase("Floating Rate");
		boolean isFixed = bondCouponType.equalsIgnoreCase("FIXED");
		boolean isExotic = bondCouponType.equalsIgnoreCase("EXOTIC");
		boolean isVariable = bondCouponType.equalsIgnoreCase("VARIABLE");

		bond.setIsExotic(isExotic);
		bond.setFixedB(isFixed || isVariable);
	}
	
	private void manageCouponType(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		String couponType = "";
		if (bond.getIsExotic()) {
			couponType = CommonConstants.EXOTIC;
		}
		else if (bond.getRateIndex() != null) {
			couponType = CommonConstants.FLOATING_RATE;
		}
		else if (bond.getFixedB()) {
			Vector schedule = bond.getCouponSchedule();
			if (schedule != null && schedule.size() > 0) {
				couponType = CommonConstants.VARIABLE;
			}
			else {
				couponType = CommonConstants.FIXED;
			}
		}
			
		bondJAXB.setCouponType(couponType);
	}
	
	private void manageCallSchedule(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}

		bond.setNotificationDateRoll(DateRoll.R_FOLLOWING);
		if (bondJAXB.getCallSchedules() == null) {
			bond.setSchedule(new Vector());      
		}
		else {
			List<CallSchedule> scheduleJAXB = bondJAXB.getCallSchedules().getCallSchedule();
			Vector schedule = bond.getSchedule();
			if (schedule != null) {
				schedule.removeAllElements();
			} else {
				schedule = new Vector();
			}
			
			if (scheduleJAXB != null) {
				for(int i = 0; i < scheduleJAXB.size(); ++i) {
					PutCallDate pcDate = new PutCallDate();
					CallSchedule callSchedule = (CallSchedule)scheduleJAXB.get(i);
					if (callSchedule.getOptionType().equalsIgnoreCase("CALL")) {
						pcDate.setOptionType(2);
					} else {
						pcDate.setOptionType(1);
					}
					if (callSchedule.getExerciseType().equalsIgnoreCase("Bermudan")) {
						pcDate.setExerciseType("Bermudan");
					} else if (callSchedule.getExerciseType().equalsIgnoreCase("European")) {
						pcDate.setExerciseType("European");               }
					if (callSchedule.getExerciseType().equalsIgnoreCase("American")) {
						pcDate.setExerciseType("American");
					}
					if (callSchedule.getRedemptionDate() != null) {
						pcDate.setDeliveryDate(UploaderTradeUtil.getDate(callSchedule.getRedemptionDate(), "yyyy-MM-dd"));
					}
					if (callSchedule.getNotifDate() != null) {
						pcDate.setExpiryDate(UploaderTradeUtil.getDate(callSchedule.getNotifDate(), "yyyy-MM-dd"));
					}
					if (bond instanceof BondConvertible) {
						if (ValidationUtil.isNotNull(callSchedule.getPrice()) && callSchedule.getPrice() != 0.0D) {
							pcDate.setPrice(callSchedule.getPrice());
							pcDate.setTargetUnit(RoundingMethod.roundNearest(callSchedule.getBondUnit() / callSchedule.getPrice() * bond.getFaceValue(), 2));
						} else if (ValidationUtil.isNotNull(callSchedule.getTargetUnit()) && callSchedule.getTargetUnit() != 0.0D) {
							pcDate.setTargetUnit(callSchedule.getTargetUnit());
							pcDate.setPrice(RoundingMethod.roundNearest(callSchedule.getBondUnit() / callSchedule.getTargetUnit() * bond.getFaceValue(), 2));
						}
					} else {
						pcDate.setPrice(callSchedule.getPrice());
						pcDate.setTargetUnit(callSchedule.getTargetUnit());
					}

					pcDate.setIsExercised(callSchedule.isExercisedB());
					pcDate.setRedemptionAmount(callSchedule.getRedemptionAmount());
					pcDate.setInterestCleanupB(callSchedule.isInterestCleanupB());
					pcDate.setAutoExercise(callSchedule.isMandatoryB());
					pcDate.setBondUnit(callSchedule.getBondUnit());
					pcDate.setFirstExerciseDate(UploaderTradeUtil.getDate(callSchedule.getFirstExerciseDate(), "yyyy-MM-dd"));
					pcDate.setPercentageOfFace(Util.rateToNumber(callSchedule.getPercentage()));
					if (callSchedule.getSettlement().equalsIgnoreCase("Physical")) {
						pcDate.setSettlementType("Physical");
					} else {
						pcDate.setSettlementType("Cash");
					}
					schedule.add(pcDate);
				}
				
				Collections.sort(schedule, new ComparatorPutCallDate());
			}
			
			bond.setSchedule(schedule);
		}
	}

	
	private void manageCallSchedule(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getSchedule() != null) {
			Vector<PutCallDate> s = bond.getSchedule();
			CallSchedules css = bondJAXB.getCallSchedules();
			if (css == null) {
				css = new CallSchedules();
			}
			
			List<CallSchedule> csl = css.getCallSchedule();
			
			for (int i = 0; i < s.size(); i++) {
				PutCallDate pcd = s.get(i);
				CallSchedule cs = new CallSchedule();
				
				if (pcd.getOptionType() == 2) {
					cs.setOptionType("CALL");
				}
				else {
					cs.setOptionType("PUT");
				}
				cs.setExerciseType(pcd.getExerciseType());
				
				if (pcd.getDeliveryDate() != null) {
					cs.setRedemptionDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(pcd.getDeliveryDate()));
				}
				if (pcd.getExpiryDate() != null) {
					cs.setNotifDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(pcd.getExpiryDate()));
				}
				
				if (bond instanceof BondConvertible) {
					if (ValidationUtil.isNotNull(pcd.getPrice()) && pcd.getPrice() != 0.0D) {
						cs.setPrice(pcd.getPrice());
						cs.setTargetUnit(RoundingMethod.roundNearest(pcd.getBondUnit() * pcd.getPrice() / bond.getFaceValue(), 2));
					} else if (ValidationUtil.isNotNull(pcd.getTargetUnit()) && pcd.getTargetUnit() != 0.0D) {
						cs.setTargetUnit(pcd.getTargetUnit());
						cs.setPrice(RoundingMethod.roundNearest(pcd.getBondUnit() * pcd.getTargetUnit() / bond.getFaceValue(), 2));
					}
				} else {
					cs.setPrice(pcd.getPrice());
					cs.setTargetUnit(pcd.getTargetUnit());
				}
				
				cs.setExercisedB(pcd.getIsExercised());
				cs.setRedemptionAmount(pcd.getRedemptionAmount());
				cs.setInterestCleanupB(pcd.isInterestCleanupB());
				cs.setMandatoryB(pcd.getAutoExercise());
				cs.setBondUnit(pcd.getBondUnit());
				if (pcd.getFirstExerciseDate() != null) {
					cs.setFirstExerciseDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(pcd.getFirstExerciseDate()));
				}
				cs.setPercentage(Util.numberToRate(pcd.getPercentageOfFace()));
				cs.setSettlement(pcd.getSettlementType());

				csl.add(cs);
			}
			
			bondJAXB.setCallSchedules(css);
		}
	}
	
	private void manageABS(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond instanceof BondAssetBacked && bondJAXB.getABSDetails() != null) {
			BondAssetBacked assetBackedBond = (BondAssetBacked)bond;
			
			ABSDetails absDetails = bondJAXB.getABSDetails();

			assetBackedBond.setCollateralType(absDetails.getCollateral());
			assetBackedBond.setPoolFactorType(absDetails.getScheduleType());
			assetBackedBond.setEarlyRedemptionDate(UploaderTradeUtil.getDate(absDetails.getEarlyRedemptionDate(), "yyyy-MM-dd"));

			assetBackedBond.setPrincipalPercentage(absDetails.getPrincipalFraction() > 0.0D ? absDetails.getPrincipalFraction() / 100.0D : absDetails.getPrincipalFraction());
			assetBackedBond.setABSSeries(absDetails.getSeries());
			assetBackedBond.setABSClass(absDetails.getClazz());
			Vector groups = new Vector();
			if (absDetails.getGroups() != null && !Util.isEmpty(absDetails.getGroups().getGroupName())) {
				groups.addAll(absDetails.getGroups().getGroupName());
			}
			assetBackedBond.setCollateralGroups(groups);
			assetBackedBond.setPayDownOffset(absDetails.getPaymentLag());
			assetBackedBond.setPayDownOffsetBusDayB(absDetails.isPaymentLagBusB());
			assetBackedBond.setPrepaymentType(Util.isEmpty(absDetails.getPrepaymentType()) ? "" : absDetails.getPrepaymentType());
			assetBackedBond.setPayDownDateRoll(DateRoll.valueOf(absDetails.getDateRoll()));
			assetBackedBond.setDelayDays(absDetails.getFactorDelayDays());
			assetBackedBond.setDelayDaysBusDayB(absDetails.isFactorDelayDaysBusB());
			if (absDetails.getFactorSchedules() != null) {
				bond.setPoolFactorSchedule((new BondUtil()).populatefactorSchedule(assetBackedBond.getPoolFactorSchedule(), absDetails.getFactorSchedules().getFactorSchedule()));
			}
		}
	}
	
	private void manageABS(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond instanceof BondAssetBacked) {
			BondAssetBacked assetBackedBond = (BondAssetBacked)bond;
			ABSDetails absDetails = bondJAXB.getABSDetails();
			if (absDetails == null) {
				absDetails = new ABSDetails();
			}
			
			absDetails.setCollateral(assetBackedBond.getCollateralType());
			absDetails.setScheduleType(assetBackedBond.getPoolFactorType());
			if (assetBackedBond.getEarlyRedemptionDate() != null) {
				absDetails.setEarlyRedemptionDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(assetBackedBond.getEarlyRedemptionDate()));
			}
			absDetails.setPrincipalFraction(assetBackedBond.getPrincipalPercentage() * 100.0D);
			absDetails.setSeries(assetBackedBond.getABSSeries());
			absDetails.setClazz(assetBackedBond.getABSClass());
			
			if (assetBackedBond.getCollateralGroups() != null) {
				Groups groups = absDetails.getGroups();
				if (groups == null) {
					groups = new Groups();
				}
				List<String> groupsList = groups.getGroupName();
				
				groupsList.addAll(assetBackedBond.getCollateralGroups());
				
				absDetails.setGroups(groups);
			}
			
			absDetails.setPaymentLag(assetBackedBond.getPayDownOffset());
			absDetails.setPaymentLagBusB(assetBackedBond.getPayDownOffsetBusDayB());
			absDetails.setPrepaymentType(assetBackedBond.getPrepaymentType());
			absDetails.setDateRoll(assetBackedBond.getPayDownDateRoll().toString());
			absDetails.setFactorDelayDays(assetBackedBond.getDelayDays());
			absDetails.setFactorDelayDaysBusB(assetBackedBond.getDelayDaysBusDayB());
			
			
			if (assetBackedBond.getPoolFactorSchedule() != null) {
				FactorSchedules fss = absDetails.getFactorSchedules();
				if (fss == null) {
					fss = new FactorSchedules();
				}
				
				TreeMap<JDate, PoolFactorEntry> pfs = assetBackedBond.getPoolFactorSchedule();
				List<FactorSchedule> fsl = fss.getFactorSchedule();
				for (Map.Entry<JDate, PoolFactorEntry> entry : pfs.entrySet()) {
					JDate date = entry.getKey();
					PoolFactorEntry pfe = entry.getValue();

					FactorSchedule fs = new FactorSchedule();

					if (pfe.getEffectiveDate() != null) {
						fs.setEffectiveDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(pfe.getEffectiveDate()));
					}
					fs.setFactor(pfe.getPoolFactor());
					fs.setInterestShortfall(pfe.getInterestShortfallAmt());
					fs.setInterestReim(pfe.getInterestShortfallReimAmt());
					fs.setPrincipalShortfall(pfe.getInterestShortfallAmt());
					fs.setPrincipalReim(pfe.getPrincipalShortfallReimAmt());
					fs.setWritedown(pfe.getWritedownAmt());
					fs.setWritedownReim(pfe.getWritedownReimAmt());
					fs.setImpliedWritedown(pfe.getImpliedWritedownAmt());
					fs.setImpliedWritedownReim(pfe.getImpliedWritedownReimAmt());
					fs.setWAC(pfe.getWAC());
					if (pfe.getWAM() != null) {
						fs.setWAM(UploaderTradeUtil.convertToXMLGregorinaCalendar(pfe.getWAM()));
					}
					fs.setCoupon(pfe.getCoupon());
					fs.setPrice(pfe.getPrice());
					fs.setFinancingRate(pfe.getFinancingRate());
					fs.setWALA(pfe.getWALA());
					
					fsl.add(fs);
				}
				
				absDetails.setFactorSchedules(fss);
			}
			
			bondJAXB.setABSDetails(absDetails);
		}
	}
	
	private void manageConvertible(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond instanceof BondConvertible && bondJAXB.getConvertibleDetails() != null) {
			BondConvertible bondConvertible = (BondConvertible)bond;
			ConvertibleDetails convertibleJAXB = bondJAXB.getConvertibleDetails();

			com.calypso.tk.core.Product product = ValidationUtil.getProductByCode(convertibleJAXB.getProductCodeType(), convertibleJAXB.getProductCodeValue());
			if (product != null) {
				bondConvertible.setTargetProduct((com.calypso.tk.product.Equity)product);
			}

			Double strike = convertibleJAXB.getIssueStrike();
			if (ValidationUtil.isNotNull(strike)) {
				bondConvertible.setIssueConversionPrice(strike);
			}

			XMLGregorianCalendar startDate = convertibleJAXB.getStartDate();
			bondConvertible.setConversionStartDate(UploaderTradeUtil.calendarToJDate(startDate));

			XMLGregorianCalendar endDate = convertibleJAXB.getEndDate();
			bondConvertible.setConversionEndDate(UploaderTradeUtil.calendarToJDate(endDate));

			ConversionPriceSchedules resetSchedules = convertibleJAXB.getConversionPriceSchedules();
			if (resetSchedules != null && !Util.isEmpty(resetSchedules.getConversionPriceSchedule())) {
				TreeSet<ConversionReset> schedule = bondConvertible.getConversionPriceSchedule();

				bondConvertible.setConversionPriceSchedule(this.getConversionPriceSchedule(schedule, resetSchedules.getConversionPriceSchedule()));
			} else {
				bondConvertible.setConversionPriceSchedule(new TreeSet());
			}
		}
	}

	private TreeSet getConversionPriceSchedule(TreeSet<ConversionReset> scheduleSet, List<ConversionPriceSchedule> resetScheduleList) {
		if (scheduleSet != null) {
			scheduleSet.clear();
		} else {
			scheduleSet = new TreeSet();
		}
		
		ConversionReset reset;
		for (Iterator iter = resetScheduleList.iterator(); iter.hasNext(); scheduleSet.add(reset)) {
			ConversionPriceSchedule resetSchedule = (ConversionPriceSchedule)iter.next();
			reset = new ConversionReset();

			String type = resetSchedule.getType();
			reset.setDirection(type);

			XMLGregorianCalendar date = resetSchedule.getDate();
			if (date != null) {
				reset.setDate(UploaderTradeUtil.calendarToJDate(date));
			}

			Double cap = resetSchedule.getCap();
			if (ValidationUtil.isNotNull(cap) && cap > 0.0D) {
				reset.setCap(cap);
			}

			Double floor = resetSchedule.getFloor();
			if (ValidationUtil.isNotNull(floor) && floor > 0.0D) {
				reset.setFloor(floor);
			}

			Double multiplier = resetSchedule.getMultiplier();
			if (ValidationUtil.isNotNull(multiplier) && multiplier > 0.0D) {
				reset.setMultiplier(multiplier);
			}

			Boolean isPriceSet = resetSchedule.isPriceSetB();
			if (isPriceSet != null) {
				reset.setIsReset(isPriceSet);
			}

			Double historicalPrice = resetSchedule.getHistoricalPrice();
			if (ValidationUtil.isNotNull(historicalPrice) && historicalPrice > 0.0D) {
				reset.setConversionPrice(historicalPrice);
			}
		}

		return scheduleSet;
	}

	private void manageConvertible(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond instanceof BondConvertible) {
			BondConvertible bondConvertible = (BondConvertible)bond;
			
			ConvertibleDetails convertibleJAXB = bondJAXB.getConvertibleDetails();
			if (convertibleJAXB == null) {
				convertibleJAXB = new ConvertibleDetails();
			}
			
			ConversionPriceSchedules cpss = convertibleJAXB.getConversionPriceSchedules();
			if (cpss == null) {
				cpss = new ConversionPriceSchedules();
			}
			List<ConversionPriceSchedule> cpsl = cpss.getConversionPriceSchedule();
			TreeSet<ConversionReset> schedule = bondConvertible.getConversionPriceSchedule();
			for (ConversionReset conversionReset : schedule) {
				ConversionPriceSchedule cps = new ConversionPriceSchedule();
				
				cps.setCap(conversionReset.getCap());
				cps.setFloor(conversionReset.getFloor());
				cps.setHistoricalPrice(conversionReset.getConversionPrice());
				if (conversionReset.getDate() != null) {
					cps.setDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(conversionReset.getDate()));
				}
				cps.setType(conversionReset.getDirection());
				cps.setPriceSetB(conversionReset.getIsReset());
				cps.setMultiplier(conversionReset.getMultiplier());
				
				cpsl.add(cps);
			}
			convertibleJAXB.setConversionPriceSchedules(cpss);
			
			convertibleJAXB.setIssueStrike(bondConvertible.getIssueConversionPrice());
			com.calypso.tk.core.Product product = bondConvertible.getTargetProduct();
			convertibleJAXB.setProductCodeType("ISIN");
			convertibleJAXB.setProductCodeValue(product.getSecCode("ISIN"));
			if (bondConvertible.getConversionStartDate() != null) {
				convertibleJAXB.setStartDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(bondConvertible.getConversionStartDate()));
			}
			if (bondConvertible.getConversionEndDate() != null) {
				convertibleJAXB.setEndDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(bondConvertible.getConversionEndDate()));
			}
			
			bondJAXB.setConvertibleDetails(convertibleJAXB);
		}
	}
	
	private void manageLotteryWinner(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}

		LotteryWinnerSchedules jaxbLotteryWinnerSchedules = bondJAXB.getLotteryWinnerSchedules();

		if (jaxbLotteryWinnerSchedules != null && !Util.isEmpty(jaxbLotteryWinnerSchedules.getLotteryWinnerSchedule())) {
			TreeSet lotteryWinnerSchedule = bond.getLotteryWinnerSchedule();
			if (lotteryWinnerSchedule != null) {
				lotteryWinnerSchedule.clear();
			} else {
				lotteryWinnerSchedule = new TreeSet();
			}

			Iterator iter = jaxbLotteryWinnerSchedules.getLotteryWinnerSchedule().iterator();
			while(iter.hasNext()) {
				LotteryWinnerSchedule jaxbLotteryWinnerSchedule = (LotteryWinnerSchedule)iter.next();
				if (jaxbLotteryWinnerSchedule != null) {
					LotteryWinnerRedemption lotteryWinnerRedemption = new LotteryWinnerRedemption();

					JDate jdate = jaxbLotteryWinnerSchedule.getDate() != null ? UploaderTradeUtil.getDate(jaxbLotteryWinnerSchedule.getDate()) : JDate.getNow();
					lotteryWinnerRedemption.setDate(jdate);

					Double jaxbNotional = ValidationUtil.isNotNull(jaxbLotteryWinnerSchedule.getNotional()) ? jaxbLotteryWinnerSchedule.getNotional() : 0.0D;
					lotteryWinnerRedemption.setNotional(jaxbNotional);

					Double jaxbPrice = ValidationUtil.isNotNull(jaxbLotteryWinnerSchedule.getPrice()) ? jaxbLotteryWinnerSchedule.getPrice() : 0.0D;
					lotteryWinnerRedemption.setPrice(jaxbPrice);

					lotteryWinnerSchedule.add(lotteryWinnerRedemption);
				}
			}

			bond.setLotteryWinnerSchedule(lotteryWinnerSchedule);
		}
	}
	
	private void manageLotteryWinner(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getLotteryWinnerSchedule() != null) {
			LotteryWinnerSchedules jaxbLotteryWinnerSchedules = bondJAXB.getLotteryWinnerSchedules();

			if (jaxbLotteryWinnerSchedules == null) {
				jaxbLotteryWinnerSchedules = new LotteryWinnerSchedules();
			}
			
			TreeSet<LotteryWinnerRedemption> lotteryWinnerSchedule = bond.getLotteryWinnerSchedule();
			List<LotteryWinnerSchedule> lwsl = jaxbLotteryWinnerSchedules.getLotteryWinnerSchedule();
			for (LotteryWinnerRedemption lwr : lotteryWinnerSchedule) {
				LotteryWinnerSchedule lws = new LotteryWinnerSchedule();
				
				if (lwr.getDate() != null) {
					lws.setDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(lwr.getDate()));
				}
				lws.setNotional(lwr.getNotional());
				lws.setPrice(lwr.getPrice());
				
				lwsl.add(lws);
			}
			
			bondJAXB.setLotteryWinnerSchedules(jaxbLotteryWinnerSchedules);
		}
	}
	
	private void manageInflation(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		InflationDetails inflationDetailsJAXB = bondJAXB.getInflationDetails();
		if (inflationDetailsJAXB != null) {
		   RateIndexDef rateIndexDef = inflationDetailsJAXB.getRateIndex();
		   Tenor tenor = new Tenor(rateIndexDef.getTenor());
		   RateIndex rateIndex = UploaderTradeUtil.getRateIndex(rateIndexDef.getCurrency(), rateIndexDef.getIndex(), tenor, rateIndexDef.getIndexSource(), null);

		   if (rateIndex != null) {
		      bond.setNotionalIndex(rateIndex);
		   }

		   boolean guaranteedNotionalB = inflationDetailsJAXB.isGuaranteedNotionalB();
		   Class[] paramTypes = new Class[]{Boolean.TYPE};
		   Object[] args = new Object[]{guaranteedNotionalB};
		   DataUploaderUtil.invokeMethod(bond, "setNotionalGuaranteedB", paramTypes, args);

		   String floorType = inflationDetailsJAXB.getInflationFloor();
		   
		   if (floorType.equals("None")) {
		      floorType = "GUARANTEED_NONE";
		   } else if (floorType.equals("Guaranteed Coupons and Principal")) {
		      floorType = "GUARANTEED_COUPONS_AND_PRINCIPAL";
		   } else if (floorType.equals("Guaranteed Principal")) {
		      floorType = "GUARANTEED_PRINCIPAL";
		   }

		   Class cls = null;
		   try {
		      cls = Class.forName("com.calypso.tk.product.Bond$NotionalGuaranteedEnum");
		   } catch (ClassNotFoundException var18) {
		      Log.error(200, var18.getMessage());
		   }         Object[] args1;
		   if (cls != null) {
		      Class[] paramTypes2 = new Class[]{cls};
		      Object[] enums = paramTypes2[0].getEnumConstants();            args1 = enums;            int var14 = enums.length;
		      for(int var15 = 0; var15 < var14; ++var15) {               Object enum1 = args1[var15];
		         if (floorType.equalsIgnoreCase(enum1.toString())) {
		            Object[] args2 = new Object[]{enum1};
		            DataUploaderUtil.invokeMethod(bond, "setNotionalGuaranteedType", paramTypes2, args2);
		            break;
		         }
		      }
		   }

		   JDate indexDate = UploaderTradeUtil.getDate(inflationDetailsJAXB.getIndexDate());
		   Class[] paramTypes1 = new Class[]{JDate.class};
		   args1 = new Object[]{indexDate};
		   DataUploaderUtil.invokeMethod(bond, "setNotionalIndexDate", paramTypes1, args1);

		   boolean roundPrice = inflationDetailsJAXB.isRoundGrossPriceBeforeAdjustmentB();
		   Class[] paramTypes3 = new Class[]{Boolean.TYPE};
		   Object[] args3 = new Object[]{roundPrice};
		   DataUploaderUtil.invokeMethod(bond, "setRoundGrossPriceBeforeAdjB", paramTypes3, args3);

		   bond.setNotionalIndexValue(inflationDetailsJAXB.getIndexValue());
		   bond.setInflationRounding(inflationDetailsJAXB.getRounding());
		   bond.setInflationRoundingMethod(inflationDetailsJAXB.getRoundingMethod());

		   if (inflationDetailsJAXB.isOverrideIndexAttributesB()) {
		      bond.setNotionalIndexLag(Tenor.string2Code(inflationDetailsJAXB.getIndexLag()));
		      bond.setNotionalCalcMethod(inflationDetailsJAXB.getCalculationMethod());
		      bond.setNotionalInterpMethod(inflationDetailsJAXB.getInterpolationMethod());
		   } else {
		      bond.setNotionalCalcMethod((String)null);
		   }
		}
	}
	
	private void manageInflation(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.getNotionalGuaranteedType() != null && !(bond.getNotionalGuaranteedType().equals(NotionalGuaranteedEnum.GUARANTEED_NONE))) {
			InflationDetails inflationDetailsJAXB = bondJAXB.getInflationDetails();
			if (inflationDetailsJAXB == null) {
				inflationDetailsJAXB = new InflationDetails();
			}
			
			inflationDetailsJAXB.setCalculationMethod(bond.getNotionalCalcMethod());
			inflationDetailsJAXB.setGuaranteedNotionalB(true);
			if (bond.getNotionalIndexDate() != null) {
				inflationDetailsJAXB.setIndexDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(bond.getNotionalIndexDate()));
			}
			inflationDetailsJAXB.setIndexLag(Tenor.code2String(bond.getNotionalIndexLag()));
			inflationDetailsJAXB.setIndexValue(bond.getNotionalIndexValue());
			String floorType = "None";
			if (bond.getNotionalGuaranteedType().equals(NotionalGuaranteedEnum.GUARANTEED_COUPONS_AND_PRINCIPAL)) {
				floorType = "Guaranteed Coupons and Principal";
			}
			else {
				floorType = "Guaranteed Principal";
			}
			inflationDetailsJAXB.setInflationFloor(floorType);
			inflationDetailsJAXB.setInterpolationMethod(bond.getNotionalInterpMethod());
			if (!Util.isEmpty(bond.getNotionalCalcMethod())) {
				inflationDetailsJAXB.setOverrideIndexAttributesB(true);
			}
			else {
				inflationDetailsJAXB.setOverrideIndexAttributesB(false);	
			}
			RateIndex ri = bond.getNotionalIndex();
			if (ri != null) {
				RateIndexDefToRateIndexConverter ric = new RateIndexDefToRateIndexConverter();
				inflationDetailsJAXB.setRateIndex(ric.convertFrom(ri, null));
			}
			
			inflationDetailsJAXB.setRoundGrossPriceBeforeAdjustmentB(bond.isRoundGrossPriceBeforeAdjB());
			inflationDetailsJAXB.setRounding(bond.getInflationRounding());
			inflationDetailsJAXB.setRoundingMethod(bond.getInflationRoundingMethod());
			
			bondJAXB.setInflationDetails(inflationDetailsJAXB);
		}
	}
	
	private void managePaymentInKind(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}

		if (bondJAXB.getPaymentInKind() != null) {
			boolean isPIKable = bondJAXB.getPaymentInKind().isPaymentInKindB();
			String paymentInKindType = bondJAXB.getPaymentInKind().getPaymentInKindType();
			bond.setIsPIKable(isPIKable);
			if (isPIKable) {
				if (!Util.isEmpty(paymentInKindType)) {
					Vector pikSchedule;
					if (paymentInKindType.equalsIgnoreCase("PIK_SCHEDULE") && bondJAXB.getPaymentInKind().getPaymentInKindSchedules() != null && !Util.isEmpty(bondJAXB.getPaymentInKind().getPaymentInKindSchedules().getPaymentInKindSchedule())) {

						pikSchedule = (Vector)DataUploaderUtil.callMethod(bond, "getPIKSchedule", ReflectUtil.EMPTY_CLASS_ARRAY, ReflectUtil.EMPTY_ARRAY);
						if (pikSchedule != null) {
							pikSchedule.removeAllElements();
						} else {
							pikSchedule = new Vector();
						}

						List<CouponStrikeSchedule> pikScheduleList = bondJAXB.getPaymentInKind().getPaymentInKindSchedules().getPaymentInKindSchedule();
						if (!Util.isEmpty(pikScheduleList)) {
							Iterator iter = pikScheduleList.iterator();
							while (iter.hasNext()) {
								CouponStrikeSchedule schedule = (CouponStrikeSchedule)iter.next();
								JDate date = UploaderTradeUtil.calendarToJDate(schedule.getDate());
								Double rate = schedule.getRate() != null ? DataUploaderUtil.stringToRate(String.valueOf(schedule.getRate())) : 0.0D;
								if (date != null) {
									CouponDate cd = new CouponDate(date, rate);
									pikSchedule.add(cd);
								}
							}

							DataUploaderUtil.invokeMethod(bond, "setPIKSchedule", new Class[]{Vector.class}, new Object[]{pikSchedule});
						}
					} else if (paymentInKindType.equalsIgnoreCase("NO_SCHEDULE")) {
						pikSchedule = (Vector)DataUploaderUtil.callMethod(bond, "getPIKSchedule", ReflectUtil.EMPTY_CLASS_ARRAY, ReflectUtil.EMPTY_ARRAY);
						if (!Util.isEmpty(pikSchedule)) {
							pikSchedule.clear();
						}

						bond.setPIKDate(UploaderTradeUtil.getDate(bondJAXB.getPaymentInKind().getDate(), "yyyy-MM-dd"));
						bond.setPIKRate(Util.rateToNumber(bondJAXB.getPaymentInKind().getRate()));
					}
				} else {
					bond.setPIKDate(UploaderTradeUtil.getDate(bondJAXB.getPaymentInKind().getDate(), "yyyy-MM-dd"));
					bond.setPIKRate(Util.rateToNumber(bondJAXB.getPaymentInKind().getRate()));
				}
			}
		}
	}
	
	private void managePaymentInKind(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		if (bond.isPIKable()) {
			PaymentInKind pik = bondJAXB.getPaymentInKind();
			if (pik == null) {
				pik = new PaymentInKind();
			}
			
			pik.setPaymentInKindB(true);
			
			Vector<CouponDate> pikSchedule = (Vector<CouponDate>)bond.getPIKSchedule();
			if (pikSchedule != null && pikSchedule.size() > 0) {
				pik.setPaymentInKindType("PIK_SCHEDULE");
				
				PaymentInKindSchedules pikss = pik.getPaymentInKindSchedules();
				if (pikss == null) {
					pikss = new PaymentInKindSchedules();
				}
				
				List<CouponStrikeSchedule> cssl = pikss.getPaymentInKindSchedule();
				for (int i = 0; i < pikSchedule.size(); i++) {
					CouponDate cd = pikSchedule.get(i);
					CouponStrikeSchedule css = new CouponStrikeSchedule();
					
					if (cd.getEndDate() != null) {
						css.setDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(cd.getEndDate()));
					}
					css.setRate(cd.getCouponRate());
					
					cssl.add(css);
				}
				pik.setPaymentInKindSchedules(pikss);
			}
			else {
				pik.setPaymentInKindType("NO_SCHEDULE");
				pik.setPaymentInKindSchedules(null);
				
				if (bond.getPIKDate() != null) {
					pik.setDate(UploaderTradeUtil.convertToXMLGregorinaCalendar(bond.getPIKDate()));
				}
				pik.setRate(Util.numberToRate(bond.getPIKRate()));
			}

			bondJAXB.setPaymentInKind(pik);
		}
	}
	
	private void manageExDividendSchedule(BondProductInfo bondJAXB, Bond bond) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		ExDividendSchedules jaxbExdividendSchedules = bondJAXB.getExDividendSchedules();
		if (jaxbExdividendSchedules != null) {
		   Vector<JDate> schedule = this.getDateSchedule(jaxbExdividendSchedules.getExDividendSchedule());
		   if (!Util.isEmpty(schedule)) {
			   bond.setExDivSchedule(schedule);
		   }
		}
	}
	
	private Vector<JDate> getDateSchedule(List<XMLGregorianCalendar> jaxbDateSchedule) {
		Vector<JDate> exDivSchedule = null;
		if (!Util.isEmpty(jaxbDateSchedule)) {
			exDivSchedule = new Vector();         
			Iterator iter = jaxbDateSchedule.iterator();         
			while(iter.hasNext()) {
				XMLGregorianCalendar schedule = (XMLGregorianCalendar)iter.next();
				if (jaxbDateSchedule != null) {
					exDivSchedule.add(UploaderTradeUtil.getDate(schedule));
				}         
			}
		}

		return exDivSchedule;
	}
	
	private void manageExDividendSchedule(Bond bond, BondProductInfo bondJAXB) {
		if (bondJAXB == null || bond == null) {
			return;
		}
		
		Vector<JDate> exDivSchedule = bond.getExDivSchedule();
		if (exDivSchedule != null && exDivSchedule.size() > 0) {
			ExDividendSchedules jaxbExdividendSchedules = bondJAXB.getExDividendSchedules();
			if (jaxbExdividendSchedules == null) {
				jaxbExdividendSchedules = new ExDividendSchedules();
			}

			List<XMLGregorianCalendar> call = jaxbExdividendSchedules.getExDividendSchedule();
			for (int i = 0; i < exDivSchedule.size(); i++) {
				if (exDivSchedule.get(i) != null) {
					call.add(UploaderTradeUtil.convertToXMLGregorinaCalendar(exDivSchedule.get(i)));
				}
			}

			bondJAXB.setExDividendSchedules(jaxbExdividendSchedules);
		}
	}
}
