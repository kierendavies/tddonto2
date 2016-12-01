package za.ac.uct.cs.tddontoi;

import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class AxiomTester {
    private OWLReasoner reasoner;

    public AxiomTester(OWLReasoner reasoner) {
        this.reasoner = reasoner;
    }

    public TestResult testSubClassOf(OWLSubClassOfAxiom axiom) {
        return null;
    }
}
