package calypsox.apps.reporting.util.control;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

@SuppressWarnings("rawtypes")
public class FilterComboBox extends JComboBox {

    /**
	 * 
	 */
    private static final long serialVersionUID = -1840918730921173695L;

    protected Object[] theWholeItems;

    protected boolean isCaseSensitive;

    /**
     * Default Filtering Combo Box, case sensitive and with emtpy item list
     */
    public FilterComboBox() {
	this(true);
	initGui();
    }

    /**
     * Filtering Combo Box with emtpy item list
     * 
     * @param isCaseSensitive
     *            true is the popup list is filtered according to the case,
     *            false otherwise
     */
    public FilterComboBox(final boolean isCaseSensitive) {
	super();
	this.isCaseSensitive = isCaseSensitive;

	initGui();
    }

    /**
     * Filtering Combo Box for the given list
     * 
     * @param theWholeItems
     *            the item list
     * @param isCaseSensitive
     *            isCaseSensitive true is the popup list is filtered according
     *            to the case, false otherwise
     */
    @SuppressWarnings("unchecked")
	public FilterComboBox(final Object[] theWholeItems,
	    final boolean isCaseSensitive) {
	super(theWholeItems);
	this.theWholeItems = theWholeItems;
	this.isCaseSensitive = isCaseSensitive;

	initGui();
    }

    /**
     * Filtering Combo Box for the given list. Case sensitive
     * 
     * @param theWholeItems
     *            the item list
     */
    public FilterComboBox(final Object[] theWholeItems) {
	this(theWholeItems, true);
    }

    public Object[] getTheWholeItems() {
	return this.theWholeItems;
    }

    @SuppressWarnings({ "unchecked" })
	public void setTheWholeItems(final Object[] theWholeItems) {
	this.theWholeItems = theWholeItems;

	final JTextField textField = (JTextField) getEditor()
		.getEditorComponent();
	final String selectedText = textField.getText();

	final DefaultComboBoxModel model = new DefaultComboBoxModel();

	for (int i = 0; i < theWholeItems.length; i++) {
	    model.addElement(theWholeItems[i]);
	}

	setModel(model);
	setSelectedItem(selectedText);

	filterCombo(selectedText, selectedText, false);
    }

    public boolean isCaseSensitive() {
	return this.isCaseSensitive;
    }

    public void setCaseSensitive(final boolean isCaseSensitive) {
	this.isCaseSensitive = isCaseSensitive;
    }

    protected void initGui() {
	setEditable(true);

	final JTextField textField = (JTextField) getEditor()
		.getEditorComponent();
	textField.addKeyListener(new KeyListener() {

	    @Override
	    public void keyPressed(final KeyEvent eventKey) {
	    }

	    @Override
	    public void keyReleased(final KeyEvent eventKey) {
	    }

	    @Override
	    public void keyTyped(final KeyEvent eventKey) {
		if ((eventKey.getSource() == textField)
			&& !isSpecialKey(eventKey)) {
		    final char keyPressed = eventKey.getKeyChar();
		    filterComboKeyPressed(keyPressed);
		}
	    }

	});

	setSelectedIndex(-1);
    }

    protected void filterComboKeyPressed(final char keyPressed) {
	final JTextField textField = (JTextField) getEditor()
		.getEditorComponent();
	final String initialText = textField.getText();
	String enteredText = textField.getText() == null ? "" : textField
		.getText();

	if ((keyPressed != KeyEvent.VK_DELETE)
		&& (keyPressed != KeyEvent.VK_BACK_SPACE)) {
	    enteredText = new StringBuilder(enteredText).insert(
		    textField.getCaretPosition(), keyPressed).toString();
	}

	filterCombo(enteredText, initialText, true);
    }

    protected void filterCombo(final String enteredText,
	    final String initialText, final boolean displayPopup) {
	final JTextField textField = (JTextField) getEditor()
		.getEditorComponent();

	final Thread filterThread = new Thread() {
	    @SuppressWarnings({ "unchecked" })
		@Override
	    public void run() {
		if (FilterComboBox.this.theWholeItems != null) {
		    final DefaultComboBoxModel model = new DefaultComboBoxModel();
		    if ((enteredText != null) && !enteredText.equals("")) {
			for (int i = 0; i < FilterComboBox.this.theWholeItems.length; i++) {
			    if (isFilterOK(enteredText,
				    FilterComboBox.this.theWholeItems[i])) {
				model.addElement(FilterComboBox.this.theWholeItems[i]);
			    }
			}
		    } else {
			for (int i = 0; i < FilterComboBox.this.theWholeItems.length; i++) {
			    model.addElement(FilterComboBox.this.theWholeItems[i]);
			}
		    }

		    SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			    final int currentPosition = textField
				    .getCaretPosition();
			    final String text = textField.getText();

			    setModel(model);
			    setSelectedIndex(-1);

			    if (displayPopup) {
				showPopup();
			    }

			    textField.setText(text);
			    textField.setCaretPosition(currentPosition);
			}
		    });
		}
	    }
	};

	filterThread.start();

    }

    protected boolean isFilterOK(final String enteredText, final Object itemList) {
	if (itemList != null) {
	    if (isCaseSensitive()
		    && itemList.toString().startsWith(enteredText)) {
		return true;
	    } else if (!isCaseSensitive()) {
		if (enteredText != null) {
		    return itemList.toString().toUpperCase()
			    .startsWith(enteredText.toUpperCase());
		}
	    }
	}

	return false;
    }

    private boolean isSpecialKey(final KeyEvent eventKey) {
	if (eventKey.isActionKey() || eventKey.isControlDown()
		|| eventKey.isAltDown() || eventKey.isMetaDown()
		|| (eventKey.getKeyChar() == KeyEvent.VK_ENTER)) {
	    return true;
	}

	return false;
    }

}
