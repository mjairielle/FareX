package com.farex;

public class AdminScreen extends Screen {
    public AdminScreen() {
        getChildren().addAll(heading("Admin Dashboard"), text(Api.summary()),
                secondary("Log out", this::logout));
    }
}
