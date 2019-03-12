package cls.atoll.motu.objs;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductAttachmentType {
    EXTERNAL_CATALOG_LOCATION("externalCatalogLocation"), EXTERNAL_LOCATION("externalLocations"), INTERNAL_LOCATION("internalLocations");

    private final String name;

    ProductAttachmentType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
