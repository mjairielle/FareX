package farex.shared.user;

import farex.shared.fare.DiscountedFare;

public class UserDemo {
    public static void main(String[] args) {
        User[] users = {
            new Passenger("p1", "Ana", "ana@mail.com", new DiscountedFare()),
            new Operator("o1", "Ben", "ben@mail.com", "ABC-123"),
            new Admin("a1", "Cy", "cy@mail.com")
        };
        for (User u : users) {
            System.out.println(u.getName() + " -> " + u.homeScreen());
        }
    }
}