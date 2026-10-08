package com.farex;

import farex.shared.data.Store;
import farex.shared.user.User;
import java.nio.file.Path;

/** Who is logged in, and the one Store every screen uses. CSV files live in ./data */
public final class Session {
    public static final Store store = new Store(Path.of("data"));
    public static User user;

    private Session() {}
}
