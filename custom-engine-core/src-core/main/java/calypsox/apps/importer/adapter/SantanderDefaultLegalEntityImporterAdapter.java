package calypsox.apps.importer.adapter;

import calypsox.processing.LegalEntityPreTranslator;
import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.processing.error.MessageType;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.PersistenceSession;

import javax.xml.bind.JAXBException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Vector;

public class SantanderDefaultLegalEntityImporterAdapter extends SantanderDefaultImporterAdapter {

    private static final String ZIP_CODE = "zipCode";
    private static final String STATE = "state";
    private static final String CITY = "city";
    private static final String COUNTRY = "country";
    private static final String COUNTERPARTY = "CounterParty";
    private static final String CLIENT = "Client";
    private static final String MAILING_ADDRESS = "mailingAddress";
    private static final String EMAIL_ADDRESS = "emailAddress";
    private static final String DEFAULT_CONTACT = "Default";
    private static final String ALL = "ALL";

    public SantanderDefaultLegalEntityImporterAdapter() throws AdapterException {
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
        LegalEntity leTranslated = null;
        try {
            preTranslate(jaxbObject);
            final Object translated = translateObject(jaxbObject, persistenceSession);
            this.importedObjects.add(translated);

            if (translated instanceof LegalEntity) {
                final Vector messages = new Vector();
                leTranslated = (LegalEntity) translated;
                if (!leTranslated.isValidInput(messages)) {
                    throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, messages.toString());
                }
            }

            // We do a post process, generating and saving the contact information.
            if (null != leTranslated) {
                Collection<LegalEntityAttribute> attributes = leTranslated.getLegalEntityAttributes();
                String mailingAddress = "", emailAddress = "", country = "", city = "", state = "", zipCode = "";
                boolean attrRemoved = false;
                Iterator iter = attributes.iterator();
                while (iter.hasNext()) {
                    LegalEntityAttribute att = (LegalEntityAttribute) iter.next();
                    if (att.getAttributeType().equals(MAILING_ADDRESS)) {
                        mailingAddress = att.getAttributeValue();
                        attributes.remove(att); // We remove from attributes the mailingAddress one.
                        attrRemoved = true;
                    } else if (att.getAttributeType().equals(EMAIL_ADDRESS)) {
                        emailAddress = att.getAttributeValue();
                        attributes.remove(att); // We remove from attributes the emailAddress one.
                        attrRemoved = true;
                    } else if (att.getAttributeType().equals(COUNTRY)) {
                        country = att.getAttributeValue();
                        attributes.remove(att); // We remove from attributes the country one.
                        attrRemoved = true;
                    } else if (att.getAttributeType().equals(CITY)) {
                        city = att.getAttributeValue();
                        attributes.remove(att); // We remove from attributes the city one.
                        attrRemoved = true;
                    } else if (att.getAttributeType().equals(STATE)) {
                        state = att.getAttributeValue();
                        attributes.remove(att); // We remove from attributes the state one.
                        attrRemoved = true;
                    } else if (att.getAttributeType().equals(ZIP_CODE)) {
                        zipCode = att.getAttributeValue();
                        attributes.remove(att); // We remove from attributes the zipCode one.
                        attrRemoved = true;
                    }

                    // To read again the attributes from the object, building the vector with the correct information.
                    if (attrRemoved) {
                        attributes = leTranslated.getLegalEntityAttributes();
                        iter = attributes.iterator();
                        attrRemoved = false;
                    }
                }

                try {
                    // We save the Legal Entity in the system after removing the attributes that we don't want (i.e.
                    // ZipCode, mailingAddrees, etc.).
                    persistenceSession.save(leTranslated);
                    postProcessLE(leTranslated.getAuthName(), mailingAddress, emailAddress, country, city, state,
                            zipCode);
                } catch (RemoteException e) {
                    throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e);
                }
            }
        } catch (final PersistenceException e) {
            throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e);
        } catch (final SecurityException e) {
            throw new ErrorMessage(jaxbObject, MessageType.SECURITY_ERROR, e);
        }
    }

    @Override
    protected void preTranslate(final com.calypso.jaxb.xml.Object jaxbObject) throws ErrorMessage {
        // Do the conversion here, iso_code to country name
        // jaxbObject.get
        final LegalEntityPreTranslator preProcessor = new LegalEntityPreTranslator();
        preProcessor.process(jaxbObject);
        super.preTranslate(jaxbObject);

    }

    /**
     * Method to save the contact associated with the LE inserted in the system.
     *
     * @param authNameLE     LE related with the contact to save.
     * @param mailingAddress mailingAddress field.
     * @param emailAddress   emailAddress field.
     * @param country        Country field.
     * @param city           City field.
     * @param state          State field.
     * @param zipCode        Zip Code field.
     * @throws RemoteException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void postProcessLE(String authNameLE, String mailingAddress, String emailAddress, String country,
                               String city, String state, String zipCode) throws RemoteException {

        LegalEntity le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(authNameLE);

        if (!"".equals(mailingAddress) && !"".equals(emailAddress)) {

            LEContact existingLEContact = DSConnection.getDefault().getRemoteReferenceData()
                    .findContact(le.getEntityId(), ALL, DEFAULT_CONTACT, ALL, 0, le.getEnteredDate().getJDate(TimeZone.getDefault()), "", "");

            LEContact contact = null;
            if (null != existingLEContact) {
                contact = existingLEContact;
            } else {
                contact = new LEContact();
                contact.setContactType(DEFAULT_CONTACT);
            }

            contact.setEmailAddress(emailAddress);
            contact.setMailingAddress(mailingAddress);
            contact.setCountry(country);
            contact.setCityName(city);
            contact.setState(state);
            contact.setZipCode(zipCode);
            contact.setLegalEntityId(le.getEntityId());
            contact.setProductType(ALL);
            contact.setLegalEntityRole(ALL);

            int leContactId = DSConnection.getDefault().getRemoteReferenceData().save(contact);
            if (leContactId <= 0) {
                throw new RemoteException("Contact " + contact.getAuthName() + " not saved");
            }

            // We remove the information in the Legal Entity Attribute to avoid the duplicated values.
            Vector leAttributes = DSConnection.getDefault().getRemoteReferenceData()
                    .getLegalEntityAttributes("LEGAL_ENTITY_ID = " + le.getId());
            if ((null != leAttributes) && !leAttributes.isEmpty()) {
                for (int numAttributes = 0; numAttributes < leAttributes.size(); numAttributes++) {
                    LegalEntityAttribute att = (LegalEntityAttribute) leAttributes.get(numAttributes);
                    if (null != att) {
                        if (att.getAttributeType().equals(EMAIL_ADDRESS)
                                || att.getAttributeType().equals(MAILING_ADDRESS)) {
                            att.setAttributeValue("");

                            // We save the Legal Entity Attributes modified.
                            int attId = DSConnection.getDefault().getRemoteReferenceData().save(att);
                            if (attId <= 0) {
                                throw new RemoteException("Legal Entity Attribute " + att.getAttributeType()
                                        + " not updated");
                            }
                        }
                    }
                }
            }
        }

        // We look for the 'Client' role, and if we don't find it in the LE to insert in the system, we include it
        // in the list.
        boolean findClient = false;
        boolean isCpty = false;
        Vector<String> leRoleList = le.getRoleList();
        if ((null != leRoleList) && !leRoleList.isEmpty()) {
            for (int numRole = 0; numRole < leRoleList.size(); numRole++) {
                if (COUNTERPARTY.equals(leRoleList.get(numRole))) {
                    isCpty = true;
                }
                // We check if the role is equals to 'Client'.
                if (CLIENT.equals(leRoleList.get(numRole))) {
                    findClient = true;
                }
            }
        }
        // Including the 'Client' role.
        if (!findClient && isCpty) {
            leRoleList.add(CLIENT);
            le.setRoleList(leRoleList);
        } else if (findClient && !isCpty) { // Excluding the 'Client' role.
            leRoleList.remove(CLIENT);
            le.setRoleList(leRoleList);
        }

        // We save the Legal Entity modified.
        int leId = DSConnection.getDefault().getRemoteReferenceData().save(le);
        if (leId <= 0) {
            throw new RemoteException("Legal Entity " + le.getAuthName() + " not updated");
        }

    }
}
