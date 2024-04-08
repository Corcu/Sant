package calypsox.processing;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import com.calypso.jaxb.xml.LegalEntityIdentifierAdapter;
import com.calypso.jaxb.xml.LegalEntityIdentifiers;
import org.jfree.util.Log;

import com.calypso.jaxb.xml.DomainValueIdentifier;
import com.calypso.jaxb.xml.Identifier;
import com.calypso.jaxb.xml.Identifiers;
import com.calypso.jaxb.xml.LegalEntity;
import com.calypso.jaxb.xml.LegalEntityAttribute;
import com.calypso.jaxb.xml.Object;
import com.calypso.processing.PreTranslateProcessor;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.DomainValues.DomainValuesRow;
import com.calypso.tk.service.DSConnection;

public class LegalEntityPreTranslator implements PreTranslateProcessor {

	private static final String MAPPING_RISK_SECTOR = "MAPPING_RISK_SECTOR";
	private static final String RISK_SECTOR = "RISK_SECTOR";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void process(final Object jaxbObject) throws ErrorMessage {
		if (jaxbObject instanceof LegalEntity) {
			final LegalEntity legalEntity = (LegalEntity) jaxbObject;

			setParentLegalEntity(legalEntity);

			final Identifiers countryIdentifier = legalEntity.getCountry();
			final List<LegalEntityAttribute> legalEntityAttributes = legalEntity.getAttribute();
			final List<Identifier> identifierList = countryIdentifier.getIdentifier();

			Identifier isoCodeIdentifier = null;
			if (!Util.isEmpty(identifierList)) {
				isoCodeIdentifier = identifierList.get(0);
			}

			if (isoCodeIdentifier != null) {
				try {
					final Country country = DSConnection.getDefault().getRemoteReferenceData()
							.getCountry(isoCodeIdentifier.getCode());
					if (country != null) {
						isoCodeIdentifier.setCode(country.getName());
					}
				} catch (final RemoteException e) {
					Log.error(LegalEntityPreTranslator.class, e);
				}
			}

			// Roles comparation between jaxbLEObject recieved and calypsoLEObject to add new ones, it means overwite
			// jaxbLEObject role list

			// get jaxbLegalEntity roles list on xml format
			List<DomainValueIdentifier> recievedRolesXmlFormat = legalEntity.getRoles();

			if (recievedRolesXmlFormat != null) {

				// get calypsoLegalEntity from DB
				com.calypso.tk.core.LegalEntity calypsoLegalEntity = BOCache.getLegalEntity(DSConnection.getDefault(),
						legalEntity.getCode());

				if (calypsoLegalEntity != null) {

					// get calypsoLegalEntity roles on string format
					Vector<String> actualRolesStringFormat = calypsoLegalEntity.getRoleList();

					if ((actualRolesStringFormat != null) && !actualRolesStringFormat.isEmpty()) {

						// parse jaxbLegalEntity roles on xml format to string format
						List<String> recievedRolesStringFormat = xmlRoleListToStringRoleList(recievedRolesXmlFormat);

						// comapre with calypsoLegalEntity roles and get final role list
						List<String> finalRolesStringFormat = compareAndGetFinalRoles(recievedRolesStringFormat,
								actualRolesStringFormat);

						// parse final role list from string format to xml format
						List<DomainValueIdentifier> jaxbLegalEntityFinalRoles = stringRoleListToXmlRoleList(finalRolesStringFormat);

						// update jaxbLegalEntity role list
						recievedRolesXmlFormat.clear();
						recievedRolesXmlFormat.addAll(jaxbLegalEntityFinalRoles);

					}

				}

			}

			// Implementation to do the mapping between the SSEE code and the Description to show to the users (in the
			// attribute RISK_SECTOR).
			if (!legalEntityAttributes.isEmpty()) {
				for (int i = 0; i < legalEntityAttributes.size(); i++) {
					LegalEntityAttribute leAtt = legalEntityAttributes.get(i);
					if (null != leAtt) {
						DomainValueIdentifier domainValueIdentifier = leAtt.getAttributeType();
						Identifiers identifiers = domainValueIdentifier.getDomainValue();
						if (null != identifiers) {
							List<Identifier> listRSIdentifiers = identifiers.getIdentifier();
							if ((null != listRSIdentifiers) && !listRSIdentifiers.isEmpty()) {
								Identifier identifRiskSector = listRSIdentifiers.get(0);
								if (null != identifRiskSector) {
									if ((null != identifRiskSector.getCode())
											&& identifRiskSector.getCode().contains(RISK_SECTOR)) {
										String leAttributeValue = leAtt.getAttributeValue();
										if (null != leAttributeValue) {
											try {
												Vector mapRiskSector = DSConnection.getDefault()
														.getRemoteReferenceData().getDomainValues(MAPPING_RISK_SECTOR);
												if (null != mapRiskSector) {
													for (int j = 0; j < mapRiskSector.size(); j++) {
														if (leAttributeValue.equals(mapRiskSector.get(j))) {
															DomainValuesRow domainValuesRow = DSConnection
																	.getDefault()
																	.getRemoteReferenceData()
																	.getDomainValuesRow(MAPPING_RISK_SECTOR,
																			leAttributeValue);
															if (null != domainValuesRow) {
																leAttributeValue = domainValuesRow.getComment();
																leAtt.setAttributeValue(leAttributeValue);
															}

															break;
														}
													}
												}
											} catch (RemoteException e) {
												Log.error(LegalEntityPreTranslator.class, e);
											}
										}

										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public List<String> xmlRoleListToStringRoleList(List<DomainValueIdentifier> jaxbLegalEntityRoles) {

		List<String> legalEntityRolesParsed = new ArrayList<String>();

		for (DomainValueIdentifier domainValueIdentifier : jaxbLegalEntityRoles) {
			Identifiers identifiers = domainValueIdentifier.getDomainValue();
			if (identifiers != null) {
				List<Identifier> roleListIdentifiers = identifiers.getIdentifier();
				if ((roleListIdentifiers != null) && !roleListIdentifiers.isEmpty()) {
					Identifier roleIdentifier = roleListIdentifiers.get(0);
					if (roleIdentifier != null) {
						String roleCode = roleIdentifier.getCode();
						if (!Util.isEmpty(roleCode)) {
							String role = xmlRoleToStringRole(roleCode);
							legalEntityRolesParsed.add(role);
						}
					}
				}

			}
		}
		return legalEntityRolesParsed;

	}

	public List<DomainValueIdentifier> stringRoleListToXmlRoleList(List<String> stringLegalEntityRoles) {

		List<DomainValueIdentifier> legalEntityRolesParsed = new ArrayList<DomainValueIdentifier>();

		for (String role : stringLegalEntityRoles) {

			Identifier roleIdentifier = new Identifier();
			String roleCode = stringRoleToXmlRole(role);
			roleIdentifier.setCode(roleCode);
			roleIdentifier.setCodifier("convention");

			Identifiers identifiers = new Identifiers();
			identifiers.getIdentifier().add(roleIdentifier);

			DomainValueIdentifier domainValueIdentifier = new DomainValueIdentifier();
			domainValueIdentifier.setDomainValue(identifiers);
			legalEntityRolesParsed.add(domainValueIdentifier);

		}

		return legalEntityRolesParsed;

	}

	public String xmlRoleToStringRole(String roleToParse) {
		return roleToParse.substring(roleToParse.indexOf('#') + 1);
	}

	public String stringRoleToXmlRole(String roleToParse) {
		return "role#".concat(roleToParse);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<String> compareAndGetFinalRoles(List<String> newRoles, Vector<String> actualRoles) {

		Collection rolesToAdd = new HashSet(newRoles);
		Collection finalRoles = new HashSet(actualRoles);
		// get new roles to add
		rolesToAdd.removeAll(actualRoles);
		// add new roles to existing ones
		finalRoles.addAll(rolesToAdd);

		return new ArrayList(finalRoles);
	}

	private void setParentLegalEntity(LegalEntity jaxbLE){
		List<Identifier> identifiers=Optional.ofNullable(jaxbLE.getParent()).map(Identifiers::getIdentifier).orElse(new ArrayList<>());
		if(identifiers.isEmpty()){
			int parentLEId=Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(),jaxbLE.getCode()))
					.map(com.calypso.tk.core.LegalEntity::getParentId).orElse(0);
			if(parentLEId>0){
				LegalEntityIdentifiers parentLEIdentifier=Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(),parentLEId))
						.map(com.calypso.tk.core.LegalEntity::getCode).map(code-> buildLEIdentifier(code)).orElse(null);
				jaxbLE.setParent(parentLEIdentifier);
			}
		}
	}

	private LegalEntityIdentifiers buildLEIdentifier(String leCode){
		return new LegalEntityIdentifierAdapter(leCode).getLegalEntityIdentifiers();
	}
}
