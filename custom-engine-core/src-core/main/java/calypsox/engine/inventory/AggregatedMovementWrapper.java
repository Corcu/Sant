/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine.inventory;

import java.util.HashMap;
import java.util.Map;

import com.calypso.tk.core.JDate;

/**
 * This class allows to aggregate movements per type (THEOR, ACTUAL, BLOQUEO) and date Movement @ D = Sum of Previous
 * Movement Movement @ D+3 = Movement calculated @ D+3 passed in method + Movement @ D+2 + Movement @ D+1 + Movement @ D
 * 
 * @author Patrice Guerrido
 * @version 1.0
 */
public class AggregatedMovementWrapper {

	/**
	 * Movements by date
	 */
	private final Map<JDate, Map<String, Double>> mainMap = new HashMap<JDate, Map<String, Double>>();

	public double getMovement(final JDate date, final String key) {
		if ((this.mainMap.get(date) == null) || (this.mainMap.get(date).get(key) == null)) {
			return 0.0;
		}
		return this.mainMap.get(date).get(key);
	}

	/**
	 * Adds a movement to a previous date
	 * 
	 * @param previousDate
	 * @param currentDate
	 * @param key
	 * @param movement
	 */
	public void addMovement(final JDate previousDate, final JDate currentDate, String key, final double movement) {
		double previousMovement = 0.0;
		if (previousDate != null) {
			if ((this.mainMap.get(previousDate) != null) && (this.mainMap.get(previousDate).get(key) != null)) {
				previousMovement = this.mainMap.get(previousDate).get(key);
			}
		}

		Map<String, Double> innerMap = this.mainMap.get(currentDate);
		if (innerMap == null) {
			innerMap = new HashMap<String, Double>();
			this.mainMap.put(currentDate, innerMap);
		}
		innerMap.put(key, previousMovement + movement);

	}
}
