package calypsox.tk.report;

import calypsox.tk.camel.processor.confirmation.CalypsoConfirmationResponseProcessor;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class ABCTradeReportStyle extends TradeReportStyle {

    public static final String CONFIRMATION_STATUS="Confirmation Status";
    public static final String VALIDATION_STATUS="Validation Status";
    public static final String DEAL_STATUS="Deal Status";
    public static final String ENTERED_DATE_HOUR="Entered Date Day Time";
    public static final String CONFIRMATION_DATE_HOUR="Confirmation Day Time";
    public static final String CONFIRMATION_DATE="Confirmation Date";
    public static final String CONFIRMATION_CHANNEL="Confirmation Channel";

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        Object result;
        Trade trade=row.getProperty(Trade.class.getSimpleName());
        PdfConfirmationStatus confirmationStatus=getConfirmationStatus(trade);
        if(CONFIRMATION_STATUS.equals(columnName)){
            result=confirmationStatus.getValue();
        }else if(VALIDATION_STATUS.equals(columnName)){
            result=getValidationStatus(trade);
        }else if(ENTERED_DATE_HOUR.equals(columnName)){
            result=getEnteredTime(trade);
        }else if(CONFIRMATION_DATE.equals(columnName)){
            result=getConfirmationDate(trade,confirmationStatus);
        }else if(CONFIRMATION_DATE_HOUR.equals(columnName)){
            result=getConfirmationTime(trade,confirmationStatus);
        }else if(CONFIRMATION_CHANNEL.equals(columnName)){
            result=getConfirmationChannel();
        }else if(DEAL_STATUS.equals(columnName)){
        result=getDealStatus(row.getProperty(ABCTradeReport.IS_SETTLED_TRADE_PROP));
    }else{
            result=super.getColumnValue(row,columnName,errors);
        }
        return result;
    }

    private PdfConfirmationStatus getConfirmationStatus(Trade trade){
        return Optional.ofNullable(trade)
                .map(t->t.getKeywordValue(CalypsoConfirmationResponseProcessor.MATCH_STATUS_KWD))
                .map(PdfConfirmationStatus::lookUp)
                .orElse(PdfConfirmationStatus.BOHOLDING);
    }

    private String getValidationStatus(Trade trade){
        return Optional.ofNullable(trade)
                .map(Trade::getStatus)
                .map(t-> MissingSDIStatus.lookUp(t.getStatus()))
                .map(MissingSDIStatus::getReportValue)
                .orElse(MissingSDIStatus.CREATED.getReportValue());

    }

    private String getEnteredTime(Trade trade){
        return Optional.ofNullable(trade)
                .map(Trade::getEnteredDate)
                .map(this::extractDayTimeFromDatetime)
                .orElse("");
    }

    private String getConfirmationChannel(){
        return "E-MAIL";
    }

    private String getDealStatus(boolean isSettledTrade){
        return isSettledTrade ? DealStatus.VENCIDA.name() : DealStatus.VIVA.name();
    }

    private String getConfirmationTime(Trade trade,PdfConfirmationStatus confirmationStatus){
        return Optional.ofNullable(trade)
                .filter(t -> confirmationStatus.equals(PdfConfirmationStatus.COMPLETED))
                .map(t->t.getKeywordValue(CalypsoConfirmationResponseProcessor.CONF_DATETIME_KWD))
                .map(this::getTimeFromConfirmationDateKwd)
                .orElse("");
    }

    private String getConfirmationDate(Trade trade,PdfConfirmationStatus confirmationStatus){
        return Optional.ofNullable(trade)
                .filter(t -> confirmationStatus.equals(PdfConfirmationStatus.COMPLETED))
                .map(t->t.getKeywordValue(CalypsoConfirmationResponseProcessor.CONF_DATETIME_KWD))
                .map(this::getDateFromConfirmationDateKwd)
                .orElse("");
    }

    private String getDateFromConfirmationDateKwd(String dateValue){
        return getParseConfirmationDateKwd(dateValue,0);

    }

    private String getTimeFromConfirmationDateKwd(String dateValue){
        return getParseConfirmationDateKwd(dateValue,1);

    }

    private String getParseConfirmationDateKwd(String dateValue, int position){
        return Optional.ofNullable(dateValue)
                .map(st->st.split(" "))
                .filter(st->st.length==2)
                .map(st->st[position])
                .orElse("");

    }

    private String extractDayTimeFromDatetime(JDatetime datetime){
        return formatDatetime(datetime,"HH:mm:ss");
    }

    private String formatDatetime(JDatetime datetime,String pattern){
        SimpleDateFormat format=new SimpleDateFormat(pattern);
        return format.format(datetime);
    }

    private enum PdfConfirmationStatus{

        BOHOLDING("NO CONFIRMADA"),
        COMPLETED("CONFIRMADA"),
        DISPATCHFAILURE("INCIDENCIA ENVIO"),
        PRODUCTVARIABLESINCOMPLETED("INCIDENCIA EN PROCESO"),
        SENT("AFIRMADA Y PDTE. CONFIRMAR");

        String abcConfirmationValue;

        PdfConfirmationStatus(String abcConfirmationValue){
            this.abcConfirmationValue=abcConfirmationValue;
        }

        String getValue(){
            return this.abcConfirmationValue;
        }

        static PdfConfirmationStatus lookUp(String sourceMatchingStatus){
            PdfConfirmationStatus status;
            try {
                status = Optional.ofNullable(sourceMatchingStatus)
                        .map(str->str.replace(" ","")).map(String::toUpperCase)
                        .map(PdfConfirmationStatus::valueOf)
                        .orElse(BOHOLDING);
            }catch(IllegalArgumentException exc){
                status=BOHOLDING;
            }
            return status;
        }
    }

    private enum MissingSDIStatus{
        CREATED,
        PARTENON,
        PENDING,
        VALIDATED("VALIDADA");

        String reportValue;

        MissingSDIStatus(){
            this.reportValue="FIRMADA Y NO VALIDADA";
        }

        MissingSDIStatus(String reportValue){
            this.reportValue=reportValue;
        }
        static MissingSDIStatus lookUp(String sourceStatus){
            MissingSDIStatus status;
            try {
                status = Optional.ofNullable(sourceStatus)
                        .map(MissingSDIStatus::valueOf)
                        .orElse(VALIDATED);
            }catch (IllegalArgumentException exc){
                status=VALIDATED;
            }
            return status;
        }

        String getReportValue(){
            return this.reportValue;
        }
    }

    private enum DealStatus{
        VIVA,
        VENCIDA
    }
}
