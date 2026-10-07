package io.github.p1k0chu.criteria_sync.mixin;

import io.github.p1k0chu.criteria_sync.AwardCriterion;
import io.github.p1k0chu.criteria_sync.CriteriaSync;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Shadow
    @Final
    private List<ServerPlayer> players;

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        int playerId = player.getId();
        var advs = player.getAdvancements();

        for (var advHolder : server.getAdvancements().getAllAdvancements()) {
            if (CriteriaSync.isAdvancementBlocked(advHolder.toString()) || advHolder.value().display().isEmpty()) {
                continue;
            }
            var progress = advs.getOrStartProgress(advHolder);

            for (var otherPlayer : players) {
                if (otherPlayer.getId() == playerId) continue;
                var otherAdvs = otherPlayer.getAdvancements();
                var otherCompleted = otherAdvs.getOrStartProgress(advHolder).getCompletedCriteria();

                for (String criterion : progress.getCompletedCriteria()) {
                    ((AwardCriterion) otherAdvs).criteria_sync$awardSynced(advHolder, criterion);
                }
                for (String criterion : otherCompleted) {
                    ((AwardCriterion) advs).criteria_sync$awardSynced(advHolder, criterion);
                }
            }
        }
    }
}
