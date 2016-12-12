package za.ac.uct.cs.tddonto2;

public enum TestResult {
    ENTAILED, ABSENT, INCOHERENT, INCONSISTENT, MISSING_ENTITY, PRE_INCOHERENT, PRE_INCONSISTENT;

    public static TestResult max(TestResult r1, TestResult r2) {
        return r1.compareTo(r2) > 0 ? r1 : r2;
    }

    public String humanize() {
        switch (this) {
            case ENTAILED: return "Entailed";
            case ABSENT: return "Absent";
            case INCOHERENT: return "Incoherent";
            case INCONSISTENT: return "Inconsistent";
            case MISSING_ENTITY: return "Missing entity";
            case PRE_INCOHERENT: return "Failed precondition: ontology incoherent";
            case PRE_INCONSISTENT: return "Failed precondition: ontology inconsistent";
        }
        return null;
    }
}
