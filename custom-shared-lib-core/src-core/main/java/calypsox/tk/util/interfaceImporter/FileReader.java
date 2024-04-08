package calypsox.tk.util.interfaceImporter;

/**
 * Read line interface
 * 
 * @author aela
 * 
 */
interface FileReader<T> {

	/**
	 * from a flat line builds the bean containing the "line" calypso object.
	 */
	T readLine(String record, String spliter, int lineNb, boolean useControlLine) throws Exception;
}
