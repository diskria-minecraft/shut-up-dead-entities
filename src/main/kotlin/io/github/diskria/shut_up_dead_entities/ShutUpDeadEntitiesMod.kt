package io.github.diskria.shut_up_dead_entities

import net.fabricmc.api.ClientModInitializer

class ShutUpDeadEntitiesMod : ClientModInitializer {

    override fun onInitializeClient() {}

    companion object {
        const val FADE_OUT_TICKS = 20
    }
}
