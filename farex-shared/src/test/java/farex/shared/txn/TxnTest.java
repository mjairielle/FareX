package farex.shared.txn;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class TxnTest {

    @Test
    void testCardLoadReceipt() {
        Instant now = Instant.now();
        CardLoad load = new CardLoad("tx1", "p1", "0067676767", 10000, 50000, 60000, now, "GCash");
        assertEquals("CARD_LOAD", load.type());
        assertEquals(10000, load.getAmount());
        assertEquals("Loaded P100.00 via GCash", load.describe());
    }

    @Test
    void testFarePaymentReceipt() {
        Instant now = Instant.now();
        FarePayment pay = new FarePayment("tx2", "p1", "0067676767", 1640, 60000, 58360, now, "o1", 5.0);
        assertEquals("FARE_PAYMENT", pay.type());
        assertEquals(1640, pay.getAmount());
        assertTrue(pay.describe().contains("Fare P16.40, 5.0 km"));
    }
}
