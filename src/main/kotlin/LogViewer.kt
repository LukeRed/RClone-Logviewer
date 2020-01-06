
import javafx.application.Application
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.ranges.contains
import kotlin.ranges.rangeTo

val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm")
val backupRuns = mutableMapOf<Date, MutableMap<String, File>>()
val logFiles = mutableListOf<LogFile>()

fun main(args: Array<String>) {
    Application.launch(LogViewerApp::class.java, "")
}

fun scanAndCheck(path: String) {
    val logFolder = File(path)
    val logfileList = logFolder.list()
    backupRuns.clear()
    logFiles.clear()
    var tempRuns = mutableMapOf<Date, MutableMap<String, File>>()
    // For each pathname in the pathnames array
    for (pathname in logfileList) {
        if(pathname.endsWith("txt")) {
            try {
                val date = dateFormat.parse(pathname.substring(0, 16))
                val drive = pathname.substring(17, pathname.length-4)
                tempRuns[date] = tempRuns[date]?:mutableMapOf();
                tempRuns[date]?.put(drive, File(logFolder, pathname))
            } catch (e: ParseException) {
                println("Invalid file: $pathname")
            }
        }
        // Print the names of files and directories
    }
    for ((date, logfiles) in tempRuns) {
        println("$date")
        for ((title, file) in logfiles) {
            print("  └── $title - ")
            logFiles.add(LogFile(file, date, title))
        }
        println()
    }
    backupRuns.putAll(tempRuns)
}


class LogFile(val date: Date, val drive: String) {
    var total = 0L
    var averageSpeed = 0L
    var errors = 0
    var checks = 0
    var transferred = 0
    var duration = 0.0

    var added: List<String> = mutableListOf()
    private var moved: MutableList<String> = mutableListOf()
    var changed: List<String> = mutableListOf()
    var deleted: List<String> = mutableListOf()

    constructor(path: File, date: Date, drive: String) : this(date, drive) {
        val logfileLines = path.readLines()
        val count = logfileLines.size
        // Check if Transferred line is where we expect it
        if(logfileLines[count-6].startsWith("Transferred:")) {
            // Handle final status report
            val transferredLine =  logfileLines[count-6].split(":")[1].split(",")

            // Extract total
            val totalHR = transferredLine[0].split("/")[1].trim()
            total = fromHRBytes(totalHR)

            averageSpeed = fromHRBytes(transferredLine[2].split("/")[0].trim())


            duration = fromHRTime(logfileLines[count-2].split(":")[1].trim())
            errors = logfileLines[count-5].split(":")[1].trim().toInt()
            checks = logfileLines[count-4].split("/")[1].split(",")[0].trim().toInt()
            transferred = logfileLines[count-3].split("/")[1].split(",")[0].trim().toInt()

        } else {
            println("logfile $path incomplete")
        }
        var addedTemp = mutableListOf<String>()
        var changedTemp = mutableListOf<String>()
        for (currentLine in logfileLines) {
            if(currentLine.endsWith("(new)")) {
                val filename = currentLine.substring(28, currentLine.length-14);
                if(moved.contains(filename)) {
                    moved.remove(filename)
                    changedTemp.add(filename)
                } else {
                    addedTemp.add(filename)
                }
            }
            if(currentLine.endsWith("Moved (server side)")) {
                moved.add(currentLine.substring(28, currentLine.length-21))
            }
        }
        added = addedTemp.distinct()
        changed = changedTemp.distinct()
        deleted = moved.distinct()

        println("New: ${added.size}, Changed: ${changed.size}, Removed: ${deleted.size} - Total: ${toHRBytes(total)} (${toHRBytes(averageSpeed)}/s in ${toHRTime(duration)}) - $errors Errors, $transferred transferred, $checks checks")
    }

}

fun fromHRTime(t: String): Double {
    val time = t.trim()
    var total = 0.0
    if(time == "-") return 0.0
    var i = 0
    while(i < time.length) {
        var j = i
        while(time[j].isDigit()||time[j]=='.') { j++ }
        var k = j
        while(k < time.length && !time[k].isDigit()) { k++ }
        if(k+1 == time.length) k = time.length
        val value = time.substring(i, j).toDouble()
        val letter = time.substring(j,k)
        total += when(letter) {
            "ms" -> value / 1000.0
            "s" -> value
            "m" -> value * 60
            "h" -> value * 60 * 60
            "d" -> value * 24 * 60 * 60
            else -> 0.0
        }
        i = k + 1
    }
    return total
}
fun fromHRBytes(text: String): Long {
    val split = text.split(" ");
    val bytes = split[0].toDouble()
    return when(split[1].trim().toLowerCase()){
        "bytes" -> bytes
        "kbytes" -> bytes * 1024
        "mbytes" -> bytes * 1024 * 1024
        "gbytes" -> bytes * 1024 * 1024 * 1024
        "tbytes" -> bytes * 1024 * 1024 * 1024 * 1024
        "pbytes" -> bytes * 1024 * 1024 * 1024 * 1024 * 1024
        else -> bytes
    }.roundToLong()
}
fun toHRBytes(bytes: Long): String {
    return when(bytes) {
        in (0..1024) -> "$bytes "
        in (1024+1..(1024*1024)) -> "${(bytes / 10.24).roundToInt()/100.0} k"
        in (1024*1024+1..(1024*1024*1024)) -> "${(bytes / 1024/10.24).roundToInt()/100.0} M"
        in (1024L*1024L*1024L+1L..(1024L*1024L*1024L*1024L)) -> "${(bytes / 1024/1024/10.24).roundToInt()/100.0} G"
        in (1024L*1024L*1024L*1024L+1L..(1024L*1024L*1024L*1024L*1024L)) -> "${(bytes / (1024/1024/1024/10.24)).roundToInt()/100.0} T"
        in (1024L*1024L*1024L*1024L*1024L+1L..Long.MAX_VALUE) -> "${(bytes / 1024 /1024/1024/1024/10.24).roundToInt()/100.0} P"
        else -> "Undefined "
    } + "Bytes"
}
fun toHRTime(seconds: Double): String {
    return when(seconds) {
        in (60.0..(60.0 * 15)) -> "${(seconds / 60).roundToInt()} min, ${if(seconds % 1 == 0.0) {(seconds % 60).toInt()} else {seconds % 60}} seconds"
        in ((15 * 60.0)..(60.0 * 60)) -> "${(seconds / 60).roundToInt()} min"
        in ((60.0 * 60)..(60.0 * 60 * 24)) -> "${(seconds / 60 / 60).roundToInt()} hours, ${((seconds / 60) % 60).roundToInt()} minutes"
        in ((60.0 * 60 * 24)..Double.MAX_VALUE) -> "${(seconds / 60 / 60 / 24).roundToInt()} days, ${((seconds / 60 / 60) % 24).roundToInt()} hours"
        else -> "$seconds seconds"
    }
}