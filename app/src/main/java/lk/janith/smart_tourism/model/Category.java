package lk.janith.smart_tourism.model;

public class Category {

    private final String id;
    private final String title;
    private final String imageUrl;
    private final String description;
    private final boolean active;
    private boolean selected;

    public Category(String title, boolean selected) {
        this(null, title, null, null, true, selected);
    }

    public Category(String id, String title, String imageUrl, String description,
                    boolean active, boolean selected) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.description = description;
        this.active = active;
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
