package cpw.mods.socketchat;

import com.google.gson.JsonObject;

public class ServerEvent {
    public static String connected() {
        final JsonObject je = JsonHandler.build("event");
        je.addProperty("event","connected");
        return je.toString();
    }

    public static String disconnected() {
        final JsonObject je = JsonHandler.build("event");
        je.addProperty("event","disconnected");
        return je.toString();
    }
}
