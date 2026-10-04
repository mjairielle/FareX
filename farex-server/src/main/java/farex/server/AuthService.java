package farex.server;

import farex.shared.Model.User;

/** users.csv. Register, login, tokens, profile. Admins are created only by an existing admin. */
public class AuthService {
    public User register(String name, String email, String phone, String password, String roles) {
        // TODO: validate input, reject duplicate email or phone, hash with PBKDF2 (JDK), append users.csv
        // ponytail: roles may hold PASSENGER and OPERATOR together, ADMIN is refused here
        throw new UnsupportedOperationException("TODO");
    }

    /** Returns a random token. */
    public String login(String emailOrPhone, String password) {
        // TODO: check hash, status must be ACTIVE, put token in a ConcurrentHashMap (lost on restart)
        throw new UnsupportedOperationException("TODO");
    }

    public User userFor(String token) { throw new UnsupportedOperationException("TODO"); }
    public void updateProfile(String accountId, String name, String phone, String email) { throw new UnsupportedOperationException("TODO"); }
    public void changePin(String accountId, String oldPin, String newPin) { throw new UnsupportedOperationException("TODO"); }
}
