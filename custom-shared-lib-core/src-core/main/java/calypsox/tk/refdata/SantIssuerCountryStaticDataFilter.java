package calypsox.tk.refdata;

import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.HedgeRelationship;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.util.concentrationlimits.SantConcentrationLimitsProductMapper;

public class SantIssuerCountryStaticDataFilter
        implements StaticDataFilterInterface {

    public static final String SDF_ATTRIBUTE_ISSUER_COUNTRY = "Issuer Country";

    @Override
    public boolean fillTreeList(DSConnection ds, TreeList treeList) {
        treeList.add(StaticDataFilterElement.PRODUCT,
                SDF_ATTRIBUTE_ISSUER_COUNTRY);

        return false;
    }

    @Override
    public Vector getDomain(DSConnection ds, String s) {
        Vector<Country> countries = BOCache.getCountries(ds);

        Vector<String> countryNames = new Vector<String>();
        for (Country country : countries) {
            countryNames.add(country.getName());
        }

        return countryNames;
    }

    @Override
    public void getDomainValues(DSConnection ds, Vector domainValues) {
        domainValues.add(SDF_ATTRIBUTE_ISSUER_COUNTRY);
    }

    @Override
    public Vector getTypeDomain(String attributeName) {
        Vector<String> typeDomain = new Vector<String>();
        typeDomain.add(StaticDataFilterElement.S_IN);

        return typeDomain;
    }

    @Override
    public Object getValue(Trade trade, LegalEntity le, String role,
            Product product, BOTransfer transfer, BOMessage message,
            TradeTransferRule rule, ReportRow reportRow, Task task,
            Account glAccount, CashFlow cashflow,
            HedgeRelationship relationship, String filterElement,
            StaticDataFilterElement element) {
        String issuerCountry = null;

        if (product != null) {
            int issuerId = 0;
            if (product instanceof Bond) {
                Bond bond = (Bond) product;
                issuerId = bond.getIssuerId();
            } else if (product instanceof Equity) {
                Equity equity = (Equity) product;
                issuerId = equity.getIssuerId();
            }

            issuerCountry = SantConcentrationLimitsProductMapper
                    .getCountryFromIssuerId(issuerId);
        }

        return issuerCountry;
    }

    @Override
    public boolean isTradeNeeded(String arg0) {
        return false;
    }

}
