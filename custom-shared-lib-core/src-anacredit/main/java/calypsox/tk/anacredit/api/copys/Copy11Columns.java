package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.attributes.*;
import com.calypso.tk.core.Log;


public enum Copy11Columns {

	ID_ENTIDAD(new Numeric(4)),
	FECHA_ALTA_RELACION(    new DateString()),
	FECHA_DATOS(    new DateString()),
	FECHA_BAJA_RELACION(new DateString()),
	CODIGO_OPERACION( new Alpha(50)),
	TIPO_GARANTIA_REAL(new Alpha(3)),
	ALCANCE_GARANTIA_REAL(new Alpha(3)),
	CODIGO_GARANTIA_REAL(new Alpha(60)),
	CODIGO_ACTIVO_REC_GARANTIA(new Alpha(60)),
	FILLER (new FILLER(2)),
	ORDEN_PRELACION_GARANTIA( new Numeric(3)),
	IMPORTE_HIPOTECARIA_PRIN(new Decimal(15, 2, true)),
	IMPORTE_HIPOTECARIA_INT(new Decimal(15, 2, true)),
	ACTIVOS_GARANTIA_INM(new Alpha(2)),
	TIPO_ACTIVO_REC_GARANTIA(new Alpha(3)),
	IMPORTE_GARANTIA(new Decimal(15, 2, true, DataAttribute.SIGNAL_PLUS)),
	FECHA_FORMALIZACION_GARANTIA(new DateString()),
	IMPORTE_GARANTIA_CREDITICIA(new Decimal(15, 2, true, DataAttribute.SIGNAL_PLUS)),
	DERECHO_COBRO_GARANTIA(new Decimal(15, 2, true)),
	VENCIMIENTO_COBERTURA(new DateString()),
	GARANTIA_PRINCIPAL_PRIORITARIA(new Numeric(1)),
	FILLER1(new FILLER(79));

    ;
	Attribute _attr ;

	Copy11Columns(Attribute attr) {
		_attr = attr;
	}
	public String parseValue(Object o) {
		try  {
			if (null != o) {
				
				return  _attr.formatValue(o);
			}
		} catch (Exception e) {
			Log.error(this.name(), toString() + " value :'" + o.toString() +"'", e);
			
		}
		return null;
	}
	public boolean isFiller() {
	    return _attr.getDataType() == DataType.FILLER;
	}
	public String toString() {
		return " " + this.name() + " " + _attr.toString();
	}
	
}
