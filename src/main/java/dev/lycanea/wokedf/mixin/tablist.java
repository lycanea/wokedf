package dev.lycanea.wokedf.mixin;

import dev.lycanea.wokedf.client.WokedfClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class tablist {
    @Inject(method = "getPlayerName", at = @At(value = "RETURN"), cancellable = true)
    private void changeTablistName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(WokedfClient.updatePlayerlistEntry(entry, cir));
    }
}
