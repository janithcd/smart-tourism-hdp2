package lk.janith.smart_tourism.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.data.BookingStore;

public class BookingsFragment extends Fragment {
    private LinearLayout bookingList;
    private TextView emptyState;

    public BookingsFragment() {
        super(R.layout.fragment_bookings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bookingList = view.findViewById(R.id.bookingList);
        emptyState = view.findViewById(R.id.bookingEmptyState);
        showBookings();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bookingList != null) showBookings();
    }

    private void showBookings() {
        bookingList.removeAllViews();
        List<BookingStore.Booking> bookings = BookingStore.getAll(requireContext());
        emptyState.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (BookingStore.Booking booking : bookings) {
            View card = inflater.inflate(R.layout.item_booking, bookingList, false);
            ImageView bookingImage = card.findViewById(R.id.bookingImage);
            bookingImage.setImageResource(booking.imageResId);
            bookingImage.setScaleType("Tour".equals(booking.type)
                    ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.CENTER_INSIDE);
            ((TextView) card.findViewById(R.id.bookingType)).setText(booking.type);
            ((TextView) card.findViewById(R.id.bookingTitle)).setText(booking.title);
            ((TextView) card.findViewById(R.id.bookingDate)).setText(
                    booking.time.isEmpty()
                            ? getString(R.string.booking_date_value, booking.date)
                            : getString(R.string.booking_date_time_value, booking.date, booking.time));
            ((TextView) card.findViewById(R.id.bookingDetails)).setText(
                    getString(R.string.booking_details_value, booking.duration, booking.pax,
                            booking.pickup, booking.price));
            ((TextView) card.findViewById(R.id.bookingReference)).setText(
                    getString(R.string.booking_reference, booking.id.substring(0, 8).toUpperCase(java.util.Locale.ROOT)));
            ((TextView) card.findViewById(R.id.bookingPayment)).setText(
                    getString(R.string.booking_payment_value, booking.paymentMethod, booking.paymentStatus));
            MaterialButton remove = card.findViewById(R.id.btnRemoveBooking);
            remove.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.booking_remove_title)
                    .setMessage(R.string.booking_remove_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.booking_remove_action, (dialog, which) -> {
                        if (BookingStore.remove(requireContext(), booking.id)) {
                            showBookings();
                        } else {
                            Toast.makeText(requireContext(), R.string.booking_save_failed, Toast.LENGTH_LONG).show();
                        }
                    })
                    .show());
            bookingList.addView(card);
        }
    }
}
