package arcs.android.host

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import arcs.android.sdk.host.ArcHostHelper
import arcs.android.storage.handle.AndroidHandleManager
import arcs.core.allocator.TestingHost
import arcs.core.host.EntityHandleManager
import arcs.core.host.ParticleRegistration
import arcs.sdk.Particle
import arcs.sdk.android.storage.service.DefaultConnectionFactory
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KClass

open class TestExternalArcHostService(val arcHost: TestingAndroidHost) : Service() {
    val arcHostHelper: ArcHostHelper by lazy {
        ArcHostHelper(this, arcHost)
    }

    init {
        arcHost.serviceContext = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        arcHostHelper.onStartCommand(intent)
        return result
    }

    override fun onBind(intent: Intent?): IBinder? = null

    class FakeLifecycle : Lifecycle() {
        override fun addObserver(p0: LifecycleObserver) = Unit
        override fun removeObserver(p0: LifecycleObserver) = Unit
        override fun getCurrentState(): State = State.CREATED
    }

    open class TestingAndroidHost(vararg particles: ParticleRegistration) : TestingHost(*particles) {
        lateinit var serviceContext: Context

        override val entityHandleManager: EntityHandleManager by lazy {
            EntityHandleManager(
                AndroidHandleManager(
                    serviceContext,
                    FakeLifecycle(),
                    Dispatchers.Default,
                    DefaultConnectionFactory(serviceContext, TestBindingDelegate(serviceContext))
                )
            )
        }
    }
}
