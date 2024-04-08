/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.product;

import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class EquityCustomData implements ProductCustomData {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected int _productId;
    protected int _version = 0;

    // StockLendingRates values
    protected Double active_available_qty;
    protected Double fee;
    protected Double qty_on_loan;
    protected Double rebate;
    protected String expired_date_type;
    protected JDate expired_date;
    protected JDate last_update;

    // haircut stuff
    protected Double haircut_eurex;
    protected Double haircut_meff;

    public EquityCustomData() {
    }

    // only StockLendingRates stuff
    public EquityCustomData(int bondId, Double active_available_qty, Double fee, Double qty_on_loan, Double rebate,
                            String expired_date_type, JDate expired_date, JDate last_update) {
        this._productId = bondId;
        this.active_available_qty = active_available_qty;
        this.fee = fee;
        this.qty_on_loan = qty_on_loan;
        this.rebate = rebate;
        this.expired_date_type = expired_date_type;
        this.expired_date = expired_date;
        this.last_update = last_update;
    }

    // StockLendingRates stuff + MMOO haircuts
    public EquityCustomData(int bondId, Double active_available_qty, Double fee, Double qty_on_loan, Double rebate,
                            String expired_date_type, JDate expired_date, JDate last_update, Double hcMeff, Double hcEurex) {

        this(bondId, active_available_qty, fee, qty_on_loan, rebate, expired_date_type, expired_date, last_update);
        this.haircut_eurex = hcEurex;
        this.haircut_meff = hcMeff;
    }

    public void setProductId(int id) {
        this._productId = id;
    }

    @Override
    public int getProductId() {
        return this._productId;
    }

    @Override
    public int getProductSubId() {
        return 0;
    }

    @Override
    public int getVersion() {
        return this._version;
    }

    @Override
    public void setVersion(int version) {
        this._version = version;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void doAudit(Auditable old, Vector audits) {
        AuditValue.doAudit(this, old, audits);

        if (audits.size() > 0) {
            this._version++;
        }
        for (int i = 0; i < audits.size(); i++) {
            ((AuditValue) audits.elementAt(i)).setVersion(this._version);
        }
    }

    @Override
    public Object clone() {
        EquityCustomData customData = new EquityCustomData(this._productId, this.active_available_qty, this.fee,
                this.qty_on_loan, this.rebate, this.expired_date_type, this.expired_date, this.last_update);
        customData.setVersion(this._version);
        return customData;
    }

    public boolean auditCompare(EquityCustomData oldData) {
        boolean areEqual = false;
        areEqual = ((oldData != null) && (this._productId == oldData._productId));
        areEqual &= (this.active_available_qty == oldData.active_available_qty);
        areEqual &= (this.fee == oldData.fee);
        areEqual &= (this.qty_on_loan == oldData.qty_on_loan);
        areEqual &= (this.rebate == oldData.rebate);
        areEqual &= (this.expired_date_type == oldData.expired_date_type);
        areEqual &= (this.expired_date == oldData.expired_date);
        areEqual &= (this.last_update == oldData.last_update);
        return areEqual;
    }

    @Override
    public void undo(DSConnection ds, AuditValue av) {
        AuditValue.undo(ds, av, this);
    }

    @Override
    public long getLongId() {
        return this._productId;
    }

    @Override
    public void setLongId(long id) {
        this._productId = Math.toIntExact(id);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void diff(Authorizable old, Vector diffs, String user, JDatetime mdate) {
    }

    @Override
    public void apply(DSConnection ds, PendingModification modif) {
    }

    @Override
    public String getAuthName() {
        return null;
    }

    // for StockLendingRates stuff

    /**
     * @return the active_available_qty
     */
    public Double getActive_available_qty() {
        return this.active_available_qty;
    }

    /**
     * @param active_available_qty the active_available_qty to set
     */
    public void setActive_available_qty(Double active_available_qty) {
        this.active_available_qty = active_available_qty;
    }

    /**
     * @return the fee
     */
    public Double getFee() {
        return this.fee;
    }

    /**
     * @param fee the fee to set
     */
    public void setFee(Double fee) {
        this.fee = fee;
    }

    /**
     * @return the qty_on_loan
     */
    public Double getQty_on_loan() {
        return this.qty_on_loan;
    }

    /**
     * @param qty_on_loan the qty_on_loan to set
     */
    public void setQty_on_loan(Double qty_on_loan) {
        this.qty_on_loan = qty_on_loan;
    }

    /**
     * @return the rebate
     */
    public Double getRebate() {
        return this.rebate;
    }

    /**
     * @param rebate the rebate to set
     */
    public void setRebate(Double rebate) {
        this.rebate = rebate;
    }

    /**
     * @return the expired_date_type
     */
    public String getExpired_date_type() {
        return this.expired_date_type;
    }

    /**
     * @param expired_date_type the expired_date_type to set
     */
    public void setExpired_date_type(String expired_date_type) {
        this.expired_date_type = expired_date_type;
    }

    /**
     * @return the expired_date
     */
    public JDate getExpired_date() {
        return this.expired_date;
    }

    /**
     * @param expired_date the expired_date to set
     */
    public void setExpired_date(JDate expired_date) {
        this.expired_date = expired_date;
    }

    /**
     * @return the last_update
     */
    public JDate getLast_update() {
        return this.last_update;
    }

    /**
     * @param last_update the last_update to set
     */
    public void setLast_update(JDate last_update) {
        this.last_update = last_update;
    }

    // GSM: 03/09/2014. Added custom MMOO haircuts

    /**
     * @return the haircut_fed
     */
    public Double getHaircut_eurex() {
        return this.haircut_eurex;
    }

    /**
     * @param haircut_fed the haircut_fed to set
     */
    public void setHaircut_eurex(Double haircut_eurex) {
        this.haircut_eurex = haircut_eurex;
    }

    /**
     * @return the haircut_meff
     */
    public Double getHaircut_meff() {
        return this.haircut_meff;
    }

    /**
     * @param haircut_meff the haircut_meff to set
     */
    public void setHaircut_meff(Double haircut_meff) {
        this.haircut_meff = haircut_meff;
    }

}
