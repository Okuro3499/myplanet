package org.ole.planet.myplanet.ui.chat

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.ole.planet.myplanet.datamanager.ApiInterface
import org.ole.planet.myplanet.model.ChatModel
import org.ole.planet.myplanet.utilities.Utilities
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Singleton
class ChatApiHelper @Inject constructor(private val apiInterface: ApiInterface) {
    fun fetchAiProviders(result: (Map<String, Boolean>?) -> Unit) {
        apiInterface.checkAiProviders("${Utilities.hostUrl}checkProviders/")?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val providers: Map<String, Boolean> = Gson().fromJson(
                            response.body()!!.string(),
                            object : TypeToken<Map<String, Boolean>>() {}.type
                        )
                        result(providers)
                    } catch (e: Exception) {
                        result(null)
                    }
                } else {
                    result(null)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                result(null)
            }
        })
    }

    fun sendChatRequest(content: RequestBody, callback: Callback<ChatModel>) {
        apiInterface.chatGpt(Utilities.hostUrl, content)?.enqueue(callback)
    }
}
