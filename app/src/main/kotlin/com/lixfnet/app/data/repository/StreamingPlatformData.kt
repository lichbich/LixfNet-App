package com.lixfnet.app.data.repository

import com.lixfnet.app.data.model.Movie

data class PlatformTitleEntry(
    val vietnameseTitle: String,
    val originalTitle: String,
    val slug: String,
    val year: Int = 2026,
    val type: String = "series",
    val posterUrl: String = "",
    val backdropUrl: String = ""
)

object StreamingPlatformData {

    val NETFLIX_VN_SERIES_TUDUM = listOf(
        PlatformTitleEntry(
            vietnameseTitle = "Bác Sĩ X: Mafia Áo Trắng",
            originalTitle = "Doctor X: Mafia in White",
            slug = "bac-si-x",
            year = 2026,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/jzqPdwpiDTPzvnbV0pZzTtOIgWz.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "100 Ngày Lừa Dối",
            originalTitle = "100 Days of Deception",
            slug = "100-ngay-lua-doi",
            year = 2026,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/yukp2lBvw4ENQvdLN1slneX6jxN.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Nạp Đầy Trái Tim",
            originalTitle = "Take Charge of My Heart",
            slug = "nap-day-trai-tim-phan-1",
            year = 2026,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/vmtWZWZufg1NuolKGjDeeNNDiOM.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Below: Ác Mộng Đại Dương",
            originalTitle = "Below",
            slug = "below",
            year = 2026,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/6pRKi7ngThLHVtRzT8ICruRkvD1.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Gả Kim Thoa",
            originalTitle = "Where the Mask Ends",
            slug = "ga-kim-thoa-ga-tram-vang",
            year = 2026,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/ozczlsMlKUhzB11ygTj4QVEGS7v.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Dược Sư Tự Sự",
            originalTitle = "The Apothecary Diaries",
            slug = "duoc-su-tu-su-phan-1",
            year = 2023,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/sJZcy8QsOPuRPiCfoCWVB3r9Ohg.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Không Thể Thay Thế",
            originalTitle = "Irreplaceable",
            slug = "khong-the-thay-the-2026",
            year = 2026,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/bjPBi1w8FOIAv6fpcftoOJ3vIei.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Đám Cưới Đom Đóm",
            originalTitle = "Firefly Wedding",
            slug = "dam-cuoi-dom-dom",
            year = 2026,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/g7SUj8IElRHURwZDH3sLGKl8vNV.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Đầu Xuân Tươi Sáng",
            originalTitle = "The Early Spring",
            slug = "dau-xuan-tuoi-sang",
            year = 2026,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/uCpDUdogzpqzFKVPtoPLGCLyXbj.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Bác Sĩ X Ngoại Khoa (Phần 1)",
            originalTitle = "Doctor X Surgeon Michiko Daimon",
            slug = "bac-si-x-ngoai-khoa-daimon-michiko-phan-1",
            year = 2012,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/z8hlncTkxO58t5KAvNRTXnm29Ii.jpg"
        )
    )

    val NETFLIX_VN_FILMS_TUDUM = listOf(
        PlatformTitleEntry(
            vietnameseTitle = "Bản Năng Thú",
            originalTitle = "Animals",
            slug = "ban-nang-thu",
            year = 2026,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/iISM36voS1E8gX9BM0DVK0kgi3E.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Tomb Raider: Huyền Thoại Bắt Đầu",
            originalTitle = "Tomb Raider",
            slug = "tomb-raider-huyen-thoai-bat-dau",
            year = 2018,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/xslSyf2YxLJJJmp8jgtznT8PeYC.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Ma Cây: Lửa Địa Ngục",
            originalTitle = "Evil Dead Burn",
            slug = "ma-cay-troi-day",
            year = 2026,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/yAtHcEJdoIjV2BJUvZz89A88pAB.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Thỏ Ơi!!",
            originalTitle = "Bunny!",
            slug = "tho-oi",
            year = 2026,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/qa5aZpO8tQqhlDaE0flalS1TuIS.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Truy Tìm Long Diên Hương",
            originalTitle = "Fish, Fists and Ambergris",
            slug = "truy-tim-long-dien-huong",
            year = 2025,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/7fHlcEovyJY1YhTd8FaJXj1mUk8.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Kong: Đảo Đầu Lâu",
            originalTitle = "Kong: Skull Island",
            slug = "kong-dao-dau-lau",
            year = 2017,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/ktotbBFrmO58kAKoPvpbChy53EB.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Mai",
            originalTitle = "Mai",
            slug = "mai",
            year = 2024,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/2nF8xD200rcDawuCg5ObxxqA2fC.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Sự Im Lặng Của Bầy Cừu",
            originalTitle = "The Silence of the Lambs",
            slug = "su-im-lang-cua-bay-cuu",
            year = 1991,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/2dfScR0ZhDOVRV2Qco758YgLzXG.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Ben-Hur",
            originalTitle = "Ben-Hur",
            slug = "ben-hur",
            year = 2016,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/xrkr6jM12oYy6XKhtc35IFZCwM7.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Con Nhà Siêu Giàu Châu Á",
            originalTitle = "Crazy Rich Asians",
            slug = "con-nha-sieu-giau-chau-a",
            year = 2018,
            type = "movie",
            posterUrl = "https://image.tmdb.org/t/p/w500/cGjk424xYHsHXUaEGKg0vYw5lMH.jpg"
        )
    )

    val APPLE_TV_ORIGINALS = listOf(
        PlatformTitleEntry(
            vietnameseTitle = "Những Điệp Viên Hết Thời",
            originalTitle = "Slow Horses",
            slug = "nhung-diep-vien-het-thoi-phan-1",
            year = 2022,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/AdYr4DjOgXvDUMwu6vEhZy1Rnxk.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Ted Lasso (Phần 1)",
            originalTitle = "Ted Lasso",
            slug = "ted-lasso-phan-1",
            year = 2020,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/uRHsiw1wLxPHFXkkv4Ix1s0O6f4.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Hầm Silo (Phần 1)",
            originalTitle = "Silo",
            slug = "ham-silo-phan-1",
            year = 2023,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/r2QXomqKjkKHVtYGGtkf2l2Y7go.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Người Chạy Xuyên Không Gian",
            originalTitle = "Dark Matter",
            slug = "vat-chat-toi-phan-1",
            year = 2024,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/2DPmTlv8F0V1TQBPmlsGOVOhtWk.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Cuộc Chiến Không Gian",
            originalTitle = "For All Mankind",
            slug = "cuoc-chien-khong-gian-phan-1",
            year = 2019,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/JP3DItWMbrrLiKR5AYUfpsNf2b.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Cắt Rời Ký Ức (Phần 1)",
            originalTitle = "Severance",
            slug = "cat-roi-ky-uc-phan-1",
            year = 2022,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/pPHpeI2X1qEd1CS1SeyrdhZ4qnT.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Lực Lượng Nền Tảng",
            originalTitle = "Foundation",
            slug = "luc-luong-nen-tang-phan-1",
            year = 2021,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/tg9I5pOY4M9CKj8U0cxVBTsm5eh.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Cape Fear",
            originalTitle = "Cape Fear",
            slug = "cape-fear",
            year = 2025,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/3ZLy3cfQa9prahoRq1teMmr5pvW.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Widow's Bay",
            originalTitle = "Widow's Bay",
            slug = "widows-bay",
            year = 2025,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/5lcxWLVAEICkFpuAiV1aMy7ZZj3.jpg"
        ),
        PlatformTitleEntry(
            vietnameseTitle = "Bác Sĩ Trị Liệu - Shrinking",
            originalTitle = "Shrinking",
            slug = "shrinking-phan-1",
            year = 2023,
            type = "series",
            posterUrl = "https://image.tmdb.org/t/p/w500/zEFKMNPBKq6JG7uuDkzTQ9WwErn.jpg"
        )
    )

    val NETFLIX_VN_TUDUM_DICTIONARY: Map<String, PlatformTitleEntry> = mapOf(
        "Bunny!" to PlatformTitleEntry("Thỏ Ơi!!", "Hey, Bunny!", "tho-oi", 2026, "movie", "https://image.tmdb.org/t/p/w500/qa5aZpO8tQqhlDaE0flalS1TuIS.jpg"),
        "Fish, Fists and Ambergris" to PlatformTitleEntry("Truy Tìm Long Diên Hương", "Fish, Fists and Ambergris", "truy-tim-long-dien-huong", 2025, "movie", "https://image.tmdb.org/t/p/w500/7fHlcEovyJY1YhTd8FaJXj1mUk8.jpg"),
        "Animals" to PlatformTitleEntry("Bản Năng Thú", "Animals", "ban-nang-thu", 2026, "movie", "https://image.tmdb.org/t/p/w500/iISM36voS1E8gX9BM0DVK0kgi3E.jpg"),
        "Tomb Raider" to PlatformTitleEntry("Tomb Raider: Huyền Thoại Bắt Đầu", "Tomb Raider", "tomb-raider-huyen-thoai-bat-dau", 2018, "movie", "https://image.tmdb.org/t/p/w500/xslSyf2YxLJJJmp8jgtznT8PeYC.jpg"),
        "Evil Dead Burn" to PlatformTitleEntry("Ma Cây: Lửa Địa Ngục", "Evil Dead Burn", "ma-cay-troi-day", 2026, "movie", "https://image.tmdb.org/t/p/w500/yAtHcEJdoIjV2BJUvZz89A88pAB.jpg"),
        "Kong: Skull Island" to PlatformTitleEntry("Kong: Đảo Đầu Lâu", "Kong: Skull Island", "kong-dao-dau-lau", 2017, "movie", "https://image.tmdb.org/t/p/w500/ktotbBFrmO58kAKoPvpbChy53EB.jpg"),
        "Mai" to PlatformTitleEntry("Mai", "Mai", "mai", 2024, "movie", "https://image.tmdb.org/t/p/w500/2nF8xD200rcDawuCg5ObxxqA2fC.jpg"),
        "The Silence of the Lambs" to PlatformTitleEntry("Sự Im Lặng Của Bầy Cừu", "The Silence of the Lambs", "su-im-lang-cua-bay-cuu", 1991, "movie", "https://image.tmdb.org/t/p/w500/2dfScR0ZhDOVRV2Qco758YgLzXG.jpg"),
        "Ben-Hur" to PlatformTitleEntry("Ben-Hur", "Ben-Hur", "hoang-tu-benhur", 2016, "movie", "https://image.tmdb.org/t/p/w500/xrkr6jM12oYy6XKhtc35IFZCwM7.jpg"),
        "Crazy Rich Asians" to PlatformTitleEntry("Con Nhà Siêu Giàu Châu Á", "Crazy Rich Asians", "con-nha-sieu-giau-chau-a", 2018, "movie", "https://image.tmdb.org/t/p/w500/cGjk424xYHsHXUaEGKg0vYw5lMH.jpg"),
        "Black Adam" to PlatformTitleEntry("Black Adam", "Black Adam", "black-adam", 2022, "movie", "https://image.tmdb.org/t/p/w500/pFlaoHTZeyNkG83vxsAJiGzfSsa.jpg"),
        "Surviving Hurricane Otis" to PlatformTitleEntry("Vượt Qua Bão Otis", "Surviving Hurricane Otis", "vuot-qua-bao-otis", 2026, "movie", "https://image.tmdb.org/t/p/w500/7I92z0VfH0P7H9y4jQp6a.jpg"),
        "The 4 Rascals" to PlatformTitleEntry("Bộ Tứ Báo Thủ", "The 4 Rascals", "bo-tu-bao-thu", 2025, "movie", "https://image.tmdb.org/t/p/w500/pA2k9Yq5s4t6jP0n8x7Z1w3m.jpg"),
        "Blackout" to PlatformTitleEntry("Cuộc Chiến Bí Ẩn", "Blackout", "cuoc-chien-bi-an", 2026, "movie", "https://image.tmdb.org/t/p/w500/8gL9zX1s4xY6k4x7L3p2.jpg"),
        "Doctor X: Mafia in White" to PlatformTitleEntry("Bác Sĩ X: Mafia Áo Trắng", "Doctor X: Mafia in White", "bac-si-x", 2026, "series", "https://image.tmdb.org/t/p/w500/jzqPdwpiDTPzvnbV0pZzTtOIgWz.jpg"),
        "100 Days of Deception" to PlatformTitleEntry("100 Ngày Lừa Dối", "100 Days of Deception", "100-ngay-lua-doi", 2026, "series", "https://image.tmdb.org/t/p/w500/yukp2lBvw4ENQvdLN1slneX6jxN.jpg"),
        "Take Charge of My Heart" to PlatformTitleEntry("Nạp Đầy Trái Tim", "Take Charge of My Heart", "nap-day-trai-tim-phan-1", 2026, "series", "https://image.tmdb.org/t/p/w500/vmtWZWZufg1NuolKGjDeeNNDiOM.jpg"),
        "Below" to PlatformTitleEntry("Below: Ác Mộng Đại Dương", "Below", "below", 2026, "series", "https://image.tmdb.org/t/p/w500/6pRKi7ngThLHVtRzT8ICruRkvD1.jpg"),
        "Where the Mask Ends" to PlatformTitleEntry("Gả Kim Thoa", "Where the Mask Ends", "ga-kim-thoa-ga-tram-vang", 2026, "series", "https://image.tmdb.org/t/p/w500/ozczlsMlKUhzB11ygTj4QVEGS7v.jpg"),
        "The Apothecary Diaries" to PlatformTitleEntry("Dược Sư Tự Sự", "The Apothecary Diaries", "duoc-su-tu-su-phan-1", 2023, "series", "https://image.tmdb.org/t/p/w500/sJZcy8QsOPuRPiCfoCWVB3r9Ohg.jpg"),
        "The Apothecary Diaries: Season 3" to PlatformTitleEntry("Dược Sư Tự Sự (Phần 3)", "The Apothecary Diaries", "duoc-su-tu-su-phan-3", 2026, "series", "https://image.tmdb.org/t/p/w500/sJZcy8QsOPuRPiCfoCWVB3r9Ohg.jpg"),
        "Irreplaceable" to PlatformTitleEntry("Không Thể Thay Thế", "Irreplaceable", "khong-the-thay-the-2026", 2026, "series", "https://image.tmdb.org/t/p/w500/bjPBi1w8FOIAv6fpcftoOJ3vIei.jpg"),
        "Firefly Wedding" to PlatformTitleEntry("Đám Cưới Đom Đóm", "Firefly Wedding", "dam-cuoi-dom-dom", 2026, "series", "https://image.tmdb.org/t/p/w500/g7SUj8IElRHURwZDH3sLGKl8vNV.jpg"),
        "The Early Spring" to PlatformTitleEntry("Đầu Xuân Tươi Sáng", "The Early Spring", "dau-xuan-tuoi-sang", 2026, "series", "https://image.tmdb.org/t/p/w500/uCpDUdogzpqzFKVPtoPLGCLyXbj.jpg"),
        "Cyberpunk: Edgerunners 2" to PlatformTitleEntry("Cyberpunk: Edgerunners (Phần 2)", "Cyberpunk: Edgerunners", "cyberpunk-edgerunners-phan-2", 2026, "series", "https://image.tmdb.org/t/p/w500/7JvWbKkX3iXk8LkWlX2kU1M3rP.jpg"),
        "Cyberpunk: Edgerunners" to PlatformTitleEntry("Cyberpunk: Edgerunners", "Cyberpunk: Edgerunners", "cyberpunk-edgerunners-phan-2", 2022, "series", "https://image.tmdb.org/t/p/w500/7JvWbKkX3iXk8LkWlX2kU1M3rP.jpg"),
        "Dragon Ball Super: Beerus" to PlatformTitleEntry("Dragon Ball Super: Beerus", "Dragon Ball Super: Beerus", "dragon-ball-super-beerus", 2026, "series", "https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg"),
        "Extraordinary Attorney Minami" to PlatformTitleEntry("Nữ Luật Sư Kỳ Lạ Minami", "Extraordinary Attorney Minami", "nu-luat-su-ky-la-minami", 2026, "series", "https://image.tmdb.org/t/p/w500/4zZ3zQY4sRkm3F3QhWl6z5p9Hk.jpg")
    )

    fun cleanTudumTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex(":\\s*(Season\\s*\\d+|Limited\\s*Series|\\d+)$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun findTudumEntry(rawTitle: String): PlatformTitleEntry? {
        val clean = cleanTudumTitle(rawTitle)
        
        // 1. Direct dictionary match
        NETFLIX_VN_TUDUM_DICTIONARY[rawTitle]?.let { return it }
        NETFLIX_VN_TUDUM_DICTIONARY[clean]?.let { return it }
        NETFLIX_VN_TUDUM_DICTIONARY.entries.find { 
            it.key.equals(clean, ignoreCase = true) || it.key.equals(rawTitle, ignoreCase = true) 
        }?.value?.let { return it }

        // 2. Search in hardcoded lists
        val allStatic = NETFLIX_VN_SERIES_TUDUM + NETFLIX_VN_FILMS_TUDUM + APPLE_TV_ORIGINALS
        allStatic.find { 
            it.originalTitle.equals(clean, ignoreCase = true) || 
            it.originalTitle.equals(rawTitle, ignoreCase = true) ||
            it.vietnameseTitle.equals(clean, ignoreCase = true)
        }?.let { return it }

        // 3. Partial match
        NETFLIX_VN_TUDUM_DICTIONARY.entries.find {
            clean.contains(it.key, ignoreCase = true) || it.key.contains(clean, ignoreCase = true)
        }?.value?.let { return it }

        return null
    }
}
