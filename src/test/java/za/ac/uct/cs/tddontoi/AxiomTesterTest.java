package za.ac.uct.cs.tddontoi;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AxiomTesterTest {
    private static final String pizzaPath = "src/test/resources/pizza.owl";
    private static final String pizzaPrefix = "http://www.co-ode.org/ontologies/pizza/pizza.owl#";

    private OWLDataFactory dataFactory;
    private AxiomTester axiomTester;

    private OWLClass parseClass(String name) {
        return dataFactory.getOWLClass(IRI.create(pizzaPrefix, name));
    }

    private OWLNamedIndividual parseIndiv(String name) {
        return dataFactory.getOWLNamedIndividual(IRI.create(pizzaPrefix, name));
    }

    private OWLObjectProperty parseObjProp(String name) {
        return dataFactory.getOWLObjectProperty(IRI.create(pizzaPrefix, name));
    }

    @Before
    public void setUp() throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(pizzaPath));
        OWLReasoner reasoner = new Reasoner(ontology);

        // Ensure preconditions
        assertTrue(reasoner.isConsistent());
        assertEquals(reasoner.getUnsatisfiableClasses().getSize(), 1);  // always includes owl:Nothing

        axiomTester = new AxiomTester(reasoner);
    }

    @Test
    public void testSubClassOf() throws Exception {
        assertEquals(
                TestResult.ENTAILED,
                axiomTester.testSubClassOf(
                        parseClass("Margherita"),
                        parseClass("NamedPizza")
                )
        );
        assertEquals(
                TestResult.ENTAILED,
                axiomTester.testSubClassOf(
                        dataFactory.getOWLObjectSomeValuesFrom(parseObjProp("hasTopping"), parseClass("MeatTopping")),
                        dataFactory.getOWLObjectComplementOf(parseClass("VegetarianPizza"))
                )
        );
        assertEquals(
                TestResult.ABSENT,
                axiomTester.testSubClassOf(
                        parseClass("Pizza"),
                        parseClass("NamedPizza")
                )
        );
        assertEquals(
                TestResult.INCOHERENT,
                axiomTester.testSubClassOf(
                        parseClass("PizzaBase"),
                        parseClass("PizzaTopping")
                )
        );
        assertEquals(
                TestResult.INCONSISTENT,
                axiomTester.testSubClassOf(
                        parseClass("Country"),
                        dataFactory.getOWLObjectComplementOf(parseClass("Country"))
                )
        );
    }

    @Test
    public void testEquivalentClasses() throws Exception {
        assertEquals(
                TestResult.ENTAILED,
                axiomTester.testEquivalentClasses(
                        parseClass("SpicyPizza"),
                        parseClass("SpicyPizzaEquivalent")
                )
        );
        assertEquals(
                TestResult.ABSENT,
                axiomTester.testEquivalentClasses(
                        parseClass("Country"),
                        parseClass("Pizza")
                )
        );
        assertEquals(
                TestResult.INCOHERENT,
                axiomTester.testEquivalentClasses(
                        parseClass("Country"),
                        parseClass("Pizza"),
                        parseClass("PizzaBase"),
                        parseClass("PizzaTopping")
                )
        );
    }

    @Test
    public void testDisjointClasses() throws Exception {
        assertEquals(
                TestResult.ENTAILED,
                axiomTester.testDisjointClasses(
                        parseClass("Pizza"),
                        parseClass("PizzaBase"),
                        parseClass("PizzaTopping")
                )
        );
        assertEquals(
                TestResult.ABSENT,
                axiomTester.testDisjointClasses(
                        parseClass("SpicyPizza"),
                        parseClass("VegetarianPizza")
                )
        );
        assertEquals(
                TestResult.INCOHERENT,
                axiomTester.testDisjointClasses(
                        parseClass("Pizza"),
                        parseClass("Pizza")
                )
        );
    }

    @Test
    public void testDisjointUnion() throws Exception {
        assertEquals(
                TestResult.ENTAILED,
                axiomTester.testDisjointUnion(
                        parseClass("Food"),
                        parseClass("Pizza"),
                        dataFactory.getOWLObjectIntersectionOf(parseClass("Food"), dataFactory.getOWLObjectComplementOf(parseClass("Pizza")))
                )
        );
        assertEquals(
                TestResult.ABSENT,
                axiomTester.testDisjointUnion(
                        parseClass("Food"),
                        parseClass("Pizza"),
                        parseClass("PizzaBase"),
                        parseClass("PizzaTopping")
                )
        );
        assertEquals(
                TestResult.INCOHERENT,
                axiomTester.testDisjointUnion(
                        parseClass("Food"),
                        parseClass("Pizza"),
                        parseClass("PizzaBase")
                )
        );
    }

    @Test
    public void testSameIndividual() throws Exception {
        assertEquals(
                TestResult.ENTAILED,
                axiomTester.testSameIndividual(
                        parseIndiv("England"),
                        parseIndiv("England")
                )
        );
        assertEquals(
                TestResult.ABSENT,
                axiomTester.testSameIndividual(
                        parseIndiv("England"),
                        parseIndiv("Scotland")
                )
        );
        assertEquals(
                TestResult.INCONSISTENT,
                axiomTester.testSameIndividual(
                        parseIndiv("England"),
                        parseIndiv("France")
                )
        );
    }

    @Test
    public void testDifferentIndividuals() throws Exception {
        assertEquals(
                TestResult.ENTAILED,
                axiomTester.testDifferentIndividuals(
                        parseIndiv("England"),
                        parseIndiv("France")
                )
        );
        assertEquals(
                TestResult.ABSENT,
                axiomTester.testDifferentIndividuals(
                        parseIndiv("England"),
                        parseIndiv("Scotland")
                )
        );
        assertEquals(
                TestResult.INCONSISTENT,
                axiomTester.testDifferentIndividuals(
                        parseIndiv("England"),
                        parseIndiv("England")
                )
        );
    }
}