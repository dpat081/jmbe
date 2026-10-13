package jmbe.codec;

import jmbe.codec.ambe.AMBEAudioCodec;
import jmbe.codec.imbe.IMBEAudioCodec;
import jmbe.iface.IAudioWithMetadata;
import jmbe.iface.IPayloadTransformAudioCodec;
import org.junit.Test;
import static org.junit.Assert.*;

/** Integration smoke tests; not real-call audio or P25 interoperability tests. */
public class PayloadTransformAudioCodecTest
{
    private static IPayloadTransformAudioCodec codec(boolean ambe)
    {
        return ambe ? new AMBEAudioCodec() : new IMBEAudioCodec();
    }

    private static byte[] frame(boolean ambe)
    {
        return new byte[ambe ? 9 : 18];
    }

    @Test
    public void encodedEntryPointsInvokeCallbackExactlyOnce()
    {
        for(boolean ambe : new boolean[]{true, false})
        {
            IPayloadTransformAudioCodec codec = codec(ambe);
            int[] calls = {0};
            codec.setPayloadTransform((payload, bits) ->
            {
                calls[0]++;
                assertEquals(ambe ? 49 : 88, bits);
                assertEquals(ambe ? 7 : 11, payload.length);
                return payload;
            });
            assertEquals(160, codec.getAudio(frame(ambe)).length);
            assertEquals(1, calls[0]);
            assertEquals(160, codec.getAudioWithMetadata(frame(ambe)).getAudio().length);
            assertEquals(2, calls[0]);
        }
    }

    @Test
    public void resetDisablesCallbackUntilReinstalled()
    {
        for(boolean ambe : new boolean[]{true, false})
        {
            IPayloadTransformAudioCodec codec = codec(ambe);
            int[] calls = {0};
            codec.setPayloadTransform((payload, bits) -> { calls[0]++; return payload; });
            codec.getAudio(frame(ambe));
            assertEquals(1, calls[0]);
            codec.reset();
            codec.getAudioWithMetadata(frame(ambe));
            assertEquals(1, calls[0]);
            codec.setPayloadTransform((payload, bits) -> { calls[0]++; return payload; });
            codec.getAudio(frame(ambe));
            assertEquals(2, calls[0]);
        }
    }

    @Test
    public void callbacksAreInstanceLocalAndNullDisablesThem()
    {
        for(boolean ambe : new boolean[]{true, false})
        {
            IPayloadTransformAudioCodec first = codec(ambe);
            IPayloadTransformAudioCodec second = codec(ambe);
            int[] calls = {0, 0};
            first.setPayloadTransform((payload, bits) -> { calls[0]++; return payload; });
            second.setPayloadTransform((payload, bits) -> { calls[1]++; return payload; });
            first.getAudioWithMetadata(frame(ambe));
            assertEquals(1, calls[0]);
            assertEquals(0, calls[1]);
            second.getAudio(frame(ambe));
            assertEquals(1, calls[1]);
            first.setPayloadTransform(null);
            first.getAudio(frame(ambe));
            assertEquals(1, calls[0]);
            second.getAudioWithMetadata(frame(ambe));
            assertEquals(2, calls[1]);
        }
    }

    @Test
    public void identityMatchesNoTransformOnFreshZeroFrameFixture()
    {
        for(boolean ambe : new boolean[]{true, false})
        {
            IPayloadTransformAudioCodec plain = codec(ambe);
            IPayloadTransformAudioCodec identity = codec(ambe);
            identity.setPayloadTransform((payload, bits) -> payload);
            assertArrayEquals(plain.getAudio(frame(ambe)), identity.getAudio(frame(ambe)), 0.0f);

            plain = codec(ambe);
            identity = codec(ambe);
            identity.setPayloadTransform((payload, bits) -> payload);
            IAudioWithMetadata expected = plain.getAudioWithMetadata(frame(ambe));
            IAudioWithMetadata actual = identity.getAudioWithMetadata(frame(ambe));
            assertArrayEquals(expected.getAudio(), actual.getAudio(), 0.0f);
            assertEquals(expected.hasMetadata(), actual.hasMetadata());
            assertEquals(expected.getMetadata(), actual.getMetadata());
        }
    }

    @Test
    public void callbackFailurePropagatesThroughBothEncodedEntryPoints()
    {
        for(boolean ambe : new boolean[]{true, false})
            for(boolean metadata : new boolean[]{true, false})
            {
                IPayloadTransformAudioCodec codec = codec(ambe);
                IllegalStateException failure = new IllegalStateException("test callback failure");
                codec.setPayloadTransform((payload, bits) -> { throw failure; });
                try
                {
                    if(metadata) codec.getAudioWithMetadata(frame(ambe));
                    else codec.getAudio(frame(ambe));
                    fail("Expected callback failure");
                }
                catch(IllegalStateException actual)
                {
                    assertSame(failure, actual);
                }
            }
    }
}
