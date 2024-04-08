package calypsox.engine.medusa.utils.xml;

import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringWriter;

/**
 * Sant Marshaller
 *
 * @param <T> param
 * @author xIS15793
 */
public class SantMarshaller<T> {

    private final JAXBContext jc;

    /**
     * constructor
     *
     * @param packageName package name
     */
    public SantMarshaller(final String packageName) {
        try {
            this.jc = JAXBContext.newInstance(packageName);
        } catch (final JAXBException e) {
            throw new RuntimeException(e); // NOPMD
        }
    }

    /**
     * unmarshall
     *
     * @param xmlDocument document
     * @return T
     * @throws Exception exception
     */
    @SuppressWarnings("unchecked")
    public T unMarshall(final Document xmlDocument) throws Exception {
        final Unmarshaller unmarshaller = this.jc.createUnmarshaller();
        final T element = (T) unmarshaller.unmarshal(xmlDocument);
        return element;
    }

    /**
     * marshall
     *
     * @param t t
     * @return string
     * @throws Exception exception
     */
    public String marshall(final T t) throws Exception {
        final Marshaller marshaller = this.jc.createMarshaller();

        final StringWriter writer = new StringWriter();
        marshaller.marshal(t, writer);

        final StringBuffer buffer = writer.getBuffer();
        buffer.trimToSize();

        return buffer.toString().trim();

    }
}
