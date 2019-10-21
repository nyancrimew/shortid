package ch.deletescape.shortid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShortIdTest {
    @Test
    fun `Test defaults`() {
        assertEquals(
            "ShortId(worker=0, epoch=1451606400000000000, abc=Abc(alphabet=\"gzmZM7VINvOFcpho01x-fYPs8Q_urjq6RkiWGn4SHDdK5t2TAJbaBLEyUwlX9C3e\"))",
            ShortId.getDefault().toString()
        )
    }

    @Test
    fun `Test setDefault`() {
        assertEquals(
            "ShortId(worker=0, epoch=1451606400000000000, abc=Abc(alphabet=\"gzmZM7VINvOFcpho01x-fYPs8Q_urjq6RkiWGn4SHDdK5t2TAJbaBLEyUwlX9C3e\"))",
            ShortId.getDefault().toString()
        )

        // Different worker, different seed (thus different shuffling)
        ShortId.setDefault(ShortId(1, ShortId.DefaultABC, 2))
        assertEquals(
            "ShortId(worker=1, epoch=1451606400000000000, abc=Abc(alphabet=\"ip8bKduCDxnMQy-JrVHAN5h1s396jBvmFZOL0Pg2WTqwIE7f4ackXzoUSYlGt_eR\"))",
            ShortId.getDefault().toString()
        )
    }
}