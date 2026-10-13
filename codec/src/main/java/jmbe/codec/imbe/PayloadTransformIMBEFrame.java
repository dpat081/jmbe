package jmbe.codec.imbe;

import java.util.Arrays;
import jmbe.binary.BinaryFrame;
import jmbe.iface.IPayloadTransform;

/**
 * Optional payload transform after IMBE correction and derandomization.
 * The original IMBEFrame remains unchanged. Codec integration is separate.
 */
public class PayloadTransformIMBEFrame extends IMBEFrame
{
    private static final int[] STARTS = {0,23,46,69,92,107,122,137};
    private static final int[] LENGTHS = {12,12,12,12,11,11,11,7};
    private final IMBEFundamentalFrequency mTransformedFrequency;

    /**
     * Payload order is the information prefix of each corrected codeword in
     * ascending codeword order, followed by the seven unprotected bits.
     * It is packed MSB-first into 11 bytes. This ordering still requires
     * independent P25 interoperability verification.
     *
     * @param data encoded 18-byte IMBE frame
     * @param transform optional corrected-payload transform; null leaves bits unchanged
     */
    public PayloadTransformIMBEFrame(byte[] data, IPayloadTransform transform)
    {
        super(validate(data));
        if(transform == null)
        {
            mTransformedFrequency = super.getFundamentalFrequency();
            return;
        }
        BinaryFrame frame = getFrame();
        byte[] payload = new byte[11];
        int offset = 0;
        for(int block = 0; block < STARTS.length; block++)
            for(int i = 0; i < LENGTHS[block]; i++, offset++)
                if(frame.get(STARTS[block] + i))
                    payload[offset >>> 3] |= (byte)(0x80 >>> (offset & 7));

        byte[] original = payload.clone();
        byte[] output = transform.transform(payload, 88);
        if(!Arrays.equals(original, payload))
            throw new IllegalArgumentException("Payload transform modified its input");
        if(output == null || output.length != 11)
            throw new IllegalArgumentException("Payload transform must return 88 MSB-first bits in eleven bytes");
        output = output.clone();
        offset = 0;
        for(int block = 0; block < STARTS.length; block++)
            for(int i = 0; i < LENGTHS[block]; i++, offset++)
            {
                boolean value = (output[offset >>> 3] & (0x80 >>> (offset & 7))) != 0;
                int index = STARTS[block] + i;
                if(frame.get(index) != value) frame.flip(index);
            }
        mTransformedFrequency = IMBEFundamentalFrequency.fromValue(frame.getInt(VECTOR_B0));
    }

    private static byte[] validate(byte[] data)
    {
        if(data == null || data.length != 18)
            throw new IllegalArgumentException("IMBE frame must contain eighteen bytes");
        return data.clone();
    }

    /** Model-parameter generation in IMBEFrame uses this accessor. */
    @Override
    public IMBEFundamentalFrequency getFundamentalFrequency()
    {
        return mTransformedFrequency;
    }
}
