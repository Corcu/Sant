//
// Generated By:JAX-WS RI IBM 2.1.6 in JDK 6 (JAXB RI IBM JAXB 2.1.10 in JDK 6)
//


package com.isban.efs2.webservice;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "EFS2WS", targetNamespace = "http://webservice.efs2.isban.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface EFS2WS {


    /**
     * 
     * @param arg0
     * @return
     *     returns com.isban.efs2.webservice.Efs2Response
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "handleRequest", targetNamespace = "http://webservice.efs2.isban.com/", className = "com.isban.efs2.webservice.HandleRequest")
    @ResponseWrapper(localName = "handleRequestResponse", targetNamespace = "http://webservice.efs2.isban.com/", className = "com.isban.efs2.webservice.HandleRequestResponse")
    public Efs2Response handleRequest(
        @WebParam(name = "arg0", targetNamespace = "")
        Efs2Request arg0);

}