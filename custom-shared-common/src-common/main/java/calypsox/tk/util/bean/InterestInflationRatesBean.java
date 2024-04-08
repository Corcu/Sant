package calypsox.tk.util.bean;

public class InterestInflationRatesBean {

	// variables
	private String indexKey;
	private String price;
	private String date;
	private int nElem;

	// constructor
	public InterestInflationRatesBean(String[] values) {

		setIndexKey(values[0]);
		setPrice(values[1]);
		setNElem(2);

		if ((values.length == 3) && !values[2].trim().equals("")) {// chapucilla
			setDate(values[2]);
			setNElem(3);
		}

	}

	// getters and setters
	public String getIndexKey() {
		return this.indexKey;
	}

	public void setIndexKey(String indexKey) {
		this.indexKey = indexKey;
	}

	public String getPrice() {
		return this.price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getDate() {
		return this.date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getNElem() {
		return this.nElem;
	}

	public void setNElem(int nElem) {
		this.nElem = nElem;
	}

}
