package com.djamal.ch.netflix.data

/** Catalogue de démonstration : flux de test publics (films libres Blender, etc.) et affiches aléatoires picsum. */
object DemoSource : IptvSource {

    private const val BUNNY = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
    private const val TEARS = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    private const val ANGEL = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"
    private const val SINTEL = "https://download.blender.org/durian/trailer/sintel_trailer-480p.mp4"
    private const val BIPBOP = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"

    private class Clip(val id: String, val title: String, val category: String, val url: String, val plot: String)

    private val clips = listOf(
        Clip("bunny", "Big Buck Bunny", "anim", BUNNY, "Un lapin géant au grand cœur décide de se venger de trois rongeurs qui le harcèlent."),
        Clip("tears", "Tears of Steel", "anim", TEARS, "À Amsterdam, un groupe de scientifiques tente de sauver le monde d'une armée de robots."),
        Clip("sintel", "Sintel", "anim", SINTEL, "Une jeune guerrière parcourt le monde à la recherche du bébé dragon qu'elle avait recueilli."),
        Clip("angel", "Angel One", "anim", ANGEL, "Un court extrait de démonstration en plusieurs qualités."),
        Clip("nuit", "La Nuit des robots", "action", TEARS, "Une course contre la montre dans une ville envahie par les machines."),
        Clip("dragon", "Le Dernier Dragon", "action", SINTEL, "Un voyage à travers des terres glacées pour retrouver un ami perdu."),
        Clip("foret", "La Forêt enchantée", "action", BUNNY, "Les habitants d'une forêt paisible s'unissent contre trois brutes."),
        Clip("signal", "Signal", "action", BIPBOP, "Un mystérieux signal de test diffusé en boucle."),
        Clip("aube", "Avant l'aube", "drame", ANGEL, "Une nuit, deux destins, une décision."),
        Clip("retour", "Le Retour", "drame", SINTEL, "Des années plus tard, elle revient sur les lieux de son enfance."),
        Clip("acier", "Cœur d'acier", "drame", TEARS, "Un ingénieur doit choisir entre son amour et son œuvre."),
        Clip("prairie", "La Prairie", "drame", BUNNY, "Une journée ordinaire qui ne l'est pas du tout."),
    )
    private val byId = clips.associateBy { it.id }

    private fun poster(seed: String) = "https://picsum.photos/seed/$seed/400/600"
    private fun backdrop(seed: String) = "https://picsum.photos/seed/$seed/1280/720"

    override suspend fun catalog() = Catalog(
        liveCategories = listOf(Category("test", "Flux de test"), Category("cinema", "Cinéma")),
        live = listOf(
            Media(Kind.LIVE, "bipbop", "Apple Bip Bop", backdrop("bipbop"), "test"),
            Media(Kind.LIVE, "angel", "Angel One TV", backdrop("angeltv"), "test"),
            Media(Kind.LIVE, "bunny", "Bunny Channel", backdrop("bunnytv"), "cinema"),
            Media(Kind.LIVE, "tears", "Steel TV", backdrop("steeltv"), "cinema"),
        ),
        movieCategories = listOf(Category("anim", "Animation"), Category("action", "Action & aventure"), Category("drame", "Drames")),
        movies = clips.mapIndexed { i, clip ->
            Media(Kind.MOVIE, clip.id, clip.title, poster(clip.id), clip.category, rating = "${6 + i % 4}.${(i * 3) % 10}", plot = clip.plot, added = i.toLong())
        },
        seriesCategories = listOf(Category("blender", "Séries animées")),
        series = listOf(
            Media(Kind.SERIES, "blender", "Blender Open Movies", poster("blender"), "blender", rating = "8.1", plot = "Les courts métrages libres de la Blender Foundation, réunis en une saison."),
            Media(Kind.SERIES, "robots", "Chroniques robotiques", poster("robots"), "blender", rating = "7.4", plot = "Deux saisons d'histoires de machines et d'humains."),
        ),
    )

    override suspend fun details(media: Media) = when (media.kind) {
        Kind.SERIES -> {
            val seasons = if (media.id == "robots") 2 else 1
            Details(
                plot = media.plot, backdrop = backdrop(media.id), year = "2008", genre = "Animation",
                episodes = (1..seasons).flatMap { s ->
                    clips.filter { it.category == "anim" }.mapIndexed { i, clip ->
                        Episode(clip.id, s, i + 1, clip.title, backdrop("${media.id}$s$i"), clip.plot, null)
                    }
                },
            )
        }
        else -> Details(plot = media.plot, backdrop = backdrop(media.id), year = "2012", genre = "Démo", duration = "00:10:00")
    }

    override fun streamUrl(kind: Kind, id: String, ext: String?) = byId[id]?.url ?: BIPBOP
}
