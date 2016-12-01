package za.ac.uct.cs.tddontoi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;

import static org.junit.Assert.*;

public class AxiomTesterTest {
    private OWLReasoner reasoner;

    @Before
    public void setUp() throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("src/test/resources/pizza.owl"));
        reasoner = new Reasoner(ontology);
        assertTrue(reasoner.isConsistent());
        assertEquals(reasoner.getUnsatisfiableClasses().getSize(), 1);  // always includes owl:Nothing
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSubClassOf() throws Exception {
        fail();
    }
}