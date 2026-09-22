package io.github.diskria.shut_up_dead_entities.client

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.lapis.annotations.Env
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.Origin
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundEngine.PlayResult
import net.minecraft.client.sounds.SoundManager
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BubbleColumnBlock
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.RespawnAnchorBlock
import net.minecraft.world.level.block.entity.BeaconBlockEntity
import net.minecraft.world.level.block.entity.ConduitBlockEntity
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.level.material.WaterFluid
import net.minecraft.world.phys.AABB
import org.spongepowered.asm.mixin.injection.At

@KMixin(ClientLevel::class, Env.Client)
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
        val boundSound = boundSoundToEntityOrNull(x, y, z, sound, source, volume, pitch, seed)
            ?: boundSoundToBlockOrNull(x, y, z, sound, source, volume, pitch, seed)
        return original.call(soundManager, boundSound ?: instance)
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
        val boundSound = boundSoundToEntityOrNull(x, y, z, sound, source, volume, pitch, seed)
            ?: boundSoundToBlockOrNull(x, y, z, sound, source, volume, pitch, seed)
        original.call(soundManager, boundSound ?: instance, delay)
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

    private fun boundSoundToBlockOrNull(
        x: Double,
        y: Double,
        z: Double,
        sound: SoundEvent,
        source: SoundSource,
        volume: Float,
        pitch: Float,
        seed: Long,
    ): BlockBoundSoundInstance? {
        val soundPath = sound.location.path
        val blockStatePredicate = SOUND_TO_BLOCK_PREDICATES[soundPath] ?: return null
        val pos = BlockPos.containing(x, y, z)
        val isAlivePredicate: () -> Boolean = { blockStatePredicate(level, pos) }
        if (!isAlivePredicate()) return null
        return BlockBoundSoundInstance(
            event = sound,
            source = source,
            volume = volume,
            pitch = pitch,
            x = x,
            y = y,
            z = z,
            seed = seed,
            isAlive = isAlivePredicate,
        )
    }

    companion object {
        private val SOUND_TO_BLOCK_PREDICATES: Map<String, (Level, BlockPos) -> Boolean> = mapOf(
            "block.water.ambient" to { level, pos ->
                val state = level.getBlockState(pos)
                val fluidState = state.fluidState
                fluidState.`is`(FluidTags.WATER) && !fluidState.isSource && !fluidState.getValue(WaterFluid.FALLING)
            },
            "block.fire.ambient" to { level, pos ->
                level.getBlockState(pos).`is`(BlockTags.FIRE)
            },
            "block.campfire.crackle" to { level, pos ->
                CampfireBlock.isLitCampfire(level.getBlockState(pos))
            },
            "block.lava.ambient" to { level, pos ->
                val state = level.getBlockState(pos)
                if (state.fluidState.`is`(FluidTags.LAVA)) {
                    val aboveState = level.getBlockState(pos.above())
                    aboveState.isAir || !aboveState.isSolidRender
                } else false
            },
            "block.portal.ambient" to { level, pos ->
                level.getBlockState(pos).`is`(BlockTags.PORTALS)
            },
            "block.beacon.activate" to { level, pos ->
                level.getBlockEntity(pos) is BeaconBlockEntity
            },
            "block.beacon.ambient" to { level, pos ->
                (level.getBlockEntity(pos) as? BeaconBlockEntity)?.beamSections?.isNotEmpty() == true
            },
            "block.bubble_column.upwards_ambient" to { level, pos ->
                level.getBlockState(pos).block is BubbleColumnBlock
            },
            "block.bubble_column.whirlpool_ambient" to { level, pos ->
                level.getBlockState(pos).block is BubbleColumnBlock
            },
            "block.conduit.activate" to { level, pos ->
                level.getBlockEntity(pos) is ConduitBlockEntity
            },
            "block.conduit.ambient" to { level, pos ->
                (level.getBlockEntity(pos) as? ConduitBlockEntity)?.isActive == true
            },
            "block.conduit.ambient.short" to { level, pos ->
                (level.getBlockEntity(pos) as? ConduitBlockEntity)?.isActive == true
            },
            "block.respawn_anchor.charge" to { level, pos ->
                level.getBlockState(pos).block is RespawnAnchorBlock
            },
            "block.respawn_anchor.ambient" to { level, pos ->
                val state = level.getBlockState(pos)
                state.block is RespawnAnchorBlock && state.getValue(RespawnAnchorBlock.CHARGE) > 0
            },
        )
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
