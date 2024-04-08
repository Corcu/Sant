package calypsox.tk.upload.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

import calypsox.tk.upload.uploader.LyncsSDFilterBuilder;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.jaxb.*;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.upload.util.ValidationUtil;

import calypsox.tk.core.RandomString;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class ValidateMarginCallContract extends com.calypso.tk.upload.validator.ValidateMarginCallContract {
    public static String RAND_NAME_SEPARATOR = "__";

    public void validate(CalypsoObject object, Vector<BOException> errors) {
        MarginCallContract jaxbMarginCall = (MarginCallContract) object;

//		GregorianCalendar c = new GregorianCalendar();
//		c.setTime(new Date());
//		try {
//			XMLGregorianCalendar date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
//			jaxbMarginCall.getMarginCallDetails().setStartDateTime(date2);
//		} catch (DatatypeConfigurationException e) {
//			e.printStackTrace();
//		}
//		object = jaxbMarginCall;


        // This method will only return real errors
        validateMAEffectiveDate(jaxbMarginCall, errors);

        // Continue only if no error has been found
        if (Util.isEmpty(errors)) {
            // Achtung, only change name (ext ref) if there are no errors that will prevent Calypso from doing his stuff
            String originalName = jaxbMarginCall.getMarginCallConfigName();
            boolean nameHasBeenModified = false;
            if ("NEW".equalsIgnoreCase(jaxbMarginCall.getAction())) {
                List<Integer> list = ValidationUtil.getMarginCallContractIdByName(jaxbMarginCall.getMarginCallConfigName());
                if (!Util.isEmpty(list)) {
                    MarginCallConfig marginCall = BOCache.getMarginCallConfig(DSConnection.getDefault(), (Integer) list.get(0));

                    // If NEW and name already exists, avoid collisions
                    if (marginCall != null) {
                        RandomString randomID = new RandomString(8);
                        jaxbMarginCall.setMarginCallConfigName(jaxbMarginCall.getMarginCallConfigName() + RAND_NAME_SEPARATOR + randomID.nextString());
                        nameHasBeenModified = true;
                    }
                }
            }

            super.validate(object, errors);

            errors = checkLyncErrors(jaxbMarginCall, errors);

            // if real errors have been found, set name as before
            if (nameHasBeenModified && !Util.isEmpty(errors) && !GatewayUtil.checkIfExceptionsAreWarnings(errors)) {
                jaxbMarginCall.setMarginCallConfigName(originalName);
            }
        }
    }

    private Vector<BOException> checkLyncErrors(MarginCallContract jaxbMarginCall, Vector<BOException> errors) {
        if (errors != null) {
            for (int i = 0; i < errors.size(); i++) {
                if (i >= 0 && errors.get(i).toString().contains("00913")) {
                    errors.remove(i);
                    i--;
                }

                if (i >= 0 && errors.get(i).toString().contains("02012")) {
                    errors.remove(i);
                    TimeDetails details = new TimeDetails();
                    details.setTimeZone("Europe/Paris");
                    GregorianCalendar c = new GregorianCalendar();
                    c.setTime(new Date());
                    c.set(Calendar.HOUR_OF_DAY, 15);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    try {
                        XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
                        details.setTime(date);
                        if (jaxbMarginCall.getValuationDetails() == null) {
                            jaxbMarginCall.setValuationDetails(new ValuationDetails());
                        }
                        jaxbMarginCall.getValuationDetails().setValuationTime(details);

                    } catch (DatatypeConfigurationException e) {
                        Log.error(this, e.getCause());
                    }

                    i--;
                }

                if (i >= 0 && errors.get(i).toString().contains("30312")) {
                    errors.remove(i);
                    GregorianCalendar c = new GregorianCalendar();
                    Date date = new GregorianCalendar(1970, Calendar.JANUARY, 01).getTime();
                    c.setTime(date);
                    try {
                        XMLGregorianCalendar date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
                        if (jaxbMarginCall.getMarginCallDetails() == null) {
                            jaxbMarginCall.setMarginCallDetails(new MarginCallDetails());
                        }
                        jaxbMarginCall.getMarginCallDetails().setStartDateTime(date2);

                    } catch (DatatypeConfigurationException e) {
                        e.printStackTrace();
                    }
                    i--;
                }

                if (jaxbMarginCall.getMarginCallDetails().getContractType() != null) {
                    if (jaxbMarginCall.getMarginCallDetails().getContractType().equalsIgnoreCase("ISMA")
                            || jaxbMarginCall.getMarginCallDetails().getContractType().equalsIgnoreCase("OSLA")) {
                        if (i >= 0 && errors.get(i).toString().contains("Contract Direction")
                                && (errors.get(i).toString().contains("UNSTATED") ||
                                errors.get(i).toString().contains("OTHER"))) {
                            errors.remove(i);
                            i--;
                        }

                        if (i >= 0 && errors.get(i).toString().contains("Valuation Agent Type")
                                && (jaxbMarginCall.getValuationDetails().getValuationAgentType().equalsIgnoreCase("UNSTATED")
                                || jaxbMarginCall.getValuationDetails().getValuationAgentType().equalsIgnoreCase("OTHER"))) {
                            errors.remove(i);
                            i--;
                        }

                        if (i >= 0 && errors.get(i).toString().contains("00222")
                                && errors.get(i).toString().contains("Missing currency")) {
                            jaxbMarginCall.setEligibleCurrencyDetails(new EligibleCurrencyDetails());
                            errors.remove(i);
                            i--;
                        }
                    }
                }
            }
        }

        return errors;
    }

    private void validateMAEffectiveDate(MarginCallContract jaxbMarginCall, Vector<BOException> errors) {
        String cutoffDateStr = LocalCache.getDomainValueComment(DSConnection.getDefault(), "TRLYNCS_EFFECTIVE_DATE", "cutoffDate");
        if (!Util.isEmpty(cutoffDateStr)) {
            LocalDate contractEffDate = formatDate(getAttribute(jaxbMarginCall, "MA_EFFECTIVE_DATE"));
            LocalDate cutoffDate = formatDate(cutoffDateStr);
            if (contractEffDate.isBefore(cutoffDate)) {
                errors.add(ErrorExceptionUtils.createException("21001", " MA_Effective_Date ", "54703", "MA_Effective_Date before cutoff", 0));
            }
        }

    }

    private LocalDate formatDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(date, formatter);
    }

    private String getAttribute(MarginCallContract jaxbMarginCall, String attrName) {
        String res = "";
        List<Attribute> attributes = Optional.ofNullable(jaxbMarginCall.getAdditionalInfo()).map(MarginCallContract.AdditionalInfo::getAttributes)
                .map(Attributes::getAttribute).orElse(new ArrayList<>());
        for (Attribute attribute : attributes) {
            if (attribute != null && attrName.equals(attribute.getAttributeName())) {
                res = attribute.getAttributeValue();
                break;
            }
        }
        return res;
    }
}
