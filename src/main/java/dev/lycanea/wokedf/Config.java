package dev.lycanea.wokedf;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.*;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.util.Identifier;

public class Config {
    // Create and configure the handler: file path, format (JSON5), pretty print, etc.
    public static final ConfigClassHandler<Config> HANDLER =
            ConfigClassHandler.createBuilder(Config.class)
                    .id(Identifier.of("wokedf", "config"))
                    .serializer(config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(YACLPlatform.getConfigDir().resolve("wokedf.json5"))
                            .setJson5(true)
                            .build())
                    .build();

    @TickBox
    @AutoGen(category = "Player List")
    @SerialEntry
    public boolean playerListPronouns = true;

    @TickBox
    @AutoGen(category = "Player List")
    @SerialEntry
    public boolean playerListJoinBadge = true;

    // implement later
//    @TickBox
//    @AutoGen(category = "Chat")
//    @SerialEntry
//    public boolean chatPronouns = true;
//
//    @TickBox
//    @AutoGen(category = "Chat")
//    @SerialEntry
//    public boolean chatJoinBadge = true;
//
//    @TickBox
//    @AutoGen(category = "Ingame")
//    @SerialEntry
//    public boolean ingamePronouns = true;
//
//    @TickBox
//    @AutoGen(category = "Ingame")
//    @SerialEntry
//    public boolean ingameJoinBadge = true;
}
