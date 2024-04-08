package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.AnacreditConstants;
import com.calypso.tk.core.Util;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * COPY3 Record - OPERACIONES
 */
public class Copy3Record {

	private TreeMap<Copy3Columns , String> _parsedValues = new TreeMap<>();
	private TreeMap<String , Object> _keeper = new TreeMap<>();
	private boolean _isOK  = true;



	private void initDefaults() {

		setValue(   Copy3Columns.ID_ENTIDAD, AnacreditConstants.STR_ENTIDAD_0049);
		setValue(   Copy3Columns.APLICACION_ORIGEN, AnacreditConstants.STR_ORIGEN_A003);
		setValue(   Copy3Columns.ID_CENTRO_CONTABLE,  AnacreditConstants.STR_ID_CENTRO_CONTABLE);
		setValue(   Copy3Columns.SUBORDINACION_PRODUCTO_AC,  "S4");
		setValue(   Copy3Columns.FINALIDAD_AC,  "F59");
		setValue(   Copy3Columns.DECLARADO_CIR_TERCERA_ENTIDAD, 0);
		setValue(   Copy3Columns.TIPO_RIESGO_SUBYACENTE, "00");
		setValue(   Copy3Columns.TRAMITE_RECUPERACION, "T4");
		setValue(   Copy3Columns.PRINCIPAL_INICIAL, 0);
		setValue(   Copy3Columns.LIMITE_INICIAL, 0);
		setValue(   Copy3Columns.IMPORTE_CONCEDIDO, 0);
		setValue(   Copy3Columns.REFINANCIACION, "00");
		setValue(   Copy3Columns.SUBVENCION_OPERACION, "ZZZ");
		setValue(	Copy3Columns.CANAL_CONTRATACION_AC, "");
		setValue(	Copy3Columns.PROVINCIA_NEGOCIO, "90");
		setValue(   Copy3Columns.EDIF_FINANC_ESTADO, "E4");
		setValue(   Copy3Columns.EDIF_FINANC_LICENCIA, "L10");
		setValue(   Copy3Columns.INMUEBLE_FINANC_NUM_VIVIENDAS, 0);
		setValue(   Copy3Columns.CODIGO_PROMOC_INMOB_FINAN, AnacreditConstants.EMPTY_STRING);
		setValue(   Copy3Columns.DESC_PROMOC_INMOB_FINAN, AnacreditConstants.EMPTY_STRING);
		setValue(   Copy3Columns.ORDEN_HIPOTECA, "0");
		setValue(   Copy3Columns.TIPO_GARANTIA_REAL_PPAL, 999);
		setValue(   Copy3Columns.COBERTURA_GARANTIA_REAL, "C3");
		setValue(   Copy3Columns.TIPO_GARANT_PERSONAL_PPAL_AC, "01");
		setValue(   Copy3Columns.COBERTURA_GARANTIA_PERSONAL, "C3");
		setValue(   Copy3Columns.TIPO_ACT_RECIB_PAGO_MES, "T1");
		setValue(   Copy3Columns.TIPO_SUBROGACION_MES, "O99");
		setValue(   Copy3Columns.FRECUENCIA_PAGO_PRINC_INT, "0");

		setValue(   Copy3Columns.SALDO_DEUDOR_VENCIDO, 0);
		setValue(   Copy3Columns.PRODUCTOS_VENCIDOS, 0);
		setValue(   Copy3Columns.PRODUCTOS_DUDOSOS_NO_CONSOLID, 0);
		setValue(   Copy3Columns.INTERESES_DEMORA_CONSOLIDADOS, 0);
		setValue(   Copy3Columns.INTERESES_DEMORA_NO_CONSOLID, 0);
		setValue(   Copy3Columns.TIPO_CODIGO_VALOR, "01");
		setValue(   Copy3Columns.PRODUCTO_RECURSO, "1");
		setValue(   Copy3Columns.INSTRUMENTO_FIDUCIARIO, "2");
		setValue(   Copy3Columns.FINANCIACION_PROYECTO, "0");
		setValue(   Copy3Columns.FECHA_FINAL_CARENCIA_PRINCIPAL, AnacreditConstants.STR_MIN_DATE_11111112);
		setValue(   Copy3Columns.TIPO_FUENTE_DE_CARGA, 1);
		setValue(   Copy3Columns.SITUACION_IMPAGO_OPERACION, "0");
		setValue(   Copy3Columns.FECHA_RENOVACION, AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_SITUACION_IMPAGO_OPE, AnacreditConstants.STR_MIN_DATE_11111112);
		setValue(   Copy3Columns.RECONOCIMIENTO_BALANCE, "1");
		setValue(   Copy3Columns.VALOR_ACTUAL_COM_GARAN_CONCED, "0");
		setValue(   Copy3Columns.PROVISION_RIESGO_NORMAL, "0");
		setValue(   Copy3Columns.SITUACION_OPERATIVA, "S8");
		setValue(   Copy3Columns.ESTADO_CUMPLIMIENTO, 9);
		setValue(   Copy3Columns.FECHA_PRIMER_INCUMPLIMIENTO_SIN_FALL_PARC, AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_REFINANCIACION, AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_INICIO_MORATORIA_COVID19, AnacreditConstants.STR_MIN_DATE_11111112);
		setValue(   Copy3Columns.FECHA_FIN_MORATORIA_COVID19, AnacreditConstants.STR_MIN_DATE_11111112);

		setValue(   Copy3Columns.MET_CALC_COBERT_RIESGO_NORMAL, "0");
		setValue(   Copy3Columns.MET_CALC_COBERT_RIESGO_DUDOSO, "0");
		setValue(   Copy3Columns.RENOVACION_AUTOMATICA, "0");
		setValue(   Copy3Columns.NUM_CUOTAS_IMPAGADAS_PPAL, 0);
		setValue(   Copy3Columns.CONTRATO_RENEGOCIADO, 0);
		setValue(   Copy3Columns.CONOCIMIENTO_GARANT_PERS_PPAL, 1);
		setValue(   Copy3Columns.ESTADO_REFIN_REEST_RENEG, 5);
		setValue(   Copy3Columns.FECHA_PRIMERA_LIQUIDACION,  AnacreditConstants.STR_MIN_DATE_11111112);
		setValue(   Copy3Columns.TIPO_REFERENCIA_SUSTITUTIVO,  11);
		setValue(   Copy3Columns.FREC_REVISION_TIPO_INT_PER,  "0");
		setValue(   Copy3Columns.PROXIMA_REVISION_TIPO_INTERES,  AnacreditConstants.STR_MIN_DATE_11111112);
		setValue(   Copy3Columns.FECHA_RENEGOCIACION,  AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_PRIMER_INCUMPLIMIENTO,  AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_ULTIMO_IMPAGO_INCUMPLIM,  AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_ULTIMO_VTO_INTERESES,  AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_PROXIMO_VTO_INTERESES,  AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_PROXIMO_VTO_PRINCIPAL,  AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_ULTIMO_VTO_PRINICIPAL,  AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.FECHA_PRIMER_INCUMPL_OP_REFIN,  AnacreditConstants.STR_MAX_DATE_99991231);
		setValue(   Copy3Columns.ORIGINADO_CON_DETERIORO,  AnacreditConstants.EMPTY_STRING);
		setValue(   Copy3Columns.CALCULO_SITUACION_IMPAGO, 2);
		setValue(   Copy3Columns.FALLIDOS_PARCIALES,  AnacreditConstants.EMPTY_STRING);
		setValue(   Copy3Columns.VERSION,  AnacreditConstants.VERSION);


	}
		
	public Copy3Record() {
		init();
		initDefaults();
	}

	public String getLine() {
		StringBuilder sb = new StringBuilder();
		for (Copy3Columns column : Copy3Columns.values()) {
			if(Arrays.stream(showAsEmptyColumn.values()).anyMatch((t) -> t.name().equalsIgnoreCase(column.name()))) {
				sb.append(column.parseValue(""));
			}else {
				String value = "";
				if (column.isFiller()) {
					value = column.parseValue("");
				} else {
					value = _parsedValues.get(column);
					if (Util.isEmpty(value))  {
						value = column.parseValue("");
					}
				}
				sb.append(value);
			}
		}
		return sb.append("").toString();
	}
	
	public boolean isOK() {
		return _isOK ;
	}

	private void init() {
		for (Copy3Columns col: Copy3Columns.values()) {
			_parsedValues.putIfAbsent(col, "");
		}
	}
	
	public boolean setValue(Copy3Columns column, Object value) {

		String parsedValue = column.parseValue(value);

		if (null == parsedValue  || parsedValue.length() == 0) {
			System.out.println("Return value is empty : " + column.toString());
		} else {
			_parsedValues.put(column, parsedValue);
			return true;
		}
		return false;
	}

	public Object getValue(Copy3Columns column) {
		Object value = _parsedValues.get(column);
		if (null == value) {
			return "";
		}
		return value;
	}

	private boolean filterColumn(Copy3Columns column){
		return true;
	}

	public enum showAsEmptyColumn {
		TIPO_CARTERA_IFRS9
	}

	public void keep(String label, Object value)  {
		_keeper.put(label, value);
	}

	public Object retrieve(String label)  {
		return _keeper.get(label);
	}
	

}
