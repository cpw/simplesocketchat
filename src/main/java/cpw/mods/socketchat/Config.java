package cpw.mods.socketchat;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    final ForgeConfigSpec.ConfigValue<String> host;
    final ForgeConfigSpec.ConfigValue<Integer> port;

    Config(ForgeConfigSpec.Builder builder) {
        builder.comment("Web socket host name");
        host = builder.define("host", "localhost");

        builder.comment("Web socket host port");
        port = builder.define("port", 9996);
    }
}
