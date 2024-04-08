package calypsox.tk.collateral.marginCall.reader;

import java.io.InputStream;

import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;

public class MarginCallReaderFactory {

	private static MarginCallReaderFactory instance = null;
	private ExternalMarginCallImportContext context = null;

	/**
	 * @param context
	 */
	public MarginCallReaderFactory(ExternalMarginCallImportContext context) {
		this.context = context;
	}

	/**
	 * @param context
	 * @return
	 */
	public static synchronized MarginCallReaderFactory getInstance(
			ExternalMarginCallImportContext context) {
		if (instance == null) {
			return new MarginCallReaderFactory(context);
		}
		return instance;
	}

	/**
	 * @param mcBean
	 * @return
	 */
	public ExternalMarginCallReader getMarginCallMapper(InputStream is) {
		if (is != null) {
			return new FileExternalMarginCallReader(is, context);
		}
		return null;
	}

}
