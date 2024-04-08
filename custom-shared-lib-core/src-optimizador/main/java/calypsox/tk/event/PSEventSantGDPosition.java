/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.event;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;

public class PSEventSantGDPosition extends PSEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4415584413815640942L;

	protected String positionMessage = null;

	public String getPositionMessage() {
		return this.positionMessage;
	}

	public void setPositionMessage(String positionMessage) {
		this.positionMessage = positionMessage;
	}

	public PSEventSantGDPosition() {
		super();
	}

	public PSEventSantGDPosition(String positionMessage) {
		super();
		this.positionMessage = positionMessage;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.positionMessage = in.readUTF();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeUTF(!Util.isEmpty(this.positionMessage) ? this.positionMessage : "");
	}

	@Override
	public String getEventType() {
		return "SantGDPosition";
	}

	@Override
	public String toString() {
		return getEventType();
	}
}
