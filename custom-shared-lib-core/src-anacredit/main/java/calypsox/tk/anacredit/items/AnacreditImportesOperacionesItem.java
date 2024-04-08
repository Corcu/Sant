package calypsox.tk.anacredit.items;

public class AnacreditImportesOperacionesItem extends AnacreditOperacionesItem {

    public AnacreditImportesOperacionesItem() {
        super();
    }

    String tipo_importe = "";
    String importe_euros = "";
    String importe_divisa = "";
    String id_plan_contable = "";
    String entidad_grupo_origen = "";
    String cuenta_grupo = "";
    String subcuenta = "";
    String id_centro_contable_a = "";


    public String getTipo_importe() {
        return tipo_importe;
    }

    public String getImporte_euros() {
        return importe_euros;
    }

    public String getImporte_divisa() {
        return importe_divisa;
    }

    public String getId_plan_contable() {
        return id_plan_contable;
    }

    public void setTipo_importe(String tipo_importe) {
        this.tipo_importe = tipo_importe;
    }

    public void setImporte_euros(String importe_euros) {
        this.importe_euros = importe_euros;
    }

    public void setImporte_divisa(String importe_divisa) {
        this.importe_divisa = importe_divisa;
    }

    public void setId_plan_contable(String id_plan_contable) {
        this.id_plan_contable = id_plan_contable;
    }

    @Override
    public String getEntidad_grupo_origen() {
        return entidad_grupo_origen;
    }

    @Override
    public void setEntidad_grupo_origen(String entidad_grupo_origen) {
        this.entidad_grupo_origen = entidad_grupo_origen;
    }

    @Override
    public String getCuenta_grupo() {
        return cuenta_grupo;
    }

    @Override
    public void setCuenta_grupo(String cuenta_grupo) {
        this.cuenta_grupo = cuenta_grupo;
    }

    @Override
    public String getSubcuenta() {
        return subcuenta;
    }

    @Override
    public void setSubcuenta(String subcuenta) {
        this.subcuenta = subcuenta;
    }

    @Override
    public String getId_centro_contable_a() {
        return id_centro_contable_a;
    }

    @Override
    public void setId_centro_contable_a(String id_centro_contable_a) {
        this.id_centro_contable_a = id_centro_contable_a;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.fecha_extraccion);
        builder.append(this.id_contrato);
        builder.append(this.tipo_importe);
        builder.append(this.aplicacion_origen);
        builder.append(this.id_entidad);
        builder.append(this.id_centro_contable);
        builder.append(this.provincia_negocio);
        builder.append(this.pais_negocio);
        builder.append(this.tipo_cartera);
        builder.append(this.moneda);
        builder.append(this.producto_ac);
        builder.append(this.importe_euros);
        builder.append(this.importe_divisa);
        builder.append(this.rc_factor_conversion_orden);
        builder.append(this.id_plan_contable);
        builder.append(this.tipo_cartera_ifrs9);
        builder.append(this.entidad_grupo_origen);
        builder.append(this.cuenta_grupo);
        builder.append(this.subcuenta);
        builder.append(this.id_centro_contable_a);

        return builder.toString();
    }
}
