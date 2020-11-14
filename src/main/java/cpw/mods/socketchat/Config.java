package cpw.mods.socketchat;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.UUID;

public class Config {
    final ForgeConfigSpec.ConfigValue<String> host;
    final ForgeConfigSpec.ConfigValue<Integer> port;
    final ForgeConfigSpec.ConfigValue<String> serveruuid;

    Config(ForgeConfigSpec.Builder builder) {
        builder.comment("Web socket host name");
        host = builder.define("host", "localhost");

        builder.comment("Web socket host port");
        port = builder.define("port", 9996);

        builder.comment("Server uuid");
        serveruuid = builder.define("serveruuid", UUID.randomUUID().toString());
    }
}
