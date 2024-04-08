package calypsox.tk.refdata;

import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.HedgeRelationship;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteProduct;

import calypsox.tk.bo.CustomFallidasfallidasReportClientCacheAdapter;

/**
 * The Class IsNotAmortDateStaticDataFilter.
 *
 * @
 */
public class isNotAmortDateStaticDataFilter implements StaticDataFilterInterface {

    /** TreeList Parent reference for custom FLOWDATE_EQUALS_AMORTDATEs. */
    private static final String PARENT_CUSTOM_REFERENCE = "Custom";
    
    /** FLOWDATE_EQUALS_AMORTDATE. */
	private static final String FLOWDATE_EQUALS_AMORTDATE = "Is flow date different than bond amortization date";

	/**
	 * @param trade
	 * @param le
	 * @param role
	 * @param product
	 * @param transfer
	 * @param message
	 * @param rule
	 * @param reportRow
	 * @param task
	 * @param glAccount
	 * @param cashflow
	 * @param relationship
	 * @param filterElement
	 * @param element
	 * @return
	 * @see com.calypso.tk.refdata.StaticDataFilterInterface#getValue(com.calypso.tk.core.Trade,
	 *      com.calypso.tk.core.LegalEntity, java.lang.String,
	 *      com.calypso.tk.core.Product, com.calypso.tk.bo.BOTransfer,
	 *      com.calypso.tk.bo.BOMessage, com.calypso.tk.bo.TradeTransferRule,
	 *      com.calypso.tk.report.ReportRow, com.calypso.tk.bo.Task,
	 *      com.calypso.tk.refdata.Account, com.calypso.tk.core.CashFlow,
	 *      com.calypso.tk.core.HedgeRelationship, java.lang.String,
	 *      com.calypso.tk.refdata.StaticDataFilterElement)
	 */
	@Override
	public Object getValue(final Trade trade, final LegalEntity le, final String role, final Product product,
			final BOTransfer transfer, final BOMessage message, final TradeTransferRule rule, final ReportRow reportRow,
			final Task task, final Account glAccount, final CashFlow cashflow, final HedgeRelationship relationship,
			final String filterElement, final StaticDataFilterElement element) {

		if (FLOWDATE_EQUALS_AMORTDATE.equals(filterElement) && (reportRow != null)) {

			return dateNotAmort(reportRow);

		}
		return true;
	}

	private Boolean dateNotAmort(ReportRow reportRow) {

		boolean existDate= true;
		Product sec = null;
		BOTransfer transfer = (BOTransfer) reportRow.getProperty(ReportRow.TRANSFER);
		int productID = transfer.getProductId();
		RemoteProduct remoteProduct = null;
		
		try {
			sec = (Product) remoteProduct.getProduct(productID);
		} catch (CalypsoServiceException e) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error al recuperar el remote product de la transfer: " + transfer.getLongId() + " : "
							+ e.getCause());
			e.printStackTrace();
		}

		if (sec instanceof Bond && sec.getFlows() != null) {
		
//		try
//		{
//			sec = DSConnection.getDefault().getRemoteProduct().getProduct(transfer.getProductId());
//		}		catch  (CalypsoServiceException e1){
//			throw new CalypsoServiceException("Product with id: " + transfer.getProductId() + " is not a Bond");
//		}
		
		//Recuperamos los cashFlows
		CashFlowSet cashFlows = sec.getFlows();
		
		if (sec!=null && sec instanceof Bond) {	
			CashFlowSet principalFlows = cashFlows.filterFlows(CashFlow.PRINCIPAL);
			if (principalFlows != null && !principalFlows.isEmpty()) {
					JDate bondPmtDt = null;
					for (CashFlow cashFlow : principalFlows) {
						bondPmtDt = cashFlow.getDate();
						if (bondPmtDt == null) {
							existDate = true;
						} else if (bondPmtDt.equals(transfer.getSettleDate())) {
							existDate = false;
							break;
						}
					}
			}
		}
		}
		return existDate;
	}


	/**
	 * @param paramDSConnection
	 * @param ls
	 * @return
	 * @see com.calypso.tk.refdata.StaticDataFilterInterface#fillTreeList(com.calypso.tk.service.DSConnection,
	 *      com.calypso.apps.util.TreeList)
	 */
	@Override
	public boolean fillTreeList(final DSConnection paramDSConnection, final TreeList ls) {
		ls.add(PARENT_CUSTOM_REFERENCE, FLOWDATE_EQUALS_AMORTDATE);
		return true;
	}

	/**
	 * @param paramDSConnection
	 * @param v
	 * @see com.calypso.tk.refdata.StaticDataFilterInterface#getDomainValues(com.calypso.tk.service.DSConnection,
	 *      java.util.Vector)
	 */
	@Override
	public void getDomainValues(final DSConnection paramDSConnection, final Vector v) {
		v.addElement(FLOWDATE_EQUALS_AMORTDATE);
	}

	/**
	 * @param keyword
	 * @return
	 * @see com.calypso.tk.refdata.StaticDataFilterInterface#getTypeDomain(java.lang.String)
	 */
	@Override
	public Vector getTypeDomain(final String keyword) {
		final Vector<String> v = new Vector<>();
		if (keyword.equals(FLOWDATE_EQUALS_AMORTDATE)) {
			v.addElement(StaticDataFilterElement.S_IS);
		}
		return v;
	}

	/**
	 * @param paramDSConnection
	 * @param paramString
	 * @return
	 * @see com.calypso.tk.refdata.StaticDataFilterInterface#getDomain(com.calypso.tk.service.DSConnection,
	 *      java.lang.String)
	 */
	@Override
	public Vector getDomain(final DSConnection paramDSConnection, final String paramString) {
		return null;
	}

	/**
	 * @param paramString
	 * @return
	 * @see com.calypso.tk.refdata.StaticDataFilterInterface#isTradeNeeded(java.lang.String)
	 */
	@Override
	public boolean isTradeNeeded(final String paramString) {

		return false;
	}
	

					
}
