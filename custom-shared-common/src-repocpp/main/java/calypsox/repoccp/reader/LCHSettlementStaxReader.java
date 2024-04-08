package calypsox.repoccp.reader;

import calypsox.repoccp.model.lch.LCHHeader;
import calypsox.repoccp.model.lch.LCHSettlement;
import calypsox.repoccp.model.lch.netting.*;
import calypsox.repoccp.model.lch.settlement.LCHSettlementReport;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LCHSettlementStaxReader {

    private static final boolean WARN_UNMAPPED_TAGS = true;

    private static final Map<String, ValueSetter<calypsox.repoccp.model.lch.LCHHeader, String, ?>> HEADER_MAPPING = new HashMap<String, ValueSetter<calypsox.repoccp.model.lch.LCHHeader, String, ?>>() {{
        put("reportName", new StringSetter<>(calypsox.repoccp.model.lch.LCHHeader::setReportName));
        put("businessDate", new JDateSetter<>(calypsox.repoccp.model.lch.LCHHeader::setBusinessDate));
        put("emptyReport", new StringSetter<>(calypsox.repoccp.model.lch.LCHHeader::setEmptyReport));
        put("totalNoOfRecords", new IntegerSetter<>(calypsox.repoccp.model.lch.LCHHeader::setTotalNoOfRecords));
        put("creationTimestamp", new StringSetter<>(calypsox.repoccp.model.lch.LCHHeader::setCreationTimestamp));
        put("sendTo", new StringSetter<>(calypsox.repoccp.model.lch.LCHHeader::setSendTo));

    }};

    private static final Map<String, ValueSetter<LCHNetPosition, String, ?>> NET_POSITION_MAPPING = new HashMap<String, ValueSetter<LCHNetPosition, String, ?>>() {{
        put("isin", new StringSetter<>(LCHNetPosition::setIsin));
        put("isinName", new StringSetter<>(LCHNetPosition::setIsinName));
        put("nominalAmount", new DoubleSetter<>(LCHNetPosition::setNominalAmount));
        put("nominalCurrency", new StringSetter<>(LCHNetPosition::setNominalCurrency));
        put("cashAmount", new DoubleSetter<>(LCHNetPosition::setCashAmount));
        put("cashAmountCurrency", new StringSetter<>(LCHNetPosition::setCashAmountCurrency));
        put("bondsReceiver", new StringSetter<>(LCHNetPosition::setBondsReceiver));
        put("bondsDeliverer", new StringSetter<>(LCHNetPosition::setBondsDeliverer));
        put("cashReceiver", new StringSetter<>(LCHNetPosition::setCashReceiver));
        put("cashDeliverer", new StringSetter<>(LCHNetPosition::setCashDeliverer));
        put("netPositionType", new StringSetter<>(LCHNetPosition::setNetPositionType));
    }};

    private static final Map<String, ValueSetter<LCHNettingSetIdentifier, String, ?>> NETTING_SET_IDENTIFIER_MAPPING = new HashMap<String, ValueSetter<LCHNettingSetIdentifier, String, ?>>() {{
        put("dealerId", new StringSetter<>(LCHNettingSetIdentifier::setDealerId));
        put("dealerName", new StringSetter<>(LCHNettingSetIdentifier::setDealerName));
        put("isin", new StringSetter<>(LCHNettingSetIdentifier::setIsin));
        put("isinName", new StringSetter<>(LCHNettingSetIdentifier::setIsinName));
        put("lchMarketCode", new StringSetter<>(LCHNettingSetIdentifier::setLchMarketCode));
        put("houseClient", new StringSetter<>(LCHNettingSetIdentifier::setHouseClient));
        put("settlementDate", new JDateSetter<>(LCHNettingSetIdentifier::setSettlementDate));
        put("settlementCurrency", new StringSetter<>(LCHNettingSetIdentifier::setSettlementCurrency));
        put("membersCsdIcsdTriPartySystem", new StringSetter<>(LCHNettingSetIdentifier::setMembersCsdIcsdTriPartySystem));
    }};

    private static final Map<String, ValueSetter<LCHObligationSetIdentifier, String, ?>> OBLIGATION_SET_IDENTIFIER_MAPPING = new HashMap<String, ValueSetter<LCHObligationSetIdentifier, String, ?>>() {{
        put("isin", new StringSetter<>(LCHObligationSetIdentifier::setIsin));
        put("isinName", new StringSetter<>(LCHObligationSetIdentifier::setIsinName));
        put("lchMarketCode", new StringSetter<>(LCHObligationSetIdentifier::setLchMarketCode));
        put("houseClient", new StringSetter<>(LCHObligationSetIdentifier::setHouseClient));
        put("intendedSettlementDate", new JDateSetter<>(LCHObligationSetIdentifier::setIntendedSettlementDate));
        put("settlementCurrency", new StringSetter<>(LCHObligationSetIdentifier::setSettlementCurrency));
        put("membersCsdIcsdTriPartySystem", new StringSetter<>(LCHObligationSetIdentifier::setMembersCsdIcsdTriPartySystem));
    }};


    private static final Map<String, ValueSetter<LCHObligation, String, ?>> OBLIGATION_MAPPING = new HashMap<String, ValueSetter<LCHObligation, String, ?>>() {{
        put("settlementReferenceInstructed", new StringSetter<>(LCHObligation::setSettlementReferenceInstructed));
        put("lchCsdIcsdTriPartySystem", new StringSetter<>(LCHObligation::setLchCsdIcsdTriPartySystem));
        put("membersAccount", new StringSetter<>(LCHObligation::setMembersAccount));
        put("nominalInstructed", new DoubleSetter<>(LCHObligation::setNominalInstructed));
        put("cashAmountInstructed", new DoubleSetter<>(LCHObligation::setCashAmountInstructed));
        put("cashAmountCurrency", new StringSetter<>(LCHObligation::setCashAmountCurrency));
        put("bondsReceiver", new StringSetter<>(LCHObligation::setBondsReceiver));
        put("bondsDeliverer", new StringSetter<>(LCHObligation::setBondsDeliverer));
        put("cashReceiver", new StringSetter<>(LCHObligation::setCashReceiver));
        put("cashDeliverer", new StringSetter<>(LCHObligation::setCashDeliverer));
        put("settlementType", new StringSetter<>(LCHObligation::setSettlementType));
    }};

    private static final Map<String, ValueSetter<LCHNettedTrade, String, ?>> TRADE_MAPPING = new HashMap<String, ValueSetter<LCHNettedTrade, String, ?>>() {{
        put("registeredDate", new StringSetter<>(LCHNettedTrade::setRegisteredDate));
        put("registeredTime", new StringSetter<>(LCHNettedTrade::setRegisteredTime));
        put("buyerSellerReference", new StringSetter<>(LCHNettedTrade::setBuyerSellerReference));
        put("tradeSourceName", new StringSetter<>(LCHNettedTrade::setTradeSourceName));
        put("lchNovatedTradeReference", new StringSetter<>(LCHNettedTrade::setLchNovatedTradeReference));
        put("buyerSeller", new StringSetter<>(LCHNettedTrade::setBuyerSeller));
        put("tradeType", new StringSetter<>(LCHNettedTrade::setTradeType));
        put("nominal", new DoubleSetter<>(LCHNettedTrade::setNominal));
        put("nominalCurrency", new StringSetter<>(LCHNettedTrade::setNominalCurrency));
        put("cashAmount", new DoubleSetter<>(LCHNettedTrade::setCashAmount));
        put("cashAmountCurrency", new StringSetter<>(LCHNettedTrade::setCashAmountCurrency));
    }};

    private static final Map<String, ValueSetter<LCHSettlement, String, ?>> SETTLEMENT_MAPPING = new HashMap<String, ValueSetter<LCHSettlement, String, ?>>() {{
        put("dealerId", new StringSetter<>(LCHSettlement::setDealerId));
        put("dealerName", new StringSetter<>(LCHSettlement::setDealerName));
        put("clearerId", new StringSetter<>(LCHSettlement::setClearerId));
        put("clearerName", new StringSetter<>(LCHSettlement::setClearerName));
        put("houseClient", new StringSetter<>(LCHSettlement::setHouseClient));
        put("intendedSettlementDate", new StringSetter<>(LCHSettlement::setIntendedSettlementDate));
        put("settlementReferenceInstructed", new StringSetter<>(LCHSettlement::setSettlementReferenceInstructed));
        put("isin", new StringSetter<>(LCHSettlement::setIsin));
        put("isinName", new StringSetter<>(LCHSettlement::setIsinName));
        put("lchMarketCode", new StringSetter<>(LCHSettlement::setLchMarketCode));
        put("memberCsdIcsdTriPartySystem", new StringSetter<>(LCHSettlement::setMemberCsdIcsdTriPartySystem));
        put("memberAccount", new StringSetter<>(LCHSettlement::setMemberAccount));
        put("lchCsdIcsdTriPartySystem", new StringSetter<>(LCHSettlement::setMemberCsdIcsdTriPartySystem));
        put("lchAccount", new StringSetter<>(LCHSettlement::setLchAccount));
        put("nominalInstructed", new DoubleSetter<>(LCHSettlement::setNominalInstructed));
        put("nominalCurrency", new StringSetter<>(LCHSettlement::setNominalCurrency));
        put("cashAmountInstructed", new DoubleSetter<>(LCHSettlement::setCashAmountInstructed));
        put("cashAmountCurrency", new StringSetter<>(LCHSettlement::setCashAmountCurrency));
        put("bondsReceiver", new StringSetter<>(LCHSettlement::setBondsReceiver));
        put("bondsDeliverer", new StringSetter<>(LCHSettlement::setBondsDeliverer));
        put("cashReceiver", new StringSetter<>(LCHSettlement::setCashReceiver));
        put("cashDeliverer", new StringSetter<>(LCHSettlement::setCashDeliverer));
        put("settlementType", new StringSetter<>(LCHSettlement::setSettlementType));
        put("settlementStatus", new StringSetter<>(LCHSettlement::setSettlementStatus));
        put("parentInstructionReference", new StringSetter<>(LCHSettlement::setParentInstructionReference));
    }};

    public LCHNettingReport read(String filePath) throws XMLStreamException, FileNotFoundException {

        XMLStreamReader reader = null;
        try {
            reader = XmlStaxReader.getStreamReader(filePath);

            String currentElement;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    currentElement = reader.getLocalName();
                    if ("nettingReport".equals(currentElement))
                        return readNettingReport(reader);
                }
            }
            Log.error(this, "Root element [nettingReport] not found");
            return null;
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (XMLStreamException e) {
                Log.error(this, e);
            }
        }
    }

    public LCHSettlementReport readSettlementReport(File reportFile) throws XMLStreamException, IOException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(reportFile.toPath()))) {
            XMLStreamReader reader = null;
            try {
                reader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
                String currentElement;
                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event == XMLStreamConstants.START_ELEMENT) {
                        currentElement = reader.getLocalName();
                        if ("settlementReport".equals(currentElement)) {
                            return readSettlementReport(reader);
                        }

                    }
                }

            } finally {
                if (reader != null)
                    reader.close();
            }
        }
        return null;
    }

    private LCHSettlementReport readSettlementReport(XMLStreamReader reader) throws XMLStreamException {
        LCHSettlementReport settlementReport = new LCHSettlementReport();
        String currentElement;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                currentElement = reader.getLocalName();
                switch (currentElement) {
                    case "header":
                        settlementReport.setHeader(parseEntity(reader, currentElement, new LCHHeader(), HEADER_MAPPING));
                        if (settlementReport.getHeader() == null)
                            return null;
                        break;
                    case "settlements":
                        settlementReport.setSettlements(parseSettlements(reader));
                        if (settlementReport.getSettlements() == null)
                            return null;
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("settlementReport".equals(reader.getLocalName()))
                    return settlementReport;
            }
        }
        Log.error(this, "Missing </settlementReport> closing tag.");
        return null;
    }

    private List<LCHSettlement> parseSettlements(XMLStreamReader reader) throws XMLStreamException {
        List<LCHSettlement> settlements = new ArrayList<>();

        String currentTag;
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    currentTag = reader.getLocalName();
                    if ("settlement".equals(currentTag)) {
                        settlements.add(parseEntity(reader, currentTag, new LCHSettlement(), SETTLEMENT_MAPPING));
                    } else {
                        Log.error(this, String.format("Unexpected tag  %s within settlements.", currentTag));
                        return null;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("settlements".equalsIgnoreCase(reader.getLocalName())) {
                        return settlements;
                    }
            }
        }
        Log.error(this, "Missing closing tag  </settlements>.");
        return null;
    }

    public LCHNettingReport readNettingReport(XMLStreamReader reader) throws XMLStreamException {
        LCHNettingReport nettingReport = new LCHNettingReport();
        String currentElement;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                currentElement = reader.getLocalName();
                switch (currentElement) {
                    case "header":
                        nettingReport.setHeader(parseEntity(reader, currentElement, new LCHHeader(), HEADER_MAPPING));
                        if (nettingReport.getHeader() == null)
                            return null;
                        break;
                    case "netting":
                        nettingReport.setNetting(parseNetting(reader));
                        if (nettingReport.getNetting() == null)
                            return null;
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("nettingReport".equals(reader.getLocalName()))
                    return nettingReport;
            }
        }
        Log.error(this, "Missing </nettingReport> closing tag.");
        return null;
    }

    private LCHNetting parseNetting(XMLStreamReader reader) throws XMLStreamException {
        LCHNetting netting = new LCHNetting();
        String currentElement;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                currentElement = reader.getLocalName();
                switch (currentElement) {
                    case "nettingSet":
                        netting.addNettingSet(parseNettingSet(reader));
                        break;
                    case "obligationSet":
                        netting.addObligationSet(parseObligationSet(reader));
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String closingTag = reader.getLocalName();
                if ("netting".equals(closingTag)) {
                    return netting;
                }
            }
        }
        Log.error(this, "Missing </netting> closing tag.");
        return null;
    }


    private LCHObligationSet parseObligationSet(XMLStreamReader reader) throws XMLStreamException {
        LCHObligationSet obligationSet = new LCHObligationSet();
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "obligationSetIdentifier":
                            obligationSet.setIdentifier(parseEntity(reader, reader.getLocalName(), new LCHObligationSetIdentifier(), OBLIGATION_SET_IDENTIFIER_MAPPING));
                            if (obligationSet.getIdentifier() == null)
                                return null;
                            break;
                        case "obligationInputs":
                            obligationSet.setNettedPositions(parseObligationInputs(reader));
                            if (obligationSet.getNettedPositions() == null)
                                return null;
                            break;
                        case "obligationOutputs":
                            obligationSet.setObligations(parseObligations(reader));
                            if (obligationSet.getObligations() == null)
                                return null;
                            break;

                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("obligationSet".equals(reader.getLocalName()))
                        return obligationSet;


            }
        }
        Log.error(this, "Missing </obligationSet> closing tag.");
        return null;
    }

    private List<LCHNetPosition> parseObligationInputs(XMLStreamReader reader) throws XMLStreamException {
        List<LCHNetPosition> netPositions = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("netPositions".equals(localName)) {
                    netPositions.add(parseEntity(reader, localName, new LCHNetPosition(), NET_POSITION_MAPPING));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String localName = reader.getLocalName();
                if ("obligationInputs".equals(localName))
                    return netPositions;
            }
        }

        Log.error(this, "Missing </obligationInputs> closing tag.");
        return null;
    }

    private List<LCHObligation> parseObligations(XMLStreamReader reader) throws XMLStreamException {

        List<LCHObligation> obligations = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("obligations".equals(localName)) {
                    obligations.add(parseEntity(reader, localName, new LCHObligation(), OBLIGATION_MAPPING));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String localName = reader.getLocalName();
                if ("obligationOutputs".equals(localName))
                    return obligations;
            }
        }

        Log.error(this, "Missing </obligationOutputs> closing tag.");
        return null;
    }

    private LCHNettingSet parseNettingSet(XMLStreamReader reader) throws XMLStreamException {

        LCHNettingSet nettingSet = new LCHNettingSet();
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "nettingSetIdentifier":
                            nettingSet.setIdentifier(parseEntity(reader, "nettingSetIdentifier", new LCHNettingSetIdentifier(), NETTING_SET_IDENTIFIER_MAPPING));
                            if (nettingSet.getIdentifier() == null)
                                return null;
                            break;
                        case "nettingInputs":
                            nettingSet.setNettedTrades(parseNettingInputs(reader));
                            if (nettingSet.getNettedTrades() == null)
                                return null;
                            break;
                        case "nettingOutputs":
                            nettingSet.setNetPositions(parseNettingOutputs(reader));
                            if (nettingSet.getNetPositions() == null)
                                return null;
                            break;

                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("nettingSet".equals(reader.getLocalName()))
                        return nettingSet;
            }
        }
        Log.error(this, "Missing </nettingSet> closing tag.");
        return null;
    }

    private List<LCHNetPosition> parseNettingOutputs(XMLStreamReader reader) throws XMLStreamException {
        List<LCHNetPosition> nettedPosition = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    if ("netPositions".equals(reader.getLocalName())) {
                        nettedPosition.add(parseEntity(reader, "netPositions", new LCHNetPosition(), NET_POSITION_MAPPING));
                    } else {
                        Log.error(this, String.format("Unexpected tag %s within netPositions.", reader.getLocalName()));
                        return null;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("nettingOutputs".equalsIgnoreCase(reader.getLocalName())) {
                        return nettedPosition;
                    }
            }
        }
        Log.error(this, "Missing </nettingOutputs> closing tag.");
        return null;
    }

    private <T> T
    parseEntity(XMLStreamReader reader, String tagName, T entity, Map<String, ValueSetter<T, String, ?>>
            entityMapper) throws XMLStreamException {
        String currentTag = null;
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    currentTag = reader.getLocalName();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (currentTag != null) {
                        ValueSetter<T, String, ?> setter = entityMapper.get(currentTag);
                        if (setter != null) {
                            setter.setValue(entity, eventType, reader.getText());
                        } else if (!WARN_UNMAPPED_TAGS) {
                            Log.warn(this, String.format("No mapping for tag %s.", currentTag));
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    currentTag = null;
                    if (tagName.equalsIgnoreCase(reader.getLocalName())) {
                        return entity;
                    }
                    break;
            }


        }
        Log.error(this, String.format("Missing closing tag %s.", tagName));
        return null;
    }

    private List<LCHNettedTrade> parseNettingInputs(XMLStreamReader reader) throws XMLStreamException {
        List<LCHNettedTrade> nettedTrades = new ArrayList<>();

        String currentTag;
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    currentTag = reader.getLocalName();
                    if ("trade".equals(currentTag)) {
                        nettedTrades.add(parseEntity(reader, currentTag, new LCHNettedTrade(), TRADE_MAPPING));
                    } else {
                        Log.error(this, String.format("Unexpected tag  %s within nettingInputs.", currentTag));
                        return null;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("nettingInputs".equalsIgnoreCase(reader.getLocalName())) {
                        return nettedTrades;
                    }
            }
        }
        Log.error(this, "Missing closing tag  </nettingInputs>.");
        return null;
    }


    public interface VoidBiFunction<T, V> {
        void apply(T object, V value);
    }

    private static abstract class ValueSetter<T, V, C> {
        private final VoidBiFunction<T, C> setterFunction;

        private ValueSetter(VoidBiFunction<T, C> setterFunction) {
            this.setterFunction = setterFunction;
        }

        protected boolean setValue(T object, int eventType, V value) {
            if (eventType == XMLEvent.CHARACTERS) {
                setterFunction.apply(object, convert(value));
                return true;
            }
            return false;
        }

        protected abstract C convert(V val);

    }

    private static class StringSetter<T> extends ValueSetter<T, String, String> {
        private StringSetter(VoidBiFunction<T, String> setterFunction) {
            super(setterFunction);
        }

        @Override
        protected String convert(String val) {
            return val;
        }
    }

    private static class IntegerSetter<T> extends ValueSetter<T, String, Integer> {

        private IntegerSetter(VoidBiFunction<T, Integer> setterFunction) {
            super(setterFunction);
        }

        @Override
        protected Integer convert(String val) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                Log.error("invalid number.", e);
                return 0;
            }
        }
    }

    private static class DoubleSetter<T> extends ValueSetter<T, String, Double> {

        private DoubleSetter(VoidBiFunction<T, Double> setterFunction) {
            super(setterFunction);
        }

        @Override
        protected Double convert(String val) {
            return Double.parseDouble(val);
        }
    }

    private static class JDateSetter<T> extends ValueSetter<T, String, JDate> {

        private JDateSetter(VoidBiFunction<T, JDate> setterFunction) {
            super(setterFunction);
        }

        @Override
        protected JDate convert(String val) {
            try {
                return JDate.valueOf((new SimpleDateFormat("yyyy-MM-dd")).parse(val));
            } catch (ParseException e) {
                Log.error("invalid date.", e);
                return null;
            }
        }
    }
}
