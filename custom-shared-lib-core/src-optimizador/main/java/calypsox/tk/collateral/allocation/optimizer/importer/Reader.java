package calypsox.tk.collateral.allocation.optimizer.importer;

public interface Reader<T> {

	/**
	 * from a flat line builds the bean containing the "line" .
	 */
	T readLine(String record) throws Exception;
}
