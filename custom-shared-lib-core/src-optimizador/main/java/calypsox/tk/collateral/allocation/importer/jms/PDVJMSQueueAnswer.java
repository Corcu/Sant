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
package calypsox.tk.collateral.allocation.importer.jms;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.util.JMSQueueAnswer;
import com.calypso.infra.util.Util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author aela
 */
public class PDVJMSQueueAnswer extends JMSQueueAnswer {

    public static MessageFormat ackMessage = new MessageFormat("{0}|{1}");

    public static MessageFormat nackMessage = new MessageFormat("{0}|{1}|{2}");

    private List<String> messages = new ArrayList<>();

    protected String importKey = "";

    @Override
    public String toString() {
        return generateAnswer();
    }

    public PDVJMSQueueAnswer(String importKey) {
        super();
        this.importKey = importKey;
    }

    public PDVJMSQueueAnswer() {
        this(null);
    }

    /**
     * @return
     */
    protected String generateAnswer() {
        StringBuffer finalMessage = new StringBuffer("");
        if (!Util.isEmpty(messages)) {
            for (String message : messages) {
                if (finalMessage.length() == 0) {
                    finalMessage.append(message);
                } else {
                    finalMessage.append("\n");
                    finalMessage.append(message);
                }
            }
        }
        return finalMessage.toString();
    }

    /**
     * @return the importKey
     */
    public String getImportKey() {
        return importKey;
    }

    /**
     * @param importKey the importKey to set
     */
    public void setImportKey(String importKey) {
        this.importKey = importKey;
    }

    public void addMessages(List<Object> invalidItems, String status) {
        String nackMessage = null;
        AllocImportErrorBean invalidAllocBean = null;
        ExternalAllocationBean allocBean = null;

        String defalultErrorMessage = "Unexpected error occured while importing the allocation: ";
        String errorMessage = "";

        for (Object errorBean : invalidItems) {

            invalidAllocBean = (AllocImportErrorBean) errorBean;
            if (invalidAllocBean == null) {
                continue;
            }
            allocBean = invalidAllocBean.getAllocBean();
            String pdvID = "";
            if (allocBean != null) {
                pdvID = allocBean.getAttributes().get(
                        PDVUtil.COLLAT_NUM_FRONT_ID_FIELD);
                if (pdvID == null) {
                    pdvID = "";
                }
            }
            errorMessage = invalidAllocBean.getValue();
            if (Util.isEmpty(invalidAllocBean.getCode())) {
                errorMessage = defalultErrorMessage + errorMessage;
            }
            nackMessage = PDVJMSQueueAnswer.nackMessage.format(new String[]{
                    pdvID, status, errorMessage});
            messages.add(nackMessage);
        }

    }

    /**
     * @param validItems
     * @param status
     */
    public void addAckMessages(List<Object> validItems, String status) {
        String nackMessage = null;

        for (Object okBean : validItems) {

            String pdvID = (String) okBean;
            if (pdvID == null) {
                pdvID = "";
            }

            nackMessage = PDVJMSQueueAnswer.ackMessage.format(new String[]{
                    pdvID, status});
            messages.add(nackMessage);
            // System.out.println("***********************"+nackMessage);
        }
    }

    public void addMessage(String status, String id, String errorMsg) {
        if (JMSQueueAnswer.OK.equals(status)) {
            messages.add(PDVJMSQueueAnswer.ackMessage.format(new String[]{id, status, errorMsg}));
        } else {
            messages.add(PDVJMSQueueAnswer.nackMessage.format(new String[]{id, status, errorMsg}));
        }
    }
}
