package calypsox.tk.bo;


import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.datasign.DataSignService;
import com.santander.restservices.datasign.model.DataSignInput;
import com.santander.restservices.jwt.JwtTokenSimpleServicePaymentsHub;
import com.santander.restservices.oauth.PaymentsHubOauthTokenService;
import com.santander.restservices.paymentshub.PaymentsHubService;
import com.santander.restservices.paymentshub.model.PaymentsHubError;
import com.santander.restservices.paymentshub.model.PaymentsHubErrors;
import com.santander.restservices.paymentshub.model.PaymentsHubInput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PaymentsHubHelper {


    public static final String PH_AUTHORIZATION_SCOPE = "makePayments";
    public static final String PH_AUTHORIZATION_GRANT_TYPE_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public static final String INIT_SESION_MAX_ATTEMPTS = "3";
    public static final String SCATOKEN_EXPIRATION_TIME = "180";
    public static final String SCATOKEN_ALGORITHM = "SHA256";
    public static final String DV_PH_SCATOKEN_EXPIRATION_TIME = "SCATokenExpirationTime";
    private static String _jwt = null;
    private static String _accessToken = null;
    private static String _scaToken = null;
    private int status = -1;
    private boolean connectionFail = false;
    private boolean httpOk = false;
    private Map<String, List<String>> responseHeaders = null;
    private String responseMessage = null;
    private String textMessage = null;
    private static final String LOG_CATEGORY = PaymentsHubHelper.class.getName();
    private static final String ERROR_UNAUTHORIZED = "Call PaymentsHub service is Unauthorized: status [%s] BOMessage Id [%s] jwt [%s] _access [%s] _scaToken [%s]. ";
    private static final String ERROR_RESPONSE_SERVICE = "Error invoking PaymentsHub service: BOMessage Id [%s] ";
    private static final String ERROR_INVOKE_SERVICE = "Error invoking PaymentsHub service: status [%s] BOMessage Id [%s] jwt [%s] _access [%s] _scaToken [%s]. ";
    private static final String ERROR_SENDING_REQUEST = "Error sending request to PaymentsHub: BOMessage Id [%s] with exception : [%s - %s]";
    private static final String SUCCESS_DESCRIPTION_SERVICE = "Success - Status [%s]. Description [%s] ";
    private static final String SUCCESS_SERVICE = "Success - Status [%s]";
    private static final String ERROR_TOKEN = "Tokens error BOMessage Id [%s] - Check the AccessToken [%s] param and the SCAToken [%s] param.";
    private static final String ERROR_DESCRIPTION_SERVICE = "Error - Status [%s]. Description [%s] ";
    private static final String ERROR_SERVICE = "Error - Status [%s]";


    public PaymentsHubHelper() {
        responseHeaders = new HashMap<String, List<String>>();
    }


    public int getStatus() {
        return status;
    }


    public void setStatus(int status) {
        this.status = status;
    }


    public boolean isConnectionFail() {
        return connectionFail;
    }


    public void setConnectionFail(boolean connectionFail) {
        this.connectionFail = connectionFail;
    }


    public boolean isHttpOk() {
        return httpOk;
    }


    public void setHttpOk(boolean httpOk) {
        this.httpOk = httpOk;
    }


    public Map<String, List<String>> getResponseHeaders() {
        return (responseHeaders != null) ? new HashMap<String, List<String>>(responseHeaders) : null; // Kiuwan
    }


    public void addResponseHeader(final String key, final List<String> value) {
        responseHeaders.put(key, value);
    }


    public String getResponseMessage() {
        return responseMessage;
    }


    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }


    public String getTextMessage() {
        return textMessage;
    }


    public void setTextMessage(String textMessage) {
        this.textMessage = textMessage;
    }


    // Config


    /**
     * Init session
     *
     * @param attempts
     * @param phMessage
     * @return
     */
    private synchronized static void initSession(final PaymentsHubMessage phMessage) {
        if (!(_jwt != null && _accessToken != null)) {
            // Get JWT Token
            getJwtToken();
            if (_jwt != null) {
                // Get Access Token
                getAccessToken();
            }
        } else {
            final String msg = "The session is initiated.";
            Log.debug(LOG_CATEGORY, msg);
        }
        // Get SCA Token
        getSCAToken(phMessage);
    }


    /**
     * Get JWT Token.
     *
     * @return
     */
    private static boolean getJwtToken() {
        boolean control = false;
        // Call JwtTokenSimpleService
        final JwtTokenSimpleServicePaymentsHub service = new JwtTokenSimpleServicePaymentsHub();
        final int status = service.callService();
        if (ApiRestAdapter.isOKHttpStatus(status)) {
            _jwt = (service.getOutput() != null) ? service.getOutput().getToken() : null;
            _accessToken = null;
            control = true;
        } else {
            _jwt = null;
            _accessToken = null;
        }
        return control;
    }


    /**
     * Get Access Token
     *
     * @return
     */
    private static boolean getAccessToken() {
        boolean control = false;
        if (_jwt != null) {
            final PaymentsHubOauthTokenService service = new PaymentsHubOauthTokenService();
            service.setProxy(true); // Use Proxy
            service.setScope(PH_AUTHORIZATION_SCOPE);
            service.setGrantType(PH_AUTHORIZATION_GRANT_TYPE_BEARER);
            service.setJwtToken(_jwt);
            final int status = service.callService();
            if (ApiRestAdapter.isOKHttpStatus(status)) {
                // Set _accessToken
                _accessToken = (service.getOutput() != null) ? service.getOutput().getAccessToken() : null;
                control = true;
            } else {
                _jwt = null;
                _accessToken = null;
            }
        }
        return control;
    }


    /**
     * Get SCA Token
     *
     * @return
     */
    private static boolean getSCAToken(final PaymentsHubMessage phMessage) {
        boolean control = false;
        if (_jwt != null) {
            final String data = phMessage.getPaymentsHubText();
            final int expirationTime = getExpirationTime();
            final String uniqueTxId = PaymentsHubUtil.getUUID(phMessage.getBOTransfer());
            // Create Input
            final DataSignInput input = new DataSignInput();
            input.setData(data);
            input.setAlgorithm(SCATOKEN_ALGORITHM); // By default
            input.setExpirationTime(expirationTime);
            input.setUniqueTransactionId(uniqueTxId);
            // Call DataSignService
            final DataSignService service = new DataSignService(input);
            service.setToken(_jwt);
            final int status = service.callService();
            if (ApiRestAdapter.isOKHttpStatus(status)) {
                // Set _scaToken
                _scaToken = (service.getOutput() != null) ? service.getOutput().getJwtToken() : null;
                control = true;
            } else {
                _jwt = null;
                _accessToken = null;
                _scaToken = null;
            }
        }
        return control;
    }


    /**
     * Call PaymentsHub Service.
     *
     * @param boMessage
     * @param pricingEnv
     */
    public void callPaymentsHubService(final BOMessage boMessage, final PricingEnv pricingEnv) {
        boolean control = false;
        String debug = null;
        // Create and build PaymentsHub Message
        final PaymentsHubMessage phMessage = new PaymentsHubMessage(boMessage);
        phMessage.setPricingEnv(pricingEnv);
        phMessage.build(false);
        // Init Session
        initSession(phMessage);
        // Call the Service
        control = callPaymentsHubService(phMessage);
        // Set if Http OK.
        setHttpOk(control);
        if (!control) {
            // Log
            if (ApiRestAdapter.isUnauthorizedHttpStatus(getStatus())) {
                debug = String.format(ERROR_UNAUTHORIZED, String.valueOf(getStatus()), String.valueOf(boMessage.getLongId()),
                        (!Util.isEmpty(_jwt)) ? "OK" : "KO", (!Util.isEmpty(_accessToken)) ? "OK" : "KO",
                        (!Util.isEmpty(_scaToken)) ? "OK" : "KO");
            } else {
                debug = String.format(ERROR_INVOKE_SERVICE, String.valueOf(getStatus()), String.valueOf(boMessage.getLongId()),
                        (!Util.isEmpty(_jwt)) ? "OK" : "KO", (!Util.isEmpty(_accessToken)) ? "OK" : "KO",
                        (!Util.isEmpty(_scaToken)) ? "OK" : "KO");
            }
            Log.error(LOG_CATEGORY, debug);
            // If Unauthorized
            // If AccessToken/SCAToken Null
            // If Server Error
            if (isConnectionFail()) {
                _jwt = null;
                _accessToken = null;
                _scaToken = null;
            }
            setTextMessage("");
        }
    }


    /**
     * Call PaymentsHub Service.
     *
     * @param phMessage
     * @return
     */
    private boolean callPaymentsHubService(final PaymentsHubMessage phMessage) {
        boolean control = false;
        String debug = null;
        // Checks if the call is configured.
        if (isPaymentsHubCallServiceConfigured()) {
            // Get PaymentsHubInput
            final PaymentsHubInput input = phMessage.getPaymentRequest();
            try {
                // Call the PaymentsHubService
                final PaymentsHubService service = new PaymentsHubService();
                service.setProxy(true); // Use Proxy
                service.setInput(input);
                service.setAccessToken(_accessToken);
                service.setScaToken(_scaToken);
                final int status = service.callService();
                // Set status
                setStatus(status);
                // Set textMessage sent to PaymensHub
                final String phTextPretty = phMessage.getTexMessageWithDefaultPrettyPrinter();
                setTextMessage(phTextPretty); // as Pretty format
                if (ApiRestAdapter.isOKHttpStatus(status)) { // Status == 2xx
                    // Process when OK
                    processOkStatus(status, service);
                    control = true;
                } else { // Status != 2xx
                    debug = String.format(ERROR_RESPONSE_SERVICE, phMessage.getBOMessage().getLongId());
                    Log.error(LOG_CATEGORY, debug);
                    // Process when KO
                    processKoStatus(status, service);
                    // Log
                    Log.error(LOG_CATEGORY, toString());
                }
            } catch (final Exception e) {
                debug = String.format(ERROR_SENDING_REQUEST, String.valueOf(phMessage.getBOMessage().getLongId()), e.toString(), e.getMessage());
                Log.error(LOG_CATEGORY, debug, e);
            }
        } else {
            debug = String.format(ERROR_TOKEN, phMessage.getBOMessage().getLongId(), (!Util.isEmpty(_accessToken)) ? "OK" : "KO", (!Util.isEmpty(_scaToken)) ? "OK" : "KO");
            Log.error(LOG_CATEGORY, debug);
            // Connection failed - AccessToken/SCAToken KO
            setConnectionFail(true);
        }
        return control;
    }


    /**
     * Process when the PaymentsHub Service response with OK Status.
     *
     * @param status
     * @param service
     */
    private void processOkStatus(final int status, final PaymentsHubService service) {
        // Set Output Msg
        final String outputMessage = (service.getOutput() != null) ? service.getOutput().getInfo() : "";
        final String debug = String.format(SUCCESS_DESCRIPTION_SERVICE, String.valueOf(status), outputMessage);
        // Set Response Message
        if (!Util.isEmpty(outputMessage)) {
            setResponseMessage(debug);
        } else {
            setResponseMessage(String.format(SUCCESS_SERVICE, String.valueOf(status)));
        }
        // Additional Info
        if (service.getResponseHeaders() != null) {
            service.getResponseHeaders().forEach((name, value) -> { addResponseHeader(name, value); });
        }
        Log.debug(LOG_CATEGORY, debug);
    }


    /**
     * Process when the PaymentsHub Service response with KO Status
     *
     * Status == 0 -> error Service configuration (properties file, ApiRestAdapter)
     * Status == -1 -> error invoking PH Service before the call
     * Status != 2xx -> service response
     *
     * @param status
     * @param service
     */
    private void processKoStatus(final int status, final PaymentsHubService service) {
        // Set Error Msg
        final String errorMessage = (service.getError() != null) ? service.getError().toString() : "";
        String debug = String.format(ERROR_DESCRIPTION_SERVICE, String.valueOf(getStatus()), errorMessage);
        // Set Response Message
        String msg = "";
        final List<PaymentsHubError> phErrorList = (service.getError() != null) ? service.getError().getErrors() : null;
        if (!Util.isEmpty(phErrorList)) {
            msg = String.format(ERROR_DESCRIPTION_SERVICE, String.valueOf(status), phErrorList.get(0).getInfoMessage());
            setResponseMessage(msg);
        } else {
            if (!Util.isEmpty(errorMessage)) {
                setResponseMessage(debug);
            } else {
                msg = String.format(ERROR_SERVICE, String.valueOf(status));
                setResponseMessage(msg);
            }
        }
        // If Unauthorized - Status == 401
        if (ApiRestAdapter.isUnauthorizedHttpStatus(status)) {
            // Connection failed - Unauthorized
            setConnectionFail(true);
        }
        // If Server Error - Status == 5xx OR Status < 0
        if (ApiRestAdapter.isServerErrorHttpStatus(getStatus()) || status < 0) {
            debug = PaymentsHubUtil.getLogPatternPrefix().concat(" ").concat(debug);
            // Connection failed - Server Error - Other error in the call
            setConnectionFail(true);
        }
        Log.error(LOG_CATEGORY, debug);
    }


    /**
     * Check if the AccessToken and the SCAToken are not empty.
     *
     * @return
     */
    private static boolean isPaymentsHubCallServiceConfigured() {
        return !Util.isEmpty(_accessToken) && !Util.isEmpty(_scaToken);
    }


    @Override
    public String toString() {
        final StringBuilder msg = new StringBuilder();
        msg.append("Status Code: ").append(getStatus()).append("\n");
        msg.append("Status Message: ").append((getResponseMessage() != null) ? getResponseMessage() : "").append("\n");
        msg.append("\n -- Text Message -- \n");
        msg.append((getTextMessage() != null) ? getTextMessage() : "").append("\n");
        return msg.toString();
    }


    /**
     * Get SCAToken ExpirationTime.
     *
     * @return
     */
    private static int getExpirationTime() {
        final String expirationTime = PaymentsHubUtil.getPaymenstHubParameterValue(DV_PH_SCATOKEN_EXPIRATION_TIME, SCATOKEN_EXPIRATION_TIME);
        int expTime = 0;
        if (!Util.isEmpty(expirationTime) && Util.isNumber(expirationTime)) {
            expTime = Integer.valueOf(expirationTime);
        }
        return expTime;
    }


}
