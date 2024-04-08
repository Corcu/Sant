package calypsox.tk.util;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

/**
 * Rename the credit ratings configurations so they will nort reflect the contract name
 * 
 * @author aela
 */
public class ScheduledTaskRenameHaircutAndCreaditRating extends ScheduledTask {

	private static final String TASK_INFORMATION = "Rename the credit ratings configurations so they will nort reflect the contract name.";
	private final Map<Integer, MarginCallCreditRatingConfiguration> creditConfiguration = new HashMap<Integer, MarginCallCreditRatingConfiguration>();

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	@Override
	public boolean process(final DSConnection dsCon, final PSConnection connPS) {
		boolean result = true;
		// *******************
		// * Launch the main handling service
		// *******************
		try {
			result = renameProcess(dsCon);
		} catch (Exception e) {
			Log.error(this, e);
		}
		return result;
	}

	/**
	 * @param dsCon
	 * @return
	 */
	private boolean renameProcess(DSConnection dsCon) {
		boolean result = true;
		try {
			List<MarginCallCreditRatingConfiguration> remoteCreditConfiguration = ServiceRegistry.getDefault()
					.getCollateralServer().getAllMarginCallCreditRatingConfig();
			if (!Util.isEmpty(remoteCreditConfiguration)) {
				for (MarginCallCreditRatingConfiguration mccrc : remoteCreditConfiguration) {
					this.creditConfiguration.put(mccrc.getId(), mccrc);
				}
			}

			// get the list of all contracts
			List<CollateralConfig> contracts = ServiceRegistry.getDefault().getCollateralDataServer()
					.getAllMarginCallConfig();
			for (CollateralConfig cc : contracts) {
				if (cc == null) {
					continue;
				}
				try {
					renameCreditrating(cc);
				} catch (Exception e) {
					result = false;
					Log.error(this, e);
				}
			}

		} catch (RemoteException e) {
			result = false;
			Log.error(this, e);
		}
		return result;
	}

	/**
	 * @param cc
	 * @throws RemoteException
	 */
	private void renameCreditrating(CollateralConfig cc) throws RemoteException {
		int poRatingId = cc.getPoRatingsConfigId();
		int leRatingId = cc.getLeRatingsConfigId();
		boolean isSameID = false;

		if (poRatingId == leRatingId) {
			isSameID = true;
		}

		if (isSameID) {
			if (poRatingId > 0) {
				MarginCallCreditRatingConfiguration poRating = this.creditConfiguration.get(poRatingId);
				if (poRating != null) {
					String oldName = poRating.getName();
					poRating.setName("MAD_BOTH_" + cc.getId());
					ServiceRegistry.getDefault().getCollateralServer().save(poRating, null);
					Log.system("RENAMING_HC_CR", "Renaming BOTH Credit rating : " + cc.getId() + ";" + oldName + ";"
							+ poRating.getName());
				}
			}
		} else {
			if (poRatingId > 0) {
				MarginCallCreditRatingConfiguration poRating = this.creditConfiguration.get(poRatingId);
				if (poRating != null) {
					String oldName = poRating.getName();
					poRating.setName("MAD_PO_" + cc.getId());
					ServiceRegistry.getDefault().getCollateralServer().save(poRating, null);
					Log.system("RENAMING_HC_CR", "Renaming PO Credit rating : " + cc.getId() + ";" + oldName + ";"
							+ poRating.getName());
				}
			}
			if (leRatingId > 0) {
				MarginCallCreditRatingConfiguration leRating = this.creditConfiguration.get(leRatingId);
				if (leRating != null) {
					String oldName = leRating.getName();
					leRating.setName("MAD_LE_" + cc.getId());
					ServiceRegistry.getDefault().getCollateralServer().save(leRating, null);
					Log.system("RENAMING_HC_CR", "Renaming LE Credit rating : " + cc.getId() + ";" + oldName + ";"
							+ leRating.getName());

				}
			}
		}
	}
}