/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoDialogInterface;
import com.calypso.apps.util.CalypsoLayout;
import com.calypso.apps.util.DomainWindowListener;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.apps.util.TableUtil;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.SystemKeyword;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

// CAL_621_
public class SantKeywordsFrame extends JDialog
implements DomainWindowListener, CalypsoDialogInterface {

  private static final String APPLY_BUTTON = "Apply";
  private static final String CANCEL_BUTTON = "Cancel";

  /**
   * serial version id
   */
  private static final long serialVersionUID = 1L;

  private final JButton applyButton;
  private final JButton cancelButton;
  private final Frame frame;

  @SuppressWarnings("rawtypes")
  private Vector _readOnly;
  private final JScrollPane JScrollPane1;
  private final JTable keywordJTable;
  private KeywordTableModel keywordTableModel;

  private final String action;
  private Trade trade;

  private String buttonSelected;

  public SantKeywordsFrame(final Frame frame, final String action,
      final Trade currentTrade, final List<String> kwToShow,
      final Vector<String> readOnlyKw) {
    super(frame);

    buttonSelected = null;

    this.frame = frame;
    this.action = action;

    applyButton = new JButton();

    cancelButton = new JButton();

    JScrollPane1 = new JScrollPane();
    keywordJTable = new JTable();

    try {
      init(kwToShow);
    } catch (final Exception e) {
      Log.error(this, e);
    }

    initDomains(currentTrade, kwToShow, readOnlyKw);
  }

  private void init(final List<String> kwToShow) throws Exception {
    setModal(true);
    getContentPane().setLayout(null);

    setVisible(false);

    applyButton.setText(APPLY_BUTTON);
    applyButton.setActionCommand("OK");
    applyButton.setAlignmentY(0.5F);
    getContentPane().add(applyButton);

    cancelButton.setText("Cancel");
    cancelButton.setActionCommand("Cancel");
    cancelButton.setAlignmentY(0.5F);
    getContentPane().add(cancelButton);

    getContentPane().add(JScrollPane1);

    if (kwToShow.size() < 6) {
      setSize(470, 240);
      applyButton.setBounds(15, 165, 85, 25);
      cancelButton.setBounds(352, 166, 85, 25);
      JScrollPane1.setBounds(10, 23, 428, 112);
    } else {
      setSize(470, 420);
      applyButton.setBounds(15, 345, 85, 25);
      cancelButton.setBounds(352, 346, 85, 25);
      JScrollPane1.setBounds(10, 23, 428, 302);
    }
    JScrollPane1.getViewport().add(keywordJTable);
    keywordJTable.setBounds(0, 0, 525, 0);

    final MenuAction menuAction = new MenuAction();
    applyButton.addActionListener(menuAction);
    cancelButton.addActionListener(menuAction);
  }

  @SuppressWarnings({ "unchecked" })
  protected void initDomains(final Trade currentTrade,
      final List<String> keywordsToShow,
      final Vector<String> readOnlyKw) {
    try {
      _readOnly = SystemKeyword.getDomain();
      _readOnly.addAll(readOnlyKw);

      keywordJTable.setCellSelectionEnabled(true);
      keywordTableModel = new KeywordTableModel();
      keywordTableModel.setColumnHeaders();
      keywordTableModel.setTo(keywordJTable);
      keywordJTable.getSelectionModel().setSelectionMode(2);
      keywordJTable.setAutoResizeMode(0);
    } catch (final Exception exception) {
      Log.error(this, exception);
    }
    final CalypsoLayout calypsolayout = new CalypsoLayout();
    calypsolayout.init(getContentPane(), 0);
    calypsolayout.setAttach(JScrollPane1, CalypsoLayout.RESIZE);
    calypsolayout.setAttach(applyButton, 202);
    calypsolayout.setAttach(cancelButton, 204);

    final StringBuffer title = new StringBuffer("Keyword Window - ");
    title.append(action).append(" - ").append(currentTrade);
    setTitle(title.toString());

    displayKeywords(currentTrade, keywordsToShow);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void displayKeywords(final Trade currentTrade,
      final List<String> keywordsToShow) {

    if (currentTrade != null) {
      trade = currentTrade;

      final LinkedHashMap hashmap = new LinkedHashMap();

      for (final String kwName : keywordsToShow) {
        final String kwValue = trade.getKeywordValue(kwName);
        final Keyword keyword = new Keyword(kwName, kwValue);

        hashmap.put(kwName, keyword);
      }

      int l1 = 0;
      for (final String kwName : keywordsToShow) {
        final Keyword keyword = (Keyword) hashmap.get(kwName);

        keywordTableModel.insertRowAt(l1);
        keywordTableModel.setValueAt(kwName, l1, 0);
        final Object obj1 = SystemKeyword.getDisplayValue(kwName,
            keyword._value);
        if (obj1 != null) {
          keywordTableModel.setValueAt(obj1, l1, 1);
        } else {
          keywordTableModel.setValueAt(keyword._value, l1, 1);
        }
        l1++;
      }

      // possible values for every kw specified in the DV
      for (int i = 0; i < keywordTableModel.getRowCount(); i++) {
        final String kwName = (String) keywordTableModel
            .getValueAt(i, 0);
        final Keyword keyword = (Keyword) hashmap.get(kwName);
        Vector keywordValues = Util
            .getKeywordValueDomain(keyword._name);
        if (keywordValues == null) {
          keywordValues = Util.getKeywordValueDomain(kwName);
        }

        if (!Util.isEmpty(keywordValues)) {
          for (int j = 0; j < keywordValues.size(); j++) {
            final String value = (String) keywordValues.get(j);

            if (Util.isEmpty(value)) {
              keywordValues.remove(j);
            }
          }
        }

        if (!Util.isEmpty(keywordValues)) {
          keywordTableModel.setCellChoices(i, 1, keywordValues,
              false);
        } else {
          keywordTableModel.setCellChoices(i, 1, null, true);
        }
      }

      keywordTableModel.notifyOnNewValue(true);
      keywordTableModel.refresh();

      TableUtil.adjust(keywordJTable);

    }

  }

  @Override
  public void setVisible(final boolean flag) {
    // center the keywords window
    if (flag) {
      setLocationRelativeTo(null);
    }
    super.setVisible(flag);
  }

  @Override
  public void domainSaved(final String s) {
    // do nothing
  }

  public String getButtonSelected() {
    return buttonSelected;
  }

  class KeywordTableModel extends TableModelUtil {

    /**
     * serial version id
     */
    private static final long serialVersionUID = 1L;

    public void setColumnHeaders() {
      setColumnName(0, "Name");
      setColumnName(1, "Value");
      setColumnEditable(0, false);
      setColumnEditable(1, true);
    }

    @Override
    public boolean isCellEditable(final int i, final int j) {
      if (j == 0) {
        return false;
      }
      final String s = (String) getValueAt(i, 0);
      if (_readOnly.contains(s)) {
        return false;
      } else {
        return true;
      }
    }

    @Override
    public void newValueAt(final int i, final int j, final Object obj) {
      // do nothing
    }

    public KeywordTableModel() {
      super(2, 0);

      setColumnHeaders();
    }
  }

  class Keyword {

    public String _name;
    public String _value;
    public final SantKeywordsFrame santKeywordFrame;

    public Keyword(final String s, final String s1) {
      super();

      santKeywordFrame = SantKeywordsFrame.this;

      _name = s;
      _value = s1;
    }
  }

  class MenuAction implements ActionListener {

    /**
     * Call specific method given the action applied
     */
    @Override
    public void actionPerformed(final ActionEvent actionevent) {
      final Object obj = actionevent.getSource();
      if (obj == applyButton) {
        applyButton_ActionPerformed();
      }
      if (obj == cancelButton) {
        cancelButton_ActionPerformed();
      }
    }

    public final SantKeywordsFrame this$0;

    MenuAction() {
      super();
      this$0 = SantKeywordsFrame.this;
    }
  }

  /**
   * method to retrieve new trade keywords with new values.
   *
   * @return trade keywords
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Hashtable<String, String> getKeywordsFromFrame() {
    final Hashtable<String, String> newKeywords = new Hashtable();

    final int i = keywordTableModel.getRowCount();
    for (int j = 0; j < i; j++) {
      final String kwName = (String) keywordTableModel.getValueAt(j,
          0);
      String kwValue = (String) keywordTableModel.getValueAt(j, 1);

      if (!Util.isEmpty(kwName)) {
        // do not remove! will generate nullpointer
        if (Util.isEmpty(kwValue)) {
          kwValue = "";
        }
        newKeywords.put(kwName, kwValue);
      }
    }

    if (newKeywords.size() > 0) {
      final Hashtable<String, String> tradeKeywords = trade
          .getKeywords();
      if (tradeKeywords != null) {
        for (int k = 0; k < i; k++) {
          final String kwName = (String) keywordTableModel
              .getValueAt(k, 0);
          final String kwValue = newKeywords.get(kwName);

          final String tradeValue = tradeKeywords.get(kwName);

          // if kw doesn't exists on the trade and new value is null,
          // remove from newKeywords
          if (Util.isEmpty(tradeValue)) {
            if (Util.isEmpty(kwValue)) {
              newKeywords.remove(kwName);
            }
          }
        }
      }
    }

    setVisible(false);

    return newKeywords;
  }

  /**
   * cancel button
   */
  void cancelButton_ActionPerformed() {
    buttonSelected = CANCEL_BUTTON;

    setVisible(false);
    dispose();
  }

  /**
   * apply button
   */
  void applyButton_ActionPerformed() {
    final boolean actionApplicable = TradeWorkflow.isTradeActionApplicable(
        trade, Action.valueOf(action),
        DSConnection.getDefault(), null);

    if (actionApplicable) {
      applyActionOnTrade();

      buttonSelected = APPLY_BUTTON;
    } else {
      final StringBuffer errorMessage = new StringBuffer("Action ");
      errorMessage.append(action).append(" is not valid on status ")
      .append(trade.getStatus().toString());

      // AppUtil.displayError(errorMessage.toString(),
      // "Action could not be applied", this.frame);
      AppUtil.displayError(frame, "Action could not be applied", errorMessage.toString(), null);
    }
  }

  /**
   * Apply action on trade
   */
  private void applyActionOnTrade() {
    boolean result = true;
    String errorMessage = "";

    // getting keywords from frame with new values
    final Hashtable<String, String> keywords = getKeywordsFromFrame();

    if (!Util.isEmpty(keywords)) {
        Trade newTrade = new Trade();
		newTrade = trade.clone();
     
		final Hashtable<String, String> tradeAtts = trade.getKeywords();
		//final Iterator<Entry<String, String>> it = tradeAtts.entrySet().iterator();
		final Iterator<Entry<String, String>> it = keywords.entrySet().iterator();

		// delete current keyword values
		while (it.hasNext()) {
		  final Entry<String, String> entry = it.next();

		  newTrade.removeKeyword(entry.getKey());
		}

		tradeAtts.putAll(keywords);
		newTrade.setKeywords(tradeAtts);
        
		newTrade.setAction(Action.valueOf(action));
		try {
			DSConnection.getDefault().getRemoteTrade().save(newTrade);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Could not save keywords for trade " + newTrade.getLongId());
		}
    }

    if (result) {
      final StringBuffer message = new StringBuffer("Action ");
      message.append(action).append(" applied on trade ");
      message.append(trade.getLongId());

      AppUtil.displayMessage(message.toString(), frame);
    } else {
      // AppUtil.displayError(errorMessage, "Action could not be applied",
      // frame);

      AppUtil.displayError(frame, "Action could not be applied", errorMessage.toString(), null);
    }

  }
  
}
