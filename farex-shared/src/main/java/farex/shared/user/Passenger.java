package farex.shared.user;

import farex.shared.fare.FareRule;

public class Passenger extends User {
    private final String passengerType;

    public Passenger(String id, String name, String email, String passengerType) {
        super(id, name, email);
        this.passengerType = passengerType;
    }

    public String getPassengerType() { return passengerType; }
    public FareRule getFareRule() { return FareRule.of(passengerType); }

    @Override public String role() { return "PASSENGER"; }
    @Override public String homeScreen() { return "PassengerHome"; }
}
