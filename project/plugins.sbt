addSbtPlugin("me.lessis" % "less-sbt" % "0.1.9")

addSbtPlugin("com.github.philcali" % "sbt-jslint" % "0.1.1")

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(
    Resolver.ivyStylePatterns)
