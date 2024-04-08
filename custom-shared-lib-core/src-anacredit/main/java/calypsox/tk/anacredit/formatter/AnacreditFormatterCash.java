package calypsox.tk.anacredit.formatter;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditPersonaOperacionesItem;
import calypsox.tk.anacredit.util.AnacreditUtilities;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.InterestBearingEntry;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.*;
import com.calypso.tk.report.ReportRow;

import java.util.*;
import java.util.stream.Collectors;

import static calypsox.tk.anacredit.loader.AnacreditLoaderUtil.INVENTORY_CASH_POS;
import static calypsox.tk.anacredit.loader.AnacreditLoaderUtil.calculateInterestDaily;

public class AnacreditFormatterCash extends AnacreditFormatter implements IPersonaFormatter {


   /**
     * Items wuth only interest paid in the period. NO Position for this movements
     * @param valDate
     * @param errors
     * @return
     */
    public AnacreditOperacionesItem formatInterestPaymentItem(CollateralConfig config, Trade trade, JDate valDate, PricingEnv pricingEnv, Vector<String> errors) {

        String currency = trade.getTradeCurrency();
        Book book = trade!=null ? trade.getBook() : null;
        AnacreditOperacionesItem item =  buildCommonContractRecord(config, valDate, currency, errors,book);
        if (item == null) {
            return null;
        }

        // Sumar Daily accrual del interest Bearing en negativo
        double interestDailyDivisa =  0.0;
        double interestDailyEUR =  0.0;

        if(trade!=null) {
            InterestBearing interestBearing = (InterestBearing) trade.getProduct();
            interestDailyDivisa = getDaily(interestBearing, valDate);
            double d = calculateInterestDaily(interestBearing, valDate);

            interestDailyEUR = AnacreditUtilities.convertToEUR(interestDailyDivisa, interestBearing.getCurrency(), valDate, pricingEnv);
        } else   {
            log(LogLevel.WARN, config,  "InterestBearing trade not found for this contract.", errors);
        }

        if (Double.compare(0.0, interestDailyEUR ) == 0) {
            //log(LogLevel.WARN, config,  "Interest is zeros. "+ currency, errors);
            return null;

        }

        item.setIntereses_devengados(interestDailyEUR);
        item.setIntereses_devengos(interestDailyDivisa);

        // fill zeros as there is no position to this contract
        item.setValor_nominal_d(0d);
        item.setSaldo_deudor_no_vencido(0d);
        item.setValor_nominal("+"+formatUnsignedNumber(0d, 15,0,""));

        //V2.19/2.20
        item.setReduccion_principal_importe_avalista("+"+formatUnsignedNumber(0,15,2,""));
        item.setReduccion_principal_avales_ejecutado("+"+formatUnsignedNumber(0,15,2,""));
        item.setReduc_principal_importe_avalista(0.0);
        item.setReduc_principal_avales_ejecutado(0.0);

        return item;

    }

    public AnacreditOperacionesItem formatContractPositionItem(CollateralConfig config, CashPositionDTO marginCallPosition , ReportRow rowInfo, JDate valDate, PricingEnv pricingEnv,Vector<String> errors) {
        String currency = marginCallPosition.getCurrency();
        String date =  formatDate(valDate,8);
        String datePlusOne = formatDate(valDate.addBusinessDays(1, Util.string2Vector(HOLIDAYS)),8);

        // reject position positive for this extraction
        if(null!=marginCallPosition && marginCallPosition.getAllInValue()>0){
            log(LogLevel.WARN, config,  "Position is Positive.", errors);
            return null;
        }
        Book f_book = (Book) rowInfo.getProperty("F_BOOK");
        AnacreditOperacionesItem item =  buildCommonContractRecord(config, valDate, currency, errors,f_book);
        if (item == null) {
            return null;
        }

        InventoryCashPosition inventoryCashPosition = rowInfo.getProperty(INVENTORY_CASH_POS);
        if (null == inventoryCashPosition) {
            log(LogLevel.ERR, config,"InventoryCash not found for this contract.", errors );
        }

        item.setSaldo_deudor_no_vencido(inventoryCashPosition ,numChar,valDate,pricingEnv);
        item.setSaldo_deudor_no_ven(inventoryCashPosition);
        item.setValor_nominal(item.getSaldo_deudor_no_vencido());
        item.setValor_nominal_d(item.getSaldo_deudor_no_ven());

        Trade tradeInterestBearing = (Trade) rowInfo.getProperties().get("INTEREST_BEARING");
        // Sumar Daily accrual del interest Bearing en negativo
        double interestDailyDivisa =  0.0;
        double interestDailyEUR =  0.0;

        if(tradeInterestBearing!=null) {
            InterestBearing interestBearing = (InterestBearing) tradeInterestBearing.getProduct();
            interestDailyDivisa = getDaily(interestBearing, valDate);
            interestDailyEUR = AnacreditUtilities.convertToEUR(interestDailyDivisa, interestBearing.getCurrency(), valDate, pricingEnv);
        } else   {
            log(LogLevel.WARN, config,  "InterestBearing trade not found for this contract.", errors);
        }

        if ((null == marginCallPosition || marginCallPosition.getAllInValue() == 0)
                && Double.compare(0.0, interestDailyEUR ) == 0) {
            log(LogLevel.ERR, config,  "Position and Interest are zeros for this contract. "+ currency, errors);
            return null;
        }
        item.setIntereses_devengados(interestDailyEUR);
        item.setIntereses_devengos(interestDailyDivisa);

        //V2.19/2.20
        item.setReduccion_principal_importe_avalista("+"+formatUnsignedNumber(0,15,2,""));
        item.setReduccion_principal_avales_ejecutado("+"+formatUnsignedNumber(0,15,2,""));
        item.setReduc_principal_importe_avalista(0.0);
        item.setReduc_principal_avales_ejecutado(0.0);

        return item;

    }


    private AnacreditOperacionesItem buildCommonContractRecord(CollateralConfig config, JDate valDate, String currency, Vector<String> errors,Book book) {
        AnacreditOperacionesItem item = new AnacreditOperacionesItem();
        String date =  formatDate(valDate,8);
        String datePlusOne = formatDate(valDate.addBusinessDays(1, Util.string2Vector(HOLIDAYS)),8);
        Account account = _mapper.loadAccount(String.valueOf(config.getId()), currency);
        if(account == null){
            log(LogLevel.ERR, config,  "Line error, contract: "+ config.getId() + " - Account not found.", errors);
            return null;
        }

        String j_minorista = _mapper.getJMin(config.getLegalEntity());
        if(Util.isEmpty(j_minorista)){
            log(LogLevel.ERR, config,  " J MIN not found for LegalEntity: " + config.getLegalEntity().getCode(), errors);
            return null;
        }

        AccountInterestConfig accInterest = _mapper.getAccInterest(account);
        setCommonAccountValues(accInterest, item, valDate);
        setContractAccountValues(accInterest, item);
        //campos comunes
        initDefaultOperacionesCopy3(item);
        removeFiller(item);
        //Campos únicos
        initDefaultOperContractItem(item);
        //ID de linea (id and id interno)
        String tipoCartera = _mapper.getTipoCartera(book);
        item.setId_contrato(config, account, date,tipoCartera);
        item.setMoneda(formatStringWithBlankOnRight(_mapper.getCurrencyMap(currency, errors, item.getId_contrato()),3));

        item.setTipo_cartera_ifrs9(_mapper.getTipoCartera(config.getBook()));
        if(_mapper.validAgent(config.getLegalEntity())){
            item.setTipo_cartera_ifrs9("99");
            item.setReconocimiento_balance("3");
        }

        item.setId_persona_contraparte_direct(j_minorista);
        item.setFecha_primera_liquidacion(date);
        item.setFecha_estado_cumplimiento(date);
        item.setTipo_referencia_sustitutivo(accInterest);
        item.setProxima_revision_tipo_interes(datePlusOne);
        item.setCartera_prudencial();

        item.setFecha_ultimo_vto_intereses(date); //Trade fijo
        item.setFecha_proximo_vto_intereses(datePlusOne); //Trade fijo
        item.setFecha_proximo_vto_principal(datePlusOne); //Trade fijo

        //Fechas
        initDefaultDates(item,date,datePlusOne);
        return item;

    }

    /**
     * @param interestBearing
     * @param valDate
     * @return
     */
    public Double getDaily(InterestBearing interestBearing,JDate valDate) {

        Double sumInterst = 0.0;
        if(null!=interestBearing && null!=valDate){
            JDate system = valDate.addBusinessDays(1, Util.string2Vector("SYSTEM"));
            if(valDate.getMonth() != system.getMonth()){
                int monthLength = valDate.getMonthLength();
                valDate = valDate.addDays(monthLength-valDate.getDayOfMonth());
            }

            Vector<InterestBearingEntry> entries = (Vector<InterestBearingEntry>) interestBearing.getEntries();
            List<InterestBearingEntry> collect = entries.stream().filter(interest -> InterestBearingEntry.POSITION.equalsIgnoreCase(interest.getEntryType())).collect(Collectors.toList());

            if(!Util.isEmpty(entries)){
                for(InterestBearingEntry entry : collect){
                    JDate entryDate = entry.getEntryDate();
                    if(entryDate.lte(valDate) && InterestBearingEntry.POSITION.equalsIgnoreCase(entry.getEntryType()) &&
                            entry.getAmount()<0){
                        InterestBearingEntry interes=interestBearing.getEntry("INTEREST",entryDate );
                        if (interes!=null)
                            sumInterst = sumInterst + interes.getAmount();

                        InterestBearingEntry ajuste=interestBearing.getEntry("ADJUSTMENT",entryDate );
                        if (ajuste!=null)
                            sumInterst += ajuste.getAmount();
                    }
                }
            }
        }

        return sumInterst;
    }

    public void setCommonAccountValues(AccountInterestConfig accInterest, AnacreditOperacionesItem item,JDate valDate){
        String interestType = "";
        String indice_referencia_ac = "";
        String diferencial_sobre_indice_refe = "+"+formatUnsignedNumber(0,7,4,"");
        Double tedr = 0.0;
        Double rent = 0.0;

        if(null!=accInterest){
            String accountDir = _mapper.getAccountDir(accInterest);
            if(FLOAT.equalsIgnoreCase(accountDir)){
                interestType = M14;
                indice_referencia_ac = I_29;
                AccountInterestConfigRange range = _mapper.getRange(accInterest);
                if (range != null) {
                    RateIndexDefaults rateIndexDefault = _mapper.getRateIndexDefault(range);
                    if (rateIndexDefault != null) {
                        indice_referencia_ac = _mapper.getRateCode(rateIndexDefault);
                        Double spread = _mapper.getSpread(accInterest);
                        diferencial_sobre_indice_refe = "+"+formatUnsignedNumber(spread,7,4,"");
                        Double rateIndexQuote = _mapper.getRateIndexQuote(range,valDate);
                        tedr = rateIndexQuote + spread;
                    }
                }
            }else if(FIXED.equalsIgnoreCase(accountDir)){
                interestType = M12;
                indice_referencia_ac = I_99;
                diferencial_sobre_indice_refe = "+"+formatUnsignedNumber(909.09,7,4,"");
                tedr = accInterest.getThreshold();
            }

            Vector ranges = accInterest.getRanges();
            if(!Util.isEmpty(ranges) && ranges.get(0) instanceof AccountInterestConfigRange){
                AccountInterestConfigRange ra = (AccountInterestConfigRange) ranges.get(0);
                if(ra.isFloor()){
                    rent = ra.getFloor();
                }
            }
        }

        item.setSuelo_rentabilidad(formatUnsignedNumber(rent,8,5,""));
        item.setModalidad_tipo_interes_ac(formatStringWithBlankOnRight(interestType,3));
        item.setTedr(tedr);
        item.setIndice_referencia_ac(formatStringWithBlankOnRight(indice_referencia_ac,3));
        item.setDiferencial_sobre_indice_refe(formatStringWithBlankOnRight(diferencial_sobre_indice_refe,8));
    }

    public void setContractAccountValues(AccountInterestConfig accInterest,AnacreditOperacionesItem item){
        String refvencimiento = "";
        String frecrev = "";
        if(null!=accInterest){
            String accountDir = _mapper.getAccountDir(accInterest);
            if(FLOAT.equalsIgnoreCase(accountDir)){
                refvencimiento = "1";
                frecrev = "16";

            } else if(FIXED.equalsIgnoreCase(accountDir)){
                refvencimiento  = "0";
                frecrev  = "0";
            }
        }
        if(checkIndice(item.getIndice_referencia_ac())){
            refvencimiento = "0";
        }
        item.setTipo_referencia_vencimiento(formatStringWithBlankOnRight(refvencimiento,3));
        item.setFrec_revision_tipo_int_per(formatStringWithBlankOnRight(frecrev,2));
    }


    /**
     CONTRATO FIJO
     */
    private void initDefaultOperContractItem(AnacreditOperacionesItem item){
        item.setProducto_ac(AP021);
        item.setEsquema_amort_operaciones_ac("03");
        item.setProducto_entidad(formatStringWithBlankOnRight("MC",5));
        item.setProducto_subproducto(formatStringWithBlankOnRight("MC",6));
        item.setSubordinacion_producto_ac("S0");
        item.setFinalidad_ac(formatStringWithBlankOnRight("F37",4));
        item.setPrincipal_inicial("+"+formatUnsignedNumber(0,15,0,""));
        item.setLimite_inicial("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setImporte_concedido("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCanal_contratacion_ac("C02");
        item.setProvincia_negocio("28");

        item.setInmueble_financ_num_viviendas(formatUnsignedNumber(0,13,0,""));
        item.setCodigo_promoc_inmob_finan(formatStringWithBlankOnRight("0",20));
        item.setDesc_promoc_inmob_finan(formatStringWithBlankOnRight("0",50));
        item.setPlazo_residual("P2");
        item.setCodigo_ratio_referencia(formatStringWithBlankOnRight("",3));
        //item.setPlazo_origen_m("05");
        item.setPlazo_origen_m(formatStringWithBlankOnRight("",2));
        item.setEstado_cumplimiento(formatStringWithBlankOnRight("1",3));
        item.setFinanciacion_proyecto(formatStringWithBlankOnRight("2",2));
    }
    /**
     * Common Constants for Cash Operations Items (Contract and Operation)
     * @param item
     */
    @Override
    protected void initDefaultOperacionesCopy3(AnacreditOperacionesItem item){
        //High level and inmutable constants for all items
        super.initDefaultOperacionesCopy3(item);

        //Common Cash Item constants
        item.setCodigo_valor(formatStringWithBlankOnRight("",12));
        item.setOrigen_operacion(formatStringWithBlankOnRight("O10",4));
        item.setPorcent_particip_sindicados(formatUnsignedNumber(100,8,5,""));
        item.setSituacion_ope_riesgo_direc_ac(formatStringWithBlankOnRight("S10",4));
        item.setActivos_valor_razobl("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setPasivos_valor_razonable("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCotiza(formatStringWithBlankOnRight("",3));
        item.setEntidad_depositaria("24");
        item.setJerarquia_valor_razonable("0");
        item.setTipo_codigo_valor(formatStringWithBlankOnRight("",2));
        item.setFrecuencia_pago_princ_int(formatStringWithBlankOnRight("4",2));
        item.setFase_deterioro(formatStringWithBlankOnRight("23",2));
        item.setMet_calc_cobert_riesgo_normal(formatStringWithBlankOnRight("2",2));
    }


    /**
     TRADE VARIABLE
     */
    public AnacreditOperacionesItem formatTradeItem(CollateralConfig config, Trade trade, JDate valDate, PricingEnv pricingEnv, Vector<String> errors){

        AnacreditOperacionesItem item = new AnacreditOperacionesItem();
        if(null!=config){
            String date =  formatDate(valDate,8);
            String datePlusOne =  formatDate(valDate.addBusinessDays(1,Util.string2Vector(HOLIDAYS)),8);
            JDate maturityDate = null;

            Account account = _mapper.loadAccount(String.valueOf(config.getId()),trade.getTradeCurrency());
            AccountInterestConfig accInterest = _mapper.getAccInterest(account);
            setCommonAccountValues(accInterest,item,valDate);
            //Campos comunes
            initDefaultOperacionesCopy3(item);
            removeFiller(item);
            //Campos únicos
            initDefaultOperTradeItem(item);

            if(account == null){
                log(LogLevel.ERR,config, "trade: "+ trade.getLongId() + " - Account not found.", errors );
                return null;
            }
            Book book = trade.getBook();
            String tipoCartera = _mapper.getTipoCartera(book);
            //ID de linea
            item.setId_contrato(trade, account, date,tipoCartera); //set id and id interno

            //Campos variables
            item.setMoneda(formatStringWithBlankOnRight(_mapper.getCurrencyMap(trade.getTradeCurrency(), errors, item.getId_contrato()),3));

            item.setSaldo_deudor_no_vencido(trade,numChar,valDate,pricingEnv); //comprobar
            item.setValor_nominal("+"+formatUnsignedNumber(0, 15,0,""));
            item.setSaldo_deudor_no_ven(trade);

            item.setFecha_estado_cumplimiento(formatDate(trade.getTradeDate().getJDate(TimeZone.getDefault()),8));

            String plazo = "";
            String plazom = "";
            Product product = trade.getProduct();
            Long diff = 0L;
            if(product instanceof MarginCall){

                maturityDate = ((MarginCall) product).getMaturityDate(trade);
                long difference = valDate.getDate().getTime() - maturityDate.getDate().getTime();
                diff = new Long(Math.abs(difference / (1000*60*60*24)));
                plazo = _mapper.getPlazoResidual(diff.intValue());
                plazom = _mapper.getPlazoOrigenM(diff.intValue());

            }
            setTradeAccountValues(accInterest,item,diff.intValue());
            item.setFecha_vencimiento(formatDate(maturityDate,8));
            item.setPlazo_residual(plazo);

            item.setTipo_cartera_ifrs9(_mapper.getTipoCartera(config.getBook()));

            item.setTipo_referencia_sustitutivo(accInterest);

            String j_minorista = _mapper.getJMin(trade.getCounterParty());
            if(Util.isEmpty(j_minorista)){
                log(LogLevel.ERR,config, "trade: "+ trade.getLongId() + " - J MIN not found for LegalEntity: " + trade.getCounterParty().getCode(), errors );
                return null;
            }else if(_mapper.validAgent(trade.getCounterParty())){
                item.setTipo_cartera_ifrs9("99");
                item.setReconocimiento_balance("3");
            }
            item.setId_persona_contraparte_direct(j_minorista);
            item.setCartera_prudencial();
            item.setFecha_baja_def_cirbe(AnacreditConstants.STR_MAX_DATE_99991231);
            String tradeDate = formatDate(trade.getTradeDate().getJDate(TimeZone.getDefault()), 8);
            item.setFecha_emision_originacion(tradeDate);
            item.setFecha_cancelacion(datePlusOne);

            item.setProxima_revision_tipo_interes("11111112"); //TODO validation

            //V2.19/2.20
            item.setReduccion_principal_importe_avalista("+"+formatUnsignedNumber(0,15,2,""));
            item.setReduccion_principal_avales_ejecutado("+"+formatUnsignedNumber(0,15,2,""));
            item.setReduc_principal_importe_avalista(0.0);
            item.setReduc_principal_avales_ejecutado(0.0);

            //Fechas
            initDefaultDates(item,date,datePlusOne);

        }
        return item;
    }

    /**
     TRADE FIJO
     */
    private void initDefaultOperTradeItem(AnacreditOperacionesItem item){
        item.setProducto_ac(CP010);
        item.setEsquema_amort_operaciones_ac(formatStringWithBlankOnRight("  ",2));
        item.setProducto_entidad("MCPLZ");
        item.setProducto_subproducto(formatStringWithBlankOnRight("MCPLZ",6));
        item.setSubordinacion_producto_ac("S4");
        item.setFinalidad_ac(formatStringWithBlankOnRight("F59",4));
        item.setPrincipal_inicial("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setLimite_inicial("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setImporte_concedido("+"+formatUnsignedNumber(0,numChar,0,""));
        item.setCanal_contratacion_ac(formatStringWithBlankOnRight("",3));
        item.setProvincia_negocio("90");

        item.setInmueble_financ_num_viviendas(formatUnsignedNumber(0,13,0,""));
        item.setCodigo_promoc_inmob_finan(formatStringWithBlankOnRight("",20));
        item.setDesc_promoc_inmob_finan(formatStringWithBlankOnRight("",50));
        item.setFecha_ultimo_vto_intereses("99991231");
        item.setFecha_proximo_vto_intereses("99991231");
        item.setFecha_proximo_vto_principal("99991231");
        item.setFecha_primera_liquidacion("11111112");
        item.setIntereses_devengados(0.0);
        item.setCodigo_ratio_referencia(formatStringWithBlankOnRight("",3));
        item.setEstado_cumplimiento(formatStringWithBlankOnRight("9",3));
        item.setFinanciacion_proyecto(formatStringWithBlankOnRight("0",2));
    }

    private void setTradeAccountValues(AccountInterestConfig accInterest,AnacreditOperacionesItem item,int diff) {
        String refvencimiento = "";
        String frecrev = "";
        if(null!=accInterest){

            String accountDir = _mapper.getAccountDir(accInterest);
            if(FLOAT.equalsIgnoreCase(accountDir)){
                refvencimiento = "1";
                AccountInterestConfigRange range = _mapper.getRange(accInterest);
                RateIndexDefaults rateIndexDefault = _mapper.getRateIndexDefault(range);
                String rateFrec = _mapper.getRateFrec(rateIndexDefault);
                if(Util.isEmpty(rateFrec)){
                    frecrev = "12";
                }else{
                    frecrev = rateFrec;
                }
            }else if(FIXED.equalsIgnoreCase(accountDir)){
                if(diff>365){
                    refvencimiento  = "365";
                }else{
                    refvencimiento = String.valueOf(diff);
                }
                frecrev  = "0";
            }
        }

        if(checkIndice(item.getIndice_referencia_ac())){
            refvencimiento = "0";
        }
        //Diferente para line de trade
        item.setTipo_referencia_vencimiento(formatStringWithBlankOnRight(refvencimiento,3));
        //Diferente para linea de trade:
        item.setFrec_revision_tipo_int_per(formatStringWithBlankOnRight(frecrev,2));
    }


    /**
     * COPY 4
     * @param valDate
     */
    public AnacreditPersonaOperacionesItem formatPersonaItem(AnacreditOperacionesItem item, JDate valDate){ //TODO change
        String date = formatDate(valDate,8);
        AnacreditPersonaOperacionesItem operPerItem = null;
        if(null!=item){
            operPerItem = new AnacreditPersonaOperacionesItem();
            operPerItem.setId_entidad("0049");
            operPerItem.setFecha_alta_relacion(formatDate(valDate,8));
            operPerItem.setFecha_extraccion(date);
            operPerItem.setFecha_baja_relacion(item.getFecha_vencimiento());
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
        }

        return operPerItem;
    }


    @Override
    public List<AnacreditPersonaOperacionesItem> formatPersonaItem(AnacreditOperacionesItem item, ReportRow row, JDate valDate, Vector<String> errors) {
        List<AnacreditPersonaOperacionesItem> result = new ArrayList<>();
        AnacreditPersonaOperacionesItem personaItem = getPersonaDefaults(item, valDate);
        result.add(personaItem);
        return result;
    }


    public AnacreditOperacionesItem formatContractItem(CollateralConfig config, CashPositionDTO marginCallPosition , ReportRow rowInfo, JDate valDate, PricingEnv pricingEnv, Stack<String> errors) {

        AnacreditOperacionesItem item = new AnacreditOperacionesItem();

        String currency = marginCallPosition.getCurrency();
        String date =  formatDate(valDate,8);
        String datePlusOne = formatDate(valDate.addBusinessDays(1, Util.string2Vector(HOLIDAYS)),8);

        InventoryCashPosition inventoryCashPosition = rowInfo.getProperty(INVENTORY_CASH_POS);
        if (null == inventoryCashPosition) {
            log(LogLevel.ERR, config,"InventoryCash not found for this contract.", errors );
        }

        Trade tradeInterestBearing = (Trade) rowInfo.getProperties().get("INTEREST_BEARING");
        // Sumar Daily accrual del interest Bearing en negativo
        double interestDailyDivisa =  0.0;
        double interestDailyEUR =  0.0;

        if(tradeInterestBearing!=null) {
            InterestBearing interestBearing = (InterestBearing) tradeInterestBearing.getProduct();
            interestDailyDivisa = getDaily(interestBearing, valDate);
            interestDailyEUR = AnacreditUtilities.convertToEUR(interestDailyDivisa, interestBearing.getCurrency(), valDate, pricingEnv);
        } else   {
            log(LogLevel.WARN, config,  "InterestBearing trade not found for this contract.", errors);
        }

        if ((null == marginCallPosition || marginCallPosition.getAllInValue() == 0)
                && Double.compare(0.0, interestDailyEUR ) == 0) {
            log(LogLevel.ERR, config,  "Position and Interest are zeros for this contract. "+ currency, errors);
            return null;
        }

        // reject position positive for this extraction
        if(null!=marginCallPosition && marginCallPosition.getAllInValue()>0){
            //log(LogLevel.WARN, config,  "Position is Positive.", errors);
            return null;
        }

        Account account = _mapper.loadAccount(String.valueOf(config.getId()), currency);
        if(account == null){
            log(LogLevel.ERR, config,  "Line error, contract: "+ config.getId() + " - Account not found.", errors);
            return null;
        }

        AccountInterestConfig accInterest = _mapper.getAccInterest(account);
        setCommonAccountValues(accInterest, item, valDate);
        setContractAccountValues(accInterest, item);
        //campos comunes
        initDefaultOperacionesCopy3(item);

        removeFiller(item);
        //Campos únicos
        initDefaultOperContractItem(item);

        //ID de linea (id and id interno)
        item.setId_contrato(config, account, date,"");
        //Campos variables
        item.setMoneda(formatStringWithBlankOnRight(_mapper.getCurrencyMap(currency, errors, item.getId_contrato()),3));



        //TODO - Check the correct position object!!!!!!
        item.setSaldo_deudor_no_vencido(inventoryCashPosition ,numChar,valDate,pricingEnv);
        item.setSaldo_deudor_no_ven(inventoryCashPosition);

        item.setValor_nominal(item.getSaldo_deudor_no_vencido());
        item.setValor_nominal_d(item.getSaldo_deudor_no_ven());

        String j_minorista = _mapper.getJMin(config.getLegalEntity());
        item.setTipo_cartera_ifrs9(_mapper.getTipoCartera(config.getBook()));

        if(Util.isEmpty(j_minorista)){
            log(LogLevel.ERR, config,  " J MIN not found for LegalEntity: " + config.getLegalEntity().getCode(), errors);
            return null;
        }
        else if(_mapper.validAgent(config.getLegalEntity())){
            item.setTipo_cartera_ifrs9("99");
            item.setReconocimiento_balance("3");
        }

        item.setId_persona_contraparte_direct(j_minorista);

        item.setIntereses_devengados(interestDailyEUR);
        item.setIntereses_devengos(interestDailyDivisa);

        item.setFecha_primera_liquidacion(date);
        item.setFecha_estado_cumplimiento(date);
        item.setTipo_referencia_sustitutivo(accInterest);
        item.setProxima_revision_tipo_interes(datePlusOne);
        item.setCartera_prudencial();

        item.setFecha_ultimo_vto_intereses(date); //Trade fijo
        item.setFecha_proximo_vto_intereses(datePlusOne); //Trade fijo
        item.setFecha_proximo_vto_principal(datePlusOne); //Trade fijo

        //Fechas
        initDefaultDates(item,date,datePlusOne);
        return item;

    }

    /**
     *
     * Set Tipo_referencia_vencimiento to 0 when Indice_referencia_ac are I30 or I31
     * @param indice
     * @return
     */
    private boolean checkIndice(String indice){
        return !Util.isEmpty(indice) && ("I30".equalsIgnoreCase(indice) || "I31".equalsIgnoreCase(indice));
    }

}
