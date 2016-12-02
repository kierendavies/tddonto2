package za.ac.uct.cs.tddonto2;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.swing.*;
import java.awt.*;

public class TestsViewComponent extends AbstractOWLViewComponent {
    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("This is TDDOnto2");
        add(label, BorderLayout.CENTER);

        System.out.println("Trying a test");
        OWLReasoner reasoner = getOWLModelManager().getReasoner();
        OWLDataFactory dataFactory = getOWLDataFactory();
        AxiomTester axiomTester = new AxiomTester(reasoner);
        TestResult result = axiomTester.test(
                dataFactory.getOWLSubClassOfAxiom(
                        dataFactory.getOWLClass(IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#", "Pizza")),
                        dataFactory.getOWLClass(IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#", "Food"))
                )
        );
        System.out.println("Success: " + result);
    }

    @Override
    protected void disposeOWLView() {
    }
}