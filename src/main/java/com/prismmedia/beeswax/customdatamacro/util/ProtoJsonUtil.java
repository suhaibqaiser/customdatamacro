package com.prismmedia.beeswax.customdatamacro.util;

import com.beeswax.openrtb.Openrtb;
import com.google.protobuf.TextFormat;

/**
 * Generic ProtoJsonUtil to be used to serialize and deserialize Proto to json
 *
 *
 */
public final class ProtoJsonUtil {

    /**
     * Convert from Text Proto
     * @param text
     * @return
     */
    public static Openrtb.BidRequest parseFromProtoBuf(final CharSequence text) {
        try {
            Openrtb.BidRequest.Builder builder = Openrtb.BidRequest.newBuilder();
            TextFormat.getParser().merge(text, builder);
            return builder.build();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


}

