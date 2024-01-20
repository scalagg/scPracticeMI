package gg.tropic.practice.player

import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.parties.service.PartyService
import gg.tropic.practice.commands.TournamentCommand
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.player.hotbar.LobbyHotbarService
import gg.tropic.practice.queue.QueueEntry
import gg.tropic.practice.queue.QueueService
import gg.tropic.practice.queue.QueueState
import gg.tropic.practice.services.TournamentManagerService
import net.evilblock.cubed.ScalaCommonsSpigot
import net.evilblock.cubed.serializers.Serializers
import org.bukkit.Bukkit
import java.util.UUID

data class LobbyPlayer(
    val uniqueId: UUID
)
{
    var maintainStateTimeout = -1L

    val stateUpdateLock = Any()
    var state: PlayerState = PlayerState.None
        set(value)
        {
            field = value

            if (field != PlayerState.None)
            {
                val bukkit = Bukkit.getPlayer(uniqueId)!!
                LobbyHotbarService
                    .get(value)
                    .applyToPlayer(bukkit)
            }
        }

    private var queueState: QueueState? = null
    private var queueEntry: QueueEntry? = null

    fun buildQueueID() = queueState?.let {
        "${it.kitId}:${it.queueType.name}:${it.teamSize}v${it.teamSize}"
    }

    fun queueState() = queueState!!
    fun queueEntry() = queueEntry!!

    fun inQueue() = queueState != null
    fun validateQueueEntry() = queueEntry != null
    fun queuedForTime() = System.currentTimeMillis() - queueState!!.joined
    fun queuedForKit() = KitService.cached().kits[queueState!!.kitId]
    fun queuedForType() = queueState!!.queueType
    fun queuedForTeamSize() = queueState!!.teamSize

    var hasSyncedInitialQueueState = false
    fun syncQueueStateIfRequired()
    {
        if (!hasSyncedInitialQueueState)
        {
            return
        }

        syncQueueState()
    }

    fun syncQueueState()
    {
        queueState = ScalaCommonsSpigot.instance.kvConnection
            .sync()
            .hget(
                "tropicpractice:queue-states",
                uniqueId.toString()
            )
            ?.let {
                Serializers
                    .gson
                    .fromJson(it, QueueState::class.java)
            }

        val userInParty = PartyService.findPartyByUniqueId(uniqueId) != null
        val userInTournament = TournamentManagerService.isInTournament(uniqueId)
        val newState = when (true)
        {
            userInParty -> PlayerState.InParty
            userInTournament -> PlayerState.InTournament
            (queueState != null) -> PlayerState.InQueue
            else -> PlayerState.Idle
        }

        // newState is just a wrapped null check
        // for queueState. If it passes, sync the entry
        if (newState == PlayerState.InQueue)
        {
            syncQueueEntry()
        } else
        {
            if (
                newState == PlayerState.InParty &&
                state != PlayerState.InParty
                )
            {
                val player = Bukkit.getPlayer(uniqueId)

                when (state)
                {
                    PlayerState.InQueue -> QueueService
                        .leaveQueue(
                            player, true
                        )
                    PlayerState.InTournament -> TournamentCommand.onLeave(
                        ScalaPlayer(player, LobbyPlayerService.audiences, LobbyPlayerService.plugin)
                    )
                    else -> { }
                }
            } else
            {
                // determined that player is idle, so we remove their queue entry
                queueEntry = null
            }
        }

        // keep current state until the server processes our queue join
        // request and actually updates the queue entry
        if (maintainStateTimeout > System.currentTimeMillis())
        {
            // if we notice that the server pushed whatever state we're expecting (whether it's a queue
            // join/leave that we set temporarily), we'll remove the timeout and continue with our day.
            if (newState == state)
            {
                maintainStateTimeout = -1L
            } else
            {
                return
            }
        }

        // don't try to acquire lock if we don't need to
        if (newState != state)
        {
            synchronized(stateUpdateLock) {
                // check if the state has changed before we acquired the lock
                if (newState != state)
                {
                    state = newState
                }
            }
        }
    }

    fun syncQueueEntry()
    {
        // we depend on their queue state
        // to be fulfilled in order to continue
        val queueState = queueState
            ?: return

        val queueId = "${queueState.kitId}:${queueState.queueType.name}:${queueState.teamSize}v${queueState.teamSize}"

        queueEntry = ScalaCommonsSpigot.instance.kvConnection
            .sync()
            .hget(
                "tropicpractice:queues:$queueId:entries",
                uniqueId.toString()
            )
            ?.let {
                Serializers
                    .gson
                    .fromJson(it, QueueEntry::class.java)
            }
    }
}
