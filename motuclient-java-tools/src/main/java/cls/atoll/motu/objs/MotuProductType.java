package cls.atoll.motu.objs;

public enum MotuProductType {
    COLLECTION(true), DATASET(true), GRANULE(false);

    private final boolean isGroup;

    MotuProductType(final boolean isGroup) {
        this.isGroup = isGroup;
    }

    public boolean isGroup() {
        return isGroup;
    }
}
