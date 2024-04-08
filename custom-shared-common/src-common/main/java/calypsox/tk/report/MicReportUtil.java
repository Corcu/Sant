package calypsox.tk.report;

import calypsox.tk.report.extracontable.MICExtracontableField;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

import java.util.HashMap;

@SuppressWarnings("rawtypes")
public class MicReportUtil {

	public static final String CENTRO = "CENTRO";
	public static final String CDISIN = "CDISIN";
	public static final String EMPRECON = "EMPRECON";
	public static final String CDSECTOR = "CDSECTOR";
	public static final String IDTIPOPE = "IDTIPOPE";
	public static final String IDPERGRU = "IDPERGRU";
	public static final String MOCONTRV = "MOCONTRV";
	public static final String NUCONTRA = "NUCONTRA";
	public static final String NUMDGODG = "NUMDGODG";
	public static final String IDCOBCON = "IDCOBCON";
	public static final String CDPAIEMI = "CDPAIEMI";
	public static final String CDPRODUC = "CDPRODUC";
	public static final String CDPRODUC_INTERNAL = "CDPRODUC_INTERNAL";
	public static final String IDSUCACO = "IDSUCACO";
	public static final String IDSUCACO_INTERNAL = "IDSUCACO_INTERNAL";
	public static final String MOCTACTO = "MOCTACTO";
	public static final String CDREINGR = "CDREINGR";
	public static final String CDGLSCON = "CDGLSCON";
	public static final String CDGLSEMI = "CDGLSEMI";
	public static final String CDGLSENT = "CDGLSENT";
	public static final String CDPAISCO = "CDPAISCO";
	public static final String CODCONTR = "CODCONTR";
	public static final String COEMISOR = "COEMISOR";
	public static final String DESCOCON = "DESCOCON";
	public static final String CDCIDEMI = "CDCIDEMI";
	public static final String FECHACON = "FECHACON";
	public static final String FECHAVEN = "FECHAVEN";
	public static final String SECBECON = "SECBECON";
	public static final String SECBEEMI = "SECBEEMI";
	public static final String TIPOINTE = "TIPOINTE";
	public static final String CDPORTF1 = "CDPORTF1";
	public static final String CDPORTF1_EMPTY = "CDPORTF1_EMPTY";
	public static final String CDTIPOP3 = "CDTIPOP3";
	public static final String CDESTROP = "CDESTROP";
	public static final String CDTIPCO3 = "CDTIPCO3";
	public static final String CDSENTID = "CDSENTID";
	public static final String IMNOMINA = "IMNOMINA";
	public static final String CDJCONTR = "CDJCONTR";
	public static final String CDNUOPFR = "CDNUOPFR";
	public static final String CDNUOPBA = "CDNUOPBA";
	public static final String CDNUEVBA = "CDNUEVBA";
	public static final String FEVALO1  = "FEVALO1 ";
	public static final String IINTRES = "I-INTRES";
	public static final String IMPPRINC = "IMPPRINC";
	public static final String FEINIFIJ = "FEINIFIJ";
	public static final String FEVENFIJ = "FEVENFIJ";
	public static final String IDCARTER = "IDCARTER";
	public static final String IDCARTER_ZERO = "IDCARTER_ZERO";
	public static final String CDTIPOPC = "CDTIPOPC";
	public static final String CDSUREFI = "CDSUREFI";
	public static final String IDSUBORD = "IDSUBORD";
	public static final String IDANOCTA = "IDANOCTA";
	public static final String IDDERIMP = "IDDERIMP";
	public static final String IDSEGREG = "IDSEGREG";
	public static final String TCREFINT = "TCREFINT";
	public static final String PRODUCT_ID = "PRODUCT_ID";
	public static final String INTERNAL = "INTERNAL";
	public static final String INTERNAL_Y = "INTERNAL_Y";
	public static final String INTERNAL_N = "INTERNAL_N";
	public static final String AGENTE = "AGENTE";
	public static final String AUTOCARTERA = "AUTOCARTERA";
	public static final String AUTOCARTERA_SI = "AUTOCARTERA_SI";
	public static final String AUTOCARTERA_NO ="AUTOCARTERA_NO";
	public static final String EQUITY_TYPE = "EQUITY_TYPE";
	public static final String DIRECTION = "DIRECTION";
	public static final String ACCOUNTINGRULE = "ACCOUNTINGRULE";
	public static final String ACCOUNTINGRULE_REAL = "ACCOUNTINGRULE_REAL";
	public static final String ACCOUNTINGRULE_GESTION = "ACCOUNTINGRULE_GESTION";
	public static final String ACCOUNT_PROCESSING_ORG = "Account.Processing Org";

	public static final String CNST_BOOK_NAME = "ESSR_REPOSMM";

	public static final String ISSUE_TYPE = "ISSUE_TYPE";

	protected static HashMap<String, MICExtracontableField<?>> micFieldDescriptor = new HashMap<String, MICExtracontableField<?>>();

	static {
		micFieldDescriptor.put(CENTRO  ,new MICExtracontableField(4));
		micFieldDescriptor.put(CDISIN  ,new MICExtracontableField(12));
		micFieldDescriptor.put(EMPRECON,new MICExtracontableField(4));
		micFieldDescriptor.put(CDSECTOR,new MICExtracontableField(3));
		micFieldDescriptor.put(IDTIPOPE,new MICExtracontableField(1));
		micFieldDescriptor.put(IDPERGRU,new MICExtracontableField(9));
		micFieldDescriptor.put(MOCONTRV,new MICExtracontableField(3));
		micFieldDescriptor.put(NUCONTRA,new MICExtracontableField(7));
		micFieldDescriptor.put(NUMDGODG,new MICExtracontableField(8));
		micFieldDescriptor.put(IDCOBCON,new MICExtracontableField(3));
		micFieldDescriptor.put(CDPAIEMI,new MICExtracontableField(3));
		micFieldDescriptor.put(CDPRODUC,new MICExtracontableField(3));
		micFieldDescriptor.put(CDPRODUC_INTERNAL,new MICExtracontableField(3));
		micFieldDescriptor.put(IDSUCACO,new MICExtracontableField(3));
		micFieldDescriptor.put(IDSUCACO_INTERNAL,new MICExtracontableField(3));
		micFieldDescriptor.put(MOCTACTO,new MICExtracontableField(3));
		micFieldDescriptor.put(CDREINGR,new MICExtracontableField(50));
		micFieldDescriptor.put(CDGLSCON,new MICExtracontableField(11));
		micFieldDescriptor.put(CDGLSEMI,new MICExtracontableField(6));
		micFieldDescriptor.put(CDGLSENT,new MICExtracontableField(11));
		micFieldDescriptor.put(CDPAISCO,new MICExtracontableField(2));
		micFieldDescriptor.put(CODCONTR,new MICExtracontableField(11));
		micFieldDescriptor.put(COEMISOR,new MICExtracontableField(4));
		micFieldDescriptor.put(DESCOCON,new MICExtracontableField(35));
		micFieldDescriptor.put(CDCIDEMI,new MICExtracontableField(10));
		micFieldDescriptor.put(FECHACON,new MICExtracontableField(10));
		micFieldDescriptor.put(FECHAVEN,new MICExtracontableField(10));
		micFieldDescriptor.put(SECBECON,new MICExtracontableField(3));
		micFieldDescriptor.put(SECBEEMI,new MICExtracontableField(3));
		micFieldDescriptor.put(TIPOINTE,new MICExtracontableField(15));
		micFieldDescriptor.put(CDPORTF1,new MICExtracontableField(15));
		micFieldDescriptor.put(CDPORTF1_EMPTY,new MICExtracontableField(15));
		micFieldDescriptor.put(CDTIPOP3,new MICExtracontableField(3));
		micFieldDescriptor.put(CDESTROP,new MICExtracontableField(5));
		micFieldDescriptor.put(CDTIPCO3,new MICExtracontableField(15));
		micFieldDescriptor.put(CDSENTID,new MICExtracontableField(1));
		micFieldDescriptor.put(IMNOMINA,new MICExtracontableField(17));
		micFieldDescriptor.put(CDJCONTR,new MICExtracontableField(11));
		micFieldDescriptor.put(CDNUOPFR,new MICExtracontableField(30));
		micFieldDescriptor.put(CDNUOPBA,new MICExtracontableField(30));
		micFieldDescriptor.put(CDNUEVBA,new MICExtracontableField(30));
		micFieldDescriptor.put(FEVALO1 ,new MICExtracontableField(10));
		micFieldDescriptor.put(IINTRES, new MICExtracontableField(17));
		micFieldDescriptor.put(IMPPRINC,new MICExtracontableField(17));
		micFieldDescriptor.put(FEINIFIJ,new MICExtracontableField(10));
		micFieldDescriptor.put(FEVENFIJ,new MICExtracontableField(10));
		micFieldDescriptor.put(IDCARTER,new MICExtracontableField(1));
		micFieldDescriptor.put(IDCARTER_ZERO,new MICExtracontableField(1));
		micFieldDescriptor.put(CDTIPOPC,new MICExtracontableField(1));
		micFieldDescriptor.put(CDSUREFI,new MICExtracontableField(10));
		micFieldDescriptor.put(IDSUBORD,new MICExtracontableField(1));
		micFieldDescriptor.put(IDANOCTA,new MICExtracontableField(1));
		micFieldDescriptor.put(IDDERIMP,new MICExtracontableField(1));
		micFieldDescriptor.put(IDSEGREG,new MICExtracontableField(3));
		micFieldDescriptor.put(TCREFINT,new MICExtracontableField(21));
		micFieldDescriptor.put(EQUITY_TYPE,new MICExtracontableField(14));
		micFieldDescriptor.put(ACCOUNTINGRULE,new MICExtracontableField(22));
		micFieldDescriptor.put(ACCOUNTINGRULE_REAL,new MICExtracontableField(22));
		micFieldDescriptor.put(ACCOUNTINGRULE_GESTION,new MICExtracontableField(22));
		micFieldDescriptor.put(PRODUCT_ID,new MICExtracontableField(16));
		micFieldDescriptor.put(AUTOCARTERA,new MICExtracontableField(2));
		micFieldDescriptor.put(AUTOCARTERA_SI,new MICExtracontableField(2));
		micFieldDescriptor.put(AUTOCARTERA_NO,new MICExtracontableField(2));
		micFieldDescriptor.put(INTERNAL,new MICExtracontableField(1));
		micFieldDescriptor.put(INTERNAL_Y,new MICExtracontableField(1));
		micFieldDescriptor.put(INTERNAL_N,new MICExtracontableField(1));
		micFieldDescriptor.put(AGENTE,new MICExtracontableField(11));
		micFieldDescriptor.put(DIRECTION,new MICExtracontableField(5));
		micFieldDescriptor.put(ACCOUNT_PROCESSING_ORG,new MICExtracontableField(11));
		micFieldDescriptor.put(ISSUE_TYPE,new MICExtracontableField(14));
	}

	@SuppressWarnings("unchecked")
	protected synchronized static Object formatColumnValue(String columnName, Object o) {
		MICExtracontableField field = micFieldDescriptor.get(columnName);
		if(field==null) {
			return o;
		}
		field.setContent(o);
		return field.getContent();
	}

	public static String getIDSUCACO(Equity product, boolean isInternal) {
		String alias = getAlias(product, isInternal);

		if(alias.equals("RVCACOAONE")) return "522";
		if(alias.equals("RVCAINAONE")) return "523";
		if(alias.equals("RVCACODSNE")) return "524";
		if(alias.equals("RVCAINDSNE")) return "525";
		if(alias.equals("RVCACOPPNE")) return "526";
		if(alias.equals("RVCAINPPNE")) return "527";
		if(alias.equals("RVCACOPPAM")) return "528";
		if(alias.equals("RVCAINPPAM")) return "529";
		if(alias.equals("RVCACOEGAM")) return "530";
		if(alias.equals("RVCAINEGAM")) return "531";
		if(alias.equals("RVCACOPFAM")) return "532";
		if(alias.equals("RVCAINPFAM")) return "533";
		if(alias.equals("RVCACOCINE")) return "534";
		if(alias.equals("RVCAINCINE")) return "535";
		if(alias.equals("RVCACOADNE")) return "536";
		if(alias.equals("RVCAINADNE")) return "537";
		if(alias.equals("RVAUCOAONE")) return "522";
		if(alias.equals("RVAUINAONE")) return "523";
		if(alias.equals("RVAUCODSNE")) return "524";
		if(alias.equals("RVCACINDSNE")) return "525";
		if(alias.equals("RVCACOD2NE")) return "538";
		if(alias.equals("RVCAIND2NE")) return "539";
		if(alias.equals("RVCACOAOOV")) return "522";
		if(alias.equals("RVCAINAOOV")) return "523";
		if(alias.equals("RVCACODSOV")) return "524";
		if(alias.equals("RVCAINDSOV")) return "525";
		if(alias.equals("RVCACOPPOV")) return "526";
		if(alias.equals("RVCAINPPOV")) return "527";
		if(alias.equals("RVCACOPFOV")) return "532";
		if(alias.equals("RVCAINPFOV")) return "533";
		if(alias.equals("RVCACOCIOV")) return "534";
		if(alias.equals("RVCAINCIOV")) return "535";
		if(alias.equals("RVCACOADOV")) return "536";
		if(alias.equals("RVCAINADOV")) return "537";
		if(alias.equals("RVAUCOAOOV")) return "522";
		if(alias.equals("RVAUINAOOV")) return "523";
		if(alias.equals("RVAUCODSOV")) return "524";
		if(alias.equals("RVCACINDSOV")) return "525";
		if(alias.equals("RVCACOD2OV")) return "538";
		if(alias.equals("RVCAIND2OV")) return "539";
        if(alias.equals("RVCACOETNE")) return "568";
        if(alias.equals("RVCAINETNE")) return "569";
        if(alias.equals("RVCAINV2NE")) return "603";
        if(alias.equals("RVCACOV2NE")) return "602";
        if(alias.equals("RVCAINV2OV")) return "603";
        if(alias.equals("RVCACOV2OV")) return "602";

        return "";
	}

	public static String getCDPRODUC(Equity product, boolean isInternal) {

		String alias = getAlias(product, isInternal);

		if(alias.equals("RVCACOAONE") ||
				alias.equals("RVCAINAONE") ||
				alias.equals("RVCACODSNE") ||
				alias.equals("RVCAINDSNE") ||
				alias.equals("RVCACOPPNE") ||
				alias.equals("RVCAINPPNE") ||
				alias.equals("RVCACOPPAM") ||
				alias.equals("RVCAINPPAM") ||
				alias.equals("RVCACOEGAM") ||
				alias.equals("RVCAINEGAM") ||
				alias.equals("RVCACOPFAM") ||
				alias.equals("RVCAINPFAM") ||
				alias.equals("RVCACOCINE") ||
				alias.equals("RVCAINCINE") ||
				alias.equals("RVCACOADNE") ||
				alias.equals("RVCAINADNE") ||
				alias.equals("RVCAIND2NE") ||
				alias.equals("RVCACOD2NE") ||
				alias.equals("RVCACOAOOV") ||
				alias.equals("RVCAINAOOV") ||
				alias.equals("RVCACODSOV") ||
				alias.equals("RVCAINDSOV") ||
				alias.equals("RVCACOPPOV") ||
				alias.equals("RVCAINPPOV") ||
				alias.equals("RVCACOPFOV") ||
				alias.equals("RVCAINPFOV") ||
				alias.equals("RVCACOCIOV") ||
				alias.equals("RVCAINCIOV") ||
				alias.equals("RVCACOADOV") ||
				alias.equals("RVCAINADOV") ||
				alias.equals("RVCAIND2OV") ||
				alias.equals("RVCACOD2OV") ||
                alias.equals("RVCAINETNE") ||
                alias.equals("RVCACOETNE") ||
                alias.equals("RVCAINV2NE") ||
                alias.equals("RVCACOV2NE") ||
                alias.equals("RVCAINV2OV") ||
                alias.equals("RVCACOV2OV"))	{
			return "408";
		}

		if(alias.equals("RVAUCOAONE")||
				alias.equals("RVAUINAONE")||
				alias.equals("RVAUCODSNE")||
				alias.equals("RVCACINDSNE") ||
				alias.equals("RVAUCOAOOV")||
				alias.equals("RVAUINAOOV")||
				alias.equals("RVAUCODSOV")||
				alias.equals("RVCACINDSOV"))
			return "984";

		return "";

	}

	public static String getAlias(Equity product, boolean isInternal){
		String equityType = product.getSecCode("EQUITY_TYPE")!=null ? product.getSecCode("EQUITY_TYPE") : "";
		String accountingLink = getAccountingLink(product);
		String issuerName = "";
		if(product.getIssuer()!=null)
			issuerName = product.getIssuer().getCode()!=null ? product.getIssuer().getCode() : "";
		String result = "";
		if("Negociacion".equalsIgnoreCase(accountingLink)) {
			if ("CS".equalsIgnoreCase(equityType)) {
				if ("BSTE".equalsIgnoreCase(issuerName)){
					if (isInternal) {
						result = "RVAUINAONE";
					} else {
						result = "RVAUCOAONE";
					}
				} else{
					if (isInternal) {
						result = "RVCAINAONE";
					} else {
						result = "RVCACOAONE";
					}
				}
			} else if ("DERSUS".equalsIgnoreCase(equityType)) {
				if ("BSTE".equalsIgnoreCase(issuerName)){
					if (isInternal) {
						result = "RVCACINDSNE";
					} else {
						result = "RVAUCODSNE";
					}
				} else{
					if (isInternal) {
						result = "RVCAINDSNE";
					} else {
						result = "RVCACODSNE";
					}
				}
			} else if ("PS".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINPPNE";
				} else {
					result = "RVCACOPPNE";
				}
			} else if ("INSW".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINCINE";
				} else {
					result = "RVCACOCINE";
				}
			} else if ("ADR".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINADNE";
				} else {
					result = "RVCACOADNE";
				}
			} else if ("PFI".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINPFAM";
				} else {
					result = "RVCACOPFAM";
				}
			} else if ("CO2".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAIND2NE";
				} else {
					result = "RVCACOD2NE";
				}
			} else if ("VCO2".equalsIgnoreCase(equityType)) {
                if (isInternal) {
                    result = "RVCAINV2NE";
                } else {
                    result = "RVCACOV2NE";
                }
            } else if ("ETF".equalsIgnoreCase(equityType)) {
                if (isInternal) {
                    result = "RVCAINETNE";
                } else {
                    result = "RVCACOETNE";
                }
			}
		} else if ("Inversion crediticia".equalsIgnoreCase(accountingLink)) {
			if ("PS".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINPPAM";
				} else {
					result = "RVCACOPPAM";
				}
			} else if ("PEGROP".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINEGAM";
				} else {
					result = "RVCACOEGAM";
				}
			}
		} else if ("Otros a valor razonable".equalsIgnoreCase(accountingLink)) {
			if ("CS".equalsIgnoreCase(equityType)) {
				if ("BSTE".equalsIgnoreCase(issuerName)){
					if (isInternal) {
						result = "RVAUINAOOV";
					} else {
						result = "RVAUCOAOOV";
					}
				} else{
					if (isInternal) {
						result = "RVCAINAOOV";
					} else {
						result = "RVCACOAOOV";
					}
				}
			} else if ("DERSUS".equalsIgnoreCase(equityType)) {
				if ("BSTE".equalsIgnoreCase(issuerName)){
					if (isInternal) {
						result = "RVCACINDSOV";
					} else {
						result = "RVAUCODSOV";
					}
				} else{
					if (isInternal) {
						result = "RVCAINDSOV";
					} else {
						result = "RVCACODSOV";
					}
				}
			} else if ("PS".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINPPOV";
				} else {
					result = "RVCACOPPOV";
				}
			} else if ("INSW".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINCIOV";
				} else {
					result = "RVCACOCIOV";
				}
			} else if ("ADR".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINADOV";
				} else {
					result = "RVCACOADOV";
				}
			} else if ("PFI".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAINPFOV";
				} else {
					result = "RVCACOPFOV";
				}
			} else if ("CO2".equalsIgnoreCase(equityType)) {
				if (isInternal) {
					result = "RVCAIND2OV";
				} else {
					result = "RVCACOD2OV";
				}
			} else if ("VCO2".equalsIgnoreCase(equityType)) {
                if (isInternal) {
                    result = "RVCAINV2OV";
                } else {
                    result = "RVCACOV2OV";
                }
            }
		}

		return result;
	}

	public static String getAccountingLink(Equity product){
		String bookName = product.getSecCode("SEC_PORTFOLIO")!=null ? product.getSecCode("SEC_PORTFOLIO") : CNST_BOOK_NAME;

		Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
		if(book == null) {
			return null;
		}
		String accountingLink = book.getAccountingBook().getName();

		return accountingLink;
	}

}
