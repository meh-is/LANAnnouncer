package is.meh.minecraft.lan_announcer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LANAnnouncer implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("lan-announcer");
    private ServerAnnouncer ipv4Announcer;
    private ServerAnnouncer ipv6Announcer;

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER) {
            ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
            ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        } else {
            LOGGER.warn(
                "This mod is only intended for dedicated multiplayer" +
                "servers, not clients. Clients can start their own built-in" +
                "LAN broadcast by using `Open to LAN`"
            );
        }
    }

    // Prevent conflicts with the packet payload
    private String sanitizeMOTD(String motd) {
        return motd.replace("[", "").replace("]", "");
    }

    private void onServerStarted(MinecraftServer server) {
        String motd = sanitizeMOTD(server.getServerMotd());
        int server_port = server.getServerPort();

        byte[] message = ("[MOTD]" + motd + "[/MOTD][AD]" + server_port + "[/AD]").getBytes();

        // Initialize and start the IPv4 and IPv6 announcers
        ipv4Announcer = new ServerAnnouncer("224.0.2.60", message);
        ipv4Announcer.startAnnouncing();

        ipv6Announcer = new ServerAnnouncer("ff75:230::60", message);
        ipv6Announcer.startAnnouncing();
    }

    private void onServerStopping(MinecraftServer server) {
        ipv4Announcer.stopAnnouncing();
        ipv6Announcer.stopAnnouncing();
    }

    static class ServerAnnouncer {
        private final byte[] message;
        private DatagramSocket socket;
        private ScheduledExecutorService executorService;
        private boolean running;
        private InetAddress address;

        public ServerAnnouncer(String ipAddress, byte[] message) {
            try {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                address = InetAddress.getByName(ipAddress);
            } catch (IOException e) {
                LOGGER.error("ServerAnnouncer: ", e);
            }

            this.message = message;
        }

        public void startAnnouncing() {
            running = true;
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(this::announce, 0, 1500, TimeUnit.MILLISECONDS);
        }

        private void announce() {
            if (!running) {
                executorService.shutdown();
                return;
            }

            try {
                DatagramPacket packet = new DatagramPacket(message, message.length, address, 4445);
                socket.send(packet);
            } catch (IOException e) {
                LOGGER.error("Error in startAnnouncing", e);
            }
        }

        public void stopAnnouncing() {
            running = false;

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        }
    }
}
