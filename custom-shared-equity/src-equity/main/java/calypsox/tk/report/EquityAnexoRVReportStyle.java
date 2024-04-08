package calypsox.tk.report;

import calypsox.tk.core.SantanderUtil;
import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.*;


public class EquityAnexoRVReportStyle extends EquityMisPlusCarteraReportStyle {


	private static final long serialVersionUID = 1L;

	public static final String ENTIDAD_PARTICIPADA = "ENTIDAD_PARTICIPADA";
	public static final String CLASE_TITULOS = "CLASE_TITULOS";
	public static final String NIF_PARTICIPADA = "NIF_PARTICIPADA";
	public static final String ACTIVIDAD = "ACTIVIDAD";
	public static final String COD_PAIS = "COD_PAIS";
	public static final String CODIGO_MONEDA = "CODIGO_MONEDA";
	public static final String COTIZACION = "COTIZACION";
	public static final String CODIGO_CARGABAL = "CODIGO_CARGABAL";
	public static final String ISIN = "ISIN";
	public static final String TIPO_CARTERA = "TIPO_CARTERA";
	public static final String CAPITAL_SOCIAL = "CAPITAL_SOCIAL";
	public static final String TITULOS_EMITIDOS = "TITULOS_EMITIDOS";
	public static final String TITULOS = "TITULOS";
	public static final String NOMINAL = "NOMINAL";
	public static final String DERECHO_VOTO_PORCENTAJE = "DERECHO_VOTO_PORCENTAJE";
	public static final String COSTE_ORIGEN = "COSTE_ORIGEN";
	public static final String COSTE_EUR = "COSTE_EUR";
	public static final String VALOR_CONTABLE = "VALOR_CONTABLE";
	public static final String VALOR_RAZONABLE = "VALOR_RAZONABLE";
	public static final String AJUSTE_POR_VALORACION = "AJUSTE_POR_VALORACION";
	public static final String IMPORTE_BRUTO = "IMPORTE_BRUTO";
	public static final String PATRIMONIO_NETO = "PATRIMONIO_NETO";
	public static final String EFECTO_FISCAL = "EFECTO_FISCAL";
	public static final String CORRECION_DE_VALOR = "CORRECION_DE_VALOR";
	public static final String CORRECION_DE_CAMBIO = "CORRECION_DE_CAMBIO";


	public static final String EQUITY_TYPE = "EQUITY_TYPE";
	public static final String CALYPSO_PRODUCT = "CALYPSO_PRODUCT";

	public static final ArrayList<String> emptyColumns = new ArrayList<String>();
	public static final HashMap<String,String> columnToColumn = new HashMap<String,String>();

	static {

		emptyColumns.add(ACTIVIDAD);
		emptyColumns.add(COD_PAIS);
		emptyColumns.add(CODIGO_CARGABAL);
		emptyColumns.add(CAPITAL_SOCIAL);
		emptyColumns.add(TITULOS_EMITIDOS);
		emptyColumns.add(DERECHO_VOTO_PORCENTAJE);
		emptyColumns.add(AJUSTE_POR_VALORACION);
		emptyColumns.add(IMPORTE_BRUTO);
		emptyColumns.add(PATRIMONIO_NETO);
		emptyColumns.add(EFECTO_FISCAL);
		emptyColumns.add(CORRECION_DE_VALOR);
		emptyColumns.add(CORRECION_DE_CAMBIO);

	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		if(emptyColumns.contains(columnName))
			return "";
		if(columnToColumn.containsKey(columnName)) {
			return getColumnValue(row,columnToColumn.get(columnName),errors);
		}

		final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
		final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
		final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

		if (columnName.equals(ENTIDAD_PARTICIPADA)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				return le != null ? le.getName() : "";
			}
			return "";
		}

		if (columnName.equals(CLASE_TITULOS)) {
			String equityType = (String)super.getColumnValue(row, EQUITY_TYPE, errors);
			return "PFI".equalsIgnoreCase(equityType) ? "P" : "A";
		}

		if (columnName.equals(NIF_PARTICIPADA)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				LegalEntity le = equity.getIssuer();
				String cif = getLegalEntityAttribute(le, "TAXID");
				return le != null ? cif : "";
			}
			return "";
		}

		if (columnName.equals(CODIGO_MONEDA)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				String currency = equity.getCurrency() != null ? equity.getCurrency() : "";
				CurrencyDefault currency1 = LocalCache.getCurrencyDefault(currency);
				String codDMN = currency1.getAttribute("cod_DMN");
				return codDMN;
			}
			return "";
		}

		if (columnName.equals(COTIZACION)) {
			return "S";
		}

		if (columnName.equals(ISIN)) {
			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				return equity != null ? equity.getName() : "";
			}
			return "";
		}

		if (columnName.equals(TIPO_CARTERA)) {
			return "20";
		}

		if (columnName.equals(TITULOS)
				|| columnName.equals(NOMINAL)) {
			String strNominal = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Nominal").toString().replace(".","").replace(",",".");
			return strNominal;
		}

		if (columnName.equals(COSTE_ORIGEN)) {

			return formatResult(getSuperAmount(row, "Amount" , errors));
		}

		if (columnName.equals(COSTE_EUR)) {
			String currency = (String) super.getColumnValue(row, "Currency" , errors);
			if ("EUR".equalsIgnoreCase(currency)){
				return formatResult(getSuperAmount(row, "Amount" , errors));
			}
			double amount = getSuperAmount(row, "Amount" , errors);
			return formatResult(amount * getFixing(row, valDate, errors));
		}

		if (columnName.equals(VALOR_CONTABLE)
				|| columnName.equals(VALOR_RAZONABLE)) {

			Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			String isin = "";
			if (product instanceof Equity) {
				Equity equity = (Equity) product;
				isin = equity != null ? "Equity.ISIN_" + equity.getSecCode("Common") : "";
			}

			try {
				Double quote = getQuoteValuesFromISIN(isin, valDate);
				Double quantityValue = getSuperAmount(row, "Quantity" , errors);

				if(quantityValue==null || quote==null)
					return formatResult(0);

				return formatResult(quantityValue * quote);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return "";
		}

		return super.getColumnValue(row, columnName, errors);
	}

	public Object formatResult(Object o) {
		return UtilReport.formatResult(o, '.');
	}

	private double getFixing(ReportRow row, JDate valDate, Vector errors) {
		Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
		String productCurrency = product.getCurrency();

		if (!productCurrency.equals(SantanderUtil.EUR)) {
			return CollateralUtilities.getFXRate(valDate, SantanderUtil.EUR, productCurrency);
		}
		return 1.0d;
	}

	@SuppressWarnings("unchecked")
	public Double getQuoteValuesFromISIN(final String quoteName, final JDate quoteDate) throws RemoteException {

		String sql = "quote_name='" + quoteName + "' and quote_date=" + Util.date2SQLString(quoteDate) + " and quote_set_name='OFFICIAL'";
		Vector<QuoteValue> quoteValues = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(sql);
		Map<String, Double> map = new HashMap<String, Double>();
		for (QuoteValue value : quoteValues) {
			return value.getClose();
		}
		return null;
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

	private double getSuperAmount(ReportRow row, String columnName, Vector errors) {
		String amountS = (String)super.getColumnValue(row, columnName, errors);
		if (com.calypso.infra.util.Util.isEmpty(amountS)) {
			return 0.0d;
		}
		return Double.valueOf(amountS);
	}

}
