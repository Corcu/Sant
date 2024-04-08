//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantacion de la referencia de enlace (JAXB) XML v2.2.8-b130911.1802 
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perderan si se vuelve a compilar el esquema de origen. 
// Generado el: 2020.02.03 a las 09:32:46 AM CET 
//


package calypsox.engine.dataimport.BondElements;

import java.math.BigDecimal;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the calypsox.engine.dataimport.BondElements package. 
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

    private final static QName _Symbol_QNAME = new QName("", "symbol");
    private final static QName _FechaCallFin_QNAME = new QName("", "fechaCallFin");
    private final static QName _Subscriber_QNAME = new QName("", "subscriber");
    private final static QName _FechaCall_QNAME = new QName("", "fechaCall");
    private final static QName _PoolFactorValue_QNAME = new QName("", "poolFactorValue");
    private final static QName _PoolFactorDate_QNAME = new QName("", "poolFactorDate");
    private final static QName _DivisaLiquid_QNAME = new QName("", "divisaLiquid");
    private final static QName _EjercicioOp_QNAME = new QName("", "ejercicioOp");
    private final static QName _FacAmortOp_QNAME = new QName("", "facAmortOp");
    private final static QName _CouponRedemptionPaymentCall_QNAME = new QName("", "couponRedemptionPaymentCall");
    private final static QName _PorcentajeNomOp_QNAME = new QName("", "porcentajeNomOp");
    private final static QName _CantidadAmortOp_QNAME = new QName("", "cantidadAmortOp");
    private final static QName _CouponRedemptionPaymentPF_QNAME = new QName("", "couponRedemptionPaymentPF");
    private final static QName _NumTitulosCall_QNAME = new QName("", "numTitulosCall");
    private final static QName _StrikePriceCall_QNAME = new QName("", "strikePriceCall");
    private final static QName _Action_QNAME = new QName("", "action");
    private final static QName _TransactionKey_QNAME = new QName("", "transactionKey");
    private final static QName _FechaEjercicio_QNAME = new QName("", "fechaEjercicio");
    private final static QName _FormaEjercicioOp_QNAME = new QName("", "formaEjercicioOp");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: calypsox.engine.dataimport.BondElements
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PoolFactor }
     * 
     */
    public PoolFactor createPoolFactor() {
        return new PoolFactor();
    }

    /**
     * Create an instance of {@link ListaPoolFactor }
     * 
     */
    public ListaPoolFactor createListaPoolFactor() {
        return new ListaPoolFactor();
    }

    /**
     * Create an instance of {@link ServiceOptionExercise }
     * 
     */
    public ServiceOptionExercise createServiceOptionExercise() {
        return new ServiceOptionExercise();
    }

    /**
     * Create an instance of {@link ListaCall }
     * 
     */
    public ListaCall createListaCall() {
        return new ListaCall();
    }

    /**
     * Create an instance of {@link FecCall }
     * 
     */
    public FecCall createFecCall() {
        return new FecCall();
    }

    /**
     * Create an instance of {@link RDFlowTransaction }
     * 
     */
    public RDFlowTransaction createRDFlowTransaction() {
        return new RDFlowTransaction();
    }

    /**
     * Create an instance of {@link ListSubscribers }
     * 
     */
    public ListSubscribers createListSubscribers() {
        return new ListSubscribers();
    }

    /**
     * Create an instance of {@link ServicePoolFactor }
     * 
     */
    public ServicePoolFactor createServicePoolFactor() {
        return new ServicePoolFactor();
    }

    /**
     * Create an instance of {@link RDFlowXML }
     * 
     */
    public RDFlowXML createRDFlowXML() {
        return new RDFlowXML();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "symbol")
    public JAXBElement<String> createSymbol(String value) {
        return new JAXBElement<String>(_Symbol_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "fechaCallFin")
    public JAXBElement<String> createFechaCallFin(String value) {
        return new JAXBElement<String>(_FechaCallFin_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "subscriber")
    public JAXBElement<String> createSubscriber(String value) {
        return new JAXBElement<String>(_Subscriber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "fechaCall")
    public JAXBElement<String> createFechaCall(String value) {
        return new JAXBElement<String>(_FechaCall_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "poolFactorValue")
    public JAXBElement<BigDecimal> createPoolFactorValue(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_PoolFactorValue_QNAME, BigDecimal.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "poolFactorDate")
    public JAXBElement<String> createPoolFactorDate(String value) {
        return new JAXBElement<String>(_PoolFactorDate_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "divisaLiquid")
    public JAXBElement<String> createDivisaLiquid(String value) {
        return new JAXBElement<String>(_DivisaLiquid_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "ejercicioOp")
    public JAXBElement<String> createEjercicioOp(String value) {
        return new JAXBElement<String>(_EjercicioOp_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "facAmortOp")
    public JAXBElement<String> createFacAmortOp(String value) {
        return new JAXBElement<String>(_FacAmortOp_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "couponRedemptionPaymentCall")
    public JAXBElement<String> createCouponRedemptionPaymentCall(String value) {
        return new JAXBElement<String>(_CouponRedemptionPaymentCall_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Byte }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "porcentajeNomOp")
    public JAXBElement<Byte> createPorcentajeNomOp(Byte value) {
        return new JAXBElement<Byte>(_PorcentajeNomOp_QNAME, Byte.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "cantidadAmortOp")
    public JAXBElement<String> createCantidadAmortOp(String value) {
        return new JAXBElement<String>(_CantidadAmortOp_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "couponRedemptionPaymentPF")
    public JAXBElement<String> createCouponRedemptionPaymentPF(String value) {
        return new JAXBElement<String>(_CouponRedemptionPaymentPF_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "numTitulosCall")
    public JAXBElement<String> createNumTitulosCall(String value) {
        return new JAXBElement<String>(_NumTitulosCall_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Byte }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "strikePriceCall")
    public JAXBElement<Byte> createStrikePriceCall(Byte value) {
        return new JAXBElement<Byte>(_StrikePriceCall_QNAME, Byte.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "action")
    public JAXBElement<String> createAction(String value) {
        return new JAXBElement<String>(_Action_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Integer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "transactionKey")
    public JAXBElement<Integer> createTransactionKey(Integer value) {
        return new JAXBElement<Integer>(_TransactionKey_QNAME, Integer.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "fechaEjercicio")
    public JAXBElement<String> createFechaEjercicio(String value) {
        return new JAXBElement<String>(_FechaEjercicio_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "formaEjercicioOp")
    public JAXBElement<String> createFormaEjercicioOp(String value) {
        return new JAXBElement<String>(_FormaEjercicioOp_QNAME, String.class, null, value);
    }

}
