/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import com.calypso.tk.core.Log;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

/**
 * Adapter Config for MQ queues. It will do all the configuration needed. It expects the setQueueName and
 * setProperties() method to be override, before initialization, with the parameters required to set it up.
 *
 * @author Guillermo Solano
 * @version 1.1
 * @date 12/12/2013
 */
public class SantanderMQIEAdapterConfig extends SantanderIEAdapterConfig implements IEAdapterConfig {

    /**
     * Enum that defines the property name in the file configuration. Be very careful not to touch this configuration
     * unless you want to change the properties file
     */
    public enum PROPERTY {

        FACTORY_NAME("mq.queue.connectionFactory"),
        URL("mq.url"),
        MODE_TYPE_CLASS("mq.modetypeclass"),
        INPUT_QUEUE_NAME("mq.input.queue.name"),
        OUTPUT_QUEUE_NAME("mq.output.queue.name"),
        OP_MODE("mq.opmode"),
        TRANSACTED("mq.transacted");

        private String name;

        PROPERTY(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    } // end enum properties names

    /**
     * factory name
     */
    private String factoryName;
    /**
     * url file with the MQ configuration
     */
    private String url;
    /**
     * modeTypeClass
     */
    private String modeTypeClass;
    /**
     * operation mode (0,1,2)
     */
    private String opMode;
    /**
     * input queue name
     */
    private String inputQueueName;
    /**
     * output queue name
     */
    private String outputQueueName;
    /**
     * transacted or not status
     */
    private boolean transacted;

    /**
     * Empty constructor
     */
    public SantanderMQIEAdapterConfig() {
    }

    /**
     * SantanderMQIEAdapterConfig Constructor
     *
     * @param properties Properties to load the IEAdapter.
     * @param type       Type of the adapter.
     * @param listener   Listener to call back when a message is received.
     * @throws ConnectException if any mandatory propertie is missing
     */
    public SantanderMQIEAdapterConfig(final String type) throws ConnectException {

        super(type);
        // is configured checks for the file, reads it & saves in the properties file
        if (isConfigured()) {
            // read properties
            initProperties();
        }
        // we ensure to have all the required properties
        checkParameters();
        Log.info(this, "Mandatory MQ Parameters Processed");
        // show queue configuration into the Log
        final String configuration = getMQConfiguration();
        Log.info(this, configuration);
    }

    /**
     * @return the receiver adapter
     */
    @Override
    public synchronized SantanderIEAdapter getSantReceiverIEAdapter() {
        // call the MQ adapter
        if (this.santIEAdapter == null) {
            this.santIEAdapter = new SantanderMQIEAdapter(this);

        }
        return this.santIEAdapter;
    }

    /**
     * @return the IEAdapter of the sender. Same as Receiver
     */
    @Override
    public IEAdapter getSenderIEAdapter() {

        return getReceiverIEAdapter();
    }

    /**
     * @return Snapshot of the configuration of the MQ properties
     */
    private String getMQConfiguration() {

        final StringBuffer c = new StringBuffer();
        c.append("---- MQ PROPERTIES FILE SNAPSHOT ----- \n");
        c.append(PROPERTY.FACTORY_NAME.getName()).append(" -> ").append(this.factoryName).append("\n");
        c.append(PROPERTY.URL.getName()).append(" -> ").append(this.url).append("\n");
        c.append(PROPERTY.MODE_TYPE_CLASS.getName()).append(" -> ").append(this.modeTypeClass).append("\n");
        c.append(PROPERTY.INPUT_QUEUE_NAME.getName()).append(" -> ").append(this.inputQueueName).append("\n");
        c.append(PROPERTY.OUTPUT_QUEUE_NAME.getName()).append(" -> ").append(this.outputQueueName).append("\n");
        c.append(PROPERTY.OP_MODE.getName()).append(" -> ").append(this.opMode).append("\n");
        c.append(PROPERTY.TRANSACTED.getName()).append(" -> ");
        c.append(super.getProperties().getProperty(PROPERTY.TRANSACTED.getName())).append("\n");

        return c.toString();
    }

    /**
     * Takes the values read from the properties
     */
    protected void initProperties() {

        this.factoryName = super.getProperties().getProperty(PROPERTY.FACTORY_NAME.getName());
        this.url = super.getProperties().getProperty(PROPERTY.URL.getName());
        this.modeTypeClass = super.getProperties().getProperty(PROPERTY.MODE_TYPE_CLASS.getName());
        this.inputQueueName = super.getProperties().getProperty(PROPERTY.INPUT_QUEUE_NAME.getName());
        this.outputQueueName = super.getProperties().getProperty(PROPERTY.OUTPUT_QUEUE_NAME.getName());
        this.opMode = super.getProperties().getProperty(PROPERTY.OP_MODE.getName());
        final String trans = super.getProperties().getProperty(PROPERTY.TRANSACTED.getName());
        this.transacted = trans != null ? trans.trim().equalsIgnoreCase("true") || trans.trim().equalsIgnoreCase("1")
                : false;
    }

    /**
     * Ensures all the mandatory parameters have been read from the file.
     *
     * @throws ConnectException is any param is missing from the file properties
     */
    private void checkParameters() throws ConnectException {

        final String err = "ERR: Missing parameter in " + super.getPropertyFileName() + " file : " + getAdapterType();
        if ((this.factoryName == null) || this.factoryName.isEmpty()) {
            throw new ConnectException(err + "jms.queue.connectionFactory=");
        }
        this.factoryName = this.factoryName.trim();

        if ((this.url == null) || this.url.isEmpty()) {
            throw new ConnectException(err + "jms.url=");
        }
        this.url = this.url.trim();

        if ((this.modeTypeClass == null) || this.modeTypeClass.isEmpty()) {
            throw new ConnectException(err + "jms.modetypeclass=");
        }
        this.modeTypeClass = this.modeTypeClass.trim();

        if ((this.opMode == null) || this.opMode.isEmpty()) {
            throw new ConnectException(err + "jms.opmode=");
        }
        this.opMode = this.opMode.trim();
        if (!this.opMode.equals("1") && !this.opMode.equals("2") && !this.opMode.equals("0")) {
            throw new ConnectException("ERR: Missing parameter in " + super.getPropertyFileName() + " file : "
                    + "Property " + getAdapterType() + "jms.opmode= must be 0,1,2");
        }

        if (this.opMode.equals("1") || this.opMode.equals("2")) {

            if ((this.inputQueueName == null) || this.inputQueueName.isEmpty()) {
                throw new ConnectException(err + "input.queue.name=");
            }
            this.inputQueueName = this.inputQueueName.trim();
        }

        if (this.opMode.equals("0") || this.opMode.equals("2")) {
            if ((this.outputQueueName == null) || this.outputQueueName.isEmpty()) {
                throw new ConnectException(err + "output.queue.name=");
            }
            this.outputQueueName = this.outputQueueName.trim();
        }
    }

    /**
     * @return factory name
     */
    public String getFactoryName() {
        return this.factoryName;
    }

    /**
     * @return MQ url
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * @return Mode Type
     */
    public String getModeTypeClass() {
        return this.modeTypeClass;
    }

    /**
     * @return operational mode
     */
    public String getOpMode() {
        return this.opMode;
    }

    /**
     * @return input queue name
     */
    public String getInputQueueName() {
        return this.inputQueueName;
    }

    /**
     * @return output queue name
     */
    public String getOutputQueueName() {
        return this.outputQueueName;
    }

    /**
     * @return is transactional
     */
    public boolean isTransacted() {
        return this.transacted;
    }

    // not used
    @Override
    public void checkTimer() {
        return;
    }

}
