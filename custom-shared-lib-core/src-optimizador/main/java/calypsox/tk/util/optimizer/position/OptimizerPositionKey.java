package calypsox.tk.util.optimizer.position;

public class OptimizerPositionKey {
	
	public OptimizerPositionKey(int securityId, int bookId) {
		super();
		this.securityId = securityId;
		this.bookId = bookId;
	}

	public int getSecurityId() {
		return securityId;
	}

	@Override
	public String toString() {
		return "OptimizerPositionKey [securityId=" + securityId + ", bookId="
				+ bookId + "]";
	}

	public void setSecurityId(int securityId) {
		this.securityId = securityId;
	}

	public int getBookId() {
		return bookId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.bookId;
		result = (prime * result) + this.securityId;

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		OptimizerPositionKey other = (OptimizerPositionKey) obj;
		if (this.securityId != other.securityId) {
			return false;
		}
		if (this.bookId != other.bookId) {
			return false;
		}
		return true;
	}

	public void setBookId(int bookId) {
		this.bookId = bookId;
	}

	protected int securityId = 0;
	protected int bookId = 0;
}
