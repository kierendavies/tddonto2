package za.ac.uct.cs.tddonto2;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.OWLExpressionParser;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxInlineAxiomParser;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import javax.swing.*;
import java.awt.*;

public class TestSuiteViewComponent extends AbstractOWLViewComponent {

    OWLExpressionParser<OWLAxiom> parser;
    private JTextArea textArea;
    private JButton evalButton;
    private JLabel axiomLabel;
    private JLabel resultLabel;

    @Override
    protected void initialiseOWLView() throws Exception {
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(
                new BidirectionalShortFormProviderAdapter(
                        getOWLModelManager().getActiveOntology().getOWLOntologyManager(),
                        getOWLModelManager().getOntologies(),
                        new SimpleShortFormProvider()
                )
        );
        parser = new ManchesterOWLSyntaxInlineAxiomParser(getOWLDataFactory(), entityChecker);

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        textArea = new JTextArea(1, 40);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        add(textArea, constraints);

        evalButton = new JButton("Evaluate");
        evalButton.addActionListener(e -> evaluateTest());
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.NONE;
        add(evalButton, constraints);

        axiomLabel = new JLabel("No axiom");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(axiomLabel, constraints);

        resultLabel = new JLabel("No result");
        constraints.gridy = 2;
        add(resultLabel, constraints);
    }

    @Override
    protected void disposeOWLView() {
    }

    private void evaluateTest() {
        axiomLabel.setText("");
        resultLabel.setText("");

        try {
            OWLAxiom axiom = parser.parse(textArea.getText());
            axiomLabel.setText(axiom.toString());

            AxiomTester axiomTester = new AxiomTester(getOWLModelManager().getOWLReasonerManager().getCurrentReasoner());
            TestResult result = axiomTester.test(axiom);
            resultLabel.setText(result.toString());
        } catch (Exception ex) {
            resultLabel.setText(ex.getMessage());
        }
    }
}
