package calypsox.tk.csdr;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CSDRFiFlowLineBean {

    private final String trnMsg;
    private final String book;
    private final String extTradeId;
    private final String foTradeId;
    private final String extXferId;
    private final String glcs;

    public CSDRFiFlowLineBean(List<String> tokenizedLine){
        this.trnMsg= Optional.ofNullable(tokenizedLine).map(line->line.get(0)).orElse("");
        this.book=Optional.ofNullable(tokenizedLine).map(line->line.get(1)).orElse("");
        this.extTradeId=Optional.ofNullable(tokenizedLine).map(line->line.get(2)).orElse("");
        this.foTradeId=Optional.ofNullable(tokenizedLine).map(line->line.get(3)).orElse("");
        this.extXferId=Optional.ofNullable(tokenizedLine).map(line->line.get(4)).orElse("");
        this.glcs=Optional.ofNullable(tokenizedLine).map(line->line.get(5)).orElse("");
    }

    public String getTrnMsg() {
        return trnMsg;
    }

    public String getBook() {
        return book;
    }


    public String getExtTradeId() {
        return extTradeId;
    }

    public String getExtXferId() {
        return extXferId;
    }

    public String getFoTradeId() {
        return foTradeId;
    }

    public String getGlcs() {
        return glcs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CSDRFiFlowLineBean lineBean = (CSDRFiFlowLineBean) o;
        return trnMsg.equals(lineBean.trnMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trnMsg);
    }
}
