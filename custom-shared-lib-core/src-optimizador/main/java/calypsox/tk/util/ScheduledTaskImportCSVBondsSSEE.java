package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.service.RemoteProduct;

public class ScheduledTaskImportCSVBondsSSEE extends AbstractProcessFeedScheduledTask {

	private static final long serialVersionUID = 123L;

	private static final String ISIN = "ISIN";

	private static final String SEPARATOR_DOMAIN_STRING = "Separator";

	private static final String TASK_INFORMATION = "Import Market Data Bonds from a CSV file.";

	private static final String TIME_FORMAT = "Time Format";

	private static final String QUOTE_SET = "QuoteSet";

	protected static final String PROCESS = "Load bond prices";

	private static final String DIRTY_PRICE = "DirtyPrice";

	private static final String CLEAN_PRICE = "CleanPrice";

	private RemoteMarketData remoteMarketData;

	private RemoteProduct remoteProduct;

	private BufferedReader inputFileStream;

	private String file = "";

	private boolean proccesOK = true;

	private boolean controlMOK = true;

	private static final int NUMBER_FIELDS = 9;

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	/**
	 * ST Attributes Definition
	 */
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
		attributeList.add(attribute(TIME_FORMAT));
		attributeList.add(attribute(QUOTE_SET)
				.domain(new ArrayList<String>(Arrays.asList(new String[] { DIRTY_PRICE, CLEAN_PRICE }))));

		return attributeList;
	}

//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(SEPARATOR_DOMAIN_STRING);
//		attr.add(TIME_FORMAT);
//		attr.add(QUOTE_SET);
//		return attr;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getAttributeDomain(String attr, @SuppressWarnings("rawtypes") Hashtable currentAttr) {
//		if (QUOTE_SET.equals(attr)) {
//			Vector<String> v = new Vector<String>();
//			v.add(DIRTY_PRICE);
//			v.add(CLEAN_PRICE);
//			return v;
//		}
//		return super.getAttributeDomain(attr, currentAttr);
//	}

	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		final String path = getAttribute(FILEPATH);
		final String startFileName = getAttribute(STARTFILENAME);
		final String quoteSet = getAttribute(QUOTE_SET);
		Integer quotePosFile = getQuotePositionFile(quoteSet);
		SimpleDateFormat timeFormat = new SimpleDateFormat(getAttribute(TIME_FORMAT));
		String date = timeFormat.format(getValuationDatetime());

		final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName + date);

		this.remoteProduct = conn.getRemoteProduct();
		this.remoteMarketData = conn.getRemoteMarketData();

		if (files.size() == 1) {
			this.file = files.get(0);
			final String filePath = path + this.file;

			// FileUtility.copyFileToDirectory(filePath, path + "/copy/");

			String line = null;
			String[] values = null;
			final HashMap<String, String[]> importedQuotes = new HashMap<String, String[]>();
			Vector<QuoteValue> quoteValues = new Vector<QuoteValue>();
			Vector<Product> bonds = null;
			boolean stopFile = false;
			int importedOK = 0;
			int i = 0;
			try {
				this.inputFileStream = new BufferedReader(new FileReader(filePath));

				for (i = 0; !stopFile && ((line = this.inputFileStream.readLine()) != null); i++) {

					if (!line.contains("*****") && !line.isEmpty()) {

						if (CollateralUtilities.checkFields(line, '|', NUMBER_FIELDS)) {

							values = CollateralUtilities.splitMejorado(NUMBER_FIELDS, "|", true, line);
							for (int ii = 0; ii < values.length; ii++) {
								values[ii] = values[ii].trim();
							}

							quoteValues = new Vector<QuoteValue>();
							if (!importedQuotes.containsKey(values[0])) {
								try {
									if (areCorrectValues(values)) {

										importedQuotes.put(values[0], values);

										bonds = getBonds(values[0], i);

										if (!Util.isEmpty(bonds)) {

											if (addQuoteValuesPerLine(line, values, importedQuotes, quoteValues, bonds,
													quoteSet, quotePosFile, i)) {

												importedOK = saveQuoteValues(values, quoteValues, importedOK, i);
											}
										}

									} else {
										Log.error(this,
												"Line " + (i + 1) + ": The values of the line are not correct.");
										// this.proccesOK = false;
									}

								} catch (final NumberFormatException e) {
									Log.error(this, "NumberFormatException. " + e.toString());
									this.proccesOK = false;
								}

							} else {
								Log.error(this, "Line " + (i + 1) + ":Duplicate isin " + values[0] + " in the file.");
								// this.proccesOK = false;
							}

						} else {
							Log.error(this, "Line " + (i + 1) + ":Error checking the number of fields.");
							this.proccesOK = false;
						}

					} else {
						stopFile = true;
					}

				}
			} catch (final RemoteException e) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while saving trades", e);

				this.proccesOK = false;
			} catch (final FileNotFoundException e) {
				// critical error 1
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + filePath, e);
				this.proccesOK = false;
				ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
				this.controlMOK = false;
			} catch (final IOException e) {
				// Unexpected error opening the file.
				// Critical error 2
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + filePath, e);
				this.proccesOK = false;
				ControlMErrorLogger.addError(ErrorCodeEnum.IOException, "Unexpected error opening the file");
				this.controlMOK = false;
			} finally {
				if (this.inputFileStream != null) {
					try {
						this.inputFileStream.close();
					} catch (final IOException e) {
						Log.error(LOG_CATEGORY_SCHEDULED_TASK,
								"Error while trying close input stream for the CSV file <" + getFileName()
										+ "> open previously",
								e);
						ControlMErrorLogger.addError(ErrorCodeEnum.IOException, "Unexpected error closing the file");
						this.controlMOK = false;
					}
				}
			}
			Log.system(LOG_CATEGORY_SCHEDULED_TASK, importedOK + " Quotes imported OK of " + i + " lines.");
		} else {

			StringBuffer buffer = new StringBuffer();
			buffer.append("The number of matches for the filename in the path ");
			buffer.append("specified is 0 or greater than 1. Please fix the problem.");
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, buffer.toString());
			ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, buffer.toString());
			this.proccesOK = false;
			this.controlMOK = false;
		}

		if (this.controlMOK) {
			ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
		}

		return this.proccesOK;

	}

	/*
	 * Return quote position file depending of the quoteSet.
	 */
	private Integer getQuotePositionFile(String quoteSet) {
		if (CLEAN_PRICE.equals(quoteSet)) {
			return 1;
		} else if (DIRTY_PRICE.equals(quoteSet)) {
			return 2;
		}
		return null;
	}

	/**
	 * 
	 * @param values
	 * @param quoteValues
	 * @param importedOK
	 * @return
	 */
	private int saveQuoteValues(String[] values, Vector<QuoteValue> quoteValues, int importedOK, int i) {
		try {
			this.remoteMarketData.saveQuoteValues(quoteValues);

			for (QuoteValue qv : quoteValues) {
				Log.system("", "Line " + (i + 1) + ":ISIN " + values[0] + " Imported OK: " + qv.getName() + "[ "
						+ qv.getQuoteType() + "-" + qv.getClose() + " ]");
			}
			importedOK++;
		} catch (final RemoteException e) {
			Log.error(this, "Error saving quote Values." + e.toString());
			Log.error(this, e); //sonar
			this.proccesOK = false;
		}

		return importedOK;
	}

	/**
	 * Add Quotes CleanPrice and DirtyPrice for same ISIN
	 * 
	 * @param line
	 * @param values
	 * @param importedQuotes
	 * @param quoteValues
	 * @param bonds
	 * @param quoteSet
	 * @param i
	 * @param i
	 */
	private boolean addQuoteValuesPerLine(String line, String[] values, final HashMap<String, String[]> importedQuotes,
			Vector<QuoteValue> quoteValues, Vector<Product> bonds, String quoteSet, int quotePosFile, int i) {
		boolean res = true;
		QuoteValue qv;

		if (bonds.size() == 1) {
			qv = createQuoteValues(importedQuotes, bonds, this._currentDate, i + 1, line, values[0], quoteSet,
					quotePosFile);
			if (qv != null) {
				quoteValues.add(qv);
			} else {
				res = false;
			}
		} else {
			// Error cause there are more than one bond for a unique Isin

			StringBuffer str = new StringBuffer();
			res = false;
			int j = 0;

			for (Product product : bonds) {
				if (j != 0) {
					str.append(",");
				}
				str.append(product.getId());
				j++;
			}
			Log.error(LOG_CATEGORY_SCHEDULED_TASK,
					"Line " + (i + 1) + ":Error: More than one bond found by a unique Isin. Isin = " + values[0]
							+ ". Bond products found [" + str.toString() + "]");
		}

		return res;
	}

	/**
	 * true if values are correct
	 * 
	 * @param values
	 * @return
	 */
	private boolean areCorrectValues(String[] values) {

		String quoteSet = getAttribute(QUOTE_SET);
		Boolean isin = !values[0].isEmpty();
		Boolean cleanPrice = Double.valueOf(values[1]) > 0;
		Boolean dirtyPrice = Double.valueOf(values[2]) > 0;

		if (!Util.isEmpty(quoteSet)) {
			if (quoteSet.equals(DIRTY_PRICE)) {
				return isin && dirtyPrice;
			}
			if (quoteSet.equals(CLEAN_PRICE)) {
				return isin && cleanPrice;
			}
		}
		return false;

	}

	/**
	 * Method that creates the new Quote Values to insert into Calypso, in the
	 * specified date.
	 * 
	 * @param importedQuotes
	 *            Hashmap object with the information about the lines read from
	 *            the file to import.
	 * @param bonds
	 *            Bonds retrieved from the database.
	 * @param jdate
	 *            Date in which we import the prices for the bonds.
	 * @param isin
	 * @return A vector with the new Quote Values created to insert into
	 *         Calypso.
	 */
	private QuoteValue createQuoteValues(final HashMap<String, String[]> importedQuotes, final Vector<Product> bonds,
			final JDate jdate, final int line, final String stringLine, String isin, String quoteType, int value) {
		QuoteValue result = null;

		for (final Product bond : bonds) {
			
			if(!Util.isEmpty(bond.getQuoteName())) {
				result = new QuoteValue(quoteType, bond.getQuoteName(), jdate, quoteType);
				result.setClose(new Double(importedQuotes.get(bond.getSecCode(ISIN))[value]).doubleValue() / 100);
			}else {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while doing the conversion between the ISIN "
						+ bond.getSecCode(ISIN) + " and its quote name");
				this.proccesOK = false;
			}
		}

		return result;
	}

	/**
	 * This method calls other one to retrieve the Bonds from Calypso.
	 * 
	 * @param importedQuotes
	 *            Hashmap object with the information about the lines read from
	 *            the file to import.
	 * @return A vector with the bonds retrieved from the system.
	 * @throws RemoteException
	 *             Exception occurred when there is an error while trying to
	 *             obtain the bonds from the system with the ISIN codes
	 *             specified into the file to import.
	 */
	private Vector<Product> getBonds(String isin, int i) throws RemoteException {

		return getBondsFromDatabase(isin, i);
	}

	/**
	 * Method used to retrieve from the database of our system (Calypso) the
	 * bonds with the ISIN codes specified into the file to import.
	 * 
	 * @param localQuoteHashMap
	 *            Hashmap with the information about the lines read from the
	 *            file to import.
	 * @return A vector with all bonds retrieved from the database.
	 * @throws RemoteException
	 *             Exception occurred when there is an error while trying to
	 *             obtain the bonds from the system with the ISIN codes
	 *             specified into the file to import.
	 */
	@SuppressWarnings("unchecked")
	protected Vector<Product> getBondsFromDatabase(String isin, int i) {
		final Vector<Product> products = new Vector<Product>();

		try {
			products.addAll(this.remoteProduct.getProductsByCode("ISIN", isin));
		} catch (final Exception e) {
			Log.error(this, "Line " + (i + 1) + ":No product found for this Isin " + isin + ".");
			Log.error(this, e); //sonar
			//this.proccesOK = false;
		}

		return products;
	}

	@Override
	public String getFileName() {
		return this.file;
	}
}
