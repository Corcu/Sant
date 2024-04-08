package calypsox.repoccp.reader;

import calypsox.repoccp.model.ReconCCP;
import calypsox.repoccp.model.ReconCCPTrade;
import com.calypso.tk.core.Log;
import io.reactivex.Emitter;
import io.reactivex.Flowable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Used for reading large xml files with a reasonable and constant memory usage
 *
 * @author aalonsop
 */
public abstract class XmlStaxReader implements ReconCCPReader {
    @Override
    public List<ReconCCP> read(String filePath) {
        List<ReconCCP> mappedObjects = new ArrayList<>();
        XMLStreamReader reader = null;
        try {

            reader = getStreamReader(filePath);

            while (reader.hasNext()) {
                ReconCCP currentObject = next(reader);
                if (currentObject != null) {
                    mappedObjects.add(currentObject);
                }
            }
        } catch (XMLStreamException | FileNotFoundException exc) {
            Log.error(this, exc);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    Log.error(this, e);
                }
            }
        }

        return mappedObjects;
    }

    public static XMLStreamReader getStreamReader(String filePath) throws XMLStreamException, FileNotFoundException {
        final InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
        return XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
    }

    /*
     * Factory for processing trades from different exchanges
     *  LV4V: BANQUE CENTRALE DE COMPENSATION
     * 	ECAG: EUREX CLEARING AG
     * 	LGWM: LCH CLEARNET – REPOCLEAR (www.lch.com)
     * 	5MSR: BME CLEARING SA
     */
    public static ReconCCPReader chooseReader(String filepath) throws XMLStreamException, FileNotFoundException {
        XMLStreamReader reader = getStreamReader(filepath);
        if (reader.hasNext()) {
            reader.next();
            String namespace = reader.getNamespaceURI();
            if (namespace.contains("www.lch.com")) {
                return new LCHStaxReader();
            }
            /*else if (namespace.contains("www.eurex.com")) {
                return new EUREXStaxReader();
            } else if (namespace.contains("www.bolsasymercados.es")) {
                return new BMEStaxReader();
            } else if (namespace.contains("www.lchsa?.com")) {
                return new LCHSAStaxReader();
            }*/
        }
        reader.close();
        return null;
    }

    public abstract ReconCCP next(XMLStreamReader reader) throws XMLStreamException;

}
