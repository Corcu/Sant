package calypsox.tk.anacredit.api.attributes;

public abstract class Attribute {

	int _size;
	int _decimals;
	boolean _signed;
	DataType _dataType = null;
	int _length = -1;
	
	protected int getLegth() {
		if (_length < 0) {
			_length = _size + _decimals + (_signed ? 1 : 0);
		}
		return _length;
	}
	
	public Attribute(DataType dt, int size) {
		_dataType = dt;
		_size = size;
	}

	protected void setSize(int _size) {
		this._size = _size;
	}

	protected void setDecimals(int _decimals) {
		this._decimals = _decimals;
	}

	public int getDecimals() {
		return _decimals;
	}

	protected void setSigned(boolean _signed) {
		this._signed = _signed;
	}

	protected boolean isSigned() {
		return _signed;
	}

	protected void setDataType(DataType _dataType) {
		this._dataType = _dataType;
	}
	
	public DataType getDataType() {
		return _dataType;
	}
	
	public int getSize() {
		return _size;
	}
	
	public abstract String formatValue(Object o) throws Exception;
		
	public boolean check(String parsed) {
		return parsed.length() == getLegth();
	}
	
	
	public String toString() {
		return "[" + _dataType + " size=" + getSize() + " decimals=" + getDecimals() + " signed=" + _signed + " length=" + getLegth() + "]";
	}
}
