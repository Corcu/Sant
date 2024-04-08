package calypsox.engine.accounting;

import calypsox.camel.CamelConnectionManagement;
import calypsox.camel.routes.CreRouteBuilder;
import calypsox.tk.bo.cremapping.BOCreMappingFactory;
import calypsox.tk.bo.cremapping.event.SantBOCre;
import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.engine.util.SantEngineUtil;
import calypsox.tk.event.PSEventPostingLiquidation;
import com.calypso.engine.accounting.CreSenderEngine;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.PostingArray;

import javax.management.Attribute;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * @author acd
 */
public class CreOnlineSenderEngine extends CreSenderEngine {

    protected static final String DELIMITER = ",";
    protected static final String ADDITIONAL_CRE_ATT = "AdditionalCreAttributes";
    protected static final String TESTING_MODE = "TESTING_MODE";
    protected static final String SEND_ROUTE = "direct:creMessages";
    protected static final String FILTER = ".CreEventTypesToBlock";

    Properties properties = new Properties();
    CamelConnectionManagement camelConnectionManagement = new CamelConnectionManagement();
    protected BOCreMappingFactory creMappingFactory = BOCreMappingFactory.getFactory();
    protected BOCreUtils boCreUtils = BOCreUtils.getInstance();
    String testingMode = "";


    public CreOnlineSenderEngine(DSConnection dsCon, String hostName, int esPort) {
        super(dsCon, hostName, esPort);
    }

    @Override
    protected void init(EngineContext engineContext) {
        super.init(engineContext);
        testingMode = engineContext.getInitParameter(TESTING_MODE, (String)null);

        properties = SantEngineUtil.getInstance().readProperties(getEngineContext());
        initCamelContext(properties);

        //Init Camel connection
        if(camelConnectionManagement.getContext()!=null){
            Log.system(CreOnlineSenderEngine.class.getName(),"Init camel connection for CreOnlineSenderEngine: ");
        }else{
            Log.error(this,"Error initializing CamelContext.");
        }

        writeCamelContext();
    }

    protected void initCamelContext(Properties properties){
        camelConnectionManagement.initCamelContext(properties,new CreRouteBuilder()).start();
    }

    protected boolean saveCre(BOCreWrapper creWrapper, PSEventCre event, boolean sendB){
        boolean result = false;
        if(sendB){
            result = processBOCre(creWrapper, event);
        }
        return result;
    }


    private List<String> getNeededAdditionalCREAttributes(Trade trade) {
        if(null!=trade){
            String domainValueComment = LocalCache.getDomainValueComment(DSConnection.getDefault(), ADDITIONAL_CRE_ATT, trade.getProductType());

            if(Util.isEmpty(domainValueComment)) {
                domainValueComment = LocalCache.getDomainValueComment(DSConnection.getDefault(), ADDITIONAL_CRE_ATT, "Default");
            }

            if(!Util.isEmpty(domainValueComment)) {
                domainValueComment.split(DELIMITER);
                return Arrays.asList(domainValueComment.split(DELIMITER));
            }
        }

    	return new ArrayList<String>();
    }

    protected boolean processBOCre(BOCreWrapper creWrapper,PSEventCre event) {
        boolean sent = false;

        BOCre cre=creWrapper.cre;
        Trade trade = boCreUtils.getTrade(cre.getTradeLongId());
        doSleepifPredate(trade,cre);

        creWrapper.santBOCre = creMappingFactory.getCreType(cre,trade);

        if(blockCresByFilter(creWrapper.santBOCre )){
            Log.info(this,"Sending cre blocked by Filter: "+ creWrapper.santBOCre.getProductType()+FILTER + " - CreType: " + creWrapper.santBOCre.getCreType()+ "CreId: " + cre.getId());
            this.processCreEvent(event);
            return true;
        }

        if(missingRepoAccrualPartenonIDFilter(creWrapper)){
            Log.info(this,"Missing PartenonID for: "+ creWrapper.santBOCre .getProductType() + " - CreType: " + creWrapper.santBOCre.getEventType()+ "CreId: " + cre.getId());
            this.processCreEvent(event);
            return true;
        }

        generateLog(trade,event,cre);
        this.processCreEvent(event);
        if(creWrapper.santBOCre !=null){
                addAdditionalCreAtt(creWrapper.cre,trade,creWrapper.santBOCre );
                if(saveCre(creWrapper,trade)) {
                    sent = sendMessage(event,creWrapper.santBOCre.getCreLine().toString());
                }
        }else{
            Log.system(this.getClass().getName(), "No Cre Type found for: " + cre!=null ? cre.getEventType() :"");
        }

        if(!sent&&!"DELETED".equals(creWrapper.cre.getStatus())){
            updateCreToSend(creWrapper.cre,event);
        }

        Log.info(this,"Time processing creID: " +cre.getId() + " eventType: " + cre.getEventType() );
        return sent;
    }

    /**
     * Block cres configured on ProductType.CreEventTypesToBlock
     * Block by date if domainValue Date is configured on ProductType.CreEventTypesToBlock
     *
     * @param creType
     * @return
     */
    protected boolean blockCresByFilter(SantBOCre creType){
        if(Optional.ofNullable(creType).isPresent()){
            final String productType = creType.getProductType();
            final String eventType = creType.getEventType();
            if(!Util.isEmpty(productType) && !Util.isEmpty(eventType)){
                String domainName = productType.concat(FILTER);
                Vector<String> boCreEventTypesToBlock = LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
                final boolean dateFilter = Arrays.stream(boCreEventTypesToBlock.toArray()).map(String.class::cast).anyMatch("Date"::equalsIgnoreCase);
                final boolean blockEventType = Arrays.stream(boCreEventTypesToBlock.toArray()).map(String.class::cast).anyMatch(eventType::equalsIgnoreCase);
                if(blockEventType && dateFilter){
                    try {
                        String date = LocalCache.getDomainValueComment(DSConnection.getDefault(),domainName,"Date");
                        final JDate dateToBlock = JDate.valueOf(date);
                        final JDate effectiveDate = creType.getEffectiveDate();
                        return effectiveDate.before(dateToBlock);
                    }catch (Exception e){
                        Log.error(this.getClass().getSimpleName(),"Error filter cre:" + e);
                        return false;
                    }
                }
                return blockEventType;
            }
        }
        return false;
    }

    protected boolean missingRepoAccrualPartenonIDFilter(BOCreWrapper wrapper){
        return wrapper.santBOCre != null
                && wrapper.santBOCre.getTrade() != null
                && wrapper.santBOCre.getTrade().getProduct() instanceof Repo
                && wrapper.santBOCre.getEventType().equalsIgnoreCase("ACCRUAL")
                && Util.isEmpty(wrapper.santBOCre.getPartenonId());
    }


    protected void writeCamelContext(){
        List<Map<String, String>> values = new ArrayList<>();
        try {
            QueryExp qe = Query.isInstanceOf(new StringValueExp("org.apache.camel.management.mbean.ManagedCamelContext"));
            MBeanServer ms = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> contexts = ms.queryNames(new ObjectName("org.apache.camel:*"), qe);
            if(Util.isEmpty(contexts)){
                for (ObjectName context : contexts) {
                    Map<String, String> curMap = new HashMap<String, String>();
                    String[] attributes=new String[] { "CamelId", "MinProcessingTime","MeanProcessingTime", "MaxProcessingTime" };;
                    AttributeList al = null;
                    al = ms.getAttributes(context, attributes);
                    List<javax.management.Attribute> ale = al.asList();
                    for (int i = 0; i < ale.size(); i++) {
                        Attribute attribute = ale.get(i);
                        String val = attribute.getValue() != null ? attribute.getValue().toString() : "";
                        curMap.put(attribute.getName(), val);
                    }
                    values.add(curMap);
                }
                StringBuilder contextLog = new StringBuilder();
                contextLog.append("CamelIds:");
                values.forEach(va -> {
                    contextLog.append(" " + va.get("CamelId")+",");
                });
                Log.system(this.getClass().getSimpleName(),"CamelContext: " + contextLog.toString());
            }

        } catch (Exception e) {
          Log.error(this.getClass().getSimpleName(),"Error:" + e.getCause());
        }
    }

    protected boolean saveCre(BOCreWrapper creWrapper,Trade trade) {
        CreArray cres = new CreArray();
        cres.add(creWrapper.cre);
        try {
            this._ds.getRemoteBO().saveCres(cres);
            Log.system(this.getClass().getName(),"Cre with id: " +  creWrapper.cre.getId() + " , Sent Status: " +  creWrapper.cre.getSentStatus() + " and accountBalance: " +  creWrapper.cre.getAttributeValue("accountBalance") + " saved successfully");
            return true;
        } catch (CalypsoServiceException e) {
            return failoverCREReprocess(creWrapper,cres,trade,e);
        }
    }

    private boolean failoverCREReprocess(BOCreWrapper creWrapper, CreArray cres, Trade trade, CalypsoServiceException e){
        boolean needsResend=false;
        Log.error(this,"Error:" +e);
        if(null!=creWrapper && null!=creWrapper.cre){
            Log.error(this,"Error saving cre: " + creWrapper.cre.getId());
            Log.system(this.getClass().getName(), "Reload Cre for saving again. " +creWrapper.cre.getId() );
            try {
                cres.clear();
                BOCre reloadedCre = this._ds.getRemoteBO().getBOCre(creWrapper.cre.getId());
                if(null!=reloadedCre) {
                        creWrapper.cre=reloadedCre;
                        creWrapper.santBOCre=creMappingFactory.getCreType(creWrapper.cre,trade);
                        addAdditionalCreAtt(creWrapper.cre, trade, creWrapper.santBOCre);
                        if (! creWrapper.cre.getStatus().equals("DELETED")) {
                            if ( creWrapper.cre.getStatus().equals("NEW")) {
                                creWrapper.cre.setSentStatus("SENT");
                            } else {
                                creWrapper.cre.setSentStatus("RE_SENT");
                            }
                            needsResend = true;
                        } else {
                            creWrapper.cre.setSentStatus("DELETED");
                        }
                        cres.add( creWrapper.cre);

                        this._ds.getRemoteBO().saveCres(cres);
                        Log.system(this.getClass().getName(), "Cre with id: " +  creWrapper.cre.getId() + " , Status: " +  creWrapper.cre.getStatus() + " , Sent Status: " +  creWrapper.cre.getSentStatus() + " and accountBalance: " +  creWrapper.cre.getAttributeValue("accountBalance") + " saved successfully");
                }
            } catch (CalypsoServiceException ex) {
                Log.error(this,"Error reloading/saving CRE: " +  creWrapper.cre.getId() + " " + ex);
                needsResend=false;
            }
        }
        return needsResend;
    }


    protected void updateCreToSend(BOCre cre, PSEvent event) {
        CreArray cres = new CreArray();
        if (null != cre) {
            try {
                BOCre boCre = this._ds.getRemoteBO().getBOCre(cre.getId());
                if (null != boCre) {
                    boCre.setSentStatus("");
                    cres.add(boCre);
                }
                this._ds.getRemoteBO().saveCres(cres);
                Log.system(this.getClass().getName(), "The sending process failed, updating Sent Status of Cre with id: " + cre.getId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error saving CRE: " + cre.getId() + " " + e);
                saveRetryCre(cre, event, cres);
            }
        }
    }

    private boolean saveRetryCre(BOCre cre, PSEvent event, CreArray cres) {
        try {
            cres.clear();
            BOCre boCre = this._ds.getRemoteBO().getBOCre(cre.getId());
            if (null != boCre) {
                boCre.setSentStatus("");
                cres.add(boCre);
            }
            this._ds.getRemoteBO().saveCres(cres);
            Log.system(this.getClass().getName(), "The retry sending process failed, updating Sent Status of Cre with id: " + cre.getId());
            return true;
        } catch (CalypsoServiceException ex) {
            Log.error(this, "Error saving CRE: " + cre.getId() + " " + ex);
            return false;
        }
    }

    /**
     * Send Message
     * @param messsage
     * @return
     */
    private boolean sendMessage(PSEventCre event,String messsage){
        if( camelConnectionManagement.getContext() !=null){
            try {
                if(!isTestingMode()){
                    Log.debug(this.getClass().getSimpleName(),"CamelID: " + camelConnectionManagement.getContext().getName()
                            + " EngineName: " + this.getEngineName()
                            + " PSEventID: " + event.getLongId()
                            + " ThreadID: " + Thread.currentThread().getId());
                    camelConnectionManagement.sendMessage(SEND_ROUTE,messsage);
                }
                return true;
            } catch (Exception e) {
                Log.error(this,"Error sending message " + e.getCause());
            }
        }
        return false;
    }

    /**
     * Send Message Posting
     * @param messsage
     * @return
     */
    private boolean sendPostingMessage(long postingId, String messsage){
        if( camelConnectionManagement.getContext() !=null){
            try {
                if(!isTestingMode()){
                    camelConnectionManagement.sendMessage(SEND_ROUTE,messsage);
                }
                return true;
            } catch (Exception e) {
                Log.error(this,"Error sending message " + e.getCause().getMessage());
            }
        }
        return false;
    }


    protected boolean isTestingMode(){
        return "True".equalsIgnoreCase(testingMode) || "Y".equalsIgnoreCase(testingMode);
    }

    /**
     * Sleep for 2s if trade COT or COT_REV is predate
     * @param trade
     * @param cre
     *
     */
    protected void doSleepifPredate(Trade trade, BOCre cre){
        if(null!=trade && null!=cre){
            if(boCreUtils.isPredate(cre, trade)){
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Log.error(this,e );
                }
            }
        }
    }


    @SuppressWarnings("rawtypes")
	public boolean process(PSEvent event) {
        if (event instanceof PSEventCre) {
           if (this._creSenderListener != null) {
              this._creSenderListener.newEvent(event);
           }
           else if (Log.isCategoryLogged(TRACE)) {
              Log.debug(TRACE, "Event: " + event);
           }
           Vector tasks = new Vector();
           try {
               return this.handleEvent((PSEventCre)event, tasks);
           } catch (Exception e) {
              Log.error(this, e);
              return false;
           }
        }
        else if (event instanceof PSEventPostingLiquidation) {
            Vector tasks = new Vector();
            try {
               return this.handleEvent((PSEventPostingLiquidation)event, tasks);
            } catch (Exception e1) {
               Log.error(this, e1);
               return false;
            }
        }
        else {
           return false;
        }
     }


    public boolean handleEvent(PSEventPostingLiquidation event, Vector exceptions) {
        BOPosting posting;
        boolean processed = false;

        try {
        	long postingId = event.getPostingId();
        	posting = DSConnection.getDefault().getRemoteBackOffice().getBOPosting(postingId);
        	if(posting!=null) {

        		processed = this.processPostingEvent(event);
        		if(processed) {
		            if (savePosting(posting, event, true)) {
		               // mark as processed
		               return true;
		            }
        		}
        	}
        	return false;
        } catch (Exception e) {
           return false;
        }
     }


    protected boolean savePosting(BOPosting posting, PSEventPostingLiquidation event, boolean sendB) throws Exception {
        boolean result = false;
        if(sendB){
            result = processBOPosting(posting, event);
            if(result){
                savePosting(posting,event);
            }
        }
        return result;
    }


    private void savePosting(BOPosting posting, PSEvent event) throws CalypsoServiceException {
        PostingArray postings = new PostingArray();
        posting.setStatus("SENT");
        posting.setSentDate(new JDatetime());
        postings.add(posting);
        this._ds.getRemoteBO().savePostings(postings);
    }


    private boolean processBOPosting(BOPosting posting, PSEventPostingLiquidation event) throws CalypsoServiceException {
        boolean sent = false;

        Trade trade = boCreUtils.getTrade(posting.getTradeLongId());

        final SantBOCre creType = creMappingFactory.getCreType(posting,trade);
        if(creType!=null){
                sent = sendPostingMessage(posting.getTradeLongId(), creType.getCreLine().toString());
        }else{
            Log.system(this.getClass().getName(), "No Posting Type found for: " + posting!=null ? posting.getEventType() :"");
        }
        return sent;
    }


    private boolean processPostingEvent(PSEventPostingLiquidation event) {
        try {
           this._ds.getRemoteTrade().eventProcessed(event.getLongId(), this.getEngineName());
           return true;
        } catch (Exception var3) {
           Log.error(this, var3);
           return false;
        }
     }

    @Override
    public boolean handleEvent(PSEventCre event, Vector exceptions) {
        BOCre cre = event.getBOCre();
        JDate today = null;
        if (cre.getBookId() > 0) {
            Book book = BOCache.getBook(this._ds, cre.getBookId());
            if (book != null && book.getLocation() != null) {
                today = (new JDatetime()).getJDate(book.getLocation());
            }
        }

        if (today == null) {
            today = JDate.getNow();
        }

        JDate effectiveDate = cre.getEffectiveDate();
        if("SECLENDING_FEE".equalsIgnoreCase(cre.getEventType())){
            Trade tradeToProcess = BOCreUtils.getInstance().getTrade(cre.getTradeLongId());
            if(sendPastSLFee(cre,tradeToProcess)){
                cre.setSentStatus("DELETED");
                cre.addAttribute("SL_MIG", "Block by SL_MIG_DATE");
                return saveBOCre(event, this.getEngineName(),cre);
            }
            effectiveDate = boCreUtils.getMaturityPDVFee(cre,tradeToProcess);
            /*
            Trade trade = boCreUtils.getTrade(cre.getTradeLongId());
            if(null!=trade && trade. getProduct() instanceof SecLending){
                JDate endDate = ((SecLending)trade.getProduct()).getEndDate();
                if(endDate.getMonth() == today.getMonth()){
                    effectiveDate = endDate;
                }
            }
             */
        }

        String isActive = Optional.ofNullable(LocalCache.getDomainValueComment(this._ds, "CodeActivationDV", "CreEffecitveDate" )).orElse("");
        if (!isActive.isEmpty()) {
            JDatetime todayDateTime = new JDatetime().add(0, -2, 0, 0, 0);
            today = todayDateTime.getJDate(TimeZone.getDefault());
        }

        if (this._checkEffectiveDate && effectiveDate.after(today)) {
            return this.processCreEvent(event);
        } else {
            int retry = 0;

            //Wrapper used to keep objects reference while reloading cre in case of error
            BOCreWrapper creWrapper=new BOCreWrapper(cre);
            while(retry <= this._numberOfTry) {
                try {
                    cre=creWrapper.cre;
                    if (cre.getStatus().equals("NEW") && !this.isNotSent(cre)) {
                        return this.processCreEvent(event);
                    }

                    if ("DELETED".equalsIgnoreCase(cre.getStatus())) {
                        return this.processCreEvent(event);
                    }

                   /* if (cre.getSentStatus() != null && cre.getSentStatus().equals("DELETED")) {
                        return this.processCreEvent(event);
                    }

                    if (cre.getStatus().equals("DELETED") && this.isNotSent(cre)) {
                        cre.setSentStatus("DELETED");
                        return saveBOCre(event, this.getEngineName(),cre);

                    }

                    if (cre.getStatus().equals("DELETED") && cre.getSentStatus() != null && cre.getSentStatus().equals("RE_SENT")) {
                        return this.processCreEvent(event);
                    }*/

                    if (cre.getEventType().equalsIgnoreCase("SECLENDING_FEE") && cre.getCreType().equalsIgnoreCase("REVERSAL") && !cre.getOriginalEventType().equalsIgnoreCase("CANCELED_TRADE")){
                        cre.setSentStatus("NOT_SENT");
                        return saveBOCre(event, this.getEngineName(),cre);
                    }

                    if (cre.getStatus().equals("NEW")) {
                        cre.setSentStatus("SENT");
                    } else {
                        cre.setSentStatus("RE_SENT");
                    }

                    JDatetime now = new JDatetime();
                    cre.setSentDate(now);
                    if (this.saveCre(creWrapper, event, true)) {
                        String message = cre.toString() + "SENT";
                        this.onNewEvent(message);
                        return true;
                    }
                } catch (Exception var10) {
                    if (retry >= this._numberOfTry) {
                        Log.error(this, var10);
                        return false;
                    }

                    try {
                        String error = "Retry processing Cre " + cre.getId();
                        creWrapper.cre = this._ds.getRemoteBO().getBOCre(cre.getId());
                        ++retry;
                        if (null == creWrapper.cre) {
                            Log.error(this, error + ".  Not found in the database.  Original exception : " + var10);
                            return false;
                        }

                        Log.system(CreOnlineSenderEngine.class.getName(), error);
                    } catch (Exception var9) {
                        Log.error(this, var9);
                        return false;
                    }
                }
            }

            return false;
        }
    }

    boolean processCreEvent(PSEventCre event) {
        try {
            this._ds.getRemoteTrade().eventProcessed(event.getLongId(), this.getEngineName());
            return true;
        } catch (Exception var3) {
            Log.error(this, var3);
            return false;
        }
    }

    boolean isNotSent(BOCre cre) {
        return cre.getSentStatus() == null || cre.getSentStatus().trim().length() == 0 || cre.getSentStatus().equals("null");
    }

    /**
     * Log para comprobar duplicados en el envio eliminar despues.
     *
     * @param trade
     * @param event
     * @param cre
     */
    protected void generateLog(Trade trade, PSEvent event, BOCre cre){
        long tradeId = null != trade ? trade.getLongId() : 0L;
        Log.system(CreOnlineSenderEngine.class.getName(),"Processing EventId: " + event.getLongId() + " CreId: " + cre.getId() + " TradeId: "+ tradeId);
    }

    private boolean sendPastSLFee(BOCre cre, Trade trade){
        if(null!=cre && null!=trade){
            JDate sl_mig = trade.getKeywordAsJDate("SL_MIG");
            if(null!=sl_mig){
                return cre.getEffectiveDate().lte(sl_mig);
            }
        }
        return false;
    }

    public boolean saveBOCre(PSEvent event, String engineName, BOCre cre){
        CreArray array = new CreArray();
        if(null!=cre){
            try {
                array.add(cre);
                DSConnection.getDefault().getRemoteBackOffice().saveCres(event.getLongId(), engineName, array, false);
                return true;
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error saving Cre: " + e.getCause());
            }
        }
        return false;
    }


    protected void addAdditionalCreAtt(BOCre cre,Trade trade,SantBOCre creType){
        List<String> neededAttributes = getNeededAdditionalCREAttributes(trade);
        if(neededAttributes.contains(BOCreConstantes.CONTRAT_ATT))
            cre.addAttribute(BOCreConstantes.CONTRAT_ATT, String.valueOf(creType.getContractId()));
        if(neededAttributes.contains(BOCreConstantes.ACCOUNT_BALACE_ATT))
            cre.addAttribute(BOCreConstantes.ACCOUNT_BALACE_ATT,String.valueOf(creType.getAccountBalance()));
    }

    @Override
    public void stop() {
        if( camelConnectionManagement.getContext() !=null){
            try {
                camelConnectionManagement.stopConnection();
            } catch (Exception exc) {
                Log.error(this.getClass().getSimpleName(), "Errors while stopping camel connection", exc.getCause());
            }
        }
        super.stop();
    }


    protected static class BOCreWrapper{
        BOCre cre;
        SantBOCre santBOCre;

        BOCreWrapper(BOCre cre){
            this.cre=cre;
        }
    }
}

