package farex.shared.card;

/** One pretend card in memory, for laptop runs and tests. */
public class SimCardIO implements CardIO {
    private final String uid;
    private byte[] payload = new byte[CardPayload.SIZE];

    public SimCardIO(String uid) { this.uid = uid; }

    @Override public String readUid() { return uid; }
    @Override public byte[] readPayload() { return payload.clone(); }
    @Override public void writePayload(byte[] p) { payload = p.clone(); }
}
