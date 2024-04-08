package com.calypso.tk.upload.jaxb;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CustomDataListAdapter {

    private final CustomDataList customDataList;

    private final String pricingEnvCustomDataName="uploadConfig-PricingEnv";
    private final String pricingEnvCustomDataV="Uploader";


    public CustomDataListAdapter(CalypsoTrade calypsoTrade){
        this.customDataList= calypsoTrade.getCustomDataList();
    }

    public void addPricingEnvCustomData(){
        List<CustomData> customDataChildList= Optional.ofNullable(this.customDataList.getCustomData())
                        .orElse(new ArrayList<>());
        if(isEmptyUploadedPricingEnvConfig(customDataChildList)) {
            customDataChildList.add(buildPricingEnvCustomData());
            this.customDataList.customData = customDataChildList;
        }
    }

    public CustomData buildPricingEnvCustomData(){
        CustomData pricingEnvCustomData=new CustomData();
        pricingEnvCustomData.name=pricingEnvCustomDataName;
        pricingEnvCustomData.value=pricingEnvCustomDataV;
        return pricingEnvCustomData;
    }

    public CustomDataList getCustomDataList(){
        return this.customDataList;
    }

    private boolean isEmptyUploadedPricingEnvConfig(List<CustomData> customDataChildList){
        for(CustomData customData:customDataChildList){
            if(pricingEnvCustomDataName.equals(customData.name)
                    &&pricingEnvCustomDataV.equals(customData.value)){
                return false;
            }
        }
        return true;
    }
}
