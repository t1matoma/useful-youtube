package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

private const val ATOM_NAMESPACE = "http://www.w3.org/2005/Atom"
private const val YOUTUBE_NAMESPACE = "http://www.youtube.com/xml/schemas/2015"
private const val MEDIA_NAMESPACE = "http://search.yahoo.com/mrss/"

/** Atom feed served at `https://www.youtube.com/feeds/videos.xml?channel_id={id}`. */
@Serializable
@XmlSerialName("feed", ATOM_NAMESPACE, "")
data class RssFeedDto(
    @XmlElement(true)
    @XmlSerialName("title", ATOM_NAMESPACE, "")
    val title: String,
    val entries: List<RssEntryDto> = emptyList(),
)

@Serializable
@XmlSerialName("entry", ATOM_NAMESPACE, "")
data class RssEntryDto(
    @XmlElement(true)
    @XmlSerialName("videoId", YOUTUBE_NAMESPACE, "yt")
    val videoId: String,
    @XmlElement(true)
    @XmlSerialName("channelId", YOUTUBE_NAMESPACE, "yt")
    val channelId: String,
    @XmlElement(true)
    @XmlSerialName("title", ATOM_NAMESPACE, "")
    val title: String,
    @XmlElement(true)
    @XmlSerialName("published", ATOM_NAMESPACE, "")
    val published: String,
    val author: RssAuthorDto,
    val mediaGroup: RssMediaGroupDto? = null,
)

@Serializable
@XmlSerialName("author", ATOM_NAMESPACE, "")
data class RssAuthorDto(
    @XmlElement(true)
    @XmlSerialName("name", ATOM_NAMESPACE, "")
    val name: String,
)

@Serializable
@XmlSerialName("group", MEDIA_NAMESPACE, "media")
data class RssMediaGroupDto(
    val thumbnail: RssThumbnailDto? = null,
    @XmlElement(true)
    @XmlSerialName("description", MEDIA_NAMESPACE, "media")
    val description: String = "",
)

@Serializable
@XmlSerialName("thumbnail", MEDIA_NAMESPACE, "media")
data class RssThumbnailDto(
    val url: String,
)
