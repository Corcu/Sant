package calypsox.tk.anacredit.util.acesscontrol;

/**
 *
 */
public class AnacreditACBean {

    private String _rfRv;
    private String _isin; //external reference to first file and J_MIN_PADRE to second file.
    private String _cotiza;
    private String _jerarquia;
    private int _recordNumber;

    public AnacreditACBean() {
    }

    public AnacreditACBean(String rfRv, int recNumber, String isin, String cotiza, String jerarquia) {
        this._rfRv = rfRv;
        this._recordNumber = recNumber;
        this._isin = isin;
        this._cotiza = cotiza;
        this._jerarquia = jerarquia;
    }
    public int getRecordNumber() {
        return _recordNumber;
    }
    public String getIsin() {
        return _isin;
    }
    public String getCotiza() {
        return _cotiza;
    }
    public String getJerarquia() {
        return _jerarquia;
    }

    public void setIsin(String isin) {
        this._isin  = isin;
    }
    public void setCotiza(String cotiza) {
        this._cotiza  = cotiza;
    }
    public void setJerarquia(String jerarquia) {
        this._jerarquia  = jerarquia;
    }

    @Override
    public String toString() {
        return "AnacreditBean{" +
                "type=" + _rfRv + '\'' +
                "recNumber=" + _recordNumber + '\'' +
                "isin='" + _isin + '\'' +
                ", cotiza='" + _cotiza + '\'' +
                ", jerarquia='" + _jerarquia + '\'' +
                '}';
    }
}
