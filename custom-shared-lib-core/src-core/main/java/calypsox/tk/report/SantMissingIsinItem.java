package calypsox.tk.report;

//Project: MISSING_ISIN

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 * @author Carlos Humberto Cejudo Bermejo <c.cejudo.bermejo@accenture.com >
 *
 */
public class SantMissingIsinItem {

    private String isin;
    private String agent;
    private String cpShortName;
    private String currentDate;
    private String contractType;
    private String csdType;

    public String getCsdType() {
		return csdType;
	}

	public void setCsdType(String csdType) {
		this.csdType = csdType;
	}

	public String getContractType() {
		return contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}


	/**
     * @return ISIN
     */
    public String getIsin() {
        return isin;
    }

    /**
     * @param isin
     *            - ISIN
     */
    public void setIsin(String isin) {
        this.isin = isin;
    }

    /**
     * @return Agent
     */
    public String getAgent() {
        return agent;
    }

    /**
     * @param agent
     *            - Agent
     */
    public void setAgent(String agent) {
        this.agent = agent;
    }

    /**
     * @return Cp short name
     */
    public String getCpShortName() {
        return cpShortName;
    }

    /**
     * @param cpShortName
     *            - Cp Short Name
     */
    public void setCpShortName(String cpShortName) {
        this.cpShortName = cpShortName;
    }

    /**
     * @return - Current Date
     */
    public String getCurrentDate() {
        return currentDate;
    }

    /**
     * @param currentDate
     *            - Current Date
     */
    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

}
