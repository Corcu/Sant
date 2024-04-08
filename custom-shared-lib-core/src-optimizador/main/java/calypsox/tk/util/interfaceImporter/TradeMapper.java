package calypsox.tk.util.interfaceImporter;

import com.calypso.tk.core.Trade;

/**
 * Read
 * 
 * @author aela
 * 
 */
interface TradeMapper<T> {

	Trade map(T trade) throws Exception;
}
