package calypsox.tk.anacredit.formatter;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.Copy3Columns;
import calypsox.tk.anacredit.api.copys.Copy3Record;
import calypsox.tk.anacredit.api.copys.Copy4Columns;
import calypsox.tk.anacredit.api.copys.Copy4Record;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditPersonaOperacionesItem;
import calypsox.tk.anacredit.processor.AnacreditProcessor;
import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.util.SantReportingUtil;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AnacreditFormatter {

    public static final String I_99 = "I99";
    public static final String I_29 = "I29";
    protected static int numChar = 15;
    protected static final String EUR = "EUR";
    protected static final String HOLIDAYS = "SYSTEM";
    public static final String M12 = "M12";
    public static final String M16 = "M16";
    public static final String M14 = "M14";
    public static final String M9 = "M9";
    public static final String FLOAT = "FLOAT";
    public static final String FIXED = "FIXED";
    public static final String EURIBOR = "EURIBOR";
    public static final String LIBOR = "LIBOR";
    public static final String MIBOR = "MIBOR";
    public static final String ISIN = "ISIN";

    public static final List PRODUCTOS_DERECHO_REEMBOLSO = Arrays.asList("AP011", "AV010", "AV013", "AV034", "AV011", "AV012",
            "AV031", "AV032", "AV033", "AV030", "PV010", "PV011", "PV030", "PV050", "PV070");

    public static final String AP021 = "AP021";
    public static final String CP010 = "CP010";

    public static final String LOG_ERROR = "(ERROR )";

    public enum LogLevel {
        WARN("Warning"),
        ERR("ERROR");
        private String level;
        LogLevel(String s) {
            this.level = s;
        }
        public String toString(){
            return level;
        }
        public String getLevel(){
            return level;
        }
    }

    protected AnacreditMapper _mapper = new AnacreditMapper();

    private RemoteSantReportingService _reportingService = null;




    public static String formatBlank(String value, int length){
        final StringBuilder str = new StringBuilder();

        for (int i = 0; i < length; ++i) {
            str.append(' ');
        }
        return str.toString();
    }
    public static String formatStringDateNull() {
        return formatBlank("",8 );
    }

    public static String formatStringWithBlankOnRight(final String value,
                                                      final int length) {
        if(!Util.isEmpty(value)){
            final String pattern = "%-" + length + "." + length + "s";
            return String.format(pattern, value).substring(0, length);
        }
        return formatBlank("",length );
    }

    public static String formatStringWithBlankOnLeft(final String value,
                                                     final int length) {
        if(!Util.isEmpty(value)){
            final String pattern = "%" + length + "." + length + "s";
            return String.format(pattern, value).substring(0, length);
        }
        return formatBlank("",length );
    }


    protected Double forceSignal(Double value, int signal ) {
        if (signal<0) {
            return forceMinus(value);
        }
        return forcePlus(value);
    }

    protected Double opositeSignal(Double value, double signal ) {
        if (signal<0) {
            return forcePlus(value);
        }
        return forceMinus(value);
    }


    protected Double forceMinus(Double value ) {
        // just for representative conformity to host
        Double result = -0.00;
        if (value != null && !value.isNaN()) {
            result = Math.abs(value)*-1;
        }
        return result;
    }

    protected Double forcePlus(Double value ) {
        // just for representative conformity to host
        Double result = -0.00;
        if (value != null && !value.isNaN()) {
            result = Math.abs(value);
        }
        return result;
    }


    public static String formatUnsignedNumber(final double value,int length,
                                              final int decimals, String separator) {
        if (decimals != 0) {
            if(Util.isEmpty(separator)){
                length = length + 1;
            }
            final String pattern = "%0" + (length) + "." + decimals + "f";
            final DecimalFormatSymbols symbols = new DecimalFormatSymbols();

            return String.format(pattern, Math.abs(value))
                    .replace(symbols.getDecimalSeparator() + "", separator);
        }

        final String pattern = "%0" + (length) + "." + decimals + "f";
        return String.format(pattern, Math.abs(value));
    }

    public static String formatDate(JDate date, int length){
        if(null!=date){
            JDateFormat format = new JDateFormat("yyyyMMdd");
            return format.format(date);
        }
        return formatBlank("", length);
    }

    public static java.util.Date parseDate(String date){
        if (!Util.isEmpty(date)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            try {
                return sdf.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String formatDate(Date date, int length){
        if(null!=date){
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            return format.format(date);
        }
        return formatBlank("", length);
    }

    /**
     FECHAS
     */
    protected void initDefaultDates(AnacreditOperacionesItem item, String date, String datePlusOne){
        item.setFecha_extraccion(date);
        //Contrato
        item.setFecha_baja_def_cirbe(AnacreditConstants.STR_MAX_DATE_99991231);
        item.setFecha_emision_originacion(date);
        item.setFecha_vencimiento(datePlusOne);
        item.setFecha_cancelacion(datePlusOne);
    }

    /**
     * FILLER
     *
     * @param item
     */
    protected void removeFiller(AnacreditOperacionesItem item){
        item.setFecha_formalizacion(formatStringWithBlankOnRight("",8));
        item.setSindicato(formatStringWithBlankOnRight("",1));
        item.setSituacion_operativa1(formatStringWithBlankOnRight("",2));
        item.setTipo_interes_nominal(formatStringWithBlankOnRight("",8));
        item.setTipo_interes_subvencionado(formatStringWithBlankOnRight("",8));
        item.setTipo_interes_demora(formatStringWithBlankOnRight("",8));
        item.setCom_comp_costes_concesion(formatStringWithBlankOnRight("",16));
        item.setClase_riesgo_contraparte_directa(formatStringWithBlankOnRight("",3));
        item.setRiesgo_exc_cob_riesgo_pais(formatStringWithBlankOnRight("",2));
        item.setTipo_cartera(formatStringWithBlankOnRight("",2));
        item.setInd_op_no_corriente(formatStringWithBlankOnRight("",1));
        item.setVinculacion_mercado_hipotecario(formatStringWithBlankOnRight("",2));
        item.setValor_corregido_garantia(formatStringWithBlankOnRight("",16));
        item.setDisponible_subrogacion_terceros(formatStringWithBlankOnRight("",16));
        item.setLtv(formatStringWithBlankOnRight("",5));
        item.setDividendos_cobrados_mes(formatStringWithBlankOnRight("",16));
        item.setTipo_derivado(formatStringWithBlankOnRight("",2));
        item.setDerivados_tipo_mercado(formatStringWithBlankOnRight("",1));
        item.setImporte_pendiente_desembolso(formatStringWithBlankOnRight("",16));
        item.setIntereses_devengados_mes(formatStringWithBlankOnRight("",16));
        item.setEstado_cumplimiento1(formatStringWithBlankOnRight("",1));
        item.setEstado_refin_reest(formatStringWithBlankOnRight("",1));
        item.setCompra_o_venta(formatStringWithBlankOnRight("",1));
        item.setPasivos_valor_razonable_riesgo_credito(formatStringWithBlankOnRight("",16));
        item.setId_persona_agente_observado(formatStringWithBlankOnRight("",50));
        item.setId_persona_contraparte_solvencia(formatStringWithBlankOnRight("",50));
        item.setContrato_sindicado1(formatStringWithBlankOnRight("",50));
        item.setOperacion_adquirida(formatStringWithBlankOnRight("",2));
        item.setOp_no_orig_gest_terc(formatStringWithBlankOnRight("",2));
        item.setTipo_correccion_valor_deterioro(formatStringWithBlankOnRight("",2));
        item.setFecha_situacion_operacion_rd(formatStringWithBlankOnRight("",8));
        item.setFecha_clasificacion_dudosa(formatStringWithBlankOnRight("",8));
        item.setRc_factor_conversion_orden(formatStringWithBlankOnRight("",5));
    }

    /**
     * Generic/static  fields in common
     * @param item
     */
    protected void initDefaultOperacionesCopy3(AnacreditOperacionesItem item){

        item.setImportes_vencidos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProductos_fallidos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setGastos_exigibles_fallidos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setImporte_libros_activo("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCobertura_acumulada("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setFallidos_acumulados("+"+formatUnsignedNumber(0,numChar,0,"")); //TODO
        item.setIntereses_demora_fallidos("+"+formatUnsignedNumber(0,numChar,0,"")); //TODO
        item.setPlazo_origen_m(formatStringWithBlankOnRight("",2));

        item.setId_entidad("0049");
        item.setAplicacion_origen("A003");
        item.setId_centro_contable(formatUnsignedNumber(1999,6,0,""));
        item.setDeclarado_cir_tercera_entidad("0");
        item.setPais_negocio("0724");
        item.setActivo_op_valores(formatStringWithBlankOnRight("",5));
        item.setTipo_riesgo_subyacente("00");
        item.setTramite_recuperacion("T4");
        item.setRefinanciacion("00");
        item.setSubvencion_operacion("ZZZ");
        item.setEdif_financ_estado("E4");
        item.setEdif_financ_licencia("L10");
        item.setTipo_garantia_real_ppal("999");
        item.setOrden_hipoteca("0");
        item.setCobertura_garantia_real("C3");
        item.setTipo_garant_personal_ppal_ac("01");
        item.setCobertura_garantia_personal("C3");
        item.setFecha_primer_incumplimiento("99991231");
        item.setFecha_ultimo_impago_incumplim("99991231");
        item.setSaldo_deudor_vencido("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProductos_vencidos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProductos_dudosos_no_consolid("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setIntereses_demora_consolidados("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setIntereses_demora_no_consolid("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setGastos_activados("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setResto_valor_contable_activos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_acreedor("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setLimite("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setDisponible_inmediato("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setDisponible_condicionado("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setPpal_cobrado_mes_amort_corrnt("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setPpal_cobrado_mes_amort_antici("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setVar_deuda_cond_presc("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setTipo_act_recib_pago_mes("T1");
        item.setReduc_deuda_act_recib_pag_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setTipo_subrogacion_mes("O99");
        item.setReduc_deuda_subrogacion_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setTipo_refinanc_mes("R1");
        item.setReduc_deuda_refinanc_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setGestion_op_transferida_mes(formatStringWithBlankOnRight("",1));
        item.setImporte_transferido_mes_ci("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setFecha_ultimo_vto_prinicipal("99991231");
        item.setNum_cuotas_impagadas_ppal(formatUnsignedNumber(0,13,0,""));
        item.setTecho_rentabilidad(formatUnsignedNumber(0,8,0,""));
        item.setClasificacion_oper_b3(formatUnsignedNumber(0,5,0,""));
        item.setCobertura_riesgo_dudoso("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCobertura_riesgo_pais("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProvision_riesgo_dudoso("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProvision_riesgo_pais("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setComisiones_pendientes_devengo("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCoste_transaccion("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setPrima_descnto_adquis_asuncion("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setDescuento_por_deterioro("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setActivos_valor_razobl("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setActivos_valor_razobl_rsgo_cre("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setMicro_cobertura("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setValor_residual_no_garantizado("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setValor_residual_comp_terceros("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setValor_razonable("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaneamientos_directos_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setIntereses_cobrados_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setNumero_ocurrencias(formatUnsignedNumber(0,20,0,""));
        item.setPorcentaje_partici_capital(formatUnsignedNumber(0,8,0,""));
        item.setImporte_liq_compras_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setImporte_liq_ventas_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCobert_perdidas_partcip_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setNocional("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCorrecciones_valor_valores("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setOperacion_no_declarable_cirbe("0");
        item.setFecha_renovacion("99991231");
        item.setDividendos_devengados_mes("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_contingente("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setPrincipal_vencido_mes_subvenc("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setTipo_tarjeta(formatStringWithBlankOnRight("",1));
        item.setSaldo_deudor_excedido_vencido("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setContrato_renegociado("0");
        item.setPrincipal_vdo_refin_pte_amort("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setFecha_primer_incumpl_op_refin("99991231");
        item.setConocimiento_garant_pers_ppal("1");
        item.setSaldo_deudor_dudoso_no_vencid("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_deudor_dudoso_vencido("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_deudor_dudoso_exced_ven("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProductos_vencidos_dudosos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setIntereses_demora_consol_dudos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setResto_valor_cont_actvos_dudos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCobertura_riesgo_normal("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setIntereses_devengados_dudosos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setIntereses_exced_devengado("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setIntereses_exced_devengado_dud("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_contingente_dudoso("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_contingente_vencido("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_contingente_dudoso_venc("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setEstado_refin_reest_reneg(formatStringWithBlankOnRight("5",2));
        item.setId_contrato_juridico(formatStringWithBlankOnRight("",50));
        item.setProducto_recurso("1");
        item.setInstrumento_fiduciario("2");
        item.setImport_recuperad_acum_sit_imp("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setFecha_refin_reest("99991231");
        item.setFecha_final_carencia_principal(formatStringWithBlankOnRight("11111112",8));
        item.setSaldo_vivo_nominal("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_deudor_fallido_no_venc("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_deudor_fallido_vencido("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProvision_fuera_balance("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setAct_val_raz_ries_cred_ant_adq("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_fuera_balance("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setTipo_fuente_de_carga(formatStringWithBlankOnRight("1",2));
        item.setFecha_renegociacion("99991231");
        item.setSituacion_impago_operacion(formatStringWithBlankOnRight("0",2));
        item.setFecha_situacion_impago_ope("11111112");
        item.setReconocimiento_balance("1");
        item.setSaldo_deudor_excedido_no_venc("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSaldo_deudor_dud_exce_no_venc("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setMet_calc_cobert_riesgo_dudoso(formatStringWithBlankOnRight("0",2));
        item.setRenovacion_automatica("0");
        item.setValor_residual_no_garan_dudos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setValor_residual_comp_ter_dudos("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProductos_dudosos_deven_orden("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setValor_actual_com_garan_conced("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setProvision_riesgo_normal("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setTotal_intereses_deven_credito("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setOtras_periodificaciones_activ("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setSituacion_operativa(formatStringWithBlankOnRight("S8",3));
        item.setContrato_sindicado(formatStringWithBlankOnRight("",60));
        item.setId_persona_contraparte_directa_cir(formatStringWithBlankOnRight("",50));
        item.setTedr_venc("+"+formatUnsignedNumber(0,7,0,""));
        item.setTedr_desc_exc("+"+formatUnsignedNumber(0,7,0,""));

        item.setOperacion_titulares_exonerados(formatStringWithBlankOnRight("0",1));
        item.setFecha_primer_incumplimiento_sin_fall_parc("99991231");
        item.setClassif_oper_anejo_ix(formatStringWithBlankOnRight("",3));
        item.setFecha_refinanciacion("99991231");
        item.setFallidos_parciales(formatStringWithBlankOnRight(" ",16));
        item.setEntidad_grupo_origen(formatStringWithBlankOnRight(" ",15));
        item.setCuenta_grupo(formatStringWithBlankOnRight("",20));
        item.setSubcuenta(formatStringWithBlankOnRight("",10));
        item.setId_centro_contable_a(formatStringWithBlankOnRight("",10));
        item.setMoratoria_covid19(formatStringWithBlankOnRight("",02));
        item.setFecha_inicio_moratoria_covid19("11111112");
        item.setFecha_fin_moratoria_covid19("11111112");

        item.setFecha_modificacion(formatStringWithBlankOnRight("",8));
        item.setModificacion(formatStringWithBlankOnRight("",3));
        item.setAumento_plazo(formatStringWithBlankOnRight("",1));
        item.setAumento_plazo_fecha(formatStringWithBlankOnRight("",8));
        item.setPrestamos_participativos(formatStringWithBlankOnRight("",1));
        item.setPrestamos_participativos_fecha(formatStringWithBlankOnRight("",8));
        item.setReduccion_principal(formatStringWithBlankOnRight("",1));
        item.setReduccion_principal_fecha(formatStringWithBlankOnRight("",8));

        item.setOriginado_con_deterioro(formatStringWithBlankOnRight("",1));
        item.setCalculo_situacion_impago("1");
        item.setFiller(formatStringWithBlankOnRight("",590));
        item.setVersion("V0223");
    }

    protected AnacreditPersonaOperacionesItem getPersonaDefaults(AnacreditOperacionesItem item, JDate valDate) {
        String date = formatDate(valDate,8);
        AnacreditPersonaOperacionesItem operPerItem;
        operPerItem = new AnacreditPersonaOperacionesItem();
        operPerItem.setId_entidad("0049");
        operPerItem.setFecha_alta_relacion(formatDate(valDate,8));
        operPerItem.setFecha_extraccion(date);
        operPerItem.setFecha_baja_relacion(item.getFecha_vencimiento());

        // Primer dia del proximo mes seguiente a la fecha vencimiento
        Date posDate = parseDate(item.getFecha_vencimiento());
        if (posDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(posDate);
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
            operPerItem.setFecha_baja_relacion( formatDate(cal.getTime(),8));
        }

        operPerItem.setId_contrato(item.getId_contrato());
        operPerItem.setId_contrato_interno(item.getId_contrato_interno());//set id and id interno
        operPerItem.setId_persona(formatStringWithBlankOnRight(item.getId_persona_contraparte_direct(),30));
        operPerItem.setNaturaleza_intervencion(formatStringWithBlankOnRight("10",2));
        operPerItem.setGrupo_titulares_mancomunados(formatUnsignedNumber(0,13,0,""));
        operPerItem.setPorcentaje_principal_subvencionado(formatStringWithBlankOnRight("",8));
        operPerItem.setPorcentaje_intereses_subvencionados(formatStringWithBlankOnRight("",8));
        operPerItem.setPorcentaje_participacion(formatUnsignedNumber(100,8,5,""));
        operPerItem.setRelacion_no_declarable_cirbe(formatStringWithBlankOnRight("",1));
        operPerItem.setImporte_maximo_responsabilidad_conjunta("+"+formatUnsignedNumber(0,15,2,""));
        operPerItem.setImporte_responsabilidad_conjunta("+"+formatUnsignedNumber(0,15,2,""));
        operPerItem.setGrado_relevancia_garante(formatUnsignedNumber(0,13,2,""));
        operPerItem.setProvincia_negocio(item.getProvincia_negocio());
        operPerItem.setTitular_no_convenio_acreedores(formatStringWithBlankOnRight(" ",1));
        operPerItem.setTratamiento_especial_cirbe(formatStringWithBlankOnRight(" ",1));
        operPerItem.setFiller1(formatStringWithBlankOnRight("",105));
        return operPerItem;
    }


    protected void log(LogLevel level, CollateralConfig config, String message, Vector<String> errors ) {
        AnacreditProcessor.createMessage(level, String.valueOf(config.getLongId()), message, errors);
    }

    protected void log(LogLevel level, String identifier, String message, Vector<String> errors ) {
        AnacreditProcessor.createMessage(level, identifier, message, errors);
    }
    protected void log(LogLevel level, Long identifier, String message, Vector<String> errors ) {
        AnacreditProcessor.createMessage(level, String.valueOf(identifier), message, errors);
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

    protected String getPlazo(JDate valDate, JDate repoEndDate) {
        if (null != valDate && null != repoEndDate) {
            Integer daysBetween = calculateDiff(valDate, repoEndDate);
            return _mapper.getPlazoResidual(daysBetween);
        }
        return "";
    }

    private Integer calculateDiff(JDate valDate, JDate repoEndDate) {
        if (null != valDate && null != repoEndDate) {
            long difference = valDate.getDate().getTime() - repoEndDate.getDate().getTime();
            return Integer.valueOf((int) Math.abs(difference / (1000*60*60*24)));
        }
        return 0;
    }



    protected Copy4Record buildCopy4Record(Copy3Columns column, String tipoImporte, String ccy, Copy3Record copy3Record)  {
        Copy4Record copy4Record = new Copy4Record();
        copy4Record.initializeFromCopy3(copy3Record);
        copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, tipoImporte);
        Double valueDivisa = (Double) copy3Record.retrieve(column.name());
        if (Double.compare(Math.abs(valueDivisa), 0.00D) == 0) {
            return null;
        }
        copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, valueDivisa);
        if ("EUR".equals(ccy)) {
            copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, valueDivisa);
        } else {
            Double valueEur = (Double) copy3Record.retrieve(column.name()+"_EUR");
            copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, valueEur);
        }
        return copy4Record;
    }

    protected RemoteSantReportingService getReportingService() {
        if (_reportingService == null) {
            _reportingService = SantReportingUtil.getSantReportingService(DSConnection.getDefault());
        }
        return _reportingService;

    }



}
