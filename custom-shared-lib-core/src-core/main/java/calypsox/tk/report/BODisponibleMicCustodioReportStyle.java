package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.util.*;

public class BODisponibleMicCustodioReportStyle extends BODisponibleSecurityPositionReportStyle{
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
    public static final String INTERNAL = MicReportUtil.INTERNAL;
    public static final String AUTOCARTERA = MicReportUtil.AUTOCARTERA;
    public static final String AGENTE = MicReportUtil.AGENTE;
    public static final String EQUITY_TYPE = MicReportUtil.EQUITY_TYPE;
    public static final String DIRECTION = MicReportUtil.DIRECTION;
    public static final String ACCOUNTINGRULE = MicReportUtil.ACCOUNTINGRULE;

    protected static final String SI = "SI";
    protected static final String NO = "NO";

    public static final String INV_PRODUCT = "INV_PRODUCT";

    public static final String BOND_SEC_CODE_REF_INTERNA = "REF_INTERNA";

    public static final ArrayList<String> emptyColumns = new ArrayList<String>();
    public static final HashMap<String,String> columnToColumn = new HashMap<String,String>();

    static {

        columnToColumn.put(CDISIN,"PRODUCT_CODE.ISIN");
        columnToColumn.put(MOCTACTO,"Currency");
        columnToColumn.put(EQUITY_TYPE,"PRODUCT_CODE.EQUITY_TYPE");
        columnToColumn.put(PRODUCT_ID,"Product Id");
        columnToColumn.put(AGENTE,"Agent");
        columnToColumn.put(TCREFINT,"PRODUCT_CODE.ISIN");
    }
    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnId, Vector errors)
            throws InvalidParameterException {

        Object result = null;

        if(emptyColumns.contains(columnId))
            result = "";
        if(columnToColumn.containsKey(columnId)) {
            result = super.getColumnValue(row,columnToColumn.get(columnId),errors);
        }

        Inventory inventory = row.getProperty(ReportRow.INVENTORY);
        LegalEntity issuer = Optional.ofNullable(row.getProperty(ISSUER)).filter(LegalEntity.class::isInstance).map(LegalEntity.class::cast).orElse(new LegalEntity());
        Product product = inventory.getProduct();
        String entity = BOCreUtils.getInstance().getEntity(inventory.getBook().getName());

        if (columnId.equals(CENTRO)) {
            if (product!=null){
                result = BOCreUtils.getInstance().getCentroContable(product, entity, false);
            } else
                result = "";
        } else if (columnId.equals(EMPRECON)) {
            result = BOCreUtils.getInstance().getEntityCod(entity, false);
        } else if (columnId.equals(CDPAIEMI)) {
            Country country = BOCache.getCountry(DSConnection.getDefault(), issuer.getCountry());
            if(country!=null){
                result = country.getISOCode();
            } else
                result = "";
        } else if (columnId.equals(CDGLSEMI)) {
            result = issuer.getCode();
        } else if (columnId.equals(CDGLSENT)) {
            String bookName = (String)super.getColumnValue(row, BOOK, errors);
            result = BOCreUtils.getInstance().getEntity(bookName);
        }
        else if (columnId.equals(CDCIDEMI)) {
            String cif =  getLegalEntityAttribute(issuer, "TAXID");;
            result = !Util.isEmpty(cif) ? cif : "";
        }
        else if (columnId.equals(SECBEEMI)) {
            final LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, issuer.getId(), "ALL", "SECTORCONTABLE");
            result = attr != null ? attr.getAttributeValue() : "";
        }
        else if (columnId.equals(IDCARTER)) {
            String bookName = "";
            bookName = product.getSecCode("SEC_PORTFOLIO");
            if(Util.isEmpty(bookName)) {
                bookName = (String)super.getColumnValue(row, BOOK, errors);
            }
            if(Util.isEmpty(bookName)) {
                Book book = row.getProperty(BOOK);
                bookName = book != null ? book.getName() : "";
            }

            if(!Util.isEmpty(bookName)) {
                Book book = BOCache.getBook(DSConnection.getDefault(), bookName);
                AccountingBook acctBook = book.getAccountingBook();
                if(acctBook != null) {
                    String acctBookName = acctBook.getName();
                    if(acctBookName.equals("Negociacion")) {
                        result = "0";
                    }
                    else if(acctBookName.equals("Disponible para la venta")) {
                        result = "DV";
                    }
                    else if(acctBookName.equals("Inversion crediticia")) {
                        result = "2";
                    }
                    else if(acctBookName.equals("Inversion a vencimiento")) {
                        result = "COS";
                    }
                    else if(acctBookName.equals("Otros a valor razonable")) {
                        result = "3";
                    }
                }
            }
            else
                result = "0";
        }
        else if (columnId.equals(AUTOCARTERA)) {
            result = issuer.getCode().equals("BSTE") ? SI : NO;
        }
        else if(columnId.equals(DIRECTION)) {
            BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
            Amount quantity = (Amount)super.getColumnValue(row, Util.dateToMString(context.endDate), errors);
            if(quantity!=null) {
                if(quantity.get()>=0)
                    result = "LARGA";
                else
                    result = "CORTA";
            }
        }
        else if(columnId.equals(IMNOMINA)) {
            BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
            Amount quantity = (Amount)super.getColumnValue(row, Util.dateToMString(context.endDate), errors);
            result = quantity;
        }
        else if(columnId.equals(IMPPRINC)) {
            BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
            Amount quantity = (Amount)super.getColumnValue(row, Util.dateToMString(context.endDate), errors);
            result = quantity;
        }

        else if(columnId.equals(CDPRODUC)) {
            result = row.getProperty(PRODUCT);
        }
        else if(columnId.equals(IDSUCACO)) {
            result = row.getProperty(PRODUCT_SUBTYPE);
        }
        else if (columnId.equals(CDESTROP)) {
            result = "ESP  ";
        }

        else if (columnId.equals(DESCOCON)) {
            result = issuer.getName();
        }

        else if (columnId.equals(FECHAVEN)) {
            return "9999/12/31";
        }else if (ISSUE_TYPE.equals(columnId)) {
            result = "";
        }

        if(result ==null)
            return MicReportUtil.formatColumnValue(columnId, super.getColumnValue(row, columnId, errors));
        else
            return MicReportUtil.formatColumnValue(columnId, result);
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
