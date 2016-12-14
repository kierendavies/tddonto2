package za.ac.uct.cs.tddonto2;

import org.semanticweb.owlapi.model.OWLAxiom;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class TestSuiteModel extends AbstractTableModel {
    private List<OWLAxiom> axioms;
    private List<TestResult> results;
    private TestResult preconditionsResult;

    public TestSuiteModel() {
        axioms = new ArrayList<>();
        results = new ArrayList<>();
        preconditionsResult = null;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Axiom";
            case 1:
                return "Result";
            default:
                throw new IndexOutOfBoundsException("Column index must be less than 2");
        }
    }

    @Override
    public int getRowCount() {
        return axioms.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return axioms.get(rowIndex).toString();
            case 1:
                if (results.get(rowIndex) == null) {
                    return "";
                } else {
                    return results.get(rowIndex).humanize();
                }
            default:
                throw new IndexOutOfBoundsException("Column index must be less than 2");
        }
    }

    public void add(OWLAxiom axiom) {
        axioms.add(axiom);
        results.add(null);
    }

    public void remove(int index) {
        axioms.remove(index);
        results.remove(index);
        fireTableDataChanged();
    }

    public void remove(int[] indices) {
        for (int i : indices) {
            axioms.remove(i);
            results.remove(i);
        }
        fireTableDataChanged();
    }

    private void evaluate(AxiomTester tester, int index) {
        TestResult result = tester.test(axioms.get(index));
        results.set(index, result);
    }

    // TODO: nice reporting
    public void evaluateAll(AxiomTester tester) {
        preconditionsResult = tester.testPreconditions();
        if (preconditionsResult != null) {
            for (int i = 0; i < axioms.size(); i++) {
                results.set(i, preconditionsResult);
            }
        } else {
            for (int i = 0; i < axioms.size(); i++) {
                evaluate(tester, i);
            }
        }
        fireTableRowsUpdated(0, axioms.size());
    }

    public void evaluateOnly(AxiomTester tester, int[] indices) {
        preconditionsResult = tester.testPreconditions();
        if (preconditionsResult != null) {
            for (int i : indices) {
                results.set(i, preconditionsResult);
            }
        } else {
            for (int i : indices) {
                evaluate(tester, i);
            }
        }
        fireTableRowsUpdated(0, axioms.size());  // TODO: this range could be smaller
    }

//    @Override
//    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
//    }
}
