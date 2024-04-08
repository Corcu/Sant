package calypsox.tk.report;

import calypsox.tk.pledge.util.TripartyPledgeProrateCalculator;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.product.Repo;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class RepoContingenciaReport extends TradeReport {

    public static final String SLB = "1111";
    public static final String MADRID = "1999";
    public static final String FATHER_REPO = "FATHER_REPO";

    ConcurrentHashMap<Long, Trade> listOfRepos = new ConcurrentHashMap<Long, Trade>();

    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
        ConcurrentLinkedQueue<ReportRow> finalRows = new ConcurrentLinkedQueue<>();
        if(output==null)
            return null;
        TripartyPledgeProrateCalculator calculator = new TripartyPledgeProrateCalculator();

        ReportRow[] rows = output.getRows();
        createRepoCache(rows);

        for(int i=0; i<rows.length; i++) {
            ReportRow row = rows[i];

            Trade trade = row.getProperty(ReportRow.TRADE);
            if(trade.getProduct() instanceof Pledge){
                if(validPledge((Pledge)trade.getProduct())){
                    long repoId = parseInternalRef(trade.getInternalReference());
                    Trade fatherRepo = null;
                    if(listOfRepos.containsKey(repoId)){
                        fatherRepo = listOfRepos.get(repoId);
                    }else {
                        fatherRepo = loadFatherRepo(repoId);
                    }
                    if(fatherRepo!=null){
                        trade.addKeyword("MurexTradeID",fatherRepo.getKeywordValue("MurexTradeID"));
                        row.setProperty(FATHER_REPO,fatherRepo);
                    }

                    calculator.calculate(trade,fatherRepo,row);
                    finalRows.add(row);
                }
            }else if (checkInternalCntrContable(trade) && trade.getProduct() instanceof Repo) {
                if(validRepo(trade)){
                    finalRows.add(row);
                }
            }
        }
        final ReportRow[] finalReportRows = finalRows.toArray(new ReportRow[finalRows.size()]);
        output.setRows(finalReportRows);
        return output;
    }

    private boolean validPledge(Pledge pledge){
        if(null!=pledge){
            final JDate pledgeStartDate = pledge.getStartDate();
            return null!=pledgeStartDate && getValDate().gte(pledgeStartDate);
        }
        return false;
    }

    private boolean validRepo(Trade trade){
        if(null!=trade && null!=trade.getProduct() && trade.getProduct() instanceof Repo){
            Repo repo = (Repo) trade.getProduct();
            String isGCPoolingKW = trade.getKeywordValue("isGCPooling");
            final JDate repoStartDate = repo.getStartDate();
            if( Repo.SUBTYPE_TRIPARTY.equalsIgnoreCase(repo.getSubType())
                    && !"true".equalsIgnoreCase(isGCPoolingKW)
                    && null!=repoStartDate ){
                return getValDate().before(repoStartDate);
            }
            return true;
        }
        return false;
    }

    private Trade loadFatherRepo(long id){
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrade(id);
        } catch (CalypsoServiceException e) {
            Log.error(this,e);
        }
        return null;
    }

    private Long parseInternalRef(String internalRef){
        if(!Util.isEmpty(internalRef)){
            try {
                return Long.parseLong(internalRef);
            }catch (Exception e){
                Log.error(this,e);
            }
        }
        return 0L;
    }

    private void createRepoCache(ReportRow[] rows){
        listOfRepos.putAll(Arrays.stream(rows).parallel()
                .map(row -> (Trade) row.getProperty(ReportRow.TRADE))
                .filter(trade -> trade.getProduct() instanceof Repo)
                .collect(Collectors.toMap(Trade::getLongId, trade -> trade)));

    }

    private boolean checkInternalCntrContable(Trade trade) {
        try {
            Trade tradeMirrorBook = DSConnection.getDefault().getRemoteTrade().getTrade(trade.getMirrorTradeId());

            String partenonTrade = null != trade ? trade.getKeywordValue("PartenonAccountingID") : "";
            String partenonMirrorTrade = null != tradeMirrorBook ? tradeMirrorBook.getKeywordValue("PartenonAccountingID") : "";

            String codCentroTrade = getCodigoCentro(trade, partenonTrade);
            String codCentroTradeMirror = getCodigoCentro(trade, partenonMirrorTrade);

            if (trade.getMirrorTradeId() > 0){
                return (validateIstreasury(codCentroTrade) || validateIstreasury(codCentroTradeMirror)) ?
                        (validateIstreasury(codCentroTrade) && validateIstreasury(codCentroTradeMirror)) ? false : true: false;
            }
            return true;
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error: " + e);
        }
        return true;
    }

    private String getCodigoCentro(Trade trade, String keyword) {
        return checkString(keyword, 4, 8);
    }

    private String checkString(String value, int init, int fin) {
        if (value != null && value.length() >= fin) {
            return Optional.ofNullable(value.substring(init, fin)).orElse("");
        } else {
            return "";
        }
    }

    private boolean validateIstreasury(String value) {
        return (SLB.equalsIgnoreCase(value) || MADRID.equalsIgnoreCase(value)) ? true : false;
    }

}
