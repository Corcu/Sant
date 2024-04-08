/**
 * 
 */
package calypsox.tk.collateral.allocation.reader;

import java.io.FileInputStream;
import java.io.InputStream;

import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;

import com.sun.xml.bind.StringInputStream;

/**
 * 
 * Factory to get allocation reader from the being imported alloc bean
 * 
 * @author aela
 * 
 */
public class AllocationReaderFactory {

	private static AllocationReaderFactory instance = null;
	private  ExternalAllocationImportContext context = null;
	
	/**
	 * @param context
	 */
	public AllocationReaderFactory(ExternalAllocationImportContext context) {
		this.context = context;
	}

	/**
	 * @param context
	 * @return
	 */
	public static synchronized AllocationReaderFactory getInstance(ExternalAllocationImportContext context) {
		if (instance == null) {
			return new AllocationReaderFactory(context);
		}
		return instance;
	}

	/**
	 * @param allocBean
	 * @return
	 */
	public ExternalAllocationReader getAllocationMapper(InputStream is) {
		if(is !=null) {
			if (is instanceof StringInputStream) {
				return new StringExternalAllocationReader(is, context);
			} else if(is instanceof FileInputStream) {
				return new FileExternalAllocationReader(is, context);
			}
		}
		return null;
	}


}
