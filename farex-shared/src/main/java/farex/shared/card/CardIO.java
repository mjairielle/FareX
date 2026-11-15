package farex.shared.card;

/** The only door to a physical card. Android Classic now, Ultralight later, SimCardIO on a laptop. */
public interface CardIO {
    String readUid();
    byte[] readPayload();
    void writePayload(byte[] payload);
}
