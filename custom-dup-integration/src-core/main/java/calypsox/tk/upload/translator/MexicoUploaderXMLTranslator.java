package calypsox.tk.upload.translator;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Log;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.CalypsoUploadDocument;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.jaxb.TradeKeywords;
import com.calypso.tk.upload.services.IUploadMessage;
import com.calypso.tk.upload.translator.UploaderXMLTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class MexicoUploaderXMLTranslator extends UploaderXMLTranslator {

    @Override
    public CalypsoUploadDocument translate(IUploadMessage uploadMessage, Vector<BOException> exceptions) {
        try{//TODO Remove
            Object sourceObject = uploadMessage.getSourceObject();
            if (sourceObject == null) {
                sourceObject = uploadMessage.getSourceMessage();
            }
            System.out.println(sourceObject);
        }catch (Exception e){
            Log.error(this,"Error: " + e);
        }

        CalypsoUploadDocument translate = super.translate(uploadMessage, exceptions);
        setDataUploaderSourceKeyword(translate,uploadMessage);
        return translate;
    }

    private void setDataUploaderSourceKeyword(CalypsoUploadDocument translate,IUploadMessage uploadMessage){
        String source = Optional.ofNullable(uploadMessage).map(IUploadMessage::getSource).orElse("Mexico");
        List<CalypsoTrade> calypsoTrades = Optional.ofNullable(translate).map(CalypsoUploadDocument::getCalypsoTrade).orElse(new ArrayList<>());
        calypsoTrades.forEach(calypsoTrade ->{
            TradeKeywords tradeKeywords = calypsoTrade.getTradeKeywords();
            Keyword k = new Keyword();
            k.setKeywordName("DataUploaderSource");
            k.setKeywordValue(source);
            tradeKeywords.getKeyword().add(k);
            calypsoTrade.setTradeKeywords(tradeKeywords);
        });
    }

}
