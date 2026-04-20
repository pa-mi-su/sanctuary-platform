package app.sanctuary.api.content.support;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ContentRequestValidator {

    public void validateMonthDay(int month, int day) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be between 1 and 12");
        }
        if (day < 1 || day > 31) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "day must be between 1 and 31");
        }
    }

    public void validateDateRange(LocalDate start, LocalDate end, int maxDaysInclusive) {
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must be on or after start");
        }

        if (start.plusDays(maxDaysInclusive - 1L).isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date range exceeds maximum supported span");
        }
    }
}
