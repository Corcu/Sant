package calypsox.tk.upload.customizer;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.DomainValues.DomainValuesRow;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.customizer.DefaultUploadCustomizer;
import com.calypso.tk.upload.jaxb.CalypsoLegalEntity;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.uploader.UploadObject;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.upload.jaxb.Attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * 
 * @author X620985
 *
 */
public class MxLEUploadCustomizer extends DefaultUploadCustomizer {

	private static final String MX_CUSTODIA = "MX_Custodia";
	private static final String ALL = "ALL";
	private static final String MX_PO = "MX_PO";
	private static final String FALSE = "false";
	private static final String XFER_AGENT_ACCOUNT = "XferAgentAccount";
	private static final String CTE_Y = "Y";
	private static final String MX_CASH_ACC = "MX_CASH_ACC";
	private static final String MX_SEC_ACC = "MX_SEC_ACC";
	private static final String MX_SDI_CASH_PRODUCTS = "MX_SDI_CASH.PRODUCTS";
	private static final String MX_SDI_SEC_PRODUCTS = "MX_SDI_SEC.PRODUCTS";
	private static final String MX_SDI_CASH_CURRENCIES = "MX_SDI_CASH.CURRENCIES";
	private static final String MX_SDI_SEC_CURRENCIES = "MX_SDI_SEC.CURRENCIES";
	private static final String MX_SDI_CASH = "MX_SDI_CASH";
	private static final String MX_SDI_SEC = "MX_SDI_SEC";
	private static final String MX_SDFILTER = "MX_SDFilter";
	private static final String MX_SDI_CONTACT = "MX_SDI_Contact";
	private static final String MX_SDI_METHOD = "MX_SDI_METHOD";
	private static final String MX_LE_INDEVAL = "MX_LE_INDEVAL";
	private static final String MX_IND_CONTACT = "MX_IND_CONTACT";
	private static final String CASH = "CASH";
	private static final String SECURITY = "SECURITY";
	private static final String CLIENT = "Client";
	private static final String _CASH = "_CASH";
	private static final String _CDY = "_CDY";
	private static final String MX_LE_ATTR = "MX_LE_ATTR"; 
	private static final String MX_CUSTODIA_IN = "MX_CUSTODIA_IN"; 
	private static final String MX_IS_GLOBAL = "MX_IS_GLOBAL"; 

	private List<String> leAttrToCheckIfTheyExist = new ArrayList<String>(Arrays.asList("MX_CUSTODIA_IN",
			"MX_INVESTMENT_PROFILE_ID", "MX_INVESTMENT_SERVICE", "MX_ISFIDEICOMISO", "MX_IS_GLOBAL", "MX_IS_OWNED_BY",
			"MX_LOCKDESC", "MX_COD_SUCURSAL", "MX_COD_SEGMENTO", "MX_SECTOR_CONTABLE", "MX_ID_PADRE", "MX_ID_FISCAL",
			"MX_TIPO_CONTRATO", "MX_PO", "MX_EJECUTIVO", "MX_CASH_ACC", "MX_SEC_ACC"));

	private List<String> leAttrToCheckIfItsValueExists = new ArrayList<String>(Arrays.asList("MX_CUSTODIA_IN",
			"MX_INVESTMENT_PROFILE_ID", "MX_INVESTMENT_SERVICE", "MX_ISFIDEICOMISO", "MX_IS_GLOBAL", "MX_IS_OWNED_BY",
			               "MX_COD_SUCURSAL", "MX_COD_SEGMENTO", "MX_SECTOR_CONTABLE",                
			"MX_TIPO_CONTRATO", "MX_PO", "MX_EJECUTIVO", "MX_CASH_ACC", "MX_SEC_ACC"));
	
	private List<String> valuesYesAndNo = new ArrayList<String>(Arrays.asList("Y", "N"));

	/**
	 * Validations on the file of the Legal Entity of Mexico. If any is not met, a
	 * NACK will be generated and the Legal Entity will not be created in the
	 * system. 
	 */
	@Override
	public void validate(CalypsoObject calypsoObject, Object object, Vector<BOException> error) {
		// TODO Auto-generated method stub
		super.validate(calypsoObject, object, error);

		CalypsoLegalEntity calypsoLegalEntity = (CalypsoLegalEntity) calypsoObject;

		validateCountry(calypsoLegalEntity, error);
		checkIfLEAttributesExist(leAttrToCheckIfTheyExist, calypsoLegalEntity, error);
		checkIfLEAttributesValueExist(leAttrToCheckIfItsValueExists, calypsoLegalEntity, error);
		checkPossibleAttributeValues(calypsoLegalEntity, error);
		validateAccounts(calypsoLegalEntity, error);
		updateActionIfNecessary(calypsoLegalEntity);
	}
	
	/**
	 * After creating the Legal Entity of Mexico, objects associated with it will be
	 * created: a contact, 2 accounts and 2 SDI (settlement instructions)
	 */
	@Override
	public void postSave(UploadObject uploadObject) {
		if (uploadObject == null || uploadObject.getJaxbObject() == null
				|| !(uploadObject.getJaxbObject() instanceof CalypsoLegalEntity)) {
			return;
		}
		CalypsoLegalEntity calypsoLegalEntity = (CalypsoLegalEntity) uploadObject.getJaxbObject();

		if (!Action.S_NEW.equalsIgnoreCase(calypsoLegalEntity.getAction())
				&& !Action.S_AMEND.equalsIgnoreCase(calypsoLegalEntity.getAction())
				&& !Action.S_UPDATE.equalsIgnoreCase(calypsoLegalEntity.getAction())) {
			return;
		} else {
			if (isMexLeWithInternalCustody(calypsoLegalEntity)) {

				LegalEntity legalEntity = retrieveLegalEntityByShortName(calypsoLegalEntity.getShortName());

				createNewLEAttributesIfNecessary(calypsoLegalEntity, legalEntity);
				createContactIfNecessary(calypsoLegalEntity, legalEntity);
				List<Integer> leAccountsId = createAccountsIfNecessary(calypsoLegalEntity, legalEntity);
				createSDIsIfNecessary(calypsoLegalEntity, legalEntity, leAccountsId);
			}
		}
	}

	
	/**
	 * The Legal Entity of Mexico will be informed with the Action = 'AMEND' for both 
	 * additions and modifications. Therefore, we are going to check if the Legal Entity 
	 * exists in the DB and if not, we will update the value of Action to 'NEW'
	 * 
	 * @param calypsoLegalEntity
	 */
	protected void updateActionIfNecessary(CalypsoLegalEntity calypsoLegalEntity) {
		if (Action.S_AMEND.equalsIgnoreCase(calypsoLegalEntity.getAction())) {
			if  (!this.doesThisLegalEntityExistInBBDD(calypsoLegalEntity.getShortName())) {
				calypsoLegalEntity.setAction(Action.S_NEW);
			}
		}
	}
	
	/**
	 * Check if a Legal Entity with the indicated Short Name exists in the DB
	 * 
	 * @param legalEntityName
	 * @return
	 */
	private boolean doesThisLegalEntityExistInBBDD(final String legalEntityName) {
		boolean exist = true;
		
		LegalEntity legalEntity = this.retrieveLegalEntityByShortName(legalEntityName);
		if (legalEntity == null) {
			exist = false;
		}
		return exist;
	}

	/**
	 * Validate country
	 * 
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void validateCountry(final CalypsoLegalEntity calypsoLegalEntity, Vector<BOException> error) {
		if (Util.isEmpty(calypsoLegalEntity.getCountry())) {
			error.add(ErrorExceptionUtils.createException("21001", "Country", "10265", "The country must be informed"));
		}
	}

	/**
	 * In the event that a new cash and securitites account has to be created, it
	 * will be validated if possible with the name provided
	 * 
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void validateAccounts(final CalypsoLegalEntity calypsoLegalEntity, Vector<BOException> error) {
		if (!Action.S_NEW.equalsIgnoreCase(calypsoLegalEntity.getAction())
				&& !Action.S_AMEND.equalsIgnoreCase(calypsoLegalEntity.getAction())
				&& !Action.S_UPDATE.equalsIgnoreCase(calypsoLegalEntity.getAction())) {
			return;
		} else {
			if (isMexLeWithInternalCustody(calypsoLegalEntity)) {
				validateCashAccount(calypsoLegalEntity, error);
				validateSecAccount(calypsoLegalEntity, error);
			}
		}
	}

	/**
	 * It is checked that the value of the MX_CASH_ACC attribute is correctly
	 * informed
	 * 
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void validateCashAccount(final CalypsoLegalEntity calypsoLegalEntity, Vector<BOException> error) {
		final String mxCashAccount = retrieveMxCashAccount(calypsoLegalEntity);
		final String expectedValue = calypsoLegalEntity.getShortName() + _CASH;

		if ((Util.isEmpty(mxCashAccount)) || (!mxCashAccount.equalsIgnoreCase(expectedValue))) {
			error.add(ErrorExceptionUtils.createException("21001", MX_CASH_ACC, "00920",
					"The MX_CASH_ACC attribute must have the value " + expectedValue));
		}
	}

	/**
	 * It is checked that the value of the MX_SEC_ACC attribute is correctly
	 * informed
	 * 
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void validateSecAccount(final CalypsoLegalEntity calypsoLegalEntity, Vector<BOException> error) {
		final String mxSecAccount = retrieveMxSecAccount(calypsoLegalEntity);
		final String expectedValue = calypsoLegalEntity.getShortName() + _CDY;

		if ((Util.isEmpty(mxSecAccount)) || (!mxSecAccount.equalsIgnoreCase(expectedValue))) {
			error.add(ErrorExceptionUtils.createException("21001", MX_SEC_ACC, "00920",
					"The MX_SEC_ACC attribute must have the value " + expectedValue));
		}
	}

	/**
	 * Check if all indicated LE Attributes exist, according to theirs name
	 * 
	 * @param attrValueToCheckIfTheyExist
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void checkIfLEAttributesExist(final List<String> leAttrToCheckIfTheyExist,
			final CalypsoLegalEntity calypsoLegalEntity, Vector<BOException> error) {
		for (String leAttributeName : leAttrToCheckIfTheyExist) {
			if (retrieveLEAttributeByName(calypsoLegalEntity, leAttributeName) == null) {
				error.add(ErrorExceptionUtils.createException("21001", leAttributeName, "10265",
						"The " + leAttributeName + " attribute must be informed"));
			}
		}
	}

	/**
	 * Check if all indicated LE Attributes have an informed value. It only gives an
	 * error if the attribute with the indicated name exists, but its value is not
	 * informed
	 * 
	 * @param attrToCheckIfTheyExist
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void checkIfLEAttributesValueExist(final List<String> leAttrToCheckIfItsValueExists,
			final CalypsoLegalEntity calypsoLegalEntity, Vector<BOException> error) {
		for (String leAttributeName : leAttrToCheckIfItsValueExists) {
			if ((retrieveLEAttributeByName(calypsoLegalEntity, leAttributeName) != null) && Util
					.isEmpty(retrieveLEAttributeByName(calypsoLegalEntity, leAttributeName).getAttributeValue())) {
				error.add(ErrorExceptionUtils.createException("21001", leAttributeName, "10265",
						"The value of the " + leAttributeName + " attribute must be informed"));
			}
		}
	}
	
	/**
	 * For several attributes, it is checked that its value is one of the possible
	 * 
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void checkPossibleAttributeValues(final CalypsoLegalEntity calypsoLegalEntity, 
			                                             Vector<BOException> error) {
		checkValueOfTheMxInternalCustodyAttribute(calypsoLegalEntity, error);
		checkValueOfTheAttributeMxIsGlobalContract(calypsoLegalEntity, error);
	}
	
	/**
	 * It is checked that the value of the MX_CUSTODIA_IN attribute is one of the possible values
	 * 
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void checkValueOfTheMxInternalCustodyAttribute(final CalypsoLegalEntity calypsoLegalEntity, 
            Vector<BOException> error) {
		if ((retrieveLEAttributeByName(calypsoLegalEntity, MX_CUSTODIA_IN) != null)) {	
			String attributeValue = retrieveLEAttributeByName(calypsoLegalEntity, MX_CUSTODIA_IN).getAttributeValue();
			if (!valuesYesAndNo.contains(attributeValue)) {
				String messageException = "The value of the " + MX_CUSTODIA_IN + " attribute must be informed with "
						+ "one of the following values: " + valuesYesAndNo.toString();
				error.add(ErrorExceptionUtils.createException("21001", MX_CUSTODIA_IN, "Wrong value in field", messageException));
			}
		}
	}
	
	/**
	 * It is checked that the value of the MX_IS_GLOBAL attribute is one of the possible values
	 * 
	 * @param calypsoLegalEntity
	 * @param error
	 */
	private void checkValueOfTheAttributeMxIsGlobalContract(final CalypsoLegalEntity calypsoLegalEntity, 
            Vector<BOException> error) {
		if ((retrieveLEAttributeByName(calypsoLegalEntity, MX_IS_GLOBAL) != null)) {	
			String attributeValue = retrieveLEAttributeByName(calypsoLegalEntity, MX_IS_GLOBAL).getAttributeValue();
			if (!valuesYesAndNo.contains(attributeValue)) {
				String messageException = "The value of the " + MX_IS_GLOBAL + " attribute must be informed with "
						+ "one of the following values: " + valuesYesAndNo.toString();
				error.add(ErrorExceptionUtils.createException("21001", MX_IS_GLOBAL, "Wrong value in field", messageException));
			}
		}
	}	

	/**
	 * It is checked if the Legal Entity is from Mexico and internal custody
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	protected boolean isMexLeWithInternalCustody(final CalypsoLegalEntity calypsoLegalEntity) {
		boolean result = false;

		Attribute leAttrMxCustodiaIn = this.retrieveLEAttributeByName(calypsoLegalEntity, "MX_CUSTODIA_IN");
		if ((leAttrMxCustodiaIn != null) && CTE_Y.equalsIgnoreCase(leAttrMxCustodiaIn.getAttributeValue())) {
			result = true;
		}
		return result;
	}

	/**
	 * Create a series of LEAttributes in case they are not already created
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @return
	 */
	protected List<Integer> createNewLEAttributesIfNecessary(final CalypsoLegalEntity calypsoLegalEntity,
			final LegalEntity legalEntity) {
		List<Integer> leAttributesId = new ArrayList<Integer>();
		boolean leAttributeToCreateMayExist = false;

		if (Action.S_NEW.equalsIgnoreCase(calypsoLegalEntity.getAction())) {
			leAttributeToCreateMayExist = false;
			leAttributesId = this.createNewLEAttributes(calypsoLegalEntity, legalEntity, leAttributeToCreateMayExist);

		} else if (Action.S_AMEND.equalsIgnoreCase(calypsoLegalEntity.getAction())
				|| Action.S_UPDATE.equalsIgnoreCase(calypsoLegalEntity.getAction())) {

			leAttributeToCreateMayExist = true;
			leAttributesId = this.createNewLEAttributes(calypsoLegalEntity, legalEntity, leAttributeToCreateMayExist);
		}

		return leAttributesId;
	}

	/**
	 * I am going to create the LEAttributes defined in the Domain Name MX_LE_ATTR
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param leAttributeToCreateMayExist
	 * @return
	 */
	protected List<Integer> createNewLEAttributes(final CalypsoLegalEntity calypsoLegalEntity,
			final LegalEntity legalEntity, final boolean leAttributeToCreateMayExist) {

		List<Integer> leAttributesId = new ArrayList<Integer>();

		final int processingOrgId = this.getProcessingOrgId(calypsoLegalEntity);

		// New LEAttributes to be created
		Vector<String> mxLEAttributesNames = this.getDomainValuesOfDomainName(MX_LE_ATTR);

		for (String leAttributeName : mxLEAttributesNames) {
			leAttributesId.add(this.createNewLEAttributeIfNecessary(legalEntity, leAttributeName,
					leAttributeToCreateMayExist, processingOrgId));
		}

		return leAttributesId;
	}

	/**
	 * I create a specific LEAttribute in case it is not already created
	 * 
	 * @param legalEntity
	 * @param leAttributeName
	 * @param leAttributeToCreateMayExist
	 * @param processingOrgId
	 * @return
	 */
	protected int createNewLEAttributeIfNecessary(final LegalEntity legalEntity, final String leAttributeName,
			final boolean leAttributeToCreateMayExist, final int processingOrgId) {
		int leAttributeId = -1;
		
		if (this.isTheLEAttributeCreated(legalEntity, leAttributeName)) {

			if (leAttributeToCreateMayExist) {
				// Example: an Amend or Update has been made on the legal entity and the 
				// the LEAttribute had already been previously created

				Log.info(this, "The Legal Entity " + legalEntity.getName() + " has an attribute with the name "
						+ leAttributeName + " so it will not be necessary to create it.");

			} else {
				// When creating a new Legal Entity, the attribute that you want to create 
				// should not exist. In any case, it will check its existence.

				Log.error(this,
						"The Legal Entity " + legalEntity.getName() + " has an attribute with the name "
								+ leAttributeName
								+ ", so it will not be possible to create another attribute with the same name");
			}
		} else {
			// This LEAttribute is not created, so I am going to create it
			leAttributeId = this.createNewLEAttribute(legalEntity.getId(), leAttributeName, processingOrgId);
		}

		return leAttributeId;
	}

	/**
	 * Check if the LegalEntity object has an attribute with the name indicated as a
	 * parameter
	 * 
	 * @param legalEntity
	 * @param leAttributeName
	 * @return
	 */
	private boolean isTheLEAttributeCreated(final LegalEntity legalEntity, final String leAttributeName) {
		boolean leAttributeCreated = false;

		@SuppressWarnings("unchecked")
		Collection<LegalEntityAttribute> leAttributes = legalEntity.getLegalEntityAttributes();
		
		if (!Util.isEmpty(leAttributeName) && !Util.isEmpty(leAttributes)) {
			for (LegalEntityAttribute leAttribute : leAttributes) {
				if (leAttributeName.equalsIgnoreCase(leAttribute.getAttributeType())) {
					leAttributeCreated = true;
				}
			}
		}

		return leAttributeCreated;
	}
	
	/**
	 * Create a new LegalEntityAttribute and save it in DB
	 * 
	 * @param legalEntityId
	 * @param leAttributeName
	 * @param processingOrgId
	 * @return
	 */
	private int createNewLEAttribute(final int legalEntityId, final String leAttributeName, final int processingOrgId) {
		int leAttributeId = -1;
		
		final String leAttributeValue = this.getDomainValueComment(MX_LE_ATTR, leAttributeName);
		
		final LegalEntityAttribute leAttribute = new LegalEntityAttribute();
		leAttribute.setProcessingOrgId(processingOrgId);
		leAttribute.setLegalEntityId(legalEntityId);
		leAttribute.setLegalEntityRole(LegalEntityAttribute.ALL);
		leAttribute.setAttributeType(leAttributeName);
		leAttribute.setAttributeValue(leAttributeValue);

		leAttributeId = this.saveLEAttribute(leAttribute);
		
		return leAttributeId;
	}
	
	/**
	 * Save the LegalEntityAttribute in the DB
	 * 
	 * @param leAttribute
	 * @return
	 */
	private int saveLEAttribute(final LegalEntityAttribute leAttribute) {
		int leAttributeId = -1;

		try {
			leAttributeId = DSConnection.getDefault().getRemoteReferenceData().save(leAttribute);
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this, "The LEAttribute associated with Legal Entity " + leAttribute.getLegalEntityId() + " could not be created : "
					+ calypsoServiceException.toString());
		}

		if (leAttributeId > 0) {
			Log.info(this, "LegalEntityAttribute with id " + leAttributeId + " created for the Legal Entity : " + leAttribute.getLegalEntityId());
		}
		
		return leAttributeId;
	}

	/**
	 * If the Legal Entity is internal custody, the contact associated with the
	 * Legal Entity will be created if it does not exist. This happens if you are
	 * creating a new Legal Entity or if you are modifying the Legal Entity and it
	 * was not previously created.
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @return
	 */
	protected int createContactIfNecessary(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity) {
		int leContactId = -1;
		boolean contactToCreateMayExist = false;

		if (Action.S_NEW.equalsIgnoreCase(calypsoLegalEntity.getAction())) {
			contactToCreateMayExist = false;
			leContactId = this.createContact(calypsoLegalEntity, legalEntity, contactToCreateMayExist);

		} else if (Action.S_AMEND.equalsIgnoreCase(calypsoLegalEntity.getAction())
				|| Action.S_UPDATE.equalsIgnoreCase(calypsoLegalEntity.getAction())) {

			contactToCreateMayExist = true;
			leContactId = this.createContact(calypsoLegalEntity, legalEntity, contactToCreateMayExist);
		}

		return leContactId;
	}

	/**
	 * A Contact associated with the Legal Entity is created
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @return
	 */
	protected int createContact(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final boolean contactToCreateMayExist) {

		int leContactId = -1;

		if (this.isTheContactCreated(calypsoLegalEntity, legalEntity)) {

			if (contactToCreateMayExist) {
				// Example: an Amend or Update has been made on the legal entity and the contact
				// associated with said legal entity had already been previously created

				Log.info(this, "There is an contact associated with the Legal Entity " + legalEntity.getName()
						+ " so it will not be necessary to create it.");

			} else {
				// Example: when a new Legal Entity is created, it must have no contacts and one
				// must be created. In principle, it would not be possible for there to be any
				// contact under this casuistry, but if there were, another would not be created
				// and an error would be given indicating that one already exists.

				// It will not try to create the contact, but it will write an error log.

				Log.error(this, "There is an contact associated with the Legal Entity " + legalEntity.getName()
						+ " so, no new contact will be created.");
			}

		} else {
			// There is no contact created, so I'm going to create a new one

			LEContact contact = new LEContact();
			contact.setLegalEntityId(legalEntity.getId());
			contact.setContactType(MX_CUSTODIA);
			contact.setCountry(legalEntity.getCountry());
			contact.setLegalEntityRole(ALL);
			contact.setProductType(ALL);
			contact.setProcessingOrgId(this.getProcessingOrgId(calypsoLegalEntity));

			leContactId = saveLEContact(contact, legalEntity.getId());
		}

		return leContactId;
	}

	/**
	 * The LEContact is created and saved in BBDD
	 * 
	 * @param leContact
	 * @param legalEntityId
	 * @return
	 */
	private int saveLEContact(final LEContact leContact, final int legalEntityId) {
		int leContactId = -1;

		try {
			leContactId = DSConnection.getDefault().getRemoteReferenceData().save(leContact);
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this, "The contact associated with Legal Entity " + legalEntityId + " could not be created : "
					+ calypsoServiceException.toString());
		}

		if (leContactId > 0) {
			Log.info(this, "Contact with id " + leContactId + " created for the Legal Entity : " + legalEntityId);
		}
		
		return leContactId;
	}

	/**
	 * If the Legal Entity is internal custody, the Cash and Securities accounts
	 * associated with the Legal Entity will be created if it does not exist. This
	 * happens if you are creating a new Legal Entity or if you are modifying the
	 * Legal Entity and it was not previously created.
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @return
	 */
	protected List<Integer> createAccountsIfNecessary(final CalypsoLegalEntity calypsoLegalEntity,
			final LegalEntity legalEntity) {
		List<Integer> leAccountsId = new ArrayList<Integer>();
		boolean accountToCreateMayExist = false;

		if (Action.S_NEW.equalsIgnoreCase(calypsoLegalEntity.getAction())) {
			accountToCreateMayExist = false;
			leAccountsId = this.createAccounts(calypsoLegalEntity, legalEntity, accountToCreateMayExist);

		} else if (Action.S_AMEND.equalsIgnoreCase(calypsoLegalEntity.getAction())
				|| Action.S_UPDATE.equalsIgnoreCase(calypsoLegalEntity.getAction())) {

			accountToCreateMayExist = true;
			leAccountsId = this.createAccounts(calypsoLegalEntity, legalEntity, accountToCreateMayExist);
		}

		return leAccountsId;
	}

	/**
	 * A cash account and a securities account will be created for the Legal Entity
	 * of Mexico
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param accountToCreateMayExist
	 * @return
	 */
	protected List<Integer> createAccounts(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final boolean accountToCreateMayExist) {
		List<Integer> leAccountsId = new ArrayList<Integer>();

		String accountDescription = this.getAccountDescription(calypsoLegalEntity);
		int processingOrgId = this.getProcessingOrgId(calypsoLegalEntity);

		int cashAccountId = createCashAccount(calypsoLegalEntity, legalEntity, accountDescription, processingOrgId,
				accountToCreateMayExist);
		int secAccountId = createSecAccount(calypsoLegalEntity, legalEntity, accountDescription, processingOrgId,
				accountToCreateMayExist);

		leAccountsId.add(cashAccountId);
		leAccountsId.add(secAccountId);

		return leAccountsId;
	}

	/**
	 * A cash account is created
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param accountDescription
	 * @param processingOrgId
	 * @param accountToCreateMayExist
	 * @return
	 */
	protected int createCashAccount(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final String accountDescription, final int processingOrgId, final boolean accountToCreateMayExist) {
		int cashAccountId = -1;

		String mxCashAccount = retrieveMxCashAccount(calypsoLegalEntity);

		if (this.isTheAccountCreated(mxCashAccount, processingOrgId)) {

			if (accountToCreateMayExist) {
				// Example: an Amend or Update has been made on the legal entity and the account
				// associated with said legal entity had already been previously created

				Log.info(this, "There is an account associated with the Legal Entity " + legalEntity.getName()
						+ " with the name " + mxCashAccount + " so it will not be necessary to create it.");

			} else {
				// Example: when creating a new legal entity, it is indicated that an account
				// must be
				// created with a name that already exists. This would give an error when
				// creating the account.

				// It will not try to create the account, but it will write an error log.

				Log.error(this,
						"There is an account associated with the Legal Entity " + legalEntity.getName()
								+ " with the name " + mxCashAccount
								+ " so it will not be possible to create another account with the same name.");
			}
		} else {
			// There is no account created, so I'm going to create a new one
			cashAccountId = this.createAccount(calypsoLegalEntity, legalEntity, mxCashAccount, accountDescription,
					processingOrgId);
		}

		return cashAccountId;
	}

	/**
	 * Returns the description of the account to create
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	private String getAccountDescription(final CalypsoLegalEntity calypsoLegalEntity) {
		String accountDescription = "";

		String processingOrgName = retrieveProcessingOrgName(calypsoLegalEntity);
		if (!Util.isEmpty(processingOrgName)) {
			accountDescription = "Client account in " + processingOrgName;
		}
		return accountDescription;
	}

	/**
	 * Returns the attribute MX_CASH_ACC (Mexico Cash Account)
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	private String retrieveMxCashAccount(final CalypsoLegalEntity calypsoLegalEntity) {
		String mxCashAccount = "";

		Attribute leAttr = this.retrieveLEAttributeByName(calypsoLegalEntity, MX_CASH_ACC);
		if (leAttr != null) {
			mxCashAccount = leAttr.getAttributeValue();
		}

		return mxCashAccount;
	}

	/**
	 * A securities account is created
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param accountDescription
	 * @param processingOrgId
	 * @param accountToCreateMayExist
	 * @return
	 */
	protected int createSecAccount(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final String accountDescription, final int processingOrgId, final boolean accountToCreateMayExist) {
		int secAccountId = -1;

		String mxSecAccount = retrieveMxSecAccount(calypsoLegalEntity);

		if (this.isTheAccountCreated(mxSecAccount, processingOrgId)) {

			if (accountToCreateMayExist) {
				// Example: an Amend or Update has been made on the legal entity and the account
				// associated with said legal entity had already been previously created

				Log.info(this, "There is an account associated with the Legal Entity " + legalEntity.getName()
						+ " with the name " + mxSecAccount + " so it will not be necessary to create it.");

			} else {
				// Example: when creating a new legal entity, it is indicated that an account
				// must be
				// created with a name that already exists. This would give an error when
				// creating the account.

				// It will not try to create the account, but it will write an error log.

				Log.error(this,
						"There is an account associated with the Legal Entity " + legalEntity.getName()
								+ " with the name " + mxSecAccount
								+ " so it will not be possible to create another account with the same name.");
			}
		} else {
			// There is no account created, so I'm going to create a new one
			secAccountId = this.createAccount(calypsoLegalEntity, legalEntity, mxSecAccount, accountDescription,
					processingOrgId);
		}

		return secAccountId;
	}

	/**
	 * If the Legal Entity is internal custody, the Cash and Securities SDI
	 * associated with the Legal Entity will be created if it does not exist. This
	 * happens if you are creating a new Legal Entity or if you are modifying the
	 * Legal Entity and it was not previously created.
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @return
	 */
	protected List<Integer> createSDIsIfNecessary(final CalypsoLegalEntity calypsoLegalEntity,
			final LegalEntity legalEntity, final List<Integer> leAccountsId) {

		List<Integer> sDIsId = new ArrayList<Integer>();
		boolean sDIToCreateMayExist = false;

		if (Action.S_NEW.equalsIgnoreCase(calypsoLegalEntity.getAction())) {
			sDIToCreateMayExist = false;
			sDIsId = this.createSDIs(calypsoLegalEntity, legalEntity, sDIToCreateMayExist, leAccountsId);

		} else if (Action.S_AMEND.equalsIgnoreCase(calypsoLegalEntity.getAction())
				|| Action.S_UPDATE.equalsIgnoreCase(calypsoLegalEntity.getAction())) {

			sDIToCreateMayExist = true;
			sDIsId = this.createSDIs(calypsoLegalEntity, legalEntity, sDIToCreateMayExist, leAccountsId);
		}

		return sDIsId;
	}

	/**
	 * A cash SDI and a security SDI will be created for the Legal Entity of Mexico
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param sDIToCreateMayExist
	 * @return
	 */
	protected List<Integer> createSDIs(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final boolean sDIToCreateMayExist, final List<Integer> leAccountsId) {

		List<Integer> sDIsId = new ArrayList<Integer>();

		LegalEntity leProcessingOrg = this.getLegalEntityProcessingOrg(calypsoLegalEntity);

		int cashSdiId = this.createCashSDI(calypsoLegalEntity, legalEntity, leProcessingOrg, sDIToCreateMayExist,
				leAccountsId);
		int secSdiId = this.createSecSDI(calypsoLegalEntity, legalEntity, leProcessingOrg, sDIToCreateMayExist,
				leAccountsId);

		sDIsId.add(cashSdiId);
		sDIsId.add(secSdiId);

		return sDIsId;
	}

	/**
	 * Create a cash SDI
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param leProcessingOrg
	 * @param sDIToCreateMayExist
	 * @param leAccountsId
	 * @return
	 */
	protected int createCashSDI(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final LegalEntity leProcessingOrg, final boolean sDIToCreateMayExist, final List<Integer> leAccountsId) {

		int cashSdiId = -1;

		final String description = this.getCashSdiDescription(calypsoLegalEntity);
		int leMxIndevalId = this.getLEMxIndevalId();

		if (this.isTheCashSdiCreated(description, leMxIndevalId, legalEntity.getId())) {

			if (sDIToCreateMayExist) {
				// Example: an Amend or Update has been made on the legal entity and the SDI
				// associated with said legal entity had already been previously created

				Log.info(this, "There is a cash SDI associated with the Legal Entity " + legalEntity.getName()
						+ " with the name " + description + " so it will not be necessary to create it.");

			} else {
				// Example: when creating a new legal entity, it is indicated that an SDI
				// must be
				// created with a name that already exists. This would give an error when
				// creating the SDI.

				// It will not try to create the SDI, but it will write an error log.

				Log.error(this, "There is a cash SDI associated with the Legal Entity " + legalEntity.getName()
						+ " with the name " + description + " so no new cash SDI will be created.");
			}
		} else {
			// There is no SDI created, so I'm going to create a new one
			cashSdiId = this.createCashSdiAndSave(calypsoLegalEntity, legalEntity, leProcessingOrg, leMxIndevalId,
					leAccountsId);
		}

		return cashSdiId;
	}

	/**
	 * Construct a cash SettleDeliveryInstruction object and save it to the DB
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param leProcessingOrg
	 * @param leMxIndevalId
	 * @param leAccountsId
	 * @return
	 */
	private int createCashSdiAndSave(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final LegalEntity leProcessingOrg, final int leMxIndevalId, final List<Integer> leAccountsId) {

		int sdiId = -1;

		SettleDeliveryInstruction sdi = new SettleDeliveryInstruction();

		/* INFORMATION OF THE LEGAL ENTITY BENEFICIARY -- BEGINNING */

		// 'Cash/Security' = CASH
		sdi.setType(CASH);

		// Role
		sdi.setRole(CLIENT);

		// Beneficiary Contact Type
		sdi.setBeneficiaryContactType(this.getDomainValueComment(MX_SDI_CASH, MX_SDI_CONTACT));

		sdi.setBeneficiaryId(legalEntity.getId());

		sdi.setProcessingOrg(leProcessingOrg);

		// Products
		sdi.setProductList(this.getDomainValuesOfDomainName(MX_SDI_CASH_PRODUCTS));

		// Currencies
		sdi.setCurrencyList(this.getDomainValuesOfDomainName(MX_SDI_CASH_CURRENCIES));

		// Static Data Filter Name
		sdi.setStaticFilterSet(this.getDomainValueComment(MX_SDI_CASH, MX_SDFILTER));

		// Pay/Rec = BOTH
		sdi.setPayReceive(SettleDeliveryInstruction.BOTH);

		// 'Trade Counterparty' == ALL
		/*
		 * If a specific Trade Counterparty had to be specified, it would be with the
		 * following method, in which its id would be indicated. In our case there is
		 * nothing to do
		 */
		// sdi.setTradeCounterPartyId(idTradeCounterparty);

		final String description = this.getCashSdiDescription(calypsoLegalEntity);
		sdi.setDescription(description);

		sdi.setPreferredB(true);

		// Method
		final String method = this.getDomainValueComment(MX_SDI_CASH, MX_SDI_METHOD);
		sdi.setMethod(method);

		// Effective Date From / To
		sdi.setEffectiveDateFrom(JDate.valueOf(2022, 2, 1));
		sdi.setEffectiveDateTo(JDate.valueOf(2122, 5, 26));

		/* INFORMATION OF THE LEGAL ENTITY BENEFICIARY -- END */

		/* INFORMATION OF THE LEGAL ENTITY AGENT -- BEGINNING */

		// Agent Id ---- ID of the Legal Entity INDEVAL of Mexico
		sdi.setAgentId(leMxIndevalId);

		// Agent Contact Type
		sdi.setAgentContactType(this.getDomainValueComment(MX_SDI_CASH, MX_IND_CONTACT));

		// Field 'A/C'. In our case it will be the cash account created for our Legal
		// Entity
		String mxCashAccount = retrieveMxCashAccount(calypsoLegalEntity);
		sdi.setAgentAccount(mxCashAccount);

		/* INFORMATION OF THE LEGAL ENTITY AGENT -- END */

		// save new account
		sdiId = saveSDI(sdi);

		return sdiId;
	}

	/**
	 * Create a security SDI
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param leProcessingOrg
	 * @param sDIToCreateMayExist
	 * @param leAccountsId
	 * @return
	 */
	protected int createSecSDI(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final LegalEntity leProcessingOrg, final boolean sDIToCreateMayExist, final List<Integer> leAccountsId) {

		int secSdiId = -1;

		final String description = this.getSecSdiDescription(calypsoLegalEntity);

		if (this.isTheSecSdiCreated(description, legalEntity.getId())) {

			if (sDIToCreateMayExist) {
				// Example: an Amend or Update has been made on the legal entity and the SDI
				// associated with said legal entity had already been previously created

				Log.info(this, "There is a security SDI associated with the Legal Entity " + legalEntity.getName()
						+ " with the name " + description + " so it will not be necessary to create it.");

			} else {
				// Example: when creating a new legal entity, it is indicated that an SDI
				// must be
				// created with a name that already exists. This would give an error when
				// creating the SDI.

				// It will not try to create the SDI, but it will write an error log.

				Log.error(this, "There is a security SDI associated with the Legal Entity " + legalEntity.getName()
						+ " with the name " + description + " so no new security SDI will be created.");
			}
		} else {
			// There is no SDI created, so I'm going to create a new one
			secSdiId = this.createSecSdiAndSave(calypsoLegalEntity, legalEntity, leProcessingOrg, leAccountsId);
		}

		return secSdiId;
	}

	/**
	 * Construct a security SettleDeliveryInstruction object and save it to the DB
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param leProcessingOrg
	 * @param leAccountsId
	 * @return
	 */
	private int createSecSdiAndSave(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final LegalEntity leProcessingOrg, final List<Integer> leAccountsId) {

		int sdiId = -1;

		SettleDeliveryInstruction sdi = new SettleDeliveryInstruction();

		// 'Cash/Security' = SECURITY
		sdi.setType(SECURITY);

		// Role
		sdi.setRole(CLIENT);

		// Beneficiary Contact Type
		sdi.setBeneficiaryContactType(this.getDomainValueComment(MX_SDI_SEC, MX_SDI_CONTACT));

		sdi.setBeneficiaryId(legalEntity.getId());

		sdi.setProcessingOrg(leProcessingOrg);

		// Products
		sdi.setProductList(this.getDomainValuesOfDomainName(MX_SDI_SEC_PRODUCTS));

		// Currencies
		sdi.setCurrencyList(this.getDomainValuesOfDomainName(MX_SDI_SEC_CURRENCIES));

		// Static Data Filter Name
		sdi.setStaticFilterSet(this.getDomainValueComment(MX_SDI_SEC, MX_SDFILTER));

		// Pay/Rec = BOTH
		sdi.setPayReceive(SettleDeliveryInstruction.BOTH);

		// 'Trade Counterparty' == ALL
		/*
		 * If a specific Trade Counterparty had to be specified, it would be with the
		 * following method, in which its id would be indicated. In our case there is
		 * nothing to do
		 */
		// sdi.setTradeCounterPartyId(idTradeCounterparty);

		final String description = this.getSecSdiDescription(calypsoLegalEntity);
		sdi.setDescription(description);

		sdi.setPreferredB(true);

		// Method
		final String method = this.getDomainValueComment(MX_SDI_SEC, MX_SDI_METHOD);
		sdi.setMethod(method);

		sdi.setDirectRelationship(true);

		// Effective Date From / To
		sdi.setEffectiveDateFrom(JDate.valueOf(2022, 2, 1));
		sdi.setEffectiveDateTo(JDate.valueOf(2122, 5, 26));

		// Direct DDA. It is reported with the id of the security account
		sdi.setGeneralLedgerAccount(leAccountsId.get(1));

		// save new account
		sdiId = saveSDI(sdi);

		return sdiId;
	}

	/**
	 * Returns the description field of the Cash SDI
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	private String getCashSdiDescription(final CalypsoLegalEntity calypsoLegalEntity) {
		String description = "";

		// Description = 'Method' + '/' + 'LE Agent Name' + '/' + 'Cash Account Name'
		final String method = this.getDomainValueComment(MX_SDI_CASH, MX_SDI_METHOD);
		final String leMxIndevalName = this.getDomainValueComment(MX_SDI_CASH, MX_LE_INDEVAL);
		String mxCashAccountName = this.retrieveMxCashAccount(calypsoLegalEntity);

		description = method + "/" + leMxIndevalName + "/" + mxCashAccountName;

		return description;
	}

	/**
	 * Returns the description field of the Cash SDI
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	private String getSecSdiDescription(final CalypsoLegalEntity calypsoLegalEntity) {
		String description = "";

		// Description = 'Method' + '/' + 'LE Agent Name' + '/' + 'Cash Account Name'
		final String method = this.getDomainValueComment(MX_SDI_SEC, MX_SDI_METHOD);
		String mxSecAccountName = this.retrieveMxSecAccount(calypsoLegalEntity);

		description = method + "/" + mxSecAccountName;

		return description;
	}

	/**
	 * Save an SDI in DB
	 * 
	 * @param sdi
	 * @return
	 */
	private int saveSDI(final SettleDeliveryInstruction sdi) {
		int sdiId = -1;

		try {
			sdiId = DSConnection.getDefault().getRemoteReferenceData().save(sdi);
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this, "Error saving the SDI: " + calypsoServiceException.toString());
		}

		return sdiId;
	}

	/**
	 * Retrieve the Domain Values of a Domain Name
	 * 
	 * @param domainName
	 * @return
	 */
	private Vector<String> getDomainValuesOfDomainName(final String domainName) {
		Vector<String> result = new Vector<String>();

		Vector<String> auxiliar = this.getDomainValuesWithCache(domainName);
		if (Util.isEmpty(auxiliar)) {
			auxiliar = this.getDomainValuesWithoutCache(domainName);
		}
		if (!Util.isEmpty(auxiliar)) {
			result = auxiliar;
		}

		return result;
	}

	/**
	 * Retrieve the Domain Values of a Domain Name using cache
	 * 
	 * @param domainName
	 * @return
	 */
	private Vector<String> getDomainValuesWithCache(final String domainName) {
		return LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
	}

	/**
	 * Retrieve the Domain Values of a Domain Name without using cache
	 * 
	 * @param domainName
	 * @return
	 */
	private Vector<String> getDomainValuesWithoutCache(final String domainName) {
		Vector<String> domainValues = null;

		try {
			domainValues = DSConnection.getDefault().getRemoteReferenceData().getDomainValues(domainName);
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this,
					"Error retrieving the Domain Values of " + domainName + " : " + calypsoServiceException.toString());
		}

		return domainValues;
	}

	/**
	 * Retrieve the comment of a Domain Value
	 * 
	 * @param domainName
	 * @param domainValue
	 * @return
	 */
	private String getDomainValueComment(final String domainName, final String domainValue) {
		String domainValueComment = "";

		domainValueComment = this.getDomainValueCommentWithCache(domainName, domainValue);

		if (Util.isEmpty(domainValueComment)) {
			domainValueComment = this.getDomainValueCommentWithoutCache(domainName, domainValue);
		}
		domainValueComment = this.ifStringNullSetEmpty(domainValueComment);

		return domainValueComment;
	}

	/**
	 * Retrieve the comment of a Domain Value using the cache
	 * 
	 * @param domainName
	 * @param domainValue
	 * @return
	 */
	private String getDomainValueCommentWithCache(final String domainName, final String domainValue) {
		return LocalCache.getDomainValueComment(DSConnection.getDefault(), domainName, domainValue);
	}

	/**
	 * Retrieve the comment of a Domain Value without using the cache
	 * 
	 * @param domainName
	 * @param domainValue
	 * @return
	 */
	private String getDomainValueCommentWithoutCache(final String domainName, final String domainValue) {
		String domainValueComment = "";

		DomainValuesRow domainValuesRow = null;
		try {
			domainValuesRow = DSConnection.getDefault().getRemoteReferenceData().getDomainValuesRow(domainName,
					domainValue);
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this, "Error retrieving the comment for Domain Name " + domainName + " and Domain Value "
					+ domainValue + " : " + calypsoServiceException.toString());
		}

		if (domainValuesRow != null) {
			domainValueComment = domainValuesRow.getComment();
		}

		return domainValueComment;
	}

	/**
	 * If the string is null, it returns empty.
	 * 
	 * @param stringParam
	 * @return
	 */
	private String ifStringNullSetEmpty(final String stringParam) {
		String result = stringParam;

		if (result == null) {
			result = "";
		}

		return result;
	}

	/**
	 * Returns the attribute MX_SEC_ACC (Mexico Securities Account)
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	private String retrieveMxSecAccount(final CalypsoLegalEntity calypsoLegalEntity) {
		String mxCashAccount = "";

		Attribute leAttr = this.retrieveLEAttributeByName(calypsoLegalEntity, MX_SEC_ACC);
		if (leAttr != null) {
			mxCashAccount = leAttr.getAttributeValue();
		}

		return mxCashAccount;
	}

	/**
	 * An account is created
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @param accountName
	 * @param description
	 * @param processingOrgId
	 * @return
	 */
	private int createAccount(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity,
			final String accountName, final String description, final int processingOrgId) {

		int accountId = -1;

		Account account = new Account();
		account.setCreationDate(new JDatetime());

		// set the account name and description
		account.setName(accountName);
		account.setDescription(description);

		account.setProcessingOrgId(processingOrgId);
		account.setCurrency(Account.ANY);

		// type settle and configuration
		account.setAccountType(Account.SETTLE);
		account.setCallAccountB(false);

		// counterparty and role
		account.setLegalEntityId(legalEntity.getId());
		account.setLegalEntityRole(LegalEntity.CLIENT);

		// other options
		account.setAccountStatus("");
		account.setAttributes(null);
		account.setUser(DSConnection.getDefault().getUser());

		// account properties
		account.setAccountProperty(Account.PROPAGATE, FALSE);
		account.setAccountProperty(XFER_AGENT_ACCOUNT, accountName);

		// save new account
		accountId = saveAccount(account, legalEntity.getId());

		return accountId;
	}

	/**
	 * The account is created and saved in BBDD
	 * 
	 * @param account
	 * @param legalEntityId
	 * @return
	 */
	private int saveAccount(final Account account, final int legalEntityId) {
		int accountId = -1;

		try {
			accountId = DSConnection.getDefault().getRemoteAccounting().save(account);
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this, "The account associated with Legal Entity " + legalEntityId + " could not be created : "
					+ calypsoServiceException.toString());
		}

		if (accountId > 0) {
			Log.info(this, "Account with id " + accountId + " created for the Legal Entity : " + legalEntityId);
		}

		return accountId;
	}

	/**
	 * Looking for the contact associated with the Legal Entity
	 * 
	 * @param calypsoLegalEntity
	 * @param legalEntity
	 * @return
	 */
	private LEContact searchForContact(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity) {
		LEContact leContact = null;

		// The contact is searched using the cache
		leContact = this.getContactWithCache(calypsoLegalEntity, legalEntity);

		if (leContact == null) {
			// The contact is searched without using the cache
			leContact = this.getContactWithoutCache(calypsoLegalEntity, legalEntity);
		}

		return leContact;
	}

	/**
	 * Retrieve the contact using the cache
	 * 
	 * @param accountName
	 * @param processingOrgId
	 * @return
	 */
	private LEContact getContactWithCache(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity) {
		return BOCache.getContact(DSConnection.getDefault(), ALL, legalEntity, MX_CUSTODIA, ALL,
				this.getProcessingOrgId(calypsoLegalEntity));
	}

	/**
	 * Retrieve the contact by calling the DB using DSConnection.getDefault() and
	 * without using the cache
	 * 
	 * @param accountName
	 * @return
	 */
	private LEContact getContactWithoutCache(final CalypsoLegalEntity calypsoLegalEntity,
			final LegalEntity legalEntity) {
		LEContact contact = null;

		try {
			@SuppressWarnings("unchecked")
			Vector<LEContact> leContacts = DSConnection.getDefault().getRemoteReferenceData()
					.getLEContacts(legalEntity.getId());
			if (!Util.isEmpty(leContacts)) {
				contact = leContacts.get(0);
			}
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this, "Error retrieving the contact associated with the Legal Entity " + legalEntity.getName()
					+ " without cache: " + calypsoServiceException.toString());
		}

		return contact;
	}

	/**
	 * Returns true if the legal entity has an associated contact
	 * 
	 * @return
	 */
	private boolean isTheContactCreated(final CalypsoLegalEntity calypsoLegalEntity, final LegalEntity legalEntity) {
		boolean contactCreated = true;

		LEContact leContact = searchForContact(calypsoLegalEntity, legalEntity);
		if (leContact == null) {
			contactCreated = false;
		}

		return contactCreated;
	}

	/**
	 * Looking for the account associated with the Legal Entity
	 * 
	 * @param accountName
	 * @return
	 */
	private Account searchForAccount(final String accountName, final int processingOrgId) {
		Account account = null;

		// The account is searched using the cache
		account = this.getAccountingWithCache(accountName, processingOrgId);

		if (account == null) {
			// The account is searched without using the cache
			account = this.getAccountingWithoutCache(accountName);
		}

		return account;
	}

	/**
	 * Retrieve the account using the cache
	 * 
	 * @param accountName
	 * @param processingOrgId
	 * @return
	 */
	private Account getAccountingWithCache(final String accountName, final int processingOrgId) {
		return BOCache.getAccount(DSConnection.getDefault(), accountName, processingOrgId, Account.ANY);
	}

	/**
	 * Retrieve the account by calling the DB using DSConnection.getDefault() and
	 * without using the cache
	 * 
	 * @param accountName
	 * @return
	 */
	private Account getAccountingWithoutCache(final String accountName) {
		Account account = null;

		try {
			@SuppressWarnings("unchecked")
			Vector<Account> accounts = DSConnection.getDefault().getRemoteAccounting().getAccountsByName(accountName);
			if (!Util.isEmpty(accounts)) {
				account = accounts.get(0);
			}
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this, "Error recovering account " + accountName + " remotely without cache"
					+ calypsoServiceException.toString());
		}

		return account;
	}

	/**
	 * Returns true if the account name indicated by parameter exists in the system
	 * 
	 * @return
	 */
	private boolean isTheAccountCreated(final String accountName, final int processingOrgId) {
		boolean accountCreated = true;

		Account account = searchForAccount(accountName, processingOrgId);
		if (account == null) {
			accountCreated = false;
		}

		return accountCreated;
	}

	/**
	 * A Legal Entity is retrieved by its short name
	 * 
	 * @param shortName
	 * @return
	 */
	private LegalEntity retrieveLegalEntityByShortName(final String shortName) {
		LegalEntity legalEntity = null;

		if (!Util.isEmpty(shortName)) {
			try {
				legalEntity = DSConnection.getDefault().getRemoteReferenceData()
						.getLegalEntity(shortName.toUpperCase());
			} catch (CalypsoServiceException calypsoServiceException) {
				Log.error(this, "The legal entity named " + shortName + " could not be retrieved : "
						+ calypsoServiceException.toString());
			}
		}

		return legalEntity;
	}

	/**
	 * Finds the position an attribute name occupies within the attribute list of
	 * the CalypsoLegalEntity object
	 * 
	 * @param calypsoLegalEntity
	 * @param searchAttributeName
	 * @return
	 */
	private int findPositionLEAttribute(final CalypsoLegalEntity calypsoLegalEntity, final String searchAttributeName) {
		int positionLEAttribute = -1;
		List<Attribute> attributeList = calypsoLegalEntity.getAttributes().getAttribute();

		for (int index = 0; index < attributeList.size(); index++) {
			String attrName = attributeList.get(index).getAttributeName();
			if ((!Util.isEmpty(searchAttributeName)) && searchAttributeName.equals(attrName)) {
				positionLEAttribute = index;
			}
		}

		return positionLEAttribute;
	}

	/**
	 * Retrieve the LEAttribute by name within the CalypsoLegalEntity object
	 * 
	 * @param calypsoLegalEntity
	 * @param attributeName
	 * @return
	 */
	private Attribute retrieveLEAttributeByName(final CalypsoLegalEntity calypsoLegalEntity,
			final String attributeName) {
		Attribute leAttribute = null;
		int positionLEAttribute = this.findPositionLEAttribute(calypsoLegalEntity, attributeName);

		if (positionLEAttribute >= 0) {
			leAttribute = calypsoLegalEntity.getAttributes().getAttribute().get(positionLEAttribute);
		}

		return leAttribute;
	}

	/**
	 * Retrieves the name of the Processing Org of the Legal Entity of Mexico
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	private String retrieveProcessingOrgName(final CalypsoLegalEntity calypsoLegalEntity) {
		String processingOrgName = "";

		Attribute leAttr = this.retrieveLEAttributeByName(calypsoLegalEntity, MX_PO);
		if (leAttr != null) {
			processingOrgName = leAttr.getAttributeValue();
		}

		return processingOrgName;
	}

	/**
	 * Retrieve the id of the Processing Org
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	private int getProcessingOrgId(final CalypsoLegalEntity calypsoLegalEntity) {
		int processingOrgId = -1;

		LegalEntity legalEntity = this.getLegalEntityProcessingOrg(calypsoLegalEntity);
		if (legalEntity != null) {
			processingOrgId = legalEntity.getId();
		}

		return processingOrgId;
	}

	/**
	 * Retrieve the Processing Org Legal Entity
	 * 
	 * @param calypsoLegalEntity
	 * @return
	 */
	private LegalEntity getLegalEntityProcessingOrg(final CalypsoLegalEntity calypsoLegalEntity) {
		LegalEntity processingOrg = null;
		String processingOrgName = retrieveProcessingOrgName(calypsoLegalEntity);

		if (!Util.isEmpty(processingOrgName)) {
			processingOrg = retrieveLegalEntityByShortName(processingOrgName);
		}
		return processingOrg;
	}

	/**
	 * Returns the id of the Legal Entity INDEVAL of Mexico
	 * 
	 * @return
	 */
	private int getLEMxIndevalId() {
		int leMxIndevalId = -1;

		LegalEntity legalEntity = this.getLegalEntityMxIndeval();
		if (legalEntity != null) {
			leMxIndevalId = legalEntity.getId();
		}

		return leMxIndevalId;
	}

	/**
	 * Returns the Legal Entity INDEVAL of Mexico
	 * 
	 * @return
	 */
	private LegalEntity getLegalEntityMxIndeval() {
		LegalEntity leMxIndeval = null;

		String leMxIndevalName = this.getDomainValueComment(MX_SDI_CASH, MX_LE_INDEVAL);

		if (!Util.isEmpty(leAttrToCheckIfItsValueExists)) {
			leMxIndeval = retrieveLegalEntityByShortName(leMxIndevalName);
		}

		return leMxIndeval;
	}

	/**
	 * It checks if the cash SDI is already created in the system
	 * 
	 * @param name
	 * @param leAgentId
	 * @param legalEntityId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean isTheCashSdiCreated(final String name, final int leAgentId, final int legalEntityId) {
		boolean sdiCreated = true;

		// BOCache.getSettleDeliveryInstruction(dsconnection, id);

		// SELECT * FROM LE_SETTLE_DELIVERY
		// WHERE NAME = 'MX_CUSTODY/INDEMXMMXXX/Nico092_CASH'
		// AND AGENT_LE = 644850
		// AND BENE_LE = 839287
		final StringBuffer whereSQL = new StringBuffer();
		whereSQL.append("NAME = ").append(Util.string2SQLString(name)).append(" ");
		whereSQL.append("AND AGENT_LE = ").append(leAgentId).append(" ");
		whereSQL.append("AND BENE_LE = ").append(legalEntityId).append(" ");

		Vector<SettleDeliveryInstruction> sDIs = this.getRemoteSDI(whereSQL.toString());

		if (Util.isEmpty(sDIs)) {
			sdiCreated = false;

		} else if (sDIs.size() > 1) {
			sdiCreated = false;
			Log.error(this,
					"The query performed should return zero or one record and is returning more than one. Where of query: "
							+ whereSQL.toString());
		}

		return sdiCreated;
	}

	/**
	 * It checks if the security SDI is already created in the system
	 * 
	 * @param name
	 * @param legalEntityId
	 * @return
	 */
	private boolean isTheSecSdiCreated(final String name, final int legalEntityId) {
		boolean sdiCreated = true;

		// BOCache.getSettleDeliveryInstruction(dsconnection, id);

		// SELECT * FROM LE_SETTLE_DELIVERY
		// WHERE NAME = 'MX_CUSTODY/Nico092_CDY'
		// AND BENE_LE = 839287
		final StringBuffer whereSQL = new StringBuffer();
		whereSQL.append("NAME = ").append(Util.string2SQLString(name)).append(" ");
		whereSQL.append("AND BENE_LE = ").append(legalEntityId).append(" ");

		Vector<SettleDeliveryInstruction> sDIs = this.getRemoteSDI(whereSQL.toString());

		if (Util.isEmpty(sDIs)) {
			sdiCreated = false;

		} else if (sDIs.size() > 1) {
			sdiCreated = false;
			Log.error(this,
					"The query performed should return zero or one record and is returning more than one. Where of query: "
							+ whereSQL.toString());
		}

		return sdiCreated;
	}

	/**
	 * Query to BBDD on the LE_SETTLE_DELIVERY table to retrieve SDIs
	 * 
	 * @param where
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Vector<SettleDeliveryInstruction> getRemoteSDI(final String where) {
		Vector<SettleDeliveryInstruction> sDIs = null;

		try {
			sDIs = DSConnection.getDefault().getRemoteReferenceData().getSettleDeliveryInstructions("", where, null);
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this,
					"Error retrieving the query on the LE_SETTLE_DELIVERY table according to the following where: "
							+ sDIs + " Exception: " + calypsoServiceException.toString());
		}

		return sDIs;
	}

}
