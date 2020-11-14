package cpw.mods.socketchat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Function;
import java.util.stream.Stream;

public class JsonHandler {
    static JsonObject build(final String type) {
        JsonObject res = new JsonObject();
        res.addProperty("type", type);
        res.addProperty("serveruuid", SimpleSocketChat.INSTANCE.config.serveruuid.get());
        return res;
    }

    static <T> JsonObject buildArray(JsonObject target, String propertyName, Stream<T> source, Function<T, String> converter) {
        JsonArray results = new JsonArray();
        source.map(converter).forEach(results::add);
        target.add(propertyName, results);
        return target;
    }
}
