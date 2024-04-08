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
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Tenor;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.product.Repo;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

/**
 * @author jailson.viana
 */
public class ScheduledTaskUpdCouponCurrentTerm extends ScheduledTask {

	public static final String FILE = "File";
	private static Map<String, String> csvMap = new HashMap<>();
	private static Trade trade = null;
	private static Product product = null;
	private static Repo repo = null;
	private static Cash cash = null;

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
				String tenor = tempArr[1];

				csvMap.put(id, tenor);

			}
			br.close();
			updateCouponCurrentTerm();
		} catch (IOException e) {
			Log.error(this, e.getMessage());
		}

		return res;
	}

	private static void updateCouponCurrentTerm()
			throws CollateralServiceException, CalypsoServiceException, NumberFormatException {
		for (String s : csvMap.keySet()) {

			trade = ServiceRegistry.getDefault().getTradeServer().getTrade(Long.valueOf(s.trim()));

			product = trade.getProduct();

			Repo repoPledgeFather = null;

			Action action = Action.AMEND;
			
			if (product instanceof Repo) {
				repo = ((Repo) product);
				cash = repo.getCash();

				if (cash.getRateIndex() != null) {

					cash.getRateIndex().setTenor(new Tenor(Integer.valueOf(csvMap.get(s))));

					trade.setAction(action);
					
					ServiceRegistry.getDefault().getTradeServer().save(trade);

					Log.system(ScheduledTaskUpdValAgentTypeMrgCallConfigs.class.getName(),
							"Repo Updated: " + trade.getLongId());

				}

			} else if (product instanceof Pledge) {
				
				String internalReference = trade.getInternalReference();
				
				Trade fatherRepoPledge = getTripartyRepo(internalReference);
				
				repoPledgeFather = (Repo)fatherRepoPledge.getProduct();
				
				if (repoPledgeFather.getRateIndex() != null) {
					
					repoPledgeFather.getRateIndex().setTenor(new Tenor(Integer.valueOf(csvMap.get(s))));
					
					fatherRepoPledge.setAction(action);
					
					ServiceRegistry.getDefault().getTradeServer().save(fatherRepoPledge);
					
				}
				
			}

		}
	}

	private static Trade getTripartyRepo(String interalRef) {
		try {
			final long fatherTripartyRepoID = Long.parseLong(interalRef);
			return DSConnection.getDefault().getRemoteTrade().getTrade(fatherTripartyRepoID);
		} catch (CalypsoServiceException e) {
			Log.error(ScheduledTaskUpdCouponCurrentTerm.class, "Error parsing internalReference " + interalRef + " " + e);
		}
		return null;
	}

	@Override
	public Vector getDomainAttributes() {
		final Vector result = super.getDomainAttributes();
		result.add(FILE);
		return result;
	}

	@Override
	public String getTaskInformation() {
		return "Update tenor in repo operations";
	}

}
