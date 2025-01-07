WITH RECURSIVE spark_particles AS (
    -- Initial spark generation
    SELECT 
        1 as particle_id,
        RANDOM() * 360 as angle,
        10 as velocity,
        0 as lifetime,
        50 as x_pos,
        50 as y_pos,
        255 as brightness,
        'spark' as particle_type
    
    UNION ALL
    
    -- Particle propagation and physics
    SELECT
        particle_id + 1,
        angle + RANDOM() * 15 - 7.5,  -- Slight angle variation
        CASE 
            WHEN particle_type = 'spark' THEN velocity * 0.95  -- Spark deceleration
            ELSE velocity * 0.98  -- Regular particle deceleration
        END,
        lifetime + 1,
        x_pos + (velocity * COS(RADIANS(angle))),
        y_pos + (velocity * SIN(RADIANS(angle))),
        CASE 
            WHEN particle_type = 'spark' THEN 
                GREATEST(0, brightness - 25)  -- Sparks fade faster
            ELSE 
                GREATEST(0, brightness - 15)  -- Regular fade
        END,
        CASE 
            WHEN RANDOM() < 0.2 AND particle_type = 'spark' THEN 'ember'
            ELSE particle_type
        END
    FROM spark_particles
    WHERE lifetime < 10 AND brightness > 0
    LIMIT 100  -- Maximum particles
)

-- Query to visualize the particle system
SELECT 
    particle_id,
    particle_type,
    ROUND(x_pos, 2) as x,
    ROUND(y_pos, 2) as y,
    ROUND(velocity, 2) as speed,
    brightness,
    CASE 
        WHEN particle_type = 'spark' THEN 
            CONCAT('★ ', -- Spark symbol
                  REPEAT('*', GREATEST(1, brightness::int / 50)))
        ELSE 
            CONCAT('• ', -- Ember symbol
                  REPEAT('.', GREATEST(1, brightness::int / 50)))
    END as visual_effect,
    CASE 
        WHEN particle_type = 'spark' THEN 
            CONCAT('#FF', 
                  LPAD(TO_HEX(brightness), 2, '0'),
                  '00')  -- Reddish sparks
        ELSE 
            CONCAT('#FF', 
                  LPAD(TO_HEX(brightness), 2, '0'),
                  LPAD(TO_HEX(brightness/2), 2, '0'))  -- Orange-ish embers
    END as color
FROM spark_particles
WHERE particle_id <= 50  -- Show first 50 particles
ORDER BY lifetime, particle_id;

-- Explosion trigger function
CREATE OR REPLACE FUNCTION trigger_explosion(
    origin_x FLOAT,
    origin_y FLOAT,
    intensity INT
) RETURNS TABLE (
    particle_id INT,
    x FLOAT,
    y FLOAT,
    type VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.particle_id,
        origin_x + (RANDOM() * intensity * COS(RADIANS(p.angle))),
        origin_y + (RANDOM() * intensity * SIN(RADIANS(p.angle))),
        CASE WHEN RANDOM() < 0.7 THEN 'spark' ELSE 'ember' END
    FROM (
        SELECT 
            GENERATE_SERIES(1, intensity) as particle_id,
            RANDOM() * 360 as angle
    ) p;
END;
$$ LANGUAGE plpgsql;

-- Example usage:
-- SELECT * FROM trigger_explosion(100, 100, 20);  -- Create explosion at (100,100) with 20 particles
