package lk.janith.smart_tourism.data;

import java.util.Calendar;
import java.util.Locale;

/** Bounded controls for the booking form, using the device's local calendar day. */
public final class BookingSelection {
    private BookingSelection() { }

    public static int maxTravelers(int capacity) {
        return Math.max(1, Math.min(capacity, 30));
    }

    public static int travelers(int requested, int capacity) {
        return Math.max(1, Math.min(requested, maxTravelers(capacity)));
    }

    public static Calendar parseDate(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        String[] parts = value.split("-");
        try {
            Calendar date = Calendar.getInstance();
            date.clear();
            date.setLenient(false);
            date.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]), 12, 0, 0);
            date.getTimeInMillis(); // Reject impossible dates rather than normalizing them.
            return date;
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    public static Calendar startOfDay(Calendar day) {
        Calendar copy = (Calendar) day.clone();
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy;
    }

    public static boolean canPreviousDay(String value, Calendar today) {
        Calendar selected = parseDate(value);
        return selected != null && selected.after(startOfDay(today))
                && !sameDay(selected, today);
    }

    public static String shiftDay(String value, int offset, Calendar today) {
        Calendar selected = parseDate(value);
        Calendar minimum = startOfDay(today);
        if (selected == null || selected.before(minimum)) selected = (Calendar) minimum.clone();
        selected.add(Calendar.DAY_OF_MONTH, offset);
        if (selected.before(minimum)) selected = minimum;
        return String.format(Locale.US, "%04d-%02d-%02d", selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH) + 1, selected.get(Calendar.DAY_OF_MONTH));
    }

    private static boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }
}
