package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOMessageHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.service.DSConnection;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class BOTripartyMessageHandler extends com.calypso.tk.bo.BOTripartyMessageHandler {
  BOMessageHandler handler = new BOMessageHandler();
  @Override
  public boolean isMessageRequired(BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, PSEvent event, Vector exceptions, DSConnection ds) {
    if (isBond(transfer,trade) || isEquity(transfer,trade) || isSecLending(transfer,trade)|| isNettingType(transfer) || isSecLendingPairOff(transfer,trade)
            || isTransferAgent(transfer,trade) || isProductAccepted(transfer,trade)){
      return handler.isMessageRequired(message, oldMessage, trade, transfer, event, exceptions, ds);
    }
    return super.isMessageRequired(message, oldMessage, trade, transfer, event, exceptions, ds);
  }

  @Override
  public boolean isMessageAccepted(BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, PSEvent event, List<Task> exceptions, DSConnection ds) {
	  if (isBond(transfer,trade) || isEquity(transfer,trade) || isSecLending(transfer,trade) || isRepo(transfer,trade)
            || isNettingType(transfer) || isSecLendingPairOff(transfer,trade) || isTransferAgent(transfer,trade) || isProductAccepted(transfer,trade)){
      return handler.isMessageAccepted(message, oldMessage, trade, transfer, event, exceptions, ds);
    }
    return super.isMessageAccepted(message, oldMessage, trade, transfer, event, exceptions, ds);
  }

  private boolean isProductAccepted(BOTransfer boTransfer, Trade trade){
    String productType = getProductType( boTransfer, trade);
    List<String> boMessageHandlerProductsAccepted = DomainValues.values("BOMessageHandlerProductsAccepted");
    return boMessageHandlerProductsAccepted.stream().filter(value -> !value.isEmpty()).anyMatch(value -> value.equalsIgnoreCase(productType));
  }

  private String getProductType(BOTransfer boTransfer, Trade trade){
    String productType = Optional.ofNullable(boTransfer.getProductType()).orElse("");
    if(Util.isEmpty(productType)){
      productType = Optional.ofNullable(trade).map(Trade::getProduct).map(Product::getType).orElse("");
    }
    return productType;
  }



  private boolean isSecLending(BOTransfer xfer,Trade trade){
    boolean res;
    if(xfer!=null){
      res= SecLending.class.getSimpleName().equals(xfer.getProductType());
    }else{
      res= Optional.ofNullable(trade).map(Trade::getProduct).map(prod->prod instanceof SecLending)
              .orElse(false);
    }
    return res;
  }

  private boolean isRepo(BOTransfer xfer,Trade trade){
    boolean res;
    if(xfer!=null){
      res= Repo.class.getSimpleName().equals(xfer.getProductType());
    }else{
      res= Optional.ofNullable(trade).map(Trade::getProduct).map(prod->prod instanceof Repo)
              .orElse(false);
    }
    return res;
  }

  private boolean isEquity(BOTransfer xfer,Trade trade){
	    boolean res;
	    if(xfer!=null){
	      res= Equity.class.getSimpleName().equals(xfer.getProductType());
	    }else{
	      res= Optional.ofNullable(trade).map(Trade::getProduct).map(prod->prod instanceof Equity)
	              .orElse(false);
	    }
	    return res;
	  }

    private boolean isBond(BOTransfer xfer,Trade trade){
        boolean res;
        if(xfer!=null){
            res= !Util.isEmpty(xfer.getProductType()) && xfer.getProductType().startsWith(Bond.class.getSimpleName());
        }else{
            res= Optional.ofNullable(trade).map(Trade::getProduct).map(prod->prod instanceof Bond)
                    .orElse(false);
        }
        return res;
    }

    private boolean isTransferAgent(BOTransfer xfer, Trade trade){
      boolean res;
      if(xfer!=null){
        res= TransferAgent.class.getSimpleName().equals(xfer.getProductType());
      }else{
        res= Optional.ofNullable(trade).map(Trade::getProduct).map(prod->prod instanceof TransferAgent)
                .orElse(false);
      }
      return res;
    }

  private boolean isSecLendingPairOff(BOTransfer transfer, Trade trade){
    return null!=transfer && "SecLending".equalsIgnoreCase(transfer.getProductType()) && transfer.getNettingType().contains("PairOff");
  }




  private boolean isNettingType(BOTransfer transfer){
    return null != transfer && ("CounterpartyOTC".equalsIgnoreCase(transfer.getNettingType())
        || "CounterpartyRV".equalsIgnoreCase(transfer.getNettingType())
        || "RV_Broker".equalsIgnoreCase(transfer.getNettingType())
        || transfer.getNettingType().startsWith("RV_PairOff")
        || "PairOff".equalsIgnoreCase(transfer.getNettingType())
            || "Counterparty".equalsIgnoreCase(transfer.getNettingType())
            || transfer.getNettingType().contains("PairOff")
            || transfer.getNettingType().contains("CounterpartyRepo")
            || transfer.getNettingType().equalsIgnoreCase("CCP_Counterparty"));
  }
}
