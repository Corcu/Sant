package calypsox.tk.util.emir;

import com.calypso.tk.core.JDatetime;

import java.util.HashMap;
import java.util.Map;

public class SnapshotStatus {
  private boolean booked = false;
  private boolean bookedFar = false;
  private int bookedVersion = 0;
  private int bookedFarVersion = 0;

  private boolean canceled = false;
  private boolean canceledFar = false;
  private int canceledVersion = 0;
  private int canceledFarVersion = 0;

  private boolean terminated = false;
  private boolean terminatedFar = false;
  private int terminatedVersion = 0;
  private int terminatedFarVersion = 0;

  private JDatetime confirmationDateTime = null;
  private JDatetime confirmationDateTimeFar = null;

  // DDR v25
  private boolean leiChanged = false;
  private boolean leiChangedFar = false;
  private int leiChangedVersion = 0;
  private int leiChangedFarVersion = 0;

  private boolean isReportable = true;

  private boolean utiChanged = false;
  private boolean utiChangedFar = false;
  private int utiChangedVersion = 0;
  private int utiChangedFarVersion = 0;

  private boolean rateAmendment = false;
  private int rateAmendmentVersion = 0;

  private boolean isCancelReissue = false;
  private int cancelReissueVersion = 0;

  private boolean isPortfolioModification = false;
  private int portfolioModificationVersion = 0;

  private boolean isPortfolioAssignment = false;
  private int portfolioAssignmentVersion = 0;

  private boolean isAdditionalFlowAmendment = false;
  private int additionalFlowAmendmentVersion = 0;

  private boolean isRestructured = false;
  private int restructuredVersion = 0;

  private boolean delegateReportabilityChanged = false;

  private boolean isSharesModification = false;
  private int sharesModificationVersion = 0;

  private boolean isCptyAmend = false;
  private int cptyAmendVersion = 0;

  private boolean isMaturityExtension = false;
  private int maturityExtensionVersion = 0;

  private boolean isUndo = false;
  private int undoVersion = 0;

  private boolean isUndoTerm = false;
  private int undoTermVersion = 0;

  private boolean isAmortizationChange = false;
  private int amortizationChangeVersion = 0;


  private boolean isModifyUserField = false;
  private int modifyUserFieldVersion = 0;



  private final Map<String, String> keywordsTradingVenue = new HashMap<String, String>();

  public void clear() {
    setDelegateReportabilityChanged(false);
    setBooked(false);
    setBookedFar(false);
    setBookedVersion(0);
    setBookedFarVersion(0);
    setCanceled(false);
    setCanceledFar(false);
    setCanceledVersion(0);
    setCanceledFarVersion(0);
    setTerminated(false);
    setTerminatedFar(false);
    setTerminatedVersion(0);
    setTerminatedFarVersion(0);
    setConfirmationDateTime(null);
    setConfirmationDateTimeFar(null);
    setLeiChanged(false);
    setLeiChangedFar(false);
    setLeiChangedVersion(0);
    setLeiChangedFarVersion(0);
    setReportable(true);
    setUtiChanged(false);
    setUtiChangedFar(false);
    setUtiChangedVersion(0);
    setUtiChangedFarVersion(0);
    setRateAmendment(false);
    setRateAmendmentVersion(0);
    setCancelReissue(false);
    setCancelReissueVersion(0);
    setPortfolioModification(false);
    setPortfolioModificationVersion(0);
    setPortfolioAssignment(false);
    setPortfolioAssignmentVersion(0);
    setRestructured(false);
    setRestructuredVersion(0);
    setSharesModification(false);
    setSharesModificationVersion(0);
    setCptyAmend(false);
    setCptyAmendVersion(0);
    setMaturityExtension(false);
    setMaturityExtensionVersion(0);
    setUndo(false);
    setUndoVersion(0);
    setUndoTerm(false);
    setUndoTermVersion(0);
    setModifyUserField(false);
    setModifyUserFieldVersion(0);
    setAmortizationChange(false);
    setAmortizationChangeVersion(0);

    getKeywordsTradingVenue().clear();
  }

  public boolean isBooked() {
    return booked;
  }

  public void setBooked(boolean booked) {
    this.booked = booked;
  }

  public boolean isBookedFar() {
    return bookedFar;
  }

  public void setBookedFar(boolean bookedFar) {
    this.bookedFar = bookedFar;
  }

  public int getBookedVersion() {
    return bookedVersion;
  }

  public void setBookedVersion(int bookedVersion) {
    this.bookedVersion = bookedVersion;
  }

  public int getBookedFarVersion() {
    return bookedFarVersion;
  }

  public void setBookedFarVersion(int bookedFarVersion) {
    this.bookedFarVersion = bookedFarVersion;
  }

  public boolean isCanceled() {
    return canceled;
  }

  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  public boolean isCanceledFar() {
    return canceledFar;
  }

  public void setCanceledFar(boolean canceledFar) {
    this.canceledFar = canceledFar;
  }

  public int getCanceledVersion() {
    return canceledVersion;
  }

  public void setCanceledVersion(int canceledVersion) {
    this.canceledVersion = canceledVersion;
  }

  public int getCanceledFarVersion() {
    return canceledFarVersion;
  }

  public void setCanceledFarVersion(int canceledFarVersion) {
    this.canceledFarVersion = canceledFarVersion;
  }

  public boolean isTerminated() {
    return terminated;
  }

  public void setTerminated(boolean terminated) {
    this.terminated = terminated;
  }

  public boolean isTerminatedFar() {
    return terminatedFar;
  }

  public void setTerminatedFar(boolean terminatedFar) {
    this.terminatedFar = terminatedFar;
  }

  public int getTerminatedVersion() {
    return terminatedVersion;
  }

  public void setTerminatedVersion(int terminatedVersion) {
    this.terminatedVersion = terminatedVersion;
  }

  public int getTerminatedFarVersion() {
    return terminatedFarVersion;
  }

  public void setTerminatedFarVersion(int terminatedFarVersion) {
    this.terminatedFarVersion = terminatedFarVersion;
  }

  public JDatetime getConfirmationDateTime() {
    return confirmationDateTime;
  }

  public void setConfirmationDateTime(JDatetime confirmationDateTime) {
    this.confirmationDateTime = confirmationDateTime;
  }

  public JDatetime getConfirmationDateTimeFar() {
    return confirmationDateTimeFar;
  }

  public void setConfirmationDateTimeFar(JDatetime confirmationDateTimeFar) {
    this.confirmationDateTimeFar = confirmationDateTimeFar;
  }

  public boolean isLeiChanged() {
    return leiChanged;
  }

  public void setLeiChanged(boolean leiChanged) {
    this.leiChanged = leiChanged;
  }

  public boolean isLeiChangedFar() {
    return leiChangedFar;
  }

  public void setLeiChangedFar(boolean leiChangedFar) {
    this.leiChangedFar = leiChangedFar;
  }

  public int getLeiChangedVersion() {
    return leiChangedVersion;
  }

  public void setLeiChangedVersion(int leiChangedVersion) {
    this.leiChangedVersion = leiChangedVersion;
  }

  public int getLeiChangedFarVersion() {
    return leiChangedFarVersion;
  }

  public void setLeiChangedFarVersion(int leiChangedFarVersion) {
    this.leiChangedFarVersion = leiChangedFarVersion;
  }

  public boolean isReportable() {
    return isReportable;
  }

  public void setReportable(boolean isReportable) {
    this.isReportable = isReportable;
  }

  public boolean isUtiChanged() {
    return utiChanged;
  }

  public void setUtiChanged(boolean utiChanged) {
    this.utiChanged = utiChanged;
  }

  public boolean isUtiChangedFar() {
    return utiChangedFar;
  }

  public void setUtiChangedFar(boolean utiChangedFar) {
    this.utiChangedFar = utiChangedFar;
  }

  public int getUtiChangedVersion() {
    return utiChangedVersion;
  }

  public void setUtiChangedVersion(int utiChangedVersion) {
    this.utiChangedVersion = utiChangedVersion;
  }

  public int getUtiChangedFarVersion() {
    return utiChangedFarVersion;
  }

  public void setUtiChangedFarVersion(int utiChangedFarVersion) {
    this.utiChangedFarVersion = utiChangedFarVersion;
  }

  public void setRateAmendment(boolean rateChanged) {
    this.rateAmendment = rateChanged;
  }

  public boolean isRateAmendment() {
    return rateAmendment;
  }

  public void setRateAmendmentVersion(int rateAmendmentVersion) {
    this.rateAmendmentVersion = rateAmendmentVersion;
  }

  public int getRateAmendmentVersion() {
    return rateAmendmentVersion;
  }

  public Map<String, String> getKeywordsTradingVenue() {
    final Map<String, String> kwsClone = new HashMap<String, String>();
    for (final Map.Entry<String, String> entry : keywordsTradingVenue.entrySet()) {
      kwsClone.put(entry.getKey(), entry.getValue());
    }
    return kwsClone;
  }

  public void addKeywordsTradingVenue(final String key, final String value) {
    keywordsTradingVenue.put(key, value);
  }

  public boolean isCancelReissue() {
    return isCancelReissue;
  }

  public void setCancelReissue(boolean cancelReissue) {
    isCancelReissue = cancelReissue;
  }

  public int getCancelReissueVersion() {
    return cancelReissueVersion;
  }

  public void setCancelReissueVersion(int cancelReissueVersion) {
    this.cancelReissueVersion = cancelReissueVersion;
  }

  public boolean isPortfolioModification() {
    return isPortfolioModification;
  }

  public void setPortfolioModification(boolean portfolioModification) {
    isPortfolioModification = portfolioModification;
  }

  public int getPortfolioModificationVersion() {
    return portfolioModificationVersion;
  }

  public void setPortfolioModificationVersion(int portfolioModificationVersion) {
    this.portfolioModificationVersion = portfolioModificationVersion;
  }

  public boolean isPortfolioAssignment() {
    return isPortfolioAssignment;
  }

  public void setPortfolioAssignment(boolean portfolioAsignment) {
    isPortfolioAssignment = portfolioAsignment;
  }

  public int getPortfolioAssignmentVersion() {
    return portfolioAssignmentVersion;
  }

  public void setPortfolioAssignmentVersion(int portfolioAsignmentVersion) {
    this.portfolioAssignmentVersion = portfolioAsignmentVersion;
  }

  public boolean isAdditionalFlowAmendment() {
    return isAdditionalFlowAmendment;
  }

  public void setAdditionalFlowAmendment(boolean additionalFlowAmendment) {
    isAdditionalFlowAmendment = additionalFlowAmendment;
  }

  public boolean isRestructured() {
    return isRestructured;
  }

  public void setRestructured(boolean restructured) {
    isRestructured = restructured;
  }


  public void setRestructuredVersion(int isRestructuredVersion) {
    this.restructuredVersion = isRestructuredVersion;
  }

  public int getRestructuredVersion() {
    return restructuredVersion;
  }

  public boolean isSharesModification() {
    return isSharesModification;
  }

  public void setSharesModification(boolean sharesModification) {
    isSharesModification = sharesModification;
  }

  public int getSharesModificationVersion() {
    return sharesModificationVersion;
  }

  public void setSharesModificationVersion(int sharesModificationVersion) {
    this.sharesModificationVersion = sharesModificationVersion;
  }

  public boolean isCounterpartyAmendment() {
    return isCptyAmend;
  }

  public void setCptyAmend(boolean cptyAmend) {
    isCptyAmend = cptyAmend;
  }

  public int getCptyAmendVersion() {
    return cptyAmendVersion;
  }

  public void setCptyAmendVersion(int cptyAmendVersion) {
    this.cptyAmendVersion = cptyAmendVersion;
  }

  public boolean isMaturityExtension() {
    return isMaturityExtension;
  }

  public void setMaturityExtension(boolean maturityExtension) {
    isMaturityExtension = maturityExtension;
  }

  public int getMaturityExtensionVersion() {
    return maturityExtensionVersion;
  }

  public void setMaturityExtensionVersion(int maturityExtensionVersion) {
    this.maturityExtensionVersion = maturityExtensionVersion;
  }

  public boolean isUndo() {
    return isUndo;
  }

  public void setUndo(boolean undo) {
    isUndo = undo;
  }

  public int getUndoVersion() {
    return undoVersion;
  }

  public void setUndoVersion(int undoVersion) {
    this.undoVersion = undoVersion;
  }

  public int getUndoTermVersion() {
    return undoTermVersion;
  }

  public void setUndoTermVersion(int undoTermVersion) {
    this.undoTermVersion = undoTermVersion;
  }

  public boolean isUndoTerm() {
    return isUndoTerm;
  }

  public void setUndoTerm(boolean undoTerm) {
    isUndoTerm = undoTerm;
  }

  public boolean isModifyUserField() {
    return isModifyUserField;
  }

  public void setModifyUserField(boolean modifyUserField) {
    isModifyUserField = modifyUserField;
  }

  public int getModifyUserFieldVersion() {
    return modifyUserFieldVersion;
  }

  public void setModifyUserFieldVersion(int modifyUserFieldVersion) {
    this.modifyUserFieldVersion = modifyUserFieldVersion;
  }

  public int getAdditionalFlowAmendmentVersion() {
    return additionalFlowAmendmentVersion;
  }

  public void setAdditionalFlowAmendmentVersion(int additionalFlowAmendmentVersion) {
    this.additionalFlowAmendmentVersion = additionalFlowAmendmentVersion;
  }

  public boolean isAmortizationChange() {
    return isAmortizationChange;
  }

  public void setAmortizationChange(boolean amortizationChange) {
    isAmortizationChange = amortizationChange;
  }

  public int getAmortizationChangeVersion() {
    return amortizationChangeVersion;
  }

  public void setAmortizationChangeVersion(int amortizationChangeVersion) {
    this.amortizationChangeVersion = amortizationChangeVersion;
  }

  public boolean isDelegateReportabilityChanged() {
    return delegateReportabilityChanged;
  }

  public void setDelegateReportabilityChanged(boolean delegateReportabilityChanged) {
    this.delegateReportabilityChanged = delegateReportabilityChanged;
  }
}
