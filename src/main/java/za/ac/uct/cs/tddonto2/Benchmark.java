package za.ac.uct.cs.tddonto2;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.util.ArrayList;

public class Benchmark {
    // Put ontologies in src/main/resources and add their paths here like this.
    private static final String[] ontologyPaths = {
            "src/main/resources/pizza.owl"
    };

    // Change this to whatever you want
    private static final int TRIALS = 5;

    private static void genAndTestAxioms(OWLOntology ontology, OWLReasoner reasoner) {
        SimpleTestGenerator testGenerator = new SimpleTestGenerator(ontology.getOWLOntologyManager(), ontology);
        AxiomTester axiomTester = new AxiomTester(reasoner);

        System.out.println("Testing with ontology " + ontology.getOWLOntologyManager().getOntologyDocumentIRI(ontology));

        // Test SubClassOf
        for (int i = 0; i < TRIALS; i++) {
            OWLAxiom axiom = testGenerator.generateSubClassOfAxiom(ontology.getOWLOntologyManager(), ontology, 0);
            long startTime = System.nanoTime();
            axiomTester.test(axiom);
            long duration = System.nanoTime() - startTime;
            System.out.println("" + reasoner.getClass().getName() + " SubClassOf " + duration);
        }

        // Test EquivalentClasses
        for (int i = 0; i < TRIALS; i++) {
            OWLAxiom axiom = testGenerator.generateEquivalentClassesAxiom(ontology.getOWLOntologyManager(), ontology, 0);
            long startTime = System.nanoTime();
            axiomTester.test(axiom);
            long duration = System.nanoTime() - startTime;
            System.out.println("" + reasoner.getClass().getName() + " EquivalentClasses " + duration);
        }

        // You can fill in the rest.  Just change the generateWhatever method call, and the log message
        // (first and last lines of the loop).
    }

    public static void main(String[] args) throws OWLOntologyCreationException {
        ArrayList<OWLOntology> ontologies = new ArrayList<OWLOntology>(ontologyPaths.length);
        for (String ontologyPath : ontologyPaths) {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
            ontologies.add(ontology);
        }

        for (OWLOntology ontology : ontologies) {
            // Test HermiT
            OWLReasoner hermitReasoner = new org.semanticweb.HermiT.ReasonerFactory().createNonBufferingReasoner(ontology);
            genAndTestAxioms(ontology, hermitReasoner);

            // Test more reasoners...
            // You have to figure out how to construct them, then call genAndTestAxioms like above
        }
    }
}
