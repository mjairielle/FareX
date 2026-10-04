package farex.app.operator;

/** While on shift, log when the jeepney stops near a known stop or stays still. Rows go to local stops CSV. */
public class GpsTracker {
    public void onPosition(double lat, double lng) {
        // TODO: near a known stop? log arrival. Stopped somewhere new? log a candidate for admin review
    }
}
