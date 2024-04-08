package calypsox.tk.report;


import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class FiReport {
    
    private DSConnection dsCon = null;
    private PricingEnv pricingEnv = null;
    private JDate startDate = null;
    private JDate endDate = null;
    private static String EMPTY_VALUE = "";
    private static String ZERO_VALUE = "0.000";
    private TradeArray trades211 = null;
    private TradeArray trades212 = null;
    private TradeArray trades2411 = null;
    private TradeArray trades2412 = null;
    private TradeArray trades2413 = null;
    
    Double nominalStart211 = 0.0;
    Double nominalStart212 = 0.0;
    Double nominalStart2411 = 0.0;
    Double nominalStart2412 = 0.0;
    Double nominalStart2413 = 0.0;
    Double nominalEnd211 = 0.0;
    Double nominalEnd212 = 0.0;
    Double nominalEnd2411 = 0.0;
    Double nominalEnd2412 = 0.0;
    Double nominalEnd2413 = 0.0;

    Double unrealizedStart211 = 0.0;
    Double unrealizedStart212 = 0.0;
    Double unrealizedStart2411 = 0.0;
    Double unrealizedStart2412 = 0.0;
    Double unrealizedStart2413 = 0.0;
    Double unrealizedEnd211 = 0.0;
    Double unrealizedEnd212 = 0.0;
    Double unrealizedEnd2411 = 0.0;
    Double unrealizedEnd2412 = 0.0;
    Double unrealizedEnd2413 = 0.0;

    Double averagePrice211 = 0.0;
    Double averagePrice212 = 0.0;
    Double averagePrice2411 = 0.0;
    Double averagePrice2412 = 0.0;
    Double averagePrice2413 = 0.0;

    Double valueF4 = 0.0;
    Double valueF5 = 0.0;
    Double valueF6 = 0.0;
    Double valueF7 = 0.0;
    Double valueF8 = 0.0;
    Double valueF9 = 0.0;
    Double valueF10 = 0.0;
    Double valueF11 = 0.0;
    Double valueF12 = 0.0;
    
    Double valueH4 = 0.0;
    Double valueH5 = 0.0;
    Double valueH6 = 0.0;
    Double valueH7 = 0.0;
    Double valueH8 = 0.0;
    Double valueH9 = 0.0;
    Double valueH10 = 0.0;
    Double valueH11 = 0.0;
    Double valueH12 = 0.0;
    
    Double valueJ4 = 0.0;
    Double valueJ5 = 0.0;
    Double valueJ6 = 0.0;
    Double valueJ7 = 0.0;
    Double valueJ8 = 0.0;
    Double valueJ9 = 0.0;
    Double valueJ10 = 0.0;
    Double valueJ11 = 0.0;
    Double valueJ12 = 0.0;
    
    Double valueL4 = 0.0;
    Double valueL5 = 0.0;
    Double valueL6 = 0.0;
    Double valueL7 = 0.0;
    Double valueL8 = 0.0;
    Double valueL9 = 0.0;
    Double valueL10 = 0.0;
    Double valueL11 = 0.0;
    Double valueL12 = 0.0;
    
    Double valueN4 = 0.0;
    Double valueN5 = 0.0;
    Double valueN6 = 0.0;
    Double valueN7 = 0.0;
    Double valueN8 = 0.0;
    Double valueN9 = 0.0;
    Double valueN10 = 0.0;
    Double valueN11 = 0.0;
    Double valueN12 = 0.0;
    
    Double valueP4 = 0.0;
    Double valueP5 = 0.0;
    Double valueP6 = 0.0;
    Double valueP7 = 0.0;
    Double valueP8 = 0.0;
    Double valueP9 = 0.0;
    Double valueP10 = 0.0;
    Double valueP11 = 0.0;
    Double valueP12 = 0.0;
    
    Double valueR4 = 0.0;
    Double valueR5 = 0.0;
    Double valueR6 = 0.0;
    Double valueR7 = 0.0;
    Double valueR8 = 0.0;
    Double valueR9 = 0.0;
    Double valueR10 = 0.0;
    Double valueR11 = 0.0;
    Double valueR12 = 0.0;
    
    Double valueT4 = 0.0;
    Double valueT5 = 0.0;
    Double valueT6 = 0.0;
    Double valueT7 = 0.0;
    Double valueT8 = 0.0;
    Double valueT9 = 0.0;
    Double valueT10 = 0.0;
    Double valueT11 = 0.0;
    Double valueT12 = 0.0;
    
    Double valueV4 = 0.0;
    Double valueV5 = 0.0;
    Double valueV6 = 0.0;
    Double valueV7 = 0.0;
    Double valueV8 = 0.0;
    Double valueV9 = 0.0;
    Double valueV10 = 0.0;
    Double valueV11 = 0.0;
    Double valueV12 = 0.0;
    
    Double valueX4 = 0.0;
    Double valueX5 = 0.0;
    Double valueX6 = 0.0;
    Double valueX7 = 0.0;
    Double valueX8 = 0.0;
    Double valueX9 = 0.0;
    Double valueX10 = 0.0;
    Double valueX11 = 0.0;
    Double valueX12 = 0.0;
    
    Double valueZ4 = 0.0;
    Double valueZ5 = 0.0;
    Double valueZ6 = 0.0;
    Double valueZ7 = 0.0;
    Double valueZ8 = 0.0;
    Double valueZ9 = 0.0;
    Double valueZ10 = 0.0;
    Double valueZ11 = 0.0;
    Double valueZ12 = 0.0;
    
    Double valueAB4 = 0.0;
    Double valueAB5 = 0.0;
    Double valueAB6 = 0.0;
    Double valueAB7 = 0.0;
    Double valueAB8 = 0.0;
    Double valueAB9 = 0.0;
    Double valueAB10 = 0.0;
    Double valueAB11 = 0.0;
    Double valueAB12 = 0.0;
    
    
    public static XSSFWorkbook getWorkbook(JDate valDate, PricingEnv pEnv, Vector<String> holidays){
        Log.info(FiReport.class.getSimpleName(), "Create the FiReport");
        FiReport fiReport = new FiReport();
        Log.info(FiReport.class.getSimpleName(), "Call Init method");
        fiReport.init(valDate, pEnv, holidays);
        Log.info(FiReport.class.getSimpleName(), "Get Trades");
        fiReport.getTrades();
        Log.info(FiReport.class.getSimpleName(), "Process Calculations");
        fiReport.processCalculations();
        Log.info(FiReport.class.getSimpleName(), "Fill Calculations");
        fiReport.fillCalculations();
        Log.info(FiReport.class.getSimpleName(), "Get the book");
        XSSFWorkbook book = fiReport.getBook();
        Log.info(FiReport.class.getSimpleName(), "End the XSSFWorkbook");
        return book;
    }

    
    private void processCalculations(){

        Log.system(this.getClass().getSimpleName(), "AltaBajaNominalContable 211: " + trades211.size());
        Double[] amounts211 = getAltaBajaNominalContable(trades211);
        valueF6 = amounts211[0];
        valueH6 = amounts211[1];
        valueJ6 = amounts211[2];
        valueL6 = amounts211[3];

        Log.system(this.getClass().getSimpleName(), "AltaBajaNominalContable 212: " + trades212.size());
        Double[] amounts212 = getAltaBajaNominalContable(trades212);
        valueF7 = amounts212[0];
        valueH7 = amounts212[1];
        valueJ7 = amounts212[2];
        valueL7 = amounts212[3];

        Log.system(this.getClass().getSimpleName(), "AltaBajaNominalContable 2411: " + trades2411.size());
        Double[] amounts2411 = getAltaBajaNominalContable(trades2411);
        valueF10 = amounts2411[0];
        valueH10 = amounts2411[1];
        valueJ10 = amounts2411[2];
        valueL10 = amounts2411[3];

        Log.system(this.getClass().getSimpleName(), "AltaBajaNominalContable 2412: " + trades2412.size());
        Double[] amounts2412 = getAltaBajaNominalContable(trades2412);
        valueF11 = amounts2412[0];
        valueH11 = amounts2412[1];
        valueJ11 = amounts2412[2];
        valueL11 = amounts2412[3];

        Log.system(this.getClass().getSimpleName(), "AltaBajaNominalContable 2413: " + trades2413.size());
        Double[] amounts2413 = getAltaBajaNominalContable(trades2413);
        valueF12 = amounts2413[0];
        valueH12 = amounts2413[1];
        valueJ12 = amounts2413[2];
        valueL12 = amounts2413[3];

        Log.info(this.getClass().getSimpleName(), "Call getImporteLiquidacion");
        getImporteLiquidacion();

        Log.info(this.getClass().getSimpleName(), "Call getPositionKeeper");
        getPositionKeeper();

        Log.info(this.getClass().getSimpleName(), "Call manageRevalorizacionesYAjustesNegativosYSaneamientosDirectos");
        manageRevalorizacionesYAjustesNegativosYSaneamientosDirectos();

        Log.info(this.getClass().getSimpleName(), "Call manageAjusteTipoCambioNominal");
        manageAjusteTipoCambioNominal();

        Log.info(this.getClass().getSimpleName(), "Call manageAjusteTipoCambioContable");
        manageAjusteTipoCambioContable();

        Log.info(this.getClass().getSimpleName(), "Call manageSaldoBalanceFinPeriodoNominal");
        manageSaldoBalanceFinPeriodoNominal();

        Log.info(this.getClass().getSimpleName(), "Call manageSaldoBalanceFinPeriodoContable");
        manageSaldoBalanceFinPeriodoContable();

        Log.info(this.getClass().getSimpleName(), "Call manageRevalorizacion");
        manageRevalorizacion();
    }


    private void fillCalculations(){

        // linea 4 --> sum(linea 5, linea 8)
        // linea 5 --> sum(linea 6, linea 7)
        // linea 6 --> VALUE_0
        // linea 7 --> IMPLEMENTED
        // linea 8 --> sum(linea 10, linea 11, linea 12)
        // linea 9 --> sum(linea 10, linea 11, linea 12)
        // linea 10 --> IMPLEMENTED
        // linea 11 --> IMPLEMENTED
        // linea 12 --> IMPLEMENTED

        valueF8 = valueF10 + valueF11 + valueF12;
        valueF9 = valueF8;
        valueF5 = valueF6 + valueF7;
        valueF4 = valueF5 + valueF8;

        valueH8 = valueH10 + valueH11 + valueH12;
        valueH9 = valueH8;
        valueH5 = valueH6 + valueH7;
        valueH4 = valueH5 + valueH8;

        valueJ8 = valueJ10 + valueJ11 + valueJ12;
        valueJ9 = valueJ8;
        valueJ5 = valueJ6 + valueJ7;
        valueJ4 = valueJ5 + valueJ8;

        valueL8 = valueL10 + valueL11 + valueL12;
        valueL9 = valueL8;
        valueL5 = valueL6 + valueL7;
        valueL4 = valueL5 + valueL8;

        valueN8 = valueN10 + valueN11 + valueN12;
        valueN9 = valueN8;
        valueN5 = valueN6 + valueN7;
        valueN4 = valueN5 + valueN8;

        valueP8 = valueP10 + valueP11 + valueP12;
        valueP9 = valueP8;
        valueP5 = valueP6 + valueP7;
        valueP4 = valueP5 + valueP8;

        valueR8 = valueR10 + valueR11 + valueR12;
        valueR9 = valueR8;
        valueR5 = valueR6 + valueR7;
        valueR4 = valueR5 + valueR8;

        valueT8 = valueT10 + valueT11 + valueT12;
        valueT9 = valueT8;
        valueT5 = valueT6 + valueT7;
        valueT4 = valueT5 + valueT8;

        valueV8 = valueV10 + valueV11 + valueV12;
        valueV9 = valueV8;
        valueV5 = valueV6 + valueV7;
        valueV4 = valueV5 + valueV8;

        valueX8 = valueX10 + valueX11 + valueX12;
        valueX9 = valueX8;
        valueX5 = valueX6 + valueX7;
        valueX4 = valueX5 + valueX8;

        valueZ8 = valueZ10 + valueZ11 + valueZ12;
        valueZ9 = valueZ8;
        valueZ5 = valueZ6 + valueZ7;
        valueZ4 = valueZ5 + valueZ8;

        valueAB8 = valueAB10 + valueAB11 + valueAB12;
        valueAB9 = valueAB8;
        valueAB5 = valueAB6 + valueAB7;
        valueAB4 = valueAB5 + valueAB8;
    }


    private String getSectoresContables(String dv){
        StringBuilder sectorList = new StringBuilder();
        String dvSc = LocalCache.getDomainValueComment(dsCon, "domainName", dv);
        if(Util.isEmpty(dvSc)){
            return sectorList.toString();
        }
        String[] sectorSplit = dvSc.split(",");
        if((sectorSplit == null) || sectorSplit.length < 1){
            return sectorList.toString();
        }

        for (int i=0; i < sectorSplit.length; i++) {
            String dvSector = sectorSplit[i];
            if (dvSector.contains("-")){
                String[] rango = dvSector.split("-");
                int start = Integer.parseInt(rango[0].trim());
                int end = Integer.parseInt(rango[1].trim());
                for(int y=start; y<=end; y++){
                    sectorList.append("'" + formatSectorContable(String.valueOf(y)) + "',");
                }
            }
            else{
                sectorList.append("'" + formatSectorContable(dvSector.trim()) + "',");
            }
        }
        String sectores = sectorList.toString();
        if(sectores.endsWith(",")){
            sectores = sectores.substring(0,sectores.length()-1);
        }
        return sectores;
    }
    
    
    private String formatSectorContable(String sector){
        String sectorReturn = sector;
        if(sectorReturn.length() != 3) {
            for (int i=sectorReturn.length() ; i<3; i++) {
                sectorReturn = "0" + sectorReturn;
            }
        }
        return sectorReturn;
    }


    private List<String> getSectoresContablesAsList(String dv){
        List<String> sectores = new ArrayList<String>();

        String dvSc = LocalCache.getDomainValueComment(dsCon, "domainName", dv);
        if(Util.isEmpty(dvSc)){
            return sectores;
        }
        String[] sectorSplit = dvSc.split(",");
        if((sectorSplit == null) || sectorSplit.length < 1){
            return sectores;
        }

        for (int i=0; i < sectorSplit.length; i++) {
            String dvSector = sectorSplit[i];
            if (dvSector.contains("-")){
                String[] rango = dvSector.split("-");
                int start = Integer.parseInt(rango[0].trim());
                int end = Integer.parseInt(rango[1].trim());
                for(int y=start; y<=end; y++){
                    sectores.add(formatSectorContable(String.valueOf(y)));
                }
            }
            else{
                sectores.add(formatSectorContable(dvSector.trim()));
            }
        }

        return sectores;
    }


    private void init(JDate valDate, PricingEnv pEnv, Vector<String> holidays) {
        dsCon = DSConnection.getDefault();
        pricingEnv = pEnv;
        endDate = valDate.addBusinessDays(1,holidays).addBusinessDays(-1,holidays);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(valDate.getDate(TimeZone.getDefault()));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        final JDate firstDayCurrMonth = JDate.valueOf(calendar.getTime());
        startDate = firstDayCurrMonth.addMonths(-2).addBusinessDays(-1,holidays).addBusinessDays(1,holidays);
        Log.system(this.getClass().getSimpleName(),"\n" + "ValDate: " + valDate.toString() + " --> StartDate: " + startDate + "  " + "EndDate: " + endDate.toString() + "\n");
        // 1. 01/01/yyyy - 31/03/yyyy --- ValDate: 31/03/2020 --> StartDate: 01/01/2020  EndDate: 31/03/2020
        // 2. 01/04/yyyy - 30/06/yyyy --- ValDate: 30/06/2020 --> StartDate: 01/04/2020  EndDate: 30/06/2020
        // 3. 01/07/yyyy - 30/09/yyyy --- ValDate: 30/09/2020 --> StartDate: 01/07/2020  EndDate: 30/09/2020
        // 4. 01/10/yyyy - 31/12/yyyy --- ValDate: 31/12/2020 --> StartDate: 01/10/2020  EndDate: 31/12/2020
    }
    
    
    private void getTrades() {
        trades211 = getTrades211();
        trades212 = getTrades212();
        trades2411 = getTrades2411();
        trades2412 = getTrades2412();
        trades2413 = getTrades2413();
    }
    
    
    private TradeArray getTrades211() {
        TradeArray trades = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String sectoresContables = getSectoresContables("ReportFI134_SectorContable_211");
        
        StringBuilder fromClause = new StringBuilder("product_desc p, product_sec_code psc, legal_entity le, le_attribute leAttr");
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("trade.trade_status NOT IN ('CANCELED') AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') >= '" + dateFormat.format(startDate.getDate()) + "' AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') <= '" + dateFormat.format(endDate.getDate()) + "' AND ");
        whereClause.append("trade.product_id = p.product_id AND ");
        whereClause.append("p.product_type = 'Equity' AND ");
        whereClause.append("p.product_id = psc.product_id AND ");
        whereClause.append("psc.sec_code = 'EQUITY_TYPE' AND ");
        whereClause.append("psc.code_value IN ('PEGROP') AND ");
        whereClause.append("p.issuer_id = le.legal_entity_id AND ");
        whereClause.append("le.legal_entity_id = leAttr.legal_entity_id AND ");
        whereClause.append("leAttr.attribute_type = 'SECTORCONTABLE' AND ");
        whereClause.append("leAttr.attribute_value IN (" + sectoresContables + ")");
        whereClause.append("AND NOT EXISTS (SELECT keyword_value FROM trade_keyword WHERE trade_id = trade.trade_id AND keyword_name='Internal')");
        try {
            trades = dsCon.getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
        } catch (final RemoteException e) {
            Log.error(this.getClass().getSimpleName(), "Error while getting trade query");
        }
        
        String logMsg = "\n" + "Trades 211: " + trades.size() + "\n" + "From" + "\n" + fromClause.toString() + "\n" + "Where" + "\n" + whereClause.toString() + "\n";
        Log.system(this.getClass().getSimpleName(),logMsg);
        return trades;
    }
    
    
    private TradeArray getTrades212() {
        TradeArray trades = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String sectoresContables = getSectoresContables("ReportFI134_SectorContable_211");
    
        StringBuilder fromClause = new StringBuilder("product_desc p, product_sec_code psc, legal_entity le, le_attribute leAttr");
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("trade.trade_status NOT IN ('CANCELED') AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') >= '" + dateFormat.format(startDate.getDate()) + "' AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') <= '" + dateFormat.format(endDate.getDate()) + "' AND ");
        whereClause.append("trade.product_id = p.product_id AND ");
        whereClause.append("p.product_type = 'Equity' AND ");
        whereClause.append("p.product_id = psc.product_id AND ");
        whereClause.append("psc.sec_code = 'EQUITY_TYPE' AND ");
        whereClause.append("psc.code_value IN ('PEGROP') AND ");
        whereClause.append("p.issuer_id = le.legal_entity_id AND ");
        whereClause.append("le.legal_entity_id = leAttr.legal_entity_id AND ");
        whereClause.append("leAttr.attribute_type = 'SECTORCONTABLE' AND ");
        whereClause.append("leAttr.attribute_value NOT IN (" + sectoresContables + ")");
        whereClause.append("AND NOT EXISTS (SELECT keyword_value FROM trade_keyword WHERE trade_id = trade.trade_id AND keyword_name='Internal')");
        try {
            trades = dsCon.getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
        } catch (final RemoteException e) {
            Log.error(this.getClass().getSimpleName(), "Error while getting trade query");
        }
    
        String logMsg = "\n" + "Trades 212: " + trades.size() + "\n" + "From" + "\n" + fromClause.toString() + "\n" + "Where" + "\n" + whereClause.toString() + "\n";
        Log.system(this.getClass().getSimpleName(), logMsg);
        return trades;
    }
    
    
    private TradeArray getTrades2411() {
        TradeArray trades = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String sectoresContables = getSectoresContables("ReportFI134_SectorContable_2411");
    
        StringBuilder fromClause = new StringBuilder("product_desc p, product_sec_code psc, legal_entity le, le_attribute leAttr");
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("trade.trade_status NOT IN ('CANCELED') AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') >= '" + dateFormat.format(startDate.getDate()) + "' AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') <= '" + dateFormat.format(endDate.getDate()) + "' AND ");
        whereClause.append("trade.product_id = p.product_id AND ");
        whereClause.append("p.product_type = 'Equity' AND ");
        whereClause.append("p.product_id = psc.product_id AND ");
        whereClause.append("psc.sec_code = 'EQUITY_TYPE' AND ");
        whereClause.append("psc.code_value NOT IN ('PEGROP','PS') AND ");
        whereClause.append("p.issuer_id = le.legal_entity_id AND ");
        whereClause.append("le.legal_entity_id = leAttr.legal_entity_id AND ");
        whereClause.append("leAttr.attribute_type = 'SECTORCONTABLE' AND ");
        whereClause.append("leAttr.attribute_value IN (" + sectoresContables + ")");
        whereClause.append("AND NOT EXISTS (SELECT keyword_value FROM trade_keyword WHERE trade_id = trade.trade_id AND keyword_name='Internal')");
        try {
            trades = dsCon.getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
        } catch (final RemoteException e) {
            Log.error(this.getClass().getSimpleName(), "Error while getting trade query");
        }
        
        String logMsg = "\n" + "Trades 2411: " + trades.size() + "\n" + "From" + "\n" + fromClause.toString() + "\n" + "Where" + "\n" + whereClause.toString() + "\n";
        Log.system(this.getClass().getSimpleName(),logMsg);
        return trades;
    }
    
    
    private TradeArray getTrades2412() {
        TradeArray trades = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String sectoresContables = getSectoresContables("ReportFI134_SectorContable_2412");
    
        StringBuilder fromClause = new StringBuilder("product_desc p, product_sec_code psc, legal_entity le, le_attribute leAttr");
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("trade.trade_status NOT IN ('CANCELED') AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') >= '" + dateFormat.format(startDate.getDate()) + "' AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') <= '" + dateFormat.format(endDate.getDate()) + "' AND ");
        whereClause.append("trade.product_id = p.product_id AND ");
        whereClause.append("p.product_type = 'Equity' AND ");
        whereClause.append("p.product_id = psc.product_id AND ");
        whereClause.append("psc.sec_code = 'EQUITY_TYPE' AND ");
        whereClause.append("psc.code_value NOT IN ('PEGROP','PS') AND ");
        whereClause.append("p.issuer_id = le.legal_entity_id AND ");
        whereClause.append("le.legal_entity_id = leAttr.legal_entity_id AND ");
        whereClause.append("leAttr.attribute_type = 'SECTORCONTABLE' AND ");
        whereClause.append("leAttr.attribute_value IN (" + sectoresContables + ")");
        whereClause.append("AND NOT EXISTS (SELECT keyword_value FROM trade_keyword WHERE trade_id = trade.trade_id AND keyword_name='Internal')");
        try {
            trades = dsCon.getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
        } catch (final RemoteException e) {
            Log.error(this.getClass().getSimpleName(), "Error while getting trade query");
        }
    
        String logMsg = "\n" + "Trades 2412: " + trades.size() + "\n" + "From" + "\n" + fromClause.toString() + "\n" + "Where" + "\n" + whereClause.toString() + "\n";
        Log.system(this.getClass().getSimpleName(),logMsg);
        return trades;
    }
    
    
    private TradeArray getTrades2413() {
        TradeArray trades = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String sectoresContables = getSectoresContables("ReportFI134_SectorContable_2413");
    
        StringBuilder fromClause = new StringBuilder("product_desc p, product_sec_code psc, legal_entity le, le_attribute leAttr");
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("trade.trade_status NOT IN ('CANCELED') AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') >= '" + dateFormat.format(startDate.getDate()) + "' AND ");
        whereClause.append("TO_CHAR(trade.settlement_date, 'YYYY-MM-DD') <= '" + dateFormat.format(endDate.getDate()) + "' AND ");
        whereClause.append("trade.product_id = p.product_id AND ");
        whereClause.append("p.product_type = 'Equity' AND ");
        whereClause.append("p.product_id = psc.product_id AND ");
        whereClause.append("psc.sec_code = 'EQUITY_TYPE' AND ");
        whereClause.append("psc.code_value NOT IN ('PEGROP','PS') AND ");
        whereClause.append("p.issuer_id = le.legal_entity_id AND ");
        whereClause.append("le.legal_entity_id = leAttr.legal_entity_id AND ");
        whereClause.append("leAttr.attribute_type = 'SECTORCONTABLE' AND ");
        whereClause.append("leAttr.attribute_value IN (" + sectoresContables + ")");
        whereClause.append("AND NOT EXISTS (SELECT keyword_value FROM trade_keyword WHERE trade_id = trade.trade_id AND keyword_name='Internal')");
        try {
            trades = dsCon.getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
        } catch (final RemoteException e) {
            Log.error(this.getClass().getSimpleName(), "Error while getting trade query");
        }
    
        String logMsg = "\n" + "Trades 2413: " + trades.size() + "\n" + "From" + "\n" + fromClause.toString() + "\n" + "Where" + "\n" + whereClause.toString() + "\n";
        Log.system(this.getClass().getSimpleName(),logMsg);
        return trades;
    }
    

    private Double[] getAltaBajaNominalContable(TradeArray tradeArray){
        Double[] rst = new Double[4];
        Double altaNominal = 0.0;
        Double altaContable = 0.0;
        Double bajaNominal = 0.0;
        Double bajaContable = 0.0;
        for (Trade trade : tradeArray.asList()) {
            Product product = trade.getProduct();
            if(product instanceof Equity){
                Equity equity = (Equity) product;
                Double feesAmount = getAllFeesAmount(trade);
                if(equity.getBuySell(trade)==1){
                    altaNominal += convertEUR(trade.getTradeCurrency(),trade.getQuantity(), trade.getSettleDate());
                    altaContable += convertEUR(trade.getTradeCurrency(), trade.getQuantity(), trade.getSettleDate()) + feesAmount;
                }
                else {
                    bajaNominal += convertEUR(trade.getTradeCurrency(),trade.getQuantity(), trade.getSettleDate());
                    bajaContable += convertEUR(trade.getTradeCurrency(),trade.getQuantity(), trade.getSettleDate()) + feesAmount;
                }
            }
        }
        rst[0] = altaNominal;
        rst[1] = altaContable;
        rst[2] = bajaNominal;
        rst[3] = bajaContable;
        return rst;
    }
    
    
    private Double getImporteLiquidacionTrades(TradeArray tradeArray){
        Double rst = 0.0;
        TransferArray transferArray = null;
        for (Trade trade : tradeArray.asList()) {
            Product product = trade.getProduct();
            if(product instanceof Equity){
                Equity equity = (Equity) product;
                try {
                    transferArray = dsCon.getRemoteBO().getBOTransfers(trade.getLongId());
                    for (int i = 0; i < transferArray.size(); i++) {
                        BOTransfer transfer = transferArray.get(i);
                        if ("SETTLED".equalsIgnoreCase(transfer.getStatus().getStatus())){
                            rst += convertEUR(transfer.getSettlementCurrency(), transfer.getOtherAmount(), transfer.getSettleDate());
                        }
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this.getClass().getSimpleName(), "Error while retrieving the transfers.");
                }
            }
        }
        return rst;
    }
    
    
    private void getPositionKeeper(){


        Double localUnrealizedEnd211 = 0.0;
        Double localUnrealizedEnd212 = 0.0;
        Double localUnrealizedEnd2411 = 0.0;
        Double localUnrealizedEnd2412 = 0.0;
        Double localUnrealizedEnd2413= 0.0;
        Double localUnrealizedStart211 = 0.0;
        Double localUnrealizedStart212 = 0.0;
        Double localUnrealizedStart2411 = 0.0;
        Double localUnrealizedStart2412 = 0.0;
        Double localUnrealizedStart2413= 0.0;
        Double localNominalEnd211 = 0.0;
        Double localNominalEnd212 = 0.0;
        Double localNominalEnd2411 = 0.0;
        Double localNominalEnd2412 = 0.0;
        Double localNominalEnd2413= 0.0;
        Double localNominalStart211 = 0.0;
        Double localNominalStart212 = 0.0;
        Double localNominalStart2411 = 0.0;
        Double localNominalStart2412 = 0.0;
        Double localNominalStart2413= 0.0;
        Double localAveragePrice211 = 0.0;
        Double localAveragePrice212 = 0.0;
        Double localAveragePrice2411 = 0.0;
        Double localAveragePrice2412 = 0.0;
        Double localAveragePrice2413= 0.0;

        String reportType = "EquityMisPlusCartera";
        String templateName = "EquityMisPlusCarteraReal";
        DefaultReportOutput outputStartDate = null;
        DefaultReportOutput outputEndDate = null;
        StringBuilder logMsg = new StringBuilder("");
        Log.system(this.getClass().getSimpleName(), "\n" + "*** POSITION KEEPER ***");

        try {
            Log.system(this.getClass().getSimpleName(), "\n" + "Call to EquityMisPlus Report at End Date");
            outputEndDate = (DefaultReportOutput) generateReportOutput(reportType, templateName, new JDatetime(endDate, TimeZone.getDefault()));
            Log.system(this.getClass().getSimpleName(), "\n" + "Call to EquityMisPlus Report at Start Date");
            outputStartDate = (DefaultReportOutput) generateReportOutput(reportType, templateName, new JDatetime(startDate, TimeZone.getDefault()));
            Log.system(this.getClass().getSimpleName(), "\n" + "Finish call report EquityMisPlus");
            HashMap<String, Object> hashMap= new HashMap<String, Object>();
            List<String> sectorList211 = getSectoresContablesAsList("ReportFI134_SectorContable_211");
            List<String> sectorList2411 = getSectoresContablesAsList("ReportFI134_SectorContable_2411");
            List<String> sectorList2412 = getSectoresContablesAsList("ReportFI134_SectorContable_2412");
            List<String> sectorList2413 = getSectoresContablesAsList("ReportFI134_SectorContable_2413");

            if(outputStartDate != null && outputEndDate != null) {
                ReportRow[] rowsStartDate = outputStartDate.getRows();
                logMsg = new StringBuilder("\n" + "POSITION KEEPER A INICIO DE TRIMESTRE (" + startDate.toString() + ") " + rowsStartDate.length + " posiciones." + "\n");
                for (int i = 0; i < rowsStartDate.length; i++) {
                    hashMap = (HashMap<String, Object>) rowsStartDate[i].getProperties().get("HashMap");
                    logMsg.append("   PositionId: " + hashMap.get("Position Id") + " - ProductId: " + hashMap.get("Product Id") + " - ISIN: " + hashMap.get("ISIN") + " - Currency: " + hashMap.get("Currency") + " - Nominal: " + hashMap.get("Nominal") + " - Unrealized: " + hashMap.get("Unrealized") + "\n");
                }
                Log.system(this.getClass().getSimpleName(), "\n" + logMsg.toString());

                ReportRow[] rowsEndDate = outputEndDate.getRows();
                logMsg = new StringBuilder("\n" + "POSITION KEEPER A FIN DE TRIMESTRE (" + endDate.toString() + ") " + rowsEndDate.length + " posiciones." + "\n");
                for (int i = 0; i < rowsEndDate.length; i++) {
                    hashMap = (HashMap<String, Object>) rowsEndDate[i].getProperties().get("HashMap");
                    logMsg.append("   " + hashMap.get("Position Id") + " " + hashMap.get("Product Id") + " " + hashMap.get("ISIN") + " " + hashMap.get("Currency") + " " + hashMap.get("Unrealized") + "\n");
                }
                Log.system(this.getClass().getSimpleName(), "\n" + logMsg.toString());
            }

            if(outputStartDate != null) {
                ReportRow[] rowsStartDate = outputStartDate.getRows();
                logMsg = new StringBuilder("\n" + "SECTORES CONTABLES - POSITION KEEPER A INICIO DE TRIMESTRE (" + startDate.toString() + ") " + rowsStartDate.length + " posiciones." + "\n");
                for (int i = 0; i < rowsStartDate.length; i++) {
                    //system.out.println("<--- START ---> " + i);
                    hashMap = (HashMap<String, Object>) rowsStartDate[i].getProperties().get("HashMap");
                    int productId = (int) hashMap.get("Product Id");
                    Product product = dsCon.getRemoteProduct().getProduct(productId);
                    if (product != null &&  product instanceof Equity) {
                        Equity equity =(Equity) product;
                        String equityType = equity.getSecCode("EQUITY_TYPE");
                        String ccy = hashMap.get("Currency").toString();
                        Double unrealizedCcy = (Double.parseDouble(hashMap.get("Unrealized").toString().replace(".", "").replace(",", ".")));
                        Double nominalCcy = (Double.parseDouble(hashMap.get("Nominal").toString().replace(".", "").replace(",", ".")));
                        Double unrealized = convertEUR(ccy, unrealizedCcy, startDate);
                        Double nominal = convertEUR(ccy, nominalCcy, startDate);
                        if(equity.getIssuer() == null){
                            logMsg.append("\n" + "ISSUER NULL (" + unrealized + ")  ");
                        }
                        else {
                            String sectorContable = getLegalEntityAtribute(equity.getIssuer(), "SECTORCONTABLE");
                            if ("PEGROP".equalsIgnoreCase(equityType) && sectorList211.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedStart211  (" + localUnrealizedStart211 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalStart211  (" + localNominalStart211 + " + " + nominal + ")  ");
                                localUnrealizedStart211 += unrealized;
                                localNominalStart211 += nominal;
                            } else if ("PEGROP".equalsIgnoreCase(equityType) && !sectorList211.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedStart212  (" + localUnrealizedStart212 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalStart212  (" + localNominalStart212 + " + " + nominal + ")  ");
                                localUnrealizedStart212 += unrealized;
                                localNominalStart212 += nominal;
                            } else if (!"PEGROP".equalsIgnoreCase(equityType) && !"PS".equalsIgnoreCase(equityType) && sectorList2411.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedStart2411  (" + localUnrealizedStart2411 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalStart2411  (" + localNominalStart2411 + " + " + nominal + ")  ");
                                localUnrealizedStart2411 += unrealized;
                                localNominalStart2411 += nominal;
                            } else if (!"PEGROP".equalsIgnoreCase(equityType) && !"PS".equalsIgnoreCase(equityType) && sectorList2412.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedStart2412  (" + localUnrealizedStart2412 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalStart2412  (" + localNominalStart2412 + " + " + nominal + ")  ");
                                localUnrealizedStart2412 += unrealized;
                                localNominalStart2412 += nominal;
                            } else if (!"PEGROP".equalsIgnoreCase(equityType) && !"PS".equalsIgnoreCase(equityType) && sectorList2413.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedStart2413  (" + localUnrealizedStart2413 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalStart2413  (" + localNominalStart2413 + " + " + nominal + ")  ");
                                localUnrealizedStart2413 += unrealized;
                                localNominalStart2413 += nominal;
                            } else {
                                logMsg.append("\n" + "NO SUM  (Nominal: " + nominal + " Unrealized: " + unrealized + ")  ");
                            }
                        }
                    }
                    logMsg.append("PositionId: " + hashMap.get("Position Id") + " - ProductId: " + hashMap.get("Product Id") + " - ISIN: " + hashMap.get("ISIN") + " - Currency: " + hashMap.get("Currency") + " - Nominal: " + hashMap.get("Nominal") + " - Unrealized: " + hashMap.get("Unrealized") + "\n");
                }
                Log.system(this.getClass().getSimpleName(), "\n" + logMsg.toString());
            }

            //system.out.println("SEGUNDO SECTOR CONTABLEEEEEEEEEEE");
            logMsg = new StringBuilder("\n" + "SECTORES CONTABLES - POSITION KEEPER a " + endDate.toString() +"\n");
            if(outputEndDate != null) {
                ReportRow[] rowsEndDate = outputEndDate.getRows();
                logMsg = new StringBuilder("\n" + "SECTORES CONTABLES - POSITION KEEPER A FIN DE TRIMESTRE (" + endDate.toString() + ") " + rowsEndDate.length + " posiciones." + "\n");
                for (int i = 0; i < rowsEndDate.length; i++) {
                    //system.out.println("<--- END ---> " + i);
                    hashMap = (HashMap<String, Object>) rowsEndDate[i].getProperties().get("HashMap");
                    int productId = (int) hashMap.get("Product Id");
                    Product product = dsCon.getRemoteProduct().getProduct(productId);
                    if (product != null &&  product instanceof Equity) {
                        Equity equity =(Equity) product;
                        String equityType = equity.getSecCode("EQUITY_TYPE");
                        String ccy = hashMap.get("Currency").toString();
                        Double unrealizedCcy = (Double.parseDouble(hashMap.get("Unrealized").toString().replace(".", "").replace(",", ".")));
                        Double nominalCcy = (Double.parseDouble(hashMap.get("Nominal").toString().replace(".", "").replace(",", ".")));
                        Double averagePriceCcy = 1.0d;;
                        if(!Util.isEmpty(hashMap.get("Average Price").toString())) {
                            averagePriceCcy = (Double.parseDouble(hashMap.get("Average Price").toString().replace(".", "").replace(",", ".")));
                        }
                        Double unrealized = convertEUR(ccy, unrealizedCcy, endDate);
                        Double nominal = convertEUR(ccy, nominalCcy, endDate);
                        Double averagePrice = convertEUR(ccy, averagePriceCcy, endDate);
                        if(equity.getIssuer() == null){
                            logMsg.append("\n" + "ISSUER NULL (" + unrealized + ")  ");
                        }
                        else {
                            String sectorContable = getLegalEntityAtribute(equity.getIssuer(), "SECTORCONTABLE");
                            if ("PEGROP".equalsIgnoreCase(equityType) && sectorList211.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedEndt211  (" + localUnrealizedEnd211 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalEnd211  (" + localNominalEnd211 + " + " + nominal + ")  ");
                                logMsg.append("\n" + "localAveragePrice211  (" + localAveragePrice211 + " + " + nominal * averagePrice + ")  ");
                                localUnrealizedEnd211 += unrealized;
                                localNominalEnd211 += nominal;
                                localAveragePrice211 += averagePrice * nominal;
                            } else if ("PEGROP".equalsIgnoreCase(equityType) && !sectorList211.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedEnd212  (" + localUnrealizedEnd212 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalEnd212  (" + localNominalEnd212 + " + " + nominal + ")  ");
                                logMsg.append("\n" + "localAveragePrice212  (" + localAveragePrice212 + " + " + nominal * averagePrice + ")  ");
                                localUnrealizedEnd212 += unrealized;
                                localNominalEnd212 += nominal;
                                localAveragePrice212 += averagePrice * nominal;
                            } else if (!"PEGROP".equalsIgnoreCase(equityType) && !"PS".equalsIgnoreCase(equityType) && sectorList2411.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedEnd2411  (" + localUnrealizedEnd2411 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalEnd2411  (" + localNominalEnd2411 + " + " + nominal + ")  ");
                                logMsg.append("\n" + "localAveragePrice2411  (" + localAveragePrice2411 + " + " + nominal * averagePrice + ")  ");
                                localUnrealizedEnd2411 += unrealized;
                                localNominalEnd2411 += nominal;
                                localAveragePrice2411 += averagePrice * nominal;
                            } else if (!"PEGROP".equalsIgnoreCase(equityType) && !"PS".equalsIgnoreCase(equityType) && sectorList2412.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedEnd2412  (" + localUnrealizedEnd2412 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalEnd2412  (" + localNominalEnd2412 + " + " + nominal + ")  ");
                                logMsg.append("\n" + "localAveragePrice2412  (" + localAveragePrice2412 + " + " + nominal * averagePrice + ")  ");
                                localUnrealizedEnd2412 += unrealized;
                                localNominalEnd2412 += nominal;
                                localAveragePrice2412 += averagePrice * nominal;
                            } else if (!"PEGROP".equalsIgnoreCase(equityType) && !"PS".equalsIgnoreCase(equityType) && sectorList2413.contains(sectorContable)) {
                                logMsg.append("\n" + "localUnrealizedEnd2413  (" + localUnrealizedEnd2413 + " + " + unrealized + ")  ");
                                logMsg.append("\n" + "localNominalEnd2413  (" + localNominalEnd2413 + " + " + nominal + ")  ");
                                logMsg.append("\n" + "localAveragePrice2413  (" + localAveragePrice2413 + " + " + nominal * averagePrice + ")  ");
                                localUnrealizedEnd2413 += unrealized;
                                localNominalEnd2413 += nominal;
                                localAveragePrice2413 += averagePrice * nominal;
                            } else {
                                logMsg.append("\n" + "NO SUM  (Nominal: " + nominal + " Unrealized: " + unrealized + ")  ");
                            }
                        }
                    }
                    logMsg.append("PositionId: " + hashMap.get("Position Id") + " - ProductId: " + hashMap.get("Product Id") + " - ISIN: " + hashMap.get("ISIN") + " - Currency: " + hashMap.get("Currency") + " - Nominal: " + hashMap.get("Nominal") + " - Unrealized: " + hashMap.get("Unrealized") + "\n");
                }
                Log.system(this.getClass().getSimpleName(), "\n" + logMsg.toString());
            }

        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), "Error while getting the product from Data Server.");
        } catch (RemoteException e) {
            Log.error(this.getClass().getSimpleName(), "Error while generating Report Output.");
        }

        unrealizedEnd211 = localUnrealizedEnd211;
        unrealizedEnd212 = localUnrealizedEnd212;
        unrealizedEnd2411 = localUnrealizedEnd2411;
        unrealizedEnd2412 = localUnrealizedEnd2412;
        unrealizedEnd2413 = localUnrealizedEnd2413;
        nominalEnd211 = localNominalEnd211;
        nominalEnd212 = localNominalEnd212;
        nominalEnd2411 = localNominalEnd2411;
        nominalEnd2412 = localNominalEnd2412;
        nominalEnd2413 = localNominalEnd2413;

        unrealizedStart211 = localUnrealizedStart211;
        unrealizedStart212 = localUnrealizedStart212;
        unrealizedStart2411 = localUnrealizedStart2411;
        unrealizedStart2412 = localUnrealizedStart2412;
        unrealizedStart2413 = localUnrealizedStart2413;
        nominalStart211 = localNominalStart211;
        nominalStart212 = localNominalStart212;
        nominalStart2411 = localNominalStart2411;
        nominalStart2412 = localNominalStart2412;
        nominalStart2413 = localNominalStart2413;

        averagePrice211 = localAveragePrice211;
        averagePrice212 = localAveragePrice212;
        averagePrice2411 = localAveragePrice2411;
        averagePrice2412 = localAveragePrice2412;
        averagePrice2413 = localAveragePrice2413;
    }


    private void manageRevalorizacionesYAjustesNegativosYSaneamientosDirectos(){
        Log.system(this.getClass().getSimpleName(), "Revalorizaciones Y Ajustes Negativos Y Saneamientos Directos.");

        if(unrealizedEnd211 - unrealizedStart211 > 0.0){
            valueP6 = unrealizedEnd211 - unrealizedStart211;
            valueR6 = 0.000;
        }
        else{
            valueP6 = 0.000;
            valueR6 = unrealizedEnd211 - unrealizedStart211;
        }

        if(unrealizedEnd212 - unrealizedStart212 > 0.0){
            valueP7 = unrealizedEnd212 - unrealizedStart212;
            valueR7 = 0.000;
        }
        else{
            valueP7 = 0.000;
            valueR7 = unrealizedEnd212 - unrealizedStart212;
        }

        if(unrealizedEnd2411 - unrealizedStart2411 > 0.0){
            valueP10 = unrealizedEnd2411 - unrealizedStart2411;
            valueR10 = 0.000;
        }
        else{
            valueP10 = 0.000;
            valueR10 = unrealizedEnd2411 - unrealizedStart2411;
        }

        if(unrealizedEnd2412 - unrealizedStart2412 > 0.0){
            valueP11 = unrealizedEnd2412 - unrealizedStart2412;
            valueR11 = 0.000;
        }
        else{
            valueP11 = 0.000;
            valueR11 = unrealizedEnd2412 - unrealizedStart2412;
        }

        if(unrealizedEnd2413 - unrealizedStart2413 > 0.0){
            valueP12 = unrealizedEnd2413 - unrealizedStart2413;
            valueR12 = 0.000;
        }
        else{
            valueP12 = 0.000;
            valueR12 = unrealizedEnd2413 - unrealizedStart2413;
        }
    }


    private void manageAjusteTipoCambioNominal() {
        Log.system(this.getClass().getSimpleName(), "Ajuste Tipo Cambio Nominal.");
        valueT6 = nominalEnd211 - nominalStart211 + valueF6 - valueJ6;
        valueT7 = nominalEnd212 - nominalStart212 + valueF7 - valueJ7;
        valueT10 = nominalEnd2411 - nominalStart2411 + valueF10 - valueJ10 ;
        valueT11 = nominalEnd2412 - nominalStart2412 + valueF11 - valueJ11 ;
        valueT12 = nominalEnd2413 - nominalStart2413 + valueF12 - valueJ12 ;
    }


    private void manageAjusteTipoCambioContable() {
        Log.system(this.getClass().getSimpleName(), "Ajuste Tipo Cambio Contable.");
        valueV6 = unrealizedEnd211 - unrealizedStart211 + valueH6 - valueL6 + valueP6 - valueR6;
        valueV7 = unrealizedEnd212 - unrealizedStart212 + valueH7 - valueL7 + valueP7 - valueR7;
        valueV10 = unrealizedEnd2411 - unrealizedStart2411 + valueH10 - valueL10 + valueP10 - valueR10;
        valueV11 = unrealizedEnd2412 - unrealizedStart2412 + valueH11 - valueL11 + valueP11 - valueR11;
        valueV12 = unrealizedEnd2413 - unrealizedStart2413 + valueH12 - valueL12 + valueP12 - valueR12;
    }


    private void manageSaldoBalanceFinPeriodoNominal() {
        Log.system(this.getClass().getSimpleName(), "Saldo Balance Fin Periodo Nominal.");
        valueX6 = nominalEnd211;
        valueX7 = nominalEnd212;
        valueX10 = nominalEnd2411;
        valueX11 = nominalEnd2412;
        valueX12 = nominalEnd2413;
    }


    private void manageSaldoBalanceFinPeriodoContable() {
        Log.system(this.getClass().getSimpleName(), "Saldo Balance Fin Periodo Contable.");
        Double fees211 = getFeeAmounts(trades211);
        Double fees212 = getFeeAmounts(trades212);
        Double fees2411 = getFeeAmounts(trades2411);
        Double fees2412 = getFeeAmounts(trades2412);
        Double fees2413= getFeeAmounts(trades2413);

        valueZ6 = averagePrice211 + fees211;
        valueZ7 = averagePrice212 + fees212;
        valueZ10 = averagePrice2411 + fees2411;
        valueZ11 = averagePrice2412 + fees2412;
        valueZ12 = averagePrice2413 + fees2413;
    }


    private void manageRevalorizacion() {
        Log.system(this.getClass().getSimpleName(), "Revalorizacion.");
        valueAB6 = nominalEnd211;
        valueAB7 = nominalEnd212;
        valueAB10 = nominalEnd2411;
        valueAB11 = nominalEnd2412;
        valueAB12 = nominalEnd2413;
    }


    private Double getFeeAmounts(TradeArray tradeArray) {
        Double feeAmount = 0.0;
        for (Trade trade : tradeArray.asList()) {
            Product product = trade.getProduct();
            if(product instanceof Equity){
                Equity equity = (Equity) product;
                feeAmount += getAllFeesAmount(trade);
            }
        }
        return feeAmount;
    }


    private void getImporteLiquidacion() {
        Log.system(this.getClass().getSimpleName(), "ImporteLiquidacion 211: " + trades211.size());
        valueN6 = getImporteLiquidacionTrades(trades211);

        Log.system(this.getClass().getSimpleName(), "ImporteLiquidacion 212: " + trades212.size());
        valueN7 = getImporteLiquidacionTrades(trades212);

        Log.system(this.getClass().getSimpleName(), "ImporteLiquidacion 2411: " + trades2411.size());
        valueN10 = getImporteLiquidacionTrades(trades2411);

        Log.system(this.getClass().getSimpleName(), "ImporteLiquidacion 2412: " + trades2412.size());
        valueN11 = getImporteLiquidacionTrades(trades2412);

        Log.system(this.getClass().getSimpleName(), "ImporteLiquidacion 2413: " + trades2413.size());
        valueN12 = getImporteLiquidacionTrades(trades2413);
    }


    private String getLegalEntityAtribute(LegalEntity entity, String keyword) {
        LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(dsCon, 0, entity.getId(),"ALL", keyword);
        if (attr != null)
            return attr.getAttributeValue();
        return "";
    }


    protected ReportOutput generateReportOutput(String type, String templateName, JDatetime valDatetime) throws RemoteException {
        Report reportToFormat = createReport(type, templateName, pricingEnv, valDatetime);
        if (reportToFormat == null) {
            Log.info(this.getClass().getSimpleName(), "Invalid report type: " + type + " or no trades to process.");
            return null;
        } else if (reportToFormat.getReportTemplate() == null) {
            Log.error(this.getClass().getSimpleName(), "Invalid report template: " + type);
            return null;
        } else {
            Vector<String> holidays = new Vector<String>();
            holidays.add("SYSTEM");
            reportToFormat.getReportTemplate().setHolidays(holidays);
            if (TimeZone.getDefault() != null) {
                reportToFormat.getReportTemplate().setTimeZone(TimeZone.getDefault());
            }
            Vector<String> errorMsgs = new Vector<String>();
            return reportToFormat.load(errorMsgs);
        }
    }
    
    
    @SuppressWarnings("deprecation")
    protected Report createReport(String type, String templateName, PricingEnv env, JDatetime valDatetime) throws RemoteException {
        Report report;
        try {
            String template = "tk.report." + type + "Report";
            report = (Report) InstantiateUtil.getInstance(template, true);
            report.setPricingEnv(env);
            report.setValuationDatetime(valDatetime);
        } catch (Exception e) {
            Log.error(this.getClass().getSimpleName(), "Error getting the instance of report", e);
            report = null;
        }
        if (report != null && !Util.isEmpty(templateName)) {
            ReportTemplate template1 = dsCon.getRemoteReferenceData().getReportTemplate(ReportTemplate.getReportName(type), templateName);
            if (template1 == null) {
                Log.error(this.getClass().getSimpleName(), "Template " + templateName + " Not Found for " + type + " Report");
            } else {
                report.setReportTemplate(template1);
                template1.setValDate(new JDatetime().getJDate());
                template1.callBeforeLoad();
            }
        }
        return report;
    }
    
    
    private Double getAllFeesAmount(Trade trade){
        Double sumFeeAmount = 0.0;
        Vector<Fee> fees= Optional.ofNullable(trade).map(Trade::getFeesList).orElse(new Vector<>());
        if (!Util.isEmpty(fees)) {
            for (Fee fee : fees) {
                sumFeeAmount += convertEUR(fee.getCurrency() ,fee.getAmount(), fee.getFeeDate());
            }
        }
        return sumFeeAmount;
    }
    
    
    public String formatAmount(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("###0.000");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        if (value != null) {
            return myFormatter.format(value);
        } else {
            return "";
        }
    }
    

    protected Double convertEUR(String ccy, Double amount, JDate jDate) {
        Double amountEur = 1.0d;
        if (!"EUR".equalsIgnoreCase(ccy)) {
            QuoteValue quote = null;
            try {
                quote = pricingEnv.getQuoteSet().getFXQuote("EUR", ccy, jDate);
                if ((quote != null) && !Double.isNaN(quote.getClose())) {
                    amount = amount / quote.getClose();
                } else {
                    quote = pricingEnv.getQuoteSet().getFXQuote(ccy, "EUR", jDate);
                    if ((quote != null) && !Double.isNaN(quote.getClose())) {
                        amountEur = amount * quote.getClose();
                    } else {
                        Log.error(this.getClass().getSimpleName(), "There is no quote on " + jDate + " for EUR/" + ccy);
                    }
                }
            }
            catch(MarketDataException e){
                    Log.error(this.getClass().getSimpleName(), "Could not get quote.");
            }
        }
        return amountEur;
    }
    

    public XSSFWorkbook getBook(){
        
        XSSFWorkbook book = new XSSFWorkbook();
        XSSFSheet sheet = book.createSheet();
        XSSFRow row1 = sheet.createRow(0);
        XSSFRow row2 = sheet.createRow(1);
        XSSFRow row3 = sheet.createRow(2);
        XSSFRow row4 = sheet.createRow(3);
        XSSFRow row5 = sheet.createRow(4);
        XSSFRow row6 = sheet.createRow(5);
        XSSFRow row7 = sheet.createRow(6);
        XSSFRow row8 = sheet.createRow(7);
        XSSFRow row9 = sheet.createRow(8);
        XSSFRow row10 = sheet.createRow(9);
        XSSFRow row11 = sheet.createRow(10);
        XSSFRow row12 = sheet.createRow(11);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell1B = row1.createCell((short) 1);
        XSSFRichTextString content1B = new XSSFRichTextString("DATOS SOLICITADOS");
        cell1B.setCellValue(content1B);

        XSSFCell cell1E = row1.createCell((short) 4);
        XSSFRichTextString content1E = new XSSFRichTextString("Altas");
        cell1E.setCellValue(content1E);

        XSSFCell cell1I = row1.createCell((short) 8);
        XSSFRichTextString content1I = new XSSFRichTextString("Bajas");
        cell1I.setCellValue(content1I);

        XSSFCell cell1Q = row1.createCell((short) 16);
        XSSFRichTextString content1Q = new XSSFRichTextString("Ajustes Negativos y Saneamientos");
        cell1Q.setCellValue(content1Q);

        XSSFCell cell1S = row1.createCell((short) 18);
        XSSFRichTextString content1S = new XSSFRichTextString("Ajustes por tipo de cambio");
        cell1S.setCellValue(content1S);

        XSSFCell cell1W = row1.createCell((short) 22);
        XSSFRichTextString content1W = new XSSFRichTextString("Saldo en Balance a fin de periodo");
        cell1W.setCellValue(content1W);

        XSSFCell cell1AC = row1.createCell((short) 28);
        XSSFRichTextString content1AC = new XSSFRichTextString("PRO-MEMORIA");
        cell1AC.setCellValue(content1AC);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell2E = row3.createCell((short) 4);
        XSSFRichTextString content2E = new XSSFRichTextString("Valor Nominal");
        cell2E.setCellValue(content2E);

        XSSFCell cell2G = row3.createCell((short) 6);
        XSSFRichTextString content2G = new XSSFRichTextString("Valor Contable");
        cell2G.setCellValue(content2G);

        XSSFCell cell2I = row3.createCell((short) 8);
        XSSFRichTextString content2I = new XSSFRichTextString("Valor Nominal");
        cell2I.setCellValue(content2I);

        XSSFCell cell2K = row3.createCell((short) 10);
        XSSFRichTextString content2K = new XSSFRichTextString("Valor Contable");
        cell2K.setCellValue(content2K);

        XSSFCell cell2M = row3.createCell((short) 12);
        XSSFRichTextString content2M = new XSSFRichTextString("Importe Liquidacion");
        cell2M.setCellValue(content2M);

        XSSFCell cell2O = row3.createCell((short) 14);
        XSSFRichTextString content2O = new XSSFRichTextString("Revalorizaciones");
        cell2O.setCellValue(content2O);

        XSSFCell cell2S = row3.createCell((short) 18);
        XSSFRichTextString content2S = new XSSFRichTextString("Valor Nominal");
        cell2S.setCellValue(content2S);

        XSSFCell cell2U = row3.createCell((short) 20);
        XSSFRichTextString content2U = new XSSFRichTextString("Valor Contable");
        cell2U.setCellValue(content2U);

        XSSFCell cell2W = row3.createCell((short) 22);
        XSSFRichTextString content2W = new XSSFRichTextString("Valor Nominal");
        cell2W.setCellValue(content2W);

        XSSFCell cell2Y = row3.createCell((short) 24);
        XSSFRichTextString content2Y = new XSSFRichTextString("Valor Contable");
        cell2Y.setCellValue(content2Y);

        XSSFCell cell2AA = row3.createCell((short) 26);
        XSSFRichTextString content2AA = new XSSFRichTextString("Valor Realizacion");
        cell2AA.setCellValue(content2AA);

        XSSFCell cell2AC = row3.createCell((short) 28);
        XSSFRichTextString content2AC = new XSSFRichTextString("VALORES PRESTADOS");
        cell2AC.setCellValue(content2AC);

        XSSFCell cell2AE = row3.createCell((short) 30);
        XSSFRichTextString content2AE = new XSSFRichTextString("VALORES RECIBIDOS");
        cell2AE.setCellValue(content2AE);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell4A = row4.createCell((short) 0);
        XSSFRichTextString content4A = new XSSFRichTextString("2. Instrumentos de patrimonio");
        cell4A.setCellValue(content4A);

        XSSFCell cell4E = row4.createCell((short) 4);
        XSSFRichTextString content4E = new XSSFRichTextString("0060");
        cell4E.setCellValue(content4E);

        XSSFCell cell4F = row4.createCell((short) 5);
        XSSFRichTextString content4F = new XSSFRichTextString(formatAmount(valueF4));
        cell4F.setCellValue(content4F);

        XSSFCell cell4G = row4.createCell((short) 6);
        XSSFRichTextString content4G = new XSSFRichTextString("0160");
        cell4G.setCellValue(content4G);

        XSSFCell cell4H = row4.createCell((short) 7);
        XSSFRichTextString content4H = new XSSFRichTextString(formatAmount(valueH4));
        cell4H.setCellValue(content4H);

        XSSFCell cell4I = row4.createCell((short) 8);
        XSSFRichTextString content4I = new XSSFRichTextString("0260");
        cell4I.setCellValue(content4I);

        XSSFCell cell4J = row4.createCell((short) 9);
        XSSFRichTextString content4J = new XSSFRichTextString(formatAmount(valueJ4));
        cell4J.setCellValue(content4J);

        XSSFCell cell4K = row4.createCell((short) 10);
        XSSFRichTextString content4K = new XSSFRichTextString("0360");
        cell4K.setCellValue(content4K);

        XSSFCell cell4L = row4.createCell((short) 11);
        XSSFRichTextString content4L = new XSSFRichTextString(formatAmount(valueL4));
        cell4L.setCellValue(content4L);

        XSSFCell cell4M = row4.createCell((short) 12);
        XSSFRichTextString content4M = new XSSFRichTextString("0460");
        cell4M.setCellValue(content4M);

        XSSFCell cell4N = row4.createCell((short) 13);
        XSSFRichTextString content4N = new XSSFRichTextString(formatAmount(valueN4));
        cell4N.setCellValue(content4N);

        XSSFCell cell4O = row4.createCell((short) 14);
        XSSFRichTextString content4O = new XSSFRichTextString("0560");
        cell4O.setCellValue(content4O);

        XSSFCell cell4P = row4.createCell((short) 15);
        XSSFRichTextString content4P = new XSSFRichTextString(formatAmount(valueP4));
        cell4P.setCellValue(content4P);

        XSSFCell cell4Q = row4.createCell((short) 16);
        XSSFRichTextString content4Q = new XSSFRichTextString("0660");
        cell4Q.setCellValue(content4Q);

        XSSFCell cell4R = row4.createCell((short) 17);
        XSSFRichTextString content4R = new XSSFRichTextString(formatAmount(valueR4));
        cell4R.setCellValue(content4R);

        XSSFCell cell4S = row4.createCell((short) 18);
        XSSFRichTextString content4S = new XSSFRichTextString("0760");
        cell4S.setCellValue(content4S);

        XSSFCell cell4T = row4.createCell((short) 19);
        XSSFRichTextString content4T = new XSSFRichTextString(formatAmount(valueT4));
        cell4T.setCellValue(content4T);

        XSSFCell cell4U = row4.createCell((short) 20);
        XSSFRichTextString content4U = new XSSFRichTextString("0860");
        cell4U.setCellValue(content4U);

        XSSFCell cell4V= row4.createCell((short) 21);
        XSSFRichTextString content4V= new XSSFRichTextString(formatAmount(valueV4));
        cell4V.setCellValue(content4V);

        XSSFCell cell4W= row4.createCell((short) 22);
        XSSFRichTextString content4W= new XSSFRichTextString("0960");
        cell4W.setCellValue(content4W);

        XSSFCell cell4X = row4.createCell((short) 23);
        XSSFRichTextString content4X = new XSSFRichTextString(formatAmount(valueX4));
        cell4X.setCellValue(content4X);

        XSSFCell cell4Y = row4.createCell((short) 24);
        XSSFRichTextString content4Y = new XSSFRichTextString("1060");
        cell4Y.setCellValue(content4Y);

        XSSFCell cell4Z = row4.createCell((short) 25);
        XSSFRichTextString content4Z = new XSSFRichTextString(formatAmount(valueZ4));
        cell4Z.setCellValue(content4Z);

        XSSFCell cell4AA = row4.createCell((short) 26);
        XSSFRichTextString content4AA = new XSSFRichTextString("1160");
        cell4AA.setCellValue(content4AA);

        XSSFCell cell4AB = row4.createCell((short) 27);
        XSSFRichTextString content4AB = new XSSFRichTextString(formatAmount(valueAB4));
        cell4AB.setCellValue(content4AB);

        XSSFCell cell4AC = row4.createCell((short) 28);
        XSSFRichTextString content4AC = new XSSFRichTextString("1260");
        cell4AC.setCellValue(content4AC);

        XSSFCell cell4AD = row4.createCell((short) 29);
        XSSFRichTextString content4AD = new XSSFRichTextString(ZERO_VALUE);
        cell4AD.setCellValue(content4AD);

        XSSFCell cell4AE = row4.createCell((short) 30);
        XSSFRichTextString content4AE = new XSSFRichTextString("1360");
        cell4AE.setCellValue(content4AE);

        XSSFCell cell4AF = row4.createCell((short) 31);
        XSSFRichTextString content4AF = new XSSFRichTextString(ZERO_VALUE);
        cell4AF.setCellValue(content4AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell5A = row5.createCell((short) 0);
        XSSFRichTextString content5A = new XSSFRichTextString("  2.1. Participaciones en el grupo");
        cell5A.setCellValue(content5A);

        XSSFCell cell5E = row5.createCell((short) 4);
        XSSFRichTextString content5E = new XSSFRichTextString("0061");
        cell5E.setCellValue(content5E);

        XSSFCell cell5F = row5.createCell((short) 5);
        XSSFRichTextString content5F = new XSSFRichTextString(formatAmount(valueF5));
        cell5F.setCellValue(content5F);

        XSSFCell cell5G = row5.createCell((short) 6);
        XSSFRichTextString content5G = new XSSFRichTextString("0161");
        cell5G.setCellValue(content5G);

        XSSFCell cell5H = row5.createCell((short) 7);
        XSSFRichTextString content5H = new XSSFRichTextString(formatAmount(valueH5));
        cell5H.setCellValue(content5H);

        XSSFCell cell5I = row5.createCell((short) 8);
        XSSFRichTextString content5I = new XSSFRichTextString("0261");
        cell5I.setCellValue(content5I);

        XSSFCell cell5J = row5.createCell((short) 9);
        XSSFRichTextString content5J = new XSSFRichTextString(formatAmount(valueJ5));
        cell5J.setCellValue(content5J);

        XSSFCell cell5K = row5.createCell((short) 10);
        XSSFRichTextString content5K = new XSSFRichTextString("0361");
        cell5K.setCellValue(content5K);

        XSSFCell cell5L = row5.createCell((short) 11);
        XSSFRichTextString content5L = new XSSFRichTextString(formatAmount(valueL5));
        cell5L.setCellValue(content5L);

        XSSFCell cell5M = row5.createCell((short) 12);
        XSSFRichTextString content5M = new XSSFRichTextString("0461");
        cell5M.setCellValue(content5M);

        XSSFCell cell5N = row5.createCell((short) 13);
        XSSFRichTextString content5N = new XSSFRichTextString(formatAmount(valueN5));
        cell5N.setCellValue(content5N);

        XSSFCell cell5O = row5.createCell((short) 14);
        XSSFRichTextString content5O = new XSSFRichTextString("0561");
        cell5O.setCellValue(content5O);

        XSSFCell cell5P = row5.createCell((short) 15);
        XSSFRichTextString content5P = new XSSFRichTextString(formatAmount(valueP5));
        cell5P.setCellValue(content5P);

        XSSFCell cell5Q = row5.createCell((short) 16);
        XSSFRichTextString content5Q = new XSSFRichTextString("0661");
        cell5Q.setCellValue(content5Q);

        XSSFCell cell5R = row5.createCell((short) 17);
        XSSFRichTextString content5R = new XSSFRichTextString(formatAmount(valueR5));
        cell5R.setCellValue(content5R);

        XSSFCell cell5S = row5.createCell((short) 18);
        XSSFRichTextString content5S = new XSSFRichTextString("0761");
        cell5S.setCellValue(content5S);

        XSSFCell cell5T = row5.createCell((short) 19);
        XSSFRichTextString content5T = new XSSFRichTextString(formatAmount(valueT5));
        cell5T.setCellValue(content5T);

        XSSFCell cell5U = row5.createCell((short) 20);
        XSSFRichTextString content5U = new XSSFRichTextString("0861");
        cell5U.setCellValue(content5U);

        XSSFCell cell5V= row5.createCell((short) 21);
        XSSFRichTextString content5V= new XSSFRichTextString(formatAmount(valueV5));
        cell5V.setCellValue(content5V);

        XSSFCell cell5W= row5.createCell((short) 22);
        XSSFRichTextString content5W= new XSSFRichTextString("0961");
        cell5W.setCellValue(content5W);

        XSSFCell cell5X = row5.createCell((short) 23);
        XSSFRichTextString content5X = new XSSFRichTextString(formatAmount(valueX5));
        cell5X.setCellValue(content5X);

        XSSFCell cell5Y = row5.createCell((short) 24);
        XSSFRichTextString content5Y = new XSSFRichTextString("1061");
        cell5Y.setCellValue(content5Y);

        XSSFCell cell5Z = row5.createCell((short) 25);
        XSSFRichTextString content5Z = new XSSFRichTextString(formatAmount(valueZ5));
        cell5Z.setCellValue(content5Z);

        XSSFCell cell5AA = row5.createCell((short) 26);
        XSSFRichTextString content5AA = new XSSFRichTextString("1161");
        cell5AA.setCellValue(content5AA);

        XSSFCell cell5AB = row5.createCell((short) 27);
        XSSFRichTextString content5AB = new XSSFRichTextString(formatAmount(valueAB5));
        cell5AB.setCellValue(content5AB);

        XSSFCell cell5AC = row5.createCell((short) 28);
        XSSFRichTextString content5AC = new XSSFRichTextString("1261");
        cell5AC.setCellValue(content5AC);

        XSSFCell cell5AD = row5.createCell((short) 29);
        XSSFRichTextString content5AD = new XSSFRichTextString(ZERO_VALUE);
        cell5AD.setCellValue(content5AD);

        XSSFCell cell5AE = row5.createCell((short) 30);
        XSSFRichTextString content5AE = new XSSFRichTextString("1361");
        cell5AE.setCellValue(content5AE);

        XSSFCell cell5AF = row5.createCell((short) 31);
        XSSFRichTextString content5AF = new XSSFRichTextString(ZERO_VALUE);
        cell5AF.setCellValue(content5AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell6A = row6.createCell((short) 0);
        XSSFRichTextString content6A = new XSSFRichTextString("     2.1.1. De Entidades de Credito");
        cell6A.setCellValue(content6A);

        XSSFCell cell6E = row6.createCell((short) 4);
        XSSFRichTextString content6E = new XSSFRichTextString("0062");
        cell6E.setCellValue(content6E);

        XSSFCell cell6F = row6.createCell((short) 5);
        XSSFRichTextString content6F = new XSSFRichTextString(formatAmount(valueF6));
        cell6F.setCellValue(content6F);

        XSSFCell cell6G = row6.createCell((short) 6);
        XSSFRichTextString content6G = new XSSFRichTextString("0162");
        cell6G.setCellValue(content6G);

        XSSFCell cell6H = row6.createCell((short) 7);
        XSSFRichTextString content6H = new XSSFRichTextString(formatAmount(valueH6));
        cell6H.setCellValue(content6H);

        XSSFCell cell6I = row6.createCell((short) 8);
        XSSFRichTextString content6I = new XSSFRichTextString("0262");
        cell6I.setCellValue(content6I);

        XSSFCell cell6J = row6.createCell((short) 9);
        XSSFRichTextString content6J = new XSSFRichTextString(formatAmount(valueJ6));
        cell6J.setCellValue(content6J);

        XSSFCell cell6K = row6.createCell((short) 10);
        XSSFRichTextString content6K = new XSSFRichTextString("0362");
        cell6K.setCellValue(content6K);

        XSSFCell cell6L = row6.createCell((short) 11);
        XSSFRichTextString content6L = new XSSFRichTextString(formatAmount(valueL6));
        cell6L.setCellValue(content6L);

        XSSFCell cell6M = row6.createCell((short) 12);
        XSSFRichTextString content6M = new XSSFRichTextString("0462");
        cell6M.setCellValue(content6M);

        XSSFCell cell6N = row6.createCell((short) 13);
        XSSFRichTextString content6N = new XSSFRichTextString(formatAmount(valueN6));
        cell6N.setCellValue(content6N);

        XSSFCell cell6O = row6.createCell((short) 14);
        XSSFRichTextString content6O = new XSSFRichTextString("0562");
        cell6O.setCellValue(content6O);

        XSSFCell cell6P = row6.createCell((short) 15);
        XSSFRichTextString content6P = new XSSFRichTextString(formatAmount(valueP6));
        cell6P.setCellValue(content6P);

        XSSFCell cell6Q = row6.createCell((short) 16);
        XSSFRichTextString content6Q = new XSSFRichTextString("0662");
        cell6Q.setCellValue(content6Q);

        XSSFCell cell6R = row6.createCell((short) 17);
        XSSFRichTextString content6R = new XSSFRichTextString(formatAmount(valueR6));
        cell6R.setCellValue(content6R);

        XSSFCell cell6S = row6.createCell((short) 18);
        XSSFRichTextString content6S = new XSSFRichTextString("0762");
        cell6S.setCellValue(content6S);

        XSSFCell cell6T = row6.createCell((short) 19);
        XSSFRichTextString content6T = new XSSFRichTextString(formatAmount(valueT6));
        cell6T.setCellValue(content6T);

        XSSFCell cell6U = row6.createCell((short) 20);
        XSSFRichTextString content6U = new XSSFRichTextString("0862");
        cell6U.setCellValue(content6U);

        XSSFCell cell6V= row6.createCell((short) 21);
        XSSFRichTextString content6V= new XSSFRichTextString(formatAmount(valueV6));
        cell6V.setCellValue(content6V);

        XSSFCell cell6W= row6.createCell((short) 22);
        XSSFRichTextString content6W= new XSSFRichTextString("0962");
        cell6W.setCellValue(content6W);

        XSSFCell cell6X = row6.createCell((short) 23);
        XSSFRichTextString content6X = new XSSFRichTextString(formatAmount(valueX6));
        cell6X.setCellValue(content6X);

        XSSFCell cell6Y = row6.createCell((short) 24);
        XSSFRichTextString content6Y = new XSSFRichTextString("1062");
        cell6Y.setCellValue(content6Y);

        XSSFCell cell6Z = row6.createCell((short) 25);
        XSSFRichTextString content6Z = new XSSFRichTextString(formatAmount(valueZ6));
        cell6Z.setCellValue(content6Z);

        XSSFCell cell6AA = row6.createCell((short) 26);
        XSSFRichTextString content6AA = new XSSFRichTextString("1162");
        cell6AA.setCellValue(content6AA);

        XSSFCell cell6AB = row6.createCell((short) 27);
        XSSFRichTextString content6AB = new XSSFRichTextString(formatAmount(valueAB6));
        cell6AB.setCellValue(content6AB);

        XSSFCell cell6AC = row6.createCell((short) 28);
        XSSFRichTextString content6AC = new XSSFRichTextString("1262");
        cell6AC.setCellValue(content6AC);

        XSSFCell cell6AD = row6.createCell((short) 29);
        XSSFRichTextString content6AD = new XSSFRichTextString(ZERO_VALUE);
        cell6AD.setCellValue(content6AD);

        XSSFCell cell6AE = row6.createCell((short) 30);
        XSSFRichTextString content6AE = new XSSFRichTextString("1362");
        cell6AE.setCellValue(content6AE);

        XSSFCell cell6AF = row6.createCell((short) 31);
        XSSFRichTextString content6AF = new XSSFRichTextString(ZERO_VALUE);
        cell6AF.setCellValue(content6AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell7A = row7.createCell((short) 0);
        XSSFRichTextString content7A = new XSSFRichTextString("     2.1.2. Otras");
        cell7A.setCellValue(content7A);

        XSSFCell cell7E = row7.createCell((short) 4);
        XSSFRichTextString content7E = new XSSFRichTextString("0063");
        cell7E.setCellValue(content7E);

        XSSFCell cell7F = row7.createCell((short) 5);
        XSSFRichTextString content7F = new XSSFRichTextString(formatAmount(valueF7));
        cell7F.setCellValue(content7F);

        XSSFCell cell7G = row7.createCell((short) 6);
        XSSFRichTextString content7G = new XSSFRichTextString("0163");
        cell7G.setCellValue(content7G);

        XSSFCell cell7H = row7.createCell((short) 7);
        XSSFRichTextString content7H = new XSSFRichTextString(formatAmount(valueH7));
        cell7H.setCellValue(content7H);

        XSSFCell cell7I = row7.createCell((short) 8);
        XSSFRichTextString content7I = new XSSFRichTextString("0263");
        cell7I.setCellValue(content7I);

        XSSFCell cell7J = row7.createCell((short) 9);
        XSSFRichTextString content7J = new XSSFRichTextString(formatAmount(valueJ7));
        cell7J.setCellValue(content7J);

        XSSFCell cell7K = row7.createCell((short) 10);
        XSSFRichTextString content7K = new XSSFRichTextString("0363");
        cell7K.setCellValue(content7K);

        XSSFCell cell7L = row7.createCell((short) 11);
        XSSFRichTextString content7L = new XSSFRichTextString(formatAmount(valueL7));
        cell7L.setCellValue(content7L);

        XSSFCell cell7M = row7.createCell((short) 12);
        XSSFRichTextString content7M = new XSSFRichTextString("0463");
        cell7M.setCellValue(content7M);

        XSSFCell cell7N = row7.createCell((short) 13);
        XSSFRichTextString content7N = new XSSFRichTextString(formatAmount(valueN7));
        cell7N.setCellValue(content7N);

        XSSFCell cell7O = row7.createCell((short) 14);
        XSSFRichTextString content7O = new XSSFRichTextString("0563");
        cell7O.setCellValue(content7O);

        XSSFCell cell7P = row7.createCell((short) 15);
        XSSFRichTextString content7P = new XSSFRichTextString(formatAmount(valueP7));
        cell7P.setCellValue(content7P);

        XSSFCell cell7Q = row7.createCell((short) 16);
        XSSFRichTextString content7Q = new XSSFRichTextString("0663");
        cell7Q.setCellValue(content7Q);

        XSSFCell cell7R = row7.createCell((short) 17);
        XSSFRichTextString content7R = new XSSFRichTextString(formatAmount(valueR7));
        cell7R.setCellValue(content7R);

        XSSFCell cell7S = row7.createCell((short) 18);
        XSSFRichTextString content7S = new XSSFRichTextString("0763");
        cell7S.setCellValue(content7S);

        XSSFCell cell7T = row7.createCell((short) 19);
        XSSFRichTextString content7T = new XSSFRichTextString(formatAmount(valueT7));
        cell7T.setCellValue(content7T);

        XSSFCell cell7U = row7.createCell((short) 20);
        XSSFRichTextString content7U = new XSSFRichTextString("0863");
        cell7U.setCellValue(content7U);

        XSSFCell cell7V= row7.createCell((short) 21);
        XSSFRichTextString content7V= new XSSFRichTextString(formatAmount(valueV7));
        cell7V.setCellValue(content7V);

        XSSFCell cell7W= row7.createCell((short) 22);
        XSSFRichTextString content7W= new XSSFRichTextString("0963");
        cell7W.setCellValue(content7W);

        XSSFCell cell7X = row7.createCell((short) 23);
        XSSFRichTextString content7X = new XSSFRichTextString(formatAmount(valueX7));
        cell7X.setCellValue(content7X);

        XSSFCell cell7Y = row7.createCell((short) 24);
        XSSFRichTextString content7Y = new XSSFRichTextString("1063");
        cell7Y.setCellValue(content7Y);

        XSSFCell cell7Z = row7.createCell((short) 25);
        XSSFRichTextString content7Z = new XSSFRichTextString(formatAmount(valueZ7));
        cell7Z.setCellValue(content7Z);

        XSSFCell cell7AA = row7.createCell((short) 26);
        XSSFRichTextString content7AA = new XSSFRichTextString("1163");
        cell7AA.setCellValue(content7AA);

        XSSFCell cell7AB = row7.createCell((short) 27);
        XSSFRichTextString content7AB = new XSSFRichTextString(formatAmount(valueAB7));
        cell7AB.setCellValue(content7AB);

        XSSFCell cell7AC = row7.createCell((short) 28);
        XSSFRichTextString content7AC = new XSSFRichTextString("1263");
        cell7AC.setCellValue(content7AC);

        XSSFCell cell7AD = row7.createCell((short) 29);
        XSSFRichTextString content7AD = new XSSFRichTextString(ZERO_VALUE);
        cell7AD.setCellValue(content7AD);

        XSSFCell cell7AE = row7.createCell((short) 30);
        XSSFRichTextString content7AE = new XSSFRichTextString("1363");
        cell7AE.setCellValue(content7AE);

        XSSFCell cell7AF = row7.createCell((short) 31);
        XSSFRichTextString content7AF = new XSSFRichTextString(ZERO_VALUE);
        cell7AF.setCellValue(content7AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell8A = row8.createCell((short) 0);
        XSSFRichTextString content8A = new XSSFRichTextString("  2.4. Otros instrumentos de capital");
        cell8A.setCellValue(content8A);

        XSSFCell cell8E = row8.createCell((short) 4);
        XSSFRichTextString content8E = new XSSFRichTextString("0075");
        cell8E.setCellValue(content8E);

        XSSFCell cell8F = row8.createCell((short) 5);
        XSSFRichTextString content8F = new XSSFRichTextString(formatAmount(valueF8));
        cell8F.setCellValue(content8F);

        XSSFCell cell8G = row8.createCell((short) 6);
        XSSFRichTextString content8G = new XSSFRichTextString("0175");
        cell8G.setCellValue(content8G);

        XSSFCell cell8H = row8.createCell((short) 7);
        XSSFRichTextString content8H = new XSSFRichTextString(formatAmount(valueH8));
        cell8H.setCellValue(content8H);

        XSSFCell cell8I = row8.createCell((short) 8);
        XSSFRichTextString content8I = new XSSFRichTextString("0275");
        cell8I.setCellValue(content8I);

        XSSFCell cell8J = row8.createCell((short) 9);
        XSSFRichTextString content8J = new XSSFRichTextString(formatAmount(valueJ8));
        cell8J.setCellValue(content8J);

        XSSFCell cell8K = row8.createCell((short) 10);
        XSSFRichTextString content8K = new XSSFRichTextString("0375");
        cell8K.setCellValue(content8K);

        XSSFCell cell8L = row8.createCell((short) 11);
        XSSFRichTextString content8L = new XSSFRichTextString(formatAmount(valueL8));
        cell8L.setCellValue(content8L);

        XSSFCell cell8M = row8.createCell((short) 12);
        XSSFRichTextString content8M = new XSSFRichTextString("0475");
        cell8M.setCellValue(content8M);

        XSSFCell cell8N = row8.createCell((short) 13);
        XSSFRichTextString content8N = new XSSFRichTextString(formatAmount(valueN8));
        cell8N.setCellValue(content8N);

        XSSFCell cell8O = row8.createCell((short) 14);
        XSSFRichTextString content8O = new XSSFRichTextString("0575");
        cell8O.setCellValue(content8O);

        XSSFCell cell8P = row8.createCell((short) 15);
        XSSFRichTextString content8P = new XSSFRichTextString(formatAmount(valueP8));
        cell8P.setCellValue(content8P);

        XSSFCell cell8Q = row8.createCell((short) 16);
        XSSFRichTextString content8Q = new XSSFRichTextString("0675");
        cell8Q.setCellValue(content8Q);

        XSSFCell cell8R = row8.createCell((short) 17);
        XSSFRichTextString content8R = new XSSFRichTextString(formatAmount(valueR8));
        cell8R.setCellValue(content8R);

        XSSFCell cell8S = row8.createCell((short) 18);
        XSSFRichTextString content8S = new XSSFRichTextString("0775");
        cell8S.setCellValue(content8S);

        XSSFCell cell8T = row8.createCell((short) 19);
        XSSFRichTextString content8T = new XSSFRichTextString(formatAmount(valueT8));
        cell8T.setCellValue(content8T);

        XSSFCell cell8U = row8.createCell((short) 20);
        XSSFRichTextString content8U = new XSSFRichTextString("0875");
        cell8U.setCellValue(content8U);

        XSSFCell cell8V= row8.createCell((short) 21);
        XSSFRichTextString content8V= new XSSFRichTextString(formatAmount(valueV8));
        cell8V.setCellValue(content8V);

        XSSFCell cell8W= row8.createCell((short) 22);
        XSSFRichTextString content8W= new XSSFRichTextString("0975");
        cell8W.setCellValue(content8W);

        XSSFCell cell8X = row8.createCell((short) 23);
        XSSFRichTextString content8X = new XSSFRichTextString(formatAmount(valueX8));
        cell8X.setCellValue(content8X);

        XSSFCell cell8Y = row8.createCell((short) 24);
        XSSFRichTextString content8Y = new XSSFRichTextString("1075");
        cell8Y.setCellValue(content8Y);

        XSSFCell cell8Z = row8.createCell((short) 25);
        XSSFRichTextString content8Z = new XSSFRichTextString(formatAmount(valueZ8));
        cell8Z.setCellValue(content8Z);

        XSSFCell cell8AA = row8.createCell((short) 26);
        XSSFRichTextString content8AA = new XSSFRichTextString("1175");
        cell8AA.setCellValue(content8AA);

        XSSFCell cell8AB = row8.createCell((short) 27);
        XSSFRichTextString content8AB = new XSSFRichTextString(formatAmount(valueAB8));
        cell8AB.setCellValue(content8AB);

        XSSFCell cell8AC = row8.createCell((short) 28);
        XSSFRichTextString content8AC = new XSSFRichTextString("1275");
        cell8AC.setCellValue(content8AC);

        XSSFCell cell8AD = row8.createCell((short) 29);
        XSSFRichTextString content8AD = new XSSFRichTextString(ZERO_VALUE);
        cell8AD.setCellValue(content8AD);

        XSSFCell cell8AE = row8.createCell((short) 30);
        XSSFRichTextString content8AE = new XSSFRichTextString("1375");
        cell8AE.setCellValue(content8AE);

        XSSFCell cell8AF = row8.createCell((short) 31);
        XSSFRichTextString content8AF = new XSSFRichTextString(ZERO_VALUE);
        cell8AF.setCellValue(content8AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell9A = row9.createCell((short) 0);
        XSSFRichTextString content9A = new XSSFRichTextString("            - Cotizadas");
        cell9A.setCellValue(content9A);

        XSSFCell cell9E = row9.createCell((short) 4);
        XSSFRichTextString content9E = new XSSFRichTextString("0076");
        cell9E.setCellValue(content9E);

        XSSFCell cell9F = row9.createCell((short) 5);
        XSSFRichTextString content9F = new XSSFRichTextString(formatAmount(valueF9));
        cell9F.setCellValue(content9F);

        XSSFCell cell9G = row9.createCell((short) 6);
        XSSFRichTextString content9G = new XSSFRichTextString("0176");
        cell9G.setCellValue(content9G);

        XSSFCell cell9H = row9.createCell((short) 7);
        XSSFRichTextString content9H = new XSSFRichTextString(formatAmount(valueH9));
        cell9H.setCellValue(content9H);

        XSSFCell cell9I = row9.createCell((short) 8);
        XSSFRichTextString content9I = new XSSFRichTextString("0276");
        cell9I.setCellValue(content9I);

        XSSFCell cell9J = row9.createCell((short) 9);
        XSSFRichTextString content9J = new XSSFRichTextString(formatAmount(valueJ9));
        cell9J.setCellValue(content9J);

        XSSFCell cell9K = row9.createCell((short) 10);
        XSSFRichTextString content9K = new XSSFRichTextString("0376");
        cell9K.setCellValue(content9K);

        XSSFCell cell9L = row9.createCell((short) 11);
        XSSFRichTextString content9L = new XSSFRichTextString(formatAmount(valueL9));
        cell9L.setCellValue(content9L);

        XSSFCell cell9M = row9.createCell((short) 12);
        XSSFRichTextString content9M = new XSSFRichTextString("0476");
        cell9M.setCellValue(content9M);

        XSSFCell cell9N = row9.createCell((short) 13);
        XSSFRichTextString content9N = new XSSFRichTextString(formatAmount(valueN9));
        cell9N.setCellValue(content9N);

        XSSFCell cell9O = row9.createCell((short) 14);
        XSSFRichTextString content9O = new XSSFRichTextString("0576");
        cell9O.setCellValue(content9O);

        XSSFCell cell9P = row9.createCell((short) 15);
        XSSFRichTextString content9P = new XSSFRichTextString(formatAmount(valueP9));
        cell9P.setCellValue(content9P);

        XSSFCell cell9Q = row9.createCell((short) 16);
        XSSFRichTextString content9Q = new XSSFRichTextString("0676");
        cell9Q.setCellValue(content9Q);

        XSSFCell cell9R = row9.createCell((short) 17);
        XSSFRichTextString content9R = new XSSFRichTextString(formatAmount(valueR9));
        cell9R.setCellValue(content9R);

        XSSFCell cell9S = row9.createCell((short) 18);
        XSSFRichTextString content9S = new XSSFRichTextString("0776");
        cell9S.setCellValue(content9S);

        XSSFCell cell9T = row9.createCell((short) 19);
        XSSFRichTextString content9T = new XSSFRichTextString(formatAmount(valueT9));
        cell9T.setCellValue(content9T);

        XSSFCell cell9U = row9.createCell((short) 20);
        XSSFRichTextString content9U = new XSSFRichTextString("0876");
        cell9U.setCellValue(content9U);

        XSSFCell cell9V= row9.createCell((short) 21);
        XSSFRichTextString content9V= new XSSFRichTextString(formatAmount(valueV9));
        cell9V.setCellValue(content9V);

        XSSFCell cell9W= row9.createCell((short) 22);
        XSSFRichTextString content9W= new XSSFRichTextString("0976");
        cell9W.setCellValue(content9W);

        XSSFCell cell9X = row9.createCell((short) 23);
        XSSFRichTextString content9X = new XSSFRichTextString(formatAmount(valueX9));
        cell9X.setCellValue(content9X);

        XSSFCell cell9Y = row9.createCell((short) 24);
        XSSFRichTextString content9Y = new XSSFRichTextString("1076");
        cell9Y.setCellValue(content9Y);

        XSSFCell cell9Z = row9.createCell((short) 25);
        XSSFRichTextString content9Z = new XSSFRichTextString(formatAmount(valueZ9));
        cell9Z.setCellValue(content9Z);

        XSSFCell cell9AA = row9.createCell((short) 26);
        XSSFRichTextString content9AA = new XSSFRichTextString("1176");
        cell9AA.setCellValue(content9AA);

        XSSFCell cell9AB = row9.createCell((short) 27);
        XSSFRichTextString content9AB = new XSSFRichTextString(formatAmount(valueAB9));
        cell9AB.setCellValue(content9AB);

        XSSFCell cell9AC = row9.createCell((short) 28);
        XSSFRichTextString content9AC = new XSSFRichTextString("1276");
        cell9AC.setCellValue(content9AC);

        XSSFCell cell9AD = row9.createCell((short) 29);
        XSSFRichTextString content9AD = new XSSFRichTextString(ZERO_VALUE);
        cell9AD.setCellValue(content9AD);

        XSSFCell cell9AE = row9.createCell((short) 30);
        XSSFRichTextString content9AE = new XSSFRichTextString("1376");
        cell9AE.setCellValue(content9AE);

        XSSFCell cell9AF = row9.createCell((short) 31);
        XSSFRichTextString content9AF = new XSSFRichTextString(ZERO_VALUE);
        cell9AF.setCellValue(content9AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell10A = row10.createCell((short) 0);
        XSSFRichTextString content10A = new XSSFRichTextString("     2.4.1.1. De Entidades de Credito");
        cell10A.setCellValue(content10A);

        XSSFCell cell10E = row10.createCell((short) 4);
        XSSFRichTextString content10E = new XSSFRichTextString("0077");
        cell10E.setCellValue(content10E);

        XSSFCell cell10F = row10.createCell((short) 5);
        XSSFRichTextString content10F = new XSSFRichTextString(formatAmount(valueF10));
        cell10F.setCellValue(content10F);

        XSSFCell cell10G = row10.createCell((short) 6);
        XSSFRichTextString content10G = new XSSFRichTextString("0177");
        cell10G.setCellValue(content10G);

        XSSFCell cell10H = row10.createCell((short) 7);
        XSSFRichTextString content10H = new XSSFRichTextString(formatAmount(valueH10));
        cell10H.setCellValue(content10H);

        XSSFCell cell10I = row10.createCell((short) 8);
        XSSFRichTextString content10I = new XSSFRichTextString("0277");
        cell10I.setCellValue(content10I);

        XSSFCell cell10J = row10.createCell((short) 9);
        XSSFRichTextString content10J = new XSSFRichTextString(formatAmount(valueJ10));
        cell10J.setCellValue(content10J);

        XSSFCell cell10K = row10.createCell((short) 10);
        XSSFRichTextString content10K = new XSSFRichTextString("0377");
        cell10K.setCellValue(content10K);

        XSSFCell cell10L = row10.createCell((short) 11);
        XSSFRichTextString content10L = new XSSFRichTextString(formatAmount(valueL10));
        cell10L.setCellValue(content10L);

        XSSFCell cell10M = row10.createCell((short) 12);
        XSSFRichTextString content10M = new XSSFRichTextString("0477");
        cell10M.setCellValue(content10M);

        XSSFCell cell10N = row10.createCell((short) 13);
        XSSFRichTextString content10N = new XSSFRichTextString(formatAmount(valueN10));
        cell10N.setCellValue(content10N);

        XSSFCell cell10O = row10.createCell((short) 14);
        XSSFRichTextString content10O = new XSSFRichTextString("0577");
        cell10O.setCellValue(content10O);

        XSSFCell cell10P = row10.createCell((short) 15);
        XSSFRichTextString content10P = new XSSFRichTextString(formatAmount(valueP10));
        cell10P.setCellValue(content10P);

        XSSFCell cell10Q = row10.createCell((short) 16);
        XSSFRichTextString content10Q = new XSSFRichTextString("0677");
        cell10Q.setCellValue(content10Q);

        XSSFCell cell10R = row10.createCell((short) 17);
        XSSFRichTextString content10R = new XSSFRichTextString(formatAmount(valueR10));
        cell10R.setCellValue(content10R);

        XSSFCell cell10S = row10.createCell((short) 18);
        XSSFRichTextString content10S = new XSSFRichTextString("0777");
        cell10S.setCellValue(content10S);

        XSSFCell cell10T = row10.createCell((short) 19);
        XSSFRichTextString content10T = new XSSFRichTextString(formatAmount(valueT10));
        cell10T.setCellValue(content10T);

        XSSFCell cell10U = row10.createCell((short) 20);
        XSSFRichTextString content10U = new XSSFRichTextString("0877");
        cell10U.setCellValue(content10U);

        XSSFCell cell10V= row10.createCell((short) 21);
        XSSFRichTextString content10V= new XSSFRichTextString(formatAmount(valueV10));
        cell10V.setCellValue(content10V);

        XSSFCell cell10W= row10.createCell((short) 22);
        XSSFRichTextString content10W= new XSSFRichTextString("0977");
        cell10W.setCellValue(content10W);

        XSSFCell cell10X = row10.createCell((short) 23);
        XSSFRichTextString content10X = new XSSFRichTextString(formatAmount(valueX10));
        cell10X.setCellValue(content10X);

        XSSFCell cell10Y = row10.createCell((short) 24);
        XSSFRichTextString content10Y = new XSSFRichTextString("1077");
        cell10Y.setCellValue(content10Y);

        XSSFCell cell10Z = row10.createCell((short) 25);
        XSSFRichTextString content10Z = new XSSFRichTextString(formatAmount(valueZ10));
        cell10Z.setCellValue(content10Z);

        XSSFCell cell10AA = row10.createCell((short) 26);
        XSSFRichTextString content10AA = new XSSFRichTextString("1177");
        cell10AA.setCellValue(content10AA);

        XSSFCell cell10AB = row10.createCell((short) 27);
        XSSFRichTextString content10AB = new XSSFRichTextString(formatAmount(valueAB10));
        cell10AB.setCellValue(content10AB);

        XSSFCell cell10AC = row10.createCell((short) 28);
        XSSFRichTextString content10AC = new XSSFRichTextString("1277");
        cell10AC.setCellValue(content10AC);

        XSSFCell cell10AD = row10.createCell((short) 29);
        XSSFRichTextString content10AD = new XSSFRichTextString(ZERO_VALUE);
        cell10AD.setCellValue(content10AD);

        XSSFCell cell10AE = row10.createCell((short) 30);
        XSSFRichTextString content10AE = new XSSFRichTextString("1377");
        cell10AE.setCellValue(content10AE);

        XSSFCell cell10AF = row10.createCell((short) 31);
        XSSFRichTextString content10AF = new XSSFRichTextString(ZERO_VALUE);
        cell10AF.setCellValue(content10AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell11A = row11.createCell((short) 0);
        XSSFRichTextString content11A = new XSSFRichTextString("     2.4.1.2. De Otros Sectores Residentes");
        cell11A.setCellValue(content11A);

        XSSFCell cell11E = row11.createCell((short) 4);
        XSSFRichTextString content11E = new XSSFRichTextString("0078");
        cell11E.setCellValue(content11E);

        XSSFCell cell11F = row11.createCell((short) 5);
        XSSFRichTextString content11F = new XSSFRichTextString(formatAmount(valueF11));
        cell11F.setCellValue(content11F);

        XSSFCell cell11G = row11.createCell((short) 6);
        XSSFRichTextString content11G = new XSSFRichTextString("0178");
        cell11G.setCellValue(content11G);

        XSSFCell cell11H = row11.createCell((short) 7);
        XSSFRichTextString content11H = new XSSFRichTextString(formatAmount(valueH11));
        cell11H.setCellValue(content11H);

        XSSFCell cell11I = row11.createCell((short) 8);
        XSSFRichTextString content11I = new XSSFRichTextString("0278");
        cell11I.setCellValue(content11I);

        XSSFCell cell11J = row11.createCell((short) 9);
        XSSFRichTextString content11J = new XSSFRichTextString(formatAmount(valueJ11));
        cell11J.setCellValue(content11J);

        XSSFCell cell11K = row11.createCell((short) 10);
        XSSFRichTextString content11K = new XSSFRichTextString("0378");
        cell11K.setCellValue(content11K);
    
        XSSFCell cell11L = row11.createCell((short) 11);
        XSSFRichTextString content11L = new XSSFRichTextString(formatAmount(valueL11));
        cell11L.setCellValue(content11L);
    
        XSSFCell cell11M = row11.createCell((short) 12);
        XSSFRichTextString content11M = new XSSFRichTextString("0478");
        cell11M.setCellValue(content11M);
    
        XSSFCell cell11N = row11.createCell((short) 13);
        XSSFRichTextString content11N = new XSSFRichTextString(formatAmount(valueN11));
        cell11N.setCellValue(content11N);

        XSSFCell cell11O = row11.createCell((short) 14);
        XSSFRichTextString content11O = new XSSFRichTextString("0578");
        cell11O.setCellValue(content11O);

        XSSFCell cell11P = row11.createCell((short) 15);
        XSSFRichTextString content11P = new XSSFRichTextString(formatAmount(valueP11));
        cell11P.setCellValue(content11P);

        XSSFCell cell11Q = row11.createCell((short) 16);
        XSSFRichTextString content11Q = new XSSFRichTextString("0678");
        cell11Q.setCellValue(content11Q);

        XSSFCell cell11R = row11.createCell((short) 17);
        XSSFRichTextString content11R = new XSSFRichTextString(formatAmount(valueR11));
        cell11R.setCellValue(content11R);

        XSSFCell cell11S = row11.createCell((short) 18);
        XSSFRichTextString content11S = new XSSFRichTextString("0778");
        cell11S.setCellValue(content11S);

        XSSFCell cell11T = row11.createCell((short) 19);
        XSSFRichTextString content11T = new XSSFRichTextString(formatAmount(valueT11));
        cell11T.setCellValue(content11T);

        XSSFCell cell11U = row11.createCell((short) 20);
        XSSFRichTextString content11U = new XSSFRichTextString("0878");
        cell11U.setCellValue(content11U);

        XSSFCell cell11V= row11.createCell((short) 21);
        XSSFRichTextString content11V= new XSSFRichTextString(formatAmount(valueV11));
        cell11V.setCellValue(content11V);

        XSSFCell cell11W= row11.createCell((short) 22);
        XSSFRichTextString content11W= new XSSFRichTextString("0978");
        cell11W.setCellValue(content11W);

        XSSFCell cell11X = row11.createCell((short) 23);
        XSSFRichTextString content11X = new XSSFRichTextString(formatAmount(valueX11));
        cell11X.setCellValue(content11X);

        XSSFCell cell11Y = row11.createCell((short) 24);
        XSSFRichTextString content11Y = new XSSFRichTextString("1078");
        cell11Y.setCellValue(content11Y);

        XSSFCell cell11Z = row11.createCell((short) 25);
        XSSFRichTextString content11Z = new XSSFRichTextString(formatAmount(valueZ11));
        cell11Z.setCellValue(content11Z);

        XSSFCell cell11AA = row11.createCell((short) 26);
        XSSFRichTextString content11AA = new XSSFRichTextString("1178");
        cell11AA.setCellValue(content11AA);

        XSSFCell cell11AB = row11.createCell((short) 27);
        XSSFRichTextString content11AB = new XSSFRichTextString(formatAmount(valueAB11));
        cell11AB.setCellValue(content11AB);

        XSSFCell cell11AC = row11.createCell((short) 28);
        XSSFRichTextString content11AC = new XSSFRichTextString("1278");
        cell11AC.setCellValue(content11AC);

        XSSFCell cell11AD = row11.createCell((short) 29);
        XSSFRichTextString content11AD = new XSSFRichTextString(ZERO_VALUE);
        cell11AD.setCellValue(content11AD);

        XSSFCell cell11AE = row11.createCell((short) 30);
        XSSFRichTextString content11AE = new XSSFRichTextString("1378");
        cell11AE.setCellValue(content11AE);

        XSSFCell cell11AF = row11.createCell((short) 31);
        XSSFRichTextString content11AF = new XSSFRichTextString(ZERO_VALUE);
        cell11AF.setCellValue(content11AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        XSSFCell cell12A = row12.createCell((short) 0);
        XSSFRichTextString content12A = new XSSFRichTextString("     2.4.1.3. De Otros Sectores No Residentes");
        cell12A.setCellValue(content12A);

        XSSFCell cell12E = row12.createCell((short) 4);
        XSSFRichTextString content12E = new XSSFRichTextString("0079");
        cell12E.setCellValue(content12E);

        XSSFCell cell12F = row12.createCell((short) 5);
        XSSFRichTextString content12F = new XSSFRichTextString(formatAmount(valueF12));
        cell12F.setCellValue(content12F);

        XSSFCell cell12G = row12.createCell((short) 6);
        XSSFRichTextString content12G = new XSSFRichTextString("0179");
        cell12G.setCellValue(content12G);

        XSSFCell cell12H = row12.createCell((short) 7);
        XSSFRichTextString content12H = new XSSFRichTextString(formatAmount(valueH12));
        cell12H.setCellValue(content12H);

        XSSFCell cell12I = row12.createCell((short) 8);
        XSSFRichTextString content12I = new XSSFRichTextString("0279");
        cell12I.setCellValue(content12I);

        XSSFCell cell12J = row12.createCell((short) 9);
        XSSFRichTextString content12J = new XSSFRichTextString(formatAmount(valueJ12));
        cell12J.setCellValue(content12J);

        XSSFCell cell12K = row12.createCell((short) 10);
        XSSFRichTextString content12K = new XSSFRichTextString("0379");
        cell12K.setCellValue(content12K);

        XSSFCell cell12L = row12.createCell((short) 11);
        XSSFRichTextString content12L = new XSSFRichTextString(formatAmount(valueL12));
        cell12L.setCellValue(content12L);

        XSSFCell cell12M = row12.createCell((short) 12);
        XSSFRichTextString content12M = new XSSFRichTextString("0479");
        cell12M.setCellValue(content12M);

        XSSFCell cell12N = row12.createCell((short) 13);
        XSSFRichTextString content12N = new XSSFRichTextString(formatAmount(valueN12));
        cell12N.setCellValue(content12N);

        XSSFCell cell12O = row12.createCell((short) 14);
        XSSFRichTextString content12O = new XSSFRichTextString("0579");
        cell12O.setCellValue(content12O);

        XSSFCell cell12P = row12.createCell((short) 15);
        XSSFRichTextString content12P = new XSSFRichTextString(formatAmount(valueP12));
        cell12P.setCellValue(content12P);

        XSSFCell cell12Q = row12.createCell((short) 16);
        XSSFRichTextString content12Q = new XSSFRichTextString("0679");
        cell12Q.setCellValue(content12Q);

        XSSFCell cell12R = row12.createCell((short) 17);
        XSSFRichTextString content12R = new XSSFRichTextString(formatAmount(valueR12));
        cell12R.setCellValue(content12R);

        XSSFCell cell12S = row12.createCell((short) 18);
        XSSFRichTextString content12S = new XSSFRichTextString("0779");
        cell12S.setCellValue(content12S);

        XSSFCell cell12T = row12.createCell((short) 19);
        XSSFRichTextString content12T = new XSSFRichTextString(formatAmount(valueT12));
        cell12T.setCellValue(content12T);

        XSSFCell cell12U = row12.createCell((short) 20);
        XSSFRichTextString content12U = new XSSFRichTextString("0879");
        cell12U.setCellValue(content12U);

        XSSFCell cell12V= row12.createCell((short) 21);
        XSSFRichTextString content12V= new XSSFRichTextString(formatAmount(valueV12));
        cell12V.setCellValue(content12V);

        XSSFCell cell12W= row12.createCell((short) 22);
        XSSFRichTextString content12W= new XSSFRichTextString("0979");
        cell12W.setCellValue(content12W);

        XSSFCell cell12X = row12.createCell((short) 23);
        XSSFRichTextString content12X = new XSSFRichTextString(formatAmount(valueX12));
        cell12X.setCellValue(content12X);

        XSSFCell cell12Y = row12.createCell((short) 24);
        XSSFRichTextString content12Y = new XSSFRichTextString("1079");
        cell12Y.setCellValue(content12Y);

        XSSFCell cell12Z = row12.createCell((short) 25);
        XSSFRichTextString content12Z = new XSSFRichTextString(formatAmount(valueZ12));
        cell12Z.setCellValue(content12Z);

        XSSFCell cell12AA = row12.createCell((short) 26);
        XSSFRichTextString content12AA = new XSSFRichTextString("1179");
        cell12AA.setCellValue(content12AA);

        XSSFCell cell12AB = row12.createCell((short) 27);
        XSSFRichTextString content12AB = new XSSFRichTextString(formatAmount(valueAB12));
        cell12AB.setCellValue(content12AB);

        XSSFCell cell12AC = row12.createCell((short) 28);
        XSSFRichTextString content12AC = new XSSFRichTextString("1279");
        cell12AC.setCellValue(content12AC);

        XSSFCell cell12AD = row12.createCell((short) 29);
        XSSFRichTextString content12AD = new XSSFRichTextString(ZERO_VALUE);
        cell12AD.setCellValue(content12AD);

        XSSFCell cell12AE = row12.createCell((short) 30);
        XSSFRichTextString content12AE = new XSSFRichTextString("1379");
        cell12AE.setCellValue(content12AE);

        XSSFCell cell12AF = row12.createCell((short) 31);
        XSSFRichTextString content12AF = new XSSFRichTextString(ZERO_VALUE);
        cell12AF.setCellValue(content12AF);

        ////////////////////////////////////////////////////////////////////////////////////////////

        return book;
    }

}
