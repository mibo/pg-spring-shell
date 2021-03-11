package org.github.mibo.shell.commands

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.shell.Availability
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.net.URI

@ShellComponent
class GitHubCmd(val config: Environment) {

  val prefix = "github"

  var env: String = "dev"
  var repo = ""

  @ShellMethod("Set env")
  fun env(@ShellOption(defaultValue = "dev") env: String) {
    if (config.getProperty("$prefix.$env.token") != null) {
      println("Changed env to $env")
      this.env = env
    } else {
      println("Unable to change env to $env (keep ${this.env})")
    }
  }

  @ShellMethod("Repository")
  fun repo(@ShellOption repo: String) {
    this.repo = repo
    println("Changed repo to $repo")
  }

  fun searchAvailability(): Availability? {
    return if (repo.isEmpty()) Availability.unavailable("no repository set") else Availability.available()
  }

  //https://api.github.com/search/code\?q=test+in:file+language:kotlin+repo:mibo/miftp
  @ShellMethod("Search")
  fun search(searchTerms: List<String>) {
    println("Search params: $searchTerms")
    val searchQuery = searchTerms.joinToString("+")
    val query = if (repo.isEmpty()) searchQuery else "$searchQuery+repo:$repo"
    println("Search query: $query")
    val uri = URI(baseUrl("/search/code?q=$query"))
    println("Request url: ${uri.toString()}")
    val rest = RestTemplateBuilder().build()
    val headers = HttpHeaders()
    headers.set("Accept", "application/vnd.github.v3+json")
    val response = rest.exchange(uri, HttpMethod.GET, HttpEntity(null, headers), String::class.java)
    response.body?.let {
      val gson = GsonBuilder().setPrettyPrinting().create()
      val json = gson.fromJson(it, JsonObject::class.java)
      println(gson.toJson(json))
    }
  }

  private fun baseUrl(postfix: String): String {

    var normalizedPostfix = if (postfix.endsWith("/")) postfix.substring(0, postfix.length-2) else postfix
    normalizedPostfix = if (normalizedPostfix.startsWith("/")) normalizedPostfix.substring(1) else normalizedPostfix
    val baseurl = config.getProperty("$prefix.$env.url")
    println("Url: $baseurl")

    if (baseurl == null) {
      throw IllegalStateException();
    }
    return if (baseurl.endsWith("/")) {
      baseurl + normalizedPostfix
    } else {
      "$baseurl/$normalizedPostfix"
    }
  }

  @ShellMethod("List url")
  fun url(@ShellOption(defaultValue = "dev") token: String) {

    println("Url: " + config.getProperty("$prefix.$env.url"))
  }

  @ShellMethod("List token")
  fun token(@ShellOption(value=["-t"], defaultValue = "github.token") token: String) {

    println("Token: " + config.getProperty("$prefix.$env.token"))
  }
}