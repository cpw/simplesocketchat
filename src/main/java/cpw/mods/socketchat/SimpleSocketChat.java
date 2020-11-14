package cpw.mods.socketchat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod("simplesocketchat")
public class SimpleSocketChat {
    private final Config config;
    private ExecutorService executor;
    private WebSocketClient webSocketClient;

    public SimpleSocketChat() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        final Pair<Config, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Config::new);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, pair.getRight());
        config = pair.getLeft();
        modEventBus.addListener(this::onConfigLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onConfigLoad(ModConfig.Loading configEvent) {
        if (executor!=null) {
            executor.shutdown();
        }
        webSocketClient = new WebSocketClient(config.host.get(), config.port.get());
    }

    private void onServerStarting(final FMLServerAboutToStartEvent event) {
        executor = Executors.newSingleThreadExecutor(WebSocketClient::newThread);
        CompletableFuture.supplyAsync(webSocketClient::setupChannel, executor).whenCompleteAsync((wsc, throwable) -> handleConnection(event.getServer(), wsc, throwable), executor);
    }

    private void handleConnection(final MinecraftServer server, final WebSocketClient wsc, final Throwable throwable) {
        wsc.sendTextFrame(ServerEvent.connected());
        wsc.setIncomingConsumer(text->handleIncomingMessage(text, server));
        MinecraftForge.EVENT_BUS.addListener((ServerChatEvent serverChatEvent) -> {
            JsonObject jo = new JsonObject();
            jo.addProperty("type", "message");
            jo.addProperty("uuid", serverChatEvent.getPlayer() != null? serverChatEvent.getPlayer().getUniqueID().toString(): Util.DUMMY_UUID.toString());
            jo.addProperty("message", serverChatEvent.getComponent().getString());
            wsc.sendTextFrame(jo.toString());
        });
    }

    private void handleIncomingMessage(final String text, final MinecraftServer server) {
        JsonObject message = new JsonParser().parse(text).getAsJsonObject();
        final String type = message.get("type").getAsString();
        switch (type) {
            case "message":
                final IFormattableTextComponent component = ITextComponent.Serializer.getComponentFromJson(message.get("message"));
                server.execute(()->server.getPlayerList().getPlayers().forEach(p->p.sendMessage(component, Util.DUMMY_UUID)));
                break;
            case "command":
                CommandSource cs = server.getCommandSource();
                List<ITextComponent> captures = new ArrayList<>();
                CommandSource me = new CommandSource(new SSCCommandConsumer(captures), cs.getPos(), cs.getRotation(), cs.getWorld(), 4, "simplesocketchat", new StringTextComponent("simplesocketchat"), server, null);
                server.execute(()->runCommand(server, message, me, captures));
        }
    }

    private void runCommand(final MinecraftServer server, final JsonObject message, final CommandSource cs, final List<ITextComponent> captures) {
        try {
            final int command = server.getCommandManager().getDispatcher().execute(message.get("command").getAsString(), cs);
        } catch (CommandSyntaxException e) {
        }
        executor.submit(()-> {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", "commandresult");
            jsonObject.add("cmduuid", message.get("cmduuid"));
            JsonArray results = new JsonArray();
            captures.stream().map(ITextComponent::getString).forEach(results::add);
            jsonObject.add("results", results);
            webSocketClient.sendTextFrame(jsonObject.toString());
        });
    }

    private void onServerShutdown(final FMLServerStoppedEvent event) {
        executor.submit(webSocketClient::disconnect);
        executor.shutdown();
    }

    private static class SSCCommandConsumer implements ICommandSource {
        private final List<ITextComponent> captures;

        public SSCCommandConsumer(final List<ITextComponent> captures) {
            this.captures = captures;
        }

        @Override
        public void sendMessage(final ITextComponent component, final UUID senderUUID) {
            captures.add(component);
        }

        @Override
        public boolean shouldReceiveFeedback() {
            return true;
        }

        @Override
        public boolean shouldReceiveErrors() {
            return true;
        }

        @Override
        public boolean allowLogging() {
            return false;
        }
    }
}
