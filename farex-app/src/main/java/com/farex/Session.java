package com.farex;

import farex.shared.user.User;

/** Who is logged in. All data lives on the server, reached through Api. */
public final class Session {
    public static User user;

    private Session() {}
}
