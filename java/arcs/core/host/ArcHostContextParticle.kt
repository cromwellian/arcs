package arcs.core.host

import arcs.core.storage.api.WriteCollectionHandle
import arcs.core.storage.api.WriteSingletonHandle

class ArcHostContextParticle : AbstractArcHostParticle() {
    suspend fun writeArcHostContext(arcId: String, hostId: String, context: ArcHostContext) {
        val connections = context.particles.flatMap {
            it.key.handles.map { handle ->
                ArcHostParticle_HandleConnections(
                    arcId,
                    it.key.particleName,
                    handle.key,
                    handle.value.storageKey.toString(),
                    handle.value.mode.name,
                    handle.value.type.tag.name,
                    handle.value.ttl?.minutes?.toDouble() ?: 0.0
                )
            }
        }
        val arcState = ArcHostParticle_ArcHostContext(arcId, hostId, context.arcState.name)
        val particles = context.particles.map {
            ArcHostParticle_Particles(
                arcId,
                it.key.particleName,
                it.key.location,
                it.value.particleState.name,
                it.value.consecutiveFailureCount.toDouble()
            )
        }
        handles.arcHostContext.store(arcState)
        handles.particles.clear()
        particles.forEach { handles.particles.store(it) }
        handles.handleConnections.clear()
        connections.forEach { handles.handleConnections.store(it) }
    }
}
