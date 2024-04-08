//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantación de la referencia de enlace (JAXB) XML v2.2.6 
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perderán si se vuelve a compilar el esquema de origen. 
// Generado el: PM.04.23 a las 12:05:49 PM CEST 
//


package calypsox.tk.interfaces.optimizer.importstatus;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the calypsox.tk.interfaces.optimizer.importstatus package. 
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


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: calypsox.tk.interfaces.optimizer.importstatus
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ImportStatusList }
     * 
     */
    public ImportStatusList createImportStatusList() {
        return new ImportStatusList();
    }

    /**
     * Create an instance of {@link ImportStatusList.ImportStatus }
     * 
     */
    public ImportStatusList.ImportStatus createImportStatusListImportStatus() {
        return new ImportStatusList.ImportStatus();
    }

    /**
     * Create an instance of {@link Error }
     * 
     */
    public Error createError() {
        return new Error();
    }

    /**
     * Create an instance of {@link ErrorsList }
     * 
     */
    public ErrorsList createErrorsList() {
        return new ErrorsList();
    }

}
