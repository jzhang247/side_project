package notreallyagroup.backend.mrchd;

import com.google.gson.Gson;
import notreallyagroup.backend.model.LatLng;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MerchandiseInfo {
    protected String id;
    protected String title;
    protected String description;
    protected String price;
    protected LatLng latLng;
    protected HashMap<String, String> attributes;

    public String id(){
        return id;
    }
    public String name() {
        return title;
    }
    public String description() {
        return description;
    }
    public String price() {
        return price;
    }
    public LatLng latLng() {
        return latLng;
    }
    public Map<String, String> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }





}
