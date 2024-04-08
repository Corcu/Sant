package calypsox.repoccp.model;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import org.apache.poi.ss.formula.functions.T;

import java.util.Optional;

/**
 * @author aalonsop
 */
public interface ReconCCP<T> {
    ReconCCPMatchingResult match(T object);
    ReconCCPMatchingResult match(T object, double amtTolerance);

    public boolean matchReference(T object);

    public ReconCCPMatchingResult matchFields(T object);
}
