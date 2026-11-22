package com.example.pixietubfix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(PixieTubFix.MOD_ID)
public class PixieTubFix {
    public static final String MOD_ID = "pixietubfix";

    /** Minimum fall distance reported to the Pressing Tub when a player lands in it. */
    public static final float MIN_STOMP_DISTANCE = 1.5f;

    private static final Map<UUID, Long> LAST_STOMP = new ConcurrentHashMap<>();
    private static final Set<UUID> AIRBORNE = ConcurrentHashMap.newKeySet();

    public PixieTubFix() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /** Soft check by registry ID: kaleidoscopetavern / kaleidoscope_tavern : *pressing_tub*. */
    public static boolean isPressingTub(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().startsWith("kaleidoscope") && id.getPath().contains("pressing_tub");
    }

    /** Called by the mixin so the tick fallback doesn't trigger a second stomp. */
    public static void markStomp(Player player) {
        LAST_STOMP.put(player.getUUID(), player.level().getGameTime());
    }

    /**
     * Fallback: vanilla skips Block#fallOn entirely when fallDistance is 0 (fall-immune origins).
     * Detect the air -> ground transition on the server and call fallOn manually.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }
        Player player = event.player;
        UUID id = player.getUUID();

        if (!player.onGround()) {
            AIRBORNE.add(id);
            return;
        }
        if (!AIRBORNE.remove(id)) {
            return; // wasn't airborne, so this isn't a landing
        }

        Level level = player.level();
        long now = level.getGameTime();
        Long last = LAST_STOMP.get(id);
        if (last != null && now - last <= 1) {
            return; // vanilla path (via the mixin) already handled this landing
        }

        for (BlockPos pos : new BlockPos[]{player.getOnPos(), player.blockPosition()}) {
            BlockState state = level.getBlockState(pos);
            if (isPressingTub(state)) {
                LAST_STOMP.put(id, now);
                state.getBlock().fallOn(level, state, pos, player, MIN_STOMP_DISTANCE);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        AIRBORNE.remove(id);
        LAST_STOMP.remove(id);
    }
}
