package notreallyagroup.backend.mrchd;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CategoryManager {
    public Map<String, CategoryInfo> categories = new HashMap<>();

    public CategoryManager() {
        {
            var cat = new CategoryInfo("screen");
            cat.add(new AttributeInfo(AttributeTypes.Float, "size"));
            cat.add(new AttributeInfo(AttributeTypes.Float, "refreshRate"));
            cat.add(new AttributeInfo(AttributeTypes.Float, "resolutionLongAxis"));
            cat.add(new AttributeInfo(AttributeTypes.Float, "resolutionShortAxis"));
            categories.put(cat.name, cat);
        }
        {
            var cat = new CategoryInfo("hard_drive");
            cat.add(new AttributeInfo(AttributeTypes.Float, "capacityInGigabytes"));
            cat.add(new AttributeInfo(AttributeTypes.Float, "readSpeedInGigabytes"));
            cat.add(new AttributeInfo(AttributeTypes.Float, "writeSpeedInGigabytes"));
            categories.put(cat.name, cat);
        }
    }
}
