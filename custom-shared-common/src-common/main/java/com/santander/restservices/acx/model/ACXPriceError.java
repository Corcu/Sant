package com.santander.restservices.acx.model;

import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

import java.util.List;
import java.util.stream.Collectors;


public class ACXPriceError extends ApiRestModelRoot {

    private String timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;

    private String httpCode;
    private String httpMessage;
    private String moreInformation;

    private List<ErrorItem> errors;

    public ACXPriceError() {
    }

    public ACXPriceError(ACXPriceError error) {
        loadModelData(error);
    }

    public String getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(String httpCode) {
        this.httpCode = httpCode;
    }

    public String getHttpMessage() {
        return httpMessage;
    }

    public void setHttpMessage(String httpMessage) {
        this.httpMessage = httpMessage;
    }

    public String getMoreInformation() {
        return moreInformation;
    }

    public void setMoreInformation(String moreInformation) {
        this.moreInformation = moreInformation;
    }

    public List<ErrorItem> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorItem> errors) {
        this.errors = errors;
    }

    @Override
    public boolean checkModelDataLoaded() {
        return (this.httpCode != null);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void loadModelData(ApiRestModel model) {
        if (model instanceof ACXPriceError) {
            ACXPriceError data = (ACXPriceError) model;

            setHttpCode(data.getHttpCode());
            setHttpMessage(data.getHttpMessage());
            setMoreInformation(data.getMoreInformation());

            setError(data.getError());
            setStatus(data.getStatus());
            setMessage(data.getMessage());
            setPath(data.getPath());
            setTimestamp(data.getTimestamp());

            setErrors(data.getErrors());
        }
    }

    @Override
    public Class<ACXPriceError> retriveModelClass() {
        return ACXPriceError.class;
    }

    @Override
    public String pullTextMessage() {
        if (httpMessage != null)
            return httpCode + " " + httpMessage + ": " + moreInformation;

        if (errors != null) {
            return errors.stream()
                    .map(e -> e.getCode() + " " + e.getMessage() + ": " + e.getDescription())
                    .collect(Collectors.joining("\n"));
        }

        if (error != null) {
            return status + " " + error + ": " + message + " " + path;
        }

        return super.pullTextMessage();
    }

    private static class ErrorItem {
        private int code;
        private String message;
        private String description;
        private String level;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }
}
