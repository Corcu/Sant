package calypsox.tk.bo;

import com.calypso.tk.bo.AbstractXMLFormatter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.AdviceDocumentBuilder;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.export.DataExportBuilder;
//import com.calypso.tk.export.DataExportFileBuilder;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;

/**
 * Generate DATAEXPORTERMSG DataUploader Format on Local
 */
public class DATAEXPORTERMSGXMLFormatter extends AbstractXMLFormatter {

    @Override
    public AdviceDocument generate(PricingEnv env, BOMessage message, boolean newDocument, DSConnection dsCon) throws MessageFormatException {
        String format = message.getAttribute("exportFormat");
        if (!Util.isEmpty(format)) {
            PSEventMessage event = new PSEventMessage();
            event.setBoMessage(message);
            String mesage = this.createExportBuilder(format).exportInUploaderXML(message);

            AdviceDocumentBuilder docBuilder = AdviceDocumentBuilder.create(message).datetime(DSConnection.getDefault().getServerCurrentDatetime());
            if (mesage != null) {
                docBuilder.document(new StringBuffer(mesage));
            }

            docBuilder.characterEncoding(MessageFormatter.getCharacterEncoding(message, dsCon));
            docBuilder.userName(dsCon.getUser());
            return docBuilder.build();
        }
        return super.generate(env, message, newDocument, dsCon);
    }

    private DataExportBuilder createExportBuilder(String format) {
        DataExporterConfig exporterDataConfig = new DataExporterConfig();
        // new DataExportFileBuilder(exporterDataConfig, format);

        return null;
    }
}
