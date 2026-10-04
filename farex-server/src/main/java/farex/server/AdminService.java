package farex.server;

import farex.shared.Enums.PassengerType;
import farex.shared.Model.*;

/** Writes users, routes, stops, fares, vehicles, promotions CSVs. Every method needs an ADMIN caller, checked in the route. */
public class AdminService {
    public void setSuspended(String accountId, boolean suspended) { throw new UnsupportedOperationException("TODO"); }
    public void verifyDiscount(String accountId, PassengerType type) { throw new UnsupportedOperationException("TODO"); }

    /** Approve moves the row from stop_candidates.csv into stops.csv. Reject deletes it (GPS noise). */
    public void reviewStop(String candidateId, boolean approve) { throw new UnsupportedOperationException("TODO"); }

    public void saveRoute(Route route) { throw new UnsupportedOperationException("TODO"); }

    /** Appends a new row with effective date. Old rows stay, so an operator mid shift keeps the old table. */
    public void publishFares(FareTable table) { throw new UnsupportedOperationException("TODO"); }

    public void assignVehicle(String plateNo, String operatorId) { throw new UnsupportedOperationException("TODO"); }
    public void retireVehicle(String plateNo) { throw new UnsupportedOperationException("TODO"); }
    public void createPromotion(Promotion promo) { throw new UnsupportedOperationException("TODO"); }
}
