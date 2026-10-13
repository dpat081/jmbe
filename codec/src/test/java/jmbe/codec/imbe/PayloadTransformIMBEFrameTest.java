package jmbe.codec.imbe;

import java.lang.reflect.Field;
import java.util.Random;
import jmbe.binary.BinaryFrame;
import org.junit.Test;
import static org.junit.Assert.*;

/** Frame-level regression tests, not P25 encryption interoperability tests. */
public class PayloadTransformIMBEFrameTest
{
    private static Object field(IMBEFrame frame, String name) throws Exception
    {
        Field field = IMBEFrame.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(frame);
    }

    private static boolean informationBit(int index)
    {
        return (index >= 0 && index < 12) || (index >= 23 && index < 35) ||
            (index >= 46 && index < 58) || (index >= 69 && index < 81) ||
            (index >= 92 && index < 103) || (index >= 107 && index < 118) ||
            (index >= 122 && index < 133) || (index >= 137 && index < 144);
    }

    private static void rejected(Runnable action)
    {
        try
        {
            action.run();
            fail("Expected IllegalArgumentException");
        }
        catch(IllegalArgumentException expected)
        {
            //Expected contract rejection.
        }
    }

    @Test
    public void identityMatchesOriginalDecoder() throws Exception
    {
        Random random = new Random(109L);
        for(int sample = 0; sample < 512; sample++)
        {
            byte[] input = new byte[18];
            if(sample != 0) random.nextBytes(input);
            byte[] original = input.clone();
            IMBEFrame baseline = new IMBEFrame(input);
            int[] calls = {0};
            PayloadTransformIMBEFrame identity = new PayloadTransformIMBEFrame(input, (payload, bits) ->
            {
                calls[0]++;
                assertEquals(88, bits);
                assertEquals(11, payload.length);
                int offset = 0;
                for(int index = 0; index < 144; index++)
                    if(informationBit(index))
                    {
                        boolean packed = (payload[offset >>> 3] & (0x80 >>> (offset & 7))) != 0;
                        assertEquals(baseline.getFrame().get(index), packed);
                        offset++;
                    }
                assertEquals(88, offset);
                return payload;
            });
            assertEquals(1, calls[0]);
            assertArrayEquals(original, input);
            for(int index = 0; index < 144; index++)
                assertEquals(baseline.getFrame().get(index), identity.getFrame().get(index));
            assertEquals(baseline.getFundamentalFrequency(), identity.getFundamentalFrequency());
            assertArrayEquals((int[])field(baseline, "mErrors"), (int[])field(identity, "mErrors"));
            assertEquals(field(baseline, "mErrorCountTotal"), field(identity, "mErrorCountTotal"));
        }
    }

    @Test
    public void nullTransformLeavesCorrectedFrameUnchanged()
    {
        byte[] input = new byte[18];
        IMBEFrame baseline = new IMBEFrame(input);
        PayloadTransformIMBEFrame unchanged = new PayloadTransformIMBEFrame(input, null);
        for(int index = 0; index < 144; index++)
            assertEquals(baseline.getFrame().get(index), unchanged.getFrame().get(index));
        assertEquals(baseline.getFundamentalFrequency(), unchanged.getFundamentalFrequency());
    }

    @Test
    public void transformedInformationChangesFrequencyButNotParityOrErrorCounts() throws Exception
    {
        byte[] input = new byte[18];
        IMBEFrame baseline = new IMBEFrame(input);
        byte[] replacement = new byte[11];
        replacement[0] = (byte)0x80;
        replacement[10] = 0x06;
        PayloadTransformIMBEFrame transformed = new PayloadTransformIMBEFrame(input,
            (payload, bits) -> replacement);
        BinaryFrame frame = transformed.getFrame();
        int offset = 0;
        for(int index = 0; index < 144; index++)
        {
            if(informationBit(index))
            {
                boolean expected = (replacement[offset >>> 3] & (0x80 >>> (offset & 7))) != 0;
                assertEquals(expected, frame.get(index));
                offset++;
            }
            else assertEquals(baseline.getFrame().get(index), frame.get(index));
        }
        assertEquals(88, offset);
        assertEquals(131, frame.getInt(IMBEFrame.VECTOR_B0));
        assertEquals(IMBEFundamentalFrequency.fromValue(131), transformed.getFundamentalFrequency());
        assertArrayEquals((int[])field(baseline, "mErrors"), (int[])field(transformed, "mErrors"));
        assertEquals(field(baseline, "mErrorCountTotal"), field(transformed, "mErrorCountTotal"));
    }

    @Test
    public void invalidEncodedLengthsAreRejected()
    {
        rejected(() -> new PayloadTransformIMBEFrame(null, null));
        rejected(() -> new PayloadTransformIMBEFrame(new byte[17], null));
        rejected(() -> new PayloadTransformIMBEFrame(new byte[19], null));
    }

    @Test
    public void malformedTransformOutputIsRejected()
    {
        rejected(() -> new PayloadTransformIMBEFrame(new byte[18], (payload, bits) -> null));
        rejected(() -> new PayloadTransformIMBEFrame(new byte[18], (payload, bits) -> new byte[10]));
        rejected(() -> new PayloadTransformIMBEFrame(new byte[18], (payload, bits) -> new byte[12]));
        rejected(() -> new PayloadTransformIMBEFrame(new byte[18], (payload, bits) ->
        {
            payload[0] ^= (byte)0x80;
            return payload;
        }));
    }

    @Test
    public void callbackFailurePropagates()
    {
        IllegalStateException failure = new IllegalStateException("test callback failure");
        try
        {
            new PayloadTransformIMBEFrame(new byte[18], (payload, bits) -> { throw failure; });
            fail("Expected callback failure");
        }
        catch(IllegalStateException actual)
        {
            assertSame(failure, actual);
        }
    }
}
