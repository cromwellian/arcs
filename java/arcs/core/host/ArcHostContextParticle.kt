package arcs.core.host

import arcs.core.common.toArcId
import arcs.core.data.Capabilities
import arcs.core.data.CollectionType
import arcs.core.data.EntityType
import arcs.core.data.Plan
import arcs.core.data.SingletonType
import arcs.core.data.Ttl
import arcs.core.storage.CapabilitiesResolver
import arcs.core.storage.StorageKeyParser
import arcs.core.type.Tag
import arcs.core.type.Type

/**
 * An implicit [Particle] that lives only within the [ArcHost] and used as a utility class to
 * serialize/deserialize [ArcHostContext] information from [Handle]s. It does not live in an
 * Arc or participate in normal [Particle] lifecycle.
 */
class ArcHostContextParticle : AbstractArcHostParticle() {
    /**
     * Given an [ArcId], [hostId], and [ArcHostContext], convert these types to Arc Schema
     * types, and write them to the appropriate handles. See `ArcHostContext.arcs` for schema
     * definitions.
     */
    suspend fun writeArcHostContext(arcId: String, hostId: String, context: ArcHostContext) {
        try {
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
        } catch (e: Exception) {
            val xx = true
        }
    }

    suspend fun readArcHostContext(
        arcId: String, hostId: String,
        instantiateParticle: suspend (ParticleIdentifier) -> Particle
    ): ArcHostContext? {
        val arcStateEntity = handles.arcHostContext.fetch()
        val particleEntities = handles.particles.fetchAll()
        val connectionEntities = handles.handleConnections.fetchAll()
        if (arcStateEntity == null || particleEntities.isEmpty() || connectionEntities.isEmpty()) {
            return null
        }

        val instantiatedParticles = particleEntities.map {
            it.particleName to instantiateParticle(ParticleIdentifier.from(it.location))
        }.associateBy({ it.first }, { it.second })

        val handleConnections = connectionEntities.map {
            it.particleName to (it.handleName to Plan.HandleConnection(
                StorageKeyParser.parse(it.storageKey),
                HandleMode.valueOf(it.mode),
                fromTag(instantiatedParticles[it.particleName]!!, it.type, it.handleName),
                it.ttl?.let { num -> if (num != 0.0) Ttl.Minutes(num.toInt()) else null }
            ))
        }.groupBy({ it.first })

        val particles = particleEntities.map {
            val particle = instantiateParticle(ParticleIdentifier.from(it.location))
            Plan.Particle(
                it.particleName,
                it.location,
                handleConnections[it.particleName]?.associateBy(
                    { it.second.first },
                    { it.second.second }
                ) ?: throw IllegalArgumentException(
                        "Can't find handleConnection for ${it.location} in $arcId"
                  )
            ) to ParticleContext(
                particle,
                mutableMapOf(),
                ParticleState.valueOf(it.particleState),
                it.consecutiveFailures.toInt()
            )
        }.associateBy({ it.first }, { it.second })

        return ArcHostContext(
            particles.toMutableMap(),
            arcStateEntity?.let {
                ArcState.valueOf(it.arcState)
            } ?: ArcState.NeverStarted
        )
    }

    fun fromTag(particle: Particle, tag: String, handleName: String): Type {
        val schema = particle.handles.getEntitySpec(handleName).schema()
        return when (Tag.valueOf(tag)) {
            Tag.Singleton -> SingletonType(EntityType(schema))
            Tag.Collection -> CollectionType(EntityType(schema))
            Tag.Entity -> EntityType(schema)
            else -> throw IllegalArgumentException(
                "Illegal Tag $tag when deserializing ArcHostContext"
            )
        }
    }

    /**
     * When recipe2plan is finished, the 'Plan' to serialize/deserialize ArcHost information
     * will mostly be code-genned, and this method will mostly go away.
     */
    fun createArcHostContextPersistencePlan(arcId: String, hostId: String): Plan.Partition {
        val resolver = CapabilitiesResolver(
            CapabilitiesResolver.CapabilitiesResolverOptions(arcId.toArcId())
        )

        // Because we don't have references/collections support yet, we use 3 handles/schemas
        val arcStateKey = resolver.createStorageKey(
            Capabilities.TiedToRuntime,
            ArcHostParticle_ArcHostContext_Spec.schema,
            "${hostId}_arcState"
        )

        val particlesStateKey = resolver.createStorageKey(
            Capabilities.TiedToRuntime,
            ArcHostParticle_Particles_Spec.schema,
            "${hostId}_arcState_particles"
        )

        val handleConnectionsKey = resolver.createStorageKey(
            Capabilities.TiedToRuntime,
            ArcHostParticle_HandleConnections_Spec.schema,
            "${hostId}_arcState_handleConnections"
        )

        return Plan.Partition(
            arcId,
            hostId,
            listOf(
                Plan.Particle(
                    "ArcHostContextParticle",
                    ArcHostContextParticle::class.toParticleIdentifier().id,
                    mapOf(
                        "arcHostContext" to Plan.HandleConnection(
                            arcStateKey!!,
                            HandleMode.ReadWrite,
                            SingletonType(
                                EntityType(ArcHostParticle_ArcHostContext_Spec.schema)
                            )
                        ),
                        "particles" to Plan.HandleConnection(
                            particlesStateKey!!,
                            HandleMode.ReadWrite,
                            CollectionType(
                                EntityType(ArcHostParticle_Particles_Spec.schema)
                            )
                        ),
                        "handleConnections" to Plan.HandleConnection(
                            handleConnectionsKey!!,
                            HandleMode.ReadWrite,
                            CollectionType(
                                EntityType(ArcHostParticle_HandleConnections_Spec.schema)
                            )
                        )
                    )
                )
            )
        )
    }
}

