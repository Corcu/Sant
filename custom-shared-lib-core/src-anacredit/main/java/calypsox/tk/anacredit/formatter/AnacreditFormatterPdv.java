package calypsox.tk.anacredit.formatter;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.ParseUtil;
import calypsox.tk.anacredit.api.copys.*;
import calypsox.tk.anacredit.processor.CacheModuloD;
import calypsox.tk.anacredit.util.ActivoOpValores;
import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.anacredit.util.AnacreditUtilities;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.report.AnacreditPdvReportStyle;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

public class AnacreditFormatterPdv extends AnacreditFormatter {

    private static final String TKW_PARTENON_ACCOUNTING_ID = "PartenonAccountingID";
    private static final String KEEP_INTERES_DEVENGADOS = "INTERES_DEVENGADOS";
    private static final String KEEP_INTERES_DEVENGADOS_EUR = "INTERES_DEVENGADOS_EUR";
    private static final String KEEP_NOMINAL_EUR = "NOMINAL_EUR";
    private static final String KEEP_NOMINAL_DIVISA = "NOMINAL_DIV";
    private static final String KEEP_SALDO_CTG_EUR = "SALDO_CTG_EUR";
    private static final String KEEP_SALDO_CTG = "SALDO_CTG";
    private static final String KEEP_SALDO_DEUDOR_NOVENC_EUR = "SALDO_DEUD_NOVENC_EUR";
    private static final String KEEP_SALDO_DEUDOR_NOVENC = "SALDO_DEUD_NOVENC";

    private static final String KEEP_REDUCCION_PRINCIPAL_IMPORTE_AVALISTA_EUR = "REDUCCION_PRINCIPAL_IMPORTE_AVALISTA_EUR";
    private static final String KEEP_REDUCCION_PRINCIPAL_IMPORTE_AVALISTA = "REDUCCION_PRINCIPAL_IMPORTE_AVALISTA";
    private static final String KEEP_REDUCCION_PRINCIPAL_AVALES_EJECUTADO_EUR = "REDUCCION_PRINCIPAL_AVALES_EJECUTADO_EUR";
    private static final String KEEP_REDUCCION_PRINCIPAL_AVALES_EJECUTADO = "REDUCCION_PRINCIPAL_AVALES_EJECUTADO";

    private static AnacreditPdvReportStyle _style = new AnacreditPdvReportStyle();
    /**
     * Format RECORD Copy3 - Operaciones
     * @param reportRow
     * @param errorMsgs
     * @return
     */
    public Copy3Record formatRecOperacionesCopy3(ReportRow reportRow, PricingEnv pEnv, Vector holidays, Vector errorMsgs) {
        try {

            Copy3Record copy3Record = new Copy3Record();

            Trade trade = reportRow.getProperty(ReportRow.TRADE);
            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            JDate valDatePlus  = valDate.addBusinessDays(1, holidays);
            SecLending secLending = (SecLending) trade.getProduct();
            Product underlying = secLending.getSecurity();

            LegalEntity counterparty = trade.getCounterParty();

            JDate secLendingEndDate = secLending.getEndDate();
            if (secLendingEndDate == null) {
                secLendingEndDate = valDate;
            }

            if (Util.isEmpty(trade.getKeywordValue(TKW_PARTENON_ACCOUNTING_ID))) {
                log(AnacreditFormatter.LogLevel.ERR, trade.getLongId(), " - Attribute PARTENON ID is empty.", errorMsgs );
                return null;
            }

            String idContrato = trade.getKeywordValue(TKW_PARTENON_ACCOUNTING_ID);
            if (secLending.isOpen()) {
                idContrato = idContrato + "_" + ParseUtil.formatDate(valDate,8);
            }
            copy3Record.setValue(Copy3Columns.ID_CONTRATO, idContrato);
            copy3Record.setValue(Copy3Columns.ID_CONTRATO_INTERNO, trade.getLongId());

            boolean isSellTrade = _mapper.isSell(trade);
            String productISIN = underlying.getSecCode("ISIN");
            if (Util.isEmpty(productISIN)) {
                return null;
            }

            // Check JMIN Conterparty
            String jminCpty = checkLegalEntityAttrJMIN(errorMsgs, trade.getLongId(), counterparty);
            if (Util.isEmpty(jminCpty))  {
                return null;
            }

            LegalEntity productIssuer = getProductIssuer(underlying);
            if (productIssuer == null) {
                log(AnacreditFormatter.LogLevel.ERR, trade.getLongId(), " - Product Issuer not found for ISIN =" + underlying.getSecCode("ISIN"), errorMsgs );
                return null;
            }

            // Check JMIN Issuer
            String jminIssuer = checkLegalEntityAttrJMIN(errorMsgs, trade.getLongId(), productIssuer);
            if (Util.isEmpty(jminIssuer))  {
                return null;
            }


            if (valDate.before(secLending.getStartDate())) {
                // Operaciones em Compromiso no salem
                return null;
            }

            copy3Record.setValue(Copy3Columns.ID_PERSONA_CONTRAPARTE_DIRECT, jminCpty);
            copy3Record.setValue(Copy3Columns.FECHA_EXTRACCION, valDate);

            copy3Record.setValue(Copy3Columns.CODIGO_VALOR, productISIN);

            String paisNegocio = _mapper.getPaisNegocio(trade.getBook().getLegalEntity());
            copy3Record.setValue(Copy3Columns.PAIS_NEGOCIO, paisNegocio);

            //PRODUCTO
            if (isSellTrade) {
                copy3Record.setValue(Copy3Columns.PRODUCTO_AC, AnacreditConstants.STR_TV010);
            } else {
                copy3Record.setValue(Copy3Columns.PRODUCTO_AC, AnacreditConstants.STR_TV011);
            }

            //PRODUCTO_AC
            if (underlying instanceof Equity) {
                if (isSellTrade) {
                    copy3Record.setValue(Copy3Columns.PRODUCTO_ENTIDAD, AnacreditConstants.STR_SLEV);
                } else {
                    copy3Record.setValue(Copy3Columns.PRODUCTO_ENTIDAD, AnacreditConstants.STR_SLEC);
                }
            }
            if (underlying instanceof Bond) {
                if (isSellTrade) {
                    copy3Record.setValue(Copy3Columns.PRODUCTO_ENTIDAD, AnacreditConstants.STR_PREV);
                } else {
                    copy3Record.setValue(Copy3Columns.PRODUCTO_ENTIDAD, AnacreditConstants.STR_PREC);
                }
            }

            //ACTIVO_OP_VALORES
            //String activoOpValores = _mapper.getActivoOpValores(underlying);
            String activoOpValores = ActivoOpValores.instance().get(underlying);
            copy3Record.setValue(Copy3Columns.ACTIVO_OP_VALORES, activoOpValores);


            if (secLending.isOpen()) {
                copy3Record.setValue(Copy3Columns.FECHA_EMISION_ORIGINACION, valDate);
            } else {
                copy3Record.setValue(Copy3Columns.FECHA_EMISION_ORIGINACION, secLending.getStartDate());
            }

            copy3Record.setValue(Copy3Columns.FECHA_ESTADO_CUMPLIMIENTO, copy3Record.getValue(Copy3Columns.FECHA_EMISION_ORIGINACION));

            JDate fechaVencimiento = valDatePlus;
            if (!secLending.isOpen()) {
                fechaVencimiento = secLendingEndDate;
            }
            copy3Record.setValue(Copy3Columns.FECHA_VENCIMIENTO, fechaVencimiento);
            copy3Record.setValue(Copy3Columns.FECHA_CANCELACION, fechaVencimiento);


            // Used in Copy4a
            copy3Record.keep(Copy3Columns.FECHA_VENCIMIENTO.name(), fechaVencimiento);

            if(isSellTrade) {
                copy3Record.setValue(Copy3Columns.ORIGEN_OPERACION, "O10");
            } else {
                copy3Record.setValue(Copy3Columns.ORIGEN_OPERACION, AnacreditConstants.EMPTY_STRING);
            }


            if (isSellTrade) {
                copy3Record.setValue(Copy3Columns.PORCENT_PARTICIP_SINDICADOS, 100.00);
            } else {
                copy3Record.setValue(Copy3Columns.PORCENT_PARTICIP_SINDICADOS, 0);
            }


            //SITUACION_OPE_RIESGO_DIREC_AC
            if (isSellTrade) {
                copy3Record.setValue(Copy3Columns.SITUACION_OPE_RIESGO_DIREC_AC , AnacreditConstants.STR_S10);
            } else {
                copy3Record.setValue(Copy3Columns.SITUACION_OPE_RIESGO_DIREC_AC , AnacreditConstants.STR_S140);
            }

            //COTIZA
            String jerarquia = _mapper.getSecCodeJerarquia(underlying);
            copy3Record.setValue(Copy3Columns.COTIZA, _mapper.getCotiza(jerarquia));

            Double nominal = reportRow.getProperty("Nominal");
            if (nominal == null || nominal.isNaN() || nominal  == 0.0d ) {
                log(AnacreditFormatter.LogLevel.ERR, trade.getLongId(), " - Invalid NOMINAL" , errorMsgs );
                return null;
            }

            Double saldoContingente = reportRow.getProperty("NPV_COLLAT");

            if (saldoContingente == null || saldoContingente.isNaN() || saldoContingente  == 0.0d ) {
                log(AnacreditFormatter.LogLevel.ERR, trade.getLongId(), " - Invalid SALDO CONTINGENTE" , errorMsgs );
                return null;
            }

            /*
            if (Double.compare(0.00d, nominal) == 0
                    && Double.compare(0.00d, saldoContingente) == 0) {
                log(AnacreditFormatter.LogLevel.ERR, trade.getLongId(), " - Nominal / Saldo Contingente are ZEROS" , errorMsgs );
                return null;
            }
            */
            saldoContingente = Math.abs(saldoContingente)*-1;
            nominal = Math.abs(nominal)*-1;

            double nominalEur = AnacreditUtilities.convertToEUR(nominal, underlying.getCurrency(),
                    valDate, null);
            copy3Record.keep(KEEP_NOMINAL_DIVISA, nominal);
            copy3Record.keep(KEEP_NOMINAL_EUR, nominalEur);
            copy3Record.setValue(Copy3Columns.VALOR_NOMINAL, nominalEur);

            Double saldoContingenteEUR  = AnacreditUtilities.convertToEUR(saldoContingente,
                    underlying.getCurrency(), valDate, null);

            copy3Record.keep(KEEP_SALDO_CTG, saldoContingente);
            copy3Record.keep(KEEP_SALDO_CTG_EUR, saldoContingenteEUR);
            copy3Record.setValue(Copy3Columns.SALDO_CONTINGENTE, saldoContingenteEUR);

            String  tipo_cartera_irfs9 = _mapper.getTipoCartera(BOCache.getBook(DSConnection.getDefault(), trade.getBook().getId()));
            copy3Record.setValue(Copy3Columns.TIPO_CARTERA_IFRS9, tipo_cartera_irfs9);

            copy3Record.setValue(Copy3Columns.JERARQUIA_VALOR_RAZONABLE, _mapper.getJerarquiaValorRazonable(jerarquia));

            copy3Record.setValue(Copy3Columns.OPERACION_NO_DECLARABLE_CIRBE, isSellTrade ? "0" : "1");

            copy3Record.setValue(Copy3Columns.PRODUCTO_SUBPRODUCTO, copy3Record.getValue(Copy3Columns.PRODUCTO_ENTIDAD));

            String carteraPrudencial = AnacreditMapper.getCarteraPrudencial(String.valueOf(copy3Record.getValue(Copy3Columns.TIPO_CARTERA_IFRS9)));
            copy3Record.setValue(Copy3Columns.CARTERA_PRUDENCIAL, carteraPrudencial);

            copy3Record.setValue(Copy3Columns.FECHA_REFIN_REEST, AnacreditConstants.STR_MAX_DATE_99991231);

            if (isSellTrade) {
                copy3Record.setValue(Copy3Columns.FRECUENCIA_PAGO_PRINC_INT, "4");
            }

            copy3Record.setValue(Copy3Columns.FASE_DETERIORO, "0");

            String  derechos_reenbolso = _mapper.getDerechosReenbolso(String.valueOf(copy3Record.getValue(Copy3Columns.PRODUCTO_AC)));
            copy3Record.setValue(Copy3Columns.DERECHOS_REEMBOLSO, derechos_reenbolso);


            copy3Record.setValue(Copy3Columns.OPERACION_TITULARES_EXONERADOS, "0");

            //PLAZO RESIDUAL
            if (!isSellTrade) {
                copy3Record.setValue(Copy3Columns.PLAZO_RESIDUAL, "P8");
            } else {
                if (secLending.isOpen()) {
                    copy3Record.setValue(Copy3Columns.PLAZO_RESIDUAL, "P2");
                } else  {
                    long difference = valDate.getDate().getTime() - secLendingEndDate.getDate().getTime();
                    Long daysBetween = Math.abs(difference / (1000*60*60*24));
                    String plazo = _mapper.getPlazoResidual(daysBetween.intValue());
                    copy3Record.setValue(Copy3Columns.PLAZO_RESIDUAL, plazo);
                }
            }



            copy3Record.setValue(Copy3Columns.TIPO_GARANTIA_REAL_PPAL, 999);
            copy3Record.setValue(Copy3Columns.COBERTURA_GARANTIA_REAL, "C3");

            String ccy = _mapper.getCurrencyMap(underlying.getCurrency(), errorMsgs, String.valueOf(trade.getLongId()));
            copy3Record.setValue(Copy3Columns.MONEDA, ccy);

            // Mapeos de Garantias (Solo para ventas)
            if (isSellTrade) {
               String tipoFuenteCarga = _mapper.getTipoFuenteCarga(underlying, productIssuer);
               copy3Record.setValue(Copy3Columns.TIPO_FUENTE_DE_CARGA, tipoFuenteCarga);
                MarginCallConfig mccContract = CacheModuloD.getMarginCallConfig(reportRow);
                if (mccContract != null) {
                   copy3Record.setValue(Copy3Columns.COBERTURA_GARANTIA_REAL, "C1");
                   copy3Record.setValue(Copy3Columns.TIPO_GARANTIA_REAL_PPAL, 173);
               }
            }

            copy3Record.setValue(Copy3Columns.DIFERENCIAL_SOBRE_INDICE_REFE , +909.09);

            if (isSellTrade) {
                copy3Record.setValue(Copy3Columns.ESQUEMA_AMORT_OPERACIONES_AC, AnacreditConstants.EMPTY_STRING);
            } else {
                copy3Record.setValue(Copy3Columns.ESQUEMA_AMORT_OPERACIONES_AC, "13");
            }

            copy3Record.setValue(Copy3Columns.ENTIDAD_DEPOSITARIA, "25");
            copy3Record.setValue(Copy3Columns.FINALIDAD_AC, AnacreditConstants.STR_F59);


            if (isSellTrade) {
                copy3Record.setValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC, AnacreditConstants.STR_M12);
            } else {
                copy3Record.setValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC, AnacreditConstants.STR_M16);

            }

            copy3Record.setValue(Copy3Columns.INDICE_REFERENCIA_AC , AnacreditConstants.STR_I99);

            if (AnacreditConstants.STR_M16.equals(copy3Record.getValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC))
                    || AnacreditConstants.STR_M9.equals(copy3Record.getValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC)))  {
                copy3Record.setValue(Copy3Columns.SIN_DEVENGO_INTERES, "1");
            } else {
                copy3Record.setValue(Copy3Columns.SIN_DEVENGO_INTERES, "0");
            }

            copy3Record.setValue(Copy3Columns.TIPO_REFERENCIA_VENCIMIENTO, 0);
            copy3Record.setValue(Copy3Columns.TIPO_REFINANC_MES, "R1");


            if (isSellTrade
                    && secLending.getFixedRate() != 0.00D) {
                String indice = Optional.ofNullable(copy3Record.getValue(Copy3Columns.INDICE_REFERENCIA_AC)).map(String::valueOf).orElse("");
                int tipoVencimiento = 1;
                if("I30".equalsIgnoreCase(indice) ||
                        "I31".equalsIgnoreCase(indice)){
                    tipoVencimiento = 0;
                }
                copy3Record.setValue(Copy3Columns.TIPO_REFERENCIA_VENCIMIENTO, tipoVencimiento);
            }

            copy3Record.setValue(Copy3Columns.FECHA_BAJA_DEF_CIRBE, AnacreditConstants.STR_MAX_DATE_99991231);
            copy3Record.setValue(Copy3Columns.INTERESES_DEVENGADOS, 0);


            //V2.19 - V2.20
            copy3Record.setValue(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO, 0.0);
            copy3Record.setValue(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA, 0);

            //copy3Record.keep(KEEP_REDUCCION_PRINCIPAL_AVALES_EJECUTADO, 0.0);
            //copy3Record.keep(KEEP_REDUCCION_PRINCIPAL_AVALES_EJECUTADO_EUR, 0.0);
            //copy3Record.keep(KEEP_REDUCCION_PRINCIPAL_IMPORTE_AVALISTA, 0.0);
            //copy3Record.keep(KEEP_REDUCCION_PRINCIPAL_IMPORTE_AVALISTA_EUR, 0.0);


            //V2.25
            copy3Record.setValue(Copy3Columns.REVOLVING, 0);
            copy3Record.setValue(Copy3Columns.INGRESOS_O, 0);
            copy3Record.setValue(Copy3Columns.CANAL_DISTRIBUCION, "D99");
            copy3Record.setValue(Copy3Columns.FILLER,  AnacreditConstants.EMPTY_STRING);
            copy3Record.setValue(Copy3Columns.OPERACION_PRECONCEDIDA,  AnacreditConstants.EMPTY_STRING);
            copy3Record.setValue(Copy3Columns.RDL_19_2022_MEDIDA_APLICADA,  0);
            copy3Record.setValue(Copy3Columns.RDL_19_2022_FECHA_MEDIDA_APLICADA, AnacreditConstants.EMPTY_STRING);
            copy3Record.setValue(Copy3Columns.RDL_19_2022_DEUDA_PENDIENTE, 0);
            copy3Record.setValue(Copy3Columns.RDL_19_2022_MESES_AMPLIACION_PLAZO, 0);

            String modalidadTipoInteres = (String) copy3Record.getValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC);

            if (!Util.isEmpty(modalidadTipoInteres)){
                if (modalidadTipoInteres.equals(AnacreditConstants.STR_M12)){
                    copy3Record.setValue(Copy3Columns.TIPO_INTERES_NOMINAL, ((SecLending) trade.getProduct()).getFeeBillingRate());
                }
                if (modalidadTipoInteres.equals(AnacreditConstants.STR_M16)){
                    copy3Record.setValue(Copy3Columns.TIPO_INTERES_NOMINAL, 0);
                }
            }
            copy3Record.setValue(Copy3Columns.AGENTE_O_INTERMEDIARIO,  AnacreditConstants.EMPTY_STRING);
            copy3Record.setValue(Copy3Columns.AGENTE_O_INTERMEDIARIO_ID_PERSONA,  AnacreditConstants.EMPTY_STRING);

            return copy3Record;

        } catch (Exception e) {
            Log.error(this, "Error creating COPY3 Record. Will be ignored!", e);
        }
        return null;
    }

    protected String  checkLegalEntityAttrJMIN(Vector errorMsgs, Long identifier, LegalEntity legalEntity) {
        if (null == legalEntity) {
            log(AnacreditFormatter.LogLevel.ERR, identifier, " - LegalEntity is null.", errorMsgs );
        }

        String jMinAttr = _mapper.getJMin(legalEntity);
        if (Util.isEmpty(jMinAttr)) {
            log(AnacreditFormatter.LogLevel.ERR, identifier, " - J MIN not found for legalEntity " + legalEntity.getCode(), errorMsgs );
        }
        return jMinAttr;
    }

    public List<Copy4ARecord> formatCopy4APersona(ReportRow reportRow, Vector errorMsgs) {

        try {
            ArrayList<Copy4ARecord> personas = new ArrayList<>();

            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            Trade trade = reportRow.getProperty(ReportRow.TRADE);

            Copy3Record copy3Record = reportRow.getProperty(AnacreditConstants.COPY_3);

            SecLending secLending = (SecLending) trade.getProduct();
            Product underlying = secLending.getSecurity();

            if (null == copy3Record.retrieve(Copy3Columns.FECHA_VENCIMIENTO.name()))  {
                log(AnacreditFormatter.LogLevel.ERR, trade.getLongId(), " - Error getting FECHA_VENCIMIENTO from Copy3", errorMsgs );
                return personas;
            }

            LegalEntity counterparty = trade.getCounterParty();
            String jminCpty = checkLegalEntityAttrJMIN(errorMsgs, trade.getLongId(), counterparty);
            if (Util.isEmpty(jminCpty)) {
                return personas;
            }

            LegalEntity issuer = getProductIssuer(underlying);
            String jminIssuer = checkLegalEntityAttrJMIN(errorMsgs, trade.getLongId(), issuer);
            if (Util.isEmpty(jminIssuer)) {
                return personas;
            }

            // Contrapartida
            Copy4ARecord copy4aCpty = new Copy4ARecord();
            copy4aCpty.initializeFromCopy3Record(valDate, copy3Record);

            //Fecha Baja Relacion - Primer dias Natual a la fecha vencimiento
            if (null != copy3Record.retrieve(Copy3Columns.FECHA_VENCIMIENTO.name()))  {
                JDate fechaVenc = (JDate) copy3Record.retrieve(Copy3Columns.FECHA_VENCIMIENTO.name());
                Calendar cal = Calendar.getInstance();
                cal.setTime(fechaVenc.getDate());
                cal.add(Calendar.MONTH, 1);
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
                JDate jDate = JDate.valueOf(cal.getTime());
                copy4aCpty.setValue(Copy4AColumns.FECHA_BAJA_RELACION, JDate.valueOf(cal.getTime()));
            }

            String naturaleza_intervencion = _mapper.isSell(trade) ? "10" : "01";
            copy4aCpty.setValue(Copy4AColumns.NATURALEZA_INTERVENCION, naturaleza_intervencion);
            copy4aCpty.setValue(Copy4AColumns.ID_PERSONA, jminCpty);

            // Issuer - Emissor
            Copy4ARecord copy4aIssuer = new Copy4ARecord();
            copy4aIssuer.initializeFromCopy3Record(valDate, copy3Record);

            boolean isSell = _mapper.isSell(trade);

            copy4aIssuer.setValue(Copy4AColumns.NATURALEZA_INTERVENCION, isSell ? "55" : "56");
            copy4aIssuer.setValue(Copy4AColumns.ID_PERSONA, jminIssuer);

            //Fecha Baja Relacion - Primer dias Natual a la fecha vencimiento
            if (null != copy3Record.retrieve(Copy3Columns.FECHA_VENCIMIENTO.name()))  {
                JDate fechaVenc = (JDate) copy3Record.retrieve(Copy3Columns.FECHA_VENCIMIENTO.name());
                Calendar cal = Calendar.getInstance();
                cal.setTime(fechaVenc.getDate());
                cal.add(Calendar.MONTH, 1);
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
                JDate jDate = JDate.valueOf(cal.getTime());
                copy4aIssuer.setValue(Copy4AColumns.FECHA_BAJA_RELACION, JDate.valueOf(cal.getTime()));
            }

            personas.add(copy4aCpty);
            personas.add(copy4aIssuer);
            return personas;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy4A Records. Will be Ignored", e);
        }
        return null;
    }

    public List<Copy4Record> formatImportesCopy4(ReportRow reportRow, Vector errorMsgs) {
        try {
            ArrayList<Copy4Record> importes = new ArrayList<>();
            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            Copy3Record copy3Record = reportRow.getProperty(AnacreditConstants.COPY_3);

            if (copy3Record.retrieve(KEEP_NOMINAL_DIVISA) != null) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "40");

                Double valorNominal = (Double) copy3Record.retrieve(KEEP_NOMINAL_DIVISA);
                Double valorNominalEur = (Double) copy3Record.retrieve(KEEP_NOMINAL_EUR);

                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(valorNominal));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(valorNominalEur));
                importes.add(copy4Record);
            }

            if (copy3Record.retrieve(KEEP_SALDO_CTG) != null) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "27");
                Double saldonCotingente = (Double) copy3Record.retrieve(KEEP_SALDO_CTG);
                Double saldonCotingenteEur = (Double) copy3Record.retrieve(KEEP_SALDO_CTG_EUR);

                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(saldonCotingente));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(saldonCotingenteEur));
                importes.add(copy4Record);
            }

            if (copy3Record.retrieve(KEEP_SALDO_DEUDOR_NOVENC) != null) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "02");
                Double saldoDeudorNoVencido = (Double) copy3Record.retrieve(KEEP_SALDO_DEUDOR_NOVENC);
                Double saldoDeudorNoVencidoEur = (Double) copy3Record.retrieve(KEEP_SALDO_DEUDOR_NOVENC_EUR);

                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(saldoDeudorNoVencido));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(saldoDeudorNoVencidoEur));
                importes.add(copy4Record);
            }


            if (copy3Record.retrieve(KEEP_INTERES_DEVENGADOS) != null) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "15");

                Double interesDevengados = (Double) copy3Record.retrieve(KEEP_INTERES_DEVENGADOS);
                Double interesDevengadosEur = (Double) copy3Record.retrieve(KEEP_INTERES_DEVENGADOS_EUR);

                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(interesDevengados));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(interesDevengadosEur));
                importes.add(copy4Record);
            }

            /////V2.19 - V2.20
            if (copy3Record.retrieve(KEEP_REDUCCION_PRINCIPAL_IMPORTE_AVALISTA) != null) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "687");

                Double reducAvalista = (Double) copy3Record.retrieve(KEEP_REDUCCION_PRINCIPAL_IMPORTE_AVALISTA);
                Double reducAvalistaEur = (Double) copy3Record.retrieve(KEEP_REDUCCION_PRINCIPAL_IMPORTE_AVALISTA_EUR);

                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(reducAvalista));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(reducAvalistaEur));
                importes.add(copy4Record);
            }

            if (copy3Record.retrieve(KEEP_REDUCCION_PRINCIPAL_AVALES_EJECUTADO) != null) {
                Copy4Record copy4Record = new Copy4Record();
                copy4Record.initializeFromCopy3(copy3Record);
                copy4Record.setValue(Copy4Columns.TIPO_IMPORTE, "688");

                Double reducEjecutado = (Double) copy3Record.retrieve(KEEP_REDUCCION_PRINCIPAL_AVALES_EJECUTADO);
                Double reducEjecutadoEur = (Double) copy3Record.retrieve(KEEP_REDUCCION_PRINCIPAL_AVALES_EJECUTADO_EUR);

                copy4Record.setValue(Copy4Columns.IMPORTE_DIVISA, forceMinus(reducEjecutado));
                copy4Record.setValue(Copy4Columns.IMPORTE_EUROS, forceMinus(reducEjecutadoEur));
                importes.add(copy4Record);
            }

            return importes;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy4 Records. Will be Ignored", e);
        }
        return null;

    }

    public List<Copy11Record> formatCopy11Garantias(CacheModuloD cache, ReportRow reportRow, Vector errorMsgs) {


        try {

            Trade trade = reportRow.getProperty(ReportRow.TRADE);
            SecLending secLending = (SecLending) trade.getProduct();

            if (!_mapper.isSell(trade)) {
                // Garantias para ventas solamente
                return null;
            }

            MarginCallConfig mccContract = CacheModuloD.getMarginCallConfig(reportRow);
            if (mccContract == null) {
                return  null;
            }


            String codigoGarantia = CacheModuloD.buildKey(trade, secLending);
            if (Util.isEmpty(codigoGarantia)) {
                return null;
            }

            ArrayList<Copy11Record> garantias = new ArrayList<>();
            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            Product underlying = secLending.getSecurity();

            Copy3Record copy3Record = reportRow.getProperty(AnacreditConstants.COPY_3);

            // Contrapartida
            Copy11Record copy11Record = new Copy11Record();
            copy11Record.setValue(Copy11Columns.FECHA_ALTA_RELACION, copy3Record.getValue(Copy3Columns.FECHA_EMISION_ORIGINACION));
            copy11Record.setValue(Copy11Columns.FECHA_DATOS, valDate);
            copy11Record.setValue(Copy11Columns.FECHA_BAJA_RELACION, AnacreditConstants.STR_MAX_DATE_99991231);
            copy11Record.setValue(Copy11Columns.CODIGO_OPERACION, copy3Record.getValue(Copy3Columns.ID_CONTRATO));
            copy11Record.setValue(Copy11Columns.TIPO_GARANTIA_REAL, "T03");
            copy11Record.setValue(Copy11Columns.ALCANCE_GARANTIA_REAL, "A01");
            copy11Record.setValue(Copy11Columns.CODIGO_GARANTIA_REAL, codigoGarantia);
            copy11Record.setValue(Copy11Columns.CODIGO_ACTIVO_REC_GARANTIA, copy11Record.getValue(Copy11Columns.CODIGO_GARANTIA_REAL));
            copy11Record.setValue(Copy11Columns.IMPORTE_HIPOTECARIA_PRIN, 0);
            copy11Record.setValue(Copy11Columns.IMPORTE_HIPOTECARIA_INT, 0);
            copy11Record.setValue(Copy11Columns.ACTIVOS_GARANTIA_INM, "A2");
            String tipoRecGarantia = _mapper.getTipoActivoRecGarantia(underlying);
            copy11Record.setValue(Copy11Columns.TIPO_ACTIVO_REC_GARANTIA, tipoRecGarantia);
            copy11Record.setValue(Copy11Columns.FECHA_FORMALIZACION_GARANTIA, AnacreditConstants.STR_MAX_DATE_99991231);
            JDatetime fechaFormalizacionFarantia = getFechaFormGarantia(mccContract, underlying);
            if (fechaFormalizacionFarantia != null) {
                copy11Record.setValue(Copy11Columns.FECHA_FORMALIZACION_GARANTIA, fechaFormalizacionFarantia);
            }

            if (copy3Record.retrieve(KEEP_SALDO_CTG_EUR) != null) {
                Double saldonCotingente = (Double) copy3Record.retrieve(KEEP_SALDO_CTG_EUR);
                copy11Record.setValue(Copy11Columns.IMPORTE_GARANTIA, saldonCotingente);
                copy11Record.setValue(Copy11Columns.IMPORTE_GARANTIA_CREDITICIA, saldonCotingente);
            } else {
                log(LogLevel.ERR, trade.getLongId(), "Invaid SALDO.CONTINGENTE ! copy11 skipped", errorMsgs);
                return null;
            }

            copy11Record.setValue(Copy11Columns.DERECHO_COBRO_GARANTIA, 0);
            copy11Record.setValue(Copy11Columns.VENCIMIENTO_COBERTURA, AnacreditConstants.STR_MAX_DATE_99991231);
            copy11Record.setValue(Copy11Columns.GARANTIA_PRINCIPAL_PRIORITARIA,"1");


            garantias.add(copy11Record);
            return garantias;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy11 Records. Will be Ignored", e);
        }
        return null;

    }

    private JDatetime   getFechaFormGarantia(MarginCallConfig mccContract, Product underlying) {
        StringBuilder sql = new StringBuilder(" select min(trade.trade_date_time) ");
        sql.append(" from trade, trade_keyword, product_desc ");
        sql.append(" where ");
        sql.append(" product_desc.product_id  = trade.product_id ");
        sql.append(" and product_desc.product_type in ('SecLending') ");
        sql.append(" and trade_keyword.trade_id = trade.trade_id ");
        sql.append(" and trade_keyword.keyword_name = 'MARGIN_CALL_CONFIG_ID' ");
        sql.append(" and trade_keyword.keyword_value = " + mccContract.getId() );
        sql.append(" and product_desc.und_security_id = " + underlying.getId() );
        final Vector<?> rawResultSet;
        try {
            rawResultSet = getReportingService().executeSelectSQL(sql.toString());
            if (rawResultSet.size() > 2) {
                final Vector<Vector<JDatetime>> result = SantanderUtil.getInstance().getDataFixedResultSetWithType(rawResultSet,
                        JDatetime.class);
                for (final Vector<JDatetime> v : result) {
                    final JDatetime date = v.get(0);
                    if (date != null) {
                        return date;
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Copy13Record> formatCopy13ActivosFinanceros(CacheModuloD cache, ReportRow reportRow, Vector errorMsgs) {
        try {
            Trade trade = reportRow.getProperty(ReportRow.TRADE);
            SecLending secLending = (SecLending) trade.getProduct();

            if (!_mapper.isSell(trade)) {
                // Garantias para ventas solamente
                return null;
            }

            MarginCallConfig mccContract = CacheModuloD.getMarginCallConfig(reportRow);
            if (mccContract == null) {
                return  null;
            }

            ArrayList<Copy13Record> activos = new ArrayList<>();
            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            Product underlying = secLending.getSecurity();

            // Contrapartida
            Copy13Record copy13 = new Copy13Record();

            copy13.setValue(Copy13Columns.FECHA_DATOS, valDate);

            String codigoGarantia = CacheModuloD.buildKey(trade, secLending);
            if (Util.isEmpty(codigoGarantia)) {
                return null;
            }
            copy13.setValue(Copy13Columns.CODIGO_ACTIVO_REC_GARANTIA, codigoGarantia);
            copy13.setValue(Copy13Columns.REFERENCIA_INTERNA, codigoGarantia);


            String tipoRecGarantia = _mapper.getTipoActivoRecGarantia(underlying);
            copy13.setValue(Copy13Columns.TIPO_ACTIVO_REC_GARANTIA, tipoRecGarantia);

            LegalEntity productIssuer = getProductIssuer(underlying);
            if (productIssuer == null) {
                log(AnacreditFormatter.LogLevel.ERR, trade.getLongId(), " - Product Issuer not found for ISIN =" + underlying.getSecCode("ISIN"), errorMsgs );
                return null;
            }
            // Check JMIN Issuer
            String jminIssuer = checkLegalEntityAttrJMIN(errorMsgs, trade.getLongId(), productIssuer);
            if (Util.isEmpty(jminIssuer))  {
                return null;
            }
            copy13.setValue(Copy13Columns.CODIGO_EMISOR, jminIssuer);
            copy13.setValue(Copy13Columns.CODIGO_VALOR, underlying.getSecCode("ISIN"));

            String jerarquia = underlying.getSecCode(AnacreditConstants.ATTR_ANACREDIT_JERARQUIA);
            String cotizaD = _mapper.getCotizaModuloD(jerarquia);
            copy13.setValue(Copy13Columns.COTIZA, cotizaD);


            Double nominalGarantia = cache.getTotalNominal(codigoGarantia);
            if (nominalGarantia == null || nominalGarantia.isNaN() || nominalGarantia.compareTo(0.0d) == 0) {
                log(AnacreditFormatter.LogLevel.ERR, trade.getLongId(), " - copy13 - INVALID Total Nominal for the instrument ", errorMsgs );
                return null;
            }

            copy13.setValue(Copy13Columns.VALOR_NOMINAL_GARANTIA, nominalGarantia);
            copy13.setValue(Copy13Columns.VALOR_ORIGINAL_GARANTIA, nominalGarantia);

            copy13.setValue(Copy13Columns.FECHA_BAJA_GARANTIA, AnacreditConstants.STR_MAX_DATE_99991231);

            copy13.setValue(Copy13Columns.GARANTIA_NO_DECLARABLE_CIRBE, 0);

            String jminCpty = checkLegalEntityAttrJMIN(errorMsgs, trade.getLongId(), trade.getCounterParty());
            if (Util.isEmpty(jminCpty))  {
                return null;
            }

            copy13.setValue(Copy13Columns.ID_PROVEEDOR_GARANTIA, jminCpty);

            copy13.setValue(Copy13Columns.FECHA_VALOR_ORIGINAL_GARANTIA, AnacreditConstants.STR_MAX_DATE_99991231);
            JDatetime valorOrigGarantia = getFechaFormGarantia(mccContract, underlying);
            if (valorOrigGarantia != null) {
                copy13.setValue(Copy13Columns.FECHA_VALOR_ORIGINAL_GARANTIA, valorOrigGarantia);
            }

            Double valorGarantia = cache.getTotalMarketValue(codigoGarantia);

            copy13.setValue(Copy13Columns.IMPORTE_GARANTIA, valorGarantia);
            copy13.setValue(Copy13Columns.TIPO_IMPORTE_GARANTIA, 1);
            copy13.setValue(Copy13Columns.MET_VALORACION_GARANTIA, 3);
            copy13.setValue(Copy13Columns.FECHA_VALORACION_GARANTIA, valDate);
            copy13.setValue(Copy13Columns.VERSION, "V0219");

            Double totalQtd = cache.getTotalQtd(codigoGarantia);
            // Convert to EUR to report on file
            if (totalQtd != null) {
                copy13.setValue(Copy13Columns.NUM_TITULOS_PARTICIPACIONES, totalQtd.intValue());
            }

            if (!cache.contains(codigoGarantia)) {
                activos.add(copy13);
                cache.addReported(codigoGarantia);
            }


            return activos;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy13 Records. Will be Ignored", e);
        }
        return null;
    }

}
