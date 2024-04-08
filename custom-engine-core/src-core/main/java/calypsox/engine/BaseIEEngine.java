/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc.
 * 595 Market Street, Suite 1980, San Francisco, CA  94105, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Calypso Technology, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Calypso Technology.
 *
 */
package calypsox.engine;

import calypsox.engine.advice.ImportMessageEngine;
import calypsox.tk.util.TibcoQueueIEAdapter;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class CalypsoMLIEEngine.
 * <p>
 * Base abstract class of an Engine dedicated to import/export messages to/from Calypso.<br/>
 * Data Import/Export is messages received/sent through any Medium: files, Queue, etc.
 *
 * @author Bruno P.
 * @version 1.0
 * @since 03/18/2011
 */
public abstract class BaseIEEngine extends ImportMessageEngine {

    private boolean publishTask = true;
    public static final String CONFIG_NAME = "SantanderJMSQueue";

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public BaseIEEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    /**
     * Method that initialize the engine
     *
     * @param engineContext context of engine
     */
    @Override
    protected void init(EngineContext engineContext) {
        // TODO Auto-generated method stub
        setEngineName(engineContext.getEngineName());

        super.init(engineContext);

        // Initialize the adapter type in the Default. Used by so IEAdapter
       /* Properties properties = Defaults.getProperties();
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(SantanderIEAdapterConfig.ADAPTER_TYPE, getType());
        Defaults.setProperties(properties);*/

        //Migration V14 22/01/2015
        //setEngineName("SANT_ImportMessageEngine_" + type); modified 26/10/2017 - engines

    }

    /**
     * Process the message and create any necessary tasks
     *
     * @param message the incoming message to process
     * @param tasks   a List to fill with com.calypso.tk.bo.Task to publish if needed.
     * @return true if the message was handled correctly, otherwise false
     */
    public abstract boolean handleIncomingMessage(String message, List<Task> tasks) throws Exception;

    /**
     * Process the received PSEvent: build outgoing message - and create any necessary tasks
     *
     * @param msg   the outgoing event to process
     * @param tasks a List to fill with com.calypso.tk.bo.Task to publish if needed.
     * @return true if the message was handled correctly, otherwise false
     */
    public abstract String handleOutgoingMessage(PSEvent event, List<Task> tasks) throws Exception;

    /**
     * Write a message through the adapter
     *
     * @param adapter IEAdapter used to send the message
     * @param message message to write
     * @return true if everything is fine.
     */
    @Override
    public boolean writeMessage(IEAdapter adapter, String message) {
        boolean ret = false;
        if (this.getIEAdapter() == null) {
            if (Util.isEmpty(message)) {
                //AAP MIG 14.4 Changed to debug level to avoid log saturation
                Log.debug(this, "Empty message: nothing to write.");
            } else {
                ret = adapter.getIEAdapterConfig().getSenderIEAdapter().write(message);
            }
        }
        return ret;
    }

    /**
     * React to incoming message
     *
     * @param adapter IEAdapter used
     * @param msg     new incoming message
     * @return true if everything is fine.
     */
    @Override
    public boolean newMessage(IEAdapter adapter, ExternalMessage msg) {
        if (getIEAdapter() == null) {
            setIEAdapter(adapter);
        }
        return super.newMessage(adapter, msg);
    }

    /**
     * Processing for Incoming External messages :
     * <p>
     * <li>Get The Message from the adapater</li>
     * <li>Parse the Message if necessary</li>
     * <li>Process the received message in Calypso</li>
     * <li>Acknowledge the message</li>
     * <p>
     * called from IEAdapter.callBackListener if there's no config or no parser in the config
     */
    @Override
    public final boolean newMessage(IEAdapter adapter, String mess) {
        if (getIEAdapter() == null) {
            setIEAdapter(adapter);
        }
        boolean ret = false;
        List<Task> tasks = new ArrayList<>();
        try {
            ret = handleIncomingMessage(mess, tasks);
        } catch (Exception exception) {
            Log.error(this, getEngineName(), exception);
            tasks.add(buildTask(exception));
        }
        publishTask(tasks);
        return ret;
    }

    /**
     * Process an event
     *
     * @param event incoming event
     * @return true if everything is fine.
     */
    @Override
    public boolean process(PSEvent event) {
        boolean ret = false;
        List<Task> tasks = new ArrayList<>();
        try {
            String message = handleOutgoingMessage(event, tasks);
            // If it's a trade we need the trade to get the JmsReference if it's
            // a mirror
            if ((event instanceof PSEventTrade) && (getIEAdapter() instanceof TibcoQueueIEAdapter)) {
                ret = writeMessage((TibcoQueueIEAdapter) getIEAdapter(), message);
            } else {
                ret = writeMessage(getIEAdapter(), message);
            }
            if (ret) {
                getDS().getRemoteTrade().eventProcessed(event.getLongId(), getEngineName());
            }
        } catch (Exception exception) {
            Log.error(this, getEngineName(), exception);
            tasks.add(buildTask(exception));
        }
        publishTask(tasks);
        return ret;
    }

    /**
     * Generate a Task
     *
     * @param exception source of the task
     * @return a new Task
     */
    protected Task buildTask(Exception exception) {
        return buildTask(exception, 0, "EX_" + getIeAdapterConfigName(), Task.EXCEPTION_EVENT_CLASS);
    }

    /**
     * Publish the tasks
     *
     * @param tasks tasks to published
     */
    protected void publishTask(List<Task> tasks) {
        if (Util.isEmpty(tasks) || !isPublishTask()) {
            return;
        }
        try {
            getDS().getRemoteBO().saveAndPublishTasks(new TaskArray(tasks), 0, getEngineName());
        } catch (RemoteException e) {
            Log.error(this, getEngineName() + ": Failed to saveAndPublishTasks: " + tasks, e);
        }
    }

    /**
     * Generate a Task
     *
     * @param exception  source of the task
     * @param tradeId    trade id if the exception is related to a trade
     * @param eventType  task event type
     * @param eventClass task event class
     * @return a new Task
     */
    protected Task buildTask(Exception exception, long tradeId, String eventType, String eventClass) {
        return buildTask(getExceptionMessage(exception), tradeId, eventType, eventClass);
    }

    /**
     * Generate a Task
     *
     * @param comment    comment related to the task
     * @param tradeId    trade id if the exception is related to a trade
     * @param eventType  task event type
     * @param eventClass task event class
     * @return a new Task
     */
    protected Task buildTask(String comment, long tradeId, String eventType, String eventClass) {
        Task task = new Task();
        task.setObjectLongId(tradeId);
        task.setTradeLongId(tradeId);
        task.setEventClass(eventClass);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setSource(getEngineName());
        task.setComment(comment);
        return task;
    }

    /**
     * Get the exception message
     *
     * @param exception related exception
     * @return the exception message
     */
    public String getExceptionMessage(Exception exception) {
        return exception.getMessage();
    }

    /**
     * Get if the Task publishing status
     *
     * @return true if the engine is supposed to generate a task
     */
    public boolean isPublishTask() {
        return this.publishTask;
    }

    /**
     * Set if the Task publishing status
     *
     * @param publishTask true is the engine is supposed to generate tasks
     */
    public void setPublishTask(boolean publishTask) {
        this.publishTask = publishTask;
    }

    /**
     * Method that close the Adapter used
     *
     * @param willTerminate
     */
    @Override
    protected void poststop(boolean willTerminate) {
        try {
            if (getIEAdapter() != null) {
                getIEAdapter().stop();
            }
            super.poststop(willTerminate);
        } catch (Exception e) {
            Log.error(this, "Exception while closing connection");
            Log.error(this, e);//Sonar
        }
    }
}
