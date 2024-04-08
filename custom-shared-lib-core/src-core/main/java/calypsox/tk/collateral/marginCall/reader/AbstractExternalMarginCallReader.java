package calypsox.tk.collateral.marginCall.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;

import com.calypso.tk.core.Log;

public abstract class AbstractExternalMarginCallReader implements
		ExternalMarginCallReader {

	protected InputStream is;
	protected ExternalMarginCallImportContext context;

	/**
	 * @param is
	 * @param context
	 */
	public AbstractExternalMarginCallReader(InputStream is,
			ExternalMarginCallImportContext context) {
		this.is = is;
		this.context = context;
	}

	/**
	 * 
	 */
	public AbstractExternalMarginCallReader() {
	}

	@Override
	public List<ExternalMarginCallBean> readMarginCalls(
			List<MarginCallImportErrorBean> errors) throws Exception {

		List<ExternalMarginCallBean> listExtMCs = new ArrayList<ExternalMarginCallBean>();

		if (is != null) {
			String line = "";
			ExternalMarginCallBean externalMarginCallBean = null;
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				if (is != null) {
					while ((line = reader.readLine()) != null) {
						externalMarginCallBean = readMarginCall(line, errors);
						if (externalMarginCallBean != null) {
							listExtMCs.add(externalMarginCallBean);
						}
					}
				}
			} finally {
				try {
					is.close();
				} catch (IOException ignore) {
					Log.error(this, ignore); // sonar
				}
			}
		}

		return listExtMCs;

	}

	@Override
	public abstract ExternalMarginCallBean readMarginCall(String line,
			List<MarginCallImportErrorBean> errors) throws Exception;
}
