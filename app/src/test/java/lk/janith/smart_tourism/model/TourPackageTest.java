package lk.janith.smart_tourism.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TourPackageTest {
    @Test
    public void parsesOriginalFirestoreTourWithoutBreakingDemoPrice() {
        Map<String, Object> record = new HashMap<>();
        record.put("title", "Heritage tour");
        record.put("categoryId", "culture");
        record.put("price", 390.5);
        record.put("description", "Highlights");
        record.put("route", "Colombo to Kandy");
        record.put("overview", "Five days in Sri Lanka");
        record.put("imageUrl", "https://example.invalid/tour.jpg");
        record.put("featured", true);
        record.put("active", true);

        TourPackage tour = TourPackage.fromRecord("firestore-id", record, 42);

        assertEquals("firestore-id", tour.getId());
        assertEquals("culture", tour.getCategoryId());
        assertEquals("$390.50", tour.getPrice());
        assertEquals(390.5, (Double) tour.getWishlistPrice(), 0.001);
        assertEquals("Colombo to Kandy", tour.getRoute());
        assertEquals("Five days in Sri Lanka", tour.getOverview());
        assertEquals("https://example.invalid/tour.jpg", tour.getImageUrl());
        assertTrue(tour.isFeatured());
        assertTrue(tour.isActive());
    }

    @Test
    public void readsLegacyWishlistStringPriceAndPackageId() {
        Map<String, Object> record = new HashMap<>();
        record.put("packageId", "real-package-id");
        record.put("title", "Sample tour");
        record.put("price", "$520");
        record.put("description", "Sigiriya and Kandy");
        record.put("imageResId", 123);
        record.put("active", false);

        TourPackage tour = TourPackage.fromRecord("wishlist-id", record, 42);

        assertEquals("real-package-id", tour.getId());
        assertEquals("$520", tour.getPrice());
        assertEquals("$520", tour.getWishlistPrice());
        assertEquals("Sigiriya and Kandy", tour.getRoute());
        assertEquals(123, tour.getImageResId());
        assertFalse(tour.isActive());

        TourPackage bundled = new TourPackage("Local demo", "5 Days", "$390", "Colombo", 7);
        assertEquals("$390", bundled.getPrice());
        assertEquals(7, bundled.getImageResId());
    }
}
