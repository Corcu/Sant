package calypsox.tk.util;

import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.report.SantMCallPositionValCreReport;
import com.calypso.helper.CoreAPI;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallPosition;
import com.calypso.tk.collateral.SecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventPositionValuation;
import com.calypso.tk.mo.LiquidationConfig;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * @author Custom
 */
public class ScheduledTaskCUSTOM_EOD_SEC_MARGINCALL_VALUATION extends ScheduledTask {

    private static final long serialVersionUID = 7443884454056107499L;
    TaskArray tasks = new TaskArray();
    private static final String CONTRACT_ID_ATTR_STR = "ContractId List";
    private static final String ACCOUNTING_FILTER = "Accounting Sec Filter";
    private static final String ACCOUNTING_SENT_REV = "Send Reversal";
    private static final String CRE_TYPE = "Cre Type";
    private static final String PRODUCT_TYPE = "Product Type";
    private static final String SLB_CIRCUIT_PO = "SLB Circuit PO";
    private static final String ACCOUNTING_SECURITY = "ACCOUNTING_SECURITY";
    private static final String USE_ARRAY_EVENT = "Use PSEvent Array";
    private static final String EVENTS_PER_ARRAY = "Events per Array";
    private static final String ENABLE_TRIPARTY = "Enable Triparty";
    private static final String LOAD_ALL = "Load All";
    private static final String CONTRACT_TYPE = "Contract Type";


    private static final String ALL_STR = "All";


    @Override
    public Vector<String> getDomainAttributes() {
        Vector<String> v = new Vector();
        v.addElement("Collateral Config Level");
        v.addElement("Position Type");
        v.addElement(CONTRACT_ID_ATTR_STR);
        v.addElement(ACCOUNTING_FILTER);
        v.addElement(ACCOUNTING_SENT_REV);
        v.addElement(CRE_TYPE);
        v.addElement(PRODUCT_TYPE);
        v.addElement(SLB_CIRCUIT_PO);
        v.addElement(USE_ARRAY_EVENT);
        v.addElement(EVENTS_PER_ARRAY);
        v.addElement(ENABLE_TRIPARTY);
        v.addElement(CONTRACT_TYPE);
        v.addElement(LOAD_ALL);

        return v;
    }

    public String getTaskInformation() {
        return "Publish the PSEventPositionValuation so the accounting engine/cre engine can generate entries";
    }


    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(CONTRACT_ID_ATTR_STR).description("List of contracts ids."));
        //attributeList.add(attribute(ACCOUNTING_FILTER).description("Filter for load Cres.")); //Just for collateral - >=4.5.1 module version.
        attributeList.add(attribute(ACCOUNTING_SENT_REV).category("Reprocess").description("Send COT_REV on creation date."));
        attributeList.add(attribute(CRE_TYPE).category("Reprocess").description("Select Cre Event Type."));
        attributeList.add(attribute(PRODUCT_TYPE).category("Product").description("Select Product Type RF or RV."));
        attributeList.add(attribute(SLB_CIRCUIT_PO).category("LegalEntity").description("Select PO branch"));
        attributeList.add(attribute(USE_ARRAY_EVENT).category("Cres").description("Use array PSEvent"));
        attributeList.add(attribute(EVENTS_PER_ARRAY).category("Cres").description("Num of events per array"));
        attributeList.add(attribute(ENABLE_TRIPARTY).category("Contract").description("Filter by Triparty Contracts"));
        attributeList.add(attribute(CONTRACT_TYPE).category("Contract").description("Contract Type"));
        attributeList.add(attribute(LOAD_ALL).category("Contract").description("Load all contracts"));


        return attributeList;
    }




    @Override
    public Vector<String> getAttributeDomain(String attribute, Hashtable currentAttr) {
        Vector<String> result = null;
        if ("Position Type".equals(attribute)) {
            result = new Vector();
            result.addAll(MarginCallPosition.getAllPositionTypes());
        } else if ("Collateral Config Level".equals(attribute)) {
            result = new Vector();
            result.add("");
            result.add("Master");
            result.add("Exposure Group");
        } else if (ACCOUNTING_FILTER.equals(attribute)) {
            result = new Vector();
            result.add("");
            result.add("true");
            result.add("false");
            //result.addAll(getContractSDF());
        } else if (ACCOUNTING_SENT_REV.equals(attribute)) {
            result = new Vector();
            result.add("");
            result.add("true");
            result.add("false");
        }else if (USE_ARRAY_EVENT.equals(attribute)) {
            result = new Vector();
            result.add("");
            result.add("true");
            result.add("false");
        } else if (CRE_TYPE.equals(attribute)) {
            result = new Vector();
            result.add("");
            result.addAll(getCreTypes());
        } else if (PRODUCT_TYPE.equals(attribute)) {
            result = new Vector();
            result.add(ALL_STR);
            result.add("Bond");
            result.add("Equity");
        }else if (SLB_CIRCUIT_PO.equals(attribute)) {
            result = new Vector();
            result.add("");
            result.addAll(LocalCache.getDomainValues(DSConnection.getDefault(), "mccAdditionalField.SLB_CIRCUIT_PO"));
        }else if (ENABLE_TRIPARTY.equals(attribute)) {
            result = new Vector();
            result.add("");
            result.add("true");
            result.add("false");
        }else if (LOAD_ALL.equals(attribute)) {
            result = new Vector();
            result.add("");
            result.add("true");
            result.add("false");
        }else if(CONTRACT_TYPE.equals(attribute)) {
        	result = new Vector();
        	result.add("");
        	result.addAll(CollateralConfig.getContractTypes());
        }

        return result;
    }

    private List<String> parseContractIdList(String contractIdList) {
        return Collections.list(new StringTokenizer(contractIdList, ";")).stream().map(id -> (String) id).collect(Collectors.toList());
    }

    public boolean process(DSConnection ds, PSConnection ps) {
        boolean sendReversal = false;
        boolean ret = true;

        //Init remote services
        if (this._publishB || this._sendEmailB) {
            ret = super.process(ds, ps);
        }

        sendReversal = "true".equalsIgnoreCase(getAttribute(ACCOUNTING_SENT_REV));

        Task task = new Task();
        CoreAPI.setObjectId(task, (long) this.getId());
        task.setEventClass("Exception");
        task.setNewDatetime(this.getValuationDatetime());
        task.setUnderProcessingDatetime(this.getDatetime());
        task.setUndoTradeDatetime(this.getUndoDatetime());
        task.setDatetime(this.getDatetime());
        task.setPriority(1);
        task.setId(0L);
        task.setSource(this.getType());
        StringBuilder sb = new StringBuilder("ScheduledTask " + this.getId() + ": ");
        this._executeB = true;
        if (this._executeB) {
            if (!sendReversal) {
                ret = this.publishPositionValuationEvents(this.getAttribute(CONTRACT_ID_ATTR_STR));
            } else {
                sendReversalToday();
            }
        }
        if (!ret) {
            task.setComment(sb.toString());
            task.setEventType("EX_EXCEPTION");
            task.setPriority(2);
        } else {
            task.setComment(sb.toString() + " Successfully processed");
            task.setEventType("EX_INFORMATION");
        }

        task.setCompletedDatetime(new JDatetime());
        task.setStatus(0);
        this.tasks.add(task);

        try {
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(this.tasks, 0L, (String) null);
        } catch (Exception var7) {
            Log.error("ScheduledTaskEOD_SEC_MARGINCALL_VALUATION", var7);
        }

        return ret;
    }

    private boolean publishPositionValuationEvents(String contractIdListString) {
        boolean result = true;
        String positionType = this.getAttribute("Position Type");
        String configLevel = this.getAttribute("Collateral Config Level");
        String contractFilter = this.getAttribute(ACCOUNTING_FILTER);
        String slbContractPo = this.getAttribute(SLB_CIRCUIT_PO);
        String useArrayEvent = this.getAttribute(USE_ARRAY_EVENT);
        String loadAll = this.getAttribute(LOAD_ALL);
        String contractType = this.getAttribute(CONTRACT_TYPE);


        LegalEntity po = this.getProcessingOrg();
        int poId = 0;
        if (po != null) {
            poId = po.getId();
        }


        Report report = new SantMCallPositionValCreReport();
        report.setValuationDatetime(this.getValuationDatetime());
        ReportTemplate template = new MarginCallPositionValuationReportTemplate();
        Vector events;
        if (poId != 0) {
            events = new Vector();
            events.add(poId);
            template.put("PROCESSING_ORG_IDS", events);
        }


        if (!Util.isEmpty(contractIdListString)) {
            template.put("MARGIN_CALL_CONFIG_IDS", contractIdListString);
        }else if(!Util.isEmpty(slbContractPo) ){
            final List<String> contractsIds = loadBranchContracts(po,slbContractPo);
            if(!Util.isEmpty(contractsIds)){
                template.put("MARGIN_CALL_CONFIG_IDS", String.join(",", contractsIds));
            }else {
                return true;
            }
        }else if(null!=contractFilter && Boolean.parseBoolean(contractFilter)){
            final List<String> contractsIds = loadContractsPoFilter(po);
            if(!Util.isEmpty(contractsIds)){
                template.put("MARGIN_CALL_CONFIG_IDS", String.join(",", contractsIds));
            }else {
                return true;
            }
        }else if(null!=loadAll && Boolean.parseBoolean(loadAll)){
            template.put("MARGIN_CALL_CONFIG_IDS", "");
        }
        
        if(null!=contractType && !contractType.isEmpty()) {
        	template.put("MARGIN_CALL_CONFIG_TYPE", contractType);
        }

        //Just for collateral - >=4.5.1 module version...
        /*
        if (!Util.isEmpty(contractFilter)) {
            template.put("COLLATERAL_CONTRACT_FILTER", contractFilter);
        }
         */
        template.put("CONFIG_LEVEL", configLevel);
        template.put("POSITION_UNDERLYING_TYPE", "Security");
        template.put("POSITION_TYPE", positionType);
        report.setReportTemplate(template);
        events = new Vector();

        try {
            DefaultReportOutput output = report.load(new Vector());
            ReportRow[] reportRows = output.getRows();

            for (int index = 0; index < reportRows.length; ++index) {
                ReportRow row = reportRows[index];
                SecurityPosition secPos = row.getProperty("Default");
                if (filterByContractAF(secPos) && filterByPorductType(secPos)) {
                    events.add(this.createPositionValuationEvent(secPos));
                }
            }
        } catch (Exception var15) {
            result = false;
            Log.error(this, var15);
        }

        if (!Util.isEmpty(events)) {
            if(!Util.isEmpty(useArrayEvent) && Boolean.parseBoolean(useArrayEvent)){
                try {
                    getReadWriteDS(getDSConnection()).getRemoteTrade().saveAndPublish(events,true,getNumEventsPerArray());
                } catch (RemoteException var14) {
                    result = false;
                    Log.error(this, var14);
                }
            }else {
                try {
                    DSConnection.getDefault().getRemoteTrade().saveAndPublish(events);
                } catch (RemoteException var14) {
                    result = false;
                    Log.error(this, var14);
                }
            }
        }

        return result;
    }

    private int getNumEventsPerArray(){
        int numEvents = 5000;
        String numEventsPerArray = this.getAttribute(EVENTS_PER_ARRAY);
        try{
            numEvents = Optional.ofNullable(numEventsPerArray).map(Integer::valueOf).orElse(numEvents);
        }catch (Exception e){
            Log.error(this,"Error parsing: " + e);
        }
        return numEvents;
    }

    private PSEventPositionValuation createPositionValuationEvent(MarginCallPosition position) throws Exception { //TODO add Nominal
        SecurityPosition secPos = (SecurityPosition) position;
        PSEventPositionValuation event = new PSEventPositionValuation();
        int prodId = secPos.getCandidate().getId();
        MarginCall marginCall = new MarginCall();
        marginCall.setId(prodId);
        marginCall.setMarginCallId(prodId);
        marginCall.setOrdererRole("ProcessingOrg");
        marginCall.setOrdererLeId(secPos.getCollateralConfig().getPoId());
        event.setProduct(marginCall);

        launchSetPositionIdMethod(event, prodId);
        event.setPricingEnvName(this.getPricingEnv());
        event.setBookId(secPos.getBookId());
        event.setValuationDate(this.getValuationDatetime());
        event.setPlCurrency(secPos.getCurrency());
        event.setLiquidationConfig(LiquidationConfig.getDEFAULT());
        event.setPositionAggregationId(position.getCollateralConfig().getId());
        Vector<PricerMeasure> measures = new Vector();
        PricerMeasure accrualMeasure = this.createPricerMeasure(3, secPos);
        PricerMeasure npvMeasure = this.createPricerMeasure(2, secPos);
        PricerMeasure npvNetMeasure = this.createPricerMeasure(61, secPos);
        PricerMeasure nominalMeasure = this.createPricerMeasure(666, secPos);
        measures.addElement(accrualMeasure);
        measures.addElement(npvMeasure);
        measures.addElement(npvNetMeasure);
        measures.addElement(nominalMeasure);
        event.setMeasures(measures);
        return event;
    }

    private void launchSetPositionIdMethod(PSEventPositionValuation event, int prodId) {
        try {
            Method method = getSetPositionIdMethod(event, "setPositionId", Integer.TYPE);
            if (method != null) {
                method.invoke(event, prodId);
            } else {
                method = getSetPositionIdMethod(event, "setPositionLongId", Long.TYPE);
                method.invoke(event, prodId);
            }
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException exc) {
            Log.error(this.getClass().getSimpleName(), exc.getCause());
        }
    }

    private Method getSetPositionIdMethod(PSEventPositionValuation event, String methodName, Class<?> argType) {
        Method method = null;
        try {
            method = event.getClass().getDeclaredMethod(methodName, argType);
        } catch (NoSuchMethodException exc) {
            Log.debug(this.getClass().getSimpleName(), "No such method in event class", exc.getCause());
        }
        return method;
    }

    private PricerMeasure createPricerMeasure(int measureId, SecurityPosition position) {
        PricerMeasure measure = new PricerMeasure();
        measure.setType(measureId);
        measure.setCurrency(position.getCurrency());
        double value = 0.0D;
        double accrual = position.getAccrual() * 100.0D;
        double nominal = position.getNominal();
        switch (measureId) {
            case 2:
                value = position.getValue();
                break;
            case 3:
                value = accrual * nominal / 100.0D;
                break;
            case 61:
                double npv = position.getValue();
                accrual = accrual * nominal / 100.0D;
                value = npv - accrual;
                break;
            case 666:
                value = nominal;
                break;
        }

        measure.setValue(value);
        return measure;
    }

    private List<String> getContractSDF() {
        List<String> staticDataFilterGroupMemberNames = new ArrayList<>();
        try {
            staticDataFilterGroupMemberNames = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilterGroupMemberNames(Util.string2Vector("CollateralConfig"));
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Filters for contract. " + e);
        }
        return staticDataFilterGroupMemberNames;
    }

    public boolean isValidInput(Vector<String> messages) {
        boolean isValid = super.isValidInput(messages);
        if (this.getAttribute("Position Type") == null) {
            messages.add("Position type is mandatory");
            isValid = false;
        }

        return isValid;
    }

    private List<String> getCreTypes() {
        return LocalCache.getDomainValues(DSConnection.getDefault(), "accEventType");
    }


    private void sendReversalToday() {
        String where = createWhere();
        if (!Util.isEmpty(where)) {
            try {
                CreArray boCres = DSConnection.getDefault().getRemoteBackOffice().getBOCres(null, where, null);
                if (null != boCres && !Util.isEmpty(boCres.getCres())) {
                    changeEffectiveDate(boCres);
                    updateCres(boCres);
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading BOCres: " + e);
            }
        }
    }

    private String createWhere() {
        StringBuffer where = new StringBuffer();
        final String attribute = getAttribute(CRE_TYPE);
        if (!Util.isEmpty(attribute)) {
            where.append(" effective_date >= " + Util.date2SQLString(getValuationDatetime()));
            where.append(" AND bo_cre_type like '" + attribute + "'");
            where.append(" AND cre_type like 'REVERSAL' ");
            where.append(" AND (sent_status NOT IN ('SENT','DELETED','CANCELED') OR sent_status IS NULL)");

            return where.toString();
        }
        return "";
    }


    private void changeEffectiveDate(CreArray boCres) {
        for (BOCre cre : boCres.getCres()) {
            cre.setEffectiveDate(JDate.getNow());
        }
    }

    private boolean filterByContractAF(SecurityPosition position){
        return null!=position && null!=position.getCollateralConfig() && "true".equalsIgnoreCase(position.getCollateralConfig().getAdditionalField(ACCOUNTING_SECURITY));
    }

    private boolean filterByPorductType(SecurityPosition position) {
        final String attribute = getAttribute(PRODUCT_TYPE);
        if (null != position && !Util.isEmpty(attribute)) {
            return isTargetProduct(attribute, position.getProduct());
        }
        return false;
    }

    private boolean isTargetProduct(String attribute, Product positionProduct) {
        boolean res = false;
        switch (attribute) {
            case "Equity":
                res = isEquityPosition(positionProduct);
                break;
            case "Bond":
                res = isBondPosition(positionProduct);
                break;
            case "All":
                res = isBondPosition(positionProduct) || isEquityPosition(positionProduct);
                break;
        }
        return res;
    }


    private boolean isBondPosition(Product positionProduct) {
        return positionProduct instanceof Bond;
    }

    private boolean isEquityPosition(Product positionProduct) {
        return positionProduct instanceof Equity;
    }

    private void updateCres(CreArray boCres) {

        Arrays.stream(boCres.getCres()).parallel().forEach(cre ->{
            try {
                DSConnection.getDefault().getRemoteBO().save(cre,0L,null);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error saving cres: " + e);
            }
        });

    }

    private List<String> loadContractsPoFilter(LegalEntity po){
        List<String> contractsIds = new ArrayList<>();
        if(null!=po){
            final List<CollateralConfig> marginCallConfigByAdditionalField = loadContractsByFilters();
            contractsIds = marginCallConfigByAdditionalField.stream()
                    .filter(contract -> contract.getProcessingOrg().equals(po))
                    .filter(contract -> Util.isEmpty(contract.getAdditionalField("SLB_CIRCUIT_PO")))
                    .map(CollateralConfig::getId)
                    .map(String::valueOf).collect(Collectors.toList());
        }
        return contractsIds;
    }

    private List<String> loadBranchContracts(LegalEntity po,String slbContractPo){
        List<String> contractsIds = new ArrayList<>();
        if(null!=po && !Util.isEmpty(slbContractPo)){
            final List<CollateralConfig> marginCallConfigByAdditionalField = loadContractsByFilters();
            contractsIds = marginCallConfigByAdditionalField.stream()
                    .filter(contract -> contract.getProcessingOrg().equals(po))
                    .filter(contract -> slbContractPo.equalsIgnoreCase(contract.getAdditionalField("SLB_CIRCUIT_PO")))
                    .map(CollateralConfig::getId)
                    .map(String::valueOf).collect(Collectors.toList());
        }
        return contractsIds;
    }

    /**
     * No es posible agregar mas filtros por Additional FIlter en el método getMarginCallConfigByAdditionalField
     *
     * @return
     */
    private List<CollateralConfig> loadContractsByFilters(){
        List<CollateralConfig> marginCallConfigByAdditionalField = new ArrayList<>();

        final RemoteSantCollateralService baseSantCollateralService = DSConnection.getDefault()
                .getRMIService("baseSantCollateralService", RemoteSantCollateralService.class);
        HashMap<String,String> aditionalFields = new HashMap<>();
        String contractType = this.getAttribute(CONTRACT_TYPE);
        aditionalFields.put("ACCOUNTING_SECURITY","True");

        try {
            marginCallConfigByAdditionalField = baseSantCollateralService.getMarginCallConfigByAdditionalField(aditionalFields);
        } catch (PersistenceException e) {
            Log.error(this,"Error loading contracts id: " +e.getCause());
        }
        if(Boolean.parseBoolean(getAttribute(ENABLE_TRIPARTY)) && !Util.isEmpty(marginCallConfigByAdditionalField)){
            marginCallConfigByAdditionalField = filterTripartyContracts(marginCallConfigByAdditionalField);
        }
        if(contractType != null && !contractType.isEmpty()) {
        	marginCallConfigByAdditionalField = filterByContractType(marginCallConfigByAdditionalField);
        }
        return marginCallConfigByAdditionalField;
    }

    private List<CollateralConfig> filterTripartyContracts(List<CollateralConfig> listOfContracts){
        ConcurrentLinkedQueue<CollateralConfig> tempContractList = new ConcurrentLinkedQueue<>();
        if(!Util.isEmpty(listOfContracts)){
            listOfContracts.parallelStream().forEach(contract -> {
                if(contract.isTriParty()){
                    tempContractList.add(contract);
                }
            });
        }
        return new ArrayList<>(tempContractList);
    }
    
    private List<CollateralConfig> filterByContractType(List<CollateralConfig> listOfContracts){
        ConcurrentLinkedQueue<CollateralConfig> tempContractList = new ConcurrentLinkedQueue<>();
        String contractType = this.getAttribute(CONTRACT_TYPE);
        if(!Util.isEmpty(listOfContracts)){
            listOfContracts.parallelStream().forEach(contract -> {
                if(contract.getContractType().equals(contractType)){
                    tempContractList.add(contract);
                }
            });
        }
        return new ArrayList<>(tempContractList);
    }
    


}
