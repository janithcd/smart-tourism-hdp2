package lk.janith.smart_tourism.data;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lk.janith.smart_tourism.R;

public final class ProfilePhoto {
    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private ProfilePhoto() {
    }

    private static File fileFor(Context context, String uid) {
        // Hash the account ID so it cannot become a path and is not visible in a filename.
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("SHA-256").digest(uid.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        StringBuilder name = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            name.append(Character.forDigit((value >>> 4) & 0xf, 16));
            name.append(Character.forDigit(value & 0xf, 16));
        }
        return new File(new File(context.getNoBackupFilesDir(), "profile_photos"), name + ".avatar");
    }

    public static void load(ImageView avatar, String oldProfileUrl, String uid) {
        Glide.with(avatar).clear(avatar);
        avatar.setImageResource(R.drawable.account_circle_24px);

        if (uid != null) {
            File localPhoto = fileFor(avatar.getContext(), uid);
            if (localPhoto.isFile() && localPhoto.length() > 0) {
                Glide.with(avatar).load(localPhoto)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .error(R.drawable.account_circle_24px)
                        .into(avatar);
                return;
            }
        }

        // Photos saved by the older URL editor can still be shown.
        if (oldProfileUrl != null && oldProfileUrl.startsWith("https://")) {
            Glide.with(avatar).load(oldProfileUrl)
                    .circleCrop()
                    .error(R.drawable.account_circle_24px)
                    .into(avatar);
        }
    }

    public static void save(Context context, Uri source, String uid) throws IOException {
        File destination = fileFor(context, uid);
        File directory = destination.getParentFile();
        if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
            throw new IOException("Could not create the private photo directory");
        }

        File temporary = File.createTempFile("avatar_", ".tmp", directory);
        try {
            try (InputStream input = context.getContentResolver().openInputStream(source);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                if (input == null) throw new IOException("Could not read the selected image");
                byte[] buffer = new byte[8192];
                long size = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    size += count;
                    if (size > MAX_BYTES) throw new PhotoTooLargeException();
                    output.write(buffer, 0, count);
                }
                if (size == 0) throw new IOException("The selected image is empty");
                output.getFD().sync();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(temporary.getAbsolutePath(), options);
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                throw new IOException("The selected file is not a readable image");
            }
            if (!temporary.renameTo(destination)) {
                throw new IOException("Could not save the profile photo");
            }
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    public static void delete(Context context, String uid) {
        File photo = fileFor(context, uid);
        if (photo.exists()) photo.delete();
    }

    public static final class PhotoTooLargeException extends IOException {
        public PhotoTooLargeException() {
            super("The image exceeds the 5 MiB limit");
        }
    }
}
