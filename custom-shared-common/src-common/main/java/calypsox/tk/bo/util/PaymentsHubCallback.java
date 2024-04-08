package calypsox.tk.bo.util;


import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public class PaymentsHubCallback {


    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";


    /** Message Id. Field 'instrId' */
    @JsonProperty("idempotentReference")
    private String idempotentReference;


    /** Message Status */
    @JsonProperty("status")
    private String status;


    /** Communication Status */
    @JsonProperty("communicationStatus")
    private String communicationStatus;


    /** Additional Info */
    @JsonProperty("additionalInfo")
    private String additionalInfo;


    /** System Identificator */
    @JsonProperty("source")
    private String source;


    /** Timestamp message changed. Format "YYYY-MM-DD hh:mm:ss.ssssss" UTC */
    @JsonProperty("timestamp")
    private String timestamp;


    @JsonIgnore
    private JDatetime time;


    public String getIdempotentReference() {
        return idempotentReference;
    }


    public void setIdempotentReference(String idempotentReference) {
        this.idempotentReference = idempotentReference;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


    public String getCommunicationStatus() {
        return communicationStatus;
    }


    public void setCommunicationStatus(String communicationStatus) {
        this.communicationStatus = communicationStatus;
    }


    public String getAdditionalInfo() {
        return additionalInfo;
    }


    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }


    public String getSource() {
        return source;
    }


    public void setSource(String source) {
        this.source = source;
    }


    public String getTimestamp() {
        return timestamp;
    }


    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    public JDatetime getTime() {
        return time;
    }


    public void setTime(JDatetime time) {
        this.time = time;
    }


    /**
     * Get PaymentsHubRequest from text.
     *
     * @param  text
     * @return
     */
    public static PaymentsHubCallback parseText(final String text) {
        PaymentsHubCallback paymentsHubCallback = null;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            paymentsHubCallback = mapper.readValue(text, PaymentsHubCallback.class);
            // Set JDatetime timestamp
            if (!Util.isEmpty(paymentsHubCallback.getTimestamp())) {
                final JDatetime jdatetime = parseTimestamp(paymentsHubCallback.getTimestamp());
                if (jdatetime != null) {
                    paymentsHubCallback.setTime(jdatetime);
                }
            }
        } catch (JsonParseException | JsonMappingException e) {
            final String msg = String.format("Error parsing PaymentsHubRequest data.");
            Log.error(PaymentsHubCallback.class, msg, e);
        } catch (final IOException e) {
            final String msg = String.format("Error getting PaymentsHubRequest data.");
            Log.error(PaymentsHubCallback.class, msg, e);
        }
        return paymentsHubCallback;
    }


    /**
     * Parse timestamp to JDatetime
     *
     * @param  timestampStr
     * @return
     */
    public static JDatetime parseTimestamp(final String timestampStr) {
        JDatetime datetime = null;
        try {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);
            final LocalDateTime ldt = LocalDateTime.parse(timestampStr, formatter);
            datetime = new JDatetime(ldt, ZoneId.of("UTC"));
        } catch (final DateTimeParseException e) {
            final String msg = String.format("Error parsing Timestamp field.");
            Log.error(PaymentsHubCallback.class, msg, e);
        }
        return datetime;
    }


}
