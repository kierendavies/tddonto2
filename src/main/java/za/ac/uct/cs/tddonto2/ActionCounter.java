package za.ac.uct.cs.tddonto2;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

class Action {
    private static final double CLICK_TIME = 1.0;
    private static final double KEYSTROKE_TIME = 0.3;

    public final double clicks;
    public final double keystrokes;

    public Action(double clicks, double keystrokes) {
        this.clicks = clicks;
        this.keystrokes = keystrokes;
    }

    public double timeTaken() {
        return clicks * CLICK_TIME + keystrokes * KEYSTROKE_TIME;
    }

    @Override
    public String toString() {
        return "[" + clicks + " c, " + keystrokes + " k]";
    }
}

public class ActionCounter {
    private static final int REASONING_TRIALS = 10;
    private static final OWLObjectRenderer renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

    private final OWLOntology ontology;
    private final OWLDataFactory dataFactory;
    private final String iriPrefix;
    private final OWLReasoner reasoner;

    private final double reasoningTime;
    private final double averageClassNameLength;
    private final double averageObjectPropertyNameLength;
    private final double averageIndividualNameLength;
    private final double averageClassHierarchyDepth;
    private final double averageObjectPropertyHierarchyDepth;

    public ActionCounter(OWLOntology ontology, String iriPrefix, OWLReasonerFactory reasonerFactory) {
        this.ontology = ontology;
        this.iriPrefix = iriPrefix;
        dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

        double reasoningTime = 0;
        for (int i = 0; i < REASONING_TRIALS; i++) {
            double startTime = System.nanoTime();
            reasonerFactory.createNonBufferingReasoner(ontology);
            double endTime = System.nanoTime();
            reasoningTime += endTime - startTime;
        }
        reasoningTime /= REASONING_TRIALS;
        reasoningTime /= 1e9;  // ns to s
        this.reasoningTime = reasoningTime;

        averageClassNameLength = ontology.getClassesInSignature()
                .stream().mapToInt(this::nameLength).average().orElse(0);
        averageObjectPropertyNameLength = ontology.getObjectPropertiesInSignature()
                .stream().mapToInt(this::nameLength).average().orElse(0);
        averageIndividualNameLength = ontology.getIndividualsInSignature()
                .stream().mapToInt(this::nameLength).average().orElse(0);
        averageClassHierarchyDepth = ontology.getClassesInSignature()
                .stream().mapToInt(this::hierarchyDepth).average().orElse(0);
        averageObjectPropertyHierarchyDepth = ontology.getObjectPropertiesInSignature()
                .stream().mapToInt(this::hierarchyDepth).average().orElse(0);
        // TODO sloooooow.  do bfs or something
    }

    private int nameLength(OWLEntity c) {
        return c.getIRI().getShortForm().length();
    }

    private int hierarchyDepth(OWLClass c) {
        if (c.isTopEntity()) {
            return 0;
        } else {
            int minDepth = Integer.MAX_VALUE;
            for (Node<OWLClass> node : reasoner.getSuperClasses(c, true)) {
                int depth = 1 + hierarchyDepth(node.getRepresentativeElement());
                if (depth < minDepth) minDepth = depth;
            }
            return minDepth;
        }
    }

    private int hierarchyDepth(OWLObjectPropertyExpression c) {
        if (c.isTopEntity()) {
            return 0;
        } else {
            int minDepth = Integer.MAX_VALUE;
            for (Node<OWLObjectPropertyExpression> node : reasoner.getSuperObjectProperties(c, true)) {
                int depth = 1 + hierarchyDepth(node.getRepresentativeElement());
                if (depth < minDepth) minDepth = depth;
            }
            return minDepth;
        }
    }

    private static Action tddActionForAxiom(OWLAxiom a) {
        return new Action(1, renderer.render(a).length());
    }

    // i

    private List<Action> basicActionsForSubClassOf(String c, String d) {
        return basicActionsForSubClassOf(
                dataFactory.getOWLClass(IRI.create(iriPrefix, c)),
                dataFactory.getOWLClass(IRI.create(iriPrefix, d))
        );
    }

    private List<Action> basicActionsForSubClassOf(OWLClass c, OWLClass d) {
        return basicActionsForSubClassOf(nameLength(d), hierarchyDepth(d));
    }

    private static List<Action> basicActionsForSubClassOf(double dLength, double dDepth) {
        return Arrays.asList(
                new Action(4, dLength),
                new Action(2, 0),
                new Action(4, dLength),
                new Action(6 + dDepth, 0)
        );
    }

    private List<Action> basicActionsForSubClassOf() {
        return basicActionsForSubClassOf(averageClassNameLength, averageClassHierarchyDepth);
    }

    private Action tddActionForSubClassOf() {
        return new Action(1, 11 + 2*averageClassNameLength);
    }

    // ii

    private List<Action> basicActionsForSubClassOfObjectSomeOrAllValuesFrom(OWLClass c, OWLObjectProperty r, OWLClass d) {
        return basicActionsForSubClassOfObjectSomeOrAllValuesFrom(nameLength(r), nameLength(d), hierarchyDepth(r), hierarchyDepth(d));
    }

    private static List<Action> basicActionsForSubClassOfObjectSomeOrAllValuesFrom(double rLength, double dLength, double rDepth, double dDepth) {
        return Arrays.asList(
                new Action(4, rLength + 4 + dLength),
                new Action(8 + rDepth + dDepth, 0)
        );
    }

    private List<Action> basicActionsForSubClassOfObjectSomeOrAllValuesFrom() {
        return basicActionsForSubClassOfObjectSomeOrAllValuesFrom(
                averageObjectPropertyNameLength,
                averageClassNameLength,
                averageObjectPropertyHierarchyDepth,
                averageClassHierarchyDepth
        );
    }

    private Action tddActionForSubClassOfObjectSomeOrAllValuesFrom() {
        return new Action(1, 15 + 2*averageClassNameLength + averageObjectPropertyNameLength);
    }

    // iii

    private List<Action> basicActionsForDisjointClasses(OWLClass c, OWLClass d) {
        return basicActionsForDisjointClasses(nameLength(d), hierarchyDepth(d));
    }

    private static List<Action> basicActionsForDisjointClasses(double dLength, double dDepth) {
        return Arrays.asList(
                new Action(4, dLength),
                new Action(5 + dDepth, 0)
        );
    }

    private List<Action> basicActionsForDisjointClasses() {
        return basicActionsForDisjointClasses(averageClassNameLength, averageClassHierarchyDepth);
    }

    private Action tddActionForDisjointClasses() {
        return new Action(1, 14 + 2*averageClassNameLength);
    }

    // iv, v

    private List<Action> basicActionsForObjectPropertyDomainOrRange(OWLObjectProperty r, OWLClass c) {
        return basicActionsForObjectPropertyDomainOrRange(nameLength(c), hierarchyDepth(c));
    }

    private static List<Action> basicActionsForObjectPropertyDomainOrRange(double cLength, double cDepth) {
        return Arrays.asList(
                new Action(4, cLength),
                new Action(5 + cDepth, 0)
        );
    }

    private List<Action> basicActionsForObjectPropertyDomainOrRange() {
        return basicActionsForObjectPropertyDomainOrRange(averageClassNameLength, averageClassHierarchyDepth);
    }

    private Action tddActionForObjectPropertyDomain() {
        return new Action(1, 15 + averageClassNameLength + averageObjectPropertyNameLength);
    }

    private Action tddActionForObjectPropertyRange() {
        return new Action(1, 24 + averageClassNameLength + averageObjectPropertyNameLength);
    }

    // vi

    private List<Action> basicActionsForClassAssertion(OWLNamedIndividual i, OWLClass c) {
        return basicActionsForClassAssertion(nameLength(c), hierarchyDepth(c));
    }

    private static List<Action> basicActionsForClassAssertion(double cLength, double cDepth) {
        return Arrays.asList(
                new Action(4, cLength),
                new Action(5 + cDepth, 0),
                new Action(5, 0)
        );
    }

    private List<Action> basicActionsForClassAssertion() {
        return basicActionsForClassAssertion(averageClassNameLength, averageClassHierarchyDepth);
    }

    private Action tddActionForClassAssertion() {
        return new Action(1, 5 + averageIndividualNameLength + averageClassNameLength);
    }

    // vii

    private List<Action> basicActionsForSubClassOfObjectCardinalityRestriction(OWLClass c, OWLObjectProperty r, OWLClass d) {
        return basicActionsForSubClassOfObjectCardinalityRestriction(nameLength(r), nameLength(d), hierarchyDepth(r), hierarchyDepth(d));
    }

    private static List<Action> basicActionsForSubClassOfObjectCardinalityRestriction(double rLength, double dLength, double rDepth, double dDepth) {
        return Arrays.asList(
                new Action(4, rLength + 5 + dLength),
                new Action(8 + rDepth + dDepth, 1)
        );
    }

    private List<Action> basicActionsForSubClassOfObjectCardinalityRestriction() {
        return basicActionsForSubClassOfObjectCardinalityRestriction(
                averageObjectPropertyNameLength,
                averageClassNameLength,
                averageObjectPropertyHierarchyDepth,
                averageClassHierarchyDepth
        );
    }

    private Action tddActionForSubClassOfObjectCardinalityRestriction() {
        return new Action(1, 15 + 2*averageClassNameLength + averageObjectPropertyNameLength);
    }

    // viii

    private List<Action> basicActionsForTypedAxiom(OWLAxiom a) {
        return basicActionsForTypedAxiom(renderer.render(a).length());
    }

    private static List<Action> basicActionsForTypedAxiom(double aLength) {
        return Arrays.asList(
                new Action(4, aLength)
        );
    }

    private Action tddActionForTypedAxiom() {
        return null;
    }

    // ix

    private List<Action> basicActionsForSubClassOfObjectSomeAndAllValuesFrom(OWLClass c, OWLObjectProperty r, OWLClass d) {
        return basicActionsForSubClassOfObjectSomeAndAllValuesFrom(nameLength(r), nameLength(d), hierarchyDepth(r), hierarchyDepth(d));
    }

    private static List<Action> basicActionsForSubClassOfObjectSomeAndAllValuesFrom(double rLength, double dLength, double rDepth, double dDepth) {
        return Arrays.asList(
                new Action(7, 2*rLength + 8 + 2*dLength),
                new Action(15 + 2*rDepth + 2*dDepth, 0),
                new Action(4, 17 + 2*rLength + 2*dLength)
        );
    }

    private List<Action> basicActionsForSubClassOfObjectSomeAndAllValuesFrom() {
        return basicActionsForSubClassOfObjectSomeAndAllValuesFrom(
                averageObjectPropertyNameLength,
                averageClassNameLength,
                averageObjectPropertyHierarchyDepth,
                averageClassHierarchyDepth
        );
    }

    private Action tddActionForSubClassOfObjectSomeAndAllValuesFrom() {
        return new Action(1, 22 + 3*averageClassNameLength + 2*averageObjectPropertyNameLength);
    }

    // x

    private List<Action> basicActionsForComplicatedThing() {
        return Arrays.asList(
                new Action(6, 12 + 3*averageClassNameLength + 2*averageObjectPropertyNameLength),
                new Action(
                        10 + averageClassHierarchyDepth + averageObjectPropertyHierarchyDepth,
                        8 + 2*averageClassNameLength + averageObjectPropertyNameLength
                ),
                new Action(3, 15 + 3*averageClassNameLength + 2*averageObjectPropertyNameLength)
        );
    }

    private Action tddActionForComplicatedThing() {
        return new Action(1, 26 + 4*averageClassNameLength + 2*averageObjectPropertyNameLength);
    }

    private void printCalcSet(List<Action> basicActions, Action tddAction) {
        System.out.printf(
                "\tBasic %6.2f s\n",
                basicActions.stream().mapToDouble(Action::timeTaken).min().orElse(Double.NaN) + reasoningTime
        );
        System.out.printf(
                "\tTDD   %6.2f s\n",
                tddAction.timeTaken()
        );
    }

    public void printCalculations() {
        System.out.printf("Reasoning time %.2f s\n", reasoningTime);
        System.out.printf("Class name %.2f\n", averageClassNameLength);
        System.out.printf("Class depth %.2f\n", averageClassHierarchyDepth);
        System.out.printf("Object property name %.2f\n", averageObjectPropertyNameLength);
        System.out.printf("Object property depth %.2f\n", averageObjectPropertyHierarchyDepth);
        System.out.printf("Individual name %.2f\n", averageIndividualNameLength);
        System.out.println();

        System.out.println("(i) SubClassOf");
        printCalcSet(
                basicActionsForSubClassOf(),
                tddActionForSubClassOf()
        );

        System.out.println("(ii) SubClassOfObjectSomeOrAllValuesFrom");
        printCalcSet(
                basicActionsForSubClassOfObjectSomeOrAllValuesFrom(),
                tddActionForSubClassOfObjectSomeOrAllValuesFrom()
        );

        System.out.println("(iii) DisjointClasses");
        printCalcSet(
                basicActionsForDisjointClasses(),
                tddActionForDisjointClasses()
        );

        System.out.println("(iv) ObjectPropertyDomain");
        printCalcSet(
                basicActionsForObjectPropertyDomainOrRange(),
                tddActionForObjectPropertyDomain()
        );

        System.out.println("(v) ObjectPropertyRange");
        printCalcSet(
                basicActionsForObjectPropertyDomainOrRange(),
                tddActionForObjectPropertyRange()
        );

        System.out.println("(vi) ClassAssertion");
        printCalcSet(
                basicActionsForClassAssertion(),
                tddActionForClassAssertion()
        );

        System.out.println("(vii) ObjectPropertyRange");
        printCalcSet(
                basicActionsForObjectPropertyDomainOrRange(),
                tddActionForObjectPropertyRange()
        );

        System.out.println("(viii) TypedAxiom");
        System.out.println("\tskipping");

        System.out.println("(ix) SubClassOfObjectSomeAndAllValuesFrom");
        printCalcSet(
                basicActionsForSubClassOfObjectSomeAndAllValuesFrom(),
                tddActionForSubClassOfObjectSomeAndAllValuesFrom()
        );

        System.out.println("(x) ComplicatedThing");
        printCalcSet(
                basicActionsForComplicatedThing(),
                tddActionForComplicatedThing()
        );
    }

    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLReasonerFactory hermitReasonerFactory = new org.semanticweb.HermiT.ReasonerFactory();

        // TODO use actual test ontologies

        String pizzaPath = "src/test/resources/pizza.owl";
        String pizzaPrefix = "http://www.co-ode.org/ontologies/pizza/pizza.owl#";
        OWLOntologyManager pizzaManager = OWLManager.createOWLOntologyManager();
        OWLOntology pizzaOntology = pizzaManager.loadOntologyFromOntologyDocument(new File(pizzaPath));
        ActionCounter pizzaAc = new ActionCounter(pizzaOntology, pizzaPrefix, hermitReasonerFactory);
        pizzaAc.printCalculations();
    }
}
