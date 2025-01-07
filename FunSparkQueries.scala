// Create SparkSession
val spark = SparkSession.builder()
  .appName("FunSparkQueries")
  .getOrCreate()

// Register temp views for our examples
spark.sql("""
  WITH player_actions AS (
    SELECT 
      player_id,
      EXPLODE(ARRAY(
        NAMED_STRUCT('action', 'fireball', 'power', 100, 'cooldown', 5),
        NAMED_STRUCT('action', 'ice_blast', 'power', 80, 'cooldown', 3),
        NAMED_STRUCT('action', 'lightning', 'power', 120, 'cooldown', 8)
      )) as spell
  )
  
  -- Fun with window functions and spell analysis
  SELECT 
    player_id,
    spell.action as spell_name,
    spell.power as spell_power,
    
    -- Calculate spell efficiency (power per cooldown)
    ROUND(spell.power / spell.cooldown, 2) as efficiency,
    
    -- Rank spells by power
    DENSE_RANK() OVER (ORDER BY spell.power DESC) as power_rank,
    
    -- Add some fun spell descriptions
    CASE 
      WHEN spell.power > 100 THEN '⚡ Super Powerful!'
      WHEN spell.power > 80 THEN '🔥 Pretty Strong'
      ELSE '❄️ Decent Damage'
    END as power_description,
    
    -- Calculate time between spells
    LAG(spell.action) OVER (PARTITION BY player_id ORDER BY spell.power) as previous_spell,
    
    -- Add some combat commentary
    CONCAT(
      'Cast ', 
      spell.action,
      ' dealing ',
      spell.power,
      ' damage every ',
      spell.cooldown,
      ' seconds!'
    ) as battle_log
    
  FROM player_actions
  -- Let's get fancy with some spell filtering
  WHERE spell.power >= 
    (SELECT AVG(spell.power) FROM player_actions)
  
  -- Sort by efficiency for optimal spell rotation
  ORDER BY efficiency DESC
""").show(false)

// Add some aggregations for spell analysis
spark.sql("""
  WITH spell_stats AS (
    SELECT 
      spell.action,
      spell.power,
      spell.cooldown,
      power / cooldown as dps
    FROM player_actions
  )
  
  SELECT 
    'Spell Analysis 🧙' as report_title,
    COUNT(DISTINCT action) as unique_spells,
    ROUND(AVG(power), 2) as avg_power,
    ROUND(AVG(dps), 2) as avg_dps,
    MAX(power) as highest_burst,
    MIN(cooldown) as fastest_cast
  FROM spell_stats
""").show(false)
