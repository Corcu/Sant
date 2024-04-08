/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util;

import java.rmi.RemoteException;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

public class GenericValidations {
	@SuppressWarnings("unused")
	private static final String ISIN = "ISIN";
	private static final String BOOK_WARNING = "BOOK_WARNING";
	private static final String PRODUCT_OK = "PRODUCT_OK";
	private static final String NO_PRODUCT = "NO_PRODUCT";
	private static final String DIFFERENT_CCYS = "DIFFERENT_CCYS";
	private static final String NO_ISIN = "NO_ISIN";
	private static final String BOND = "BOND";
	private static final String EQUITY = "EQUITY";

	/**
	 * To validate if there is a Legal Entity in the system identified by the String passed as a parameter, PO, CP,
	 * Agent, etc.
	 * 
	 * @param legalEntity
	 *            String identifying the Legal Entity.
	 * @return TRUE if exists, FALSE if does not exist.
	 * @throws RemoteException
	 *             Problem accessing to the database.
	 */
	public static boolean validateLegalEntity(final String legalEntity) throws RemoteException {
		final LegalEntity le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(legalEntity);
		if (le == null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * To validate if exists a book in the system using the internal mapping in Calypso. For instance, we need to do a
	 * mapping between the book obtained from SUSI and the Calypso one.
	 * 
	 * @param sourceBook
	 *            Book provided by the source system.
	 * @param alias
	 *            Alias used to identify the source system, to do the mapping.
	 * @return TRUE if exists, FALSE if does not exist.
	 * @throws RemoteException
	 *             Problem accessing to the database.
	 */
	public static boolean validatePortfolioMapped(final String sourceBook, final String alias) throws RemoteException {
		final String bookMapped = CollateralUtilities.getBookMapped(sourceBook, alias);
		if ((null != bookMapped) && !"".equals(bookMapped) && !bookMapped.startsWith(BOOK_WARNING)) {
			// We get the BOOK from the system.
			final Book book = DSConnection.getDefault().getRemoteReferenceData().getBook(bookMapped);
			if (book == null) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	// GSM: 13/06/2013. Short Portfolio name DEV. Read first short name
	// In case short is not found, try long name
	/**
	 * To validate if exists a book in the system using the internal mapping in Calypso. For instance, we need to do a
	 * mapping between the book obtained from SUSI and the Calypso one. It will first check for the Short alias name for
	 * the book, in case we don't have a match, it will try to long name.
	 * 
	 * @param longBookName
	 *            long book name provided by the source system.
	 * @param bookName
	 *            short book name provided by the source system.
	 * @param aliasLong
	 *            long Alias used to identify the source system, to do the mapping.
	 * @param aliasShort
	 *            short Alias used to identify the source system, to do the mapping.
	 * @return TRUE if exists, FALSE if does not exist.
	 * @throws RemoteException
	 *             Problem accessing to the database.
	 */
	public static boolean validatePortfolioMapped(final String bookName, final String aliasLong, final String aliasShort)
			throws RemoteException {

		Book mapped = null;

		if ((bookName == null) || bookName.isEmpty()) {
			return false;
		}

		try {

			if ((aliasShort != null) && !aliasShort.isEmpty()) {
				// GSM: 13/06/2013. Short Portfolio name DEV. Read first short name
				mapped = CollateralUtilities.mapBook(bookName, aliasShort);
			}

		} catch (Exception e) {
			Log.error(GenericValidations.class, e); //sonar
			if ((aliasLong != null) && !aliasLong.isEmpty()) {
				// GSM: 13/06/2013. In case short is not found, try long name
				try {

					mapped = CollateralUtilities.mapBook(bookName, aliasLong);

				} catch (Exception e1) {
					// not short or long name found
					Log.error(GenericValidations.class, e1);
					return false;
				}
			}
		}

		// if book found:
		return (mapped != null);

	}

	/**
	 * To validate if exists or not the ISIN in the system.
	 * 
	 * @param isin
	 *            String identifying the ISIN we want to search.
	 * @return TRUE if exists, FALSE in the other case.
	 * @throws RemoteException
	 *             Problem accessing to the database.
	 */
	@SuppressWarnings("unchecked")
	public static String validateIsin(String productType, final String isin, String ccy) throws RemoteException {
		boolean isCorrectType = false;
		final Vector<Product> products = DSConnection.getDefault().getRemoteProduct().getProductsByCode("ISIN", isin);
		if (!Util.isEmpty(products)) {
			for (int j = 0; j < products.size(); j++) {
				final Product product = products.get(j);
				if ((product instanceof Bond) && (productType.equals(BOND))) {
					isCorrectType = true;
					if (product.getCurrency().equals(ccy)) {
						return PRODUCT_OK + j;
					}
				}
				if ((product instanceof Equity) && (productType.equals(EQUITY))) {
					isCorrectType = true;
					if (product.getCurrency().equals(ccy)) {
						return PRODUCT_OK + j;
					}
				}
			}
			if (isCorrectType) {
				return DIFFERENT_CCYS;
			} else {
				return NO_PRODUCT;
			}
		}
		return NO_ISIN;

	}
}