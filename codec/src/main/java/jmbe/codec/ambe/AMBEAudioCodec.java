/*
 * Copyright (C) 2015-2019 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package jmbe.codec.ambe;

import jmbe.audio.AudioWithMetadata;
import jmbe.codec.FrameType;
import jmbe.iface.IAudioWithMetadata;
import jmbe.iface.IPayloadTransform;
import jmbe.iface.IPayloadTransformAudioCodec;

/** Audio converter for AMBE 3600 x 2450 encoded frames. */
public class AMBEAudioCodec implements IPayloadTransformAudioCodec
{
    public static final String CODEC_NAME = "AMBE 3600 x 2450";
    private AMBESynthesizer mSynthesizer = new AMBESynthesizer();
    private IPayloadTransform mPayloadTransform;

    public AMBEAudioCodec() { }

    @Override
    public void setPayloadTransform(IPayloadTransform transform)
    {
        mPayloadTransform = transform;
    }

    public float[] getAudio(byte[] frameData)
    {
        return getAudio(new AMBEFrame(frameData, mPayloadTransform));
    }

    /** Synthesizes an already decoded frame; does not apply the transform again. */
    public float[] getAudio(AMBEFrame ambeFrame)
    {
        return mSynthesizer.getAudio(ambeFrame);
    }

    @Override
    public IAudioWithMetadata getAudioWithMetadata(byte[] frameData)
    {
        AMBEFrame frame = new AMBEFrame(frameData, mPayloadTransform);
        AudioWithMetadata result = AudioWithMetadata.create(getAudio(frame));
        if(frame.getFrameType() == FrameType.TONE)
        {
            Tone tone = frame.getToneParameters().getTone();
            if(Tone.CALL_PROGRESS_TONES.contains(tone))
                result.addMetadata("CALL PROGRESS", tone.toString());
            else if(Tone.DISCRETE_TONES.contains(tone))
                result.addMetadata("TONE", tone.toString());
            else if(Tone.DTMF_TONES.contains(tone))
                result.addMetadata("DTMF", tone.toString());
            else if(Tone.KNOX_TONES.contains(tone))
                result.addMetadata("KNOX", tone.toString());
        }
        return result;
    }

    @Override
    public void reset()
    {
        mPayloadTransform = null;
        mSynthesizer.reset();
    }

    @Override
    public String getCodecName() { return CODEC_NAME; }
}
