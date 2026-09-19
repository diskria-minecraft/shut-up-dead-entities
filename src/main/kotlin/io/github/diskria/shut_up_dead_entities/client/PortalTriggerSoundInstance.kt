package io.github.diskria.shut_up_dead_entities.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.resources.sounds.TickableSoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

@Environment(EnvType.CLIENT)
class PortalTriggerSoundInstance(
    private val player: LocalPlayer,
    sound: SoundEvent,
    private val initialPitch: Float,
    private val initialVolume: Float
) : SimpleSoundInstance(
    /* location = */ sound.location,
    /* source = */ SoundSource.AMBIENT,
    /* volume = */ initialVolume,
    /* pitch = */ initialPitch,
    /* random = */ SoundInstance.createUnseededRandom(),
    /* looping = */ false,
    /* delay = */ 0,
    /* attenuation = */ SoundInstance.Attenuation.NONE,
    /* x = */ 0.0,
    /* y = */ 0.0,
    /* z = */ 0.0,
    /* relative = */ true,
), TickableSoundInstance {

    private var fadeOutTicks = FADE_OUT_TICKS
    private var stopped = false

    override fun isStopped(): Boolean = stopped

    override fun tick() {
        if (player.portalProcess?.isInsidePortalThisTick == true) {
            return
        }
        if (fadeOutTicks >= 0) {
            val progress = fadeOutTicks.toFloat() / FADE_OUT_TICKS
            volume = initialVolume * progress
            pitch = initialPitch * progress
            fadeOutTicks--
        } else {
            stopped = true
        }
    }

    companion object {
        private const val FADE_OUT_TICKS = 20
    }
}
