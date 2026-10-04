package farex.app.operator;

/** Shift and PIN lock. Phone stays open all day, PIN stops a passenger from opening the earnings screen. */
public class OperatorSession {
    private boolean unlocked, onShift;

    public boolean unlock(String pin) { throw new UnsupportedOperationException("TODO: compare hash of pin"); }
    public void lock() { unlocked = false; }
    public void startShift() { throw new UnsupportedOperationException("TODO: needs assigned vehicle, start GpsTracker"); }
    public void endShift() { throw new UnsupportedOperationException("TODO: force one last sync"); }
    public boolean isOnShift() { return onShift; }
}
