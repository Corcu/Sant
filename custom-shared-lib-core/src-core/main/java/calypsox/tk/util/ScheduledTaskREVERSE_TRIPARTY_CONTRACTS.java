package calypsox.tk.util;

import calypsox.tk.bo.ReverseTripartyContractsFileReader;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.ScheduledTaskMESSAGE_MATCHING;

import java.util.Arrays;
import java.util.List;

public class ScheduledTaskREVERSE_TRIPARTY_CONTRACTS extends ScheduledTask {
	
	public static final String REVERSE_TRIPARTY_MESSAGE_TYPE = "ReverseTripartyContracts";
	public static final String EXTERNAL_MESSAGE_TYPE = "ExternalMessageType";
	public static final String TRIPARTY_AGENT="Triparty agent";
	
	
	
	protected final List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributes = Arrays.asList(new AttributeDefinition[]{
				attribute(ReverseTripartyContractsFileReader.FILTER_PO_NAME),
				attribute(ReverseTripartyContractsFileReader.FILTER_LE_NAME),
				attribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_TYPES),
				attribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_GROUPS),
				attribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_FILTER),
				attribute(ReverseTripartyContractsFileReader.FILTER_CONTRACTS_IDS),
				attribute(ReverseTripartyContractsFileReader.FILTER_STATUS),
				attribute(ReverseTripartyContractsFileReader.FILTER_PROCESSING_TYPES),
				attribute(ReverseTripartyContractsFileReader.FILTER_DIRECTION),
				attribute(ReverseTripartyContractsFileReader.REPO_TRIPARTY).booleanType()});
		return attributes;
	}

	@Override
	public String getAttributeDescription(String attributeName) {
		return super.getAttributeDescription(attributeName);
	}

	@Override
	public String getTaskInformation() {
		return "Reverse triparty contracts";
	}

	
	@Override
	public boolean process(DSConnection ds, PSConnection ps) {
		
		ScheduledTaskMESSAGE_MATCHING matchingST = new ScheduledTaskMESSAGE_MATCHING();

		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_PO_NAME,getAttribute(ReverseTripartyContractsFileReader.FILTER_PO_NAME));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_LE_NAME,getAttribute(ReverseTripartyContractsFileReader.FILTER_LE_NAME));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_TYPES,getAttribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_TYPES));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_GROUPS,getAttribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_GROUPS));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_FILTER,getAttribute(ReverseTripartyContractsFileReader.FILTER_CONTRACT_FILTER));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_CONTRACTS_IDS,getAttribute(ReverseTripartyContractsFileReader.FILTER_CONTRACTS_IDS));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_STATUS,getAttribute(ReverseTripartyContractsFileReader.FILTER_STATUS));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_PROCESSING_TYPES,getAttribute(ReverseTripartyContractsFileReader.FILTER_PROCESSING_TYPES));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.FILTER_DIRECTION,getAttribute(ReverseTripartyContractsFileReader.FILTER_DIRECTION));
		matchingST.setAttribute(ReverseTripartyContractsFileReader.REPO_TRIPARTY,getAttribute(ReverseTripartyContractsFileReader.REPO_TRIPARTY));

		if(this.getDatetime()!=null)
			matchingST.setAttribute(ReverseTripartyContractsFileReader.DATETIME,this.getValuationDatetime().toString());
		matchingST.setAttribute("Gateway",ReverseTripartyContractsFileReader.OFFSETPOS);
		matchingST.setExecuteB(this.getExecuteB());
		matchingST.setAttribute(EXTERNAL_MESSAGE_TYPE, REVERSE_TRIPARTY_MESSAGE_TYPE);
		matchingST.setDatetime(this.getDatetime());
		matchingST.setCurrentDate(this.getCurrentDate());
		matchingST.setValuationTime(this.getValuationTime());
		return matchingST.process(ds, ps);

	}

}
