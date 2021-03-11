package org.github.mibo.shell.support

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.apache.http.NameValuePair
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.springframework.http.HttpEntity

import java.io.IOException

import java.util.ArrayList

import java.io.UnsupportedEncodingException
import java.lang.Exception


class OAuthTokenService {

  val gson = GsonBuilder().setPrettyPrinting().create()

  @Throws(IOException::class)
  private fun post(clientId: String, clientSecret: String, oAuthUrl: String): String {
    HttpClients.createDefault().use { client ->
      return execute(client, prepareRequest(clientId, clientSecret, oAuthUrl))
    }
  }

  @Throws(UnsupportedEncodingException::class)
  private fun prepareRequest(clientId: String, clientSecret: String, oAuthUrl: String): HttpPost {
    val httpPost = HttpPost(oAuthUrl)
    val params: MutableList<NameValuePair> = ArrayList()
    params.add(BasicNameValuePair("grant_type", "client_credentials"))
    params.add(BasicNameValuePair("client_id", clientId))
    params.add(BasicNameValuePair("client_secret", clientSecret))
    params.add(BasicNameValuePair("response_type", "token"))
    httpPost.setEntity(UrlEncodedFormEntity(params))
    httpPost.setHeader("Accept", "application/json")
    httpPost.setHeader("Content-type", "application/x-www-form-urlencoded")
    return httpPost
  }

  @Throws(ClientProtocolException::class, IOException::class)
  private fun execute(client: CloseableHttpClient, httpPost: HttpPost): String {
    client.execute(httpPost).use { response ->
      val responseEntity = response.entity
      val json = gson.fromJson(responseEntity.toString(), JsonObject::class.java)
      println("Json: $json")
      return json.get("access_token").asString
    }
  }

  fun jwtToken(clientId: String, clientSecret: String, oAuthUrl: String): String {
    try {
      return post(clientId, clientSecret, oAuthUrl)
    } catch (e: Exception) {
      println("Exception while extracting JWT Token: ${e.message}")
    }
    return ""
  }

}