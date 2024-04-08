package calypsox.tk.csdr;

import com.calypso.analytics.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.product.factory.LegalEntityRoleEnum;
import com.calypso.tk.product.factory.TradeSimpleTransferFactory;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CSDRFiFlowTradeBuilder {

    List<CSDRFiFlowLineBean> csdrFileLines;

    String dummyCptyName="DUMMY_AGENT";

    Action actionToApply=Action.AMEND;

    JDate valueDate=JDate.getNow();

    public static final String SWIFTSEMETRADEKWD="OriginalFailedMsgIdFromFIFLOW";
    public static final String CLIENTREF="ClientRef";
    public static final String ORIGINALCPTYNAME="OriginalCounterpartyName";
    public static final String ORIGINALCPTYID="OriginalCounterpartyId";
    private static final String EXTXFERID="OriginalTransferIdFromFIFLOW";
    private static final String MXTRADEIDKWD="MurexTradeID";
    private static final String ORIGINALBOTRADEIDKWD="OriginalTradeIdFromFIFLOW";


    public CSDRFiFlowTradeBuilder(List<CSDRFiFlowLineBean> csdrFileLines, String dummyCptyName, JDate valueDate){
        this.csdrFileLines=csdrFileLines;
        if(!Util.isEmpty(dummyCptyName)){
            this.dummyCptyName=dummyCptyName;
        }
        if(valueDate!=null){
            this.valueDate=valueDate;
        }
    }

    /**
     * Duplicates are not being handled here. Input List<CSDRFiFlowLineBean> must be duplicate free.
     * @return Dummy trade array
     */
    public ExternalArray build(){
        List<Trade> trades=new ArrayList<>();
        for(CSDRFiFlowLineBean lineBean:csdrFileLines){
            try {
                Trade trade=buildTrade(lineBean);
                trades.add(trade);
            } catch (CalypsoServiceException exc) {
                Log.error(this,exc.getCause());
            }
        }
        return buildExternalArray(trades);
    }

    private Trade buildTrade(CSDRFiFlowLineBean lineBean) throws CalypsoServiceException {

        Trade trade=findExistingTradeForReference(lineBean);

        if(trade.getLongId()==0L) {
           buildNewTrade(trade,lineBean);
        }else{
            //trade.setSettleDate(JDate.getNow());
            trade.setAction(actionToApply);
            Log.debug(this,"Updating trade with id = "+trade.getLongId()+" for TRNREF = "+lineBean.getTrnMsg());
        }
        buildTradeKeywords(trade,lineBean);
        return trade;
    }

    private void buildNewTrade(Trade trade, CSDRFiFlowLineBean lineBean) throws CalypsoServiceException {
        TradeSimpleTransferFactory factory = new TradeSimpleTransferFactory();
        factory.fromTemplate(buildTemplateInfo(lineBean), trade, JDate.getNow().getJDatetime(), new ArrayList<>(), new ArrayList<>());

        trade.setTradeDate(valueDate.getJDatetime());
        trade.setSettleDate(valueDate);
        trade.setExternalReference(lineBean.getExtTradeId());
        trade.addKeyword(SWIFTSEMETRADEKWD, lineBean.getTrnMsg());
        trade.addKeyword(CLIENTREF, lineBean.getTrnMsg());
        trade.setAction(Action.NEW);

        trade.setAllocatedLongSeed(DSConnection.getDefault().getRemoteAccess().allocateLongSeed("trade",1));
        //trade.addKeyword("PenaltyTradeId", String.valueOf(trade.getAllocatedLongSeed()));

        Log.debug(this,"Creating new trade for TRNREF = "+lineBean.getTrnMsg());
    }
    private void buildTradeKeywords(Trade trade, CSDRFiFlowLineBean lineBean){
        trade.addKeyword(EXTXFERID, lineBean.getExtXferId());
        trade.addKeyword(ORIGINALCPTYNAME,lineBean.getGlcs());
        trade.addKeyword(ORIGINALCPTYID,buildLeId(lineBean));
        trade.addKeyword(MXTRADEIDKWD,lineBean.getFoTradeId());
        trade.addKeyword(ORIGINALBOTRADEIDKWD,lineBean.getExtTradeId());
    }
    private TemplateInfo buildTemplateInfo(CSDRFiFlowLineBean lineBean){
        Book book=buildBook(lineBean);
        SimpleTransfer simpleTransfer=buildSimpleTransferProduct(book);

        TemplateInfo template=new TemplateInfo();
        template.setDate(JDate.getNow());
        template.setProduct(simpleTransfer);
        template.setProductType(simpleTransfer.getClass().getSimpleName());
        template.setBookId(Optional.ofNullable(book).map(Book::getId).orElse(0));
        template.setRole(LegalEntityRoleEnum.Agent.getName());
        template.setTraderName("");
        template.setSalesName("");
        template.setQuantity(1);
        template.setTradeCurrency("EUR");
        template.setSettleCurrency("EUR");
        template.setUser("calypso_user");
        template.setSettleDate(JDate.getNow());
        template.setCptyId(Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(),"DUMMY_AGENT"))
                .map(LegalEntity::getId).orElse(0));

        return  template;
    }

    private SimpleTransfer buildSimpleTransferProduct(Book book){
        SimpleTransfer simpleTransfer=new SimpleTransfer();
        simpleTransfer.setFlowType("PRINCIPAL");
        simpleTransfer.setOrdererRole(LegalEntityRoleEnum.ProcessingOrg.getName());
        simpleTransfer.setOrdererLeId(Optional.ofNullable(book).map(Book::getLegalEntity).map(LegalEntity::getId).orElse(0));
        simpleTransfer.setCurrencyCash("EUR");
        simpleTransfer.setPrincipal(1.0d);
        return simpleTransfer;
    }

    private Trade findExistingTradeForReference(CSDRFiFlowLineBean lineBean){
        Trade trade=new Trade();
        try {
            TradeArray trades=DSConnection.getDefault().getRemoteTrade().getTradesByKeywordNameAndValue(SWIFTSEMETRADEKWD,lineBean.getTrnMsg());
            if(!trades.isEmpty()){
                for(Trade arrTrade:trades.getTrades()){
                    if(isTargetDummyTrade(arrTrade)
                            &&TradeWorkflow.isTradeActionApplicable(arrTrade, actionToApply, DSConnection.getDefault(), null)){
                        trade=arrTrade;
                        break;
                    }
                }
            }
        } catch (CalypsoServiceException exc) {
            Log.error(this,exc.getCause());
        }
        return trade;
    }

    private Book buildBook(CSDRFiFlowLineBean lineBean){
        String bookName=lineBean.getBook();
        Book book= BOCache.getBook(DSConnection.getDefault(),bookName);
        if(book==null){
            Log.debug(this,"Could find book with name: "+bookName+" setting DUMMY_BOOK instead");
            book= BOCache.getBook(DSConnection.getDefault(),"DUMMY_BOOK");
        }
        return book;
    }

    private String buildLeId(CSDRFiFlowLineBean lineBean){
        String glcs=lineBean.getGlcs();
        return Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(),glcs))
                .map(LegalEntity::getId).map(String::valueOf).orElse("");
    }
    private ExternalArray buildExternalArray(List<Trade> trades){
        ExternalArray array=null;
        if(!Util.isEmpty(trades)){
            try {
                array=new ExternalArray(trades);
            } catch (InvalidClassException exc) {
                Log.error(this,exc.getCause());
            }
        }
        return array;
    }

    public static boolean isTargetDummyTrade(Trade trade){
        boolean isNotPenalty=Optional.ofNullable(trade)
                .map(Trade::getProduct).filter(p->p instanceof SimpleTransfer)
                .map(p-> !"PENALTY".equals(((SimpleTransfer) p).getFlowType()))
                .orElse(false);

        boolean isFIFLOWTrade=Optional.ofNullable(trade)
                .map(t->t.getKeywordValue(SWIFTSEMETRADEKWD))
                .map(kwd->!Util.isEmpty(kwd))
                .orElse(false);

        boolean isNotCanceled=Optional.ofNullable(trade).map(t -> !Status.CANCELED.equals(trade.getStatus().getStatus()))
                .orElse(false);

        return isNotPenalty&&isFIFLOWTrade&&isNotCanceled;
    }
}
