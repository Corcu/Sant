package calypsox.tk.bo.obb;

import com.calypso.tk.core.JDate;

public interface OBBGenericMapInterface {

    String loadBranch();
    String loadAppIdentifier();
    String loadPostingID();
    JDate loadAccDate();
    String loadDescription();
    String loadEntity();
    String loadCenter();
    String loadProduct();
    String loadContract();
    String loadSubProduct();
    String loadSubAccount();
    String loadCurrency();
    String loadCcyCounterValue();
    Double loadAmount();
    Double loadAmountCounterValue();
    String loadOperatingPosition();
    String loadSign();
    String loadType();
    String loadTopic();
    String loadOriginContract();
    Double loadAmountSource();
    String loadCcySource();
    String loadAccIdentifier();

}
