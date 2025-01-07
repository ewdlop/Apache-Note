import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

// Create Spark session with a joke-friendly name
val spark = SparkSession.builder()
  .appName("ProgrammerJokeGenerator")
  .getOrCreate()

import spark.implicits._

// Create dataset of programming concepts
val programmingConcepts = Seq(
  ("Java", "verbosity"),
  ("Python", "indentation"),
  ("JavaScript", "undefined"),
  ("CSS", "alignment")
).toDF("language", "feature")

// Create dataset of programmer reactions
val programmerReactions = Seq(
  ("verbosity", "writes 100 lines"),
  ("indentation", "spaces vs tabs war"),
  ("undefined", "is not a function"),
  ("alignment", "works on Chrome only")
).toDF("feature", "reaction")

// Build the joke setup
val jokeSetup = programmingConcepts
  .join(programmerReactions, "feature")
  .withColumn("joke_line", 
    concat(
      lit("Why did the "), 
      col("language"), 
      lit(" programmer quit their job? Because "), 
      col("reaction"),
      lit(" 😅")
    ))

// Add punchline using window function
val finalJoke = jokeSetup
  .withColumn("random_pick", rand())
  .orderBy("random_pick")
  .limit(1)
  .select("joke_line")

// Tell the joke
finalJoke.show(false)

// Add meta-joke about Spark processing
println("""
  |Note: This joke was processed in parallel across 
  |multiple nodes just to tell a single programming joke. 
  |Talk about distributed comedy! 🚀
  """.stripMargin)

// Cleanup our comedy cluster
spark.stop()
