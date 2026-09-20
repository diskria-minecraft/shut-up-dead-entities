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
abstract class EntityBoundSoundInstanceMixin(@Origin private val sound: EntityBoundSoundInstance) {

    private var initialVolume: Float = sound.volume
    private var initialPitch: Float = sound.pitch
    private var fadeOutTicks: Int = ShutUpDeadEntitiesMod.FADE_OUT_TICKS
    private var isFadingOut: Boolean = false

    @WrapOperation(
        method = ["tick"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isRemoved()Z")]
    )
    fun interceptEntityRemovedCheck(entity: Entity, original: Operation<Boolean>): Boolean {
        if (entity !is LivingEntity) {
            return original.call(entity)
        }
        if (!isFadingOut && entity.isDeadOrDying) {
            isFadingOut = true
        }
        if (isFadingOut) {
            if (fadeOutTicks > 0) {
                val progress = fadeOutTicks.toFloat() / ShutUpDeadEntitiesMod.FADE_OUT_TICKS
                sound.volume = initialVolume * progress
                sound.pitch = initialPitch * progress
                fadeOutTicks--
                return false
            } else {
                return true
            }
        }
        return false
    }
}
