/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

public class ScheduledTaskGenerationSignalFileMIS extends ScheduledTaskCSVREPORT {

	// START OA 27/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = 15488701865L;

	// END OA OA 27/11/2013

	@Override
	public boolean process(final DSConnection paramDSConnection, final PSConnection paramPSConnection) {

		boolean boolReturn = false;

		// super
		if (super.process(paramDSConnection, paramPSConnection)) {

			// file generation
			String fileName = getFileName();
			if (fileName.startsWith("file://")) {
				fileName = fileName.substring(7);
			}
			fileName = fileName.substring(0, fileName.indexOf('.')) + ".eof";

			try {
				final FileWriter writer = new FileWriter(fileName);
				writer.write("File generation done.\n");
				writer.close();
				boolReturn = true;
			} catch (final FileNotFoundException e) {
				Log.error(this,
						"The filename is not valid. Please configure the scheduled task with a valid filename: "
								+ fileName, e);
			} catch (final IOException e) {
				Log.error(this, "An error ocurred while writing the files: " + fileName, e);
			}

		}
		return boolReturn;
	}
}
