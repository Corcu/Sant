package com.calypso.tk.upload.jaxb;

import calypsox.tk.refdata.sdfilter.criterionimpl.IsPledgeAccountClosingSDFilterCriterion;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.uploader.calypso.mapping.converter.XMLGregorianCalendarToJDateConverter;
import com.github.dozermapper.core.DozerConverter;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class PledgeTradeDataConverter extends DozerConverter<Trade, CalypsoTrade> {

    XMLGregorianCalendarToJDateConverter dateConverter = new XMLGregorianCalendarToJDateConverter();

    public PledgeTradeDataConverter() {
        super(Trade.class, CalypsoTrade.class);
    }

    @Override
    public CalypsoTrade convertTo(Trade source, CalypsoTrade destination) {
        if(source!=null && source.getProduct() instanceof Pledge) {
            Pledge pledge= (Pledge) source.getProduct();
            if (destination == null) {
                destination = new CalypsoTrade();
            }
            destination.setProductType("Pledge");
            destination.setAction(source.getAction().toString());
            destination.setStartDate(this.dateConverter.convertFrom(pledge.getStartDate()));
            destination.setMaturityDate(this.dateConverter.convertFrom(pledge.getEndDate()));
            destination.setTraderName(source.getTraderName());
            destination.setSalesPerson(source.getSalesPerson());
            destination.setTradeSettleDate(null);
            destination.setTradeNotional(((Pledge) source.getProduct()).getQuantity());

            addKeyword("fixedRate","0",destination);
            setAccountClosingKwd(source,destination);
            setNotNullInternalRef(destination);
            addKeyword("SecurityType", Optional.ofNullable(pledge.getSecurity())
                    .filter(p->p instanceof com.calypso.tk.product.Bond).map(p -> Bond.class.getSimpleName()).orElse(Equity.class.getSimpleName()),destination);

        }
        return destination;
    }

    private void addKeyword(String kwdName, String kwdValue, CalypsoTrade destination){
        TradeKeywords kwds=destination.getTradeKeywords();
        Keyword kwd=new Keyword();
        kwd.setKeywordName(kwdName);
        kwd.setKeywordValue(kwdValue);
        if(kwds!=null&&kwds.getKeyword()!=null){
            kwds.getKeyword().add(kwd);
        }
    }
    @Override
    public Trade convertFrom(CalypsoTrade calypsoTrade, Trade trade) {
        return null;
    }


    private void setNotNullInternalRef(CalypsoTrade destination){
        if(destination.getInternalReference()==null){
            destination.setInternalReference("");
        }
    }

    /**
     * In case of an AC, this KEY_REVERSE kwd is needed by Murex to correctly perform the AC event
     * @param source
     * @param destination
     */
    private void setAccountClosingKwd(Trade source,CalypsoTrade destination){
        SDFilterInput filterInput=SDFilterInput.build(source);
        IsPledgeAccountClosingSDFilterCriterion filterCriterion=new IsPledgeAccountClosingSDFilterCriterion();
        if(filterCriterion.getValue(filterInput)) {
            addKeyword("KEY_REVERSE", String.valueOf(source.getLongId()), destination);
        }
    }

}
