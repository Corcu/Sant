package calypsox.tk.pricer;

import java.util.List;
import java.util.TimeZone;

import com.calypso.helper.RemoteAPI;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerInput;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.util.SecFinanceSecuritiesUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMark;

import calypsox.tk.util.ScheduledTaskImportRepoMtM;
import calypsox.util.collateral.CollateralUtilities;

public class PricerMeasureBOND_AMORT extends PricerMeasure {
	public void calculateInactive(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer) throws PricerException {
		this.calculate(trade, valDatetime, env, pricer, new PricerInput());
	}


	public void calculate(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer, PricerInput input) throws PricerException {
		if (trade == null || trade.getProduct() == null || !(trade.getProduct() instanceof Repo)) {
			return;
		}
		
		Repo repo = (Repo)trade.getProduct();
		JDate valeDate = valDatetime.getJDate(TimeZone.getDefault());
		
		List<Collateral> collaterals = SecFinanceSecuritiesUtil.getActiveSec(repo, valeDate);
		double remaining = 0.0D;
		if (!Util.isEmpty(collaterals)) {
			Collateral collateral = collaterals.get(0);
			remaining = repo.getRemainingQuantity(collateral, valeDate);
			
			double faceValue = collateral.getSecurity().getPrincipal(valeDate);
	        remaining *= faceValue;
	        
	        this.setValue(Math.abs(remaining));
			this.setCurrency(collateral.getCurrency()); 
		}
		else {
			this.setValue(0.0D);
		}
	}


	public boolean isImplementedByPricer(Pricer pricer) {
		return true;
	}
}
