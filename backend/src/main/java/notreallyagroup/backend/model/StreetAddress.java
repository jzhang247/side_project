package notreallyagroup.backend.model;

public record StreetAddress(
        String line1,
        String line2,
        String city,
        String stateOrProvince,
        String country,
        String zipCode
) {
}
