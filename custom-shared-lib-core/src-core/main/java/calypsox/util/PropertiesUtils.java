/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.calypso.tk.core.Log;

/**
 * This class reads a properties file in any directory added to the classpath. Also includes some the option of loading
 * by default some properties and check the parsing of the properties.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 03/01/2014
 * 
 */
public class PropertiesUtils {

	/**
	 * Name of the properties file
	 */
	private final String propertiesFileName;
	/**
	 * Properties
	 */
	private Properties properties;
	/**
	 * List of names that should include the properties file (optional)
	 */
	private final List<String> propertiesNamesList;
	/**
	 * Default properties in case a parsing error has occurred
	 */
	private final List<String> propertiesDefaultValuesList;

	private boolean force;

	/**
	 * Main Constructor
	 * 
	 * @param propertiesFileName
	 *            name of the properties file
	 * @param propertiesList
	 *            list of names that must be in the file
	 * @param propertiesDefault
	 *            list of default values in case of error (in order of insertion)
	 */
	public PropertiesUtils(final String propertiesFileName, final List<String> propertiesList,
			final List<String> propertiesDefault) {

		this.propertiesFileName = propertiesFileName;
		this.propertiesNamesList = propertiesList;
		this.propertiesDefaultValuesList = propertiesDefault;
		this.properties = new Properties();
		this.force = false;
	}

	/**
	 * 
	 * ain Constructor
	 * 
	 * @param propertiesFileName
	 *            name of the properties file
	 * @param propertiesList
	 *            Array of names that must be in the file
	 * @param propertiesDefault
	 *            Array default values in case of error (in order of insertion)
	 */
	public PropertiesUtils(final String propertiesFileName, final String[] propertiesList,
			final String[] propertiesDefault) {

		this(propertiesFileName, new ArrayList<String>(Arrays.asList(propertiesList)), new ArrayList<String>(
				Arrays.asList(propertiesDefault)));
	}

	/**
	 * Constructor (won't add default properties if parsin error occurs).
	 * 
	 * @param propertiesFileName
	 *            name of the properties file
	 * @param propertiesList
	 *            list of names that must be in the file
	 */
	public PropertiesUtils(final String propertiesFileName, final List<String> propertiesList) {

		this(propertiesFileName, propertiesList, null);
	}

	/**
	 * Minimum Constructor. Not default properties in case of error and no properties name check will be done.
	 * 
	 * @param propertiesFileName
	 *            name of the properties filee
	 */
	public PropertiesUtils(final String propertiesFileName) {

		this(propertiesFileName, null);
	}

	/**
	 * It will read the attributes, check them and log the values read from file (or default values in case of error).
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception {

		readAttributesFromConfigFile();
		checkProperties();
		logProperties();
	}

	/**
	 * @see forceReadPropertiesAgain() to force the call of this method
	 * @return true if the properties were setup and read fromt the configuration file
	 */
	public boolean readAttributesFromConfigFile() {

		// to avoid reading again the fail. force boolean can be change to read again anyway
		if (!this.force && (this.properties != null) && !this.properties.isEmpty()) {
			this.force = false;
			return true;
		}

		final URL attributesFile = Thread.currentThread().getContextClassLoader().getResource(this.propertiesFileName);
		InputStream inStream = null;

		if (attributesFile == null) {
			Log.info(this, " No Log configuration file found:  " + this.propertiesFileName
					+ ". Use DEFAULT configuration properties.");
			return false;
		}

		try {
			inStream = attributesFile.openStream();
		} catch (IOException e) {
			Log.error(this, e); //sonar
			return false;
		}

		this.properties = new Properties();
		if (inStream != null) {
			try {
				this.properties.load(inStream);
			} catch (final IOException e) {
				Log.error(this, "Can not load properties stream file from: " + this.propertiesFileName);
				Log.error(this, e); //sonar
				return false;
			}
		} else {
			Log.error(this, "Can not find properties file from: " + this.propertiesFileName);
			return false;
		}

		if (this.properties.isEmpty()) {
			Log.error(this, "Properties file " + this.propertiesFileName + " is empty. Check file.");
			return false;
		}

		return true;

	}

	/**
	 * It will check the properties processed. If any property name is missing or the number of properties doesn't match
	 * it will try to parse the default properties that were added optionally.
	 * 
	 * @throws Exception
	 *             if there was a parsing error however no default properties has been added
	 */
	public boolean checkProperties() throws Exception {

		if (propertiesNamesListAdded()) {

			// can check properties names
			boolean useDefault = checkPropertiesNames();
			// check number
			if (!useDefault) {
				useDefault = checkPropertiesNumber();
			}

			if (useDefault) {
				// if any parsing errors and defaults not added, cannot continue
				if (!defaultPropertiesAdded()) {
					Log.error(this, "No default properties added and there is a parsing error. Can NOT continue");
					throw new Exception("Parsing error in " + this.propertiesFileName);
				}
				// othercase use default
				useDefaultProperties();
			}

		} else {

			Log.info(
					this,
					"Assuming "
							+ this.propertiesFileName
							+ " is correct. No names list has been added and, in case of parsing error, no default properties can be used. Cannot check properties");
			return false;
		}

		return true;
	}

	/**
	 * Log as info all the properties file (or default in case of any error).
	 */
	public void logProperties() {

		final StringBuffer sb = new StringBuffer();
		final Set<String> proNames = this.properties.stringPropertyNames();
		sb.append("--- PROPERTIES read FROM " + this.propertiesFileName + " ---" + "\n");
		if (proNames.isEmpty()) {
			sb.append("File is empty or not found");
		} else {
			for (String name : proNames) {

				sb.append(name).append("=").append(this.properties.getProperty(name));
				sb.append("\n");
			}
		}
		Log.info(this, sb.toString());
	}

	/**
	 * @param name
	 *            of the property
	 * @return value of the property
	 */
	public String getProperty(final String name) {

		if (this.properties != null) {
			return this.properties.getProperty(name);
		}
		return null;
	}

	/**
	 * @return a map with the properties pair name=value
	 */
	public Map<String, String> getPropertiesMap() {

		if (this.properties != null) {

			final HashMap<String, String> map = new HashMap<String, String>();
			final Set<String> proNames = this.properties.stringPropertyNames();
			for (String name : proNames) {
				map.put(name, this.properties.getProperty(name));
			}
			return map;
		}
		return null;
	}

	/**
	 * @param forces
	 *            to read again the attributes file when processing it
	 * 
	 */
	public void forceReadPropertiesAgain() {
		this.force = true;
	}

	/**
	 * @return the propertiesFileName
	 */
	public String getPropertiesFileName() {
		return this.propertiesFileName;
	}

	/**
	 * @return the propertiesNamesList
	 */
	public List<String> getPropertiesNamesList() {
		return this.propertiesNamesList;
	}

	/**
	 * @return the propertiesDefaultValuesList
	 */
	public List<String> getPropertiesDefaultValuesList() {
		return this.propertiesDefaultValuesList;
	}

	// **********************************//
	// ******* PRIVATE METHODS *******//
	// ********************************//

	/**
	 * puts the default properties
	 */
	private void useDefaultProperties() {

		Log.info(this, "Parsing errors in " + this.propertiesFileName + ". Using default properties");
		this.properties = new Properties();
		Iterator<String> nameIte = this.propertiesNamesList.iterator();
		Iterator<String> valueIte = this.propertiesDefaultValuesList.iterator();
		while (nameIte.hasNext()) {
			this.properties.put(nameIte.next(), valueIte.next());
		}

	}

	/**
	 * @return if the properties fields name number matches the list of properties names size
	 */
	private boolean checkPropertiesNumber() {

		final boolean ret = this.propertiesNamesList.size() != this.properties.stringPropertyNames().size();
		if (ret) {
			Log.error(this,
					"the number of fields in properties file does NOT match number of fields in properties List");
		}
		return ret;

	}

	/**
	 * @return if if the properties fields names matches the list of properties names and are the same
	 */
	private boolean checkPropertiesNames() {

		final StringBuffer sb = new StringBuffer();
		final Set<String> proNames = this.properties.stringPropertyNames();

		for (String name : this.propertiesNamesList) {

			if (!proNames.contains(name)) {
				sb.append("Attribute name " + name + " is missing in properties file!");
				sb.append("\n");
			}
		}
		if (sb.length() > 0) {
			Log.error(this, sb.toString());
			return true;
		}
		return false;

	}

	/**
	 * @return if the default values has been added
	 */
	private boolean defaultPropertiesAdded() {

		if ((this.propertiesDefaultValuesList == null) || this.propertiesDefaultValuesList.isEmpty()) {
			Log.info(
					this,
					"No properties default values has been notified. If properties file cannot be read it, default values WILL NOT be used as alternative.");
			return false;
		}

		return true;
	}

	/**
	 * @return if the default properties names list has been added
	 */
	private boolean propertiesNamesListAdded() {

		if ((this.propertiesNamesList == null) || this.propertiesNamesList.isEmpty()) {
			Log.info(this,
					"No properties Names List has been notified. Number and names of properties cannot be verified");
			return false;
		}

		return true;
	}

	// TESTS
	// @SuppressWarnings({ "unused" })
	// public static void main(String[] pepe) throws Exception {
	//
	// final String args[] = { "-env", "dev5-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
	// DSConnection ds = null;
	// try {
	// ds = ConnectionUtil.connect(args, "MainEntry");
	// } catch (ConnectException e) {
	// e.printStackTrace();
	// }
	//
	// try {
	// // false property name
	// PropertiesUtils pro = new PropertiesUtils("unkown.txt");
	// pro.process();
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// // only name
	// PropertiesUtils p = new PropertiesUtils("connection.properties.doddfrankMQ");
	// p.process();
	// p.readAttributesFromConfigFile();
	// p.forceReadPropertiesAgain();
	// p.readAttributesFromConfigFile();
	//
	// // adding list of attributes names
	// // ok
	// String[] a = { "doddfrankmq.mq.queue.connectionFactory", "doddfrankmq.mq.url", "doddfrankmq.mq.modetypeclass",
	// "doddfrankmq.mq.input.queue.name", "doddfrankmq.mq.output.queue.name", "doddfrankmq.mq.opmode",
	// "doddfrankmq.mq.transacted" };
	// ArrayList<String> namesA = new ArrayList<String>(Arrays.asList(a));
	// p = new PropertiesUtils("connection.properties.doddfrankMQ", namesA);
	// p.process();
	//
	// // missing name
	// String[] b = { "doddfrankmq.mq.queue.connectionFactory", "error.mq.url", "doddfrankmq.mq.modetypeclass",
	// "doddfrankmq.mq.input.queue.name", "doddfrankmq.mq.output.queue.name", "doddfrankmq.mq.opmode",
	// "transacted" };
	// ArrayList<String> namesB = new ArrayList<String>(Arrays.asList(b));
	// p = new PropertiesUtils("connection.properties.doddfrankMQ", namesB);
	// try {
	// p.process();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// // missing number
	// String[] c = { "doddfrankmq.mq.queue.connectionFactory", "doddfrankmq.mq.url", "doddfrankmq.mq.modetypeclass",
	// "doddfrankmq.mq.input.queue.name", "doddfrankmq.mq.output.queue.name", "doddfrankmq.mq.opmode" };
	// ArrayList<String> namesC = new ArrayList<String>(Arrays.asList(c));
	// p = new PropertiesUtils("connection.properties.doddfrankMQ", namesC);
	// try {
	// p.process();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// // adding default attributes
	// String[] defaultValues = { "QueueConnectionFactory", "tcp://localhost:61616",
	// "org.apache.activemq.jndi.ActiveMQInitialContextFactory", "test", "testIncome", "2", "false" };
	// ArrayList<String> defaultList = new ArrayList<String>(Arrays.asList(defaultValues));
	// // ok
	// ArrayList<String> names = new ArrayList<String>(Arrays.asList(a));
	//
	// p = new PropertiesUtils("connection.properties.doddfrankMQ", names, defaultList);
	// p.process();
	//
	// // missing name
	// p = new PropertiesUtils("connection.properties.doddfrankMQ", namesB, defaultList);
	// try {
	// p.process();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// // missing number
	// p = new PropertiesUtils("connection.properties.doddfrankMQ", namesC, defaultList);
	// try {
	// p.process();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// System.out.println(p.getPropertiesDefaultValuesList());
	// System.out.println(p.getPropertiesMap());
	//
	// /*
	// * doddfrankmq.mq.queue.connectionFactory=QueueConnectionFactory doddfrankmq.mq.url=tcp://localhost:61616
	// * doddfrankmq.mq.modetypeclass=org.apache.activemq.jndi.ActiveMQInitialContextFactory
	// * doddfrankmq.mq.input.queue.name=test doddfrankmq.mq.output.queue.name=testIncome doddfrankmq.mq.opmode=2
	// * doddfrankmq.mq.transacted=false
	// */
	// }

}
