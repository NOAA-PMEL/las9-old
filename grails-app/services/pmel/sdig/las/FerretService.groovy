package pmel.sdig.las

import grails.transaction.Transactional
import java.io.File
import pmel.sdig.las.Ferret
import pmel.sdig.las.Task

@Transactional
class FerretService {

	def runScript(File file) {

	}

	def Map runScript (StringBuffer script) {

		def result = [:]
		
		def ferret = Ferret.first()

		
		File sp = File.createTempFile("script", ".jnl", new File(ferret.tempDir));
		
		sp.withWriter { out ->
			out.writeLine(script.toString().stripIndent())
		}

		Task task = new Task(ferret, sp.getAbsolutePath() )
		try {
            task.run()
		} catch (Exception e ) {
            task.appendError("ERROR: Exception running task.  ")
			task.appendError(e.getMessage())
        }


		if ( task.hasErrors() ) {
			result["error"] = true
			result["message"] = task.getErrors().toString();
		} else {
			result["error"] = false
		}

		return result
		
	}
	
}
