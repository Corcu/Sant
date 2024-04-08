package calypsox.tk.upload.uploader;

import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.ArrayList;
import java.util.Vector;

public class LyncsSDFilterBuilder {

    protected static String searchNameConstraint(String constType) {
        switch (constType) {
            case "<currencies>":
                return "currencyConstraint";
            case "<countries>":
                return "issuerCountryConstraint";
            case "<supranationalIssuers>":
                return "supranationalIssuersConstraint";
            case "<indexes>":
                return "indexConstraint";
            case "<icadCodes>":
                return "icadConstraint";
            case "<agencies>":
                return "agencyConstraint";
            case "<commoditySubtypes>":
                return "commoditySubTypeConstraint";
            case "<depositoryReceiptSubtypes>":
                return "depositoryReceiptSubTypeConstraint";
            case "<mbsSubtypes>":
                return "mbsSubTypeConstraint";
            case "<callable>":
                return "callableConstraint";
            case "<convertible>":
                return "convertibleConstraint";
            case "<fiSubtypes>":
                return "fiSubTypeConstraint";
            case "<issueDate>":
                return "issueDateConstraint";
            case "<ratingAgencyCoverage>":
                return "ratingAgencyCoverageConstraint";
            case "<listing>":
                return "listingConstraint";
            case "<clearstreamAssetDefinition>":
                return "clearstreamAssetDefinitionConstraint";
            case "<bnymAssetDefinition>":
                return "bnymAssetDefinitionConstraint";
            case "<debtSeniority>":
                return "debtSeniorityConstraint";
            case "<assetClass>":
                return "assetClassConstraint";
            case "<couponType>":
                return "couponTypeConstraint";
            case "<exchange>":
                return "exchangeConstraint";
            default:
                return "EmptyConstraint";
        }
    }

    protected static StaticDataFilterElement selectConstraint(String[] constraint) {
        StaticDataFilterElement sdElement = null;
        if (constraint != null && constraint[0] != null) {
            if (constraint[0].equalsIgnoreCase("currencyConstraint")) {
                sdElement = createCurrencyConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("issuerCountryConstraint")) {
                sdElement = createCountryConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("supranationalIssuersConstraint")) {
                sdElement = createSupranationalIssuersConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("indexConstraint")) {
                sdElement = createIndexConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("icadConstraint")) {
                sdElement = createIcadConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("agencyConstraint")) {
                sdElement = createAgencyConstraint(constraint[1]);
            } else if (constraint[0].equalsIgnoreCase("commoditySubTypeConstraint")) {
                sdElement = createCommoditySubTypeConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("depositoryReceiptSubTypeConstraint")) {
                sdElement = createDepositoryReceiptSubTypeConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("mbsSubTypeConstraint")) {
                sdElement = createMbsSubTypeConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("callableConstraint")) {
                sdElement = createCallableConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("convertibleConstraint")) {
                sdElement = createConvertibleConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("fiSubTypeConstraint")) {
                sdElement = createFiSubTypeConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("couponTypeConstraint")) {
                sdElement = createCouponTypeConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("assetClassConstraint")) {
                sdElement = createAssetClassConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("exchangeConstraint")) {
                sdElement = createExchangeConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("issueDateConstraint")) {
                sdElement = createIssueDateConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("listingConstraint")) {
                sdElement = createListingConstraint(constraint[1], constraint[2]);
            } else if (constraint[0].equalsIgnoreCase("debtSeniorityConstraint")) {
                sdElement = createDebtSeniorityConstraint(constraint[1], constraint[2]);
            }
        }

        return sdElement;
    }


    protected static StaticDataFilterElement createCurrencyConstraint(String value, String operator) {
        value = constraintValueToString(value);
        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Currency");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));

        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createCountryConstraint(String value, String operator) {
        value = constraintValueToString(value);
        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Issuer Country");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createSubTypeConstraint(String value) {
        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Product Sub Type");
        element.setOperatorType(SDFilterOperatorType.IN);

        Vector<String> values = new Vector<>();
        if(value.equalsIgnoreCase("Government")){
            values.add("Bond.Government");
        } else {
            values.add(value);
        }
        element.setValues(values);
        return element;
    }

    //REVISAR
    protected static StaticDataFilterElement createSupranationalIssuersConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Trade Currency");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));

        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }

    protected static StaticDataFilterElement createIndexConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Underlying Security Market");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    //REVISAR
    protected static StaticDataFilterElement createIcadConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Trade Currency");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }


    //REVISAR
    protected static StaticDataFilterElement createCommoditySubTypeConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Trade Currency");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }

    //REVISAR
    protected static StaticDataFilterElement createDepositoryReceiptSubTypeConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Trade Currency");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }

    //REVISAR
    protected static StaticDataFilterElement createMbsSubTypeConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PRODUCT_CODE.MBS_MARKET_SECTOR");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }


    protected static StaticDataFilterElement createCallableConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PRODUCT_CODE.IS_CALLABLE");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createConvertibleConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PRODUCT_CODE.IS_CONVERTIBLE");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    //REVISAR
    protected static StaticDataFilterElement createFiSubTypeConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PENDIENTE");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }

    protected static StaticDataFilterElement createCouponTypeConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PRODUCT_CODE.BOND_TIPOLOGY_1");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createAssetClassConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Fund.AssetClass");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createExchangeConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Exchange");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createIssueDateConstraint(String value, String operator) {
        value = constraintValueToString(value);
        //Esto es un rango de fechas, hay que revisarlo
        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("BOND ISSUE DATE");

        if (operator.equalsIgnoreCase(SDFilterOperatorType.TENOR_RANGE.getDisplayName())) {
            String minFrom = "";
            String maxTo = "";
            element.setOperatorType(SDFilterOperatorType.TENOR_RANGE).setMinValue(minFrom);
            element.setOperatorType(SDFilterOperatorType.TENOR_RANGE).setMaxValue(maxTo);
        } else {
            element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        }
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createRatingAgencyConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PENDIENTE");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }

    protected static StaticDataFilterElement createListingConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Market Type");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createClearStreamAssetConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PENDIENTE");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }

    protected static StaticDataFilterElement createBNymAssetConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PENDIENTE");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return null;
    }

    protected static StaticDataFilterElement createDebtSeniorityConstraint(String value, String operator) {
        value = constraintValueToString(value);

        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("PRODUCT_CODE.DebtSeniority");
        element.setOperatorType(SDFilterOperatorType.valueOfDisplayName(operator.toUpperCase()));
        Vector<String> values = new Vector<>();
        values.add(value);
        element.setValues(values);
        return element;
    }

    protected static String productTypeElement(String value) {
        String[] productTypes = value.split(",");
        Boolean productFind = false;
        Vector<String> productTypesDV = LocalCache.getDomainValues(DSConnection.getDefault(), "productType");
        String result = "";

        for (String type : productTypes) {
            if (!type.equalsIgnoreCase("cash")) {
                if (!result.contains(type)) {
                    String typeT = type.substring(0, 1).toUpperCase() + type.substring(1);
                    if (productTypesDV.contains(typeT)) {
                        type = typeT;
                        productFind = true;
                    } else{
                        if (type.equalsIgnoreCase("GovernmentBonds")) {
                            type = "Bond,BondAssetBacked";
                            //Agregar el subtype Governance a los bonds
                            //Para agregarlo debemos crear otro Elemento.
                            productFind = true;
                        }

                        if (type.equalsIgnoreCase("CommonEquity")) {
                            type = "Equity,EquityIndex";
                            productFind = true;
                        }

                        if (type.equalsIgnoreCase("LetterOfCredit")) {
                            type = "AdvanceLetterCredit";
                            productFind = true;
                        }

                        if (type.equalsIgnoreCase("ConvertibleBonds")) {
                            type = "BondConvertible";
                            productFind = true;
                        }

                        if (type.equalsIgnoreCase("Commodity")) {
                            type = "Commodity";
                            productFind = true;
                        }

                        if (type.equalsIgnoreCase("ICAD")) {
                            type = "Bond";
                            productFind = true;
                        }

                        if (type.equalsIgnoreCase("AsAgreed")) {
                            type = "";
                            //este type no existe
                        }
                    }


                    if (!result.contains(type) && !type.isEmpty()) {
                        result += type + ",";
                    }
                }
            }
        }

        if (result.isEmpty())
            return "";
        else if (!productFind)
            return "";
        else
            return result.substring(0, result.length() - 1);
    }


    protected static StaticDataFilterElement createProductTypeConstraint(String line) {
        StaticDataFilterElement element = new StaticDataFilterElement();
        element.setName("Product Type");
        element.setOperatorType(SDFilterOperatorType.IN);
        Vector<String> values = new Vector<>();
        values.add(line);
        element.setValues(values);
        return element;
    }

    protected static StaticDataFilterElement createMaturityRangeConstraint(String maxTo, String minFrom, String time, String timeMin) {
        StaticDataFilterElement element = new StaticDataFilterElement();
        Vector<String> values = new Vector<>();
        element.setName("Maturity Date");
        if (!maxTo.isEmpty()) {
            element.setOperatorType(SDFilterOperatorType.TENOR_RANGE);
            if (minFrom.isEmpty()) {
                minFrom = "0;D";
            }

            if(timeMin.isEmpty() || timeMin.equalsIgnoreCase(";;")){
                timeMin = "D";
            }

            element.setOperatorType(SDFilterOperatorType.TENOR_RANGE).setMinValue(minFrom.split(";")[0] + timeMin);
            element.setOperatorType(SDFilterOperatorType.TENOR_RANGE).setMaxValue(maxTo.split(";")[0] + time);
        }

        element.setValues(values);
        return element;
    }


    protected static String[] createConstraint(String line) {
        Boolean rowConstraint = false;
        String words[] = line.split(">");
        ArrayList<String> constraintLines = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i] + ">";
            rowConstraint = false;
            String consLine = "";
            if (words[i].contains("<constraints>")) {
                while (!rowConstraint) {
                    consLine += words[i] + ";:";
                    i++;
                    words[i] = words[i] + ">";
                    if (words[i].equalsIgnoreCase("</constraints>")) {
                        rowConstraint = true;
                        constraintLines.add(consLine);
                    }
                }
            }
        }

        String[] constraint = new String[constraintLines.size()];
        int position = 0;
        for (String consLine : constraintLines) {

            Boolean modelEncontrado = false;
            Boolean operatorEncontrado = false;
            Boolean constraintRepeated = false;
            String foundedConstraint = "";
            String constraintName = "";
            String constraintValue = "";
            String operator = "";
            Boolean endModel = false;

            for (String word : consLine.split(";:")) {
                if (modelEncontrado && !constraintName.isEmpty() && !constraintValue.isEmpty() && endModel) {
                    if (operatorEncontrado) {
                        operator = constraintValueToString(word).toUpperCase();
                        break;
                    }

                    if (word.contains("<operator>")) {
                        operatorEncontrado = true;
                    }
                } else {
                    if (!constraintValue.isEmpty()) {
                        if (word.contains("</model>")) {
                            endModel = true;
                        }
                    }

                    if (!foundedConstraint.isEmpty() && constraintRepeated) {
                        constraintRepeated = false;
                        if (!constraintValue.contains(constraintValueToString(word))) {
                            constraintValue += "," + constraintValueToString(word);
                        }
                    }

                    if (!foundedConstraint.isEmpty() && foundedConstraint.equalsIgnoreCase(word)) {
                        constraintRepeated = true;
                    }

                    if (modelEncontrado && !constraintName.isEmpty() && constraintValue.isEmpty()) {
                        constraintValue = constraintValueToString(word);
                    }

                    if (modelEncontrado && constraintName.isEmpty()) {
                        foundedConstraint = word;
                        constraintName = searchNameConstraint(word);
                    }

                    if (word.contains("<model>")) {
                        modelEncontrado = true;
                    }
                }

            }

            if (!constraintName.equalsIgnoreCase("EmptyConstraint")) {
                constraint[position] = constraintName + ";" + constraintValue + ";" + operator;
                position++;
            }

        }
        return constraint;
    }


    protected static String constrainNameToNumber(String constType) {
        switch (constType) {
            case "currencyConstraint":
                return "1";
            case "issuerCountryConstraint":
                return "2";
            case "supranationalIssuersConstraint":
                return "3";
            case "indexConstraint":
                return "4";
            case "icadConstraint":
                return "5";
            case "commoditySubTypeConstraint":
                return "6";
            case "depositoryReceiptSubTypeConstraint":
                return "7";
            case "mbsSubTypeConstraint":
                return "8";
            case "callableConstraint":
                return "9";
            case "convertibleConstraint":
                return "10";
            case "fiSubTypeConstraint":
                return "11";
            case "issueDateConstraint":
                return "12";
            case "ratingAgencyCoverageConstraint":
                return "13";
            case "listingConstraint":
                return "14";
            case "clearstreamAssetDefinitionConstraint":
                return "15";
            case "bnymAssetDefinitionConstraint":
                return "16";
            case "debtSeniorityConstraint":
                return "17";
            case "assetClassConstraint":
                return "18";
            case "couponTypeConstraint":
                return "19";
            case "exchangeConstraint":
                return "20";
            case "productType":
                return "21";
            case "maturityRange":
                return "22";
            case "agencyF":
                return "23";
            case "agencyM":
                return "24";
            case "subType":
                return "26";
            default:
                if (constType.contains("Agency")) {
                    return "25";
                } else
                    return "EmptyConstraint";
        }
    }

    protected static String constraintValueToString(String value) {
        String result = "";
        int position = 0;
        while (position < value.length() && value.charAt(position) != '<') {
            result += value.charAt(position);
            position++;
        }


        return result;
    }

    protected static String searchProductTypes(String line) {
        Boolean typeEncontrado = false;
        String result = "";
        String words[] = line.split(">");

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i] + ">";

            if (typeEncontrado && result.isEmpty()) {
                result = constraintValueToString(words[i]);
                break;
            }

            if (words[i].contains("<type>")) {
                typeEncontrado = true;
            }
        }

        return result;
    }


    protected static String searchMaturityFrom(String line) {
        Boolean endMaturity = false;


        String operator = "";
        String value = "";
        String unit = "";

        String words[] = line.split(">");
        ArrayList<String> maturityWords = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i] + ">";
            if (words[i].contains("<maturityFrom>")) {
                while (!endMaturity) {
                    maturityWords.add(words[i]);
                    i++;
                    words[i] = words[i] + ">";
                    if (words[i].equalsIgnoreCase("</maturityFrom>")) {
                        endMaturity = true;
                        break;
                    }
                }
            }
        }

        for (String maturityWord : maturityWords) {
            if (maturityWord.contains("</operator>")) {
                operator = constraintValueToString(maturityWord);
                if (operator.contains(";")) {
                    operator = operator.replace(";", "");
                }
            }

            if (maturityWord.contains("</value>"))
                value = constraintValueToString(maturityWord);

            if (maturityWord.contains("</unit>"))
                unit = constraintValueToString(maturityWord);

        }

        return operator + ";" + value + ";" + unit;
    }


    protected static String searchMaturityTo(String line) {
        Boolean endMaturity = false;


        String operator = "";
        String value = "";
        String unit = "";

        String words[] = line.split(">");
        ArrayList<String> maturityWords = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i] + ">";
            if (words[i].contains("<maturityTo>")) {
                while (!endMaturity) {
                    maturityWords.add(words[i]);
                    i++;
                    words[i] = words[i] + ">";
                    if (words[i].equalsIgnoreCase("</maturityTo>")) {
                        endMaturity = true;
                        break;
                    }
                }
            }
        }


        for (String maturityWord : maturityWords) {
            if (maturityWord.contains("</operator>")) {
                operator = constraintValueToString(maturityWord);
                if (operator.contains(";")) {
                    operator = operator.replace(";", "");
                }
            }

            if (maturityWord.contains("</value>"))
                value = constraintValueToString(maturityWord);

            if (maturityWord.contains("</unit>"))
                unit = constraintValueToString(maturityWord);

        }

        return operator + ";" + value + ";" + unit;
    }

    protected static String getFirstLetterFromProducts(String line) {
        String result = "";
        String[] products = line.split(",");

        for (String product : products) {
            result += product.substring(0, 1);
        }

        return result;
    }

    protected static String getTimeFromMaturity(String max, String time) {
        return max.split(";")[0] + time;
    }


    protected static String setMinFromMaturity(String minFromOperator, String line) {
        String minFrom = "";
        if (!minFromOperator.isEmpty() && minFromOperator.contains(";") && !line.equalsIgnoreCase(";;")) {
            minFrom = minFromOperator.split(";")[0];
        }

        if (!minFromOperator.isEmpty() && !line.isEmpty() && line.equalsIgnoreCase(";;")) {
            return minFromOperator;
        }

        if (minFrom.isEmpty() && !line.isEmpty() && !line.equalsIgnoreCase(";;")) {
            minFrom = line.split(";")[1];
        }

        if (!minFrom.isEmpty() && !line.isEmpty() && !line.equalsIgnoreCase(";;")) {
            if (Integer.parseInt(line.split(";")[1]) < Integer.parseInt(minFrom)) {
                minFrom = line.split(";")[1];
            }
        }


        if (minFrom.equalsIgnoreCase("1")) {
            minFrom = "0";
        }

        String operator = "";
        if (!line.equalsIgnoreCase(";;")) {
            operator = getComparationOperator(line.split(";")[0]);
        }

        if (minFrom.isEmpty() && operator.isEmpty()) {
            return "";
        } else
            return minFrom + ";" + operator;
    }


    protected static String setMaxToMaturity(String maxToOperator, String line) {
        String maxTo = "";
        if (!maxToOperator.isEmpty() && maxToOperator.contains(";") && !line.equalsIgnoreCase(";;")) {
            maxTo = maxToOperator.split(";")[0];
        }

        if (!maxToOperator.isEmpty() && !line.isEmpty() && line.equalsIgnoreCase(";;")) {
            return maxToOperator;
        }

        if (maxTo.isEmpty() && !line.isEmpty() && !line.equalsIgnoreCase(";;")) {
            maxTo = line.split(";")[1];
        }

        if (!maxTo.isEmpty() && !line.isEmpty() && !line.equalsIgnoreCase(";;")) {
            if (Integer.parseInt(line.split(";")[1]) > Integer.parseInt(maxTo)) {
                maxTo = line.split(";")[1];
            }
        }


        String operator = "";
        if (!line.equalsIgnoreCase(";;")) {
            operator = getComparationOperator(line.split(";")[0]);
        }

        if (maxTo.isEmpty() && operator.isEmpty()) {
            return "";
        } else
            return maxTo + ";" + operator;
    }

    protected static String getComparationOperator(String line) {
        if (line.isEmpty()) {
            return "";
        } else {
            if (line.contains("gte")) {
                return "gte";
            } else if (line.contains("lte")) {
                return "lte";
            } else if (line.contains("gt")) {
                return "gt";
            } else if (line.contains("lt")) {
                return "lt";
            }
        }
        return "";
    }

    public static String getRatingAgency(String line) {
        String agencyName = "";
        String from = "";
        String to = "";


        String fromTo = searchAgencyToFrom(line);
        if (!fromTo.equalsIgnoreCase(";")) {
            from = fromTo.split(";")[0];
            to = fromTo.split(";")[1];
        }

        agencyName = searchAgencyName(line);

        if (agencyName.isEmpty() && from.isEmpty() && to.isEmpty())
            return "";
        else
            return agencyName + ";" + from + ";" + to;

    }

    private static String searchAgencyName(String line) {
        Boolean rowRatings = false;

        String agencyName = "";

        String words[] = line.split(">");
        ArrayList<String> maturityWords = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i] + ">";
            if (words[i].contains("<rowRatings>")) {
                while (!rowRatings) {
                    maturityWords.add(words[i]);
                    i++;
                    words[i] = words[i] + ">";
                    if (words[i].equalsIgnoreCase("</rowRatings>")) {
                        rowRatings = true;
                        break;
                    }
                }
            }
        }


        for (String maturityWord : maturityWords) {
            if (maturityWord.contains("</agency>")) {
                agencyName += constraintValueToString(maturityWord) + "/";
            }
        }

        return agencyName;
    }


    protected static String searchAgencyToFrom(String line) {
        Boolean rowRatings = false;

        String ratingTo = "";
        String ratingFrom = "";

        String words[] = line.split(">");
        ArrayList<String> maturityWords = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i] + ">";
            if (words[i].contains("<rowRatings>")) {
                while (!rowRatings) {
                    maturityWords.add(words[i]);
                    i++;
                    words[i] = words[i] + ">";
                    if (words[i].equalsIgnoreCase("</rowRatings>")) {
                        rowRatings = true;
                        break;
                    }
                }
            }
        }


        for (String maturityWord : maturityWords) {
            if (maturityWord.contains("</ratingTo>")) {
                ratingTo += constraintValueToString(maturityWord) + "/";
                if (ratingTo.contains(";")) {
                    ratingTo = ratingFrom.replace(";", "");
                }
            }

            if (maturityWord.contains("</ratingFrom>")) {
                ratingFrom += constraintValueToString(maturityWord) + "/";
                if (ratingFrom.contains(";")) {
                    ratingFrom = ratingFrom.replace(";", "");
                }
            }
        }

        return ratingFrom + ";" + ratingTo;
    }

    protected static StaticDataFilterElement createAgencyConstraint(String agency) {
        StaticDataFilterElement element = new StaticDataFilterElement();
        Vector<String> values = new Vector<>();
        String agencyName = agency.split(";")[0];

        if (agencyName.equals("Moodys")) {
            agencyName = "Moody";
        }

        String from = agency.split(";")[1];
        String to = agency.split(";")[2];

        element.setName("Issuer.CreditRating." + agencyName);
        element.setOperatorType(SDFilterOperatorType.IN);

        boolean fromEncontrado = false;

        Vector<String> agencyValues = LocalCache.getDomainValues(DSConnection.getDefault(), "Investment_Grade");
        for (String value : agencyValues) {
            if (value.equals(from))
                fromEncontrado = true;

            if (fromEncontrado)
                values.add(value);

            if (value.equals(to)) {
                break;
            }
        }

        if (values.isEmpty()) {
            String ext = from;
            from = to;
            to = ext;
            for (String value : agencyValues) {
                if (value.equals(from))
                    fromEncontrado = true;

                if (fromEncontrado)
                    values.add(value);

                if (value.equals(to)) {
                    break;
                }
            }
        }

        element.setValues(values);
        return element;
    }

    public static String shorterName(String sdFilterName) {
        String shortName = "";
        for (String constrain : sdFilterName.split("_")) {
            String result = "";

            if (constrain.contains("21")) {
                if (constrain.equalsIgnoreCase("21Bb")) {
                    shortName += "21" + "B" + "_";
                }
            } else if (constrain.contains("23")) {
                result = constrain.substring(2, constrain.length() - 1);
                shortName += "23" + result + "_";
            } else if (constrain.contains("24")) {
                result = constrain.substring(2, constrain.length() - 1);
                shortName += "24" + result + "_";
            } else if (constrain.contains("22")) {
                shortName += constrain + "_";
            } else if (constrain.substring(0, 1).equalsIgnoreCase("2")) {
                result = everyNth(constrain.substring(1, constrain.length()), 2);
                result = result.replace("-", "");
                shortName += "2" + result + "_";
            } else if (constrain.substring(0, 1).equalsIgnoreCase("1")) {
                result = everyNth(constrain.substring(1, constrain.length()), 3);
                result = result.replace("-", "");
                shortName += "1" + result + "_";
            } else {
                shortName += constrain + "_";
            }
        }

        return shortName;
    }

    private static String everyNth(String str, int n) {
        StringBuilder result = new StringBuilder(str);
        // This will replace every nth character with '-'
        for (int i = n - 1; i < str.length(); i += n) {
            result.setCharAt(i, '-');
        }
        return result.toString();
    }

    protected static String getMinMaxAgencyValues(String agency, String tempAgency) {
        String result = "";
        String finalNames = "";
        String finalFrom = "";
        String finalTo = "";

        if (tempAgency.isEmpty()) return "";

        for (int i = 0; i < agency.split(";")[0].split("/").length; i++) {
            if (!agency.isEmpty()) {
                String ratingName = agency.split(";")[0].split("/")[i];
                String ratingFrom = agency.split(";")[1].split("/")[i];
                String ratingTo = agency.split(";")[2].split("/")[i];

                if (tempAgency.split(";").length == 3
                        && tempAgency.split(";")[0].split("/").length < i) {

                    String tempRatingName = tempAgency.split(";")[0].split("/")[i];
                    String tempRatingFrom = tempAgency.split(";")[1].split("/")[i];
                    String tempRatingTo = tempAgency.split(";")[2].split("/")[i];

                    if (ratingName.equalsIgnoreCase(tempRatingName)) {
                        if (getAgencyValue(ratingFrom) > getAgencyValue(tempRatingFrom)) {
                            ratingFrom = tempRatingFrom;
                        } else if (getAgencyValue(ratingFrom) > getAgencyValue(tempRatingTo)) {
                            ratingFrom = tempRatingTo;
                        }

                        if (getAgencyValue(ratingTo) < getAgencyValue(tempRatingTo)) {
                            ratingTo = tempRatingTo;
                        } else if (getAgencyValue(ratingTo) < getAgencyValue(tempRatingFrom)) {
                            ratingTo = tempRatingFrom;
                        }

                    }

                    finalNames += ratingName + "/";
                    finalFrom += ratingFrom + "/";
                    finalTo += ratingTo + "/";

                } else return agency;
            } else {
                for (int j = 0; j < tempAgency.split(";")[0].split("/").length; j++) {
                    String tempRatingName = tempAgency.split(";")[0].split("/")[j];
                    String tempRatingFrom = tempAgency.split(";")[1].split("/")[j];
                    String tempRatingTo = tempAgency.split(";")[2].split("/")[j];

                    String temp = "";

                    if (getAgencyValue(tempRatingTo) < getAgencyValue(tempRatingFrom)) {
                        temp = tempRatingTo;
                        tempRatingTo = tempRatingFrom;
                        tempRatingFrom = temp;
                    } else if (getAgencyValue(tempRatingFrom) > getAgencyValue(tempRatingTo)) {
                        temp = tempRatingFrom;
                        tempRatingFrom = tempRatingTo;
                        tempRatingTo = temp;
                    }

                    finalNames += tempRatingName + "/";
                    finalFrom += tempRatingFrom + "/";
                    finalTo += tempRatingTo + "/";
                }
            }
        }

        result = finalNames + ";" + finalFrom + ";" + finalTo;
        return result;

    }


    private static int getAgencyValue(String agencyVal) {
        switch (agencyVal) {
            case "A":
                return 0;
            case "A+":
                return 1;
            case "A-":
                return 2;
            case "AA":
                return 3;
            case "AA+":
                return 4;
            case "AA-":
                return 5;
            case "AAA":
                return 6;
            case "Aaa":
                return 7;
            case "Aa1":
                return 8;
            case "Aa2":
                return 9;
            case "Aa3":
                return 10;
            case "A1":
                return 11;
            case "A2":
                return 12;
            case "A3":
                return 13;
            case "Baa1":
                return 14;
            case "Baa2":
                return 15;
            case "Baa3":
                return 16;
            case "BBB":
                return 17;
            case "BBB+":
                return 18;
            case "BBB-":
                return 19;
            default:
                return -1;
        }
    }
}
