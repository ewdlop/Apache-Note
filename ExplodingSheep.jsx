import React, { useState, useEffect } from 'react';

const ExplodingSheep = () => {
  const [exploded, setExploded] = useState(false);
  const [sparks, setSparks] = useState([]);

  const createSparks = () => {
    const newSparks = [];
    for (let i = 0; i < 12; i++) {
      const angle = (i * 30) * Math.PI / 180;
      newSparks.push({
        id: i,
        dx: Math.cos(angle) * 5,
        dy: Math.sin(angle) * 5,
        opacity: 1,
        scale: 1,
        x: 200,
        y: 150
      });
    }
    return newSparks;
  };

  const handleClick = () => {
    setExploded(true);
    setSparks(createSparks());
  };

  useEffect(() => {
    if (exploded) {
      const interval = setInterval(() => {
        setSparks(prev => prev.map(spark => ({
          ...spark,
          x: spark.x + spark.dx,
          y: spark.y + spark.dy,
          opacity: spark.opacity - 0.02,
          scale: spark.scale - 0.01
        })));
      }, 20);

      setTimeout(() => {
        clearInterval(interval);
        setExploded(false);
        setSparks([]);
      }, 2000);

      return () => clearInterval(interval);
    }
  }, [exploded]);

  return (
    <div className="w-full h-96 bg-blue-100 relative overflow-hidden" onClick={handleClick}>
      <svg viewBox="0 0 400 300" className="w-full h-full">
        {/* Sky background */}
        <rect width="400" height="300" fill="#87CEEB"/>
        
        {/* Ground */}
        <rect y="200" width="400" height="100" fill="#90EE90"/>
        
        {/* Sheep body */}
        {!exploded && (
          <g>
            {/* Body */}
            <ellipse cx="200" cy="150" rx="40" ry="30" fill="white"/>
            
            {/* Head */}
            <circle cx="170" cy="140" r="20" fill="white"/>
            
            {/* Eyes */}
            <circle cx="165" cy="135" r="3" fill="black"/>
            <circle cx="175" cy="135" r="3" fill="black"/>
            
            {/* Legs */}
            <rect x="185" y="170" width="5" height="20" fill="black"/>
            <rect x="210" y="170" width="5" height="20" fill="black"/>
            <rect x="175" y="170" width="5" height="20" fill="black"/>
            <rect x="200" y="170" width="5" height="20" fill="black"/>
          </g>
        )}
        
        {/* Explosion sparks */}
        {sparks.map(spark => (
          <g key={spark.id} 
             transform={`translate(${spark.x},${spark.y}) scale(${spark.scale})`}
             opacity={spark.opacity}>
            <polygon 
              points="0,-10 3,-3 10,0 3,3 0,10 -3,3 -10,0 -3,-3"
              fill="yellow"
              stroke="orange"
              strokeWidth="1"
            />
          </g>
        ))}
      </svg>
      
      {!exploded && (
        <div className="absolute top-4 left-0 right-0 text-center text-lg font-bold text-gray-700">
          Click the sheep to make it explode!
        </div>
      )}
    </div>
  );
};

export default ExplodingSheep;
