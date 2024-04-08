package calypsox.tk.anacredit.items;

import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.anacredit.util.AnacreditUtilities;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.CollateralConfig;

public class AnacreditOperacionesItem {

    public AnacreditOperacionesItem() {
    }

    String type = "";
    String id_entidad= "";
    String fecha_extraccion= "";
    String id_contrato_interno= "";
    String declarado_cir_tercera_entidad= "";
    String producto_ac= "";
    String producto_entidad= "";
    String subordinacion_producto_ac= "";
    String tipo_riesgo_subyacente= "";
    String finalidad_ac= "";
    String tramite_recuperacion= "";
    String principal_inicial= "";
    String limite_inicial= "";
    String importe_concedido= "";
    String fecha_emision_originacion= "";
    String fecha_vencimiento= "";
    String origen_operacion= "";
    String refinanciacion= "";
    String subvencion_operacion= "";
    String canal_contratacion_ac= "";
    String provincia_negocio= "";
    String esquema_amort_operaciones_ac= "";
    String porcent_particip_sindicados= "";
    String edif_financ_estado= "";
    String edif_financ_licencia= "";
    String inmueble_financ_num_viviendas= "";
    String codigo_promoc_inmob_finan= "";
    String desc_promoc_inmob_finan= "";
    String plazo_residual= "";
    String tipo_garantia_real_ppal= "";
    String orden_hipoteca= "";
    String cobertura_garantia_real= "";
    String tipo_garant_personal_ppal_ac= "";
    String cobertura_garantia_personal= "";
    String situacion_operativa= "";
    String fecha_primer_incumplimiento= "";
    String fecha_ultimo_impago_incumplim= "";
    String productos_dudosos_no_consolid= "";
    String intereses_demora_no_consolid= "";
    String limite= "";
    String disponible_inmediato= "";
    String disponible_condicionado= "";
    String ppal_cobrado_mes_amort_corrnt= "";
    String ppal_cobrado_mes_amort_antici= "";
    String var_deuda_cond_presc= "";
    String tipo_act_recib_pago_mes= "";
    String reduc_deuda_act_recib_pag_mes= "";
    String tipo_subrogacion_mes= "";
    String reduc_deuda_subrogacion_mes= "";
    String tipo_refinanc_mes= "";
    String reduc_deuda_refinanc_mes= "";
    String gestion_op_transferida_mes= "";
    String importe_transferido_mes_ci= "";
    String fecha_ultimo_vto_intereses= "";
    String fecha_proximo_vto_intereses= "";
    String fecha_ultimo_vto_prinicipal= "";
    String fecha_proximo_vto_principal= "";
    String num_cuotas_impagadas_ppal= "";
    String modalidad_tipo_interes_ac= "";
    String tedr= "";
    String indice_referencia_ac= "";
    String suelo_rentabilidad= "";
    String techo_rentabilidad= "";
    String diferencial_sobre_indice_refe= "";
    String situacion_ope_riesgo_direc_ac= "";
    String clasificacion_oper_b3 = "";
    String cobertura_riesgo_dudoso= "";
    String cobertura_riesgo_pais= "";
    String provision_riesgo_dudoso= "";
    String provision_riesgo_pais= "";
    String comisiones_pendientes_devengo= "";
    String coste_transaccion= "";
    String descuento_por_deterioro= "";
    String activos_valor_razobl_rsgo_cre= "";
    String valor_nominal= "";
    String nocional= "";
    String importe_pendiente_desembolso= "";
    String plazo_origen_m= "";
    String operacion_no_declarable_cirbe= "";
    String fecha_renovacion= "";
    String principal_vencido_mes_subvenc= "";
    String estado_cumplimiento= "";
    String tipo_tarjeta= "";
    String saldo_deudor_excedido_vencido= "";
    String contrato_renegociado = "";
    String principal_vdo_refin_pte_amort= "";
    String fecha_primer_incumpl_op_refin= "";
    String conocimiento_garant_pers_ppal= "";
    String saldo_deudor_dudoso_exced_ven= "";
    String intereses_exced_devengado= "";
    String intereses_exced_devengado_dud= "";
    String estado_refin_reest_reneg= "";
    String id_contrato_juridico= "";
    String tipo_cartera_ifrs9= "";
    String cartera_prudencial= "";
    String fecha_primera_liquidacion= "";
    String producto_recurso= "";
    String instrumento_fiduciario= "";
    String financiacion_proyecto= "";
    String import_recuperad_acum_sit_imp= "";
    String fecha_estado_cumplimiento= "";
    String fecha_refin_reest= "";
    String tipo_referencia_vencimiento= "";
    String codigo_ratio_referencia= "";
    String tipo_referencia_sustitutivo= "";
    String frec_revision_tipo_int_per= "";
    String proxima_revision_tipo_interes= "";
    String fecha_final_carencia_principal= "";
    String fecha_final_periodo_intereses= "";
    String frecuencia_pago_princ_int= "";
    String saldo_vivo_nominal= "";
    String saldo_deudor_fallido_no_venc= "";
    String saldo_deudor_fallido_vencido= "";
    String importes_vencidos= "";
    String productos_fallidos= "";
    String fallidos_acumulados= "";
    String intereses_demora_fallidos= "";
    String gastos_exigibles_fallidos= "";
    String fase_deterioro= "";
    String importe_libros_activo= "";
    String cobertura_acumulada= "";
    String provision_fuera_balance= "";
    String act_val_raz_ries_cred_ant_adq= "";
    String saldo_fuera_balance= "";
    String derechos_reembolso= "";
    String tipo_fuente_de_carga= "";
    String fecha_renegociacion= "";
    String situacion_impago_operacion= "";
    String fecha_situacion_impago_ope= "";
    String reconocimiento_balance= "";
    String saldo_deudor_excedido_no_venc= "";
    String saldo_deudor_dud_exce_no_venc= "";
    String met_calc_cobert_riesgo_normal= "";
    String met_calc_cobert_riesgo_dudoso= "";
    String renovacion_automatica= "";
    String valor_residual_no_garan_dudos= "";
    String valor_residual_comp_ter_dudos= "";
    String productos_dudosos_deven_orden= "";
    String valor_actual_com_garan_conced= "";
    String provision_riesgo_normal= "";
    String total_intereses_deven_credito= "";
    String otras_periodificaciones_activ= "";
    String fecha_baja_def_cirbe= "";
    String situacion_operativa1= "";
    String estado_cumplimiento1= "";
    String contrato_sindicado= "";
    String id_persona_contraparte_directa_cir = "";

    String fecha_cancelacion= "";
    String saldo_deudor_no_vencido= "";
    String saldo_deudor_vencido= "";
    String productos_vencidos= "";
    String intereses_demora_consolidados= "";
    String intereses_devengados= "";
    String id_persona_contraparte_direct= "";
    String valor_residual_comp_terceros= "";
    String cotiza= "";
    String entidad_depositaria= "";
    String dividendos_devengados_mes= "";
    String saldo_contingente= "";
    String jerarquia_valor_razonable= "";
    String cobertura_riesgo_normal= "";
    String intereses_devengados_dudosos= "";
    String valor_razonable= "";
    String saneamientos_directos_mes= "";
    String intereses_cobrados_mes= "";
    String numero_ocurrencias= "";
    String porcentaje_partici_capital = "";
    String saldo_contingente_dudoso = "";
    String saldo_contingente_vencido = "";
    String saldo_contingente_dudoso_venc = "";
    String moneda = "";
    String gastos_activados = "";
    String resto_valor_contable_activos = "";
    String saldo_acreedor = "";
    String pasivos_valor_razonable = "";
    String micro_cobertura = "";
    String prima_descnto_adquis_asuncion = "";
    String activos_valor_razobl = "";
    String valor_residual_no_garantizado = "";
    String importe_liq_compras_mes = "";
    String importe_liq_ventas_mes = "";
    String cobert_perdidas_partcip_mes = "";
    String correcciones_valor_valores = "";
    String tipo_codigo_valor = "";
    String producto_subproducto = "";
    String saldo_deudor_dudoso_no_vencid = "";
    String saldo_deudor_dudoso_vencido = "";
    String productos_vencidos_dudosos = "";
    String intereses_demora_consol_dudos = "";
    String resto_valor_cont_actvos_dudos = "";
    String tedr_venc = "";
    String tedr_desc_exc = "";
    String filler = "";

    String id_contrato= "";
    String aplicacion_origen= "";
    String id_centro_contable= "";
    String codigo_valor= "";
    String pais_negocio= "";
    String activo_op_valores= "";

    String fecha_formalicacion = "";
    String sindicato = "";

    String tipo_interes_nominal = "";
    String tipo_interes_subvencionado = "";
    String tipo_interes_demora = "";
    String com_comp_costes_concesion = "";
    String clase_riesgo_contraparte_directa = "";
    String riesgo_exc_cob_riesgo_pais = "";
    String tipo_cartera = "";
    String ind_op_no_corriente = "";
    String vinculacion_mercado_hipotecario = "";
    String valor_corregido_garantia = "";
    String disponible_subrogacion_terceros = "";
    String ltv = "";
    String dividendos_cobrados_mes = "";
    String tipo_derivado = "";
    String derivados_tipo_mercado = "";
    /*String importe_pendiente_desembolso = "";*/
    String intereses_devengados_mes = "";
    /*String estado_cumplimiento = "";*/
    String estado_refin_reest = "";
    String compra_o_venta = "";
    String pasivos_valor_razonable_riesgo_credito = "";
    String id_persona_agente_observado = "";
    String id_persona_contraparte_solvencia = "";
    String contrato_sindicado1 = "";
    String operacion_adquirida = "";
    String op_no_orig_gest_terc = "";
    String tipo_correccion_valor_deterioro = "";
    String fecha_situacion_operacion_rd = "";
    String fecha_clasificacion_dudosa = "";
    String rc_factor_conversion_orden = "";
    String operacion_titulares_exonerados = "";
    String fecha_primer_incumplimiento_sin_fall_parc = "";
    String classif_oper_anejo_ix = "";
    String fecha_refinanciacion = "";
    private String sin_devengo_interes = "";
    private String fallidos_parciales = "";
    private String entidad_grupo_origen = "";
    private String cuenta_grupo = "";
    private String subcuenta = "";
    private String id_centro_contable_a = "";
    private String moratoria_covid19 = "";
    private String fecha_inicio_moratoria_covid19 = "";
    private String fecha_fin_moratoria_covid19 = "";

    //V2.20
    private String fecha_modificacion = "";
    private String modificacion = "";
    private String aumento_plazo = "";
    private String aumento_plazo_fecha = "";
    private String prestamos_participativos = "";
    private String prestamos_participativos_fecha = "";
    private String reduccion_principal = "";
    private String reduccion_principal_fecha = "";
    private String reduccion_principal_importe_avalista = "";
    private String reduccion_principal_avales_ejecutado = "";
    private String version = "";

    Double saldo_deudor_no_ven = 0.0;
    Double intereses_devengos = 0.0;
    Double valor_nominal_d = 0.0;
    Double saldo_contingente_d = 0.0;
    Double saldo_contingente_eur = 0.0;
    Double reduc_principal_importe_avalista = 0.0;
    Double reduc_principal_avales_ejecutado = 0.0;

    //V2.23
    private String originado_con_deterioro = "";
    private String calculo_situacion_impago = "";

    public String getOriginado_con_deterioro() {
        return originado_con_deterioro;
    }

    public void setOriginado_con_deterioro(String originado_con_deterioro) {
        this.originado_con_deterioro = originado_con_deterioro;
    }

    public String getCalculo_situacion_impago() {
        return calculo_situacion_impago;
    }

    public void setCalculo_situacion_impago(String calculo_situacion_impago) {
        this.calculo_situacion_impago = calculo_situacion_impago;
    }

    public Double getReduc_principal_importe_avalista() {
        return reduc_principal_importe_avalista;
    }

    public void setReduc_principal_importe_avalista(Double reduc_principal_importe_avalista) {
        this.reduc_principal_importe_avalista = reduc_principal_importe_avalista;
    }

    public Double getReduc_principal_avales_ejecutado() {
        return reduc_principal_avales_ejecutado;
    }

    public void setReduc_principal_avales_ejecutado(Double reduc_principal_avales_ejecutado) {
        this.reduc_principal_avales_ejecutado = reduc_principal_avales_ejecutado;
    }

    public String getFecha_modificacion() {
        return fecha_modificacion;
    }

    public void setFecha_modificacion(String fecha_modificacion) {
        this.fecha_modificacion = fecha_modificacion;
    }

    public String getModificacion() {
        return modificacion;
    }

    public void setModificacion(String modificacion) {
        this.modificacion = modificacion;
    }

    public String getAumento_plazo() {
        return aumento_plazo;
    }

    public void setAumento_plazo(String aumento_plazo) {
        this.aumento_plazo = aumento_plazo;
    }

    public String getAumento_plazo_fecha() {
        return aumento_plazo_fecha;
    }

    public void setAumento_plazo_fecha(String aumento_plazo_fecha) {
        this.aumento_plazo_fecha = aumento_plazo_fecha;
    }

    public String getPrestamos_participativos() {
        return prestamos_participativos;
    }

    public void setPrestamos_participativos(String prestamos_participativos) {
        this.prestamos_participativos = prestamos_participativos;
    }

    public String getPrestamos_participativos_fecha() {
        return prestamos_participativos_fecha;
    }

    public void setPrestamos_participativos_fecha(String prestamos_participativos_fecha) {
        this.prestamos_participativos_fecha = prestamos_participativos_fecha;
    }

    public String getReduccion_principal() {
        return reduccion_principal;
    }

    public void setReduccion_principal(String reduccion_principal) {
        this.reduccion_principal = reduccion_principal;
    }

    public String getReduccion_principal_fecha() {
        return reduccion_principal_fecha;
    }

    public void setReduccion_principal_fecha(String reduccion_principal_fecha) {
        this.reduccion_principal_fecha = reduccion_principal_fecha;
    }

    public String getReduccion_principal_importe_avalista() {
        return reduccion_principal_importe_avalista;
    }

    public void setReduccion_principal_importe_avalista(String reduccion_principal_importe_avalista) {
        this.reduccion_principal_importe_avalista = reduccion_principal_importe_avalista;
    }

    public String getReduccion_principal_avales_ejecutado() {
        return reduccion_principal_avales_ejecutado;
    }

    public void setReduccion_principal_avales_ejecutado(String reduccion_principal_avales_ejecutado) {
        this.reduccion_principal_avales_ejecutado = reduccion_principal_avales_ejecutado;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Double getSaldo_deudor_no_ven() {
        return saldo_deudor_no_ven;
    }

    public void setSaldo_deudor_no_ven(InventoryCashPosition saldo_deudor_no_vencido) {
        if(null!= saldo_deudor_no_vencido){

            this.saldo_deudor_no_ven = saldo_deudor_no_vencido.getTotal();
        }else {
            this.saldo_deudor_no_ven = 0.0;
        }
    }

    public void setSaldo_deudor_no_ven(CashPositionDTO  saldo_deudor_no_vencido) {
        if(null!= saldo_deudor_no_vencido){

            this.saldo_deudor_no_ven = saldo_deudor_no_vencido.getAllInValue();
        }else {
            this.saldo_deudor_no_ven = 0.0;
        }
    }
    public void setSaldo_deudor_no_ven(Trade trade) {
        if(null!= trade && null!=trade.getProduct()){
            this.saldo_deudor_no_ven = trade.getProduct().getPrincipal();
        }else {
            this.saldo_deudor_no_ven = 0.0;
        }
    }

    public Double getIntereses_devengos() {
        return intereses_devengos;
    }

    public void setIntereses_devengos(Double intereses_devengos) {
        this.intereses_devengos = intereses_devengos;
    }

    public String getId_contrato() {
        return id_contrato;
    }

    public void setId_contrato(CollateralConfig config, Account account, String date,String accountingLink) {
        String idContrato =  account.getId() + "_" + account.getCurrency() +"_"+ config.getId() + "_" + accountingLink + "_"+ date;
        String id = AnacreditFormatter.formatStringWithBlankOnRight(idContrato, 50);
        this.id_contrato = id;
        this.id_contrato_interno = id;
    }

    public void setId_contrato(String id_contrato) {
        this.id_contrato = AnacreditFormatter.formatStringWithBlankOnRight(id_contrato, 50);;
    }

    public void setId_contrato(Trade trade, Account account, String date, String accountingLink) {

        String idContrato = account.getId() + "_" + trade.getTradeCurrency()  + "_" +  trade.getLongId() + "_"+accountingLink +"_"+ date;
        String id = AnacreditFormatter.formatStringWithBlankOnRight(idContrato, 50);
        this.id_contrato = id;
        this.id_contrato_interno = id;
    }

    public String getActivo_op_valores() {
        return activo_op_valores;
    }

    public void setActivo_op_valores(String activo_op_valores) {
        this.activo_op_valores = activo_op_valores;
    }

    public String getPais_negocio() {
        return pais_negocio;
    }

    public void setPais_negocio(String pais_negocio) {
        this.pais_negocio = pais_negocio;
    }

    public String getCodigo_valor() {
        return codigo_valor;
    }

    public void setCodigo_valor(String codigo_valor) {
        this.codigo_valor = codigo_valor;
    }

    public String getId_centro_contable() {
        return id_centro_contable;
    }

    public void setId_centro_contable(String id_centro_contable) {
        this.id_centro_contable = id_centro_contable;
    }

    public String getAplicacion_origen() {
        return aplicacion_origen;
    }

    public void setAplicacion_origen(String aplicacion_origen) {
        this.aplicacion_origen = aplicacion_origen;
    }

    public String getId_entidad() {
        return id_entidad;
    }

    public void setId_entidad(String id_entidad) {
        this.id_entidad = id_entidad;
    }

    public String getFecha_extraccion() {
        return fecha_extraccion;
    }

    public void setFecha_extraccion(String fecha_extraccion) {
        this.fecha_extraccion = fecha_extraccion;
    }

    public String getId_contrato_interno() {
        return id_contrato_interno;
    }

    public void setId_contrato_interno(String id_contrato_interno) {
        this.id_contrato_interno = id_contrato_interno;
    }

    public String getDeclarado_cir_tercera_entidad() {
        return declarado_cir_tercera_entidad;
    }

    public void setDeclarado_cir_tercera_entidad(String declarado_cir_tercera_entidad) {
        this.declarado_cir_tercera_entidad = declarado_cir_tercera_entidad;
    }

    public String getProducto_ac() {
        return producto_ac;
    }

    public void setProducto_ac(String producto_ac) {
        this.producto_ac = producto_ac;
        String derechos_reeenbolso = "2";
        if (AnacreditFormatter.PRODUCTOS_DERECHO_REEMBOLSO.contains(producto_ac)) {
            derechos_reeenbolso = "1";
        }
        setDerechos_reembolso(AnacreditFormatter.formatStringWithBlankOnRight(derechos_reeenbolso ,1));
    }

    public String getProducto_entidad() {
        return producto_entidad;
    }

    public void setProducto_entidad(String producto_entidad) {
        this.producto_entidad = producto_entidad;
    }

    public String getSubordinacion_producto_ac() {
        return subordinacion_producto_ac;
    }

    public void setSubordinacion_producto_ac(String subordinacion_producto_ac) {
        this.subordinacion_producto_ac = subordinacion_producto_ac;
    }

    public String getTipo_riesgo_subyacente() {
        return tipo_riesgo_subyacente;
    }

    public void setTipo_riesgo_subyacente(String tipo_riesgo_subyacente) {
        this.tipo_riesgo_subyacente = tipo_riesgo_subyacente;
    }

    public String getFinalidad_ac() {
        return finalidad_ac;
    }

    public void setFinalidad_ac(String finalidad_ac) {
        this.finalidad_ac = finalidad_ac;
    }

    public String getTramite_recuperacion() {
        return tramite_recuperacion;
    }

    public void setTramite_recuperacion(String tramite_recuperacion) {
        this.tramite_recuperacion = tramite_recuperacion;
    }

    public String getPrincipal_inicial() {
        return principal_inicial;
    }

    public void setPrincipal_inicial(String principal_inicial) {
        this.principal_inicial = principal_inicial;
    }

    public String getLimite_inicial() {
        return limite_inicial;
    }

    public void setLimite_inicial(String limite_inicial) {
        this.limite_inicial = limite_inicial;
    }

    public String getImporte_concedido() {
        return importe_concedido;
    }

    public void setImporte_concedido(String importe_concedido) {
        this.importe_concedido = importe_concedido;
    }

    public String getFecha_emision_originacion() {
        return fecha_emision_originacion;
    }

    public void setFecha_emision_originacion(String fecha_emision_originacion) {
        this.fecha_emision_originacion = fecha_emision_originacion;
    }

    public String getFecha_vencimiento() {
        return fecha_vencimiento;
    }

    public void setFecha_vencimiento(String fecha_vencimiento) {
        this.fecha_vencimiento = fecha_vencimiento;
    }

    public String getOrigen_operacion() {
        return origen_operacion;
    }

    public void setOrigen_operacion(String origen_operacion) {
        this.origen_operacion = origen_operacion;
    }

    public String getRefinanciacion() {
        return refinanciacion;
    }

    public void setRefinanciacion(String refinanciacion) {
        this.refinanciacion = refinanciacion;
    }

    public String getSubvencion_operacion() {
        return subvencion_operacion;
    }

    public void setSubvencion_operacion(String subvencion_operacion) {
        this.subvencion_operacion = subvencion_operacion;
    }

    public String getCanal_contratacion_ac() {
        return canal_contratacion_ac;
    }

    public void setCanal_contratacion_ac(String canal_contratacion_ac) {
        this.canal_contratacion_ac = canal_contratacion_ac;
    }

    public String getProvincia_negocio() {
        return provincia_negocio;
    }

    public void setProvincia_negocio(String provincia_negocio) {
        this.provincia_negocio = provincia_negocio;
    }

    public String getEsquema_amort_operaciones_ac() {
        return esquema_amort_operaciones_ac;
    }

    public void setEsquema_amort_operaciones_ac(String esquema_amort_operaciones_ac) {
        this.esquema_amort_operaciones_ac = esquema_amort_operaciones_ac;
    }

    public String getPorcent_particip_sindicados() {
        return porcent_particip_sindicados;
    }

    public void setPorcent_particip_sindicados(String porcent_particip_sindicados) {
        this.porcent_particip_sindicados = porcent_particip_sindicados;
    }

    public String getEdif_financ_estado() {
        return edif_financ_estado;
    }

    public void setEdif_financ_estado(String edif_financ_estado) {
        this.edif_financ_estado = edif_financ_estado;
    }

    public String getEdif_financ_licencia() {
        return edif_financ_licencia;
    }

    public void setEdif_financ_licencia(String edif_financ_licencia) {
        this.edif_financ_licencia = edif_financ_licencia;
    }

    public String getInmueble_financ_num_viviendas() {
        return inmueble_financ_num_viviendas;
    }

    public void setInmueble_financ_num_viviendas(String inmueble_financ_num_viviendas) {
        this.inmueble_financ_num_viviendas = inmueble_financ_num_viviendas;
    }

    public String getCodigo_promoc_inmob_finan() {
        return codigo_promoc_inmob_finan;
    }

    public void setCodigo_promoc_inmob_finan(String codigo_promoc_inmob_finan) {
        this.codigo_promoc_inmob_finan = codigo_promoc_inmob_finan;
    }

    public String getDesc_promoc_inmob_finan() {
        return desc_promoc_inmob_finan;
    }

    public void setDesc_promoc_inmob_finan(String desc_promoc_inmob_finan) {
        this.desc_promoc_inmob_finan = desc_promoc_inmob_finan;
    }

    public String getPlazo_residual() {
        return plazo_residual;
    }

    public void setPlazo_residual(String plazo_residual) {
        this.plazo_residual = AnacreditFormatter.formatStringWithBlankOnRight(plazo_residual,2);
    }

    public String getTipo_garantia_real_ppal() {
        return tipo_garantia_real_ppal;
    }

    public void setTipo_garantia_real_ppal(String tipo_garantia_real_ppal) {
        this.tipo_garantia_real_ppal = tipo_garantia_real_ppal;
    }

    public String getOrden_hipoteca() {
        return orden_hipoteca;
    }

    public void setOrden_hipoteca(String orden_hipoteca) {
        this.orden_hipoteca = orden_hipoteca;
    }

    public String getCobertura_garantia_real() {
        return cobertura_garantia_real;
    }

    public void setCobertura_garantia_real(String cobertura_garantia_real) {
        this.cobertura_garantia_real = cobertura_garantia_real;
    }

    public String getTipo_garant_personal_ppal_ac() {
        return tipo_garant_personal_ppal_ac;
    }

    public void setTipo_garant_personal_ppal_ac(String tipo_garant_personal_ppal_ac) {
        this.tipo_garant_personal_ppal_ac = tipo_garant_personal_ppal_ac;
    }

    public String getCobertura_garantia_personal() {
        return cobertura_garantia_personal;
    }

    public void setCobertura_garantia_personal(String cobertura_garantia_personal) {
        this.cobertura_garantia_personal = cobertura_garantia_personal;
    }

    public String getSituacion_operativa() {
        return situacion_operativa;
    }

    public void setSituacion_operativa(String situacion_operativa) {
        this.situacion_operativa = situacion_operativa;
    }

    public String getFecha_primer_incumplimiento() {
        return fecha_primer_incumplimiento;
    }

    public void setFecha_primer_incumplimiento(String fecha_primer_incumplimiento) {
        this.fecha_primer_incumplimiento = fecha_primer_incumplimiento;
    }

    public String getFecha_ultimo_impago_incumplim() {
        return fecha_ultimo_impago_incumplim;
    }

    public void setFecha_ultimo_impago_incumplim(String fecha_ultimo_impago_incumplim) {
        this.fecha_ultimo_impago_incumplim = fecha_ultimo_impago_incumplim;
    }

    public String getProductos_dudosos_no_consolid() {
        return productos_dudosos_no_consolid;
    }

    public void setProductos_dudosos_no_consolid(String productos_dudosos_no_consolid) {
        this.productos_dudosos_no_consolid = productos_dudosos_no_consolid;
    }

    public String getIntereses_demora_no_consolid() {
        return intereses_demora_no_consolid;
    }

    public void setIntereses_demora_no_consolid(String intereses_demora_no_consolid) {
        this.intereses_demora_no_consolid = intereses_demora_no_consolid;
    }

    public String getLimite() {
        return limite;
    }

    public void setLimite(String limite) {
        this.limite = limite;
    }

    public String getDisponible_inmediato() {
        return disponible_inmediato;
    }

    public void setDisponible_inmediato(String disponible_inmediato) {
        this.disponible_inmediato = disponible_inmediato;
    }

    public String getDisponible_condicionado() {
        return disponible_condicionado;
    }

    public void setDisponible_condicionado(String disponible_condicionado) {
        this.disponible_condicionado = disponible_condicionado;
    }

    public String getPpal_cobrado_mes_amort_corrnt() {
        return ppal_cobrado_mes_amort_corrnt;
    }

    public void setPpal_cobrado_mes_amort_corrnt(String ppal_cobrado_mes_amort_corrnt) {
        this.ppal_cobrado_mes_amort_corrnt = ppal_cobrado_mes_amort_corrnt;
    }

    public String getPpal_cobrado_mes_amort_antici() {
        return ppal_cobrado_mes_amort_antici;
    }

    public void setPpal_cobrado_mes_amort_antici(String ppal_cobrado_mes_amort_antici) {
        this.ppal_cobrado_mes_amort_antici = ppal_cobrado_mes_amort_antici;
    }

    public String getVar_deuda_cond_presc() {
        return var_deuda_cond_presc;
    }

    public void setVar_deuda_cond_presc(String var_deuda_cond_presc) {
        this.var_deuda_cond_presc = var_deuda_cond_presc;
    }

    public String getTipo_act_recib_pago_mes() {
        return tipo_act_recib_pago_mes;
    }

    public void setTipo_act_recib_pago_mes(String tipo_act_recib_pago_mes) {
        this.tipo_act_recib_pago_mes = tipo_act_recib_pago_mes;
    }

    public String getReduc_deuda_act_recib_pag_mes() {
        return reduc_deuda_act_recib_pag_mes;
    }

    public void setReduc_deuda_act_recib_pag_mes(String reduc_deuda_act_recib_pag_mes) {
        this.reduc_deuda_act_recib_pag_mes = reduc_deuda_act_recib_pag_mes;
    }

    public String getTipo_subrogacion_mes() {
        return tipo_subrogacion_mes;
    }

    public void setTipo_subrogacion_mes(String tipo_subrogacion_mes) {
        this.tipo_subrogacion_mes = tipo_subrogacion_mes;
    }

    public String getReduc_deuda_subrogacion_mes() {
        return reduc_deuda_subrogacion_mes;
    }

    public void setReduc_deuda_subrogacion_mes(String reduc_deuda_subrogacion_mes) {
        this.reduc_deuda_subrogacion_mes = reduc_deuda_subrogacion_mes;
    }

    public String getTipo_refinanc_mes() {
        return tipo_refinanc_mes;
    }

    public void setTipo_refinanc_mes(String tipo_refinanc_mes) {
        this.tipo_refinanc_mes = tipo_refinanc_mes;
    }

    public String getReduc_deuda_refinanc_mes() {
        return reduc_deuda_refinanc_mes;
    }

    public void setReduc_deuda_refinanc_mes(String reduc_deuda_refinanc_mes) {
        this.reduc_deuda_refinanc_mes = reduc_deuda_refinanc_mes;
    }

    public String getGestion_op_transferida_mes() {
        return gestion_op_transferida_mes;
    }

    public void setGestion_op_transferida_mes(String gestion_op_transferida_mes) {
        this.gestion_op_transferida_mes = gestion_op_transferida_mes;
    }

    public String getImporte_transferido_mes_ci() {
        return importe_transferido_mes_ci;
    }

    public void setImporte_transferido_mes_ci(String importe_transferido_mes_ci) {
        this.importe_transferido_mes_ci = importe_transferido_mes_ci;
    }

    public String getFecha_ultimo_vto_intereses() {
        return fecha_ultimo_vto_intereses;
    }

    public void setFecha_ultimo_vto_intereses(String fecha_ultimo_vto_intereses) {
        this.fecha_ultimo_vto_intereses = fecha_ultimo_vto_intereses;
    }

    public String getFecha_proximo_vto_intereses() {
        return fecha_proximo_vto_intereses;
    }

    public void setFecha_proximo_vto_intereses(String fecha_proximo_vto_intereses) {
        this.fecha_proximo_vto_intereses = fecha_proximo_vto_intereses;
    }

    public String getFecha_ultimo_vto_prinicipal() {
        return fecha_ultimo_vto_prinicipal;
    }

    public void setFecha_ultimo_vto_prinicipal(String fecha_ultimo_vto_prinicipal) {
        this.fecha_ultimo_vto_prinicipal = fecha_ultimo_vto_prinicipal;
    }

    public String getFecha_proximo_vto_principal() {
        return fecha_proximo_vto_principal;
    }

    public void setFecha_proximo_vto_principal(String fecha_proximo_vto_principal) {
        this.fecha_proximo_vto_principal = fecha_proximo_vto_principal;
    }

    public String getNum_cuotas_impagadas_ppal() {
        return num_cuotas_impagadas_ppal;
    }

    public void setNum_cuotas_impagadas_ppal(String num_cuotas_impagadas_ppal) {
        this.num_cuotas_impagadas_ppal = num_cuotas_impagadas_ppal;
    }

    public String getModalidad_tipo_interes_ac() {
        return modalidad_tipo_interes_ac;
    }

    public void setModalidad_tipo_interes_ac(String modalidad_tipo_interes_ac) {
        this.modalidad_tipo_interes_ac = AnacreditFormatter.formatStringWithBlankOnRight(modalidad_tipo_interes_ac,3);

        String sin_devengo_interes = "0";
        if (AnacreditFormatter.M16.equals(modalidad_tipo_interes_ac)
                || AnacreditFormatter.M9.equals(modalidad_tipo_interes_ac))  {
            sin_devengo_interes = "1";
        }
        setSin_devengo_interes(AnacreditFormatter.formatStringWithBlankOnRight(sin_devengo_interes,1));
    }

    public String getTedr() {
        return tedr;
    }

    public void setTedr(Double tedr) {
        String direction = "+";
        if(tedr<0){
            direction = "-";
        }
        this.tedr = direction+ AnacreditFormatter.formatUnsignedNumber(Math.abs(tedr),7,4,"");
    }

    public String getIndice_referencia_ac() {
        return indice_referencia_ac;
    }

    public void setIndice_referencia_ac(String indice_referencia_ac) {
        this.indice_referencia_ac = indice_referencia_ac;
    }

    public String getSuelo_rentabilidad() {
        return suelo_rentabilidad;
    }

    public void setSuelo_rentabilidad(String suelo_rentabilidad) {
        this.suelo_rentabilidad = suelo_rentabilidad;
    }

    public String getTecho_rentabilidad() {
        return techo_rentabilidad;
    }

    public void setTecho_rentabilidad(String techo_rentabilidad) {
        this.techo_rentabilidad = techo_rentabilidad;
    }

    public String getDiferencial_sobre_indice_refe() {
        return diferencial_sobre_indice_refe;
    }

    public void setDiferencial_sobre_indice_refe(String diferencial_sobre_indice_refe) {
        this.diferencial_sobre_indice_refe = diferencial_sobre_indice_refe;
    }

    public String getSituacion_ope_riesgo_direc_ac() {
        return situacion_ope_riesgo_direc_ac;
    }

    public void setSituacion_ope_riesgo_direc_ac(String situacion_ope_riesgo_direc_ac) {
        this.situacion_ope_riesgo_direc_ac = situacion_ope_riesgo_direc_ac;
    }

    public String getCobertura_riesgo_dudoso() {
        return cobertura_riesgo_dudoso;
    }

    public void setCobertura_riesgo_dudoso(String cobertura_riesgo_dudoso) {
        this.cobertura_riesgo_dudoso = cobertura_riesgo_dudoso;
    }

    public String getCobertura_riesgo_pais() {
        return cobertura_riesgo_pais;
    }

    public void setCobertura_riesgo_pais(String cobertura_riesgo_pais) {
        this.cobertura_riesgo_pais = cobertura_riesgo_pais;
    }

    public String getProvision_riesgo_dudoso() {
        return provision_riesgo_dudoso;
    }

    public void setProvision_riesgo_dudoso(String provision_riesgo_dudoso) {
        this.provision_riesgo_dudoso = provision_riesgo_dudoso;
    }

    public String getProvision_riesgo_pais() {
        return provision_riesgo_pais;
    }

    public void setProvision_riesgo_pais(String provision_riesgo_pais) {
        this.provision_riesgo_pais = provision_riesgo_pais;
    }

    public String getComisiones_pendientes_devengo() {
        return comisiones_pendientes_devengo;
    }

    public void setComisiones_pendientes_devengo(String comisiones_pendientes_devengo) {
        this.comisiones_pendientes_devengo = comisiones_pendientes_devengo;
    }

    public String getCoste_transaccion() {
        return coste_transaccion;
    }

    public void setCoste_transaccion(String coste_transaccion) {
        this.coste_transaccion = coste_transaccion;
    }

    public String getDescuento_por_deterioro() {
        return descuento_por_deterioro;
    }

    public void setDescuento_por_deterioro(String descuento_por_deterioro) {
        this.descuento_por_deterioro = descuento_por_deterioro;
    }

    public String getActivos_valor_razobl_rsgo_cre() {
        return activos_valor_razobl_rsgo_cre;
    }

    public void setActivos_valor_razobl_rsgo_cre(String activos_valor_razobl_rsgo_cre) {
        this.activos_valor_razobl_rsgo_cre = activos_valor_razobl_rsgo_cre;
    }

    public String getValor_nominal() {
        return valor_nominal;
    }

    public void setValor_nominal(Double valor_nominal,String currency,JDate valDate,PricingEnv pricingEnv) {
        Double aDouble = AnacreditUtilities.convertToEUR(valor_nominal, currency, valDate, pricingEnv);
        String direction = "-";
        if(aDouble==0.0){
            direction = "+";
        }
        this.valor_nominal =  direction+ AnacreditFormatter.formatUnsignedNumber(aDouble, 15,0,"");
    }

    public void setValor_nominal_d(Double valor_nominal_d) {
        this.valor_nominal_d = valor_nominal_d;
    }
    public Double getValor_nominal_d() {
        return this.valor_nominal_d;
    }

    public void setValor_nominal(String valor_nominal) {
        this.valor_nominal = valor_nominal;
    }

    public String getNocional() {
        return nocional;
    }

    public void setNocional(String nocional) {
        this.nocional = nocional;
    }

    public String getImporte_pendiente_desembolso() {
        return importe_pendiente_desembolso;
    }

    public void setImporte_pendiente_desembolso(String importe_pendiente_desembolso) {
        this.importe_pendiente_desembolso = importe_pendiente_desembolso;
    }

    public String getPlazo_origen_m() {
        return plazo_origen_m;
    }

    public void setPlazo_origen_m(String plazo_origen_m) {
        this.plazo_origen_m = AnacreditFormatter.formatStringWithBlankOnRight(plazo_origen_m,2);
    }

    public String getOperacion_no_declarable_cirbe() {
        return operacion_no_declarable_cirbe;
    }

    public void setOperacion_no_declarable_cirbe(String operacion_no_declarable_cirbe) {
        this.operacion_no_declarable_cirbe = operacion_no_declarable_cirbe;
    }

    public String getFecha_renovacion() {
        return fecha_renovacion;
    }

    public void setFecha_renovacion(String fecha_renovacion) {
        this.fecha_renovacion = fecha_renovacion;
    }

    public String getPrincipal_vencido_mes_subvenc() {
        return principal_vencido_mes_subvenc;
    }

    public void setPrincipal_vencido_mes_subvenc(String principal_vencido_mes_subvenc) {
        this.principal_vencido_mes_subvenc = principal_vencido_mes_subvenc;
    }

    public String getEstado_cumplimiento() {
        return estado_cumplimiento;
    }

    public void setEstado_cumplimiento(String estado_cumplimiento) {
        this.estado_cumplimiento = estado_cumplimiento;
    }

    public String getTipo_tarjeta() {
        return tipo_tarjeta;
    }

    public void setTipo_tarjeta(String tipo_tarjeta) {
        this.tipo_tarjeta = tipo_tarjeta;
    }

    public String getSaldo_deudor_excedido_vencido() {
        return saldo_deudor_excedido_vencido;
    }

    public void setSaldo_deudor_excedido_vencido(String saldo_deudor_excedido_vencido) {
        this.saldo_deudor_excedido_vencido = saldo_deudor_excedido_vencido;
    }

    public String getPrincipal_vdo_refin_pte_amort() {
        return principal_vdo_refin_pte_amort;
    }

    public void setPrincipal_vdo_refin_pte_amort(String principal_vdo_refin_pte_amort) {
        this.principal_vdo_refin_pte_amort = principal_vdo_refin_pte_amort;
    }

    public String getFecha_primer_incumpl_op_refin() {
        return fecha_primer_incumpl_op_refin;
    }

    public void setFecha_primer_incumpl_op_refin(String fecha_primer_incumpl_op_refin) {
        this.fecha_primer_incumpl_op_refin = fecha_primer_incumpl_op_refin;
    }

    public String getConocimiento_garant_pers_ppal() {
        return conocimiento_garant_pers_ppal;
    }

    public void setConocimiento_garant_pers_ppal(String conocimiento_garant_pers_ppal) {
        this.conocimiento_garant_pers_ppal = conocimiento_garant_pers_ppal;
    }

    public String getSaldo_deudor_dudoso_exced_ven() {
        return saldo_deudor_dudoso_exced_ven;
    }

    public void setSaldo_deudor_dudoso_exced_ven(String saldo_deudor_dudoso_exced_ven) {
        this.saldo_deudor_dudoso_exced_ven = saldo_deudor_dudoso_exced_ven;
    }

    public String getIntereses_exced_devengado() {
        return intereses_exced_devengado;
    }

    public void setIntereses_exced_devengado(String intereses_exced_devengado) {
        this.intereses_exced_devengado = intereses_exced_devengado;
    }

    public String getIntereses_exced_devengado_dud() {
        return intereses_exced_devengado_dud;
    }

    public void setIntereses_exced_devengado_dud(String intereses_exced_devengado_dud) {
        this.intereses_exced_devengado_dud = intereses_exced_devengado_dud;
    }

    public String getEstado_refin_reest_reneg() {
        return estado_refin_reest_reneg;
    }

    public void setEstado_refin_reest_reneg(String estado_refin_reest_reneg) {
        this.estado_refin_reest_reneg = estado_refin_reest_reneg;
    }

    public String getId_contrato_juridico() {
        return id_contrato_juridico;
    }

    public void setId_contrato_juridico(String id_contrato_juridico) {
        this.id_contrato_juridico = id_contrato_juridico;
    }

    public String getTipo_cartera_ifrs9() {
        return tipo_cartera_ifrs9;
    }

    public void setTipo_cartera_ifrs9(String tipo_cartera_ifrs9) {
        this.tipo_cartera_ifrs9 = tipo_cartera_ifrs9;
    }

    public String getCartera_prudencial() {
        return cartera_prudencial;
    }

    public void setCartera_prudencial() {
        String value = AnacreditMapper.getCarteraPrudencial(this.tipo_cartera_ifrs9);
        this.cartera_prudencial = AnacreditFormatter.formatStringWithBlankOnRight(value,3);
    }

    public void setCartera_prudencial(String value) {
        this.cartera_prudencial = value;
    }

    public String getFecha_primera_liquidacion() {
        return fecha_primera_liquidacion;
    }

    public void setFecha_primera_liquidacion(String fecha_primera_liquidacion) {
        this.fecha_primera_liquidacion = fecha_primera_liquidacion;
    }

    public String getProducto_recurso() {
        return producto_recurso;
    }

    public void setProducto_recurso(String producto_recurso) {
        this.producto_recurso = producto_recurso;
    }

    public String getInstrumento_fiduciario() {
        return instrumento_fiduciario;
    }

    public void setInstrumento_fiduciario(String instrumento_fiduciario) {
        this.instrumento_fiduciario = instrumento_fiduciario;
    }

    public String getFinanciacion_proyecto() {
        return financiacion_proyecto;
    }

    public void setFinanciacion_proyecto(String financiacion_proyecto) {
        this.financiacion_proyecto = financiacion_proyecto;
    }

    public String getImport_recuperad_acum_sit_imp() {
        return import_recuperad_acum_sit_imp;
    }

    public void setImport_recuperad_acum_sit_imp(String import_recuperad_acum_sit_imp) {
        this.import_recuperad_acum_sit_imp = import_recuperad_acum_sit_imp;
    }

    public String getFecha_estado_cumplimiento() {
        return fecha_estado_cumplimiento;
    }

    public void setFecha_estado_cumplimiento(String fecha_estado_cumplimiento) {
        this.fecha_estado_cumplimiento = fecha_estado_cumplimiento;
    }

    public String getFecha_refin_reest() {
        return fecha_refin_reest;
    }

    public void setFecha_refin_reest(String fecha_refin_reest) {
        this.fecha_refin_reest = fecha_refin_reest;
    }

    public String getTipo_referencia_vencimiento() {
        return tipo_referencia_vencimiento;
    }

    public void setTipo_referencia_vencimiento(String tipo_referencia_vencimiento) {
        this.tipo_referencia_vencimiento = tipo_referencia_vencimiento;
    }

    public String getCodigo_ratio_referencia() {
        return codigo_ratio_referencia;
    }

    public void setCodigo_ratio_referencia(String codigo_ratio_referencia) {
        this.codigo_ratio_referencia = codigo_ratio_referencia;
    }

    public String getTipo_referencia_sustitutivo() {
        return tipo_referencia_sustitutivo;
    }

    public void setTipo_referencia_sustitutivo(AccountInterestConfig accInterest) {
        String accountDir = AnacreditMapper.getAccountDir(accInterest);
        String refsus = "4";
        if(AnacreditFormatter.FLOAT.equalsIgnoreCase(accountDir)){
            if(null!=accInterest){
                String interestName = accInterest.getName();
                if(AnacreditFormatter.EURIBOR.equalsIgnoreCase(interestName)){
                    refsus = "1";
                }else if (AnacreditFormatter.LIBOR.equalsIgnoreCase(interestName)){
                    refsus = "2";
                }else if(AnacreditFormatter.MIBOR.equalsIgnoreCase(interestName)){
                    refsus = "3";
                }
            }
        }else if(AnacreditFormatter.FIXED.equalsIgnoreCase(accountDir)){
            refsus = "11";
        }
        this.tipo_referencia_sustitutivo = AnacreditFormatter.formatStringWithBlankOnRight(refsus,3);
    }

    public void setTipo_referencia_sustitutivo(String value) {
        this.tipo_referencia_sustitutivo = AnacreditFormatter.formatStringWithBlankOnRight(value,3);
    }

    public String getFrec_revision_tipo_int_per() {
        return frec_revision_tipo_int_per;
    }

    public void setFrec_revision_tipo_int_per(String frec_revision_tipo_int_per) {
        this.frec_revision_tipo_int_per = frec_revision_tipo_int_per;
    }

    public String getProxima_revision_tipo_interes() {
        return proxima_revision_tipo_interes;
    }

    public void setProxima_revision_tipo_interes(String proxima_revision_tipo_interes) {
        this.proxima_revision_tipo_interes = proxima_revision_tipo_interes;
    }

    public String getFecha_final_periodo_intereses() {
        return fecha_final_periodo_intereses;
    }

    public void setFecha_final_periodo_intereses(String fecha_final_periodo_intereses) {
        this.fecha_final_periodo_intereses = fecha_final_periodo_intereses;
    }

    public String getFrecuencia_pago_princ_int() {
        return frecuencia_pago_princ_int;
    }

    public void setFrecuencia_pago_princ_int(String frecuencia_pago_princ_int) {
        this.frecuencia_pago_princ_int = frecuencia_pago_princ_int;
    }

    public String getSaldo_vivo_nominal() {
        return saldo_vivo_nominal;
    }

    public void setSaldo_vivo_nominal(String saldo_vivo_nominal) {
        this.saldo_vivo_nominal = saldo_vivo_nominal;
    }

    public String getSaldo_deudor_fallido_no_venc() {
        return saldo_deudor_fallido_no_venc;
    }

    public void setSaldo_deudor_fallido_no_venc(String saldo_deudor_fallido_no_venc) {
        this.saldo_deudor_fallido_no_venc = saldo_deudor_fallido_no_venc;
    }

    public String getSaldo_deudor_fallido_vencido() {
        return saldo_deudor_fallido_vencido;
    }

    public void setSaldo_deudor_fallido_vencido(String saldo_deudor_fallido_vencido) {
        this.saldo_deudor_fallido_vencido = saldo_deudor_fallido_vencido;
    }

    public String getImportes_vencidos() {
        return importes_vencidos;
    }

    public void setImportes_vencidos(String importes_vencidos) {
        this.importes_vencidos = importes_vencidos;
    }

    public String getProductos_fallidos() {
        return productos_fallidos;
    }

    public void setProductos_fallidos(String productos_fallidos) {
        this.productos_fallidos = productos_fallidos;
    }

    public String getFallidos_acumulados() {
        return fallidos_acumulados;
    }

    public void setFallidos_acumulados(String fallidos_acumulados) {
        this.fallidos_acumulados = fallidos_acumulados;
    }

    public String getIntereses_demora_fallidos() {
        return intereses_demora_fallidos;
    }

    public void setIntereses_demora_fallidos(String intereses_demora_fallidos) {
        this.intereses_demora_fallidos = intereses_demora_fallidos;
    }

    public String getGastos_exigibles_fallidos() {
        return gastos_exigibles_fallidos;
    }

    public void setGastos_exigibles_fallidos(String gastos_exigibles_fallidos) {
        this.gastos_exigibles_fallidos = gastos_exigibles_fallidos;
    }

    public String getFase_deterioro() {
        return fase_deterioro;
    }

    public void setFase_deterioro(String fase_deterioro) {
        this.fase_deterioro = fase_deterioro;
    }

    public String getImporte_libros_activo() {
        return importe_libros_activo;
    }

    public void setImporte_libros_activo(String importe_libros_activo) {
        this.importe_libros_activo = importe_libros_activo;
    }

    public String getCobertura_acumulada() {
        return cobertura_acumulada;
    }

    public void setCobertura_acumulada(String cobertura_acumulada) {
        this.cobertura_acumulada = cobertura_acumulada;
    }

    public String getProvision_fuera_balance() {
        return provision_fuera_balance;
    }

    public void setProvision_fuera_balance(String provision_fuera_balance) {
        this.provision_fuera_balance = provision_fuera_balance;
    }

    public String getAct_val_raz_ries_cred_ant_adq() {
        return act_val_raz_ries_cred_ant_adq;
    }

    public void setAct_val_raz_ries_cred_ant_adq(String act_val_raz_ries_cred_ant_adq) {
        this.act_val_raz_ries_cred_ant_adq = act_val_raz_ries_cred_ant_adq;
    }

    public String getSaldo_fuera_balance() {
        return saldo_fuera_balance;
    }

    public void setSaldo_fuera_balance(String saldo_fuera_balance) {
        this.saldo_fuera_balance = saldo_fuera_balance;
    }

    public String getDerechos_reembolso() {
        return derechos_reembolso;
    }

    private void setDerechos_reembolso(String derechos_reembolso) {
        this.derechos_reembolso = derechos_reembolso;
    }

    public String getTipo_fuente_de_carga() {
        return tipo_fuente_de_carga;
    }

    public void setTipo_fuente_de_carga(String tipo_fuente_de_carga) {
        this.tipo_fuente_de_carga = tipo_fuente_de_carga;
    }

    public String getFecha_renegociacion() {
        return fecha_renegociacion;
    }

    public void setFecha_renegociacion(String fecha_renegociacion) {
        this.fecha_renegociacion = fecha_renegociacion;
    }

    public String getSituacion_impago_operacion() {
        return situacion_impago_operacion;
    }

    public void setSituacion_impago_operacion(String situacion_impago_operacion) {
        this.situacion_impago_operacion = situacion_impago_operacion;
    }

    public String getFecha_situacion_impago_ope() {
        return fecha_situacion_impago_ope;
    }

    public void setFecha_situacion_impago_ope(String fecha_situacion_impago_ope) {
        this.fecha_situacion_impago_ope = fecha_situacion_impago_ope;
    }

    public String getReconocimiento_balance() {
        return reconocimiento_balance;
    }

    public void setReconocimiento_balance(String reconocimiento_balance) {
        this.reconocimiento_balance = reconocimiento_balance;
    }

    public String getSaldo_deudor_excedido_no_venc() {
        return saldo_deudor_excedido_no_venc;
    }

    public void setSaldo_deudor_excedido_no_venc(String saldo_deudor_excedido_no_venc) {
        this.saldo_deudor_excedido_no_venc = saldo_deudor_excedido_no_venc;
    }

    public String getSaldo_deudor_dud_exce_no_venc() {
        return saldo_deudor_dud_exce_no_venc;
    }

    public void setSaldo_deudor_dud_exce_no_venc(String saldo_deudor_dud_exce_no_venc) {
        this.saldo_deudor_dud_exce_no_venc = saldo_deudor_dud_exce_no_venc;
    }

    public String getMet_calc_cobert_riesgo_normal() {
        return met_calc_cobert_riesgo_normal;
    }

    public void setMet_calc_cobert_riesgo_normal(String met_calc_cobert_riesgo_normal) {
        this.met_calc_cobert_riesgo_normal = met_calc_cobert_riesgo_normal;
    }

    public String getMet_calc_cobert_riesgo_dudoso() {
        return met_calc_cobert_riesgo_dudoso;
    }

    public void setMet_calc_cobert_riesgo_dudoso(String met_calc_cobert_riesgo_dudoso) {
        this.met_calc_cobert_riesgo_dudoso = met_calc_cobert_riesgo_dudoso;
    }

    public String getRenovacion_automatica() {
        return renovacion_automatica;
    }

    public void setRenovacion_automatica(String renovacion_automatica) {
        this.renovacion_automatica = renovacion_automatica;
    }

    public String getValor_residual_no_garan_dudos() {
        return valor_residual_no_garan_dudos;
    }

    public void setValor_residual_no_garan_dudos(String valor_residual_no_garan_dudos) {
        this.valor_residual_no_garan_dudos = valor_residual_no_garan_dudos;
    }

    public String getValor_residual_comp_ter_dudos() {
        return valor_residual_comp_ter_dudos;
    }

    public void setValor_residual_comp_ter_dudos(String valor_residual_comp_ter_dudos) {
        this.valor_residual_comp_ter_dudos = valor_residual_comp_ter_dudos;
    }

    public String getProductos_dudosos_deven_orden() {
        return productos_dudosos_deven_orden;
    }

    public void setProductos_dudosos_deven_orden(String productos_dudosos_deven_orden) {
        this.productos_dudosos_deven_orden = productos_dudosos_deven_orden;
    }

    public String getValor_actual_com_garan_conced() {
        return valor_actual_com_garan_conced;
    }

    public void setValor_actual_com_garan_conced(String valor_actual_com_garan_conced) {
        this.valor_actual_com_garan_conced = valor_actual_com_garan_conced;
    }

    public String getProvision_riesgo_normal() {
        return provision_riesgo_normal;
    }

    public void setProvision_riesgo_normal(String provision_riesgo_normal) {
        this.provision_riesgo_normal = provision_riesgo_normal;
    }

    public String getTotal_intereses_deven_credito() {
        return total_intereses_deven_credito;
    }

    public void setTotal_intereses_deven_credito(String total_intereses_deven_credito) {
        this.total_intereses_deven_credito = total_intereses_deven_credito;
    }

    public String getOtras_periodificaciones_activ() {
        return otras_periodificaciones_activ;
    }

    public void setOtras_periodificaciones_activ(String otras_periodificaciones_activ) {
        this.otras_periodificaciones_activ = otras_periodificaciones_activ;
    }

    public String getFecha_baja_def_cirbe() {
        return fecha_baja_def_cirbe;
    }

    public void setFecha_baja_def_cirbe(String fecha_baja_def_cirbe) {
        this.fecha_baja_def_cirbe = fecha_baja_def_cirbe;
    }

    public String getSituacion_operativa1() {
        return situacion_operativa1;
    }

    public void setSituacion_operativa1(String situacion_operativa1) {
        this.situacion_operativa1 = situacion_operativa1;
    }

    public String getEstado_cumplimiento1() {
        return estado_cumplimiento1;
    }

    public void setEstado_cumplimiento1(String estado_cumplimiento1) {
        this.estado_cumplimiento1 = estado_cumplimiento1;
    }

    public String getContrato_sindicado() {
        return contrato_sindicado;
    }

    public void setContrato_sindicado(String contrato_sindicado) {
        this.contrato_sindicado = contrato_sindicado;
    }

    public String getFecha_cancelacion() {
        return fecha_cancelacion;
    }

    public void setFecha_cancelacion(String fecha_cancelacion) {
        this.fecha_cancelacion = fecha_cancelacion;
    }

    public String getSaldo_deudor_no_vencido() {
        return saldo_deudor_no_vencido;
    }

    public void setSaldo_deudor_no_vencido(InventoryCashPosition saldo_deudor_no_vencido, int numChar, JDate valDate, PricingEnv pricingEnv) {
        double total = 0.0;
        String direction = "-";
        if(null!=saldo_deudor_no_vencido){
            total = AnacreditUtilities.convertToEUR(saldo_deudor_no_vencido.getTotal(),saldo_deudor_no_vencido.getCurrency(),valDate,pricingEnv);
        }
        if(total==0.0){
            direction = "+";
        }
        this.saldo_deudor_no_vencido =  direction+ AnacreditFormatter.formatUnsignedNumber(Math.abs(total),numChar,2,"");
    }

    public void setSaldo_deudor_no_vencido(CashPositionDTO saldo_deudor_no_vencido, int numChar, JDate valDate, PricingEnv pricingEnv) {
        double total = 0.0;
        String direction = "-";
        if(null!=saldo_deudor_no_vencido){
            total = AnacreditUtilities.convertToEUR(saldo_deudor_no_vencido.getValue(),saldo_deudor_no_vencido.getCurrency(),valDate,pricingEnv);
        }
        if(total==0.0){
            direction = "+";
        }
        this.saldo_deudor_no_vencido =  direction+ AnacreditFormatter.formatUnsignedNumber(Math.abs(total),numChar,2,"");
    }


    public void setSaldo_deudor_no_vencido(Trade trade, int numChar, JDate valDate, PricingEnv pricingEnv) {
        Double amount = 0.0;
        String direction = "-";
        if(null!=trade && trade.getProduct()!=null){
            amount = AnacreditUtilities.convertToEUR(trade.getProduct().getPrincipal(),trade.getTradeCurrency(), valDate, pricingEnv);
        }
        if(amount==0.0){
            direction = "+";
        }

        this.saldo_deudor_no_vencido = direction+ AnacreditFormatter.formatUnsignedNumber(Math.abs(amount),numChar,2,"");
    }

    public void setSaldo_deudor_no_vencido(double value) {
        String direction = "-";
        if (Double.compare(value, 0.0) > 0) {
            direction = "+";
        }
        this.saldo_deudor_no_vencido = direction+ AnacreditFormatter.formatUnsignedNumber(Math.abs(value), 15,2,"");


    }

    public String getSaldo_deudor_vencido() {
        return saldo_deudor_vencido;
    }

    public void setSaldo_deudor_vencido(String saldo_deudor_vencido) {
        this.saldo_deudor_vencido = saldo_deudor_vencido;
    }

    public String getProductos_vencidos() {
        return productos_vencidos;
    }

    public void setProductos_vencidos(String productos_vencidos) {
        this.productos_vencidos = productos_vencidos;
    }

    public String getIntereses_demora_consolidados() {
        return intereses_demora_consolidados;
    }

    public void setIntereses_demora_consolidados(String intereses_demora_consolidados) {
        this.intereses_demora_consolidados = intereses_demora_consolidados;
    }

    public String getIntereses_devengados() {
        return intereses_devengados;
    }

    public void setIntereses_devengados(Double intereses_devengados) { //TODO en funcion del TEDR signo invertido?
        String direction = "+";
        if(this.tedr.contains("+") && intereses_devengados!=0.0){
            direction = "-";
        }else if(this.tedr.contains("-")){
            direction = "+";
        }

        this.intereses_devengados = direction+ AnacreditFormatter.formatUnsignedNumber(intereses_devengados,15,2,"");
    }

    public String getId_persona_contraparte_direct() {
        return id_persona_contraparte_direct;
    }

    public void setId_persona_contraparte_direct(String jminorista) {
        this.id_persona_contraparte_direct = AnacreditFormatter.formatStringWithBlankOnRight(jminorista,50);
    }

    public String getValor_residual_comp_terceros() {
        return valor_residual_comp_terceros;
    }

    public void setValor_residual_comp_terceros(String valor_residual_comp_terceros) {
        this.valor_residual_comp_terceros = valor_residual_comp_terceros;
    }

    public String getCotiza() {
        return cotiza;
    }

    public void setCotiza(String cotiza) {
        this.cotiza = cotiza;
    }

    public String getEntidad_depositaria() {
        return entidad_depositaria;
    }

    public void setEntidad_depositaria(String entidad_depositaria) {
        this.entidad_depositaria = entidad_depositaria;
    }

    public String getDividendos_devengados_mes() {
        return dividendos_devengados_mes;
    }

    public void setDividendos_devengados_mes(String dividendos_devengados_mes) {
        this.dividendos_devengados_mes = dividendos_devengados_mes;
    }

    public String getSaldo_contingente() {
        return saldo_contingente;
    }

    public Double getSaldo_contingente_d() {
        return saldo_contingente_d;
    }
    public void setSaldo_contingente_d(double saldo_contg_d) {
        this.saldo_contingente_d = saldo_contg_d;
    }

    public void setSaldo_contingente(double saldo_contg) {
        String direction = "+";
        if(this.tedr.contains("+") && saldo_contg!=0.0){
            direction = "-";
        }   else if(this.tedr.contains("-")){
            direction = "+";
        }
        this.saldo_contingente_d = saldo_contg;
        this.saldo_contingente = direction+ AnacreditFormatter.formatUnsignedNumber(saldo_contg,15,2,"");
    }

    public void setSaldo_contingente_eur(Double saldo_contingente_eur) {
        this.saldo_contingente_eur = saldo_contingente_eur;
    }

    public Double getSaldo_contingente_eur() {
        return saldo_contingente_eur;
    }

    public void setSaldo_contingente(String saldo_contingente) {
        this.saldo_contingente = saldo_contingente;
    }


    public String getJerarquia_valor_razonable() {
        return jerarquia_valor_razonable;
    }

    public void setJerarquia_valor_razonable(String jerarquia_valor_razonable) {
        this.jerarquia_valor_razonable = jerarquia_valor_razonable;
    }

    public String getCobertura_riesgo_normal() {
        return cobertura_riesgo_normal;
    }

    public void setCobertura_riesgo_normal(String cobertura_riesgo_normal) {
        this.cobertura_riesgo_normal = cobertura_riesgo_normal;
    }

    public String getIntereses_devengados_dudosos() {
        return intereses_devengados_dudosos;
    }

    public void setIntereses_devengados_dudosos(String intereses_devengados_dudosos) {
        this.intereses_devengados_dudosos = intereses_devengados_dudosos;
    }

    public String getValor_razonable() {
        return valor_razonable;
    }

    public void setValor_razonable(String valor_razonable) {
        this.valor_razonable = valor_razonable;
    }

    public String getSaneamientos_directos_mes() {
        return saneamientos_directos_mes;
    }

    public void setSaneamientos_directos_mes(String saneamientos_directos_mes) {
        this.saneamientos_directos_mes = saneamientos_directos_mes;
    }

    public String getIntereses_cobrados_mes() {
        return intereses_cobrados_mes;
    }

    public void setIntereses_cobrados_mes(String intereses_cobrados_mes) {
        this.intereses_cobrados_mes = intereses_cobrados_mes;
    }

    public String getNumero_ocurrencias() {
        return numero_ocurrencias;
    }

    public void setNumero_ocurrencias(String numero_ocurrencias) {
        this.numero_ocurrencias = numero_ocurrencias;
    }

    public String getPorcentaje_partici_capital() {
        return porcentaje_partici_capital;
    }

    public void setPorcentaje_partici_capital(String porcentaje_partici_capital) {
        this.porcentaje_partici_capital = porcentaje_partici_capital;
    }

    public String getSaldo_contingente_dudoso() {
        return saldo_contingente_dudoso;
    }

    public void setSaldo_contingente_dudoso(String saldo_contingente_dudoso) {
        this.saldo_contingente_dudoso = saldo_contingente_dudoso;
    }

    public String getSaldo_contingente_vencido() {
        return saldo_contingente_vencido;
    }

    public void setSaldo_contingente_vencido(String saldo_contingente_vencido) {
        this.saldo_contingente_vencido = saldo_contingente_vencido;
    }

    public String getSaldo_contingente_dudoso_venc() {
        return saldo_contingente_dudoso_venc;
    }

    public void setSaldo_contingente_dudoso_venc(String saldo_contingente_dudoso_venc) {
        this.saldo_contingente_dudoso_venc = saldo_contingente_dudoso_venc;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getGastos_activados() {
        return gastos_activados;
    }

    public void setGastos_activados(String gastos_activados) {
        this.gastos_activados = gastos_activados;
    }

    public String getResto_valor_contable_activos() {
        return resto_valor_contable_activos;
    }

    public void setResto_valor_contable_activos(String resto_valor_contable_activos) {
        this.resto_valor_contable_activos = resto_valor_contable_activos;
    }

    public String getSaldo_acreedor() {
        return saldo_acreedor;
    }

    public void setSaldo_acreedor(String saldo_acreedor) {
        this.saldo_acreedor = saldo_acreedor;
    }

    public String getPasivos_valor_razonable() {
        return pasivos_valor_razonable;
    }

    public void setPasivos_valor_razonable(String pasivos_valor_razonable) {
        this.pasivos_valor_razonable = pasivos_valor_razonable;
    }

    public String getMicro_cobertura() {
        return micro_cobertura;
    }

    public void setMicro_cobertura(String micro_cobertura) {
        this.micro_cobertura = micro_cobertura;
    }

    public String getPrima_descnto_adquis_asuncion() {
        return prima_descnto_adquis_asuncion;
    }

    public void setPrima_descnto_adquis_asuncion(String prima_descnto_adquis_asuncion) {
        this.prima_descnto_adquis_asuncion = prima_descnto_adquis_asuncion;
    }

    public String getActivos_valor_razobl() {
        return activos_valor_razobl;
    }

    public void setActivos_valor_razobl(String activos_valor_razobl) {
        this.activos_valor_razobl = activos_valor_razobl;
    }

    public String getValor_residual_no_garantizado() {
        return valor_residual_no_garantizado;
    }

    public void setValor_residual_no_garantizado(String valor_residual_no_garantizado) {
        this.valor_residual_no_garantizado = valor_residual_no_garantizado;
    }

    public String getImporte_liq_compras_mes() {
        return importe_liq_compras_mes;
    }

    public void setImporte_liq_compras_mes(String importe_liq_compras_mes) {
        this.importe_liq_compras_mes = importe_liq_compras_mes;
    }

    public String getImporte_liq_ventas_mes() {
        return importe_liq_ventas_mes;
    }

    public void setImporte_liq_ventas_mes(String importe_liq_ventas_mes) {
        this.importe_liq_ventas_mes = importe_liq_ventas_mes;
    }

    public String getCobert_perdidas_partcip_mes() {
        return cobert_perdidas_partcip_mes;
    }

    public void setCobert_perdidas_partcip_mes(String cobert_perdidas_partcip_mes) {
        this.cobert_perdidas_partcip_mes = cobert_perdidas_partcip_mes;
    }

    public String getCorrecciones_valor_valores() {
        return correcciones_valor_valores;
    }

    public void setCorrecciones_valor_valores(String correcciones_valor_valores) {
        this.correcciones_valor_valores = correcciones_valor_valores;
    }

    public String getTipo_codigo_valor() {
        return tipo_codigo_valor;
    }

    public void setTipo_codigo_valor(String tipo_codigo_valor) {
        this.tipo_codigo_valor = tipo_codigo_valor;
    }

    public String getProducto_subproducto() {
        return producto_subproducto;
    }

    public void setProducto_subproducto(String producto_subproducto) {
        this.producto_subproducto = producto_subproducto;
    }

    public String getSaldo_deudor_dudoso_no_vencid() {
        return saldo_deudor_dudoso_no_vencid;
    }

    public void setSaldo_deudor_dudoso_no_vencid(String saldo_deudor_dudoso_no_vencid) {
        this.saldo_deudor_dudoso_no_vencid = saldo_deudor_dudoso_no_vencid;
    }

    public String getSaldo_deudor_dudoso_vencido() {
        return saldo_deudor_dudoso_vencido;
    }

    public void setSaldo_deudor_dudoso_vencido(String saldo_deudor_dudoso_vencido) {
        this.saldo_deudor_dudoso_vencido = saldo_deudor_dudoso_vencido;
    }

    public String getProductos_vencidos_dudosos() {
        return productos_vencidos_dudosos;
    }

    public void setProductos_vencidos_dudosos(String productos_vencidos_dudosos) {
        this.productos_vencidos_dudosos = productos_vencidos_dudosos;
    }

    public String getIntereses_demora_consol_dudos() {
        return intereses_demora_consol_dudos;
    }

    public void setIntereses_demora_consol_dudos(String intereses_demora_consol_dudos) {
        this.intereses_demora_consol_dudos = intereses_demora_consol_dudos;
    }

    public String getResto_valor_cont_actvos_dudos() {
        return resto_valor_cont_actvos_dudos;
    }

    public void setResto_valor_cont_actvos_dudos(String resto_valor_cont_actvos_dudos) {
        this.resto_valor_cont_actvos_dudos = resto_valor_cont_actvos_dudos;
    }

    public String getClasificacion_oper_b3() {
        return clasificacion_oper_b3;
    }

    public void setClasificacion_oper_b3(String clasificacion_oper_b3) {
        this.clasificacion_oper_b3 = clasificacion_oper_b3;
    }

    public String getContrato_renegociado() {
        return contrato_renegociado;
    }

    public void setContrato_renegociado(String contrato_renegociado) {
        this.contrato_renegociado = contrato_renegociado;
    }

    public String getFecha_final_carencia_principal() {
        return fecha_final_carencia_principal;
    }

    public void setFecha_final_carencia_principal(String fecha_final_carencia_principal) {
        this.fecha_final_carencia_principal = fecha_final_carencia_principal;
    }

    public String getId_persona_contraparte_directa_cir() {
        return id_persona_contraparte_directa_cir;
    }

    public void setId_persona_contraparte_directa_cir(String id_persona_contraparte_directa_cir) {
        this.id_persona_contraparte_directa_cir = id_persona_contraparte_directa_cir;
    }

    public String getTedr_venc() {
        return tedr_venc;
    }

    public void setTedr_venc(String tedr_venc) {
        this.tedr_venc = tedr_venc;
    }

    public String getTedr_desc_exc() {
        return tedr_desc_exc;
    }

    public void setTedr_desc_exc(String tedr_desc_exc) {
        this.tedr_desc_exc = tedr_desc_exc;
    }

    public String getFiller() {
        return filler;
    }

    public void setFiller(String filler) {
        this.filler = filler;
    }

    public String getFecha_formalicacion() {
        return fecha_formalicacion;
    }

    public void setFecha_formalizacion(String fecha_formalicacion) {
        this.fecha_formalicacion = fecha_formalicacion;
    }

    public String getSindicato() {
        return sindicato;
    }

    public void setSindicato(String sindicato) {
        this.sindicato = sindicato;
    }

    public String getTipo_interes_nominal() {
        return tipo_interes_nominal;
    }

    public void setTipo_interes_nominal(String tipo_interes_nominal) {
        this.tipo_interes_nominal = tipo_interes_nominal;
    }

    public String getTipo_interes_subvencionado() {
        return tipo_interes_subvencionado;
    }

    public void setTipo_interes_subvencionado(String tipo_interes_subvencionado) {
        this.tipo_interes_subvencionado = tipo_interes_subvencionado;
    }

    public String getTipo_interes_demora() {
        return tipo_interes_demora;
    }

    public void setTipo_interes_demora(String tipo_interes_demora) {
        this.tipo_interes_demora = tipo_interes_demora;
    }

    public String getCom_comp_costes_concesion() {
        return com_comp_costes_concesion;
    }

    public void setCom_comp_costes_concesion(String com_comp_costes_concesion) {
        this.com_comp_costes_concesion = com_comp_costes_concesion;
    }

    public String getClase_riesgo_contraparte_directa() {
        return clase_riesgo_contraparte_directa;
    }

    public void setClase_riesgo_contraparte_directa(String clase_riesgo_contraparte_directa) {
        this.clase_riesgo_contraparte_directa = clase_riesgo_contraparte_directa;
    }

    public String getRiesgo_exc_cob_riesgo_pais() {
        return riesgo_exc_cob_riesgo_pais;
    }

    public void setRiesgo_exc_cob_riesgo_pais(String riesgo_exc_cob_riesgo_pais) {
        this.riesgo_exc_cob_riesgo_pais = riesgo_exc_cob_riesgo_pais;
    }

    public String getTipo_cartera() {
        return tipo_cartera;
    }

    public void setTipo_cartera(String tipo_cartera) {
        this.tipo_cartera = tipo_cartera;
    }

    public String getInd_op_no_corriente() {
        return ind_op_no_corriente;
    }

    public void setInd_op_no_corriente(String ind_op_no_corriente) {
        this.ind_op_no_corriente = ind_op_no_corriente;
    }

    public String getVinculacion_mercado_hipotecario() {
        return vinculacion_mercado_hipotecario;
    }

    public void setVinculacion_mercado_hipotecario(String vinculacion_mercado_hipotecario) {
        this.vinculacion_mercado_hipotecario = vinculacion_mercado_hipotecario;
    }

    public String getValor_corregido_garantia() {
        return valor_corregido_garantia;
    }

    public void setValor_corregido_garantia(String valor_corregido_garantia) {
        this.valor_corregido_garantia = valor_corregido_garantia;
    }

    public String getDisponible_subrogacion_terceros() {
        return disponible_subrogacion_terceros;
    }

    public void setDisponible_subrogacion_terceros(String disponible_subrogacion_terceros) {
        this.disponible_subrogacion_terceros = disponible_subrogacion_terceros;
    }

    public String getLtv() {
        return ltv;
    }

    public void setLtv(String ltv) {
        this.ltv = ltv;
    }

    public String getDividendos_cobrados_mes() {
        return dividendos_cobrados_mes;
    }

    public void setDividendos_cobrados_mes(String dividendos_cobrados_mes) {
        this.dividendos_cobrados_mes = dividendos_cobrados_mes;
    }

    public String getTipo_derivado() {
        return tipo_derivado;
    }

    public void setTipo_derivado(String tipo_derivado) {
        this.tipo_derivado = tipo_derivado;
    }

    public String getDerivados_tipo_mercado() {
        return derivados_tipo_mercado;
    }

    public void setDerivados_tipo_mercado(String derivados_tipo_mercado) {
        this.derivados_tipo_mercado = derivados_tipo_mercado;
    }

    public String getIntereses_devengados_mes() {
        return intereses_devengados_mes;
    }

    public void setIntereses_devengados_mes(String intereses_devengados_mes) {
        this.intereses_devengados_mes = intereses_devengados_mes;
    }

    public String getEstado_refin_reest() {
        return estado_refin_reest;
    }

    public void setEstado_refin_reest(String estado_refin_reest) {
        this.estado_refin_reest = estado_refin_reest;
    }

    public String getCompra_o_venta() {
        return compra_o_venta;
    }

    public void setCompra_o_venta(String compra_o_venta) {
        this.compra_o_venta = compra_o_venta;
    }

    public String getPasivos_valor_razonable_riesgo_credito() {
        return pasivos_valor_razonable_riesgo_credito;
    }

    public void setPasivos_valor_razonable_riesgo_credito(String pasivos_valor_razonable_riesgo_credito) {
        this.pasivos_valor_razonable_riesgo_credito = pasivos_valor_razonable_riesgo_credito;
    }

    public String getId_persona_agente_observado() {
        return id_persona_agente_observado;
    }

    public void setId_persona_agente_observado(String id_persona_agente_observado) {
        this.id_persona_agente_observado = id_persona_agente_observado;
    }

    public String getId_persona_contraparte_solvencia() {
        return id_persona_contraparte_solvencia;
    }

    public void setId_persona_contraparte_solvencia(String id_persona_contraparte_solvencia) {
        this.id_persona_contraparte_solvencia = id_persona_contraparte_solvencia;
    }

    public String getOperacion_adquirida() {
        return operacion_adquirida;
    }

    public void setOperacion_adquirida(String operacion_adquirida) {
        this.operacion_adquirida = operacion_adquirida;
    }

    public String getOp_no_orig_gest_terc() {
        return op_no_orig_gest_terc;
    }

    public void setOp_no_orig_gest_terc(String op_no_orig_gest_terc) {
        this.op_no_orig_gest_terc = op_no_orig_gest_terc;
    }

    public String getTipo_correccion_valor_deterioro() {
        return tipo_correccion_valor_deterioro;
    }

    public void setTipo_correccion_valor_deterioro(String tipo_correccion_valor_deterioro) {
        this.tipo_correccion_valor_deterioro = tipo_correccion_valor_deterioro;
    }

    public String getFecha_situacion_operacion_rd() {
        return fecha_situacion_operacion_rd;
    }

    public void setFecha_situacion_operacion_rd(String fecha_situacion_operacion_rd) {
        this.fecha_situacion_operacion_rd = fecha_situacion_operacion_rd;
    }

    public String getFecha_clasificacion_dudosa() {
        return fecha_clasificacion_dudosa;
    }

    public void setFecha_clasificacion_dudosa(String fecha_clasificacion_dudosa) {
        this.fecha_clasificacion_dudosa = fecha_clasificacion_dudosa;
    }

    public String getRc_factor_conversion_orden() {
        return rc_factor_conversion_orden;
    }

    public void setRc_factor_conversion_orden(String rc_factor_conversion_orden) {
        this.rc_factor_conversion_orden = rc_factor_conversion_orden;
    }

    public String getContrato_sindicado1() {
        return contrato_sindicado1;
    }

    public void setContrato_sindicado1(String contrato_sindicado1) {
        this.contrato_sindicado1 = contrato_sindicado1;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getType(){
        return this.type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.id_entidad);
        builder.append(this.fecha_extraccion);
        builder.append(this.id_contrato);
        builder.append(this.id_contrato_interno);
        builder.append(this.aplicacion_origen);
        builder.append(this.id_centro_contable);
        builder.append(this.codigo_valor);
        builder.append(this.declarado_cir_tercera_entidad);
        builder.append(this.pais_negocio);
        builder.append(this.producto_ac);
        builder.append(this.producto_entidad);
        builder.append(this.activo_op_valores);
        builder.append(this.subordinacion_producto_ac);
        builder.append(this.tipo_riesgo_subyacente);
        builder.append(this.finalidad_ac);
        builder.append(this.tramite_recuperacion);
        builder.append(this.principal_inicial);
        builder.append(this.limite_inicial);
        builder.append(this.importe_concedido);
        builder.append(this.fecha_emision_originacion);
        builder.append(this.fecha_vencimiento);
        builder.append(this.fecha_cancelacion);
        builder.append(this.origen_operacion);
        builder.append(this.refinanciacion);
        builder.append(this.subvencion_operacion);
        builder.append(this.canal_contratacion_ac);
        builder.append(this.provincia_negocio);
        builder.append(this.esquema_amort_operaciones_ac);
        builder.append(this.porcent_particip_sindicados);
        builder.append(this.edif_financ_estado);
        builder.append(this.edif_financ_licencia);
        builder.append(this.inmueble_financ_num_viviendas);
        builder.append(this.codigo_promoc_inmob_finan);
        builder.append(this.desc_promoc_inmob_finan);
        builder.append(this.moneda);
        builder.append(this.plazo_residual);
        builder.append(this.tipo_garantia_real_ppal);
        builder.append(this.orden_hipoteca);
        builder.append(this.cobertura_garantia_real);
        builder.append(this.tipo_garant_personal_ppal_ac);
        builder.append(this.cobertura_garantia_personal);
        builder.append(this.fecha_primer_incumplimiento);
        builder.append(this.fecha_ultimo_impago_incumplim);
        builder.append(this.saldo_deudor_no_vencido);
        builder.append(this.saldo_deudor_vencido);
        builder.append(this.productos_vencidos);
        builder.append(this.productos_dudosos_no_consolid);
        builder.append(this.intereses_demora_consolidados);
        builder.append(this.intereses_demora_no_consolid);
        builder.append(this.gastos_activados);
        builder.append(this.resto_valor_contable_activos);
        builder.append(this.saldo_acreedor);
        builder.append(this.limite);
        builder.append(this.disponible_inmediato);
        builder.append(this.disponible_condicionado);
        builder.append(this.ppal_cobrado_mes_amort_corrnt);
        builder.append(this.ppal_cobrado_mes_amort_antici);
        builder.append(this.var_deuda_cond_presc);
        builder.append(this.tipo_act_recib_pago_mes);
        builder.append(this.reduc_deuda_act_recib_pag_mes);
        builder.append(this.tipo_subrogacion_mes);
        builder.append(this.reduc_deuda_subrogacion_mes);
        builder.append(this.tipo_refinanc_mes);
        builder.append(this.reduc_deuda_refinanc_mes);
        builder.append(this.gestion_op_transferida_mes);
        builder.append(this.importe_transferido_mes_ci);
        builder.append(this.fecha_ultimo_vto_intereses);
        builder.append(this.fecha_proximo_vto_intereses);
        builder.append(this.fecha_ultimo_vto_prinicipal);
        builder.append(this.fecha_proximo_vto_principal);
        builder.append(this.num_cuotas_impagadas_ppal);
        builder.append(this.modalidad_tipo_interes_ac);
        builder.append(this.tedr);
        builder.append(this.indice_referencia_ac);
        builder.append(this.suelo_rentabilidad);
        builder.append(this.techo_rentabilidad);
        builder.append(this.diferencial_sobre_indice_refe);
        builder.append(this.id_persona_contraparte_direct);
        builder.append(this.situacion_ope_riesgo_direc_ac);
        builder.append(this.rc_factor_conversion_orden);
        /* builder.append(this.clasificacion_oper_b3);*/
        builder.append(this.cobertura_riesgo_dudoso);
        builder.append(this.cobertura_riesgo_pais);
        builder.append(this.provision_riesgo_dudoso);
        builder.append(this.provision_riesgo_pais);
        builder.append(this.intereses_devengados);
        builder.append(this.comisiones_pendientes_devengo);
        builder.append(this.coste_transaccion);
        builder.append(this.prima_descnto_adquis_asuncion);
        builder.append(this.descuento_por_deterioro);
        builder.append(this.activos_valor_razobl);
        builder.append(this.activos_valor_razobl_rsgo_cre);
        builder.append(this.pasivos_valor_razonable);
        builder.append(this.micro_cobertura);
        builder.append(this.valor_residual_no_garantizado);
        builder.append(this.valor_residual_comp_terceros);
        builder.append(this.cotiza);
        builder.append(this.entidad_depositaria);
        builder.append(this.valor_nominal);
        builder.append(this.jerarquia_valor_razonable);
        builder.append(this.valor_razonable);
        builder.append(this.saneamientos_directos_mes);
        builder.append(this.intereses_cobrados_mes);
        builder.append(this.numero_ocurrencias);
        builder.append(this.porcentaje_partici_capital);
        builder.append(this.importe_liq_compras_mes);
        builder.append(this.importe_liq_ventas_mes);
        builder.append(this.cobert_perdidas_partcip_mes);
        builder.append(this.nocional);
        builder.append(this.correcciones_valor_valores);
        builder.append(this.plazo_origen_m);
        builder.append(this.operacion_no_declarable_cirbe);
        builder.append(this.fecha_renovacion);
        builder.append(this.tipo_codigo_valor);
        builder.append(this.dividendos_devengados_mes);
        builder.append(this.saldo_contingente);
        builder.append(this.principal_vencido_mes_subvenc);
        builder.append(this.tipo_tarjeta);
        builder.append(this.saldo_deudor_excedido_vencido);
        builder.append(this.contrato_renegociado);
        builder.append(this.principal_vdo_refin_pte_amort);
        builder.append(this.fecha_primer_incumpl_op_refin);
        builder.append(this.producto_subproducto);
        builder.append(this.conocimiento_garant_pers_ppal);
        builder.append(this.saldo_deudor_dudoso_no_vencid);
        builder.append(this.saldo_deudor_dudoso_vencido);
        builder.append(this.saldo_deudor_dudoso_exced_ven);
        builder.append(this.productos_vencidos_dudosos);
        builder.append(this.intereses_demora_consol_dudos);
        builder.append(this.resto_valor_cont_actvos_dudos);
        builder.append(this.cobertura_riesgo_normal);
        builder.append(this.intereses_devengados_dudosos);
        builder.append(this.intereses_exced_devengado);
        builder.append(this.intereses_exced_devengado_dud);
        builder.append(this.saldo_contingente_dudoso);
        builder.append(this.saldo_contingente_vencido);
        builder.append(this.saldo_contingente_dudoso_venc);
        builder.append(this.estado_refin_reest_reneg);
        builder.append(this.id_contrato_juridico);
        builder.append(AnacreditFormatter.formatStringWithBlankOnRight("",5)); //V21 show always blank
        builder.append(this.cartera_prudencial);
        builder.append(this.fecha_primera_liquidacion);
        builder.append(this.producto_recurso);
        builder.append(this.instrumento_fiduciario);
        builder.append(this.financiacion_proyecto);
        builder.append(this.import_recuperad_acum_sit_imp);
        builder.append(this.fecha_estado_cumplimiento);
        builder.append(this.fecha_refin_reest);
        builder.append(this.tipo_referencia_vencimiento);
        builder.append(this.codigo_ratio_referencia);
        builder.append(this.tipo_referencia_sustitutivo);
        builder.append(this.frec_revision_tipo_int_per);
        builder.append(this.proxima_revision_tipo_interes);
        builder.append(this.fecha_final_carencia_principal);
        builder.append(this.frecuencia_pago_princ_int);
        builder.append(this.saldo_vivo_nominal);
        builder.append(this.saldo_deudor_fallido_no_venc);
        builder.append(this.saldo_deudor_fallido_vencido);
        builder.append(this.importes_vencidos);
        builder.append(this.productos_fallidos);
        builder.append(this.fallidos_acumulados);
        builder.append(this.intereses_demora_fallidos);
        builder.append(this.gastos_exigibles_fallidos);
        builder.append(this.fase_deterioro);
        builder.append(this.importe_libros_activo);
        builder.append(this.cobertura_acumulada);
        builder.append(this.provision_fuera_balance);
        builder.append(this.act_val_raz_ries_cred_ant_adq);
        builder.append(this.saldo_fuera_balance);
        builder.append(this.derechos_reembolso);
        builder.append(this.tipo_fuente_de_carga);
        builder.append(this.fecha_renegociacion);
        builder.append(this.situacion_impago_operacion);
        builder.append(this.fecha_situacion_impago_ope);
        builder.append(this.reconocimiento_balance);
        builder.append(this.saldo_deudor_excedido_no_venc);
        builder.append(this.saldo_deudor_dudoso_no_vencid);
        builder.append(this.met_calc_cobert_riesgo_normal);
        builder.append(this.met_calc_cobert_riesgo_dudoso);
        builder.append(this.renovacion_automatica);
        builder.append(this.valor_residual_no_garan_dudos);
        builder.append(this.valor_residual_comp_ter_dudos);
        builder.append(this.productos_dudosos_deven_orden);
        builder.append(this.valor_actual_com_garan_conced);
        builder.append(this.provision_riesgo_normal);
        builder.append(this.total_intereses_deven_credito);
        builder.append(this.otras_periodificaciones_activ);
        builder.append(this.fecha_baja_def_cirbe);
        builder.append(this.situacion_operativa);
        builder.append(this.estado_cumplimiento);
        builder.append(this.contrato_sindicado);
        builder.append(this.id_persona_contraparte_directa_cir);
        builder.append(this.tedr_venc);
        builder.append(this.tedr_desc_exc);
        builder.append(this.operacion_titulares_exonerados);
        builder.append(this.fecha_primer_incumplimiento_sin_fall_parc);
        builder.append(this.classif_oper_anejo_ix);
        builder.append(this.fecha_refinanciacion);
        builder.append(this.sin_devengo_interes);
        builder.append(this.fallidos_parciales);
        builder.append(this.entidad_grupo_origen);
        builder.append(this.cuenta_grupo);
        builder.append(this.subcuenta);
        builder.append(this.id_centro_contable_a);
        builder.append(this.moratoria_covid19);
        builder.append(this.fecha_inicio_moratoria_covid19);
        builder.append(this.fecha_fin_moratoria_covid19);

        builder.append(this.fecha_modificacion);
        builder.append(this.modificacion);
        builder.append(this.aumento_plazo);
        builder.append(this.aumento_plazo_fecha);
        builder.append(this.prestamos_participativos);
        builder.append(this.prestamos_participativos_fecha);
        builder.append(this.reduccion_principal);
        builder.append(this.reduccion_principal_fecha);
        builder.append(this.reduccion_principal_importe_avalista);
        builder.append(this.reduccion_principal_avales_ejecutado);
        builder.append(this.originado_con_deterioro);
        builder.append(this.calculo_situacion_impago);
        builder.append(this.filler);
        builder.append(this.version);
        return builder.toString();
    }

    public String getOperacion_titulares_exonerados() {
        return operacion_titulares_exonerados;
    }

    public void setOperacion_titulares_exonerados(String operacion_titulares_exonerados) {
        this.operacion_titulares_exonerados = operacion_titulares_exonerados;
    }

    public String getFecha_primer_incumplimiento_sin_fall_parc() {
        return fecha_primer_incumplimiento_sin_fall_parc;
    }

    public void setFecha_primer_incumplimiento_sin_fall_parc(String fecha_primer_incumplimiento_sin_fall_parc) {
        this.fecha_primer_incumplimiento_sin_fall_parc = fecha_primer_incumplimiento_sin_fall_parc;
    }

    public String getClassif_oper_anejo_ix() {
        return classif_oper_anejo_ix;
    }

    public void setClassif_oper_anejo_ix(String classif_oper_anejo_ix) {
        this.classif_oper_anejo_ix = classif_oper_anejo_ix;
    }

    public String getFecha_refinanciacion() {
        return fecha_refinanciacion;
    }

    public void setFecha_refinanciacion(String fecha_refinanciacion) {
        this.fecha_refinanciacion = fecha_refinanciacion;
    }

    public String getSin_devengo_interes() {
        return sin_devengo_interes;
    }

    private void setSin_devengo_interes(String sin_devengo_interes) {
        this.sin_devengo_interes = sin_devengo_interes;
    }

    public String getFallidos_parciales() {
        return fallidos_parciales;
    }

    public void setFallidos_parciales(String fallidos_parciales) {
        this.fallidos_parciales = fallidos_parciales;
    }

    public String getEntidad_grupo_origen() {
        return entidad_grupo_origen;
    }

    public void setEntidad_grupo_origen(String entidad_grupo_origen) {
        this.entidad_grupo_origen = entidad_grupo_origen;
    }

    public String getCuenta_grupo() {
        return cuenta_grupo;
    }

    public void setCuenta_grupo(String cuenta_grupo) {
        this.cuenta_grupo = cuenta_grupo;
    }

    public String getSubcuenta() {
        return subcuenta;
    }

    public void setSubcuenta(String subcuenta) {
        this.subcuenta = subcuenta;
    }

    public String getId_centro_contable_a() {
        return id_centro_contable_a;
    }

    public void setId_centro_contable_a(String id_centro_contable_a) {
        this.id_centro_contable_a = id_centro_contable_a;
    }

    public String getMoratoria_covid19() {
        return moratoria_covid19;
    }

    public void setMoratoria_covid19(String moratoria_covid19) {
        this.moratoria_covid19 = moratoria_covid19;
    }

    public String getFecha_inicio_moratoria_covid19() {
        return fecha_inicio_moratoria_covid19;
    }

    public void setFecha_inicio_moratoria_covid19(String fecha_inicio_moratoria_covid19) {
        this.fecha_inicio_moratoria_covid19 = fecha_inicio_moratoria_covid19;
    }

    public String getFecha_fin_moratoria_covid19() {
        return fecha_fin_moratoria_covid19;
    }

    public void setFecha_fin_moratoria_covid19(String fecha_fin_moratoria_covid19) {
        this.fecha_fin_moratoria_covid19 = fecha_fin_moratoria_covid19;
    }
}
