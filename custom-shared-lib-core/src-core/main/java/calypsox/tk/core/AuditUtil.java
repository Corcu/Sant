package calypsox.tk.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.Auditable;
import com.calypso.tk.core.FieldModification;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.FXProductBased;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.cache.CacheableObject;

public class AuditUtil {

    /**
     * Product family domain value
     */
    public static final String PRODUCT_CLASS = "Class";

    /**
     * Dot
     */
    private static final String DOT = ".";

    /**
     * Product audit group: product.
     */
    private static final String PRODUCT = "product.";
    /**
     * Trade audit group: trade.
     */
    private static final String TRADE = "trade.";

    /**
     * ALL domain value
     */
    public static final String ALL = "ALL";

    public static AuditUtil instance;

    private AuditUtil() {
    }

    public static AuditUtil getInstance() {
	if (instance == null) {
	    instance = new AuditUtil();
	}
	return instance;
    }

    /**
     * 
     * Only to JUnit. You can use that to replace the instance with a mockito
     * instance
     */
    public static void setInstance(final AuditUtil mockInstance) {
	instance = mockInstance;
    }

    /**
     * Extended list of Trade authorized changes<br />
     * - {LBPCheckNoChangeTrade, [group.]trade.ALL} domain value contains common
     * list<br />
     * - {LBPCheckNoChangeTrade, [group.]trade."Class"productFamily} domain
     * value list for a product family<br />
     * - {LBPCheckNoChangeTrade, [group.]trade.productType} domain value
     * specific list for a product type<br />
     * 
     * @param trade
     *            involved Trade
     * @param group
     *            define behavior by group
     * @param ds
     *            data server connection
     * @return list of Trade authorized changes as String array
     */
    public Collection<String> getIgnoredChanges(final Trade trade,
	    final String group, final DSConnection ds, final String domainName) {

	if (trade != null) {
	    final List<String> newList = new ArrayList<String>();
	    final String sGroup = (group == null) ? "" : (group + DOT);
	    final String domainValue = sGroup + TRADE + trade.getProductType();
	    final String domainVF = sGroup + TRADE + PRODUCT_CLASS
		    + trade.getProductFamily();
	    AuditUtil.getInstance().checkAmendment(domainName,
		    sGroup + TRADE + ALL, newList, ds);
	    AuditUtil.getInstance().checkAmendment(domainName, domainValue,
		    newList, ds);
	    AuditUtil.getInstance().checkAmendment(domainName, domainVF,
		    newList, ds);
	    return newList;
	}
	return null;
    }

    /**
     * Extended list of Product authorized changes<br />
     * - {LBPCheckNoChangeTrade, [group.]product.ALL} domain value contains
     * common list<br />
     * - {LBPCheckNoChangeTrade, [group.]product."Class"productFamily} domain
     * value list for product family<br />
     * - {LBPCheckNoChangeTrade, [group.]product.productType} domain value
     * specific list for a product type<br />
     * 
     * @param product
     *            involved Product
     * @param group
     *            define behavior by group
     * @param ds
     *            data server connection
     * @return list of Product authorized changes as String array
     * @see AuditUtil
     */
    public Collection<String> getIgnoredChanges(final Product product,
	    final String group, final DSConnection ds, final String domainName) {
	if (product != null) {
	    final List<String> newList = new ArrayList<String>();
	    final String sGroup = (group == null) ? "" : (group + DOT);
	    final String domainValue = sGroup + PRODUCT + product.getType();
	    final String domainVF = sGroup + PRODUCT + PRODUCT_CLASS
		    + product.getProductFamily();
	    AuditUtil.getInstance().checkAmendment(domainName,
		    sGroup + PRODUCT + ALL, newList, ds);
	    AuditUtil.getInstance().checkAmendment(domainName, domainValue,
		    newList, ds);
	    AuditUtil.getInstance().checkAmendment(domainName, domainVF,
		    newList, ds);
	    return newList;
	}
	return null;
    }

    /**
     * Additional audited attributes names are added to the White list of
     * records
     * 
     * @param domainName
     *            domain name to take audit values from
     * @param domainValue
     *            domain value to take audit values from
     * @param list
     *            list of audit values to consider
     * @param ds
     *            Data Server connection
     * @return always true
     */
    public boolean checkAmendment(final String domainName,
	    final String domainValue, final Collection<String> list,
	    final DSConnection ds) {
	if ((list != null) && !Util.isEmpty(domainName)
		&& !Util.isEmpty(domainValue)) {

	    final String comment = LocalCache.getDomainValueComment(ds,
		    domainName, domainValue);
	    AuditUtil.getInstance().addToList(list, comment);
	}
	return true;
    }

    /**
     * Additional audited attributes names are added to the White list of
     * records
     * 
     * @param list
     *            white list of audited attributes names
     * @param comment
     *            Contains the a new audited attributed name to be added
     */
    protected void addToList(final Collection<String> list, final String comment) {

	if ((list != null) && !Util.isEmpty(comment)) {
	    final String[] audits = comment.trim().split(",");
	    for (final String audit : audits) {
		final String auditTrim = audit.trim();
		if (!Util.isEmpty(auditTrim)) {
		    list.add(auditTrim);
		}
	    }
	}
    }

    /**
     * Rule to retrieve identified changes for a given <i>Trade</i>
     * 
     * @param changes
     *            list of changes to be notified
     * @param onlyNames
     *            if stored changes are only names or all details
     * @param trade
     *            involved <i>Trade</i>
     * @param oldTrade
     *            old version of involved <i>Trade</i>
     * @param tradeIgnoredChanges
     *            list of <i>Trade</i> authorized changes
     * @param productIgnoredChanges
     *            list of <i>Product</i> authorized changes
     * @param feeTypesIgnored
     *            list of <i>Fees types</i> authorized changes
     * @param ds
     *            data server connection
     * @return true if changes are ignored
     */
    public boolean ignoreChanges(final Collection<String> changes,
	    final boolean onlyNames, final Trade trade, final Trade oldTrade,
	    final Collection<String> tradeIgnoredChanges,
	    final Collection<String> productIgnoredChanges,
	    final Collection<String> feeTypesIgnored, final DSConnection ds) {
	if ((trade == null) || (oldTrade == null)) {
	    return true;
	}
	boolean ignoreChange = true;
	ignoreChange &= AuditUtil.getInstance().ignoreChanges(changes,
		onlyNames, trade, oldTrade, tradeIgnoredChanges);
	ignoreChange &= AuditUtil.getInstance().ignoreChangesProduct(changes,
		onlyNames, trade.getProduct(), oldTrade.getProduct(),
		productIgnoredChanges);
	return ignoreChange;
    }

    /**
     * Rule to retrieve identified changes for a given <i>Product</i>
     * 
     * @param changes
     *            list of changes to be notified
     * @param onlyNames
     *            if stored changes are only names or all details
     * @param product
     *            involved <i>Product</i>
     * @param oldProduct
     *            old version of involved <i>Product</i>
     * @param productIgnoredChanges
     *            list of <i>Product</i> authorized changes
     * @param ds
     *            data server connection
     * @return true if changes are ignored
     */
    protected boolean ignoreChangesProduct(final Collection<String> changes,
	    final boolean onlyNames, final Product product,
	    final Product oldProduct,
	    final Collection<String> productIgnoredChanges) {
	if ((product == null) || (oldProduct == null)) {
	    return true;
	}
	boolean ignore = true;
	if (!product.hasSecondaryMarket()
		|| (product instanceof FXProductBased)) {
	    ignore = AuditUtil.getInstance().ignoreChanges(changes, onlyNames,
		    product, oldProduct, productIgnoredChanges);
	}
	return ignore;
    }

    /**
     * Rule to retrieve identified changes for a given <i>Auditable</i>. E.g.
     * <i>BOMessage</i>, <i>BOTransfer</i>
     * 
     * @param changes
     *            list of changes to be notified
     * @param onlyNames
     *            if stored changes are only names or all details
     * @param auditable
     *            involved <i>Auditable</i>
     * @param oldAuditable
     * @param ignoredChanges
     *            list of <i>Auditable</i> authorized changes
     * @return true if changes are ignored
     */
    public boolean ignoreChanges(final Collection<String> changes,
	    final boolean onlyNames, final Auditable auditable,
	    final Auditable oldAuditable,
	    final Collection<String> ignoredChanges) {
	if ((auditable == null) || (oldAuditable == null)) {
	    return true;
	}
	Auditable audited = auditable;
	try {
	    final boolean isCacheable = audited instanceof CacheableObject;
	    audited = isCacheable ? (Auditable) ((CacheableObject) audited)
		    .cloneIfImmutable() : audited;
	} catch (final CloneNotSupportedException e) {
	    Log.error("calypsox.tk.core.AuditUtil", e);
	}

	final Vector<AuditValue> audits = new Vector<AuditValue>();
	final int oldVersion = audited.getVersion();
	audited.doAudit(oldAuditable, audits);
	audited.setVersion(oldVersion);

	boolean ignoreChange = true;
	for (final AuditValue audit : audits) {
	    final boolean ignore = !Util.isEmpty(ignoredChanges)
		    && AuditUtil.getInstance().ignoreChanges(audit,
			    ignoredChanges);
	    ignoreChange &= ignore;
	    if (!ignore && (changes != null)) {
		changes.add(onlyNames ? audit.getFieldName() : audit.toString());
	    }
	}
	return ignoreChange;
    }

    /**
     * Extended list of authorized changes
     * 
     * @param audit
     *            attribute to be checked, if change is ignored or not
     * @param ignoredChanges
     *            list of ignored changes
     * @return true if changes are ignored
     */
    public boolean ignoreChanges(final AuditValue audit,
	    final Collection<String> ignoredChanges) {

	if (audit != null) {
	    final FieldModification fm = audit.getField();
	    if ((fm == null) || (fm.getName() == null)) {
		return false;
	    }
	    if (fm.getName().indexOf("authorized") >= 0) {
		return true;
	    }
	    String fieldName = audit.getFieldName();
	    final int index = fieldName.lastIndexOf("#");
	    if (index != -1) {
		fieldName = fieldName.substring(index + 1, fieldName.length());
	    }
	    if ((ignoredChanges != null) && ignoredChanges.contains(fieldName)) {
		return true;
	    }
	}
	return false;
    }
}
