package farex.shared.user;

public class Operator extends User {
    private final String vehiclePlate;

    public Operator(String id, String name, String email, String vehiclePlate) {
        super(id, name, email);
        this.vehiclePlate = vehiclePlate;
    }

    public String getVehiclePlate() { return vehiclePlate; }

    @Override public String role() { return "OPERATOR"; }
    @Override public String homeScreen() { return "OperatorShift"; }
}
