package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;

import java.util.*;

public class RepoNewCreTermMTMTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Create new Cre MTM 0.0 on Termination Date or Action Predated (AMEND/TERMINATE before today)";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        JDate repoEndDate = Optional.ofNullable(trade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getEndDate).orElse(null);
        if(JDate.getNow().equals(repoEndDate)){
            createNewMTMCre(trade,JDate.getNow());
        }else if (null!=trade
                && (trade.getAction().equals(Action.AMEND) || trade.getAction().equals(Action.TERMINATE))
                && isActionPredate(trade,oldTrade)) {
            createNewMTMCre(trade,repoEndDate);
        }


        return true;
    }

    public void createNewMTMCre(Trade trade,JDate effectiveDate){
        JDate today = JDate.getNow();
        CreArray cresToSave = new CreArray();
        BOCre newCre = createNewMTMNet(getOldMTM_NET(trade, today),effectiveDate);
        if(null!=newCre){
            cresToSave.add(newCre);
            try {
                DSConnection.getDefault().getRemoteBO().saveCres(cresToSave); //TODO Comprobar si se genera duplicado
                publishEvents(cresToSave);
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error saving Cre: " + e.getClass());
            }
        }
    }

    private void publishEvents(CreArray cresToSave) {
        if (null != cresToSave && !cresToSave.isEmpty()){
            for (BOCre cre : cresToSave){
                PSEventCre creEvent = new PSEventCre();
                creEvent.setBoCre(cre);
                try {
                    DSConnection.getDefault().getRemoteTrade().saveAndPublish(creEvent);
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Error publish the event: " + e);
                }
            }
        }
    }

    /**
     * @param trade
     * @return
     */
    private BOCre getOldMTM_NET(Trade trade, JDate effectiveDate){
        BOCre oldCre = null;
        try {
            String whereClause = buildWhereClause(trade, BOCre.NEW,effectiveDate);
            CreArray array = DSConnection.getDefault().getRemoteBO().getBOCres(null, whereClause, null);
            if (array != null && !array.isEmpty()) {
                oldCre = Arrays.stream(array.getCres()).max(Comparator.comparing(s -> s.getEffectiveDate().getJDatetime())).orElse(null);
            }
        } catch (CalypsoServiceException e) {
            Log.error("", "Could not retrieve CREs from Trade: " + e.toString());
        }
        return oldCre;
    }


    private String buildWhereClause(Trade trade, String creType, JDate effectiveDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(" bo_cre_type = '");
        sb.append(BOCreConstantes.MTM_NET);
        sb.append("'");
        sb.append(" AND trade_id = ");
        sb.append(trade.getLongId());
        sb.append(" AND cre_type = '");
        sb.append(creType);
        sb.append("'");
        sb.append(" AND cre_status = 'NEW'");
        if(null!=effectiveDate){
            sb.append(" AND trunc(effective_date) <= " + Util.date2SQLString(effectiveDate));
        }
        return sb.toString();
    }

    /**
     * @param cre
     * @param effectiveDate
     * @return
     */
    private BOCre createNewMTMNet(BOCre cre, JDate effectiveDate){
        BOCre creNew = null;
        if(Optional.ofNullable(cre).isPresent()){
            try {
                creNew = (BOCre) cre.clone();
                creNew.setId(0L);
                creNew.setEffectiveDate(effectiveDate);
                creNew.setCreationDate(JDatetime.currentTimeValueOf(effectiveDate, TimeZone.getDefault()));
                creNew.setCreType(BOCre.NEW);
                creNew.setSentDate(null);
                creNew.setStatus(BOCre.NEW);
                creNew.setSentStatus(null);
                creNew.setBookingDate(effectiveDate);
                creNew.setAmount(0,0.0);
                creNew.setVersion(0);
            } catch (CloneNotSupportedException e) {
                Log.error("","Error" + e.getCause());
            }
        }

        return creNew;
    }

    private boolean isActionPredate(Trade newTrade, Trade oldTrade){
        JDate newRepoEndDate = getEndDate(Optional.ofNullable(newTrade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).orElse(null));
        JDate oldRepoEndDate = getEndDate(Optional.ofNullable(oldTrade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).orElse(null));
        return null!=newRepoEndDate && newRepoEndDate.before(oldRepoEndDate) && newRepoEndDate.lte(JDate.getNow());
    }

    private JDate getEndDate(Repo repo){
        JDate endDate = null;
        if(null!=repo){
            if (!repo.getMaturityType().equalsIgnoreCase("OPEN")) {
                endDate = repo.getEndDate();
            } else if (repo.getSecurity() instanceof Bond) {
                Bond bond = (Bond) repo.getSecurity();
                endDate = bond.getEndDate();
            }
        }
        return endDate;
    }
}
