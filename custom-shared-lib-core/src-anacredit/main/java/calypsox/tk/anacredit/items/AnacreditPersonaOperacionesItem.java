package calypsox.tk.anacredit.items;

public class AnacreditPersonaOperacionesItem extends AnacreditOperacionesItem{

    public AnacreditPersonaOperacionesItem() {
        super();
    }

    String fecha_alta_relacion = "";
    String fecha_baja_relacion ="";
    String id_persona =	"";
    String naturaleza_intervencion ="";
    String grupo_titulares_mancomunados ="";
    String porcentaje_principal_subvencionado ="";
    String porcentaje_intereses_subvencionados ="";
    String porcentaje_participacion	= "";
    String relacion_no_declarable_cirbe = "";
    String importe_maximo_responsabilidad_conjunta ="";
    String importe_responsabilidad_conjunta ="";
    String grado_relevancia_garante = "";
    String titular_no_convenio_acreedores = "";
    String tratamiento_especial_cirbe = "";


    String filler1 = "";


    public String getId_entidad() {
        return id_entidad;
    }

    public void setId_entidad(String id_entidad) {
        this.id_entidad = id_entidad;
    }

    public String getFecha_alta_relacion() {
        return fecha_alta_relacion;
    }

    public void setFecha_alta_relacion(String fecha_alta_relacion) {
        this.fecha_alta_relacion = fecha_alta_relacion;
    }

    public String getFecha_baja_relacion() {
        return fecha_baja_relacion;
    }

    public void setFecha_baja_relacion(String fecha_baja_relacion) {
        this.fecha_baja_relacion = fecha_baja_relacion;
    }

    public String getId_persona() {
        return id_persona;
    }

    public void setId_persona(String id_persona) {
        this.id_persona = id_persona;
    }

    public String getNaturaleza_intervencion() {
        return naturaleza_intervencion;
    }

    public void setNaturaleza_intervencion(String naturaleza_intervencion) {
        this.naturaleza_intervencion = naturaleza_intervencion;
    }

    public String getGrupo_titulares_mancomunados() {
        return grupo_titulares_mancomunados;
    }

    public void setGrupo_titulares_mancomunados(String grupo_titulares_mancomunados) {
        this.grupo_titulares_mancomunados = grupo_titulares_mancomunados;
    }

    public String getPorcentaje_principal_subvencionado() {
        return porcentaje_principal_subvencionado;
    }

    public void setPorcentaje_principal_subvencionado(String porcentaje_principal_subvencionado) {
        this.porcentaje_principal_subvencionado = porcentaje_principal_subvencionado;
    }

    public String getPorcentaje_intereses_subvencionados() {
        return porcentaje_intereses_subvencionados;
    }

    public void setPorcentaje_intereses_subvencionados(String porcentaje_intereses_subvencionados) {
        this.porcentaje_intereses_subvencionados = porcentaje_intereses_subvencionados;
    }

    public String getPorcentaje_participacion() {
        return porcentaje_participacion;
    }

    public void setPorcentaje_participacion(String porcentaje_participacion) {
        this.porcentaje_participacion = porcentaje_participacion;
    }

    public String getRelacion_no_declarable_cirbe() {
        return relacion_no_declarable_cirbe;
    }

    public void setRelacion_no_declarable_cirbe(String relacion_no_declarable_cirbe) {
        this.relacion_no_declarable_cirbe = relacion_no_declarable_cirbe;
    }

    public String getImporte_maximo_responsabilidad_conjunta() {
        return importe_maximo_responsabilidad_conjunta;
    }

    public void setImporte_maximo_responsabilidad_conjunta(String importe_maximo_responsabilidad_conjunta) {
        this.importe_maximo_responsabilidad_conjunta = importe_maximo_responsabilidad_conjunta;
    }

    public String getImporte_responsabilidad_conjunta() {
        return importe_responsabilidad_conjunta;
    }

    public void setImporte_responsabilidad_conjunta(String importe_responsabilidad_conjunta) {
        this.importe_responsabilidad_conjunta = importe_responsabilidad_conjunta;
    }

    public String getGrado_relevancia_garante() {
        return grado_relevancia_garante;
    }

    public void setGrado_relevancia_garante(String grado_relevancia_garante) {
        this.grado_relevancia_garante = grado_relevancia_garante;
    }

    public String getTitular_no_convenio_acreedores() {
        return titular_no_convenio_acreedores;
    }

    public void setTitular_no_convenio_acreedores(String titular_no_convenio_acreedores) {
        this.titular_no_convenio_acreedores = titular_no_convenio_acreedores;
    }

    public void setFiller1(String filler1) {
        this.filler1 = filler1;
    }


    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append(this.id_entidad);
        builder.append(this.fecha_alta_relacion);
        builder.append(this.fecha_extraccion);
        builder.append(this.fecha_baja_relacion);
        builder.append(this.id_contrato);
        builder.append(this.id_persona);
        builder.append(this.naturaleza_intervencion);
        builder.append(this.grupo_titulares_mancomunados);
        builder.append(this.porcentaje_principal_subvencionado);
        builder.append(this.porcentaje_intereses_subvencionados);
        builder.append(this.porcentaje_participacion);
        builder.append(this.relacion_no_declarable_cirbe);
        builder.append(this.importe_maximo_responsabilidad_conjunta);
        builder.append(this.importe_responsabilidad_conjunta);
        builder.append(this.grado_relevancia_garante);
        builder.append(this.titular_no_convenio_acreedores);
        builder.append(this.tratamiento_especial_cirbe);
        builder.append(this.filler1);


        return builder.toString();
    }

    public String getTratamiento_especial_cirbe() {
        return tratamiento_especial_cirbe;
    }

    public void setTratamiento_especial_cirbe(String tratamiento_especial_cirbe) {
        this.tratamiento_especial_cirbe = tratamiento_especial_cirbe;
    }
}
