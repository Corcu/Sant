package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.util.*;

public class EquityMicCarteraReportStyle extends EquityMisPlusCarteraReportStyle {


	public static final String CENTRO = MicReportUtil.CENTRO;
	public static final String CDISIN = MicReportUtil.CDISIN;
	public static final String EMPRECON = MicReportUtil.EMPRECON;
	public static final String CDSECTOR = MicReportUtil.CDSECTOR;
	public static final String IDTIPOPE = MicReportUtil.IDTIPOPE;
	public static final String IDPERGRU = MicReportUtil.IDPERGRU;
	public static final String MOCONTRV = MicReportUtil.MOCONTRV;
	public static final String NUCONTRA = MicReportUtil.NUCONTRA;
	public static final String NUMDGODG = MicReportUtil.NUMDGODG;
	public static final String IDCOBCON = MicReportUtil.IDCOBCON;
	public static final String CDPAIEMI = MicReportUtil.CDPAIEMI;
	public static final String CDPRODUC = MicReportUtil.CDPRODUC;
	public static final String IDSUCACO = MicReportUtil.IDSUCACO;
	public static final String CDPRODUC_INTERNAL = MicReportUtil.CDPRODUC_INTERNAL;
	public static final String IDSUCACO_INTERNAL = MicReportUtil.IDSUCACO_INTERNAL;
	public static final String MOCTACTO = MicReportUtil.MOCTACTO;
	public static final String CDREINGR = MicReportUtil.CDREINGR;
	public static final String CDGLSCON = MicReportUtil.CDGLSCON;
	public static final String CDGLSEMI = MicReportUtil.CDGLSEMI;
	public static final String CDGLSENT = MicReportUtil.CDGLSENT;
	public static final String CDPAISCO = MicReportUtil.CDPAISCO;
	public static final String CODCONTR = MicReportUtil.CODCONTR;
	public static final String COEMISOR = MicReportUtil.COEMISOR;
	public static final String DESCOCON = MicReportUtil.DESCOCON;
	public static final String CDCIDEMI = MicReportUtil.CDCIDEMI;
	public static final String FECHACON = MicReportUtil.FECHACON;
	public static final String FECHAVEN = MicReportUtil.FECHAVEN;
	public static final String SECBECON = MicReportUtil.SECBECON;
	public static final String SECBEEMI = MicReportUtil.SECBEEMI;
	public static final String TIPOINTE = MicReportUtil.TIPOINTE;
	public static final String CDPORTF1 = MicReportUtil.CDPORTF1;
	public static final String CDPORTF1_EMPTY = MicReportUtil.CDPORTF1_EMPTY;
	public static final String CDTIPOP3 = MicReportUtil.CDTIPOP3;
	public static final String CDESTROP = MicReportUtil.CDESTROP;
	public static final String CDTIPCO3 = MicReportUtil.CDTIPCO3;
	public static final String CDSENTID = MicReportUtil.CDSENTID;
	public static final String IMNOMINA = MicReportUtil.IMNOMINA;
	public static final String CDJCONTR = MicReportUtil.CDJCONTR;
	public static final String CDNUOPFR = MicReportUtil.CDNUOPFR;
	public static final String CDNUOPBA = MicReportUtil.CDNUOPBA;
	public static final String CDNUEVBA = MicReportUtil.CDNUEVBA;
	public static final String FEVALO1  = MicReportUtil.FEVALO1 ;
	public static final String IINTRES = MicReportUtil.IINTRES;
	public static final String IMPPRINC = MicReportUtil.IMPPRINC;
	public static final String FEINIFIJ = MicReportUtil.FEINIFIJ;
	public static final String FEVENFIJ = MicReportUtil.FEVENFIJ;
	public static final String IDCARTER = MicReportUtil.IDCARTER;
	public static final String CDTIPOPC = MicReportUtil.CDTIPOPC;
	public static final String CDSUREFI = MicReportUtil.CDSUREFI;
	public static final String IDSUBORD = MicReportUtil.IDSUBORD;
	public static final String IDANOCTA = MicReportUtil.IDANOCTA;
	public static final String IDDERIMP = MicReportUtil.IDDERIMP;
	public static final String IDSEGREG = MicReportUtil.IDSEGREG;
	public static final String TCREFINT = MicReportUtil.TCREFINT;
	public static final String PRODUCT_ID = MicReportUtil.PRODUCT_ID;
	public static final String AUTOCARTERA = MicReportUtil.AUTOCARTERA;
	public static final String EQUITY_TYPE = MicReportUtil.EQUITY_TYPE;
	public static final String DIRECTION = MicReportUtil.DIRECTION;
	public static final String ACCOUNTINGRULE_REAL = MicReportUtil.ACCOUNTINGRULE_REAL;
	public static final String ACCOUNTINGRULE_GESTION = MicReportUtil.ACCOUNTINGRULE_GESTION;
	private static final String SI = "SI";
	private static final String NO = "NO";
	private static final String Y = "Y";
	private static final String N = "N";
	public static final String IDCARTER_ZERO = MicReportUtil.IDCARTER_ZERO;

	public static final String AUTOCARTERA_SI = MicReportUtil.AUTOCARTERA_SI;
	public static final String AUTOCARTERA_NO = MicReportUtil.AUTOCARTERA_NO;
	public static final String INTERNAL_Y = MicReportUtil.INTERNAL_Y;
	public static final String INTERNAL_N = MicReportUtil.INTERNAL_N;

	public static final String AGENTE = MicReportUtil.AGENTE;

	public static final String BOND_SEC_CODE_REF_INTERNA = "REF_INTERNA";


	public static final ArrayList<String> emptyColumns = new ArrayList<String>();
	public static final HashMap<String,String> columnToColumn = new HashMap<String,String>();

	//dummy date
	static final String DUMMY_DATE = "2001/01/01";

	static {
		emptyColumns.add(CDPORTF1_EMPTY);
		emptyColumns.add(CDSECTOR);
		emptyColumns.add(IDTIPOPE);
		emptyColumns.add(IDPERGRU);
		emptyColumns.add(NUCONTRA);
		//emptyColumns.add(CDPRODUC);
		//emptyColumns.add(IDSUCACO);
		emptyColumns.add(CDREINGR);
		emptyColumns.add(CDGLSCON);
		emptyColumns.add(CDPAISCO);
		emptyColumns.add(CODCONTR);
		emptyColumns.add(COEMISOR);
		emptyColumns.add(DESCOCON);
		emptyColumns.add(SECBECON);
		emptyColumns.add(TIPOINTE);
		emptyColumns.add(CDTIPOP3);
		emptyColumns.add(CDTIPCO3);
		emptyColumns.add(CDTIPCO3);
		emptyColumns.add(CDNUOPFR);
		emptyColumns.add(CDNUOPBA);
		emptyColumns.add(CDNUEVBA);
		emptyColumns.add(FEVALO1 );
		emptyColumns.add(IINTRES);
		emptyColumns.add(IMPPRINC);
		emptyColumns.add(FEINIFIJ);
		emptyColumns.add(FEVENFIJ);
		emptyColumns.add(CDTIPOPC);
		emptyColumns.add(CDSUREFI);
		emptyColumns.add(IDSUBORD);
		emptyColumns.add(IDANOCTA);
		emptyColumns.add(IDDERIMP);
		emptyColumns.add(IDSEGREG);
		emptyColumns.add(AGENTE);
		emptyColumns.add(CDSENTID);
		emptyColumns.add(CDJCONTR);

		columnToColumn.put(CENTRO,ACCOUNTING_CENTER);
		columnToColumn.put(CDISIN,ISIN);
		columnToColumn.put(EMPRECON,ENTITY);
		columnToColumn.put(CDPAIEMI,ISSUERCOUNTRY);
		columnToColumn.put(MOCTACTO,CURRENCY);
		columnToColumn.put(CDGLSEMI,ISSUERGLS);
		columnToColumn.put(CDGLSENT,BRANCH);
		columnToColumn.put(SECBEEMI,ISSUERSECTOR);
		columnToColumn.put(CDPORTF1,PORTAFOLIO);
		columnToColumn.put(IMNOMINA,QUANTITY);
		columnToColumn.put(IMPPRINC,QUANTITY);
        columnToColumn.put(TCREFINT,ISIN);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	@SuppressWarnings({ "rawtypes" })
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		Object result = null;

		Product product = (Product)super.getColumnValue(row, CALYPSO_PRODUCT, errors);

		if(emptyColumns.contains(columnName))
			result = "";
		if(columnToColumn.containsKey(columnName)) {
			result = super.getColumnValue(row,columnToColumn.get(columnName),errors);
		}

		if (columnName.equals(IDCARTER)) {
			String bookName = (String)super.getColumnValue(row, "Book", errors);
			if(!Util.isEmpty(bookName)) {
				Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
				AccountingBook acctBook = book.getAccountingBook();
				if(acctBook != null) {
					String acctBookName = acctBook.getName();
					if(acctBookName.equals("Negociacion")) {
						result = "0";
					}
					else if(acctBookName.equals("Inversion crediticia")) {
						result = "2";
					}
					else if(acctBookName.equals("Otros a valor razonable")) {
						result = "3";
					}
				}
			}
			else
				result = "0";
		}

		if (columnName.equals(AUTOCARTERA)) {
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				result = le != null && le.getCode().equals("BSTE")? SI : NO;
			}
			else
				result = "";
		}
		if (columnName.equals(INTERNAL_Y)) {
			result = Y;
		}
		if (columnName.equals(INTERNAL_N)) {
			result = N;
		}

		if (columnName.equals(ACCOUNTINGRULE_REAL)) {
			result = "RV_Conta_Real_EOD";
		}

		if (columnName.equals(ACCOUNTINGRULE_GESTION)) {
			result = "RV_Conta_Gestion_EOD";
		}

		if (columnName.equals(IDCARTER_ZERO)) {
			result = "0";
		}

		if (columnName.equals(PRODUCT_ID)) {
			result = product.getId();
		}

		if (columnName.equals(CDPRODUC)) {
			if(product instanceof Equity)
				result = MicReportUtil.getCDPRODUC((Equity)product,false);
		}

		if (columnName.equals(IDSUCACO)) {
			if(product instanceof Equity)
				result = MicReportUtil.getIDSUCACO((Equity)product,false);
		}

		if (columnName.equals(CDPRODUC_INTERNAL)) {
			if(product instanceof Equity)
				result = MicReportUtil.getCDPRODUC((Equity)product,true);
		}

		if (columnName.equals(IDSUCACO_INTERNAL)) {
			if(product instanceof Equity)
				result = MicReportUtil.getIDSUCACO((Equity)product,true);
		}

		//if (columnName.equals(TCREFINT)) {
		//	if (product instanceof Equity) {
		//		result = product.getSecCode(BOND_SEC_CODE_REF_INTERNA);
		//	}
		//}

		if (columnName.equals(CDGLSCON)) {
			if (product instanceof Equity) {
				result = ((Equity) product).getIssuer().getCode();
			}
		}

		if (columnName.equals(CODCONTR)) {
			if (product instanceof Equity) {
				result = ((Equity) product).getIssuer().getExternalRef();
			}
		}

		if (columnName.equals(DESCOCON)) {
			if (product instanceof Equity) {
				result = ((Equity) product).getIssuer().getName();
			}
		}

        if (columnName.equals(CDCIDEMI)) {
            String cif =  "";
            if (product instanceof Equity) {
                Equity equity = (Equity) product;
                LegalEntity le = equity.getIssuer();
                if (le != null){
                    cif = getLegalEntityAttribute(le, "TAXID");
                }
            }
            result = !Util.isEmpty(cif) ? cif : "";
        }

        if (columnName.equals(CDESTROP)) {
            result = "ESP  ";
        }

		if (columnName.equals(FECHACON)) {
			return DUMMY_DATE;
		}
        
        if (columnName.equals(FECHAVEN)) {
            //Calendar calendar = Calendar.getInstance();
            //calendar.set(Calendar.DAY_OF_MONTH, 31);
            //calendar.set(Calendar.MONTH, 11);
            //calendar.set(Calendar.YEAR, 9999);
            //return JDate.valueOf(calendar.getTime());
            return "9999/12/31";
        }
        
		if (result == null )
			return MicReportUtil.formatColumnValue(columnName, super.getColumnValue(row, columnName, errors));
		else
			return MicReportUtil.formatColumnValue(columnName, result);
	}

	@Override
	public Object formatResult(Object o) {
		return o;
	}

    public String getLegalEntityAttribute(final LegalEntity le, final String att) {
        String rst = "";
        if (le != null) {
            final Collection<?> atts = le.getLegalEntityAttributes();
            // FIX in case a LE does NOT have attributes
            if (atts == null) {
                if (le != null) {
                    Log.error(this.getClass(), le.getName() + " does not have LE attributes configured");
                }
                return rst;
            }
            LegalEntityAttribute current;
            final Iterator<?> it = atts.iterator();

            while (it.hasNext() && (Util.isEmpty(rst))) {
                current = (LegalEntityAttribute) it.next();
                if (current.getAttributeType().equalsIgnoreCase(att)) {
                    rst = current.getAttributeValue();
                }
            }
        }
        return rst;
    }


}
