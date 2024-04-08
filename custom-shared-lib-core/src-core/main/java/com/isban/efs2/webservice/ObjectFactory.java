//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantaci?n de la referencia de enlace (JAXB) XML v2.2.7
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perder?n si se vuelve a compilar el esquema de origen.
// Generado el: 2014.01.02 a las 03:34:20 PM CET 
//


package com.isban.efs2.webservice;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.isban.efs2.webservice package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Efs2Response_QNAME = new QName("http://webservice.efs2.isban.com/", "efs2Response");
    private final static QName _Efs2Request_QNAME = new QName("http://webservice.efs2.isban.com/", "efs2Request");
    private final static QName _HandleRequest_QNAME = new QName("http://webservice.efs2.isban.com/", "handleRequest");
    private final static QName _HandleRequestResponse_QNAME = new QName("http://webservice.efs2.isban.com/", "handleRequestResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.isban.efs2.webservice
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link FxDates }
     * 
     */
    public FxDates createFxDates() {
        return new FxDates();
    }

    /**
     * Create an instance of {@link FxDates.Dates }
     * 
     */
    public FxDates.Dates createFxDatesDates() {
        return new FxDates.Dates();
    }

    /**
     * Create an instance of {@link Efs2Response }
     * 
     */
    public Efs2Response createEfs2Response() {
        return new Efs2Response();
    }

    /**
     * Create an instance of {@link Efs2Request }
     * 
     */
    public Efs2Request createEfs2Request() {
        return new Efs2Request();
    }

    /**
     * Create an instance of {@link HandleRequest }
     * 
     */
    public HandleRequest createHandleRequest() {
        return new HandleRequest();
    }

    /**
     * Create an instance of {@link HandleRequestResponse }
     * 
     */
    public HandleRequestResponse createHandleRequestResponse() {
        return new HandleRequestResponse();
    }

    /**
     * Create an instance of {@link EquityGMPList }
     * 
     */
    public EquityGMPList createEquityGMPList() {
        return new EquityGMPList();
    }

    /**
     * Create an instance of {@link EquityGMP }
     * 
     */
    public EquityGMP createEquityGMP() {
        return new EquityGMP();
    }

    /**
     * Create an instance of {@link EquityByDmd }
     * 
     */
    public EquityByDmd createEquityByDmd() {
        return new EquityByDmd();
    }

    /**
     * Create an instance of {@link Dividend }
     * 
     */
    public Dividend createDividend() {
        return new Dividend();
    }

    /**
     * Create an instance of {@link ValueData }
     * 
     */
    public ValueData createValueData() {
        return new ValueData();
    }

    /**
     * Create an instance of {@link QuotesLists }
     * 
     */
    public QuotesLists createQuotesLists() {
        return new QuotesLists();
    }

    /**
     * Create an instance of {@link EquityRepoMargin }
     * 
     */
    public EquityRepoMargin createEquityRepoMargin() {
        return new EquityRepoMargin();
    }

    /**
     * Create an instance of {@link QuotesList }
     * 
     */
    public QuotesList createQuotesList() {
        return new QuotesList();
    }

    /**
     * Create an instance of {@link FxSpot }
     * 
     */
    public FxSpot createFxSpot() {
        return new FxSpot();
    }

    /**
     * Create an instance of {@link RepoMargin }
     * 
     */
    public RepoMargin createRepoMargin() {
        return new RepoMargin();
    }

    /**
     * Create an instance of {@link Quote }
     * 
     */
    public Quote createQuote() {
        return new Quote();
    }

    /**
     * Create an instance of {@link com.isban.efs2.webservice.Entry }
     * 
     */
    public com.isban.efs2.webservice.Entry createEntry() {
        return new com.isban.efs2.webservice.Entry();
    }

    /**
     * Create an instance of {@link EquityDividend }
     * 
     */
    public EquityDividend createEquityDividend() {
        return new EquityDividend();
    }

    /**
     * Create an instance of {@link Parameters }
     * 
     */
    public Parameters createParameters() {
        return new Parameters();
    }

    /**
     * Create an instance of {@link Depo }
     * 
     */
    public Depo createDepo() {
        return new Depo();
    }

    /**
     * Create an instance of {@link FxSwapPointForDate }
     * 
     */
    public FxSwapPointForDate createFxSwapPointForDate() {
        return new FxSwapPointForDate();
    }

    /**
     * Create an instance of {@link FxDates.Dates.Entry }
     * 
     */
    public FxDates.Dates.Entry createFxDatesDatesEntry() {
        return new FxDates.Dates.Entry();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Efs2Response }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webservice.efs2.isban.com/", name = "efs2Response")
    public JAXBElement<Efs2Response> createEfs2Response(Efs2Response value) {
        return new JAXBElement<Efs2Response>(_Efs2Response_QNAME, Efs2Response.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Efs2Request }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webservice.efs2.isban.com/", name = "efs2Request")
    public JAXBElement<Efs2Request> createEfs2Request(Efs2Request value) {
        return new JAXBElement<Efs2Request>(_Efs2Request_QNAME, Efs2Request.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HandleRequest }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webservice.efs2.isban.com/", name = "handleRequest")
    public JAXBElement<HandleRequest> createHandleRequest(HandleRequest value) {
        return new JAXBElement<HandleRequest>(_HandleRequest_QNAME, HandleRequest.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HandleRequestResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://webservice.efs2.isban.com/", name = "handleRequestResponse")
    public JAXBElement<HandleRequestResponse> createHandleRequestResponse(HandleRequestResponse value) {
        return new JAXBElement<HandleRequestResponse>(_HandleRequestResponse_QNAME, HandleRequestResponse.class, null, value);
    }

}
