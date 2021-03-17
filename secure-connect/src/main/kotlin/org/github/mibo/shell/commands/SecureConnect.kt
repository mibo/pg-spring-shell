package org.github.mibo.shell.commands

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.github.mibo.shell.support.OAuthTokenService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.lang.IllegalArgumentException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

@ShellComponent
class SecureConnect(val env: Environment, val args: ApplicationArguments) {

  private var connected = false
  private var token: String = "<not requested>"
  private val config = loadConfig()

  private final fun loadConfig(): Map<String, Any> {
    val configArg = args.getOptionValues("config")
    if (configArg.isEmpty()) {
      println("No configuration name given")
      throw IllegalArgumentException("No configuration name given")
    }
    val configName = configArg[0]
    println("Config name: $configName")
    val yaml = Yaml()
    if (Files.exists(Path.of(configName))) {
      val obj: Map<String, Any> = yaml.load(FileInputStream(File(configName)))
      println(obj)
      return obj
    } else {
      println("No file found at ${Path.of(configName)}. Try classpath.")
      val inputStream = this.javaClass
        .classLoader
        .getResourceAsStream(configName)
      val obj: Map<String, Any> = yaml.load(inputStream)
      println(obj)
      return obj
    }
  }

  @ShellMethod("Secure Connect to server")
  fun secon() {
//    loadConfig()
//  fun connect(@ShellOption(value = ["-u", "--url"]) url: String) {
    val endpoint = getProperty("secureConnect", "oauth2", "endpoint")
    val clientId = getProperty("secureConnect", "oauth2", "clientid")
    val clientSecret = getProperty("secureConnect", "oauth2", "clientsecret")
    token = OAuthTokenService().jwtToken(clientId, clientSecret, endpoint)
//    println("Token: $token")
  }

  fun getProperty(vararg name: String): String {
    var map = config
    var value: Any? = null
    for (n in name) {
      value = map[n]
      if (value is Map<*, *>) {
        map = value as Map<String, Any>
      }
    }
    return value as String
//    return getPropertyR(config, name)
  }

//  fun getPropertyR(config: Map<String, Any>, vararg name: String): String {
//    for (n in name) {
//
//    }
//  }

  @ShellMethod("Request with token")
  fun get(@ShellOption(value = ["-u", "--url"]) urlName: String,
          @ShellOption(value = ["-p", "--param"]) params: List<String>) {

    if (!connected) {
      secon()
    }
    val url = env.getProperty("secureConnect.connect.urls.$urlName")
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