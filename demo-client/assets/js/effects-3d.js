/**
 * ChatApp 3D Interactive & Audio FX Engine v2.0
 * ─────────────────────────────────────────────────────────────────────────────
 * • Three.js WebGL Interactive Cybernetic Grid & Cosmic Particle Horizon
 * • 3D Holographic Core Orb with Multi-tier Gyroscopes & Plasma Nucleus
 * • 3D Microservice Cluster Topology Graph (Admin Real-Time Visualizer)
 * • 3D Interactive Floating Chat Orb (User Empty State Companion)
 * • Physics-based 3D Multi-Layer Tilt Engine with Z-Depth Parallax & Specular Glare
 * • Web Audio API Sci-Fi Synthesizer (Zero external audio files required)
 * • 3D Tumbling Emoji & Confetti Particle Physics
 * • Dynamic Audio Equalizer Waveform Visualizer
 * • Smooth Cubic Number Counter Animator
 * ─────────────────────────────────────────────────────────────────────────────
 */

(function () {
  'use strict';

  // ═══════════════════════════════════════════════════════════════════════════
  // 1. SOUND FX SYNTHESIS ENGINE (Web Audio API)
  // ═══════════════════════════════════════════════════════════════════════════
  class SoundFX {
    constructor() {
      this.ctx = null;
      this.enabled = localStorage.getItem('chat_sound_enabled') !== 'false';
    }

    init() {
      if (!this.ctx && (window.AudioContext || window.webkitAudioContext)) {
        const AudioCtx = window.AudioContext || window.webkitAudioContext;
        this.ctx = new AudioCtx();
      }
      if (this.ctx && this.ctx.state === 'suspended') {
        this.ctx.resume();
      }
    }

    toggle() {
      this.enabled = !this.enabled;
      localStorage.setItem('chat_sound_enabled', this.enabled);
      if (this.enabled) this.playPop();
      return this.enabled;
    }

    // Gentle tactile click
    playTick() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;
      try {
        const t = this.ctx.currentTime;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(800, t);
        osc.frequency.exponentialRampToValueAtTime(1400, t + 0.035);
        gain.gain.setValueAtTime(0.04, t);
        gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.035);
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        osc.start(t);
        osc.stop(t + 0.035);
      } catch (e) {}
    }

    // Pop feedback for emojis / buttons
    playPop() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;
      try {
        const t = this.ctx.currentTime;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(320, t);
        osc.frequency.exponentialRampToValueAtTime(750, t + 0.07);
        gain.gain.setValueAtTime(0.09, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.07);
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        osc.start(t);
        osc.stop(t + 0.07);
      } catch (e) {}
    }

    // Laser whoosh for sending message
    playSend() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;
      try {
        const t = this.ctx.currentTime;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(520, t);
        osc.frequency.exponentialRampToValueAtTime(1040, t + 0.12);
        gain.gain.setValueAtTime(0.12, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.14);
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        osc.start(t);
        osc.stop(t + 0.14);
      } catch (e) {}
    }

    // Crystal double-bell chime for incoming messages
    playReceive() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;
      try {
        const t = this.ctx.currentTime;
        const osc1 = this.ctx.createOscillator();
        const osc2 = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc1.type = 'sine';
        osc1.frequency.setValueAtTime(659.25, t); // E5
        osc1.frequency.exponentialRampToValueAtTime(987.77, t + 0.12); // B5

        osc2.type = 'sine';
        osc2.frequency.setValueAtTime(987.77, t + 0.1);
        osc2.frequency.exponentialRampToValueAtTime(1318.51, t + 0.28); // E6

        gain.gain.setValueAtTime(0.12, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.32);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(this.ctx.destination);

        osc1.start(t);
        osc1.stop(t + 0.15);
        osc2.start(t + 0.1);
        osc2.stop(t + 0.32);
      } catch (e) {}
    }

    // Harmonic celebration arpeggio
    playSuccess() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;
      try {
        const t = this.ctx.currentTime;
        const freqs = [523.25, 659.25, 783.99, 1046.50, 1318.51];
        freqs.forEach((freq, i) => {
          const osc = this.ctx.createOscillator();
          const gain = this.ctx.createGain();
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(freq, t + i * 0.055);
          gain.gain.setValueAtTime(0.08, t + i * 0.055);
          gain.gain.exponentialRampToValueAtTime(0.001, t + i * 0.055 + 0.22);
          osc.connect(gain);
          gain.connect(this.ctx.destination);
          osc.start(t + i * 0.055);
          osc.stop(t + i * 0.055 + 0.22);
        });
      } catch (e) {}
    }

    // Sci-Fi broadcast chime alert
    playBroadcast() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;
      try {
        const t = this.ctx.currentTime;
        [440, 554.37, 659.25, 880].forEach((freq, idx) => {
          const osc = this.ctx.createOscillator();
          const gain = this.ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, t + idx * 0.08);
          gain.gain.setValueAtTime(0.12, t + idx * 0.08);
          gain.gain.exponentialRampToValueAtTime(0.001, t + idx * 0.08 + 0.35);
          osc.connect(gain);
          gain.connect(this.ctx.destination);
          osc.start(t + idx * 0.08);
          osc.stop(t + idx * 0.08 + 0.35);
        });
      } catch (e) {}
    }

    // Warning tone
    playAlert() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;
      try {
        const t = this.ctx.currentTime;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(480, t);
        osc.frequency.setValueAtTime(360, t + 0.09);
        gain.gain.setValueAtTime(0.1, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.22);
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        osc.start(t);
        osc.stop(t + 0.22);
      } catch (e) {}
    }
  }

  window.soundFX = new SoundFX();

  // ═══════════════════════════════════════════════════════════════════════════
  // 2. 3D TILT ENGINE WITH MULTI-LAYER Z-DEPTH PARALLAX & SPECULAR GLARE
  // ═══════════════════════════════════════════════════════════════════════════
  class TiltEngine {
    constructor() {
      this.cards = new Set();
      this.init();
    }

    init() {
      this.scanAndAttach();
      const observer = new MutationObserver(() => this.scanAndAttach());
      observer.observe(document.body, { childList: true, subtree: true });
    }

    scanAndAttach() {
      // ONLY attach 3D tilt to small cards: .stat-card, .meme-card, .podium-card, .auth-card
      // NEVER attach to tables, forms, or charts to prevent jitter and maintain usability
      const elements = document.querySelectorAll(
        '.stat-card, .meme-card, .podium-card, .auth-card'
      );
      elements.forEach(el => {
        if (
          el.closest('.table-card') ||
          el.closest('.chart-card') ||
          el.closest('.sec-card') ||
          el.classList.contains('table-card') ||
          el.classList.contains('sec-card') ||
          el.classList.contains('chart-card')
        ) {
          return;
        }
        if (!this.cards.has(el)) {
          this.attach(el);
          this.cards.add(el);
        }
      });
    }

    attach(el) {
      el.style.transformStyle = 'preserve-3d';
      el.style.transition = 'transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s, border-color 0.3s';

      // Setup internal parallax layers
      const depthElements = el.querySelectorAll('.stat-icon, .stat-value, .podium-badge, [data-depth]');
      depthElements.forEach(item => {
        item.style.transformStyle = 'preserve-3d';
      });

      // Glare overlay
      let glare = el.querySelector('.tilt-glare');
      if (!glare) {
        glare = document.createElement('div');
        glare.className = 'tilt-glare';
        glare.style.cssText = `
          position: absolute; inset: 0; pointer-events: none; border-radius: inherit;
          background: radial-gradient(circle at 50% 50%, rgba(255,255,255,0.18), transparent 60%);
          opacity: 0; transition: opacity 0.3s ease; z-index: 20;
        `;
        el.appendChild(glare);
      }

      const maxTilt = 8; // gentle, subtle tilt
      let cachedRect = null;
      let rafId = null;

      const onMouseEnter = () => {
        // Cache rect BEFORE transform is applied so getBoundingClientRect never fluctuates during tilt
        el.style.transition = 'transform 0.08s ease-out';
        cachedRect = el.getBoundingClientRect();
      };

      const onMouseMove = (e) => {
        if (!cachedRect) cachedRect = el.getBoundingClientRect();
        const x = e.clientX - cachedRect.left;
        const y = e.clientY - cachedRect.top;
        const centerX = cachedRect.width / 2;
        const centerY = cachedRect.height / 2;

        const targetX = ((y - centerY) / centerY) * -maxTilt;
        const targetY = ((x - centerX) / centerX) * maxTilt;

        if (rafId) cancelAnimationFrame(rafId);
        rafId = requestAnimationFrame(() => {
          el.style.transform = `perspective(800px) rotateX(${targetX.toFixed(2)}deg) rotateY(${targetY.toFixed(2)}deg) translateY(-3px)`;

          // Parallax depth for internal floating items
          depthElements.forEach(item => {
            const depth = parseFloat(item.getAttribute('data-depth')) || 16;
            item.style.transform = `translateZ(${depth}px)`;
          });

          if (glare) {
            glare.style.opacity = '1';
            glare.style.background = `radial-gradient(circle at ${x}px ${y}px, rgba(255,255,255,0.18), transparent 55%)`;
          }
        });
      };

      const onMouseLeave = () => {
        if (rafId) cancelAnimationFrame(rafId);
        cachedRect = null;
        el.style.transition = 'transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s';
        el.style.transform = 'perspective(800px) rotateX(0deg) rotateY(0deg) translateY(0)';
        depthElements.forEach(item => {
          item.style.transform = 'translateZ(0px)';
        });
        if (glare) glare.style.opacity = '0';
      };

      el.addEventListener('mouseenter', onMouseEnter);
      el.addEventListener('mousemove', onMouseMove);
      el.addEventListener('mouseleave', onMouseLeave);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 3. THREE.JS / WEBGL CYBER HORIZON & STARFIELD BACKGROUND
  // ═══════════════════════════════════════════════════════════════════════════
  class Background3D {
    constructor(canvasId = 'bgCanvas3D') {
      this.canvasId = canvasId;
      this.init();
    }

    init() {
      let canvas = document.getElementById(this.canvasId);
      if (!canvas) {
        canvas = document.createElement('canvas');
        canvas.id = this.canvasId;
        canvas.style.cssText = `
          position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
          pointer-events: none; z-index: 0; opacity: 0.72;
        `;
        document.body.prepend(canvas);
      }

      this.canvas = canvas;

      if (window.THREE) {
        try {
          this.initThreeJS();
        } catch (e) {
          console.warn('Three.js background WebGL failed, falling back to 2D canvas:', e);
          this.initCanvasFallback();
        }
      } else {
        this.initCanvasFallback();
      }
    }

    initThreeJS() {
      const THREE = window.THREE;
      const width = window.innerWidth;
      const height = window.innerHeight;

      const scene = new THREE.Scene();
      const camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1200);
      camera.position.set(0, 30, 240);

      const renderer = new THREE.WebGLRenderer({ canvas: this.canvas, alpha: true, antialias: true });
      renderer.setSize(width, height);
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

      // ── Particle Constellation ──
      const particleCount = 220;
      const geometry = new THREE.BufferGeometry();
      const positions = new Float32Array(particleCount * 3);
      const velocities = [];

      for (let i = 0; i < particleCount * 3; i += 3) {
        positions[i] = (Math.random() - 0.5) * 550;
        positions[i + 1] = (Math.random() - 0.5) * 380;
        positions[i + 2] = (Math.random() - 0.5) * 260;
        velocities.push({
          x: (Math.random() - 0.5) * 0.2,
          y: (Math.random() - 0.5) * 0.2,
          z: (Math.random() - 0.5) * 0.2,
        });
      }

      geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));

      const particleMat = new THREE.PointsMaterial({
        color: 0x06b6d4,
        size: 3.4,
        transparent: true,
        opacity: 0.85,
        blending: THREE.AdditiveBlending,
      });

      const particleSystem = new THREE.Points(geometry, particleMat);
      scene.add(particleSystem);

      // ── Cyber Terrain Wave Mesh ──
      const planeGeo = new THREE.PlaneGeometry(600, 400, 32, 24);
      planeGeo.rotateX(-Math.PI / 2.3);
      planeGeo.translate(0, -90, 0);

      const planeMat = new THREE.MeshBasicMaterial({
        color: 0x3b82f6,
        wireframe: true,
        transparent: true,
        opacity: 0.14,
      });
      const terrain = new THREE.Mesh(planeGeo, planeMat);
      scene.add(terrain);

      // ── Floating Geometric Polyhedra ──
      const polyMat1 = new THREE.MeshBasicMaterial({ color: 0x8b5cf6, wireframe: true, transparent: true, opacity: 0.22 });
      const polyMat2 = new THREE.MeshBasicMaterial({ color: 0x06b6d4, wireframe: true, transparent: true, opacity: 0.2 });

      const icoMesh1 = new THREE.Mesh(new THREE.IcosahedronGeometry(60, 1), polyMat1);
      icoMesh1.position.set(160, -20, -50);
      scene.add(icoMesh1);

      const icoMesh2 = new THREE.Mesh(new THREE.OctahedronGeometry(45, 1), polyMat2);
      icoMesh2.position.set(-180, 50, -80);
      scene.add(icoMesh2);

      // Mouse Parallax & Gravity
      let mouseX = 0, mouseY = 0;
      let targetX = 0, targetY = 0;
      window.addEventListener('mousemove', (e) => {
        mouseX = (e.clientX - width / 2) * 0.045;
        mouseY = (e.clientY - height / 2) * 0.045;
      });

      let clock = 0;

      const animate = () => {
        requestAnimationFrame(animate);
        clock += 0.015;

        targetX += (mouseX - targetX) * 0.05;
        targetY += (mouseY - targetY) * 0.05;

        particleSystem.rotation.y += 0.0008;
        particleSystem.rotation.x += 0.0004;
        particleSystem.position.x = targetX * 0.6;
        particleSystem.position.y = -targetY * 0.6;

        icoMesh1.rotation.x += 0.003;
        icoMesh1.rotation.y += 0.004;
        icoMesh2.rotation.y -= 0.0035;
        icoMesh2.rotation.z += 0.0025;

        // Dynamic wave movement on terrain plane
        const posArray = planeGeo.attributes.position.array;
        for (let i = 0; i < posArray.length; i += 3) {
          const u = posArray[i];
          const v = posArray[i + 1];
          posArray[i + 2] = Math.sin(u * 0.02 + clock) * 12 + Math.cos(v * 0.02 + clock * 0.8) * 10;
        }
        planeGeo.attributes.position.needsUpdate = true;

        // Particle drifting
        const pos = geometry.attributes.position.array;
        for (let i = 0; i < particleCount; i++) {
          const idx = i * 3;
          pos[idx] += velocities[i].x;
          pos[idx + 1] += velocities[i].y;
          pos[idx + 2] += velocities[i].z;

          if (Math.abs(pos[idx]) > 275) velocities[i].x *= -1;
          if (Math.abs(pos[idx + 1]) > 190) velocities[i].y *= -1;
          if (Math.abs(pos[idx + 2]) > 130) velocities[i].z *= -1;
        }
        geometry.attributes.position.needsUpdate = true;

        renderer.render(scene, camera);
      };

      animate();

      window.addEventListener('resize', () => {
        const w = window.innerWidth;
        const h = window.innerHeight;
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
        renderer.setSize(w, h);
      });
    }

    initCanvasFallback() {
      const ctx = this.canvas.getContext('2d');
      let w = (this.canvas.width = window.innerWidth);
      let h = (this.canvas.height = window.innerHeight);

      const particles = Array.from({ length: 90 }, () => ({
        x: Math.random() * w,
        y: Math.random() * h,
        z: Math.random() * 2 + 0.5,
        vx: (Math.random() - 0.5) * 0.5,
        vy: (Math.random() - 0.5) * 0.5,
        radius: Math.random() * 2 + 1,
        color: Math.random() > 0.5 ? 'rgba(6, 182, 212, ' : 'rgba(139, 92, 246, ',
      }));

      let mouseX = w / 2, mouseY = h / 2;
      window.addEventListener('mousemove', (e) => {
        mouseX = e.clientX;
        mouseY = e.clientY;
      });

      const draw = () => {
        ctx.clearRect(0, 0, w, h);

        for (let i = 0; i < particles.length; i++) {
          const p = particles[i];
          p.x += p.vx;
          p.y += p.vy;

          if (p.x < 0) p.x = w;
          if (p.x > w) p.x = 0;
          if (p.y < 0) p.y = h;
          if (p.y > h) p.y = 0;

          ctx.beginPath();
          ctx.arc(p.x, p.y, p.radius * p.z, 0, Math.PI * 2);
          ctx.fillStyle = p.color + (0.3 + p.z * 0.2) + ')';
          ctx.fill();

          for (let j = i + 1; j < particles.length; j++) {
            const p2 = particles[j];
            const dist = Math.hypot(p.x - p2.x, p.y - p2.y);
            if (dist < 110) {
              ctx.beginPath();
              ctx.moveTo(p.x, p.y);
              ctx.lineTo(p2.x, p2.y);
              ctx.strokeStyle = `rgba(6, 182, 212, ${0.15 * (1 - dist / 110)})`;
              ctx.lineWidth = 0.7;
              ctx.stroke();
            }
          }
        }
        requestAnimationFrame(draw);
      };

      draw();
      window.addEventListener('resize', () => {
        w = this.canvas.width = window.innerWidth;
        h = this.canvas.height = window.innerHeight;
      });
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 4. 3D HOLOGRAPHIC CORE ORB (Auth Showcase & Hero Section)
  // ═══════════════════════════════════════════════════════════════════════════
  class HolographicOrb3D {
    constructor(containerId) {
      this.container = document.getElementById(containerId);
      if (!this.container) return;
      this.init();
    }

    init() {
      if (!window.THREE) return;
      try {
        const THREE = window.THREE;
        const width = this.container.clientWidth || 320;
        const height = this.container.clientHeight || 320;

        const scene = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
        camera.position.z = 8.5;

        const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
        renderer.setSize(width, height);
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this.container.innerHTML = '';
        this.container.appendChild(renderer.domElement);

        // Core Glowing Wireframe Sphere
        const sphereGeo = new THREE.SphereGeometry(2.1, 26, 26);
        const sphereMat = new THREE.MeshBasicMaterial({
          color: 0x06b6d4,
          wireframe: true,
          transparent: true,
          opacity: 0.75,
        });
        const sphere = new THREE.Mesh(sphereGeo, sphereMat);
        scene.add(sphere);

        // Inner Pulsating Plasma Crystal
        const innerGeo = new THREE.IcosahedronGeometry(1.25, 1);
        const innerMat = new THREE.MeshBasicMaterial({
          color: 0x8b5cf6,
          wireframe: true,
          transparent: true,
          opacity: 0.95,
        });
        const innerCore = new THREE.Mesh(innerGeo, innerMat);
        scene.add(innerCore);

        // Outer 3D Gyroscope Rings
        const ringGeo = new THREE.TorusGeometry(3.1, 0.045, 16, 120);
        const ring1 = new THREE.Mesh(ringGeo, new THREE.MeshBasicMaterial({ color: 0x38bdf8, transparent: true, opacity: 0.85 }));
        const ring2 = new THREE.Mesh(ringGeo, new THREE.MeshBasicMaterial({ color: 0xa855f7, transparent: true, opacity: 0.75 }));
        const ring3 = new THREE.Mesh(new THREE.TorusGeometry(3.6, 0.03, 16, 120), new THREE.MeshBasicMaterial({ color: 0x06b6d4, transparent: true, opacity: 0.5 }));
        
        ring1.rotation.x = Math.PI / 3;
        ring2.rotation.y = Math.PI / 4;
        ring3.rotation.z = Math.PI / 6;

        scene.add(ring1);
        scene.add(ring2);
        scene.add(ring3);

        // Particle halo
        const haloGeo = new THREE.BufferGeometry();
        const haloCount = 60;
        const haloPos = new Float32Array(haloCount * 3);
        for (let i = 0; i < haloCount * 3; i += 3) {
          const theta = Math.random() * Math.PI * 2;
          const rad = 2.4 + Math.random() * 0.8;
          haloPos[i] = Math.cos(theta) * rad;
          haloPos[i + 1] = (Math.random() - 0.5) * 1.5;
          haloPos[i + 2] = Math.sin(theta) * rad;
        }
        haloGeo.setAttribute('position', new THREE.BufferAttribute(haloPos, 3));
        const haloMat = new THREE.PointsMaterial({ color: 0x67e8f9, size: 0.12, transparent: true, opacity: 0.8 });
        const haloParticles = new THREE.Points(haloGeo, haloMat);
        scene.add(haloParticles);

        let mouseX = 0, mouseY = 0;
        window.addEventListener('mousemove', (e) => {
          const rect = this.container.getBoundingClientRect();
          mouseX = ((e.clientX - rect.left) / width - 0.5) * 2;
          mouseY = ((e.clientY - rect.top) / height - 0.5) * 2;
        });

        let t = 0;
        const animate = () => {
          requestAnimationFrame(animate);
          t += 0.015;

          sphere.rotation.y += 0.007;
          sphere.rotation.x += 0.003;

          innerCore.rotation.y -= 0.014;
          innerCore.rotation.z += 0.009;
          const scalePulse = 1.0 + Math.sin(t * 2) * 0.08;
          innerCore.scale.set(scalePulse, scalePulse, scalePulse);

          ring1.rotation.z += 0.009;
          ring2.rotation.x += 0.011;
          ring3.rotation.y -= 0.006;
          haloParticles.rotation.y += 0.005;

          sphere.rotation.x += (mouseY * 0.6 - sphere.rotation.x) * 0.05;
          sphere.rotation.y += (mouseX * 0.6 - sphere.rotation.y) * 0.05;

          renderer.render(scene, camera);
        };

        animate();
      } catch (err) {
        console.warn('HolographicOrb3D init error:', err);
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 5. 3D INTERACTIVE MICROSERVICE CLUSTER TOPOLOGY (Admin Visualizer)
  // ═══════════════════════════════════════════════════════════════════════════
  class ServiceCluster3D {
    constructor(containerId = 'serviceClusterCanvas') {
      this.container = document.getElementById(containerId);
      if (!this.container) return;
      this.init();
    }

    init() {
      if (!window.THREE) return;
      try {
        const THREE = window.THREE;
        const width = this.container.clientWidth || 800;
        const height = this.container.clientHeight || 240;

        const scene = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(50, width / height, 0.1, 1000);
        camera.position.set(0, 15, 65);

        const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
        renderer.setSize(width, height);
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this.container.innerHTML = '';
        this.container.appendChild(renderer.domElement);

        // Define microservice cluster nodes
        const nodesData = [
          { name: 'WS-Gateway', x: 0, y: 0, z: 12, color: 0x06b6d4, type: 'core', desc: 'Port 8083 • Netty WebSocket' },
          { name: 'Auth-Service', x: -28, y: 8, z: -5, color: 0x8b5cf6, type: 'svc', desc: 'Port 8081 • Spring Security JWT' },
          { name: 'Chat-Service', x: 28, y: 8, z: -5, color: 0x3b82f6, type: 'svc', desc: 'Port 8082 • Chat Messaging API' },
          { name: 'Redis Pub/Sub', x: 0, y: 16, z: -15, color: 0xef4444, type: 'db', desc: 'Port 6379 • In-Memory Fanout' },
          { name: 'Cassandra DB', x: 24, y: -14, z: -10, color: 0x10b981, type: 'db', desc: 'Port 9042 • NoSQL Message Store' },
          { name: 'PostgreSQL', x: -24, y: -14, z: -10, color: 0xf59e0b, type: 'db', desc: 'Port 5432 • Auth & Accounts DB' },
        ];

        const nodesGroup = new THREE.Group();
        const nodeMeshes = [];

        nodesData.forEach(d => {
          const geo = d.type === 'core' 
            ? new THREE.DodecahedronGeometry(4.2, 1) 
            : new THREE.IcosahedronGeometry(3.4, 0);

          const mat = new THREE.MeshBasicMaterial({
            color: d.color,
            wireframe: true,
            transparent: true,
            opacity: 0.9,
          });

          const mesh = new THREE.Mesh(geo, mat);
          mesh.position.set(d.x, d.y, d.z);
          mesh.userData = d;

          // Inner glowing core
          const coreGeo = new THREE.SphereGeometry(1.6, 12, 12);
          const coreMat = new THREE.MeshBasicMaterial({ color: d.color, transparent: true, opacity: 0.7 });
          const coreMesh = new THREE.Mesh(coreGeo, coreMat);
          mesh.add(coreMesh);

          // Outer ring
          const ringGeo = new THREE.TorusGeometry(4.8, 0.06, 12, 40);
          const ringMat = new THREE.MeshBasicMaterial({ color: d.color, transparent: true, opacity: 0.5 });
          const ringMesh = new THREE.Mesh(ringGeo, ringMat);
          ringMesh.rotation.x = Math.PI / 3;
          mesh.add(ringMesh);

          nodesGroup.add(mesh);
          nodeMeshes.push(mesh);
        });

        scene.add(nodesGroup);

        // Connect nodes with glowing lines & data energy packets
        const connections = [
          [0, 1], // Gateway -> Auth
          [0, 2], // Gateway -> Chat
          [0, 3], // Gateway -> Redis
          [1, 5], // Auth -> Postgres
          [2, 4], // Chat -> Cassandra
          [2, 3], // Chat -> Redis
        ];

        const linesMat = new THREE.LineBasicMaterial({ color: 0x38bdf8, transparent: true, opacity: 0.35 });
        connections.forEach(([fromIdx, toIdx]) => {
          const from = nodesData[fromIdx];
          const to = nodesData[toIdx];
          const pts = [new THREE.Vector3(from.x, from.y, from.z), new THREE.Vector3(to.x, to.y, to.z)];
          const geo = new THREE.BufferGeometry().setFromPoints(pts);
          const line = new THREE.Line(geo, linesMat);
          scene.add(line);
        });

        // Data pulse particles traveling along lines
        const packetCount = connections.length * 2;
        const packetGeo = new THREE.BufferGeometry();
        const packetPositions = new Float32Array(packetCount * 3);
        packetGeo.setAttribute('position', new THREE.BufferAttribute(packetPositions, 3));
        const packetMat = new THREE.PointsMaterial({ color: 0x00f2fe, size: 2.5, transparent: true, opacity: 0.95 });
        const packetSystem = new THREE.Points(packetGeo, packetMat);
        scene.add(packetSystem);

        let mouseX = 0, mouseY = 0;
        this.container.addEventListener('mousemove', (e) => {
          const rect = this.container.getBoundingClientRect();
          mouseX = ((e.clientX - rect.left) / width - 0.5) * 2;
          mouseY = ((e.clientY - rect.top) / height - 0.5) * 2;
        });

        let clock = 0;
        const animate = () => {
          requestAnimationFrame(animate);
          clock += 0.02;

          nodesGroup.rotation.y = mouseX * 0.25;
          nodesGroup.rotation.x = -mouseY * 0.25;

          nodeMeshes.forEach((mesh, idx) => {
            mesh.rotation.y += 0.01;
            mesh.rotation.x += 0.005;
            const scale = 1.0 + Math.sin(clock * 3 + idx) * 0.05;
            mesh.scale.set(scale, scale, scale);
          });

          // Update moving packet positions
          const pos = packetGeo.attributes.position.array;
          connections.forEach(([fIdx, tIdx], connIdx) => {
            const from = nodesData[fIdx];
            const to = nodesData[tIdx];
            const progress = (clock * 0.8 + connIdx * 0.3) % 1;
            const idx = connIdx * 3;
            pos[idx] = from.x + (to.x - from.x) * progress;
            pos[idx + 1] = from.y + (to.y - from.y) * progress;
            pos[idx + 2] = from.z + (to.z - from.z) * progress;
          });
          packetGeo.attributes.position.needsUpdate = true;

          renderer.render(scene, camera);
        };

        animate();

        window.addEventListener('resize', () => {
          if (!this.container) return;
          const w = this.container.clientWidth || 800;
          const h = this.container.clientHeight || 240;
          camera.aspect = w / h;
          camera.updateProjectionMatrix();
          renderer.setSize(w, h);
        });
      } catch (err) {
        console.warn('ServiceCluster3D init error:', err);
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 6. 3D INTERACTIVE CHAT COMPANION ORB (User Empty State)
  // ═══════════════════════════════════════════════════════════════════════════
  class EmptyChatCompanion3D {
    constructor(containerId = 'noChatCompanion') {
      this.container = document.getElementById(containerId);
      if (!this.container) return;
      this.init();
    }

    init() {
      if (!window.THREE) return;
      try {
        const THREE = window.THREE;
        const width = this.container.clientWidth || 180;
        const height = this.container.clientHeight || 180;

        const scene = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
        camera.position.z = 7;

        const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
        renderer.setSize(width, height);
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this.container.innerHTML = '';
        this.container.appendChild(renderer.domElement);

        // Floating Hologram Diamond
        const geo = new THREE.OctahedronGeometry(2.0, 1);
        const mat = new THREE.MeshBasicMaterial({ color: 0x06b6d4, wireframe: true, transparent: true, opacity: 0.85 });
        const mesh = new THREE.Mesh(geo, mat);
        scene.add(mesh);

        // Inner Core
        const innerGeo = new THREE.IcosahedronGeometry(1.0, 0);
        const innerMat = new THREE.MeshBasicMaterial({ color: 0x8b5cf6, transparent: true, opacity: 0.7 });
        const innerMesh = new THREE.Mesh(innerGeo, innerMat);
        scene.add(innerMesh);

        // Orbital Ring
        const ringGeo = new THREE.TorusGeometry(2.8, 0.04, 16, 60);
        const ring = new THREE.Mesh(ringGeo, new THREE.MeshBasicMaterial({ color: 0x38bdf8, transparent: true, opacity: 0.7 }));
        ring.rotation.x = Math.PI / 3;
        scene.add(ring);

        let mouseX = 0, mouseY = 0;
        window.addEventListener('mousemove', (e) => {
          const rect = this.container.getBoundingClientRect();
          mouseX = ((e.clientX - rect.left) / width - 0.5) * 2;
          mouseY = ((e.clientY - rect.top) / height - 0.5) * 2;
        });

        let clock = 0;
        const animate = () => {
          requestAnimationFrame(animate);
          clock += 0.02;

          mesh.rotation.y += 0.012;
          mesh.rotation.x += 0.008;
          mesh.position.y = Math.sin(clock * 1.5) * 0.25;

          innerMesh.rotation.y -= 0.018;
          innerMesh.position.y = mesh.position.y;

          ring.rotation.z += 0.015;
          ring.position.y = mesh.position.y;

          mesh.rotation.x += (mouseY * 0.5 - mesh.rotation.x) * 0.05;
          mesh.rotation.y += (mouseX * 0.5 - mesh.rotation.y) * 0.05;

          renderer.render(scene, camera);
        };

        animate();
      } catch (err) {
        console.warn('EmptyChatCompanion3D init error:', err);
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 7. 3D CONFETTI & EMOJI PARTICLE PHYSICS
  // ═══════════════════════════════════════════════════════════════════════════
  function triggerConfetti(origin = { x: 0.5, y: 0.5 }) {
    if (window.confetti) {
      window.confetti({
        particleCount: 65,
        spread: 80,
        origin: origin,
        colors: ['#06b6d4', '#8b5cf6', '#10b981', '#f59e0b', '#ec4899', '#3b82f6'],
        disableForReducedMotion: true,
      });
    }
  }

  function triggerReactionBurst(e, emoji) {
    const x = e.clientX || window.innerWidth / 2;
    const y = e.clientY || window.innerHeight / 2;

    for (let i = 0; i < 7; i++) {
      const el = document.createElement('div');
      el.className = 'floating-reaction-particle';
      el.textContent = emoji;
      el.style.cssText = `
        position: fixed; left: ${x}px; top: ${y}px; pointer-events: none;
        font-size: ${22 + Math.random() * 12}px; z-index: 10000;
        transition: transform 0.85s cubic-bezier(0.2, 0.8, 0.2, 1), opacity 0.85s ease;
        transform: translate(-50%, -50%) scale(0.6) rotate(0deg); opacity: 1;
        filter: drop-shadow(0 4px 12px rgba(0,0,0,0.4));
      `;
      document.body.appendChild(el);

      const angle = (Math.PI * 2 * i) / 7 + (Math.random() - 0.5) * 0.5;
      const distance = 45 + Math.random() * 60;
      const tx = Math.cos(angle) * distance;
      const ty = Math.sin(angle) * distance - 50;
      const rot = (Math.random() - 0.5) * 120;

      requestAnimationFrame(() => {
        el.style.transform = `translate(calc(-50% + ${tx}px), calc(-50% + ${ty}px)) scale(1.3) rotate(${rot}deg)`;
        el.style.opacity = '0';
      });

      setTimeout(() => el.remove(), 900);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 8. NUMBER COUNTER ANIMATOR
  // ═══════════════════════════════════════════════════════════════════════════
  function animateValue(elementOrId, start, end, duration = 850) {
    const obj = typeof elementOrId === 'string' ? document.getElementById(elementOrId) : elementOrId;
    if (!obj || isNaN(end)) return;
    let startTimestamp = null;
    const step = (timestamp) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / duration, 1);
      const ease = 1 - Math.pow(1 - progress, 3);
      obj.textContent = Math.floor(progress * (end - start) + start);
      if (progress < 1) {
        window.requestAnimationFrame(step);
      } else {
        obj.textContent = end;
      }
    };
    window.requestAnimationFrame(step);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 9. RIPPLE CLICK WITH COLOR GLOW
  // ═══════════════════════════════════════════════════════════════════════════
  function attachRipples() {
    document.addEventListener('click', (e) => {
      const btn = e.target.closest('.btn-primary, .btn-secondary, .btn-sidebar, .btn-send, .btn-new, .btn-action');
      if (!btn) return;

      const rect = btn.getBoundingClientRect();
      const circle = document.createElement('span');
      const diameter = Math.max(rect.width, rect.height);
      const radius = diameter / 2;

      circle.style.width = circle.style.height = `${diameter}px`;
      circle.style.left = `${e.clientX - rect.left - radius}px`;
      circle.style.top = `${e.clientY - rect.top - radius}px`;
      circle.className = 'ripple-effect';
      circle.style.cssText += `
        position: absolute; border-radius: 50%; pointer-events: none;
        transform: scale(0); animation: rippleAnim 0.6s linear;
        background: rgba(255, 255, 255, 0.35); z-index: 5;
      `;

      const prev = btn.querySelector('.ripple-effect');
      if (prev) prev.remove();

      btn.style.position = btn.style.position === 'static' ? 'relative' : btn.style.position;
      btn.style.overflow = 'hidden';
      btn.appendChild(circle);

      window.soundFX.playTick();
      setTimeout(() => circle.remove(), 600);
    });
  }

  // Export to global scope
  window.Effects3D = {
    SoundFX,
    TiltEngine,
    Background3D,
    HolographicOrb3D,
    ServiceCluster3D,
    EmptyChatCompanion3D,
    triggerConfetti,
    triggerReactionBurst,
    animateValue,
  };

  // Auto initialize when DOM is ready
  document.addEventListener('DOMContentLoaded', () => {
    new Background3D();
    new TiltEngine();
    attachRipples();
  });
})();
