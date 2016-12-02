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

    @SuppressWarnings("unchecked")
    public TestResult test(OWLAxiom axiom, boolean testPreconditions) throws UnsupportedOperationException {
        if (testPreconditions) {
            if (!reasoner.isConsistent()) {
                return PRE_INCONSISTENT;
            } else if (reasoner.getUnsatisfiableClasses().getSize() > 1) {
                return PRE_INCOHERENT;
            }
        }

        for (OWLEntity e : axiom.getSignature()) {
            if (!reasoner.getRootOntology().containsEntityInSignature(e, true)) {
                return MISSING_ENTITY;
            }
        }

        if (axiom instanceof OWLSubClassOfAxiom) {
            OWLSubClassOfAxiom scoAxiom = (OWLSubClassOfAxiom) axiom;
            return testSubClassOf(scoAxiom.getSubClass(), scoAxiom.getSuperClass());
        } else if (axiom instanceof OWLEquivalentClassesAxiom) {
            return testEquivalentClasses(((OWLEquivalentClassesAxiom) axiom).getClassExpressions());
        } else if (axiom instanceof OWLDisjointClassesAxiom) {
            return testDisjointClasses(((OWLDisjointClassesAxiom) axiom).getClassExpressionsAsList());
        } else if (axiom instanceof OWLDisjointUnionAxiom) {
            OWLDisjointUnionAxiom duAxiom = (OWLDisjointUnionAxiom) axiom;
            return testDisjointUnion(duAxiom.getOWLClass(), duAxiom.getClassExpressions());
        } else if (axiom instanceof OWLSameIndividualAxiom) {
            // OWLSameIndividualAxiom is not allowed to contain anonymous
            // individuals, but the type signature of getIndividuals is not
            // specific, so we need to do this horrible casting.
            return testSameIndividual((Set<OWLNamedIndividual>) (Set<?>) ((OWLSameIndividualAxiom) axiom).getIndividuals());
        } else if (axiom instanceof OWLDifferentIndividualsAxiom) {
            // As above
            return testDifferentIndividuals((Set<OWLNamedIndividual>) (Set<?>) ((OWLDifferentIndividualsAxiom) axiom).getIndividuals());
        } else if (axiom instanceof OWLClassAssertionAxiom) {
            OWLClassAssertionAxiom caAxiom = (OWLClassAssertionAxiom) axiom;
            if (!(caAxiom.getIndividual() instanceof OWLNamedIndividual)) {
                // Can't test anonymous individuals. Not sure if they're even allowed.
                throw new UnsupportedOperationException();
            }
            return testClassAssertion(caAxiom.getClassExpression(), (OWLNamedIndividual) caAxiom.getIndividual());
        }

        throw new UnsupportedOperationException();
    }

    public TestResult test(OWLAxiom axiom) {
        return test(axiom, true);
    }

    // TBox

    public TestResult testSubClassOf(OWLClassExpression c, OWLClassExpression d) {
        OWLClassExpression cAndNotD = dataFactory.getOWLObjectIntersectionOf(c, dataFactory.getOWLObjectComplementOf(d));
        if (!reasoner.getInstances(cAndNotD, false).isEmpty()) {
            return INCONSISTENT;
        } else if (reasoner.getSubClasses(cAndNotD, false).getFlattened().size() > 1  // always contains owl:Nothing
                || !reasoner.getEquivalentClasses(cAndNotD).getEntitiesMinusBottom().isEmpty()) {
            return INCOHERENT;
        } else if (reasoner.isSatisfiable(cAndNotD)) {
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
            csAsList = new ArrayList<>(cs.size());
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

    public TestResult testDisjointUnion(OWLClass n, Collection<OWLClassExpression> cs) {
        Set<OWLClassExpression> csAsSet;
        if (cs instanceof Set) {
            csAsSet = (Set<OWLClassExpression>) cs;
        } else {
            csAsSet = new HashSet<OWLClassExpression>(cs.size());
            csAsSet.addAll(cs);
        }

        TestResult equivResult = testEquivalentClasses(n, dataFactory.getOWLObjectUnionOf(cs));
        TestResult disjResult = testDisjointClasses(cs);
        return TestResult.max(equivResult, disjResult);
    }

    public TestResult testDisjointUnion(OWLClass n, OWLClassExpression... cs) {
        return testDisjointUnion(n, Arrays.asList(cs));
    }

    // ABox

    public TestResult testSameIndividual(Collection<OWLNamedIndividual> as) {
        OWLNamedIndividual a1 = as.iterator().next();
        if (reasoner.getSameIndividuals(a1).getEntities().containsAll(as)) {
            return ENTAILED;
        }
        for (OWLNamedIndividual a : as) {
            NodeSet<OWLNamedIndividual> differentIndividuals = reasoner.getDifferentIndividuals(a);
            for (OWLNamedIndividual b : as) {
                if (differentIndividuals.containsEntity(b)) {
                    return INCONSISTENT;
                }
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
            asAsList = new ArrayList<>(as.size());
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

    public TestResult testClassAssertion(OWLClassExpression c, OWLNamedIndividual a) {
        if (reasoner.getInstances(c, false).containsEntity(a)) {
            return ENTAILED;
        } else if (reasoner.getInstances(dataFactory.getOWLObjectComplementOf(c), false).containsEntity(a)) {
            return INCONSISTENT;
        } else {
            return ABSENT;
        }
    }
}
