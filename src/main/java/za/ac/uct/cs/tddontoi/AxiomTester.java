package za.ac.uct.cs.tddontoi;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Arrays;
import java.util.Collection;

import static za.ac.uct.cs.tddontoi.TestResult.*;

public class AxiomTester {
    private final OWLReasoner reasoner;
    private final OWLDataFactory dataFactory;
    private final OWLClass nothing;

    public AxiomTester(OWLReasoner reasoner) {
        this.reasoner = reasoner;
        dataFactory = reasoner.getRootOntology().getOWLOntologyManager().getOWLDataFactory();
        nothing = dataFactory.getOWLNothing();
    }

    private boolean isSatisfiable(OWLClassExpression c) {
        return reasoner.isSatisfiable(c);
    }

    private boolean hasNamedSubClasses(OWLClassExpression c) {
        return reasoner.getSubClasses(c, false).getFlattened().size() > 1  // always contains owl:Nothing
            || reasoner.getEquivalentClasses(c).getEntitiesMinus(nothing).size() > 0;
    }

    private boolean hasInstances(OWLClassExpression c) {
        return !reasoner.getInstances(c, false).isEmpty();
    }

    public TestResult testSubClassOf(OWLClassExpression c, OWLClassExpression d) {
        OWLClassExpression cAndNotD = dataFactory.getOWLObjectIntersectionOf(c, dataFactory.getOWLObjectComplementOf(d));
        if (hasInstances(cAndNotD)) {
            return INCONSISTENT;
        } else if (hasNamedSubClasses(cAndNotD)) {
            return INCOHERENT;
        } else if (isSatisfiable(cAndNotD)) {
            return ABSENT;
        } else {
            return ENTAILED;
        }
    }

    public TestResult testEquivalentClasses(Collection<OWLClassExpression> cs) {
        TestResult worstResult = ENTAILED;
        for (OWLClassExpression c : cs) {
            for (OWLClassExpression d : cs) {
                if (c == d) continue;
                TestResult result = testSubClassOf(c, d);
                if (result.compareTo(worstResult) > 0) {
                    worstResult = result;
                }
            }
        }
        return worstResult;
    }

    public TestResult testEquivalentClasses(OWLClassExpression... cs) {
        return testEquivalentClasses(Arrays.asList(cs));
    }
}
