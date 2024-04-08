/**
 * 
 */
package calypsox.tk.util;

import java.io.File;

import calypsox.regulation.util.FileRowsTransposer;

import com.calypso.infra.util.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

/**O
 * Quick ST to process the file produced by the ST ScheduledTaskCSVREPORT.
 * 
 * @author aela
 * 
 */
public class ScheduledTaskEMIR_EXTRACT extends ScheduledTaskCSVREPORT {

	@Override
	public boolean process(DSConnection arg0, PSConnection arg1) {

		boolean processOK = super.process(arg0, arg1);

		if (processOK) {
			// transpose the temporary file
			String producedFile = getFileName();
			if (!Util.isEmpty(producedFile)
					&& producedFile.startsWith("file://")) {

				producedFile = producedFile.substring(7, producedFile.length()-3);
				producedFile += getAttribute("REPORT FORMAT");
			}
			File file = new File(producedFile);
			if (file.exists() && !file.isDirectory()) {
				FileRowsTransposer.transposeFile(producedFile,
						getAttribute(ScheduledTaskCSVREPORT.DELIMITEUR), null);
			}
		}

		return processOK;
	}

}
