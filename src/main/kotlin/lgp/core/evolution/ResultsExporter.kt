package lgp.core.evolution

import com.opencsv.CSVWriter
import lgp.core.environment.Environment
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

abstract class ResultAggregator<T>(val environment: Environment<T>) : Module {
    abstract val results: List<ExportableResult<T>>

    abstract fun add(result: ExportableResult<T>)
    abstract fun addAll(results: List<ExportableResult<T>>)

}

/**
 * A simple result source that builds an internal collection of results.
 *
 * This implementation is intended to be used within the system, as part of the environment context. Such a
 * usage will allow for internal components to aggregate their results to one central location.
 */
class BaseResultAggregator<T>(environment: Environment<T>) : ResultAggregator<T>(environment) {

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
    fun clear() {
        this.results.clear()
    }

    override fun toString(): String {
        if (this.results.isEmpty())
            return "No results."

        val table = StringBuilder()
        val firstResult = this.results.first().export()
        val header = firstResult.map { result -> result.first }.toList()

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
 * Provides the ability to output results from a [ResultSource].
 */
interface ResultOutputProvider<T> {
    fun writeResultsFrom(source: ResultAggregator<T>)
}

/**
 * A collection of [ResultOutputProvider] implementations for common scenarios.
 */
object ResultOutputProviders {

    /**
     *
     */
    class RawResultTableOutputProvider<T>(val filename: String): ResultOutputProvider<T> {

        private val writer = Files.newBufferedWriter(Paths.get(this.filename))

        override fun writeResultsFrom(source: ResultAggregator<T>) {
            writer.use { out ->
                out.write(source.toString())
            }
        }
    }

    /**
     * Provides the ability to write results to a CSV file.
     */
    class CsvResultOutputProvider<T>(val filename: String) : ResultOutputProvider<T> {

        private val csvWriter: CSVWriter = CSVWriter(
                Files.newBufferedWriter(Paths.get(this.filename))
        )

        override fun writeResultsFrom(source: ResultAggregator<T>) {
            // Is there any results?
            if (source.results.isEmpty())
                return

            // Grab header information
            val first = source.results.first().export()
            val header = first.map { (name, _) -> name }

            // Write header row
            this.csvWriter.writeNext(header.toTypedArray())

            // Write value rows
            source.results.map { result ->
                val exported = result.export()
                val values = exported.map { (_, value) -> value }

                this.csvWriter.writeNext(values.toTypedArray())
            }

            // Done!
            this.csvWriter.flush()
        }
    }
}

/**
 * Exports results using a given [ResultOutputProvider].
 */
class ResultExporter<T>(val outputProvider: ResultOutputProvider<T>) {

    /**
     * Exports [results] using [outputProvider].
     */
    fun export(results: ResultAggregator<T>) {
        this.outputProvider.writeResultsFrom(results)
    }

}