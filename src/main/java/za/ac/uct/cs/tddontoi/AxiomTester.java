package za.ac.uct.cs.tddontoi;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.*;

import static za.ac.uct.cs.tddontoi.TestResult.*;

public class AxiomTester {
    private final OWLReasoner reasoner;
    private final OWLDataFactory dataFactory;

    public AxiomTester(OWLReasoner reasoner) {
        this.reasoner = reasoner;
        dataFactory = reasoner.getRootOntology().getOWLOntologyManager().getOWLDataFactory();
    }

    private boolean isSatisfiable(OWLClassExpression c) {
        return reasoner.isSatisfiable(c);
    }

    private boolean hasNamedSubClasses(OWLClassExpression c) {
        return reasoner.getSubClasses(c, false).getFlattened().size() > 1  // always contains owl:Nothing
            || reasoner.getEquivalentClasses(c).getEntitiesMinusBottom().size() > 0;
    }

    private boolean hasInstances(OWLClassExpression c) {
        return !reasoner.getInstances(c, false).isEmpty();
    }

    // TBox

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
                worstResult = TestResult.max(worstResult, result);
            }
        }
        return worstResult;
    }

    public TestResult testEquivalentClasses(OWLClassExpression... cs) {
        return testEquivalentClasses(Arrays.asList(cs));
    }

    public TestResult testDisjointClasses(Collection<OWLClassExpression> cs) {
        List<OWLClassExpression> csAsList;
        if (cs instanceof List) {
            csAsList = (List<OWLClassExpression>) cs;
        } else {
            csAsList = new ArrayList<OWLClassExpression>();
            csAsList.addAll(cs);
        }

        TestResult worstResult = ENTAILED;
        for (int i = 0; i < csAsList.size() - 1; i++) {
            for (int j = i + 1; j < csAsList.size(); j++) {
                TestResult result = testSubClassOf(csAsList.get(i), dataFactory.getOWLObjectComplementOf(csAsList.get(j)));
                worstResult = TestResult.max(worstResult, result);
            }
        }
        return worstResult;
    }

    public TestResult testDisjointClasses(OWLClassExpression... cs) {
        return testDisjointClasses(Arrays.asList(cs));
    }

    public TestResult testDisjointUnion(OWLClass n, OWLClassExpression... cs) {
        TestResult equivResult = testEquivalentClasses(n, dataFactory.getOWLObjectUnionOf(cs));
        TestResult disjResult = testDisjointClasses(cs);
        return TestResult.max(equivResult, disjResult);
    }

    // ABox

    public TestResult testSameIndividual(Collection<OWLNamedIndividual> as) {
        OWLNamedIndividual a1 = as.iterator().next();
        if (reasoner.getSameIndividuals(a1).getEntities().containsAll(as)) {
            return ENTAILED;
        }
        for (OWLNamedIndividual a : as) {
            Set<OWLNamedIndividual> differentIndividuals = reasoner.getDifferentIndividuals(a).getFlattened();
            differentIndividuals.retainAll(as);
            if (!differentIndividuals.isEmpty()) {
                return INCONSISTENT;
            }
        }
        return ABSENT;
    }

    public TestResult testSameIndividual(OWLNamedIndividual... as) {
        return testSameIndividual(Arrays.asList(as));
    }

    public TestResult testDifferentIndividuals(Collection<OWLNamedIndividual> as) {
        List<OWLNamedIndividual> asAsList;
        if (as instanceof List) {
            asAsList = (List<OWLNamedIndividual>) as;
        } else {
            asAsList = new ArrayList<OWLNamedIndividual>();
            asAsList.addAll(as);
        }

        for (int i = 0; i < asAsList.size(); i++) {
            Node<OWLNamedIndividual> sameIndividuals = reasoner.getSameIndividuals(asAsList.get(i));
            for (int j = 0; j < asAsList.size(); j++) {
                if (i != j && sameIndividuals.contains(asAsList.get(j))) {
                    return INCONSISTENT;
                }
            }
        }
        for (int i = 0; i < asAsList.size(); i++) {
            NodeSet<OWLNamedIndividual> differentIndividuals = reasoner.getDifferentIndividuals(asAsList.get(i));
            for (int j = 0; j < asAsList.size(); j++) {
                if (i != j && !differentIndividuals.containsEntity(asAsList.get(j))) {
                    return ABSENT;
                }
            }
        }
        return ENTAILED;
    }

    public TestResult testDifferentIndividuals(OWLNamedIndividual... as) {
        return testDifferentIndividuals(Arrays.asList(as));
    }
}
