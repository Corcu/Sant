package calypsox.tk.pricer;

import calypsox.tk.util.mxmtm.MxMtmTradeHandler;
import calypsox.tk.util.mxmtm.RepoMtmTradeHandler;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerInput;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;

import java.util.Optional;

public class PricerMeasureMTM_NET_MUREX extends PricerMeasure {

	public static final String MTM_NET_MUREX="MTM_NET_MUREX";


	@Override
	public void calculateInactive(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer) throws PricerException {
		this.calculate(trade, valDatetime, env, pricer, new PricerInput());
	}


	@Override
	public void calculate(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer, PricerInput input) throws PricerException {
		String productType= Optional.ofNullable(trade).map(Trade::getProduct).map(Product::getType).orElse(Repo.class.getSimpleName());
		MTM_NET_MUREXCalculator calculator=getCalculatorInstance(productType);
		if(calculator!=null){
			calculator.calculate(trade,valDatetime,env,pricer,this);
		}

	}


	@Override
	public boolean isImplementedByPricer(Pricer pricer) {
		return true;
	}


	public MTM_NET_MUREXCalculator getCalculatorInstance(String productType){
		MTM_NET_MUREXCalculator calculator=null;
		if(productType!=null) {
			String simpleClassName = productType+MTM_NET_MUREXCalculator.class.getSimpleName();
			try {
				String fullClassName=MTM_NET_MUREXCalculator.class.getName().replace(MTM_NET_MUREXCalculator.class.getSimpleName(),simpleClassName);
				calculator = Optional.of(Class.forName(fullClassName))
						.map(this::createInstance).orElse(null);
			} catch (ClassNotFoundException exc) {
				Log.error(MxMtmTradeHandler.class, exc.getCause());
			}
		}
		return calculator;
	}

	private MTM_NET_MUREXCalculator createInstance(Class<?> calculator){
		MTM_NET_MUREXCalculator calculatorInstance=null;
		try {
			Object classInstance=calculator.newInstance();
			if(classInstance instanceof MTM_NET_MUREXCalculator){
				calculatorInstance= (MTM_NET_MUREXCalculator) classInstance;
			}
		} catch (InstantiationException | IllegalAccessException exc) {
			Log.error(MTM_NET_MUREXCalculator.class,exc.getCause());
		}
		return calculatorInstance;
	}
}
