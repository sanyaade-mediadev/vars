package org.mbari.vars.arctic

import org.mbari.math.CoallateFunction
import org.mbari.movie.Timecode
import org.slf4j.LoggerFactory
import vars.annotation.VideoArchiveSetDAO
import vars.annotation.VideoFrame
import vars.ToolBox

/**
 * Created by brian on 1/29/14.
 */
class MergeData {
    private final log = LoggerFactory.getLogger(getClass())
    final File logFile
    final String videoArchiveName
    final offsetFrames = 29.97 * 5 // +/- 5 second offset
    final logRecords
    final toolBox = new ToolBox()

    final convertVideoFrameToFrames = { videoFrame ->
        def timecode = new Timecode(videoFrame.timecode)
        timecode.frames
    }

    final convertLogRecordToFrames = { LogRecord lr ->
        def t = lr.time
        def frames = Double.MAX_VALUE
        if (t) {
            def timecode = new Timecode("${t}:00")
            frames = timecode.frames
        }
        return frames
    }

    MergeData(String videoArchiveName, File logFile) {
        this.logFile = logFile
        this.videoArchiveName = videoArchiveName
        logRecords = LogReader.read(logFile)
    }

    Map<VideoFrame, LogRecord> collate() {
        def dao = toolBox.toolBelt.annotationDAOFactory.newVideoArchiveDAO()
        dao.startTransaction()
        def videoArchive = dao.findByName(videoArchiveName)
        if (!videoArchive) {
            throw new RuntimeException("Unable to find ${videoArchiveName} in the VARS database")
        }
        def videoFrames = videoArchive.videoFrames
        dao.endTransaction()
        dao.close()
        log.info("Found ${videoFrames?.size()} videoFrames")
        def collatedData = [:]
        try {
            collatedData = CoallateFunction.coallate(videoFrames, convertVideoFrameToFrames,
                logRecords, convertLogRecordToFrames, offsetFrames)
        }
        catch (Exception e) {
            def annoDates = videoFrames.collect {it.timecode} findAll { it != null } sort()
            def fileDates = logRecords.collect {it.time} findAll { it != null }  sort()
            log.warn("""\
                Unable to match dates between the annotations and the data in the ROV log file.

                Annotations:
                    start timecode:\t${annoDates[0]}
                    end timecode:\t${annoDates[-1]}

                $logFile
                    start time:\t${fileDates[0]}
                    end time:\t${fileDates[-1]}
            """.stripIndent(), e)
        }
        return collatedData
    }

    Map<VideoFrame, LogRecord> update(Map data) {
        VideoArchiveSetDAO dao = toolBox.toolBelt.annotationDAOFactory.newVideoArchiveSetDAO()
        def newData = [:]
        dao.startTransaction()
        data.each { VideoFrame videoFrame, LogRecord datum ->

            videoFrame = dao.find(videoFrame)

            def physicalData = videoFrame.physicalData
            physicalData.depth = datum.depth
            physicalData.latitude = datum.latitude
            physicalData.longitude = datum.longitude
            physicalData.temperature = datum.temperature

            newData[videoFrame] = datum

        }
        dao.endTransaction()
        dao.close()
        return newData
    }

    Map<VideoFrame, LogRecord> apply() {
        update(collate())
    }


}
