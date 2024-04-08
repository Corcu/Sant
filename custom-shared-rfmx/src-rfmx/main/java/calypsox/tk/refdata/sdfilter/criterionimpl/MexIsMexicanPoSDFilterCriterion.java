package calypsox.tk.refdata.sdfilter.criterionimpl;

import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * @author x266033
 *
 *         Checks if the trade PO corresponds with one of the values of the DN
 *         MX_POs
 */
public class MexIsMexicanPoSDFilterCriterion extends AbstractSDFilterCriterion<Boolean> {

	/** SDFilterCriterion attribute name **/
	private static final String CRITERION_NAME = "MexIsMexicanPo";

	private static final String MX_POS = "MX_POs";

	@Override
	public SDFilterCategory getCategory() {
		return SDFilterCategory.TRADE;
	}

	@Override
	public Class<Boolean> getValueType() {
		return Boolean.class;
	}

	@Override
	public String getName() {
		return CRITERION_NAME;
	}

	@Override
	public Boolean getValue(SDFilterInput input) {
		Boolean value = false;
		final Trade trade = input.getTrade();

		final Vector<String> MexicanPOs = LocalCache.getDomainValues(DSConnection.getDefault(), MX_POS);

		if (trade != null) {
			final String processingOrg = getProcessingOrgName(trade);
			if (!Util.isEmpty(MexicanPOs) && MexicanPOs.contains(processingOrg)) {
				value = true;
			}
		}

		return value;
	}

	protected String getProcessingOrgName(final Trade trade) {
		String processingOrgName = "";
		final LegalEntity processingOrg = getProcessingOrg(trade);

		if (processingOrg != null) {
			// shortname of the legal entity
			processingOrgName = processingOrg.getCode();
		}

		return processingOrgName;
	}

	private LegalEntity getProcessingOrg(final Trade trade) {
		LegalEntity processingOrg = null;

		if (trade.getBook() != null) {
			final Book book = trade.getBook();
			final int idProcessingOrg = book.getProcessingOrgBasedId();
			processingOrg = getRemoteLegalEntity(idProcessingOrg);
		}

		return processingOrg;
	}

	/**
	 * Retrieve the legal entity
	 */
	protected LegalEntity getRemoteLegalEntity(final int idProcessingOrg) {
		LegalEntity legalEntity = null;

		try {
			legalEntity = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(idProcessingOrg);
		} catch (final CalypsoServiceException calypsoServiceException) {
			Log.error(this, "Error retrieving the legal entity with the PO: " + idProcessingOrg);

		}

		return legalEntity;
	}

}
