package calypsox.tk.report;

public class AcuerdoIsinItem {

    private String cpty;
    private String isin;
    private String acuerdo;
    private String ccy;

    /**
     * @return the isin
     */
    public String getIsin() {
        return this.isin;
    }

    /**
     * @param isin the isin to set
     */
    @SuppressWarnings("unused")
    public void setIsin(String isin) {
        this.isin = isin;
    }

    /**
     * @return the ccy
     */
    public String getCcy() {
        return this.ccy;
    }

    /**
     * @param ccy the ccy to set
     */
    @SuppressWarnings("unused")
    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    /**
     * @return the cpty
     */
    public String getCpty() {
        return this.cpty;
    }

    /**
     * @param cpty the cpty to set
     */
    @SuppressWarnings("unused")
    public void setCpty(String cpty) {
        this.ccy = cpty;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AcuerdoIsinItem other = (AcuerdoIsinItem) obj;
        if (this.ccy == null) {
            if (other.ccy != null) {
                return false;
            }
        } else if (!this.ccy.equals(other.ccy)) {
            return false;
        }
        if (this.acuerdo == null) {
            if (other.acuerdo != null) {
                return false;
            }
        } else if (!this.acuerdo.equals(other.acuerdo)) {
            return false;
        }
        if (this.isin == null) {
            if (other.isin != null) {
                return false;
            }
        } else if (!this.isin.equals(other.isin)) {
            return false;
        }
        return true;
    }

    /**
     * @return the cesta
     */
    public String getAcuerdo() {
        return this.acuerdo;
    }

    /**
     * @param cesta
     */
    @SuppressWarnings("unused")
    public void setAcuerdo(String acuerdo) {
        this.acuerdo = acuerdo;
    }

    public AcuerdoIsinItem(String isin, String acuerdo, String ccy, String cpty) {
        super();
        this.isin = isin;
        this.acuerdo = acuerdo;
        this.ccy = ccy;
        this.cpty = cpty;
    }
}
