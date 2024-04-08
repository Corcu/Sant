package calypsox.tk.upload.validator;

import calypsox.ctm.util.CTMTradeFinder;
import calypsox.ctm.util.CTMUploaderConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.upload.jaxb.CalypsoTrade;

import java.util.function.Function;

/**
 * @author aalonsop
 */
public class ValidateCalypsoTradeBondION extends ValidateCalypsoTradeBondCTM{

    @Override
    protected Trade getBlockTrade(CalypsoTrade dupTrade, Function<String, Trade> finderFunction, String referenceKwdName) {
        return super.getBlockTrade(dupTrade,CTMTradeFinder::findIONBlockTrade,CTMUploaderConstants.ALLOCATED_FROM_MX_GLOBALID);
    }
}
