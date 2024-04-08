package calypsox.tk.report;

import calypsox.tk.report.extracontable.MICExtracontableBean;
import calypsox.tk.report.extracontable.MICExtracontableFields;
import com.calypso.tk.report.BOPositionReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;


/**
 * @author aalonsop
 * Extracontable report
 */
public class PositionBasedMICExtracontableReportStyle extends BOPositionReportStyle {


    public static final String CENTRO = MICExtracontableFields.CENTRO.name();
    public static final String CDISIN = MICExtracontableFields.CDISIN.name();
    public static final String EMPRECON = MICExtracontableFields.EMPRECON.name();
    public static final String CDSECTOR = MICExtracontableFields.CDSECTOR.name();
    public static final String IDTIPOPE = MICExtracontableFields.IDTIPOPE.name();
    public static final String IDPERGRU = MICExtracontableFields.IDPERGRU.name();
    public static final String MOCONTRV = MICExtracontableFields.MOCONTRV.name();
    public static final String NUCONTRA = MICExtracontableFields.NUCONTRA.name();
    public static final String NUMDGODG = MICExtracontableFields.NUMDGODG.name();
    public static final String IDCOBCON = MICExtracontableFields.IDCOBCON.name();
    public static final String CDPAIEMI = MICExtracontableFields.CDPAIEMI.name();
    public static final String CDPRODUC = MICExtracontableFields.CDPRODUC.name();
    public static final String IDSUCACO = MICExtracontableFields.IDSUCACO.name();
    public static final String MOCTACTO = MICExtracontableFields.MOCTACTO.name();
    public static final String CDREINGR = MICExtracontableFields.CDREINGR.name();
    public static final String CDGLSCON = MICExtracontableFields.CDGLSCON.name();
    public static final String CDGLSEMI = MICExtracontableFields.CDGLSEMI.name();
    public static final String CDGLSENT = MICExtracontableFields.CDGLSENT.name();
    public static final String CDPAISCO = MICExtracontableFields.CDPAISCO.name();
    public static final String CODCONTR = MICExtracontableFields.CODCONTR.name();
    public static final String COEMISOR = MICExtracontableFields.COEMISOR.name();
    public static final String DESCOCON = MICExtracontableFields.DESCOCON.name();
    public static final String CDCIDEMI = MICExtracontableFields.CDCIDEMI.name();
    public static final String FECHACON = MICExtracontableFields.FECHACON.name();
    public static final String FECHAVEN = MICExtracontableFields.FECHAVEN.name();
    public static final String SECBECON = MICExtracontableFields.SECBECON.name();
    public static final String SECBEEMI = MICExtracontableFields.SECBEEMI.name();
    public static final String TIPOINTE = MICExtracontableFields.TIPOINTE.name();
    public static final String CDPORTF1 = MICExtracontableFields.CDPORTF1.name();
    public static final String CDTIPOP3 = MICExtracontableFields.CDTIPOP3.name();
    public static final String CDESTROP = MICExtracontableFields.CDESTROP.name();
    public static final String CDTIPCO3 = MICExtracontableFields.CDTIPCO3.name();
    public static final String CDSENTID = MICExtracontableFields.CDSENTID.name();
    public static final String IMNOMINA = MICExtracontableFields.IMNOMINA.name();
    public static final String CDJCONTR = MICExtracontableFields.CDJCONTR.name();
    public static final String CDNUOPFR = MICExtracontableFields.CDNUOPFR.name();
    public static final String CDNUOPBA = MICExtracontableFields.CDNUOPBA.name();
    public static final String CDNUEVBA = MICExtracontableFields.CDNUEVBA.name();
    public static final String FEVALO1 = MICExtracontableFields.FEVALO1.name();
    public static final String IINTRES = MICExtracontableFields.IINTRES.name();
    public static final String IMPPRINC = MICExtracontableFields.IMPPRINC.name();
    public static final String FEINIFIJ = MICExtracontableFields.FEINIFIJ.name();
    public static final String FEVENFIJ = MICExtracontableFields.FEVENFIJ.name();
    public static final String IDCARTER = MICExtracontableFields.IDCARTER.name();
    public static final String CDTIPOPC = MICExtracontableFields.CDTIPOPC.name();
    public static final String CDSUREFI = MICExtracontableFields.CDSUREFI.name();
    public static final String IDSUBORD = MICExtracontableFields.IDSUBORD.name();
    public static final String IDANOCTA = MICExtracontableFields.IDANOCTA.name();
    public static final String IDDERIMP = MICExtracontableFields.IDDERIMP.name();
    public static final String IDSEGREG = MICExtracontableFields.IDSEGREG.name();
    public static final String TCREFINT = MICExtracontableFields.TCREFINT.name();
    public static final String PRODUCT_ID = MICExtracontableFields.PRODUCT_ID.name();
    public static final String ACCOUNTINGRULE = MICExtracontableFields.ACCOUNTINGRULE.name();
    public static final String AUTOCARTERA = MICExtracontableFields.AUTOCARTERA.name();
    public static final String EQUITY_TYPE = MICExtracontableFields.EQUITY_TYPE.name();
    public static final String INTERNAL = MICExtracontableFields.INTERNAL.name();
    public static final String SUV = MICExtracontableFields.SUV.name();
    public static final String DIRECTION = MICExtracontableFields.DIRECTION.name();
    public static final String IDCARTER_ZERO = MICExtracontableFields.IDCARTER_ZERO.name();
    public static final String UNDERLYING_TYPE = MICExtracontableFields.UNDERLYING_TYPE.name();
    public static final String CONTRACT_TYPE = MICExtracontableFields.CONTRACT_TYPE.name();


    public static final String AGENTE = MICExtracontableFields.AGENTE.name();


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) {

        Object columnValue;
        MICExtracontableBean bean = row.getProperty(MICExtracontableReport.ROW_PROP_NAME);
        if (CENTRO.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIdCent();
        } else if (CDISIN.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodIsin();
        } else if (EMPRECON.equalsIgnoreCase(columnId)) {
            columnValue = bean.getEmprContrato();
        } else if (CDSECTOR.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodSector();
        } else if (IDTIPOPE.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndTipOper();
        } else if (IDPERGRU.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndPertGrupo();
        } else if (MOCONTRV.equalsIgnoreCase(columnId)) {
            columnValue = bean.getMonedaContravalor();
        } else if (NUCONTRA.equalsIgnoreCase(columnId)) {
            columnValue = bean.getNumContrato();
        } else if (NUMDGODG.equalsIgnoreCase(columnId)) {
            columnValue = bean.getNumOperacDGO();
        } else if (IDCOBCON.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndCoberCont();
        } else if (CDPAIEMI.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodPaisEmisor();
        } else if (CDPRODUC.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodProducto();
        } else if (IDSUCACO.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndSubCa();
        } else if (MOCTACTO.equalsIgnoreCase(columnId)) {
            columnValue = bean.getMonContr();
        } else if (CDREINGR.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodRefInGr();
        } else if (CDGLSCON.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodGLSContrapar();
        } else if (CDGLSEMI.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodGLSEmisor();
        } else if (CDGLSENT.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodGLSEntidad();
        } else if (CDPAISCO.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodPaisContrapar();
        } else if (CODCONTR.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodContrapar();
        } else if (COEMISOR.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodEmisor();
        } else if (DESCOCON.equalsIgnoreCase(columnId)) {
            columnValue = bean.getDescCodContrapar();
        } else if (CDCIDEMI.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodCifEmi();
        } else if (FECHACON.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFContrata();
        } else if (FECHAVEN.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFVenci();
        } else if (SECBECON.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSecBancoEspContrapar();
        } else if (SECBEEMI.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSecBancoEspEmisor();
        } else if (TIPOINTE.equalsIgnoreCase(columnId)) {
            columnValue = bean.getTipoInteres();
        } else if (CDPORTF1.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodPortf();
        } else if (CDTIPOP3.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodTipoOpe3();
        } else if (CDESTROP.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodEstrOpe();
        } else if (CDTIPCO3.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodTipoCobertura();
        } else if (CDSENTID.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodSentido();
        } else if (IMNOMINA.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImpNominal();
        } else if (CDJCONTR.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodJContrapar();
        } else if (CDNUOPFR.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodNumOpeFront();
        } else if (CDNUOPBA.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodNumOpeBack();
        } else if (CDNUEVBA.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodNumEventoBack();
        } else if (FEVALO1.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFvalor();
        } else if (IINTRES.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImpIntereses();
        } else if (IMPPRINC.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImpPrincipal();
        } else if (FEINIFIJ.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFIniFij();
        } else if (FEVENFIJ.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFVenciFij();
        } else if (IDCARTER.equals(columnId)) {
            columnValue = bean.getClaseContable();
        } else if (TCREFINT.equals(columnId)) {
            columnValue = bean.getTcRefInt();
        } else if (CDTIPOPC.equals(columnId)) {
            columnValue = bean.getTipOpcion();
        } else if (CDSUREFI.equals(columnId)) {
            columnValue = bean.getSubyRF();
        } else if (IDSUBORD.equals(columnId)) {
            columnValue = bean.getIndSubordi();
        } else if (IDANOCTA.equals(columnId)) {
            columnValue = bean.getIndAnotCuenta();
        } else if (IDDERIMP.equals(columnId)) {
            columnValue = bean.getIndDerivativeImp();
        } else if (IDSEGREG.equals(columnId)) {
            columnValue = bean.getIndSegregation();
        } else if (UNDERLYING_TYPE.equals(columnId)) {
            columnValue = bean.getUnderlyingType();
        } else if (CONTRACT_TYPE.equals(columnId)) {
            columnValue = bean.getContractType();
        } else if (ACCOUNTINGRULE.equals(columnId)) {
            columnValue = bean.getAccountingRule();
        } else if (AUTOCARTERA.equals(columnId)) {
            columnValue = bean.getAutoCartera();
        } else if (PRODUCT_ID.equals(columnId)) {
            columnValue = bean.getProductId();
        } else if (DIRECTION.equals(columnId)) {
            columnValue = bean.getDirection();
        } else if (SUV.equals(columnId)) {
            columnValue = bean.getSuv();
        } else if (INTERNAL.equals(columnId)) {
            columnValue = bean.getInternal();
        }else if (EQUITY_TYPE.equals(columnId)) {
            columnValue = bean.getEquityType();
        }else if (AGENTE.equals(columnId)) {
            columnValue = bean.getAgente();
        } else {
            columnValue = super.getColumnValue(row, columnId, errors);
        }

        if (columnId.equals(IDCARTER_ZERO)) {
            columnValue = "0";
        }

        return columnValue;
    }

}
