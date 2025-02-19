package com.mucheng.mucute.client.game.module.visual

import com.mucheng.mucute.client.game.InterceptablePacket
import com.mucheng.mucute.client.game.Module
import com.mucheng.mucute.client.game.ModuleCategory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket

class FreeCameraModule : Module("free_camera", ModuleCategory.Visual) {

    private var originalPosition: Vector3f? = null

    private val enableFlyNoClipPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(
                arrayOf(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS,
                    Ability.MAY_FLY,
                    Ability.FLY_SPEED,
                    Ability.WALK_SPEED,
                    Ability.NO_CLIP,
                    Ability.OPERATOR_COMMANDS
                )
            )
            walkSpeed = 0.1f
            flySpeed = 0.15f
        })
    }

    private val disableFlyNoClipPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(
                arrayOf(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS,
                    Ability.OPERATOR_COMMANDS
                )
            )
            walkSpeed = 0.1f
        })
    }

    private var isFlyNoClipEnabled = false

    @OptIn(DelicateCoroutinesApi::class)
    override fun onEnabled() {
        super.onEnabled()
        if (isSessionCreated) {
            GlobalScope.launch {
                for (i in 5 downTo 1) {
                    val countdownMessage = "§l§b[MuCute] §r§7FreeCam will enable in §e$i §7seconds"
                    sendCountdownMessage(countdownMessage)
                    delay(1000)
                }

                // Store original position when enabled after countdown
                originalPosition = Vector3f.from(
                    session.localPlayer.posX,
                    session.localPlayer.posY,
                    session.localPlayer.posZ
                )
                enableFlyNoClipPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
                session.clientBound(enableFlyNoClipPacket)
                isFlyNoClipEnabled = true
            }
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated && originalPosition != null) {
            // Return to original position when disabled
            val motionPacket = SetEntityMotionPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                motion = originalPosition
            }
            session.clientBound(motionPacket)
            originalPosition = null

            disableFlyNoClipPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            session.clientBound(disableFlyNoClipPacket)
            isFlyNoClipEnabled = false
        }
    }

    private fun sendCountdownMessage(message: String) {
        val textPacket = TextPacket().apply {
            type = TextPacket.Type.RAW
            isNeedsTranslation = false
            this.message = message
            xuid = ""
            sourceName = ""
        }

        session.clientBound(textPacket)
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket && isEnabled) {
            interceptablePacket.intercept()
        }
    }
}