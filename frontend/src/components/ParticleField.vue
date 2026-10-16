<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

type Particle = {
  x: number
  y: number
  vx: number
  vy: number
  radius: number
  tone: 'blue' | 'cyan' | 'slate'
}

const canvasRef = ref<HTMLCanvasElement | null>(null)

let context: CanvasRenderingContext2D | null = null
let particles: Particle[] = []
let animationFrame = 0
let resizeObserver: ResizeObserver | null = null
let reducedMotionQuery: MediaQueryList | null = null
let width = 0
let height = 0
let reducedMotion = false

const particleColors = {
  blue: 'rgba(37, 99, 235, 0.42)',
  cyan: 'rgba(8, 145, 178, 0.34)',
  slate: 'rgba(71, 85, 105, 0.24)',
}

const connectionColor = 'rgba(37, 99, 235, 0.09)'

const createParticles = () => {
  const mobile = width < 640
  const count = Math.min(mobile ? 18 : 42, Math.max(12, Math.round(width / 38)))
  const tones: Particle['tone'][] = ['blue', 'cyan', 'slate']

  particles = Array.from({ length: count }, (_, index) => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * (mobile ? 0.12 : 0.2),
    vy: (Math.random() - 0.5) * (mobile ? 0.1 : 0.16),
    radius: 0.8 + Math.random() * 1.25,
    tone: tones[index % tones.length]!,
  }))
}

const draw = () => {
  if (!context) return

  context.clearRect(0, 0, width, height)

  const connectionDistance = width < 640 ? 86 : 118
  for (let i = 0; i < particles.length; i += 1) {
    const particle = particles[i]!

    for (let j = i + 1; j < particles.length; j += 1) {
      const target = particles[j]!
      const dx = particle.x - target.x
      const dy = particle.y - target.y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < connectionDistance) {
        context.beginPath()
        context.strokeStyle = connectionColor
        context.globalAlpha = 1 - distance / connectionDistance
        context.moveTo(particle.x, particle.y)
        context.lineTo(target.x, target.y)
        context.stroke()
      }
    }

    context.globalAlpha = 1
    context.beginPath()
    context.fillStyle = particleColors[particle.tone]
    context.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2)
    context.fill()
  }
}

const update = () => {
  for (const particle of particles) {
    particle.x += particle.vx
    particle.y += particle.vy

    if (particle.x < -4) particle.x = width + 4
    if (particle.x > width + 4) particle.x = -4
    if (particle.y < -4) particle.y = height + 4
    if (particle.y > height + 4) particle.y = -4
  }
}

const animate = () => {
  if (document.hidden || reducedMotion) return
  update()
  draw()
  animationFrame = window.requestAnimationFrame(animate)
}

const startAnimation = () => {
  window.cancelAnimationFrame(animationFrame)
  draw()
  if (!document.hidden && !reducedMotion) {
    animationFrame = window.requestAnimationFrame(animate)
  }
}

const resize = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const rect = canvas.getBoundingClientRect()
  width = rect.width
  height = rect.height

  const pixelRatio = Math.min(window.devicePixelRatio || 1, 1.5)
  canvas.width = Math.round(width * pixelRatio)
  canvas.height = Math.round(height * pixelRatio)
  context = canvas.getContext('2d')
  context?.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)

  createParticles()
  startAnimation()
}

const handleVisibilityChange = () => {
  startAnimation()
}

const handleMotionChange = (event: MediaQueryListEvent) => {
  reducedMotion = event.matches
  startAnimation()
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return

  reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  reducedMotion = reducedMotionQuery.matches
  reducedMotionQuery.addEventListener('change', handleMotionChange)
  document.addEventListener('visibilitychange', handleVisibilityChange)

  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(canvas)
  resize()
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(animationFrame)
  resizeObserver?.disconnect()
  reducedMotionQuery?.removeEventListener('change', handleMotionChange)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <canvas ref="canvasRef" class="particle-field" aria-hidden="true" />
</template>

<style scoped>
.particle-field {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
