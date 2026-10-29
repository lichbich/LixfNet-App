package com.lixfnet.app.data.repository

data class PlatformTitleEntry(
    val vietnameseTitle: String,
    val originalTitle: String,
    val slug: String = "",
    val tmdbId: Int? = null,
    val year: Int = 2026,
    val type: String = "series",
    val posterUrl: String = "",
    val backdropUrl: String = ""
)

object StreamingPlatformData {

    /**
     * Official Netflix Vietnam Tudum Dictionary
     * Synchronized 100% with Web NetLix dictionary.
     * Contains valid tmdbId for official TMDB poster & backdrop resolution.
     */
    val NETFLIX_VN_TUDUM_DICTIONARY: Map<String, PlatformTitleEntry> = mapOf(
        // Series
        "Doctor X: Mafia in White" to PlatformTitleEntry(
            vietnameseTitle = "Bác Sĩ X: Mafia Áo Trắng",
            originalTitle = "Doctor X: Mafia in White",
            slug = "bac-si-x",
            tmdbId = 293610,
            year = 2026,
            type = "series"
        ),
        "Doctor X" to PlatformTitleEntry(
            vietnameseTitle = "Bác Sĩ X: Mafia Áo Trắng",
            originalTitle = "Doctor X: Mafia in White",
            slug = "bac-si-x",
            tmdbId = 293610,
            year = 2026,
            type = "series"
        ),
        "Doctor-X" to PlatformTitleEntry(
            vietnameseTitle = "Bác Sĩ X: Mafia Áo Trắng",
            originalTitle = "Doctor X: Mafia in White",
            slug = "bac-si-x",
            tmdbId = 293610,
            year = 2026,
            type = "series"
        ),
        "100 Days of Deception" to PlatformTitleEntry(
            vietnameseTitle = "100 Ngày Lừa Dối",
            originalTitle = "100 Days of Deception",
            slug = "100-ngay-lua-doi",
            tmdbId = 293607,
            year = 2026,
            type = "series"
        ),
        "Take Charge of My Heart" to PlatformTitleEntry(
            vietnameseTitle = "Nạp Đầy Trái Tim",
            originalTitle = "Take Charge of My Heart",
            slug = "nap-day-trai-tim-phan-1",
            tmdbId = 289423,
            year = 2026,
            type = "series"
        ),
        "Below" to PlatformTitleEntry(
            vietnameseTitle = "Below: Ác Mộng Đại Dương",
            originalTitle = "Below",
            slug = "below",
            tmdbId = 285322,
            year = 2026,
            type = "series"
        ),
        "Where the Mask Ends" to PlatformTitleEntry(
            vietnameseTitle = "Gả Kim Thoa",
            originalTitle = "Where the Mask Ends",
            slug = "ga-kim-thoa-ga-tram-vang",
            tmdbId = 300146,
            year = 2026,
            type = "series"
        ),
        "The Apothecary Diaries" to PlatformTitleEntry(
            vietnameseTitle = "Dược Sư Tự Sự",
            originalTitle = "The Apothecary Diaries",
            slug = "duoc-su-tu-su-phan-1",
            tmdbId = 220542,
            year = 2023,
            type = "series"
        ),
        "The Apothecary Diaries: Season 3" to PlatformTitleEntry(
            vietnameseTitle = "Dược Sư Tự Sự (Phần 3)",
            originalTitle = "The Apothecary Diaries",
            slug = "duoc-su-tu-su-phan-3",
            tmdbId = 220542,
            year = 2026,
            type = "series"
        ),
        "Irreplaceable" to PlatformTitleEntry(
            vietnameseTitle = "Không Thể Thay Thế",
            originalTitle = "Irreplaceable",
            slug = "khong-the-thay-the-2026",
            tmdbId = 297982,
            year = 2026,
            type = "series"
        ),
        "Firefly Wedding" to PlatformTitleEntry(
            vietnameseTitle = "Đám Cưới Đom Đóm",
            originalTitle = "Firefly Wedding",
            slug = "dam-cuoi-dom-dom",
            tmdbId = 320909,
            year = 2026,
            type = "series"
        ),
        "The Early Spring" to PlatformTitleEntry(
            vietnameseTitle = "Đầu Xuân Tươi Sáng",
            originalTitle = "The Early Spring",
            slug = "dau-xuan-tuoi-sang",
            tmdbId = 299952,
            year = 2026,
            type = "series"
        ),
        "Cyberpunk: Edgerunners" to PlatformTitleEntry(
            vietnameseTitle = "Cyberpunk: Edgerunners",
            originalTitle = "Cyberpunk: Edgerunners",
            slug = "cyberpunk-edgerunners-phan-2",
            tmdbId = 105248,
            year = 2022,
            type = "series"
        ),
        "Cyberpunk: Edgerunners 2" to PlatformTitleEntry(
            vietnameseTitle = "Cyberpunk: Edgerunners (Phần 2)",
            originalTitle = "Cyberpunk: Edgerunners",
            slug = "cyberpunk-edgerunners-phan-2",
            tmdbId = 105248,
            year = 2026,
            type = "series"
        ),
        "Dragon Ball Super: Beerus" to PlatformTitleEntry(
            vietnameseTitle = "Dragon Ball Super: Beerus",
            originalTitle = "Dragon Ball Super: Beerus",
            slug = "dragon-ball-super-beerus",
            tmdbId = 312359,
            year = 2026,
            type = "series"
        ),
        "Extraordinary Attorney Minami" to PlatformTitleEntry(
            vietnameseTitle = "Nữ Luật Sư Kỳ Lạ Minami",
            originalTitle = "Extraordinary Attorney Minami",
            slug = "nu-luat-su-ky-la-minami",
            tmdbId = 332713,
            year = 2026,
            type = "series"
        ),
        "Doctor X Surgeon Michiko Daimon" to PlatformTitleEntry(
            vietnameseTitle = "Bác Sĩ X Ngoại Khoa (Phần 1)",
            originalTitle = "Doctor-X: Surgeon Michiko Daimon",
            slug = "bac-si-x-ngoai-khoa-daimon-michiko-phan-1",
            tmdbId = 46052,
            year = 2012,
            type = "series"
        ),

        // Films
        "Bunny!" to PlatformTitleEntry(
            vietnameseTitle = "Thỏ Ơi!!",
            originalTitle = "Hey, Bunny!",
            slug = "tho-oi",
            tmdbId = 1528577,
            year = 2026,
            type = "movie"
        ),
        "Fish, Fists and Ambergris" to PlatformTitleEntry(
            vietnameseTitle = "Truy Tìm Long Diên Hương",
            originalTitle = "Fish, Fists and Ambergris",
            slug = "truy-tim-long-dien-huong",
            tmdbId = 1569555,
            year = 2025,
            type = "movie"
        ),
        "Animals" to PlatformTitleEntry(
            vietnameseTitle = "Bản Năng Thú",
            originalTitle = "Animals",
            slug = "ban-nang-thu",
            tmdbId = 1236045,
            year = 2026,
            type = "movie"
        ),
        "Tomb Raider" to PlatformTitleEntry(
            vietnameseTitle = "Tomb Raider: Huyền Thoại Bắt Đầu",
            originalTitle = "Tomb Raider",
            slug = "tomb-raider-huyen-thoai-bat-dau",
            tmdbId = 338970,
            year = 2018,
            type = "movie"
        ),
        "Evil Dead Burn" to PlatformTitleEntry(
            vietnameseTitle = "Ma Cây: Lửa Địa Ngục",
            originalTitle = "Evil Dead Burn",
            slug = "ma-cay-troi-day",
            tmdbId = 1212763,
            year = 2026,
            type = "movie"
        ),
        "Kong: Skull Island" to PlatformTitleEntry(
            vietnameseTitle = "Kong: Đảo Đầu Lâu",
            originalTitle = "Kong: Skull Island",
            slug = "kong-dao-dau-lau",
            tmdbId = 293167,
            year = 2017,
            type = "movie"
        ),
        "Mai" to PlatformTitleEntry(
            vietnameseTitle = "Mai",
            originalTitle = "Mai",
            slug = "mai",
            tmdbId = 1210973,
            year = 2024,
            type = "movie"
        ),
        "The Silence of the Lambs" to PlatformTitleEntry(
            vietnameseTitle = "Sự Im Lặng Của Bầy Cừu",
            originalTitle = "The Silence of the Lambs",
            slug = "su-im-lang-cua-bay-cuu",
            tmdbId = 274,
            year = 1991,
            type = "movie"
        ),
        "Ben-Hur" to PlatformTitleEntry(
            vietnameseTitle = "Ben-Hur",
            originalTitle = "Ben-Hur",
            slug = "hoang-tu-benhur",
            tmdbId = 271969,
            year = 2016,
            type = "movie"
        ),
        "Crazy Rich Asians" to PlatformTitleEntry(
            vietnameseTitle = "Con Nhà Siêu Giàu Châu Á",
            originalTitle = "Crazy Rich Asians",
            slug = "con-nha-sieu-giau-chau-a",
            tmdbId = 455207,
            year = 2018,
            type = "movie"
        ),
        "Black Adam" to PlatformTitleEntry(
            vietnameseTitle = "Black Adam",
            originalTitle = "Black Adam",
            slug = "black-adam",
            tmdbId = 436270,
            year = 2022,
            type = "movie"
        ),
        "Surviving Hurricane Otis" to PlatformTitleEntry(
            vietnameseTitle = "Vượt Qua Bão Otis",
            originalTitle = "Surviving Hurricane Otis",
            slug = "vuot-qua-bao-otis",
            tmdbId = 1780005,
            year = 2026,
            type = "movie"
        ),
        "The 4 Rascals" to PlatformTitleEntry(
            vietnameseTitle = "Bộ Tứ Báo Thủ",
            originalTitle = "The 4 Rascals",
            slug = "bo-tu-bao-thu",
            tmdbId = 1391703,
            year = 2025,
            type = "movie"
        ),
        "Blackout" to PlatformTitleEntry(
            vietnameseTitle = "Cuộc Chiến Bí Ẩn",
            originalTitle = "Blackout",
            slug = "cuoc-chien-bi-an",
            tmdbId = 1624183,
            year = 2026,
            type = "movie"
        )
    )

    /**
     * Apple TV+ Originals dictionary with TMDB IDs
     */
    val APPLE_TV_ORIGINALS: List<PlatformTitleEntry> = listOf(
        PlatformTitleEntry(
            vietnameseTitle = "Những Điệp Viên Hết Thời",
            originalTitle = "Slow Horses",
            slug = "nhung-diep-vien-het-thoi-phan-1",
            tmdbId = 94810,
            year = 2022,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Huấn Luyện Viên Ted Lasso (Phần 1)",
            originalTitle = "Ted Lasso",
            slug = "ted-lasso-phan-1",
            tmdbId = 97546,
            year = 2020,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Hầm Silo (Phần 1)",
            originalTitle = "Silo",
            slug = "ham-silo-phan-1",
            tmdbId = 125988,
            year = 2023,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Người Chạy Xuyên Không Gian",
            originalTitle = "Dark Matter",
            slug = "vat-chat-toi-phan-1",
            tmdbId = 203857,
            year = 2024,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Cuộc Chiến Không Gian",
            originalTitle = "For All Mankind",
            slug = "cuoc-chien-khong-gian-phan-1",
            tmdbId = 87917,
            year = 2019,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Cắt Rời Ký Ức (Phần 1)",
            originalTitle = "Severance",
            slug = "cat-roi-ky-uc-phan-1",
            tmdbId = 95396,
            year = 2022,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Lực Lượng Nền Tảng",
            originalTitle = "Foundation",
            slug = "luc-luong-nen-tang-phan-1",
            tmdbId = 93740,
            year = 2021,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Bản Tin Sáng (Phần 1)",
            originalTitle = "The Morning Show",
            slug = "ban-tin-sang-phan-1",
            tmdbId = 80752,
            year = 2019,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Monarch: Thế Giới Quái Thú (Phần 1)",
            originalTitle = "Monarch: Legacy of Monsters",
            slug = "monarch-the-gioi-quai-thu-phan-1",
            tmdbId = 156902,
            year = 2023,
            type = "series"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Liệu Pháp Sự Thật (Phần 1)",
            originalTitle = "Shrinking",
            slug = "shrinking-phan-1",
            tmdbId = 125992,
            year = 2023,
            type = "series"
        )
    )

    val DEFAULT_SERIES_TITLES = listOf(
        "Doctor X: Mafia in White",
        "100 Days of Deception",
        "Take Charge of My Heart",
        "Below",
        "Where the Mask Ends",
        "Dragon Ball Super: Beerus",
        "Irreplaceable",
        "Firefly Wedding",
        "The Early Spring",
        "Extraordinary Attorney Minami"
    )

    val DEFAULT_FILMS_TITLES = listOf(
        "Bunny!",
        "Fish, Fists and Ambergris",
        "Animals",
        "Tomb Raider",
        "Evil Dead Burn",
        "Kong: Skull Island",
        "Mai",
        "The Silence of the Lambs",
        "Ben-Hur",
        "Crazy Rich Asians"
    )

    val NETFLIX_VN_SERIES_TUDUM: List<PlatformTitleEntry> by lazy {
        DEFAULT_SERIES_TITLES.mapNotNull { findTudumEntry(it) }
    }

    val NETFLIX_VN_FILMS_TUDUM: List<PlatformTitleEntry> by lazy {
        DEFAULT_FILMS_TITLES.mapNotNull { findTudumEntry(it) }
    }

    fun cleanTudumTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex(":\\s*(Season\\s*\\d+|Limited\\s*Series|\\d+)$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun findTudumEntry(rawTitle: String): PlatformTitleEntry? {
        val clean = cleanTudumTitle(rawTitle)

        // 1. Exact or Clean match in dictionary
        NETFLIX_VN_TUDUM_DICTIONARY[rawTitle]?.let { return it }
        NETFLIX_VN_TUDUM_DICTIONARY[clean]?.let { return it }

        // 2. Case-insensitive dictionary match
        NETFLIX_VN_TUDUM_DICTIONARY.entries.find {
            it.key.equals(clean, ignoreCase = true) || it.key.equals(rawTitle, ignoreCase = true)
        }?.value?.let { return it }

        // 3. Match in Apple TV list
        APPLE_TV_ORIGINALS.find {
            it.originalTitle.equals(clean, ignoreCase = true) ||
            it.originalTitle.equals(rawTitle, ignoreCase = true) ||
            it.vietnameseTitle.equals(clean, ignoreCase = true)
        }?.let { return it }

        // 4. Substring dictionary match
        NETFLIX_VN_TUDUM_DICTIONARY.entries.find {
            val k = it.key.lowercase()
            val c = clean.lowercase()
            k.contains(c) || c.contains(k)
        }?.value?.let { return it }

        return null
    }
}
