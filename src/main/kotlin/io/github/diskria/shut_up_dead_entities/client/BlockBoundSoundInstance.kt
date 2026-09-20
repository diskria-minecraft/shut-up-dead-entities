package io.github.diskria.shut_up_dead_entities.client

import io.github.diskria.shut_up_dead_entities.ShutUpDeadEntitiesMod
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource

class BlockBoundSoundInstance(
    event: SoundEvent,
    source: SoundSource,
    volume: Float,
    pitch: Float,
    x: Double,
    y: Double,
    z: Double,
    seed: Long,
    private val isAlive: () -> Boolean,
) : AbstractTickableSoundInstance(event, source, RandomSource.create(seed)) {

    private val initialVolume: Float = volume
    private val initialPitch: Float = pitch
    private var fadeOutTicks: Int = ShutUpDeadEntitiesMod.FADE_OUT_TICKS
    private var isFadingOut: Boolean = false

    init {
        this.volume = volume
        this.pitch = pitch
        this.x = x
        this.y = y
        this.z = z
    }

    override fun tick() {
        if (!isFadingOut && !isAlive()) {
            isFadingOut = true
        }
        if (isFadingOut) {
            if (fadeOutTicks > 0) {
                val progress = fadeOutTicks.toFloat() / ShutUpDeadEntitiesMod.FADE_OUT_TICKS
                volume = initialVolume * progress
                pitch = initialPitch * progress
                fadeOutTicks--
            } else {
                stop()
            }
        }
    }
}
