package calypsox.tk.bo;

import calypsox.util.partenon.PartenonUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Optional;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PARTENONMSGRepoMessageFormatter extends MessageFormatter {

    public String parsePARTENON_ROW(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {
        String partenonMessage = "";
        if(null!=trade){
            partenonMessage = generateMsg(trade);
        }
        return partenonMessage;
    }

    /**
     * 0:Alta 1:Baja 2:Modificacion
     * @param trade
     * @return
     */
    public String generateMsg(Trade trade){
        StringWriter sw = new StringWriter();
        sw.append("CALYPSO").append(";");
        sw.append(PartenonUtil.getInstance().getAction(trade)).append(";");
        sw.append(String.valueOf(trade.getLongId())).append(";");
        sw.append(trade.getBook().getName()).append(";");
        sw.append(trade.getCounterParty().getExternalRef()).append(";");
        sw.append(getAlias(trade)).append(";");
        if(PartenonUtil.getInstance().isBookChange(trade)){
            sw.append(getBookTradeidKey(trade)).append(";");
        }

        return sw.toString();
    }

    /**
     *
     * El campo de construye así
     * $Alias = “”;
     *
     * If Book.AccountingLink ==”Inversion crediticia” o “Disponible para la venta” Then $Alias.append(“IC”) Else $Alias.append(Trade.BondProduct.ProductCode.ISSUE_TYPE)
     * // ejemplo : Alias=IC $Alias.append(“IRDREPOREPO”); // Alias=ICIRDREPOREPO
     *
     * If Trade.Direction ==”Reverse” Then $Alias.append(“BUY”) Else $Alias.append(“SELL”) // ejemplo : Alias=ICIRDREPOREPOBUY
     *
     * If Trade.MirrorBook is not null Then $Alias.append(“INTERNA”) Else $Alias.append(“EXTERNA”) // ejemplo : Alias=ICIRDREPOREPOBUYINTERNA
     * @return
     */


    public String getAlias(Trade trade){
        final Product product = trade.getProduct();
        String result=getIRD();
        if(Optional.ofNullable(product).map(Product::getSubType).filter(Repo.SUBTYPE_BSB::equalsIgnoreCase).isPresent()){
            result=getBSBIRD();
        }
        result=getBookAlias(trade)+result;
        result=result+getDirection(product);
        result=result+getInternalStr(trade);
        result=result+getSecCode(trade,"ISSUE_SUBTYPE");
        result=result+getBond(trade);
        return result;
    }

    protected String getIRD(){
        return "IRDREPOREPO";
    }

    protected String getBSBIRD(){
        return "IRDBSB";
    }

    protected String getBookAlias(Trade trade){
        String bookAlias="";
        String disponibleVentaStr="Disponible para la venta";
        String inversionCrediticia="Inversion crediticia";
        String accBookName="";
        String productSubType="";
        if(Optional.ofNullable(trade).isPresent()){
            accBookName = Optional.ofNullable(trade.getBook().getAccountingBook().getName()).orElse("");
            productSubType = trade.getProductSubType();
        }
        if(accBookName.equalsIgnoreCase(disponibleVentaStr) || accBookName.equalsIgnoreCase(inversionCrediticia)){
            bookAlias = "IC";
        }
        if(Optional.ofNullable(trade).filter(t -> "true".equalsIgnoreCase(t.getKeywordValue("isGCPooling"))).isPresent()){
            bookAlias = bookAlias + "GC";
        }else if ("Triparty".equalsIgnoreCase(productSubType)){
            bookAlias = bookAlias + "TR";
        }else if(!"Y".equalsIgnoreCase(getSecCode(trade,"IS_COVERED"))){
            bookAlias = bookAlias + getSecCode(trade,"ISSUE_TYPE");
        }

        return bookAlias;
    }

    protected String getDirection(Product product){
        return Optional.ofNullable(product).filter(Repo.class::isInstance).map(Repo.class::cast).map(repo -> repo.getDirection(Repo.SUBTYPE_BSB, repo.getSign())).map(String::toUpperCase).orElse("");
    }

    protected String getInternalStr(Trade trade){
        String res="EXTERNA";
        if(Optional.ofNullable(trade.getMirrorBook()).isPresent()){
            res="INTERNA";
        }
        return res;
    }

    protected String getSecCode(Trade trade, String code){
       return Optional.ofNullable(trade)
                .filter(t -> t.getProduct() instanceof Repo)
                .map(p -> ((Repo)p.getProduct()).getSecurity())
                .map(product -> product.getSecCode(code)).orElse("");
    }

    protected String getBond(Trade trade){
        StringBuilder res = new StringBuilder();
        String leCode = "";
        Product product = getSecProduct(trade);

        if(product instanceof Bond){
            int issuerId = ((Bond) product).getIssuerId();
            LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), issuerId);
            if(null!=legalEntity){
                leCode = legalEntity.getCode();
                 if("BSTE".equalsIgnoreCase(leCode)){
                    res.append("AUTOC");
                }
            }
        }

        if("LT".equalsIgnoreCase(getSecCode(trade,"ISSUE_TYPE"))){
            return res.toString();
        }

        if("Y".equalsIgnoreCase(getSecCode(trade,"IS_COVERED"))){
            if("0019".equalsIgnoreCase(getSecCode(trade,"COLLATERAL DESCRIPTION"))
                    || "0019".equalsIgnoreCase(getSecCode(trade,"COLLATERALDESCRIPTION"))){
                res.append("CEDTERR");
            }else{
                res.append("CEDHIPO");
            }
            return res.toString();
        }

        if("Y".equalsIgnoreCase(getSecCode(trade,"IS_SUBORDINATED"))){
            res.append("SUBOR");
            return res.toString();
        }

        String isin = getSecCode(trade, "ISIN");
        Pattern patternFive = Pattern.compile("ES[0-9]5");
        Pattern patternThree = Pattern.compile("ES[0-9]3");
        Matcher matcherFive = patternFive.matcher(isin);
        Matcher matcherThree = patternThree.matcher(isin);

        if(matcherFive.find() || matcherThree.find()){
            String sector = getSector(trade);
            if("086".equalsIgnoreCase(sector)){
                res.append("TITULHIPO");
                return res.toString();
            }else if("084".equalsIgnoreCase(sector)){
                res.append("TITULACT");
                return res.toString();
            }
        }

        String industry_sector = getSecCode(trade, "INDUSTRY_SECTOR");
        if("Mortgage Securities".equalsIgnoreCase(industry_sector)){
            res.append("TITULHIPO");
            return res.toString();
        }else if("Asset Backed Securities".equalsIgnoreCase(industry_sector)){
            res.append("TITULACT");
            return res.toString();
        }

        if("FTJE".equalsIgnoreCase(leCode)){
            res.append("TITULACT");
            return res.toString();
        }

        if("Y".equalsIgnoreCase(getSecCode(trade,"IS_CONVERTIBLE"))){
            res.append("CONV");
            return res.toString();
        }

        if("Y".equalsIgnoreCase(getSecCode(trade,"IS_EXDIVIDEND"))){
            res.append("EXDIV");
            return res.toString();
        }

        if("Y".equalsIgnoreCase(getSecCode(trade,"IS_OPTIONABLE"))){
            res.append("OPC");
            return res.toString();
        }

        return res.toString();
    }

    protected Product getSecProduct(Trade trade){
        return Optional.ofNullable(trade)
                .filter(t -> t.getProduct() instanceof Repo)
                .map(p -> ((Repo) p.getProduct()).getSecurity()).orElse(null);
    }

    protected String getSector(Trade trade) {
        Collection attributes = trade.getCounterParty().getLegalEntityAttributes();// 86

        if (null == attributes) return "";

        for (Object object : attributes) {
            LegalEntityAttribute attribute = (LegalEntityAttribute) object;
            if ("SECTORCONTABLE".equalsIgnoreCase(attribute.getAttributeType())) {
                return attribute.getAttributeValue();
            }
        }
        return "";// 91
    }

    protected String getBookTradeidKey(Trade trade){
        return Optional.ofNullable(trade).filter(t->t.getBook()!=null).map(tr -> tr.getBook().getName() + tr.getLongId()).orElse("");
    }

}

