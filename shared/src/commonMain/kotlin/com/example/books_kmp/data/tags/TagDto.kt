package com.example.books_kmp.data.tags

import com.example.books_kmp.domain.model.Tag
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
)

fun TagDto.toTag(): Tag = Tag(id = id, name = name, isDefault = isDefault)
