package calypsox.util;

public enum MarginCallNotifType {
	MC_NOTICE("MC_NOTICE"),
	MC_RECOUPONING("MC_RECOUPONING"), 
	MC_DELIVERY("MC_DELIVERY"), 
	MC_PARTIAL_DELIVERY("MC_PARTIAL_DELIVERY"), 
	MC_SUBSTITUTION_REQUEST("MC_SUBSTITUTION_REQUEST"), 
	MC_SUBSTITUTION("MC_SUBSTITUTION"), 
	MC_INTEREST("MC_INTEREST"), 
	MC_PORTFOLIO_VALUATION("MC_PORTFOLIO_VALUATION"),
	MC_PORTFOLIO_RECONCILIATION("MC_PORTFOLIO_RECONCILIATION");
	
	private String notifType;
	
	MarginCallNotifType (String notifType) {
		this.notifType = notifType;
	}
	public String getNoticeType() {
		return notifType;
	}
}
