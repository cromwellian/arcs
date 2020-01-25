/*
 * Copyright 2019 Google LLC.
 *
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 *
 * Code distributed by Google as part of this project is also subject to an additional IP rights
 * grant found at
 * http://polymer.github.io/PATENTS.txt
 */
package arcs.android.host

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.os.ResultReceiver
import arcs.core.host.ArcHost
import arcs.core.host.HostRegistry
import arcs.core.host.ParticleIdentifier
import arcs.core.host.PlanPartition
import arcs.core.sdk.Particle
import arcs.jvm.host.ServiceLoaderHostRegistry
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

const val ARC_HOST_INTENT = "arcs.android.host.ARC_HOST"
const val OPERATION = "OPERATION"
const val OPERATION_ARG = "OPERATION_ARG"
const val OPERATION_RECEIVER = "OPERATION_RECEIVER"
const val OPERATION_RESULT = "OPERATION_RESULT"

/**
 * A [HostRegistry] that discovers available [ArcHost] services by using [PackageManager] to
 * query Android [Service] declarations in AndroidManfiest which can respond to a specific Inte.t
 * Stub [ArcHost] instances are created which communicate with the [Service] via [Intent]-based
 * RPC.
 *
 * In AndroidManifest.xml a <service> will need to be declared as follows for auto-discovery:
 * <service android:name=".MyService" android:exported="false">
 *   <intent-filter>
 *     <action android:name="arcs.android.host.ARCS_HOST" />
 *   </intent-filter>
 * </service>
 *
 * These [ArcHost] implementations are [ExternalHost]s mostly assumed to have
 * pre-registered particles. [ProdHost] will still find its [Particle] implementations
 * via [ServiceLoaderHostRegistry]
 */
class AndroidManifestHostRegistry(val ctx: Context) : HostRegistry {

    private val arcHosts: MutableList<ArcHost> = mutableListOf()

    init {
        runBlocking {
            arcHosts.addAll(findHostsByManifest())

            // Load [ProdHost] and other annotation based isolated hosts/particles.
            arcHosts.addAll(ServiceLoaderHostRegistry.availableArcHosts().map { it ->
                IntentArcHostAdapter(ctx, ComponentName(ctx, it::class.java))
            })
        }
    }

    private fun findHostsByManifest(): List<ArcHost> =
        ctx.packageManager.queryIntentServices(
                Intent(ARC_HOST_INTENT),
                PackageManager.MATCH_ALL
            )
            .filter { it -> it.serviceInfo != null }
            .map { it ->
                IntentArcHostAdapter(
                    ctx, ComponentName(
                        it.serviceInfo.packageName,
                        it.serviceInfo.name
                    )
                )
            }


    enum class Operation {
        START_ARC, STOP_ARC, REGISTER_PARTICLE, UNREGISTER_PARTICLE,
        GET_REGISTERED_PARTICLES
    }

    class IntentArcHostAdapter(val ctx: Context, val arcHostComponentName: ComponentName) :
        ArcHost {

        override suspend fun registerParticle(particle: ParticleIdentifier) =
            sendIntentToArcHostServiceForResult(
                Operation.REGISTER_PARTICLE, particle.toComponentName()
            ) {}

        override suspend fun unregisterParticle(particle: ParticleIdentifier) =
            sendIntentToArcHostServiceForResult(
                Operation.UNREGISTER_PARTICLE, particle.toComponentName()
            ) {}


        override suspend fun registeredParticles(): List<ParticleIdentifier> =
            sendIntentToArcHostServiceForResult(Operation.GET_REGISTERED_PARTICLES) {
                it as List<Parcelable>
                it.map {
                    it as ComponentName
                    it.toParticleIdentifier()
                }
            }

        override suspend fun startArc(partition: PlanPartition) {
            sendIntentToArcHostService(Operation.START_ARC, partition.toParcelable())
        }

        override suspend fun stopArc(partition: PlanPartition) {
            sendIntentToArcHostService(Operation.STOP_ARC, partition.toParcelable())
        }

        private fun sendIntentToArcHostService(
            operation: Operation,
            argument: Parcelable? = null,
            receiver: ResultReceiver? = null
        ): Unit {
            ctx.startService(
                Intent(ARC_HOST_INTENT).setComponent(arcHostComponentName)
                    .putExtra(OPERATION, operation)
                    .putExtra(
                        OPERATION_ARG,
                        argument
                    )
                    .putExtra(OPERATION_RECEIVER, receiver)
            )
        }

        class ResultReceiverContinuation<T>(
            val continuation: Continuation<T>, val block: (Parcelable) -> T
        ) : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) =
                continuation.resume(block(resultData?.getParcelable(OPERATION_RESULT)!!))
        }

        private suspend fun <T> sendIntentToArcHostServiceForResult(
            operation: Operation,
            argument: Parcelable? = null,
            block: (Parcelable) -> T
        ): T = suspendCoroutine<T> {
            sendIntentToArcHostService(
                operation, argument, ResultReceiverContinuation(it, block)
            )
        }

        override fun hashCode(): Int {
            return arcHostComponentName.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as IntentArcHostAdapter
            if (arcHostComponentName != other.arcHostComponentName) return false
            return true
        }
    }


    override suspend fun availableArcHosts(): List<ArcHost> = arcHosts

    override suspend fun registerHost(host: ArcHost) {
        arcHosts.add(IntentArcHostAdapter(ctx, ComponentName(ctx, host::class.java)))
    }

    override suspend fun unregisterHost(host: ArcHost) {
        arcHosts.remove(IntentArcHostAdapter(ctx, ComponentName(ctx, host::class.java)))
    }

}
