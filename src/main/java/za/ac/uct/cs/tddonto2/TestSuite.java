package za.ac.uct.cs.tddonto2;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;

import java.util.ArrayList;
import java.util.List;

public class TestSuite {
    private List<OWLClassAxiom> axioms;

    public TestSuite() {
        axioms = new ArrayList<>();
    }

    public List<OWLClassAxiom> getAxioms() {
        return axioms;
    }

    public void add(OWLClassAxiom axiom) {
        axioms.add(axiom);
    }

    public void remove(OWLClassAxiom axiom) {
        axioms.remove(axiom);
    }

    // TODO: nice reporting
    public TestResult evaluate(AxiomTester tester) {
        TestResult preconditionsResult = tester.testPreconditions();
        if (preconditionsResult != null) {
            return preconditionsResult;
        }

        TestResult worstResult = TestResult.ENTAILED;
        for (OWLAxiom axiom : axioms) {
            TestResult result = tester.test(axiom);
            worstResult = TestResult.max(worstResult, result);
        }
        return worstResult;
    }
}
