package dev.lycanea.wokedf.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WokedfClient implements ClientModInitializer {

    public static final String MOD_ID = "wokedf";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Map<String, String> userPronouns = new HashMap<>();
    private static final LinkedHashSet<String> queue = new LinkedHashSet<>();
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register((MinecraftClient client) -> {
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
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

                    if (!userPronouns.containsKey(username)) {
                        userPronouns.put(username, pronouns);
                        return false;
                    }
                    userPronouns.put(username, pronouns);
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
                if (userPronouns.containsKey(username)) return;
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

            queue.add(username);
        }
        return null;
    }

    public static Text updatePlayerlistEntry(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (onDF()) {
            String pronouns = getUserPronouns(entry.getProfile().getName());
            if (pronouns != null) {
                Text added = Text.literal(" (" + pronouns + ")")
                        .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
                return cir.getReturnValue().copy().append(added);
            }
        }
        return cir.getReturnValue();
    }
}
