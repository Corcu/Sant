package calypsox.tk.bo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.digitalplatformnotif.NotifDigitalPlatformService;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformAction;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformInput;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformRecipient;
import com.santander.restservices.filenet.InsertDocumentService;
import com.santander.restservices.jwt.JwtTokenSimpleServiceNotifDigitalPlatform;
import com.santander.restservices.oauth.NotifDigitalPlatformOauthTokenService;

import calypsox.tk.bo.util.FormatUtil;
import calypsox.tk.bo.util.StringUtil;

public class CANotifDigitalPlatformHelper {

	private static final String LOG_CATEGORY = CANotifDigitalPlatformHelper.class.getName();
	public static final String AUTHORIZATION_GRANT_TYPE_BAERER = "urn:ietf:params:oauth:grant-type:jwt-bearer";

	public static final int INIT_SESION_MAX_ATTEMPTS = 3;

	private static String _jwt = null;
	private static String _access_write = null;
	private static JDatetime _access_write_timestamp = null;
	private int status = -1;
	private String errorMsg = null;
	private String textMsg = null;

	public static CANotifDigitalPlatformHelper getInstance() {
		return new CANotifDigitalPlatformHelper();
	}

	/**
	 * Init session in Api
	 * 
	 * @param nameService name service to find the connection parameters
	 * @param attempts  number of connection attemps
	 * @return true if the connection has been established successfully
	 */
	private static synchronized boolean initSession(String nameService, int attempts) {
		boolean control = false;

		if (_jwt == null)
			getJwtToken(nameService);

		if (_jwt != null) {
			getAccessToken(nameService);

			if ((_access_write != null)) {
				control = true;
			} else {
				if (attempts > 0)
					control = initSession(nameService, --attempts);
			}
		} else {
			if (attempts > 0)
				control = initSession(nameService, --attempts);
		}

		return control;
	}
	
	/**
	 * Get the JWT Token
	 * @param nameService service name to find the connection parameters
	 * @return true if get the token correctly
	 */
	private static boolean getJwtToken(String nameService) {
		boolean control = false;
		JwtTokenSimpleServiceNotifDigitalPlatform service = null;
		int status = -1;

		service = new JwtTokenSimpleServiceNotifDigitalPlatform(nameService);
		status = service.callService();

		if (ApiRestAdapter.isOKHttpStatus(status)) {
			_jwt = (service.getOutput() != null) ? service.getOutput().getToken() : null;
			_access_write = null;
			_access_write_timestamp = null;
			control = true;
		} else {
			final String msg = String.format("Status JwtToken service : %d ", status);
			Log.error(LOG_CATEGORY, msg);
			Log.error(LOG_CATEGORY, service.getError().getErrors().toString());

			_jwt = null;
			_access_write = null;
			_access_write_timestamp = null;

		}

		return control;
	}

	/**
	 * Get the Oauth token
	 * @param nameService service name to find the connection parameters
	 * @return true if get the token correctly
	 */
	private static boolean getAccessToken(String nameService) {

		boolean control = false;
		NotifDigitalPlatformOauthTokenService service = null;
		int status = -1;

		if (_jwt != null) {
			service = new NotifDigitalPlatformOauthTokenService(nameService);

			service.setScope("vdr.notify");
			service.setGrantType(AUTHORIZATION_GRANT_TYPE_BAERER);
			service.setJwtToken(_jwt);

			status = service.callService();

			if (ApiRestAdapter.isOKHttpStatus(status)) {

				_access_write = (service.getOutput() != null) ? service.getOutput().getAccessToken() : null;
				_access_write_timestamp = (_access_write != null) ? new JDatetime() : null;

				control = true;
			} else {

				final String msg = String.format(
						"Status AccessToken service : [%d] . Tokens to null : JWT [%s] - AccessToken [%s] ", status,
						_jwt, _access_write);
				Log.error(LOG_CATEGORY, msg);
				Log.error(LOG_CATEGORY, service.getError().getErrorDescription());

				_jwt = null;
				_access_write = null;
				_access_write_timestamp = null;

			}
		}

		return control;
	}

	/**
	 * Check if the access token is expired
	 * @return true if token is expired
	 */
	private static synchronized boolean isAccessTokenSessionExpired() {
		String debug = "";

		JDatetime limitDatetime = _access_write_timestamp;

		// if null, init the accesToken session
		if (limitDatetime == null) {
			return true;
		}

		// Get Values from DomainValues
		final String expiryTime = "600"; // 600 seconds
		final String gapTime = "100"; // 10 seconds

		if (NumberUtils.isCreatable(expiryTime) && NumberUtils.isCreatable(gapTime)) {
			final int expiryTimeInt = Integer.parseInt(expiryTime);
			final int gapTimeInt = Integer.parseInt(gapTime);
			final int time = (expiryTimeInt - gapTimeInt) * (1000); // milliseconds
			limitDatetime = _access_write_timestamp.add(time);

		} else {
			debug = String.format("Check values for Expiry Time [%s] and Gap Time [%s], they are not correct values.",
					expiryTime, gapTime);
			Log.debug(LOG_CATEGORY, debug);
		}

		// Get Now
		final JDatetime now = new JDatetime();

		final String nowStr = FormatUtil.getInstance(now).parseDateTimeToString(FormatUtil.DATE_TIME_DEFAULT_FORMAT);
		final String limitStr = FormatUtil.getInstance(limitDatetime)
				.parseDateTimeToString(FormatUtil.DATE_TIME_DEFAULT_FORMAT);
		final JDatetime accessDateTime = _access_write_timestamp;
		final String accessDatetimeStr = FormatUtil.getInstance(accessDateTime)
				.parseDateTimeToString(FormatUtil.DATE_TIME_DEFAULT_FORMAT);

		if (now.after(limitDatetime)) {
			// Session expired
			debug = String.format(
					"Session expired : Log in at [%s]. Expired Token [%s]. Try to call service at [%s] - Limit at [%s]",
					accessDatetimeStr, _access_write, nowStr, limitStr);
			Log.debug(LOG_CATEGORY, debug);

			_access_write_timestamp = null;
			_access_write = null;

			return true; // Session expired

		}

		// Session Active
		debug = String.format(
				"Session active : Log in at [%s]. Active Token [%s]. Try to call service at [%s] - Limit at [%s].",
				accessDatetimeStr, _access_write, nowStr, limitStr);
		Log.debug(LOG_CATEGORY, debug);
		return false; // Session not expired

	}

	/**
	 * Call the service to send the notification
	 * @param nameService nameService service name to find the connection parameters
	 * @param tittle tittle field on the request body
	 * @param content content field on the request body
	 * @param application application field on the request body
	 * @param action action field on the request body
	 * @param recipients recipients field on the request body
	 * @param isImportant isImportant field on the request body
	 * @return true if the notificacion is sent correctly
	 */
	public boolean callService(String nameService, String tittle, String content, String application,
			Map<String, Object> action, Map<String, Object> recipients, boolean isImportant) {
		boolean control = true;
		String debug = null;

		// Check Expired write access token
		final boolean isExpired = isAccessTokenSessionExpired();

		// Init session
		control = (isExpired) ? initSession(nameService, INIT_SESION_MAX_ATTEMPTS) : control;

		if (!control) {
			debug = (new StringBuilder()).append("Unable to invoke Digital Platform Notification service: ")
					.append("jwt [").append((_jwt != null) ? "OK" : "KO").append("] ").append("_access_write [")
					.append((_access_write != null) ? "OK" : "KO").append("] ").toString();

			Log.error(LOG_CATEGORY, debug);
			return control;
		}

		control = false;

		control = false;
		NotifDigitalPlatformInput input = null;
		NotifDigitalPlatformService service = null;
		NotifDigitalPlatformAction actionDP = new NotifDigitalPlatformAction();
		NotifDigitalPlatformRecipient recipientsDP = new NotifDigitalPlatformRecipient();
		List<NotifDigitalPlatformRecipient> array = new ArrayList<>();
		StringBuilder msg = null;

		try {
			service = new NotifDigitalPlatformService(nameService);
			actionDP.load(action);
			recipientsDP.load(recipients);
			array.add(recipientsDP);

			service.setAccessToken(_access_write);

			//Set body fields
			service.setTittle(tittle);
			service.setAction(actionDP);
			service.setApplication(application);
			service.setContent(content);
			service.setImportant(isImportant);
			service.setRecipients(array);

			this.status = service.callService();

			if (ApiRestAdapter.isOKHttpStatus(this.status)) {

				this.errorMsg = "Notification sent  successfully";

				control = true;

			} else {
				this.errorMsg = (service.getError() != null) ? service.getError().toString() : null;

				debug = (new StringBuilder()).append("Error sendig DP notification: ").append("tittle [").append(tittle)
						.append("] ").append("content [").append(content).append("] ").append("application [")
						.append(application).append("] ").append("status [").append(status).append("] ")
						.append("error [").append(this.errorMsg).append("] ").toString();

				if (InsertDocumentService.AUTHENTICATION_ERROR != status) {
					Log.error(LOG_CATEGORY, debug);
				}
			}

			input = service.getInput();

			msg = (new StringBuilder()).append("Error sendig DP notification: ").append("tittle [").append(tittle)
					.append("] ").append("content [").append(content).append("] ").append("application [")
					.append(application).append("] ").append("status [").append(status).append("] ").append("error [")
					.append(this.errorMsg).append("] ");

			if (input != null) {
				msg.append(printJsonString(input));
				msg.append("\n ");
			}

			this.textMsg = msg.toString();

			if (!ApiRestAdapter.isOKHttpStatus(this.status)) {
				if (InsertDocumentService.AUTHENTICATION_ERROR == status) {
					final String accessDatetimeStr = (_access_write_timestamp != null)
							? FormatUtil.getInstance(_access_write_timestamp).parseDateTimeToString(
									FormatUtil.DATE_TIME_DEFAULT_FORMAT)
							: "";
					debug = String.format(
							"Authentication Error sending Digital Platform Notification : Status [%d] - Log in at [%s] - Access Token [%s].",
							status, accessDatetimeStr, _access_write);
					Log.debug(LOG_CATEGORY, debug);
				} else {
					Log.error(LOG_CATEGORY, this.textMsg);
				}
			}

		} catch (Exception e) {
			this.errorMsg = (e.getMessage() != null) ? StringUtil.truncate(e.getMessage(), 255)
					: "Error inserting FileNet document";

			debug = (new StringBuilder()).append("Error sending DP notification: ").append("tittle [").append(tittle)
					.append("] ").append("content [").append(content).append("] ").append("application [")
					.append(application).append("] ").append("with exception ").append(e.toString()).append(" - ")
					.append(e.getMessage()).append(" ").toString();

			Log.error(LOG_CATEGORY, debug, e);
		}

		return control;

	}

	private String printJsonString(ApiRestModel model) {
		String out = null;

		ObjectMapper mapper = null;

		String debug = null;

		try {
			mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);

			if (model != null)
				out = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
		} catch (JsonProcessingException e) {
			debug = (new StringBuilder()).append("Error printing json ").append("with exception ").append(e.toString())
					.append(" - ").append(e.getMessage()).append(" ").toString();

			Log.error(LOG_CATEGORY, debug);
		}

		return out;
	}

}
