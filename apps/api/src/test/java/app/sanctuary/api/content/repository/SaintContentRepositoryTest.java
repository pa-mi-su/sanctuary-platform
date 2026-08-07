package app.sanctuary.api.content.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import app.sanctuary.api.content.dto.SaintDetailDto;
import app.sanctuary.api.content.dto.SaintSummaryDto;
import app.sanctuary.api.content.support.SupportedLanguage;

@ExtendWith(MockitoExtension.class)
class SaintContentRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @EnumSource(SupportedLanguage.class)
    @SuppressWarnings("unchecked")
    void feastDayQuerySelectsAndSortsByLocalizedName(SupportedLanguage language) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(8), eq(7)))
            .thenReturn(List.<SaintSummaryDto>of());
        SaintContentRepository repository = new SaintContentRepository(jdbcTemplate);

        repository.findByFeastDay(8, 7, language);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), eq(8), eq(7));
        assertThat(sql.getValue())
            .contains("name_" + language.code() + " AS name")
            .contains("ORDER BY name_" + language.code());
    }

    @ParameterizedTest
    @EnumSource(SupportedLanguage.class)
    @SuppressWarnings("unchecked")
    void searchQuerySelectsFiltersAndSortsByLocalizedName(SupportedLanguage language) {
        when(jdbcTemplate.query(
            anyString(),
            any(RowMapper.class),
            eq("Sixtus"),
            eq("%sixtus%"),
            eq("%sixtus%")
        )).thenReturn(List.<SaintSummaryDto>of());
        SaintContentRepository repository = new SaintContentRepository(jdbcTemplate);

        repository.list(language, " Sixtus ");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
            sql.capture(),
            any(RowMapper.class),
            eq("Sixtus"),
            eq("%sixtus%"),
            eq("%sixtus%")
        );
        assertThat(sql.getValue())
            .contains("name_" + language.code() + " AS name")
            .contains("LOWER(name_" + language.code() + ") LIKE ?")
            .contains("ORDER BY feast_month, feast_day, name_" + language.code());
    }

    @ParameterizedTest
    @EnumSource(SupportedLanguage.class)
    @SuppressWarnings("unchecked")
    void detailQuerySelectsLocalizedName(SupportedLanguage language) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("08-07_saint_cajetan")))
            .thenReturn(List.<SaintDetailDto>of());
        SaintContentRepository repository = new SaintContentRepository(jdbcTemplate);

        repository.findBySlug("08-07_saint_cajetan", language);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
            sql.capture(),
            any(RowMapper.class),
            eq("08-07_saint_cajetan")
        );
        assertThat(sql.getValue()).contains("name_" + language.code() + " AS name");
    }
}
