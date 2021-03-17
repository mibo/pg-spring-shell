package org.github.mibo.shell.commands

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.github.mibo.shell.support.OAuthTokenService
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.lang.IllegalArgumentException
import java.net.URI

@ShellComponent
class SecureConnect(val config: Environment) {

  private var connected = false
  private var token: String = "<not requested>"

  @ShellMethod("Secure Connect to server")
  fun secon() {
//  fun connect(@ShellOption(value = ["-u", "--url"]) url: String) {
    val endpoint = config.getRequiredProperty("secureConnect.oauth2.endpoint")
    val clientId = config.getRequiredProperty("secureConnect.oauth2.clientid")
    val clientSecret = config.getRequiredProperty("secureConnect.oauth2.clientsecret")
    token = OAuthTokenService().jwtToken(clientId, clientSecret, endpoint)
//    println("Token: $token")
  }

  @ShellMethod("Request with token")
  fun get(@ShellOption(value = ["-u", "--url"]) urlName: String,
          @ShellOption(value = ["-p", "--param"]) params: List<String>) {

    if (!connected) {
      secon()
    }
    val url = config.getProperty("secureConnect.connect.urls.$urlName")
    if (url != null) {
      val uri = URI(replacePlaceholder(url, params))
      val rest = RestTemplateBuilder().build()
      val headers = HttpHeaders()
      headers["Authorization"] = "Bearer $token"
      val requestEntity = HttpEntity(null, headers)
      println("Send get request to $uri")
  //    println("Request headers $headers")
      val response = rest.exchange(uri, HttpMethod.GET, requestEntity, String::class.java)
  //    println(response.statusCode)
      if (response.statusCode.is2xxSuccessful) {
        response.body?.let {
          val gson = GsonBuilder().setPrettyPrinting().create()
          val json = gson.fromJson(it, JsonObject::class.java)
          println(gson.toJson(json))
        }
      } else {
        println("Not successful request: ${response.statusCode}")
      }
    } else {
      println("Given url name $urlName is not configured")
    }
  }

  private fun replacePlaceholder(url: String, params: List<String>): String {
    println("Replace placeholders in $url with $params")
    val regex = Regex("\\{}")
    val placeholderCount = regex.findAll(url).count()
    if (placeholderCount != params.size) {
      println("Placeholder count not fit to parameter count")
      throw IllegalArgumentException("Placeholder count not fit to parameter count")
    }
    var result = url
    params.forEach {
      result = result.replaceFirst("{}", it)
    }
    return result
  }
}