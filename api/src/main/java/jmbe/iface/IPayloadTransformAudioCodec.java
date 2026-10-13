package jmbe.iface;

/** Optional extension; the existing IAudioCodec contract is unchanged. */
public interface IPayloadTransformAudioCodec extends IAudioCodec
{
    /**
     * Installs the transform for subsequent encoded-frame calls. Null disables it.
     * Caller must serialize configuration, decoding and reset on each instance.
     * Stateful transforms must not be shared across calls or timeslots.
     * reset() clears this reference but does not close the caller-owned transform.
     */
    void setPayloadTransform(IPayloadTransform transform);
}
