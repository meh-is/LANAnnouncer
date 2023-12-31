package is.meh.minecraft.lan_announcer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
        LOGGER.debug("onInitialize");
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
    }

    // Prevent conflicts with the packet payload
    private String sanitizeMOTD(String motd) {
        return motd.replace("[", "").replace("]", "");
    }

    private void onServerStarted(MinecraftServer server) {
        LOGGER.info("onServerStarted");
        String motd = sanitizeMOTD(server.getServerMotd());
        int server_port = server.getServerPort();

        String payload = "[MOTD]" + motd + "[/MOTD][AD]" + server_port + "[/AD]";
        LOGGER.info("payload: {}", payload);

        byte[] message = payload.getBytes();

        // Initialize and start the IPv4 and IPv6 announcers
        ipv4Announcer = new ServerAnnouncer("224.0.2.60", message);
        ipv4Announcer.startAnnouncing();

        ipv6Announcer = new ServerAnnouncer("ff75:230::60", message);
        ipv6Announcer.startAnnouncing();

        LOGGER.debug("started");
    }

    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("onServerStopping");

        ipv4Announcer.stopAnnouncing();
        ipv6Announcer.stopAnnouncing();

        LOGGER.debug("stopped...");
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
