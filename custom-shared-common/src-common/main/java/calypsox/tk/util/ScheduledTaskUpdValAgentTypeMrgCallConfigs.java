package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

/**
 * @author jailson.viana
 */
public class ScheduledTaskUpdValAgentTypeMrgCallConfigs extends ScheduledTask {

	public static final String FILE = "File";
	private static Map<String, String> csvMap = new HashMap<>();

	@Override
	public boolean process(DSConnection ds, PSConnection ps) {
		boolean res = true;
		String filePath = getAttribute(FILE);

		File file = new File(filePath);

		FileReader fr;
		BufferedReader br;
		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line = " ";
			String[] tempArr;
			while ((line = br.readLine()) != null) {
				tempArr = line.split(",");

				String id = tempArr[0];
				String type = tempArr[1];

				csvMap.put(id, type);

			}
			br.close();
			updateContract();
		} catch (IOException e) {
			Log.error(this, e.getMessage());
		}

		return res;
	}

	private static void updateContract() throws CollateralServiceException {
		for (String s : csvMap.keySet()) {
			CollateralConfig contract = ServiceRegistry.getDefault().getCollateralDataServer()
					.getMarginCallConfig(Integer.valueOf(s));
			contract.setValuationAgentType(csvMap.get(s).toUpperCase());

			ServiceRegistry.getDefault().getCollateralDataServer().save(contract);

			Log.system(ScheduledTaskUpdValAgentTypeMrgCallConfigs.class.getName(),
					"Contract Updated: " + contract.getName());
		}
	}

	@Override
	public Vector getDomainAttributes() {
		final Vector result = super.getDomainAttributes();
		result.add(FILE);
		return result;
	}

	@Override
	public String getTaskInformation() {
		return "Update Agent Type Value in CollateralConfigs";
	}

}
