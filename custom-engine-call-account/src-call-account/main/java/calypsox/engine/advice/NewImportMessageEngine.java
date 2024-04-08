package calypsox.engine.advice;

import calypsox.tk.util.ExtendedIEAdapterListener;
import calypsox.tk.util.SantanderIEAdapter;
import calypsox.tk.util.SantanderIEAdapterConfig;
import com.calypso.engine.Engine;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.ExternalMessageHandler;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.*;

public abstract class NewImportMessageEngine extends Engine implements ExtendedIEAdapterListener {

    /**
     * Default name of the engine.
     */
    private static final String ENGINE_NAME_STR = "NewImportMessageEngine";

    /**
     * Type used to get the properties file.
     */
    private String type = null;

    /**
     * Variable to be used with config parameter of the engine.
     */
    private String ieAdapterConfigName = null;

    /**
     * Config adapter for the engine.
     */
    private SantanderIEAdapterConfig ieAdapterConfig = null;

    /**
     * Adapter for the engine.
     */
    private SantanderIEAdapter ieAdapter = null;

    /**
     * Setter to set the value of the type.
     *
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * getter to obtatin the parameter type.
     *
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Constructor of the class.
     *
     * @param dsCon    Calypso connection
     * @param hostName Name of the host to connect.
     * @param esPort   Port to connect.
     */
    public NewImportMessageEngine(DSConnection dsCon, String hostName, int esPort) {
        super(dsCon, hostName, esPort);
    }

    /**
     * @param configName
     * @param dsCon
     * @param hostName
     * @param esPort
     * @deprecated
     */
    @Deprecated
    public NewImportMessageEngine(String configName, DSConnection dsCon, String hostName, int esPort) {
        super(dsCon, hostName, esPort);
        this.ieAdapterConfigName = configName;
        if (this.ieAdapterConfigName != null) {
            setEngineName(this.ieAdapterConfigName + ENGINE_NAME_STR);
        } else {
            setEngineName(ENGINE_NAME_STR);
        }
    }

    /**
     * Initialize of the engine. Initialize LogEngine and get the parameter config.
     */
    @Override
    protected void init(EngineContext engineContext) {
        super.init(engineContext);
        initConfigAndTypeParams(engineContext);
        this.ieAdapterConfigName = engineContext.getInitParameter("config", null);
        if (Util.isEmpty(this.ieAdapterConfigName))
            Log.error(this, "Config name not specified. Check if " + getEngineName() + ".config is specified in the engine startup properties");
    }

    @Override
    public synchronized boolean start() throws ConnectException {
        return start(true);
    }

    /**
     *
     */
    public void initConfigAndTypeParams(EngineContext engineContext) {
        this.ieAdapterConfigName = engineContext.getInitParameter("config", null);
        this.type = engineContext.getInitParameter("type", "");
    }

    /**
     * Starts the connection to JMS queue using the adapter and properties for the engine to instantiate it.
     * This method solves the problem with the connection to wrong queues because it set the adapterType and do not use Defaults class.
     */
    @Override
    public synchronized boolean start(boolean batch) throws ConnectException {
        this._doBatch = true;

        if (this.ieAdapterConfigName == null) {
            Log.error(this, "Parameter config not configured properly ");
            throw new ConnectException(
                    "Config is not specified, specify config using -DImportMessageEngine.config=configname");
        }

        if (this.ieAdapterConfig == null) {
            this.ieAdapterConfig = SantanderIEAdapter.getConfig(this.ieAdapterConfigName);
            // Setting up the adapter type to connect directly to the correct queue.
            ieAdapterConfig.setAdapterType(getType());
        }
        if ((this.ieAdapterConfig == null) || (!(this.ieAdapterConfig.isConfigured()))) {
            Log.error("MatchingMessage", "*** config not configured properly ");
            throw new ConnectException("Configuration " + this.ieAdapterConfigName + " not configured properly ");
        }
        this.ieAdapter = this.ieAdapterConfig.getSantReceiverIEAdapter();
        if (this.ieAdapter == null) {
            throw new ConnectException("No incoming adapter configured");
        }
        this.ieAdapter.setListener(this);
        boolean started = super.start(batch);
        logConnectionParameters();
        this.ieAdapter.init();
        this.ieAdapterConfig.checkTimer();
        return started;
    }

    public boolean newMessage(IEAdapter adapter, String mess) {
        throw new UnsupportedOperationException("Default ImportMessageEngine does not support newMessage(.., String)");
    }

    /**
     * New message received in the queue.
     */
    public boolean newMessage(IEAdapter adapter, ExternalMessage msg) {
        if (msg == null) {
            return false;
        }
        boolean proc = false;
        if (adapter == this.ieAdapter) {
            proc = handleIncomingMessage(msg);
        }
        return proc;
    }

    /**
     * Method used to hadle the message received.
     *
     * @param externalMessage Message received.
     * @return
     */
    public boolean handleIncomingMessage(ExternalMessage externalMessage) {
        boolean status = true;
        try {
            ExternalMessageHandler handler = null;
            String handlerClassName = "tk.bo." + externalMessage.getType() + "MessageHandler";
            try {
                handler = (ExternalMessageHandler) InstantiateUtil.getInstance(handlerClassName, true);
            } catch (Exception e) {
                Log.error(this, e); //sonar 02/11/2017
                Log.info(this, handlerClassName + " not found.");
            }
            if (handler != null) {
                status = handler.handleExternalMessage(externalMessage, getPricingEnv(), null, null, this._ds, null);
            } else {
                status = SwiftParserUtil.processExternalMessage(externalMessage, getPricingEnv(), null, null, this._ds,
                        null);
            }

            if (this.ieAdapter.getTransactionEnabled())/* 164 */ this.ieAdapter.commit();
        } catch (Exception e) {
            Log.error(this, e); //sonar 02/11/2017
            try {
                if (this.ieAdapter.getTransactionEnabled())/* 168 */ this.ieAdapter.rollback();
            } catch (ConnectException e1) {
                Log.error(this, e1); //sonar 02/11/2017
                Log.error(this, "Error during queue session rollback() for config " + this.ieAdapter.getConfigName());
            }
            Log.error(this, "Error while parsing ExternalMessage ");
            return false;
        }
        return status;
    }

    /**
     * Domain values change.
     */
    @Override
    public void processDomainChange(PSEventDomainChange ad) {
        super.processDomainChange(ad);
        if (ad.getType() == 5)
            this.ieAdapterConfig.checkTimer();
    }

    /**
     * Not used.
     */
    public boolean process(PSEvent event) {
        return true;
    }

    /**
     * Disconnect the engine.
     */
    public void onDisconnect(IEAdapter adapter) {
        Log.error(this, " Message Adapter disconnected, waited to be connected again ");
    }

    /**
     * Stops the adapter.
     */
    @Override
    protected void poststop(boolean willTerminate) {
        try {
            if (this.ieAdapter != null)/* 196 */ this.ieAdapter.stop();
        } catch (Exception e) {
            Log.error(this, e); //sonar 02/11/2017
        }
    }

    @Override
    public void setIEAdapter(IEAdapter adapter) {
        if (adapter instanceof SantanderIEAdapter) {
            this.ieAdapter = (SantanderIEAdapter) adapter;
        } else {
            Log.error(NewImportMessageEngine.class.getSimpleName(), "Couldn't set ImportMessageEngine IEAdapter, " +
                    "it must be extends SantanderIEAdapter class");
        }

    }

    @Override
    public IEAdapter getIEAdapter() {
        return ieAdapter;
    }

    public IEAdapterConfig getIEAdapterConfig() {
        return this.ieAdapterConfig;
    }

    public void setIEAdapterConfig(IEAdapterConfig adapterConfig) {
        if (adapterConfig instanceof SantanderIEAdapterConfig) {
            this.ieAdapterConfig = (SantanderIEAdapterConfig) adapterConfig;
        } else {
            Log.error(this.getLogCategory(), "Cannot set a non SantanderIEAdapterConfig: " + adapterConfig.getClass().getName());
        }
    }

    protected void logConnectionParameters() {
        if (ieAdapter != null) {
            Log.debug(this.getClass().getSimpleName(), "Connecting to " + ieAdapter.getConfigName() + " config name.");
        }
    }

    /**
     * @return
     */
    public String getIeAdapterConfigName() {
        return this.ieAdapterConfigName;
    }
}