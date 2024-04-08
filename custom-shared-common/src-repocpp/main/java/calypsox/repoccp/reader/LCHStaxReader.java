package calypsox.repoccp.reader;

import calypsox.repoccp.model.ReconCCP;
import calypsox.repoccp.model.lch.*;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.util.InstantiateUtil;
import org.apache.commons.beanutils.BeanUtils;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * @author aalonsop
 */
public class LCHStaxReader extends XmlStaxReader {

    LCHSetIdentifier setIdentifier;

    boolean nettingSet, obligationSet = false;


    /**
     * If desired work in a simple JAXB implementation.
     * Anyway for PRO Stax will be used due to its memory consumption
     *
     * @param reader the XMLStreamReader
     * @return A mapped trade's tag block
     * @throws XMLStreamException if XML error
     */
    @Override
    public ReconCCP next(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("trade".equals(localName)) {
                    LCHTrade lchTrade = parseTrade(reader);
                    if (isCouponIdentifierY(setIdentifier)) {
                        return null;
                    }
                    if (lchTrade != null && setIdentifier != null) {
                        lchTrade.setIdentifier(setIdentifier);
                    }
                    return lchTrade;
                } else if ("netPositions".equals(localName) && nettingSet) {
                    LCHNetPositions netPositions = parseNetPositions(reader);
                    if (isCouponIdentifierY(setIdentifier)) {
                        return null;
                    }
                    if (netPositions != null) {
                        netPositions.setNettingSetIdentifier(setIdentifier);
                        netPositions.setObligationSet(obligationSet);
                        netPositions.setNettingSet(nettingSet);
                    }
                    return netPositions;
                } else if ("netPositions".equals(localName) && obligationSet) {
                    LCHNetPositions netPositions = parseNetPositions(reader);
                    if (isCouponIdentifierY(setIdentifier)) {
                        return null;
                    }
                    if (netPositions != null) {
                        netPositions.setObligationSetIdentifier(setIdentifier);
                        netPositions.setObligationSet(obligationSet);
                        netPositions.setNettingSet(nettingSet);
                    }
                    return netPositions;
                } else if ("obligations".equals(localName)) {
                    LCHObligations lchObligations = parseObligations(reader);
                    if (isCouponIdentifierY(setIdentifier)) {
                        return null;
                    }
                    if (setIdentifier != null && lchObligations != null) {
                        lchObligations.setIdentifier(setIdentifier);
                    }
                    return lchObligations;
                } else if ("nettingSetIdentifier".equals(localName) || "obligationSetIdentifier".equals(localName)) {
                    setIdentifier = parseNettingSetIdentifier(reader);
                } else if ("settlement".equals(localName)) {
                    LCHSettlement settlement = parseSettlement(reader);
                    if ("In-Settlement".equalsIgnoreCase(settlement.getSettlementStatus())) {
                        return settlement;
                    } else {
                        return null;
                    }
                } else if ("nettingSet".equals(localName)) {
                    nettingSet = true;
                } else if ("obligationSet".equals(localName)) {
                    obligationSet = true;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String localName = reader.getLocalName();
                if ("nettingSet".equals(localName)) {
                    nettingSet = false;
                } else if ("obligationSet".equals(localName)) {
                    obligationSet = false;
                }
            }
        }
        return null;
    }

    private boolean isCouponIdentifierY(LCHSetIdentifier identifier) {
        return identifier != null && !Util.isEmpty(identifier.getCouponIdentifier()) && "Y".equalsIgnoreCase(identifier.getCouponIdentifier());
    }

    private LCHSettlement parseSettlement(XMLStreamReader reader) throws XMLStreamException {
        LCHSettlement lchSettlements = new LCHSettlement();
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "dealerId":
                            int eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setDealerId(reader.getText());
                            }
                            break;
                        case "dealerName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setDealerName(reader.getText());
                            }
                            break;
                        case "clearerId":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setClearerId(reader.getText());
                            }
                            break;
                        case "clearerName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setClearerName(reader.getText());
                            }
                            break;
                        case "houseClient":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setHouseClient(reader.getText());
                            }
                            break;
                        case "intendedSettlementDate":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setIntendedSettlementDate(reader.getText());
                            }
                            break;
                        case "settlementReferenceInstructed":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setSettlementReferenceInstructed(reader.getText());
                            }
                            break;
                        case "isin":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setIsin(reader.getText());
                            }
                            break;
                        case "isinName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setIsinName(reader.getText());
                            }
                            break;
                        case "lchMarketCode":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setLchMarketCode(reader.getText());
                            }
                            break;
                        case "memberCsdIcsdTriPartySystem":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setMemberCsdIcsdTriPartySystem(reader.getText());
                            }
                            break;
                        case "memberAccount":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setMemberAccount(reader.getText());
                            }
                            break;
                        case "lchCsdIcsdTriPartySystem":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setLchCsdIcsdTriPartySystem(reader.getText());
                            }
                            break;
                        case "lchAccount":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setLchAccount(reader.getText());
                            }
                            break;
                        case "nominalInstructed":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setNominalInstructed(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "nominalRemaining":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setNominalRemaining(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "nominalCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setNominalCurrency(reader.getText());
                            }
                            break;
                        case "cashAmountInstructed":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setCashAmountInstructed(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "cashAmountRemaining":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setCashAmountRemaining(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "cashAmountCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setCashAmountCurrency(reader.getText());
                            }
                            break;
                        case "bondsReceiver":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setBondsReceiver(reader.getText());
                            }
                            break;
                        case "bondsDeliverer":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setBondsDeliverer(reader.getText());
                            }
                            break;
                        case "cashReceiver":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setCashReceiver(reader.getText());
                            }
                            break;
                        case "cashDeliverer":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setCashDeliverer(reader.getText());
                            }
                            break;
                        case "settlementType":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setSettlementType(reader.getText());
                            }
                            break;
                        case "settlementStatus":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setSettlementStatus(reader.getText());
                            }
                            break;
                        case "parentInstructionReference":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                lchSettlements.setParentInstructionReference(reader.getText());
                            }
                            break;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("settlement".equalsIgnoreCase(reader.getLocalName())) {
                        return lchSettlements;
                    }
                    break;
            }
        }
        return lchSettlements;
    }

    private LCHSetIdentifier parseNettingSetIdentifier(XMLStreamReader reader) throws XMLStreamException {
        LCHSetIdentifier setIdentifier = new LCHSetIdentifier();
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "clearerId":
                            int eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setClearerId(reader.getText());
                            }
                            break;
                        case "clearerName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setClearerName(reader.getText());
                            }
                            break;
                        case "isin":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setIsin(reader.getText());
                            }
                            break;
                        case "isinName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setIsinName(reader.getText());
                            }
                            break;
                        case "lchMarketCode":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setLchMarketCode(reader.getText());
                            }
                            break;
                        case "houseClient":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setHouseClient(reader.getText());
                            }
                            break;
                        case "settlementDate":
                        case "intendedSettlementDate":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setSettlementDate(reader.getText());
                            }
                            break;
                        case "settlementCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setSettlementCurrency(reader.getText());
                            }
                            break;
                        case "membersCsdIcsdTriPartySystem":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setMembersCsdIcsdTriPartySystem(reader.getText());
                            }
                            break;
                        case "couponIdentifier":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                setIdentifier.setCouponIdentifier(reader.getText());
                            }
                            break;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("nettingSetIdentifier".equalsIgnoreCase(reader.getLocalName())
                            || "obligationSetIdentifier".equalsIgnoreCase(reader.getLocalName())) {
                        return setIdentifier;
                    }
                    break;
            }
        }
        return setIdentifier;
    }

    private LCHNetPositions parseNetPositions(XMLStreamReader reader) throws XMLStreamException {
        LCHNetPositions netPositions = new LCHNetPositions();
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "nominalAmount":
                            int eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setNominal(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "nominalCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setNominalCurrency(reader.getText());
                            }
                            break;
                        case "cashAmount":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setCashAmount(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "cashAmountCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setCashCurrency(reader.getText());
                            }
                            break;
                        case "bondsReceiver":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setBondsReceiver(reader.getText());
                            }
                            break;
                        case "bondsDeliverer":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setBondsDeliverer(reader.getText());
                            }
                            break;
                        case "cashReceiver":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setCashReceiver(reader.getText());
                            }
                            break;
                        case "cashDeliverer":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setCashDeliverer(reader.getText());
                            }
                            break;
                        case "netPositionType":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                netPositions.setNetPositionType(reader.getText());
                            }
                            break;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("netPositions".equalsIgnoreCase(reader.getLocalName())) {
                        return netPositions;
                    }
                    break;
            }
        }
        return netPositions;
    }

    private LCHObligations parseObligations(XMLStreamReader reader) throws XMLStreamException {
        LCHObligations obligations = new LCHObligations();
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "settlementReferenceInstructed":
                            int eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setSettlementReferenceInstructed(reader.getText());
                            }
                            break;
                        case "lchCsdIcsdTriPartySystem":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setLchCsdIcsdTriPartySystem(reader.getText());
                            }
                            break;
                        case "membersAccount":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setMembersAccount(reader.getText());
                            }
                            break;
                        case "lchAccount":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setLchAccount(reader.getText());
                            }
                            break;
                        case "nominalInstructed":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setNominalInstructed(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "nominalCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setNominalCurrency(reader.getText());
                            }
                            break;
                        case "cashAmountInstructed":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setCashAmountInstructed(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "cashAmountCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setCashAmountCurrency(reader.getText());
                            }
                            break;
                        case "bondsReceiver":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setBondsReceiver(reader.getText());
                            }
                            break;
                        case "bondsDeliverer":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setBondsDeliverer(reader.getText());
                            }
                            break;
                        case "cashReceiver":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setCashReceiver(reader.getText());
                            }
                            break;
                        case "cashDeliverer":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setCashDeliverer(reader.getText());
                            }
                            break;
                        case "settlementType":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                obligations.setSettlementType(reader.getText());
                            }
                            break;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("obligations".equalsIgnoreCase(reader.getLocalName())) {
                        return obligations;
                    }
                    break;
            }
        }
        return obligations;
    }

    /**
     * Does the mapping
     * NOT WORKING YET, code looping over the xml stream is not fine
     *
     * @return the LCHTrade
     * @throws XMLStreamException
     */
    private LCHTrade parseTrade(XMLStreamReader reader) throws XMLStreamException {
        LCHTrade trade = new LCHTrade() {
            @Override
            public boolean matchReference(Trade calypsoTrade) {

                String elPlatform = calypsoTrade.getKeywordValue(Mx_Electplatf);
                String platformId;
                if (calypsoTrade.getProduct() instanceof Bond) {
                    platformId = calypsoTrade.getKeywordValue(Mx_ELECPLATID);
                    if (Util.isEmpty(platformId)) {
                        platformId = calypsoTrade.getKeywordValue(Mx_Electplatid);
                    }
                    if (Util.isEmpty(platformId)) {
                        platformId = calypsoTrade.getKeywordValue(Mx_GID);
                    }
                } else {
                      platformId = calypsoTrade.getKeywordValue(Mx_Electplatid);
                }
                if (Util.isEmpty(platformId))
                    return false;

                switch (elPlatform) {
                    case "BTEC":
                        if (Util.isEmpty(platformId))
                            return false;
                        if (!Util.isEmpty(getTradeSourceCode()) && !"BT".equals(getTradeSourceCode()))
                            return false;
                        if (calypsoTrade.getProduct() instanceof Bond) {
                            String id = platformId.substring(0, Math.min(8, platformId.length() - 1));
                            return !Util.isEmpty(getBuyerSellerReference()) && getBuyerSellerReference().length() > 3 && id.equals(getBuyerSellerReference().substring(3));
                        } else if (calypsoTrade.getProduct() instanceof Repo) {
                            String[] parts = platformId.split("_");
                            return parts.length > 1 && parts[1].length() > 3 && parts[1].equals(getBuyerSellerReference().substring(3));
                        }
                        return false;
                    case "VOZ":
                        return calypsoTrade.getProduct() instanceof Repo && (("CYO" + platformId).equals(getBuyerSellerReference())
                                || (!Util.isEmpty(getBuyerSellerReference()) && getBuyerSellerReference().length() > 3 && platformId.equals(getBuyerSellerReference().substring(3))));
                    case "MTS":
                        if (Util.isEmpty(getBuyerSellerReference()) || getBuyerSellerReference().length() < 8)
                            return false;

                        String lchRef = this.getBuyerSellerReference().substring(7, Math.min(13, getBuyerSellerReference().length() - 1));
                        if (calypsoTrade.getProduct() instanceof Bond) {
                            if (Util.isEmpty(platformId))
                                return false;
                            String ref = platformId.replace("MTS", "").trim();

                            return ref.equals(lchRef);
                        }
                        if (calypsoTrade.getProduct() instanceof Repo) {
                            String gid = calypsoTrade.getKeywordValue("Mx GID");
                            if (Util.isEmpty(gid))
                                gid = calypsoTrade.getKeywordValue("Mx Global ID");

                            if (Util.isEmpty(gid) || !gid.startsWith("REPO_"))
                                return false;
                            gid = gid.replace("REPO_", "").trim();
                            return gid.equals(lchRef);

                        }
                    default:
                        return this.getBuyerSellerReference().equals(calypsoTrade.getKeywordValue("BuyerSellerReference"));
                }
            }

            @Override
            protected long getExternalId() {
                try {
                    return Long.parseLong(this.getBuyerSellerReference().substring(3, 11));
                } catch (NumberFormatException e) {
                    Log.error(this, e.getCause());
                }
                return 0L;
            }
        };

        String tradeSourceNameInst = "";
        String tradeTypeInst = "";

        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "dealerId":
                            int eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setDealerId(reader.getText());
                            }
                            break;
                        case "dealerName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setDealerName(reader.getText());
                            }
                            break;
                        case "clearerId":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setClearerId(reader.getText());
                            }
                            break;
                        case "clearerName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setClearerName(reader.getText());
                            }
                            break;
                        case "registrationStatus":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRegistrationStatus(reader.getText());
                            }
                            break;
                        case "tradeDate":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setTradeDate(reader.getText());
                            }
                            break;
                        case "tradeTime":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setTradeTime(reader.getText());
                            }
                            break;
                        case "lchTradeReference":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setLchTradeReference(reader.getText());
                            }
                            break;
                        case "buyerSellerReference":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setBuyerSellerReference(reader.getText());
                            }
                            break;
                        case "tradeSourceReference":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setTradeSourceReference(reader.getText());
                            }
                            break;
                        case "tradeSourceCode":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setTradeSourceCode(reader.getText());
                            }
                            break;
                        case "tradeSourceName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                String tradeSourceName = reader.getText();
                                trade.setTradeSourceName(tradeSourceName);
                                if (tradeSourceName.contains("MTS")) {
                                    tradeSourceNameInst = "MTS";
                                } else if (tradeSourceName.contains("CME")) {
                                    tradeSourceNameInst = "BTEC";
                                } else if (tradeSourceName.contains("ETCMS")) {
                                    tradeSourceNameInst = "ETCMS";
                                }
                            }
                            break;
                        case "lchNovatedTradeReference":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setLchNovatedTradeReference(reader.getText());
                            }
                            break;
                        case "buyerSeller":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setBuyerSeller(reader.getText());
                            }
                            break;
                        case "houseClient":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setHouseClient(reader.getText());
                            }
                            break;
                        case "tradeType":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                String tradeType = reader.getText();
                                trade.setTradeType(tradeType);
                                if ("REPO".equals(tradeType)) {
                                    tradeTypeInst = "Repo";
                                } else {
                                    tradeTypeInst = "Bond";
                                }
                            }
                            break;
                        case "isin":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setIsin(reader.getText());
                            }
                            break;
                        case "isinName":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setIsinName(reader.getText());
                            }
                            break;
                        case "lchMarketCode":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setLchMarketCode(reader.getText());
                            }
                            break;
                        case "tradePrice":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setTradePrice(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "nominal":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setNominal(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "nominalCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setNominalCurrency(reader.getText());
                            }
                            break;
                        case "cashCurrency":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setCashCurrency(reader.getText());
                            }
                            break;
                        case "registeredRejectedDate":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRegisteredRejectedDate(reader.getText());
                            }
                            break;
                        case "registeredRejectedTime":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRegisteredRejectedTime(reader.getText());
                            }
                            break;
                        case "memberCSD":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setMemberCSD(reader.getText());
                            }
                            break;
                        case "accruedCoupon":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setAccruedCoupon(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "intendedSettlementDate":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setIntendedSettlementDate(reader.getText());
                            }
                            break;
                        case "cashAmount":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setCashAmount(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "MIC":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setMIC(reader.getText());
                            }
                            break;
                        case "haircut":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setHaircut(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "uniqueTradeIdentifier":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setUniqueTradeIdentifier(reader.getText());
                            }
                            break;
                        case "repoType":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRepoType(reader.getText());
                            }
                            break;
                        case "startDate":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRepoStartDate(reader.getText());
                            }
                            break;
                        case "startCash":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRepoStartCash(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "endDate":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRepoEndDate(reader.getText());
                            }
                            break;
                        case "endCash":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRepoEndCash(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "repoRate":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setRepoRate(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "forfeitAmount":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setForfeitAmount(Double.parseDouble(reader.getText()));
                            }
                            break;
                        case "portfolioCode":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setPortfolioCode(reader.getText());
                            }
                            break;
                        case "dayCountConvention":
                            eventType = reader.next();
                            if (eventType == XMLEvent.CHARACTERS) {
                                trade.setDayCountConvention(reader.getText());
                            }
                            break;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("trade".equalsIgnoreCase(reader.getLocalName())) {
                        LCHTrade childTrade = instantiateChild(trade, tradeTypeInst, tradeSourceNameInst);
                        if (childTrade != null) {
                            trade = childTrade;
                        }
                        return trade;
                    }
                    break;
            }
        }
        return trade;
    }

    private LCHTrade instantiateChild(LCHTrade parent, String tradeTypeInst, String tradeSourceNameInst) {
        String packageName = "";
        if (!Util.isEmpty(tradeSourceNameInst)) {
            packageName = tradeSourceNameInst.toLowerCase(Locale.ROOT) + ".";
        }

        Object trade;
        try {
            trade = InstantiateUtil.getInstance("calypsox.repoccp.model.lch." + packageName + tradeTypeInst + tradeSourceNameInst + "LCHTrade", true, true);
        } catch (InstantiationException | IllegalAccessException e) {
            try {
                trade = InstantiateUtil.getInstance("calypsox.repoccp.model.lch." + packageName + tradeSourceNameInst + "LCHTrade", true, true);
            } catch (InstantiationException | IllegalAccessException exc) {
                return null;
            }
        }
        try {
            BeanUtils.copyProperties(trade, parent);
        } catch (InvocationTargetException | IllegalAccessException e) {
            Log.error(this, e.getCause());
        }

        return (LCHTrade) trade;
    }
}
