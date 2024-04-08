/*
*
* Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
* All rights reserved.
* 
*/
package calypsox.engine.im.simulator.output;

public class SantInitialMarginSimulatorOutput {
	public final static String SEPARATOR = "|";

	private int contractId;
	private String contractCcy;

	public SantInitialMarginSimulatorOutput() {
	}

	public int getContractId() {
		return this.contractId;
	}

	public void setContractId(int contractId) {
		this.contractId = contractId;
	}

	public String getContractCcy() {
		return this.contractCcy;
	}

	public void setContractCcy(String contractCcy) {
		this.contractCcy = contractCcy;
	}

	public String generateOutput() {
		StringBuilder infoFormatted = new StringBuilder();

		infoFormatted.append(this.contractId);
		infoFormatted.append(SEPARATOR);

		infoFormatted.append(this.contractCcy);
		infoFormatted.append(SEPARATOR);

		return infoFormatted.toString();
	}
}
