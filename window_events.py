# COMMAND ----------
# Notebook: Fun Data Processing Pipeline
from pyspark.sql import functions as F
from pyspark.sql.types import *

# COMMAND ----------
# Create sample game events data
game_events_schema = StructType([
    StructField("player_id", StringType()),
    StructField("timestamp", TimestampType()),
    StructField("event_type", StringType()),
    StructField("location", StructType([
        StructField("x", DoubleType()),
        StructField("y", DoubleType())
    ])),
    StructField("details", MapType(StringType(), StringType()))
])

# COMMAND ----------
# Generate sample data
sample_data = spark.createDataFrame([
    ("p1", "2024-01-01 10:00:00", "SPELL_CAST", {"x": 100.0, "y": 200.0}, {"spell": "fireball", "damage": "50"}),
    ("p2", "2024-01-01 10:00:05", "MOVEMENT", {"x": 150.0, "y": 175.0}, {"speed": "fast", "direction": "north"}),
], game_events_schema)

# COMMAND ----------
# Widget for interactive filtering
dbutils.widgets.dropdown("event_type", "SPELL_CAST", ["SPELL_CAST", "MOVEMENT", "ITEM_USE"])

# COMMAND ----------
# Process with Delta Lake
sample_data.write.format("delta").mode("overwrite").saveAsTable("game_events")

# COMMAND ----------
# Complex aggregation with window functions
from pyspark.sql.window import Window

window_spec = Window.partitionBy("player_id").orderBy("timestamp")

analysis_df = spark.sql("""
SELECT 
    player_id,
    timestamp,
    event_type,
    details['spell'] as spell_name,
    location.x as x_pos,
    location.y as y_pos,
    LAG(event_type) OVER (PARTITION BY player_id ORDER BY timestamp) as previous_event,
    COUNT(*) OVER (PARTITION BY player_id) as total_player_actions
FROM game_events
WHERE event_type = getArgument("event_type")
""")

# COMMAND ----------
# Create a Temporary View for SQL queries
analysis_df.createOrReplaceTempView("event_analysis")

# COMMAND ----------
# MLflow tracking example
import mlflow

with mlflow.start_run(run_name="game_analysis"):
    mlflow.log_param("event_type", dbutils.widgets.get("event_type"))
    
    # Calculate some metrics
    metrics = spark.sql("""
        SELECT 
            COUNT(DISTINCT player_id) as unique_players,
            COUNT(*) as total_events,
            AVG(x_pos) as avg_x_position,
            AVG(y_pos) as avg_y_position
        FROM event_analysis
    """).collect()[0]
    
    mlflow.log_metrics({
        "unique_players": metrics[0],
        "total_events": metrics[1],
        "avg_x_position": metrics[2],
        "avg_y_position": metrics[3]
    })

# COMMAND ----------
# Create visualization with Plotly
from plotly import graph_objects as go

def plot_player_movements():
    movement_data = spark.sql("""
        SELECT 
            player_id,
            x_pos,
            y_pos
        FROM event_analysis
        ORDER BY timestamp
    """).toPandas()
    
    fig = go.Figure()
    
    for player in movement_data['player_id'].unique():
        player_data = movement_data[movement_data['player_id'] == player]
        fig.add_trace(go.Scatter(
            x=player_data['x_pos'],
            y=player_data['y_pos'],
            mode='lines+markers',
            name=f'Player {player}'
        ))
    
    fig.update_layout(
        title="Player Movement Map",
        xaxis_title="X Position",
        yaxis_title="Y Position"
    )
    
    return fig

displayHTML(plot_player_movements().to_html())

# COMMAND ----------
# Delta Lake Time Travel
historical_data = spark.sql("""
    SELECT * FROM game_events VERSION AS OF 1
    WHERE event_type = 'SPELL_CAST'
""")

# COMMAND ----------
# Structured Streaming example
streaming_events = (spark
    .readStream
    .format("delta")
    .table("game_events")
    .groupBy("event_type", F.window("timestamp", "1 minute"))
    .agg(F.count("*").alias("events_per_minute"))
)

# Write stream to Delta table
query = (streaming_events
    .writeStream
    .format("delta")
    .outputMode("complete")
    .table("event_metrics")
)

# COMMAND ----------
%md
## Analysis Summary
- Processed game events with complex nested structures
- Used Delta Lake for reliable data storage
- Implemented MLflow tracking for metrics
- Created interactive visualizations
- Demonstrated streaming capabilities
