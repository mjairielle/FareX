package FareX;
public class Promotion { 
    private String promoID; 
    private double multiplier; 
    private LocalDate start, end; 
    private Route targetRoute; }

public class RewardsAccount {
    private String accountID;
    private double totalPoints;

    public double earn(double distanceKm, Promotion activePromo) {}
    public boolean redeem(double points) {}
}