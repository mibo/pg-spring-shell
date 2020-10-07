package org.github.mibo.shell.commands

import org.springframework.core.env.Environment
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class GitHubCmd(val config: Environment) {

  var env: String = "dev"
  val prefix = "github"

  @ShellMethod("Set env")
  fun env(@ShellOption(defaultValue = "dev") env: String) {
    if (config.getProperty("$prefix.$env.token") != null) {
      println("Changed env to $env")
      this.env = env
    } else {
      println("Unable to change env to $env (keep ${this.env})")
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