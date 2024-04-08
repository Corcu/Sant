package calypsox.tk.bo;

import javax.jms.QueueConnectionFactory;

import com.calypso.tk.core.Util;
import com.tibco.tibjms.TibjmsConnectionFactory;

public class UploadJMSQueueIEAdapter extends com.calypso.tk.bo.UploadJMSQueueIEAdapter {
	private static String SSL_PASS_PROP = "tibco.tibjms.ssl.password";
	private String sslPassword;
	
	public UploadJMSQueueIEAdapter(int opMode) {
		super(opMode);
	}

	@Override
	protected void configureFactory(QueueConnectionFactory factory) {
		if (factory instanceof TibjmsConnectionFactory) {
			TibjmsConnectionFactory tibJMSFactory = ((TibjmsConnectionFactory)factory);

			if (!Util.isEmpty(getSSLPassword())) {
				tibJMSFactory.setSSLPassword(getSSLPassword());
			}
		}
	}

	@Override
	protected void initProperties() {
		super.initProperties();

		String propVal = null;
		if ((propVal = super.getProperty(SSL_PASS_PROP)) != null) {
			this.setSSLPassword(propVal);
		}
	}
	
	public void setSSLPassword(String sslPassword) {
		this.sslPassword = sslPassword;
	}

	public String getSSLPassword() {
		return this.sslPassword;
	}
}
