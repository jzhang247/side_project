package notreallyagroup.backend.mrchd;

import java.util.HashMap;
import java.util.Map;

public class CategoryInfo {

    public String name;
    public Map<String, AttributeInfo> attributes = new HashMap<>();


    public CategoryInfo(String name) {
        this.name = name;
        add(new AttributeInfo(AttributeTypes.String, "title"));
        add(new AttributeInfo(AttributeTypes.String, "description"));
        add(new AttributeInfo(AttributeTypes.Float, "price"));
    }

    public CategoryInfo add(AttributeInfo attribute) {
        attributes.put(attribute.name, attribute);
        return this;
    }

}
