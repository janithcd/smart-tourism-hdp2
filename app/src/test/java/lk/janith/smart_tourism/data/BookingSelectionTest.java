package lk.janith.smart_tourism.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class BookingSelectionTest {
    private static Calendar day(int year, int month, int date) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, date, 15, 30);
        return calendar;
    }

    @Test
    public void passengerCounterStopsAtListingCapacity() {
        assertEquals(1, BookingSelection.travelers(0, 3));
        assertEquals(3, BookingSelection.travelers(9, 3));
        assertEquals(8, BookingSelection.travelers(8, 8));
        assertEquals(30, BookingSelection.maxTravelers(100));
    }

    @Test
    public void dateControlsCrossMonthAndDoNotSelectYesterday() {
        Calendar today = day(2026, 10, 31);
        assertEquals("2026-11-01", BookingSelection.shiftDay("", 1, today));
        assertEquals("2026-10-31", BookingSelection.shiftDay("2026-11-01", -1, today));
        assertEquals("2026-10-31", BookingSelection.shiftDay("2026-10-31", -1, today));
        assertFalse(BookingSelection.canPreviousDay("", today));
        assertFalse(BookingSelection.canPreviousDay("2026-10-31", today));
        assertTrue(BookingSelection.canPreviousDay("2026-11-01", today));
    }

    @Test
    public void invalidDatesAreNotAcceptedByTheStepper() {
        Calendar today = day(2026, 2, 28);
        assertNull(BookingSelection.parseDate("2026-02-29"));
        assertEquals("2026-03-01", BookingSelection.shiftDay("2026-02-29", 1, today));
        assertEquals("2028-02-29", BookingSelection.shiftDay("2028-02-28", 1, today));
    }
}
