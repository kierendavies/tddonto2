package za.ac.uct.cs.tddontoi;

public enum TestResult {
    ENTAILED, ABSENT, INCOHERENT, INCONSISTENT;

    public static TestResult max(TestResult r1, TestResult r2) {
        return r1.compareTo(r2) > 0 ? r1 : r2;
    }
}
