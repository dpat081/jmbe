/*
 * jmbe - Java MBE Library
 * Copyright (C) 2015 Dennis Sheirer
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
package jmbe.codec.imbe;

import jmbe.audio.AudioWithoutMetadata;
import jmbe.iface.IAudioWithMetadata;
import jmbe.iface.IPayloadTransform;
import jmbe.iface.IPayloadTransformAudioCodec;

public class IMBEAudioCodec implements IPayloadTransformAudioCodec
{
    public static final String CODEC_NAME = "IMBE";
    private IMBESynthesizer mSynthesizer;
    private IPayloadTransform mPayloadTransform;

    public IMBEAudioCodec()
    {
        mSynthesizer = new IMBESynthesizer();
    }

    @Override
    public void setPayloadTransform(IPayloadTransform transform)
    {
        mPayloadTransform = transform;
    }

    private IMBEFrame decodeFrame(byte[] frameData)
    {
        return mPayloadTransform == null ? new IMBEFrame(frameData) :
            new PayloadTransformIMBEFrame(frameData, mPayloadTransform);
    }

    public float[] getAudio(byte[] frameData)
    {
        return mSynthesizer.getAudio(decodeFrame(frameData));
    }

    @Override
    public IAudioWithMetadata getAudioWithMetadata(byte[] frameData)
    {
        return AudioWithoutMetadata.create(mSynthesizer.getAudio(decodeFrame(frameData)));
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
