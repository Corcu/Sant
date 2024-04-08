package calypsox.apps.importer.adapter;


import java.rmi.RemoteException;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.infra.util.Util;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.processing.error.MessageType;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAdapter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.PersistenceSession;

import calypsox.engine.dataimport.CalypsoMLBondIEEngine;
import calypsox.processing.BondPreTranslator;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tools.santfilesaver.SantFileSaver;
import calypsox.tools.santfilesaver.SantFileSaverException;
import calypsox.util.collateral.CollateralUtilities;

/**
 * @author aela
 */
public class SantDefaultBondImporterAdapter extends SantanderDefaultImporterAdapter {

	private static final String MX_IS_MEX_BOND = "MX_IS_MEX_BOND";
	private static final String MX_TV_EMISORA_SERIE = "MX_TV_EMISORA_SERIE";
	private static final String CTE_Y = "Y";
	
    public SantDefaultBondImporterAdapter() throws AdapterException {
        super();
    }

    @Override
    public void importXML(final String s) throws AdapterException {
        try {
            importMessage(s, getPersistenceSession(), getUnmarshaller());
        } catch (final JAXBException jaxbexception) {
            throw new AdapterException("Errors occured while importing.", jaxbexception);
        } catch (final ErrorMessage errormessage) {
            throw new AdapterException("Errors occured while importing.", errormessage);
        }
    }

    /**
     * This method will translate the jaxbObject and save the translation using the persistenceSession.
     *
     * @param jaxbObject         the object to import.
     * @param persistenceSession the PersistenceSession to use to save the translated object.
     * @throws ErrorMessage if a translation or persistence error occurs.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void importObject(final com.calypso.jaxb.xml.Object jaxbObject,
                                final PersistenceSession persistenceSession) throws ErrorMessage {
        Bond bondTranslated;// = null;
        boolean save = true;
        try {
        	String bondsEngineName = CalypsoMLBondIEEngine.class.getSimpleName().toLowerCase();
        	try {
        		String refInterna = CalypsoMLBondIEEngine.getRefInternaFromXML(jaxbObject.toString());
				SantFileSaver.saveFile(bondsEngineName, "in", "xml", "bond_in_" + refInterna, 0L, jaxbObject.toString());
			} catch (SantFileSaverException e1) {
				Log.error(bondsEngineName, "Error saving message in a file.");
			}
        	
            preTranslate(jaxbObject);
            final Object translated = translateObject(jaxbObject, persistenceSession);
            this.importedObjects.add(translated);

            if (translated instanceof Bond) {
                final Vector messages = new Vector();
                bondTranslated = (Bond) translated;
                if (!bondTranslated.isValidInput(messages)) {
                    // build a task for each error message
                    buildTasks(messages, bondTranslated.getId());
                    throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, messages.toString());
                }

                /* If the bond id is 0, it means that the issue processed in the CDUF is new according to 
                 * the Product Code defined as unique. Currently, only the INTERNAL_REF is being used. 
                 * Window: Configuration --> Product --> Code */
                if ((bondTranslated.getId() == 0) && this.updateBondProductBasedOnNonMandatoryProductCode(bondTranslated, messages)) {
                    save = false;
                    try {
                        addCusipCodeFromISIN(bondTranslated);
                        DSConnection.getDefault().getRemoteProduct().saveBond(bondTranslated, true);
                    } catch (CalypsoServiceException e) {
                        messages.add("Error Updating bond with ID: " + bondTranslated.getId() + "ISIN: " + bondTranslated.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN) + " Error: " + e);
                        buildTasks(messages, bondTranslated.getId());
                        throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, messages.toString());
                    }
                }
            }
            /* The following bond products will be processed here:
             *     1) Those bond products with informed id (value other than 0). This will happen when in the 
             *        CDFU the Product Code defined as unique in Calypso coincides with an issue that already 
             *        exists in Calypo.
             *     
             *     2) With bond product with id equal to 0, but that have not passed the validation carried 
             *     out in the 'updateBondProductBasedOnNonMandatoryProductCode' method 
            */
            if (save) {
                addCusipCodeFromISIN(translated);
                // persist the translated object depending on the import action
                persistenceSession.save(translated);
            }

        } catch (final PersistenceException e) {
            throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e);
        } catch (final SecurityException e) {
            throw new ErrorMessage(jaxbObject, MessageType.SECURITY_ERROR, e);
        }
    }
    
    /**
     * Check if the bond product needs to be updated, according to the non-mandatory Product Code.
     * 
     * @param bond
     * @param messages
     * @return
     */
    private boolean updateBondProductBasedOnNonMandatoryProductCode(Product bond, Vector messages) {
    	boolean updateBondIssue = false;
    	
    	// Mexican issue. The way to identify that the issue is Mexican is that 
    	// the Sec. Code 'MX_IS_MEX_BOND' is informed with 'Y'
    	/* The way to validate if the bond issue exists in the 
         * system will be through the Sec. Code Sec. Code MX_TV_EMISORA_SERIE 
    	 * and the currency.
    	 * If the bond issue exists, it will be updated instead of creating a new one. */
    	if (this.isItAMexicanIssue(bond)) {  
    		updateBondIssue = updateBondProducByMxTVEmisoraSerieAndCurrency(bond, messages);
    		
    	// Non-Mexican emissions. Rest of cases
        /* In the general case, the way to validate if the bond issue exists in the 
         * system will be through the Sec. Code ISIN and the currency.
         * If the bond issue exists, it will be updated instead of creating a new one. */
    	}else {
    		updateBondIssue = this.updateBondProducByIsinAndCurrency(bond, messages);
    	}
    	
    	return updateBondIssue;
    }
    
    /**
     * Returns if the issue is Mexican or not
     * 
     * @param bond
     * @return
     */
    private boolean isItAMexicanIssue(final Product bond) {
    	boolean result = false;
    	
    	String mxIsMexBond = bond.getSecCode(MX_IS_MEX_BOND);
    	
    	if (!Util.isEmpty(mxIsMexBond) && (CTE_Y.equalsIgnoreCase(mxIsMexBond))) {
    		result = true;
    	}
    	
    	return result;
    }
    
    /**
     * It will check if the mexican bond issue being processed is already created in the system according 
     * to its MX_TV_EMISORA_SERIE and currency. If so, instead of creating a new one it will update the existing one. 
     * 
     * @param bond
     * @param messages
     * @return
     */
    @SuppressWarnings("unchecked")
	private boolean updateBondProducByMxTVEmisoraSerieAndCurrency(Product bond, Vector messages) {
    	boolean updatedBondIssue = false;
    	
        final String mxTvEmisioraSerie = bond.getSecCode(MX_TV_EMISORA_SERIE);

        if (Util.isEmpty(mxTvEmisioraSerie)) {
            return false;
        }

        // get products matching by ISIN
        Vector<Product> matchingProducts = this.searchProductsBySecCode(MX_TV_EMISORA_SERIE, mxTvEmisioraSerie.trim());

        // if already exist products, check currency
        if (!Util.isEmpty(matchingProducts)) {
            for (Product product : matchingProducts) {
                if (bond.getCurrency().equals(product.getCurrency())) {
                    bond.setVersion(product.getVersion());
                    bond.setId(product.getId());
                    // update bond
                    messages.add("Update bond with " + MX_TV_EMISORA_SERIE + " = " + mxTvEmisioraSerie + 
                    		      " and Currency = " + bond.getCurrency());
                    updatedBondIssue = true;

                }
            }
        }
        return updatedBondIssue;   	
    }

    /**
     * It will check if the bond issue being processed is already created in the system according 
     * to its ISIN and currency. If so, instead of creating a new one it will update the existing one.
     * 
     * @param bond
     * @param messages
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean updateBondProducByIsinAndCurrency(Product bond, Vector messages) {

        String isin = bond.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN);

        if (Util.isEmpty(isin)) {
            return false;
        }

        // get products matching by ISIN
        Vector<Product> matchingProducts = getMatchingProductsByIsin(isin.trim());

        // if already exist products, check currency
        if (!Util.isEmpty(matchingProducts)) {
            for (Product product : matchingProducts) {
                if (bond.getCurrency().equals(product.getCurrency())) {
                    bond.setVersion(product.getVersion());
                    bond.setId(product.getId());
                    // update bond
                    messages.add("Update bond with ISIN = " + isin + " and Currency = "
                            + bond.getCurrency());
                    return true;

                }
            }
        }
        return false;
    }
    
    /**
     * Search in the BBDD all the bond products that have a certain Sec. Code 
     * (according to the name of the Sec. Code and its value).
     * 
     * @param secCodeName
     * @param secCodeValue
     * @return
     */
    @SuppressWarnings("unchecked")
	private Vector<Product> searchProductsBySecCode(final String secCodeName, final String secCodeValue) {
    	Vector<Product> products = null;

        try {
        	products = DSConnection.getDefault().getRemoteProduct().getProductsByCode(secCodeName, secCodeValue);
        } catch (RemoteException remoteException) {
            Log.error(this, "It has not been possible to retrieve any bonus product searching for "
            		+ "the Sec. Code with name " + secCodeName + " and value " + secCodeValue + ".\n");
        }

        return products;
    }

    @SuppressWarnings("unchecked")
    private Vector<Product> getMatchingProductsByIsin(String isin) {

        Vector<Product> matchingProducts = null;

        try {
            matchingProducts = DSConnection.getDefault().getRemoteProduct()
                    .getProductsByCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN, isin);
        } catch (RemoteException e) {
            Log.error(this, "Cannot get any bond related to ISIN = " + isin + ".\n", e);
        }

        return matchingProducts;

    }

    private void buildTasks(Vector<String> messages, int bondId) {
        if (!Util.isEmpty(messages)) {
            for (String message : messages) {
                getTasks().add(buildTask(message, bondId, "EX_BOND_IMPORT", Task.EXCEPTION_EVENT_CLASS));
            }
        }

    }

    @Override
    protected void preTranslate(final com.calypso.jaxb.xml.Object jaxbObject) throws ErrorMessage {
        // Do the conversion here, iso_code to country name
        // jaxbObject.get
        final BondPreTranslator preProcessor = new BondPreTranslator();
        preProcessor.process(jaxbObject);
        super.preTranslate(jaxbObject);

    }

    /**
     * Generate a Task
     *
     * @param comment    comment related to the task
     * @param objectId    trade id if the exception is related to a trade
     * @param eventType  task event type
     * @param eventClass task event class
     * @return a new Task
     */
    protected Task buildTask(String comment, long objectId, String eventType, String eventClass) {
        Task task = new Task();
        task.setObjectLongId(objectId);
        task.setTradeLongId(0);
        task.setEventClass(eventClass);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setSource("BondImportEngine");
        task.setComment(comment);
        return task;
    }

    /**
     * Fill Cusip code code for US ISINs
     *
     * @param translated    bond
     */
    private void addCusipCodeFromISIN(Object translated) {

        if (translated instanceof Bond) {
            String isin = ((Bond) translated).getSecCode("ISIN");
            String cusip = ((Bond) translated).getSecCode("CUSIP");
            if (Util.isEmpty(cusip) && CollateralUtilities.checkIsinCode(isin)) {
                ((Bond) translated).setSecCode("CUSIP", CollateralUtilities.getCusipFromIsin(isin));
            }
        }
        if (translated instanceof BondAdapter) {
            String isin = ((BondAdapter) translated).getBond().getSecCode("ISIN");
            String cusip = ((BondAdapter) translated).getBond().getSecCode("CUSIP");
            if (Util.isEmpty(cusip) && CollateralUtilities.checkIsinCode(isin)) {
                ((BondAdapter) translated).getBond().setSecCode("CUSIP", CollateralUtilities.getCusipFromIsin(isin));
            }
        }
    }

}
