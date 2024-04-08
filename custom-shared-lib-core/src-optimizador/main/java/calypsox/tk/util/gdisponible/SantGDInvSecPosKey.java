/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.gdisponible;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.service.DSConnection;

public class SantGDInvSecPosKey {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.bookId;
		result = (prime * result) + this.accountId;
		result = (prime * result) + this.agentId;
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
		SantGDInvSecPosKey other = (SantGDInvSecPosKey) obj;
		if (this.securityId != other.securityId) {
			return false;
		}
		if (this.bookId != other.bookId) {
			return false;
		}
		if (this.accountId != other.accountId) {
			return false;
		}
		if (this.agentId != other.agentId) {
			return false;
		}
		if ((this.positionType != null) && !this.positionType.equals(other.positionType)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SantInvSecPosKey [positionType=" + this.positionType + ", securityId="
				+ this.securityId + ", bookId=" + BOCache.getBook(DSConnection.getDefault(), this.bookId) + ", agentId=" + BOCache.getLegalEntityCode(DSConnection.getDefault(), this.agentId) + ", accountId="
				+ this.accountId + "]";
	}

	private int securityId = 0;
	private int bookId = 0;
	private int agentId = 0;
	private int accountId = 0;
	private String positionType = null;

	public SantGDInvSecPosKey(int securityId, String positionType, int bookId, int agentId, int accountId) {
		this.securityId = securityId;
		this.bookId = bookId;
		this.agentId = agentId;
		this.accountId = accountId;

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

	public int getAgentId() {
		return this.agentId;
	}

	public void setAgentId(int agentId) {
		this.agentId = agentId;
	}

	public int getAccountId() {
		return this.accountId;
	}

	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}

	public String getPositionType() {
		return this.positionType;
	}

	public void setPositionType(String positionType) {
		this.positionType = positionType;
	}
}
