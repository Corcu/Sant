package calypsox.tk.util;

import calypsox.camel.CamelConnectionManagement;
import calypsox.tk.bo.obb.OBBGenericBean;
import calypsox.tk.camel.routes.OBBGenericRoutesBuilder;
import calypsox.util.OBBReportUtil;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.util.PostingArray;
import com.calypso.tk.util.ScheduledTaskREPORT;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static calypsox.tk.util.ScheduledTaskOBB_GENERIC_POSTING_SENT.INTEREST.INTEREST;


public class ScheduledTaskOBB_GENERIC_POSTING_SENT extends ScheduledTaskREPORT {
    private CamelConnectionManagement connectionManagement;
    private static final String PROPERTIES_FILENAME = "obbgeneric.connection.properties";
    private Properties properties = new Properties();

    public static final String ATT_TEST_MODE = "Test Mode";
    public static final String ATT_NOT_SET_AGREGO = "DoNotSetAgrego";
    private static final String SEND_ROUTE = "direct:obbGenericMessages";
    public enum COT{COT}
    public enum COT_REV{COT_REV, COT_REVERSAL, COT_REV_REVERSAL}
    public enum MTM_FULL{MTM_FULL, MTM_FULL_BASE}
    public enum MTM_REV{MTM_FULL_REVERSAL, MTM_FULL_BASE_REVERSAL}
    public enum INTEREST{INTEREST, INTEREST_BASE, TERMINATION_FEE, TERMINATION_FEE_BASE, INTEREST_REVERSAL, INTEREST_BASE_REVERSAL}
    public enum MATURE{MATURE, MATURE_REVERSAL}
    public enum FX_TRANSLATION{FX_TRANSLATION, FX_TRANSLATION_REVERSAL}
    public enum FX_TRANSLATION_D01{FX_TRANSLATION_D01, FX_TRANSLATION_D01_REVERSAL}
    public enum OTHERS{OTHERS}

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(ATT_TEST_MODE).description("No sent Posting Messages."));
        attributeList.add(attribute(ATT_NOT_SET_AGREGO).booleanType().description("Do not set agrego."));
        return attributeList;
    }

    @Override
    public Vector getDomainAttributes() {
        Vector<String> domainAttributes = super.getDomainAttributes();
        domainAttributes.add(ATT_TEST_MODE);
        domainAttributes.add(4,ATT_NOT_SET_AGREGO);
        return domainAttributes;
    }

    @Override
    public Vector getAttributeDomain(String attr, Hashtable currentAttr) {
        Vector v = new Vector();
        if (attr.equals(ATT_NOT_SET_AGREGO)) {
            v = new Vector();
            v.addElement("true");
            v.addElement("false");
            return v;
        }
        return v;
    }

    @Override
    protected void modifyTemplate(Report reportToFormat) {
        boolean agrego = getBooleanAttribute(ATT_NOT_SET_AGREGO);
        Attributes atts = reportToFormat.getReportTemplate().getAttributes();
        atts.add(ATT_NOT_SET_AGREGO,agrego);
        reportToFormat.getReportTemplate().setAttributes(atts);
    }

    @Override
    public String getTaskInformation() {
        return "Generate and send OBB Generic Lines (Postings) to MIC";
    }

    /**
     * Init camel connection
     */
    private void initConnection(){
        connectionManagement = new CamelConnectionManagement();
        loadProperties();
        if(Optional.ofNullable(properties).isPresent()){
            connectionManagement.initCamelContext(properties, new OBBGenericRoutesBuilder()).start();
        }else{
            Log.system(ScheduledTaskOBB_GENERIC_POSTING_SENT.class.getName(),"No obbgeneric.connection.properties found.");
        }
    }

    /**
     * Load properties for camel connection.
     */
    private void loadProperties() {
        try {
            this.properties = GatewayUtil.readPropertyFile(PROPERTIES_FILENAME);
        } catch (IOException var5) {
            Log.error(this, "Error while loading obbgeneric.connection.properties", var5);
        }
    }


    @Override
    protected String saveReportOutput(ReportOutput output, String format, String type, String[] fileNames, StringBuffer sb) {
        initConnection();

        HashMap<String,BOPostingGroup> groupMessages = new HashMap<>();
        List<Class<? extends Enum>> myEnums = new ArrayList<>();
        myEnums.add(COT.class);
        myEnums.add(COT_REV.class);
        myEnums.add(MTM_FULL.class);
        myEnums.add(MTM_REV.class);
        myEnums.add(INTEREST.class);
        myEnums.add(MATURE.class);
        myEnums.add(FX_TRANSLATION.class);
        myEnums.add(FX_TRANSLATION_D01.class);

        if(null!=output){
            DefaultReportOutput reportOutput = (DefaultReportOutput) output;
            if(null!=reportOutput.getRows()){
                Arrays.stream(reportOutput.getRows()).forEach(line ->{
                    OBBGenericBean bean = line.getProperty("Default");

                    //Extrac FXTrasnlation
                    String events = "";
                    for(Class<? extends Enum> enu : myEnums){
                        if(null!=bean && isInEnum(bean,enu)){
                            events = Arrays.stream(enu.getEnumConstants()).map(Enum::toString).collect(Collectors.joining(","));
                        }
                    }
                    if(Util.isEmpty(events)){
                        events = Arrays.stream(OTHERS.values()).map(Enum::toString).collect(Collectors.joining(","));
                    }

                    if(!Util.isEmpty(events)) {
                        String key = generateKey(events,bean);

                        if(groupMessages.containsKey(key)){
                            BOPostingGroup boPostingGroup = groupMessages.get(key);
                            if(null!=boPostingGroup){
                                BOPostingMessages boPostingMessa = boPostingGroup.getPostingMessages().stream().filter(boPostingMessages -> boPostingMessages.getSize() < 10).findFirst().orElse(null);
                                boPostingGroup.addPostingId(bean.getBoPosting());

                                if(null==boPostingMessa){
                                    BOPostingMessages otherBOPostingMessage = new BOPostingMessages();
                                    List<String> otherList = new ArrayList<>();
                                    otherList.add(bean.toString());
                                    otherBOPostingMessage.setMainPostingId(bean.getPostingID());
                                    otherBOPostingMessage.setPostingMessages(otherList);
                                    boPostingGroup.getPostingMessages().add(otherBOPostingMessage);
                                }else{
                                    List<String> postingMessages = boPostingMessa.getPostingMessages();
                                    bean.setPostingID(boPostingMessa.getMainPostingId());
                                    postingMessages.add(bean.toString());
                                    boPostingMessa.setPostingMessages(postingMessages);
                                    boPostingMessa.sumSize();
                                }
                            }
                        }else{
                            List<BOPosting> postings = new ArrayList<>(); //Lista de posting a guardar
                            List<BOPostingMessages> postingMessages = new ArrayList<>(); //lista con las agrupaciones de los posting
                            BOPostingMessages boPostingMessages = new BOPostingMessages(); // objeto con agrupacion de posting y main ID para ese grupo
                            List<String> messages = new ArrayList<>(); //lista de mensajes a enviar.

                            messages.add(bean.toString());
                            postings.add(bean.getBoPosting());
                            boPostingMessages.setPostingMessages(messages);
                            boPostingMessages.setMainPostingId(bean.getPostingID());

                            postingMessages.add(boPostingMessages);
                            BOPostingGroup boPostingGroup = new BOPostingGroup(events, postingMessages, postings);

                            groupMessages.put(key,boPostingGroup);
                        }
                    }
                });
            }
            //Send and save posting messages
            groupMessages.forEach((key, value) -> publishMessage(value));
        }

        stopCamelConnection();

        return "";
    }

    private void publishMessage(BOPostingGroup groupMessages){
        List<BOPostingMessages> postingMessages = groupMessages.getPostingMessages();
        for(BOPostingMessages boPostingMessages : postingMessages){
            String messageToSend = boPostingMessages.getPostingMessages().stream().collect(Collectors.joining(";"));
            sendMessage(messageToSend);
        }
        //After send update all BOPosting Messages
        updatePostingGroups(groupMessages);
    }

    private void sendMessage(String message){
        if(Optional.ofNullable(connectionManagement.getContext()).isPresent()){
            try {
                connectionManagement.sendMessage(SEND_ROUTE,message);
            } catch (Exception e) {
                Log.error(this,"Error sending message: "+message+" "+e.getCause());
            }
        }
    }

    /**
     * Optimizar guardar en hilos o de 999 en 999
     * @param boPostingGroup
     * @return
     */
    private void updatePostingGroups(BOPostingGroup boPostingGroup){
        PostingArray postingsToSave = new PostingArray();
        List<Long> postingsIds = new ArrayList<>();
        boPostingGroup.getPosting().forEach(posting -> {
            if(null!=posting && !"SENT".equalsIgnoreCase(posting.getStatus())){
                posting.setStatus("SENT");
                postingsToSave.add(posting);
                postingsIds.add(posting.getId());
            }
        });
        try {
            DSConnection.getDefault().getRemoteBO().savePostings(postingsToSave);
        } catch (CalypsoServiceException e) {
            //Reload and save
            Log.error(this,"Errors updating posting: " + e.getCause());
            Log.info(this,"Reloading and Saving postings...");
            reloadPostingsAndResave(postingsIds);
        }
    }

    public <E extends Enum<E>> boolean isInEnum(OBBGenericBean bean, Class<E> enumClass) {
        for (E e : enumClass.getEnumConstants()) {
            String eventType = bean.getBoPosting().getEventType();
            //Escpecifico para FX_TRANSLATION donde se teen que mirar el operating position
            if("FX_TRANSLATION".equalsIgnoreCase(bean.getBoPosting().getEventType()) && isD01OperatingPosition(bean)){
                eventType = eventType + "_" + "D01";
            }
            //Específico para Reversal
            if("REVERSAL".equalsIgnoreCase(bean.getBoPosting().getPostingType())){
                eventType = eventType + "_" + bean.getBoPosting().getPostingType();
            }
            if(e.name().equals(eventType)) {
                return true;
            }
        }
        return false;
    }

    private void reloadPostingsAndResave(List<Long> postingsIds){
        PostingArray postingToSave = new PostingArray();
        String idList = postingsIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String where = "bo_posting_id IN ("+idList+")";
        try {
            PostingArray boPostings = DSConnection.getDefault().getRemoteBO().getBOPostings(where, null);
            boPostings.forEach(boPosting -> {
                if(null!=boPosting && !"SENT".equalsIgnoreCase(boPosting.getStatus())){
                    boPosting.setStatus("SENT");
                    postingToSave.add(boPosting);
                }
            });
            DSConnection.getDefault().getRemoteBO().savePostings(postingToSave);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error reloading postings: " + e.getCause());
        }
    }

    private String generateKey(String events, OBBGenericBean bean){
        String key = "";
        String event = Arrays.stream(INTEREST.values()).filter(value -> events.contains(value.name())).map(Enum::name).findFirst().orElse("");
        if(!Util.isEmpty(event)){
            key = events +"/"+bean.getBoPosting().getTransferLongId();
        }else {
            key = events +"/"+bean.getTrade().getLongId();
        }
        return key;
    }

    public boolean isD01OperatingPosition(OBBGenericBean bean) {
        if("D01".equalsIgnoreCase(bean.getOperatingPosition())){
            return true;
        }
        String creditDebit = bean.getCreditDebit();
        int accountID = 0;
        if("Credit".equalsIgnoreCase(creditDebit)){
            accountID = bean.getBoPosting().getDebitAccountId();
        }else if("Debit".equalsIgnoreCase(creditDebit)){
            accountID = bean.getBoPosting().getCreditAccountId();
        }

        if(accountID > 0){
            try {
                Account account = DSConnection.getDefault().getRemoteAccounting().getAccount(accountID);
                if(Optional.ofNullable(account).isPresent()){
                    if("PosicionOperativa".equalsIgnoreCase(OBBReportUtil.getAccountTypeValue(account))
                            || com.calypso.infra.util.Util.isEmpty(OBBReportUtil.getAccountTypeValue(account))){
                        String accountPosicionValue = OBBReportUtil.getAccountPosicionValue(account);
                        if("D01".equalsIgnoreCase(accountPosicionValue)){
                            return true;
                        }
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(OBBReportUtil.class.getName(),"Error loading Account: " + accountID + " "+ e.getCause());
            }
        }
        return false;
    }

    private void stopCamelConnection(){
        if(connectionManagement!=null
                && connectionManagement.getContext()!=null){
            connectionManagement.stopConnection();
        }
    }

    @Override
    public void stop() {
        stopCamelConnection();
        super.stop();
    }

    private static class BOPostingMessages {
        int size = 1;

        String mainPostingId = "";
        List<String> postingMessages = new ArrayList<>();

        public String getMainPostingId() {
            return mainPostingId;
        }

        public void setMainPostingId(String mainPostingId) {
            this.mainPostingId = mainPostingId;
        }

        public List<String> getPostingMessages() {
            return postingMessages;
        }

        public void setPostingMessages(List<String> postingMessages) {
            this.postingMessages = postingMessages;
        }

        public int getSize() {
            return size;
        }

        public void sumSize() {
            this.size++;
        }
    }

    private static class BOPostingGroup {
        String eventType = "";
        List<BOPosting> postings = new ArrayList<>();
        List<BOPostingMessages> postingMessages = new ArrayList<>();

        public BOPostingGroup( String eventType, List<BOPostingMessages> postingMessages,List<BOPosting> postings) {
            this.eventType = eventType;
            this.postingMessages = postingMessages;
            this.postings = postings;
        }

        public List<BOPostingMessages> getPostingMessages() {
            return postingMessages;
        }

        public List<BOPosting> getPosting() {
            return postings;
        }

        public void addPostingId(BOPosting posting) {
            if(!Util.isEmpty(this.postings) && null!=posting && !this.postings.contains(posting)){
                this.postings.add(posting);
            }
        }
    }
}