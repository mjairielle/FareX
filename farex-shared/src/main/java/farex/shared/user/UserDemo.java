package farex.shared.user;

public class UserDemo {
    public static void main(String[] args) {
        User[] users = {
            new Passenger("p1", "Ana", "ana@mail.com", "STUDENT"),
            new Operator("o1", "Ben", "ben@mail.com", "ABC-123"),
            new Admin("a1", "Cy", "cy@mail.com")
        };
        for (User u : users) {
            System.out.println(u.getName() + " -> " + u.homeScreen());
        }
    }
}
