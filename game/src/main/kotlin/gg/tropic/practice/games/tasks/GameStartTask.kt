package gg.tropic.practice.games.tasks

import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameState
import gg.tropic.practice.games.event.GameStartEvent
import gg.tropic.practice.resetAttributes
import gg.tropic.practice.games.team.GameTeamSide
import gg.scala.lemon.util.QuickAccess.username
import me.lucko.helper.scheduler.Task
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.visibility.VisibilityHandler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.TitlePart
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.scoreboard.DisplaySlot

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
class GameStartTask(
    private val game: GameImpl
) : Runnable
{
    lateinit var task: Task

    override fun run()
    {
        if (this.game.activeCountdown >= 5)
        {
            this.game.state = GameState.Starting

            this.game.toBukkitPlayers()
                .filterNotNull()
                .forEach { player ->
                    val team = this.game
                        .getTeamOf(player)
                        .side.ordinal

                    val spawn = this.game
                        .arena.spawns[team]!!

                    VisibilityHandler.update(player)
                    NametagHandler.reloadPlayer(player)

                    val objective = player.scoreboard
                        .getObjective("commonsHealth")
                        ?: player.scoreboard
                            .registerNewObjective(
                                "commonsHealth", "health"
                            )

                    objective.displaySlot = DisplaySlot.BELOW_NAME
                    objective.displayName = "${CC.D_RED}${Constants.HEART_SYMBOL}"

                    player.resetAttributes()

                    Tasks.sync {
                        player.teleport(
                            spawn.location(this.game.arenaWorld)
                        )
                    }

                    this.game.ladder.populate(player)
                    player.updateInventory()
                }

            val teamVersus = this.game.teams.values
                .reversed()
                .map { it.players.size }
                .joinToString("v")

            this.game.sendMessage(
                "",
                "${CC.D_AQUA}Starting $teamVersus game between:",
                "${CC.WHITE}${
                    this.game.teams[GameTeamSide.A]!!.players
                        .joinToString(", ") { 
                            it.username() 
                        }
                } and ${
                    this.game.teams[GameTeamSide.B]!!.players
                        .joinToString(", ") {
                            it.username()
                        }
                }",
                ""
            )
        }

        when (this.game.activeCountdown)
        {
            5, 4, 3, 2, 1 ->
            {
                this.game.audiences {
                    it.sendTitlePart(
                        TitlePart.TITLE,
                        Component
                            .text(this.game.activeCountdown)
                            .decorate(TextDecoration.BOLD)
                            .color(NamedTextColor.GREEN)
                    )

                    it.sendTitlePart(
                        TitlePart.SUBTITLE,
                        Component.text("The game is starting!")
                    )
                }

                this.game.sendMessage("${CC.SEC}The game starts in ${CC.PRI}${this.game.activeCountdown}${CC.SEC} second${
                    if (this.game.activeCountdown == 1) "" else "s"
                }...")
                this.game.playSound(Sound.NOTE_STICKS)
            }
        }

        if (this.game.activeCountdown <= 0)
        {
            val event = GameStartEvent(this.game)
            Bukkit.getPluginManager().callEvent(event)

            if (event.isCancelled)
            {
                this.game.closeAndCleanup(event.cancelMessage)
                this.task.closeAndReportException()
                return
            }

            this.game.startTimestamp = System.currentTimeMillis()

            this.game.sendMessage("${CC.GREEN}The game has started!")
            this.game.playSound(Sound.NOTE_PLING)
            this.game.audiences { it.clearTitle() }

            this.game.state = GameState.Playing
            this.task.closeAndReportException()
            return
        }

        this.game.activeCountdown--
    }
}