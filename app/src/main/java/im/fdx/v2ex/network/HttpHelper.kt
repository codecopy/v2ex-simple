package im.fdx.v2ex.network

import com.crashlytics.android.Crashlytics
import com.readystatesoftware.chuck.ChuckInterceptor
import im.fdx.v2ex.MyApp
import im.fdx.v2ex.network.cookie.MyCookieJar
import im.fdx.v2ex.network.cookie.SharedPrefsPersistor
import okhttp3.*
import java.io.IOException

/**
 * Created by fdx on 2016/11/20.
 * fdx will maintain it
 */

object HttpHelper {

    val myCookieJar: MyCookieJar = MyCookieJar(SharedPrefsPersistor(MyApp.get()))

    val OK_CLIENT: OkHttpClient = OkHttpClient().newBuilder()
//                        .addInterceptor(HttpLoggingInterceptor())
            //            .connectTimeout(10, TimeUnit.SECONDS)
            //            .writeTimeout(10, TimeUnit.SECONDS)
            //            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)  //禁止重定向
        .addInterceptor(ChuckInterceptor(MyApp.get()))//好东西，查看Okhttp数据
            .addInterceptor { chain ->
                chain.proceed(chain.request())
            }
            .addInterceptor { chain ->
                val request = chain.request()
                        .newBuilder()
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7")
                        .header("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6")
                        .header("Host", "www.v2ex.com")
                        .header("Cache-Control", "max-age=0")
                        //  .header("X-Requested-With", "com.android.browser")
                        //  .header("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3013.3 Mobile Safari/537.36");
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" + " (KHTML, like Gecko) Chrome/58.0.3013.3 Safari/537.36")
                        .build()
                chain.proceed(request)
            }
            .cookieJar(myCookieJar)
            .build()

}

fun vCall(url: String): Call = HttpHelper.OK_CLIENT.newCall(Request.Builder().url(url).build())


fun Call.start(callback: Callback) {
    enqueue(object : Callback {
        override fun onFailure(call: Call?, e: IOException?) {
            Crashlytics.logException(e)
            try {
                callback.onFailure(call!!, e!!)
            } catch (e2: Exception) {
                Crashlytics.logException(e2)
                e2.printStackTrace()
            }
        }

        override fun onResponse(call: Call?, response: Response?) {
            try {
                callback.onResponse(call!!, response!!)
            } catch (e: Exception) {
                Crashlytics.logException(e)
                e.printStackTrace()
            }
        }
    })
}

//
//fun Call.start(onResp: (Call?, Response?) -> Unit, onFailure: (Call?, IOException?)-> Unit){
//    start(object: Callback {
//        override fun onFailure(call: Call, e: IOException) {
//            onFailure(call , e)
//        }
//
//        override fun onResponse(call: Call, response: Response) {
//            onResp(call, response)
//        }
//    })
//}



