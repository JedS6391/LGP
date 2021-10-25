package nz.co.jedsimson.lgp.core.evolution.model

import nz.co.jedsimson.lgp.core.evolution.ExportableResult

/**
 * An [ExportableResult] implementation that represents evolution statistics for a particular run.
 *
 * Because evolution statistics are collected on a per-generation basis, this type of exportable result
 * collects data on each generation for each run.
 *
 * @param run The run this result relates to.
 * @param statistics Evolution statistics for a particular generation.
 */
class RunBasedExportableResult<T>(
        val run: Int,
        private val statistics: EvolutionStatistics
) : ExportableResult<T> {

    /**
     * Exports this result as a mapping of statistic names to statistic values.
     */
    override fun export(): List<Pair<String, String>> {
        val out = mutableListOf(
            Pair("run", this.run.toString())
        )

        out.addAll(statistics.data.map { (name, value) ->
            Pair(name, value.toString())
        })

        return out
    }
}
