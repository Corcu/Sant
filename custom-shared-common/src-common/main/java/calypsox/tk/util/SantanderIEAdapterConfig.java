package calypsox.tk.util;

import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public abstract class SantanderIEAdapterConfig implements IEAdapterConfig {

    protected SantanderIEAdapter santIEAdapter = null;

    public static final int RECEIVER = 1;
    public static final int SENDER = 0;
    public static final int BOTH = 2;

    private String adapterType;

    protected Properties properties = null;
    public static final String ADAPTER_TYPE = "ADAPTER_TYPE";
    protected static final String OPMODE_PROP_NAME = "jms.opmode";


    public SantanderIEAdapterConfig() {
    }


    public SantanderIEAdapterConfig(final String adapterType) {
        setAdapterType(adapterType);
    }

    public String getAdapterType() {
        return adapterType.toLowerCase();
    }

    public void setAdapterType(String adapterType) {
        this.adapterType = adapterType;
    }

    public final String getPropertyFileName() {
        final int indexOfDot = adapterType.indexOf('.');
        return (indexOfDot == -1 ? adapterType : adapterType.substring(0, indexOfDot)) + ".connection.properties";
    }

    /**
     * @return
     */
    public abstract SantanderIEAdapter getSantReceiverIEAdapter();

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public void setProperties(final Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        return isConfigured(getPropertyFileName());
    }


    @Override
    public boolean isConfigured(final String configFileName) {
        if (this.properties == null) {
            this.properties = load(configFileName);
        }

        if ((this.properties == null) || this.properties.isEmpty()) {
            Log.error(this, "*** SantanderIEAdapterConfig not configured properly " + " Please check " + configFileName
                    + " " + new JDatetime(), new java.util.InvalidPropertiesFormatException(configFileName));
            return false;
        }
        return true;
    }

    protected Properties load(final String fileName) {
        Properties props = new Properties();
        try {
            final ClassLoader cl = Defaults.class.getClassLoader();
            InputStream stream = null;
            stream = cl.getResourceAsStream(fileName);
            if (stream != null) {
                props.load(stream);
                try {
                    stream.close();
                } catch (final Exception e) {
                    Log.error(this, e);
                    return null;
                }
            }
            if (Log.isCategoryLogged(Log.TRACE)) {
                Log.debug("SantanderIEAdapterConfig", " properties loaded : " + props.toString());
            }
        } catch (final Exception ae) {
            Log.error("SantanderIEAdapterConfig", ae);
            return null;
        }

        // Filter value from type
        String type = getAdapterType();
        if ((props != null) && (type != null)) {
            final Properties newProps = new Properties();
            type = type + ".";

            @SuppressWarnings("rawtypes") final Enumeration enumeration = props.keys();
            String key = null;
            String value = null;
            while (enumeration.hasMoreElements()) {
                key = (String) enumeration.nextElement();

                if (key.startsWith(type)) {
                    value = (String) props.get(key);
                    key = key.replace(type, "");
                    newProps.put(key, value);
                }
            }
            props = newProps;
        }
        return props;
    }

    @Override
    public IEAdapter getReceiverIEAdapter() {
        return getSantReceiverIEAdapter();
    }

    @Override
    public void checkTimer() {
    }
}
