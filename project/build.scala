import sbt._
import Keys._
import java.net.URLClassLoader

object ApplicationBuild extends Build {

  // http://kailuowang.blogspot.com/2013/05/define-arbitrary-tasks-in-play-21.html
  def registerTask(name: String, taskClass: String, description: String) = {
    val sbtTask = (dependencyClasspath in Runtime) map { (deps) =>
      val depURLs = deps.map(_.data.toURI.toURL).toArray
      val classLoader = new URLClassLoader(depURLs, null)
      val task = classLoader.
        loadClass(taskClass).
        newInstance().
        asInstanceOf[Runnable]
      task.run()
    }
    TaskKey[Unit](name, description) <<= sbtTask.dependsOn(compile in Compile)
  }


}
