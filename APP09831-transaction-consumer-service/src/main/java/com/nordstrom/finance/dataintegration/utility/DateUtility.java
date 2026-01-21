package com.nordstrom.finance.dataintegration.utility;

import com.nordstrom.finance.dataintegration.mapper.constant.TimeZone;
import com.nordstrom.standard.TimezoneId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class DateUtility {
  private DateUtility() {}

  public static LocalDate toLocalDate(Instant instant) {
    return instant != null ? instant.atZone(ZoneId.systemDefault()).toLocalDate() : null;
  }

  public static LocalDate updateTimeZoneAndGetDate(TimezoneId timezone, Instant date) {
    if (null == timezone || timezone == TimezoneId.UNKNOWN) {
      return toLocalDate(date);
    }

    ZoneId zoneId = TimeZone.valueOf(timezone.name()).toZoneId();
    return LocalDate.ofInstant(date, zoneId);
  }
}
