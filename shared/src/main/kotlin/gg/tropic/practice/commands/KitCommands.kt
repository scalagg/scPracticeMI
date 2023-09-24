package gg.tropic.practice.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.tropic.practice.kit.Kit
import gg.tropic.practice.kit.KitService
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.kit.group.KitGroup
import gg.tropic.practice.kit.group.KitGroupService
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.potion.PotionEffectType
import java.util.*

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
@AutoRegister
@CommandAlias("kit")
@CommandPermission("practice.command.kit")
object KitCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onDefault(help: CommandHelp)
    {
        help.showHelp()
    }

    /*@AssignPermission
    @Subcommand("delete")
    @CommandCompletion("@kits")
    @Description("Delete an existing kit.")
    fun onDelete(player: ScalaPlayer, kit: Kit)
    {
        // TODO: ensure no matches are ongoing with this kit

    }*/

    @AssignPermission
    @Subcommand("info")
    @CommandCompletion("@kits")
    @Description("Show information about a given kit.")
    fun onInfo(player: ScalaPlayer, kit: Kit)
    {
        player.sendMessage("${CC.GREEN}Information for the kit ${CC.WHITE}${kit.displayName}")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}ID: ${CC.WHITE}${kit.id}")
        player.sendMessage("${CC.GRAY}Enabled: ${if (kit.enabled) "${CC.GREEN}True" else "${CC.RED}False"}")
        player.sendMessage("${CC.GRAY}Icon: ${CC.WHITE}${
            kit.displayIcon.type.name.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase(Locale.getDefault())
                else
                    it.toString()
            }
        }")
        player.sendMessage(" ")
        // TODO: fancy message
        player.sendMessage("${CC.GRAY}Metadata: ${CC.WHITE}Click to view.")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}Armor contents ${CC.WHITE}(${kit.armorContents.size})${CC.GRAY}: ${CC.WHITE}Click to view.")
        player.sendMessage("${CC.GRAY}Inventory contents ${CC.WHITE}(${kit.contents.size})${CC.GRAY}: ${CC.WHITE}Click to view.")
    }

    @AssignPermission
    @Subcommand("toggle")
    @CommandCompletion("@kits")
    @Description("Toggle a kit's enabled state.")
    fun onToggle(player: ScalaPlayer, kit: Kit)
    {
        kit.enabled = !kit.enabled

        with(KitService.cached()) {
            KitService.cached().kits[kit.id] = kit
            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have ${if (kit.enabled) "${CC.B_GREEN}enabled" else "${CC.RED}disabled"} ${CC.GREEN}the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
        )
    }

    @AssignPermission
    @Subcommand("description")
    @CommandCompletion("@kits")
    @Description("Edit a kit's description.")
    fun onDescription(player: ScalaPlayer, kit: Kit, description: String)
    {
        kit.description = description

        with(KitService.cached()) {
            KitService.cached().kits[kit.id] = kit
            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the description for the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN} to:",
            description
        )
    }

    @AssignPermission
    @Subcommand("features add")
    @CommandCompletion("@kits @stranger-feature-flags")
    @Description("Add a feature flag to a kit.")
    fun onFeaturesAdd(player: ScalaPlayer, kit: Kit, feature: FeatureFlag)
    {
        if (kit.features(feature))
        {
            throw ConditionFailedException(
                "This kit already features the flag ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        if (kit.features.keys.intersect(feature.incompatibleWith()).isNotEmpty())
        {
            throw ConditionFailedException(
                "This kit already features a flag that is incompatible with ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        if (feature.requires.any { it !in kit.features.keys })
        {
            throw ConditionFailedException(
                "This kit does not feature the required flags ${CC.YELLOW}${feature.requires.joinToString(", ")}${CC.RED} to add the flag ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        with(KitService.cached()) {
            kit.features[feature] = mutableMapOf()
            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have added the feature flag ${CC.YELLOW}${feature.name}${CC.GREEN} to the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN} with all default metadata."
        )
    }

    @Subcommand("features metadata remove")
    @Description("Remove metadata from a feature flag associated with a kit.")
    @CommandCompletion("@kits @existing-feature-flags @existing-feature-flags-schemakeys")
    fun onFeatureMetadataRemove(
        player: ScalaPlayer, kit: Kit, feature: FeatureFlag,
        @Single key: String
    )
    {
        if (!kit.features(feature))
        {
            throw ConditionFailedException(
                "This kit is not associated with the feature ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        val metadata = kit.features[feature]!!

        if (key !in metadata.keys)
        {
            throw ConditionFailedException(
                "This kit does not have metadata with the key ${CC.YELLOW}$key${CC.RED} associated with the feature ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        metadata -= key

        with(KitService.cached()) {
            kit.features[feature] = metadata
            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have removed metadata with the key ${CC.YELLOW}$key${CC.GREEN} from the feature flag ${CC.YELLOW}${feature.name}${CC.GREEN} for kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
        )
    }

    @Subcommand("features metadata add")
    @Description("Add metadata to a feature flag associated with a kit.")
    @CommandCompletion("@kits @existing-feature-flags @stranger-feature-flags-schemakeys")
    fun onFeatureMetadataAdd(
        player: ScalaPlayer, kit: Kit, feature: FeatureFlag,
        @Single key: String, @Single value: String
    )
    {
        if (!kit.features(feature))
        {
            throw ConditionFailedException(
                "This kit is not associated with the feature ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        val metadata = kit.features[feature]!!

        if (key in metadata.keys)
        {
            throw ConditionFailedException(
                "This kit already has metadata with the key ${CC.YELLOW}$key${CC.RED} associated with the feature ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        if (key !in feature.schema.keys)
        {
            throw ConditionFailedException(
                "The feature flag ${CC.YELLOW}${feature.name}${CC.RED} does not have a schema key with the name ${CC.YELLOW}$key${CC.RED}. The available schema keys are ${CC.YELLOW}${feature.schema.keys.joinToString(", ")}${CC.RED}."
            )
        }

        metadata[key] = value

        with(KitService.cached()) {
            kit.features[feature] = metadata
            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have added metadata with the key ${CC.YELLOW}$key${CC.GREEN} and value ${CC.YELLOW}$value${CC.GREEN} to the feature flag ${CC.YELLOW}${feature.name}${CC.GREEN} for kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
        )
    }

    @Subcommand("features metadata list")
    @CommandCompletion("@kits @existing-feature-flags")
    @Description("List all metadata for a feature flag associated with a kit.")
    fun onFeatureMetadataList(player: ScalaPlayer, kit: Kit, feature: FeatureFlag)
    {
        if (!kit.features(feature))
        {
            throw ConditionFailedException(
                "This kit is not associated with the feature ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        val metadata = kit.features[feature]!!

        if (metadata.isEmpty())
        {
            throw ConditionFailedException(
                "This kit does not have any metadata associated with the feature ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        player.sendMessage(
            "${CC.GREEN}Metadata for feature flag ${CC.B_GREEN}${feature.name}${CC.GREEN} for kit ${CC.B_GREEN}${kit.displayName}${CC.GREEN}:"
        )

        metadata.forEach { (k, v) ->
            player.sendMessage(" - ${CC.WHITE}$k: $v")
        }
    }

    @AssignPermission
    @Subcommand("features remove")
    @Description("Remove a feature flag from a kit.")
    @CommandCompletion("@kits @existing-feature-flags")
    fun onFeaturesRemove(player: ScalaPlayer, kit: Kit, feature: FeatureFlag)
    {
        if (!kit.features(feature))
        {
            throw ConditionFailedException(
                "This kit is not associated with the feature ${CC.YELLOW}${feature.name}${CC.RED}."
            )
        }

        with(KitService.cached()) {
            kit.features -= feature
            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have removed the feature flag ${CC.YELLOW}${feature.name}${CC.GREEN} from the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
        )
    }

    @CommandCompletion("@kits")
    @Subcommand("features list")
    @Description("View all feature flags associated with this kit.")
    fun onFeaturesView(player: ScalaPlayer, kit: Kit)
    {
        player.sendMessage("${CC.GREEN}Feature flags for kit ${CC.B_WHITE}${kit.displayName}${CC.GREEN}:")

        if (kit.features.isEmpty())
        {
            player.sendMessage("${CC.RED}None")
            return
        }

        kit.features
            .forEach { (flag, meta) ->
                player.sendMessage(" - ${CC.WHITE}${flag.name}")

                if (meta.isNotEmpty())
                {
                    player.sendMessage("    ${CC.GRAY}Metadata:")
                    meta.forEach { (k, v) ->
                        player.sendMessage("     ${CC.WHITE}$k: $v")
                    }
                }
            }
    }

    @AssignPermission
    @Subcommand("inventory getcontents")
    @CommandCompletion("@kits")
    @Description("Equips the current kit contents to your player.")
    fun onLoadContents(sender: ScalaPlayer, kit: Kit)
    {
        val inventory = kit.contents
        val armor = kit.armorContents
        val player = sender.bukkit()

        player.inventory.contents = inventory
        player.inventory.armorContents = armor

        sender.sendMessage("${CC.GREEN}You have received inventory contents for the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}.")
    }

    @AssignPermission
    @Subcommand("inventory setcontents")
    @CommandCompletion("@kits")
    @Description("Sets your inventory to the kit's contents.")
    fun onSetContents(sender: ScalaPlayer, kit: Kit)
    {
        val player = sender.bukkit()
        val inventory = player.inventory.contents
        val armor = player.inventory.armorContents

        kit.contents = inventory
        kit.armorContents = armor

        with(KitService.cached()) {
            KitService.cached().kits[kit.id] = kit
            KitService.sync(this)
        }

        sender.sendMessage("${CC.GREEN}You have saved inventory contents for the kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}.")
    }

    @AssignPermission
    @Subcommand("inventory effects add")
    @CommandCompletion("@kits @effects")
    @Description("Add effects to this kit.")
    fun onKitAddEffect(player: ScalaPlayer, kit: Kit, potionEffectType: PotionEffectType, amplifier: Int)
    {
        kit.potionEffects[potionEffectType] = amplifier

        with(KitService.cached()) {
            KitService.cached().kits[kit.id] = kit
            KitService.sync(this)
        }

        player.sendMessage("${CC.GREEN}You have added the potion effect ${CC.YELLOW}${
            potionEffectType.name.lowercase()
                .replaceFirstChar {
                    it.titlecase(Locale.getDefault()) 
                }
        } ${CC.GREEN}with an amplifier of ${CC.YELLOW}$amplifier${CC.GREEN}.")
    }

    @Subcommand("inventory effects list")
    @CommandCompletion("@kits")
    @Description("Show all effects that a kit has.")
    fun onKitEffectList(player: ScalaPlayer, kit: Kit)
    {
        player.sendMessage(
            "${CC.GREEN}${kit.displayName}'s game effects:"
        )

        val effects = kit.potionEffects.entries
        val fancyMessage = FancyMessage()

        if (effects.isNotEmpty())
        {
            for ((index, entry) in effects.withIndex())
            {
                val listComponents = FancyMessage()
                    .withMessage(
                        "${CC.GRAY}${
                            entry.key.name.lowercase().replaceFirstChar { 
                                it.titlecase(Locale.getDefault()) 
                            }
                        } (Amplifier ${entry.value})${if (index != effects.size - 1) ", " else ""}"
                    )

                fancyMessage.components.addAll(listComponents.components)
            }

            fancyMessage.sendToPlayer(player.bukkit())
        } else
        {
            player.sendMessage(
                "${CC.RED}None"
            )
        }
    }

    @AssignPermission
    @Subcommand("inventory effects remove")
    @CommandCompletion("@kits @effects")
    @Description("Removes the given effect from the kit.")
    fun onKitRemoveEffect(player: ScalaPlayer, kit: Kit, potionEffectType: PotionEffectType)
    {
        if (!kit.potionEffects.containsKey(potionEffectType))
        {
            throw ConditionFailedException(
                "The kit ${CC.YELLOW}${kit.displayName} ${CC.RED}does contain the effect${
                    potionEffectType.name.lowercase()
                        .replaceFirstChar {
                            it.titlecase(Locale.getDefault())
                        }
                }"
            )
        }

        kit.potionEffects.remove(potionEffectType)

        with (KitService.cached()) {
            KitService.cached().kits[kit.id] = kit
            KitService.sync(this)
        }

        player.sendMessage("${CC.GREEN}You have removed the potion effect ${CC.YELLOW}${
            potionEffectType.name.lowercase()
                .replaceFirstChar {
                    it.titlecase(Locale.getDefault())
                }
        } ${CC.GREEN}from the ${CC.YELLOW}${kit.displayName} ${CC.GREEN}kit.")
    }

    @Subcommand("list")
    @Description("List all kits.")
    fun onList(player: ScalaPlayer)
    {
        val listFancyMessage = FancyMessage()
        val kits = KitService.cached().kits

        player.sendMessage(
            "${CC.GREEN}Kits"
        )

        for ((i, kit) in kits.values.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${kit.displayName}${
                        if (i != (kits.size - 1)) ", " else ""
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view kit information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/kit info ${kit.id}"
                )

            listFancyMessage.components.add(uniqueInfoComponent.components[0])
        }

        if (listFancyMessage.components.isNotEmpty())
        {
            listFancyMessage.sendToPlayer(player.bukkit())
        } else
        {
            player.sendMessage(
                "${CC.RED}None"
            )
        }
    }

    @AssignPermission
    @Subcommand("groups add")
    @CommandCompletion("@kits @stranger-kit-groups-to-kit")
    @Description("Associate a kit with a kit group.")
    fun onGroupsAdd(player: ScalaPlayer, kit: Kit, group: KitGroup)
    {
        if (kit.id in group.contains)
        {
            throw ConditionFailedException(
                "The kit group ${CC.YELLOW}${group.id}${CC.RED} is already associated with kit ${CC.YELLOW}${kit.displayName}${CC.RED}."
            )
        }

        group.contains += kit.id
        KitGroupService.sync(KitGroupService.cached())

        player.sendMessage(
            "${CC.GREEN}Kit group ${CC.YELLOW}${group.id}${CC.GREEN} is now associated with kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
        )
    }

    @Subcommand("groups list")
    @CommandCompletion("@kits")
    @Description("List all associated kit groups with a kit.")
    fun onGroupsList(player: ScalaPlayer, kit: Kit)
    {
        player.sendMessage(
            "${CC.GREEN}All associated kit groups for kit ${CC.B_WHITE}${kit.displayName}${CC.GREEN}:",
            "${CC.WHITE}[click a kit group to view information]"
        )

        val listFancyMessage = FancyMessage()
        val associated = KitGroupService.groupsOf(kit)
        for ((i, group) in associated.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${group.id}${CC.WHITE}${
                        if (i != (associated.size - 1)) ", " else "."
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view map information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/kitgroup info ${group.id}"
                )

            listFancyMessage.components.addAll(
                uniqueInfoComponent.components
            )
        }

        if (listFancyMessage.components.isNotEmpty())
        {
            listFancyMessage.sendToPlayer(player.bukkit())
        } else
        {
            player.sendMessage(
                "${CC.RED}None"
            )
        }
    }

    @AssignPermission
    @Subcommand("groups remove")
    @CommandCompletion("@kits @associated-kit-groups-with-kit")
    @Description("Remove an associated kit group from a kit.")
    fun onGroupsRemove(player: ScalaPlayer, kit: Kit, group: KitGroup)
    {
        if (kit.id !in group.contains)
        {
            throw ConditionFailedException(
                "The kit group ${CC.YELLOW}${group.id}${CC.RED} is not associated with kit ${CC.YELLOW}${kit.displayName}${CC.RED}."
            )
        }

        val associated = KitGroupService.groupsOf(kit)
        if (associated.size == 1)
        {
            throw ConditionFailedException(
                "You cannot remove this kit group when there are no other kit groups associated with this kit. Please run ${CC.YELLOW}/kit groups add ${kit.id} __default__${CC.RED} to add back the default group before you remove the ${CC.YELLOW}${group.id}${CC.RED} group."
            )
        }

        group.contains -= kit.id
        KitGroupService.sync(KitGroupService.cached())

        player.sendMessage(
            "${CC.GREEN}Kit group ${CC.YELLOW}${group.id}${CC.GREEN} is now associated with kit ${CC.YELLOW}${kit.displayName}${CC.GREEN}."
        )
    }

    @AssignPermission
    @Subcommand("create")
    @Description("Create a new kit.")
    fun onCreate(player: ScalaPlayer, @Single id: String)
    {
        val lowercaseID = id.lowercase()

        // TODO: ensure no matches are ongoing with this kit

        if (KitService.cached().kits[lowercaseID] != null)
        {
            throw ConditionFailedException(
                "A kit with the ID ${CC.YELLOW}$lowercaseID${CC.RED} already exists."
            )
        }

        val kit = Kit(
            id = lowercaseID,
            displayName = id
                .replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.getDefault())
                    else
                        it.toString()
                }
        )

        with(KitService.cached()) {
            kits[lowercaseID] = kit

            with(KitGroupService) {
                with(cached()) {
                    default().contains += lowercaseID
                    KitGroupService.sync(this)
                }
            }

            KitService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You created a new kit with the ID ${CC.YELLOW}$lowercaseID${CC.GREEN}."
        )
    }
}
