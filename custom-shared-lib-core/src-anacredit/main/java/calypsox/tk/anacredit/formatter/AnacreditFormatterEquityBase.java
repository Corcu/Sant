package calypsox.tk.anacredit.formatter;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.*;
import calypsox.tk.anacredit.util.ActivoOpValores;
import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.anacredit.util.EquityTypeIdentifier;
import calypsox.tk.anacredit.util.EurBasedAmount;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.report.AnacreditEQPositionReport;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import static calypsox.tk.report.AnacreditOperacionesReportStyle.IMPORTE_LIQ_COMPRAS_MES;
import static calypsox.tk.report.AnacreditOperacionesReportStyle.IMPORTE_LIQ_VENTAS_MES;

public class AnacreditFormatterEquityBase extends AnacreditFormatter {

    private static final String TKW_PARTENON_ACCOUNTING_ID = "PartenonAccountingID";

    /**
     * Format RECORD Copy3 - Operaciones
     * @param reportRow
     * @param errorMsgs
     * @return
     */
    public Copy3Record formatRecOperacionesCopy3(ReportRow reportRow, PricingEnv pEnv, Vector holidays, Vector errorMsgs) {
        try {
            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            JDate valDatePlus  = valDate.addBusinessDays(1, holidays);

            PricingEnv pEnvOfficial = reportRow.getProperty(AnacreditEQPositionReport.PROPERTY_OFFICIAL_PENV);

            Copy3Record r = new Copy3Record();

            EquityTypeIdentifier identifier = buildIdentifier(valDatetime, reportRow);

            r.setValue(Copy3Columns.FECHA_EXTRACCION, valDate);

            r.setValue( Copy3Columns.DECLARADO_CIR_TERCERA_ENTIDAD, 0);

             if (identifier.isEQPLZ()) {
                 r.setValue( Copy3Columns.CODIGO_VALOR, AnacreditConstants.EMPTY_STRING);
             }
             else {
                r.setValue( Copy3Columns.CODIGO_VALOR, identifier.getISIN());
             }

            String paisNegocio = _mapper.getPaisNegocio(identifier.getBook().getLegalEntity());
            r.setValue(Copy3Columns.PAIS_NEGOCIO, paisNegocio);

            if (identifier.isEQPLZ()) {
                if (Util.isEmpty(identifier.getTrade().getKeywordValue(TKW_PARTENON_ACCOUNTING_ID))) {
                    log(AnacreditFormatter.LogLevel.ERR, identifier.getTrade().getLongId(), " - Attribute PARTENON ID is empty.", errorMsgs );
                    return null;
                }

                if (Util.isEmpty(AnacreditMapper.getJMin(identifier.getCounterPartyOrAgent()))) {
                    log(AnacreditFormatter.LogLevel.ERR, identifier.getTrade().getLongId(), " - Attribute JMIN is Empry for Legal Entity "
                                    + identifier.getCounterPartyOrAgent().getCode(), errorMsgs );
                    return null;
                }

            } else {
                if (identifier.getPosition() == null) {
                    return null;
                }
            }

            if (identifier.isEQNOT()) {
                Double dDividend = reportRow.getProperty(AnacreditEQPositionReport.PROPERTY_TOTAL_DIVIDEND);
                Double dTotalSell = reportRow.getProperty(IMPORTE_LIQ_VENTAS_MES);
                Double dTotalBuy = reportRow.getProperty(IMPORTE_LIQ_COMPRAS_MES);
                if ( ((dDividend == null) || (dDividend == 0))
                        && ((dTotalSell == null) || (dTotalSell == 0))
                        && ((dTotalBuy == null) || (dTotalBuy == 0)) ){
                    log(AnacreditFormatter.LogLevel.ERR, identifier.getISIN(), " - EQNOT sin  campos de valor.", errorMsgs );
                    return null;
                }
            }


            if (identifier.isEQPLZ()) {
                String ccy = _mapper.getCurrencyMap(identifier.getTrade().getTradeCurrency(),
                        errorMsgs, String.valueOf(identifier.getTrade().getLongId()));
                r.setValue(Copy3Columns.MONEDA, ccy);
            } else  {
                String ccy = _mapper.getCurrencyMap(identifier.getPosition().getSettleCurrency(),
                        errorMsgs, identifier.getProduct().getDescription());
                r.setValue( Copy3Columns.MONEDA, ccy);
            }

            if (identifier.isEQPLZ())  {
                r.setValue( Copy3Columns.FECHA_EMISION_ORIGINACION, identifier.getTrade().getTradeDate());
            }
            else {
                JDatetime datetime = getFechaEmisionOriginacion(identifier);
                if (datetime !=null) {
                    r.setValue( Copy3Columns.FECHA_EMISION_ORIGINACION, datetime);
                } else {
                    r.setValue( Copy3Columns.FECHA_EMISION_ORIGINACION, AnacreditConstants.STR_MAX_DATE_99991231);
                }

            }

            if (identifier.isEQPLZ()) {
                r.setValue( Copy3Columns.FECHA_VENCIMIENTO, identifier.getTrade().getSettleDate());
                r.setValue( Copy3Columns.FECHA_CANCELACION, identifier.getTrade().getSettleDate());
            }
            else if (identifier.isEQPRF()) {
                r.setValue( Copy3Columns.FECHA_VENCIMIENTO, AnacreditConstants.STR_DATE_20501231);
                r.setValue( Copy3Columns.FECHA_CANCELACION, AnacreditConstants.STR_DATE_20501231);
            }
            else {
                r.setValue( Copy3Columns.FECHA_VENCIMIENTO, AnacreditConstants.STR_MAX_DATE_99991231);
                r.setValue( Copy3Columns.FECHA_CANCELACION, AnacreditConstants.STR_MAX_DATE_99991231);
            }


            r.setValue( Copy3Columns.CONTRATO_RENEGOCIADO, "0");

            if (identifier.isEQ()) {
                r.setValue( Copy3Columns.PRODUCTO_AC, ActivoOpValores.instance().get(identifier.getProduct()));
                r.setValue( Copy3Columns.PRODUCTO_ENTIDAD, "EQ");
                r.setValue( Copy3Columns.PRODUCTO_SUBPRODUCTO, "EQ");

            }
            else if (identifier.isEQDES()) {
                r.setValue( Copy3Columns.PRODUCTO_AC, ActivoOpValores.PP180);
                r.setValue( Copy3Columns.PRODUCTO_ENTIDAD, "EQDES");
                r.setValue( Copy3Columns.PRODUCTO_SUBPRODUCTO, "EQDES");
                if (identifier.isEQPreferente()) {
                    r.setValue( Copy3Columns.ACTIVO_OP_VALORES, ActivoOpValores.RF165);
                }
                else {
                    r.setValue( Copy3Columns.ACTIVO_OP_VALORES, ActivoOpValores.instance().get(identifier.getProduct()));
                }

            }
            else if (identifier.isEQPRF()) {
                r.setValue( Copy3Columns.PRODUCTO_AC, ActivoOpValores.RF165);
                r.setValue( Copy3Columns.PRODUCTO_ENTIDAD, "EQPRF");
                r.setValue( Copy3Columns.SUBORDINACION_PRODUCTO_AC, "S0");
                r.setValue( Copy3Columns.FINALIDAD_AC, "F64");
                r.setValue( Copy3Columns.ORIGEN_OPERACION , "O74");
                r.setValue( Copy3Columns.PORCENT_PARTICIP_SINDICADOS , "100");
                r.setValue( Copy3Columns.PRODUCTO_SUBPRODUCTO, "EQPRF");
                r.setValue( Copy3Columns.FINANCIACION_PROYECTO,  2);
            }
            else if (identifier.isEQNOT()) {
                if (identifier.isEQPreferente()) {
                    r.setValue( Copy3Columns.PRODUCTO_AC, ActivoOpValores.RF165);

                }
                else {
                    r.setValue( Copy3Columns.PRODUCTO_AC, ActivoOpValores.instance().get(identifier.getProduct()));
                }
                r.setValue( Copy3Columns.PRODUCTO_ENTIDAD, "EQNOT");
                r.setValue( Copy3Columns.PRODUCTO_SUBPRODUCTO, "EQN");
            }
            else if (identifier.isEQPLZ()) {
                r.setValue( Copy3Columns.PRODUCTO_AC, AnacreditConstants.STR_CP010);
                r.setValue( Copy3Columns.PRODUCTO_ENTIDAD, "EQPLZ");
                r.setValue( Copy3Columns.ORIGEN_OPERACION , "O10");
                r.setValue( Copy3Columns.PORCENT_PARTICIP_SINDICADOS , "100");
                r.setValue( Copy3Columns.PRODUCTO_SUBPRODUCTO, "EQPLZ");

            }

            if (!identifier.isEQPRF() && !identifier.isEQPLZ())  {
                r.setValue( Copy3Columns.ESQUEMA_AMORT_OPERACIONES_AC , "13");
            }

            r.setValue(Copy3Columns.PLAZO_RESIDUAL, "P8");
            if (identifier.isEQPLZ()) {
                r.setValue(Copy3Columns.PLAZO_RESIDUAL, "P2");
            }

            r.setValue(Copy3Columns.GASTOS_ACTIVADOS, 0);

            if (identifier.isEQPLZ()) {
                double aDouble = identifier.computeTradeSettlementAmount();
                EurBasedAmount saldoDeudorNovenc = new EurBasedAmount(identifier.getCcy(), aDouble).invoke(valDate, pEnvOfficial);
                saldoDeudorNovenc.forceSigno(-1);
                r.keep(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO.name(), saldoDeudorNovenc);
                r.setValue(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO, saldoDeudorNovenc.getEurAmount());

            }

            r.setValue(Copy3Columns.SALDO_CONTINGENTE, +0.00);


            if (!identifier.isEQPLZ()) {
                Double closePrice = reportRow.getProperty(AnacreditEQPositionReport.PROPERTY_CLOSE_PRICE);
                Double valorRazonable =  identifier.getNominal()*closePrice;

                if (identifier.isEQ() || identifier.isEQPRF()) {

                    EurBasedAmount dSaldoDeudorNoVenc = reportRow.getProperty(AnacreditEQPositionReport.PROPERTY_SALDO_DEUDOR_NOVENC);
                    EurBasedAmount dActivosValorRazobl = reportRow.getProperty(AnacreditEQPositionReport.PROPERTY_ACTIVOS_VALOR_RAZOBL);

                    if ((dSaldoDeudorNoVenc == null)  || (dSaldoDeudorNoVenc.getOriginalAmount() == 0.0d)) {
                        log(AnacreditFormatter.LogLevel.ERR, identifier.getISIN() + identifier.getCounterPartyOrAgent() , " - Invalid SALDO.DEUDOR.NO.VENCIDO", errorMsgs );
                        return null;
                    }
                    /*
                    if (dActivosValorRazobl == null || (dActivosValorRazobl.getOriginalAmount() == 0.0d)) {
                        log(AnacreditFormatter.LogLevel.ERR, identifier.getISIN() + identifier.getCounterPartyOrAgent() , " - Invalid ACTIVOS.VALOR.RAZONABLE", errorMsgs );
                        return null;
                    }
                     */
                    if ((valorRazonable == null) || (valorRazonable == 0.0d)) {
                        log(AnacreditFormatter.LogLevel.ERR, identifier.getISIN() + identifier.getCounterPartyOrAgent() , " - Invalid VALOR.RAZONABLE", errorMsgs );
                        return null;
                    }

                    int signo = 1;
                    if (dSaldoDeudorNoVenc != null && dActivosValorRazobl != null
                            && valorRazonable != null )  {
                        double diff = (Math.abs(valorRazonable) - Math.abs(dSaldoDeudorNoVenc.getOriginalAmount()));
                        if (diff >= 0) {
                            signo = -1;
                        }
                    }

                    if (dSaldoDeudorNoVenc != null) {
                        dSaldoDeudorNoVenc.forceSigno(-1);
                        r.keep(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO.name(), dSaldoDeudorNoVenc);
                        r.setValue(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO, forceMinus(dSaldoDeudorNoVenc.getEurAmount()));
                    }

                    if (dActivosValorRazobl != null) {
                        dActivosValorRazobl.forceSigno(signo);
                        r.setValue(Copy3Columns.ACTIVOS_VALOR_RAZOBL, dActivosValorRazobl.getEurAmount());
                        r.keep(Copy3Columns.ACTIVOS_VALOR_RAZOBL.name(), dActivosValorRazobl);
                    }

                    if (valorRazonable != null) {
                        EurBasedAmount value = new EurBasedAmount(identifier.getCcy(), valorRazonable).invoke(valDate, pEnvOfficial);
                        value.forceSigno(-1);
                        r.setValue(Copy3Columns.VALOR_RAZONABLE, value.getEurAmount());
                        r.keep(Copy3Columns.VALOR_RAZONABLE.name(), value);
                    }
                }

                if (identifier.isEQDES()) {

                    if (valorRazonable != null) {
                        EurBasedAmount value = new EurBasedAmount(identifier.getCcy(), 0.0).invoke(valDate, pEnvOfficial);
                        value.forceSigno(1);
                        r.setValue(Copy3Columns.VALOR_RAZONABLE, value.getEurAmount());
                        r.keep(Copy3Columns.VALOR_RAZONABLE.name(), value);
                    }

                    if (closePrice != null) {
                        double dSaldoAcreedor = identifier.getNominal() * closePrice;
                        EurBasedAmount saldoAcreedor = new EurBasedAmount(identifier.getCcy(), dSaldoAcreedor).invoke(valDate, pEnvOfficial);
                        saldoAcreedor.forceSigno(1);
                        r.keep(Copy3Columns.SALDO_ACREEDOR.name(), saldoAcreedor);
                        r.setValue(Copy3Columns.SALDO_ACREEDOR, saldoAcreedor.getEurAmount());
                    }
                }

                if (identifier.isEQPRF()) {
                    r.keep(Copy3Columns.INTERESES_COBRADOS_MES.name(), +0.00);
                    Double dDividend = reportRow.getProperty(AnacreditEQPositionReport.PROPERTY_TOTAL_DIVIDEND);
                    if (dDividend != null) {
                        EurBasedAmount dividend = new EurBasedAmount(identifier.getCcy(), dDividend).invoke(valDate, pEnvOfficial);
                        r.keep(Copy3Columns.INTERESES_COBRADOS_MES.name(), dividend);
                        r.setValue(Copy3Columns.INTERESES_COBRADOS_MES, dividend.getEurAmount());
                    }
                }
            }

            // NUMERO DE OCURRENCIAS

            if (identifier.isEQPLZ()) {
                r.setValue(Copy3Columns.VALOR_NOMINAL, forceMinus(0.00d));
            }
            else  {
                Double dNominal = identifier.getNominal();

                int signo = -1;
                if (identifier.isEQDES()) {
                    signo = 1;
                }

                if (dNominal != null) {
                    EurBasedAmount nominal = new EurBasedAmount(identifier.getCcy(), dNominal).invoke(valDate, pEnvOfficial);
                    nominal.forceSigno(signo);
                    r.setValue(Copy3Columns.VALOR_NOMINAL, nominal.getEurAmount());
                    r.keep(Copy3Columns.VALOR_NOMINAL.name(), nominal);
                }
            }


            r.setValue( Copy3Columns.TIPO_REFINANC_MES , "R1");

            r.setValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC, AnacreditConstants.STR_M16);
            r.setValue(Copy3Columns.DIFERENCIAL_SOBRE_INDICE_REFE , +909.09);
            r.setValue(Copy3Columns.INDICE_REFERENCIA_AC , AnacreditConstants.STR_I99);

            if (!identifier.isEQPLZ())
                r.setValue(Copy3Columns.ID_PERSONA_CONTRAPARTE_DIRECT, AnacreditMapper.getJMin(identifier.getProductIssuer()));
            else {
                r.setValue(Copy3Columns.ID_PERSONA_CONTRAPARTE_DIRECT , AnacreditMapper.getJMin(identifier.getCounterPartyOrAgent()));
            }


            String jerarquia = _mapper.getSecCodeJerarquia(identifier.getProduct());
            //COTIZA
            if (identifier.isEQPLZ()) {
                r.setValue(Copy3Columns.COTIZA, _mapper.getCotiza(AnacreditConstants.EMPTY_STRING));
            }
            else {
                r.setValue(Copy3Columns.COTIZA, _mapper.getCotiza(jerarquia));
            }

            if (identifier.isEQPRF() || identifier.isEQPLZ())  {
                r.setValue(Copy3Columns.SITUACION_OPE_RIESGO_DIREC_AC , AnacreditConstants.STR_S10);
            }
            else {
                r.setValue(Copy3Columns.SITUACION_OPE_RIESGO_DIREC_AC , AnacreditConstants.STR_S140);
            }


            if (identifier.isEQNOT() || identifier.isEQDES()) {
                r.setValue(Copy3Columns.ENTIDAD_DEPOSITARIA, "25");
            } else {
                String entidadDepositaria = AnacreditMapper.getEntidadDepositaria(identifier);
                r.setValue(Copy3Columns.ENTIDAD_DEPOSITARIA, entidadDepositaria);
            }

            String  tipo_cartera_irfs9 = _mapper.getTipoCartera(BOCache.getBook(DSConnection.getDefault(), identifier.getBook().getId()));
            if(identifier.isEQNOT()) {
                tipo_cartera_irfs9 = "99";
            }

            r.setValue(Copy3Columns.TIPO_CARTERA_IFRS9, tipo_cartera_irfs9);


            if (identifier.isEQPLZ()) {
                r.setValue(Copy3Columns.JERARQUIA_VALOR_RAZONABLE, "0");
            }
            else {
                r.setValue(Copy3Columns.JERARQUIA_VALOR_RAZONABLE, _mapper.getJerarquiaValorRazonable(jerarquia));
            }

            if (identifier.isEQ()) {
                r.setValue(Copy3Columns.NUMERO_OCURRENCIAS, identifier.getNominal());
            }

            if (identifier.isEQ() || identifier.isEQNOT() ) {



                r.setValue(Copy3Columns.IMPORTE_LIQ_VENTAS_MES, +0.00d);
                r.setValue(Copy3Columns.IMPORTE_LIQ_COMPRAS_MES, +0.00d);

                Double dTotalBuy = reportRow.getProperty(IMPORTE_LIQ_COMPRAS_MES);
                if (dTotalBuy != null) {
                    EurBasedAmount totalBuy = new EurBasedAmount(identifier.getCcy(), dTotalBuy).invoke(valDate, pEnvOfficial);
                    totalBuy.forceSigno(-1);
                    r.setValue(Copy3Columns.IMPORTE_LIQ_COMPRAS_MES, totalBuy.getEurAmount());
                    r.keep(Copy3Columns.IMPORTE_LIQ_COMPRAS_MES.name(), totalBuy);
                }

                Double dTotalSell = reportRow.getProperty(IMPORTE_LIQ_VENTAS_MES);
                if (dTotalSell != null) {
                    EurBasedAmount totalSell = new EurBasedAmount(identifier.getCcy(), dTotalSell).invoke(valDate, pEnvOfficial);
                    totalSell.forceSigno(1);
                    r.setValue(Copy3Columns.IMPORTE_LIQ_VENTAS_MES, totalSell.getEurAmount());
                    r.keep(Copy3Columns.IMPORTE_LIQ_VENTAS_MES.name(), totalSell);
                }
            }

            r.setValue(Copy3Columns.PLAZO_ORIGEN_M, AnacreditConstants.EMPTY_STRING);

            if (identifier.isEQPRF() || identifier.isEQPLZ())  {
                r.setValue(Copy3Columns.OPERACION_NO_DECLARABLE_CIRBE, "0");
            }
            else {
                r.setValue(Copy3Columns.OPERACION_NO_DECLARABLE_CIRBE, "1");
            }

            if (identifier.isEQPLZ())  {
                r.setValue(Copy3Columns.TIPO_CODIGO_VALOR, "0");
            }

            if (identifier.isEQPLZ()) {
                String carteraPrudencial = AnacreditMapper.getCarteraPrudencial(String.valueOf(r.getValue(Copy3Columns.TIPO_CARTERA_IFRS9)));
                r.setValue(Copy3Columns.CARTERA_PRUDENCIAL, carteraPrudencial);
            }
            else {
                r.setValue(Copy3Columns.CARTERA_PRUDENCIAL, "ZZZ");
            }

            if (identifier.isEQPLZ() || identifier.isEQPRF()) {
                r.setValue(Copy3Columns.FRECUENCIA_PAGO_PRINC_INT, "4");
            }
            else {
                r.setValue(Copy3Columns.FRECUENCIA_PAGO_PRINC_INT, AnacreditConstants.EMPTY_STRING);
            }
            r.setValue(Copy3Columns.FECHA_BAJA_DEF_CIRBE, AnacreditConstants.STR_MAX_DATE_99991231);
            r.setValue(Copy3Columns.FECHA_ESTADO_CUMPLIMIENTO , r.getValue(Copy3Columns.FECHA_EMISION_ORIGINACION));
            r.setValue( Copy3Columns.FECHA_REFIN_REEST, AnacreditConstants.STR_MAX_DATE_99991231);
            r.setValue(Copy3Columns.TIPO_REFERENCIA_VENCIMIENTO, 0);
            r.setValue(Copy3Columns.CODIGO_RATIO_REFERENCIA,  AnacreditConstants.EMPTY_STRING);
            r.setValue(Copy3Columns.SALDO_VIVO_NOMINAL, +0);
            r.setValue(Copy3Columns.FASE_DETERIORO, "0");
            r.setValue(Copy3Columns.IMPORTE_LIBROS_ACTIVO, +0);
            r.setValue(Copy3Columns.COBERTURA_ACUMULADA, +0);
            r.setValue(Copy3Columns.PROVISION_FUERA_BALANCE, +0);
            r.setValue(Copy3Columns.ACTIVOS_VALOR_RAZOBL_RSGO_CRE, +0);
            r.setValue(Copy3Columns.SALDO_FUERA_BALANCE, +0);
            r.setValue(Copy3Columns.DERECHOS_REEMBOLSO, 2);
            r.setValue(Copy3Columns.TIPO_FUENTE_DE_CARGA, 1);
            r.setValue(Copy3Columns.SIN_DEVENGO_INTERES, "1");

            if (identifier.isEQPLZ()) {
                r.setValue(Copy3Columns.ID_CONTRATO, identifier.getTrade().getKeywordValue(TKW_PARTENON_ACCOUNTING_ID));
                r.setValue(Copy3Columns.ID_CONTRATO_INTERNO, String.valueOf(identifier.getTrade().getLongId()));
            } else {
                String ccy = String.valueOf(r.getValue(Copy3Columns.MONEDA));
                StringBuilder sb = new StringBuilder();
                sb.append(identifier.getProduct().getSecCode("ISIN"));
                sb.append(paisNegocio);
                sb.append(ccy);
                sb.append(r.getValue(Copy3Columns.ENTIDAD_DEPOSITARIA));
                sb.append(tipo_cartera_irfs9.trim());
                sb.append(r.getValue(Copy3Columns.PRODUCTO_AC));
                r.setValue(Copy3Columns.ID_CONTRATO, sb.toString());
                r.setValue(Copy3Columns.ID_CONTRATO_INTERNO, sb.toString());
            }

            //V2.19 - V2.20
            //r.keep(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO.name(), 0.0);
            //r.keep(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA.name(), 0.0);
            //r.setValue(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO, 0.0);
            //r.setValue(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA, 0.0);

            r.setValue(Copy3Columns.ID_ENTIDAD, getIdEntidad(identifier));
            r.setValue(Copy3Columns.ID_CENTRO_CONTABLE,  getCentroContable(identifier));

            return r;

        } catch (Exception e) {
            Log.error(this, "Error creating COPY3 Record. Will be ignored!", e);
        }
        return null;
    }

    private EquityTypeIdentifier buildIdentifier(JDatetime valDate, ReportRow reportRow) {
        EquityTypeIdentifier eqType = null;
        if (reportRow.getProperty(ReportRow.INVENTORY) != null) {
            eqType = new EquityTypeIdentifier(reportRow.getProperty(ReportRow.INVENTORY));
        } else {
            eqType = new EquityTypeIdentifier(valDate, reportRow.getProperty(ReportRow.TRADE));
        }
        return eqType;
    }

    protected String  checkLegalEntityAttrJMIN(Vector errorMsgs, Long identifier, LegalEntity legalEntity) {
        if (null == legalEntity) {
            log(LogLevel.ERR, String.valueOf(identifier), " - LegalEntity is null.", errorMsgs );
        }

        String jMinAttr = _mapper.getJMin(legalEntity);
        if (Util.isEmpty(jMinAttr)) {
            log(LogLevel.ERR, String.valueOf(identifier), " - J MIN not found for legalEntity " + legalEntity.getCode(), errorMsgs );
        }
        return jMinAttr;
    }

    public List<Copy4ARecord> formatCopy4APersona(ReportRow reportRow, Vector errorMsgs) {

        try {
            ArrayList<Copy4ARecord> personas = new ArrayList<>();

            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());


            EquityTypeIdentifier identifier = buildIdentifier(valDatetime, reportRow);

            Copy3Record copy3Record = reportRow.getProperty(AnacreditConstants.COPY_3);

            LegalEntity persona = identifier.isEQPLZ() ?
                    identifier.getCounterPartyOrAgent() : identifier.getProductIssuer();

            String jminPersona = AnacreditMapper.getJMin(persona);
            if (Util.isEmpty(jminPersona)) {
                return personas;
            }

            Copy4ARecord copy4ARecord = new Copy4ARecord();
            copy4ARecord.initializeFromCopy3Record(valDate, copy3Record);
            copy4ARecord.setValue(Copy4AColumns.FECHA_ALTA_RELACION, copy3Record.getValue(Copy3Columns.FECHA_EMISION_ORIGINACION));
            copy4ARecord.setValue(Copy4AColumns.FECHA_BAJA_RELACION, copy3Record.getValue(Copy3Columns.FECHA_VENCIMIENTO));


            if (identifier.isEQDES()) {
                copy4ARecord.setValue(Copy4AColumns.NATURALEZA_INTERVENCION, "56");
            }
            else {
                copy4ARecord.setValue(Copy4AColumns.NATURALEZA_INTERVENCION, "10");
            }
            copy4ARecord.setValue(Copy4AColumns.ID_PERSONA, jminPersona);

            copy4ARecord.setValue(Copy4AColumns.ID_ENTIDAD, getIdEntidad(identifier));

            personas.add(copy4ARecord);
            return personas;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy4A Records. Will be Ignored", e);
        }
        return null;
    }

    public List<Copy4Record> formatImportesCopy4(ReportRow reportRow, Vector errorMsgs) {
        try {
            ArrayList<Copy4Record> importes = new ArrayList<>();
            Copy3Record copy3Record = reportRow.getProperty(AnacreditConstants.COPY_3);

            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            EquityTypeIdentifier identifier = buildIdentifier(valDatetime, reportRow);

            EurBasedAmount saldoDeudorNoVencido = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO.name());
            if (saldoDeudorNoVencido != null && saldoDeudorNoVencido.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "02");
                Double value = saldoDeudorNoVencido.getOriginalAmount();
                Double valueEur = saldoDeudorNoVencido.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(value));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(valueEur));
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }

            EurBasedAmount interesDevengados = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.INTERESES_DEVENGADOS.name());
            if (interesDevengados != null && interesDevengados.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "15");
                Double value = interesDevengados.getOriginalAmount();
                Double valueEur = interesDevengados.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(value));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(valueEur));
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }

            EurBasedAmount activosValorRazonable = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.ACTIVOS_VALOR_RAZOBL.name());
            if (activosValorRazonable != null && activosValorRazonable.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "17");
                Double value = activosValorRazonable.getOriginalAmount();
                Double valueEur = activosValorRazonable.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, value);
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, valueEur);
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }


            EurBasedAmount nominal = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.VALOR_NOMINAL.name());
            if (nominal != null && nominal.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "40");
                Double value = nominal.getOriginalAmount();
                Double valueEur = nominal.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, value);
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS,  valueEur);
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }

            EurBasedAmount valorRazonable = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.VALOR_RAZONABLE.name());
            if (valorRazonable != null && valorRazonable.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "61");
                Double value = valorRazonable.getOriginalAmount();
                Double valueEur = valorRazonable.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, value);
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, valueEur);
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }

            EurBasedAmount saldoAcreedor = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.SALDO_ACREEDOR.name());
            if (saldoAcreedor != null && saldoAcreedor.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "19");
                Double value = saldoAcreedor.getOriginalAmount();
                Double valueEur = saldoAcreedor.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, value);
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, valueEur);
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }

            EurBasedAmount importeLiqComprasMes = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.IMPORTE_LIQ_COMPRAS_MES.name());
            if (importeLiqComprasMes != null && importeLiqComprasMes.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "180");
                Double value = importeLiqComprasMes.getOriginalAmount();
                Double valueEur = importeLiqComprasMes.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(value));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(valueEur));
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }

            EurBasedAmount importesLiqVentasMes = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.IMPORTE_LIQ_VENTAS_MES.name());
            if (importesLiqVentasMes != null && importesLiqVentasMes.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "181");
                Double value = importesLiqVentasMes.getOriginalAmount();
                Double valueEur = importesLiqVentasMes.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forcePlus(value));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forcePlus(valueEur));
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }

            EurBasedAmount dividendosDevengadosMes = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.DIVIDENDOS_DEVENGADOS_MES.name());
            if (dividendosDevengadosMes != null && dividendosDevengadosMes.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "248");
                Double value = dividendosDevengadosMes.getOriginalAmount();
                Double valueEur = dividendosDevengadosMes.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forcePlus(value));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forcePlus(valueEur));
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }


            //V2.19 - V2.20
            EurBasedAmount reducAvalista = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA.name());
            if (reducAvalista != null && reducAvalista.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "687");
                Double value = reducAvalista.getOriginalAmount();
                Double valueEur = reducAvalista.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forcePlus(value));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forcePlus(valueEur));
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }

            EurBasedAmount reducEjecutado = (EurBasedAmount) copy3Record.retrieve(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO.name());
            if (reducEjecutado != null && reducEjecutado.getOriginalAmount() != 0.00d) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "688");
                Double value = reducEjecutado.getOriginalAmount();
                Double valueEur = reducEjecutado.getEurAmount();
                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forcePlus(value));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forcePlus(valueEur));
                copy4Record.setValue(Copy4Columns.ID_ENTIDAD, getIdEntidad(identifier));
                importes.add(copy4Record);
            }




            return importes;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy4 Records. Will be Ignored", e);
        }
        return null;

    }

    private JDatetime   getFechaEmisionOriginacion(EquityTypeIdentifier identifier) {
        StringBuilder sql = new StringBuilder(" select min(settlement_date) ");
        sql.append(" from trade,  product_desc ");
        sql.append(" where ");
        sql.append(" product_desc.product_id  = trade.product_id ");
        sql.append(" and product_desc.und_security_id = " + identifier.getUnderlying().getId() );

        final Vector<?> rawResultSet;
        try {
            rawResultSet = getReportingService().executeSelectSQL(sql.toString());
            if (rawResultSet.size() > 2) {
                final Vector<Vector<JDatetime>> result = SantanderUtil.getInstance().getDataFixedResultSetWithType(rawResultSet,
                        JDatetime.class);
                for (final Vector<JDatetime> v : result) {
                    if (v.get(0) instanceof JDatetime){
                        final JDatetime date = v.get(0);
                        if (date != null) {
                            return date;
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, "Error executing SQL , e");
        }
        return null;

    }


    private String getIdEntidad(EquityTypeIdentifier identifier){
        String idEntidad = AnacreditConstants.STR_ENTIDAD_0049;
        if (identifier.isEQPLZ() && identifier.getTrade()!=null) {
            Trade trade = identifier.getTrade();
            if(trade!=null) {
                Book book = trade.getBook();
                if (book != null) {
                    String entity = BOCreUtils.getInstance().getEntity(book.getName());
                    idEntidad = BOCreUtils.getInstance().getEntityCod(entity, true);
                }
            }
        }
        else if (identifier.getPosition()!=null) {
            InventorySecurityPosition position = identifier.getPosition();
            Book book = position.getBook();
            if (book!=null) {
                String entity = BOCreUtils.getInstance().getEntity(book.getName());
                idEntidad = BOCreUtils.getInstance().getEntityCod(entity, true);
            }
        }
        return idEntidad;
    }


    private String getCentroContable(EquityTypeIdentifier identifier){
        String idCentroContable = AnacreditConstants.STR_ID_CENTRO_CONTABLE;
        if (identifier.isEQPLZ() && identifier.getTrade()!=null) {
            String partenonAccountingId = identifier.getTrade().getKeywordValue(TKW_PARTENON_ACCOUNTING_ID);
            idCentroContable = (!Util.isEmpty(partenonAccountingId) && partenonAccountingId.length()>=9) ? partenonAccountingId.substring(4,8) : idCentroContable;
        }
        else if (identifier.getPosition()!=null) {
            InventorySecurityPosition position = identifier.getPosition();
            Product product = position.getProduct();
            Book book = position.getBook();
            if (product!=null && book!=null) {
                String entity = BOCreUtils.getInstance().getEntity(book.getName());
                idCentroContable = BOCreUtils.getInstance().getCentroContable(product, entity, false);
            }
        }
        return idCentroContable;
    }


}
