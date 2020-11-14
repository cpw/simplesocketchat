package cpw.mods.socketchat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.Event;

import java.net.URI;
import java.util.function.Consumer;

public class WebSocketClient {
    private Channel channel;
    private final String inetAddress;
    private final int inetPort;
    private WebSocketClientHandler clientHandler;

    public WebSocketClient(final String inetAddress, final int inetPort) {
        this.inetAddress = inetAddress;
        this.inetPort = inetPort;
    }

    WebSocketClient setupChannel() {
        final URI webSocketURL = URI.create("ws://"+inetAddress+":"+inetPort);
        clientHandler = new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(webSocketURL, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));
        final NioEventLoopGroup group = new NioEventLoopGroup(1);
        final ChannelFuture remoteConnect = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(inetAddress, inetPort)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) {
                        ch.pipeline().addLast("codec", new HttpClientCodec());
                        ch.pipeline().addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));
                        ch.pipeline().addLast("websocket", WebSocketClientCompressionHandler.INSTANCE);
                        ch.pipeline().addLast("wshandler", clientHandler);
                    }
                })
                .connect();
        remoteConnect.syncUninterruptibly();
        clientHandler.handshakeFuture().syncUninterruptibly();
        channel = remoteConnect.channel();
        return this;
    }

    void disconnect() {
        channel.close().syncUninterruptibly();
    }

    void setIncomingConsumer(Consumer<String> incoming) {
        clientHandler.setIncomingConsumer(incoming);
    }
    public static Thread newThread(final Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.setName("WebSocketClientThread");
        return t;
    }

    public void sendTextFrame(final String string) {
        channel.writeAndFlush(new TextWebSocketFrame(string));
    }

    public void setStatusHandler(Consumer<WebSocketClientHandler.Status> consumer) {
        clientHandler.setStatusHandler(consumer);
    }
}
