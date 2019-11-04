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

package arcs.crdt

import arcs.crdt.CrdtSet.Operation.Add
import arcs.crdt.CrdtSet.Operation.Remove
import arcs.crdt.internal.Actor
import arcs.common.Referencable
import arcs.common.ReferenceId
import arcs.crdt.internal.VersionMap

/** A [CrdtModel] capable of managing a mutable reference. */
class CrdtSingleton<T : Referencable>(
    /** Function to construct a new, empty [Data] object with a given [VersionMap]. */
    dataBuilder: (VersionMap) -> Data<T> = { versionMap -> DataImpl(versionMap) },
    initialVersion: VersionMap = VersionMap(),
    initialData: T? = null,
    singletonToCopy: CrdtSingleton<T>? = null
) : CrdtModel<CrdtSingleton.Data<T>, CrdtSingleton.Operation<T>, T?> {
    private var set: CrdtSet<T>

    override val data: Data<T>
        get() = set.data as Data<T>
    override val consumerView: T?
        // Get any value, or null if no value is present.
        get() = set.consumerView.minBy { it.id }

    init {
        CrdtException.require(initialData == null || singletonToCopy == null) {
            "Cannot instantiate CrdtSingleton by supplying both initialData AND singletonToCopy"
        }
        set = when {
            initialData != null -> CrdtSet(
                DataImpl(
                    initialVersion,
                    mutableMapOf(initialData.id to CrdtSet.DataValue(initialVersion, initialData))
                )
            )
            singletonToCopy != null -> singletonToCopy.set.copy()
            else -> CrdtSet(DataImpl(initialVersion), dataBuilder)
        }
    }

    /**
     * Simple constructor to build a [CrdtSingleton] from an initial value and starting at a given
     * [VersionMap].
     */
    constructor(versionMap: VersionMap, data: T?) : this(
        initialVersion = versionMap,
        initialData = data
    )

    override fun merge(other: Data<T>): MergeChanges<Data<T>, Operation<T>> {
        set.merge(other)
        // Always return CrdtChange.Data change records, since we cannot perform an op-based change.
        return MergeChanges(CrdtChange.Data(data), CrdtChange.Data(data))
    }

    override fun applyOperation(op: Operation<T>): Boolean = op.applyTo(set)

    override fun updateData(newData: Data<T>) = set.updateData(newData)

    /** Makes a deep copy of this [CrdtSingleton]. */
    internal fun copy(): CrdtSingleton<T> = CrdtSingleton(singletonToCopy = this)

    /** Abstract representation of the data stored by a [CrdtSingleton]. */
    interface Data<T : Referencable> : CrdtSet.Data<T> {
        override fun copy(): Data<T>
    }

    /** Concrete representation of the data stored by a [CrdtSingleton]. */
    class DataImpl<T : Referencable>(
        override var versionMap: VersionMap = VersionMap(),
        override val values: MutableMap<ReferenceId, CrdtSet.DataValue<T>> = mutableMapOf()
    ) : Data<T> {
        override fun copy(): Data<T> =
            DataImpl(versionMap = VersionMap(versionMap), values = HashMap(values))
    }

    sealed class Operation<T : Referencable>(
        open val actor: Actor,
        override val clock: VersionMap
    ) : CrdtOperationAtTime {
        /** Mutates [data] based on the implementation of the [Operation]. */
        internal abstract fun applyTo(set: CrdtSet<T>): Boolean

        /** An [Operation] to update the value stored by the [CrdtSingleton]. */
        open class Update<T : Referencable>(
            override val actor: Actor,
            override val clock: VersionMap,
            val value: T
        ) : Operation<T>(actor, clock) {
            override fun applyTo(set: CrdtSet<T>): Boolean {
                // Remove does not require an increment, but the caller of this method will have
                // incremented its version, so we hack a version with t-1 for this actor.
                val removeClock = VersionMap(clock)
                removeClock[actor]--

                // If we can't remove all existing values, we can't update the value.
                if (!Clear<T>(actor, removeClock).applyTo(set)) return false

                // After removal of all existing values, we simply need to add the new value.
                return set.applyOperation(Add(clock, actor, value))
            }
        }

        /** An [Operation] to clear the value stored by the [CrdtSingleton]. */
        open class Clear<T : Referencable>(
            override val actor: Actor,
            override val clock: VersionMap
        ) : Operation<T>(actor, clock) {
            override fun applyTo(set: CrdtSet<T>): Boolean {
                // Clear all existing values if our clock allows it.

                val removeOps = set.originalData.values
                    .map { (_, value) -> Remove(clock, actor, value.value) }

                removeOps.forEach { set.applyOperation(it) }
                return true
            }
        }
    }

    companion object {
        /** Creates a [CrdtSingleton] from pre-existing data. */
        fun <T : Referencable> createWithData(
            data: Data<T>,
            dataBuilder: (VersionMap) -> Data<T> = { DataImpl(it) }
        ) = CrdtSingleton(dataBuilder).apply { set = CrdtSet(data, dataBuilder) }
    }
}
