package io.github.diskria.shut_up_dead_entities.client

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.Origin
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundEngine.PlayResult
import net.minecraft.client.sounds.SoundManager
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import org.spongepowered.asm.mixin.injection.At

@KMixin(ClientLevel::class)
abstract class ClientLevelMixin(@Origin private val level: ClientLevel) {

    @WrapOperation(
        method = ["playSound"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)" +
                $$"Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"
        )]
    )
    fun interceptPlay(
        soundManager: SoundManager,
        instance: SoundInstance,
        original: Operation<PlayResult>,
        @Local(name = ["x"], argsOnly = true) x: Double,
        @Local(name = ["y"], argsOnly = true) y: Double,
        @Local(name = ["z"], argsOnly = true) z: Double,
        @Local(name = ["sound"], argsOnly = true) sound: SoundEvent,
        @Local(name = ["source"], argsOnly = true) source: SoundSource,
        @Local(name = ["volume"], argsOnly = true) volume: Float,
        @Local(name = ["pitch"], argsOnly = true) pitch: Float,
        @Local(name = ["seed"], argsOnly = true) seed: Long,
    ): PlayResult {
        val entityBound = boundSoundToEntityOrNull(x, y, z, sound, source, volume, pitch, seed)
        return original.call(soundManager, entityBound ?: instance)
    }

    @WrapOperation(
        method = ["playSound"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundManager;playDelayed(Lnet/minecraft/client/resources/sounds/SoundInstance;I)V"
        )]
    )
    fun interceptPlayDelayed(
        soundManager: SoundManager,
        instance: SoundInstance,
        delay: Int,
        original: Operation<Void>,
        @Local(name = ["x"], argsOnly = true) x: Double,
        @Local(name = ["y"], argsOnly = true) y: Double,
        @Local(name = ["z"], argsOnly = true) z: Double,
        @Local(name = ["sound"], argsOnly = true) sound: SoundEvent,
        @Local(name = ["source"], argsOnly = true) source: SoundSource,
        @Local(name = ["volume"], argsOnly = true) volume: Float,
        @Local(name = ["pitch"], argsOnly = true) pitch: Float,
        @Local(name = ["seed"], argsOnly = true) seed: Long,
    ) {
        val entityBound = boundSoundToEntityOrNull(x, y, z, sound, source, volume, pitch, seed)
        original.call(soundManager, entityBound ?: instance, delay)
    }

    private fun boundSoundToEntityOrNull(
        x: Double,
        y: Double,
        z: Double,
        sound: SoundEvent,
        source: SoundSource,
        volume: Float,
        pitch: Float,
        seed: Long,
    ): EntityBoundSoundInstance? {
        val soundId = sound.location
        val soundPath = soundId.path
        if (!soundPath.startsWith("entity.")) return null
        val pathPart = soundPath.substringAfter('.').substringBefore('.').ifEmpty { return null }
        val sourceId = Identifier.fromNamespaceAndPath(soundId.namespace, pathPart)
        val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(sourceId).orElse(null) ?: return null
        val aliveEntity = level.findAliveEntityAt(entityType, x, y, z) {
            sound != it.deathSound && sound != it.getHurtSound(it.lastDamageSource ?: level.damageSources().generic())
        }
        val targetEntity = aliveEntity ?: return null
        return EntityBoundSoundInstance(sound, source, volume, pitch, targetEntity, seed)
    }
}

private inline fun <T : Entity> Level.findAliveEntityAt(
    type: EntityTypeTest<Entity, T>,
    x: Double,
    y: Double,
    z: Double,
    crossinline predicate: (LivingEntity) -> Boolean,
): LivingEntity? {
    val result = ArrayList<T>(1)
    getEntities(
        /* type = */ type,
        /* bb = */ AABB(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0),
        /* selector = */ { it is LivingEntity && it.isAlive && predicate(it) },
        /* output = */ result,
        /* maxResults = */ 1,
    )
    return result.firstOrNull() as? LivingEntity
}
