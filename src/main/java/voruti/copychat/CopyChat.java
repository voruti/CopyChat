package voruti.copychat;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Plugin(
        id = "copychat",
        name = "CopyChat",
        version = BuildConstants.VERSION,
        description = "A Velocity plugin that copies chat messages to players on other servers.",
        url = "https://github.com/voruti/CopyChat",
        authors = {"voruti"}
)
public class CopyChat {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Enabled.");
    }

    @Subscribe
    public EventTask onPlayerChat(PlayerChatEvent event) {
        return EventTask.async(() -> {
            Collection<Player> allPlayers = server.getAllPlayers();
            logger.trace("All current players: {}", allPlayers);

            Optional<ServerConnection> serverConnection = event.getPlayer().getCurrentServer();
            if (serverConnection.isPresent()) {
                Collection<Player> receivingAnywayPlayers = serverConnection.get()
                        .getServer().getPlayersConnected();
                logger.trace("Players {} will receive the message anyway", receivingAnywayPlayers);

                Collection<Player> leftOutPlayers = allPlayers.stream()
                        .filter(everyPlayer -> receivingAnywayPlayers.stream().noneMatch(receivingPlayer ->
                                everyPlayer.getGameProfile().getId().equals(receivingPlayer.getGameProfile().getId())))
                        .collect(Collectors.toList());
                logger.trace("Players {} are left out and need a copy of the message", leftOutPlayers);

                if (leftOutPlayers.isEmpty()) {
                    logger.trace("No players are left out: ending");
                    return;
                }

                String message = event.getMessage();
                Player sender = event.getPlayer();
                logger.trace("Event analyzed as message \"{}\" from {}", message, sender);

                TextComponent textComponent = Component.text(
                        String.format("<%s> %s", sender.getGameProfile().getName(), message));
                logger.trace("Prepared text component {} for sending", textComponent);

                leftOutPlayers.forEach(player -> {
                    player.sendMessage(textComponent);
                    logger.trace("Sent text to {}", player);
                });
            } else {
                logger.debug("Server connection of sending player {} not found: aborting", event.getPlayer());
            }
        });
    }
}
