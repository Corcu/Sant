package calypsox.tk.util.bean;

import java.io.Serializable;

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 *
 */
public class AverageVolumeBean implements Serializable {


	private static final long serialVersionUID = 737356814440751939L;

	protected String isin;
	protected String currency;
	protected String sec_code;
	
	public AverageVolumeBean(String[] fields) {
		if (fields.length > 2) {
			this.isin = fields[0];
			this.currency = fields[2];
			this.sec_code = fields[1];
		}
	}
	
	public String getIsin() {
		return isin;
	}
	
	public void setIsin(String isin) {
		this.isin = isin;
	}
	
	public String getCurrency() {
		return currency;
	}
	
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public String getSec_code() {
		return sec_code;
	}
	
	public void setSec_code(String sec_code) {
		this.sec_code = sec_code;
	}
	
}
