package gg.tropic.practice.map

import com.grinderwolf.swm.api.SlimePlugin
import com.grinderwolf.swm.api.loaders.SlimeLoader
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
@Service
object MapReplicationService
{
    @Inject
    lateinit var plugin: ExtendedScalaPlugin

    private lateinit var slimePlugin: SlimePlugin
    private lateinit var loader: SlimeLoader

    // TODO: New map changes don't propagate properly, might
    //  require restart for all game servers.
    private val readyMaps = mutableMapOf<UUID, ReadyMapTemplate>()
    private val mapReplications = mutableListOf<BuiltMapReplication>()

    @Configure
    fun configure()
    {
        slimePlugin = plugin.server.pluginManager
            .getPlugin("SlimeWorldManager") as SlimePlugin

        // TODO: Mongo?
        loader = slimePlugin.getLoader("file")

        populateSlimeCache()
    }

    private fun populateSlimeCache()
    {
        for (arena in MapService.cached().maps.values)
        {
            kotlin.runCatching {
                val slimeWorld = slimePlugin
                    .loadWorld(
                        loader,
                        arena.associatedSlimeTemplate,
                        true,
                        SlimePropertyMap() /* TODO: arena.properties Do we need this? */
                    )

                readyMaps[arena.identifier] = ReadyMapTemplate(slimeWorld)

                plugin.logger.info(
                    "Populated slime cache with SlimeWorld for arena ${arena.name}."
                )
            }.onFailure {
                plugin.logger.log(
                    Level.SEVERE, "Failed to populate cache", it
                )
            }
        }
    }

    fun generateArenaWorld(arena: Map): CompletableFuture<World>
    {
        val worldName =
            "${arena.name}-${
                UUID.randomUUID().toString().substring(0..8)
            }"

        val readyMap = readyMaps[arena.identifier]
            ?: return CompletableFuture.failedFuture(
                IllegalStateException("Map ${arena.name} does not have a ready SlimeWorld. Map changes have not propagated to this server?")
            )

        slimePlugin.generateWorld(readyMap.slimeWorld)

        // TODO: This is a slightly weird way to do this, improve? Keep track of world generation state?
        return CompletableFuture
            .supplyAsync {
                var world = Bukkit.getWorld(worldName)

                while (world == null)
                {
                    world = Bukkit.getWorld(worldName)
                }

                return@supplyAsync world
            }
            .thenApply {
                it.setGameRuleValue("naturalRegeneration", "false")
                it.setGameRuleValue("sendCommandFeedback", "false")
                it.setGameRuleValue("logAdminCommands", "false")

                // TODO: forward this directly to the requested game
                mapReplications += BuiltMapReplication(arena, it)
                return@thenApply it
            }
    }
}