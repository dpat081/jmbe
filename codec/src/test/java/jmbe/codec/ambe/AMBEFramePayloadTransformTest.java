package jmbe.codec.ambe;

import java.lang.reflect.Field;
import java.util.Random;
import jmbe.iface.IPayloadTransform;
import org.junit.Test;
import static org.junit.Assert.*;

/** Frame-level tests only; no P25 keystream or audio interoperability claim. */
public class AMBEFramePayloadTransformTest
{
    private static int[] parameters(AMBEFrame frame) throws Exception
    {
        Field field = AMBEFrame.class.getDeclaredField("mB");
        field.setAccessible(true);
        return (int[])field.get(frame);
    }

    private static void assertRejected(IPayloadTransform transform)
    {
        try
        {
            new AMBEFrame(new byte[9], transform);
            fail("Expected malformed transform to be rejected");
        }
        catch(IllegalArgumentException expected)
        {
            //Expected contract rejection.
        }
    }

    @Test
    public void identityMatchesNoTransformForDeterministicFrames() throws Exception
    {
        Random random = new Random(109L);
        for(int sample = 0; sample < 512; sample++)
        {
            byte[] encoded = new byte[9];
            if(sample != 0) random.nextBytes(encoded);
            byte[] original = encoded.clone();
            AMBEFrame plain = new AMBEFrame(encoded);
            int[] calls = {0};
            AMBEFrame identity = new AMBEFrame(encoded, (payload, bitCount) ->
            {
                calls[0]++;
                assertEquals(49, bitCount);
                assertEquals(7, payload.length);
                assertEquals(0, payload[6] & 0x7F);
                return payload;
            });
            assertEquals(1, calls[0]);
            assertArrayEquals(original, encoded);
            assertEquals(plain.getFrameType(), identity.getFrameType());
            assertEquals(plain.getFundamentalFrequency(), identity.getFundamentalFrequency());
            assertArrayEquals(plain.getErrors(), identity.getErrors());
            assertArrayEquals(parameters(plain), parameters(identity));
            if(plain.isToneFrame())
            {
                assertEquals(plain.getToneParameters().getTone(), identity.getToneParameters().getTone());
                assertEquals(plain.getToneParameters().getAmplitude(), identity.getToneParameters().getAmplitude());
            }
        }
    }

    @Test
    public void explicitNullMatchesExistingConstructor() throws Exception
    {
        byte[] input = new byte[9];
        AMBEFrame existing = new AMBEFrame(input);
        AMBEFrame explicitNull = new AMBEFrame(input, null);
        assertEquals(existing.getFrameType(), explicitNull.getFrameType());
        assertEquals(existing.getFundamentalFrequency(), explicitNull.getFundamentalFrequency());
        assertArrayEquals(existing.getErrors(), explicitNull.getErrors());
        assertArrayEquals(parameters(existing), parameters(explicitNull));
    }

    @Test
    public void transformedPayloadIsInterpretedWithoutChangingCorrectionCounts() throws Exception
    {
        byte[] input = new byte[9];
        AMBEFrame existing = new AMBEFrame(input);
        AMBEFrame transformed = new AMBEFrame(input, (payload, bitCount) ->
        {
            byte[] output = new byte[7];
            output[0] = 0x10;
            return output;
        });
        assertFalse(transformed.isToneFrame());
        assertArrayEquals(new int[]{8,0,0,0,0,0,0,0,0}, parameters(transformed));
        assertEquals(AMBEFundamentalFrequency.fromValue(8), transformed.getFundamentalFrequency());
        assertArrayEquals(existing.getErrors(), transformed.getErrors());
    }

    @Test
    public void nullOutputIsRejected()
    {
        assertRejected((payload, bitCount) -> null);
    }

    @Test
    public void incorrectLengthsAreRejected()
    {
        assertRejected((payload, bitCount) -> new byte[6]);
        assertRejected((payload, bitCount) -> new byte[8]);
    }

    @Test
    public void nonzeroUnusedBitsAreRejected()
    {
        assertRejected((payload, bitCount) ->
        {
            byte[] output = payload.clone();
            output[6] |= 1;
            return output;
        });
    }

    @Test
    public void inputMutationIsRejected()
    {
        assertRejected((payload, bitCount) ->
        {
            payload[0] ^= (byte)0x80;
            return payload;
        });
    }

    @Test
    public void callbackFailureIsNotSilentlyReplacedByCiphertext()
    {
        IllegalStateException failure = new IllegalStateException("test callback failure");
        try
        {
            new AMBEFrame(new byte[9], (payload, bitCount) -> { throw failure; });
            fail("Expected callback failure");
        }
        catch(IllegalStateException actual)
        {
            assertSame(failure, actual);
        }
    }
}
