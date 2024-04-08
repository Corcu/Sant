package calypsox.apps.collateral.eligibilityImporter;


import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.List;

public class EligibilityImporterBean {

    private CollateralConfig marginCallConfig;
    private int contractID;
    private String contractName;
    private int rowNumber;
    private StaticDataFilter staticDataFilter;
    private List<String> sdfName;
    private String side;

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public List<String> getSdfName() {
        return sdfName;
    }

    public void setSdfName(List<String> sdfName) {
        this.sdfName = sdfName;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public void setMarginCallConfig(CollateralConfig marginCallConfig) {
        this.marginCallConfig = marginCallConfig;
    }

    public int getContractID() {
        return contractID;
    }

    public void setContractID(int contractID) {
        this.contractID = contractID;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public StaticDataFilter getStaticDataFilter() {
        return staticDataFilter;
    }

    public void setStaticDataFilter(StaticDataFilter staticDataFilter) {
        this.staticDataFilter = staticDataFilter;
    }

    public CollateralConfig getMarginCallConfig() {
            return this.marginCallConfig;
    }

    public boolean isByName(){
        if(Util.isEmpty(contractName)) return false;
        return true;
    }
}
