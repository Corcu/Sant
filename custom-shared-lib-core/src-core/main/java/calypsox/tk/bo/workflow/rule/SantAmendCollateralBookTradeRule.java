/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.KondorPlusMarginCallMessageFormatter;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Vector;

public class SantAmendCollateralBookTradeRule implements WfTradeRule {

  @Override
  public String getDescription() {
    return "This rule amends the book of an allocation before creating MarginCall Trade using Book mapping Logic.";
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean check(
      TaskWorkflowConfig wc,
      Trade trade,
      Trade oldTrade,
      Vector messages,
      DSConnection dsCon,
      Vector excps,
      Task task,
      Object dbCon,
      Vector events) {
    return true;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public boolean update(
      TaskWorkflowConfig wc,
      Trade trade,
      Trade oldTrade,
      Vector messages,
      DSConnection dsCon,
      Vector excps,
      Task task,
      Object dbCon,
      Vector events) {
    Log.info("SantAmendCollateralBookTradeRule", "SantAmendCollateralBookTradeRule - Start");

    if ((trade == null)
        || (trade.getProduct() == null)
        || !(trade.getProduct() instanceof MarginCall)) {
      return false;
    }

    MarginCall marginCallProduct = (MarginCall) trade.getProduct();

    CollateralConfig marginCallConfig =
        CacheCollateralClient.getCollateralConfig(
            DSConnection.getDefault(), marginCallProduct.getMarginCallId());

    Book currentBook = trade.getBook();
    if (currentBook == null) {
      Log.info(
          "SantAmendCollateralBookTradeRule",
          "book is null. So trying to load bookid=" + trade.getBookId());
      currentBook = BOCache.getBook(dsCon, trade.getBookId());
      if (currentBook == null) {
        Log.info("SantAmendCollateralBookTradeRule", "Can't load book id=" + trade.getBookId());
        messages.add("Can't load the book");
        return false;
      }
    }
    if (currentBook != null) {
      String newBookName = null;
      String aliasToLookFor = null;

      if (marginCallProduct.getSecurity() == null) { // Cash
        aliasToLookFor = KondorPlusMarginCallMessageFormatter.ALIAS_BOOK_KONDOR;
      } else {
        Product p = marginCallProduct.getSecurity();
        if (p != null) {
          // Bond
          if (p instanceof Bond) {
            if (marginCallConfig.isRehypothecable()) {
              aliasToLookFor = KondorPlusMarginCallMessageFormatter.ALIAS_BOOK_K_REHYP;
            } else {
              aliasToLookFor = KondorPlusMarginCallMessageFormatter.ALIAS_BOOK_K_NO_REHYP;
            }
            // Equity
          } else if (p instanceof Equity) {
            aliasToLookFor = KondorPlusMarginCallMessageFormatter.ALIAS_BOOK_EQUITY;
          }
        }
      }

      // We look for the book in the alias included for K+.
      newBookName = currentBook.getAttribute(aliasToLookFor);

      if (Util.isEmpty(newBookName)) {
        String msg =
            "Cannot set K+ book: Book attribute ["
                + aliasToLookFor
                + "] is missing on book  "
                + currentBook.getName();
        addTask(trade, msg, excps);
      } else {
        Book newBook = BOCache.getBook(dsCon, newBookName);
        if (newBook != null) {
          // ?Cannot set K+ book: [attribute value] not found. Please create the book in Calypso.?
          Log.info(
              "SantAmendCollateralBookTradeRule",
              "OldBook=" + currentBook.getName() + "; New Book=" + newBookName);
          trade.setBook(newBook);
        } else {
          String msg =
              "Cannot set K+ book: ["
                  + newBookName
                  + "] not found. Please create the book in Calypso. ";
          addTask(trade, msg, excps);
        }
      }
    }

    Log.info("SantAmendCollateralBookTradeRule", "SantAmendCollateralBookTradeRule - End");

    return true;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void addTask(Trade trade, String msg, Vector excps) {
    BOException boExcp =
        new BOException(trade.getLongId(), this.getClass().getName(), msg, BOException.INFORMATION);
    excps.addElement(boExcp);
  }
}
