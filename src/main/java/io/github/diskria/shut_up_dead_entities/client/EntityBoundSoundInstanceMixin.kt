package io.github.diskria.shut_up_dead_entities.client

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import io.github.diskria.lapis.annotations.KMixin
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@KMixin(EntityBoundSoundInstance::class)
class EntityBoundSoundInstanceMixin {

    private var fadeOutTicks = FADE_OUT_TICKS
    private var initialVolume: Float? = null
    private var initialPitch: Float? = null

    @Inject(method = ["<init>"], at = [At("RETURN")])
    fun initReturn(
        event: SoundEvent, source: SoundSource, volume: Float, pitch: Float, entity: Entity, seed: Long,
        callback: CallbackInfo,
    ) {
        initialVolume = volume
        initialPitch = pitch
    }

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
            val maxVolume = initialVolume ?: return
            val maxPitch = initialPitch ?: return
            val progress = fadeOutTicks.toFloat() / FADE_OUT_TICKS
            volume = maxVolume * progress
            pitch = maxPitch * progress
            fadeOutTicks--
        } else {
            original.call(instance)
        }
    }

    companion object {
        private const val FADE_OUT_TICKS = 20
    }
}
