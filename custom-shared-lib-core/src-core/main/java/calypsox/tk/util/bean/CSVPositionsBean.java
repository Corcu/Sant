package calypsox.tk.util.bean;

public class CSVPositionsBean {

	// variables
	private String boSystem;
	private String processingOrg;
	private String portfolio;
	private String isin;
	private String securityDesc;
	private String valueDate;
	private String qtyNom;
	private String qtyNominalSec;
	private String qtyNominalSecCcy;
	private String posStatus;
	private String posType;
	private String reusable;
	private String custodian;
	private String account;

	// to keep a line counter
	private int lineNumber;

	/**
	 * Bean constructor adding the line number
	 * 
	 * @param values
	 * @param line
	 */
	public CSVPositionsBean(String[] values, int line) {

		this(values);
		this.lineNumber = line;
	}

	/**
	 * Copy constructor
	 */
	public CSVPositionsBean(CSVPositionsBean copy) {

		setBoSystem(copy.getBoSystem());
		setProcessingOrg(copy.getProcessingOrg());
		setPortfolio(copy.getPortfolio());
		setIsin(copy.getIsin());
		setSecurityDesc(copy.getSecurityDesc());
		setValueDate(copy.getValueDate());
		setQtyNom(copy.getQtyNom());
		setQtyNominalSec(copy.getQtyNominalSec());
		setQtyNominalSecCcy(copy.getQtyNominalSecCcy());
		setPosStatus(copy.getPosStatus());
		setPosType(copy.getPosType());
		setReusable(copy.getReusable());
		setCustodian(copy.getCustodian());
		setAccount(copy.getAccount());
	}

	// constructor
	public CSVPositionsBean(String[] values) {

		setBoSystem(values[0]);
		setProcessingOrg(values[1]);
		setPortfolio(values[2]);
		setIsin(values[3]);
		setSecurityDesc(values[4]);
		setValueDate(values[5]);
		setQtyNom(values[6]);
		setQtyNominalSec(values[7]);
		setQtyNominalSecCcy(values[8]);
		setPosStatus(values[9]);
		setPosType(values[10]);
		setReusable(values[11]);

		// Jos? David Sevillano - 26/01/2012 - We check the length to put some data in the agent and account fields.
		if (values.length == 14) {
			setCustodian(values[12]);
			setAccount(values[13]);
		} else if (values.length == 12) {
			setCustodian("NONE");
			setAccount("NONE");
		}
	}

	// GSM: 24/06/2013. Added couple of methods to simplify the preprocess ST
	/**
	 * returns a hash key to be considered as unique
	 */

	public String getHashKey() {

		final String key = getIsin().trim() + ";" + getQtyNominalSecCcy().trim() + ";" + getPortfolio().trim() + ";"
				+ getCustodian().trim() + ";" + getAccount().trim() + ";" + getValueDate().trim(); // must be unique for
																								   // a position
		return key;

	}

	/**
	 * Converts the PositionBean in a line with the specific format: SUSI|BSTE|RMARQUES|US912828JB79|US TREASURY N/B T 3
	 * 1/2|05/31//0D/31/05/2013/3,5%|11/04/2013|NOM|123456789|USD|THEORETICAL|BUYSELL|Y|BNYN|015274|
	 */
	@Override
	public String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append(getBoSystem()).append("|"); // 1
		sb.append(getProcessingOrg()).append("|"); // 2
		sb.append(getPortfolio()).append("|"); // 3
		sb.append(getIsin()).append("|"); // 4
		sb.append(getSecurityDesc()).append("|"); // 5
		sb.append(getValueDate()).append("|"); // 6
		sb.append(getQtyNom()).append("|"); // 7
		sb.append(getQtyNominalSec()).append("|"); // 8
		sb.append(getQtyNominalSecCcy()).append("|"); // 9
		sb.append(getPosStatus()).append("|"); // 10
		sb.append(getPosType()).append("|"); // 11
		sb.append(getReusable()).append("|"); // 12
		sb.append(getCustodian()).append("|"); // 13
		sb.append(getAccount()).append("|"); // 14

		return sb.toString();

	}

	// GSM: 25/06/2013. End modifications

	/**
	 * @return the line number for log purposes
	 */
	public int getLineNumber() {
		return this.lineNumber;
	}

	/**
	 * @return the boSystem
	 */
	public String getBoSystem() {
		return this.boSystem;
	}

	/**
	 * @param boSystem
	 *            the boSystem to set
	 */
	public void setBoSystem(String boSystem) {
		this.boSystem = boSystem;
	}

	/**
	 * @return the processingOrg
	 */
	public String getProcessingOrg() {
		return this.processingOrg;
	}

	/**
	 * @param processingOrg
	 *            the processingOrg to set
	 */
	public void setProcessingOrg(String processingOrg) {
		this.processingOrg = processingOrg;
	}

	/**
	 * @return the portfolio
	 */
	public String getPortfolio() {
		return this.portfolio;
	}

	/**
	 * @param portfolio
	 *            the portfolio to set
	 */
	public void setPortfolio(String portfolio) {
		this.portfolio = portfolio;
	}

	/**
	 * @return the isin
	 */
	public String getIsin() {
		return this.isin;
	}

	/**
	 * @param isin
	 *            the isin to set
	 */
	public void setIsin(String isin) {
		this.isin = isin;
	}

	/**
	 * @return the securityDesc
	 */
	public String getSecurityDesc() {
		return this.securityDesc;
	}

	/**
	 * @param securityDesc
	 *            the securityDesc to set
	 */
	public void setSecurityDesc(String securityDesc) {
		this.securityDesc = securityDesc;
	}

	/**
	 * @return the valueDate
	 */
	public String getValueDate() {
		return this.valueDate;
	}

	/**
	 * @param valueDate
	 *            the valueDate to set
	 */
	public void setValueDate(String valueDate) {
		this.valueDate = valueDate;
	}

	/**
	 * @return the qtyNom
	 */
	public String getQtyNom() {
		return this.qtyNom;
	}

	/**
	 * @param qtyNom
	 *            the qtyNom to set
	 */
	public void setQtyNom(String qtyNom) {
		this.qtyNom = qtyNom;
	}

	/**
	 * @return the qtyNominalSec
	 */
	public String getQtyNominalSec() {
		return this.qtyNominalSec;
	}

	/**
	 * @param qtyNominalSec
	 *            the qtyNominalSec to set
	 */
	public void setQtyNominalSec(String qtyNominalSec) {
		this.qtyNominalSec = qtyNominalSec;
	}

	/**
	 * @return the qtyNominalSecCcy
	 */
	public String getQtyNominalSecCcy() {
		return this.qtyNominalSecCcy;
	}

	/**
	 * @param qtyNominalSecCcy
	 *            the qtyNominalSecCcy to set
	 */
	public void setQtyNominalSecCcy(String qtyNominalSecCcy) {
		this.qtyNominalSecCcy = qtyNominalSecCcy;
	}

	/**
	 * @return the posStatus
	 */
	public String getPosStatus() {
		return this.posStatus;
	}

	/**
	 * @param posStatus
	 *            the posStatus to set
	 */
	public void setPosStatus(String posStatus) {
		this.posStatus = posStatus;
	}

	/**
	 * @return the posType
	 */
	public String getPosType() {
		return this.posType;
	}

	/**
	 * @param posType
	 *            the posType to set
	 */
	public void setPosType(String posType) {
		this.posType = posType;
	}

	/**
	 * @return the reusable
	 */
	public String getReusable() {
		return this.reusable;
	}

	/**
	 * @param reusable
	 *            the reusable to set
	 */
	public void setReusable(String reusable) {
		this.reusable = reusable;
	}

	/**
	 * @return the custodian
	 */
	public String getCustodian() {
		return this.custodian;
	}

	/**
	 * @param custodian
	 *            the custodian to set
	 */
	public void setCustodian(String custodian) {
		this.custodian = custodian;
	}

	/**
	 * @return the account
	 */
	public String getAccount() {
		return this.account;
	}

	/**
	 * @param account
	 *            the account to set
	 */
	public void setAccount(String account) {
		this.account = account;
	}

}
