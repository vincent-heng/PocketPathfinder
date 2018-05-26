package fr.project24.fox.pocketpathfinder.retrofit

import com.google.gson.GsonBuilder
import fr.project24.fox.pocketpathfinder.MainActivity
import fr.project24.fox.pocketpathfinder.R
import fr.project24.fox.pocketpathfinder.model.Room
import io.reactivex.Observable
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.joda.time.DateTime
import retrofit2.http.GET
import retrofit2.http.Query


interface ApiService {

    @GET("/rest/room")
    fun findRoom(@Query("q") query: String): Observable<List<Room>>

    companion object Factory {
        fun create(): ApiService {
            val gson = GsonBuilder().registerTypeAdapter(DateTime::class.java, DateTimeTypeConverter()).create()
            val interceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder().addHeader("x-apikey", MainActivity.applicationContext().getString(R.string.db_x_apikey)).build()
                chain.proceed(newRequest)
            }

            val builder = OkHttpClient.Builder()
            builder.interceptors().add(interceptor)
            val client = builder.build()

            val retrofit = retrofit2.Retrofit.Builder()
                    .addCallAdapterFactory(retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory.create())
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(gson))
                    .baseUrl(MainActivity.applicationContext().resources.getString(R.string.api_url))
                    .client(client)
                    .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}
