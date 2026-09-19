package io.github.diskria.shut_up_dead_entities.client

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import io.github.diskria.lapis.annotations.KMixin
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvent
import org.spongepowered.asm.mixin.injection.At

@KMixin(LocalPlayer::class)
class LocalPlayerMixin {

    @WrapOperation(
        method = ["handlePortalTransitionEffect"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;" +
                "forLocalAmbience(Lnet/minecraft/sounds/SoundEvent;FF)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"
        )]
    )
    fun LocalPlayer.interceptPortalTriggerSoundInstance(
        sound: SoundEvent, pitch: Float, volume: Float, original: Operation<SimpleSoundInstance>,
    ): SimpleSoundInstance = PortalTriggerSoundInstance(this, sound, pitch, volume)
}
