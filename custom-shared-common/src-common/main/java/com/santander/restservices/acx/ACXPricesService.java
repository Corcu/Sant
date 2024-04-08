package com.santander.restservices.acx;

import com.santander.restservices.ApiRestModel;
import com.santander.restservices.acx.model.ACXPriceError;
import com.santander.restservices.acx.model.ACXPricesInput;
import com.santander.restservices.acx.model.ACXPricesOutput;

/**
 * ACX prices REST service Client
 * Service:
 * Generates a list of end of day prices for catalog parametrized.
 * For lighter requests where the json with the prices is returned directly.
 *
 * @author x865229
 * date 25/11/2022
 * @link <a href="https://confluence.alm.europe.cloudcenter.corp/display/CIBACX/v3.0.0+-+ACX">...</a>
 */
public class ACXPricesService extends ACXServiceBase {

    public static final String SERVICE_NAME = "ACX_Prices";

    private ACXPricesInput input = new ACXPricesInput();
    private ACXPricesOutput output = new ACXPricesOutput(null);
    private ACXPriceError error = new ACXPriceError();

    public ACXPricesService() {
        super(SERVICE_NAME);
    }

    @Override
    public boolean validateParameters() {
        return input.checkModelDataLoaded();
    }

    @Override
    public ACXPricesInput getInput() {
        return input;
    }

    @Override
    public ACXPricesOutput getOutput() {
        return output;
    }

    @Override
    public ApiRestModel getError() {
        return error;
    }

}
