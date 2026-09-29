import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;
public class Passenger {
    private String accountID;
    private String name;
    private String pin;
    private String phoneNumber;
    private String email;
    private String classification;
    private Map<String, Double> cards;
    private Queue<Double> incomingLoad;

    public Double fetchLoad(String accID){}
    public Double inquireBalance(String accID){}
    public void buyLoad(String accID){}
    public void changePin(){}
} 
