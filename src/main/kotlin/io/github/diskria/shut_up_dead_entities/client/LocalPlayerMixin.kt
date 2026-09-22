package io.github.diskria.shut_up_dead_entities.client

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import io.github.diskria.lapis.annotations.Env
import io.github.diskria.lapis.annotations.KMixin
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundEngine.PlayResult
import net.minecraft.client.sounds.SoundManager
import org.spongepowered.asm.mixin.injection.At

@KMixin(LocalPlayer::class, Env.Client)
class LocalPlayerMixin {

    @WrapOperation(
        method = ["handlePortalTransitionEffect"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)" +
                $$"Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"
        )]
    )
    fun LocalPlayer.interceptPortalTriggerSoundInstance(
        soundManager: SoundManager, instance: SoundInstance, original: Operation<PlayResult>,
    ): PlayResult {
        val playerBound = if (instance is SimpleSoundInstance) {
            PlayerBoundPortalTriggerSoundInstance(this, instance)
        } else null
        return original.call(soundManager, playerBound ?: instance)
    }
}
