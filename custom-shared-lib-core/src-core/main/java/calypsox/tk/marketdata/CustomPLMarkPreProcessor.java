/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.marketdata;

import java.util.List;

import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkPreProcessor;

/**
 * Custom class only created to avoid unappropriated error messages in the logs.
 * 
 * @author OA
 * 
 */
public class CustomPLMarkPreProcessor implements PLMarkPreProcessor {

	@Override
	public boolean preProcess(PLMark plMark, List<String> errors) {
		return true;
	}

}
