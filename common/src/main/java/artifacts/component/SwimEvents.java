package artifacts.component;

import artifacts.ability.ArtifactAbility;
import artifacts.ability.CollideWithFluidsAbility;
import artifacts.platform.PlatformServices;
import artifacts.registry.ModAbilities;
import artifacts.util.AbilityHelper;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.material.FluidState;

public class SwimEvents {

    public static void register() {
        TickEvent.PLAYER_PRE.register(SwimEvents::onPlayerTick);
    }

    private static void onPlayerTick(Player player) {
        SwimData swimData = PlatformServices.platformHelper.getSwimData(player);
        if (swimData != null) {
            if (player.isInWater() || player.isInLava() || player.fallDistance > 6) {
                if (!swimData.isWet()) {
                    swimData.setWet(true);
                }
            } else if (player.onGround() || player.getAbilities().flying) {
                swimData.setWet(false);
            }
        }
    }

    public static EventResult onPlayerSwim(Player player) {
        SwimData swimData = PlatformServices.platformHelper.getSwimData(player);
        if (swimData != null) {
            if (swimData.isSwimming()) {
                return EventResult.interruptTrue();
            } else if (AbilityHelper.hasAbilityActive(ModAbilities.SINKING.get(), player)) {
                return EventResult.interruptFalse();
            }
        }
        return EventResult.pass();
    }

    public static boolean onFluidCollision(LivingEntity player, FluidState fluidState) {
        SwimData swimData = PlatformServices.platformHelper.getSwimData(player);
        if (swimData == null || swimData.isWet() || swimData.isSwimming()) {
            return false;
        } else if (canSprintOnFluid(player, fluidState)) {
            dealLavaDamage(player, fluidState, ModAbilities.SPRINT_ON_FLUIDS.get());
            return true;
        } else if (canSneakOnFluid(player, fluidState)) {
            dealLavaDamage(player, fluidState, ModAbilities.SNEAK_ON_FLUIDS.get());
            return true;
        }
        return false;
    }

    private static boolean canSprintOnFluid(LivingEntity entity, FluidState fluidState) {
        return canCollideWithFluid(entity, fluidState, ModAbilities.SPRINT_ON_FLUIDS.get())
                && entity.isSprinting()
                && !entity.isUsingItem()
                && !entity.isCrouching();
    }

    private static boolean canSneakOnFluid(LivingEntity entity, FluidState fluidState) {
        return entity.isCrouching() && canCollideWithFluid(entity, fluidState, ModAbilities.SNEAK_ON_FLUIDS.get());
    }

    private static boolean canCollideWithFluid(LivingEntity entity, FluidState fluidState, ArtifactAbility.Type<CollideWithFluidsAbility> type) {
        return AbilityHelper.hasAbilityActive(type, entity, ability -> ability.tag().isEmpty() || fluidState.is(ability.tag().get()));
    }

    private static void dealLavaDamage(LivingEntity entity, FluidState fluidState, ArtifactAbility.Type<CollideWithFluidsAbility> type) {
        if (fluidState.is(FluidTags.LAVA) && !entity.fireImmune() && !EnchantmentHelper.hasFrostWalker(entity)
                && AbilityHelper.hasAbilityActive(
                        type, entity, ability ->
                        ability.dealLavaDamage().get() && (ability.tag().isEmpty() || fluidState.is(ability.tag().get()))
                )
        ) {
            entity.hurt(entity.damageSources().hotFloor(), 1);
        }
    }
}
