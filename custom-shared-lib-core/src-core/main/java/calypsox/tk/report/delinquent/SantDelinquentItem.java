package calypsox.tk.report.delinquent;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.SantDelinquentMarginCallReportStyle;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.refdata.CollateralConfig;

public class SantDelinquentItem {

	private String contractName;
	private String description;
	private String baseCcy;
	private JDate eventDate;
	private double grossExposure;
	private Double cptyMTM;
	private Double agreedMTM;
	private double marginAmount;
	private double valueDiff;
	private double independent;
	private double threshold;
	private double prevMarginHeld;
	private double prevMarginPending;
	private double additionalAllocaExecuted;
	private double possibleCollateral;
	private JDate delinquentSince;
	private String comment;
	private String status;

	private final Map<String, Object> columnMap = new HashMap<String, Object>();

	public SantDelinquentItem(SantMarginCallEntry santEntry) {
		compute(santEntry);
		buildMap();
	}

	private void compute(SantMarginCallEntry santEntry) {
		CollateralConfig contract = santEntry.getMarginCallConfig();
		MarginCallEntryDTO entryDTO = santEntry.getEntry();

		this.contractName = contract.getName();
		this.description = contract.getLegalEntity().getCode();
		this.baseCcy = contract.getCurrency();
		this.eventDate = entryDTO.getProcessDatetime().getJDate(TimeZone.getDefault());
		Double poMTM = (Double) entryDTO.getAttribute(CollateralStaticAttributes.MC_ENTRY_PO_MTM);
		this.grossExposure = poMTM == null ? entryDTO.getNetBalance() : poMTM;
		this.cptyMTM = (Double) entryDTO.getAttribute(CollateralStaticAttributes.MC_ENTRY_CPTY_MTM);
		this.agreedMTM = (Double) entryDTO.getAttribute(CollateralStaticAttributes.MC_ENTRY_AGREED_MTM);
		this.marginAmount = entryDTO.getGlobalRequiredMargin();
		this.valueDiff = (Double) entryDTO.getAttribute(CollateralStaticAttributes.DELINQUENT_AMOUNT);
		this.independent = entryDTO.getIndependentAmount();
		this.threshold = entryDTO.getThresholdAmount();
		this.prevMarginHeld = entryDTO.getPreviousActualCashMargin() + entryDTO.getPreviousActualSecurityMargin();
		this.prevMarginPending = entryDTO.getPreviousTotalMargin() - this.prevMarginHeld;
		this.additionalAllocaExecuted = entryDTO.getDailyCashMargin() + entryDTO.getDailySecurityMargin();
		this.possibleCollateral = entryDTO.getPreviousTotalMargin() + this.additionalAllocaExecuted;
		this.delinquentSince = CollateralUtilities.getEntryAttributeAsJDate(entryDTO,
				CollateralStaticAttributes.DELINQUENT_SINCE);

		this.comment = entryDTO.getDisputeComment();
		this.status = entryDTO.getStatus();
	}

	public Object getColumnValue(String columnName) {
		return this.columnMap.get(columnName);
	}

	private void buildMap() {
		this.columnMap.put(SantDelinquentMarginCallReportStyle.AGREEMENT_NAME, this.contractName);
		this.columnMap.put(SantDelinquentMarginCallReportStyle.DESCRIPTION, this.description);
		this.columnMap.put(SantDelinquentMarginCallReportStyle.BASE_CCY, this.baseCcy);
		this.columnMap.put(SantDelinquentMarginCallReportStyle.EVENT_DATE, this.eventDate);
		this.columnMap.put(SantDelinquentMarginCallReportStyle.GROSS_EXPOSURE, format(this.grossExposure));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.CPTY_MTM, format(this.cptyMTM));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.AGREED_MTM, format(this.agreedMTM));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.MARGIN_AMOUNT, format(this.marginAmount));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.VALUE_DIFF, format(this.valueDiff));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.INDEPENDENT, format(this.independent));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.THRESHOLD, format(this.threshold));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.PREV_MARG_HELD, format(this.prevMarginHeld));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.PREV_MARG_PENDING, format(this.prevMarginPending));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.ADDITIONAL_ALLOC_EXEC,
				format(this.additionalAllocaExecuted));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.POSSIBLE_COLLAT, format(this.possibleCollateral));
		this.columnMap.put(SantDelinquentMarginCallReportStyle.DELINQUENT_SINCE, this.delinquentSince);
		this.columnMap.put(SantDelinquentMarginCallReportStyle.COMMENT, this.comment);
		this.columnMap.put(SantDelinquentMarginCallReportStyle.CONTRACT_STATUS, this.status);

	}

	private Object format(Double amount) {
		if (amount == null) {
			return null;
		}
		return new Amount(amount, 2);

	}

}
