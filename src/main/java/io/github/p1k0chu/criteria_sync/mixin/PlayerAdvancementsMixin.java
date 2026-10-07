package io.github.p1k0chu.criteria_sync.mixin;

import io.github.p1k0chu.criteria_sync.AwardCriterion;
import io.github.p1k0chu.criteria_sync.CriteriaSync;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin implements AwardCriterion {
    @Shadow
    @Final
    private PlayerList playerList;

    @Shadow
    private ServerPlayer player;

    @Shadow
    protected abstract void markForVisibilityUpdate(AdvancementHolder advancement);

    @Shadow
    public abstract AdvancementProgress getOrStartProgress(AdvancementHolder advancement);

    @Shadow
    protected abstract void unregisterListeners(AdvancementHolder holder);

    @Shadow
    @Final
    private Set<AdvancementHolder> progressChanged;

    @Inject(method = "award", at = @At("RETURN"))
    void award(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (CriteriaSync.isAdvancementBlocked(holder.toString()) || holder.value().display().isEmpty()) {
            return;
        }

        if (cir.getReturnValue()) {
            int thisId = this.player.getId();
            for (var player : this.playerList.getPlayers()) {
                if (player.getId() != thisId) {
                    ((AwardCriterion) player.getAdvancements()).criteria_sync$awardSynced(holder, criterion);
                }
            }
        }
    }

    @Override
    public void criteria_sync$awardSynced(AdvancementHolder adv, String criterion) {
        AdvancementProgress progress = getOrStartProgress(adv);
        boolean wasDone = progress.isDone();

        if (progress.grantProgress(criterion)) {
            unregisterListeners(adv);
            progressChanged.add(adv);

            if (!wasDone && progress.isDone()) {
                markForVisibilityUpdate(adv);
            }
        }
    }
}
