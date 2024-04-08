package calypsox.tk.anacredit.util;

/**
 *
 */
public class AnacreditBean {


    private String columnaA; //external reference to first file and J_MIN_PADRE to second file.
    private String columnaJMIN;


    public AnacreditBean() {
    }

    public AnacreditBean(String columnaA, String columnaJMIN) {
        this.columnaA = columnaA;
        this.columnaJMIN = columnaJMIN;
    }

    public String getColumnaA() {
        return columnaA;
    }

    public void setColumnaA(String columnaA) {
        this.columnaA = columnaA;
    }

    public String getcolumnaJMIN() {
        return columnaJMIN;
    }

    public void setcolumnaJMIN(String columnaJMIN) {
        this.columnaJMIN = columnaJMIN;
    }

    @Override
    public String toString() {
        return "AnacreditBean{" +
                "columnaA='" + columnaA + '\'' +
                ", columnaB='" + columnaJMIN + '\'' +
                '}';
    }
}
