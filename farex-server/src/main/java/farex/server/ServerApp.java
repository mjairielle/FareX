package farex.server;

import io.javalin.Javalin;

/**
 * Entry point and every route. Routes stay thin: read params, call one service, return.
 * Replaces AuthHandler, AdminHandler, CardLoadHandler, SyncHandler, TransferHandler, TripHandler.
 */
public class ServerApp {
    public static void main(String[] args) {
        var auth = new AuthService();
        var cards = new CardService();
        var sync = new SyncService();
        var rewards = new RewardsService();
        var trips = new TripService();
        var admin = new AdminService();
        var earnings = new EarningsService();

        var app = Javalin.create().start(8080);

        // login check on every route except register, login, webhook, seed
        app.before(ctx -> { /* TODO: auth.userFor(ctx.header("Authorization")), reject if missing or SUSPENDED */ });

        // account
        app.post("/auth/register", ctx -> { /* TODO: auth.register(...) */ });
        app.post("/auth/login", ctx -> { /* TODO: auth.login(...) returns token */ });
        app.put("/me", ctx -> { /* TODO: auth.updateProfile(...) */ });
        app.put("/me/pin", ctx -> { /* TODO: auth.changePin(...) */ });
        app.get("/seed", ctx -> { /* TODO: stops, routes, fares CSVs for first run offline */ });

        // cards and loading
        app.get("/cards/{uid}/balance", ctx -> { /* TODO: cards.syncedBalance(uid) */ });
        app.post("/cards/link", ctx -> { /* TODO: cards.link(accountId, uid) */ });
        app.post("/cards/lost", ctx -> { /* TODO: cards.reportLost(accountId, oldUid, newUid) */ });
        app.post("/loads", ctx -> { /* TODO: cards.startLoad(...) returns QR */ });
        app.post("/webhooks/paymongo", ctx -> { /* TODO: cards.onWebhook(ctx.body(), header) */ });
        app.post("/credits/claim", ctx -> { /* TODO: cards.claim(...) returns SignedBalance for the NFC write */ });
        app.get("/credits", ctx -> { /* TODO: cards.pendingCredits(accountId) */ });

        // transactions and rewards
        app.get("/txns", ctx -> { /* TODO: sync.history(accountId, optional cardUid) */ });
        app.get("/rewards", ctx -> { /* TODO: rewards.total(accountId) */ });
        app.post("/rewards/redeem", ctx -> { /* TODO: rewards.redeem(accountId, points) */ });

        // trip planner
        app.get("/trip", ctx -> { /* TODO: trips.plan(from, to, passengerType) up to 3 */ });

        // operator
        app.post("/sync/txns", ctx -> { /* TODO: sync.receiveTxns(operatorId, ctx.body()) */ });
        app.post("/sync/stops", ctx -> { /* TODO: sync.receiveStopCandidates(operatorId, ctx.body()) */ });
        app.get("/operator/earnings", ctx -> { /* TODO: earnings.available(operatorId) */ });
        app.post("/operator/transfer", ctx -> { /* TODO: earnings.requestTransfer(operatorId, amount) */ });

        // admin
        app.post("/admin/users/{id}/suspend", ctx -> { /* TODO: admin.setSuspended(id, true or false) */ });
        app.post("/admin/users/{id}/discount", ctx -> { /* TODO: admin.verifyDiscount(id, type) */ });
        app.post("/admin/stops/{id}/review", ctx -> { /* TODO: admin.reviewStop(id, approve) */ });
        app.put("/admin/routes/{id}", ctx -> { /* TODO: admin.saveRoute(...) */ });
        app.post("/admin/fares", ctx -> { /* TODO: admin.publishFares(...) */ });
        app.post("/admin/vehicles", ctx -> { /* TODO: admin.assignVehicle(...) or retireVehicle(...) */ });
        app.post("/admin/promotions", ctx -> { /* TODO: admin.createPromotion(...) */ });
    }
}
