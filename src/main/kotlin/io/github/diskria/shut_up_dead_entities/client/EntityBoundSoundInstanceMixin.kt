package io.github.diskria.shut_up_dead_entities.client

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import io.github.diskria.lapis.annotations.InitStrategy
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.Origin
import io.github.diskria.shut_up_dead_entities.ShutUpDeadEntitiesMod
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.injection.At

@KMixin(EntityBoundSoundInstance::class, initStrategy = InitStrategy.Eager)
abstract class EntityBoundSoundInstanceMixin(@Origin private val instance: EntityBoundSoundInstance) {

    private var initialVolume: Float = instance.volume
    private var initialPitch: Float = instance.pitch
    private var fadeOutTicks: Int = ShutUpDeadEntitiesMod.FADE_OUT_TICKS

    @WrapOperation(
        method = ["tick"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isRemoved()Z")]
    )
    fun interceptEntityRemovedCheck(instance: Entity, original: Operation<Boolean>): Boolean =
        if (instance is LivingEntity) instance.isDeadOrDying
        else original.call(instance)

    @WrapOperation(
        method = ["tick"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/EntityBoundSoundInstance;stop()V")]
    )
    fun EntityBoundSoundInstance.interceptStopOnEntityRemove(
        instance: EntityBoundSoundInstance, original: Operation<Void>
    ) {
        if (fadeOutTicks >= 0) {
            val progress = fadeOutTicks.toFloat() / ShutUpDeadEntitiesMod.FADE_OUT_TICKS
            volume = initialVolume * progress
            pitch = initialPitch * progress
            fadeOutTicks--
        } else {
            original.call(instance)
        }
    }
}
