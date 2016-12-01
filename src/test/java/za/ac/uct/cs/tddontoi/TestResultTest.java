package za.ac.uct.cs.tddontoi;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static za.ac.uct.cs.tddontoi.TestResult.*;

public class TestResultTest {
    @Test
    public void testOrdering() throws Exception {
        assertTrue(ENTAILED.compareTo(ENTAILED) == 0);

        assertTrue(ENTAILED.compareTo(ABSENT) < 0);
        assertTrue(ABSENT.compareTo(INCOHERENT) < 0);
        assertTrue(INCOHERENT.compareTo(INCONSISTENT) < 0);

        assertTrue(INCONSISTENT.compareTo(INCOHERENT) > 0);
        assertTrue(INCOHERENT.compareTo(ABSENT) > 0);
        assertTrue(ABSENT.compareTo(ENTAILED) > 0);

        assertEquals(Collections.min(Arrays.asList(ENTAILED, ABSENT, INCOHERENT, INCONSISTENT)), ENTAILED);
        assertEquals(Collections.max(Arrays.asList(ENTAILED, ABSENT, INCOHERENT, INCONSISTENT)), INCONSISTENT);
    }
}