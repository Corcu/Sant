/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.quotes;

import com.calypso.tk.core.JDate;

public class QuoteBondBean {

    private JDate closeDate;
    private String collGroup;
    private String collGroupdesc;
    private String collType;
    private String collDescription;
    private String Colldisprice;
    private String Collprice;
    private String InterestRate;
    private String Endyear;
    private String Securitynum;

    public JDate getCloseDate() {
        return this.closeDate;
    }

    public void setCloseDate(final JDate closeDate) {
        this.closeDate = closeDate;
    }

    public String getCollGroup() {
        return this.collGroup;
    }

    public void setCollGroup(final String collGroup) {
        this.collGroup = collGroup;
    }

    public String getCollGroupdesc() {
        return this.collGroupdesc;
    }

    public void setCollGroupdesc(final String collGroupdesc) {
        this.collGroupdesc = collGroupdesc;
    }

    public String getCollType() {
        return this.collType;
    }

    public void setCollType(final String collType) {
        this.collType = collType;
    }

    public String getCollDescription() {
        return this.collDescription;
    }

    public void setCollDescription(final String collDescription) {
        this.collDescription = collDescription;
    }

    public String getColldisprice() {
        return this.Colldisprice;
    }

    public void setColldisprice(final String colldisprice) {
        this.Colldisprice = colldisprice;
    }

    public String getCollprice() {
        return this.Collprice;
    }

    public void setCollprice(final String collprice) {
        this.Collprice = collprice;
    }

    public String getInterestRate() {
        return this.InterestRate;
    }

    public void setInterestRate(final String interestRate) {
        this.InterestRate = interestRate;
    }

    public String getEndyear() {
        return this.Endyear;
    }

    public void setEndyear(final String endyear) {
        this.Endyear = endyear;
    }

    public String getSecuritynum() {
        return this.Securitynum;
    }

    public void setSecuritynum(final String securitynum) {
        this.Securitynum = securitynum;
    }

}
