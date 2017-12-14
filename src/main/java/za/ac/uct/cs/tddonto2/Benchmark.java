package za.ac.uct.cs.tddonto2;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

public class Benchmark {
    // Change this to whatever you want
    private static final int WARMUP_TRIALS = 1;
    private static final int TRIALS = 10;
    private static final int MAX_CARDINALITY_RESTRICTION = 3;

    private static final Random random = new Random();

    private static List<Double> timeTest(AxiomTester axiomTester, Supplier<OWLAxiom> axiomSupplier) {
        for (int i = 0; i < WARMUP_TRIALS; i++) {
            OWLAxiom axiom = axiomSupplier.get();
            axiomTester.test(axiom);
        }
        List<Double> durations = new ArrayList<>();
        for (int i = 0; i < TRIALS; i++) {
            if (durations.stream().mapToDouble((x) -> x).sum() * 1e-9 > 300) {
                break;
            }
            OWLAxiom axiom = axiomSupplier.get();
//            System.out.println(axiom);
            double startTime = System.nanoTime();
            axiomTester.test(axiom);
            double endTime = System.nanoTime();
            durations.add((endTime - startTime) * 1e-9);
        }
        return durations;
    }

    private static void printStats(List<Double> values) {
        double count = values.size();
        double sum = 0;
        double sumOfSquares = 0;
        double min = values.get(0);
        double max = values.get(0);

        for (double value : values) {
            sum += value;
            sumOfSquares += value * value;
            if (value < min) min = value;
            if (value > max) max = value;
        }

        double mean = sum / count;
        double variance = (sumOfSquares / count) - (mean * mean);
        double stdev = Math.sqrt(variance);

        System.out.printf("min   %.6f s\n", min);
        System.out.printf("max   %.6f s\n", max);
        System.out.printf("mean  %.6f s\n", mean);
        System.out.printf("stdev %.6f s\n", stdev);
    }

    private static <T> T chooseRandom(ArrayList<T> things) {
        return things.get(random.nextInt(things.size()));
    }

    private static void genAndTestAxioms(OWLOntology ontology, OWLReasoner reasoner) {
        SimpleTestGenerator testGenerator = new SimpleTestGenerator(ontology.getOWLOntologyManager(), ontology);
        AxiomTester axiomTester = new AxiomTester(reasoner);
        OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();

        ArrayList<OWLClass> classes = new ArrayList<>(ontology.getClassesInSignature());
        classes.add(dataFactory.getOWLClass(IRI.create("tddonto2#", "Class1")));
        ArrayList<OWLNamedIndividual> individuals = new ArrayList<>(ontology.getIndividualsInSignature());
        individuals.add(dataFactory.getOWLNamedIndividual(IRI.create("tddonto2#", "Individual1")));
        ArrayList<OWLObjectProperty> objectProperties = new ArrayList<>(ontology.getObjectPropertiesInSignature());
        objectProperties.add(dataFactory.getOWLObjectProperty(IRI.create("tddonto2#", "Property1")));

        System.out.println("(i) SubClassOf");
        printStats(timeTest(axiomTester, () ->
            dataFactory.getOWLSubClassOfAxiom(
                    chooseRandom(classes),
                    chooseRandom(classes)
            )
        ));

        System.out.println("(ii) SubClassOfObjectSomeOrAllValuesFrom");
        printStats(timeTest(axiomTester, () -> {
            OWLClassExpression superclass;
            if (random.nextBoolean()) {
                superclass = dataFactory.getOWLObjectSomeValuesFrom(
                        chooseRandom(objectProperties),
                        chooseRandom(classes)
                );
            } else {
                superclass = dataFactory.getOWLObjectAllValuesFrom(
                        chooseRandom(objectProperties),
                        chooseRandom(classes)
                );
            }
            return dataFactory.getOWLSubClassOfAxiom(
                    chooseRandom(classes),
                    superclass
            );
        }));

        System.out.println("(iii) DisjointClasses");
        printStats(timeTest(axiomTester, () ->
            dataFactory.getOWLDisjointClassesAxiom(
                chooseRandom(classes),
                chooseRandom(classes)
            )
        ));

        System.out.println("(iv) ObjectPropertyDomain");
        printStats(timeTest(axiomTester, () ->
//            dataFactory.getOWLObjectPropertyDomainAxiom(
//                    chooseRandom(objectProperties),
//                    chooseRandom(classes)
//            )
            dataFactory.getOWLSubClassOfAxiom(
                    dataFactory.getOWLObjectSomeValuesFrom(
                            chooseRandom(objectProperties),
                            dataFactory.getOWLThing()
                    ),
                    chooseRandom(classes)
            )
        ));

        System.out.println("(v) ObjectPropertyRange");
        printStats(timeTest(axiomTester, () ->
//            dataFactory.getOWLObjectPropertyRangeAxiom(
//                    chooseRandom(objectProperties),
//                    chooseRandom(classes)
//            )
            dataFactory.getOWLSubClassOfAxiom(
                    dataFactory.getOWLObjectSomeValuesFrom(
                            dataFactory.getOWLObjectInverseOf(chooseRandom(objectProperties)),
                            dataFactory.getOWLThing()
                    ),
                    chooseRandom(classes)
            )
        ));

        System.out.println("(vi) ClassAssertion");
        printStats(timeTest(axiomTester, () ->
            dataFactory.getOWLClassAssertionAxiom(
                    chooseRandom(classes),
                    chooseRandom(individuals)
            )
        ));

        System.out.println("(vii) SubClassOfObjectCardinalityRestriction");
        printStats(timeTest(axiomTester, () -> {
            OWLClassExpression superclass;
            int branch = random.nextInt(3);
            if (branch == 0) {
                superclass = dataFactory.getOWLObjectMinCardinality(
                        random.nextInt(MAX_CARDINALITY_RESTRICTION) + 1,
                        chooseRandom(objectProperties),
                        chooseRandom(classes)
                );
            } else if (branch == 1) {
                superclass = dataFactory.getOWLObjectMaxCardinality(
                        random.nextInt(MAX_CARDINALITY_RESTRICTION) + 1,
                        chooseRandom(objectProperties),
                        chooseRandom(classes)
                );
            } else {
                superclass = dataFactory.getOWLObjectExactCardinality(
                        random.nextInt(MAX_CARDINALITY_RESTRICTION) + 1,
                        chooseRandom(objectProperties),
                        chooseRandom(classes)
                );
            }
            return dataFactory.getOWLSubClassOfAxiom(
                    chooseRandom(classes),
                    superclass
            );
        }));

        System.out.println("(ix) SubClassOfObjectSomeAndAllValuesFrom");
        printStats(timeTest(axiomTester, () -> {
            OWLObjectProperty objectProperty = chooseRandom(objectProperties);
            OWLClass clas = chooseRandom(classes);
            return dataFactory.getOWLSubClassOfAxiom(
                    chooseRandom(classes),
                    dataFactory.getOWLObjectIntersectionOf(
                            dataFactory.getOWLObjectSomeValuesFrom(
                                    objectProperty,
                                    clas
                            ),
                            dataFactory.getOWLObjectAllValuesFrom(
                                    objectProperty,
                                    clas
                            )
                    )
            );
        }));

        System.out.println("(x) ComplicatedThing");
        printStats(timeTest(axiomTester, () ->
            dataFactory.getOWLSubClassOfAxiom(
                    chooseRandom(classes),
                    dataFactory.getOWLObjectIntersectionOf(
                            dataFactory.getOWLObjectSomeValuesFrom(
                                    chooseRandom(objectProperties),
                                    chooseRandom(classes)
                            ),
                            dataFactory.getOWLObjectSomeValuesFrom(
                                    chooseRandom(objectProperties),
                                    dataFactory.getOWLObjectUnionOf(
                                            chooseRandom(classes),
                                            chooseRandom(classes)
                                    )
                            )
                    )
            )
        ));
    }

    public static void main(String[] args) throws OWLOntologyCreationException {

        OWLReasonerFactory[] reasonerFactories = new OWLReasonerFactory[] {
                new org.semanticweb.HermiT.ReasonerFactory()
        };

        String[] ontologyPaths = {
                "src/main/resources/pizza.owl",
                "src/main/resources/AfricanWildlifeOntology1.owl",
//                "src/main/resources/DMOP/DMOPresaved.owl"
//                "src/main/resources/tonesOntologies/adolena.owl",
//                "src/main/resources/tonesOntologies/amino-acid.owl",
//                "src/main/resources/tonesOntologies/atom-common.rdf",
//                "src/main/resources/tonesOntologies/atom-primitive.rdf",
//                "src/main/resources/tonesOntologies/biochemical-reaction-complex.rdf",
//                "src/main/resources/tonesOntologies/biopax-level2.owl",
//                "src/main/resources/tonesOntologies/brokenPizza.owl",
//                "src/main/resources/tonesOntologies/chemical.rdf",
//                "src/main/resources/tonesOntologies/chemistry-primitive.rdf",
//                "src/main/resources/tonesOntologies/CONFTOOL-EKAW.owl",
//                "src/main/resources/tonesOntologies/CRS-CONFTOOL.owl",
//                "src/main/resources/tonesOntologies/CRS-EKAW.owl",
//                "src/main/resources/tonesOntologies/CRS-SIGKDD.owl",
//                "src/main/resources/tonesOntologies/cton.owl",  // 6 s
//                "src/main/resources/tonesOntologies/DAM.rdf",
//                "src/main/resources/tonesOntologies/DOLCE_Lite_397.owl",
//                "src/main/resources/tonesOntologies/download.rdf",
//                "src/main/resources/tonesOntologies/eukariotic.owl",
//                "src/main/resources/tonesOntologies/family-tree.owl",
//                "src/main/resources/tonesOntologies/family.owl",
//                "src/main/resources/tonesOntologies/foodswap.owl",
//                "src/main/resources/tonesOntologies/galen.owl",
//                "src/main/resources/tonesOntologies/galen.rdf",  // 12 s
//                "src/main/resources/tonesOntologies/generations-minus-same-individual-axioms.owl",
//                "src/main/resources/tonesOntologies/generations.owl",
//                "src/main/resources/tonesOntologies/GH5Complete.owl",
//                "src/main/resources/tonesOntologies/GRO.rdf",
//                "src/main/resources/tonesOntologies/IEDM.owl",
//                "src/main/resources/tonesOntologies/iso-19103.rdf",
//                "src/main/resources/tonesOntologies/ka.owl",
//                "src/main/resources/tonesOntologies/koala.owl",
//                "src/main/resources/tonesOntologies/molecule-complex.rdf",
                "src/main/resources/tonesOntologies/Movie.owl",  // 10 s
//                "src/main/resources/tonesOntologies/mygrid-moby-service.rdf",
//                "src/main/resources/tonesOntologies/mygrid-unclassified.rdf",
//                "src/main/resources/tonesOntologies/nautilus.rdf",
//                "src/main/resources/tonesOntologies/nulo.rdf",
//                "src/main/resources/tonesOntologies/ontology.rdf",
//                "src/main/resources/tonesOntologies/Ontology1191594278.owl",  // 8 s
//                "src/main/resources/tonesOntologies/Ontology1217839243861.owl",
//                "src/main/resources/tonesOntologies/Ontology1225724807074194000.rdf",
//                "src/main/resources/tonesOntologies/Ontology1225725433367251000.rdf",
//                "src/main/resources/tonesOntologies/organic-compound-complex.rdf",
//                "src/main/resources/tonesOntologies/organic-functional-group-complex.rdf",
//                "src/main/resources/tonesOntologies/particle.owl",
//                "src/main/resources/tonesOntologies/people.owl",
//                "src/main/resources/tonesOntologies/pharmacogenomics-complex.rdf",
//                "src/main/resources/tonesOntologies/pharmacogenomics-primitive.rdf",
//                "src/main/resources/tonesOntologies/physics-complex.rdf",
//                "src/main/resources/tonesOntologies/policyContainmentTest.owl",
//                "src/main/resources/tonesOntologies/property-complex.rdf",
//                "src/main/resources/tonesOntologies/propertyinferences.owl",
//                "src/main/resources/tonesOntologies/protege.rdf",
//                "src/main/resources/tonesOntologies/reaction.owl",
//                "src/main/resources/tonesOntologies/ribosome.owl",
//                "src/main/resources/tonesOntologies/SC.owl",
//                "src/main/resources/tonesOntologies/SIGKDD-EKAW.owl",
//                "src/main/resources/tonesOntologies/software.rdf",
//                "src/main/resources/tonesOntologies/subatomic-particle-complex.rdf",
                "src/main/resources/tonesOntologies/substance.owl",  // 59 s
//                "src/main/resources/tonesOntologies/tambis-patched.owl",
//                "src/main/resources/tonesOntologies/unit-primitive.rdf",
//                "src/main/resources/tonesOntologies/units.owl",
//                "src/main/resources/tonesOntologies/univ-bench.owl",
//                "src/main/resources/tonesOntologies/university.owl",
//                "src/main/resources/tonesOntologies/unnamed.owl",
//                "src/main/resources/tonesOntologies/UnsatCook.owl"
        };

        for (OWLReasonerFactory reasonerFactory : reasonerFactories) {
            System.out.printf("Using reasoner factory %s\n", reasonerFactory.getClass().getName());
            for (String ontologyPath : ontologyPaths) {
                try {
                    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                    OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
                    System.out.printf("Using ontology %s\n", ontologyPath);
//                    double startTime = System.nanoTime();
                    OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
                    reasoner.precomputeInferences(InferenceType.values());
                    reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
//                    double endTime = System.nanoTime();
//                    System.out.printf("%.2f\n", (endTime - startTime) * 1e-9);
                genAndTestAxioms(ontology, reasoner);
                } catch (UnloadableImportException e) {
                    System.out.printf("Can't load %s\n", ontologyPath);
                }
            }
        }
    }
}
