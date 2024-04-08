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

public class ScheduledTaskGenSignalFileBad extends AbstractProcessFeedScheduledTask {

	// START OA 27/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = 2450087654201L;

	// END OA OA 27/11/2013

	@Override
	public boolean process(final DSConnection paramDSConnection, final PSConnection paramPSConnection) {

		boolean boolReturn = true;

		final String path = getAttribute(FILEPATH);
		String startFileName = getAttribute(STARTFILENAME);
		// String pathFileName = path + startFileName + "_SIGNAL.txt";
		String pathFileName = path + "SIGNAL_" + startFileName + ".txt";

		try {
			final FileWriter writer = new FileWriter(pathFileName);
			writer.write("Reprocess to be done.\n");
			writer.close();
		} catch (final FileNotFoundException e) {
			Log.error(this, "The filename is not valid. Please configure the scheduled task with a valid filename: "
					+ pathFileName, e);
			boolReturn = false;
		} catch (final IOException e) {
			Log.error(this, "An error ocurred while writing the files: " + pathFileName, e);
			boolReturn = false;
		}

		return boolReturn;
	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTaskInformation() {
		// TODO Auto-generated method stub
		return null;
	}
}
