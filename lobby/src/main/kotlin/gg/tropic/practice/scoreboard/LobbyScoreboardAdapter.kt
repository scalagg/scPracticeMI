package gg.tropic.practice.scoreboard

import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.flavor.service.Service
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.util.QuickAccess.username
import gg.tropic.practice.player.LobbyPlayerService
import gg.tropic.practice.player.PlayerState
import gg.tropic.practice.player.formattedDomain
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.services.ScoreboardTitleService
import gg.tropic.practice.settings.DuelsSettingCategory
import gg.tropic.practice.settings.isASilentSpectator
import gg.tropic.practice.settings.scoreboard.LobbyScoreboardView
import net.evilblock.cubed.scoreboard.ScoreboardAdapter
import net.evilblock.cubed.scoreboard.ScoreboardAdapterRegister
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.util.nms.MinecraftProtocol
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.entity.Player
import java.util.*

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
@Service
@ScoreboardAdapterRegister
object LobbyScoreboardAdapter : ScoreboardAdapter()
{
    override fun getLines(board: LinkedList<String>, player: Player)
    {
        val profile = LobbyPlayerService.find(player.uniqueId)
            ?: return
        board += ""
        board += "${CC.WHITE}Online: ${CC.PRI}${
            Numbers.format(ScoreboardInfoService.scoreboardInfo.online)
        }"
        board += "${CC.WHITE}Playing: ${CC.PRI}${
            Numbers.format(ScoreboardInfoService.scoreboardInfo.playing)
        }"

        if (profile.inQueue())
        {
            board += ""
            board += "${CC.PRI}${profile.queuedForType().name} Queue:"
            board += "${CC.GRAY}${profile.queuedForKit()?.displayName} ${
                profile.queuedForTeamSize()
            }v${
                profile.queuedForTeamSize()
            }"
            board += "${CC.WHITE}Queued for ${CC.PRI}${
                TimeUtil.formatIntoMMSS((profile.queuedForTime() / 1000).toInt())
            }"

            val shouldIncludeELORange = profile.validateQueueEntry() &&
                profile.queuedForType() == QueueType.Ranked &&
                MinecraftProtocol.getPlayerVersion(player) <= 5

            if (shouldIncludeELORange)
            {
                val domain = profile.queueEntry().leaderRangedELO
                    .toIntRangeInclusive()
                    .formattedDomain()

                board += "${CC.WHITE}ELO Range: ${CC.PRI}$domain"
            }
        } else if (profile.isInParty())
        {
            board += ""
            board += "${CC.PRI}Party:"

            with(profile.partyOf().delegate) {
                board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Leader: ${CC.PRI}${
                    leader.uniqueId.username()
                }"
                board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Members: ${CC.PRI}${
                    "${members.size}/${
                        if (limit == -1) "${CC.B}∞" else "$limit"
                    }"
                }"
            }
        } else
        {
            BasicsProfileService.find(player)
                ?.apply {
                    val scoreboardView = setting(
                        "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:lobby-scoreboard-view",
                        LobbyScoreboardView.None
                    )

                    if (scoreboardView != LobbyScoreboardView.None)
                    {
                        board += ""
                        board += "${CC.PRI}${scoreboardView.displayName}:"
                    }

                    when (scoreboardView)
                    {
                        LobbyScoreboardView.Dev ->
                        {
                            board += "${CC.WHITE}Game servers: ${CC.PRI}${
                                ScoreboardInfoService.scoreboardInfo.gameServers
                            }"
                            board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} Mean TPS: ${CC.GREEN}${
                                ScoreboardInfoService.scoreboardInfo.meanTPS.run {
                                    if (this > 20.0) "*20.0" else "%.1f".format(ScoreboardInfoService.scoreboardInfo.meanTPS)
                                }
                            }"
                            board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} ${CC.WHITE}Replications: ${CC.PRI}${
                                Numbers.format(ScoreboardInfoService.scoreboardInfo.availableReplications)
                            }"
                            board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE}${CC.WHITE} ${CC.WHITE}Games: ${CC.PRI}${
                                Numbers.format(ScoreboardInfoService.scoreboardInfo.runningGames)
                            } ${CC.GRAY}(${
                                "%.2f".format(ScoreboardInfoService.scoreboardInfo.percentagePlaying)
                            }%)"
                            board += ""
                            board += "${CC.WHITE}NA/EU Players: ${CC.PRI}${
                                Numbers.format(ScoreboardInfoService.scoreboardInfo.naServerTotalPlayers)
                            } ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.PRI}${
                                Numbers.format(ScoreboardInfoService.scoreboardInfo.euServerTotalPlayers)
                            }"

                        }

                        LobbyScoreboardView.Staff ->
                        {
                            fun metadataDisplay(metadata: String) = if (player.hasMetadata(metadata))
                                "${CC.GREEN}Enabled" else "${CC.RED}Disabled"

                            val basicsProfile = BasicsProfileService.find(player)
                            board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Vanish: ${metadataDisplay("vanished")}"

                            if (basicsProfile != null)
                            {
                                board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Silent Spec: ${
                                    if (player.isASilentSpectator()) "${CC.GREEN}Enabled" else "${CC.RED}Disabled"
                                }"
                            }

                            board += "${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}Mod Mode: ${metadataDisplay("mod-mode")}"
                        }

                        LobbyScoreboardView.None ->
                        {

                        }
                    }
                }
        }

        board += ""
        board += CC.GRAY + LemonConstants.WEB_LINK + "          " + CC.GRAY + "      " + CC.GRAY + "  " + CC.GRAY
    }

    override fun getTitle(player: Player) = ScoreboardTitleService.getCurrentTitle()
}
