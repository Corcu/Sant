package calypsox.tk.bo.acyg;


import com.calypso.tk.bo.BalancePosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.Account;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public class BalancesPostingBean {

    public static final String PARTENONACCOUNTINGID = "PartenonAccountingID";
    public static final String CCY_EUR = "EUR";

    private String empresaContrato = "";
    private String centroContrato = "";
    private String codigoProductoCatalogo1 = "";
    private String partenonContract = "";
    private String subContract = "";
    private String codigoProductoCatalogo2 = "";
    private String book = "";
    private String monedaContrato = "";
    private String monedaRelacionada = "";
    private String posicion = "";
    private String situacion = "";
    private String morosidad = "";
    private String importeSaldo = "";
    private String importeSaldoContValor = "";
    private String monedaContravalor = "";
    private String empresaContable = "";
    private String centroContable = "";
    private String filler = "";

    Trade trade;
    BalancePosition balance;
    Account account;
    JDate processDate;
    String currency;
    PricingEnv pricingEnv;
    BalancesPostingFormatter formatter;

    public BalancesPostingBean() {
        formatter = BalancesPostingFormatter.getInstance();
    }

    public void build() {
        this.fillPartenonInfo();
        this.setSubContract(emptyString(7));
        this.setBook(loadBook());
        this.setSituacion(emptyString(3));
        this.setMorosidad(emptyString(3));
        this.setPosicion(loadPosicion());
        this.loadImports();
        this.setMonedaRelacionada(loadMonedaRelacionada());
        this.setFiller();
    }


    public String loadPosicion() {
        String pos = Optional.ofNullable(account.getAccountProperty("Posicion")).orElse(emptyString(3));
        return pos;
    }

    public void loadImports() {
        if (account.getName().substring(0, 3).equalsIgnoreCase(balance.getCurrency())) {
            this.setMonedaContrato(formatter.formatLeftString(balance.getCurrency(), 3));
            if (balance.getTotal() >= 0.0) {
                this.setImporteSaldo("+" + formatter.formatDecimal(balance.getTotal()));
            } else {
                this.setImporteSaldo(formatter.formatDecimal(balance.getTotal()));
            }
        }
        if (CCY_EUR.equalsIgnoreCase(balance.getCurrency())) {
            this.setMonedaContravalor(formatter.formatLeftString(balance.getCurrency(), 3));
            if (balance.getTotal() >= 0.0) {
                this.setImporteSaldoContValor("+" + formatter.formatDecimal(balance.getTotal()));
            } else {
                this.setImporteSaldoContValor(formatter.formatDecimal(balance.getTotal()));
            }
        }
    }

    public void fillPartenonInfo() {
        String partenon = Optional.ofNullable(trade.getKeywordValue(PARTENONACCOUNTINGID)).orElse("");

        // if (!Util.isEmpty(partenon)) {
        this.setEmpresaContrato(checkString(partenon, 0, 4));
        this.setCentroContrato(checkString(partenon, 4, 8));
        this.setCodigoProductoCatalogo1(checkString(partenon, 8, 11));
        this.setPartenonContract(checkString(partenon, 11, 18));
        this.setCodigoProductoCatalogo2(checkString(partenon, 18, 21));

        //same info
        this.setEmpresaContable(getEmpresaContrato());
        this.setCentroContable(getCentroContrato());
        //    }
    }

    public String checkString(String value, int init, int fin) {
        if (value.length() >= fin) {
            return Optional.ofNullable(value.substring(init, fin)).orElse(emptyString(fin - init));
        } else {
            return emptyString(fin - init);
        }
    }

    public String emptyString(int lenght) {
        return StringUtils.leftPad("", lenght, " ");
    }

    public String loadBook() {
        return formatter.formatLeftString(this.trade.getBook().getName(), 15);
    }

    public String loadMonedaRelacionada() {
        if (null != trade && trade.getProduct() instanceof PerformanceSwap) {
            PerformanceSwap brs = (PerformanceSwap) trade.getProduct();
            return formatter.formatLeftString(brs.getPrimaryLegUnderlyingCurrency(), 3);
        }
        return formatter.formatLeftString("", 3);
    }


    //GETTERS AND SETTERS

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public BalancePosition getBalance() {
        return balance;
    }

    public void setBalance(BalancePosition balance) {
        this.balance = balance;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public JDate getProcessDate() {
        return processDate;
    }

    public void setProcessDate(JDate processDate) {
        this.processDate = processDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PricingEnv getPricingEnv() {
        return pricingEnv;
    }

    public void setPricingEnv(PricingEnv pricingEnv) {
        this.pricingEnv = pricingEnv;
    }

    public String getFiller() {
        return filler;
    }

    public void setFiller() {
        this.filler = formatter.formatLeftString("", 50);
    }

    public String getEmpresaContrato() {
        return empresaContrato;
    }

    public void setEmpresaContrato(String empresaContrato) {
        this.empresaContrato = empresaContrato;
    }

    public String getCentroContrato() {
        return centroContrato;
    }

    public void setCentroContrato(String centroContrato) {
        this.centroContrato = centroContrato;
    }

    public String getCodigoProductoCatalogo1() {
        return codigoProductoCatalogo1;
    }

    public void setCodigoProductoCatalogo1(String codigoProductoCatalogo1) {
        this.codigoProductoCatalogo1 = codigoProductoCatalogo1;
    }

    public String getPartenonContract() {
        return partenonContract;
    }

    public void setPartenonContract(String partenonContract) {
        this.partenonContract = partenonContract;
    }

    public String getSubContract() {
        return subContract;
    }

    public void setSubContract(String subContract) {
        this.subContract = subContract;
    }

    public String getCodigoProductoCatalogo2() {
        return codigoProductoCatalogo2;
    }

    public void setCodigoProductoCatalogo2(String codigoProductoCatalogo) {
        this.codigoProductoCatalogo2 = codigoProductoCatalogo;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public String getMonedaContrato() {
        return monedaContrato;
    }

    public void setMonedaContrato(String monedaContrato) {
        this.monedaContrato = monedaContrato;
    }

    public String getMonedaRelacionada() {
        return monedaRelacionada;
    }

    public void setMonedaRelacionada(String monedaRelacionada) {
        this.monedaRelacionada = monedaRelacionada;
    }

    public String getPosicion() {
        return posicion;
    }

    public void setPosicion(String posicion) {
        this.posicion = posicion;
    }

    public String getSituacion() {
        return situacion;
    }

    public void setSituacion(String situacion) {
        this.situacion = situacion;
    }

    public String getMorosidad() {
        return morosidad;
    }

    public void setMorosidad(String morosidad) {
        this.morosidad = morosidad;
    }

    public String getImporteSaldo() {
        return importeSaldo;
    }

    public void setImporteSaldo(String importeSaldo) {
        this.importeSaldo = importeSaldo;
    }

    public String getImporteSaldoContValor() {
        return importeSaldoContValor;
    }

    public void setImporteSaldoContValor(String importeSaldoContValor) {
        this.importeSaldoContValor = importeSaldoContValor;
    }

    public String getEmpresaContable() {
        return empresaContable;
    }

    public void setEmpresaContable(String empresaContable) {
        this.empresaContable = empresaContable;
    }

    public String getCentroContable() {
        return centroContable;
    }

    public void setCentroContable(String centroContable) {
        this.centroContable = centroContable;
    }

    public String getMonedaContravalor() {
        return monedaContravalor;
    }

    public void setMonedaContravalor(String monedaContravalor) {
        this.monedaContravalor = monedaContravalor;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        final BalancesPostingFormatter instance = BalancesPostingFormatter.getInstance();

        builder.append(instance.formatLeftString(this.getEmpresaContrato(), 4));
        builder.append(instance.formatLeftString(this.getCentroContrato(), 4));
        builder.append(instance.formatLeftString(this.getCodigoProductoCatalogo1(), 3));
        builder.append(instance.formatLeftString(this.getPartenonContract(), 7));
        builder.append(instance.formatLeftString(this.getSubContract(), 7));
        builder.append(instance.formatLeftString(this.getCodigoProductoCatalogo2(), 3));
        builder.append(instance.formatLeftString(this.getBook(), 15));
        builder.append(instance.formatLeftString(this.getMonedaContrato(), 3));
        builder.append(instance.formatLeftString(this.getMonedaRelacionada(), 3));
        builder.append(instance.formatLeftString(this.getPosicion(), 3));
        builder.append(instance.formatLeftString(this.getSituacion(), 3));
        builder.append(instance.formatLeftString(this.getMorosidad(), 3));
        builder.append(this.getImporteSaldo());
        builder.append(this.getImporteSaldoContValor());
        builder.append(instance.formatLeftString(this.getMonedaContravalor(), 3));
        builder.append(instance.formatLeftString(this.getEmpresaContable(), 4));
        builder.append(instance.formatLeftString(this.getCentroContable(), 4));
        builder.append(instance.formatLeftString(this.getFiller(), 50));

        return builder.toString();
    }
}
