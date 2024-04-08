package calypsox.tk.collateral.marginCall.validator;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCall;
import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportConstants;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.util.List;

public class ExternalMarginCallBeanValidator implements MarginCallBeanValidator {

    private static final String COLLAT_SECURITY = "COLLAT_SECURITY";
    private static final String COLLAT_CASH = "COLLAT_CASH";
    private static final String COLLATERAL = "COLLATERAL";
    private static final String SECURITY = "SECURITY";
    private static final String SLB_BUNDLE = "SLB_BUNDLE";
    private final static String PO = "ProcessingOrg";
    private DSConnection conn;
    private MarginCall mc;

    protected RemoteSantCollateralService remoteColService = null;
    //private String SLB_Bundle;

    protected ExternalMarginCallImportContext context;

    public ExternalMarginCallBeanValidator(MarginCall mc,
                                           ExternalMarginCallImportContext context) {
        this.mc = mc;
        this.context = context;
    }

//	public ExternalMarginCallBeanValidator(MarginCall mc,
//			ExternalMarginCallImportContext context, String slb_contract) {
//		this.mc = mc;
//		this.context = context;
//	}

    @Override
    public boolean validate(ExternalMarginCall bean,
                            List<MarginCallImportErrorBean> errors) {

        ExternalMarginCallBean mcBean = (ExternalMarginCallBean) bean;

        boolean isValid = true;

        // check the CollatType
        if (!isValidCollatType(mcBean.getInstrument())) {
            errors.add(new MarginCallImportErrorBean(
                    ExternalMarginCallImportConstants.ERR_INVALID_VALUE,
                    "Invalid value for field PORTFOLIO:  "
                            + mcBean.getInstrument(), mcBean));
            isValid = false;
        }

        // check the action
        if (!isValidAction(mcBean.getAction())) {
            errors.add(new MarginCallImportErrorBean(
                    ExternalMarginCallImportConstants.ERR_INVALID_VALUE,
                    "Invalid value for field action:  " + mcBean.getAction(),
                    mcBean));
            isValid = false;
        }

        // check currency
        isValid = isValid && checkCurrency(mcBean, errors);

        // check the book
        isValid = isValid && checkBook(mcBean, errors);

        // check the existence of the trade PDV and get the corresponding
        // contract
        isValid = isValid && checkCollateralConfig(mcBean, errors);

        if (mcBean.getCollateralConfig() == null) {
            return false;
        }

        // Create MarginCall
        if (mc == null) {
            mc = createMarginCall(mcBean, errors);
        }
        if (mc == null /* || mc.getId() <= 0 */) {
            errors.add(new MarginCallImportErrorBean(null,
                    "Unable to load the margin call entry for the contract ",
                    mcBean));
            return false;
        }

        // Set the MarginCall to bean
        mcBean.setMarginCall(mc);

        return isValid;
    }

    /**
     * Check the book
     *
     * @param mcBean
     * @param errors
     * @return
     */
    private boolean checkBook(ExternalMarginCallBean mcBean,
                              List<MarginCallImportErrorBean> errors) {
        Book book = BOCache.getBook(DSConnection.getDefault(),
                mcBean.getPortfolio());

        if (book == null) {
            errors.add(new MarginCallImportErrorBean(
                    ExternalMarginCallImportConstants.ERR_BOOK_NOT_VALID,
                    "Invalid Book "
                            + (Util.isEmpty(mcBean.getPortfolio()) ? ""
                            : mcBean.getPortfolio()), mcBean));
            return false;
        }

        return true;
    }

    /**
     * Checks the currency
     *
     * @param mcBean
     * @param errors
     * @return
     */
    private boolean checkCurrency(ExternalMarginCallBean mcBean,
                                  List<MarginCallImportErrorBean> errors) {
        CurrencyDefault cd = LocalCache.getCurrencyDefault(mcBean
                .getAmountCcy());
        if (cd == null) {
            errors.add(new MarginCallImportErrorBean(
                    ExternalMarginCallImportConstants.ERR_CCY_NOT_FOUND,
                    " Currency " + mcBean.getAmountCcy()
                            + " is not valid or not setup in the system",
                    mcBean));
            return false;
        }

        return true;
    }

    /**
     * @param mcBean
     * @param errors
     * @return
     */
    private boolean checkCollateralConfig(ExternalMarginCallBean mcBean,
                                          List<MarginCallImportErrorBean> errors) {

        boolean isContractFound = false;

        if (mcBean.getCollateralConfig() != null
                && mcBean.getCollateralConfig().getId() > 0) {
            return true;
        }

        // -------------------- TODO -------------------------

        // Falta que Albero me mande la query para obtener el contrato a partir
        // del PORTFOLIO

        final Book book = BOCache.getBook(DSConnection.getDefault(),
                mcBean.getPortfolio());
        final LegalEntity processingOrg = BOCache.getLegalEntity(
                DSConnection.getDefault(), book.getProcessingOrgBasedId());

        try {
            final MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
            mcFilter.setLegalEntity(mcBean.getCounterparty());
            mcFilter.setProcessingOrg(processingOrg.getCode());
            final List<CollateralConfig> listContracts = CollateralManagerUtil
                    .loadCollateralConfigs(mcFilter);

            if (!Util.isEmpty(listContracts)) {

                for (CollateralConfig contract : listContracts) {

                    //Checking the SLB Bundle attribute with its col value at the Margin Call
                    if (mcBean.getSLB_BUNDLE() != null) {
                        try {
                            String slbBundle = mcBean.getSLB_BUNDLE();
                            String fieldValue = contract.getAdditionalField(SLB_BUNDLE);
                            if (slbBundle.equalsIgnoreCase(fieldValue)) {
                                mcBean.setCollateralConfig(contract);
                                isContractFound = true;
                                return isContractFound;
                            }
                        } catch (Exception e) { //new error/warning about SLB_BUNDLE
                            Log.error(this, "Additional field SLB_BUNDLE is not correct for that Margin Call.", e);
                        }
                    }
                }

                if (listContracts.get(0) != null) {
                    // Set the collateralConfig
                    mcBean.setCollateralConfig(listContracts.get(0));
                    isContractFound = true;
                }
            }

        } catch (CollateralServiceException e) {
            Log.error(this, "Contract not found.", e);
        }


        return isContractFound;
    }

    private boolean isValidAction(String action) {
        for (PDVConstants.PDV_ACTION act : PDVConstants.PDV_ACTION.values()) {
            if (act.name().equals(action)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidCollatType(String collatType) {
        for (PDVConstants.PDV_COLLAT_TYPE colType : PDVConstants.PDV_COLLAT_TYPE
                .values()) {
            if (colType.name().equals(collatType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the value of the additionalField is the same of the contract
     *
     * @param mcBean
     *
     * @return boolean
     */

//	private boolean isValidCollateralConfig(ExternalMarginCallBean mcBean,List<CollateralConfig>contractConfigList) {
//		boolean isContractOK = false;
//		String additionalField = "SLB_BUNDLE";
//		String valueField; //mcBean.getSLB_BUNDLE();
//		List<CollateralConfig> contracts = new ArrayList<CollateralConfig>();
//		String slb_value = mcBean.getSLB_BUNDLE();
//		this.remoteColService = (RemoteSantCollateralService) conn.getRMIService("baseSantCollateralService",
//                RemoteSantCollateralService.class);
//		HashMap<String, String> addtitionalFields = new HashMap<>();
//        //addtitionalFields.put(additionalField,valueField);
//        
//		for (CollateralConfig collateralConfig : contractConfigList) {//get the list of contracts by additional field
//			if(remoteColService!=null) {
//	            try {
//	            	valueField=collateralConfig.getAdditionalField(additionalField);
//	                addtitionalFields.put(additionalField,valueField);
//	                contracts.add(remoteColService.getMarginCallConfigByAdditionalField(addtitionalFields).get(0));
//	            } catch (PersistenceException e) {
//	                Log.error(this,"Cannot load contracts: " + e);
//	            }
//	        }
//		if(contractConfigList.containsAll(contracts)) {
//			isContractOK=true;
//			return isContractOK;
//		}
//			
//		}
//
//		return isContractOK;
//	}


    /**
     * Create the MarginCall from file data.
     *
     * @param mcBean
     * @param errors
     * @return
     */
    private MarginCall createMarginCall(ExternalMarginCallBean mcBean,
                                        List<MarginCallImportErrorBean> errors) {

        final CollateralConfig cc = mcBean.getCollateralConfig();
        final int ccId = cc.getId();
        final Book book = cc.getBook();
        final String ccy = mcBean.getAmountCcy();
        final double principal = mcBean.getAmount();

        final String underlyingType = mcBean.getUnderlyingType();
        final String underlying = mcBean.getUnderlying();

        // CollateralType
        String collateralType = "";
        final String instrument = mcBean.getInstrument();
        if (COLLAT_SECURITY.equals(instrument)) {
            collateralType = SECURITY;
        } else if (COLLAT_CASH.equals(instrument)) {
            collateralType = COLLATERAL;
        }

        // Underlying and UnderlyingType -> Product Security
        Product productByCode = null;
        if (!Util.isEmpty(underlying) && !Util.isEmpty(underlyingType)) {
            try {
                productByCode = DSConnection.getDefault().getRemoteProduct()
                        .getProductByCode(underlyingType, underlying);
            } catch (RemoteException e) {
                Log.error(this, "Security not found with " + underlyingType
                        + " = " + underlying, e);
            }
        }


        // Create Margin Call
        MarginCall mc = new MarginCall();
        mc.setSecurity(null);
        mc.setFlowType(collateralType);
        mc.setLinkedLongId(ccId);
        mc.setCurrencyCash(ccy);
        if (null != book) {
            mc.setOrdererLeId(book.getLegalEntity().getId());
        }
        mc.setOrdererRole(PO);
        mc.setPrincipal(principal);

        if (productByCode != null) {
            mc.setSecurity(productByCode);
        }

        return mc;
    }
}
