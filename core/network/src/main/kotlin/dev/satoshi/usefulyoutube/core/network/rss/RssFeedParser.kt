package dev.satoshi.usefulyoutube.core.network.rss

import dev.satoshi.usefulyoutube.core.network.model.RssFeedDto
import nl.adaptivity.xmlutil.serialization.XML
import javax.inject.Inject

/** Parses YouTube channel feeds, ignoring elements the app does not use. */
class RssFeedParser
    @Inject
    constructor() {
        private val xml =
            XML.v1 {
                policy {
                    ignoreUnknownChildren()
                }
            }

        fun parse(content: String): RssFeedDto = xml.decodeFromString(RssFeedDto.serializer(), content)
    }
