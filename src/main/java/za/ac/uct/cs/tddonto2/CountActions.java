package za.ac.uct.cs.tddonto2;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Actions {
    public int clicks;
    public int keystrokes;

    public Actions(int clicks, int keystrokes) {
        this.clicks = clicks;
        this.keystrokes = keystrokes;
    }

    @Override
    public String toString() {
        return "[" + clicks + " c, " + keystrokes + " k]";
    }
}

public class CountActions {
    private static int nameLength(OWLEntity c) {
        return c.getIRI().getShortForm().length();
    }

    private static int hierarchyDepth(OWLClass c, OWLReasoner reasoner) {
        if (c.isTopEntity()) {
            return 0;
        } else {
            int minDepth = Integer.MAX_VALUE;
            for (Node<OWLClass> node : reasoner.getSuperClasses(c, true)) {
                int depth = 1 + hierarchyDepth(node.getRepresentativeElement(), reasoner);
                if (depth < minDepth) minDepth = depth;
            }
            return minDepth;
        }
    }

    private static int hierarchyDepth(OWLObjectPropertyExpression c, OWLReasoner reasoner) {
        if (c.isTopEntity()) {
            return 0;
        } else {
            int minDepth = Integer.MAX_VALUE;
            for (Node<OWLObjectPropertyExpression> node : reasoner.getSuperObjectProperties(c, true)) {
                int depth = 1 + hierarchyDepth(node.getRepresentativeElement(), reasoner);
                if (depth < minDepth) minDepth = depth;
            }
            return minDepth;
        }
    }

    // i
    private static List<Actions> basicActionsForSubClassOf(OWLClass c, OWLClass d, OWLReasoner reasoner) {
        return basicActionsForSubClassOf(nameLength(d), hierarchyDepth(d, reasoner));
    }

    private static List<Actions> basicActionsForSubClassOf(int dLength, int dDepth) {
        return Arrays.asList(
                new Actions(4, dLength),
                new Actions(2, 0),
                new Actions(4, dLength),
                new Actions(6 + dDepth, 0)
        );
    }

    // ii
    private static List<Actions> basicActionsForSubClassOfObjectSomeOrAllValuesFrom(OWLClass c, OWLObjectProperty r, OWLClass d, OWLReasoner reasoner) {
        return basicActionsForSubClassOfObjectSomeOrAllValuesFrom(nameLength(r), nameLength(d), hierarchyDepth(r, reasoner), hierarchyDepth(d, reasoner));
    }

    private static List<Actions> basicActionsForSubClassOfObjectSomeOrAllValuesFrom(int rLength, int dLength, int rDepth, int dDepth) {
        return Arrays.asList(
                new Actions(4, rLength + 4 + dLength),
                new Actions(8 + rDepth + dDepth, 0)
        );
    }

    // iii
    private static List<Actions> basicActionsForDisjointClasses(OWLClass c, OWLClass d, OWLReasoner reasoner) {
        return basicActionsForDisjointClasses(nameLength(d), hierarchyDepth(d, reasoner));
    }

    private static List<Actions> basicActionsForDisjointClasses(int dLength, int dDepth) {
        return Arrays.asList(
                new Actions(4, dLength),
                new Actions(5 + dDepth, 0)
        );
    }

    // iv, v
    private static List<Actions> basicActionsForObjectPropertyDomainOrRange(OWLObjectProperty r, OWLClass c, OWLReasoner reasoner) {
        return basicActionsForObjectPropertyDomainOrRange(nameLength(c), hierarchyDepth(c, reasoner));
    }

    private static List<Actions> basicActionsForObjectPropertyDomainOrRange(int cLength, int cDepth) {
        return Arrays.asList(
                new Actions(4, cLength),
                new Actions(5 + cDepth, 0)
        );
    }

    // vi
    private static List<Actions> basicActionsForClassAssertion(OWLNamedIndividual i, OWLClass c, OWLReasoner reasoner) {
        return basicActionsForClassAssertion(nameLength(c), hierarchyDepth(c, reasoner));
    }

    private static List<Actions> basicActionsForClassAssertion(int cLength, int cDepth) {
        return Arrays.asList(
                new Actions(4, cLength),
                new Actions(5 + cDepth, 0),
                new Actions(5, 0)
        );
    }

    // vii
    private static List<Actions> basicActionsForSubClassOfObjectCardinalityRestriction(OWLClass c, OWLObjectProperty r, OWLClass d, OWLReasoner reasoner) {
        return basicActionsForSubClassOfObjectCardinalityRestriction(nameLength(r), nameLength(d), hierarchyDepth(r, reasoner), hierarchyDepth(d, reasoner));
    }
    private static List<Actions> basicActionsForSubClassOfObjectCardinalityRestriction(int rLength, int dLength, int rDepth, int dDepth) {
        return Arrays.asList(
                new Actions(4, rLength + 5 + dLength),
                new Actions(8 + rDepth + dDepth, 1)
        );
    }

    // viii
    private static List<Actions> basicActionsForTypedAxiom(OWLAxiom a) {
        OWLObjectRenderer renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        return basicActionsForTypedAxiom(renderer.render(a).length());
    }

    private static List<Actions> basicActionsForTypedAxiom(int aLength) {
        return Arrays.asList(
                new Actions(4, aLength)
        );
    }

    // ix
    private static List<Actions> basicActionsForSubClassOfObjectSomeAndAllValuesFrom(OWLClass c, OWLObjectProperty r, OWLClass d, OWLReasoner reasoner) {
        return basicActionsForSubClassOfObjectSomeAndAllValuesFrom(nameLength(r), nameLength(d), hierarchyDepth(r, reasoner), hierarchyDepth(d, reasoner));
    }

    private static List<Actions> basicActionsForSubClassOfObjectSomeAndAllValuesFrom(int rLength, int dLength, int rDepth, int dDepth) {
        return Arrays.asList(
                new Actions(7, 2*rLength + 8 + 2*dLength),
                new Actions(15 + 2*rDepth + 2*dDepth, 0),
                new Actions(4, 17 + 2*rLength + 2*dLength)
        );
    }

    // x
    private static List<Actions> basicActionsForSubClassOfTypedExpression(OWLClass c, OWLClassExpression d) {
        return null;  // TODO
    }

    public static void main(String[] args) throws OWLOntologyCreationException {
        // TODO use actual test ontologies

        String pizzaPath = "src/test/resources/pizza.owl";
        String pizzaPrefix = "http://www.co-ode.org/ontologies/pizza/pizza.owl#";

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(pizzaPath));
        OWLReasoner reasoner = new org.semanticweb.HermiT.ReasonerFactory().createNonBufferingReasoner(ontology);

        OWLClass c = dataFactory.getOWLClass(IRI.create(pizzaPrefix, "Margherita"));
        OWLClass d = dataFactory.getOWLClass(IRI.create(pizzaPrefix, "CheeseTopping"));
        OWLObjectProperty r = dataFactory.getOWLObjectProperty(IRI.create(pizzaPrefix, "hasTopping"));
    }
}
