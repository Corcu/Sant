package calypsox.tk.util.optimizer;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import calypsox.tk.interfaces.optimizer.importstatus.ImportStatusList;

public class ImportStatusUtil {

	private static JAXBContext JC = null;
	private static Unmarshaller UNMARSHALLER = null;
	private static Marshaller MARSHALLER = null;

	private static JAXBContext getJAXBContext() throws JAXBException {
		if (JC == null) {
			JC = JAXBContext
					.newInstance("calypsox.tk.interfaces.optimizer.importstatus");
		}
		return JC;
	}

	private static Unmarshaller getUnmarshaller() throws JAXBException {
		if (UNMARSHALLER == null) {
			JAXBContext jc = getJAXBContext();
			UNMARSHALLER = jc.createUnmarshaller();
		}

		return UNMARSHALLER;
	}

	private static Marshaller getMarshaller() throws JAXBException {
		if (MARSHALLER == null) {
			JAXBContext jc = getJAXBContext();
			MARSHALLER = jc.createMarshaller();
		}

		return MARSHALLER;
	}

	public static ImportStatusList parseFile(File file) throws Exception {
		ImportStatusList importStatus = (ImportStatusList) getUnmarshaller()
				.unmarshal(file);
		return importStatus;
	}

	public static File generateFile(File file, ImportStatusList importStatus)
			throws Exception {
		getMarshaller().marshal(importStatus, file);
		// getMarshaller().marshal(importStatus, System.out);
		return file;
	}

	public static ImportStatusList parseStream(StreamSource ss)
			throws Exception {
		JAXBElement<ImportStatusList> importStatusList = (JAXBElement<ImportStatusList>) getUnmarshaller()
				.unmarshal(ss, ImportStatusList.class);

		return importStatusList == null ? null : importStatusList.getValue();
	}
}
