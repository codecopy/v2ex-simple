package im.fdx.v2ex.utils;

import android.support.annotation.NonNull;

/**
 * Created by a708 on 15-8-18.
 * 来自yaoyumeng的开源
 */

public class ContentUtils {

    public static String format(@NonNull String content) {
        return content.replace("href=\"/member/", "href=\"https://www.v2ex.com/member/")
                .replace("href=\"/i/", "href=\"https://i.v2ex.co/")
                .replace("href=\"/t/", "href=\"https://www.v2ex.com/t/")
                .replace("href=\"/go/", "href=\"https://www.v2ex.com/go/")
                .replace("<img src=\"//", "<img src=\"http://");
    }

}
