package calypsox.apps.refdata.pmm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.BookAttribute;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Util;

import calypsox.apps.refdata.PhilsUploader;

public class BookPMM implements PMMHandlerInterface {

	@Override
	public boolean additionalProcessing() {
		return false;
	}

	@Override
	public boolean saveElements() {
		int count = 0;
		for (Map.Entry<String, Object> entry : PMMCommon.MODIFIED_ELEMENTS.entrySet()) {
			Book book = (Book)entry.getValue();
			handleBookAttributes(book);
			
			try {
				PMMCommon.DS.getRemoteReferenceData().save(book);
			} catch (CalypsoServiceException e) {
				String msg = "Error saving element : " + e.toString();
				PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
				continue;
			}
			if (count % 100 == 0 && count > 0) {
				PhilsUploader.addTextToTextArea("Saved " + count + "/" + PMMCommon.MODIFIED_ELEMENTS.size() + " elements.", PMMCommon.TYPE_INFO, 2);
			}
			count++;
		}
		
		return false;
	}
	
	private void handleBookAttributes(Book book) {
		Vector<BookAttribute> bookAttrs = book.getAttributes();
		List<BookAttribute> finalBookAttrs = removeDuplicateBookAttributes(book, bookAttrs);
		book.setAttributes(finalBookAttrs);
	}
	
	private List<BookAttribute> removeDuplicateBookAttributes(Book book, Vector<BookAttribute> bookAttrs) {
		List<BookAttribute> finalBookAttrs = new ArrayList<BookAttribute>();
		Vector<String> finalAttributesNames = new Vector<String>();
		
		// First, add all new Attributes
		for (BookAttribute bookAttr : bookAttrs) {
			// new Book Attrs have a Null Book, we identify them thanks to that
			if (bookAttr.getBook() == null) {
				bookAttr.setBook(book);
				finalBookAttrs.add(bookAttr);
				finalAttributesNames.add(bookAttr.getName());
			}
		}
		
		// Then add the rest
		for (BookAttribute bookAttr : bookAttrs) {
			if (!finalAttributesNames.contains(bookAttr.getName())) {
				finalBookAttrs.add(bookAttr);
				finalAttributesNames.add(bookAttr.getName());
			}
		}
		
		return finalBookAttrs;
	}
	
	@Override
	public boolean deleteElements() {
		int count = 0;

		Vector<Book> books = new Vector<Book>();
		try {
			List<List<String>> splitCollection = PMMCommon.splitCollection(PMMCommon.ELEMENTS_IDENTIFIERS, 999);
			for (List<String> wantedElementsList : splitCollection) {
				if (Util.isEmpty(wantedElementsList)) {
					continue;
				}
				
				String where = "BOOK.BOOK_ID IN " + Util.collectionToSQLString(wantedElementsList);
				Vector<Book> contactsSplit = (Vector<Book>)PMMCommon.DS.getRemoteReferenceData().getBooks(null, where, null);
				books.addAll(contactsSplit);
			}
		} catch (CalypsoServiceException e) {
			String msg = "Error retrieving elements to delete : " + e.toString();
			PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
			return true;
		}
		
		for (Book entry : books) {
			try {
				PMMCommon.DS.getRemoteReferenceData().remove(entry);
			} catch (CalypsoServiceException e) {
				String msg = "Error deleting element : " + e.toString();
				PhilsUploader.addTextToTextArea(msg, PMMCommon.TYPE_ERROR, 2);
				continue;
			}
			if (count % 100 == 0 && count > 0) {
				PhilsUploader.addTextToTextArea("Deleted " + count + "/" + PMMCommon.ELEMENTS_IDENTIFIERS.size() + " elements.", PMMCommon.TYPE_INFO, 2);
			}
			count++;
		}
		
		return false;
	}

	@Override
	public Vector<?> loadElements(List<String> list) throws CalypsoServiceException {
		List<Book> books = null; 
		switch (PMMCommon.IDENTIFIER_NAME) {
		case "ID":
			List<Integer> intList = PMMCommon.convertList(list, s -> Integer.parseInt(s));
			books = BOCache.getBooksFromBookIds(PMMCommon.DS, new Vector<Integer>(intList));
			break;
		case "NAME":
		case "SHORTNAME":
			books = BOCache.getBooksFromBookNames(PMMCommon.DS, new Vector<String>(list));
			break;
		default:
			books = new ArrayList<Book>();
			break;
		}
		
		for (Book book : books) {
			String identifier = null;
			if (!PMMCommon.IDENTIFIER_NAME.equals("ID")) {
				identifier = book.getAuthName();
			}
			else {
				identifier = String.valueOf(book.getLongId());
			}
			PMMCommon.MODIFIABLE_ELEMENTS.put(identifier, book);
		}
		
		return new Vector<>(books);
	}

	@Override
	public void checkLoadedElements(List<String> wantedElements, Vector<?> foundElements) {
		// TODO Auto-generated method stub
	}

	@Override
	public Class<?> getObjectClass() {
		return com.calypso.tk.core.Book.class;
	}

	@Override
	public Object cloneObject(Object objectToClone) throws CloneNotSupportedException {
		return ((Book)objectToClone).clone();
	}

	@Override
	public Method getMethod(String methodName, Class<?> dataTypeClass) throws NoSuchMethodException {
		if (dataTypeClass != null) {
			return getObjectClass().getMethod(methodName, dataTypeClass);
		}
		return getObjectClass().getMethod(methodName);
	}
}
