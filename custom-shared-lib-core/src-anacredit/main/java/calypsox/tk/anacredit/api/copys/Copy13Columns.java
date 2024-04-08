package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.attributes.*;
import com.calypso.tk.core.Log;


public enum Copy13Columns {

	ID_ENTIDAD(new Numeric(4)),
	FECHA_DATOS(    new DateString()),
	CODIGO_ACTIVO_REC_GARANTIA(	new Alpha(60)),
	TIPO_ACTIVO_REC_GARANTIA(	new Alpha(3)),
	CODIGO_EMISOR(	new Alpha(50)),
	CODIGO_VALOR(	new Alpha(12)),
	COTIZA(	new Alpha(2)),
	VALOR_NOMINAL_GARANTIA(new Decimal(15,2,true, DataAttribute.SIGNAL_PLUS)),
	FECHA_BAJA_GARANTIA(  new DateString()),
	GARANTIA_NO_DECLARABLE_CIRBE(new Alpha(1)),
	ID_PROVEEDOR_GARANTIA(new Alpha(50)),
	VALOR_ORIGINAL_GARANTIA(new Decimal(15,2,true, DataAttribute.SIGNAL_PLUS)),
	FECHA_VALOR_ORIGINAL_GARANTIA(  new DateString()),
	IMPORTE_GARANTIA(new Decimal(15,2,true, DataAttribute.SIGNAL_PLUS)),
	TIPO_IMPORTE_GARANTIA(new Numeric(1)),
	MET_VALORACION_GARANTIA(new Numeric(1)),
	FECHA_VALORACION_GARANTIA(new DateString()),
	VENCIMIENTO_COBERTURA_GARANTIA(new DateString()),
	NUM_TITULOS_PARTICIPACIONES(new Decimal(15,2,true,DataAttribute.SIGNAL_PLUS)),

	REFERENCIA_INTERNA(new Alpha(64)),
	FILLER(new FILLER(135)),
	VERSION(new Alpha(5)),

	;
	Attribute _attr ;

	Copy13Columns(Attribute attr) {
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
