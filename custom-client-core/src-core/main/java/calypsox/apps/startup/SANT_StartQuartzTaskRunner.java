/**
 * 
 */
package calypsox.apps.startup;

import java.util.HashMap;
import java.util.Map;

import com.calypso.apps.startup.StartQuartzTaskRunner;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

/**
 * @author aalonsop Customizes log and some input parameters -env
 *         <environmentname> -user <username> -password <password> -taskId
 *         <taskid> [-valDate <date in format yyyy.MM.dd>]
 */
public class SANT_StartQuartzTaskRunner extends StartQuartzTaskRunner {

	public static String SYSTEM_DATE_VALUE="SYSTEM";
	public static void main(String[] args) {
		Defaults.setIsEngine(true);
		startLog(args, "QuartzTaskRunner");
		startNOGUI(args);
	}

	public static void startNOGUI(String[] args) {
		String envName = null;
		String user = null;
		String passwd = null;
		String taskIdStr = null;
		String taskExtRef = null;
		String reportExternal = null;
		String executionStep = "requesting user input";
		String currDate = null;
		String valTime = null;
		String valTime_Timezone = null;
		String standaloneString = null;
		String executingUser = null;

		envName = getOption(args, "-env");
		user = getOption(args, "-user");
		passwd = getOption(args, "-password");
		taskIdStr = getOption(args, "-task");
		taskExtRef = getOption(args, "-taskExtRef");
		reportExternal = getOption(args, "-reportExternal");
		currDate = getOption(args, "-currDate");
		valTime = getOption(args, "-valTime");
		valTime_Timezone = getOption(args, "-valTime_Timezone");
		standaloneString = getOption(args, "-standalone");

		executingUser = getOption(args, "-executingUser");
		if (Log.isCategoryLogged("StartQuartzTaskRunner")) {
			Log.debug("StartQuartzTaskRunner", "Executing user: " + executingUser);
		}
	}

	/**
	 * If valDate args is SYSTEM, then returns the current Dataserver datetime
	 * @param date
	 * @return
	 */
	private static JDatetime proccessValueDate(String date) {
			if(date.equals(SYSTEM_DATE_VALUE))
				return DSConnection.getDefault().getServerCurrentDatetime();
			return null;
	}

	private static String[] proccessArguments(String[] args) {

		return null;
	}
}
