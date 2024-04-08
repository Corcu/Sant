package calypsox.tk.report;

import calypsox.apps.reporting.OBBGenericReportTemplatePanel;
import calypsox.tk.bo.obb.OBBGenericBean;
import calypsox.util.OBBReportUtil;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.PostingReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.InstantiateUtil;

import java.util.*;
import java.util.stream.Collectors;
/**
 * @author acd
 *
 * For create new lines for other products need instanciate new class call "product"ObbBean
 * on package calypsox.tk.obb extending from @{@link OBBGenericBean}
 *
 */
public class OBBGenericReport extends PostingReport {

    private HashMap<Long,BOPosting> postingConversion = new HashMap<>();
    private HashMap<Long,BOPosting> postingMatureFxTranslationOnly = new HashMap<>();

    private static final String FX_CONVERSION = "FX_CONVERSION";
    private static final String FX_TRANSLATION = "FX_TRANSLATION";
    private static final String FX_REVALUATION = "FX_REVALUATION";
    private static final String MATURE = "MATURE";

    @Override
    public ReportOutput load(Vector errorMsgs) {
        resetGValues();
        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

        if (null != output){
            List<ReportRow> rows = Arrays.stream(output.getRows()).collect(Collectors.toList());
            rows = extractFxConversion(rows);
            //rows = extractFxTranslationOnly(rows);
            rows = createNewRows(rows);
            output = setRows(rows);
        }

        return output;
    }

    /**
     * @param rows
     */
    private List<ReportRow> createNewRows(List<ReportRow> rows){
        return rows.stream().map(this::generateObbBean)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::initValues)
                .map(ReportRow::new)
                .collect(Collectors.toList());
    }


    /**
     * Extract FXConversion Posting
     * Key = postingLinkId
     * Value = boPosting
     * @param rows
     * @return
     */
    private List<ReportRow> extractFxConversion(List<ReportRow> rows){
        List<ReportRow> filteredRows = new ArrayList<>();
        if(!Util.isEmpty(rows)){
            rows.forEach(row -> {
                final Optional<BOPosting> boPosting = Optional.ofNullable(row.getProperty("BOPosting"));
                boPosting.ifPresent(posting -> {
                    if(!FX_TRANSLATION.equalsIgnoreCase(posting.getEventType()) && FX_CONVERSION.equalsIgnoreCase(posting.getPostingType())){
                        this.postingConversion.putIfAbsent(posting.getLinkedId(),posting);
                    }else if(!FX_REVALUATION.equalsIgnoreCase(posting.getOriginalEventType())){
                        filteredRows.add(row);
                    }
                });
            });
        }
        return filteredRows;
    }

    /**
     * Extract FXConversion Posting
     * Key = tradeLongId
     * Value = boPosting
     * @param rows
     * @return
     */
    private List<ReportRow> extractFxTranslationOnly(List<ReportRow> rows){
        List<ReportRow> filteredRows = new ArrayList<>();
        if(!Util.isEmpty(rows)){
            rows.forEach(row -> {
                final Optional<BOPosting> boPosting = Optional.ofNullable(row.getProperty("BOPosting"));
                boPosting.ifPresent(posting -> {
                    if(MATURE.equalsIgnoreCase(posting.getEventType())
                            && "NEW".equalsIgnoreCase(posting.getStatus())
                            && isFxTranslationOnly(posting)){
                        this.postingMatureFxTranslationOnly.putIfAbsent(posting.getTradeLongId(),posting);
                    }else {
                        filteredRows.add(row);
                    }
                });
            });
        }
        return filteredRows;
    }

    /**
     * Generate @{@link OBBGenericBean} by defined products types.
     * @param opRow
     * @return
     */
    private Optional<OBBGenericBean> generateObbBean(ReportRow opRow){
        Optional<OBBGenericBean> bean = Optional.empty();
        if(null!=opRow){
            final Trade trade = opRow.getProperty("Trade");
            final Optional<BOPosting> boPosting = Optional.ofNullable(opRow.getProperty("BOPosting"));
            final Optional<String> debitCredit = Optional.ofNullable(opRow.getProperty("DEBIT_CREDIT"));
            final Optional<Boolean> doNotSetAgrego = Optional.ofNullable(getReportTemplate().get(OBBGenericReportTemplatePanel.SET_AGREGO));


            if(boPosting.isPresent()){
                return getProductObbBean(trade, boPosting.get(),debitCredit.orElse("None"),doNotSetAgrego.orElse(false));
            }
        }

        return bean;
    }

    /**
     * Return OBBGenericBean by Product type.
     * Filtered by conversion posting and Currency.
     * @param trade
     * @param boPosting
     * @return
     */
    private Optional<OBBGenericBean> getProductObbBean(Trade trade, BOPosting boPosting,String debitCredit,boolean doNotSetAgrego){
        String obbType = "Balance";
        if(null!=trade){
            obbType = trade.getProductType();
        }
        OBBGenericBean genericBean = null;
        String handlerClassName = "tk.bo.obb." + obbType + "ObbBean";

        try {
            genericBean = (OBBGenericBean) InstantiateUtil.getInstance(handlerClassName,true);
            if(null != genericBean){
                BOPosting translationPosting = getTranslationPosting(boPosting);
                final Account account = OBBReportUtil.getAccount(debitCredit, boPosting);
                genericBean.setAccount(account);
                genericBean.setBoPostingConvert(translationPosting);
                genericBean.setTrade(trade);
                genericBean.setBoPosting(boPosting);
                genericBean.setProcessDate(getReportTemplate().getValDate());
                genericBean.setCreditDebit(debitCredit);
                genericBean.setDoNotSetAgrego(doNotSetAgrego);

                //Filter no conversion posting
                if(!"EUR".equalsIgnoreCase(boPosting.getCurrency())
                        && !Optional.ofNullable(genericBean.getBoPostingConvert()).isPresent()){
                    return Optional.empty();
                }
            }
        } catch (InstantiationException e) {
            Log.error(this,"Error instantiating class "+handlerClassName+": " + e);
        } catch (IllegalAccessException e) {
            Log.error(this,"Error: " + e);
        }

        return Optional.ofNullable(genericBean);
    }



    /**
     * Return FX_CONVERSION Posting no EUR
     * @param boPosting
     * @return
     */
    private BOPosting getTranslationPosting(BOPosting boPosting){
        if(!"MTM_FULL".equalsIgnoreCase(boPosting.getEventType())
                && !"MTM_FULL_BASE".equalsIgnoreCase(boPosting.getEventType())
                && "REVERSAL".equalsIgnoreCase(boPosting.getPostingType())){
            return postingConversion.get(boPosting.getLinkedId());
        }else if ("MATURE".equalsIgnoreCase(boPosting.getEventType())){
            BOPosting maturePostingConv = postingConversion.get(boPosting.getId());
            if(null!=maturePostingConv && maturePostingConv.getEventType().equalsIgnoreCase("MATURE")){
                return maturePostingConv;
            }
           return null;
        }else{
            return postingConversion.get(boPosting.getId());
        }
    }


    /**
     * Can Call multi threads execution.
     * @param bean
     */
    private OBBGenericBean initValues(OBBGenericBean bean) {
        bean.buildData();
        return bean;
    }

    /**
     * Always need the trades. (control on PostingReport)
     * @param output
     * @return
     */
    @Override
    protected boolean needsTrades(DefaultReportOutput output) {
        return true;
    }

    /**
     * @param rows
     * @return
     */
    private DefaultReportOutput setRows(List<ReportRow> rows){
        DefaultReportOutput output = new DefaultReportOutput(this);
        if(!Util.isEmpty(rows)) {
            output.setRows(rows.toArray(new ReportRow[rows.size()]));
        }
        return output;
    }

    private boolean isFxTranslationOnly(BOPosting posting){
        return Optional.ofNullable(posting).isPresent() && Util.toBoolean(posting.getAttributeValue("FxTranslationOnly"));
    }

    private void resetGValues(){
        this.postingConversion = new HashMap<>();
        this.postingMatureFxTranslationOnly = new HashMap<>();
    }
}
