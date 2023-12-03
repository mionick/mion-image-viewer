package io.mionick.imageviewer.ui;

public enum ButtonId {
    OPEN_IMAGE("Open Image"),
    RESET_TRANSFORM("Reset Transform"),
    LOCK_ROTATION("LOCK_ROTATION"),
    DEFORM("DEFORM"),
    TEST("Test Feature"),
    HIDE_MENU("Show/Hide Menu"), SHOW_GRID("Show Grid");

    String value;

    ButtonId(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
