package dev.hayya.core.domain.model

/**
 * Curated offline city / town entry with administrative hierarchy for Pakistan.
 */
data class CityPreset(
    val name: String,
    val country: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val recommendedMethod: CalculationMethodOption,
    val division: String = "",
    val district: String = "",
    val authorityType: String = ""
) {
    val displayTitle: String
        get() = if (district.isNotEmpty() && district != name) {
            "$name, $district"
        } else {
            "$name, $country"
        }

    val subtitleLabel: String
        get() = when {
            division.isNotEmpty() && authorityType.isNotEmpty() -> "$district • $authorityType"
            division.isNotEmpty() -> "$district • $division"
            else -> "$country • $region"
        }
}

/**
 * Offline directory focused on Pakistan > Sindh > Larkana Division (100% coverage of
 * all 5 districts, talukas, municipal committees, and town committees), alongside major
 * Pakistani and global cities.
 */
object CityCatalog {

    val larkanaDistricts: List<String> = listOf(
        "Larkana",
        "Qambar Shahdadkot",
        "Shikarpur",
        "Jacobabad",
        "Kashmore"
    )

    val larkanaDivisionCities: List<CityPreset> by lazy {
        allCities.filter { it.division == "Larkana Division" }
    }

    val popularCities: List<CityPreset> by lazy {
        allCities.filter {
            it.division == "Larkana Division" && it.name in setOf(
                "Larkana", "Ratodero", "Naudero", "Qambar", "Shahdadkot",
                "Shikarpur", "Jacobabad", "Thul", "Kandhkot", "Kashmore"
            )
        }
    }

    val pakistanCities: List<CityPreset> by lazy {
        allCities.filter { it.country == "Pakistan" }
    }

    val allCities: List<CityPreset> = listOf(
        // =====================================================================
        // PAKISTAN > SINDH > LARKANA DIVISION (5 Districts, 37 Talukas & Towns)
        // =====================================================================

        // 1. Larkana District (8 Municipal/Taluka/Town Centers)
        CityPreset("Larkana", "Pakistan", "Sindh", 27.5590, 68.2120, CalculationMethodOption.KARACHI, "Larkana Division", "Larkana", "Municipal Corporation • District HQ"),
        CityPreset("Ratodero", "Pakistan", "Sindh", 27.8052, 68.2910, CalculationMethodOption.KARACHI, "Larkana Division", "Larkana", "Municipal Committee • Taluka HQ"),
        CityPreset("Naudero", "Pakistan", "Sindh", 27.6648, 68.3622, CalculationMethodOption.KARACHI, "Larkana Division", "Larkana", "Municipal Committee"),
        CityPreset("Dokri", "Pakistan", "Sindh", 27.3743, 68.0967, CalculationMethodOption.KARACHI, "Larkana Division", "Larkana", "Town Committee • Taluka HQ"),
        CityPreset("Bakrani", "Pakistan", "Sindh", 27.4410, 68.1843, CalculationMethodOption.KARACHI, "Larkana Division", "Larkana", "Taluka HQ"),
        CityPreset("Badah", "Pakistan", "Sindh", 27.3342, 68.0157, CalculationMethodOption.KARACHI, "Larkana Division", "Larkana", "Town Committee"),
        CityPreset("Arija", "Pakistan", "Sindh", 27.6254, 68.1504, CalculationMethodOption.KARACHI, "Larkana Division", "Larkana", "Town Committee"),
        CityPreset("Garello", "Pakistan", "Sindh", 27.4352, 68.0761, CalculationMethodOption.KARACHI, "Larkana Division", "Larkana", "Town Committee"),

        // 2. Qambar Shahdadkot District (10 Municipal/Taluka/Town Centers)
        CityPreset("Qambar", "Pakistan", "Sindh", 27.5868, 68.0010, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Municipal Committee • District HQ"),
        CityPreset("Shahdadkot", "Pakistan", "Sindh", 27.8473, 67.9068, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Municipal Committee • Taluka HQ"),
        CityPreset("Warah", "Pakistan", "Sindh", 27.4477, 67.7944, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Town Committee • Taluka HQ"),
        CityPreset("Nasirabad", "Pakistan", "Sindh", 27.3980, 67.9907, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Town Committee • Taluka HQ"),
        CityPreset("Miro Khan", "Pakistan", "Sindh", 27.7599, 68.0920, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Town Committee • Taluka HQ"),
        CityPreset("Qubo Saeed Khan", "Pakistan", "Sindh", 27.8744, 67.7089, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Town Committee • Taluka HQ"),
        CityPreset("Sijawal Junejo", "Pakistan", "Sindh", 27.8417, 68.1139, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Taluka HQ"),
        CityPreset("Wagan", "Pakistan", "Sindh", 27.4965, 67.9666, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Town Committee"),
        CityPreset("Behram", "Pakistan", "Sindh", 27.5300, 68.0300, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Town Committee"),
        CityPreset("Gaji Khuhawar", "Pakistan", "Sindh", 27.3622, 67.8078, CalculationMethodOption.KARACHI, "Larkana Division", "Qambar Shahdadkot", "Town Committee"),

        // 3. Shikarpur District (8 Municipal/Taluka/Town Centers)
        CityPreset("Shikarpur", "Pakistan", "Sindh", 27.9571, 68.6379, CalculationMethodOption.KARACHI, "Larkana Division", "Shikarpur", "Municipal Committee • District HQ"),
        CityPreset("Garhi Yasin", "Pakistan", "Sindh", 27.9000, 68.5167, CalculationMethodOption.KARACHI, "Larkana Division", "Shikarpur", "Town Committee • Taluka HQ"),
        CityPreset("Lakhi Ghulam Shah", "Pakistan", "Sindh", 27.8489, 68.6998, CalculationMethodOption.KARACHI, "Larkana Division", "Shikarpur", "Town Committee • Taluka HQ"),
        CityPreset("Khanpur (Shikarpur)", "Pakistan", "Sindh", 28.0014, 68.7289, CalculationMethodOption.KARACHI, "Larkana Division", "Shikarpur", "Town Committee • Taluka HQ"),
        CityPreset("Madeji", "Pakistan", "Sindh", 27.7531, 68.4517, CalculationMethodOption.KARACHI, "Larkana Division", "Shikarpur", "Town Committee"),
        CityPreset("Chak", "Pakistan", "Sindh", 27.8575, 68.8329, CalculationMethodOption.KARACHI, "Larkana Division", "Shikarpur", "Town Committee"),
        CityPreset("Rustam", "Pakistan", "Sindh", 27.9670, 68.8015, CalculationMethodOption.KARACHI, "Larkana Division", "Shikarpur", "Town Committee"),
        CityPreset("Sultankot", "Pakistan", "Sindh", 28.0708, 68.5944, CalculationMethodOption.KARACHI, "Larkana Division", "Shikarpur", "Major Town"),

        // 4. Jacobabad District (4 Municipal/Taluka/Town Centers)
        CityPreset("Jacobabad", "Pakistan", "Sindh", 28.2819, 68.4376, CalculationMethodOption.KARACHI, "Larkana Division", "Jacobabad", "Municipal Committee • District HQ"),
        CityPreset("Thul", "Pakistan", "Sindh", 28.2404, 68.7759, CalculationMethodOption.KARACHI, "Larkana Division", "Jacobabad", "Municipal Committee • Taluka HQ"),
        CityPreset("Garhi Khairo", "Pakistan", "Sindh", 28.0572, 67.9792, CalculationMethodOption.KARACHI, "Larkana Division", "Jacobabad", "Town Committee • Taluka HQ"),
        CityPreset("Mirpur Buriro", "Pakistan", "Sindh", 28.1808, 68.7274, CalculationMethodOption.KARACHI, "Larkana Division", "Jacobabad", "Town Committee"),

        // 5. Kashmore District (7 Municipal/Taluka/Town Centers)
        CityPreset("Kandhkot", "Pakistan", "Sindh", 28.2440, 69.1824, CalculationMethodOption.KARACHI, "Larkana Division", "Kashmore", "Municipal Committee • District HQ"),
        CityPreset("Kashmore", "Pakistan", "Sindh", 28.4333, 69.5833, CalculationMethodOption.KARACHI, "Larkana Division", "Kashmore", "Municipal Committee • Taluka HQ"),
        CityPreset("Tangwani", "Pakistan", "Sindh", 28.2789, 68.9976, CalculationMethodOption.KARACHI, "Larkana Division", "Kashmore", "Town Committee • Taluka HQ"),
        CityPreset("Guddu", "Pakistan", "Sindh", 28.4308, 69.6875, CalculationMethodOption.KARACHI, "Larkana Division", "Kashmore", "Town Committee"),
        CityPreset("Ghauspur", "Pakistan", "Sindh", 28.1394, 69.0803, CalculationMethodOption.KARACHI, "Larkana Division", "Kashmore", "Town Committee"),
        CityPreset("Buxapur", "Pakistan", "Sindh", 28.3780, 69.3653, CalculationMethodOption.KARACHI, "Larkana Division", "Kashmore", "Town Committee"),
        CityPreset("Karampur", "Pakistan", "Sindh", 28.1004, 68.9309, CalculationMethodOption.KARACHI, "Larkana Division", "Kashmore", "Town Committee"),

        // =====================================================================
        // PAKISTAN — Capital Territory & Rest of Sindh
        // =====================================================================
        CityPreset("Islamabad", "Pakistan", "Islamabad Capital Territory", 33.6844, 73.0479, CalculationMethodOption.KARACHI),
        CityPreset("Karachi", "Pakistan", "Sindh", 24.8607, 67.0011, CalculationMethodOption.KARACHI),
        CityPreset("Hyderabad", "Pakistan", "Sindh", 25.3792, 68.3683, CalculationMethodOption.KARACHI),
        CityPreset("Sukkur", "Pakistan", "Sindh", 27.7032, 68.8589, CalculationMethodOption.KARACHI),
        CityPreset("Nawabshah", "Pakistan", "Sindh", 26.2394, 68.4037, CalculationMethodOption.KARACHI),
        CityPreset("Mirpur Khas", "Pakistan", "Sindh", 25.5251, 69.0159, CalculationMethodOption.KARACHI),
        CityPreset("Khairpur", "Pakistan", "Sindh", 27.5295, 68.7617, CalculationMethodOption.KARACHI),
        CityPreset("Dadu", "Pakistan", "Sindh", 26.7329, 67.7763, CalculationMethodOption.KARACHI),
        CityPreset("Tando Allahyar", "Pakistan", "Sindh", 25.4626, 68.7192, CalculationMethodOption.KARACHI),
        CityPreset("Tando Adam", "Pakistan", "Sindh", 25.7637, 68.6610, CalculationMethodOption.KARACHI),
        CityPreset("Ghotki", "Pakistan", "Sindh", 28.0060, 69.3161, CalculationMethodOption.KARACHI),
        CityPreset("Badin", "Pakistan", "Sindh", 24.6557, 68.8372, CalculationMethodOption.KARACHI),
        CityPreset("Thatta", "Pakistan", "Sindh", 24.7475, 67.9235, CalculationMethodOption.KARACHI),
        CityPreset("Umerkot", "Pakistan", "Sindh", 25.3616, 69.7362, CalculationMethodOption.KARACHI),
        CityPreset("Sanghar", "Pakistan", "Sindh", 26.0442, 68.9369, CalculationMethodOption.KARACHI),
        CityPreset("Jamshoro", "Pakistan", "Sindh", 25.4377, 68.2897, CalculationMethodOption.KARACHI),
        CityPreset("Kotri", "Pakistan", "Sindh", 25.3660, 68.3122, CalculationMethodOption.KARACHI),
        CityPreset("Moro", "Pakistan", "Sindh", 26.6646, 68.0016, CalculationMethodOption.KARACHI),

        // =====================================================================
        // PAKISTAN — Punjab
        // =====================================================================
        CityPreset("Lahore", "Pakistan", "Punjab", 31.5204, 74.3587, CalculationMethodOption.KARACHI),
        CityPreset("Faisalabad", "Pakistan", "Punjab", 31.4155, 73.0897, CalculationMethodOption.KARACHI),
        CityPreset("Rawalpindi", "Pakistan", "Punjab", 33.5984, 73.0441, CalculationMethodOption.KARACHI),
        CityPreset("Multan", "Pakistan", "Punjab", 30.1978, 71.4697, CalculationMethodOption.KARACHI),
        CityPreset("Gujranwala", "Pakistan", "Punjab", 32.1557, 74.1871, CalculationMethodOption.KARACHI),
        CityPreset("Sialkot", "Pakistan", "Punjab", 32.4925, 74.5311, CalculationMethodOption.KARACHI),
        CityPreset("Bahawalpur", "Pakistan", "Punjab", 29.3956, 71.6836, CalculationMethodOption.KARACHI),
        CityPreset("Sargodha", "Pakistan", "Punjab", 32.0836, 72.6711, CalculationMethodOption.KARACHI),
        CityPreset("Sheikhupura", "Pakistan", "Punjab", 31.7131, 73.9783, CalculationMethodOption.KARACHI),
        CityPreset("Jhang", "Pakistan", "Punjab", 31.2700, 72.3300, CalculationMethodOption.KARACHI),
        CityPreset("Rahim Yar Khan", "Pakistan", "Punjab", 28.4200, 70.3000, CalculationMethodOption.KARACHI),
        CityPreset("Gujrat", "Pakistan", "Punjab", 32.5711, 74.0750, CalculationMethodOption.KARACHI),
        CityPreset("Kasur", "Pakistan", "Punjab", 31.1176, 74.4500, CalculationMethodOption.KARACHI),
        CityPreset("Sahiwal", "Pakistan", "Punjab", 30.6667, 73.1000, CalculationMethodOption.KARACHI),
        CityPreset("Okara", "Pakistan", "Punjab", 30.8103, 73.4516, CalculationMethodOption.KARACHI),
        CityPreset("Wah Cantt", "Pakistan", "Punjab", 33.7714, 72.7518, CalculationMethodOption.KARACHI),
        CityPreset("Dera Ghazi Khan", "Pakistan", "Punjab", 30.0500, 70.6333, CalculationMethodOption.KARACHI),
        CityPreset("Chiniot", "Pakistan", "Punjab", 31.7200, 72.9789, CalculationMethodOption.KARACHI),
        CityPreset("Jhelum", "Pakistan", "Punjab", 32.9238, 73.7255, CalculationMethodOption.KARACHI),
        CityPreset("Chakwal", "Pakistan", "Punjab", 32.9329, 72.8539, CalculationMethodOption.KARACHI),
        CityPreset("Attock", "Pakistan", "Punjab", 33.7687, 72.3621, CalculationMethodOption.KARACHI),
        CityPreset("Mianwali", "Pakistan", "Punjab", 32.5854, 71.5436, CalculationMethodOption.KARACHI),
        CityPreset("Murree", "Pakistan", "Punjab", 33.9084, 73.3903, CalculationMethodOption.KARACHI),

        // =====================================================================
        // PAKISTAN — KP, Balochistan, AJK & GB
        // =====================================================================
        CityPreset("Peshawar", "Pakistan", "Khyber Pakhtunkhwa", 34.0151, 71.5249, CalculationMethodOption.KARACHI),
        CityPreset("Mardan", "Pakistan", "Khyber Pakhtunkhwa", 34.1979, 72.0497, CalculationMethodOption.KARACHI),
        CityPreset("Mingora", "Pakistan", "Khyber Pakhtunkhwa", 34.7758, 72.3625, CalculationMethodOption.KARACHI),
        CityPreset("Abbottabad", "Pakistan", "Khyber Pakhtunkhwa", 34.1463, 73.2117, CalculationMethodOption.KARACHI),
        CityPreset("Kohat", "Pakistan", "Khyber Pakhtunkhwa", 33.5820, 71.4493, CalculationMethodOption.KARACHI),
        CityPreset("Dera Ismail Khan", "Pakistan", "Khyber Pakhtunkhwa", 31.8327, 70.9024, CalculationMethodOption.KARACHI),
        CityPreset("Quetta", "Pakistan", "Balochistan", 30.1798, 67.0097, CalculationMethodOption.KARACHI),
        CityPreset("Gwadar", "Pakistan", "Balochistan", 25.1225, 62.3202, CalculationMethodOption.KARACHI),
        CityPreset("Turbat", "Pakistan", "Balochistan", 26.0023, 63.0440, CalculationMethodOption.KARACHI),
        CityPreset("Khuzdar", "Pakistan", "Balochistan", 27.8000, 66.6167, CalculationMethodOption.KARACHI),
        CityPreset("Muzaffarabad", "Pakistan", "Azad Kashmir", 34.3700, 73.4708, CalculationMethodOption.KARACHI),
        CityPreset("Mirpur", "Pakistan", "Azad Kashmir", 33.1486, 73.7482, CalculationMethodOption.KARACHI),
        CityPreset("Gilgit", "Pakistan", "Gilgit-Baltistan", 35.9187, 74.3125, CalculationMethodOption.KARACHI),
        CityPreset("Skardu", "Pakistan", "Gilgit-Baltistan", 35.3000, 75.6167, CalculationMethodOption.KARACHI),

        // =====================================================================
        // GLOBAL REFERENCE CITIES
        // =====================================================================
        CityPreset("Makkah", "Saudi Arabia", "Middle East", 21.4225, 39.8262, CalculationMethodOption.UMM_AL_QURA),
        CityPreset("Madinah", "Saudi Arabia", "Middle East", 24.4672, 39.6112, CalculationMethodOption.UMM_AL_QURA),
        CityPreset("Dubai", "UAE", "Middle East", 25.2048, 55.2708, CalculationMethodOption.DUBAI),
        CityPreset("London", "United Kingdom", "Europe", 51.5074, -0.1278, CalculationMethodOption.MUSLIM_WORLD_LEAGUE),
        CityPreset("New York", "USA", "North America", 40.7128, -74.0060, CalculationMethodOption.NORTH_AMERICA),
        CityPreset("Toronto", "Canada", "North America", 43.6532, -79.3832, CalculationMethodOption.NORTH_AMERICA)
    )

    fun search(
        query: String,
        onlyPakistan: Boolean = false,
        division: String? = null,
        district: String? = null
    ): List<CityPreset> {
        var base = allCities
        if (onlyPakistan) {
            base = base.filter { it.country == "Pakistan" }
        }
        if (!division.isNullOrBlank()) {
            base = base.filter { it.division.equals(division, ignoreCase = true) }
        }
        if (!district.isNullOrBlank()) {
            base = base.filter { it.district.equals(district, ignoreCase = true) }
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return base
        return base.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.district.contains(trimmed, ignoreCase = true) ||
                it.division.contains(trimmed, ignoreCase = true) ||
                it.region.contains(trimmed, ignoreCase = true) ||
                it.country.contains(trimmed, ignoreCase = true)
        }
    }
}
