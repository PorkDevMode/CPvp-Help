package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EzAnchor extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Integer> detonateSlot = sgGeneral.add(new IntSetting.Builder()
        .name("detonate-slot")
        .description("Hotbar slot to switch to for detonation")
        .defaultValue(1)
        .range(1, 9)
        .build()
    );

    private final Setting<Integer> minSwapDelay = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-swap-delay")
        .description("Minimum delay after swapping to glowstone in ticks")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> maxSwapDelay = sgGeneral.add(new IntSetting.Builder()
        .name("maximum-swap-delay")
        .description("Maximum delay after swapping to glowstone in ticks")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> minChargeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-charge-delay")
        .description("Minimum delay after charging the anchor in ticks")
        .defaultValue(1)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> maxChargeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("maximum-charge-delay")
        .description("Maximum delay after charging the anchor in ticks")
        .defaultValue(1)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> minSwitchDelay = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-switch-delay")
        .description("Minimum delay after switching to detonate slot in ticks")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> maxSwitchDelay = sgGeneral.add(new IntSetting.Builder()
        .name("maximum-switch-delay")
        .description("Maximum delay after switching to detonate slot in ticks")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> minSwitchBackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-switch-back-delay")
        .description("Maximum delay after switching to detonate slot in ticks")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> maxSwitchBackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("maximum-switch-back-delay")
        .description("Maximum delay after switching to detonate slot in ticks")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Map<BlockPos, AnchorState> _anchor_timers = new HashMap<>();
    private final Random _random = new Random();

    public EzAnchor() {
        super(AddonTemplate.CATEGORY, "ez-anchor", "Automatically charges and detonates respawn anchors");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        _anchor_timers.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            AnchorState state = entry.getValue();

            if (mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR && state.stage != Stage.SWITCHING_BACK) {
                return true;
            }

            if (state.ticks_remaining > 0) {
                state.ticks_remaining--;
                return false;
            }

            if (state.stage == Stage.SWITCHING_BACK) {
                FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
                InvUtils.swap(anchor.slot(), false);
                return true;
            }

            if (state.stage == Stage.SWAPPING) {
                FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
                if (!glowstone.found()) return true;

                state.original_slot = glowstone.slot();
                InvUtils.swap(glowstone.slot(), false);

                state.stage = Stage.CHARGING;
                int delay = GetRandomDelay(minSwapDelay.get(), maxSwapDelay.get());
                state.ticks_remaining = delay;
                return false;
            }

            if (state.stage == Stage.CHARGING) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(pos.toCenterPos(), mc.player.getHorizontalFacing(), pos, false));

                state.stage = Stage.SWITCHING;
                int delay = GetRandomDelay(minChargeDelay.get(), maxChargeDelay.get());
                state.ticks_remaining = delay;
                return false;
            }

            if (state.stage == Stage.SWITCHING) {
                int detonate_slot_index = detonateSlot.get() - 1;
                InvUtils.swap(detonate_slot_index, false);

                state.stage = Stage.DETONATING;
                int delay = GetRandomDelay(minSwitchDelay.get(), maxSwitchDelay.get());
                state.ticks_remaining = delay;
                return false;
            }

            if (state.stage == Stage.DETONATING) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(pos.toCenterPos(), mc.player.getHorizontalFacing(), pos, false));

                state.stage = Stage.SWITCHING_BACK;
                int delay = GetRandomDelay(minSwitchBackDelay.get(), maxSwitchBackDelay.get());
                state.ticks_remaining = delay;
                return false;
            }

            return true;
        });

        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult block_hit = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = block_hit.getBlockPos();

        if (mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR) return;
        if (_anchor_timers.containsKey(pos)) return;

        FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
        if (!glowstone.found()) return;

        _anchor_timers.put(pos, new AnchorState());
    }

    private int GetRandomDelay(int min, int max) {
        return min + _random.nextInt(Math.max(1, max - min + 1));
    }

    private enum Stage {
        SWAPPING,
        CHARGING,
        SWITCHING,
        DETONATING,
        SWITCHING_BACK
    }

    private static class AnchorState {
        Stage stage = Stage.SWAPPING;
        int ticks_remaining = 0;
        int original_slot = -1;
    }
}
