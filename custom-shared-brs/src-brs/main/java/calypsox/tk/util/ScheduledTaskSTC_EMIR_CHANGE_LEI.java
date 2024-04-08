/*
 *
 * Copyright (c) 2011 Kaupthing Bank
 * Borgartn 19, IS-105 Reykjavik, Iceland
 * All rights reserved.
 *
 */
/**
 *
 */
package calypsox.tk.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskREPORT;
import calypsox.tk.bo.workflow.rule.SantUtiUsiGenerationTradeRule;



public class ScheduledTaskSTC_EMIR_CHANGE_LEI extends ScheduledTaskREPORT {



	private static final long serialVersionUID = -1L;
	private static final String PROPERTY_AUDIT_VALUE = "AuditValue";
	private static final String MODIF_ATTRIBUTE_VALUE = "_attributeValue";
	private static final String CREATE_ATTRIBUTE_VALUE = "_CREATE_";
	private static final String DELETE_ATTRIBUTE_VALUE = "_DELETE_";
	private static final String TYPE_PERFORMANCE_SWAP = "PerformanceSwap";
	private static final String KEYWORD_UTI_TRADE_ID = "UTI_REFERENCE";
	private static final String KEYWORD_TEMP_UTI_TRADE_ID = "TempUTITradeId";
    private static final String KEYWORD_PREVIOUSLEIVALUE = "PreviousLEIValue";
    private static final String ACTION_AMEND = Action.S_AMEND;
	
	
  /*
   * (non-Javadoc)
   *
   * @see com.calypso.tk.util.ScheduledTaskREPORT#getTaskInformation()
   */
  @Override
  public String getTaskInformation() {
    return "Search change in attribute LEI.";
  }

  
  
  /*
   * (non-Javadoc)
   *
   * @see
   * com.calypso.tk.util.ScheduledTaskREPORT#process(com.calypso.tk.service
   * .DSConnection, com.calypso.tk.event.PSConnection)
   */
  @Override
  public boolean process(final DSConnection dsCon, final PSConnection psconnection) {
	  
    final List<String> errors = new ArrayList<String>();
    final List<ChangeLeiItem> items = getAuditReportData(dsCon, errors);

    if (items != null) {
      if (items.isEmpty()) {
        Log.info(this, "No LEs with changes in attribute LEI at date " + getValuationDatetime().getJDate(TimeZone.getDefault()));
      } else {
        items.stream().forEach(i -> updateTrades(i, dsCon, errors));
      }
    }

    // Check errors
    if (!errors.isEmpty()) {
      Log.info(this, StringUtils.join(errors, ";"));
    }

    return true;

  }

  
  
  /**
   * Update Trades.
   *
   * @param item
   * @param dsconnection
   */
  private void updateTrades(final ChangeLeiItem item, final DSConnection dsCon, final List<String> errors) {
    final List<Long> tradeIds = getTradeIdsVivos(item.getLegalEntityId(), dsCon, errors);
    tradeIds.stream().map(tradeId -> getTrade(dsCon, tradeId)).filter(trade -> (trade != null))
    .forEach(trade -> saveTradeKeyword(trade, item, dsCon, errors));
  }

  
  
  /**
   * Add kws to insert into trade. If OldLEIValue exists, use it. In other case, use the GLCS.
   *
   * @param item
   * @return
   */
  private Hashtable<String, String> getKeywordsToInsert(final Trade trade, final ChangeLeiItem item,
      final DSConnection dsCon) {

    final Hashtable<String, String> kwToInsert = new Hashtable<String, String>();

    // Add keyword PREVIOUSLEIVALUE
    final String oldValue = item.getOldValue();

    if (!Util.isEmpty(oldValue)) {
      kwToInsert.put(KEYWORD_PREVIOUSLEIVALUE, oldValue);
    } else {
      kwToInsert.put(KEYWORD_PREVIOUSLEIVALUE, item.getLegalEntityCode());
    }

    // Add keyword UTI
    addKeywordUti(kwToInsert, trade, dsCon);

    return kwToInsert;
  }

  
  
  /**
   * Generate and add UTI keyword.
   *
   * @param kwToInsert
   * @param trade
   * @param ds
   */
  @SuppressWarnings("rawtypes")
  private void addKeywordUti(final Hashtable<String, String> kwToInsert, final Trade trade, final DSConnection dsCon) {

	final SantUtiUsiGenerationTradeRule tradeRule = new SantUtiUsiGenerationTradeRule();

	if (Util.isEmpty(trade.getKeywordValue(KEYWORD_UTI_TRADE_ID)) && Util.isEmpty(trade.getKeywordValue(KEYWORD_TEMP_UTI_TRADE_ID))) {
        // Cloned Trade
        final Trade clonedTrade = trade.clone();
    	LegalEntity po = trade.getBook().getLegalEntity();
	    final Collection<LegalEntityAttribute> poAttrs = BOCache.getLegalEntityAttributes(dsCon, po.getId());
	    final LegalEntity cp = trade.getCounterParty();
	    final Collection<LegalEntityAttribute> cpAttrs = BOCache.getLegalEntityAttributes(dsCon, cp.getId());
	    if(tradeRule.isTradeReportable(trade, poAttrs, cpAttrs)) {
	    	// Call to the trade rule 'SantUtiUsiGeneration' 
	    	tradeRule.update(null, clonedTrade, null, new Vector(), dsCon, null, null, null, null);
	        String uti = clonedTrade.getKeywordValue(KEYWORD_UTI_TRADE_ID);
	        if (Util.isEmpty(uti)) {
	          // Generate UTI_Temp    
	    	    uti = tradeRule.generateUti(clonedTrade, poAttrs);
	        }
	        if (!Util.isEmpty(uti)) {
	          // Add keywords
	          kwToInsert.put(KEYWORD_UTI_TRADE_ID, uti);
	        }
	    }
	}

  }

  
  
  /**
   * Save the trade adding the new keyword PreviousLEIValue.
   *
   * @param trade
   * @param item
   * @param ds
   */
  @SuppressWarnings("unchecked")
  private void saveTradeKeyword(final Trade trade, final ChangeLeiItem item, final DSConnection ds, final List<String> errors) {

    if (trade == null) {
    	String message = "Trade is null";
		Log.error(this, message);
		return;
    }
    	
    trade.setAction(Action.AMEND);
    final Hashtable<String, String> kwToInsert = getKeywordsToInsert(trade, item, ds);

    if (Util.isEmpty(kwToInsert)) {
    	String message = "Trade " + trade.getLongId() + ". Keywords are empty";
		Log.error(this, message);
		return;
    }
    
    Trade newTrade = new Trade();
  	newTrade = trade.clone();
       
  	Hashtable<String, String> tradeAtts = trade.getKeywords();
  	Iterator<Entry<String, String>> it = kwToInsert.entrySet().iterator();

	// delete current keyword values
	while (it.hasNext()) {
		final Entry<String, String> entry = it.next();
		newTrade.removeKeyword(entry.getKey());
  	}

	if(tradeAtts == null) {
		tradeAtts = new Hashtable<String, String>();
	}
	
  	tradeAtts.putAll(kwToInsert);
  	newTrade.setKeywords(tradeAtts); 
  	newTrade.setAction(Action.AMEND);
  	
  	try {
  		DSConnection.getDefault().getRemoteTrade().save(newTrade);
  		String message = "Action '" + ACTION_AMEND + "' applied on trade " + trade.getLongId();
  	    Log.info(this, message);  			
  	} catch (CalypsoServiceException e) {
  		String message = "Could not save keywords and apply action " +  ACTION_AMEND + "for trade " + newTrade.getLongId();
  		Log.error(this, message);
  	}
  }

  
  
  /**
   * Get the trade ids with a CPTY_ID.
   *
   * @param leId
   * @param ds
   * @return
   */
  private List<Long> getTradeIdsVivos(final int leId, final DSConnection ds, final List<String> errors) {

	long[] tradeArray = null;
	List<Long> tradeIdList = new ArrayList<Long>();
    //final JDate valDate = getValuationDatetime().getJDate(TimeZone.getDefault());
    final JDatetime valDate = getValuationDatetime();

    StringBuffer fromClause = new StringBuffer("product_desc pd");

    StringBuffer whereClause = new StringBuffer();
    whereClause.append(" trade.cpty_id = ");
    whereClause.append(String.valueOf(leId));
    whereClause.append(" AND pd.product_id = trade.product_id ");
    whereClause.append(" AND trade.trade_status NOT IN ('CANCELED','TERMINATED','MATURED') ");
    whereClause.append(" AND pd.product_type ='");
    whereClause.append(TYPE_PERFORMANCE_SWAP);
    whereClause.append("'");
    whereClause.append(" AND TRUNC(pd.maturity_date) >= ");
    whereClause.append(Util.date2SQLString(valDate));
      
    TradeFilter tradeFilter = new TradeFilter();
	tradeFilter.setSQLFromClause(fromClause.toString());
	tradeFilter.setSQLWhereClause(whereClause.toString());

	try {
		tradeArray = DSConnection.getDefault().getRemoteTrade(). getTradeIds(tradeFilter, null, false);
	} catch (CalypsoServiceException e) {
		Log.error(this, "Error retrieving query. " + e.getMessage());
	}
    
	
	if(tradeArray != null) {
	    for (int i=0; i<tradeArray.length; i++) {
	    	tradeIdList.add(tradeArray[i]);
	    }
  	}
    
    return tradeIdList;    
  }



  /**
   * Filter AuditValue: only with _attributeVale as FieldName
   *
   * @param av
   * @return
   */
  private static boolean isAttributeValue(final AuditValue av) {
    return av != null
        && (MODIF_ATTRIBUTE_VALUE.equals(av.getFieldName()) || CREATE_ATTRIBUTE_VALUE.equals(av.getFieldName()) || DELETE_ATTRIBUTE_VALUE
            .equals(av.getFieldName()));
  }

  
  
  /**
   * Create ChangeLeiItem from AuditValue.
   *
   * @param av
   * @param items
   */
  private void createItem(final AuditValue av, final List<ChangeLeiItem> items, final DSConnection dsconnection) {
    final ChangeLeiItem item = new ChangeLeiItem();
    item.setLegalEntityAttributeId(av.getEntityLongId());
    item.setLegalEntityAttributeName(av.getEntityName());
    item.setModifDate(av.getModifDate());
    item.setLegalEntityId(av.getRelatedObjectId());
    item.setLegalEntityCode(getLegalEntityCodeFromAudit(av, dsconnection));
    item.setVersion(av.getVersion());

    if (MODIF_ATTRIBUTE_VALUE.equals(av.getFieldName())) {
      item.setOldValue(av.getField().getOldValue());
      item.setNewValue(av.getField().getNewValue());
    } else {
      if (CREATE_ATTRIBUTE_VALUE.equals(av.getFieldName())) {
        item.setNewValue(getLegalEntityAttributeValue(av));
      } else if (DELETE_ATTRIBUTE_VALUE.equals(av.getFieldName())) {
        item.setOldValue(getLegalEntityAttributeValue(av));
      }
    }

    items.add(item);
  }

  
  
  /**
   * In case of CREATE of DELETE, the AuditValue has then information into compress object. This
   * object is, in this particular case, a LegalEntityAttribute object. So, it is possible to access
   * to the last attribute value.
   *
   * @param av
   * @return
   */
  private static String getLegalEntityAttributeValue(final AuditValue av) {
    if (av.getFieldObjectValue() instanceof LegalEntityAttribute) {
      final LegalEntityAttribute leAttr = (LegalEntityAttribute) av.getFieldObjectValue();
      if (leAttr != null) {
        return leAttr.getAttributeValue();
      }
    }
    return null;
  }

  
  
  /**
   * Get data from AuditReport.
   *
   * @return
   */
  private List<ChangeLeiItem> getAuditReportData(final DSConnection dsconnection, final List<String> errors) {
    final Map<Integer, ChangeLeiItem> itemsByLegalEntityId = new TreeMap<Integer, ChangeLeiItem>();
    ReportOutput reportOutput = null;
    try {
      reportOutput = generateReportOutput(getAttribute(REPORT_TYPE), getAttribute(REPORT_TEMPLATE_NAME),
          getValuationDatetime(), DSConnection.getDefault(), new StringBuffer("desc"));

      final List<ChangeLeiItem> items = new ArrayList<ChangeLeiItem>();
      final DefaultReportOutput rst = (DefaultReportOutput) reportOutput;
      final Stream<ReportRow> streamReportRows = Stream.of(rst.getRows());
      streamReportRows.map(row -> (AuditValue) row.getProperty(PROPERTY_AUDIT_VALUE))
      .filter(av -> isAttributeValue(av)).forEach(av -> createItem(av, items, dsconnection));

      // If there are several changes on a LEI attribute in the same LegalEntity, it will choose the
      // last change.
      items.stream().forEach(
          item -> itemsByLegalEntityId.merge(item.getLegalEntityId(), item, (prevItem, nextItem) -> nextItem));

      return new ArrayList<ChangeLeiItem>(itemsByLegalEntityId.values());

    } catch (final RemoteException e1) {
      final String msg = "Couldn't create Report";
      errors.add(msg + ":" + e1.getMessage());
      Log.info(this, msg, e1);
    }

    return null;
  }

  
  
  /**
   * Get the LegalEntity Code.
   *
   * @param av
   * @return
   */
  private String getLegalEntityCodeFromAudit(final AuditValue av, final DSConnection ds) {
    LegalEntity le;
    if (av.getRelatedObjectId() <= 0) {
      return null;
    }
    le = BOCache.getLegalEntity(ds, av.getRelatedObjectId());
    if (le != null) {
      return le.getCode();
    } else {
      return "";
    }
  }



  /**
   * Generate report output.
   *
   * @param reportType
   *            the report type
   * @param reportTemplate
   *            the report template
   * @param valDate
   *            the val date
   * @param dsconnection
   *            the dsconnection
   * @param errors
   *            the errors
   * @return the report output
   * @throws RemoteException
   *             the remote exception
   */
  @SuppressWarnings("rawtypes")
  protected ReportOutput generateReportOutput(final String reportType, final String reportTemplate, 
		  final JDatetime valDate, final DSConnection dsconnection, final StringBuffer errors) throws RemoteException {

	final PricingEnv pricingenv = dsconnection.getRemoteMarketData().getPricingEnv(_pricingEnv, valDate);
    final Report report = createReport(reportType, reportTemplate, errors, pricingenv);
    if (report == null) {
      Log.error(this, (new StringBuilder()).append("Invalid report type: ").append(reportType).toString());
      errors.append((new StringBuilder()).append("Invalid report type: ").append(reportType).append("\n").toString());
      return null;
    }
    if (report.getReportTemplate() == null) {
      Log.error(this, (new StringBuilder()).append("Invalid report template: ").append(reportType).toString());
      errors.append((new StringBuilder()).append("Invalid report template: ").append(reportType).append("\n").toString());
      return null;
    }
    final Vector vector = getHolidays();
    report.getReportTemplate().setHolidays(vector);
    if (getTimeZone() != null) {
      report.getReportTemplate().setTimeZone(getTimeZone());
    }
    final Vector vector1 = new Vector();
    return report.load(vector1);
  }
  
  
}



class ChangeLeiItem {
  private long legalEntityAttributeId;
  private String legalEntityAttributeName;
  private JDatetime modifDate;
  private String oldValue;
  private String newValue;
  private int legalEntityId;
  private String legalEntityCode;
  private int version;

  public long getLegalEntityAttributeId() {
    return legalEntityAttributeId;
  }

  public void setLegalEntityAttributeId(long legalEntityAttributeId) {
    this.legalEntityAttributeId = legalEntityAttributeId;
  }

  public String getLegalEntityAttributeName() {
    return legalEntityAttributeName;
  }

  public void setLegalEntityAttributeName(String legalEntityAttributeName) {
    this.legalEntityAttributeName = legalEntityAttributeName;
  }

  public JDatetime getModifDate() {
    return modifDate;
  }

  public void setModifDate(JDatetime modifDate) {
    this.modifDate = modifDate;
  }

  public String getOldValue() {
    return oldValue;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public int getLegalEntityId() {
    return legalEntityId;
  }

  public void setLegalEntityId(int legalEntityId) {
    this.legalEntityId = legalEntityId;
  }

  public String getLegalEntityCode() {
    return legalEntityCode;
  }

  public void setLegalEntityCode(String legalEntityCode) {
    this.legalEntityCode = legalEntityCode;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public String toString() {
    final String toString = "legalEntityAttributeName = [" + legalEntityAttributeName + "] - " + "modifDate = ["
        + modifDate + "] - " + "oldValue = [" + oldValue + "] - " + "newValue = [" + newValue + "] - "
        + "legalEntityId = [" + legalEntityId + "] - " + "legalEntityCode = [" + legalEntityCode + "] ";

    return toString;
  }
}
