package dev.lycanea.wokedf.client;

import dev.lycanea.wokedf.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WokedfClient implements ClientModInitializer {

    public static final String MOD_ID = "wokedf";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Map<String, String> userPronouns = new HashMap<>();
    public static final Map<String, Instant> userJoined = new HashMap<>();
    private static final LinkedHashSet<String> queue = new LinkedHashSet<>();
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        Config.HANDLER.load();
        ClientTickEvents.END_CLIENT_TICK.register((MinecraftClient client) -> {
            tickCounter++;
            if (onDF() && tickCounter >= 20 && MinecraftClient.getInstance().player != null) {
                tickCounter = 0;
                for (PlayerListEntry entry : MinecraftClient.getInstance().player.networkHandler.getPlayerList()) {
                    getUserPronouns(entry.getProfile().getName());
                    getUserJoinDate(entry.getProfile().getName());
                }
                processQueue();
            }
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (message.getString().matches("(?s).*\\R.*")) {
                Pattern pattern = Pattern.compile(
                        "Profile of ([^\\n\\s(]+) (?:\\(([^)]+)\\))?[\\s\\S]*?→ Joined: ([A-Za-z]+ \\d{1,2}, \\d{4})"
                );
                Matcher matcher = pattern.matcher(message.getString().trim());

                if (matcher.find()) {
                    String username = matcher.group(1).trim();
                    String pronouns = matcher.group(2);
                    String joined = matcher.group(3);

                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);

                    LocalDate date = LocalDate.parse(joined, formatter);
                    Instant joinInstant = date.atStartOfDay(ZoneOffset.UTC).toInstant();

                    boolean bwa = userPronouns.containsKey(username) && userJoined.containsKey(username);

                    userPronouns.put(username, pronouns);
                    userJoined.put(username, joinInstant);
                    return bwa;
                }
            }
            return true;
        });
    }

    public static void processQueue() {
        if (!queue.isEmpty()) {
            if (onDF() && MinecraftClient.getInstance().player != null) {
                String username = queue.removeFirst();
                // double check that its not already processed for some reason idk lmao just incase
                if (userPronouns.containsKey(username) && userJoined.containsKey(username)) return;
                if (!(MinecraftClient.getInstance().world == null)) {
                    MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().player.networkHandler.sendChatCommand("profile " + username));
                }
            }
        }
    }

    public static boolean onDF() {
        if (MinecraftClient.getInstance().player != null) {
            return Objects.requireNonNull(MinecraftClient.getInstance().player.networkHandler.getServerInfo()).address.endsWith("mcdiamondfire.com");
        }
        return false;
    }

    public static String getUserPronouns(String username) {
        if (onDF()) {
            String value = userPronouns.get(username);
            if (value != null) {
                return value;
            }

            if (!userPronouns.containsKey(username)) {
                queue.add(username);
            }
        }
        return null;
    }

    public static Instant getUserJoinDate(String username) {
        if (onDF()) {
            Instant value = userJoined.get(username);
            if (value != null) {
                return value;
            }

            queue.add(username);
        }
        return null;
    }

    public static Text updatePlayerlistEntry(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        MutableText returnValue = cir.getReturnValue().copy();
        if (onDF()) {
            String pronouns = getUserPronouns(entry.getProfile().getName());
            Instant joinDate = getUserJoinDate(entry.getProfile().getName());

            if (joinDate != null && Config.HANDLER.instance().playerListJoinBadge) {
                Instant now = Instant.now();
                long daysSince = Duration.between(joinDate, now).toDays();
                if (daysSince < 30) {
                    Formatting color = Formatting.YELLOW;
                    if (daysSince <= 14) color = Formatting.GOLD;
                    if (daysSince <= 3) color = Formatting.RED;
                    Text newIcon = Text.literal( " ℹ " + daysSince)
                            .setStyle(Style.EMPTY.withColor(color));
                    returnValue.append(newIcon);
                }
            }
            if (pronouns != null && Config.HANDLER.instance().playerListPronouns) {
                Text added = Text.literal(" (" + pronouns + ")")
                        .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
                returnValue.append(added);
            }
        }
        return returnValue;
    }
}
