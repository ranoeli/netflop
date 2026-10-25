package com.djamal.ch.netflix.data

enum class Kind { LIVE, MOVIE, SERIES }

data class Category(val id: String, val name: String)

data class Media(
    val kind: Kind,
    val id: String,
    val name: String,
    val image: String?,
    val categoryId: String?,
    val rating: String? = null,
    val plot: String? = null,
    val ext: String? = null,
    val added: Long = 0,
    // Renseignés uniquement pour la rangée « Reprendre la lecture »
    val progress: Float? = null,
    val resume: PlayRequest? = null,
)

data class Episode(
    val id: String,
    val season: Int,
    val number: Int,
    val title: String,
    val image: String?,
    val plot: String?,
    val ext: String?,
)

data class Details(
    val plot: String? = null,
    val backdrop: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val duration: String? = null,
    val cast: String? = null,
    val rating: String? = null,
    val ext: String? = null,
    val episodes: List<Episode> = emptyList(),
)

data class Catalog(
    val liveCategories: List<Category>,
    val live: List<Media>,
    val movieCategories: List<Category>,
    val movies: List<Media>,
    val seriesCategories: List<Category>,
    val series: List<Media>,
)

/** Tout ce qu'il faut pour lancer une lecture, et la reprendre plus tard. */
data class PlayRequest(
    val key: String,
    val title: String,
    val image: String?,
    val url: String,
    val live: Boolean,
)

interface IptvSource {
    suspend fun catalog(): Catalog
    suspend fun details(media: Media): Details

    /** Pour un épisode : kind = SERIES et id = id de l'épisode. */
    fun streamUrl(kind: Kind, id: String, ext: String?): String
}

const val USER_AGENT = "Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36"
