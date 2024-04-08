package calypsox.tk.report;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.Pair;
import com.calypso.tk.core.InvalidConfigurationException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.CAApplyInfo;
import com.calypso.tk.product.CorporateActionHandlerUtil;
import com.calypso.tk.product.corporateaction.CAGenerationAction;
import com.calypso.tk.product.corporateaction.CAGenerationContext;
import com.calypso.tk.product.corporateaction.CAGenerationContextBuilder;
import com.calypso.tk.product.corporateaction.CAGenerationHandlerUtil;
import com.calypso.tk.product.corporateaction.CAPositionType;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.report.CAApplyTradeReportTemplate;
import com.calypso.tk.report.CAApplyTradeSimulationReportTemplate;
import com.calypso.tk.report.CACompositeReportTemplate;
import com.calypso.tk.report.CAReport;
import com.calypso.tk.report.CAReportTemplate;
import com.calypso.tk.report.DateTenorFilterDescriptor;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.risk.AnalysisProgress;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;


public class CARFApplyTradeSimulationReport extends com.calypso.tk.report.CAApplyTradeReport {


    private static final long serialVersionUID = -6601715823232799615L;
    private transient CAApplyInfo __caApplyInfo;


    public CARFApplyTradeSimulationReport() {
    }


    public DefaultReportOutput load(Vector errorMsgs) {
        DefaultReportOutput output = new DefaultReportOutput(this);
        CAApplyTradeSimulationReportTemplate caApplyTradeReportTemplate = (CAApplyTradeSimulationReportTemplate)this.getReportTemplate();
        this.generateCAFirst(errorMsgs);
        List<CA> cas = loadCA(caApplyTradeReportTemplate, errorMsgs);
        if (Util.isEmpty(cas)) {
            return output;
        } else {
            this.setThreadLocalAnalysisProgress(caApplyTradeReportTemplate, "Simulating Application of " + cas.size() + " CA");
            try {
                this.__caApplyInfo = caApplyTradeReportTemplate.buildCAApplyInfo(cas, this.getPricingEnv());
                TradeArray caTrades = this.__caApplyInfo.applyNow();
                if (!Util.isEmpty(caTrades)) {
                    this.filterCATrades(caApplyTradeReportTemplate, caTrades);
                    caApplyTradeReportTemplate.setTrades(caTrades);
                    caApplyTradeReportTemplate.setCaApplyInfo(this.__caApplyInfo);
                    output = super.load(errorMsgs);
                }
            } catch (Exception var6) {
                Log.error(this, var6);
                errorMsgs.add(var6.getMessage());
            }
            if (!Util.isEmpty(errorMsgs)) {
                CorporateActionHandlerUtil.logError(Util.collectionToString(errorMsgs, Util.LINE_SEPARATOR));
            }
            return output;
        }
    }


    private void setThreadLocalAnalysisProgress(CAApplyTradeSimulationReportTemplate caApplyTradeReportTemplate, String message) {
        AnalysisProgress progress = null;
        progress = caApplyTradeReportTemplate.getTransientAnalysisProgress();
        if (progress != null) {
            CorporateActionHandlerUtil.setThreadLocalAnalysisProgress(progress, message);
        }
    }


    private void generateCAFirst(Vector errorMsgs) {
        List<CA> cas = new ArrayList();
        CAApplyTradeSimulationReportTemplate reportTemplate = (CAApplyTradeSimulationReportTemplate)this.getReportTemplate();
        CAReportTemplate caReportTemplate = reportTemplate.getCAReportTemplate();
        boolean isGenerateCAFirst = Boolean.TRUE.equals(reportTemplate.get("Generate Ca First"));
        if (isGenerateCAFirst) {
            Product undl = caReportTemplate.getUnderlying();
            Collection<Product> undls = caReportTemplate.getUnderlyings();
            if (undl == null && undls == null) {
                errorMsgs.add("Please select an underlying product.");
            } else {
                HashSet<Product> underlyings = new HashSet();
                if (undl != null) {
                    underlyings.add(undl);
                } else {
                    underlyings.addAll(undls);
                }
                String generateDateType = (String)reportTemplate.get("Date Type");
                Pair<JDate, JDate> startEndDate = this.getStartEndDate(caReportTemplate, generateDateType, errorMsgs);
                boolean isGenerateCAIssuance = Boolean.TRUE.equals(reportTemplate.get("Generate Related Issuances"));
                boolean isGenerateDrawnBond = Boolean.TRUE.equals(reportTemplate.get("Generate Drawn Bond"));
                CAGenerationContextBuilder builder = CAGenerationContextBuilder.builder(this.getPricingEnv(), underlyings).startDate((JDate)startEndDate.first()).backValueDate((JDate)startEndDate.first()).endDate((JDate)startEndDate.second()).processDate(generateDateType).generateCAIssuance(isGenerateCAIssuance).generateDrawnBond(isGenerateDrawnBond);
                this.setThreadLocalAnalysisProgress(reportTemplate, "Simulating Generation of " + underlyings);
                try {
                    Map<CA, CAGenerationAction> generateLoadAndMergeCA = CAGenerationHandlerUtil.generateLoadAndMergeCA(CAGenerationContext.createNew(builder));
                    if (generateLoadAndMergeCA != null) {
                        cas.addAll(generateLoadAndMergeCA.keySet());
                    }
                } catch (InvalidConfigurationException var15) {
                    Log.error(this, var15);
                }
            }
        }
        int i = 0;
        for(int negativeId = 0; i < cas.size(); ++i) {
            CA ca = (CA)cas.get(i);
            if (ca.getId() == 0) {
                --negativeId;
                ca.setId(negativeId);
            }
        }
        caReportTemplate.setProducts(cas);
    }


    private Pair<JDate, JDate> getStartEndDate(CAReportTemplate caReportTemplate, String generateDateType, Vector errorMsgs) {
        JDate startDate = JDate.getNow();
        JDate endDate = JDate.getNow();
        String dateType = "Payment Date";
        if ("Ex Date".equals(generateDateType)) {
            dateType = "Ex Date";
        } else if ("Record Date".equals(generateDateType)) {
            dateType = "Record Date";
        }
        DateTenorFilterDescriptor desc = DateTenorFilterDescriptor.fromReportTemplate(dateType, caReportTemplate);
        if (desc != null) {
            UserDefaults userDefaults = DSConnection.getDefault().getUserDefaults();
            Vector holidays = null;
            if (userDefaults != null) {
                holidays = userDefaults.getHolidays();
            }
            JDate startDateTmp = desc.getStartDate(this.getValDate(), holidays, false);
            if (startDateTmp != null) {
                startDate = startDateTmp;
            }
            JDate endDateTmp = desc.getEndDate(this.getValDate(), holidays, false);
            if (endDateTmp != null) {
                endDate = endDateTmp;
            }
        }
        if (startDate.after(endDate)) {
            errorMsgs.add("Please put a Start " + generateDateType + " before End " + generateDateType + ".");
        }
        return new Pair(startDate, endDate);
    }

	private void filterCATrades(CAApplyTradeReportTemplate template, Collection<Trade> caTrades) {
		boolean isAgentAdjustBookOnly = Boolean.TRUE.equals(template.get("Agent Aggregation Only"));
		List<CAPositionType> caPositionTypes = (List) template.get("CA Position Type");
		String po = (String) template.get("Trade Processing Org");
		List<String> listPO = po != null && !po.isEmpty() ? Arrays.asList(po.split(",")) : null;
		boolean filterLe = listPO != null && !listPO.isEmpty();
		Iterator iterator = caTrades.iterator();
		while (true) {
			while (iterator.hasNext()) {
				Trade caTrade = (Trade) iterator.next();
				// Filter Trades by ProcessingOrg
				if (filterByAgent(caTrade)) {
					iterator.remove();
				} else if (isAgentAdjustBookOnly && "Agent".equals(caTrade.getRole())
						&& !CorporateActionHandlerUtil.isAdjustmentBook(caTrade.getBook())) {
					iterator.remove();
				} else if (!Util.isEmpty(caPositionTypes)) {
					CAPositionType tradePositionType = CAPositionType.getPositionType(caTrade);
					if (tradePositionType == null || !caPositionTypes.contains(tradePositionType)) {
						iterator.remove();
					}
				} else if (filterLe && !listPO.contains(Trade.getLegalEntity(caTrade).getCode())) {
					iterator.remove();
				}
			}
			return;
		}
	}


    public void cancel() {
        if (this.__caApplyInfo != null) {
            this.__caApplyInfo.shutdownNow();
        }

    }


    private boolean filterByAgent(Trade trade) {
        Boolean filter = false;
        LegalEntity le = trade.getCounterParty();
        if(!Util.isEmpty(le.getCode())){
            if(("CounterParty".equalsIgnoreCase(trade.getRole()) || "Agent".equalsIgnoreCase(trade.getRole()))  && "DUMMY_AGENT".equalsIgnoreCase(le.getCode())){
                return true;
            }
        }
        return filter;
    }


    List<CA> loadCA(CACompositeReportTemplate template, List<String> errorMsgs) {
        return loadCA(template.getCAReportTemplate(), errorMsgs);
    }


    List<CA> loadCA(CAReportTemplate template, List<String> errorMsgs) {
        List<CA> cas = template.getProducts();
        if (com.calypso.infra.util.Util.isEmpty(cas)) {
            CAReport caReport = (CAReport)Report.getReport("CA");
            caReport.setReportTemplate(template);
            if (template.getValDate() != null) {
                caReport.setValuationDatetime(template.getValDate().getJDatetime());
            }
            try {
                cas = caReport.loadProducts();
                template.setProducts(cas);
            } catch (RemoteException var5) {
                Log.error("CAApplyTradeSimulationReport", var5);
                errorMsgs.add(var5.getMessage());
            }
        }
        return cas;
    }


}
