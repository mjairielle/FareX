package farex.shared;

/** Every enum in one file. Add values here, not new files. */
public final class Enums {
    private Enums() {}

    public enum Role { PASSENGER, OPERATOR, ADMIN }
    public enum AccountStatus { ACTIVE, SUSPENDED }
    public enum PassengerType { REGULAR, STUDENT, SENIOR, PWD }
    public enum CardStatus { ACTIVE, FROZEN }
    public enum PaymentMethod { GCASH, MAYA, BANK, OTHERS }
    public enum TxnType { CARD_LOAD, FARE_PAYMENT, POINTS_REDEEMED, OPERATOR_TRANSFER }
    public enum LoadStatus { AWAITING_PAYMENT, PAID, EXPIRED, FAILED }
}
