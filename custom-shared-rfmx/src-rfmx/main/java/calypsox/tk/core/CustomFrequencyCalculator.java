package calypsox.tk.core;

import java.util.Optional;
import java.util.Vector;

import com.calypso.tk.core.Frequency;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Tenor;

/**
 * @author X620985.
 * New custom frequencies are created that will be added to the Core Calypso, 
 * defined in the Frequency class. 28D, 30D, 31D, 60D, 90D, 180D, etc.
 */
public class CustomFrequencyCalculator implements com.calypso.tk.core.FrequencyCalculator {

	/* New frequencies, defined at custom level: 28D, 30D, 31D, 60D, 90D, 180D, etc. */
	enum CustomFrequency {
		TWENTY_EIGHT_DAYS("28D", 28),
		THIRTY_DAYS("30D", 30), 
		THIRTY_ONE_DAYS("31D", 31), 
		SIXTY_DAYS("60D", 60),
		NINETY_DAYS("90D", 90), 
		ONE_HUNDRED_EIGHTY_DAYS("180D", 180), 
		THREE_HUNDRED_SIXTY_DAYS("360D", 360),
		THREE_HUNDRED_SIXTY_FIVE_DAYS("365D", 365),
		SEVEN_HUNDRED_TWENTY_EIGHT_DAYS("728D", 728),
		ONE_THOUSAND_NINETY_EIGHT_DAYS("1098D", 1098),
		ONE_THOUSAND_EIGHT_HUNDRED_NINETY_DAYS("1890D", 1890)
		;

		/* Alphanumeric value that indicates the name of the frequency.
		 * Example: 28D, 30D, 31D, 60D, 90D, 180D, 360D, etc. */
		private String name;
		
		/* Numerical value that indicates the number of days of the frequency. 
		 * Example: 28, 30, 31, 60, 90, 180, 360, etc. */
		private int code;

		// Constructor of a (custom) frequency
		CustomFrequency(String nameParam, int codeParam) {
			this.name = nameParam;
			this.code = codeParam;
		}
		
		/* Gets the name of a (custom) frequency. Example: 28D, 30D, 31D, etc. */
		public String getName() {
			return this.name;
		}
		
		/* Gets the code (numeric) of a (custom) frequency. Example: 28, 30, 31, etc. */
		public int getCode() {
			return code;
		}
		
		// Construct a Tenor object based on the name of the (custom) frequency.
		public Tenor toTenor() {
			return new Tenor(name);
		}
		
		/* Based on a tenor object, it will look to see if the associated frequency has 
		 * a code of 30 (30 days equals one month), in which case it will return Frequency.F_MONTHLY. 
		 * The same for the cases: F_QUARTERLY, F_SEMI_ANNUAL and F_ANNUAL. 
		 * In all other cases it will return null. */
		public Frequency toFrequency() {
			return new Tenor(name).toFrequency();
		}
		
		// Returns a frequency (custom) based on its name
		public static CustomFrequency searchByName(String name) {
			for (int i = 0; i < values().length; i++) {
				if (values()[i].getName().equals(name)) {
					return values()[i];
				}
			}
			return null;
		}
		
		// Returns a frequency (custom) based on its code (numeric)
		public static CustomFrequency searchByCode(int code) {
			for (int i = 0; i < values().length; i++) {
				if (values()[i].getCode() == code) {
					return values()[i];
				}
			}
			return null;
		}
	}

	/* Searches for a frequency (custom) based on its name, 
	 * and returns its code (numeric) */
	@Override
	public int fromString(final String frequencyName) {
		if (Optional.ofNullable(CustomFrequency.valueOf(frequencyName)).isPresent()) {
			return CustomFrequency.valueOf(frequencyName).getCode();
		}
		return 0;
	}

	/* It searches for a frequency (custom) based on its name, and if it succeeds, 
	 * it returns the result of applying the 'toFrequency()' method to said frequency. 
	 * The functionality of the 'toFrequency()' method is explained in that method. */
	@Override
	public Frequency get(final String frequencyName) {
		Optional<CustomFrequency> op = Optional.ofNullable(CustomFrequency.searchByName(frequencyName));
		if (op.isPresent()) {
			return op.get().toFrequency();
		}
		Log.info(this, "Frequency with name " + frequencyName +  " not found.");
		return null;
	}

	/* It searches for a frequency (custom) based on its code (numeric), and if it succeeds, 
	 * it returns the result of applying the 'toFrequency()' method to said frequency. 
	 * The functionality of the 'toFrequency()' method is explained in that method. */
	@Override
	public Frequency get(final int frequencyCode) {
		Optional<CustomFrequency> op = Optional.ofNullable(CustomFrequency.searchByCode(frequencyCode));
		if (op.isPresent()) {
			return op.get().toFrequency();
		}
		Log.info(this, "Frequency with code " + frequencyCode +  " not found.");
		return null;
	}

	/* Returns the name of all defined custom frequencies. */
	@Override
	public Vector<String> getDomain() {
		Vector<String> domain = new Vector<String>();
		domain.add(CustomFrequency.TWENTY_EIGHT_DAYS.getName());
		domain.add(CustomFrequency.ONE_THOUSAND_NINETY_EIGHT_DAYS.getName());
		domain.add(CustomFrequency.THIRTY_DAYS.getName());
		domain.add(CustomFrequency.THIRTY_ONE_DAYS.getName());
		domain.add(CustomFrequency.SIXTY_DAYS.getName());
		domain.add(CustomFrequency.NINETY_DAYS.getName());
		domain.add(CustomFrequency.ONE_HUNDRED_EIGHTY_DAYS.getName());
		domain.add(CustomFrequency.THREE_HUNDRED_SIXTY_DAYS.getName());
		domain.add(CustomFrequency.THREE_HUNDRED_SIXTY_FIVE_DAYS.getName());
		domain.add(CustomFrequency.SEVEN_HUNDRED_TWENTY_EIGHT_DAYS.getName());
		domain.add(CustomFrequency.ONE_THOUSAND_NINETY_EIGHT_DAYS.getName());
		domain.add(CustomFrequency.ONE_THOUSAND_EIGHT_HUNDRED_NINETY_DAYS.getName());
		return domain;
	}

	/* Searches for a frequency based on its code (numeric) and if it exists, 
	 * returns its associated tenor. */
	@Override
	public Tenor getTenor(final int frequencyCode) {
		Optional<CustomFrequency> op = Optional.ofNullable(CustomFrequency.searchByCode(frequencyCode));
		if (op.isPresent()) {
			return op.get().toTenor();
		}
		Log.info(this, "The frequency with code " + frequencyCode + 
				             " was not found, so its tenor could not be created.");
		return null;
	}

	/* Searches for a frequency based on its code (numeric) and if it exists, returns its name. */
	@Override
	public String toString(final int frequencyCode) {
		Optional<CustomFrequency> op = Optional.ofNullable(CustomFrequency.searchByCode(frequencyCode));
		if (op.isPresent()) {
			return op.get().getName();
		}
		Log.info(this, "The frequency with code " + frequencyCode + " was not found, "
				                         + "so its name cannot be returned.");
		return null;
	}

}
