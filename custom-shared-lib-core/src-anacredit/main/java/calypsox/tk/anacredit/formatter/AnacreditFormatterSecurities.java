package calypsox.tk.anacredit.formatter;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditPersonaOperacionesItem;
import calypsox.tk.anacredit.util.ActivoOpValores;
import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.anacredit.util.AnacreditUtilities;
import calypsox.tk.report.BOSecurityPositionReportStyle;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class AnacreditFormatterSecurities extends AnacreditFormatter implements IPersonaFormatter, IOperacionesFormatter {

    private static final String TV012 = "TV012";
    private static final String TV013 = "TV013";
    private static final String S140 = "S140";
    private static final String ANACREDIT_JERARQUIA = "ANACREDIT_JERARQUIA";
    private static final String TRIPV = "TRIPV";
    private static final String TRIPC = "TRIPC";
    private static final String COLV = "COLV";
    private static final String COLC = "COLC";

    /**
     * Build AnaCredit Titulos Operaciones Trade Items
     * @param contract
     * @param reportRow
     * @param valDate
     * @param pricingEnv
     * @param errors
     * @return
     */
    public List<AnacreditOperacionesItem> format(CollateralConfig contract, ReportRow reportRow, JDate valDate, PricingEnv pricingEnv, Vector<String> errors) {
        ArrayList<AnacreditOperacionesItem> result = new ArrayList<>();
        InventorySecurityPosition position  = reportRow.getProperty("Default");
        if (null == position)  {
            return result;
        }

        AnacreditOperacionesItem item = new AnacreditOperacionesItem();

        String currency = position.getSettleCurrency();
        Product product = position.getProduct();

        BigDecimal nominalDivisa = getNominalDivisa(reportRow);
        BigDecimal saldo_ctg_divisa  = getMarketValue(reportRow);

        if ( (nominalDivisa == null || Math.abs(nominalDivisa.doubleValue()) < 1.00d)
                || (saldo_ctg_divisa == null || Math.abs(saldo_ctg_divisa.doubleValue()) < 1.00d)) {
            log(LogLevel.ERR, contract, " - VALOR NOMINAL / SALDO CONTINGENTE   - Invalid low values for this item : " + product.getSecCode("ISIN"), errors );
            return result;
        }

        Double nominalEUR = null;//convertToEUR(nominalDivisa.doubleValue(), currency, valDate, null); // se fuerza a sacar de OFFICIAL
        DisplayValue fxRate = BOSecurityPositionReportStyle.getInformesInternosFXRate(reportRow, position);
        if (fxRate == null) {
            nominalEUR = AnacreditUtilities.convertToEUR(nominalDivisa.doubleValue(), currency, valDate, null); // se fuerza a sacar de OFFICIAL
        } else {
            nominalEUR = nominalDivisa.doubleValue() * fxRate.get();
        }
        Double saldo_ctg_eur =  BOSecurityPositionReportStyle.getInformesInternosMarketValueEURNoFixing(reportRow);
        if (null == saldo_ctg_eur) {
            saldo_ctg_eur = 0.0d;
        }

        if ( (nominalEUR == null || Math.abs(nominalEUR.doubleValue()) < 1.00d)
                || (saldo_ctg_eur == null || Math.abs(saldo_ctg_eur) < 1.00d)) {
            log(LogLevel.ERR, contract, " - VALOR NOMINAL / SALDO CONTINGENTE (EUR)  - Invalid low values for this item : " + product.getSecCode("ISIN"), errors );
            return result;
        }

        LegalEntity productIssuer = getProductIssuer(position.getProduct());
        if (productIssuer == null) {
            log(LogLevel.ERR, contract, "  - LE not found for Security ISSUER of : " + product.getSecCode("ISIN"), errors );
            return result;
        }

        String j_minoristaIssuer = _mapper.getJMin(productIssuer);
        if(Util.isEmpty(j_minoristaIssuer)){
            log(LogLevel.ERR, contract, " - J MIN not found for Issuer LegalEntity: " + productIssuer.getCode(), errors );
            return result;
        }

        String date =  formatDate(valDate,8);
        String datePlusOne =  formatDate(valDate.addBusinessDays(1, Util.string2Vector(HOLIDAYS)),8);
        String isin = product.getSecCode(ISIN);
        LegalEntity legalEntity = contract.getLegalEntity();

        // Common fields Operations
        initDefaultOperacionesCopy3(item);
        removeFiller(item);
        item.setFecha_extraccion(date);
        String idContrato    = getIDContrato(contract, position.getBookId(), isin);

        String idContratoInt = getIDContratoInterno(contract, position.getSettleCurrency(), _mapper.isSell(position), isin, date);

        item.setId_contrato( formatStringWithBlankOnRight(idContrato, 50));
        item.setId_contrato_interno(formatStringWithBlankOnRight(idContratoInt, 50));
        item.setCodigo_valor(formatStringWithBlankOnRight(isin,12));

        item.setPais_negocio(formatStringWithBlankOnRight(_mapper.getPaisNegocio(legalEntity) ,4));

        String productoAc = _mapper.isSell(position) ? TV012 : TV013;
        item.setProducto_ac( formatStringWithBlankOnRight(productoAc ,5));

        String productoEntidad = getProductoEntidad(contract, _mapper.isSell(position));
        item.setProducto_entidad(formatStringWithBlankOnRight(productoEntidad,5));

        String opValores = ActivoOpValores.instance().get(product);
        item.setActivo_op_valores(formatStringWithBlankOnRight(opValores,5));
        item.setFecha_emision_originacion(date);
        item.setFecha_vencimiento(datePlusOne);
        item.setFecha_cancelacion(datePlusOne);
        item.setOrigen_operacion(formatStringWithBlankOnRight("" ,4));

        item.setCanal_contratacion_ac(formatStringWithBlankOnRight("",3));


        // fija en 90 para garantias titulos
        item.setProvincia_negocio(formatStringWithBlankOnRight("90",2));

        item.setEsquema_amort_operaciones_ac("13");

        item.setPorcent_particip_sindicados(formatUnsignedNumber(0,8,5,""));
        item.setDesc_promoc_inmob_finan(formatStringWithBlankOnRight("",50));
        item.setMoneda(formatStringWithBlankOnRight(_mapper.getCurrencyMap(currency, errors, item.getId_contrato()),3));
        item.setPlazo_residual("P2");
        item.setTipo_garantia_real_ppal("999");

        item.setFecha_primer_incumplimiento("99991231");
        item.setFecha_baja_def_cirbe(AnacreditConstants.STR_MAX_DATE_99991231);

        item.setFecha_ultimo_impago_incumplim("99991231");
        item.setSaldo_deudor_no_vencido(0);
        item.setFecha_ultimo_vto_intereses("99991231");
        item.setFecha_proximo_vto_intereses("99991231");
        item.setFecha_ultimo_vto_prinicipal("99991231");
        item.setFecha_proximo_vto_principal("99991231");
        item.setModalidad_tipo_interes_ac(formatStringWithBlankOnRight(M16,3));
        item.setTedr(0.0);
        item.setIndice_referencia_ac(formatStringWithBlankOnRight(I_99,3));
        item.setSuelo_rentabilidad(formatUnsignedNumber(0,8,5,""));
        item.setTecho_rentabilidad(formatUnsignedNumber(0,8,5,""));
        item.setDiferencial_sobre_indice_refe("+9090900");
        String j_minorista = _mapper.getJMin(legalEntity);
        if(Util.isEmpty(j_minorista)){
            log(LogLevel.ERR, contract, " - J MIN not found for LegalEntity: " + legalEntity.getCode(), errors );
            return result;
        }

        item.setId_persona_contraparte_direct(j_minorista);
        item.setSituacion_ope_riesgo_direc_ac(formatStringWithBlankOnRight(S140,4));
        item.setIntereses_devengados(0.0);


        String jerarquia = position.getProduct().getSecCode(ANACREDIT_JERARQUIA);
        if (Util.isEmpty(jerarquia)){
            log(LogLevel.ERR, contract, " - ANACREDIT JERARQUIA Not found for instrument : " + isin, errors );
        }

        String cotiza = _mapper.getCotiza(jerarquia);
        item.setCotiza(formatStringWithBlankOnRight(cotiza,3));
        item.setEntidad_depositaria("25");
        item.setPasivos_valor_razonable("+"+formatUnsignedNumber(0,numChar,0,""));

        item.setOperacion_no_declarable_cirbe("1");
        item.setFecha_renovacion("99991231");
        item.setTipo_codigo_valor(formatStringWithBlankOnRight("01",2));

        item.setFecha_primer_incumpl_op_refin("99991231");
        item.setProducto_subproducto(formatStringWithBlankOnRight(item.getProducto_entidad(),6));

        String  tipo_cartera_irfs9 = _mapper.getTipoCartera(BOCache.getBook(DSConnection.getDefault(), position.getBookId()));
        item.setTipo_cartera_ifrs9(tipo_cartera_irfs9);


        item.setJerarquia_valor_razonable(formatStringWithBlankOnRight(_mapper.getJerarquiaValorRazonable(jerarquia),1));

        if(_mapper.validAgent(legalEntity)){
            item.setTipo_cartera_ifrs9("99");
            item.setReconocimiento_balance("3");
        }

        String cartera_prudencial = AnacreditMapper.getCarteraPrudencial(tipo_cartera_irfs9);
        item.setCartera_prudencial(formatStringWithBlankOnRight(cartera_prudencial, 3));
        item.setFecha_primera_liquidacion("11111112");
        item.setFinanciacion_proyecto(formatStringWithBlankOnRight("0",2));

        item.setFecha_estado_cumplimiento(date);
        item.setFecha_refin_reest("99991231");
        item.setTipo_referencia_vencimiento(formatStringWithBlankOnRight("0",3));
        item.setCodigo_ratio_referencia(formatStringWithBlankOnRight("",3));
        item.setTipo_referencia_sustitutivo("11");
        item.setFrec_revision_tipo_int_per(formatStringWithBlankOnRight("0",2));
        item.setProxima_revision_tipo_interes("11111112"); //TODO validation
        item.setFrecuencia_pago_princ_int(formatStringWithBlankOnRight("0",2));

        item.setIntereses_demora_fallidos("+"+formatUnsignedNumber(0,numChar,0,"")); //TODO
        item.setGastos_exigibles_fallidos("+"+formatUnsignedNumber(0,numChar,0,""));

        item.setFase_deterioro(formatStringWithBlankOnRight("0",2));
        item.setImporte_libros_activo("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCobertura_acumulada("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setMet_calc_cobert_riesgo_normal(formatStringWithBlankOnRight("0",2));
        item.setMet_calc_cobert_riesgo_dudoso(formatStringWithBlankOnRight("0",2));
        item.setFecha_baja_def_cirbe(AnacreditConstants.STR_MAX_DATE_99991231);
        item.setSituacion_operativa(formatStringWithBlankOnRight("S8",3));
        item.setEstado_cumplimiento(formatStringWithBlankOnRight("9",3));
        item.setContrato_sindicado(formatStringWithBlankOnRight("",60));

        //Fechas
        initDefaultDates(item,date,datePlusOne);


        // Importes
        item.setValor_nominal("-"+formatUnsignedNumber(nominalEUR, 15,2,""));
        item.setValor_nominal_d(nominalDivisa.doubleValue());
        item.setSaldo_contingente(saldo_ctg_eur.doubleValue()); // only to format in file COPY3
        item.setSaldo_contingente_eur(saldo_ctg_eur);
        item.setSaldo_contingente_d(saldo_ctg_divisa.doubleValue());

        //V2.19/2.20
        item.setReduccion_principal_importe_avalista("+"+formatUnsignedNumber(0,15,2,""));
        item.setReduccion_principal_avales_ejecutado("+"+formatUnsignedNumber(0,15,2,""));
        item.setReduc_principal_importe_avalista(0.0);
        item.setReduc_principal_avales_ejecutado(0.0);

        item.setEntidad_grupo_origen(AnacreditFormatterCash.formatStringWithBlankOnRight("",15));
        item.setCuenta_grupo(AnacreditFormatterCash.formatStringWithBlankOnRight("",20));
        item.setSubcuenta(AnacreditFormatterCash.formatStringWithBlankOnRight("",10));
        item.setId_centro_contable_a(AnacreditFormatterCash.formatStringWithBlankOnRight("",10));

        result.add(item);
        return result;
    }


    private BigDecimal getMarketValue(ReportRow reportRow) {
        Double mValue = BOSecurityPositionReportStyle.getInformesInternosMarketValue(reportRow);
        if (mValue != null) {
            BigDecimal result = new BigDecimal(mValue).setScale(2, BigDecimal.ROUND_HALF_EVEN);
            return result;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getNominalDivisa(ReportRow reportRow) {
        Double nominal = BOSecurityPositionReportStyle.getInformesInternosNominal(reportRow);
        if (nominal != null) {
            BigDecimal result = new BigDecimal(nominal).setScale(2, BigDecimal.ROUND_HALF_EVEN);
            return result;
        }
        return BigDecimal.ZERO;
    }

    private String getIDContratoInterno(CollateralConfig contract, String ccy, boolean isSell, String isin, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append(contract.isTriParty() ? "T" : "C");
        sb.append("_");
        sb.append(isin);
        sb.append("_");
        sb.append(ccy);
        sb.append("_");
        sb.append( isSell ? "V" : "C");
        sb.append("_");
        sb.append(date);
        return sb.toString();
    }

    private String getIDContrato(CollateralConfig contract, int bookId , String isin) {

        Book book = BOCache.getBook(DSConnection.getDefault(), bookId);

        StringBuilder sb = new StringBuilder();
        sb.append(contract.getId());
        sb.append("_");
        sb.append(contract.getLegalEntity().getCode());
        sb.append("_");
        sb.append(isin);
        sb.append("_");
        sb.append(book != null ? book.getName(): "");

        return sb.toString();
    }


    protected void initDefaultOperacionesCopy3(AnacreditOperacionesItem item){
        super.initDefaultOperacionesCopy3(item);
        item.setSubordinacion_producto_ac("S4");
        item.setFinalidad_ac(formatStringWithBlankOnRight("F59",4));
        item.setPrincipal_inicial("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setLimite_inicial("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setImporte_concedido("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCanal_contratacion_ac(formatStringWithBlankOnRight("",3));
        item.setInmueble_financ_num_viviendas(formatUnsignedNumber(0,13,0,""));
        item.setCodigo_promoc_inmob_finan(formatStringWithBlankOnRight("0",20));

    }

    protected String getProductoEntidad(CollateralConfig contract, boolean isSell) {
        if (contract.isTriParty()) {
            return isSell ? TRIPV : TRIPC;
        }
        return isSell ? COLV : COLC;
    }

    /**
     * COPY 4
     * @param valDate
     */
    @Override
    public List<AnacreditPersonaOperacionesItem> formatPersonaItem(AnacreditOperacionesItem item, ReportRow reportRow, JDate valDate, Vector<String> errors) {
        ArrayList<AnacreditPersonaOperacionesItem> result = new ArrayList<>();
        if (item == null) {
            return result;
        }

        InventorySecurityPosition position  =  reportRow.getProperty("Default");
        CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), position.getMarginCallConfigId());
        if (null == config)   {
            return result;
        }

        LegalEntity legalEntity = config.getLegalEntity();
        LegalEntity issuer = getProductIssuer(position.getProduct());

        // Entry for Contrapartida
        AnacreditPersonaOperacionesItem itemCpty = getPersonaDefaults(item, valDate);
        String naturaleza_intervencion = _mapper.isSell(position) ? "10" : "01";
        itemCpty.setNaturaleza_intervencion(formatStringWithBlankOnRight(naturaleza_intervencion,2));
        String jMinAttr = _mapper.getJMin(legalEntity);
        itemCpty.setId_persona(formatStringWithBlankOnRight(jMinAttr,30));
        itemCpty.setProvincia_negocio(item.getProvincia_negocio());

        // Entry for Emissor
        AnacreditPersonaOperacionesItem itemEmissor = getPersonaDefaults(item, valDate);
        itemEmissor.setNaturaleza_intervencion(formatStringWithBlankOnRight("56",2));
        String jMinIssuer  = _mapper.getJMin(issuer);
        itemEmissor.setId_persona(formatStringWithBlankOnRight(jMinIssuer,30));
        itemEmissor.setProvincia_negocio(item.getProvincia_negocio());

        result.add(itemCpty);
        result.add(itemEmissor);
        return result;
    }

    protected LegalEntity getProductIssuer(Product product) {
        if (product != null
                && product instanceof Security) {
            Security security = (Security) product;
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), security.getIssuerId());
            if (le != null)  {
                return le;
            }
        }
        return null;
    }

}
