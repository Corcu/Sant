package calypsox.tk.util.optimizer.position;

public class OptimizerExportPosition {

	public OptimizerExportPosition(String key, String value) {
		super();
		this.keyPos = key;
		this.valuePos = value;
	}

	public String getKeyPos() {
		return this.keyPos;
	}

	public void setKeyPos(String keyPos) {
		this.keyPos = keyPos;
	}

	public void setValuePos(String valuePos) {
		this.valuePos = valuePos;
	}

	private String keyPos = null;
	private String valuePos = null;

	@Override
	public String toString() {
		return "OptimizerExportPosition [keyPos=" + this.keyPos + ", valuePos="
				+ this.valuePos + "]";
	}

	public String getValuePos() {
		return this.valuePos;
	}
}
