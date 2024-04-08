package com.santander.restservices.acx;

/**
 * ACXServiceException
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXServiceException extends Exception {

    private static final long serialVersionUID = 9131387980782815885L;

    int code;

    public ACXServiceException(Exception e) {
        this(null, e);
    }
    public ACXServiceException(String msg, Exception e) {
        this(-1, msg, e);
    }
    public ACXServiceException(int code, String msg, Exception e) {
        super(msg, e);
        this.code = code;
    }

    public ACXServiceException(String msg) {
        this(-1, msg);
    }

    public ACXServiceException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
