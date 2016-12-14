package za.ac.uct.cs.tddonto2;

import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.owl.model.classexpression.OWLExpressionParserException;
import org.protege.editor.owl.model.inference.OWLReasonerManager;
import org.protege.editor.owl.model.inference.ReasonerStatus;
import org.protege.editor.owl.model.inference.ReasonerUtilities;
import org.protege.editor.owl.model.parser.ParserUtil;
import org.protege.editor.owl.ui.clsdescriptioneditor.ExpressionEditor;
import org.protege.editor.owl.ui.clsdescriptioneditor.OWLExpressionChecker;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.OWLAPIConfigProvider;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TestSuiteViewComponent extends AbstractOWLViewComponent {

    ManchesterOWLSyntaxParser parser;
    private ExpressionEditor<OWLAxiom> editor;
    private JLabel editorResultLabel;
    private JTable table;
    private TestSuiteModel testSuite;

    @Override
    protected void initialiseOWLView() throws Exception {
        parser = new ManchesterOWLSyntaxParserFixed(new OWLAPIConfigProvider(), getOWLDataFactory());
        parser.setOWLEntityChecker(
                new ShortFormEntityChecker(
                        new BidirectionalShortFormProviderAdapter(
                                getOWLModelManager().getActiveOntology().getOWLOntologyManager(),
                                getOWLModelManager().getOntologies(),
                                new SimpleShortFormProvider()
                        )
                )
        );

        setLayout(new BorderLayout(10, 10));

        // Top panel with editor

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "New test"),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        add(editorPanel, BorderLayout.NORTH);

        editor = new ExpressionEditor<>(getOWLEditorKit(), new OWLExpressionChecker<OWLAxiom>() {
            @Override
            public void check(String text) throws OWLExpressionParserException {
                createObject(text);
            }

            @Override
            public OWLAxiom createObject(String text) throws OWLExpressionParserException {
                parser.setStringToParse(text);
                try {
                    return parser.parseAxiom();
                } catch (ParserException e) {
                    throw ParserUtil.convertException(e);
                }
            }
        });
        editor.addStatusChangedListener(e -> clearEditorTestResult());
        editorPanel.add(editor, BorderLayout.CENTER);

        JPanel editorButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editorPanel.add(editorButtonsPanel, BorderLayout.SOUTH);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addTest());
        editorButtonsPanel.add(addButton);

        JButton editorEvalButton = new JButton("Evaluate");
        editorEvalButton.addActionListener(e -> evaluateEditorTest());
        editorButtonsPanel.add(editorEvalButton);

        editorResultLabel = new JLabel();
        editorButtonsPanel.add(editorResultLabel);

        // Main table

        testSuite = new TestSuiteModel();
        table = new JTable(testSuite);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(getOWLModelManager().getRendering((OWLObject) value));
            }
        });
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                TestResult result = (TestResult) value;
                if (result == null) {
                    setText("");
                } else {
                    setText(result.humanize());
                }
                if (result == TestResult.ENTAILED) {
                    setBackground(Color.GREEN);
                } else {
                    setBackground(Color.RED);
                }
            }
        });
        add(ComponentFactory.createScrollPane(table), BorderLayout.CENTER);

        // Main buttons

        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        add(mainButtonsPanel, BorderLayout.SOUTH);

        JButton evalAllButton = new JButton("Evaluate all");
        evalAllButton.addActionListener(e -> evaluateAllTests());
        mainButtonsPanel.add(evalAllButton);

        JButton evalSelectedButton = new JButton("Evaluate selected");
        evalSelectedButton.addActionListener(e -> evaluateSelectedTests());
        mainButtonsPanel.add(evalSelectedButton);

        JButton removeButton = new JButton("Remove selected");
        removeButton.addActionListener(e -> removeSelectedTests());
        mainButtonsPanel.add(removeButton);

        JButton addToOntologyButton = new JButton("Add selected to ontology");
        addToOntologyButton.addActionListener(e -> addSelectedTestsToOntology());
        mainButtonsPanel.add(addToOntologyButton);
    }

    @Override
    protected void disposeOWLView() {
    }

    private OWLAxiom parseEditor() {
        try {
            parser.setStringToParse(editor.getText());
            return parser.parseAxiom();
        } catch (OWLParserException e) {
            JOptionPane.showMessageDialog(
                    getOWLWorkspace(),
                    "Syntax error: " + e.getMessage(),
                    "Syntax error.",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
    }

    private void addTest() {
        OWLAxiom axiom = parseEditor();
        if (axiom == null) {
            return;
        }
        testSuite.add(axiom);
        testSuite.fireTableDataChanged();
    }

    private void clearEditorTestResult() {
        editorResultLabel.setText("");
    }

    private void evaluateEditorTest() {
        OWLReasonerManager reasonerManager = getOWLModelManager().getOWLReasonerManager();
        if (reasonerManager.getReasonerStatus() != ReasonerStatus.INITIALIZED) {
            ReasonerUtilities.warnUserIfReasonerIsNotConfigured(getOWLWorkspace(), reasonerManager);
            return;
        }

        AxiomTester axiomTester = new AxiomTester(reasonerManager.getCurrentReasoner());

        OWLAxiom axiom = parseEditor();
        if (axiom == null) {
            return;
        }

        TestResult preconditionResult = axiomTester.testPreconditions();
        if (preconditionResult != null) {
            editorResultLabel.setText(preconditionResult.humanize());
        } else {
            TestResult result = axiomTester.test(axiom);
            editorResultLabel.setText(result.humanize());
        }
    }

    private void evaluateAllTests() {
        OWLReasonerManager reasonerManager = getOWLModelManager().getOWLReasonerManager();
        if (reasonerManager.getReasonerStatus() != ReasonerStatus.INITIALIZED) {
            ReasonerUtilities.warnUserIfReasonerIsNotConfigured(getOWLWorkspace(), reasonerManager);
            return;
        }

        testSuite.evaluateAll(new AxiomTester(reasonerManager.getCurrentReasoner()));
    }

    private void evaluateSelectedTests() {
        OWLReasonerManager reasonerManager = getOWLModelManager().getOWLReasonerManager();
        if (reasonerManager.getReasonerStatus() != ReasonerStatus.INITIALIZED) {
            ReasonerUtilities.warnUserIfReasonerIsNotConfigured(getOWLWorkspace(), reasonerManager);
            return;
        }

        testSuite.evaluateOnly(new AxiomTester(reasonerManager.getCurrentReasoner()), table.getSelectedRows());
    }

    private void removeSelectedTests() {
        testSuite.remove(table.getSelectedRows());
    }

    private void addSelectedTestsToOntology() {

    }
}
