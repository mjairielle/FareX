package farex.shared.card;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardPayloadTest {
    private static final byte[] KEY = "test-key".getBytes();

    @Test
    void sealThenOpenRoundTrips() {
        CardIO card = new SimCardIO("0067676767");
        card.writePayload(CardPayload.seal(card.readUid(), 45995, 7, KEY));
        var d = CardPayload.open(card.readUid(), card.readPayload(), KEY).orElseThrow();
        assertEquals(45995, d.cents());
        assertEquals(7, d.counter());
    }

    @Test
    void editedBalanceIsRejected() {
        byte[] p = CardPayload.seal("0067676767", 100, 1, KEY);
        p[3] = (byte) 0xFF;
        assertTrue(CardPayload.open("0067676767", p, KEY).isEmpty());
    }

    @Test
    void payloadCopiedToAnotherCardIsRejected() {
        byte[] p = CardPayload.seal("0067676767", 100, 1, KEY);
        assertTrue(CardPayload.open("0011223344", p, KEY).isEmpty());
    }

    @Test
    void wrongKeyOrBlankCardIsRejected() {
        byte[] p = CardPayload.seal("0067676767", 100, 1, KEY);
        assertTrue(CardPayload.open("0067676767", p, "other".getBytes()).isEmpty());
        assertTrue(CardPayload.open("0067676767", new byte[16], KEY).isEmpty());
    }
}
