package calypsox.tk.report;

import static calypsox.tk.util.ScheduledTaskCSVREPORT.DISCRIMINATE_CONTRACT_TYPE;
import static calypsox.tk.util.ScheduledTaskCSVREPORT.PRODUCT_LIST;
import static calypsox.tk.util.ScheduledTaskCSVREPORT.SEPARATOR_PRODUCT_TYPES;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;

import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import calypsox.tk.report.KGR_Collateral_MarginCallReport;

/**
 * Generates de file for PO-CTPYs exportation. Due to the POs entered in the Schedule task, this report will look for
 * all the contracts for each specified PO, and for each contract, with look for all the POs and counterparties
 * associated. Finally, it looks for redundancy of pairs POs-CTPYs and generates a file with the format Process_day | PO
 * | CTPY | CTPY_full_name. Example: 23/01/2013|BDSD|5MLU|MILLENIUM PARTNERS L.P., NEW YORK
 * 
 * @author Jose Sevillano & Guillermo Solano
 * @version 3.2
 * 
 * @date 07/05/2013
 * 
 */
/*
 * GSM: 06/05/2013. Instead of contract type, LEs relations have to be excluded using the product subtype.
 */
public class ExportLegalEntityReport extends MarginCallReport {

	// private constants
	private static final long serialVersionUID = 768666933782687789L;
	private final static String CONTROL_M_ERROR_DESC = "Error while retrieving Margin Call Contract related to the Legal Entity ";
	private final static String ERROR_RESULT = "Not document generated";
	private final static String ERROR_DESC = "Legal Entity Export CANNOT continue. Error in class:";
	private final static String ATTRIBUTE_EMPTY_MESSAGE = " attribute cannot be empty!";
	private final static String CLASS_NAME = ExportLegalEntityReport.class.getCanonicalName();

	// general constants
	public static final String EXPORT_LEGAL_ENTITY_REPORT = ExportLegalEntityReport.class.getName();
	public static final String EMPTY = "";
	public static final String CLOSED = "CLOSED"; // identifies a closed MC Contract
	public static final String PO_LIST = "PO List for export LE";
	public static final String SEPARATOR_PROCESSING_ORG = "Separator for several POs";

	// var read from the template
	private String poList;
	private String separator_poList;
	private JDate valDate;
	private String productList;
	private String separator_productList;
	// GSM: 08/01/14; added Exclude Contract Type
	private String excludeMCContract;

	// JTD: 28/01/15: BAU 5.5 - added ContractType
	// private String contractType;

	/**
	 * Override method load to generate the file (the report).
	 * 
	 * @param errorsMsgs
	 *            passed by parameter
	 * @return the ReportOutput to generate the report
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorsMsgs) {

		return loadOptimizedExportLegalEntity(errorsMsgs);

	}

	/**
	 * Optimize version of the legalEntity export to generate the file (the report).
	 * 
	 * @param errorsMsgs
	 *            passed by parameter
	 * @return the ReportOutput to generate the report
	 */
	// BAU 5.5 - New column "overnight" - only export cpty susi repo
	private ReportOutput loadOptimizedExportLegalEntity(final Vector<String> errorsMsgs) {

		final DefaultReportOutput output = new StandardReportOutput(this);
		// final map, ensures redundancies control
		final Map<String, ReportRow> reportRowsMap = new HashMap<String, ReportRow>();
		Set<String> poSet = null; // non duplicate POs for each contract
		Set<LegalEntity> leSet = null; // idem with counterparties - legal ent.
		String errorMesg = EMPTY;

		/* control check and retrieve the data from the template of the schedule task */
		if (!readAndCheckTemplateAttributes()) {

			Log.error(CLASS_NAME, errorMesg = generateErrorsMessage()); // log messages
			errorsMsgs.add(errorMesg); // report messages
			return output;
		}
		// processing the report starts //
		/* Map generated with the pair: for each PO input in the schedule task, a list with all the MCcontracts */
		final Map<LegalEntity, List<CollateralConfig>> MCConfigListofPOs = retrieveMarginCallConfigListForPOMap();

		// We retrieve all the attributes.
		// final Attributes attributes = getReportTemplate().getAttributes();
		// if ((attributes.get(CONTRACT_TYPE) != null) && (attributes.get(PRODUCT_LIST) != null)) {
		// if (attributes.get(CONTRACT_TYPE).equals("ISMA") && attributes.get(PRODUCT_LIST).equals("Repo")) {
		// overnight = true;
		// }
		// }

		for (LegalEntity legalEntPO : MCConfigListofPOs.keySet()) { // for each PO input in the schedule task

			// current contract for this POs iteration
			final List<CollateralConfig> mccListOfThePO = MCConfigListofPOs.get(legalEntPO);

			// We loop over the list of Margin Calls retrieved for the PO
			for (int numMC = 0; numMC < mccListOfThePO.size(); numMC++) {

				final CollateralConfig currentMcc = mccListOfThePO.get(numMC); // current MCC

				/* we retrieve the set of POs and Counterparties for the Margin contract of the current PO legal entity */
				poSet = getSetsOfPOForMCC(legalEntPO, currentMcc); // non duplicate set of POs
				leSet = getSetsOfCTPYsForMCC(legalEntPO, currentMcc); // non duplicate set of CTPYs

				for (String PO : poSet) { // for each PO of the Set

					for (LegalEntity CTPY : leSet) { // and for each Counterparty of the current PO

						final String noRepetionKEY = PO + CTPY.getAuthName();

						if (!reportRowsMap.containsKey(noRepetionKEY)) { // we check non duplicate pair
																		 // PO+CTPY in the final Map
							final ReportRow repRow = new ReportRow(ExportLegalEntityLogic.getExportLegalEntity(PO,
									CTPY, this.valDate));

							// if (overnight) {
							// repRow = new ReportRow(ExportLegalEntityLogic.getExportLegalEntity(PO, CTPY,
							// this.valDate, overnight));
							// } else {
							// repRow = new ReportRow(ExportLegalEntityLogic.getExportLegalEntity(PO, CTPY,
							// this.valDate));
							// }

							repRow.setProperty(ReportRow.MARGIN_CALL_CONFIG, currentMcc);
							reportRowsMap.put((noRepetionKEY), repRow);
						}
					}
				}
			}
		}
		// generation of the output
		output.setRows(reportRowsMap.values().toArray(new ReportRow[0]));
		return output;
	}

	/**
	 * Reads all the attributes passed through the schedule task template, acts as a constructor of all the require
	 * class variables and finally it checks that all the necessary data for constructing the data is available
	 * 
	 * @return true is all the data is available, false othercase
	 */
	private boolean readAndCheckTemplateAttributes() {

		this.poList = this.separator_poList = EMPTY;
		this.valDate = null;
		this.productList = this.separator_productList = EMPTY;
		this.excludeMCContract = EMPTY;
		// this.contractType = EMPTY;

		// We get the valuation date to put in the export properly.
		this.valDate = getReportTemplate().getValDate();

		// We retrieve all the attributes.
		final Attributes attributes = getReportTemplate().getAttributes();

		// check we have the correct attributes from the template
		if (null != attributes.get(SEPARATOR_PRODUCT_TYPES)) {
			this.separator_productList = attributes.get(SEPARATOR_PRODUCT_TYPES).toString().trim();
		}

		if (null != attributes.get(PRODUCT_LIST)) {
			this.productList = attributes.get(PRODUCT_LIST).toString().trim();
		}

		if (null != attributes.get(PO_LIST)) {
			this.poList = attributes.get(PO_LIST).toString().trim();
		}

		if (null != attributes.get(SEPARATOR_PROCESSING_ORG)) {
			this.separator_poList = attributes.get(SEPARATOR_PROCESSING_ORG).toString().trim();
		}

		if (null != attributes.get(DISCRIMINATE_CONTRACT_TYPE)) {
			this.excludeMCContract = attributes.get(DISCRIMINATE_CONTRACT_TYPE).toString().trim();
		}

		// if (null != attributes.get(CONTRACT_TYPE)) {
		// this.contractType = attributes.get(CONTRACT_TYPE).toString().trim();
		// }

		// check conditions necessary to continue with the processing
		return conditionsToBeSatisfiedToGenerateReport();

	}

	/**
	 * Contains the logic of conditions that have to be satisfied on the class variables to continue the reporting
	 * generation.
	 * 
	 * @return true conditions satisfied
	 */
	private boolean conditionsToBeSatisfiedToGenerateReport() {

		if (this.poList.equals(EMPTY) || this.separator_poList.equals(EMPTY) || this.productList.equals(EMPTY)
				|| this.separator_productList.equals(EMPTY)) { // meter
			return false;

		}

		return true;
	}

	/**
	 * @return suitable Log error message
	 */
	private String generateErrorsMessage() {

		StringBuilder error = new StringBuilder(ERROR_DESC + EXPORT_LEGAL_ENTITY_REPORT + " \n");

		if (this.poList.equals(EMPTY)) {
			error.append(PO_LIST).append(ATTRIBUTE_EMPTY_MESSAGE);
		}

		if (this.separator_productList.equals(EMPTY)) {
			error.append(SEPARATOR_PRODUCT_TYPES).append(ATTRIBUTE_EMPTY_MESSAGE);
		}

		if (this.productList.equals(EMPTY)) {
			error.append(PRODUCT_LIST).append(ATTRIBUTE_EMPTY_MESSAGE);
		}

		if (this.separator_poList.equals(EMPTY)) {
			error.append(SEPARATOR_PROCESSING_ORG).append(ATTRIBUTE_EMPTY_MESSAGE);
		}
		// if (this.contractType.equals(EMPTY)) {
		// error.append(CONTRACT_TYPE).append(ATTRIBUTE_EMPTY_MESSAGE);
		// }

		return error.toString();
	}

	/**
	 * @return a Map with the pair {for each PO, a list with all the MCcontracts associated}
	 */
	private Map<LegalEntity, List<CollateralConfig>> retrieveMarginCallConfigListForPOMap() {

		LegalEntity legalEnt = null;
		Map<LegalEntity, List<CollateralConfig>> POMCList = new HashMap<LegalEntity, List<CollateralConfig>>();

		/* We get the information for each PO specified in the box from the Scheduled Task config. */
		final String[] poDivided = this.poList.split(this.separator_poList);

		for (int numPO = 0; numPO < poDivided.length; numPO++) {

			try {
				// We retrieve from the system the Legal Entity.
				legalEnt = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(poDivided[numPO]);

				if (legalEnt != null) {

					final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
					final List<CollateralConfig> marginCallConfigList = srvReg.getCollateralDataServer()
							.getAllMarginCallConfig(legalEnt.getId(), 0);

					// get only concrete type contracts
					final ArrayList<CollateralConfig> mccFinalList = new ArrayList<CollateralConfig>();

					for (int nc = 0; nc < marginCallConfigList.size(); nc++) {

						final CollateralConfig currentMCC = marginCallConfigList.get(nc);

						// if the MCContract is SUITABLE
						if (checkContractIsSuitable(currentMCC)) {
							mccFinalList.add(currentMCC); // contract added to extract LE relations!
						}

					}
					// We store the PO Legal Entity and his list of MarginCallConfig associated
					if (!POMCList.containsKey(legalEnt)) {
						POMCList.put(legalEnt, mccFinalList);
					}

				} else {
					Log.error(CLASS_NAME, CONTROL_M_ERROR_DESC);
					ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, ERROR_RESULT);// CONTROL-M
					// ERROR
				}

			} catch (final RemoteException e) {
				Log.error(CONTROL_M_ERROR_DESC, e); // este siempre va a ser null!!!
				ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, ERROR_RESULT);// CONTROL-M
				// ERROR
			}

		}
		return POMCList;
	}

	/**
	 * This is the method to filter any contract. For any new condition, return false to avoid adding the pair of POs &
	 * CTPYs of the current contract under analysis. If the method returns true, this contract will be valid.
	 * 
	 * @param currentMCContract
	 *            margin call contract under study
	 * @return true if the Margin Call contract is suitable for generating the files
	 */
	private boolean checkContractIsSuitable(final CollateralConfig currentMCContract) {

		// if there isn't no status of the contract (Should NOT happen)
		if ((currentMCContract.getAgreementStatus() == null)) {
			return false;
		}

		// if the status of the contract is "CLOSED" it is NOT required
		if (currentMCContract.getAgreementStatus().equals(CLOSED)) {
			return false;
		}

		// GSM: 09/01/2014. If discriminated MC is selected, contracts of this type will be discarded
		if (!this.excludeMCContract.equals(EMPTY)
				&& currentMCContract.getContractType().equalsIgnoreCase(this.excludeMCContract)) {
			return false;
		}

		// Exclude CSA Facade
		if (KGR_Collateral_MarginCallReport.CSA_FACADE.equals(currentMCContract.getContractType())){
			return false;
		}
		
		// GSM: 06/05/2013. Instead of contract type, LEs relations will be included using the product type.
		if (contractContainsProduct(currentMCContract)) {
			return true;
		}
		
		return false;
	}

	/**
	 * Extracts the list of products read as an attribute from the ST. It extracts too the product list of the contract.
	 * 
	 * @param contract
	 *            to be checked
	 * @return If the contract contains AT LEAST ONE product from the attributes, returns true.
	 */
	private boolean contractContainsProduct(final CollateralConfig contract) {

		if ((this.productList == null) || this.productList.equals(EMPTY)) {
			return false;
		}
		if ((this.separator_productList == null) || this.separator_productList.equals(EMPTY)) {
			return false;
		}
		if ((contract == null) || (contract.getProductList() == null) || contract.getProductList().isEmpty()) {
			return false;
		}

		final String[] toCheck = this.productList.split(this.separator_productList.trim());

		if ((toCheck == null) || (toCheck.length < 1)) {
			return false;
		}

		final Set<String> suitableProductList = new HashSet<String>(Arrays.asList(toCheck));
		final Set<String> contractProductList = new HashSet<String>(contract.getProductList());

		for (String productToBeChecked : suitableProductList) {

			if (contractProductList.contains(productToBeChecked.trim())) {
				return true; // at least we have this product in the contract
			}
		}

		return false;

	}

	/**
	 * @param legalEnt
	 *            main PO of the contract
	 * @param currentMcc
	 *            contract
	 * @return a Set of all the POs of the contract
	 */
	private Set<String> getSetsOfPOForMCC(LegalEntity legalEnt, CollateralConfig currentMcc) {

		Set<String> poSet = new HashSet<String>();
		List<LegalEntity> listAdditionalPO = null; // GSM

		/* We insert the PO read from the schedule panel */
		poSet.add(legalEnt.getCode().trim()); // GSM
		// We check if the Legal Entity passed is PO in the
		// MArgin Call Contract, not CP.
		if (currentMcc.getProcessingOrg().getId() == legalEnt.getId()) {

			listAdditionalPO = currentMcc.getAdditionalPO();

			for (LegalEntity le : listAdditionalPO) {
				poSet.add(le.getAuthName());
			}
		}

		return poSet;

	}

	/**
	 * @param legalEnt
	 *            main PO of the contract
	 * @param currentMcc
	 *            contract
	 * @return a Set of all the Counterparties of the contract
	 */
	private Set<LegalEntity> getSetsOfCTPYsForMCC(LegalEntity legalEnt, CollateralConfig currentMcc) {

		Set<LegalEntity> leSet = new HashSet<LegalEntity>();
		List<LegalEntity> listLE = null;

		if (currentMcc.getProcessingOrg().getId() == legalEnt.getId()) {

			listLE = currentMcc.getAdditionalLE();
			leSet.add(currentMcc.getLegalEntity());
			leSet.addAll(listLE);

		}

		return leSet;
	}
}