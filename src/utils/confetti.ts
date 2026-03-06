// Lightweight confetti burst — no external dependencies
// Fires 8 colored particles from a given origin point.

const COLORS = ['#f59e0b', '#0d9488', '#6366f1', '#ec4899', '#10b981', '#3b82f6', '#f97316', '#a78bfa'];

let stylesInjected = false;

function injectStyles() {
  if (stylesInjected) return;
  stylesInjected = true;
  const style = document.createElement('style');
  style.textContent = `
    @keyframes gala-confetti-burst {
      0%   { transform: translate(0, 0) rotate(0deg) scale(1);   opacity: 1; }
      70%  { opacity: 1; }
      100% { transform: translate(var(--cx), var(--cy)) rotate(var(--cr)) scale(0.3); opacity: 0; }
    }
    .gala-confetti {
      position: fixed;
      pointer-events: none;
      z-index: 99999;
      border-radius: 2px;
      animation: gala-confetti-burst var(--cd) cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
    }
  `;
  document.head.appendChild(style);
}

export function fireConfetti(originX?: number, originY?: number): void {
  injectStyles();
  const cx = originX ?? window.innerWidth / 2;
  const cy = originY ?? window.innerHeight * 0.55; // slightly below center
  const count = 8;

  for (let i = 0; i < count; i++) {
    const el = document.createElement('div');
    el.className = 'gala-confetti';

    const angle = (i / count) * 2 * Math.PI - Math.PI / 2; // start at top
    const spread = 70 + Math.random() * 60;
    const dx = Math.cos(angle) * spread;
    const dy = Math.sin(angle) * spread - 40; // bias upward
    const size = 7 + Math.random() * 5;
    const duration = 800 + Math.random() * 400;
    const rotation = (Math.random() > 0.5 ? 1 : -1) * (180 + Math.random() * 360);

    el.style.cssText = `
      left: ${cx}px;
      top: ${cy}px;
      width: ${size}px;
      height: ${size}px;
      background: ${COLORS[i % COLORS.length]};
      --cx: ${dx}px;
      --cy: ${dy}px;
      --cr: ${rotation}deg;
      --cd: ${duration}ms;
      animation-delay: ${i * 25}ms;
    `;

    document.body.appendChild(el);
    el.addEventListener('animationend', () => el.remove(), { once: true });
  }
}
