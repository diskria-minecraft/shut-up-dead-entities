package io.github.diskria.shut_up_dead_entities.client

import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.CrossbowAttackMob
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.function.Predicate
import javax.lang.model.element.Modifier

@KMixin(ClientLevel::class)
abstract class ClientLevelMixin {

    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val minecraft: Minecraft

    @Inject(method = ["playSound"], at = [At("HEAD")], cancellable = true)
    fun ClientLevel.playSoundHead(
        x: Double, y: Double, z: Double,
        sound: SoundEvent, source: SoundSource, volume: Float, pitch: Float, distanceDelay: Boolean, seed: Long,
        callback: CallbackInfo,
    ) {
        val player = minecraft.player ?: return
        val level = minecraft.level ?: return

        val soundId = sound.location
        val soundPath = soundId.path

        val isItemSound = soundPath.startsWith("item.")
        val isEntitySound = soundPath.startsWith("entity.")
        if (!isItemSound && !isEntitySound) return

        val pathPart = soundPath.substringAfter('.').substringBefore('.')
        if (pathPart.isEmpty() || pathPart == soundPath) return

        val sourceId = Identifier.fromNamespaceAndPath(soundId.namespace, pathPart)

        val aliveEntity = if (isItemSound) {
            val item = BuiltInRegistries.ITEM.getOptional(sourceId).orElse(null)
            if (item == Items.CROSSBOW) {
                val entityType = EntityTypeTest.forClass(Entity::class.java)
                level.findAliveEntityAt(entityType, x, y, z) { it is CrossbowAttackMob }
            } else null
        } else {
            val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(sourceId).orElse(null)
            if (entityType != null) {
                level.findAliveEntityAt(entityType, x, y, z) { sound != it.deathSound }
            } else null
        }
        val targetEntity = aliveEntity ?: return
        val soundHolder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound)
        playSeededSound(player, targetEntity, soundHolder, source, volume, pitch, seed)
        callback.cancel()
    }
}

private fun Level.findAliveEntityAt(
    type: EntityTypeTest<Entity, *>,
    x: Double, y: Double, z: Double,
    predicate: (LivingEntity) -> Boolean,
): Entity? {
    val result = ArrayList<Entity>(1)
    val selector = Predicate<Entity> { it is LivingEntity && it.isAlive && predicate(it) }
    getEntities(type, AABB(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0), selector, result, 1)
    return result.firstOrNull()
}
