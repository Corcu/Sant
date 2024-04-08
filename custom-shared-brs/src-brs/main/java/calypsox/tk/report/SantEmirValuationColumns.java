/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

/**
 * Enum class for Sant Emir Snapshot report
 *
 * @author xIS15793
 *
 */
// CAL_EMIR_026
public enum SantEmirValuationColumns {
    COMMENT,
    MESSAGETYPE,
    ACTION,
    // USIPREFIX,
    // USIVALUE,
    ASSETCLASS,
    PRODUCTVALUE,

    VALDATETIME,
    MTMVALUE,
    MTMVALUECCP,
    MTMVALUEPARTY2,
    MTMCURRENCY,
    MTMCURRENCYCCP,
    MTMCURRENCYPARTY2,
    VALUATIONTYPEPARTY1,
    SENDTO,
    PARTYREPOBLIGATION1,
    //PARTYREPOBLIGATIONL,
    // FullToDeltaSubmitter
    TRADEPARTYPREF1,
    TRADEPARTYVAL1,
    TRADEPARTYPREF2,
    TRADEPARTYVAL2,
    LEIPREFIX,
    LEIVALUE,
    TRADEPARTYTRANSACTIONID1,
    UTIPREFIX,
    UTI,
    SUBMITTEDFORPREFIX,
    SUBMITTEDVALUE,
    CLEARINGSTATUS,
    CLEARPRODUCTID,
    ACTIONTYPE1,
    PARTY1REPORTINGONL,
    PRIMASSETCLASS,

    // LEVEL;
    // FullToDeltaSubmitter - End
    ADDCOMMENTS; // DDR - Inform GLCS 08/18;
    /**
     * Get columns method for SantEmirValuation report.
     *
     * @return columns
     */
    public static String[] getColumns() {
        final SantEmirValuationColumns[] values = SantEmirValuationColumns
                .values();
        final String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].toString();
        }
        return result;
    }
}
