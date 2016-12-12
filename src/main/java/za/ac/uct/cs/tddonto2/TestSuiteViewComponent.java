package za.ac.uct.cs.tddonto2;

import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.owl.model.inference.OWLReasonerManager;
import org.protege.editor.owl.model.inference.ReasonerStatus;
import org.protege.editor.owl.model.inference.ReasonerUtilities;
import org.protege.editor.owl.ui.clsdescriptioneditor.ExpressionEditor;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.expression.OWLExpressionParser;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxInlineAxiomParser;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class TestSuiteViewComponent extends AbstractOWLViewComponent {

    OWLExpressionParser<OWLAxiom> parser;
    private ExpressionEditor<OWLClassAxiom> editor;
    private JLabel editorResultLabel;
    private JTable table;
    private DefaultTableModel tableModel;

    @Override
    protected void initialiseOWLView() throws Exception {
        parser = new ManchesterOWLSyntaxInlineAxiomParser(
                getOWLDataFactory(),
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

        editor = new ExpressionEditor<>(getOWLEditorKit(), getOWLModelManager().getOWLExpressionCheckerFactory().getClassAxiomChecker());
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

        Object[] columnHeads = {"Test axiom", "Result"};
        tableModel = new DefaultTableModel(new Object[][] {}, columnHeads);
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
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

    private void addTest() {
        tableModel.addRow(new Object[] {editor.getText(), null});
        tableModel.fireTableDataChanged();
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

        try {
            AxiomTester axiomTester = new AxiomTester(reasonerManager.getCurrentReasoner());

            TestResult preconditionResult = axiomTester.testPreconditions();
            if (preconditionResult != null) {
                editorResultLabel.setText(preconditionResult.humanize());
            } else {
                TestResult result = axiomTester.test(parser.parse(editor.getText()));
                editorResultLabel.setText(result.humanize());
            }
        } catch (OWLParserException e) {
            editorResultLabel.setText("Syntax error or missing entity");
        }
    }

    private void evaluateAllTests() {
        OWLReasonerManager reasonerManager = getOWLModelManager().getOWLReasonerManager();
        if (reasonerManager.getReasonerStatus() != ReasonerStatus.INITIALIZED) {
            ReasonerUtilities.warnUserIfReasonerIsNotConfigured(getOWLWorkspace(), reasonerManager);
            return;
        }

        AxiomTester axiomTester = new AxiomTester(reasonerManager.getCurrentReasoner());

        TestResult preconditionResult = axiomTester.testPreconditions();
        if (preconditionResult != null) {
            for (int row = 0; row < table.getRowCount(); ++row) {
                table.setValueAt(preconditionResult.humanize(), row, 1);
            }
        } else {
            for (int row = 0; row < table.getRowCount(); ++row) {
                try {
                    TestResult result = axiomTester.test(parser.parse((String) table.getValueAt(row, 0)));
                    table.setValueAt(result.humanize(), row, 1);
                } catch (OWLParserException e) {
                    table.setValueAt("Syntax error or missing entity", row, 1);
                }
            }
        }
    }

    private void evaluateSelectedTests() {
        OWLReasonerManager reasonerManager = getOWLModelManager().getOWLReasonerManager();
        if (reasonerManager.getReasonerStatus() != ReasonerStatus.INITIALIZED) {
            ReasonerUtilities.warnUserIfReasonerIsNotConfigured(getOWLWorkspace(), reasonerManager);
            return;
        }

        int[] selectedRows = table.getSelectedRows();

        AxiomTester axiomTester = new AxiomTester(reasonerManager.getCurrentReasoner());

        TestResult preconditionResult = axiomTester.testPreconditions();
        if (preconditionResult != null) {
            for (int row : selectedRows) {
                table.setValueAt(preconditionResult.humanize(), row, 1);
            }
        } else {
            for (int row : selectedRows) {
                System.out.println(row);
                try {
                    TestResult result = axiomTester.test(parser.parse((String) table.getValueAt(row, 0)));
                    table.setValueAt(result.humanize(), row, 1);
                } catch (OWLParserException e) {
                    table.setValueAt("Syntax error or missing entity", row, 1);
                }
            }
        }
    }

    private void removeSelectedTests() {
        int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            tableModel.removeRow(row);
        }
        tableModel.fireTableDataChanged();
    }

    private void addSelectedTestsToOntology() {

    }
}
