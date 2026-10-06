package farex.shared.user;

import farex.shared.fare.FareRule;

public class Passenger extends User {
    private final FareRule fareRule;

    public Passenger(String id, String name, String email, FareRule fareRule) {
        super(id, name, email);
        this.fareRule = fareRule;
    }

    public FareRule getFareRule() {
        return fareRule;
    }

    @Override
    public String homeScreen() {
        return "PassengerHome";
    }
}