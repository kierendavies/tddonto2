package za.ac.uct.cs.tddonto2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static za.ac.uct.cs.tddonto2.TestResult.*;

public class TestResultTest {
    @Test
    public void testMax() throws Exception {
        assertEquals(TestResult.max(ENTAILED, ABSENT), ABSENT);
        assertEquals(TestResult.max(ABSENT, ENTAILED), ABSENT);
        assertEquals(TestResult.max(ENTAILED, INCOHERENT), INCOHERENT);
        assertEquals(TestResult.max(INCOHERENT, ENTAILED), INCOHERENT);
        assertEquals(TestResult.max(ENTAILED, INCONSISTENT), INCONSISTENT);
        assertEquals(TestResult.max(INCONSISTENT, ENTAILED), INCONSISTENT);
        assertEquals(TestResult.max(ABSENT, INCOHERENT), INCOHERENT);
        assertEquals(TestResult.max(INCOHERENT, ABSENT), INCOHERENT);
        assertEquals(TestResult.max(ABSENT, INCONSISTENT), INCONSISTENT);
        assertEquals(TestResult.max(INCONSISTENT, ABSENT), INCONSISTENT);
        assertEquals(TestResult.max(INCOHERENT, INCONSISTENT), INCONSISTENT);
        assertEquals(TestResult.max(INCONSISTENT, INCOHERENT), INCONSISTENT);
    }
}