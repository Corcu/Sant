package calypsox.util.partenon;

import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Optional;

public class PartenonUtil {
    private static PartenonUtil instance = new PartenonUtil();
    public static String PARTENON_MODIF = "PartenonModif";

    public synchronized static PartenonUtil getInstance() {
        if (instance == null) {
            instance = new PartenonUtil();
        }
        return instance;
    }


    public boolean isPartenonChange(Trade trade){
        return Optional.ofNullable(trade).filter(t -> !Util.isEmpty(t.getKeywordValue("OldPartenonAccountingID"))).isPresent();

    }

    public String getAction(Trade trade){
        String partenonModif = Optional.ofNullable(trade).map(tr -> tr.getKeywordValue(PARTENON_MODIF)).orElse("");
        if(!Util.isEmpty(partenonModif)){
            return "3";
        }else {
            return "0";
        }
    }

    public boolean isBookChange(Trade trade){
        return Optional.ofNullable(trade).filter(tr->"Book".equalsIgnoreCase(tr.getKeywordValue(PARTENON_MODIF))).isPresent();
    }

    public String getId(Trade trade){
/*        if(isPartenonChange(trade)){
            return trade.getBook().getName() + trade.getLongId();
        }
        return String.valueOf(trade.getLongId());*/
        return trade.getBook().getName()+ "_" + trade.getLongId();
    }
}
