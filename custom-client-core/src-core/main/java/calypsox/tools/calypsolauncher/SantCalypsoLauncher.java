package calypsox.tools.calypsolauncher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Properties;

import com.calypso.apps.startup.AppStarter;
import com.calypso.apps.util.CalypsoLoginDialog;
import org.apache.commons.io.FileUtils;

import com.calypso.tk.core.Defaults;

public class SantCalypsoLauncher extends AppStarter {

	// Properties
	private static final String TRUSTSTORE_FILE_NAME = "CUSTOM_SSL_TRUSTSTORE_FILE";
	private static final String TRUSTSTORE_PASSWORD = "CUSTOM_SSL_TRUSTSTORE_PWD";
	private static final String TRUSTSTORE_SERVER_PASSWORD = "CUSTOM_SSL_TRUSTSTORE_SERVER_PWD";
	private static final String LAUNCH = "-launch";
	private static final String SERVER = "-server_mode";
	private static final String trustStore="-trust";
	private static final String trustPassword="-trustpwd";
	private static boolean serverMode=false;

	private static Properties props;

	private static String env;

	private static boolean shouldUseSsl(String[] args) {

		return Boolean.parseBoolean((String) props.getOrDefault("USE_SSL", "false"));
	}

	private static File getTrustFileForClient(String[] args){
		String trustStoreName = (String) props.getOrDefault(TRUSTSTORE_FILE_NAME, "client.truststore");
		trustStoreName = String.format("%s.%s", trustStoreName, env);

		File trustFile = null;
		try (InputStream in = SantCalypsoLauncher.class.getClassLoader().getResourceAsStream(trustStoreName)) {

			if (in == null) {
				throw new FileNotFoundException(
						String.format("Could not locate truststore file %s", trustStoreName));
			}

			trustFile = File.createTempFile("client", String.format("truststore.%s", env));

			FileUtils.copyInputStreamToFile(in, trustFile);

		} catch (IOException e) {
			// Trowing RuntimeException here since we can not change the main method
			// signature.
			throw new RuntimeException(e);
		}

		return trustFile;
	}

	private static File getTrustFileForServer(String[] args){
		String trustStoreFile = getOption(args, trustStore);

		return Paths.get(trustStoreFile).toFile();

	}

	private static void setUpTrustStoreParams(String[] args) {

		if(serverMode && !isOption(args, trustStore)){
			throw new RuntimeException(String.format("Server mode is enabled. Should provide %s", trustStore));
		}


		String trustStorePass = (String) props.getOrDefault(serverMode ? TRUSTSTORE_SERVER_PASSWORD : TRUSTSTORE_PASSWORD, "calypso");

		System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);

		File trustFile = serverMode ? getTrustFileForServer(args) : getTrustFileForClient(args);

		System.setProperty("javax.net.ssl.trustStore", trustFile.toString());
	}

	public static void main(String[] args) {

		env = getOption(args, ENV);

		props = Defaults.getProperties(env);

		serverMode = isOption(args, SERVER);

		if (shouldUseSsl(args)) {
			setUpTrustStoreParams(args);
		}

		String toBeLaunch = getOption(args, LAUNCH);
		if (toBeLaunch == null) {
			throw new IllegalArgumentException("Unknown main class to launch. Please specify -launch option");
		}

		Class<?> cls;
		try {
			cls = Class.forName(toBeLaunch);
			Method meth = cls.getMethod("main", String[].class);
			meth.invoke(null, (Object) args);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new SecurityException(String.format("Error while trying to launch class %s", toBeLaunch), e);
		}

	}

	@Override
	public void onConnect(CalypsoLoginDialog dialog, String user, String passwd, String envName) {
		//Nothing to do here
	}
}

