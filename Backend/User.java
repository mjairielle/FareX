package FareX;

public abstract class User {
    protected String accountID, name, password, phoneNumber, email;
    protected Role role;
    protected AccountStatus status;

    public boolean login(String emailOrContact, String password) { return false; }
    public void logout() { }
}
