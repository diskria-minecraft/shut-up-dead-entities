package io.github.diskria.shut_up_dead_entities.client

import io.github.diskria.shut_up_dead_entities.ShutUpDeadEntitiesMod
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.TickableSoundInstance

class PlayerBoundPortalTriggerSoundInstance(
    private val player: LocalPlayer,
    original: SimpleSoundInstance,
) : SimpleSoundInstance(
    /* location = */ original.identifier,
    /* source = */ original.source,
    /* volume = */ original.volume,
    /* pitch = */ original.pitch,
    /* random = */ original.random,
    /* looping = */ original.isLooping,
    /* delay = */ original.delay,
    /* attenuation = */ original.attenuation,
    /* x = */ original.x,
    /* y = */ original.y,
    /* z = */ original.z,
    /* relative = */ original.isRelative,
), TickableSoundInstance {

    private var initialVolume: Float = volume
    private var initialPitch: Float = pitch
    private var fadeOutTicks = ShutUpDeadEntitiesMod.FADE_OUT_TICKS
    private var stopped = false

    override fun isStopped(): Boolean = stopped

    override fun tick() {
        if (player.portalProcess?.isInsidePortalThisTick == true) {
            return
        }
        if (fadeOutTicks >= 0) {
            val progress = fadeOutTicks.toFloat() / ShutUpDeadEntitiesMod.FADE_OUT_TICKS
            volume = initialVolume * progress
            pitch = initialPitch * progress
            fadeOutTicks--
        } else {
            stopped = true
        }
    }
}
