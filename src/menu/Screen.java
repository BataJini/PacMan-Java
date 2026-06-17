package menu;

/** A CardLayout screen that wants lifecycle hooks for focus and timers. */
public interface Screen {
    default void onShow() {}
    default void onHide() {}
}
