package calypsox.tk.util.optimizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;

import calypsox.tk.interfaces.optimizer.fileready.FileReady;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;

public class FileReadyUtil {

	private static JAXBContext JC = null;
	private static Unmarshaller UNMARSHALLER = null;
	private static Marshaller MARSHALLER = null;

	private static JAXBContext getJAXBContext() throws JAXBException {
		if (JC == null) {
			JC = JAXBContext
					.newInstance("calypsox.tk.interfaces.optimizer.fileready");
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

	public static FileReady parseFile(File file) throws Exception {
		FileReady fileReady = (FileReady) getUnmarshaller().unmarshal(file);
		return fileReady;
	}

	public static File generateFile(File file, FileReady fileReady)
			throws Exception {
		getMarshaller().marshal(fileReady, file);
		return file;
	}

	public static XMLGregorianCalendar toXMLGregorianCalendar(JDate date) {
		GregorianCalendar gCalendar = new GregorianCalendar();
		gCalendar.setTime(date.getDate(TimeZone.getDefault()));
		XMLGregorianCalendar xmlCalendar = null;
		try {
			xmlCalendar = DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(gCalendar);
		} catch (DatatypeConfigurationException e) {
			Log.error(FileReadyUtil.class.getName(), e);
		}
		return xmlCalendar;
	}

	public static XMLGregorianCalendar toXMLGregorianCalendar(JDatetime datetime) {
		GregorianCalendar gCalendar = new GregorianCalendar();
		gCalendar.setTimeInMillis(datetime.getTime());
		XMLGregorianCalendar xmlCalendar = null;
		try {
			xmlCalendar = DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(gCalendar);
		} catch (DatatypeConfigurationException e) {
			Log.error(FileReadyUtil.class.getName(), e);
		}
		return xmlCalendar;
	}

	/*
	 * Converts XMLGregorianCalendar to java.util.Date in Java
	 */
	public static Date toDate(XMLGregorianCalendar calendar) {
		if (calendar == null) {
			return null;
		}
		return calendar.toGregorianCalendar().getTime();
	}

	public static FileReady parseStream(StreamSource ss) throws Exception {
		JAXBElement<FileReady> fileReady = (JAXBElement<FileReady>) getUnmarshaller()
				.unmarshal(ss, FileReady.class);
		return fileReady == null ? null : fileReady.getValue();
	}

	public static String generateStream(FileReady fileReady) throws Exception {
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		getMarshaller().marshal(fileReady, byteArrayStream);
		return byteArrayStream != null ? byteArrayStream.toString() : null;
	}
}
