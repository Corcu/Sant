/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.gdisponible;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.service.DSConnection;

public class SantGDBookInvSecPosKey {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.bookId;
		result = (prime * result) + this.securityId;
		result = (prime * result) + this.positionType.hashCode();

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
		SantGDBookInvSecPosKey other = (SantGDBookInvSecPosKey) obj;
		if (this.securityId != other.securityId) {
			return false;
		}
		if (this.bookId != other.bookId) {
			return false;
		}
		if ((this.positionType != null)
				&& !this.positionType.equals(other.positionType)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SantInvSecPosKey [positionType=" + this.positionType
				+ ", securityId=" + this.securityId + ", bookId="
				+ BOCache.getBook(DSConnection.getDefault(), this.bookId) + "]";
	}

	private int securityId = 0;
	private int bookId = 0;
	private String positionType = null;

	public SantGDBookInvSecPosKey(int securityId, String positionType,
			int bookId) {
		this.securityId = securityId;
		this.bookId = bookId;
		this.positionType = positionType;
	}

	public int getBookId() {
		return this.bookId;
	}

	public void setBookId(int bookId) {
		this.bookId = bookId;
	}

	public int getSecurityId() {
		return this.securityId;
	}

	public void setSecurityId(int securityId) {
		this.securityId = securityId;
	}

	public String getPositionType() {
		return this.positionType;
	}

	public void setPositionType(String positionType) {
		this.positionType = positionType;
	}
}
