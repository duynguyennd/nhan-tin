// SVG image generator for mock assets
const fs = require('fs');
const path = require('path');

const imgDir = path.join(__dirname, 'demo-client', 'assets', 'images');
if (!fs.existsSync(imgDir)) fs.mkdirSync(imgDir, { recursive: true });

// 1. MinIO S3 Architecture Diagram SVG
const minioSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 480" width="800" height="480">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0a0f24"/>
      <stop offset="100%" stop-color="#040714"/>
    </linearGradient>
    <linearGradient id="gradCyan" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#00f2fe"/>
      <stop offset="100%" stop-color="#4facfe"/>
    </linearGradient>
    <linearGradient id="gradRed" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#f43f5e"/>
      <stop offset="100%" stop-color="#be123c"/>
    </linearGradient>
    <linearGradient id="gradPurple" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#a855f7"/>
      <stop offset="100%" stop-color="#7c3aed"/>
    </linearGradient>
    <filter id="glow">
      <feGaussianBlur stdDeviation="3" result="blur"/>
      <feMerge>
        <feMergeNode in="blur"/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
  </defs>

  <!-- Background -->
  <rect width="800" height="480" rx="16" fill="url(#bg)" stroke="#1e293b" stroke-width="2"/>
  <rect x="20" y="20" width="760" height="440" rx="12" fill="none" stroke="rgba(255,255,255,0.06)" stroke-dasharray="6,6"/>

  <!-- Title -->
  <text x="400" y="55" font-family="'Outfit', sans-serif" font-weight="800" font-size="22" fill="#f8fafc" text-anchor="middle" letter-spacing="1">
    HỆ THỐNG LƯU TRỮ ĐA PHƯƠNG TIỆN MINIO S3
  </text>
  <text x="400" y="80" font-family="'Inter', sans-serif" font-weight="500" font-size="13" fill="#94a3b8" text-anchor="middle">
    Polyglot Media Architecture: Client ──> Chat Service ──> MinIO S3 Bucket (chat-media)
  </text>

  <!-- Box 1: Web Client SPA -->
  <g transform="translate(60, 140)">
    <rect width="180" height="180" rx="16" fill="#111827" stroke="url(#gradCyan)" stroke-width="2" filter="url(#glow)"/>
    <circle cx="90" cy="50" r="28" fill="rgba(0,242,254,0.15)"/>
    <text x="90" y="56" font-size="24" text-anchor="middle">💻</text>
    <text x="90" y="105" font-family="'Outfit', sans-serif" font-weight="700" font-size="16" fill="#f8fafc" text-anchor="middle">Web Client</text>
    <text x="90" y="128" font-family="'Inter', sans-serif" font-size="11" fill="#94a3b8" text-anchor="middle">Chunked Upload</text>
    <text x="90" y="146" font-family="'Inter', sans-serif" font-size="11" fill="#38bdf8" text-anchor="middle">Multipart Form-Data</text>
    <rect x="35" y="155" width="110" height="18" rx="9" fill="rgba(6,182,212,0.2)"/>
    <text x="90" y="167" font-family="'JetBrains Mono', monospace" font-size="10" fill="#22d3ee" text-anchor="middle">JWT Bearer Auth</text>
  </g>

  <!-- Arrow 1: Client -> Chat Service -->
  <g transform="translate(250, 215)">
    <line x1="0" y1="15" x2="70" y2="15" stroke="#38bdf8" stroke-width="3" stroke-dasharray="4,4"/>
    <polygon points="70,9 85,15 70,21" fill="#38bdf8"/>
    <text x="40" y="5" font-family="'JetBrains Mono', monospace" font-size="10" fill="#38bdf8" text-anchor="middle">POST /files</text>
  </g>

  <!-- Box 2: Chat Service Microservice -->
  <g transform="translate(345, 140)">
    <rect width="190" height="180" rx="16" fill="#111827" stroke="url(#gradPurple)" stroke-width="2" filter="url(#glow)"/>
    <circle cx="95" cy="50" r="28" fill="rgba(168,85,247,0.15)"/>
    <text x="95" y="56" font-size="24" text-anchor="middle">⚙️</text>
    <text x="95" y="105" font-family="'Outfit', sans-serif" font-weight="700" font-size="16" fill="#f8fafc" text-anchor="middle">Chat Service</text>
    <text x="95" y="128" font-family="'Inter', sans-serif" font-size="11" fill="#94a3b8" text-anchor="middle">Port 8082 • Spring Boot</text>
    <text x="95" y="146" font-family="'Inter', sans-serif" font-size="11" fill="#c084fc" text-anchor="middle">MinIO Java SDK v8.5</text>
    <rect x="40" y="155" width="110" height="18" rx="9" fill="rgba(168,85,247,0.2)"/>
    <text x="95" y="167" font-family="'JetBrains Mono', monospace" font-size="10" fill="#d8b4fe" text-anchor="middle">Auto-Thumbnail</text>
  </g>

  <!-- Arrow 2: Chat Service -> MinIO S3 -->
  <g transform="translate(545, 215)">
    <line x1="0" y1="15" x2="70" y2="15" stroke="#f43f5e" stroke-width="3" stroke-dasharray="4,4"/>
    <polygon points="70,9 85,15 70,21" fill="#f43f5e"/>
    <text x="40" y="5" font-family="'JetBrains Mono', monospace" font-size="10" fill="#f43f5e" text-anchor="middle">S3 PutObject</text>
  </g>

  <!-- Box 3: MinIO S3 Cluster -->
  <g transform="translate(640, 140)">
    <rect width="180" height="180" rx="16" fill="#111827" stroke="url(#gradRed)" stroke-width="2" filter="url(#glow)"/>
    <circle cx="90" cy="50" r="28" fill="rgba(244,63,94,0.15)"/>
    <text x="90" y="56" font-size="24" text-anchor="middle">🗄️</text>
    <text x="90" y="105" font-family="'Outfit', sans-serif" font-weight="700" font-size="16" fill="#f8fafc" text-anchor="middle">MinIO S3</text>
    <text x="90" y="128" font-family="'Inter', sans-serif" font-size="11" fill="#94a3b8" text-anchor="middle">Port 9000/9001</text>
    <text x="90" y="146" font-family="'Inter', sans-serif" font-size="11" fill="#fb7185" text-anchor="middle">Bucket: chat-media</text>
    <rect x="35" y="155" width="110" height="18" rx="9" fill="rgba(244,63,94,0.2)"/>
    <text x="90" y="167" font-family="'JetBrains Mono', monospace" font-size="10" fill="#fda4af" text-anchor="middle">High Availability</text>
  </g>

  <!-- Bottom Metric Banner -->
  <g transform="translate(80, 360)">
    <rect width="640" height="70" rx="12" fill="rgba(255,255,255,0.03)" stroke="#334155" stroke-width="1"/>
    
    <text x="100" y="32" font-family="'Outfit', sans-serif" font-weight="800" font-size="18" fill="#22c55e" text-anchor="middle">0.45 ms</text>
    <text x="100" y="52" font-family="'Inter', sans-serif" font-size="11" fill="#94a3b8" text-anchor="middle">Storage Latency</text>

    <line x1="210" y1="15" x2="210" y2="55" stroke="#334155" stroke-width="1"/>

    <text x="320" y="32" font-family="'Outfit', sans-serif" font-weight="800" font-size="18" fill="#38bdf8" text-anchor="middle">25 MB/s</text>
    <text x="320" y="52" font-family="'Inter', sans-serif" font-size="11" fill="#94a3b8" text-anchor="middle">Upload Bandwidth</text>

    <line x1="430" y1="15" x2="430" y2="55" stroke="#334155" stroke-width="1"/>

    <text x="530" y="32" font-family="'Outfit', sans-serif" font-weight="800" font-size="18" fill="#f59e0b" text-anchor="middle">S3 Compatible</text>
    <text x="530" y="52" font-family="'Inter', sans-serif" font-size="11" fill="#94a3b8" text-anchor="middle">AWS S3 SDK Standard</text>
  </g>
</svg>`;

fs.writeFileSync(path.join(imgDir, 'minio-arch.svg'), minioSvg);

// 2. Meme SVGs
function createMemeSvg(title, subtitle, emoji, bgColor) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" width="400" height="300">
    <defs>
      <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="${bgColor[0]}"/>
        <stop offset="100%" stop-color="${bgColor[1]}"/>
      </linearGradient>
    </defs>
    <rect width="400" height="300" rx="14" fill="url(#bg)"/>
    <rect x="10" y="10" width="380" height="280" rx="10" fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="2"/>
    <text x="200" y="45" font-family="'Outfit', Impact, sans-serif" font-weight="900" font-size="22" fill="#ffffff" text-anchor="middle" letter-spacing="1.5">
      ${title}
    </text>
    <circle cx="200" cy="145" r="58" fill="rgba(255,255,255,0.12)"/>
    <text x="200" y="165" font-size="70" text-anchor="middle">${emoji}</text>
    <text x="200" y="260" font-family="'Outfit', Impact, sans-serif" font-weight="900" font-size="19" fill="#fef08a" text-anchor="middle" letter-spacing="1">
      ${subtitle}
    </text>
  </svg>`;
}

fs.writeFileSync(path.join(imgDir, 'meme-happy-doge.svg'), createMemeSvg('MUCH HAPPINESS', 'VERY WEBSOCKET 0MS!', '🐕', ['#f59e0b', '#d97706']));
fs.writeFileSync(path.join(imgDir, 'meme-happy-success.svg'), createMemeSvg('SUCCESS KID', 'CLUSTER DEPLOYED CLEAN!', '🎉', ['#06b6d4', '#0284c7']));
fs.writeFileSync(path.join(imgDir, 'meme-happy-smile.svg'), createMemeSvg('FEELING GREAT', 'SPRING BOOT 3.3.5 PASS!', '😄', ['#10b981', '#047857']));
fs.writeFileSync(path.join(imgDir, 'meme-sad-pepe.svg'), createMemeSvg('SAD PEPE', 'WHEN WEBSOCKET DISCONNECTS', '😢', ['#3b82f6', '#1d4ed8']));
fs.writeFileSync(path.join(imgDir, 'meme-surprised-pikachu.svg'), createMemeSvg('SURPRISED PIKACHU', 'CASSANDRA LATENCY &lt; 2MS?!', '⚡', ['#eab308', '#ca8a04']));
fs.writeFileSync(path.join(imgDir, 'meme-neutral-poker.svg'), createMemeSvg('POKER FACE', 'WAITING FOR HEALTH CHECKS', '😐', ['#64748b', '#334155']));

console.log('Successfully generated all mock image and meme assets!');
