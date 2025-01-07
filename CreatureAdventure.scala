// A small game system with creatures and interactions
object CreatureAdventure extends App {
  // Sealed trait for our creature hierarchy
  sealed trait Creature {
    def name: String
    def power: Int
    def mood: Mood
  }
  
  // Different moods our creatures can have
  sealed trait Mood
  case object Happy extends Mood
  case object Grumpy extends Mood
  case object Sleepy extends Mood
  case object Excited extends Mood

  // Different types of creatures
  case class Dragon(name: String, power: Int, mood: Mood, 
    breathType: String) extends Creature
  case class Unicorn(name: String, power: Int, mood: Mood, 
    sparkleColor: String) extends Creature
  case class Phoenix(name: String, power: Int, mood: Mood, 
    flameIntensity: Int) extends Creature

  // Companion object for creature creation
  object Creature {
    def create(creatureType: String, name: String): Option[Creature] = {
      val random = new scala.util.Random()
      val power = random.nextInt(100) + 1
      val moods = List(Happy, Grumpy, Sleepy, Excited)
      val randomMood = moods(random.nextInt(moods.length))

      creatureType.toLowerCase match {
        case "dragon" => 
          Some(Dragon(name, power, randomMood, "fire"))
        case "unicorn" => 
          Some(Unicorn(name, power, randomMood, "rainbow"))
        case "phoenix" => 
          Some(Unicorn(name, power, randomMood, "golden"))
        case _ => None
      }
    }
  }

  // Interaction system using pattern matching
  def interact(c1: Creature, c2: Creature): String = (c1, c2) match {
    case (Dragon(n1, p1, Happy, _), Dragon(n2, p2, Happy, _)) =>
      s"$n1 and $n2 have a friendly fire-breathing contest!"
      
    case (Dragon(n1, _, Grumpy, _), other) =>
      s"$n1 is too grumpy to play with ${other.name}..."
      
    case (Unicorn(n1, _, Happy, color), Phoenix(n2, _, Excited, _)) =>
      s"$n1 creates $color sparkles while $n2 flies exciting loops!"
      
    case (c1, c2) if c1.mood == Sleepy || c2.mood == Sleepy =>
      s"Shhh... either ${c1.name} or ${c2.name} is sleeping!"
      
    case _ => "The creatures look at each other curiously..."
  }

  // Adventure log using higher-order functions
  class AdventureLog {
    private var events = List[String]()
    
    def record(event: String): Unit = {
      events = event :: events
    }
    
    def summarize: String = {
      events.reverse
        .zipWithIndex
        .map { case (event, idx) => s"${idx + 1}. $event" }
        .mkString("\n")
    }
  }

  // Let's create some creatures and have them interact!
  val log = new AdventureLog()
  
  val dragon = Creature.create("dragon", "Spark").get
  val unicorn = Creature.create("unicorn", "Rainbow").get
  val phoenix = Creature.create("phoenix", "Ember").get
  
  // Record some interactions
  List(
    (dragon, unicorn),
    (unicorn, phoenix),
    (dragon, phoenix)
  ).foreach { case (c1, c2) =>
    log.record(interact(c1, c2))
  }

  // Print the adventure summary
  println("=== Today's Magical Adventures ===")
  println(log.summarize)
}
