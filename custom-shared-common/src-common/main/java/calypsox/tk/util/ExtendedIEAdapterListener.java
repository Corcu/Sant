package calypsox.tk.util;
/*
 * Calypso API - Generic Product training tutorial - February 2008.
 * 
 * Copyright ? 2008 Calypso Technology, Inc. All Rights Reserved
 */



import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterListener;

public interface ExtendedIEAdapterListener extends IEAdapterListener {
    public void setIEAdapter(IEAdapter adapter);

    public IEAdapter getIEAdapter();

    public boolean writeMessage(IEAdapter adapter, String message);
}
