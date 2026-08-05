package com.example.books_kmp.data.tags

import com.example.books_kmp.domain.model.Tag
import kotlin.time.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TagDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String = "",
    @SerialName("user_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val userId: String = "",
    val name: String = "",
    @SerialName("is_default")
    val isDefault: Boolean = false,
    // Server-maintained, like `id`/`userId`: omitted on write only while left at its default
    // (null) — a decode-then-encode round trip would resend it. See PostgrestInstantEncodingTest.
    // Null only when absent from the response; the `tags` table always populates it via trigger,
    // so `toTag()` below treats a null as a data-integrity error rather than a real timestamp.
    @SerialName("updated_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val updatedAt: Instant? = null,
)

fun TagDto.toTag(): Tag =
    Tag(
        id = id,
        name = name,
        isDefault = isDefault,
        updatedAt = updatedAt ?: error("TagDto($id) is missing server-maintained updated_at"),
    )
