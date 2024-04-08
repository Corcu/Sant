package calypsox.tk.bo;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.acx.ACXPricesService;
import com.santander.restservices.acx.ACXServiceException;
import com.santander.restservices.acx.model.*;
import com.santander.restservices.jwt.AbstractJwtTokenSimpleService;
import com.santander.restservices.jwt.JwtTokenSimpleServiceACX;
import com.santander.restservices.oauth.ACXOauthTokenService;
import com.santander.restservices.oauth.AbstractOauthTokenService;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to call ACX API Endpoints.
 * Singleton stateful instance, keeps session tokens
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXHelper {

    public static final ACXHelper INSTANCE = new ACXHelper();

    private ACXHelper() {
    }

    public enum TokenScope {
        filenet_read("filenet.read"),
        prices_read("prices.read"),
        resources_read("resources.read");

        final String value;

        TokenScope(String value) {
            this.value = value;
        }

        public static String ofList(TokenScope... scopes) {
            return Stream.of(scopes)
                    .map(s -> s.value)
                    .collect(Collectors.joining(" "));
        }
    }

    public static final String ACX_AUTHORIZATION_GRANT_TYPE_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    private String _jwt = null;
    private String _accessToken = null;
    private static JDatetime _timestamp = null;
    private static JDatetime _expiresAt = null;

    private static final String LOG_CATEGORY = ACXHelper.class.getName();


    public List<ACXPriceResult> requestPrices(EArea area,
                                              ELayerType layerType,
                                              EUnit unit,
                                              EAssetClass assetClass,
                                              EFactorType factorType,
                                              EUnderlyingIdType underlyingIdType,
                                              Date startDate,
                                              Date endDate,
                                              List<String> underlyingList)
            throws ACXServiceException {

        // if a session is expired or not started
        boolean sessionIsNotValid = checkSessionIsInvalid();

        if (sessionIsNotValid) {
            initSession();
        }

        ACXPricesService service = new ACXPricesService();

        service.setAccessToken(_accessToken);

        ACXPricesInput p = service.getInput();

        p.setArea(area);
        p.setLayerType(layerType);
        p.setUnit(unit);
        p.setAssetClass(assetClass);
        p.setFactorType(factorType);
        p.setUnderlyingIdType(underlyingIdType);
        p.setStartDate(startDate);
        p.setEndDate(endDate);
        ACXRiskFactor riskFactor = new ACXRiskFactor();
        p.setRiskFactor(riskFactor);
        riskFactor.setUnderlyings(underlyingList);

        int status = service.callService();

        if (ApiRestAdapter.isOKHttpStatus(status)) {
            return service.getOutput().getPriceResultList();
        }

        // Error status
        throw new ACXServiceException(status, service.getError().pullTextMessage());
    }

    private synchronized void initSession() throws ACXServiceException {

        if ((_jwt != null && _accessToken != null)) {
            String msg = "The session is initiated.";
            Log.debug(LOG_CATEGORY, msg);
            return;
        }

        // Get JWT Token
        getJwtToken();
        // Get Access Token
        getAccessToken();
    }

    private void getJwtToken() throws ACXServiceException {

        // Call JwtTokenSimpleService
        AbstractJwtTokenSimpleService service = new JwtTokenSimpleServiceACX();

        int status = service.callService();

        if (ApiRestAdapter.isOKHttpStatus(status)) {
            _jwt = (service.getOutput() != null) ? service.getOutput().getToken() : null;
            _accessToken = null;
        } else {
            _jwt = null;
            _accessToken = null;
            String msg = String.format("Status of calling the JwtToken service: %d ", status);
            Log.error(LOG_CATEGORY, msg);
            throw new ACXServiceException(status, msg);
        }
    }


    private void getAccessToken() throws ACXServiceException {

        if (_jwt == null)
            throw new ACXServiceException("JwtToken is not set");

        String scope = TokenScope.ofList(TokenScope.prices_read);

        AbstractOauthTokenService service = new ACXOauthTokenService();
        service.setScope(scope);
        service.setGrantType(ACX_AUTHORIZATION_GRANT_TYPE_BEARER);
        service.setJwtToken(_jwt);

        int status = service.callService();

        if (ApiRestAdapter.isOKHttpStatus(status)) {
            // Set _accessToken
            _accessToken = (service.getOutput() != null) ? service.getOutput().getAccessToken() : null;
            _timestamp = (_accessToken != null) ? new JDatetime() : null;
            _expiresAt = (_accessToken != null) ? _timestamp.add(service.getOutput().getExpiresIn() * 1000) : null;

        } else {
            String msg = String.format(
                    "Status AccessToken service : [%d] . Mode [%s]. Tokens to null : JWT [%s] - AccessToken [%s] ",
                    status, scope, _jwt, _accessToken);

            Log.error(LOG_CATEGORY, msg);

            _jwt = null;
            _accessToken = null;
            _timestamp = null;
            _expiresAt = null;

            throw new ACXServiceException(status, msg);
        }
    }


    private boolean checkSessionIsInvalid() {

        if (_expiresAt != null) {
            // Get Now
            JDatetime now = new JDatetime();

            if (now.after(_expiresAt)) {
                // Session expired
                String debug = String.format("Session expired : Log in at [%s], expired at [%s]. Expired Token [%s].",
                        _timestamp.toString(), _expiresAt.toString(), _accessToken);
                Log.debug(LOG_CATEGORY, debug);
                _accessToken = null;
                _timestamp = null;
                _expiresAt = null;
                return true;
            }
            return false;
        }
        return true;
    }

}
