package calypsox.apps.importer.adapter;

import calypsox.apps.refdata.CustomBookValidator;
import calypsox.processing.BookPreTranslator;
import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.infra.util.Util;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.processing.error.MessageType;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.UserAccessDiff;
import com.calypso.tk.refdata.UserAccessPermission;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.PersistenceSession;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Importer Adapter to process the books XML read from an message
 *
 * @author EULA & Guillermo Solano
 */
public class SantanderDefaultBookImporterAdapter extends SantanderDefaultLegalEntityImporterAdapter {

    /*
     * Constant to define the long alias attributes for SUSI & GBO
     */
    private final static String GBO_LONG_ALIAS_ATTRIBUTE = "ALIAS_BOOK_GBO_LONG";
    private final static String SUSI_LONG_ALIAS_ATTRIBUTE = "ALIAS_BOOK_SUSI_LONG";
    
    private static final String CURRENCY_BY_PO = "CURRENCY_BY_PO";

    /**
     * Default Constructor
     *
     * @throws AdapterException
     */
    public SantanderDefaultBookImporterAdapter() throws AdapterException {
        super();
    }

    @Override
    protected void preTranslate(final com.calypso.jaxb.xml.Object jaxbObject) throws ErrorMessage {

        // Need to write Book Translator and call here.
        final BookPreTranslator preProcessor = new BookPreTranslator();
        preProcessor.process(jaxbObject);
        super.preTranslate(jaxbObject);

    }

    /**
     * This method will translate the jaxbObject and save the translation using the persistenceSession.
     *
     * @param jaxbObject         the object to import.
     * @param persistenceSession the PersistenceSession to use to save the translated object.
     * @throws ErrorMessage if a translation or persistence error occurs.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void importObject(final com.calypso.jaxb.xml.Object jaxbObject,
                                final PersistenceSession persistenceSession) throws ErrorMessage {
        Book bookTranslated = null;
        try {
            preTranslate(jaxbObject);
            Object translated = translateObject(jaxbObject, persistenceSession);
            this.importedObjects.add(translated);

            if (translated instanceof Book) {

                bookTranslated = (Book) translated;
                
                /* Updates the currency of the book, if the ProcessingOrg of this book 
                 * is inside the Domain Name 'CURRENCY_BY_PO' */ 
                updateCurrencyIfNecessary(bookTranslated);
                
                // GSM: 21/06/2013. short/long alias Adaptation. DDR Distribución de Portfolios On-line v1.0
                translated = addLongAliasAttribute(bookTranslated);
            }

            persistenceSession.save(translated);

            // copy bookBundle and caAdjustBook attributes from PO attributes
            try {
                Book book = DSConnection.getDefault().getRemoteReferenceData().getBook(bookTranslated.getName());
                boolean save = false;

                if ((book != null) && Util.isEmpty(book.getAttribute(CustomBookValidator.BOOK_BUNDLE))) {
                    if (CustomBookValidator.setBookBundleAttribute(book, new Vector())) {
                        save = true;
                    }
                }
                if ((book != null) && Util.isEmpty(book.getAttribute(CustomBookValidator.CA_ADJUST_BOOK))) {
                    if (CustomBookValidator.setBookCaAdjustBookAttribute(book)) {
                        save = true;
                    }
                }
                if (save){
                    DSConnection.getDefault().getRemoteReferenceData().save(book);
                }
            } catch (RemoteException e1) {
                throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e1);
            }

            // We do a post process, generating and saving the contact information.
            if (null != bookTranslated) {
                UserAccessPermission oldAcc = AccessUtil.getAllUserAccess();
                UserAccessPermission newAcc = null;
                Book book;
                try {
                    book = DSConnection.getDefault().getRemoteReferenceData().getBook(bookTranslated.getName());
                } catch (RemoteException e1) {
                    throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e1);
                }

                Vector vect = new Vector();
                Vector diffs = new Vector();
                vect.add("__ALL__");

                try {
                    newAcc = oldAcc.clone();
                } catch (Exception ex) {
                    Log.error(this, ex); //sonar purpose
                }

                newAcc.setBookCurrencies(book, vect);
                newAcc.setBookCurrencyPairs(book, vect);
                newAcc.setBookProducts(book, vect);

                int oldVersion = oldAcc.getVersion();

                newAcc.setVersion(++oldVersion);
                newAcc.diff(oldAcc, diffs, DSConnection.getDefault().getUser(), new JDatetime(), true);

                if (diffs.size() > 0) {
                    PendingModification pm = (PendingModification) diffs.elementAt(0);
                    UserAccessDiff uad = (UserAccessDiff) pm.getFieldObjectValue();

                    try {
                        uad.setNewEnteredUser(DSConnection.getDefault().getUser());
                        DSConnection.getDefault().getRemoteAccess().save(uad);
                    } catch (Exception e) {
                        throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e);
                    }
                }
            }
        } catch (final PersistenceException e) {
            throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e);
        } catch (final SecurityException e) {
            throw new ErrorMessage(jaxbObject, MessageType.SECURITY_ERROR, e);
        }
    }
    
    /**
     * Updates the currency of the book, if the ProcessingOrg of 
     * this book is inside the Domain Name 'CURRENCY_BY_PO'
     * 
     * @param book
     */
    private void updateCurrencyIfNecessary(Book book) {
    	final String processingOrgShortName = getProcessingOrgOfBook(book);
    	
    	Vector<String> listOfPO = getDomainValuesOfDomainName(CURRENCY_BY_PO);
    	
    	if ((listOfPO != null) && (listOfPO.contains(processingOrgShortName))) {
    		String currencyOfPO = getDomainValueComment(CURRENCY_BY_PO, processingOrgShortName);
    		
    		if (!Util.isEmpty(currencyOfPO)) {
    			// Update base currency
    			book.setBaseCurrency(currencyOfPO);
    		}
    	}
    }
    
    /**
     * Retrieves the short name of the ProccesingOrg of the Book
     * 
     * @param book
     * @return
     */
    private String getProcessingOrgOfBook(final Book book) {
    	String processingOrg = "";
    	
    	if ((book.getLegalEntity() != null) && (book.getLegalEntity().getCode() != null)) {
    		processingOrg = book.getLegalEntity().getCode();
    	}
    	
    	return processingOrg;
    }

    /**
     * In case the book to be imported is going to be updated, as GER does not send the long name, this method recover
     * the long Aliases and adds the attributes to the book to be modified.
     *
     * @param bookTranslated
     * @return
     */
    private Object addLongAliasAttribute(Book bookTranslated) {

        final String bookNameId = bookTranslated.getName();

        if ((bookNameId == null) || bookNameId.isEmpty()) {
            return bookTranslated;
        }

        Book book;
        try {
            book = DSConnection.getDefault().getRemoteReferenceData().getBook(bookNameId);
        } catch (RemoteException e) {
            Log.error(this, e);//Sonar
            return bookTranslated;
        }

        // is a new book, nothing to be done
        if (book == null) {
            return bookTranslated;
        }

        // read long attributes from DB book
        final String gboLong = giveBookAttribute(book, GBO_LONG_ALIAS_ATTRIBUTE);
        final String susiLong = giveBookAttribute(book, SUSI_LONG_ALIAS_ATTRIBUTE);

        // copy them into the book to be imported
        bookTranslated.setAttribute(GBO_LONG_ALIAS_ATTRIBUTE, gboLong);
        bookTranslated.setAttribute(SUSI_LONG_ALIAS_ATTRIBUTE, susiLong);

        return bookTranslated;
    }

    /**
     * Ensures to return at least an empty String if the attribute is null
     *
     * @param book
     * @param longAliasAttribute
     * @return the attribute of the book
     */
    private String giveBookAttribute(Book book, String longAliasAttribute) {

        final String attributeValue = book.getAttribute(longAliasAttribute);

        if (attributeValue == null) {
            return "";
        }

        return attributeValue;
    }
    
	/**
	 * Retrieve the Domain Values of a Domain Name using cache
	 *
	 * @param domainName
	 * @return
	 */
	protected Vector<String> getDomainValuesOfDomainName(final String domainName) {
		return LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
	}

	/**
	 * Retrieve the comment of a Domain Value
	 *
	 * @param domainName
	 * @param domainValue
	 * @return
	 */
	protected String getDomainValueComment(final String domainName, final String domainValue) {
		String domainValueComment = "";

		domainValueComment = getDomainValueCommentWithCache(domainName, domainValue);

		domainValueComment = ifStringNullSetEmpty(domainValueComment);

		return domainValueComment;
	}

	/**
	 * Retrieve the comment of a Domain Value using the cache
	 *
	 * @param domainName
	 * @param domainValue
	 * @return
	 */
	protected String getDomainValueCommentWithCache(final String domainName, final String domainValue) {
		return LocalCache.getDomainValueComment(DSConnection.getDefault(), domainName, domainValue);
	}

	/**
	 * If the string is null, it returns empty.
	 *
	 * @param stringParam
	 * @return
	 */
	protected String ifStringNullSetEmpty(final String stringParam) {
		String result = stringParam;

		if (result == null) {
			result = "";
		}

		return result;
	}

}
