package jmbe.iface;

/**
 * Optional transform of corrected information bits before speech or tone interpretation.
 * This contract does not define P25 bit ordering, cipher initialization, or synchronization.
 */
@FunctionalInterface
public interface IPayloadTransform
{
    /**
     * Transforms one payload. Bits are packed most-significant-bit first in each byte.
     * The input contains exactly (bitCount + 7) / 8 bytes; unused low bits are zero.
     * Implementations must not modify the input and must return a non-null array of
     * the same length with unused low bits zero. An identity transform may return
     * the unchanged input. Calls are synchronous and ordered within one codec instance.
     * Stateful transforms must not be shared across independent calls or timeslots.
     *
     * @param payload corrected information bits, excluding error-correction parity
     * @param bitCount number of valid information bits
     * @return transformed information bits in the same packing and ordering
     */
    byte[] transform(byte[] payload, int bitCount);
}
