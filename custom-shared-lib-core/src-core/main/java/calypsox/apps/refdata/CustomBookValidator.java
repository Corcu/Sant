/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.refdata;

import java.awt.Frame;
import java.util.Collection;
import java.util.Vector;

import com.calypso.apps.refdata.BookValidator;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.BookAttribute;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

public class CustomBookValidator implements BookValidator {

	public static final String BOOK_BUNDLE = "BookBundle";
	public static final String CA_ADJUST_BOOK = "CAAdjustBook";
	public static final String CA_BOOK = "CA_BOOK";
	public static final String CA_BOOK_SLB = "CA_BOOK_SLB";
	public static final String BDSD = "BDSD";

	@SuppressWarnings({ "rawtypes" })
	@Override
	public boolean isValidInput(Book book, Frame frame, Vector messages) {
		boolean ret = true;
		if ((book != null) && Util.isEmpty(book.getAttribute(BOOK_BUNDLE))) {
			ret &= setBookBundleAttribute(book, messages);
		}
		if ((book != null) &&  Util.isEmpty(book.getAttribute(CA_ADJUST_BOOK))) {
			ret &= setBookCaAdjustBookAttribute(book);
		}
		return ret;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean setBookBundleAttribute(Book book, Vector messages) {
		LegalEntity processingOrg = BOCache.getLegalEntity(DSConnection.getDefault(), book.getProcessingOrgBasedId());
		LegalEntityAttribute leAttrBundle = getLEBookBundleAttribute(BOCache.getLegalEntityAttributes(
				DSConnection.getDefault(), processingOrg.getId()));
		if (leAttrBundle == null) {
			String message = "No " + BOOK_BUNDLE + " value found for LE " + processingOrg.getName();
			messages.add(message);
			Log.error(CustomBookValidator.class.getName(), message);
			return false;
		}

		BookAttribute bookBundleAttr = getBookBundleAttribute(book.getAttributes());
		if (bookBundleAttr != null) {
			bookBundleAttr.setValue(leAttrBundle.getAttributeValue());
		} else {
			bookBundleAttr = new BookAttribute(BOOK_BUNDLE, leAttrBundle.getAttributeValue());
			bookBundleAttr.setBook(book);
			if (!Util.isEmpty(book.getAttributes())) {
				book.getAttributes().add(bookBundleAttr);
			} else {
				Vector<BookAttribute> bookAttrs = new Vector<BookAttribute>();
				bookAttrs.add(bookBundleAttr);
				book.setAttributes(bookAttrs);
			}
		}

		return true;
	}

	public static BookAttribute getBookBundleAttribute(Vector<BookAttribute> attributes) {
		for (BookAttribute bookAttr : attributes) {
			if (BOOK_BUNDLE.equals(bookAttr.getName())) {
				return bookAttr;
			}
		}
		return null;
	}

	public static LegalEntityAttribute getLEBookBundleAttribute(Collection<LegalEntityAttribute> leAttributes) {
		if (Util.isEmpty(leAttributes)) {
			return null;
		}
		for (LegalEntityAttribute legalEntityAttribute : leAttributes) {
			if (BOOK_BUNDLE.equals(legalEntityAttribute.getAttributeType())) {
				return legalEntityAttribute;
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean setBookCaAdjustBookAttribute(Book book) {

		BookAttribute bookBundleAttr = getCaAdjustBookAttribute(book.getAttributes());
		if (bookBundleAttr == null) {
			if (BDSD.equalsIgnoreCase(book.getLegalEntity().getCode()))
				bookBundleAttr = new BookAttribute(CA_ADJUST_BOOK, CA_BOOK_SLB);
			else
				bookBundleAttr = new BookAttribute(CA_ADJUST_BOOK, CA_BOOK);
			bookBundleAttr.setBook(book);
			if (!Util.isEmpty(book.getAttributes())) {
				book.getAttributes().add(bookBundleAttr);
			} else {
				Vector<BookAttribute> bookAttrs = new Vector<BookAttribute>();
				bookAttrs.add(bookBundleAttr);
				book.setAttributes(bookAttrs);
			}
		}

		return true;
	}

	public static BookAttribute getCaAdjustBookAttribute(Vector<BookAttribute> attributes) {
		for (BookAttribute bookAttr : attributes) {
			if (CA_ADJUST_BOOK.equals(bookAttr.getName())) {
				return bookAttr;
			}
		}
		return null;
	}

}
