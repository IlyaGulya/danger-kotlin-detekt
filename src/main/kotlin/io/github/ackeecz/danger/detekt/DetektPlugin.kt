package io.github.ackeecz.danger.detekt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import systems.danger.kotlin.sdk.DangerPlugin
import java.io.File
import java.io.FileInputStream

/**
 * Plugin for a danger-kotlin processing outputs of detekt tool.
 */
object DetektPlugin : DangerPlugin() {

    override val id = "danger-kotlin-detekt"

    private val DEFAULT_REPORTER = CustomReporter { message, filePath, line ->
        context.warn(
            message = message,
            file = filePath,
            line = line,
        )
    }

    /**
     * Parse XML output of detekt and report inline comment
     * to the pull request.
     *
     * @param reportFiles files representing detekt XML output
     */
    fun parseAndReport(vararg reportFiles: File) {
        val mapper = XmlMapper()
        reportFiles.forEach { file ->
            FileInputStream(file).use { fileInputStream ->
                val report = parse(mapper, fileInputStream)
                report(report)
            }
        }
    }

    fun parseAndCustomReport(reporter: CustomReporter, vararg reportFiles: File) {
        val mapper = XmlMapper()
        reportFiles.forEach { file ->
            FileInputStream(file).use { fileInputStream ->
                val report = parse(mapper, fileInputStream)
                customReport(report, reporter)
            }
        }
    }

    private fun parse(
        mapper: XmlMapper,
        fileInputStream: FileInputStream
    ): DetektReport {
        return mapper.readValue(
            fileInputStream,
            DetektReport::class.java
        )
    }

    private fun report(report: DetektReport) {
        customReport(report, DEFAULT_REPORTER)
    }

    private fun customReport(report: DetektReport, reporter: CustomReporter) {
        report.files.forEach { file ->
            val realFile = File(file.name)
            file.errors.forEach { error ->
                val line = error.line.toIntOrNull() ?: 0
                val message = "Detekt: ${error.message}, rule: ${error.source}"
                val filePath = realFile.absolutePath.removePrefix(
                    "${File("").absolutePath}/"
                )
                reporter.onReport(
                    message = message,
                    filePath = filePath,
                    line = line
                )
            }
        }
    }
}

fun interface CustomReporter {
    fun onReport(message: String, filePath: String, line: Int)
}

@JacksonXmlRootElement(namespace = "checkstyle")
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class DetektReport(
    @field:JsonProperty("file")
    @field:JacksonXmlElementWrapper(useWrapping = false)
    val files: List<ReportFile> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ReportFile(
    @field:JacksonXmlProperty val name: String = "",
    @field:JsonProperty("error")
    @field:JacksonXmlElementWrapper(useWrapping = false) val errors: List<ReportError> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ReportError(
    @field:JacksonXmlProperty val line: String = "",
    @field:JacksonXmlProperty val column: String = "",
    @field:JacksonXmlProperty val severity: String = "",
    @field:JacksonXmlProperty val message: String = "",
    @field:JacksonXmlProperty val source: String = "",
)
