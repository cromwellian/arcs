package arcs.android.accelerator

import arcs.android.accelerator.Log.logger
import com.beust.klaxon.*
import java.util.logging.Logger

object Log {
  val logger: Logger = Logger.getLogger(AcceleratorPipesShell::class.java.name)
}

/**
 * Implements a version of the Pipes-Shell directly in Kotlin. Responds to the same
 * commands as the normal runtime, except that manifest information and recipe resolution
 * has been performed at build time.
 */
class AcceleratorPipesShell(private val arcById: MutableMap<String, Arc> = mutableMapOf()) {

  fun receive(json: String) {
    logger.severe("JSON: $json")
    parseMessage(json)?.process(this)
  }

  fun parseMessage(json: String) = Klaxon()
      .parse<ShellMessage>(json)

  fun findRecipeByName(recipe: String): Any {
    when (recipe) {
      "IngestPeople" -> IngestPeopleRecipe()
    }
    throw RuntimeException("Can't find recipe $recipe")
  }

  fun spawnOrFindArc(arcId: String): Arc {
    if (!arcById.containsKey(arcId)) {
      arcById[arcId] = Arc(arcId)
    }
    return arcById[arcId]!!
  }

  fun instantiateRecipe(arc: Arc, action: Any, particles: List<ParticleData>): Boolean {
    return true
  }
}