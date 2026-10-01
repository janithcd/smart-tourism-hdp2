package lk.janith.smart_tourism.receiver;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.activity.MainActivity;

/** Posts a device notification after the app broadcasts a saved demo booking. */
public class BookingConfirmationReceiver extends BroadcastReceiver {
    public static final String ACTION_BOOKING_SAVED = "lk.janith.smart_tourism.BOOKING_SAVED";
    public static final String EXTRA_TITLE = "booking_title";
    public static final String EXTRA_REFERENCE = "booking_reference";
    private static final String CHANNEL_ID = "booking_demo_updates";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_BOOKING_SAVED.equals(intent.getAction())) return;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.booking_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT));
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String reference = intent.getStringExtra(EXTRA_REFERENCE);
        if (title == null || reference == null) return;

        Intent open = new Intent(context, MainActivity.class);
        open.putExtra("open_bookings", true);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int id = reference.hashCode() & Integer.MAX_VALUE;
        PendingIntent pending = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.calendar_today_24px)
                .setContentTitle(context.getString(R.string.booking_notification_title))
                .setContentText(context.getString(R.string.booking_notification_message, title))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(id, notification.build());
    }
}
