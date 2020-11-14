package cpw.mods.socketchat;

import com.google.gson.JsonObject;

public class ServerEvent {
    public static String connected() {
        JsonObject je = new JsonObject();
        je.addProperty("type","event");
        je.addProperty("event","connected");
        return je.toString();
    }
}
