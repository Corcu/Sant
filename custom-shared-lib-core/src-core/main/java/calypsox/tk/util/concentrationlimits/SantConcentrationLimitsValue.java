package calypsox.tk.util.concentrationlimits;

public class SantConcentrationLimitsValue {
	private double percentage = 0.0;
	private double amount = 0.0;

	public SantConcentrationLimitsValue(double percentage, double amount) {
		this.percentage = percentage;
		this.amount = amount;
	}

	public double getPercentage() {
		return percentage;
	}

	public double getAmount() {
		return amount;
	}
}
