package lk.janith.smart_tourism.data;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import lk.janith.smart_tourism.R;

public final class ProfilePhoto {
    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private ProfilePhoto() {
    }

    public static String pathFor(String uid) {
        return "profile_photos/" + uid + "/avatar";
    }

    public static StorageReference referenceFor(String uid) {
        return FirebaseStorage.getInstance().getReference().child(pathFor(uid));
    }

    public static void load(ImageView avatar, String source, String uid) {
        // A fresh token prevents an older asynchronous download from replacing a newer image.
        Object token = new Object();
        avatar.setTag(R.id.profile_photo_request, token);
        Glide.with(avatar).clear(avatar);
        avatar.setImageResource(R.drawable.account_circle_24px);

        if (source == null || source.trim().isEmpty()) return;

        if (uid != null && source.equals(pathFor(uid))) {
            referenceFor(uid).getBytes(MAX_BYTES)
                    .addOnSuccessListener(bytes -> {
                        if (avatar.getTag(R.id.profile_photo_request) == token) {
                            Glide.with(avatar).load(bytes)
                                    .circleCrop()
                                    .error(R.drawable.account_circle_24px)
                                    .into(avatar);
                        }
                    });
        } else if (source.startsWith("https://")) {
            // Keep showing photos saved through the previous URL-based profile editor.
            Glide.with(avatar).load(source)
                    .circleCrop()
                    .error(R.drawable.account_circle_24px)
                    .into(avatar);
        }
    }
}
