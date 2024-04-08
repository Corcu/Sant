package calypsox.tk.export;

import com.calypso.tk.bo.BOMessage;

public interface AbstractUploaderXMLDataExporter  {
	String export(Object sourceObject, UploaderXMLDataExporter exporter);
	void fillInfo(Object sourceObject, UploaderXMLDataExporter exporter, BOMessage boMessage);
	void linkBOMessage(Object sourceObject, BOMessage boMessage);
	String getIdentifier(Object sourceObject);
}
