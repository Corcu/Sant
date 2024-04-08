/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.io.Serializable;

import com.calypso.tk.core.JDate;

public class HFExposureAndCollateralReportItem implements Serializable {

    private static final long serialVersionUID = 6389332371417748206L;

    private int contractId;

    private String contractName;

    private String cptyLongName;

    private String cptyShortName;

    private String currency;

    private JDate valuationDate;

    private int nbTrades;

    private double sumMtm;

    private double sumIndepAmount;

    private double cptyThresHold;

    private double collateralHeld;

    private double collateralTransit;

    private double possibleCollateral;

    private double netMarginResult;

    private double minimumTransfer;

    private double marginCall;

    public int getContractId() {
	return this.contractId;
    }

    public void setContractId(final int contractId) {
	this.contractId = contractId;
    }

    public String getContractName() {
	return this.contractName;
    }

    public void setContractName(final String contractName) {
	this.contractName = contractName;
    }

    public String getCptyLongName() {
	return this.cptyLongName;
    }

    public void setCptyLongName(final String cptyLongName) {
	this.cptyLongName = cptyLongName;
    }

    public String getCptyShortName() {
	return this.cptyShortName;
    }

    public void setCptyShortName(final String cptyShortName) {
	this.cptyShortName = cptyShortName;
    }

    public String getCurrency() {
	return this.currency;
    }

    public void setCurrency(final String currency) {
	this.currency = currency;
    }

    public JDate getValuationDate() {
	return this.valuationDate;
    }

    public void setValuationDate(final JDate valuationDate) {
	this.valuationDate = valuationDate;
    }

    public int getNbTrades() {
	return this.nbTrades;
    }

    public void setNbTrades(final int nbTrades) {
	this.nbTrades = nbTrades;
    }

    public double getSumMtm() {
	return this.sumMtm;
    }

    public void setSumMtm(final double sumMtm) {
	this.sumMtm = sumMtm;
    }

    public double getSumIndepAmount() {
	return this.sumIndepAmount;
    }

    public void setSumIndepAmount(final double sumIndepAmount) {
	this.sumIndepAmount = sumIndepAmount;
    }

    public double getCptyThresHold() {
	return this.cptyThresHold;
    }

    public void setCptyThresHold(final double cptyThresHold) {
	this.cptyThresHold = cptyThresHold;
    }

    public double getCollateralHeld() {
	return this.collateralHeld;
    }

    public void setCollateralHeld(final double collateralHeld) {
	this.collateralHeld = collateralHeld;
    }

    public double getCollateralTransit() {
	return this.collateralTransit;
    }

    public void setCollateralTransit(final double collateralTransit) {
	this.collateralTransit = collateralTransit;
    }

    public double getPossibleCollateral() {
	return this.possibleCollateral;
    }

    public void setPossibleCollateral(final double possibleCollateral) {
	this.possibleCollateral = possibleCollateral;
    }

    public double getNetMarginResult() {
	return this.netMarginResult;
    }

    public void setNetMarginResult(final double netMarginResult) {
	this.netMarginResult = netMarginResult;
    }

    public double getMinimumTransfer() {
	return this.minimumTransfer;
    }

    public void setMinimumTransfer(final double minimumTransfer) {
	this.minimumTransfer = minimumTransfer;
    }

    public double getMarginCall() {
	return this.marginCall;
    }

    public void setMarginCall(final double marginCall) {
	this.marginCall = marginCall;
    }

}
