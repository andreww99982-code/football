package com.rogermichin.rmatch

import com.google.common.truth.Truth.assertThat
import com.rogermichin.rmatch.data.CountryDto
import com.rogermichin.rmatch.data.LeagueDto
import com.rogermichin.rmatch.data.LeagueResponseDto
import com.rogermichin.rmatch.data.LeagueSeasonDto
import org.junit.Test

class MapperTest {
    @Test
    fun countryDtoCarriesRealFields() {
        val dto = CountryDto(name = "Russia", code = "RU", flag = "flag")
        assertThat(dto.name).isEqualTo("Russia")
        assertThat(dto.code).isEqualTo("RU")
    }

    @Test
    fun leagueDtoContainsSeasonCoverage() {
        val dto = LeagueResponseDto(
            league = LeagueDto(id = 39, name = "Premier League", type = "League", logo = null),
            country = CountryDto(name = "England", code = "GB", flag = null),
            seasons = listOf(LeagueSeasonDto(year = 2024, current = true)),
        )
        assertThat(dto.seasons?.first()?.year).isEqualTo(2024)
    }
}
