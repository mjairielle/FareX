package farex.shared.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    void testPassengerProperties() {
        Passenger p = new Passenger("p1", "Ana", "ana@mail.com", "STUDENT");
        assertEquals("p1", p.getId());
        assertEquals("Ana", p.getName());
        assertEquals("ana@mail.com", p.getEmail());
        assertEquals("PASSENGER", p.role());
        assertEquals(20, p.getFareRule().discountPercent());
    }

    @Test
    void testOperatorProperties() {
        Operator op = new Operator("o1", "Ben", "ben@mail.com", "ABC-123");
        assertEquals("o1", op.getId());
        assertEquals("Ben", op.getName());
        assertEquals("OPERATOR", op.role());
        assertEquals("ABC-123", op.getVehiclePlate());
    }

    @Test
    void testAdminProperties() {
        Admin admin = new Admin("a1", "Cora", "cora@mail.com");
        assertEquals("a1", admin.getId());
        assertEquals("Cora", admin.getName());
        assertEquals("ADMIN", admin.role());
    }
}
