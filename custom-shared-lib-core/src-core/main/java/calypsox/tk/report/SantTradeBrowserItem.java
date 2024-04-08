package calypsox.tk.report;

import java.io.Serializable;
import java.util.HashMap;

import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntry;

import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.refdata.CollateralConfig;

public class SantTradeBrowserItem implements Serializable {

	private static final long serialVersionUID = 660805799524794471L;

	public static final String SANT_TRADE_BROWSER_ITEM = "SantTradeBrowserItem";

	public SantTradeBrowserItem() {
		this.plMrksMap = new HashMap<JDate, PLMark>();
	}

	public SantTradeBrowserItem(SantMarginCallDetailEntry smcde) {
		this();
		this.trade = smcde.getTrade();
		this.marginCall = smcde.getMarginCallConfig();
		this.marginCallEntry = null;
		this.marginCallDetailEntry = smcde.getDetailEntry();
	}

	private Trade trade;
	private CollateralConfig marginCall;
	private MarginCallEntryDTO marginCallEntry;
	private MarginCallDetailEntryDTO marginCallDetailEntry;

	private final HashMap<JDate, PLMark> plMrksMap;

	// private JDate MTMValDate;

	public MarginCallEntryDTO getMarginCallEntry() {
		return this.marginCallEntry;
	}

	public void setMarginCallEntry(final MarginCallEntryDTO marginCallEntry) {
		this.marginCallEntry = marginCallEntry;
	}

	public CollateralConfig getMarginCall() {
		return this.marginCall;
	}

	public void setMarginCall(final CollateralConfig marginCall) {
		this.marginCall = marginCall;
	}

	public Trade getTrade() {
		return this.trade;
	}

	public void setTrade(final Trade trade) {
		this.trade = trade;
	}

	public void addPLMark(final JDate date, final PLMark plMark) {
		if (date != null) {
			this.plMrksMap.put(date, plMark);
		}
	}

	public PLMark getPLMark(final JDate date) {
		if (date != null) {
			return this.plMrksMap.get(date);
		}
		return null;
	}

	public MarginCallDetailEntryDTO getMarginCallDetailEntry() {
		return this.marginCallDetailEntry;
	}

	public void setMarginCallDetailEntry(MarginCallDetailEntryDTO marginCallDetailEntry) {
		this.marginCallDetailEntry = marginCallDetailEntry;
	}

}
