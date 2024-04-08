package calypsox.tk.anacredit.formatter;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.ParseUtil;
import calypsox.tk.anacredit.api.copys.*;
import calypsox.tk.anacredit.processor.CacheModuloD;
import calypsox.tk.anacredit.util.ActivoOpValores;
import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.anacredit.util.AnacreditUtilities;
import calypsox.tk.anacredit.util.RepoTypeIdentifier;
import calypsox.tk.confirmation.builder.repo.RepoReportUtil;
import calypsox.tk.report.AnacreditRepoReportStyle;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.flow.CashFlowSimple;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.math.BigDecimal;
import java.util.*;

public class AnacreditFormatterRepo extends AnacreditFormatter {

    private static final String TKW_PARTENON_ACCOUNTING_ID = "PartenonAccountingID";
    private static AnacreditRepoReportStyle _style = new AnacreditRepoReportStyle();
    private RepoReportUtil _repoReportUtil = new RepoReportUtil();
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

            Repo repo = (Repo) trade.getProduct();
            repo.calculateAll(repo.getFlows(), pEnv, valDate);

            Product underlying = repo.getSecurity();
            LegalEntity counterparty = trade.getCounterParty();

            String productISIN = underlying != null ? underlying.getSecCode("ISIN") : "";

            if (Util.isEmpty(trade.getKeywordValue(TKW_PARTENON_ACCOUNTING_ID))) {
                log(LogLevel.ERR, trade.getLongId(), " - Attribute PARTENON ID is empty.", errorMsgs );
                return null;
            }

            // Check JMIN Conterparty
            String jminCpty = checkLegalEntityAttrJMIN(errorMsgs, trade.getLongId(), counterparty);
            if (Util.isEmpty(jminCpty))  {
                return null;
            }

            LegalEntity productIssuer = getProductIssuer(repo.getSecurity());
            if (productIssuer == null) {
                log(LogLevel.ERR, trade.getLongId(), "  - LE not found for Security ISSUER of : " + repo.getSecCode("ISIN"), errorMsgs );
                return null;
            }

            String j_minoristaIssuer = _mapper.getJMin(productIssuer);
            if(Util.isEmpty(j_minoristaIssuer)) {
                log(LogLevel.ERR, trade.getLongId(), " - J MIN not found for Issuer LegalEntity: " + productIssuer.getCode(), errorMsgs );
                return null;
            }

            //_style.precalculateColumnValues(reportRow, new String[]{"Pricer.MTM_NET_MUREX", "Sec. Nominal (Current)"}, errorMsgs);

            RepoTypeIdentifier repoTypeIdentifier = new RepoTypeIdentifier(trade, valDatetime).invoke();

            boolean isRepoRPPLZ = repoTypeIdentifier.isRepoRPPLZ();
            boolean isCTA = repoTypeIdentifier.isCTA();
            boolean isATA = repoTypeIdentifier.isATA();

            boolean isTERM = repo.getMaturityType().equals("TERM");

            Double principal = reportRow.getProperty("Principal Amount");
            Double marketValue = reportRow.getProperty("Pricer.MTM_NET_MUREX");
            Double noRound = reportRow.getProperty("Sec. Nominal (Current)");
            Double nominal = new BigDecimal(noRound).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();


            Double interesDevengados = AnacreditUtilities.calculatePM(valDatetime, trade, new PricerMeasure(PricerMeasure.ACCRUAL_FIRST), "OFFICIAL_ACCOUNTING");
            if (interesDevengados == null || interesDevengados.isNaN()) {
                interesDevengados = 0.0D;
            }


            if (!isRepoRPPLZ
                    && (nominal == null || Double.compare(nominal, 0.00D) == 0)) {
                log(LogLevel.ERR, trade.getLongId(), "  - NOMINAL is Zero or invalid for CTA or ATA trade.", errorMsgs );
                return null;
            }

            Double nominalEUR =  AnacreditUtilities.convertToEUR(nominal, repo.getCurrency(), valDate, null);
            Double principalEUR =  AnacreditUtilities.convertToEUR(principal, repo.getCurrency(), valDate, null);
            Double marketValueEUR =  AnacreditUtilities.convertToEUR(marketValue, repo.getCurrency(), valDate, null);
            Double interesDevengadosEUR =  AnacreditUtilities.convertToEUR(interesDevengados, repo.getCurrency(), valDate, null);

            if (!isRepoRPPLZ
                    && (marketValue == null || Double.compare(marketValue, 0.00D) == 0)) {
                log(LogLevel.ERR, trade.getLongId(), "  - Market Value is Zero or invalid for CTA or ATA trade.", errorMsgs );
                return null;
            }


            JDate callableDate =  _repoReportUtil.getCallableOrProjectedDate(repo, valDate);

            JDate lastCFDate = null;
            JDate nextCFDate = null;
            JDate lastInterestDate = null;
            JDate nextInterestDate = null;

            copy3Record.setValue(Copy3Columns.FECHA_EXTRACCION, valDate);

            String idContrato = trade.getKeywordValue(TKW_PARTENON_ACCOUNTING_ID);
            if (!repo.getMaturityType().equals("TERM")) {
                idContrato = idContrato + "_" + ParseUtil.formatDate(callableDate,8);
            }

            copy3Record.setValue(Copy3Columns.ID_CONTRATO, idContrato);
            copy3Record.setValue(Copy3Columns.ID_CONTRATO_INTERNO, trade.getLongId());

            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.CODIGO_VALOR, AnacreditConstants.EMPTY_STRING);
            } else {
                copy3Record.setValue(Copy3Columns.CODIGO_VALOR, productISIN);
            }


            String paisNegocio = _mapper.getPaisNegocio(trade.getBook().getLegalEntity());
            copy3Record.setValue(Copy3Columns.PAIS_NEGOCIO, paisNegocio);

            //PRODUCTO
            if (!isRepoRPPLZ) {
                if (isATA) {
                    copy3Record.setValue(Copy3Columns.PRODUCTO_AC, "AP022");
                    copy3Record.setValue(Copy3Columns.PRODUCTO_ENTIDAD, "RPATA");
                } else {
                    copy3Record.setValue(Copy3Columns.PRODUCTO_AC, "PP030");
                    copy3Record.setValue(Copy3Columns.PRODUCTO_ENTIDAD, "RPCTA");
                }
            } else {
                copy3Record.setValue(Copy3Columns.PRODUCTO_AC, "CP010");
                copy3Record.setValue(Copy3Columns.PRODUCTO_ENTIDAD, "RPPLZ");
            }

            //ACTIVO_OP_VALORES

            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.ACTIVO_OP_VALORES, AnacreditConstants.EMPTY_STRING);
            } else {
                String activoOpValores = ActivoOpValores.instance().get(underlying);
                copy3Record.setValue(Copy3Columns.ACTIVO_OP_VALORES, activoOpValores);
            }

            if (isATA) {
                copy3Record.setValue(Copy3Columns.SUBORDINACION_PRODUCTO_AC,  "S0");
             } else {
                copy3Record.setValue(Copy3Columns.SUBORDINACION_PRODUCTO_AC,  "S4");

            }

            if (isATA) {
                copy3Record.setValue(Copy3Columns.FINALIDAD_AC, AnacreditConstants.STR_F37);
            } else {
                copy3Record.setValue(Copy3Columns.FINALIDAD_AC, AnacreditConstants.STR_F59);
            }

            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.FECHA_EMISION_ORIGINACION, trade.getTradeDate());
            } else if (isTERM) {
                copy3Record.setValue(Copy3Columns.FECHA_EMISION_ORIGINACION, repo.getStartDate());
            } else if (repoTypeIdentifier.isExtendable()) {
                copy3Record.setValue(Copy3Columns.FECHA_EMISION_ORIGINACION,  repo.getStartDate());
            } else {
                copy3Record.setValue(Copy3Columns.FECHA_EMISION_ORIGINACION, valDate);
            }


            JDate fecha_vencimiento = null;
            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.FECHA_CANCELACION, repo.getStartDate());
                fecha_vencimiento = repo.getStartDate();
            } else {
                fecha_vencimiento = callableDate;
                copy3Record.setValue(Copy3Columns.FECHA_CANCELACION, callableDate);
            }

            copy3Record.setValue(Copy3Columns.FECHA_VENCIMIENTO, fecha_vencimiento);
            copy3Record.keep(Copy3Columns.FECHA_VENCIMIENTO.name(), fecha_vencimiento);

            copy3Record.setValue(Copy3Columns.ORIGEN_OPERACION, "O10");

            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.CANAL_CONTRATACION_AC, AnacreditConstants.EMPTY_STRING);
            } else if (isATA) {
                copy3Record.setValue(Copy3Columns.CANAL_CONTRATACION_AC, AnacreditConstants.STR_C02);
            } else  {
                copy3Record.setValue(Copy3Columns.CANAL_CONTRATACION_AC, AnacreditConstants.STR_C10);
            }


            if (!isATA) {
                copy3Record.setValue(Copy3Columns.PROVINCIA_NEGOCIO, "90");
            } else if ("0724".equals(_mapper.getPaisNegocio(trade.getBook().getLegalEntity()))) {
                copy3Record.setValue(Copy3Columns.PROVINCIA_NEGOCIO, "28");
            } else {
                copy3Record.setValue(Copy3Columns.PROVINCIA_NEGOCIO, "90");
            }

            if (isRepoRPPLZ) {
                 copy3Record.setValue(Copy3Columns.ESQUEMA_AMORT_OPERACIONES_AC, AnacreditConstants.EMPTY_STRING);
            } else if (isATA) {
                copy3Record.setValue(Copy3Columns.ESQUEMA_AMORT_OPERACIONES_AC, "03");
            } else  {
                copy3Record.setValue(Copy3Columns.ESQUEMA_AMORT_OPERACIONES_AC, "13");
            }


            copy3Record.setValue(Copy3Columns.PORCENT_PARTICIP_SINDICADOS, 100.00);
             if (isCTA) {
                copy3Record.setValue(Copy3Columns.PORCENT_PARTICIP_SINDICADOS, 0);
            }

            String ccy = _mapper.getCurrencyMap(trade.getSettleCurrency(), errorMsgs, String.valueOf(trade.getLongId()));
            copy3Record.setValue(Copy3Columns.MONEDA, ccy);


            //PLAZO RESIDUAL
            if (isCTA) {
                copy3Record.setValue(Copy3Columns.PLAZO_RESIDUAL, "P8");
            } else {
                if (!isTERM) {
                    copy3Record.setValue(Copy3Columns.PLAZO_RESIDUAL, "P2");
                } else  {
                    String plazo = getPlazo(valDate, callableDate);
                    copy3Record.setValue(Copy3Columns.PLAZO_RESIDUAL, plazo);
                }
            }

            if (isATA) {
                copy3Record.setValue(Copy3Columns.TIPO_GARANTIA_REAL_PPAL, 173);
                copy3Record.setValue(Copy3Columns.COBERTURA_GARANTIA_REAL, "C1");
            } else {
                copy3Record.setValue(Copy3Columns.TIPO_GARANTIA_REAL_PPAL, 999);
                copy3Record.setValue(Copy3Columns.COBERTURA_GARANTIA_REAL, "C3");
            }


            // Copy3 siempre en EUR
            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.VALOR_NOMINAL, 0);
            }   else {
                int signal = isCTA ? 1 :-1;
                copy3Record.setValue(Copy3Columns.VALOR_NOMINAL, forceSignal(nominalEUR, signal));
                copy3Record.keep(Copy3Columns.VALOR_NOMINAL.name(), forceSignal(nominal, signal));
                 if (!"EUR".equals(repo.getCurrency())) {
                    copy3Record.keep(Copy3Columns.VALOR_NOMINAL.name()+"_EUR", forceSignal(nominalEUR, signal));
                }
            }

            copy3Record.setValue(Copy3Columns.SALDO_ACREEDOR,  0);
            if (isCTA) {
                copy3Record.setValue(Copy3Columns.SALDO_ACREEDOR,  forcePlus(principalEUR));
                copy3Record.keep(Copy3Columns.SALDO_ACREEDOR.name(),  forcePlus(principal));
                if (!"EUR".equals(repo.getCurrency())) {
                    copy3Record.keep(Copy3Columns.SALDO_ACREEDOR.name()+"_EUR", forcePlus(principalEUR));
                }
            }

            copy3Record.setValue(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO,  -0.00);
            if (!isCTA) {
                copy3Record.setValue(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO,  forceMinus(principalEUR));
                copy3Record.keep(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO.name(),  forceMinus(principal));
                if (!"EUR".equals(repo.getCurrency())) {
                    copy3Record.keep(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO.name()+"_EUR", forceMinus(principalEUR));
                }
            }

            copy3Record.setValue(Copy3Columns.ACTIVOS_VALOR_RAZOBL, 0);
            if (isATA) {
                copy3Record.setValue(Copy3Columns.ACTIVOS_VALOR_RAZOBL, changeDirection(marketValueEUR));
                copy3Record.keep(Copy3Columns.ACTIVOS_VALOR_RAZOBL.name(), changeDirection(marketValue));
                if (!"EUR".equals(repo.getCurrency())) {
                    copy3Record.keep(Copy3Columns.ACTIVOS_VALOR_RAZOBL.name()+"_EUR", changeDirection(marketValueEUR));
                }
            }

            copy3Record.setValue(Copy3Columns.INTERESES_DEVENGADOS, 0);
            if (isATA) {
                copy3Record.setValue(Copy3Columns.INTERESES_DEVENGADOS, changeDirection(interesDevengadosEUR));
                copy3Record.keep(Copy3Columns.INTERESES_DEVENGADOS.name(), changeDirection(interesDevengados));
                if (!"EUR".equals(repo.getCurrency())) {
                    copy3Record.keep(Copy3Columns.INTERESES_DEVENGADOS.name()+"_EUR", changeDirection(interesDevengadosEUR));
                }
            }



            // GASTOS_ACTIVADOS
            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.GASTOS_ACTIVADOS, "0");

            }
            copy3Record.setValue(Copy3Columns.TIPO_REFINANC_MES, "R1");

            if (isRepoRPPLZ || isCTA) {
                copy3Record.setValue(   Copy3Columns.FECHA_ULTIMO_VTO_INTERESES,  AnacreditConstants.STR_MAX_DATE_99991231);
                copy3Record.setValue(   Copy3Columns.FECHA_PROXIMO_VTO_INTERESES,  AnacreditConstants.STR_MAX_DATE_99991231);
                copy3Record.setValue(   Copy3Columns.FECHA_ULTIMO_VTO_PRINICIPAL,  AnacreditConstants.STR_MAX_DATE_99991231);
                copy3Record.setValue(   Copy3Columns.FECHA_PROXIMO_VTO_PRINCIPAL,  AnacreditConstants.STR_MAX_DATE_99991231);

            } else {
                copy3Record.setValue(   Copy3Columns.FECHA_ULTIMO_VTO_INTERESES,  valDate);
                copy3Record.setValue(   Copy3Columns.FECHA_PROXIMO_VTO_INTERESES,  copy3Record.getValue(Copy3Columns.FECHA_VENCIMIENTO));
                if (!isRepoCashFixed(repo)) {
                    CashFlowSet cashFlows = repo.getFlows();

                    for (CashFlow cf : cashFlows) {
                        if ("INTEREST".equals(cf.getType())
                                || "PRINCIPAL".equals(cf.getType())) {
                            if (cf.getDate().lte(valDate)) {
                                lastCFDate = cf.getDate();
                                if ("INTEREST".equals(cf.getType())) {
                                    lastInterestDate = cf.getDate();
                                }
                            }
                            if (cf.getDate().after(valDate)) {
                                nextCFDate = cf.getDate();
                                if ("INTEREST".equals(cf.getType())) {
                                    nextInterestDate = cf.getDate();
                                }
                                break;
                            }
                        }
                    }
                    if (lastCFDate != null) {
                        copy3Record.setValue(   Copy3Columns.FECHA_ULTIMO_VTO_INTERESES,  lastCFDate);
                    }
                    if (nextCFDate != null) {
                        copy3Record.setValue(   Copy3Columns.FECHA_PROXIMO_VTO_INTERESES,  nextCFDate);
                    }
                }

                copy3Record.setValue(   Copy3Columns.FECHA_ULTIMO_VTO_PRINICIPAL,  AnacreditConstants.EMPTY_STRING);
                copy3Record.setValue(   Copy3Columns.FECHA_PROXIMO_VTO_PRINCIPAL,  copy3Record.getValue(Copy3Columns.FECHA_VENCIMIENTO));
            }


            //MODULO E
            copy3Record.setValue(Copy3Columns.CODIGO_RATIO_REFERENCIA,  AnacreditConstants.EMPTY_STRING);
            copy3Record.setValue(Copy3Columns.TIPO_REFERENCIA_VENCIMIENTO, 0);
            copy3Record.setValue(Copy3Columns.PROXIMA_REVISION_TIPO_INTERES,  AnacreditConstants.STR_MIN_DATE_11111112);
            copy3Record.setValue(Copy3Columns.DIFERENCIAL_SOBRE_INDICE_REFE , +909.09);
            Double tedr = 0.0;
            if (isRepoRPPLZ || isCTA) {
                copy3Record.setValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC, AnacreditConstants.STR_M16);
                copy3Record.setValue(Copy3Columns.TEDR, isRepoRPPLZ ?
                                                            0 :  AnacreditConstants.EMPTY_STRING);
                copy3Record.setValue(Copy3Columns.INDICE_REFERENCIA_AC , AnacreditConstants.STR_I99);

                copy3Record.setValue(Copy3Columns.TIPO_REFERENCIA_SUSTITUTIVO,  11);
                copy3Record.setValue(Copy3Columns.FREC_REVISION_TIPO_INT_PER,   "0");
                if (isRepoRPPLZ) {
                    copy3Record.setValue(Copy3Columns.DIFERENCIAL_SOBRE_INDICE_REFE , 0);
                }
            } else {
                if (repo.isFixedRate()) {
                    copy3Record.setValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC, AnacreditConstants.STR_M12);
                    copy3Record.setValue(Copy3Columns.INDICE_REFERENCIA_AC , AnacreditConstants.STR_I99);
                    copy3Record.setValue(Copy3Columns.TEDR, repo.getCurrentFixedRate(valDate));
                    copy3Record.setValue(Copy3Columns.FREC_REVISION_TIPO_INT_PER,   "0");
                } else {
                    copy3Record.setValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC, AnacreditConstants.STR_M14);
                    String indice_referencia_ac = AnacreditConstants.STR_I29;
                    String rateFrec = _mapper.getFreqRevTipoInteres(repo.getCash().getPaymentFrequency());

                    copy3Record.setValue(   Copy3Columns.TIPO_REFERENCIA_SUSTITUTIVO,  4);
                    if (repo.getRateIndex() != null
                            && repo.getRateIndex().getDefaults() != null) {
                        indice_referencia_ac = _mapper.getRateCode(repo.getRateIndex().getDefaults());
                        if (Util.isEmpty(indice_referencia_ac)) {
                            log(LogLevel.ERR, trade.getLongId(), "Rate Index not mapped", errorMsgs);
                        }
                    }

                    Double spread = repo.getSpread();
                    Double rateIndexQuote = _mapper.getRateIndexQuote(repo.getRateIndex(),valDate);
                    tedr = rateIndexQuote + spread;
                    copy3Record.setValue(Copy3Columns.INDICE_REFERENCIA_AC , indice_referencia_ac);
                    copy3Record.setValue(Copy3Columns.TEDR, tedr);
                    String refVencimiento = "1";
                    if("I30".equalsIgnoreCase(indice_referencia_ac) ||
                            "I31".equalsIgnoreCase(indice_referencia_ac) ){
                        refVencimiento = "0";
                    }
                    copy3Record.setValue(Copy3Columns.TIPO_REFERENCIA_VENCIMIENTO, refVencimiento);
                    copy3Record.setValue(Copy3Columns.DIFERENCIAL_SOBRE_INDICE_REFE , Math.abs(spread));
                    copy3Record.setValue(Copy3Columns.FREC_REVISION_TIPO_INT_PER, rateFrec);
                    // Next date for interest reset
                    JDate cfDate = null;
                    CashFlowSet cashFlows = repo.getFlows();
                    for (CashFlow cf : cashFlows) {
                        cf.getDate();
                        if (cf.getDate().gte(valDate)) {
                            if ("INTEREST".equals(cf.getType())) {
                                cfDate = cf.getDate();
                                if (cf instanceof CashFlowSimple) {
                                    CashFlowSimple cfS = (CashFlowSimple) cf;
                                    if (cfS.getResetDate() != null
                                                && cfS.getResetDate().gte(valDate)) {
                                        cfDate =  cfS.getResetDate();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (cfDate != null) {
                        copy3Record.setValue(   Copy3Columns.PROXIMA_REVISION_TIPO_INTERES,  cfDate);
                    }
                }
            }
            copy3Record.setValue(Copy3Columns.ID_PERSONA_CONTRAPARTE_DIRECT, jminCpty);

            if (isCTA) {
                copy3Record.setValue(Copy3Columns.SITUACION_OPE_RIESGO_DIREC_AC , AnacreditConstants.STR_S140);
            } else {
                copy3Record.setValue(Copy3Columns.SITUACION_OPE_RIESGO_DIREC_AC , AnacreditConstants.STR_S10);
            }

            String jerarquia = _mapper.getSecCodeJerarquia(underlying);

            //COTIZA
            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.COTIZA, AnacreditConstants.EMPTY_STRING);
            } else {
                copy3Record.setValue(Copy3Columns.COTIZA, _mapper.getCotiza(jerarquia));
            }


            copy3Record.setValue(Copy3Columns.ENTIDAD_DEPOSITARIA, "25");
            if (isRepoRPPLZ) {
                copy3Record.setValue(Copy3Columns.ENTIDAD_DEPOSITARIA, "24");
            }



            String  tipo_cartera_irfs9 = _mapper.getTipoCartera(BOCache.getBook(DSConnection.getDefault(), trade.getBook().getId()));
            copy3Record.setValue(Copy3Columns.TIPO_CARTERA_IFRS9, tipo_cartera_irfs9);
            copy3Record.setValue(   Copy3Columns.RECONOCIMIENTO_BALANCE, "1");
            if(_mapper.validAgent(trade.getCounterParty())){
                copy3Record.setValue(Copy3Columns.TIPO_CARTERA_IFRS9, "99");
                copy3Record.setValue(   Copy3Columns.RECONOCIMIENTO_BALANCE, "3");
            }

            copy3Record.setValue(Copy3Columns.FASE_DETERIORO, "0");
            copy3Record.setValue(Copy3Columns.MET_CALC_COBERT_RIESGO_DUDOSO, "0");
            copy3Record.setValue(Copy3Columns.MET_CALC_COBERT_RIESGO_NORMAL, "0");

            if (isATA) {
                if (!Arrays.asList("02_01", "11_01", "04_01", "12_01", "41_01").contains(tipo_cartera_irfs9)) {
                    //Formateo com 4
                    String riesgoAC = (String) copy3Record.getValue(Copy3Columns.SITUACION_OPE_RIESGO_DIREC_AC);
                    if (!Util.isEmpty(riesgoAC)) {
                        if (AnacreditConstants.STR_S10.equals(riesgoAC.trim())) {
                            copy3Record.setValue(Copy3Columns.FASE_DETERIORO, "23");
                            copy3Record.setValue(Copy3Columns.MET_CALC_COBERT_RIESGO_NORMAL, "2");
                            copy3Record.setValue(Copy3Columns.MET_CALC_COBERT_RIESGO_DUDOSO, "0");
                        }
                    }
                }
            }



            if (isRepoRPPLZ){
                copy3Record.setValue(Copy3Columns.JERARQUIA_VALOR_RAZONABLE, 0);
            } else {
                copy3Record.setValue(Copy3Columns.JERARQUIA_VALOR_RAZONABLE, _mapper.getJerarquiaValorRazonable(jerarquia));
            }

            copy3Record.setValue(Copy3Columns.OPERACION_NO_DECLARABLE_CIRBE, isCTA ? "1" : "0");
            copy3Record.setValue(   Copy3Columns.TIPO_CODIGO_VALOR, AnacreditConstants.EMPTY_STRING);
            if (!isRepoRPPLZ) {
                copy3Record.setValue(   Copy3Columns.TIPO_CODIGO_VALOR, "01");
            }

            copy3Record.setValue(Copy3Columns.SALDO_CONTINGENTE, 0);
            copy3Record.setValue(Copy3Columns.PRODUCTO_SUBPRODUCTO, copy3Record.getValue(Copy3Columns.PRODUCTO_ENTIDAD));


            String carteraPrudencial = AnacreditMapper.getCarteraPrudencial(String.valueOf(copy3Record.getValue(Copy3Columns.TIPO_CARTERA_IFRS9)));
            copy3Record.setValue(Copy3Columns.CARTERA_PRUDENCIAL, carteraPrudencial);

            if (isATA)  {
                copy3Record.setValue(   Copy3Columns.FECHA_PRIMERA_LIQUIDACION,  copy3Record.getValue(Copy3Columns.FECHA_EMISION_ORIGINACION));
                copy3Record.setValue(   Copy3Columns.FINANCIACION_PROYECTO,  2);
            }
            copy3Record.setValue(Copy3Columns.FECHA_ESTADO_CUMPLIMIENTO, copy3Record.getValue(Copy3Columns.FECHA_EMISION_ORIGINACION));

            copy3Record.setValue(Copy3Columns.FECHA_REFIN_REEST, AnacreditConstants.STR_MAX_DATE_99991231);

            if (isCTA) {
                copy3Record.setValue(Copy3Columns.FRECUENCIA_PAGO_PRINC_INT, 0);
            } else  {
                copy3Record.setValue(Copy3Columns.FRECUENCIA_PAGO_PRINC_INT, 4);
            }

            copy3Record.setValue(   Copy3Columns.FECHA_FINAL_CARENCIA_PRINCIPAL, AnacreditConstants.STR_MIN_DATE_11111112);

            String  derechos_reenbolso = _mapper.getDerechosReenbolso(String.valueOf(copy3Record.getValue(Copy3Columns.PRODUCTO_AC)));
            copy3Record.setValue(Copy3Columns.DERECHOS_REEMBOLSO, derechos_reenbolso);

            if (isATA)  {
                String tipoFuenteCarga = _mapper.getTipoFuenteCarga(underlying, productIssuer);
                copy3Record.setValue(Copy3Columns.TIPO_FUENTE_DE_CARGA, tipoFuenteCarga);
                copy3Record.setValue(Copy3Columns.SITUACION_IMPAGO_OPERACION, "14");
                copy3Record.setValue(Copy3Columns.FECHA_SITUACION_IMPAGO_OPE, copy3Record.getValue(Copy3Columns.FECHA_EMISION_ORIGINACION));
            }


            if (isRepoRPPLZ) {
                copy3Record.setValue(   Copy3Columns.SALDO_DEUDOR_DUDOSO_EXCEDIDO_NO_VENC, 0);
                copy3Record.setValue(   Copy3Columns.VALOR_RESIDUAL_NO_GARAN_DUDOS, 0);
                copy3Record.setValue(   Copy3Columns.OTRAS_PERIODIFICACIONES_ACTIV, 0);

            }

            copy3Record.setValue(Copy3Columns.FECHA_BAJA_DEF_CIRBE, AnacreditConstants.STR_MAX_DATE_99991231);

            if (isATA) {
                copy3Record.setValue(   Copy3Columns.ESTADO_CUMPLIMIENTO, 1);
            }

            copy3Record.setValue(Copy3Columns.OPERACION_TITULARES_EXONERADOS, "0");

            if (AnacreditConstants.STR_M16.equals(copy3Record.getValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC))
                    || AnacreditConstants.STR_M9.equals(copy3Record.getValue(Copy3Columns.MODALIDAD_TIPO_INTERES_AC)))  {
                copy3Record.setValue(Copy3Columns.SIN_DEVENGO_INTERES, "1");
            } else {
                copy3Record.setValue(Copy3Columns.SIN_DEVENGO_INTERES, "0");
            }

            //v2.19 - v2.20
            copy3Record.setValue(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO,  0.0);
            copy3Record.setValue(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA,  0.0);
            if (!"EUR".equals(repo.getCurrency())) {
                copy3Record.keep(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA.name()+"_EUR",  0.0);
                copy3Record.keep(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO.name()+"_EUR",  0.0);
            }else {
                copy3Record.keep(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA.name(),  0.0);
                copy3Record.keep(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO.name(),  0.0);
            }

            return copy3Record;
        } catch (Exception e) {
            Log.error(this, "Error creating COPY3 Record. Will be ignored!", e);
        }
        return null;
    }

    protected String  checkLegalEntityAttrJMIN(Vector errorMsgs, Long identifier, LegalEntity legalEntity) {
        if (null == legalEntity) {
            log(LogLevel.ERR, identifier, " - LegalEntity is null.", errorMsgs );
        }

        String jMinAttr = _mapper.getJMin(legalEntity);
        if (Util.isEmpty(jMinAttr)) {
            log(LogLevel.ERR, identifier, " - J MIN not found for legalEntity " + legalEntity.getCode(), errorMsgs );
        }
        return jMinAttr;
    }

    public static  boolean isRepoRPPLZ(Trade trade, JDatetime valDatetime) {
        if (trade.getProduct() instanceof  Repo) {
            if (trade.getTradeDate().lte(valDatetime)
                    && ((Repo)trade.getProduct()).getStartDate().after(valDatetime.getJDate(TimeZone.getDefault()))) {
                return true;
            }
        }
        return false;
    }

    public static  boolean isRepoCashFixed(Repo repo) {
        if (repo != null) {
            return "Fixed".equalsIgnoreCase(repo.getCash().getRateType());
        }
        return false;
    }



    public List<Copy4ARecord> formatCopy4APersona(ReportRow reportRow, Vector errorMsgs) {

        try {
            ArrayList<Copy4ARecord> personas = new ArrayList<>();

            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            Trade trade = reportRow.getProperty(ReportRow.TRADE);

            RepoTypeIdentifier repoTypeIdentifier = new RepoTypeIdentifier(trade, valDatetime).invoke();
            boolean isRepoRPPLZ = repoTypeIdentifier.isRepoRPPLZ();
            boolean isCTA = repoTypeIdentifier.isCTA();

            Copy3Record copy3Record = reportRow.getProperty(AnacreditConstants.COPY_3);


            Repo repo = (Repo) trade.getProduct();
            Product underlying = repo.getSecurity();

            JDate fechaVencimiento = (JDate) copy3Record.retrieve(Copy3Columns.FECHA_VENCIMIENTO.name());
            if (fechaVencimiento == null)  {
                log(LogLevel.ERR, trade.getLongId(), " - Error getting FECHA_VENCIMIENTO from Copy3", errorMsgs );
                return personas;
            }

            //Fecha Baja Relacion - Primer dias Natual a la fecha vencimiento
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaVencimiento.getDate());
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
            JDate fechaBajaRelacion = JDate.valueOf(cal.getTime());

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
            copy4aCpty.setValue(Copy4AColumns.FECHA_BAJA_RELACION, fechaBajaRelacion);

            if (isRepoRPPLZ) {
                copy4aCpty.setValue(Copy4AColumns.NATURALEZA_INTERVENCION, "10");
            } else if (isCTA) {
                copy4aCpty.setValue(Copy4AColumns.NATURALEZA_INTERVENCION, "01");
            } else {
                copy4aCpty.setValue(Copy4AColumns.NATURALEZA_INTERVENCION, "10");
            }
            copy4aCpty.setValue(Copy4AColumns.ID_PERSONA, jminCpty);

            // Add persona CPTY to the list
            personas.add(copy4aCpty);

            if (!isRepoRPPLZ) {
                // Issuer - Emissor
                Copy4ARecord copy4aIssuer = new Copy4ARecord();
                copy4aIssuer.initializeFromCopy3Record(valDate, copy3Record);
                copy4aIssuer.setValue(Copy4AColumns.NATURALEZA_INTERVENCION, isCTA ? "56" : "54");
                copy4aIssuer.setValue(Copy4AColumns.ID_PERSONA, jminIssuer);
                copy4aIssuer.setValue(Copy4AColumns.FECHA_BAJA_RELACION, fechaBajaRelacion);
                personas.add(copy4aIssuer);
            }

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
            Trade trade = reportRow.getProperty(ReportRow.TRADE);



            if (copy3Record.retrieve(Copy3Columns.VALOR_NOMINAL.name()) != null) {
                Copy4Record copy4Record = buildCopy4Record(Copy3Columns.VALOR_NOMINAL, "40", trade.getSettleCurrency(), copy3Record);
                if (copy4Record != null) {
                    importes.add(copy4Record);
                }
            }


            if (copy3Record.retrieve(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO.name()) != null) {
                Copy4Record copy4Record = buildCopy4Record(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO, "02", trade.getSettleCurrency(), copy3Record);
                if (copy4Record != null) {
                    importes.add(copy4Record);
                }
            }

            if (copy3Record.retrieve(Copy3Columns.SALDO_ACREEDOR.name()) != null) {
                Copy4Record copy4Record = buildCopy4Record(Copy3Columns.SALDO_ACREEDOR, "19", trade.getSettleCurrency(), copy3Record);
                if (copy4Record != null) {
                    importes.add(copy4Record);
                }
            }


            if (copy3Record.retrieve(Copy3Columns.INTERESES_DEVENGADOS.name()) != null) { // OK arrasta desde la copy3
                Copy4Record copy4Record = buildCopy4Record(Copy3Columns.INTERESES_DEVENGADOS, "15", trade.getSettleCurrency(), copy3Record);
                if (copy4Record != null) {
                    importes.add(copy4Record);
                }
            }

            if (copy3Record.retrieve(Copy3Columns.ACTIVOS_VALOR_RAZOBL.name()) != null) { // OK Arrasta desdela copy3
                Copy4Record copy4Record = buildCopy4Record(Copy3Columns.ACTIVOS_VALOR_RAZOBL, "17", trade.getSettleCurrency(), copy3Record);
                if (copy4Record != null) {
                    importes.add(copy4Record);
                }
            }

            if (copy3Record.retrieve(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA.name()) != null) { // OK Arrasta desdela copy3
                Copy4Record copy4Record = buildCopy4Record(Copy3Columns.REDUCCION_PRINCIPAL_IMPORTE_AVALISTA, "687", trade.getSettleCurrency(), copy3Record);
                if (copy4Record != null) {
                    importes.add(copy4Record);
                }
            }

            if (copy3Record.retrieve(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO.name()) != null) { // OK Arrasta desdela copy3
                Copy4Record copy4Record = buildCopy4Record(Copy3Columns.REDUCCION_PRINCIPAL_AVALES_EJECUTADO, "688", trade.getSettleCurrency(), copy3Record);
                if (copy4Record != null) {
                    importes.add(copy4Record);
                }
            }


            return importes;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy4 Records. Will be Ignored", e);
        }
        return null;

    }

    public List<Copy11Record> formatCopy11Garantias(CacheModuloD cachedData, ReportRow reportRow, Vector errorMsgs) {

        try {
            Trade trade = reportRow.getProperty(ReportRow.TRADE);
            Repo repo = (Repo) trade.getProduct();

            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());

            RepoTypeIdentifier repoTypeIdentifier = new RepoTypeIdentifier(trade, valDatetime).invoke();
            boolean isATA = repoTypeIdentifier.isATA();

            if (!isATA) {
                return null;
            }


            Product underlying = repo.getSecurity();
            JDate bondEndDate = null;
            if (repo.getSecurity() instanceof  Bond ) {
                Bond bond = (Bond) repo.getSecurity();
                bondEndDate = bond.getEndDate();
            }

            ArrayList<Copy11Record> garantias = new ArrayList<>();

            Copy3Record copy3Record = reportRow.getProperty(AnacreditConstants.COPY_3);

            // Contrapartida
            Copy11Record copy11Record = new Copy11Record();

            copy11Record.setValue(Copy11Columns.FECHA_ALTA_RELACION, copy3Record.getValue(Copy3Columns.FECHA_EMISION_ORIGINACION));
            copy11Record.setValue(Copy11Columns.FECHA_DATOS, valDate);


            copy11Record.setValue(Copy11Columns.FECHA_BAJA_RELACION, AnacreditConstants.STR_MAX_DATE_99991231);
            if (repo.getSecurity() instanceof  Bond) {
                Bond bond = (Bond) repo.getSecurity();
                if (null != bond.getEndDate()) {
                    copy11Record.setValue(Copy11Columns.FECHA_BAJA_RELACION, bond.getEndDate());
                }

            }

            copy11Record.setValue(Copy11Columns.CODIGO_OPERACION, copy3Record.getValue(Copy3Columns.ID_CONTRATO));
            copy11Record.setValue(Copy11Columns.TIPO_GARANTIA_REAL, "T10");
            copy11Record.setValue(Copy11Columns.ALCANCE_GARANTIA_REAL, "A01");

            String codigoGarantia = CacheModuloD.buildKey(trade, repo);
            if (Util.isEmpty(codigoGarantia)) {
                return null;
            }
            copy11Record.setValue(Copy11Columns.CODIGO_GARANTIA_REAL, codigoGarantia);

            copy11Record.setValue(Copy11Columns.CODIGO_ACTIVO_REC_GARANTIA, copy11Record.getValue(Copy11Columns.CODIGO_GARANTIA_REAL));
            copy11Record.setValue(Copy11Columns.IMPORTE_HIPOTECARIA_PRIN, 0);
            copy11Record.setValue(Copy11Columns.IMPORTE_HIPOTECARIA_INT, 0);
            copy11Record.setValue(Copy11Columns.ACTIVOS_GARANTIA_INM, "A2");

            String tipoRecGarantia = _mapper.getTipoActivoRecGarantia(underlying);
            copy11Record.setValue(Copy11Columns.TIPO_ACTIVO_REC_GARANTIA, tipoRecGarantia);


            Double valorGarantia = 0.0;

            if ("EUR".equals(trade.getTradeCurrency()))  {
                valorGarantia = (Double) copy3Record.retrieve(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO.name());
            } else {
                valorGarantia = (Double) copy3Record.retrieve(Copy3Columns.SALDO_DEUDOR_NO_VENCIDO.name()+"_EUR");
            }

            copy11Record.setValue(Copy11Columns.IMPORTE_GARANTIA, valorGarantia);
            copy11Record.setValue(Copy11Columns.IMPORTE_GARANTIA_CREDITICIA, valorGarantia);


            JDate fechaFormalizacionFarantia = cachedData.getFechaGarantia(codigoGarantia);
            if (fechaFormalizacionFarantia != null) {
                copy11Record.setValue(Copy11Columns.FECHA_FORMALIZACION_GARANTIA, fechaFormalizacionFarantia);
            } else {
                copy11Record.setValue(Copy11Columns.FECHA_FORMALIZACION_GARANTIA, AnacreditConstants.STR_MAX_DATE_99991231);
            }

            copy11Record.setValue(Copy11Columns.DERECHO_COBRO_GARANTIA, 0);



            if (bondEndDate == null ) {
                copy11Record.setValue(Copy11Columns.VENCIMIENTO_COBERTURA, AnacreditConstants.STR_MAX_DATE_99991231);
            } else {
                copy11Record.setValue(Copy11Columns.VENCIMIENTO_COBERTURA, bondEndDate);
            }

            copy11Record.setValue(Copy11Columns.GARANTIA_PRINCIPAL_PRIORITARIA,"1");

            garantias.add(copy11Record);

            return garantias;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy11 Records. Will be Ignored", e);
        }
        return null;

    }


        public List<Copy13Record> formatCopy13ActivosFinanceros(CacheModuloD cache, ReportRow reportRow, Vector errorMsgs) {
        try {
            Trade trade = reportRow.getProperty(ReportRow.TRADE);
            PricingEnv pEnv = reportRow.getProperty(ReportRow.PRICING_ENV);
            JDatetime valDatetime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            JDate valDate = valDatetime.getJDate(TimeZone.getDefault());

            Repo repo = (Repo) trade.getProduct();
            repo.calculate(trade, pEnv, valDate);

            RepoTypeIdentifier repoTypeIdentifier = new RepoTypeIdentifier(trade, valDatetime).invoke();
            boolean isATA = repoTypeIdentifier.isATA();

            if (!isATA) {
                return null;
            }

            ArrayList<Copy13Record> activos = new ArrayList<>();
            Product underlying = repo.getSecurity();

            Copy3Record copy3Record = reportRow.getProperty(AnacreditConstants.COPY_3);

            // Contrapartida
            Copy13Record copy13Record = new Copy13Record();

            copy13Record.setValue(Copy13Columns.FECHA_DATOS, valDate);

            String codigoGarantia = CacheModuloD.buildKey(trade, repo);
            if (cache.contains(codigoGarantia)){
                return null;
            }

            copy13Record.setValue(Copy13Columns.CODIGO_ACTIVO_REC_GARANTIA, codigoGarantia);
            copy13Record.setValue(Copy13Columns.REFERENCIA_INTERNA, codigoGarantia);

            String sTpGarantia = _mapper.getTipoActivoRecGarantia(underlying);
            copy13Record.setValue(Copy13Columns.TIPO_ACTIVO_REC_GARANTIA, sTpGarantia);

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
            copy13Record.setValue(Copy13Columns.CODIGO_EMISOR, jminIssuer);
            copy13Record.setValue(Copy13Columns.CODIGO_VALOR, underlying.getSecCode("ISIN"));

            String jerarquia = underlying.getSecCode(AnacreditConstants.ATTR_ANACREDIT_JERARQUIA);
            String cotizaD = _mapper.getCotizaModuloD(jerarquia);
            copy13Record.setValue(Copy13Columns.COTIZA, cotizaD);

            Double valorNominalTotal = cache.getTotalNominal(codigoGarantia);

            copy13Record.setValue(Copy13Columns.VALOR_NOMINAL_GARANTIA, valorNominalTotal);
            copy13Record.setValue(Copy13Columns.VALOR_ORIGINAL_GARANTIA, valorNominalTotal);


            JDate fechaVenc = (JDate) copy3Record.retrieve(Copy3Columns.FECHA_VENCIMIENTO.name());
            if (fechaVenc != null) {
                copy13Record.setValue(Copy13Columns.FECHA_VALOR_ORIGINAL_GARANTIA, fechaVenc);
            }

            Double totalMV = cache.getTotalMarketValue(codigoGarantia);
            if (totalMV == null || totalMV.isNaN() || totalMV.compareTo(0.0d) == 0) {
                return null;
            }

            copy13Record.setValue(Copy13Columns.IMPORTE_GARANTIA, totalMV);
            copy13Record.setValue(Copy13Columns.IMPORTE_GARANTIA, totalMV);
            copy13Record.setValue(Copy13Columns.TIPO_IMPORTE_GARANTIA, 1);
            copy13Record.setValue(Copy13Columns.MET_VALORACION_GARANTIA, 3);
            copy13Record.setValue(Copy13Columns.FECHA_VALORACION_GARANTIA, valDate);
            Double totalQtd = cache.getTotalQtd(codigoGarantia);

            // Convert to EUR to report on file
            if (totalQtd != null) {
                copy13Record.setValue(Copy13Columns.NUM_TITULOS_PARTICIPACIONES, totalQtd.intValue());
            }

            copy13Record.setValue(Copy13Columns.FECHA_BAJA_GARANTIA, AnacreditConstants.STR_MAX_DATE_99991231);
            if (repo.getSecurity() instanceof  Bond) {
                Bond bond = (Bond) repo.getSecurity();
                if (null != bond.getEndDate()) {
                    copy13Record.setValue(Copy13Columns.FECHA_BAJA_GARANTIA, bond.getEndDate());
                }

            }

            copy13Record.setValue(Copy13Columns.GARANTIA_NO_DECLARABLE_CIRBE, 0);
            copy13Record.setValue(Copy13Columns.ID_PROVEEDOR_GARANTIA, copy3Record.getValue(Copy3Columns.ID_PERSONA_CONTRAPARTE_DIRECT));
            copy13Record.setValue(Copy13Columns.VERSION, "V0219");

            activos.add(copy13Record);
            cache.addReported(codigoGarantia);

            return activos;

        } catch (Exception e) {
            Log.error(this, "Error generating Copy13 Records. Will be Ignored", e);
        }
        return null;
    }

    private Double changeDirection(double value){
        return value*-1;
    }

}
