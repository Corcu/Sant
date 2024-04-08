package calypsox.tk.anacredit.loader;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.ProductConst;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.EquityIndex;
import com.calypso.tk.product.sql.EquityIndexQuoteNamesSQL;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.util.DateFormattingUtil;

import java.util.*;

public class AnacreditLoaderSecuritiesRV extends AnacreditLoaderSecurity {
    @Override
    protected boolean isValidContract(CollateralConfig config, InventorySecurityPosition position) {
        return (null != position.getProduct() && position.getProduct() instanceof  Equity);
    }
}
