package calypsox.tk.util;

import calypsox.tk.bo.cremapping.BOCreMappingFactory;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.paging.BOArrayBackedStream;
import com.calypso.tk.bo.paging.BOStream;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DataServer;
import com.calypso.tk.transaction.TransactionException;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.ScheduledTaskCRE_SENDER;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author acd
 */
public class ScheduledTaskSANT_CRE_SENDER extends ScheduledTaskCRE_SENDER {

    static final String LOG_CATEGORY = ScheduledTaskSANT_CRE_SENDER.class.getSimpleName();
    private BOCreMappingFactory creMappingFactory = BOCreMappingFactory.getFactory();
    private BOCreUtils boCreUtils = BOCreUtils.getInstance();

    protected void processCres(DSConnection ds, BOStream<BOCre> cres, AtomicInteger count, StringBuffer message, AtomicBoolean error) {
        TreeMap<Long, ArrayList<BOCre>> mapCres = new TreeMap<>();
        int numberOfCres = 0;

        BOCre cre;
        for(Iterator var8 = cres.iterator(); var8.hasNext(); ++numberOfCres) {
            cre = (BOCre)var8.next();
            long id = cre.getLinkedId() > 0L ? cre.getLinkedId() : cre.getId();
            if (!mapCres.containsKey(id)) {
                mapCres.put(id, new ArrayList());
            }

            ((ArrayList)mapCres.get(id)).add(cre);
        }


        int threadCount = this.getThreadCount() < 1 ? 1 : this.getThreadCount();
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CompletionService<Integer> ecs = new ExecutorCompletionService(pool);
        int sequenceId = 0;

        for(Iterator var12 = mapCres.values().iterator(); var12.hasNext(); ++sequenceId) {
            ArrayList<BOCre> creList = (ArrayList)var12.next();
            Callable<Integer> callable = new ProcessCreCallable(creList, ds, count, message, error);
            ecs.submit(callable);
        }

        try {
            pool.shutdown();
            if (!pool.awaitTermination(1L, TimeUnit.DAYS)) {
                pool.shutdownNow();
                String msg = "Pool doesn't finish process Cres after 1 day.";
                message.append(msg);
                error.set(true);
                Log.error(LOG_CATEGORY, msg);
            }

            Integer result = 0;
            int jobsDone = 0;

            for(int i = 0; i < sequenceId; ++i) {
                Future<Integer> future = ecs.take();
                if (future.isDone()) {
                    ++jobsDone;
                    result = result + (Integer)future.get();
                }
            }

            Log.system(LOG_CATEGORY, "numberOfCres " + numberOfCres + ", jobs submitted " + sequenceId + " --> " + jobsDone + " jobs  executed with " + result + " valid cres and " + count.get() + " cres really sent.");
        } catch (ExecutionException var20) {
            message.append(var20.getMessage());
            error.set(true);
        } catch (InterruptedException var21) {
            message.append(var21.getMessage());
            error.set(true);
            Thread.currentThread().interrupt();
        } finally {
            if (pool != null) {
                pool.shutdownNow();
                cre = null;
            }

        }
    }

    protected static int processCre(BOCre cre, DSConnection ds) throws Exception{
        if (cre.getStatus().equals("NEW") && !isNotSent(cre)) {
            Log.debug(ScheduledTaskSANT_CRE_SENDER.class.getSimpleName(), " already sent");
            return 0;
        } else if (cre.getStatus().equals("DELETED") && cre.getSentStatus() != null && cre.getSentStatus().equals("DELETED")) {
            Log.debug(ScheduledTaskSANT_CRE_SENDER.class.getSimpleName(), " already deleted");
            return 0;
        } else if (cre.getStatus().equals("DELETED") && isNotSent(cre)) {
            cre.setSentStatus("DELETED");
            Log.debug(ScheduledTaskSANT_CRE_SENDER.class.getSimpleName(), " to deleted");
            CreArray creToSave = new CreArray();
            creToSave.add(cre);
            getReadWriteDS(ds).getRemoteBO().saveCres(0L, (String)null, creToSave, false);
            return 1;
        } else if (cre.getStatus().equals("DELETED") && cre.getSentStatus() != null && cre.getSentStatus().equals("RE_SENT")) {
            Log.debug(ScheduledTaskSANT_CRE_SENDER.class.getSimpleName(), " already re_sent");
            return 0;
        } else {
            /**
             * Generate PSEventCre for CreSenderEngine
             */
            JDatetime now = new JDatetime();
            cre.setSentDate(now);
            PSEventCre creEvent = new PSEventCre();
            creEvent.setBoCre(cre);
            //Publish event for CreOnlineSenderEngine
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(creEvent);
            return 1;
        }
    }

    static boolean isNotSent(BOCre cre) {
        return cre.getSentStatus() == null || cre.getSentStatus().trim().length() == 0 || cre.getSentStatus().equals("null");
    }

    protected String handleCreSender(DSConnection ds, PSConnection ps) {
        String onlyEvents = this.getAttribute("EVENTS");
        if(isSLFee(onlyEvents)){
            return handleSLFee(ds, ps);
        }else {
            return super.handleCreSender(ds, ps);
        }
    }

    private String handleSLFee(DSConnection ds, PSConnection ps){
        JDatetime valDatetime = this.getValuationDatetime();
        JDate valDate = JDate.valueOf(valDatetime, this._timeZone);
        valDate = this.getToDate(valDate);
        if (this.isIncludeNonBusinessDate()) {
            Holiday hol = Holiday.getCurrent();
            valDate = hol.nextBusinessDay(valDate, this._holidays);
            valDate = valDate.addDays(-1);
        }

        if (Log.isCategoryLogged(Log.OLD_TRACE)) {
            Log.debug(Log.OLD_TRACE, "ScheduledTask Cre Sender " + this.getId() + " ValDate: " + valDate + " Valuation Datetime " + valDatetime);
        }

        String message = null;
        BOStream<BOCre> cres = null;

        int count = 0;

        String sqlFromExtra = "trade, product_seclending";

        Vector v;
        boolean error = true;
        int retry;


        try {
            StringBuilder sqlWhereDate = new StringBuilder();
            sqlWhereDate.append(" bo_cre.trade_id = trade.trade_id AND product_seclending.product_id = trade.product_id");
            sqlWhereDate.append(" AND bo_cre.sent_status is null ");
            sqlWhereDate.append(" AND bo_cre.bo_cre_type LIKE 'SECLENDING_FEE' ");
            sqlWhereDate.append(" AND product_seclending.end_date < bo_cre.effective_date ");
            sqlWhereDate.append(" AND trunc(product_seclending.end_date) <= " + Util.date2SQLString(valDate));

            CreArray allCres = ds.getRemoteBO().getBOCres(sqlFromExtra, sqlWhereDate.toString(), null);

            StringBuilder sqlWhereDate1 = new StringBuilder();
            sqlWhereDate1.append(" bo_cre.trade_id = trade.trade_id AND product_seclending.product_id = trade.product_id");
            sqlWhereDate1.append(" AND bo_cre.sent_status is null ");
            sqlWhereDate1.append(" AND bo_cre.bo_cre_type LIKE 'SECLENDING_FEE' ");
            sqlWhereDate1.append(" AND product_seclending.end_date > bo_cre.effective_date ");
            sqlWhereDate1.append(" AND trunc(bo_cre.effective_date) <= " + Util.date2SQLString(valDate));

            CreArray tmpCres = ds.getRemoteBO().getBOCres(sqlFromExtra, sqlWhereDate1.toString(), null);

            allCres.add(tmpCres.toVector());

            try {
                allCres = (CreArray)allCres.clone();
            } catch (Exception var39) {
                Log.error(Log.OLD_TRACE, (String)null, var39);
            }

            cres = new BOArrayBackedStream(allCres);


            error = false;
        } catch (Exception var42) {
            Log.error(this, var42);
            message = var42.getMessage();
        }

        Iterator var53 = ((BOStream)cres).iterator();

        while(var53.hasNext()) {
            BOCre cre = (BOCre)var53.next();
            Log.debug(this, " creId = " + cre.getId());
            retry = 0;

            while(retry <= 1) {
                try {
                    count += this.processCre(cre, ds);
                    break;
                } catch (Exception var41) {
                    boolean lockingProblem = var41.getMessage().indexOf("has been processed") >= 0 || Util.getRootCauseOfType(var41, DBVersionMismatchException.class) != null || Util.getRootCauseOfType(var41, TransactionException.class) != null;
                    if (retry != 0 || !lockingProblem) {
                        Log.error(this, var41);
                        message = "Cre id " + cre.getId() + " not saved.";
                        error = true;
                        break;
                    }

                    try {
                        Log.info(this, "Retry processing Cre " + cre.getId());
                        cre = ds.getRemoteBO().getBOCre(cre.getId());
                        if (DataServer._isDataServer) {
                            cre = (BOCre)cre.clone();
                        }

                        ++retry;
                    } catch (Exception var40) {
                        Log.error(this, var40);
                        message = "Cre id " + cre.getId() + " not saved.";
                        error = true;
                        break;
                    }
                }
            }

            if (error) {
                break;
            }
        }

        try {
            ((BOStream)cres).close();
        } catch (RemoteException var38) {
            Log.error(this, var38);
            message = var38.toString();
            error = true;
        }

        if (count == 0) {
            Log.debug(this, "Nothing to process for CRE SENDER: " + this);
        } else {
            Log.debug(this, count + " Trade processed for CRE SENDER: " + this);
        }

        return error ? message : null;

    }


    private boolean isSLFee(String onlyEvents){
        return Util.string2Vector(onlyEvents).stream().anyMatch("SECLENDING_FEE"::equalsIgnoreCase);
    }


    public static class ProcessCreCallable implements Callable<Integer> {
        private List<BOCre> cres;
        private DSConnection ds;
        private AtomicInteger count;
        private StringBuffer message;
        private AtomicBoolean error;

        public ProcessCreCallable(List<BOCre> cres, DSConnection ds, AtomicInteger count, StringBuffer message, AtomicBoolean error) {
            this.cres = cres;
            this.ds = ds;
            this.count = count;
            this.message = message;
            this.error = error;
        }

        public Integer call() throws Exception {
            return processCresCallable(this.ds, this.cres, this.count, this.message, this.error);
        }

        public String getCres() {
            return this.cres.toString();
        }
    }

    protected static int processCresCallable(DSConnection ds, List<BOCre> cres, AtomicInteger count, StringBuffer message, AtomicBoolean error) {
        int result = 0;

        boolean b;
        for(Iterator var6 = cres.iterator(); var6.hasNext(); result += b ? 1 : 0) {
            BOCre cre = (BOCre)var6.next();
            b = processCre(ds, cre, count, message, error);
        }

        return result;
    }

    protected static boolean processCre(DSConnection ds, BOCre cre, AtomicInteger count, StringBuffer message, AtomicBoolean error) {
        Log.debug(LOG_CATEGORY, " creId = " + cre.getId());
        boolean ok = true;
        int retry = 0;

        while(retry <= 1) {
            try {
                count.addAndGet(processCre(cre, ds));
                break;
            } catch (Exception var11) {
                boolean lockingProblem = getLockingProblem(var11);
                if (retry != 0 || !lockingProblem) {
                    Log.error(LOG_CATEGORY, var11);
                    message.append("Cre id " + cre.getId() + " not saved.");
                    error.set(true);
                    ok = false;
                    break;
                }

                try {
                    Log.info(LOG_CATEGORY, "Retry processing Cre " + cre.getId());
                    cre = ds.getRemoteBO().getBOCre(cre.getId());
                    if (DataServer._isDataServer) {
                        cre = (BOCre)cre.clone();
                    }

                    ++retry;
                } catch (Exception var10) {
                    Log.error(LOG_CATEGORY, var10);
                    message.append("Cre id " + cre.getId() + " not saved.");
                    error.set(true);
                    ok = false;
                    break;
                }
            }
        }

        return ok;
    }

}
