package calypsox.tk.upload.uploader;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.upload.mapper.MurexSecLendingMapper;
import calypsox.util.SantReportingUtil;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.SDISelectorUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.EventTypeAction;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.AccountInterests;
import com.calypso.tk.refdata.*;
import com.calypso.tk.secfinance.sbl.SBL;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.jaxb.Product;
import com.calypso.tk.upload.jaxb.Termination;
import com.calypso.tk.upload.jaxb.*;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.upload.sqlbindnig.UploaderSQLBindVariable;
import com.calypso.tk.upload.util.UploaderSQLBindAPI;
import com.calypso.tk.upload.util.UploaderTradeUtil;
import com.calypso.tk.util.DataUploaderUtil;
import com.calypso.tk.util.TradeArray;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

public class UploadCalypsoTradeSecLending extends com.calypso.tk.upload.uploader.UploadCalypsoTradeSecLending {


    public static final String CONTRACT_TYPE_OSLA = "OSLA";
    public static final String KW_MX_LAST_EVENT = "MxLastEvent";
    public static final String KW_MX_LAST_EVENT_RESTRUCTURE = "mxContractEventIRESTRUCTURE";
    public static final String KW_MX_LAST_EVENT_CANCEL_REISSUE = "mxContractEventICANCEL_REISSUE";

    public static final String KW_SECLENDING_TRADE="SecLendingTrade";
    public static final String KW_WORKFLOW_SUBTYPE="WorkflowSubType";
    public static final String KW_SEC_LENDING_FEE_POOL_VALUE="SecLendingFeePool";
    private static final String INTERNA_STR="INTERNA";
    private static final String FO_SYSTEM_RV = "MRX3";
    private static final String FO_SYSTEM_RF = "MRXFI";

    public static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";

    PricingEnv pricingEnv = null;

    public static final String LOG_CATEGORY = "UploadCalypsoTradeSecLending";

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public TradeArray applyTerminateAction(Trade trade, CalypsoTrade jaxbCalypsoTrade, Vector<BOException> errors) {

        tradeArray = super.applyTerminateAction(trade, jaxbCalypsoTrade, errors);

        Termination terminationDetails = this.calypsoTrade.getTermination();

        if(terminationDetails!=null && terminationDetails.getTerminationAmount()!=null) {
            SecFinance secFinance = (SecFinance) trade.getProduct();

            Vector<EventTypeAction> actions = EventTypeAction.getEventTypeActions(secFinance, EventTypeAction.TERMINATION_ACTION, (JDate)null);

            for(EventTypeAction action : actions) {
                if(trade.getQuantity()>0.0d)
                    action.setCollateralAmount(terminationDetails.getTerminationAmount());
                else
                    action.setCollateralAmount(-terminationDetails.getTerminationAmount());
            }
        }

        return tradeArray;
    }

    protected CollateralConfig getExistingMarginCallConfig(ArrayList<CollateralConfig> filteredEligibleMarginCallConfigs, Cash cash) {

        CollateralConfig headCollateralConfig = getHeadCollateralConfig(filteredEligibleMarginCallConfigs);

        if (headCollateralConfig == null)
            return null;

        CollateralConfig configs = null;

        try {
            configs = headCollateralConfig.clone();
        } catch (CloneNotSupportedException e) {
            Log.error(LOG_CATEGORY, e);
        }

        String contractName = getContractNewName(configs, cash);

        List<CollateralConfig> allMarginCallConfig = null;

        try {
            allMarginCallConfig = ServiceRegistry.getDefault().getCollateralDataServer().getAllMarginCallConfig(headCollateralConfig.getProcessingOrg().getLegalEntityId(), headCollateralConfig.getLegalEntity().getLegalEntityId());
        } catch (CollateralServiceException e) {
            Log.error(LOG_CATEGORY, e);
        }

        Optional<CollateralConfig> existingContract = allMarginCallConfig.stream().filter(s -> s.getName().equals(contractName)).filter(s -> !(s.getAgreementStatus().equals("CLOSED"))).findFirst();

        return existingContract.orElse(null);

    }


    /**
     * return the margin call contract to set.
     * if there is no matching margin call contract we create a new margin call contract based
     * on template margin call contract changing the interest config.
     * @param trade
     * @param
     * @return
     * @throws RemoteException
     */
    @SuppressWarnings("unchecked")
    public CollateralConfig getMarginCallConfig(Trade trade, Vector<BOException> errors) throws RemoteException {

        SecLending secLending = (SecLending)trade.getProduct();

        boolean isFeeCashPool = secLending.getSecLendingType().equals("Fee Cash Pool");

        Cash cash = secLending.getNewVMPool();

        if(cash != null ) {
            ArrayList<CollateralConfig> eligibleMarginCallConfigs = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getEligibleMarginCallConfigs(trade);

            HashMap<Integer,Vector<Account>> accountPerContract = new HashMap<Integer,Vector<Account>>();

            ArrayList<CollateralConfig> filteredEligibleMarginCallConfigs = (ArrayList<CollateralConfig>)eligibleMarginCallConfigs.clone();

            // Check if there's any other contract with same name
            CollateralConfig existingCollateralConfig = getExistingMarginCallConfig(filteredEligibleMarginCallConfigs, cash);
            if(existingCollateralConfig != null)
                return existingCollateralConfig;

            ListIterator<CollateralConfig> itCollateralConfig = filteredEligibleMarginCallConfigs.listIterator();

            // match contract and call account with incoming cash.
            while(itCollateralConfig.hasNext()) {
                CollateralConfig config = itCollateralConfig.next();
                if(!CONTRACT_TYPE_OSLA.equals(config.getContractType())) {
                    itCollateralConfig.remove();
                }
                else {
                    boolean contractMatch = false;

                    if(!isFeeCashPool) {
                        contractMatch = true;
                    }
                    else
                    {
                        Vector<Account> accounts = DSConnection.getDefault().getRemoteAccounting().getAccountByAttribute("MARGIN_CALL_CONTRACT", ""+config.getId());
                        accountPerContract.put(config.getId(), accounts);

                        for(Account act : accounts) {
                            if(!act.getCallAccountB())
                                continue;

                            if(!act.getCurrency().equals(cash.getCurrency()))
                                continue;

                            // check if cash specificities match with call account interest definition
                            for(AccountInterests acctInterests : act.getAccountInterests()) {
                                AccountInterestConfig acctInterestConfig = BOCache.getAccountInterestConfig(DSConnection.getDefault(), acctInterests.getConfigId());
                                if(acctInterestConfig==null)
                                    continue;
                                AccountInterestConfigRange acctInterestConfigRange = acctInterestConfig.getRange(trade.getTradeDate().getJDate(TimeZone.getDefault()), 0.0d, cash.getCurrency());
                                if(acctInterestConfigRange==null)
                                    continue;
                                if(cash.getFixedRateB()) {
                                    if(acctInterestConfigRange.isFixed()
                                            && isEqualTo(cash.getFixedRate(), acctInterestConfigRange.getFixedRate()) && isEqualTo(cash.getAmortDayCount(),acctInterestConfig.getDaycount())) {
                                        contractMatch=true;
                                    };
                                }
                                else {
                                    if(!acctInterestConfigRange.isFixed() && isEqualTo(cash.getAmortDayCount(),acctInterestConfig.getDaycount())
                                            && isEqualTo(cash.getRateIndex(), acctInterestConfigRange.getRateIndex())
                                            && isEqualTo(cash.getSpread(), acctInterestConfigRange.getSpread())) {

                                        contractMatch=true;
                                    }
                                }
                            }
                        }
                    }
                    if(!contractMatch)
                        itCollateralConfig.remove();
                }
            }

            // found one eligible margin call contract assign it.
            if(filteredEligibleMarginCallConfigs.size()==1) {
                return filteredEligibleMarginCallConfigs.get(0);
            }


            // no margin call contract found create a new one
            if(isFeeCashPool && filteredEligibleMarginCallConfigs.size()==0) {
                CollateralConfig newCollateralConfig = saveAsNewCollateralConfig(eligibleMarginCallConfigs, accountPerContract, cash, errors);
                return newCollateralConfig;
            }

            // for Fee non cash pool if we have multiple eligible contrat we use the head.
            if(!isFeeCashPool && filteredEligibleMarginCallConfigs.size()>1) {
                CollateralConfig headConfig = getHeadCollateralConfig(filteredEligibleMarginCallConfigs);
                if(headConfig!=null)
                    return headConfig;
            }

            // no margin call contract found raise exception
            if(filteredEligibleMarginCallConfigs.size()==0 ) {
                errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99001", "Margin Call Contract not found", 0L));
            }

            // More than one eligible margin call contract raise exception.
            if(filteredEligibleMarginCallConfigs.size()>1) {
                errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "999003", "More than one eligible contract = " + eligibleMarginCallConfigs, 0L));
            }

        }

        return null;

    }

    private boolean isEqualTo(Object o1, Object o2) {
        if(o1==null)
            return o2==null;

        return o1.equals(o2);
    }

    protected void setKeywords() {
        removeKeyword(MurexSecLendingMapper.RATE_AMENDMENT_KW);
        trade.addKeyword(SDISelectorUtil.CHECK_BOOK, "false");
        trade.addKeyword(KW_WORKFLOW_SUBTYPE, KW_SEC_LENDING_FEE_POOL_VALUE);
        addInternalKeyword();
        addSlMigKeyword();
        addCollateralExclude();

        trade.addKeyword(CollateralStaticAttributes.CONTRACT_TYPE, CONTRACT_TYPE_OSLA);
        addFOSystemKeyword();
        addMexicoKeywords();
    }

    private void addMexicoKeywords(){
        boolean isDUMexicoSource = Optional.ofNullable(trade).filter(t -> "Mexico".equalsIgnoreCase(t.getKeywordValue("DataUploaderSource"))).isPresent();
        if(isDUMexicoSource){
            trade.addKeyword("FO_SYSTEM", "PRESTVAL");
            trade.addKeyword("CONTRACT_TYPE", "OSLA");
            trade.addKeyword(KW_WORKFLOW_SUBTYPE, "MEX_Workflow");
        }
    }

    private void addFOSystemKeyword() {
        SecLending sl = (SecLending)trade.getProduct();
        Vector collats = sl.getCollaterals();
        if (collats != null && collats.size() > 0) {
            Collateral col = (Collateral)collats.get(0);
            if (col.getSecurity() != null) {
                if (col.getSecurity() instanceof Equity) {
                    trade.addKeyword(TradeInterfaceUtils.TRADE_KWD_FO_SYSTEM, FO_SYSTEM_RV);
                }
                else {
                    trade.addKeyword(TradeInterfaceUtils.TRADE_KWD_FO_SYSTEM, FO_SYSTEM_RF);
                }
            }
        }
    }

    @Override
    public void upload(CalypsoObject object, Vector<BOException> errors, Object dbCon, boolean saveToDB1) {

        String previousMarginCallConfig = null;
        String previousMarginCallConfigContractType = null;
        String previousDeliveryType = null;
        if(trade!=null) {
            previousMarginCallConfig=trade.getKeywordValue("MARGIN_CALL_CONFIG_ID");
            previousMarginCallConfigContractType=trade.getKeywordValue(CollateralStaticAttributes.CONTRACT_TYPE);
            SecLending secLending = (SecLending) trade.getProduct();
            if (secLending != null) {
                previousDeliveryType = secLending.getDeliveryType();
            }
        }

        if (!calypsoTrade.getAction().equalsIgnoreCase(Action.S_NEW)) {
            validateStartDate(trade, errors);
        }

        super.upload(object, errors, dbCon, saveToDB1);

        //FORZAMOS SEC PASS THROUGH A TRUE, BORRAR CUANDO CALYPSO LO ARREGLE
        if (calypsoTrade.getAction().equalsIgnoreCase(Action.S_NEW)){
            forceSecPassThroughTrue(trade, errors);
        }

        SecLending sl = (SecLending)trade.getProduct();

        CollateralConfig collateralConfig = null;
        int CollateralConfigId = 0 ;

        if(!isCollateralExclude()){
            if(!isInternalDeal() && (Util.isEmpty(errors) || GatewayUtil.checkIfExceptionsAreWarnings(errors))) {
                if (reassignContract()) {
                    try {
                        collateralConfig = getMarginCallConfig(this.trade, errors);
                        if(collateralConfig!=null) {
                            setCollateralContractInfo(trade,sl,collateralConfig);
                        }
                    } catch (RemoteException e) {
                        errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99004", e.getMessage(), 0L));
                    }
                }
                else {
                    if(previousMarginCallConfig!=null) {
                        CollateralConfigId = Integer.parseInt(previousMarginCallConfig);
                        setCollateralContractInfo(trade,sl,CollateralConfigId,previousMarginCallConfigContractType);
                    }

                }
            }
        }

        Product newProduct = calypsoTrade.getProduct();
        com.calypso.tk.upload.jaxb.SecLending newSL = newProduct.getSecLending();
        if (!trade.getAction().equals(Action.valueOf("UNDO_TERMINATE"))) {
            sl.setMaturityType(newSL.getGeneralDetails().getTerminationType());
        }else{
            Optional<EventTypeAction> terminateAction = sl.getEventTypeActions().stream()
                    .filter(s-> SBL.getReturnActionTypes().contains(s.getActionType()))
                    .max(Comparator.comparing(EventTypeAction::getTradeVersion));
            terminateAction.ifPresent(eventTypeAction -> sl.addCancellation(eventTypeAction, trade));
        }

        if (isEvergreenPdv() && newSL.getGeneralDetails().getTerminationType().equalsIgnoreCase("OPEN")) {
            sl.setMaturityDate(null);
        } else {
            sl.setMaturityDate(getEndDate(calypsoTrade));
        }

        Collateral col = (Collateral)sl.getCollaterals().get(0);
        col.setTradeDate(trade.getTradeDate().getJDate(trade.getBook() == null ? null : trade.getBook().getLocation()));
        col.setStartDate(sl.getStartDate());
        col.setEndDate(sl.getEndDate());
        if (Action.S_AMEND.equalsIgnoreCase(calypsoTrade.getAction()) && "DAP".equals(previousDeliveryType) && "DFP".equals(newSL.getDeliveryType())) {
            sl.setInitialMarginValue(0.0D);
        }

        removeTimeToTradeDate ();
        setKeywords();
        updateDivRqmt();
    }

    private void addInternalKeyword() {
        if (Optional.ofNullable(trade.getMirrorBook()).isPresent()){
            trade.addKeyword(KW_SECLENDING_TRADE, INTERNA_STR);
        }
    }
    public void removeTimeToTradeDate () {
        Calendar cal = Calendar.getInstance(trade.getBook().getLocation());
        cal.setTime(trade.getTradeDate());

        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        JDatetime newTradeDate = new JDatetime(JDate.valueOf(cal),trade.getBook().getLocation());
        newTradeDate.setTime(cal.getTimeInMillis());

        trade.setTradeDate(newTradeDate);

    }

    public boolean reassignContract() {
        return reassignContract(calypsoTrade);
    }

    public static boolean reassignContract(CalypsoTrade trade) {
        return isNew(trade) || isCancelAndReissue(trade) || isRestructure(trade) || isRateAmendment(trade);
    }

    /**
     * CollateralConfig related attributes are set
     * @param trade
     * @param secLending
     * @param collateralConfig
     */
    private void setCollateralContractInfo(Trade trade,SecLending secLending,CollateralConfig collateralConfig){
        setCollateralContractInfo(trade, secLending, collateralConfig.getId(), collateralConfig.getContractType());
    }

    /**
     * CollateralConfig related attributes are set
     * @param trade
     * @param secLending
     * @param collateralConfigId
     */
    private void setCollateralContractInfo(Trade trade,SecLending secLending,int collateralConfigId, String collateralConfigContractType){
        secLending.setMarginCallContractId(trade, collateralConfigId);
        trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER,collateralConfigId);
        trade.addKeyword(CollateralStaticAttributes.CONTRACT_TYPE,collateralConfigContractType);
        trade.setInternalReference(String.valueOf(collateralConfigId));
    }

    protected static BigDecimal getDoubleAsBigDecimal(Double d) {
        return new BigDecimal(new Double(d).toString());
    }


    protected static String getInterestConfigNewName(Cash cash) {
        String index = "";
        if(cash.getRateIndex()!=null) {
            index = cash.getRateIndex().getName();
            index += cash.getRateIndex().getTenor();
        }

        String spread = "";

        if(cash.getFixedRateB()) {

            if(cash.getFixedRate()>=0)
                spread = "+" + getDoubleAsBigDecimal(cash.getFixedRate()).multiply(new BigDecimal(100)).stripTrailingZeros();
            else
                spread = "" + getDoubleAsBigDecimal(cash.getFixedRate()).multiply(new BigDecimal(100)).stripTrailingZeros();


        }
        else {

            BigDecimal bpSpread = getDoubleAsBigDecimal(cash.getSpread()).multiply(new BigDecimal(10000));

            if(bpSpread.compareTo(new BigDecimal(0))>=0)
                spread = "+"+bpSpread.stripTrailingZeros().toPlainString();
            else
                spread = ""+bpSpread.stripTrailingZeros().toPlainString();

        }

        String newName = cash.getCurrency()+index+spread;


        return newName;
    }

    /**
     * generate contract new name (format OSLA-BSTE-MIPM(USD-FED0D+30))
     * add index tenor to avoid duplicate account name.
     * @param templateCollateralConfig
     * @param cash
     * @return
     */
    protected static String getAccountNewName(CollateralConfig templateCollateralConfig, Cash cash) {
        return getContractNewName(templateCollateralConfig, cash, true);
    }

    protected static String getContractNewName(CollateralConfig templateCollateralConfig, Cash cash) {
        return getContractNewName(templateCollateralConfig, cash, false);
    }

    /**
     * generate contract new name (format OSLA-BSTE-MIPM(USD-FED+30))
     * @param templateCollateralConfig
     * @param cash
     * @return
     */
    protected static String getContractNewName(CollateralConfig templateCollateralConfig, Cash cash, boolean addTenor) {
        String index = "";
        if(cash.getRateIndex()!=null) {
            index = "-"+cash.getRateIndex().getName().substring(0,3);
            if(addTenor)
                index+=cash.getRateIndex().getTenor();
        }



        String spread = "";

        if(cash.getFixedRateB()) {

            if(cash.getFixedRate()>=0)
                spread = "+" + getDoubleAsBigDecimal(cash.getFixedRate()).multiply(new BigDecimal(100)).stripTrailingZeros();
            else
                spread = "" + getDoubleAsBigDecimal(cash.getFixedRate()).multiply(new BigDecimal(100)).stripTrailingZeros();

        }
        else {

            BigDecimal bpSpread = getDoubleAsBigDecimal(cash.getSpread()).multiply(new BigDecimal(10000));

            if(bpSpread.compareTo(new BigDecimal(0))>=0)
                spread = "+"+bpSpread.stripTrailingZeros().toPlainString();
            else
                spread = ""+bpSpread.stripTrailingZeros().toPlainString();

        }


        String contractType = templateCollateralConfig.getContractType();
        if(contractType.length()>4) {
            contractType = contractType.substring(0,4);
        }

        String po = templateCollateralConfig.getProcessingOrg().getCode();
        if(po.length()>4) {
            po = po.substring(0,4);
        }
        String le = templateCollateralConfig.getLegalEntity().getCode();
        if(le.length()>4) {
            le = le.substring(0,4);
        }

        String contractNewName = contractType+"-"+po+"-"+le+"("+cash.getCurrency()+index+spread+")";


        return contractNewName;
    }


    public CollateralConfig getHeadCollateralConfig(ArrayList<CollateralConfig> configs) {
        for(CollateralConfig config : configs) {
            String headClone = config.getAdditionalField("HEAD_CLONE");
            if(headClone!= null && headClone.equals("HEAD")) {
                return config;
            }
        }

        return null;
    }

    /**
     * create new collateral config based on template collateral config.
     * @param configs
     * @param accountPerContract
     * @param cash
     * @return
     * @throws CollateralServiceException
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    protected CollateralConfig saveAsNewCollateralConfig(ArrayList<CollateralConfig> configs, HashMap<Integer,Vector<Account>> accountPerContract,Cash cash, Vector<BOException> errors) throws CollateralServiceException {
        if(configs==null || configs.size()==0)
            return null;
        CollateralConfig headCollateralConfig = getHeadCollateralConfig(configs);
        if(headCollateralConfig==null) {
            errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99005", "Can't find head contract", 0L));
            return null;
        }

        Vector<Account> headAccounts = accountPerContract.get(headCollateralConfig.getId());
        if(headAccounts==null)
            try {
                headAccounts = DSConnection.getDefault().getRemoteAccounting().getAccountByAttribute("MARGIN_CALL_CONTRACT", ""+headCollateralConfig.getId());
            } catch (CalypsoServiceException e) {
                Log.error(LOG_CATEGORY, e);
                errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99007", e.getMessage(), 0L));
                return null;
            }

        if(headAccounts==null || headAccounts.size()==0) {
            Log.error(LOG_CATEGORY, "No account found on head contract : " + headCollateralConfig.getId());
            errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99008", "No account found on head contract : " + headCollateralConfig.getId(), 0L));

        }
        else {
            CollateralConfig newCollateralConfig=null;
            try {
                newCollateralConfig = headCollateralConfig.clone();
            } catch (CloneNotSupportedException e) {
                Log.error(LOG_CATEGORY, e);
                errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99006", e.getMessage(), 0L));
                return null;
            }
            String contractNewName = getContractNewName(newCollateralConfig,cash);
            newCollateralConfig.setName(contractNewName);
            newCollateralConfig.setAdditionalField("HEAD_CLONE", null);
            newCollateralConfig.setAdditionalField("AUTO_SL_CONTRACT", "true");
            newCollateralConfig.setId(0);
            newCollateralConfig.setProdStaticDataFilterName(null);
            if(headCollateralConfig.getContractType().equals(CollateralConfig.SUBTYPE_FACADE)) {
                newCollateralConfig.setParentId(headCollateralConfig.getId());
            }
            int id = ServiceRegistry.getDefault().getCollateralDataServer().save(newCollateralConfig);
            newCollateralConfig.setLongId(id);
            String accountNewName = getAccountNewName(newCollateralConfig, cash);
            createCallAccount(accountNewName, trade,headAccounts.get(0),id, errors);
            return newCollateralConfig;
        }

        return null;
    }

    /**
     * create new call account from template acct
     * change account name and interest config
     * @param accountName
     * @param trade
     * @param templateAcct
     * @param collateralConfigId
     * @return
     */

    @SuppressWarnings("unchecked")
    public Integer createCallAccount(String accountName, Trade trade, Account templateAcct, int collateralConfigId, Vector<BOException> errors) {

        SecLending secLending = (SecLending)trade.getProduct();

        Cash cash = secLending.getNewVMPool();

        if(cash != null ) {

            // Create account from template Account

            final Account account = (Account)templateAcct.clone();
            account.setId(0);
            account.setCreationDate(new JDatetime());
            // set the account name and description
            account.setName(accountName);
            account.setDescription(accountName);
            account.setExternalName(accountName);
            account.setProcessingOrgId(trade.getBook().getProcessingOrgBasedId());
            account.setCurrency(cash.getCurrency());

            // counterparty and role
            account.setLegalEntityId(trade.getCounterParty().getId());
            account.setLegalEntityRole(LegalEntity.CLIENT);
            // other options
            account.setAccountStatus("Active");
            account.setUser(DSConnection.getDefault().getUser());
            account.setAccountProperty(MARGIN_CALL_CONTRACT, ""+collateralConfigId);

            account.setVersion(0);

            // duplicate account statement config
            Vector<AccountStatementConfig> acctStatementCfg = new Vector<AccountStatementConfig>();
            for(AccountStatementConfig templateStatementCfg : templateAcct.getStatementConfigs()) {
                AccountStatementConfig statementConfig = (AccountStatementConfig)templateStatementCfg.clone();
                statementConfig.setId(0);
                acctStatementCfg.add(statementConfig);
            }
            account.setStatementConfigs(acctStatementCfg);

            //Create account interests
            Vector<AccountInterests> accountInterestsVector = new Vector<AccountInterests>();
            AccountInterests accountInterests = new AccountInterests();

            accountInterests.setActiveFrom(JDate.getNow());
            accountInterests.setCalculationType("Interest");
            accountInterestsVector.add(accountInterests);


            try {
                Integer acctInterestConfigId = null ;
                String accountInterestConfigName = getInterestConfigNewName(cash);
                AccountInterestConfig foundInterestConfig = DSConnection.getDefault().getRemoteAccounting().getAccountInterestConfig(accountInterestConfigName);

                if(foundInterestConfig!=null) {
                    acctInterestConfigId=foundInterestConfig.getId();
                }
                else {
                    AccountInterestConfig templateAcctInterestConfig = BOCache.getAccountInterestConfig(DSConnection.getDefault(), templateAcct.getAccountInterests().get(0).getConfigId());

                    //Create account interest config

                    AccountInterestConfig acctInterestConfig = (AccountInterestConfig)templateAcctInterestConfig.clone();
                    AccountInterestConfigRange acctInterestConfigRange = (AccountInterestConfigRange)((AccountInterestConfigRange)templateAcctInterestConfig.getRanges().get(0)).clone();

                    acctInterestConfigRange.setIsFixed(cash.getFixedRateB());
                    acctInterestConfigRange.setFixedRate(cash.getFixedRate());
                    acctInterestConfigRange.setAccountCurrency(cash.getCurrency());
                    acctInterestConfigRange.setRateIndex(cash.getRateIndex());
                    acctInterestConfigRange.setSpread(cash.getSpread());

                    acctInterestConfig.getRanges().clear();
                    acctInterestConfig.getRanges().add(acctInterestConfigRange);
                    acctInterestConfig.setName(accountInterestConfigName);
                    acctInterestConfig.setDaycount(cash.getAmortDayCount());

                    acctInterestConfigId = DSConnection.getDefault().getRemoteAccounting().save(acctInterestConfig);
                    acctInterestConfig.setId(acctInterestConfigId);

                }

                accountInterests.setConfigId(acctInterestConfigId);
                account.setAccountInterests(accountInterestsVector);

                AccountUtil actUtil = new AccountUtil();

                return actUtil.saveCallAccount(account, trade.getCounterParty().getId(), (Action)null, true, (TradeArray)null, (Trade)null, "CREATE", (String)null, 0.0D, (JDate)null, JDate.getNow(), (Task)null, (AccountInterestConfig)null, (AccountInterestConfig)null, true, true, DSConnection.getDefault(), DSConnection.getDefault().getUser());

            } catch (CalypsoServiceException e) {
                Log.error(LOG_CATEGORY, e);
                errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99010", e.getMessage(), 0L));
            } catch (RemoteException e) {
                Log.error(LOG_CATEGORY, e);
                errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99011", e.getMessage(), 0L));
            } catch (CalypsoException e) {
                Log.error(LOG_CATEGORY, e);
                errors.add(ErrorExceptionUtils.createWarning("21001", "Margin Call Contract", "99012", e.getMessage(), 0L));
            }

        }
        return -1;
    }

    public boolean isNew() {
        return isNew(calypsoTrade);
    }

    public static boolean isNew(CalypsoTrade trade) {
        return trade.getAction().equals(Action.S_NEW);
    }

    public  boolean isRestructure() {
        return isRestructure(calypsoTrade);
    }

    public static boolean isRestructure(CalypsoTrade trade) {
        String mxLastEvent = getKeyWordValue(trade, KW_MX_LAST_EVENT);
        if(mxLastEvent!=null
                && mxLastEvent.equals(KW_MX_LAST_EVENT_RESTRUCTURE))  {
            return true;
        }
        return false;
    }

    public boolean isCancelAndReissue() {

        return isCancelAndReissue(calypsoTrade);

    }

    public boolean isRateAmendment() {
        return isRateAmendment(calypsoTrade);
    }

    public static boolean isRateAmendment(CalypsoTrade trade) {
        String rateAmendment = getKeyWordValue(trade, MurexSecLendingMapper.RATE_AMENDMENT_KW);
        return Util.isTrue(rateAmendment);
    }


    public static boolean isCancelAndReissue(CalypsoTrade trade) {
        String mxLastEvent = getKeyWordValue(trade, KW_MX_LAST_EVENT);
        if(mxLastEvent!=null
                && mxLastEvent.equals(KW_MX_LAST_EVENT_CANCEL_REISSUE))  {
            return true;
        }
        return false;
    }


    public static String getKeyWordValue(CalypsoTrade trade, String keywordName) {
        if(trade.getTradeKeywords()!=null) {
            for(Keyword keyword : trade.getTradeKeywords().getKeyword()) {
                if(keyword.getKeywordName().equals(keywordName)) {
                    return keyword.getKeywordValue();
                }
            }
        }
        return null;

    }

    public boolean isInternalDeal() {
        LegalEntity le = trade.getCounterParty();
        if(le==null)
            return false;
        if (le.equals(trade.getBook().getLegalEntity()))
            return true;
        else
            return trade.getMirrorBook() != null
                    && trade.getMirrorBook().getLegalEntity().equals(le);
    }

    public void removeKeyword(String keywordName) {
        if(calypsoTrade.getTradeKeywords()!=null) {
            ListIterator<Keyword> keywordIt = calypsoTrade.getTradeKeywords().getKeyword().listIterator();

            while(keywordIt.hasNext()) {
                Keyword kw = keywordIt.next();
                if(kw.getKeywordName().equals(keywordName)) {
                    keywordIt.remove();
                }
            }
        }
    }

    /**
     * Add migration date
     */
    private void addSlMigKeyword(){
        if((isSlMigActivated() || isInSlMigFilter())) {
            Optional.ofNullable(getSlMigDate()).ifPresent(slMigDate -> {
                trade.addKeyword("SL_MIG", slMigDate);
            });
        }
    }

    private String getSlMigDate(){
        Vector<String> sl_mig_activation = null;
        try {
            sl_mig_activation = DSConnection.getDefault().getRemoteReferenceData().getDomainValues("SL_MIG_DATE");
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading SL_MIG_DATE. " + e);
        }
        return !Util.isEmpty(sl_mig_activation) ? sl_mig_activation.get(0) : "";
    }

    private boolean isSlMigActivated(){
        try {
            Vector<String> sl_mig_activation = DSConnection.getDefault().getRemoteReferenceData().getDomainValues("SL_MIG_ACTIVATION");
            return !Util.isEmpty(sl_mig_activation) && "true".equalsIgnoreCase(sl_mig_activation.get(0));
        } catch (CalypsoServiceException e){
            Log.error(this,"Error loading SL_MIG_ACTIVATION. " + e);
        }
        return false;
    }

    private boolean isInSlMigFilter(){
        try {
            StaticDataFilter sl_mig = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter("SL_MIG");
            if(null!=sl_mig){
                return sl_mig.accept(trade);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading SD Filter: " + e);
        }
        return false;
    }

    private void addCollateralExclude(){
        if(null!=trade && isCollateralExclude()){
            trade.addKeyword("CollateralExclude", "true");
        }
    }

    private boolean isCollateralExclude(){
        if(null!=trade){
            LegalEntity counterParty = trade.getCounterParty();
            if(null!=counterParty){
                Vector<LegalEntityAttribute> legalEntityAttributes = BOCache.getLegalEntityAttributes(DSConnection.getDefault(), counterParty.getId());
                if(!Util.isEmpty(legalEntityAttributes)){
                    String attributeValue = legalEntityAttributes.stream()
                            .filter(att -> "COLLATERAL_EXCLUDE".equalsIgnoreCase(att.getAttributeType()))
                            .map(LegalEntityAttribute::getAttributeValue).findFirst().orElse("");
                    return "true".equalsIgnoreCase(attributeValue);
                }
            }
        }
        return false;
    }

    private void updateDivRqmt(){
        String dividendNegociado = this.trade.getKeywordValue("DividendNegociado");
        if(!Util.isEmpty(dividendNegociado)){
            com.calypso.tk.core.Product product = this.trade.getProduct();
            Vector<Collateral> activeCollaterals = ((SecLending) product).getCollaterals();
            if(!Util.isEmpty(activeCollaterals)){
                activeCollaterals.get(0).setReturnPercentage(Double.parseDouble(dividendNegociado));
            }
        }
    }

    private void validateStartDate(Trade trade, Vector<BOException> errors) {
        JDate newStartDate = JDate.valueOf(calypsoTrade.getStartDate().toGregorianCalendar().getTime());
        SecLending sl = (SecLending) trade.getProduct();
        if (newStartDate != null && sl != null) {
            if (newStartDate.after(sl.getStartDate())) {
                Vector<EventTypeAction> slActions = sl.getEventTypeActions();
                if (slActions != null) {
                    slActions.stream().filter(s -> s.getActionType().equalsIgnoreCase("Fee Mark")
                            && s.getEffectiveDate().before(newStartDate)).forEach(s -> {
                        s.setIsCancelledB(true);
                    });
                }
            }
        }
    }

    private void forceSecPassThroughTrue(Trade trade, Vector<BOException> errors){
        SecLending sl = (SecLending) trade.getProduct();
        if (sl!=null){
            sl.setPassThrough(true);
        }
    }

    @Override
    public void setUndo(Trade trade) {
        trade.setAction(Action.valueOf(UploaderTradeUtil.getDefaultRejectAction((Object) null, this.calypsoTrade.getAction())));
        trade.setStatus(Status.S_VERIFIED);
        Action action = trade.getAction();

        try {
            JDatetime oldTime = trade.getUpdatedTime();
            int initialVersion = trade.getVersion();

            List<UploaderSQLBindVariable> bindVariablesList = new ArrayList();
            String w = "entity_id = " + DataUploaderUtil.valueToPreparedString(CalypsoIDAPIUtil.getId(trade), bindVariablesList);
            Vector v = UploaderSQLBindAPI.getAudit(w, " version_num DESC,modif_date DESC", bindVariablesList);

            List<AuditValue> auditValues = getLastTerminationAuditValues(v);
            auditValues.forEach(s -> {
                //Exclude the undo from Auditvalue when EventTypeAction is Full Return, Partial Return or Termination.
                if (!isEventTypeActionTerminateFromAudit(s)) {
                    trade.undo(DSConnection.getDefault(), s);
                }
            });
            trade.setUpdatedTime(oldTime);
            trade.setVersion(initialVersion);
            trade.setAction(action);
        } catch (Exception var14) {
            Log.error("TradeUtil", var14);
        }
    }

    private List<AuditValue> getLastTerminationAuditValues(Vector v) {
        int version = 0;
        List<AuditValue> listtest = Collections.list(v.elements());
        for (AuditValue av : listtest) {
            if (isLastTermination(av) > 0 && version < isLastTermination(av)) {
                version = isLastTermination(av);
            }
        }
        final int lastVersion = version;
        listtest = listtest.stream().filter(s -> s.getVersion() == lastVersion).collect(Collectors.toList());

        return listtest;
    }

    private int isLastTermination(AuditValue av) {

        if (av.getFieldName().equalsIgnoreCase("_status")
                && av.getField().getOldValue().equalsIgnoreCase("VERIFIED")
                && av.getField().getNewValue().equalsIgnoreCase("TERMINATED")) {
            return av.getVersion();
        }
        return -1;
    }

    private boolean isEventTypeActionTerminateFromAudit(AuditValue auditValue) {
        if (!auditValue.getField().getType().contains("EventTypeAction")) {
            return false;
        }
        if ((auditValue.getField().getType().contains("EventTypeAction")
                && (!auditValue.getField().getNewValue().contains("Termination")
                && !auditValue.getField().getNewValue().contains("Full Return")
                && !auditValue.getField().getNewValue().contains("Partial Return")))){
            return false;
        }
        return true;

    }

    private JDate getEndDate(CalypsoTrade calypsoTrade) {
        Date endDate = Optional.ofNullable(calypsoTrade.getMaturityDate())
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(GregorianCalendar::getTime).orElse(null);
        return null != endDate ? JDate.valueOf(endDate) : null;
    }

    private boolean isEvergreenPdv() {
        String maturityStructure = this.trade.getKeywordValue("MaturityStructure");
        return !Util.isEmpty(maturityStructure) && maturityStructure.equalsIgnoreCase("evergreen");
    }

}
