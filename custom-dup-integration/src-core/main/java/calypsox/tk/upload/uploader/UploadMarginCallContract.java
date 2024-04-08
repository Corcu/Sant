package calypsox.tk.upload.uploader;

import calypsox.tk.upload.uploader.margincall.UploadCollateralConfigMapper;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.EligibleSecurityDetails;
import com.calypso.tk.upload.jaxb.MarginCallContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class UploadMarginCallContract extends com.calypso.tk.upload.uploader.UploadCollateralMarginCallContract {

    @Override
    public void upload(CalypsoObject jaxbObject, Vector<BOException> errors, Object dbConn) {
        if (this.collateralConfig != null && jaxbObject instanceof MarginCallContract) {
            new UploadCollateralConfigMapper().map(this.collateralConfig, (MarginCallContract) jaxbObject);
        }

        if (jaxbObject instanceof MarginCallContract) {
            int specialConstraints = 0;
            ArrayList<String[]> constraints = new ArrayList<>();
            List<String> staticDataFilters = ((MarginCallContract) jaxbObject).getEligibleSecurityDetails().getStaticDataFilter();

            String sdFilterName = "";
            String productTypes = "";

            String minFrom = "";
            String maxTo = "";
            String time = "";
            String timeMin = "";
            String ratingAgency = "";
            Boolean firstLine = true;
            Boolean governmentFound = false;
            for (String line : staticDataFilters) {
                productTypes += LyncsSDFilterBuilder.searchProductTypes(line) + ",";

                if(productTypes.contains("governmentBonds") && !governmentFound){
                    specialConstraints++;
                    governmentFound = true;
                }

                if(firstLine){
                    minFrom = LyncsSDFilterBuilder.setMinFromMaturity(minFrom, LyncsSDFilterBuilder.searchMaturityFrom(line));
                    if (timeMin.isEmpty() || timeMin.equalsIgnoreCase(";;")) {
                        timeMin = LyncsSDFilterBuilder.searchMaturityFrom(line);
                        if (!timeMin.isEmpty() && timeMin.split(";").length == 3) {
                            timeMin = timeMin.split(";")[2].substring(0, 1).toUpperCase();
                        }
                    }
                }

                maxTo = LyncsSDFilterBuilder.setMaxToMaturity(maxTo, LyncsSDFilterBuilder.searchMaturityTo(line));
                if(!maxTo.isEmpty() && !maxTo.equalsIgnoreCase(";;")) firstLine = false;

                String tempRatingAgency = LyncsSDFilterBuilder.getRatingAgency(line);
                ratingAgency = LyncsSDFilterBuilder.getMinMaxAgencyValues(ratingAgency,tempRatingAgency);

                if (time.isEmpty() || time.equalsIgnoreCase(";;")) {
                    time = LyncsSDFilterBuilder.searchMaturityTo(line);
                    if (!time.isEmpty() && time.split(";").length == 3) {
                        time = time.split(";")[2].substring(0, 1).toUpperCase();
                    }
                }

                String[] constraint = LyncsSDFilterBuilder.createConstraint(line);
                for (String constr : constraint) {
                    if (constr != null && !constr.isEmpty()) {
                        String[] completeConstraint = constr.split(";");
                        boolean found = false;
                        int positionConstraints = 0;
                        for (String[] repeated : constraints) {
                            if (repeated[0].equalsIgnoreCase(completeConstraint[0]) &&
                                    repeated[1].split(",") != null && repeated[1].split(",").length > 0) {
                                String values = completeConstraint[1];
                                String newValues = "";
                                for (String value : repeated[1].split(",")) {
                                    if (!values.contains(value)) {
                                        newValues += value + ",";
                                    }
                                }

                                if (!newValues.isEmpty() && !newValues.equalsIgnoreCase(",")) {
                                    values += "," + newValues.substring(0, newValues.length() - 1);
                                }
                                repeated[1] = values;
                            }

                            if (repeated[0].equalsIgnoreCase(completeConstraint[0]) &&
                                    repeated[1].contains(completeConstraint[1])) {
                                found = true;
                                break;
                            }

                            if (repeated[0].equalsIgnoreCase(completeConstraint[0]) &&
                                    !repeated[1].contains(completeConstraint[1])) {
                                constraints.get(positionConstraints)[1] = constraints.get(positionConstraints)[1] + "," + completeConstraint[1];
                                found = true;
                                break;
                            }

                            positionConstraints++;
                        }
                        if (!found && completeConstraint != null && completeConstraint[0] != null) {
                            constraints.add(completeConstraint);
                        }
                    }


                }

            }

            if (!LyncsSDFilterBuilder.productTypeElement(productTypes).isEmpty()) {
                specialConstraints++;
            }

            if (!maxTo.isEmpty()) {
                specialConstraints++;
            }

            if (!ratingAgency.isEmpty()) {
                if(ratingAgency.split(";")[0].split("/") != null && ratingAgency.split(";")[0].split("/" +
                        "").length > 0){
                    specialConstraints += ratingAgency.split(";")[0].split("/").length;
                } else
                    specialConstraints++;
            }

            StaticDataFilterElement[] elements = new StaticDataFilterElement[constraints.size() + specialConstraints]; //se suman dos huecos para agregar productType y maturity
            ArrayList<String[]> constraintsExt = new ArrayList<>();
            int contadorElements = 0;
            for (String[] constrain : constraints) {
                StaticDataFilterElement sdElement = null;
                if (constrain[0] != null) {
                    sdElement = LyncsSDFilterBuilder.selectConstraint(constrain);
                    if (sdElement != null) {
                        elements[contadorElements] = sdElement;
                        contadorElements++;
                        constraintsExt.add(constrain);
                    }

                }
            }

            constraints = constraintsExt;

            //Agregamos el filtro del productType() si existe;
            if (!LyncsSDFilterBuilder.productTypeElement(productTypes).isEmpty()) {
                StaticDataFilterElement sdElement = null;
                String products = LyncsSDFilterBuilder.productTypeElement(productTypes);
                String prodConstraintName = LyncsSDFilterBuilder.getFirstLetterFromProducts(products);
                sdElement = LyncsSDFilterBuilder.createProductTypeConstraint(products);
                elements[contadorElements] = sdElement;
                constraints.add(new String[]{"productType", prodConstraintName});
                contadorElements++;
            }

            if(governmentFound){
                StaticDataFilterElement sdElement = null;
                sdElement = LyncsSDFilterBuilder.createSubTypeConstraint("Government");
                elements[contadorElements] = sdElement;
                constraints.add(new String[]{"subType", "Gov"});
                contadorElements++;
            }

            //Agregamos el filtro del maturityDate() si existe;
            if (!maxTo.isEmpty()) {
                StaticDataFilterElement sdElement = null;
                sdElement = LyncsSDFilterBuilder.createMaturityRangeConstraint(maxTo, minFrom, time, timeMin);
                String maturityName = LyncsSDFilterBuilder.getTimeFromMaturity(maxTo, time);
                elements[contadorElements] = sdElement;
                constraints.add(new String[]{"maturityRange", maturityName});
                contadorElements++;
            }

            //Agregamos el filtro del Issuer.Agency() si existe;
            if (!ratingAgency.isEmpty()) {
                if(ratingAgency.split(";")[0].split("/") != null && ratingAgency.split(";")[0].split("/").length > 0){
                    for (int i = 0; i < ratingAgency.split(";")[0].split("/").length; i++ ) {
                        String ratingFull = ratingAgency.split(";")[0].split("/")[i] + ";" +
                                ratingAgency.split(";")[1].split("/")[i] + ";" +
                                ratingAgency.split(";")[2].split("/")[i];

                        StaticDataFilterElement sdElement = null;
                        sdElement = LyncsSDFilterBuilder.createAgencyConstraint(ratingFull);
                        elements[contadorElements] = sdElement;
                        constraints.add(new String[]{"agency" + ratingFull.split(";")[0].substring(0,1), ratingFull.split(";")[2]});
                        contadorElements++;
                    }
                } else {
                    StaticDataFilterElement sdElement = null;
                    sdElement = LyncsSDFilterBuilder.createAgencyConstraint(ratingAgency);
                    elements[contadorElements] = sdElement;
                    constraints.add(new String[]{"agency" + ratingAgency.split(";")[0].substring(0,1), ratingAgency.split(";")[2]});
                    contadorElements++;
                }
            }


            ArrayList<StaticDataFilterElement> finalElements = new ArrayList<>();
            for (StaticDataFilterElement element : elements) {
                if (element != null) {
                    finalElements.add(element);
                }
            }
            elements = finalElements.toArray(new StaticDataFilterElement[finalElements.size()]);

            for (String[] constrain : constraints) {
                if (constrain[0] != null && constrain[1] != null) {
                    String nametoNumber = LyncsSDFilterBuilder.constrainNameToNumber(constrain[0]);
                    String value = LyncsSDFilterBuilder.constraintValueToString(constrain[1]);
                    value = value.replace(",", "");
                    sdFilterName += nametoNumber + value + "_";

                    int iterator = 0;
                    while(sdFilterName.length() >= 64){
                        sdFilterName = LyncsSDFilterBuilder.shorterName(sdFilterName);
                        iterator++;

                        if(iterator > 10){
                            int randomNumber = (int) Math.random() * 1000;
                            sdFilterName = "ErrorNamingSDFilterName" + randomNumber;
                            break;
                        }
                    }
                }
            }

            if (!sdFilterName.isEmpty()) {
                try {
                    if (DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter(sdFilterName) == null) {
                        StaticDataFilter sdf = new StaticDataFilter(sdFilterName, elements);
                        DSConnection.getDefault().getRemoteReferenceData().save(sdf);
                        EligibleSecurityDetails esd = new EligibleSecurityDetails();
                        esd.getStaticDataFilter().add(sdFilterName);
                        ((MarginCallContract) jaxbObject).setEligibleSecurityDetails(esd);
                    } else {
                        EligibleSecurityDetails esd = new EligibleSecurityDetails();
                        esd.getStaticDataFilter().add(sdFilterName);
                        ((MarginCallContract) jaxbObject).setEligibleSecurityDetails(esd);
                    }

                } catch (CalypsoServiceException e) {
                    Log.error(this, e.getCause());
                }
            }
        }

        super.upload(jaxbObject, errors, dbConn);
    }


}
