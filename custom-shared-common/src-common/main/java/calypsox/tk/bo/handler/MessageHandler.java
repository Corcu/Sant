package calypsox.tk.bo.handler;

import com.calypso.tk.bo.BOMessage;


public interface MessageHandler{
	public BOMessage parseMessage(String mesageStr) throws Exception;
}
