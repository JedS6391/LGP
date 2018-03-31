package lgp.core.evolution

import com.opencsv.CSVWriter
import lgp.core.modules.Module
import lgp.core.modules.ModuleInformation
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Represents a result that is able to be exported from the system.
 *
 * The type of result that it represents is flexible and will depend on which part of the
 * system it comes from. The idea of being *exportable* means that the internal representation
 * can be whatever, as long as it has a way to be exported to a list of string pairs.
 */
interface ExportableResult<T> {

    /**
     * Creates an exportable representation of this result.
     */
    fun export(): List<Pair<String, String>>
}

/**
 * A module that can collect [ExportableResult] instances for later export from the system.
 */
abstract class ResultAggregator<T> : Module, AutoCloseable {
    /**
     * A collection of [ExportableResult] instances.
     */
    abstract val results: List<ExportableResult<T>>

    /**
     * Add an [ExportableResult].
     */
    abstract fun add(result: ExportableResult<T>)

    /**
     * Add a collection of [ExportableResult] instances in a batch aggregation.
     */
    abstract fun addAll(results: List<ExportableResult<T>>)
}

/**
 * A collection of built-in [ResultAggregator] implementations.
 */
class ResultAggregators {
    /**
     * A simple result aggregator that builds an internal collection of results.
     *
     * Care must be taken with this aggregator, as it will keep all results in memory and only
     * relieve results once cleared. Results should either be exported regularly or the number
     * of results aggregated should be limited. Alternatively, another aggregator that flushes
     * results automatically could be used.
     *
     * Note that if using this implementation in a multi-threaded environment, there is no guarantee
     * about the order of the results produced. The built-in [Trainers] which contain result aggregation
     * logic ensure that results are grouped per-run, but there it is not certain that the runs will be
     * in order.
     */
    class InMemoryResultAggregator<T> : ResultAggregator<T>() {

        override val results: MutableList<ExportableResult<T>> = mutableListOf()

        /**
         * Add a single result.
         */
        override fun add(result: ExportableResult<T>) {
            this.results.add(result)
        }

        /**
         * Add a collection of results.
         */
        override fun addAll(results: List<ExportableResult<T>>) {
            this.results.addAll(results)
        }

        /**
         * Clear all stored results.
         */
        override fun close() {
            this.results.clear()
        }

        /**
         * Returns a representation of any currently aggregated results in a simple table format.
         */
        override fun toString(): String {
            if (this.results.isEmpty())
                return "No results."

            val table = StringBuilder()
            val header = this.results.first()
                                     .export()
                                     .map { (name, _) -> name }

            header.map { key -> table.append("%-28s".format(key)) }
            table.appendln()

            this.results.forEach { result ->
                val exported = result.export()

                exported.map { value -> table.append("%-28s".format(value.second)) }
                table.append('\n')
            }

            return table.toString()
        }

        /**
         * Provides information about the module.
         */
        override val information = ModuleInformation(
                description = "Aggregates results that are able to be exported."
        )
    }

    /**
     * A [ResultAggregator] that buffers results and flushes to a CSV output regularly.
     *
     * This implementation will avoid using large amounts of memory as any results aggregated
     * are regularly written out to file, instead of being kept in memory. Currently, the only
     * supported [ResultOutputProvider] is [ResultOutputProviders.CsvResultOutputProvider] as
     * no other output provider implementations support the buffered output required.
     *
     * Note that the buffering is done in a thread-safe way to ensure that there is no contention
     * between threads when results are being added/output simultaneously. Despite this, there is no
     * guarantee that the order of the results will be sequential in a multi-threaded environment. The
     * built-in [Trainers] ensure that the results will be grouped by their run however.
     *
     * @param outputProvider A [ResultOutputProviders.CsvResultOutputProvider] instance.
     * @param bufferSize How many results to keep in memory before writing to [outputProvider].
     * @param verbose Writes detailed output about any buffering operations to standard out.
     */
    class BufferedResultAggregator<T>(
            private val outputProvider: ResultOutputProviders.CsvResultOutputProvider<T>,
            private val bufferSize: Int = 100,
            private val verbose: Boolean = false
    ) : ResultAggregator<T>() {

        override val results: MutableList<ExportableResult<T>> = mutableListOf()

        /**
         * Adds a single [ExportableResult].
         */
        override fun add(result: ExportableResult<T>) {
            this.maybeFlushBuffer(1)

            synchronized(this) {
                this.results.add(result)
            }
        }

        /**
         * Adds a collection of [ExportableResult] instances.
         */
        override fun addAll(results: List<ExportableResult<T>>) {
            this.maybeFlushBuffer(results.size)

            this.log("Adding ${results.size} results...")
            if (results.size > this.bufferSize) {
                this.addInChunks(results)
            } else {
                synchronized(this) {
                    this.results.addAll(results)
                }
            }
        }

        /**
         * Internal method used to write a collection of [ExportableResult] instances in chunks.
         *
         * Writing in chunks will be more efficient than over-filling the buffer and having to flush
         * a large amount of results in one go.
         */
        private fun addInChunks(results: List<ExportableResult<T>>) {
            this.log("Adding in chunks...")

            results.chunked(this.bufferSize).map { chunk ->
                this.log("Adding chunk with ${chunk.size} results...")
                this.addAll(chunk)
            }
        }

        /**
         * Writes any remaining buffered results.
         */
        override fun close() {
            this.log("Writing any remaining buffered results...")
            if (results.isEmpty())
                return

            this.outputProvider.writeResultsFrom(this)

            synchronized(this) {
                this.results.clear()
            }
        }

        /**
         * Determines if the buffer needs flushing and flushes if necessary.
         */
        private fun maybeFlushBuffer(count: Int) {
            when {
                // No need to flush if adding the specified number of new results  won't overflow the buffer.
                (results.size + count) < this.bufferSize -> return
                else -> {
                    // Need to flush buffer to output provider
                    this.log("Flushing results buffer (size = ${results.size})...")
                    this.outputProvider.writeResultsFrom(this)

                    synchronized(this) {
                        this.results.clear()
                    }
                }
            }
        }

        private fun log(message: String) {
            if (this.verbose)
                println(message)
        }

        /**
         * Provides information about the module.
         */
        override val information = ModuleInformation(
                description = "Aggregates results that are able to be exported."
        )
    }

    /**
     * A [ResultAggregator] implementation which does nothing.
     *
     * Useful for situations where it is not required to aggregate results and export to an output source.
     */
    class DefaultResultAggregator<T> : ResultAggregator<T>() {
        override val results = emptyList<ExportableResult<T>>()

        override fun add(result: ExportableResult<T>) {
            // No op.
        }

        override fun addAll(results: List<ExportableResult<T>>) {
            // No op.
        }

        override fun close() {
            // No op.
        }

        override val information = ModuleInformation(
            "Default aggregator which simply does nothing."
        )
    }
}

/**
 * Provides the ability to output results from a [ResultAggregator].
 */
interface ResultOutputProvider<T> {

    /**
     * Writes any results from [source] to some output destination.
     */
    fun writeResultsFrom(source: ResultAggregator<T>)
}

/**
 * A collection of [ResultOutputProvider] implementations for common scenarios.
 */
object ResultOutputProviders {

    /**
     * Provides the ability to write a table of results to a text file.
     *
     * @param filename A file to write text results to.
     */
    class RawResultTableOutputProvider<T>(private val filename: String): ResultOutputProvider<T> {

        private val writer = Files.newBufferedWriter(Paths.get(this.filename))

        override fun writeResultsFrom(source: ResultAggregator<T>) {
            writer.use { out ->
                out.write(source.toString())
            }
        }
    }

    /**
     * Provides the ability to write results to a CSV file.
     *
     * @param filename A file to write CSV data to.
     */
    class CsvResultOutputProvider<T>(private val filename: String) : ResultOutputProvider<T> {

        private val csvWriter: CSVWriter = CSVWriter(
                Files.newBufferedWriter(Paths.get(this.filename))
        )

        private var headerHasBeenWriten: Boolean = false

        override fun writeResultsFrom(source: ResultAggregator<T>) {
            // Act on a view of the results to avoid thread-safety issues
            val results = source.results.toList()

            // Is there any results?
            if (results.isEmpty())
                return

            // Grab header information
            if (!headerHasBeenWriten) {
                val header = results.first()
                                    .export()
                                    .map { (name, _) -> name }

                // Write header row
                this.csvWriter.writeNext(header.toTypedArray())
                this.headerHasBeenWriten = true
            }

            // Write value rows
            results.map { result ->
                val exported = result.export()
                val values = exported.map { (_, value) -> value }

                this.csvWriter.writeNext(values.toTypedArray())
            }

            // Done!
            this.csvWriter.flush()
        }
    }
}

