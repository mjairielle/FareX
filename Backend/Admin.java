package Backend;

public class Admin extends User {
    public void suspendAccount(String accountID) { }
    public void reinstateAccount(String accountID) { }
    public void verifyDiscountType(String accountID, PassengerType type) { }
    public void reviewSubmittedStop(Stop jeepStop, boolean approve) { }
    public void editRoute(Route route) { }
    public void publishFareTable(FareTable faretable) { }
    public void assignVehicle(Vehicle vehicle, Operator operator) { }
    public void retireVehicle(Vehicle vehicle) { }
    public void createPromotion(Promotion promotion) { }
}
