package com.calypso.tk.collateral.command.impl;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.trade.CustomUnderlyingTradeLoaderFactory;

public class CustomUpdateCommand extends UpdateCommand {
    public CustomUpdateCommand(ExecutionContext context, MarginCallEntry entry) {
        super(context, entry);
        this.tradeFactory=new CustomUnderlyingTradeLoaderFactory();    }
}
