package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ClickCrystal extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Integer> minDelay = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-delay")
        .description("The minimum amount of delay to detonate the crystal after placing in ticks")
        .defaultValue(1)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> maxDelay = sgGeneral.add(new IntSetting.Builder()
        .name("maximum-delay")
        .description("The maximum amount of delay to detonate the crystal after placing in ticks")
        .defaultValue(1)
        .range(0, 20)
        .build()
    );

    private final Map<Integer, Integer> _crystal_timers = new HashMap<>();
    private final Random _random = new Random();

    public ClickCrystal() {
        super(AddonTemplate.CATEGORY, "click-crystal", "Automatically detonates crystals after placing them");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.crosshairTarget == null) return;

        if (mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult entity_hit = (EntityHitResult) mc.crosshairTarget;
        if (!(entity_hit.getEntity() instanceof EndCrystalEntity crystal)) return;

        double vertical_distance = crystal.getY() - mc.player.getY();
        if (vertical_distance < 1.0) return;

        int crystal_id = crystal.getId();

        if (!_crystal_timers.containsKey(crystal_id)) {
            int delay = minDelay.get() + _random.nextInt(Math.max(1, maxDelay.get() - minDelay.get() + 1));
            _crystal_timers.put(crystal_id, delay);
            return;
        }

        int remaining_ticks = _crystal_timers.get(crystal_id);
        if (remaining_ticks > 0) {
            _crystal_timers.put(crystal_id, remaining_ticks - 1);
            return;
        }

        mc.interactionManager.attackEntity(mc.player, crystal);
        mc.player.swingHand(mc.player.getActiveHand());
        _crystal_timers.remove(crystal_id);
    }
}
