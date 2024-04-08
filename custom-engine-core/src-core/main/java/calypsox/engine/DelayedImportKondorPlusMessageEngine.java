package calypsox.engine;

import calypsox.engine.advice.ImportMessageEngine;
import calypsox.engine.scheduling.IncomingKondorPlusProcessingJob;
import calypsox.tk.util.ExtendedIEAdapterListener;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.IEAdapter;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DelayedImportKondorPlusMessageEngine extends ImportMessageEngine implements ExtendedIEAdapterListener {

    public static final String ENGINE_NAME = "SANT_ImportMessageEngine_Murex";
    private IEAdapter adapter;
    /**
     * JMS Delay related properties
     */
    private static final String JMS_DELAY_TIME_DOMAIN_NAME = "IncomingKondorPlusProcessDelaySeconds";
    private long jmsSendingDelayTime = 10;

    /**
     * v6.7 processing delay
     */
    ScheduledExecutorService executorService;

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public DelayedImportKondorPlusMessageEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        jmsSendingDelayTime = getProcessingDelayTime();
    }

    /*
     *
     *
     */
    @Override
    protected synchronized void init(EngineContext engineContext) {
        super.init(engineContext);
        Log.info(this.getClass(), "Custom Log: Initializing MCLiquidation Engine...");
        // Initialize the adapter type in the Default. Used by so IEAdapter
        if (Util.isEmpty(getIeAdapterConfigName())) {
            throw new InvalidParameterException("Empty config name: cannot read Engine properties.");
        }
        // Migracion V14 22/01/2015
        setEngineName(ENGINE_NAME);
        Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Method used to hadle the message received.
     *
     * @param externalMessage Message received.
     * @return
     */
    @Override
    public boolean handleIncomingMessage(ExternalMessage externalMessage) {
        executorService.schedule(new IncomingKondorPlusProcessingJob(externalMessage), jmsSendingDelayTime, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean newMessage(IEAdapter adapter, ExternalMessage message) {
        this.adapter = adapter.getIEAdapterConfig().getReceiverIEAdapter();
        return handleIncomingMessage(message);
    }

    @Override
    protected void poststop(boolean willTerminate) {
        executorService.shutdown();
        try {
            if (this.adapter != null)
                this.adapter.stop();
            super.poststop(willTerminate);
        } catch (Exception e) {
            Log.error(this, e); //sonar 02/11/2017
            Log.error(this, "Exception while closing connection");
        }
    }


    /**
     * @param response
     * @return
     */
    public boolean writeResponse(String response) {
        boolean result = this.adapter.write(response);
        Log.debug(this, "WriteResponse::result= " + result + " & message=" + response);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.ExtendedIEAdapterListener#setIEAdapter(com.calypso.tk.
     * util.IEAdapter)
     */
    @Override
    public void setIEAdapter(IEAdapter adapter) {
        this.adapter = adapter;

    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.ExtendedIEAdapterListener#getIEAdapter()
     */
    @Override
    public IEAdapter getIEAdapter() {
        return this.adapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.ExtendedIEAdapterListener#writeMessage(com.calypso.tk.
     * util.IEAdapter, java.lang.String)
     */
    @Override
    public boolean writeMessage(IEAdapter adapter, String message) {
        if (this.adapter == null) {
            this.adapter = adapter;
        }

        boolean ret = false;
        if (Util.isEmpty(message)) {
            Log.error(this, "Empty message: nothing to write.");
        } else {
            ret = adapter.getIEAdapterConfig().getSenderIEAdapter().write(message);
        }
        return ret;
    }

    /**
     * V6.7 JMS DELAY
     */
    private void delayIncomingMessage() throws InterruptedException {
        if (jmsSendingDelayTime > 0L) {
            Log.system(this.toString(), "Delaying received message for " + jmsSendingDelayTime + " seconds");
            TimeUnit.SECONDS.sleep(jmsSendingDelayTime);
        }
    }

    /**
     * * V6.7 JMS DELAY
     *
     * @return
     */
    private long getProcessingDelayTime() {
        Optional<Vector<String>> domainValues = Optional.ofNullable(LocalCache.getDomainValues(DSConnection.getDefault(), JMS_DELAY_TIME_DOMAIN_NAME));
        return domainValues.map(this::parseLongFromStringVector).orElse(0L);
    }

    /**
     * * V6.7 JMS DELAY
     *
     * @param domainValues
     * @return
     */
    private long parseLongFromStringVector(Vector<String> domainValues) {
        long delayTime = 0;
        if (!domainValues.isEmpty()) {
            String dv = domainValues.get(0);
            try {
                delayTime = Long.valueOf(dv);
            } catch (NumberFormatException exc) {
                Log.warn(this.getClass().getSimpleName(), "Couldn't parse " + JMS_DELAY_TIME_DOMAIN_NAME + " as long", exc.getCause());
            }
        }
        return delayTime;
    }
}
