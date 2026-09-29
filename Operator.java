package FareX;

public class Operator extends User {
    private Vehicle assignedVehicle;
    private String operatorPin;
    private boolean dashboardUnlocked;
    private boolean onShift;
    private TapDevice tapDevice;
    private XPressCard payoutCard;

    public boolean unlockDashboard(String pin) { return false; }
    public void lockDashboard() { }
    public void startShift() { }
    public void endShift() { }
    public void manualSync() { }
    public void linkPayoutCard(String cardUID) { }
    public double viewEarnings() { return 0.0; }
    public EarningsTransfer requestTransfer(double amount) { return null; }
}
