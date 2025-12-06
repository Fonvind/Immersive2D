package github.fonvind.immersive2d.client.access;

public interface MouseForceUpdate {
    void immersive2d$forceNormalizedUpdate();
    void immersive2d$setLastPosition(double x, double y);
    double immersive2d$getLastX();
    double immersive2d$getLastY();
    void immersive2d$forceInternalCursorUpdate();
}
