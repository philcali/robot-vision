package capture
package app

abstract class Mode(val name: String, val desc: String) {
  def output() = {
    val absolute = (0 to (30 - name.length) / 8).map(_ => "\t").mkString
    println((" %s%s%s").format(name, absolute, desc))
  }

  def matches(action: String) = name.split(",").map(_.trim).contains(action)
}

case object Run extends Mode("run, web", "Launches embedded server")
case object Record extends Mode("record", "Records actions only")
case object SetProp extends Mode("set, add", "Sets a vision property")
case object RemoveProp extends Mode("remove, rm", "Removes a vision property")
case object ListProp extends Mode("list, ls", "Lists vision properties")
case object Help extends Mode("actions, help", "Displays this list")
case object Clear extends Mode("c, clean-keys", "Wipes stuck inputs")
case object Generate extends Mode(
  "gen, generate-key",
  "Generates a Chrome extension connection key"
)

object ValidMode {
  lazy val all = List(
    Run, Clear, Record, Generate, SetProp, RemoveProp, ListProp, Help
  )

  def unapply(action: String) = all.find(_.matches(action))
}

