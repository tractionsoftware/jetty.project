#!/bin/sh
exec scala -savecompiled "$0" "$@"
!#

import java.io.File
import java.nio.file._
import scala.util.Properties

val ERROR = Console.RED+"ERROR"+Console.RESET;
val ARROW = Console.CYAN+" => "+Console.RESET;

def PATH(f: Path): String = {
  //f.getParent+"/"+Console.CYAN+f.getName+Console.RESET;
  f.toString
}
def PRINT_XCP(t: Throwable): Unit = {
  Console.print(Console.RED);
  t.printStackTrace();
  Console.print(Console.RESET);
} 

def copy(src: Path, dest: Path): Unit =  {
  Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
  Console.println(PATH(src) + ARROW + PATH(dest));
}

//
// find our teampage source using either the 1st arg, the TEAMPAGE_HOME env, or a default that none of us actually use
//
val teampage = if (argv.size > 0) argv(1) else Properties.envOrElse("TEAMPAGE_HOME", "~/src/hg/teampage");

//
// jetty jars are inside the lib/jetty folder
//
val lib_jetty = new File(teampage + "/lib/jetty");

//
// we determine what files to copy based on what is there. that way if we add or 
// remove dependencies (e.g. websocket, spdy) we will automatically update them.
//
// to find them in the jetty source, we just want the name without the .jar extension
// 
val destnames = lib_jetty.listFiles().map(_.getName.split('.').init.mkString(".")).filter(s => s.startsWith("jetty-") && !s.contains("-sources"));

//
// now move them if we can find them
//
for (name <- destnames) {    

  val target = new File(name+"/target");
  if (target.exists()) {
    //
    // the built jars are not in a specific folder, but in the target folder under each component. 
    // we sort and use headOption to avoid the -config and -sources jars
    //
    val srcs = target.listFiles().filter(f => f.getName.startsWith("jetty-")).sortWith((x,y) => x.getName.length < y.getName.length);
    val dest = new File(lib_jetty, name+".jar");

    srcs.headOption match {
      case Some(src) => {
        try {
          // copy the .jar file
          copy(src.toPath, dest.toPath);

          // include the -sources .jar file
          copy(Paths.get(src.getPath.replace(".jar", "-sources.jar")), Paths.get(dest.getPath.replace(".jar", "-sources.jar")));
        }
        catch {
          case e: Exception => {
            // 
            // report that we couldnt find the jar
            //
            Console.println(ERROR+": Couldn't copy the jar for "+dest.getPath);                  
            PRINT_XCP(e);
          }
        }
      }
      case None => {
        // 
        // report that we couldnt find the jar
        //
        Console.println(ERROR+": Couldn't find the jar for "+dest.getPath);      
      }
    }
  }
  else {
    Console.println(ERROR+": Did Jetty compile? Couldn't find the folder for "+target.getPath);
  }
}

