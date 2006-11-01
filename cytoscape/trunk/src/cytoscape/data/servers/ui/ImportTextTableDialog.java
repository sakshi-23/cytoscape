package cytoscape.data.servers.ui;

import giny.model.Edge;
import giny.model.Node;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.jdesktop.layout.GroupLayout;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.bookmarks.Attribute;
import cytoscape.bookmarks.Bookmarks;
import cytoscape.bookmarks.DataSource;
import cytoscape.data.CyAttributes;
import cytoscape.data.readers.AttributeMappingParameters;
import cytoscape.data.readers.DefaultAttributeTableReader;
import cytoscape.data.readers.ExcelAttributeSheetReader;
import cytoscape.data.readers.TextFileDelimiters;
import cytoscape.data.readers.TextTableReader;
import cytoscape.data.readers.TextTableReader.ObjectType;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.util.BookmarksUtil;
import cytoscape.util.CyFileFilter;
import cytoscape.util.FileUtil;
import cytoscape.util.swing.JStatusBar;

// Static imports for constants & resources.
import static cytoscape.data.readers.TextFileDelimiters.*;
import static cytoscape.data.servers.ui.enums.ImportDialogIconSets.*;
import static cytoscape.data.readers.TextTableReader.ObjectType.*;

/**
 * 
 * @author kono
 */
public class ImportTextTableDialog extends JDialog implements
		PropertyChangeListener, TableModelListener {

	/**
	 * This dialog GUI will be switched based on the following parameters:
	 * 
	 * SIMPLE_ATTRIBUTE_IMPORT: Import attributes in text table.
	 * 
	 * ONTOLOGY_AND_ANNOTATION_IMPORT: Load ontology and map attributes in text
	 * table.
	 * 
	 * NETWORK_IMPORT: Import text table as a network.
	 */
	public static final int SIMPLE_ATTRIBUTE_IMPORT = 1;
	public static final int ONTOLOGY_AND_ANNOTATION_IMPORT = 2;
	public static final int NETWORK_IMPORT = 3;

	/*
	 * Signals used among Swing components in this dialog:
	 */
	public static final String LIST_DELIMITER_CHANGED = "listDelimiterChanged";
	public static final String ATTR_DATA_TYPE_CHANGED = "attrDataTypeChanged";
	public static final String ATTRIBUTE_NAME_CHANGED = "aliasTableChanged";
	public static final String SHEET_CHANGED = "sheetChanged";

	/*
	 * HTML strings for tool tip text
	 */
	private String ontologyHtml = "<html><body bgcolor=\"white\"><p><strong><font size=\"+1\" face=\"serif\"><u>%DataSourceName%</u></font></strong></p><br>"
			+ "<p><em>Data Source URL</em>: <br><font color=\"blue\">%SourceURL%</font></p><br><p><em>Description</em>:<br>"
			+ "<table width=\"300\" border=\"0\" cellspacing=\"3\" cellpadding=\"3\"><tr>"
			+ "<td rowspan=\"1\" colspan=\"1\">%Description%</td></tr></table></p></body></html>";

	private String annotationHtml = "<html><body bgcolor=\"white\"><p><strong><font size=\"+1\" face=\"serif\"><u>%DataSourceName%</u></font></strong></p><br>"
			+ "<p><em>Annotation File URL</em>: <br><font color=\"blue\">%SourceURL%</font></p><br>"
			+ "<p><em>Data Format</em>: <font color=\"green\">%Format%</font></p><br>"
			+ "<p><em>Other Information</em>:<br>"
			+ "<table width=\"300\" border=\"0\" cellspacing=\"3\" cellpadding=\"3\">"
			+ "%AttributeTable%</table></p></body></html>";

	private static final String keyTable[] = { "Alias?",
			"Column (Attribute Name)", "Data Type" };

	private static final String ID = "ID";
	private static final String GENE_ASSOCIATION = "gene_association";

	// Default color

	// Key column index
	private int keyInFile;

	// Data Type
	private ObjectType objType;

	private final int dialogType;

	private Map<String, String> annotationUrlMap;
	private Map<String, String> annotationFormatMap;
	private Map<String, Map<String, String>> annotationAttributesMap;

	private Map<String, String> ontologyUrlMap;
	private Map<String, String> ontologyDescriptionMap;

	private Map<String, Boolean> ontologyStatusMap;

	private Map<String, String[]> attributeNames;
	private List<Byte> attributeDataTypes;

	/*
	 * Tracking multiple sheets.
	 */
	private Map<String, AliasTableModel> aliasTableModelMap;
	private Map<String, JTable> aliasTableMap;
	private Map<String, Integer> primaryKeyMap;

	private String[] columnHeaders;

	private String listDelimiter;

	private boolean[] importFlag;

	private CyAttributes selectedAttributes;

	private PropertyChangeSupport changes = new PropertyChangeSupport(this);

	/**
	 * Creates new form ImportAttributesDialog
	 */
	public ImportTextTableDialog(Frame parent, boolean modal) {
		this(parent, modal, ImportTextTableDialog.SIMPLE_ATTRIBUTE_IMPORT);
	}

	public ImportTextTableDialog(Frame parent, boolean modal, int dialogType) {
		super(parent, modal);

		// Default Attribute is node attr.
		selectedAttributes = Cytoscape.getNodeAttributes();

		this.objType = NODE;
		this.dialogType = dialogType;
		this.listDelimiter = PIPE.toString();

		this.aliasTableModelMap = new HashMap<String, AliasTableModel>();
		this.aliasTableMap = new HashMap<String, JTable>();
		this.primaryKeyMap = new HashMap<String, Integer>();

		annotationUrlMap = new HashMap<String, String>();
		annotationFormatMap = new HashMap<String, String>();
		annotationAttributesMap = new HashMap<String, Map<String, String>>();

		ontologyUrlMap = new HashMap<String, String>();
		ontologyDescriptionMap = new HashMap<String, String>();
		ontologyStatusMap = new HashMap<String, Boolean>();
		attributeNames = new HashMap<String, String[]>();
		attributeDataTypes = new ArrayList<Byte>();
		initComponents();
		updateComponents();

		previewPanel.addPropertyChangeListener(this);

	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		changes.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		changes.removePropertyChangeListener(l);
	}

	/**
	 * Listening to local signals used among Swing components in this dialog.
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(LIST_DELIMITER_CHANGED)) {
			listDelimiter = evt.getNewValue().toString();
		} else if (evt.getPropertyName().equals(ATTR_DATA_TYPE_CHANGED)) {

			final Vector vec = (Vector) evt.getNewValue();
			final Integer key = (Integer) vec.get(0);
			final Byte newType = (Byte) vec.get(1);
			attributeDataTypes.set(key, newType);
			int i = 0;
			for (Byte type : attributeDataTypes) {
				System.out.println("List Vals " + i + " = " + type.toString());
				i++;
			}

			JTable curTable = aliasTableMap.get(previewPanel
					.getSelectedSheetName());
			curTable.setDefaultRenderer(Object.class, new AliasTableRenderer(
					attributeDataTypes, primaryKeyComboBox.getSelectedIndex()));
			curTable.repaint();

		} else if (evt.getPropertyName().equals(ATTRIBUTE_NAME_CHANGED)) {
			final Vector vec = (Vector) evt.getNewValue();
			final String name = (String) vec.get(1);
			final Integer column = (Integer) vec.get(0);
			updateAliasTableCell(name, column);
		} else if (evt.getPropertyName().equals(SHEET_CHANGED)) {
			/*
			 * Only when the file is in Excel format.
			 */
			final int columnCount = previewPanel.getPreviewTable()
					.getColumnCount();
			aliasTableModelMap.put(previewPanel.getSelectedSheetName(),
					new AliasTableModel(keyTable, columnCount));

			initializeAliasTable(columnCount, null);
			updatePrimaryKeyComboBox();
		}

	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code">
	private void initComponents() {

		statusBar = new JStatusBar();
		networkImportPanel = new NetworkImportOptionsPanel();

		importTypeButtonGroup = new ButtonGroup();

		attrTypePanel = new JPanel();

		counterSpinner = new javax.swing.JSpinner();
		counterLabel = new javax.swing.JLabel();
		reloadButton = new javax.swing.JButton();
		showAllRadioButton = new javax.swing.JRadioButton();
		counterRadioButton = new javax.swing.JRadioButton();

		attrTypeButtonGroup = new javax.swing.ButtonGroup();
		titleIconLabel1 = new javax.swing.JLabel();
		titleIconLabel2 = new javax.swing.JLabel();
		titleIconLabel3 = new javax.swing.JLabel();
		titleLabel = new javax.swing.JLabel();
		titleSeparator = new javax.swing.JSeparator();
		importButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();
		helpButton = new javax.swing.JButton();
		basicPanel = new javax.swing.JPanel();
		attribuiteLabel = new javax.swing.JLabel();
		nodeRadioButton = new javax.swing.JRadioButton();
		edgeRadioButton = new javax.swing.JRadioButton();
		networkRadioButton = new javax.swing.JRadioButton();
		annotationAndOntologyImportPanel = new javax.swing.JPanel();
		ontologyLabel = new javax.swing.JLabel();
		ontologyComboBox = new javax.swing.JComboBox();
		browseOntologyButton = new javax.swing.JButton();
		sourceLabel = new javax.swing.JLabel();
		annotationComboBox = new javax.swing.JComboBox();
		browseAnnotationButton = new javax.swing.JButton();

		targetDataSourceTextField = new javax.swing.JTextField();
		selectAttributeFileButton = new javax.swing.JButton();
		advancedPanel = new javax.swing.JPanel();
		advancedOptionCheckBox = new javax.swing.JCheckBox();
		textImportCheckBox = new javax.swing.JCheckBox();
		attr2annotationPanel = new javax.swing.JPanel();
		primaryKeyLabel = new javax.swing.JLabel();
		nodeKeyLabel = new javax.swing.JLabel();
		mappingAttributeComboBox = new javax.swing.JComboBox();
		aliasScrollPane = new javax.swing.JScrollPane();
		// aliasTable = new JTable();
		arrowButton1 = new javax.swing.JButton();
		ontology2annotationPanel = new javax.swing.JPanel();
		targetOntologyLabel = new javax.swing.JLabel();
		targetOntologyTextField = new javax.swing.JTextField();
		ontologyInAnnotationLabel = new javax.swing.JLabel();
		ontologyInAnnotationComboBox = new javax.swing.JComboBox();
		arrowButton2 = new javax.swing.JButton();
		textImportOptionPanel = new javax.swing.JPanel();
		delimiterPanel = new javax.swing.JPanel();
		tabCheckBox = new javax.swing.JCheckBox();
		commaCheckBox = new javax.swing.JCheckBox();
		semicolonCheckBox = new javax.swing.JCheckBox();
		spaceCheckBox = new javax.swing.JCheckBox();
		otherCheckBox = new javax.swing.JCheckBox();
		otherDelimiterTextField = new javax.swing.JTextField();
		attrNameCheckBox = new javax.swing.JCheckBox();

		previewPanel = new PreviewTablePanel();
		simpleAttributeImportPanel = new javax.swing.JPanel();
		attributeFileLabel = new javax.swing.JLabel();

		primaryLabel = new JLabel("Primary Key: ");
		primaryKeyComboBox = new JComboBox();
		primaryKeyComboBox.setEnabled(false);
		primaryKeyComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				primaryKeyComboBoxActionPerformed(evt);
			}
		});

		/*
		 * Set tooltips options.
		 */
		ToolTipManager tp = ToolTipManager.sharedInstance();
		tp.setInitialDelay(50);
		tp.setDismissDelay(50000);

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		titleIconLabel2.setIcon(RIGHT_ARROW_ICON.getIcon());

		titleIconLabel3.setIcon(new ImageIcon(Cytoscape.class
				.getResource("images/icon48.png")));

		titleLabel.setFont(new java.awt.Font("Serif", 1, 18));
		titleLabel.setText("Import and Map Annotation Table");

		titleLabel.setFont(new java.awt.Font("Serif", 1, 18));
		titleLabel.setText("Import and Map Annotation Table");

		titleSeparator.setForeground(java.awt.Color.blue);

		importButton.setText("Import");
		importButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				try {
					importButtonActionPerformed(evt);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		helpButton.setBackground(new java.awt.Color(255, 255, 255));
		helpButton.setText("?");
		helpButton.setToolTipText("Display help page...");
		helpButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
		helpButton.setPreferredSize(new java.awt.Dimension(14, 14));
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				helpButtonActionPerformed(arg0);
			}
		});

		/*
		 * Data Source Panel Layouts.
		 */
		basicPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null,
				"Data Sources",
				javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
				javax.swing.border.TitledBorder.DEFAULT_POSITION,
				new java.awt.Font("Dialog", 1, 11)));

		if (dialogType == SIMPLE_ATTRIBUTE_IMPORT
				|| dialogType == ONTOLOGY_AND_ANNOTATION_IMPORT) {
			attribuiteLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
			attribuiteLabel.setText("Attributes");

			nodeRadioButton.setText("Node");
			nodeRadioButton.setBorder(javax.swing.BorderFactory
					.createEmptyBorder(0, 0, 0, 0));
			nodeRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
			nodeRadioButton.setSelected(true);
			nodeRadioButton
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							attributeRadioButtonActionPerformed(evt);
						}
					});

			edgeRadioButton.setText("Edge");
			edgeRadioButton.setBorder(javax.swing.BorderFactory
					.createEmptyBorder(0, 0, 0, 0));
			edgeRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
			edgeRadioButton
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							attributeRadioButtonActionPerformed(evt);
						}
					});

			networkRadioButton.setText("Network");
			networkRadioButton.setBorder(javax.swing.BorderFactory
					.createEmptyBorder(0, 0, 0, 0));
			networkRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
			networkRadioButton
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							attributeRadioButtonActionPerformed(evt);
						}
					});

			org.jdesktop.layout.GroupLayout attrTypePanelLayout = new org.jdesktop.layout.GroupLayout(
					attrTypePanel);
			attrTypePanel.setLayout(attrTypePanelLayout);
			attrTypePanelLayout
					.setHorizontalGroup(attrTypePanelLayout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									attrTypePanelLayout
											.createSequentialGroup()
											.add(attribuiteLabel)
											.add(24, 24, 24)
											.add(nodeRadioButton)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(edgeRadioButton)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(networkRadioButton)
											.addContainerGap(
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													Short.MAX_VALUE)));
			attrTypePanelLayout.setVerticalGroup(attrTypePanelLayout
					.createParallelGroup(
							org.jdesktop.layout.GroupLayout.LEADING).add(
							attrTypePanelLayout.createParallelGroup(
									org.jdesktop.layout.GroupLayout.BASELINE)
									.add(attribuiteLabel).add(nodeRadioButton)
									.add(edgeRadioButton).add(
											networkRadioButton)));

		}

		/*
		 * This panel is necessary only when this is an ontology import dialog.
		 */
		if (dialogType == ONTOLOGY_AND_ANNOTATION_IMPORT) {
			
			titleIconLabel1.setIcon(REMOTE_SOURCE_ICON.getIcon());
			
			ontologyLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
			ontologyLabel.setForeground(new java.awt.Color(73, 127, 235));
			ontologyLabel.setText("Ontology");

			ontologyComboBox.setFont(new java.awt.Font("SansSerif", 1, 14));
			ontologyComboBox.setForeground(new java.awt.Color(73, 127, 235));
			ontologyComboBox.setPreferredSize(new java.awt.Dimension(68, 25));
			final ListCellRenderer ontologyLcr = ontologyComboBox.getRenderer();
			ontologyComboBox.setFont(new java.awt.Font("SansSerif", 1, 12));
			// ontologyComboBox.setForeground(new java.awt.Color(73, 127, 235));
			ontologyComboBox.setRenderer(new ListCellRenderer() {
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					JLabel cmp = (JLabel) ontologyLcr
							.getListCellRendererComponent(list, value, index,
									isSelected, cellHasFocus);
					String url = ontologyUrlMap.get(value);

					if (url != null && url.startsWith("http://")) {
						cmp.setIcon(REMOTE_SOURCE_ICON.getIcon());
					} else {
						cmp.setIcon(LOCAL_SOURCE_ICON.getIcon());
					}

					cmp.setForeground(Color.red);
					return cmp;
				}
			});

			ontologyComboBox.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent arg0) {
				}

				public void mouseEntered(MouseEvent arg0) {
				}

				public void mouseExited(MouseEvent arg0) {
				}

				public void mousePressed(MouseEvent arg0) {
				}

				public void mouseReleased(MouseEvent arg0) {
				}
			});

			ontologyComboBox
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							ontologyComboBoxActionPerformed(evt);
						}
					});

			browseOntologyButton.setText("Browse");
			browseOntologyButton.setToolTipText("Browse local ontology file");
			browseOntologyButton
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							browseOntologyButtonActionPerformed(evt);
						}
					});

			sourceLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
			sourceLabel.setText("Annotation");

			annotationComboBox.setFont(new java.awt.Font("SansSerif", 1, 14));
			annotationComboBox.setPreferredSize(new java.awt.Dimension(68, 25));

			final ListCellRenderer lcr = annotationComboBox.getRenderer();
			annotationComboBox.setFont(new java.awt.Font("SansSerif", 1, 12));
			annotationComboBox.setRenderer(new ListCellRenderer() {
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					JLabel cmp = (JLabel) lcr.getListCellRendererComponent(
							list, value, index, isSelected, cellHasFocus);
					String url = annotationUrlMap.get(value);
					// System.out.println("############ box val = " + url + ", "
					// +
					// index);
					if (url != null && url.startsWith("http://")) {
						cmp.setIcon(REMOTE_SOURCE_ICON.getIcon());
					} else {
						cmp.setIcon(LOCAL_SOURCE_ICON.getIcon());
					}

					return cmp;
				}
			});
			annotationComboBox
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							annotationComboBoxActionPerformed(evt);
						}
					});

			browseAnnotationButton.setText("Browse");
			browseAnnotationButton
					.setToolTipText("Browse local annotation file...");
			browseAnnotationButton
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							browseAnnotationButtonActionPerformed(evt);
						}
					});

			GroupLayout annotationAndOntologyImportPanelLayout = new GroupLayout(
					annotationAndOntologyImportPanel);
			annotationAndOntologyImportPanel
					.setLayout(annotationAndOntologyImportPanelLayout);
			annotationAndOntologyImportPanelLayout
					.setHorizontalGroup(annotationAndOntologyImportPanelLayout
							.createParallelGroup(GroupLayout.LEADING)
							.add(
									annotationAndOntologyImportPanelLayout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													annotationAndOntologyImportPanelLayout
															.createParallelGroup(
																	GroupLayout.LEADING)
															.add(sourceLabel)
															.add(ontologyLabel))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													annotationAndOntologyImportPanelLayout
															.createParallelGroup(
																	GroupLayout.TRAILING)
															.add(
																	annotationComboBox,
																	0,
																	591,
																	Short.MAX_VALUE)
															.add(
																	ontologyComboBox,
																	0,
																	591,
																	Short.MAX_VALUE))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													annotationAndOntologyImportPanelLayout
															.createParallelGroup(
																	GroupLayout.LEADING)
															.add(
																	browseAnnotationButton)
															.add(
																	browseOntologyButton))
											.addContainerGap()));
			annotationAndOntologyImportPanelLayout
					.setVerticalGroup(annotationAndOntologyImportPanelLayout
							.createParallelGroup(GroupLayout.LEADING)
							.add(
									annotationAndOntologyImportPanelLayout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													annotationAndOntologyImportPanelLayout
															.createParallelGroup(
																	GroupLayout.CENTER)
															.add(sourceLabel)
															.add(
																	browseAnnotationButton)
															.add(
																	annotationComboBox,
																	GroupLayout.PREFERRED_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.PREFERRED_SIZE))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													annotationAndOntologyImportPanelLayout
															.createParallelGroup(
																	GroupLayout.CENTER)
															.add(ontologyLabel)
															.add(
																	ontologyComboBox,
																	GroupLayout.PREFERRED_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.PREFERRED_SIZE)
															.add(
																	browseOntologyButton))
											.addContainerGap(
													GroupLayout.DEFAULT_SIZE,
													Short.MAX_VALUE)));
		}

		if (dialogType == SIMPLE_ATTRIBUTE_IMPORT) {
			
			titleIconLabel1.setIcon(SPREADSHEET_ICON_LARGE.getIcon());
			
			attributeFileLabel.setText("Input File");
			attributeFileLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
			selectAttributeFileButton.setText("Select File");
			selectAttributeFileButton
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							try {
								selectAttributeFileButtonActionPerformed(evt);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});

			GroupLayout simpleAttributeImportPanelLayout = new GroupLayout(
					simpleAttributeImportPanel);
			simpleAttributeImportPanel
					.setLayout(simpleAttributeImportPanelLayout);
			simpleAttributeImportPanelLayout
					.setHorizontalGroup(simpleAttributeImportPanelLayout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									simpleAttributeImportPanelLayout
											.createSequentialGroup()
											.add(attributeFileLabel)
											.add(24, 24, 24)
											.add(
													targetDataSourceTextField,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													606, Short.MAX_VALUE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(selectAttributeFileButton)
											.addContainerGap()));
			simpleAttributeImportPanelLayout
					.setVerticalGroup(simpleAttributeImportPanelLayout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									simpleAttributeImportPanelLayout
											.createParallelGroup(
													org.jdesktop.layout.GroupLayout.BASELINE)
											.add(selectAttributeFileButton)
											.add(
													targetDataSourceTextField,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
											.add(attributeFileLabel)));
		}

		GroupLayout basicPanelLayout = new GroupLayout(basicPanel);
		basicPanel.setLayout(basicPanelLayout);

		basicPanelLayout
				.setHorizontalGroup(basicPanelLayout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								basicPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.add(
												basicPanelLayout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(
																basicPanelLayout
																		.createSequentialGroup()
																		.add(
																				simpleAttributeImportPanel,
																				org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																				org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																				Short.MAX_VALUE)
																		.addContainerGap())
														.add(
																basicPanelLayout
																		.createSequentialGroup()
																		.add(
																				annotationAndOntologyImportPanel,
																				org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																				org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																				Short.MAX_VALUE)
																		.addContainerGap())
														.add(
																basicPanelLayout
																		.createSequentialGroup()
																		.add(
																				attrTypePanel,
																				org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																				org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																				org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																		.addContainerGap(
																				632,
																				Short.MAX_VALUE)))));
		basicPanelLayout
				.setVerticalGroup(basicPanelLayout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								basicPanelLayout
										.createSequentialGroup()
										.add(
												attrTypePanel,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												simpleAttributeImportPanel,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.add(
												annotationAndOntologyImportPanel,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)));

		/*
		 * Layout data for advanced panel
		 */
		advancedPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
				null, "Advanced",
				javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
				javax.swing.border.TitledBorder.DEFAULT_POSITION,
				new java.awt.Font("Dialog", 1, 11)));

		if (dialogType == SIMPLE_ATTRIBUTE_IMPORT
				|| dialogType == ONTOLOGY_AND_ANNOTATION_IMPORT) {
			advancedOptionCheckBox.setText("Show Advanced Mapping Options");
			advancedOptionCheckBox.setBorder(javax.swing.BorderFactory
					.createEmptyBorder(0, 0, 0, 0));
			advancedOptionCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
			advancedOptionCheckBox
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							advancedOptionCheckBoxActionPerformed(evt);
						}
					});

			attr2annotationPanel
					.setBackground(new java.awt.Color(250, 250, 250));
			attr2annotationPanel
					.setBorder(javax.swing.BorderFactory
							.createTitledBorder(
									new javax.swing.border.SoftBevelBorder(
											javax.swing.border.BevelBorder.RAISED),
									"Annotation File to Attribute Mapping",
									javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
									javax.swing.border.TitledBorder.DEFAULT_POSITION,
									new java.awt.Font("Dialog", 1, 11)));
			primaryKeyLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
			primaryKeyLabel.setForeground(new java.awt.Color(51, 51, 255));
			primaryKeyLabel.setText("Key Column in Annotation File");

			nodeKeyLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
			nodeKeyLabel.setForeground(new java.awt.Color(255, 0, 51));
			nodeKeyLabel.setText("Key Attribute for Network");

			mappingAttributeComboBox.setForeground(new java.awt.Color(255, 0,
					51));
			mappingAttributeComboBox.setEnabled(false);
			mappingAttributeComboBox
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							nodeKeyComboBoxActionPerformed(evt);
						}
					});

			arrowButton1.setBackground(new java.awt.Color(250, 250, 250));
			arrowButton1.setOpaque(false);
			arrowButton1.setIcon(RIGHT_ARROW_ICON.getIcon());
			arrowButton1.setBorder(null);
			arrowButton1.setBorderPainted(false);

			GroupLayout attr2annotationPanelLayout = new GroupLayout(
					attr2annotationPanel);
			attr2annotationPanel.setLayout(attr2annotationPanelLayout);
			attr2annotationPanelLayout
					.setHorizontalGroup(attr2annotationPanelLayout
							.createParallelGroup(GroupLayout.LEADING)
							.add(
									attr2annotationPanelLayout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													attr2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.LEADING)
															.add(
																	attr2annotationPanelLayout
																			.createSequentialGroup()
																			.add(
																					primaryKeyLabel)
																			.add(
																					118,
																					118,
																					118))
															.add(
																	attr2annotationPanelLayout
																			.createSequentialGroup()
																			.add(
																					primaryLabel)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					primaryKeyComboBox,
																					0,
																					0,
																					Short.MAX_VALUE))
															.add(
																	aliasScrollPane,
																	GroupLayout.DEFAULT_SIZE,
																	366,
																	Short.MAX_VALUE))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(arrowButton1)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													attr2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.LEADING)
															.add(
																	mappingAttributeComboBox,
																	0,
																	367,
																	Short.MAX_VALUE)
															.add(nodeKeyLabel))
											.addContainerGap()));
			attr2annotationPanelLayout
					.setVerticalGroup(attr2annotationPanelLayout
							.createParallelGroup(GroupLayout.LEADING)
							.add(
									attr2annotationPanelLayout
											.createSequentialGroup()
											.add(
													attr2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.BASELINE)
															.add(
																	primaryKeyLabel)
															.add(nodeKeyLabel))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													attr2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.BASELINE)
															.add(primaryLabel)
															.add(
																	primaryKeyComboBox,
																	GroupLayout.PREFERRED_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.PREFERRED_SIZE))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													attr2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.LEADING)
															.add(
																	aliasScrollPane,
																	GroupLayout.DEFAULT_SIZE,
																	100,
																	Short.MAX_VALUE)
															.add(
																	attr2annotationPanelLayout
																			.createSequentialGroup()
																			.add(
																					attr2annotationPanelLayout
																							.createParallelGroup(
																									GroupLayout.TRAILING)
																							.add(
																									mappingAttributeComboBox,
																									GroupLayout.PREFERRED_SIZE,
																									GroupLayout.DEFAULT_SIZE,
																									GroupLayout.PREFERRED_SIZE)
																							.add(
																									arrowButton1))
																			.addContainerGap()))));

		}

		textImportCheckBox.setText("Show Text File Import Options");
		textImportCheckBox.setBorder(javax.swing.BorderFactory
				.createEmptyBorder(0, 0, 0, 0));
		textImportCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		textImportCheckBox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						textImportCheckBoxActionPerformed(evt);
					}
				});

		if (dialogType == ONTOLOGY_AND_ANNOTATION_IMPORT) {

			ontology2annotationPanel.setBackground(new java.awt.Color(250, 250,
					250));
			ontology2annotationPanel
					.setBorder(javax.swing.BorderFactory
							.createTitledBorder(
									new javax.swing.border.SoftBevelBorder(
											javax.swing.border.BevelBorder.RAISED),
									"Annotation File to Ontology Mapping",
									javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
									javax.swing.border.TitledBorder.DEFAULT_POSITION,
									new java.awt.Font("Dialog", 1, 11)));
			targetOntologyLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
			targetOntologyLabel.setForeground(new java.awt.Color(73, 127, 235));
			targetOntologyLabel.setText("Ontology");

			targetOntologyTextField.setFont(new java.awt.Font("SansSerif", 3,
					14));
			targetOntologyTextField.setForeground(new java.awt.Color(73, 127,
					235));
			targetOntologyTextField.setText("Gene Ontology Full");
			targetOntologyTextField
					.setToolTipText("Ontology name which will be used for mapping.");

			ontologyInAnnotationLabel.setFont(new java.awt.Font("SansSerif", 1,
					12));
			ontologyInAnnotationLabel.setForeground(new java.awt.Color(0, 255,
					255));
			ontologyInAnnotationLabel.setText("Key Column in Annotation File");

			ontologyInAnnotationComboBox.setForeground(new java.awt.Color(0,
					255, 255));
			ontologyInAnnotationComboBox
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(
								java.awt.event.ActionEvent evt) {
							ontologyInAnnotationComboBoxActionPerformed(evt);
						}
					});

			arrowButton2.setBackground(new java.awt.Color(250, 250, 250));
			arrowButton2.setIcon(new javax.swing.ImageIcon(Cytoscape.class
					.getResource("images/ximian/stock_right-16.png")));
			arrowButton2.setBorder(null);
			arrowButton2.setBorderPainted(false);

			GroupLayout ontology2annotationPanelLayout = new GroupLayout(
					ontology2annotationPanel);
			ontology2annotationPanel.setLayout(ontology2annotationPanelLayout);
			ontology2annotationPanelLayout
					.setHorizontalGroup(ontology2annotationPanelLayout
							.createParallelGroup(GroupLayout.LEADING)
							.add(
									ontology2annotationPanelLayout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													ontology2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.LEADING)
															.add(
																	ontologyInAnnotationLabel)
															.add(
																	ontologyInAnnotationComboBox,
																	0,
																	354,
																	Short.MAX_VALUE))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(arrowButton2)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													ontology2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.TRAILING)
															.add(
																	GroupLayout.LEADING,
																	targetOntologyLabel)
															.add(
																	targetOntologyTextField,
																	GroupLayout.DEFAULT_SIZE,
																	357,
																	Short.MAX_VALUE))
											.addContainerGap()));
			ontology2annotationPanelLayout
					.setVerticalGroup(ontology2annotationPanelLayout
							.createParallelGroup(GroupLayout.LEADING)
							.add(
									ontology2annotationPanelLayout
											.createSequentialGroup()
											.add(
													ontology2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.BASELINE)
															.add(
																	ontologyInAnnotationLabel)
															.add(
																	targetOntologyLabel))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													ontology2annotationPanelLayout
															.createParallelGroup(
																	GroupLayout.BASELINE)
															.add(
																	ontologyInAnnotationComboBox,
																	GroupLayout.PREFERRED_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.PREFERRED_SIZE)
															.add(
																	targetOntologyTextField,
																	GroupLayout.PREFERRED_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.PREFERRED_SIZE)
															.add(arrowButton2))
											.addContainerGap(
													GroupLayout.DEFAULT_SIZE,
													Short.MAX_VALUE)));

		}

		textImportOptionPanel.setBorder(javax.swing.BorderFactory
				.createTitledBorder(new javax.swing.border.SoftBevelBorder(
						javax.swing.border.BevelBorder.RAISED),
						"Text File Import Options",
						javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
						javax.swing.border.TitledBorder.DEFAULT_POSITION,
						new java.awt.Font("Dialog", 1, 11)));
		delimiterPanel.setBorder(javax.swing.BorderFactory
				.createTitledBorder("Delimiter"));
		tabCheckBox.setText("Tab");
		tabCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0,
				0, 0));
		tabCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		tabCheckBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				try {
					delimiterCheckBoxActionPerformed(evt);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		commaCheckBox.setText("Comma");
		commaCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,
				0, 0, 0));
		commaCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		commaCheckBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				try {
					delimiterCheckBoxActionPerformed(evt);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		semicolonCheckBox.setText("Semicolon");
		semicolonCheckBox.setBorder(javax.swing.BorderFactory
				.createEmptyBorder(0, 0, 0, 0));
		semicolonCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		semicolonCheckBox
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						try {
							delimiterCheckBoxActionPerformed(evt);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});

		spaceCheckBox.setText("Space");
		spaceCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,
				0, 0, 0));
		spaceCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		spaceCheckBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				try {
					delimiterCheckBoxActionPerformed(evt);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		otherCheckBox.setText("Other");
		otherCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,
				0, 0, 0));
		otherCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		otherCheckBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				try {
					delimiterCheckBoxActionPerformed(evt);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		otherDelimiterTextField.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent evt) {
				try {
					otherTextFieldActionPerformed(evt);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public void keyReleased(KeyEvent arg0) {
			}

			public void keyTyped(KeyEvent evt) {
			}
		});

		GroupLayout delimiterPanelLayout = new GroupLayout(delimiterPanel);
		delimiterPanel.setLayout(delimiterPanelLayout);
		delimiterPanelLayout
				.setHorizontalGroup(delimiterPanelLayout.createParallelGroup(
						GroupLayout.LEADING).add(
						delimiterPanelLayout.createSequentialGroup().add(
								tabCheckBox).addPreferredGap(
								org.jdesktop.layout.LayoutStyle.RELATED).add(
								commaCheckBox).addPreferredGap(
								org.jdesktop.layout.LayoutStyle.RELATED).add(
								semicolonCheckBox).addPreferredGap(
								org.jdesktop.layout.LayoutStyle.RELATED).add(
								spaceCheckBox).addPreferredGap(
								org.jdesktop.layout.LayoutStyle.RELATED).add(
								otherCheckBox).addPreferredGap(
								org.jdesktop.layout.LayoutStyle.RELATED).add(
								otherDelimiterTextField,
								GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)));
		delimiterPanelLayout.setVerticalGroup(delimiterPanelLayout
				.createParallelGroup(GroupLayout.LEADING).add(
						delimiterPanelLayout.createSequentialGroup().add(
								delimiterPanelLayout.createParallelGroup(
										GroupLayout.BASELINE).add(tabCheckBox)
										.add(commaCheckBox).add(
												semicolonCheckBox).add(
												spaceCheckBox).add(
												otherCheckBox).add(
												otherDelimiterTextField,
												GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE))
								.addContainerGap(GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)));

		attrNameCheckBox.setFont(new java.awt.Font("SansSerif", 1, 12));
		attrNameCheckBox.setText("Transfer first line as attribute names");
		attrNameCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(
				0, 0, 0, 0));
		attrNameCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		attrNameCheckBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				attrNameCheckBoxActionPerformed(evt);
			}
		});

		attrNameCheckBox.setEnabled(false);

		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(100, 1,
				10000000, 10);
		counterSpinner.setModel(spinnerModel);
		counterSpinner
				.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
					public void mouseWheelMoved(
							java.awt.event.MouseWheelEvent evt) {
						counterSpinnerMouseWheelMoved(evt);
					}
				});

		counterLabel.setText("entries.");

		reloadButton.setText("Reload");
		reloadButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
		reloadButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				try {
					reloadButtonActionPerformed(evt);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		showAllRadioButton.setText("Show all entries in the file");
		showAllRadioButton.setBorder(javax.swing.BorderFactory
				.createEmptyBorder(0, 0, 0, 0));
		showAllRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));

		counterRadioButton.setText("Show first ");
		counterRadioButton.setBorder(javax.swing.BorderFactory
				.createEmptyBorder(0, 0, 0, 0));
		counterRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));

		GroupLayout textImportOptionPanelLayout = new GroupLayout(
				textImportOptionPanel);
		textImportOptionPanel.setLayout(textImportOptionPanelLayout);

		textImportOptionPanelLayout
				.setHorizontalGroup(textImportOptionPanelLayout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								textImportOptionPanelLayout
										.createSequentialGroup()
										.add(
												delimiterPanel,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												textImportOptionPanelLayout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(attrNameCheckBox)
														.add(
																textImportOptionPanelLayout
																		.createSequentialGroup()
																		.add(
																				showAllRadioButton)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				counterRadioButton)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				counterSpinner,
																				org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																				58,
																				org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				counterLabel)
																		.add(
																				12,
																				12,
																				12)
																		.add(
																				reloadButton)))
										.addContainerGap(82, Short.MAX_VALUE)));
		textImportOptionPanelLayout
				.setVerticalGroup(textImportOptionPanelLayout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								textImportOptionPanelLayout
										.createSequentialGroup()
										.add(attrNameCheckBox)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												textImportOptionPanelLayout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(showAllRadioButton)
														.add(counterRadioButton)
														.add(counterLabel)
														.add(
																counterSpinner,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
														.add(reloadButton)))
						.add(delimiterPanel,
								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE));

		GroupLayout advancedPanelLayout = new GroupLayout(advancedPanel);
		advancedPanel.setLayout(advancedPanelLayout);
		advancedPanelLayout
				.setHorizontalGroup(advancedPanelLayout
						.createParallelGroup(GroupLayout.LEADING)
						.add(
								advancedPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.add(advancedOptionCheckBox)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(textImportCheckBox)
										.addContainerGap(330, Short.MAX_VALUE))
						.add(attr2annotationPanel, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(
								GroupLayout.TRAILING, ontology2annotationPanel,
								GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(
								textImportOptionPanel,
								GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
		advancedPanelLayout.setVerticalGroup(advancedPanelLayout
				.createParallelGroup(GroupLayout.LEADING).add(
						advancedPanelLayout.createSequentialGroup().add(
								advancedPanelLayout.createParallelGroup(
										GroupLayout.BASELINE).add(
										advancedOptionCheckBox).add(
										textImportCheckBox)).addPreferredGap(
								org.jdesktop.layout.LayoutStyle.RELATED).add(
								attr2annotationPanel,
								GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE).addPreferredGap(
								org.jdesktop.layout.LayoutStyle.RELATED).add(
								ontology2annotationPanel,
								GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE).addPreferredGap(
								org.jdesktop.layout.LayoutStyle.RELATED).add(
								textImportOptionPanel,
								GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
								.addContainerGap()));
		globalLayout();

		attr2annotationPanel.setVisible(false);
		basicPanel.repaint();
		ontology2annotationPanel.setVisible(false);
		textImportOptionPanel.setVisible(false);
		pack();
	}// </editor-fold>

	private void primaryKeyComboBoxActionPerformed(ActionEvent evt) {
		keyInFile = primaryKeyComboBox.getSelectedIndex();

		previewPanel.getPreviewTable().setDefaultRenderer(
				Object.class,
				new AttributePreviewTableCellRenderer(keyInFile,
						new ArrayList<Integer>(),
						AttributePreviewTableCellRenderer.PARAMETER_NOT_EXIST,
						AttributePreviewTableCellRenderer.PARAMETER_NOT_EXIST,
						importFlag, listDelimiter));
		try {
			setStatusBar(new URL(targetDataSourceTextField.getText()));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		previewPanel.repaint();

		JTable curTable = aliasTableMap
				.get(previewPanel.getSelectedSheetName());
		curTable.setModel(aliasTableModelMap.get(previewPanel
				.getSelectedSheetName()));

		if (curTable.getCellRenderer(0, 1) != null) {

			((AliasTableRenderer) curTable.getCellRenderer(0, 1))
					.setPrimaryKey(keyInFile);
			aliasScrollPane.setViewportView(curTable);

			primaryKeyMap.put(previewPanel.getSelectedSheetName(),
					primaryKeyComboBox.getSelectedIndex());

			aliasScrollPane.setViewportView(curTable);
			curTable.repaint();
		}
	}

	private void helpButtonActionPerformed(ActionEvent evt) {

	}

	private void otherTextFieldActionPerformed(KeyEvent evt) throws IOException {
		if (otherCheckBox.isSelected()) {
			displayPreview();
		}
	}

	private void attributeRadioButtonActionPerformed(ActionEvent evt) {

		if (evt.getSource() == nodeRadioButton) {
			selectedAttributes = Cytoscape.getNodeAttributes();
			objType = NODE;
		} else if (evt.getSource() == edgeRadioButton) {
			selectedAttributes = Cytoscape.getEdgeAttributes();
			objType = EDGE;
		} else {
			selectedAttributes = Cytoscape.getNetworkAttributes();
			objType = NETWORK;
		}
		updateMappingAttributeComboBox();
		setKeyList();

	}

	private void advancedOptionCheckBoxActionPerformed(ActionEvent evt) {
		if (advancedOptionCheckBox.isSelected()) {
			attr2annotationPanel.setVisible(true);
			ontology2annotationPanel.setVisible(true);
		} else {
			attr2annotationPanel.setVisible(false);
			ontology2annotationPanel.setVisible(false);
		}

		if (dialogType == ImportTextTableDialog.SIMPLE_ATTRIBUTE_IMPORT) {
			ontology2annotationPanel.setVisible(false);
		}
		pack();
	}

	private void nodeKeyComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
		keyInFile = mappingAttributeComboBox.getSelectedIndex();

		previewPanel.getPreviewTable().setDefaultRenderer(
				Object.class,
				new AttributePreviewTableCellRenderer(keyInFile,
						getAliasList(),
						AttributePreviewTableCellRenderer.PARAMETER_NOT_EXIST,
						AttributePreviewTableCellRenderer.PARAMETER_NOT_EXIST,
						importFlag, listDelimiter));

		setKeyList();
	}

	private void browseOntologyButtonActionPerformed(
			java.awt.event.ActionEvent evt) {
		DataSourceSelectDialog dssd = new DataSourceSelectDialog(
				DataSourceSelectDialog.ONTOLOGY_TYPE, Cytoscape.getDesktop(),
				true);
		dssd.setLocationRelativeTo(Cytoscape.getDesktop());
		dssd.setVisible(true);

		String key = dssd.getSourceName();
		if (key != null) {
			ontologyComboBox.insertItemAt(key, 0);
			ontologyUrlMap.put(key, dssd.getSourceUrlString());
			ontologyComboBox.setSelectedItem(key);
			ontologyComboBox.setToolTipText(getOntologyTooltip());
		}

	}

	private void attrNameCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
		final DefaultTableModel model = (DefaultTableModel) previewPanel
				.getPreviewTable().getModel();

		if (attrNameCheckBox.isSelected()) {

			if (previewPanel.getPreviewTable() != null && model != null) {
				columnHeaders = new String[previewPanel.getPreviewTable()
						.getColumnCount()];
				for (int i = 0; i < columnHeaders.length; i++) {
					// Save the header
					columnHeaders[i] = previewPanel.getPreviewTable()
							.getColumnModel().getColumn(i).getHeaderValue()
							.toString();
					previewPanel.getPreviewTable().getColumnModel()
							.getColumn(i).setHeaderValue(
									(String) model.getValueAt(0, i));

				}
				model.removeRow(0);
				previewPanel.getPreviewTable().getTableHeader()
						.resizeAndRepaint();
			}
		} else {
			// Restore row
			String currentName = null;
			for (int i = 0; i < columnHeaders.length; i++) {
				currentName = previewPanel.getPreviewTable().getColumnModel()
						.getColumn(i).getHeaderValue().toString();
				previewPanel.getPreviewTable().getColumnModel().getColumn(i)
						.setHeaderValue(columnHeaders[i]);
				columnHeaders[i] = currentName;
			}
			model.insertRow(0, columnHeaders);
			previewPanel.getPreviewTable().getTableHeader().resizeAndRepaint();
		}

		updateAliasTable();
		updatePrimaryKeyComboBox();
		repaint();

	}

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
		dispose();
	}

	/**
	 * Load from the data source.<br>
	 * 
	 * @param evt
	 * @throws Exception
	 */
	private void importButtonActionPerformed(ActionEvent evt) throws Exception {

		/*
		 * Get Attribute Names
		 */
		final String[] attributeNames = new String[previewPanel
				.getPreviewTable().getColumnModel().getColumnCount()];
		for (int i = 0; i < previewPanel.getPreviewTable().getColumnModel()
				.getColumnCount(); i++) {
			attributeNames[i] = previewPanel.getPreviewTable().getColumnModel()
					.getColumn(i).getHeaderValue().toString();
		}

		/*
		 * Get column indecies for alias
		 */
		final List<Integer> aliasList = new ArrayList<Integer>();
		JTable curTable = aliasTableMap
				.get(previewPanel.getSelectedSheetName());
		for (int i = 0; i < curTable.getModel().getRowCount(); i++) {
			if ((Boolean) curTable.getModel().getValueAt(i, 0) == true) {
				aliasList.add(i);
			}
		}

		/*
		 * Get import flags
		 */
		final boolean[] importFlag = new boolean[previewPanel.getPreviewTable()
				.getColumnCount()];
		for (int i = 0; i < previewPanel.getPreviewTable().getColumnCount(); i++) {
			importFlag[i] = ((AttributePreviewTableCellRenderer) previewPanel
					.getPreviewTable().getCellRenderer(0, i)).getImportFlag(i);
		}

		/*
		 * Get mapping attribute
		 */
		final String mappingAttribute = mappingAttributeComboBox
				.getSelectedItem().toString();

		/*
		 * Get attribute data types
		 */

		final byte[] attributeTypes = new byte[previewPanel.getPreviewTable()
				.getColumnCount()];

		for (int i = 0; i < attributeTypes.length; i++) {
			attributeTypes[i] = attributeDataTypes.get(i);
		}

		/*
		 * Switch readers based on the type of data soueces.
		 */
		switch (dialogType) {
		case SIMPLE_ATTRIBUTE_IMPORT:
			URL source = new URL(targetDataSourceTextField.getText());

			final AttributeMappingParameters mapping = new AttributeMappingParameters(
					NODE, checkDelimiter(), listDelimiter, keyInFile,
					mappingAttribute, aliasList, attributeNames,
					attributeTypes, importFlag);

			if (source.toString().endsWith(".xls")) {
				/*
				 * Read one sheet at a time
				 */

				POIFSFileSystem excelIn = new POIFSFileSystem(source
						.openStream());
				HSSFWorkbook wb = new HSSFWorkbook(excelIn);

				// Load all sheets in the table
				for (int i = 0; i < wb.getNumberOfSheets(); i++) {

					HSSFSheet sheet = wb.getSheetAt(i);
					loadAnnotation(
							new ExcelAttributeSheetReader(sheet, mapping),
							null, null);
				}

			} else {
				loadAnnotation(
						new DefaultAttributeTableReader(source, mapping), null,
						null);
			}

			break;
		case ONTOLOGY_AND_ANNOTATION_IMPORT:
			break;
		case NETWORK_IMPORT:
			break;
		default:
			return;
		}

		// loadAnnotation(tableReader, null, null);
		dispose();
	}

	private void ontologyInAnnotationComboBoxActionPerformed(
			java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
	}

	private void browseAnnotationButtonActionPerformed(
			java.awt.event.ActionEvent evt) {

		DataSourceSelectDialog dssd = new DataSourceSelectDialog(
				DataSourceSelectDialog.ANNOTATION_TYPE, Cytoscape.getDesktop(),
				true);
		dssd.setLocationRelativeTo(Cytoscape.getDesktop());
		dssd.setVisible(true);

		String key = dssd.getSourceName();
		if (key != null) {
			annotationComboBox.insertItemAt(key, 0);
			annotationUrlMap.put(key, dssd.getSourceUrlString());
			annotationComboBox.setSelectedItem(key);
			annotationComboBox.setToolTipText(getAnnotationTooltip());
		}
	}

	private void annotationComboBoxActionPerformed(
			java.awt.event.ActionEvent evt) {
		annotationComboBox.setToolTipText(getAnnotationTooltip());
		try {
			final String selectedSourceName = annotationComboBox
					.getSelectedItem().toString();
			final URL sourceURL = new URL(annotationUrlMap
					.get(selectedSourceName));
			readAnnotationForPreview(sourceURL, checkDelimiter());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void ontologyComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
		ontologyComboBox.setToolTipText(getOntologyTooltip());
		ontologyComboBox.setForeground(Color.red);
	}

	private void selectAttributeFileButtonActionPerformed(
			java.awt.event.ActionEvent evt) throws IOException {

		File sourceUrl = FileUtil.getFile("Select local file", FileUtil.LOAD,
				new CyFileFilter[] {});
		if (sourceUrl != null) {
			targetDataSourceTextField.setText(sourceUrl.toURL().toString());

			final URL sourceURL = new URL(targetDataSourceTextField.getText());

			readAnnotationForPreview(sourceURL, checkDelimiter());
			columnHeaders = new String[previewPanel.getPreviewTable()
					.getColumnCount()];
			attrNameCheckBox.setEnabled(true);
			attrNameCheckBox.setSelected(false);
		}
	}

	private void delimiterCheckBoxActionPerformed(ActionEvent evt)
			throws IOException {
		attrNameCheckBox.setSelected(false);
		displayPreview();
	}

	private void textImportCheckBoxActionPerformed(
			java.awt.event.ActionEvent evt) {
		if (textImportCheckBox.isSelected()) {
			textImportOptionPanel.setVisible(true);
		} else {
			textImportOptionPanel.setVisible(false);
		}
		pack();
	}

	private void reloadButtonActionPerformed(java.awt.event.ActionEvent evt)
			throws IOException {
		final URL sourceURL = new URL(targetDataSourceTextField.getText());
		readAnnotationForPreview(sourceURL, checkDelimiter());
	}

	/**
	 * Actions for mouse wheel movement
	 * 
	 * @param evt
	 */
	private void counterSpinnerMouseWheelMoved(
			java.awt.event.MouseWheelEvent evt) {
		JSpinner source = (JSpinner) evt.getSource();

		SpinnerNumberModel model = (SpinnerNumberModel) source.getModel();
		Integer oldValue = (Integer) source.getValue();
		int intValue = oldValue.intValue() - evt.getWheelRotation()
				* model.getStepSize().intValue() * 10;
		Integer newValue = new Integer(intValue);
		if (model.getMaximum().compareTo(newValue) >= 0
				&& model.getMinimum().compareTo(newValue) <= 0) {
			source.setValue(newValue);
		}
	}

	/* =============================================================================================== */

	private List<Integer> getAliasList() {
		final List<Integer> aliasList = new ArrayList<Integer>();
		AliasTableModel curModel = aliasTableModelMap.get(previewPanel
				.getSelectedSheetName());
		if (curModel == null) {
			return aliasList;
		}

		for (int i = 0; i < curModel.getRowCount(); i++) {
			if ((Boolean) curModel.getValueAt(i, 0)) {
				aliasList.add(i);
			}
		}
		return aliasList;
	}

	private void displayPreview() throws IOException {
		final String selectedSourceName;
		final URL sourceURL;
		if (dialogType == ONTOLOGY_AND_ANNOTATION_IMPORT) {
			selectedSourceName = annotationComboBox.getSelectedItem()
					.toString();
			sourceURL = new URL(annotationUrlMap.get(selectedSourceName));
		} else {
			selectedSourceName = targetDataSourceTextField.getText();
			sourceURL = new URL(selectedSourceName);
		}

		readAnnotationForPreview(sourceURL, checkDelimiter());
		previewPanel.repaint();
	}

	private void updateComponents() {
		/*
		 * Do misc. GUI setups
		 */
		if (dialogType == SIMPLE_ATTRIBUTE_IMPORT) {
			setTitle("Import Annotation File");
			titleLabel.setText("Import Attribute File");
			annotationAndOntologyImportPanel.setVisible(false);
		} else if (dialogType == ONTOLOGY_AND_ANNOTATION_IMPORT) {
			setTitle("Import Ontology Data and Annotations");
			titleLabel.setText("Import Ontology and Annotation");
			ontology2annotationPanel.setVisible(false);
		} else if (dialogType == NETWORK_IMPORT) {
			setTitle("Import Network and Edge Attributes from File");
			titleLabel.setText("Import Network Table");
			annotationAndOntologyImportPanel.setVisible(false);
		}
		
		reloadButton.setEnabled(false);

		previewPanel.getPreviewTable().getTableHeader().setReorderingAllowed(
				false);
		setRadioButtonGroup();
		pack();

		/*
		 * Add items to Ontology Combobox
		 */
		setOntologyComboBox();

		/*
		 * Setup annotation Combo Box
		 */
		setAnnotationComboBox();

		updateMappingAttributeComboBox();

		setStatusBar(" - ", " No Entries Loaded for Preview.",
				"File Size: Unknown");

		// setKeyList();
	}

	private void updatePrimaryKeyComboBox() {
		final DefaultTableModel model = (DefaultTableModel) previewPanel
				.getPreviewTable().getModel();

		primaryKeyComboBox
				.setRenderer(new ComboBoxRenderer(attributeDataTypes));
		if (model != null && model.getColumnCount() > 0) {

			primaryKeyComboBox.removeAllItems();
			for (int i = 0; i < model.getColumnCount(); i++) {
				primaryKeyComboBox.addItem(previewPanel.getPreviewTable()
						.getColumnModel().getColumn(i).getHeaderValue()
						.toString());
			}
		}
		primaryKeyComboBox.setEnabled(true);

		Integer selectedIndex = primaryKeyMap.get(previewPanel
				.getSelectedSheetName());
		if (selectedIndex == null) {
			primaryKeyComboBox.setSelectedIndex(0);
		} else {
			primaryKeyComboBox.setSelectedIndex(selectedIndex);
		}
	}

	protected static ImageIcon getDataTypeIcon(byte dataType) {
		ImageIcon dataTypeIcon = null;
		if (dataType == CyAttributes.TYPE_STRING) {
			dataTypeIcon = STRING_ICON.getIcon();
		} else if (dataType == CyAttributes.TYPE_INTEGER) {
			dataTypeIcon = INT_ICON.getIcon();
		} else if (dataType == CyAttributes.TYPE_FLOATING) {
			dataTypeIcon = FLOAT_ICON.getIcon();
		} else if (dataType == CyAttributes.TYPE_BOOLEAN) {
			dataTypeIcon = BOOLEAN_ICON.getIcon();
		} else if (dataType == CyAttributes.TYPE_SIMPLE_LIST) {
			dataTypeIcon = LIST_ICON.getIcon();
		}
		return dataTypeIcon;
	}

	private void setRadioButtonGroup() {
		attrTypeButtonGroup.add(nodeRadioButton);
		attrTypeButtonGroup.add(edgeRadioButton);
		attrTypeButtonGroup.add(networkRadioButton);
		attrTypeButtonGroup.setSelected(nodeRadioButton.getModel(), true);

		importTypeButtonGroup.add(showAllRadioButton);
		importTypeButtonGroup.add(counterRadioButton);
		importTypeButtonGroup.setSelected(counterRadioButton.getModel(), true);

		tabCheckBox.setSelected(true);
		tabCheckBox.setEnabled(false);
		commaCheckBox.setEnabled(false);
		spaceCheckBox.setEnabled(false);
		semicolonCheckBox.setEnabled(false);
		otherCheckBox.setEnabled(false);
		otherDelimiterTextField.setEnabled(false);
	}

	private void setOntologyComboBox() {
		Bookmarks bookmarks = Cytoscape.getOntologyServer().getBookmarks();
		List<DataSource> annotations = BookmarksUtil.getDataSourceList(
				"ontology", bookmarks.getCategory());
		String key = null;

		for (DataSource source : annotations) {
			key = source.getName();
			ontologyComboBox.addItem(key);
			ontologyUrlMap.put(key, source.getHref());
			ontologyDescriptionMap.put(key, BookmarksUtil.getAttribute(source,
					"description"));
		}
		ontologyComboBox.setToolTipText(getOntologyTooltip());
	}

	private String getOntologyTooltip() {
		final String key = ontologyComboBox.getSelectedItem().toString();
		String tooltip = ontologyHtml.replace("%DataSourceName%", key);
		final String description = ontologyDescriptionMap.get(key);
		if (description == null) {
			tooltip = tooltip.replace("%Description%", "N/A");
		} else {
			tooltip = tooltip.replace("%Description%", description);
		}
		return tooltip.replace("%SourceURL%", ontologyUrlMap.get(key));
	}

	private String getAnnotationTooltip() {
		final String key = annotationComboBox.getSelectedItem().toString();
		String tooltip = annotationHtml.replace("%DataSourceName%", key);
		if (annotationUrlMap.get(key) == null) {
			return "";
		}
		tooltip = tooltip.replace("%SourceURL%", annotationUrlMap.get(key));

		if (annotationFormatMap.get(key) != null) {
			tooltip = tooltip.replace("%Format%", annotationFormatMap.get(key));
		} else {
			String[] parts = annotationUrlMap.get(key).split("/");
			if (parts[parts.length - 1].startsWith(GENE_ASSOCIATION)) {
				tooltip = tooltip.replace("%Format%", "Gene Association");
			}
			tooltip = tooltip.replace("%Format%",
					"General Annotation Text Table");
		}

		if (annotationAttributesMap.get(key) != null) {
			StringBuffer table = new StringBuffer();
			final Map<String, String> annotations = annotationAttributesMap
					.get(key);
			for (String anno : annotations.keySet()) {
				table.append("<tr>");
				table.append("<td><strong>" + anno + "</strong></td><td>"
						+ annotations.get(anno) + "</td>");
				table.append("</tr>");
			}

			return tooltip.replace("%AttributeTable%", table.toString());
		}
		return tooltip.replace("%AttributeTable%", "");
	}

	private void setAnnotationComboBox() {

		Bookmarks bookmarks = Cytoscape.getOntologyServer().getBookmarks();
		List<DataSource> annotations = BookmarksUtil.getDataSourceList(
				"annotation", bookmarks.getCategory());
		String key = null;
		for (DataSource source : annotations) {
			key = source.getName();
			annotationComboBox.addItem(key);
			annotationUrlMap.put(key, source.getHref());
			annotationFormatMap.put(key, source.getFormat());

			final Map<String, String> attrMap = new HashMap<String, String>();
			for (Attribute attr : source.getAttribute()) {
				attrMap.put(attr.getName(), attr.getContent());
			}
			annotationAttributesMap.put(key, attrMap);
		}

		annotationComboBox.setToolTipText(getAnnotationTooltip());
	}

	/**
	 * Generate preview table.<br>
	 * 
	 * <p>
	 * </p>
	 * 
	 * @throws IOException
	 */
	private void readAnnotationForPreview(URL sourceURL, List<String> delimiters)
			throws IOException {

		final int previewSize;
		if (showAllRadioButton.isSelected()) {
			previewSize = -1;
		} else {
			previewSize = Integer
					.parseInt(counterSpinner.getValue().toString());
		}

		previewPanel.setPreviewTable(sourceURL, delimiters,
				new AttributePreviewTableCellRenderer(keyInFile,
						new ArrayList<Integer>(),
						AttributePreviewTableCellRenderer.PARAMETER_NOT_EXIST,
						AttributePreviewTableCellRenderer.PARAMETER_NOT_EXIST,
						null, listDelimiter), previewSize);

		/*
		 * Initialize all Alias Tables
		 */
		for (int i = 0; i < previewPanel.getTableCount(); i++) {
			final int columnCount = previewPanel.getPreviewTable(i)
					.getColumnCount();

			System.out.println("%%%% tab name = "
					+ previewPanel.getSheetName(i) + ", col count = "
					+ columnCount);

			aliasTableModelMap.put(previewPanel.getSheetName(i),
					new AliasTableModel(keyTable, columnCount));

			initializeAliasTable(columnCount, null, i);

			updatePrimaryKeyComboBox();

		}

		setKeyList();

		/*
		 * Set Status bar
		 */
		setStatusBar(sourceURL);

		/*
		 * If this is not an Excel file, enable delimiter checkboxes.
		 */
		if (!sourceURL.toString().endsWith(".xls")) {
			tabCheckBox.setEnabled(true);
			commaCheckBox.setEnabled(true);
			spaceCheckBox.setEnabled(true);
			semicolonCheckBox.setEnabled(true);
			otherCheckBox.setEnabled(true);
			otherDelimiterTextField.setEnabled(true);
		}
		pack();
		repaint();
		
		reloadButton.setEnabled(true);
	}

	private void setStatusBar(URL sourceURL) {
		final String centerMessage;
		final String rightMessage;
		if (showAllRadioButton.isSelected()) {
			centerMessage = "All entries are loaded for preview.";
		} else {
			centerMessage = counterSpinner.getValue().toString()
					+ " Entries are loaded for preview.";
		}

		if (sourceURL.toString().startsWith("file:")) {
			rightMessage = "File Size: ";
		} else {
			rightMessage = "File Size Unknown (Remote Data Source)";
		}

		setStatusBar("Nuber of matched primary key & attribute pair: "
				+ previewPanel.checkKeyMatch(primaryKeyComboBox
						.getSelectedIndex()), centerMessage, rightMessage);
	}

	/**
	 * Update the list of mapping attributes.
	 * 
	 */
	private void setKeyList() {

		String selectedKeyAttribute = mappingAttributeComboBox
				.getSelectedItem().toString();

		Iterator it;

		// if(objType == NODE) {
		// it = Cytoscape.getRootGraph().nodesIterator();
		// }else if(objType == EDGE) {
		// it = Cytoscape.getRootGraph().edgesIterator();
		// } else {
		// it = Cytoscape.getNetworkSet().iterator();
		// }

		Set<String> valueSet = new TreeSet<String>();
		if (selectedKeyAttribute.equals(ID)) {
			if (objType == NODE) {
				it = Cytoscape.getRootGraph().nodesIterator();
				while (it.hasNext()) {
					Node node = (Node) it.next();
					valueSet.add(node.getIdentifier());
				}
			} else if (objType == EDGE) {
				it = Cytoscape.getRootGraph().edgesIterator();
				while (it.hasNext()) {
					valueSet.add(((Edge) it.next()).getIdentifier());
				}
			} else {
				it = Cytoscape.getNetworkSet().iterator();
				while (it.hasNext()) {
					valueSet.add(((CyNetwork) it.next()).getTitle());
				}
			}

		} else {

			final byte attrType = selectedAttributes
					.getType(selectedKeyAttribute);

			Object value = null;

			if (objType == NODE) {
				List<Node> nodeList = Cytoscape.getCyNodesList();
				for (Node node : nodeList) {
					value = selectedAttributes.getStringAttribute(node
							.getIdentifier(), selectedKeyAttribute);
					if (value != null) {
						valueSet.add(value.toString());
					}
				}
			} else if (objType == EDGE) {
				List<Edge> edgeList = Cytoscape.getCyNodesList();
				for (Edge edge : edgeList) {
					value = selectedAttributes.getStringAttribute(edge
							.getIdentifier(), selectedKeyAttribute);
					if (value != null) {
						valueSet.add(value.toString());
					}
				}
			} else {

			}

		}

		previewPanel.setKeyAttributeList(valueSet);

		// nodeKeyList.setListData(valueSet.toArray());
	}

	private void updateAliasTableCell(String name, int columnIndex) {
		JTable curTable = aliasTableMap
				.get(previewPanel.getSelectedSheetName());
		curTable.setDefaultRenderer(Object.class, new AliasTableRenderer(
				attributeDataTypes, primaryKeyComboBox.getSelectedIndex()));
		AliasTableModel curModel = aliasTableModelMap.get(previewPanel
				.getSelectedSheetName());
		curModel.setValueAt(name, columnIndex, 1);
		curTable.setModel(curModel);
		curTable.repaint();
		aliasScrollPane.repaint();
		repaint();
	}

	private void updateAliasTable() {
		JTable curTable = aliasTableMap
				.get(previewPanel.getSelectedSheetName());

		curTable.setDefaultRenderer(Object.class, new AliasTableRenderer(
				attributeDataTypes, primaryKeyComboBox.getSelectedIndex()));

		AliasTableModel curModel = aliasTableModelMap.get(previewPanel
				.getSelectedSheetName());
		for (int i = 0; i < previewPanel.getPreviewTable().getColumnCount(); i++) {
			curModel.setValueAt(previewPanel.getPreviewTable().getColumnModel()
					.getColumn(i).getHeaderValue().toString(), i, 1);
		}
		curTable.setModel(curModel);
		aliasScrollPane.setViewportView(curTable);
		aliasScrollPane.repaint();
	}

	private void initializeAliasTable(int rowCount, String[] columnNames) {
		initializeAliasTable(rowCount, columnNames, -1);

	}

	private void initializeAliasTable(int rowCount, String[] columnNames,
			int sheetIndex) {
		Object[][] keyTableData = new Object[rowCount][keyTable.length];

		AliasTableModel curModel = null;
		String tabName;

		if (sheetIndex == -1) {
			tabName = previewPanel.getSelectedSheetName();
		} else {
			tabName = previewPanel.getSheetName(sheetIndex);
		}
		curModel = aliasTableModelMap.get(tabName);

		curModel = new AliasTableModel();
		Byte[] dataTypeArray = previewPanel.getDataTypes(tabName);
		for (int i = 0; i < rowCount; i++) {
			keyTableData[i][0] = new Boolean(false);
			if (columnNames == null) {
				keyTableData[i][1] = "Column " + (i + 1);

			} else {
				keyTableData[i][1] = columnNames[i];
			}
			attributeDataTypes.add(dataTypeArray[i]);
			keyTableData[i][2] = "String";
		}

		curModel = new AliasTableModel(keyTableData, keyTable);

		aliasTableModelMap.put(tabName, curModel);

		curModel.addTableModelListener(this);
		/*
		 * Set the list and combo box
		 */
		mappingAttributeComboBox.setEnabled(true);

		JTable curTable = new JTable();
		curTable.setModel(curModel);
		aliasTableMap.put(tabName, curTable);

		curTable.setDefaultRenderer(Object.class, new AliasTableRenderer(
				attributeDataTypes, primaryKeyComboBox.getSelectedIndex()));
		curTable.setEnabled(true);
		curTable.setSelectionBackground(Color.white);
		curTable.getTableHeader().setReorderingAllowed(false);

		curTable.getColumnModel().getColumn(0).setPreferredWidth(55);
		curTable.getColumnModel().getColumn(1).setPreferredWidth(300);
		curTable.getColumnModel().getColumn(2).setPreferredWidth(100);

		aliasScrollPane.setViewportView(curTable);

		repaint();
	}

	private void updateMappingAttributeComboBox() {
		mappingAttributeComboBox.removeAllItems();
		final ListCellRenderer lcr = mappingAttributeComboBox.getRenderer();
		mappingAttributeComboBox.setRenderer(new ListCellRenderer() {
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel cmp = (JLabel) lcr.getListCellRendererComponent(list,
						value, index, isSelected, cellHasFocus);
				if (value.equals(ID)) {
					cmp.setIcon(ID_ICON.getIcon());
				} else {
					cmp.setIcon(getDataTypeIcon(selectedAttributes
							.getType(value.toString())));
				}
				return cmp;
			}
		});

		mappingAttributeComboBox.addItem(ID);
		for (String name : selectedAttributes.getAttributeNames()) {
			mappingAttributeComboBox.addItem(name);
		}
	}

	private void loadAnnotation(TextTableReader reader, String ontology,
			String source) {
		// Create LoadNetwork Task
		ImportAttributeTableTask task = new ImportAttributeTableTask(reader,
				ontology, source);

		// Configure JTask Dialog Pop-Up Box
		JTaskConfig jTaskConfig = new JTaskConfig();
		jTaskConfig.setOwner(Cytoscape.getDesktop());
		jTaskConfig.displayCloseButton(true);
		jTaskConfig.displayStatus(true);
		jTaskConfig.setAutoDispose(false);

		// Execute Task in New Thread; pops open JTask Dialog Box.
		TaskManager.executeTask(task, jTaskConfig);
	}

	private void setStatusBar(String message1, String message2, String message3) {

		statusBar.setLeftLabel(message1);
		statusBar.setCenterLabel(message2);
		statusBar.setRightLabel(message3);
	}

	/**
	 * Alias table changed.
	 */
	public void tableChanged(TableModelEvent evt) {
		final int row = evt.getFirstRow();
		final int col = evt.getColumn();
		AliasTableModel curModel = aliasTableModelMap.get(previewPanel
				.getSelectedSheetName());

		if (col == 0) {
			previewPanel.setAliasColumn(row, (Boolean) curModel.getValueAt(row,
					col));
		}
		System.out.println("Value changed: " + evt.getFirstRow() + ", "
				+ evt.getColumn() + " = " + curModel.getValueAt(row, col));
		aliasScrollPane.repaint();
	}

	private List<String> checkDelimiter() {
		final List<String> delList = new ArrayList<String>();
		if (tabCheckBox.isSelected()) {
			delList.add(TextFileDelimiters.TAB.toString());
		}

		if (commaCheckBox.isSelected()) {
			delList.add(TextFileDelimiters.COMMA.toString());
		}

		if (spaceCheckBox.isSelected()) {
			delList.add(TextFileDelimiters.SPACE.toString());
		}

		if (semicolonCheckBox.isSelected()) {
			delList.add(TextFileDelimiters.SEMICOLON.toString());
		}

		if (otherCheckBox.isSelected()) {
			delList.add(otherDelimiterTextField.getText());
		}

		return delList;
	}

	/*
	 * Layout Information for the entire dialog.<br>
	 * 
	 * <p> This layout will be switched by dialog type parameter. </p>
	 * 
	 */
	private void globalLayout() {
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);

		/*
		 * Case 1: Simple Attribute Import
		 */
		if (dialogType == SIMPLE_ATTRIBUTE_IMPORT) {

			layout
					.setHorizontalGroup(layout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.LEADING)
															.add(
																	org.jdesktop.layout.GroupLayout.TRAILING,
																	layout
																			.createSequentialGroup()
																			.add(
																					statusBar,
																					org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																					org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																					Short.MAX_VALUE)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					importButton)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					cancelButton))
															.add(
																	previewPanel,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	org.jdesktop.layout.GroupLayout.TRAILING,
																	advancedPanel,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	basicPanel,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	org.jdesktop.layout.GroupLayout.TRAILING,
																	layout
																			.createSequentialGroup()
																			.add(
																					titleIconLabel1)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					titleIconLabel2)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					titleIconLabel3)
																			.add(
																					20,
																					20,
																					20)
																			.add(
																					titleLabel)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED,
																					487,
																					Short.MAX_VALUE)
																			.add(
																					helpButton,
																					org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																					org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																					org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
															.add(
																	titleSeparator,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	931,
																	Short.MAX_VALUE))
											.addContainerGap()));
			layout
					.setVerticalGroup(layout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.TRAILING)
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					layout
																							.createParallelGroup(
																									org.jdesktop.layout.GroupLayout.BASELINE)
																							.add(
																									helpButton,
																									org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																									20,
																									org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																							.add(
																									titleIconLabel1)
																							.add(
																									titleIconLabel2)
																							.add(
																									titleIconLabel3))
																			.add(
																					2,
																					2,
																					2))
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					titleLabel)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)))
											.add(
													titleSeparator,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
													10,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													basicPanel,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													advancedPanel,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													previewPanel,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													Short.MAX_VALUE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.LEADING,
																	false)
															.add(
																	layout
																			.createParallelGroup(
																					org.jdesktop.layout.GroupLayout.BASELINE)
																			.add(
																					cancelButton)
																			.add(
																					importButton))
															.add(
																	statusBar,
																	org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
											.addContainerGap()));
			pack();

		} else if (dialogType == ONTOLOGY_AND_ANNOTATION_IMPORT) {
			layout
					.setHorizontalGroup(layout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.LEADING)
															.add(
																	org.jdesktop.layout.GroupLayout.TRAILING,
																	layout
																			.createSequentialGroup()
																			.add(
																					statusBar,
																					org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																					org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																					Short.MAX_VALUE)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					importButton)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					cancelButton))
															.add(
																	previewPanel,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	org.jdesktop.layout.GroupLayout.TRAILING,
																	advancedPanel,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	basicPanel,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	org.jdesktop.layout.GroupLayout.TRAILING,
																	layout
																			.createSequentialGroup()
																			.add(
																					titleIconLabel1)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					titleIconLabel2)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					titleIconLabel3)
																			.add(
																					20,
																					20,
																					20)
																			.add(
																					titleLabel)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED,
																					487,
																					Short.MAX_VALUE)
																			.add(
																					helpButton,
																					org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																					org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																					org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
															.add(
																	titleSeparator,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	931,
																	Short.MAX_VALUE))
											.addContainerGap()));
			layout
					.setVerticalGroup(layout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.TRAILING)
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					layout
																							.createParallelGroup(
																									org.jdesktop.layout.GroupLayout.BASELINE)
																							.add(
																									helpButton,
																									org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																									20,
																									org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																							.add(
																									titleIconLabel1)
																							.add(
																									titleIconLabel2)
																							.add(
																									titleIconLabel3))
																			.add(
																					2,
																					2,
																					2))
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					titleLabel)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)))
											.add(
													titleSeparator,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
													10,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													basicPanel,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													advancedPanel,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													previewPanel,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													Short.MAX_VALUE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.LEADING,
																	false)
															.add(
																	layout
																			.createParallelGroup(
																					org.jdesktop.layout.GroupLayout.BASELINE)
																			.add(
																					cancelButton)
																			.add(
																					importButton))
															.add(
																	statusBar,
																	org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
											.addContainerGap()));

			annotationAndOntologyImportPanel.setVisible(true);
			attrTypePanel.setVisible(true);
		} else {
			layout
					.setHorizontalGroup(layout
							.createParallelGroup(GroupLayout.LEADING)
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													layout
															.createParallelGroup(
																	GroupLayout.LEADING)
															.add(
																	GroupLayout.TRAILING,
																	previewPanel,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	GroupLayout.TRAILING,
																	advancedPanel,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	GroupLayout.TRAILING,
																	networkImportPanel,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	basicPanel,
																	GroupLayout.DEFAULT_SIZE,
																	GroupLayout.DEFAULT_SIZE,
																	Short.MAX_VALUE)
															.add(
																	GroupLayout.TRAILING,
																	layout
																			.createSequentialGroup()
																			.add(
																					titleIconLabel1)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					titleIconLabel2)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					titleIconLabel3)
																			.add(
																					20,
																					20,
																					20)
																			.add(
																					titleLabel)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED,
																					349,
																					Short.MAX_VALUE)
																			.add(
																					helpButton,
																					GroupLayout.PREFERRED_SIZE,
																					GroupLayout.DEFAULT_SIZE,
																					GroupLayout.PREFERRED_SIZE))
															.add(
																	titleSeparator,
																	GroupLayout.DEFAULT_SIZE,
																	793,
																	Short.MAX_VALUE)
															.add(
																	GroupLayout.TRAILING,
																	layout
																			.createSequentialGroup()
																			.add(
																					importButton)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					cancelButton)))
											.addContainerGap()));
			layout
					.setVerticalGroup(layout
							.createParallelGroup(GroupLayout.LEADING)
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													layout
															.createParallelGroup(
																	GroupLayout.TRAILING)
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					layout
																							.createParallelGroup(
																									GroupLayout.BASELINE)
																							.add(
																									helpButton,
																									GroupLayout.PREFERRED_SIZE,
																									20,
																									GroupLayout.PREFERRED_SIZE)
																							.add(
																									titleIconLabel1)
																							.add(
																									titleIconLabel2)
																							.add(
																									titleIconLabel3))
																			.add(
																					2,
																					2,
																					2))
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					titleLabel)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)))
											.add(titleSeparator,
													GroupLayout.PREFERRED_SIZE,
													10,
													GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(basicPanel,
													GroupLayout.PREFERRED_SIZE,
													GroupLayout.DEFAULT_SIZE,
													GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(networkImportPanel,
													GroupLayout.PREFERRED_SIZE,
													GroupLayout.DEFAULT_SIZE,
													GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(advancedPanel,
													GroupLayout.PREFERRED_SIZE,
													GroupLayout.DEFAULT_SIZE,
													GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(previewPanel,
													GroupLayout.DEFAULT_SIZE,
													GroupLayout.DEFAULT_SIZE,
													Short.MAX_VALUE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													layout
															.createParallelGroup(
																	GroupLayout.BASELINE)
															.add(cancelButton)
															.add(importButton))
											.addContainerGap()));

			annotationAndOntologyImportPanel.setVisible(false);
			attrTypePanel.setVisible(false);
			advancedOptionCheckBox.setVisible(false);
			// advancedOptionPanel.setVisible(false);
		}

		pack();
	}
	/*
	 * Case 2: Ontology + Annotation Import
	 */

	/*
	 * Case 3: Network File Import
	 */

	// Variables declaration - do not modify
	private javax.swing.JCheckBox advancedOptionCheckBox;
	private javax.swing.JPanel advancedPanel;
	// private JTable aliasTable;
	private javax.swing.JScrollPane aliasScrollPane;
	private javax.swing.JPanel annotationAndOntologyImportPanel;
	private javax.swing.JButton arrowButton1;
	private javax.swing.JButton arrowButton2;
	private javax.swing.JPanel attr2annotationPanel;
	private javax.swing.JCheckBox attrNameCheckBox;
	private javax.swing.ButtonGroup attrTypeButtonGroup;
	private javax.swing.JLabel attribuiteLabel;
	private javax.swing.JLabel attributeFileLabel;
	private javax.swing.JPanel basicPanel;
	private javax.swing.JButton browseAnnotationButton;
	private javax.swing.JButton browseOntologyButton;
	private javax.swing.JButton cancelButton;
	private javax.swing.JCheckBox commaCheckBox;
	private javax.swing.JPanel delimiterPanel;
	private javax.swing.JRadioButton edgeRadioButton;
	private javax.swing.JButton helpButton;
	private javax.swing.JButton importButton;
	private javax.swing.JRadioButton networkRadioButton;
	private javax.swing.JComboBox mappingAttributeComboBox;
	private javax.swing.JLabel nodeKeyLabel;
	private javax.swing.JRadioButton nodeRadioButton;
	private javax.swing.JPanel ontology2annotationPanel;
	private javax.swing.JComboBox ontologyComboBox;
	private javax.swing.JComboBox ontologyInAnnotationComboBox;
	private javax.swing.JLabel ontologyInAnnotationLabel;
	private javax.swing.JLabel ontologyLabel;
	private javax.swing.JTextField otherDelimiterTextField;
	private javax.swing.JCheckBox otherCheckBox;
	private PreviewTablePanel previewPanel;
	private javax.swing.JLabel primaryKeyLabel;
	private javax.swing.JButton selectAttributeFileButton;
	private javax.swing.JCheckBox semicolonCheckBox;
	private javax.swing.JPanel simpleAttributeImportPanel;
	private javax.swing.JComboBox annotationComboBox;
	private javax.swing.JLabel sourceLabel;
	private javax.swing.JCheckBox spaceCheckBox;
	private javax.swing.JCheckBox tabCheckBox;
	private javax.swing.JTextField targetDataSourceTextField;
	private javax.swing.JLabel targetOntologyLabel;
	private javax.swing.JTextField targetOntologyTextField;
	private javax.swing.JCheckBox textImportCheckBox;
	private javax.swing.JPanel textImportOptionPanel;
	private javax.swing.JLabel titleIconLabel1;
	private javax.swing.JLabel titleIconLabel2;
	private javax.swing.JLabel titleIconLabel3;
	private javax.swing.JLabel titleLabel;
	private javax.swing.JSeparator titleSeparator;
	private JComboBox primaryKeyComboBox;
	private JLabel primaryLabel;

	private JPanel attrTypePanel;
	private javax.swing.JRadioButton showAllRadioButton;
	private javax.swing.JLabel counterLabel;
	private javax.swing.JRadioButton counterRadioButton;
	private javax.swing.JButton reloadButton;
	private javax.swing.JSpinner counterSpinner;
	private javax.swing.ButtonGroup importTypeButtonGroup;

	// End of variables declaration

	JStatusBar statusBar;

	private NetworkImportOptionsPanel networkImportPanel;

	// private DefaultTableModel model;
	private AliasTableModel aliasTableModel;
	private JTable aliasTable;
}

class ComboBoxRenderer extends JLabel implements ListCellRenderer {

	private List<Byte> attributeDataTypes;

	public ComboBoxRenderer(List<Byte> attributeDataTypes) {
		this.attributeDataTypes = attributeDataTypes;
	}

	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {

		setText(value.toString());

		if (attributeDataTypes != null && attributeDataTypes.size() != 0
				&& index < attributeDataTypes.size() && index >= 0) {
			final Byte dataType = attributeDataTypes.get(index);
			if (dataType == null) {
				setIcon(null);
			} else {
				setIcon(ImportTextTableDialog.getDataTypeIcon(dataType));
			}
		} else if (attributeDataTypes != null && attributeDataTypes.size() != 0
				&& index < attributeDataTypes.size()) {

			setIcon(ImportTextTableDialog.getDataTypeIcon(attributeDataTypes
					.get(list.getSelectedIndex())));
		}
		return this;

	}
}

class AliasTableModel extends DefaultTableModel {

	AliasTableModel(String[] columnNames, int rowNum) {
		super(columnNames, rowNum);
	}

	AliasTableModel(Object[][] data, Object[] colNames) {
		super(data, colNames);
	}

	AliasTableModel() {
		super();
	}

	public Class getColumnClass(int col) {
		return getValueAt(0, col).getClass();
	}

	public boolean isCellEditable(int row, int column) {
		if (column == 0) {
			return true;
		} else {
			return false;
		}
	}
}