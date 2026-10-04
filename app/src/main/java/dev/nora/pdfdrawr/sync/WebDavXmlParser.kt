package dev.nora.pdfdrawr.sync

import org.xmlpull.v1.XmlPullParser

object WebDavXmlParser {
    private val ns: String? = null

    fun readFeed(parser: XmlPullParser): List<WebDavFile> {
        val entries = mutableListOf<WebDavFile>()

        parser.require(XmlPullParser.START_TAG, ns, "d:multistatus")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.name == "d:response") {
                entries.add(readEntry(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    private fun readEntry(parser: XmlPullParser): WebDavFile {
        parser.require(XmlPullParser.START_TAG, ns, "d:response")
        var path: String? = null
        var lastModified: String? = null
        var isDirectory = false
        var fileType: String? = null;
        var fileName: String? = null;
        var size: String? = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "d:href" -> path = readPath(parser)
                "d:propstat" -> {
                    val props = readPropStat(parser)
                    if (props.lastModified != null) lastModified = props.lastModified
                    if (props.fileType != null) fileType = props.fileType
                    if (props.fileName != null) fileName = props.fileName
                    if (props.size != null) size = props.size
                    if (props.isDirectory) isDirectory = true
                }
                else -> skip(parser)
            }
        }
        return WebDavFile(path, lastModified, isDirectory, fileType, fileName, size)
    }

    private fun readPath(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "d:href")
        val path = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "d:href")
        return path
    }

    private fun readPropStat(parser: XmlPullParser): PropStatResult {
        parser.require(XmlPullParser.START_TAG, ns, "d:propstat")
        var lastModified: String? = null
        var isDirectory = false
        var contentType: String? = null
        var fileName: String? = null
        var size: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.name == "d:prop") {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.eventType != XmlPullParser.START_TAG) {
                        continue
                    }
                    when (parser.name) {
                        "d:getlastmodified" -> lastModified = readLastModified(parser)
                        "d:resourcetype" -> isDirectory = readResourceType(parser)
                        "d:getcontenttype" -> contentType = readContentType(parser)
                        "d:displayname" -> fileName = readFilename(parser)
                        "d:getcontentlength" -> size = readFileSize((parser))
                        else -> skip(parser)
                    }
                }
            } else {
                skip(parser)
            }
        }
        return PropStatResult(lastModified, isDirectory, contentType, fileName, size)
    }

    private fun readResourceType(parser: XmlPullParser): Boolean {
        parser.require(XmlPullParser.START_TAG, null, "d:resourcetype")
        var isCollection = false

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "d:collection") {
                isCollection = true
                parser.next()
            }
        }
        return isCollection
    }

    private fun readContentType(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "d:getcontenttype")
        val type = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "d:getcontenttype")
        return type
    }

    private fun readLastModified(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "d:getlastmodified")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "d:getlastmodified")
        return result
    }

    private fun readFilename(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "d:displayname")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "d:displayname")
        return result
    }

    private fun readFileSize(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "d:getcontentlength")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "d:getcontentlength")
        return result
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private data class PropStatResult(
        val lastModified: String?,
        val isDirectory: Boolean,
        val fileType: String?,
        val fileName: String?,
        val size: String?
    )
}