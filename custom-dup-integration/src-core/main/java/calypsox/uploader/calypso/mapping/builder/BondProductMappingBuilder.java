package calypsox.uploader.calypso.mapping.builder;

import java.util.Hashtable;

import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondInfo;
import com.calypso.tk.upload.jaxb.BasketNameToBasketIdConverter;
import com.calypso.tk.upload.jaxb.BenchmarkToBenchmarkNameConverter;
import com.calypso.tk.upload.jaxb.BenchmarkToBenchmarkSecConverter;
import com.calypso.tk.upload.jaxb.BondProductInfo;
import com.calypso.tk.upload.jaxb.BondToBondProductInfoConverter;
import com.calypso.tk.upload.jaxb.CalypsoProduct;
import com.calypso.tk.upload.jaxb.FlipperToBondConverter;
import com.calypso.tk.upload.jaxb.FutureContractConverter;
import com.calypso.tk.upload.jaxb.ProductCodes;
import com.calypso.uploader.calypso.mapping.converter.DoubleToDoubleRateConverter;
import com.calypso.uploader.calypso.mapping.converter.DoubleToDoubleSpreadConverter;
import com.calypso.uploader.calypso.mapping.converter.HolidayCodeTypeToVectorConverter;
import com.calypso.uploader.calypso.mapping.converter.LegalEntityNameToIdConverter;
import com.calypso.uploader.calypso.mapping.converter.ProductSecCodeValueConverter;
import com.calypso.uploader.calypso.mapping.converter.RateIndexDefToRateIndexConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToDateRollConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToDayCountConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToFXResetConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToFrequencyConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToPeriodRuleConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToTenorConverter;
import com.calypso.uploader.calypso.mapping.converter.XMLGregorianCalendarToJDateConverter;
import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import com.github.dozermapper.core.loader.api.FieldsMappingOption;
import com.github.dozermapper.core.loader.api.FieldsMappingOptions;
import com.github.dozermapper.core.loader.api.TypeMappingBuilder;
import com.github.dozermapper.core.loader.api.TypeMappingOption;
import com.github.dozermapper.core.loader.api.TypeMappingOptions;

public class BondProductMappingBuilder extends BeanMappingBuilder {
	TypeMappingBuilder productToCalypsoProductMappingBuilder = null;
	TypeMappingBuilder bondToBondProductInfoMappingBuilder = null;
	TypeMappingBuilder bondToCalypsoProductMappingBuilder = null;
	TypeMappingBuilder bondInfoToBondProductInfoMappingBuilder = null;
	
	public BondProductMappingBuilder() {
    }

    @Override
    protected void configure() {
    	// Product against CalypsoProduct
    	this.productToCalypsoProductMappingBuilder = this.mapping(Product.class, CalypsoProduct.class, new TypeMappingOption[]{TypeMappingOptions.wildcard(false)});
    	this.productToCalypsoProductMappingBuilder.fields(this.field("_id").setMethod("setId").getMethod("getId"), this.field("productId").setMethod("setProductId").getMethod("getProductId"), new FieldsMappingOption[0]);
    	this.productToCalypsoProductMappingBuilder.fields(this.field("_comment").setMethod("setComment").getMethod("getComment()"), this.field("comment").setMethod("setComment").getMethod("getComment"), new FieldsMappingOption[0]);
    	
    	// Product Codes
    	this.productToCalypsoProductMappingBuilder.fields("this", this.field("productCodes").setMethod("setProductCodes").getMethod("getProductCodes"), new FieldsMappingOption[]{FieldsMappingOptions.useMapId("productCodes")});
    	TypeMappingBuilder productCodesBuilder = this.mapping(ProductCodes.class, Product.class, new TypeMappingOption[]{TypeMappingOptions.mapId("productCodes"), TypeMappingOptions.wildcard(false)});
    	productCodesBuilder.fields(this.field("productCode").getMethod("getProductCode"), this.field("__secCodes").accessible().setMethod("setSecCodes(java.util.Hashtable)").getMethod("getSecCodes()"), new FieldsMappingOption[]{FieldsMappingOptions.hintB(new Class[]{Hashtable.class}), FieldsMappingOptions.customConverter(ProductSecCodeValueConverter.class)});
    	
    	// Bond against CalypsoProduct
    	//bond.setExchangeCodes(ValidationUtil.getExchangeCodeList(calypsoProduct));
    	this.bondToCalypsoProductMappingBuilder = this.mapping(Bond.class, CalypsoProduct.class, new TypeMappingOption[]{TypeMappingOptions.wildcard(false)});
    	this.bondToCalypsoProductMappingBuilder.fields(this.field("_bondType").setMethod("setBondType").getMethod("getBondType"), this.field("subClass").getMethod("getSubClass").setMethod("setSubClass"), new FieldsMappingOption[0]);
    	this.bondToCalypsoProductMappingBuilder.fields(this.field("_currency").setMethod("setCurrency").getMethod("getCurrency()"), this.field("currency").setMethod("setCurrency").getMethod("getCurrency"), new FieldsMappingOption[0]);
    	this.bondToCalypsoProductMappingBuilder.fields(this.field("_country").setMethod("setCountry").getMethod("getCountry()"), this.field("country").setMethod("setCountry").getMethod("getCountry"), new FieldsMappingOption[0]);
    	this.bondToCalypsoProductMappingBuilder.fields(this.field("_name").setMethod("setName").getMethod("getName()"), this.field("name").setMethod("setName").getMethod("getName"), new FieldsMappingOption[0]);
    	this.bondToCalypsoProductMappingBuilder.fields(this.field("_totalIssued").setMethod("setTotalIssued").getMethod("getTotalIssued()"), this.field("totalIssued").setMethod("setTotalIssued").getMethod("getTotalIssued"), new FieldsMappingOption[0]);
    	
    	// Bond against BondProductInfo
    	this.productToCalypsoProductMappingBuilder.fields("this", "productInfo.bondProductInfo", new FieldsMappingOption[]{FieldsMappingOptions.useMapId("Bond")});
    	this.bondToBondProductInfoMappingBuilder = this.mapping(Bond.class, BondProductInfo.class, new TypeMappingOption[]{TypeMappingOptions.mapId("Bond"), TypeMappingOptions.wildcard(false)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_issuePrice").getMethod("getIssuePrice"), this.field("issuePrice").setMethod("setIssuePrice").getMethod("getIssuePrice"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_issueYield").getMethod("getIssueYield"), this.field("issueYield").setMethod("setIssueYield").getMethod("getIssueYield"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_minPurchaseAmt").getMethod("getMinPurchaseAmt"), this.field("minPurchaseAmount").setMethod("setMinPurchaseAmount").getMethod("getMinPurchaseAmount"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_rateIndexSpread").setMethod("setRateIndexSpread").getMethod("getRateIndexSpread"), this.field("couponSpread").getMethod("getCouponSpread"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_coupon").setMethod("setCoupon").getMethod("getCoupon"), this.field("couponRate").getMethod("getCouponRate"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_faceValue").setMethod("setFaceValue").getMethod("getFaceValue()"), this.field("faceValue").setMethod("setFaceValue").getMethod("getFaceValue"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_activeDate").getMethod("getActiveDate").setMethod("setActiveDate"), this.field("activeFrom").getMethod("getActiveFrom").setMethod("setActiveFrom"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_inactiveDate").getMethod("getInactiveDate").setMethod("setInactiveDate"), this.field("activeTo").getMethod("getActiveTo").setMethod("setActiveTo"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_resetDecimals").setMethod("setResetDecimals").getMethod("getResetDecimals()"), this.field("couponResetDec").setMethod("setCouponResetDec").getMethod("getCouponResetDec"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_rollingDay").setMethod("setRollingDay").getMethod("getRollingDay()"), this.field("couponRollDay").setMethod("setCouponRollDay").getMethod("getCouponRollDay"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_settleDays").setMethod("setSettleDays").getMethod("getSettleDays()"), this.field("settleDays").setMethod("setSettleDays").getMethod("getSettleDays"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_valueDays").setMethod("setValueDays").getMethod("getValueDays()"), this.field("accrualDays").setMethod("setAccrualDays").getMethod("getAccrualDays"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_exdividendDays").setMethod("setExdividendDays").getMethod("getExdividendDays()"), this.field("exDividendDays").setMethod("setExDividendDays").getMethod("getExDividendDays"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_accrualRounding").setMethod("setAccrualRounding").getMethod("getAccrualRounding()"), this.field("accrualDigits").setMethod("setAccrualDigits").getMethod("getAccrualDigits"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_priceDecimals").setMethod("setPriceDecimals").getMethod("getPriceDecimals()"), this.field("priceDecimals").setMethod("setPriceDecimals").getMethod("getPriceDecimals"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_yieldDecimals").setMethod("setYieldDecimals").getMethod("getYieldDecimals()"), this.field("yieldDecimals").setMethod("setYieldDecimals").getMethod("getYieldDecimals"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_roundingUnit").setMethod("setRoundingUnit").getMethod("getRoundingUnit()"), this.field("couponRateDecimals").setMethod("setCouponRateDecimals").getMethod("getCouponRateDecimals"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_withholdingTax").setMethod("setWithholdingTax").getMethod("getWithholdingTax()"), this.field("withholdingTax").setMethod("setWithholdingTax").getMethod("getWithholdingTax"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(DoubleToDoubleRateConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_accrualDaycount").setMethod("setAccrualDaycount").getMethod("getAccrualDaycount()"), this.field("couponAccrualDayCount").setMethod("setCouponAccrualDayCount").getMethod("getCouponAccrualDayCount"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_accrualRoundingMethod").setMethod("setAccrualRoundingMethod").getMethod("getAccrualRoundingMethod()"), this.field("accrualRoundingMethod").setMethod("setAccrualRoundingMethod").getMethod("getAccrualRoundingMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_allowedRedemptionType").setMethod("setAllowedRedemptionType").getMethod("getAllowedRedemptionType()"), this.field("redemptionType").setMethod("setRedemptionType").getMethod("getRedemptionType"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_announceDate").setMethod("setAnnounceDate").getMethod("getAnnounceDate()"), this.field("announceDate").setMethod("setAnnounceDate").getMethod("getAnnounceDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_applyWithholdingTaxB").setMethod("setApplyWithholdingTaxB").getMethod("getApplyWithholdingTaxB()"), this.field("applyWithholdingTaxB").setMethod("setApplyWithholdingTaxB").getMethod("isApplyWithholdingTaxB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_auctionDate").setMethod("setAuctionDate").getMethod("getAuctionDate()"), this.field("auctionDate").setMethod("setAuctionDate").getMethod("getAuctionDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_basketId").setMethod("setBasketId").getMethod("getBasketId()"), this.field("basketName").setMethod("setBasketName").getMethod("getBasketName"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(BasketNameToBasketIdConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_benchmarkProdId").setMethod("setBenchmark").getMethod("getBenchmark()"), this.field("benchmarkSec").setMethod("setBenchmarkSec").getMethod("getBenchmarkSec"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(BenchmarkToBenchmarkSecConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_futureContract").setMethod("setFutureContract").getMethod("getFutureContract()"), this.field("futureContract").setMethod("setFutureContract").getMethod("getFutureContract"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(FutureContractConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_bondStatus").setMethod("setBondStatus").getMethod("getBondStatus()"), this.field("status").setMethod("setStatus").getMethod("getStatus"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_capFactorRounding").setMethod("setCapitalizationFactorRounding").getMethod("getCapitalizationFactorRounding()"), this.field("capitilizationDecimals").setMethod("setCapitilizationDecimals").getMethod("getCapitilizationDecimals"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_capFactorRoundingMethod").setMethod("setCapitalizationFactorRoundingMethod").getMethod("getCapitalizationFactorRoundingMethod()"), this.field("capitalizationRoundingMethod").setMethod("setCapitalizationRoundingMethod").getMethod("getCapitalizationRoundingMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_couponCurrency").setMethod("setCouponCurrency").getMethod("getCouponCurrency()"), this.field("couponCurrency").setMethod("setCouponCurrency").getMethod("getCouponCurrency"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_dateRoll").setMethod("setDateRoll").getMethod("getDateRoll()"), this.field("couponDateRoll").setMethod("setCouponDateRoll").getMethod("getCouponDateRoll"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToDateRollConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_daycount").setMethod("setDaycount").getMethod("getDaycount()"), this.field("couponDayCount").setMethod("setCouponDayCount").getMethod("getCouponDayCount"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToDayCountConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_couponFrequency").setMethod("setCouponFrequency").getMethod("getCouponFrequency()"), this.field("couponFrequency").setMethod("setCouponFrequency").getMethod("getCouponFrequency"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToFrequencyConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_couponOffset").setMethod("setCouponOffset").getMethod("getCouponOffset()"), this.field("couponPaymentLag").setMethod("setCouponPaymentLag").getMethod("getCouponPaymentLag"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_couponOffsetBusDayB").setMethod("setCouponOffsetBusDayB").getMethod("getCouponOffsetBusDayB()"), this.field("couponPaymentBusB").setMethod("setCouponPaymentBusB").getMethod("isCouponPaymentBusB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields("this", this.field("flipper").setMethod("setFlipper").getMethod("getFlipper"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(FlipperToBondConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_calculatorAgentId").setMethod("setCalculatorAgentId").getMethod("getCalculatorAgentId()"), this.field("calculatorAgent").setMethod("setCalculatorAgent").getMethod("getCalculatorAgent"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(LegalEntityNameToIdConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_issuePayingAgentId").setMethod("setIssuePayingAgentId").getMethod("getIssuePayingAgentId()"), this.field("issuePayingAgent").setMethod("setIssuePayingAgent").getMethod("getIssuePayingAgent"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(LegalEntityNameToIdConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_trusteeId").setMethod("setTrusteeId").getMethod("getTrusteeId()"), this.field("trustee").setMethod("setTrustee").getMethod("getTrustee"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(LegalEntityNameToIdConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_issuerId").setMethod("setIssuerId").getMethod("getIssuerId()"), this.field("issuer").setMethod("setIssuer").getMethod("getIssuer"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(LegalEntityNameToIdConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_couponPeriodRule").setMethod("setCouponPeriodRule").getMethod("getCouponPeriodRule()"), this.field("couponPaymentRule").setMethod("setCouponPaymentRule").getMethod("getCouponPaymentRule"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(StringToPeriodRuleConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_couponRateRoundingMethod").setMethod("setCouponRateRoundingMethod").getMethod("getCouponRateRoundingMethod()"), this.field("couponRateRoundingMethod").setMethod("setCouponRateRoundingMethod").getMethod("getCouponRateRoundingMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_datedDate").setMethod("setDatedDate").getMethod("getDatedDate()"), this.field("datedDate").setMethod("setDatedDate").getMethod("getDatedDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_defaultDate").setMethod("setDefaultDate").getMethod("getDefaultDate()"), this.field("defaultDate").setMethod("setDefaultDate").getMethod("getDefaultDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_discountMethod").setMethod("setDiscountMethodAsString").getMethod("getDiscountMethodAsString()"), this.field("couponDiscountMethod").setMethod("setCouponDiscountMethod").getMethod("getCouponDiscountMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_effectiveCall").setMethod("setEffectiveCall").getMethod("getEffectiveCall()"), this.field("effectiveCall").setMethod("setEffectiveCall").getMethod("getEffectiveCall"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_effectiveCallDate").setMethod("setEffectiveCallDate").getMethod("getEffectiveCallDate()"), this.field("effectiveCallDate").setMethod("setEffectiveCallDate").getMethod("getEffectiveCallDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_exdividendDayBusB").setMethod("setExdividendDayBusB").getMethod("getExdividendDayBusB()"), this.field("exDividendBusB").setMethod("setExDividendBusB").getMethod("isExDividendBusB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_firstCouponDate").setMethod("setFirstCouponDate").getMethod("getFirstCouponDate()"), this.field("stubStartDate").setMethod("setStubStartDate").getMethod("getStubStartDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_resetDays").setMethod("setResetDays").getMethod("getResetDays()"), this.field("couponResetDays").setMethod("setCouponResetDays").getMethod("getCouponResetDays"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_benchmarkName").setMethod("setBenchmarkName").getMethod("getBenchmarkName()"), this.field("benchmark").setMethod("setBenchmark").getMethod("getBenchmark"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(BenchmarkToBenchmarkNameConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_indexFactor").setMethod("setIndexFactor").getMethod("getIndexFactor()"), this.field("couponRateIndexFactor").setMethod("setCouponRateIndexFactor").getMethod("getCouponRateIndexFactor"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_issueDate").setMethod("setIssueDate").getMethod("getIssueDate()"), this.field("issueDate").setMethod("setIssueDate").getMethod("getIssueDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_isUsingAccrualDayCountForStubs").setMethod("setIsUsingAccrualDayCountForStubs").getMethod("isUsingAccrualDayCountForStubs()"), this.field("couponUseInStubsB").setMethod("setCouponUseInStubsB").getMethod("isCouponUseInStubsB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_maturityDate").setMethod("setMaturityDate").getMethod("getMaturityDate()"), this.field("maturityDate").setMethod("setMaturityDate").getMethod("getMaturityDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_maturityTenor").setMethod("setMaturityTenor").getMethod("getMaturityTenor()"), this.field("tenor").setMethod("setTenor").getMethod("getTenor"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToTenorConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_nominalDecimals").setMethod("setNominalDecimals").getMethod("getNominalDecimals()"), this.field("nominalDecimals").setMethod("setNominalDecimals").getMethod("getNominalDecimals"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_penultimateCouponDate").setMethod("setPenultimateCouponDate").getMethod("getPenultimateCouponDate()"), this.field("stubEndDate").setMethod("setStubEndDate").getMethod("getStubEndDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_prePaidB").setMethod("setPrePaidB").getMethod("getPrePaidB()"), this.field("couponPrepaidB").setMethod("setCouponPrepaidB").getMethod("isCouponPrepaidB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_priceRoundingMethod").setMethod("setPriceRoundingMethod").getMethod("getPriceRoundingMethod()"), this.field("priceRoundingMethod").setMethod("setPriceRoundingMethod").getMethod("getPriceRoundingMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_quoteType").setMethod("setQuoteType").getMethod("getQuoteType()"), this.field("quoteType").setMethod("setQuoteType").getMethod("getQuoteType"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_rateIndexSpread").setMethod("setRateIndexSpread").getMethod("getRateIndexSpread()"), this.field("couponSpread").setMethod("setCouponSpread").getMethod("getCouponSpread"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(DoubleToDoubleSpreadConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_recordDays").setMethod("setRecordDays").getMethod("getRecordDays()"), this.field("recordDays").setMethod("setRecordDays").getMethod("getRecordDays"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_redemCurrency").setMethod("setRedemCurrency").getMethod("getRedemCurrency()"), this.field("redemptionCurrency").setMethod("setRedemptionCurrency").getMethod("getRedemptionCurrency"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_redemptionPrice").setMethod("setRedemptionPrice").getMethod("getRedemptionPrice()"), this.field("redemptionPrice").setMethod("setRedemptionPrice").getMethod("getRedemptionPrice"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(DoubleToDoubleRateConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_resetBusLagB").setMethod("setResetBusLagB").getMethod("getResetBusLagB()"), this.field("couponResetBusLagB").setMethod("setCouponResetBusLagB").getMethod("isCouponResetBusLagB"), new FieldsMappingOption[0]);
     	this.bondToBondProductInfoMappingBuilder.fields(this.field("_resetInArrearB").setMethod("setResetInArrearB").getMethod("getResetInArrearB()"), this.field("couponResetInArrearB").setMethod("setCouponResetInArrearB").getMethod("isCouponResetInArrearB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_tickSize").setMethod("setTickSize").getMethod("getTickSize()"), this.field("tickSize").setMethod("setTickSize").getMethod("getTickSize"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_yieldMethod").setMethod("setYieldMethod").getMethod("getYieldMethod()"), this.field("yieldMethod").setMethod("setYieldMethod").getMethod("getYieldMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_yieldRoundingMethod").setMethod("setYieldRoundingMethod").getMethod("getYieldRoundingMethod()"), this.field("yieldRoundingMethod").setMethod("setYieldRoundingMethod").getMethod("getYieldRoundingMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_holidays").setMethod("setHolidays").getMethod("getHolidays()"), this.field("couponHolidays").setMethod("setCouponHolidays").getMethod("getCouponHolidays"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(HolidayCodeTypeToVectorConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_resetHolidays").setMethod("setResetHolidays").getMethod("getResetHolidays()"), this.field("couponResetHolidays").setMethod("setCouponResetHolidays").getMethod("getCouponResetHolidays"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(HolidayCodeTypeToVectorConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_cutOffHolidays").setMethod("setCutOffHolidays").getMethod("getCutOffHolidays()"), this.field("cutOffHolidays").setMethod("setCutOffHolidays").getMethod("getCutOffHolidays"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(HolidayCodeTypeToVectorConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields("this", "this", new FieldsMappingOption[] { FieldsMappingOptions.customConverter(BondToBondProductInfoConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_samplePeriodShift").setMethod("setSamplePeriodShift").getMethod("isSamplePeriodShift()"), this.field("samplePeriodShiftB").setMethod("setSamplePeriodShiftB").getMethod("isSamplePeriodShiftB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_cmpCutOffLagDays").setMethod("setCompoundCutOffLag").getMethod("getCompoundCutOffLag()"), this.field("couponCompoundCutOffLag").setMethod("setCouponCompoundCutOffLag").getMethod("getCouponCompoundCutOffLag"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_cmpCutOffLagDaysB").setMethod("setCompoundCutOffLagB").getMethod("getCompoundCutOffLagB()"), this.field("couponCompoundCutOffLagB").setMethod("setCouponCompoundCutOffLagB").getMethod("isCouponCompoundCutOffLagB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_extendedType").setMethod("setExtendedType").getMethod("getExtendedType()"), this.field("securityType").setMethod("setSecurityType").getMethod("getSecurityType"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("__fxReset").setMethod("setFxReset").getMethod("getFxReset()"), this.field("couponFXReset").setMethod("setCouponFXReset").getMethod("getCouponFXReset"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(StringToFXResetConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_roundingUnit").setMethod("setRoundingUnit").getMethod("getRoundingUnit()"), this.field("couponRateDecimals").setMethod("setCouponRateDecimals").getMethod("getCouponRateDecimals"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_fxDateRoll").setMethod("setFXDateRoll").getMethod("getFXDateRoll()"), this.field("fxRoll").setMethod("setFXRoll").getMethod("getFXRoll"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(StringToDateRollConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_fxRate").setMethod("setFxRate").getMethod("getFxRate()"), this.field("fixedCouponRate").setMethod("setFixedCouponRate").getMethod("getFixedCouponRate"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("__redemptionfxReset").setMethod("setRedemptionFxReset").getMethod("getRedemptionFxReset()").accessible(true), this.field("redeemFXReset").setMethod("setRedeemFXReset").getMethod("getRedeemFXReset"), new FieldsMappingOption[] { FieldsMappingOptions.customConverter(StringToFXResetConverter.class) });
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_redemptionFxRate").setMethod("setRedemptionFxRate").getMethod("getRedemptionFxRate()"), this.field("fixedRedemptionRate").setMethod("setFixedRedemptionRate").getMethod("getFixedRedemptionRate"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_resetRoundingMethod").setMethod("setResetRoundingMethod").getMethod("getResetRoundingMethod()"), this.field("couponResetDecRoundingMethod").setMethod("setCouponResetDecRoundingMethod").getMethod("getCouponResetDecRoundingMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_assimilationDate").setMethod("setAssimilationDate").getMethod("getAssimilationDate()"), this.field("assimilationDate").setMethod("setAssimilationDate").getMethod("getAssimilationDate"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(XMLGregorianCalendarToJDateConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_compoundFrequency").setMethod("setCompoundFrequency").getMethod("getCompoundFrequency()"), this.field("couponCompoundFrequency").setMethod("setCouponCompoundFrequency").getMethod("getCouponCompoundFrequency"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToFrequencyConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_rateIndex").setMethod("setRateIndex").getMethod("getRateIndex()"), this.field("couponRateIndex").setMethod("setCouponRateIndex").getMethod("getCouponRateIndex"), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(RateIndexDefToRateIndexConverter.class)});
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_compoundMethod").setMethod("setCompoundMethod").getMethod("getCompoundMethod()"), this.field("couponInterestCalculationMethod").setMethod("setCouponInterestCalculationMethod").getMethod("getCouponInterestCalculationMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_isCutoffLagForLastCpnOnly").setMethod("setIsCutoffLagForLastCpnOnly").getMethod("isCutoffLagForLastCpnOnly()"), this.field("cutOffLagForLastCouponOnlyB").setMethod("setCutOffLagForLastCouponOnlyB").getMethod("isCutOffLagForLastCouponOnlyB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_inflationProtectedB").setMethod("setInflationProtectedB").getMethod("getInflationProtectedB()"), this.field("inflationProtectedB").setMethod("setInflationProtectedB").getMethod("isInflationProtectedB"), new FieldsMappingOption[0]);

    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_discountMarginDec").setMethod("setDiscountMarginDec").getMethod("getDiscountMarginDec()"), this.field("discountMarginDecimals").setMethod("setDiscountMarginDecimals").getMethod("getDiscountMarginDecimals"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_discMarginRoundingMethod").setMethod("setDiscMarginRoundingMethod").getMethod("getDiscMarginRoundingMethod()"), this.field("discountMarginRoundingMethod").setMethod("setDiscountMarginRoundingMethod").getMethod("getDiscountMarginRoundingMethod"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_quotingCurrency").setMethod("setQuotingCurrency").getMethod("getQuotingCurrency()"), this.field("couponQuotingCurrency").setMethod("setCouponQuotingCurrency").getMethod("getCouponQuotingCurrency"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_redeemDays").setMethod("setRedeemDays").getMethod("getRedeemDays()"), this.field("redeemDays").setMethod("setRedeemDays").getMethod("getRedeemDays"), new FieldsMappingOption[0]);
//    	this.bondToBondProductInfoMappingBuilder.fields(this.field("").setMethod("setExDivSchedule").getMethod("()"), this.field("").setMethod("").getMethod(""), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_includeExDivDateB").setMethod("setIncludeExDivDateB").getMethod("getIncludeExDivDateB()"), this.field("includeExDivDateB").setMethod("setIncludeExDivDateB").getMethod("isIncludeExDivDateB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_isEffectiveSpreadB").setMethod("setEffectiveSpreadB").getMethod("isEffectiveSpreadB()"), this.field("effectiveSpreadB").setMethod("setEffectiveSpreadB").getMethod("isEffectiveSpreadB"), new FieldsMappingOption[0]);
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("_currentCpnConvention").setMethod("setCurrentCpnConvention").getMethod("getCurrentCouponConvention()"), this.field("currentCouponConvention").setMethod("setCurrentCouponConvention").getMethod("getCurrentCouponConvention"), new FieldsMappingOption[0]);
    	
    	
    	
    	// BondInfo against BondProductInfo
    	this.bondToBondProductInfoMappingBuilder.fields(this.field("__bondInfo").setMethod("setBondInfo").getMethod("getBondInfo()"), "this", new FieldsMappingOption[]{FieldsMappingOptions.useMapId("BondInfo")});
    	this.bondInfoToBondProductInfoMappingBuilder = this.mapping(BondInfo.class, BondProductInfo.class, new TypeMappingOption[]{TypeMappingOptions.mapId("BondInfo"), TypeMappingOptions.wildcard(false)});
    	this.bondInfoToBondProductInfoMappingBuilder.fields(this.field("_commissionPaidB").setMethod("setCommissionPaidB").getMethod("getCommissionPaidB()"), this.field("commissionPaidB").setMethod("setCommissionPaidB").getMethod("isCommissionPaidB"), new FieldsMappingOption[0]);
    	this.bondInfoToBondProductInfoMappingBuilder.fields(this.field("_targetName").setMethod("setTargetName").getMethod("getTargetName()"), this.field("targetName").setMethod("setTargetName").getMethod("getTargetName"), new FieldsMappingOption[0]);
    	

    	// MODEL
    	//this.bondToBondProductInfoMappingBuilder.fields(this.field("").setMethod("").getMethod("()"), this.field("").setMethod("").getMethod(""), new FieldsMappingOption[0]);
    	
    }
}
