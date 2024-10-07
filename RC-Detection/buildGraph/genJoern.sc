def getJoern(srcDir: String, outDir: String) = {
   importCode(inputPath=srcDir,projectName="tmp")
   
   import scala.collection.mutable.Map
   var nameCount:Map[String,Int]=Map()   

   for(method <- cpg.method.l) {
      var fullName = method.fullName.split(":")(0)   
      if(nameCount.contains(fullName)) {
         var cnt = nameCount.get(fullName).get
         nameCount.update(fullName,cnt+1)
         fullName = fullName + cnt
      }else{
         nameCount.update(fullName,1)
      }
      method.dotCpg14.l |> outDir + "/" + fullName 
   }
}

@main def genAllJoern() = {
   for ( i <- Range(0,1588)) {
      var dirName = "../trainData/test" + i
      try {
         var beforeDirName = dirName + "/before"
         var beforeOutputDir = beforeDirName + "/joern"
         getJoern(beforeDirName,beforeOutputDir)

         var afterDirName = dirName + "/after"
         var afterOutputDir = afterDirName + "/joern"
         getJoern(afterDirName,afterOutputDir)
      } catch {
         case ex : Throwable =>{
            dirName |> "log.txt"
         }
      }
      println(dirName)
   }
}