package calypsox.apps.bo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;

import com.calypso.apps.bo.SAXTreeBuilder;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.Log;
import com.calypso.tk.upload.util.InfoSecUtil;
import com.calypso.tk.util.DataUploaderUtil;
import com.jidesoft.swing.JideButton;

/**
 * Calypso ML Viewer class / based on UploaderXMLTreeViewFrame
 * @author CedricAllain
 *
 */
public class CalypsoMLTreeViewFrame extends JFrame {
	
	public static final String LOG_CATEGORY = "CalypsoMLTreeViewFrame";
	
	private static final long serialVersionUID = 1L;
	JideButton closeButton = new JideButton("Close");
	JideButton exportButton = new JideButton("Export Message");
	JideButton expandTreeButton = new JideButton("Expand Tree");

	private JTree sourceTree = null;
	private JTree translatedTree = null;

	private UploaderXMLTreeActionListener uploadActionListener = null;
	long messageID = 0L;
	String output = "";
	String outputSource = "";

	public CalypsoMLTreeViewFrame(Object obj, AdviceDocument doc) {
		if (doc != null) {
			this.messageID = doc.getAdviceId();
		}
		initDomains(obj, doc);
	}

	/**
	 * init the Frame
	 * @param obj
	 * @param doc
	 */
	private void initDomains(Object obj, AdviceDocument doc) {
		setTitle("CalypsoML Advice Document Viewer");
		AppUtil.setCalypsoIcon(this);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent ev) {
				setVisible(false);
				dispose();
			}
		});
		String rootDesc = "";
		if (doc != null) {

			rootDesc = "Advice Document Id: " + doc.getId() + " BOMessage Id: " + doc.getAdviceId();
		} else {
			rootDesc = "Root";
		}
		getContentPane().setLayout(new BorderLayout());

		JComponent comp = buildCalypsoObjectViewer(obj, rootDesc, false);
		setSize(500, 550);

		if (comp != null)
			getContentPane().add("Center", comp);
		getContentPane().add("South", buildSouthPanel());

		setVisible(true);
	}

	private Component buildSouthPanel() {
		JPanel panel = new JPanel(new FlowLayout(2));

		panel.add(this.exportButton);
		panel.add(this.closeButton);

		this.closeButton.addActionListener(getUploadActionListener());
		this.exportButton.addActionListener(getUploadActionListener());
		this.expandTreeButton.addActionListener(getUploadActionListener());
		return panel;
	}



	private JComponent buildCalypsoObjectViewer(Object obj, String rootDesc, boolean isAcknowledgement) {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(rootDesc);
		SAXTreeBuilder saxTree = new SAXTreeBuilder(top);
		try {
			String xmlDocument = (String)obj;
			this.output = xmlDocument;
			InputSource is = new InputSource(new StringReader(xmlDocument));
			SAXParser saxParser = new SAXParser();
			saxParser.setContentHandler(saxTree);
			saxParser.parse(is);
		} catch (Exception ex) {
			top.add(new DefaultMutableTreeNode(ex.getMessage()));
		}

		JTree tree = new JTree(saxTree.getTree());
		tree.setEditable(true);
		expandAll(tree, true);
		return new JScrollPane(tree);
	}

	public void expandAll(JTree tree, boolean expand) {
		if (tree != null && tree.getSelectionPath() == null) {
			TreeNode root = (TreeNode) tree.getModel().getRoot();
			if (root != null) {
				expandAll(tree, new TreePath(root), expand);
			}
		} else if (tree != null) {
			expandAll(tree, tree.getSelectionPath(), true);
		}
	}

	private boolean expandAll(JTree tree, TreePath parent, boolean expand) {
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() > 0) {
			boolean childExpandCalled = false;
			for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				childExpandCalled = (expandAll(tree, path, expand) || childExpandCalled);
			}

			if (!childExpandCalled) {
				if (expand) {
					tree.expandPath(parent);
				} else {
					tree.collapsePath(parent);
				}
			}
			return true;
		}
		return false;
	}

	private class UploaderXMLTreeActionListener implements ActionListener {

		JFrame frame;

		public UploaderXMLTreeActionListener(JFrame frame) {
			this.frame = frame;
		}

		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == closeButton) {
				setVisible(false);
				dispose();
			} else if (event.getSource() == exportButton) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(0);
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileNameExtensionFilter("XML", new String[] { "xml" }));

				int returnVal = fc.showSaveDialog(frame);

				if (returnVal == 0) {
					File fileName = fc.getSelectedFile();
					String filePath = fileName.getPath();
					if (!filePath.toLowerCase().endsWith(".xml")) {
						fileName = InfoSecUtil.getFile(filePath + ".xml");
					}
					try {
						DataUploaderUtil.createFile(output, fileName.getPath());
					} catch (IOException e) {
						Log.error(LOG_CATEGORY, e);
					}

				}
			} else if (event.getSource() == expandTreeButton) {
				expandAll(sourceTree, false);
				expandAll(translatedTree, false);
			}
		}
	}

	protected ActionListener getUploadActionListener() {
		if (this.uploadActionListener == null) {
			this.uploadActionListener = new UploaderXMLTreeActionListener(null);
		}
		return this.uploadActionListener;
	}

}
