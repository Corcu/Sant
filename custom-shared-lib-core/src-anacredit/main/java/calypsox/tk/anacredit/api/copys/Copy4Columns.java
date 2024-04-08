package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.attributes.*;
import com.calypso.tk.core.Log;


public enum Copy4Columns {

	FECHA_EXTRACION(  			new DateString()),
	ID_CONTRATO(  			    new Alpha(50)),
	TIPO_IMPORTE(  			    new Alpha(3)),
	APLICACION_ORIGEN(  		new Alpha(4)),
	ID_ENTIDAD(  			    new Numeric(4)),
	ID_CENTRO_CONTABLE(  		new Numeric(6)),
	PROVINCIA_NEGOCIO(  		new Alpha(2)),
	PAIS_NEGOCIO(  			    new Alpha(4)),
	FILLER_TIPO_CARTEIRA(  		new FILLER(2)),
	MONEDA(  			        new Numeric(3)),
	PRODUCTO_AC(  			    new Alpha(5)),
	IMPORTE_EUROS(  			new Decimal(13, 2,true)),
	IMPORTE_DIVISA(  			new Decimal(13, 2,true)),
	RC_FACTOR_CONVERSION_ORDEN( new FILLER(5)),
	ID_PLAN_CONTABLE(  			new Alpha(50)),
	TIPO_CARTERA_IRFS9(  		new Alpha(5)),
	ENTIDAD_GRUPO_ORIGEN(  		new FILLER(15)),
	CUENTA_GRUPO(  		new FILLER(20)),
	SUBCUENTA(  		new FILLER(10)),
	ID_CENTRO_CONTABLE_A(  		new FILLER(10)),

    ;
	Attribute _attr ;

	Copy4Columns(Attribute attr) {
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
