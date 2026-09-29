package FareX;

import java.time.LocalDateTime;

public class TapSession {
    private String passengerRef;
    private Stop tapInStop, tapOutStop;
    private double gpsLat, gpsLng;
    private LocalDateTime inTime, outTime;
}
