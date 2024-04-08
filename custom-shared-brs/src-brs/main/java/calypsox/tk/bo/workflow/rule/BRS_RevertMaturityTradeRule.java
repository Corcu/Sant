package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.PostingArray;

public class BRS_RevertMaturityTradeRule implements WfTradeRule {

	
    @Override
    public boolean check(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade oldTrade, Vector paramVector1,
                         DSConnection dsCon, Vector paramVector2, Task paramTask, Object paramObject,
                         Vector paramVector3) {
        return true;
    }

    
    @Override
    public String getDescription() {
        return "Revert Mature Posting related to CST Posting";
    }


    @Override
    public boolean update(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade oldTrade, Vector message,
                          DSConnection dsCon, Vector paramVector2, Task paramTask, Object paramObject,
                          Vector paramVector3) {
    	PostingArray postingListToRevert = new PostingArray();
        try {
        	PostingArray postingList = dsCon.getRemoteBackOffice().getBOPostings(oldTrade.getLongId());
        	for (int i = 0; i < postingList.size(); i++) {
        		BOPosting posting = postingList.get(i);
    		    if("MATURED_TRADE".equalsIgnoreCase(posting.getOriginalEventType())){
    		    	if("NEW".equalsIgnoreCase(posting.getStatus()) && "NEW".equalsIgnoreCase(posting.getPostingType())) {
    		        	BOPosting deletedPosting;
						try {
							deletedPosting = (BOPosting)posting.clone();
							deletedPosting.setStatus("DELETED");
	    		    		postingListToRevert.add(deletedPosting);
						} catch (CloneNotSupportedException e) {
							Log.error(this,"Error while cloning posting. " + e.getCause().getMessage());
						}
						
    		    	}
    		    	else if("SENT".equalsIgnoreCase(posting.getStatus()) && "NEW".equalsIgnoreCase(posting.getPostingType())) {
        		    	BOPosting reversalPosting = generateReversalMaturityPosting(posting);
        		    	postingListToRevert.add(reversalPosting);
    		    	}
    		    }
        	}
        	dsCon.getRemoteBO().savePostings(postingListToRevert);
    	} catch (CalypsoServiceException e) {
    		Log.error(this,"Error getting posting. " + e.getCause().getMessage());
    	}
        return true;
    }

    
    private BOPosting generateReversalMaturityPosting(BOPosting posting){
    	BOPosting reversalPosting = new BOPosting();
    	reversalPosting.setId(0L);
    	reversalPosting.setTradeLongId(posting.getTradeLongId());
    	reversalPosting.setLinkedId(posting.getId());
    	reversalPosting.initProcessFlags();
    	reversalPosting.setStatus("NEW");
    	reversalPosting.setPostingType("REVERSAL");
    	reversalPosting.setOriginalEventType(posting.getOriginalEventType());
    	reversalPosting.setEffectiveDate(posting.getEffectiveDate());
    	reversalPosting.setBookingDate(posting.getBookingDate());
    	reversalPosting.setCreationDate(new JDatetime());
    	reversalPosting.setAmount(posting.getAmount());    	
    	reversalPosting.setCurrency(posting.getCurrency());
    	reversalPosting.setEventType(posting.getEventType());
    	reversalPosting.setDescription(posting.getDescription());
    	reversalPosting.setAttributes(posting.getAttributes());
    	reversalPosting.setDebitAccountId(posting.getDebitAccountId());
    	reversalPosting.setCreditAccountId(posting.getCreditAccountId());	    	
    	return reversalPosting;
    }


}