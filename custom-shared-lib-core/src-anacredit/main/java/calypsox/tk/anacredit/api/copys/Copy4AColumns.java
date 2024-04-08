package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.attributes.*;
import com.calypso.tk.core.Log;


public enum Copy4AColumns {

    ID_ENTIDAD(  			        new Numeric(4)),
    FECHA_ALTA_RELACION(  			new DateString()),
    FECHA_DATOS(  			        new DateString()),
    FECHA_BAJA_RELACION(  			new DateString()),
    ID_CONTRATO(  			        new Alpha(50)),
    ID_PERSONA(  			        new Alpha(30)),
    NATURALEZA_INTERVENCION(  		new Alpha(2)),
    GRUPO_TITULARES_MANCOMUNADOS(  		new Numeric(13)),
    PORCENTAJE_PRINCIPAL_SUBVENCIONADOS(  		new FILLER(8)),
    PORCENTAJE_INTERES_SUBVENCIONADOS(  		new FILLER(8)),
    PORCENTAJE_PARTICIPACION_(  		new Decimal(3, 5)),
    RELACION_NO_DECLARABLE_CIRBE (  		new FILLER(1)),
    IMPORTE_MAXIMO_RESPONSABILIDAD_CONJUNTA (  		new Decimal(13, 2, true)),
    IMPORTE_RESPONSABILIDAD_CONJUNTA (  			new Decimal(13, 2, true)),
    GRADO_RELEVANCIA_GARANTE (  		new Numeric(13)),
    TITULAR_NO_CONVENIO_ACREEDORES (  		new Numeric(1)),
    TRATAMIENTO_ESPECIAL_CIRBE (  		new Numeric(1)),
    FILLER                      (new FILLER(105));
    ;
	Attribute _attr ;

	Copy4AColumns(Attribute attr) {
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
