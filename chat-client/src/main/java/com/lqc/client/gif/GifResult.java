package com.lqc.client.gif;

public record GifResult(
        String provider,
        String id,
        String title,
        String previewUrl,
        String gifUrl,
        String onLoadUrl,
        String onClickUrl,
        String onSentUrl
) {}
