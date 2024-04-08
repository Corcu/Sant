package calypsox.repoccp.model.lch.netting;

import com.calypso.tk.core.JDate;

public class LCHObligationSetIdentifier extends LCHIdentifier {

    /*
  <ns2:isin>GB00BNNGP775</ns2:isin>
				<ns2:isinName>UKT 0 7/8 01/31/46</ns2:isinName>
				<ns2:lchMarketCode>GB</ns2:lchMarketCode>
				<ns1:houseClient>H</ns1:houseClient>
				<ns1:intendedSettlementDate>2023-06-09</ns1:intendedSettlementDate>
				<ns1:settlementCurrency>GBP</ns1:settlementCurrency>
				<ns1:membersCsdIcsdTriPartySystem>CRE</ns1:membersCsdIcsdTriPartySystem>
			</ns1:obligationSetIdentifier>
     */




    public JDate getIntendedSettlementDate() {
        return getSettlementDate();
    }

    public void setIntendedSettlementDate(JDate settlementDate) {
        setSettlementDate(settlementDate);
    }

}
