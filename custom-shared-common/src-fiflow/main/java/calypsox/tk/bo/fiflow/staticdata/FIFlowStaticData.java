package calypsox.tk.bo.fiflow.staticdata;

/**
 * @author aalonsop
 */
public class FIFlowStaticData {

    /*
     * Msg attributes and keywords
     */
    public static final String GDISPONIBLE_RESPONSE_MSG_ATTR = "GDisponibleResponse";

    public static final String PARTENON_ID_KEYWRD_NAME = "PartenonGDisponibleID";
    public static final String FLOW_ID_KEYWRD_NAME = "GDisponibleFlowID";


    //London Branch
    public static final String LONDON_BRANCH_LE_CODE = "BDSD";


    //Codigos entidad
    public static final String MADRID_ENTITY_CODE = "0049";
    public static final String SLB_ENTITY_CODE = "0306";

    //Codigos centro
    public static final String MADRID_CENTER_CODE = "1999";
    public static final String SLB_CENTER_CODE = "1111";



    public static final String EMPTY_STRING = "";

    public enum FlowDirection {
        COMPRA(0),
        VENTA(1);

        int mappedValue;

        FlowDirection(int mappedValue) {
            this.mappedValue = mappedValue;
        }

        public int getMappedValue() {
            return mappedValue;
        }
    }

    private FIFlowStaticData() {
        //EMPTY HIDDEN CONSTRUCTOR
    }

    public enum PortfolioType {

        NEGOCIACION("NE"),
        DISPONIBLE_PARA_LA_VENTA("DV"),
        OTROS_ACTIVOS("OT"),
        INVENTARIO_TERCEROS("NOT_MAPPED"),
        INVERSION_A_VENCIMIENTO("IV"),
        INVERSION_CREDITICIA("IC"),
        OTROS_A_VALOR_RAZONABLE("OT"),
        DESIGNADOS_A_VALOR_RAZONABLE("DR");

        String mappedValue;

        PortfolioType(String mappedValue) {
            this.mappedValue = mappedValue;
        }

        public String getMappedValue() {
            return mappedValue;
        }

        public static String formatPortfolioType(String portType) {
            return portType.replace(' ', '_').toUpperCase();
        }
    }
}
