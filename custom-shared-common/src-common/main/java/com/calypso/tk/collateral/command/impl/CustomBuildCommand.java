package com.calypso.tk.collateral.command.impl;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.trade.CustomUnderlyingTradeLoaderFactory;

/**
 * @author aalonsop
 */
public class CustomBuildCommand extends BuildCommand {

    public CustomBuildCommand(ExecutionContext context, MarginCallEntry entry) {
        super(context, entry);
        this.tradeFactory=new CustomUnderlyingTradeLoaderFactory();
    }
}
