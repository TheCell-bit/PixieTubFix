package com.example.pixietubfix.mixin;

import com.example.pixietubfix.PixieTubFix;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Raises the fall distance for players landing on Kaleidoscope Tavern's Pressing Tub.
 * Soft-checked by registry name, so there is no compile-time dependency on any other mod.
 */
@Mixin(Block.class)
public abstract class PixieTubMixin {

    @ModifyVariable(
            method = "fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float pixietubfix$boostFallDistance(float fallDistance, Level level, BlockState state,
                                                BlockPos pos, Entity entity) {
        if (entity instanceof Player player && PixieTubFix.isPressingTub(state)) {
            PixieTubFix.markStomp(player);
            return Math.max(fallDistance, PixieTubFix.MIN_STOMP_DISTANCE);
        }
        return fallDistance;
    }
}
