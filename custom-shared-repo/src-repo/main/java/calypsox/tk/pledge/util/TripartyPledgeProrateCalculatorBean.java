package calypsox.tk.pledge.util;

import com.calypso.tk.core.Trade;
import com.calypso.tk.report.ReportRow;

import java.util.ArrayList;
import java.util.List;

public class TripartyPledgeProrateCalculatorBean {
    Trade fatherRepo = null;
    List<ReportRow> rows = new ArrayList<>();
    Double mtmSUM = 0.0D;
    Double accrualSUM = 0.0D;
    Double principalSUM = 0.0D;
    Double fatherRepoTotalMTM = 0.0D;
    Double fatherRepoTotalAccrual = 0.0D;
    Double fatherRepoTotalPrincipal = 0.0D;

    public Long getFatherRepoId(){
        return null!=fatherRepo ? fatherRepo.getLongId() : 0L;
    }

    public void setFatherRepo(Trade repo){
        fatherRepo = repo;
    }
    public Trade getFatherRepo(){
        return fatherRepo;
    }
    public void addRow(ReportRow row){
        rows.add(row);
    }
    public void sumMTM(double mtm){
        mtmSUM+=mtm;
    }
    public void sumAccrual(double accrual){
        accrualSUM+=accrual;
    }
    public void sumPrincipal(double principal){
        principalSUM+=principal;
    }
    public List<ReportRow> getRows(){
        return rows;
    }
    public Double getMtmSUM() {
        return mtmSUM;
    }
    public Double getAccrualSUM() {
        return accrualSUM;
    }
    public Double getPrincipalSUM() {
        return principalSUM;
    }

    public Double getFatherRepoTotalMTM() {
        return fatherRepoTotalMTM;
    }

    public void setFatherRepoTotalMTM(Double fatherRepoTotalMTM) {
        this.fatherRepoTotalMTM = fatherRepoTotalMTM;
    }

    public Double getFatherRepoTotalAccrual() {
        return fatherRepoTotalAccrual;
    }

    public void setFatherRepoTotalAccrual(Double fatherRepoTotalAccrual) {
        this.fatherRepoTotalAccrual = fatherRepoTotalAccrual;
    }

    public Double getFatherRepoTotalPrincipal() {
        return fatherRepoTotalPrincipal;
    }

    public void setFatherRepoTotalPrincipal(Double fatherRepoTotalPrincipal) {
        this.fatherRepoTotalPrincipal = fatherRepoTotalPrincipal;
    }
}
