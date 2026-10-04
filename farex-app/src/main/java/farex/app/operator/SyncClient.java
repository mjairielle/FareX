package farex.app.operator;

import farex.app.ApiClient;

/** Runs on a timer (every few minutes) and on manual sync. Retry is safe, server skips duplicate uuids. */
public class SyncClient {
    private final ApiClient api;

    public SyncClient(ApiClient api) { this.api = api; }

    /** Upload unsent ledger rows to /sync/txns and stop rows to /sync/stops. Offline: do nothing, try next tick. */
    public void uploadBatch() {
        // TODO: remember how many local rows were already sent (small offset file), send the rest
        throw new UnsupportedOperationException("TODO");
    }
}
