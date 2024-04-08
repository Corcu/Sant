package calypsox.tk.report;

import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditInventoryOperReportStyle extends BOSecurityPositionReportStyle {

    public static final String ID_ENTIDAD = "ID_ENTIDAD";
    public static final String FECHA_EXTRACCION = "FECHA_EXTRACCION";
    public static final String ID_CONTRATO = "ID_CONTRATO";
    public static final String APLICACION_ORIGEN = "APLICACION_ORIGEN";
    public static final String ID_CENTRO_CONTABLE = "ID_CENTRO_CONTABLE";
    public static final String CODIGO_VALOR = "CODIGO_VALOR";
    public static final String ID_CONTRATO_INTERNO = "ID_CONTRATO_INTERNO";
    public static final String DECLARADO_CIR_TERCERA_ENTIDAD = "DECLARADO_CIR_TERCERA_ENTIDAD";
    public static final String PAIS_NEGOCIO = "PAIS_NEGOCIO";
    public static final String PRODUCTO_AC = "PRODUCTO_AC";
    public static final String PRODUCTO_ENTIDAD = "PRODUCTO_ENTIDAD";
    public static final String ACTIVO_OP_VALORES = "ACTIVO_OP_VALORES";
    public static final String SUBORDINACION_PRODUCTO_AC = "SUBORDINACION_PRODUCTO_AC";
    public static final String TIPO_RIESGO_SUBYACENTE = "TIPO_RIESGO_SUBYACENTE";
    public static final String FINALIDAD_AC = "FINALIDAD_AC";
    public static final String TRAMITE_RECUPERACION = "TRAMITE_RECUPERACION";
    public static final String PRINCIPAL_INICIAL = "PRINCIPAL_INICIAL";
    public static final String LIMITE_INICIAL = "LIMITE_INICIAL";
    public static final String IMPORTE_CONCEDIDO = "IMPORTE_CONCEDIDO";
    public static final String FECHA_EMISION_ORIGINACION = "FECHA_EMISION_ORIGINACION";
    public static final String FECHA_VENCIMIENTO = "FECHA_VENCIMIENTO";
    public static final String ORIGEN_OPERACION = "ORIGEN_OPERACION";
    public static final String REFINANCIACION = "REFINANCIACION";
    public static final String SUBVENCION_OPERACION = "SUBVENCION_OPERACION";
    public static final String CANAL_CONTRATACION_AC = "CANAL_CONTRATACION_AC";
    public static final String PROVINCIA_NEGOCIO = "PROVINCIA_NEGOCIO";
    public static final String ESQUEMA_AMORT_OPERACIONES_AC = "ESQUEMA_AMORT_OPERACIONES_AC";
    public static final String PORCENT_PARTICIP_SINDICADOS = "PORCENT_PARTICIP_SINDICADOS";
    public static final String EDIF_FINANC_ESTADO = "EDIF_FINANC_ESTADO";
    public static final String EDIF_FINANC_LICENCIA = "EDIF_FINANC_LICENCIA";
    public static final String INMUEBLE_FINANC_NUM_VIVIENDAS = "INMUEBLE_FINANC_NUM_VIVIENDAS";
    public static final String CODIGO_PROMOC_INMOB_FINAN = "CODIGO_PROMOC_INMOB_FINAN";
    public static final String DESC_PROMOC_INMOB_FINAN = "DESC_PROMOC_INMOB_FINAN";
    public static final String PLAZO_RESIDUAL = "PLAZO_RESIDUAL";
    public static final String TIPO_GARANTIA_REAL_PPAL = "TIPO_GARANTIA_REAL_PPAL";
    public static final String ORDEN_HIPOTECA = "ORDEN_HIPOTECA";
    public static final String COBERTURA_GARANTIA_REAL = "COBERTURA_GARANTIA_REAL";
    public static final String TIPO_GARANT_PERSONAL_PPAL_AC = "TIPO_GARANT_PERSONAL_PPAL_AC";
    public static final String COBERTURA_GARANTIA_PERSONAL = "COBERTURA_GARANTIA_PERSONAL";
    public static final String SITUACION_OPERATIVA = "SITUACION_OPERATIVA";
    public static final String FECHA_PRIMER_INCUMPLIMIENTO = "FECHA_PRIMER_INCUMPLIMIENTO" ;
    public static final String FECHA_ULTIMO_IMPAGO_INCUMPLIM = "FECHA_ULTIMO_IMPAGO_INCUMPLIM" ;
    public static final String PRODUCTOS_DUDOSOS_NO_CONSOLID = "PRODUCTOS_DUDOSOS_NO_CONSOLID" ;
    public static final String INTERESES_DEMORA_NO_CONSOLID = "INTERESES_DEMORA_NO_CONSOLID" ;
    public static final String LIMITE = "LIMITE" ;
    public static final String DISPONIBLE_INMEDIATO = "DISPONIBLE_INMEDIATO" ;
    public static final String DISPONIBLE_CONDICIONADO = "DISPONIBLE_CONDICIONADO" ;
    public static final String PPAL_COBRADO_MES_AMORT_CORRNT = "PPAL_COBRADO_MES_AMORT_CORRNT" ;
    public static final String PPAL_COBRADO_MES_AMORT_ANTICI = "PPAL_COBRADO_MES_AMORT_ANTICI" ;
    public static final String VAR_DEUDA_COND_PRESC = "VAR_DEUDA_COND_PRESC" ;
    public static final String TIPO_ACT_RECIB_PAGO_MES = "TIPO_ACT_RECIB_PAGO_MES" ;
    public static final String REDUC_DEUDA_ACT_RECIB_PAG_MES = "REDUC_DEUDA_ACT_RECIB_PAG_MES" ;
    public static final String TIPO_SUBROGACION_MES = "TIPO_SUBROGACION_MES" ;
    public static final String REDUC_DEUDA_SUBROGACION_MES = "REDUC_DEUDA_SUBROGACION_MES" ;
    public static final String TIPO_REFINANC_MES = "TIPO_REFINANC_MES" ;
    public static final String REDUC_DEUDA_REFINANC_MES = "REDUC_DEUDA_REFINANC_MES" ;
    public static final String GESTION_OP_TRANSFERIDA_MES = "GESTION_OP_TRANSFERIDA_MES" ;
    public static final String IMPORTE_TRANSFERIDO_MES_CI = "IMPORTE_TRANSFERIDO_MES_CI" ;
    public static final String FECHA_ULTIMO_VTO_INTERESES = "FECHA_ULTIMO_VTO_INTERESES" ;
    public static final String FECHA_PROXIMO_VTO_INTERESES = "FECHA_PROXIMO_VTO_INTERESES" ;
    public static final String FECHA_ULTIMO_VTO_PRINICIPAL = "FECHA_ULTIMO_VTO_PRINICIPAL" ;
    public static final String FECHA_PROXIMO_VTO_PRINCIPAL = "FECHA_PROXIMO_VTO_PRINCIPAL" ;
    public static final String NUM_CUOTAS_IMPAGADAS_PPAL = "NUM_CUOTAS_IMPAGADAS_PPAL" ;
    public static final String MODALIDAD_TIPO_INTERES_AC = "MODALIDAD_TIPO_INTERES_AC" ;
    public static final String TEDR = "TEDR" ;
    public static final String INDICE_REFERENCIA_AC = "INDICE_REFERENCIA_AC" ;
    public static final String SUELO_RENTABILIDAD = "SUELO_RENTABILIDAD" ;
    public static final String TECHO_RENTABILIDAD = "TECHO_RENTABILIDAD" ;
    public static final String DIFERENCIAL_SOBRE_INDICE_REFE = "DIFERENCIAL_SOBRE_INDICE_REFE" ;
    public static final String SITUACION_OPE_RIESGO_DIREC_AC = "SITUACION_OPE_RIESGO_DIREC_AC" ;
    public static final String COBERTURA_RIESGO_DUDOSO = "COBERTURA_RIESGO_DUDOSO" ;
    public static final String COBERTURA_RIESGO_PAIS = "COBERTURA_RIESGO_PAIS" ;
    public static final String PROVISION_RIESGO_DUDOSO = "PROVISION_RIESGO_DUDOSO" ;
    public static final String PROVISION_RIESGO_PAIS = "PROVISION_RIESGO_PAIS" ;
    public static final String COMISIONES_PENDIENTES_DEVENGO = "COMISIONES_PENDIENTES_DEVENGO" ;
    public static final String COSTE_TRANSACCION = "COSTE_TRANSACCION" ;
    public static final String DESCUENTO_POR_DETERIORO = "DESCUENTO_POR_DETERIORO" ;
    public static final String ACTIVOS_VALOR_RAZOBL_RSGO_CRE = "ACTIVOS_VALOR_RAZOBL_RSGO_CRE" ;
    public static final String VALOR_NOMINAL = "VALOR_NOMINAL" ;
    public static final String NOCIONAL = "NOCIONAL" ;
    public static final String IMPORTE_PENDIENTE_DESEMBOLSO = "IMPORTE_PENDIENTE_DESEMBOLSO" ;
    public static final String PLAZO_ORIGEN_M = "PLAZO_ORIGEN_M" ;
    public static final String OPERACION_NO_DECLARABLE_CIRBE = "OPERACION_NO_DECLARABLE_CIRBE" ;
    public static final String FECHA_RENOVACION = "FECHA_RENOVACION" ;
    public static final String PRINCIPAL_VENCIDO_MES_SUBVENC = "PRINCIPAL_VENCIDO_MES_SUBVENC" ;
    public static final String ESTADO_CUMPLIMIENTO = "ESTADO_CUMPLIMIENTO" ;
    public static final String TIPO_TARJETA = "TIPO_TARJETA" ;
    public static final String SALDO_DEUDOR_EXCEDIDO_VENCIDO = "SALDO_DEUDOR_EXCEDIDO_VENCIDO";
    public static final String PRINCIPAL_VDO_REFIN_PTE_AMORT = "PRINCIPAL_VDO_REFIN_PTE_AMORT" ;
    public static final String FECHA_PRIMER_INCUMPL_OP_REFIN = "FECHA_PRIMER_INCUMPL_OP_REFIN" ;
    public static final String CONOCIMIENTO_GARANT_PERS_PPAL = "CONOCIMIENTO_GARANT_PERS_PPAL" ;
    public static final String SALDO_DEUDOR_DUDOSO_EXCED_VEN = "SALDO_DEUDOR_DUDOSO_EXCED_VEN" ;
    public static final String INTERESES_EXCED_DEVENGADO = "INTERESES_EXCED_DEVENGADO" ;
    public static final String INTERESES_EXCED_DEVENGADO_DUD = "INTERESES_EXCED_DEVENGADO_DUD" ;
    public static final String ESTADO_REFIN_REEST_RENEG = "ESTADO_REFIN_REEST_RENEG" ;
    public static final String ID_CONTRATO_JURIDICO = "ID_CONTRATO_JURIDICO" ;
    public static final String TIPO_CARTERA_IFRS9 = "TIPO_CARTERA_IFRS9" ;
    public static final String CARTERA_PRUDENCIAL = "CARTERA_PRUDENCIAL" ;
    public static final String FECHA_PRIMERA_LIQUIDACION = "FECHA_PRIMERA_LIQUIDACION" ;
    public static final String PRODUCTO_RECURSO = "PRODUCTO_RECURSO" ;
    public static final String INSTRUMENTO_FIDUCIARIO = "INSTRUMENTO_FIDUCIARIO" ;
    public static final String FINANCIACION_PROYECTO = "FINANCIACION_PROYECTO" ;
    public static final String IMPORT_RECUPERAD_ACUM_SIT_IMP = "IMPORT_RECUPERAD_ACUM_SIT_IMP" ;
    public static final String FECHA_ESTADO_CUMPLIMIENTO = "FECHA_ESTADO_CUMPLIMIENTO" ;
    public static final String FECHA_REFIN_REEST = "FECHA_REFIN_REEST" ;
    public static final String TIPO_REFERENCIA_VENCIMIENTO = "TIPO_REFERENCIA_VENCIMIENTO" ;
    public static final String CODIGO_RATIO_REFERENCIA = "CODIGO_RATIO_REFERENCIA" ;
    public static final String TIPO_REFERENCIA_SUSTITUTIVO = "TIPO_REFERENCIA_SUSTITUTIVO" ;
    public static final String FREC_REVISION_TIPO_INT_PER = "FREC_REVISION_TIPO_INT_PER" ;
    public static final String PROXIMA_REVISION_TIPO_INTERES = "PROXIMA_REVISION_TIPO_INTERES" ;
    public static final String FECHA_FINAL_PERIODO_INTERESES = "FECHA_FINAL_PERIODO_INTERESES" ;
    public static final String FRECUENCIA_PAGO_PRINC_INT = "FRECUENCIA_PAGO_PRINC_INT" ;
    public static final String SALDO_VIVO_NOMINAL = "SALDO_VIVO_NOMINAL" ;
    public static final String SALDO_DEUDOR_FALLIDO_NO_VENC = "SALDO_DEUDOR_FALLIDO_NO_VENC" ;
    public static final String SALDO_DEUDOR_FALLIDO_VENCIDO = "SALDO_DEUDOR_FALLIDO_VENCIDO" ;
    public static final String IMPORTES_VENCIDOS = "IMPORTES_VENCIDOS";
    public static final String PRODUCTOS_FALLIDOS = "PRODUCTOS_FALLIDOS";
    public static final String FALLIDOS_ACUMULADOS = "FALLIDOS_ACUMULADOS";
    public static final String INTERESES_DEMORA_FALLIDOS = "INTERESES_DEMORA_FALLIDOS";
    public static final String GASTOS_EXIGIBLES_FALLIDOS = "GASTOS_EXIGIBLES_FALLIDOS";
    public static final String FASE_DETERIORO = "FASE_DETERIORO";
    public static final String IMPORTE_LIBROS_ACTIVO = "IMPORTE_LIBROS_ACTIVO";
    public static final String COBERTURA_ACUMULADA = "COBERTURA_ACUMULADA";
    public static final String PROVISION_FUERA_BALANCE = "PROVISION_FUERA_BALANCE";
    public static final String ACT_VAL_RAZ_RIES_CRED_ANT_ADQ = "ACT_VAL_RAZ_RIES_CRED_ANT_ADQ";
    public static final String SALDO_FUERA_BALANCE = "SALDO_FUERA_BALANCE";
    public static final String DERECHOS_REEMBOLSO = "DERECHOS_REEMBOLSO";
    public static final String TIPO_FUENTE_DE_CARGA = "TIPO_FUENTE_DE_CARGA";
    public static final String FECHA_RENEGOCIACION = "FECHA_RENEGOCIACION";
    public static final String SITUACION_IMPAGO_OPERACION = "SITUACION_IMPAGO_OPERACION";
    public static final String FECHA_SITUACION_IMPAGO_OPE = "FECHA_SITUACION_IMPAGO_OPE";
    public static final String RECONOCIMIENTO_BALANCE = "RECONOCIMIENTO_BALANCE";
    public static final String SALDO_DEUDOR_EXCEDIDO_NO_VENC = "SALDO_DEUDOR_EXCEDIDO_NO_VENC";
    public static final String SALDO_DEUDOR_DUD_EXCE_NO_VENC = "SALDO_DEUDOR_DUD_EXCE_NO_VENC";
    public static final String MET_CALC_COBERT_RIESGO_NORMAL = "MET_CALC_COBERT_RIESGO_NORMAL";
    public static final String MET_CALC_COBERT_RIESGO_DUDOSO = "MET_CALC_COBERT_RIESGO_DUDOSO";
    public static final String RENOVACION_AUTOMATICA = "RENOVACION_AUTOMATICA";
    public static final String VALOR_RESIDUAL_NO_GARAN_DUDOS = "VALOR_RESIDUAL_NO_GARAN_DUDOS";
    public static final String VALOR_RESIDUAL_COMP_TER_DUDOS = "VALOR_RESIDUAL_COMP_TER_DUDOS";
    public static final String PRODUCTOS_DUDOSOS_DEVEN_ORDEN = "PRODUCTOS_DUDOSOS_DEVEN_ORDEN";
    public static final String VALOR_ACTUAL_COM_GARAN_CONCED = "VALOR_ACTUAL_COM_GARAN_CONCED";
    public static final String PROVISION_RIESGO_NORMAL = "PROVISION_RIESGO_NORMAL";
    public static final String TOTAL_INTERESES_DEVEN_CREDITO = "TOTAL_INTERESES_DEVEN_CREDITO";
    public static final String OTRAS_PERIODIFICACIONES_ACTIV = "OTRAS_PERIODIFICACIONES_ACTIV";
    public static final String FECHA_BAJA_DEF_CIRBE = "FECHA_BAJA_DEF_CIRBE";
    public static final String SITUACION_OPERATIVA1 = "SITUACION_OPERATIVA1";
    public static final String ESTADO_CUMPLIMIENTO1 = "ESTADO_CUMPLIMIENTO1";
    public static final String CONTRATO_SINDICADO = "CONTRATO_SINDICADO";

    public static final String FECHA_CANCELACION = "FECHA_CANCELACION";
    public static final String SALDO_DEUDOR_NO_VENCIDO = "SALDO_DEUDOR_NO_VENCIDO";
    public static final String SALDO_DEUDOR_VENCIDO = "SALDO_DEUDOR_VENCIDO";
    public static final String PRODUCTOS_VENCIDOS = "PRODUCTOS_VENCIDOS";
    public static final String INTERESES_DEMORA_CONSOLIDADOS = "INTERESES_DEMORA_CONSOLIDADOS";
    public static final String INTERESES_DEVENGADOS = "INTERESES_DEVENGADOS";
    public static final String ID_PERSONA_CONTRAPARTE_DIRECT = "ID_PERSONA_CONTRAPARTE_DIRECT";
    public static final String VALOR_RESIDUAL_COMP_TERCEROS = "VALOR_RESIDUAL_COMP_TERCEROS";
    public static final String COTIZA = "COTIZA";
    public static final String ENTIDAD_DEPOSITARIA = "ENTIDAD_DEPOSITARIA";
    public static final String DIVIDENDOS_DEVENGADOS_MES = "DIVIDENDOS_DEVENGADOS_MES";
    public static final String SALDO_CONTINGENTE = "SALDO_CONTINGENTE";
    public static final String JERARQUIA_VALOR_RAZONABLE = "JERARQUIA_VALOR_RAZONABLE";
    public static final String COBERTURA_RIESGO_NORMAL = "COBERTURA_RIESGO_NORMAL";
    public static final String INTERESES_DEVENGADOS_DUDOSOS = "INTERESES_DEVENGADOS_DUDOSOS";
    public static final String VALOR_RAZONABLE = "VALOR_RAZONABLE";
    public static final String SANEAMIENTOS_DIRECTOS_MES = "SANEAMIENTOS_DIRECTOS_MES";
    public static final String INTERESES_COBRADOS_MES = "INTERESES_COBRADOS_MES";
    public static final String NUMERO_OCURRENCIAS = "NUMERO_OCURRENCIAS";
    public static final String PORCENTAJE_PARTICI_CAPITAL = "PORCENTAJE_PARTICI_CAPITAL";
    public static final String SALDO_CONTINGENTE_DUDOSO = "SALDO_CONTINGENTE_DUDOSO";
    public static final String SALDO_CONTINGENTE_VENCIDO = "SALDO_CONTINGENTE_VENCIDO";
    public static final String SALDO_CONTINGENTE_DUDOSO_VENC = "SALDO_CONTINGENTE_DUDOSO_VENC";
    public static final String MONEDA = "MONEDA";
    public static final String GASTOS_ACTIVADOS = "GASTOS_ACTIVADOS";
    public static final String RESTO_VALOR_CONTABLE_ACTIVOS = "RESTO_VALOR_CONTABLE_ACTIVOS";
    public static final String SALDO_ACREEDOR = "SALDO_ACREEDOR";
    public static final String PASIVOS_VALOR_RAZONABLE = "PASIVOS_VALOR_RAZONABLE";
    public static final String MICRO_COBERTURA = "MICRO_COBERTURA";
    public static final String PRIMA_DESCNTO_ADQUIS_ASUNCION = "PRIMA_DESCNTO_ADQUIS_ASUNCION";
    public static final String ACTIVOS_VALOR_RAZOBL = "ACTIVOS_VALOR_RAZOBL";
    public static final String VALOR_RESIDUAL_NO_GARANTIZADO = "VALOR_RESIDUAL_NO_GARANTIZADO";
    public static final String IMPORTE_LIQ_COMPRAS_MES = "IMPORTE_LIQ_COMPRAS_MES";
    public static final String IMPORTE_LIQ_VENTAS_MES = "IMPORTE_LIQ_VENTAS_MES";
    public static final String COBERT_PERDIDAS_PARTCIP_MES = "COBERT_PERDIDAS_PARTCIP_MES";
    public static final String CORRECCIONES_VALOR_VALORES = "CORRECCIONES_VALOR_VALORES";
    public static final String TIPO_CODIGO_VALOR = "TIPO_CODIGO_VALOR";
    public static final String PRODUCTO_SUBPRODUCTO = "PRODUCTO_SUBPRODUCTO";
    public static final String SALDO_DEUDOR_DUDOSO_NO_VENCID = "SALDO_DEUDOR_DUDOSO_NO_VENCID";
    public static final String SALDO_DEUDOR_DUDOSO_VENCIDO = "SALDO_DEUDOR_DUDOSO_VENCIDO";
    public static final String PRODUCTOS_VENCIDOS_DUDOSOS = "PRODUCTOS_VENCIDOS_DUDOSOS";
    public static final String INTERESES_DEMORA_CONSOL_DUDOS = "INTERESES_DEMORA_CONSOL_DUDOS";
    public static final String RESTO_VALOR_CONT_ACTVOS_DUDOS = "RESTO_VALOR_CONT_ACTVOS_DUDOS";

    public static final String OPERACION_TITULARES_EXONERADOS  = "OPERACION_TITULARES_EXONERADOS";
    public static final String FECHA_PRIMER_INCUMPLIMIENTO_SIN_FALL_PARC = "FECHA_PRIMER_INCUMPLIMIENTO_SIN_FALL_PARC";                                                       ;
    public static final String CLASIF_OPER_ANEJO_IX = "CLASIF_OPER_ANEJO_IX";
    public static final String FECHA_REFINANCIACION = "FECHA_REFINANCIACION";
    public static final String SIN_DEVENGO_INTERES = "SIN_DEVENGO_INTERES";
    public static final String FALLIDOS_PARCIALES = "FALLIDOS_PARCIALES";
    public static final String ENTIDAD_GRUPO_ORIGEN =  "ENTIDAD_GRUPO_ORIGEN";
    public static final String CUENTA_GRUPO = "CUENTA_GRUPO";
    public static final String SUBCUENTA = "SUBCUENTA";
    public static final String ID_CENTRO_CONTABLE_A = "ID_CENTRO_CONTABLE_A";

    public static final String MORATORIA_COVID19 = "MORATORIA_COVID19";
    public static final String FECHA_INICIO_MORATORIA_COVID19 = "FECHA_INICIO_MORATORIA_COVID19";
    public static final String FECHA_FIN_MORATORIA_COVID19 = "FECHA_FIN_MORATORIA_COVID19";

    public static final String LINE = "LINE";

    public static final String[] DEFAULT_COLUMNS = {LINE};

    @Override
    public String[] getDefaultColumns() {
        return DEFAULT_COLUMNS;
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
        AnacreditOperacionesItem item = new AnacreditOperacionesItem();
        if ((row == null) || (row.getProperty(AnacreditOperacionesReportTemplate.ROW_DATA) == null)) {
            return null;
        }

        Object property = row.getProperty(AnacreditOperacionesReportTemplate.ROW_DATA);
        String copy = row.getProperty("COPY");

        if(property instanceof AnacreditOperacionesItem){
            item = (AnacreditOperacionesItem) property;
        }else{
            return null;
        }
        if(ID_ENTIDAD.equalsIgnoreCase(columnName)){
            return item.getId_entidad();
        }else if(FECHA_EXTRACCION.equalsIgnoreCase(columnName)){
            return item.getFecha_extraccion();
        }else if(ID_CONTRATO.equalsIgnoreCase(columnName)){
            return item.getId_contrato();
        }else if(ID_CONTRATO_INTERNO.equalsIgnoreCase(columnName)){
            return item.getId_contrato_interno();
        }else if(APLICACION_ORIGEN.equalsIgnoreCase(columnName)){
            return item.getAplicacion_origen();
        }else if(ID_CENTRO_CONTABLE.equalsIgnoreCase(columnName)){
            return item.getId_centro_contable();
        }else if(CODIGO_VALOR.equalsIgnoreCase(columnName)){
            return item.getCodigo_valor();
        }else if(DECLARADO_CIR_TERCERA_ENTIDAD.equalsIgnoreCase(columnName)){
            return item.getDeclarado_cir_tercera_entidad();
        }else if(PAIS_NEGOCIO.equalsIgnoreCase(columnName)){
            return item.getPais_negocio();
        }else if(PRODUCTO_AC.equalsIgnoreCase(columnName)){
            return item.getProducto_ac();
        }else if(PRODUCTO_ENTIDAD.equalsIgnoreCase(columnName)){
            return item.getProducto_entidad();
        }else if(ACTIVO_OP_VALORES.equalsIgnoreCase(columnName)){
            return item.getActivo_op_valores();
        }else if(SUBORDINACION_PRODUCTO_AC.equalsIgnoreCase(columnName)){
            return item.getSubordinacion_producto_ac();
        }else if(TIPO_RIESGO_SUBYACENTE.equalsIgnoreCase(columnName)){
            return item.getTipo_riesgo_subyacente();
        }else if(FINALIDAD_AC.equalsIgnoreCase(columnName)){
            return item.getFinalidad_ac();
        }else if(TRAMITE_RECUPERACION.equalsIgnoreCase(columnName)){
            return item.getTramite_recuperacion();
        }else if(PRINCIPAL_INICIAL.equalsIgnoreCase(columnName)){
            return item.getPrincipal_inicial();
        }else if(LIMITE_INICIAL.equalsIgnoreCase(columnName)){
            return item.getLimite_inicial();
        }else if(IMPORTE_CONCEDIDO.equalsIgnoreCase(columnName)){
            return item.getImporte_concedido();
        }else if(FECHA_EMISION_ORIGINACION.equalsIgnoreCase(columnName)){
            return item.getFecha_emision_originacion();
        }else if(FECHA_VENCIMIENTO.equalsIgnoreCase(columnName)){
            return item.getFecha_vencimiento();
        }else if(ORIGEN_OPERACION.equalsIgnoreCase(columnName)){
            return item.getOrigen_operacion();
        }else if(REFINANCIACION.equalsIgnoreCase(columnName)){
            return item.getRefinanciacion();
        }else if(SUBVENCION_OPERACION.equalsIgnoreCase(columnName)){
            return item.getSubvencion_operacion();
        }else if(CANAL_CONTRATACION_AC.equalsIgnoreCase(columnName)){
            return item.getCanal_contratacion_ac();
        }else if(PROVINCIA_NEGOCIO.equalsIgnoreCase(columnName)){
            return item.getProvincia_negocio();
        }else if(ESQUEMA_AMORT_OPERACIONES_AC.equalsIgnoreCase(columnName)){
            return item.getEsquema_amort_operaciones_ac();
        }else if(PORCENT_PARTICIP_SINDICADOS.equalsIgnoreCase(columnName)){
            return item.getPorcent_particip_sindicados();
        }else if(EDIF_FINANC_ESTADO.equalsIgnoreCase(columnName)){
            return item.getEdif_financ_estado();
        }else if(EDIF_FINANC_LICENCIA.equalsIgnoreCase(columnName)){
            return item.getEdif_financ_licencia();
        }else if(INMUEBLE_FINANC_NUM_VIVIENDAS.equalsIgnoreCase(columnName)){
            return item.getInmueble_financ_num_viviendas();
        }else if(CODIGO_PROMOC_INMOB_FINAN.equalsIgnoreCase(columnName)){
            return item.getCodigo_promoc_inmob_finan();
        }else if(DESC_PROMOC_INMOB_FINAN.equalsIgnoreCase(columnName)){
            return item.getDesc_promoc_inmob_finan();
        }else if(PLAZO_RESIDUAL.equalsIgnoreCase(columnName)){
            return item.getPlazo_residual();
        }else if(TIPO_GARANTIA_REAL_PPAL.equalsIgnoreCase(columnName)){
            return item.getTipo_garantia_real_ppal();
        }else if(ORDEN_HIPOTECA.equalsIgnoreCase(columnName)){
            return item.getOrden_hipoteca();
        }else if(COBERTURA_GARANTIA_REAL.equalsIgnoreCase(columnName)){
            return item.getCobertura_garantia_real();
        }else if(TIPO_GARANT_PERSONAL_PPAL_AC.equalsIgnoreCase(columnName)){
            return item.getTipo_garant_personal_ppal_ac();
        }else if(COBERTURA_GARANTIA_PERSONAL.equalsIgnoreCase(columnName)){
            return item.getCobertura_garantia_personal();
        }else if(SITUACION_OPERATIVA.equalsIgnoreCase(columnName)){
            return item.getSituacion_operativa();
        }else if(FECHA_PRIMER_INCUMPLIMIENTO.equalsIgnoreCase(columnName)){
            return item.getFecha_primer_incumplimiento();
        }else if(FECHA_ULTIMO_IMPAGO_INCUMPLIM.equalsIgnoreCase(columnName)){
            return item.getFecha_ultimo_impago_incumplim();
        }else if(PRODUCTOS_DUDOSOS_NO_CONSOLID.equalsIgnoreCase(columnName)){
            return item.getProductos_dudosos_no_consolid();
        }else if(INTERESES_DEMORA_NO_CONSOLID.equalsIgnoreCase(columnName)){
            return item.getIntereses_demora_no_consolid();
        }else if(LIMITE.equalsIgnoreCase(columnName)){
            return item.getLimite();
        }else if(DISPONIBLE_INMEDIATO.equalsIgnoreCase(columnName)){
            return item.getDisponible_inmediato();
        }else if(DISPONIBLE_CONDICIONADO.equalsIgnoreCase(columnName)){
            return item.getDisponible_condicionado();
        }else if(PPAL_COBRADO_MES_AMORT_CORRNT.equalsIgnoreCase(columnName)){
            return item.getPpal_cobrado_mes_amort_corrnt();
        }else if(PPAL_COBRADO_MES_AMORT_ANTICI.equalsIgnoreCase(columnName)){
            return item.getPpal_cobrado_mes_amort_antici();
        }else if(VAR_DEUDA_COND_PRESC.equalsIgnoreCase(columnName)){
            return item.getVar_deuda_cond_presc();
        }else if(TIPO_ACT_RECIB_PAGO_MES.equalsIgnoreCase(columnName)){
            return item.getTipo_act_recib_pago_mes();
        }else if(REDUC_DEUDA_ACT_RECIB_PAG_MES.equalsIgnoreCase(columnName)){
            return item.getReduc_deuda_act_recib_pag_mes();
        }else if(TIPO_SUBROGACION_MES.equalsIgnoreCase(columnName)){
            return item.getTipo_subrogacion_mes();
        }else if(REDUC_DEUDA_SUBROGACION_MES.equalsIgnoreCase(columnName)){
            return item.getReduc_deuda_subrogacion_mes();
        }else if(TIPO_REFINANC_MES.equalsIgnoreCase(columnName)){
            return item.getTipo_refinanc_mes();
        }else if(REDUC_DEUDA_REFINANC_MES.equalsIgnoreCase(columnName)){
            return item.getReduc_deuda_refinanc_mes();
        }else if(GESTION_OP_TRANSFERIDA_MES.equalsIgnoreCase(columnName)){
            return item.getGestion_op_transferida_mes();
        }else if(IMPORTE_TRANSFERIDO_MES_CI.equalsIgnoreCase(columnName)){
            return item.getImporte_transferido_mes_ci();
        }else if(FECHA_ULTIMO_VTO_INTERESES.equalsIgnoreCase(columnName)){
            return item.getFecha_ultimo_vto_intereses();
        }else if(FECHA_PROXIMO_VTO_INTERESES.equalsIgnoreCase(columnName)){
            return item.getFecha_proximo_vto_intereses();
        }else if(FECHA_ULTIMO_VTO_PRINICIPAL.equalsIgnoreCase(columnName)){
            return item.getFecha_ultimo_vto_prinicipal();
        }else if(FECHA_PROXIMO_VTO_PRINCIPAL.equalsIgnoreCase(columnName)){
            return item.getFecha_proximo_vto_principal();
        }else if(NUM_CUOTAS_IMPAGADAS_PPAL.equalsIgnoreCase(columnName)){
            return item.getNum_cuotas_impagadas_ppal();
        }else if(MODALIDAD_TIPO_INTERES_AC.equalsIgnoreCase(columnName)){
            return item.getModalidad_tipo_interes_ac();
        }else if(TEDR.equalsIgnoreCase(columnName)){
            return item.getTedr();
        }else if(INDICE_REFERENCIA_AC.equalsIgnoreCase(columnName)){
            return item.getIndice_referencia_ac();
        }else if(SUELO_RENTABILIDAD.equalsIgnoreCase(columnName)){
            return item.getSuelo_rentabilidad();
        }else if(TECHO_RENTABILIDAD.equalsIgnoreCase(columnName)){
            return item.getTecho_rentabilidad();
        }else if(DIFERENCIAL_SOBRE_INDICE_REFE.equalsIgnoreCase(columnName)){
            return item.getDiferencial_sobre_indice_refe();
        }else if(SITUACION_OPE_RIESGO_DIREC_AC.equalsIgnoreCase(columnName)){
            return item.getSituacion_ope_riesgo_direc_ac();
        }else if(COBERTURA_RIESGO_DUDOSO.equalsIgnoreCase(columnName)){
            return item.getCobertura_riesgo_dudoso();
        }else if(COBERTURA_RIESGO_PAIS.equalsIgnoreCase(columnName)){
            return item.getCobertura_riesgo_pais();
        }else if(PROVISION_RIESGO_DUDOSO.equalsIgnoreCase(columnName)){
            return item.getProvision_riesgo_dudoso();
        }else if(PROVISION_RIESGO_PAIS.equalsIgnoreCase(columnName)){
            return item.getProvision_riesgo_pais();
        }else if(COMISIONES_PENDIENTES_DEVENGO.equalsIgnoreCase(columnName)){
            return item.getComisiones_pendientes_devengo();
        }else if(COSTE_TRANSACCION.equalsIgnoreCase(columnName)){
            return item.getCoste_transaccion();
        }else if(DESCUENTO_POR_DETERIORO.equalsIgnoreCase(columnName)){
            return item.getDescuento_por_deterioro();
        }else if(ACTIVOS_VALOR_RAZOBL_RSGO_CRE.equalsIgnoreCase(columnName)){
            return item.getActivos_valor_razobl_rsgo_cre();
        }else if(VALOR_NOMINAL.equalsIgnoreCase(columnName)){
            return item.getValor_nominal();
        }else if(NOCIONAL.equalsIgnoreCase(columnName)){
            return item.getNocional();
        }else if(IMPORTE_PENDIENTE_DESEMBOLSO.equalsIgnoreCase(columnName)){
            return item.getImporte_pendiente_desembolso();
        }else if(PLAZO_ORIGEN_M.equalsIgnoreCase(columnName)){
            return item.getPlazo_origen_m();
        }else if(OPERACION_NO_DECLARABLE_CIRBE.equalsIgnoreCase(columnName)){
            return item.getOperacion_no_declarable_cirbe();
        }else if(FECHA_RENOVACION.equalsIgnoreCase(columnName)){
            return item.getFecha_renovacion();
        }else if(PRINCIPAL_VENCIDO_MES_SUBVENC.equalsIgnoreCase(columnName)){
            return item.getPrincipal_vencido_mes_subvenc();
        }else if(ESTADO_CUMPLIMIENTO.equalsIgnoreCase(columnName)){
            return item.getEstado_cumplimiento();
        }else if(TIPO_TARJETA.equalsIgnoreCase(columnName)){
            return item.getTipo_tarjeta();
        }else if(SALDO_DEUDOR_EXCEDIDO_VENCIDO.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_excedido_vencido();
        }else if(PRINCIPAL_VDO_REFIN_PTE_AMORT.equalsIgnoreCase(columnName)){
            return item.getPrincipal_vdo_refin_pte_amort();
        }else if(FECHA_PRIMER_INCUMPL_OP_REFIN.equalsIgnoreCase(columnName)){
            return item.getFecha_primer_incumpl_op_refin();
        }else if(CONOCIMIENTO_GARANT_PERS_PPAL.equalsIgnoreCase(columnName)){
            return item.getConocimiento_garant_pers_ppal();
        }else if(SALDO_DEUDOR_DUDOSO_EXCED_VEN.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_dudoso_exced_ven();
        }else if(INTERESES_EXCED_DEVENGADO.equalsIgnoreCase(columnName)){
            return item.getIntereses_exced_devengado();
        }else if(INTERESES_EXCED_DEVENGADO_DUD.equalsIgnoreCase(columnName)){
            return item.getIntereses_exced_devengado_dud();
        }else if(ESTADO_REFIN_REEST_RENEG.equalsIgnoreCase(columnName)){
            return item.getEstado_refin_reest_reneg();
        }else if(ID_CONTRATO_JURIDICO.equalsIgnoreCase(columnName)){
            return item.getId_contrato_juridico();
        }else if(TIPO_CARTERA_IFRS9.equalsIgnoreCase(columnName)){
            return item.getTipo_cartera_ifrs9();
        }else if(CARTERA_PRUDENCIAL.equalsIgnoreCase(columnName)){
            return item.getCartera_prudencial();
        }else if(FECHA_PRIMERA_LIQUIDACION.equalsIgnoreCase(columnName)){
            return item.getFecha_primera_liquidacion();
        }else if(PRODUCTO_RECURSO.equalsIgnoreCase(columnName)){
            return item.getProducto_recurso();
        }else if(INSTRUMENTO_FIDUCIARIO.equalsIgnoreCase(columnName)){
            return item.getInstrumento_fiduciario();
        }else if(FINANCIACION_PROYECTO.equalsIgnoreCase(columnName)){
            return item.getFinanciacion_proyecto();
        }else if(IMPORT_RECUPERAD_ACUM_SIT_IMP.equalsIgnoreCase(columnName)){
            return item.getImport_recuperad_acum_sit_imp();
        }else if(FECHA_ESTADO_CUMPLIMIENTO.equalsIgnoreCase(columnName)){
            return item.getFecha_estado_cumplimiento();
        }else if(FECHA_REFIN_REEST.equalsIgnoreCase(columnName)){
            return item.getFecha_refin_reest();
        }else if(TIPO_REFERENCIA_VENCIMIENTO.equalsIgnoreCase(columnName)){
            return item.getTipo_referencia_vencimiento();
        }else if(CODIGO_RATIO_REFERENCIA.equalsIgnoreCase(columnName)){
            return item.getCodigo_ratio_referencia();
        }else if(TIPO_REFERENCIA_SUSTITUTIVO.equalsIgnoreCase(columnName)){
            return item.getTipo_referencia_sustitutivo();
        }else if(FREC_REVISION_TIPO_INT_PER.equalsIgnoreCase(columnName)){
            return item.getFrec_revision_tipo_int_per();
        }else if(PROXIMA_REVISION_TIPO_INTERES.equalsIgnoreCase(columnName)){
            return item.getProxima_revision_tipo_interes();
        }else if(FECHA_FINAL_PERIODO_INTERESES.equalsIgnoreCase(columnName)){
            return item.getFecha_final_periodo_intereses();
        }else if(FRECUENCIA_PAGO_PRINC_INT.equalsIgnoreCase(columnName)){
            return item.getFrecuencia_pago_princ_int();
        }else if(SALDO_VIVO_NOMINAL.equalsIgnoreCase(columnName)){
            return item.getSaldo_vivo_nominal();
        }else if(SALDO_DEUDOR_FALLIDO_NO_VENC.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_fallido_no_venc();
        }else if(SALDO_DEUDOR_FALLIDO_VENCIDO.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_fallido_vencido();
        }else if(IMPORTES_VENCIDOS.equalsIgnoreCase(columnName)){
            return item.getImportes_vencidos();
        }else if(PRODUCTOS_FALLIDOS.equalsIgnoreCase(columnName)){
            return item.getProductos_fallidos();
        }else if(FALLIDOS_ACUMULADOS.equalsIgnoreCase(columnName)){
            return item.getFallidos_acumulados();
        }else if(INTERESES_DEMORA_FALLIDOS.equalsIgnoreCase(columnName)){
            return item.getIntereses_demora_fallidos();
        }else if(GASTOS_EXIGIBLES_FALLIDOS.equalsIgnoreCase(columnName)){
            return item.getGastos_exigibles_fallidos();
        }else if(FASE_DETERIORO.equalsIgnoreCase(columnName)){
            return item.getFase_deterioro();
        }else if(IMPORTE_LIBROS_ACTIVO.equalsIgnoreCase(columnName)){
            return item.getImporte_libros_activo();
        }else if(COBERTURA_ACUMULADA.equalsIgnoreCase(columnName)){
            return item.getCobertura_acumulada();
        }else if(PROVISION_FUERA_BALANCE.equalsIgnoreCase(columnName)){
            return item.getProvision_fuera_balance();
        }else if(ACT_VAL_RAZ_RIES_CRED_ANT_ADQ.equalsIgnoreCase(columnName)){
            return item.getAct_val_raz_ries_cred_ant_adq();
        }else if(SALDO_FUERA_BALANCE.equalsIgnoreCase(columnName)){
            return item.getSaldo_fuera_balance();
        }else if(DERECHOS_REEMBOLSO.equalsIgnoreCase(columnName)){
            return item.getDerechos_reembolso();
        }else if(TIPO_FUENTE_DE_CARGA.equalsIgnoreCase(columnName)){
            return item.getTipo_fuente_de_carga();
        }else if(FECHA_RENEGOCIACION.equalsIgnoreCase(columnName)){
            return item.getFecha_renegociacion();
        }else if(SITUACION_IMPAGO_OPERACION.equalsIgnoreCase(columnName)){
            return item.getSituacion_impago_operacion();
        }else if(FECHA_SITUACION_IMPAGO_OPE.equalsIgnoreCase(columnName)){
            return item.getFecha_situacion_impago_ope();
        }else if(RECONOCIMIENTO_BALANCE.equalsIgnoreCase(columnName)){
            return item.getReconocimiento_balance();
        }else if(SALDO_DEUDOR_EXCEDIDO_NO_VENC.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_excedido_no_venc();
        }else if(SALDO_DEUDOR_DUD_EXCE_NO_VENC.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_dud_exce_no_venc();
        }else if(MET_CALC_COBERT_RIESGO_NORMAL.equalsIgnoreCase(columnName)){
            return item.getMet_calc_cobert_riesgo_normal();
        }else if(MET_CALC_COBERT_RIESGO_DUDOSO.equalsIgnoreCase(columnName)){
            return item.getMet_calc_cobert_riesgo_dudoso();
        }else if(RENOVACION_AUTOMATICA.equalsIgnoreCase(columnName)){
            return item.getRenovacion_automatica();
        }else if(VALOR_RESIDUAL_NO_GARAN_DUDOS.equalsIgnoreCase(columnName)){
            return item.getValor_residual_no_garan_dudos();
        }else if(VALOR_RESIDUAL_COMP_TER_DUDOS.equalsIgnoreCase(columnName)){
            return item.getValor_residual_comp_ter_dudos();
        }else if(PRODUCTOS_DUDOSOS_DEVEN_ORDEN.equalsIgnoreCase(columnName)){
            return item.getProductos_dudosos_deven_orden();
        }else if(VALOR_ACTUAL_COM_GARAN_CONCED.equalsIgnoreCase(columnName)){
            return item.getValor_actual_com_garan_conced();
        }else if(PROVISION_RIESGO_NORMAL.equalsIgnoreCase(columnName)){
            return item.getProvision_riesgo_normal();
        }else if(TOTAL_INTERESES_DEVEN_CREDITO.equalsIgnoreCase(columnName)){
            return item.getTotal_intereses_deven_credito();
        }else if(OTRAS_PERIODIFICACIONES_ACTIV.equalsIgnoreCase(columnName)){
            return item.getOtras_periodificaciones_activ();
        }else if(FECHA_BAJA_DEF_CIRBE.equalsIgnoreCase(columnName)){
            return item.getFecha_baja_def_cirbe();
        }else if(SITUACION_OPERATIVA1.equalsIgnoreCase(columnName)){
            return item.getSituacion_operativa1();
        }else if(ESTADO_CUMPLIMIENTO1.equalsIgnoreCase(columnName)){
            return item.getEstado_cumplimiento1();
        }else if(CONTRATO_SINDICADO.equalsIgnoreCase(columnName)){
            return item.getContrato_sindicado();
        }else if(FECHA_CANCELACION.equalsIgnoreCase(columnName)){
            return item.getFecha_cancelacion();
        }else if(SALDO_DEUDOR_NO_VENCIDO.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_no_vencido();
        }else if(SALDO_DEUDOR_VENCIDO.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_vencido();
        }else if(PRODUCTOS_VENCIDOS.equalsIgnoreCase(columnName)){
            return item.getProductos_vencidos();
        }else if(INTERESES_DEMORA_CONSOLIDADOS.equalsIgnoreCase(columnName)){
            return item.getIntereses_demora_consolidados();
        }else if(INTERESES_DEVENGADOS.equalsIgnoreCase(columnName)){
            return item.getIntereses_devengados();
        }else if(ID_PERSONA_CONTRAPARTE_DIRECT.equalsIgnoreCase(columnName)){
            return item.getId_persona_contraparte_direct();
        }else if(VALOR_RESIDUAL_COMP_TERCEROS.equalsIgnoreCase(columnName)){
            return item.getValor_residual_comp_terceros();
        }else if(COTIZA.equalsIgnoreCase(columnName)){
            return item.getCotiza();
        }else if(ENTIDAD_DEPOSITARIA.equalsIgnoreCase(columnName)){
            return item.getEntidad_depositaria();
        }else if(DIVIDENDOS_DEVENGADOS_MES.equalsIgnoreCase(columnName)){
            return item.getDividendos_devengados_mes();
        }else if(SALDO_CONTINGENTE.equalsIgnoreCase(columnName)){
            return item.getSaldo_contingente();
        }else if(JERARQUIA_VALOR_RAZONABLE.equalsIgnoreCase(columnName)){
            return item.getJerarquia_valor_razonable();
        }else if(COBERTURA_RIESGO_NORMAL.equalsIgnoreCase(columnName)){
            return item.getCobertura_riesgo_normal();
        }else if(INTERESES_DEVENGADOS_DUDOSOS.equalsIgnoreCase(columnName)){
            return item.getIntereses_devengados_dudosos();
        }else if(VALOR_RAZONABLE.equalsIgnoreCase(columnName)){
            return item.getValor_razonable();
        }else if(SANEAMIENTOS_DIRECTOS_MES.equalsIgnoreCase(columnName)){
            return item.getSaneamientos_directos_mes();
        }else if(INTERESES_COBRADOS_MES.equalsIgnoreCase(columnName)){
            return item.getIntereses_cobrados_mes();
        }else if(NUMERO_OCURRENCIAS.equalsIgnoreCase(columnName)){
            return item.getNumero_ocurrencias();
        }else if(PORCENTAJE_PARTICI_CAPITAL.equalsIgnoreCase(columnName)){
            return item.getPorcentaje_partici_capital();
        }else if(SALDO_CONTINGENTE_DUDOSO.equalsIgnoreCase(columnName)){
            return item.getSaldo_contingente_dudoso();
        }else if(SALDO_CONTINGENTE_VENCIDO.equalsIgnoreCase(columnName)){
            return item.getSaldo_contingente_vencido();
        }else if(SALDO_CONTINGENTE_DUDOSO_VENC.equalsIgnoreCase(columnName)){
            return item.getSaldo_contingente_dudoso_venc();
        }else if(MONEDA.equalsIgnoreCase(columnName)){
            return item.getMoneda();
        }else if(GASTOS_ACTIVADOS.equalsIgnoreCase(columnName)){
            return item.getGastos_activados();
        }else if(RESTO_VALOR_CONTABLE_ACTIVOS.equalsIgnoreCase(columnName)){
            return item.getResto_valor_contable_activos();
        }else if(SALDO_ACREEDOR.equalsIgnoreCase(columnName)){
            return item.getSaldo_acreedor();
        }else if(PASIVOS_VALOR_RAZONABLE.equalsIgnoreCase(columnName)){
            return item.getPasivos_valor_razonable();
        }else if(MICRO_COBERTURA.equalsIgnoreCase(columnName)){
            return item.getMicro_cobertura();
        }else if(PRIMA_DESCNTO_ADQUIS_ASUNCION.equalsIgnoreCase(columnName)){
            return item.getPrima_descnto_adquis_asuncion();
        }else if(ACTIVOS_VALOR_RAZOBL.equalsIgnoreCase(columnName)){
            return item.getActivos_valor_razobl();
        }else if(VALOR_RESIDUAL_NO_GARANTIZADO.equalsIgnoreCase(columnName)){
            return item.getValor_residual_no_garantizado();
        }else if(IMPORTE_LIQ_COMPRAS_MES.equalsIgnoreCase(columnName)){
            return item.getImporte_liq_compras_mes();
        }else if(IMPORTE_LIQ_VENTAS_MES.equalsIgnoreCase(columnName)){
            return item.getImporte_liq_ventas_mes();
        }else if(COBERT_PERDIDAS_PARTCIP_MES.equalsIgnoreCase(columnName)){
            return item.getCobert_perdidas_partcip_mes();
        }else if(CORRECCIONES_VALOR_VALORES.equalsIgnoreCase(columnName)){
            return item.getCorrecciones_valor_valores();
        }else if(TIPO_CODIGO_VALOR.equalsIgnoreCase(columnName)){
            return item.getTipo_codigo_valor();
        }else if(PRODUCTO_SUBPRODUCTO.equalsIgnoreCase(columnName)){
            return item.getProducto_subproducto();
        }else if(SALDO_DEUDOR_DUDOSO_NO_VENCID.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_dudoso_no_vencid();
        }else if(SALDO_DEUDOR_DUDOSO_VENCIDO.equalsIgnoreCase(columnName)){
            return item.getSaldo_deudor_dudoso_vencido();
        }else if(PRODUCTOS_VENCIDOS_DUDOSOS.equalsIgnoreCase(columnName)){
            return item.getProductos_vencidos_dudosos();
        }else if(INTERESES_DEMORA_CONSOL_DUDOS.equalsIgnoreCase(columnName)){
            return item.getIntereses_demora_consol_dudos();
        }else if(RESTO_VALOR_CONT_ACTVOS_DUDOS.equalsIgnoreCase(columnName)){
            return item.getResto_valor_cont_actvos_dudos();
        }else if(OPERACION_TITULARES_EXONERADOS.equalsIgnoreCase(columnName)){
            return item.getOperacion_titulares_exonerados();
        }else if(FECHA_PRIMER_INCUMPLIMIENTO_SIN_FALL_PARC.equalsIgnoreCase(columnName)){
            return item.getFecha_primer_incumplimiento_sin_fall_parc();
        }else if(CLASIF_OPER_ANEJO_IX.equalsIgnoreCase(columnName)){
            return item.getClassif_oper_anejo_ix();
        }else if(FECHA_REFINANCIACION.equalsIgnoreCase(columnName)){
            return item.getFecha_refinanciacion();
        }else if(SIN_DEVENGO_INTERES.equalsIgnoreCase(columnName)){
            return item.getSin_devengo_interes();
        }else if(FALLIDOS_PARCIALES.equalsIgnoreCase(columnName)){
            return item.getFallidos_parciales();
        }else if(ENTIDAD_GRUPO_ORIGEN.equalsIgnoreCase(columnName)){
            return item.getEntidad_grupo_origen();
        }else if(CUENTA_GRUPO.equalsIgnoreCase(columnName)){
            return item.getCuenta_grupo();
        }else if(SUBCUENTA.equalsIgnoreCase(columnName)){
            return item.getSubcuenta();
        }else if(ID_CENTRO_CONTABLE_A.equalsIgnoreCase(columnName)){
            return item.getId_centro_contable_a();
        }else if(MORATORIA_COVID19.equalsIgnoreCase(columnName)){
            return item.getMoratoria_covid19();
        }else if(FECHA_INICIO_MORATORIA_COVID19.equalsIgnoreCase(columnName)){
            return item.getFecha_inicio_moratoria_covid19();
        }else if(FECHA_FIN_MORATORIA_COVID19.equalsIgnoreCase(columnName)){
            return item.getFecha_fin_moratoria_covid19();
        }else if(LINE.equalsIgnoreCase(columnName)){
            return getLine(item);
        }else {
            return super.getColumnValue(row, columnName, errors);
        }

    }

    protected String getLine(AnacreditOperacionesItem item){
        return null!=item ? item.toString() : "";
    }
}
