package gg.tropic.practice.application

import gg.scala.aware.AwareHub
import gg.scala.aware.uri.WrappedAwareUri
import gg.scala.commons.agnostic.sync.ServerSync
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.application.api.defaults.map.MapDataSync
import net.evilblock.cubed.serializers.Serializers

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
fun main(args: Array<String>)
{
    AwareHub.configure(
        WrappedAwareUri(
            args.getOrNull(0) ?: "localhost",
            args.getOrNull(1)?.toIntOrNull() ?: 6379
        )
    ) {
        Serializers.gson
    }

    DPSRedisShared.keyValueCache
    MapDataSync.load()
    KitDataSync.load()

    ServerSync.configureIndependent()
}