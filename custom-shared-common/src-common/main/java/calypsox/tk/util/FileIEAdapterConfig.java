/*
 * Calypso API - Generic Product training tutorial - February 2008.
 * 
 * Copyright © 2008 Calypso Technology, Inc. All Rights Reserved
 */

package calypsox.tk.util;

import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Properties;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

/**
 * 
 * This Abstract class can be extended to allow a named File import/export. <br/>
 * Example: CMLIEAdapterConfig
 * 
 */

public abstract class FileIEAdapterConfig implements IEAdapterConfig {
	private Properties properties = null;
	private FileIEAdapter fileIEAdpater = null;
	private static final String CONFIG_FILE_EXT = "_config.properties";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.IEAdapterConfig#checkTimer()
	 */
	@Override
	public void checkTimer() {
	}

	public String getConfigFileName() {
		return getFileIEAdpater(IEAdapterMode.READ).getConfigName();
	}

	/**
	 * Gets the file ie adpater.
	 * 
	 * @param mode
	 *            the mode
	 * @return the file ie adpater
	 */
	public FileIEAdapter getFileIEAdpater(final IEAdapterMode mode) {
		if (this.fileIEAdpater == null) {
			setFileIEAdpater(new FileIEAdapter(mode));
			this.fileIEAdpater.setIEAdapterConfig(this);
		}
		return this.fileIEAdpater;
	}

	/*
	 * While it return is null, triggers IEAdapterListener.newMessage(IEAdapter, message)
	 * 
	 * @see com.calypso.tk.util.IEAdapterConfig#getIEParser()
	 */
	public ExternalMessage getIEParser() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.IEAdapterConfig#getProperties()
	 */
	@Override
	public Properties getProperties() {
		return this.properties;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.IEAdapterConfig#getReceiverIEAdapter()
	 */
	@Override
	public IEAdapter getReceiverIEAdapter() {
		return getFileIEAdpater(IEAdapterMode.READ);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.IEAdapterConfig#getSenderIEAdapter()
	 */
	@Override
	public IEAdapter getSenderIEAdapter() {
		return getFileIEAdpater(IEAdapterMode.WRITE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.IEAdapterConfig#isConfigured()
	 */
	@Override
	public boolean isConfigured() {
		return isConfigured(getConfigFileName());
	}

	/*
	 * Check if IEAdapterConfig is configured
	 * 
	 * @see com.calypso.tk.util.IEAdapterConfig#isConfigured(java.lang.String)
	 */
	@Override
	public boolean isConfigured(String configFileName) {
		if (this.properties == null) {
			if (!configFileName.endsWith(CONFIG_FILE_EXT)) {
				configFileName += CONFIG_FILE_EXT;
			}
			this.properties = load(configFileName);
		}
		if ((this.properties == null) || this.properties.isEmpty()) {
			throw new InvalidParameterException(configFileName
					+ " configFileName is not correct: unable to load IEAdapterConfig properties");
		}
		return true;
	}

	protected Properties load(final String fileName) {
		final Properties props = new Properties();
		final ClassLoader cl = (com.calypso.tk.core.Defaults.class).getClassLoader();
		InputStream stream = null;
		stream = cl.getResourceAsStream(fileName);

		if (stream == null) {
			return null;
		}

		try {
			props.load(stream);
			stream.close();
			if (Log.isInfo()) {
				Log.info(this, " properties loaded : " + props.toString());
			}
		} catch (final Exception e) {
			Log.error(this, e);
		}
		return props;
	}

	public void setFileIEAdpater(final FileIEAdapter fileIEAdpater) {
		this.fileIEAdpater = fileIEAdpater;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.IEAdapterConfig#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(final Properties properties) {
		this.properties = properties;
	}
}
