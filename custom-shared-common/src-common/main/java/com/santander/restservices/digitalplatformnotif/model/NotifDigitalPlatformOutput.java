package com.santander.restservices.digitalplatformnotif.model;

import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class NotifDigitalPlatformOutput extends ApiRestModelRoot{

	@Override
	public boolean checkModelDataLoaded() {
		return true;
	}

	@Override
	public void loadModelData(ApiRestModel model) {
		
	}

	@Override
	public Class<? extends ApiRestModel> retriveModelClass() {
		return  ApiRestModelRoot.class;
	}

}
