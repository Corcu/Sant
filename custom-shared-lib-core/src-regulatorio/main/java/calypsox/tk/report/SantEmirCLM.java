package calypsox.tk.report;

import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.Trade;

public class SantEmirCLM {

	public SantEmirCLM(MarginCallDetailEntryDTO marginCallDetailEntyDTO) {
		super();
		this.marginCallDetailEntyDTO = marginCallDetailEntyDTO;
		this.id = marginCallDetailEntyDTO.getId();
	}

	private MarginCallDetailEntryDTO marginCallDetailEntyDTO = null;
	private Trade trade = null;
	
	public Trade getTrade() {
		return trade;
	}

	public void setTrade(Trade trade) {
		this.trade = trade;
	}

	private int id = 0;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public MarginCallDetailEntryDTO getMarginCallDetailEntyDTO() {
		return marginCallDetailEntyDTO;
	}

	public void setMarginCallDetailEntyDTO(
			MarginCallDetailEntryDTO marginCallDetailEntyDTO) {
		this.marginCallDetailEntyDTO = marginCallDetailEntyDTO;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SantEmirCLM)) {
			return false;
		}
		return this.getId() == ((SantEmirCLM) obj).getId();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
