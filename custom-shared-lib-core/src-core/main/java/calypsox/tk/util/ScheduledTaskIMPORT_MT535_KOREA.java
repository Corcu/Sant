package calypsox.tk.util;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;



import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskMESSAGE_MATCHING;

import calypsox.tk.util.korea.KoreaMT535Helper;
import calypsox.tk.util.korea.KoreaMT535Importer;

/**
 * 
 * @author x957355
 *
 */
public class ScheduledTaskIMPORT_MT535_KOREA extends ScheduledTaskMESSAGE_MATCHING {
	private static final long serialVersionUID = 5479627084237762635L;

	@Override
	public boolean process(DSConnection ds, PSConnection ps) {

		Map<String, LinkedList<HashMap<String, String>>> map = null;
		Boolean result = true;
		String path = this.getAttribute("InputDir");
		String file = this.getAttribute("Swift File");

		KoreaMT535Helper helper = new KoreaMT535Helper();
		KoreaMT535Importer importer = new KoreaMT535Importer();
		map = importer.readExcelFile(path + file);
		if(map == null || map.isEmpty()) {
			Log.error(this, "Can not read the file");
			return false;
		}
		
		result = helper.processExternalPositions(map);
		return result;
	}

}
