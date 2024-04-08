package calypsox.processing;

import java.util.List;
import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.jaxb.xml.Book;
import com.calypso.jaxb.xml.Identifier;
import com.calypso.jaxb.xml.LegalEntityIdentifiers;
import com.calypso.jaxb.xml.Object;
import com.calypso.processing.PreTranslateProcessor;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

public class BookPreTranslator implements PreTranslateProcessor {

	private static final String ATTRIBUTE_VALUE = "ATTRIBUTE_VALUE";
	private static final String ATTRIBUTE_TYPE = "ATTRIBUTE_TYPE";
	private static final String MAPPING_ENTITY_GER = "ALIAS_ENTITY_GER";

	@SuppressWarnings("rawtypes")
	@Override
	public void process(final Object jaxbObject) throws ErrorMessage {
		if (jaxbObject instanceof Book) {
			final Book book = (Book) jaxbObject;

			// BAU 5.6 - Idientifier code of Legal Entity will be the GLCS code instead of company code

			try {

				LegalEntityIdentifiers legalEntityIdentifiers = book.getProcessingOrg();
				List<Identifier> listIdentifier = legalEntityIdentifiers.getIdentifier();

				Identifier codeProcessingOrg = null;
				if (!Util.isEmpty(listIdentifier)) {
					codeProcessingOrg = listIdentifier.get(0);
				}

				if (null != codeProcessingOrg) {
					LegalEntity legalEntity = DSConnection.getDefault().getRemoteReferenceData()
							.getLegalEntity(codeProcessingOrg.getCode().toString());

					if (legalEntity != null) {
						codeProcessingOrg.setCode(legalEntity.getAuthName());
					} else {
						Vector legalEntityVector = DSConnection
								.getDefault()
								.getRemoteReferenceData()
								.getLegalEntityAttributes(
										null,
										ATTRIBUTE_VALUE + " = '" + codeProcessingOrg.getCode() + "' AND "
												+ ATTRIBUTE_TYPE + " = '" + MAPPING_ENTITY_GER + "'");
						if ((null != legalEntityVector) && (legalEntityVector.size() > 0)) {
							LegalEntityAttribute legalEntityAtt = (LegalEntityAttribute) legalEntityVector.get(0);
							if (null != legalEntityAtt) {
								legalEntity = DSConnection.getDefault().getRemoteReferenceData()
										.getLegalEntity(legalEntityAtt.getLegalEntityId());
								if (null != legalEntity) {
									codeProcessingOrg.setCode(legalEntity.getAuthName());
								}
							}
						}
					}
				}
			} catch (Exception ex) {
				Log.error(BookPreTranslator.class, ex);
			}

		}
	}
}
