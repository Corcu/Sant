package calypsox.tk.upload.translator;

import calypsox.ctm.util.CTMChildTradeExtRefGenerator;
import calypsox.ctm.util.CTMUploaderConstants;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.upload.jaxb.*;
import com.calypso.tk.upload.services.IUploadMessage;
import com.calypso.tk.upload.translator.UploaderXMLTranslator;

import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author aalonsop
 */
public class CTMUploaderXMLTranslator extends UploaderXMLTranslator {

    @Override
    public CalypsoUploadDocument translate(IUploadMessage uploadMessage, Vector<BOException> exceptions) {
        CalypsoUploadDocument uploadDocument = super.translate(uploadMessage, exceptions);
        copyIncomingExtRefIntoKwd(uploadDocument);
        setChildExternalReference(uploadDocument);
        return uploadDocument;
    }

    /**
     * Replaces incoming externalReference with a new one, built with block's trade extRef + ctmAllocation seed value
     *
     * @param calypsoUploadDocument
     */
    private void setChildExternalReference(CalypsoUploadDocument calypsoUploadDocument) {
        executeOperationOverCalypsoTrade(calypsoUploadDocument,
                calypsoTrade -> calypsoTrade.setExternalReference(
                        getAllocatedFromDetailsKwdValueFromTrade(calypsoTrade)
                                .map(CTMChildTradeExtRefGenerator::buildCTMChildExtRef)
                                .orElse(""))
        );
    }

    private void copyIncomingExtRefIntoKwd(CalypsoUploadDocument calypsoUploadDocument) {
        executeOperationOverCalypsoTrade(calypsoUploadDocument,
                calypsoTrade ->
                        addKeywordToCalypsoTrade(calypsoTrade,
                                CTMUploaderConstants.TRADE_KEYWORD_ORIGINAL_EXTERNAL_REF,
                                calypsoTrade.getExternalReference()));
    }

    private void addKeywordToCalypsoTrade(CalypsoTrade calypsoTrade, String kwdName, String kwdValue) {
        Keyword kwdToAdd=new Keyword();
        kwdToAdd.setKeywordName(kwdName);
        kwdToAdd.setKeywordValue(kwdValue);
        Optional.ofNullable(calypsoTrade)
                .map(TradeKeywordsAdapter::new)
                .ifPresent(kwdAdapter -> kwdAdapter.addKeywordToTradeKeywordList(kwdToAdd));
    }

    private Optional<String> getAllocatedFromDetailsKwdValueFromTrade(CalypsoTrade calypsoTrade) {
        return Optional.ofNullable(calypsoTrade)
                .map(CalypsoTrade::getTradeKeywords)
                .map(TradeKeywords::getKeyword)
                .map(this::getAllocatedFromDetailsKwdFromList);
    }

    String getAllocatedFromDetailsKwdFromList(List<Keyword> keywordList) {
        return getKwdFromList(keywordList,CTMUploaderConstants.ALLOCATED_FROM_EXT_REF);

    }

    String getKwdFromList(List<Keyword> keywordList, String targetKwdName) {
        return keywordList.stream()
                .filter(kwd -> kwd.getKeywordName().equals(targetKwdName))
                .findFirst()
                .map(Keyword::getKeywordValue)
                .orElse("");

    }

    protected void executeOperationOverCalypsoTrade(CalypsoUploadDocument calypsoUploadDocument, Consumer<CalypsoTrade> operationToApply) {
        Optional.ofNullable(getCalypsoTradeFromUploadDoc(calypsoUploadDocument))
                .ifPresent(operationToApply);
    }

    protected CalypsoTrade getCalypsoTradeFromUploadDoc(CalypsoUploadDocument calypsoUploadDocument) {
        return Optional.ofNullable(calypsoUploadDocument)
                .map(CalypsoUploadDocument::getCalypsoTrade)
                .map(List::stream)
                .flatMap(Stream::findFirst)
                .orElse(null);
    }
}
