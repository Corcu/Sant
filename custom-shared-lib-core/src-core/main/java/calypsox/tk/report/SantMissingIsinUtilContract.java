package calypsox.tk.report;

public class SantMissingIsinUtilContract {
    int contractId = 0;
    long repoTripartyId = 0;

    public int getContractId() {
        return contractId;
    }

    public void setContractId(int contractId) {
        this.contractId = contractId;
    }

    public long getRepoTripartyId() {
        return repoTripartyId;
    }

    public void setRepoTripartyId(long repoTripartyId) {
        this.repoTripartyId = repoTripartyId;
    }

    public boolean isRepoTriparty() {
        if(repoTripartyId !=0){
            return true;
        }
        return false;
    }

    public boolean foundContract(){
        return contractId!=0 || repoTripartyId !=0;
    }

    @Override
    public String toString() {
        return contractId!=0 ? String.valueOf(contractId) : repoTripartyId !=0 ? String.valueOf(repoTripartyId) : "";
    }
}
