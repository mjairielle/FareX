package farex.shared.user;

public class Admin extends User {
    public Admin(String id, String name, String email) {
        super(id, name, email);
    }

    @Override public String role() { return "ADMIN"; }
    @Override public String homeScreen() { return "AdminDashboard"; }
}
