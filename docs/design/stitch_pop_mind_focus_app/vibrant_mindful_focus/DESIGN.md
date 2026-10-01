---
name: Vibrant Mindful Focus
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#3d4947'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#6d7a77'
  outline-variant: '#bcc9c6'
  surface-tint: '#006a61'
  primary: '#00685f'
  on-primary: '#ffffff'
  primary-container: '#008378'
  on-primary-container: '#f4fffc'
  inverse-primary: '#6bd8cb'
  secondary: '#9d4300'
  on-secondary: '#ffffff'
  secondary-container: '#fd761a'
  on-secondary-container: '#5c2400'
  tertiary: '#00685d'
  on-tertiary: '#ffffff'
  tertiary-container: '#008376'
  on-tertiary-container: '#f4fffb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#89f5e7'
  primary-fixed-dim: '#6bd8cb'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#005049'
  secondary-fixed: '#ffdbca'
  secondary-fixed-dim: '#ffb690'
  on-secondary-fixed: '#341100'
  on-secondary-fixed-variant: '#783200'
  tertiary-fixed: '#71f8e4'
  tertiary-fixed-dim: '#4fdbc8'
  on-tertiary-fixed: '#00201c'
  on-tertiary-fixed-variant: '#005048'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Be Vietnam Pro
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Be Vietnam Pro
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.02em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system is tailored for Gen Z high school students (aged 15–18) reclaiming attention and agency from hyper-dopaminergic short-form video algorithms. The tone departs completely from punitive digital detox or clinical productivity software. Instead, it adopts an empathetic, lively, and gamified aesthetic—celebrating flow state, intentional breaks, and mindful progress.

The design movement synthesizes **Material 3 Expressive** with **Organic Tactility**:
- Soft, inviting surfaces using exaggerated radii (24px to 32px pill containers) that remove corporate rigidity.
- High-contrast visual cues with energetic, buoyant micro-interactions that trigger healthy dopamine release without cognitive exhaustion.
- Non-judgmental, conversational Vietnamese copywriting paired with breezy, breathable spatial hierarchy.

## Colors

The palette leverages neuro-calming teal paired with the kinetic optimism of warm coral-orange to counter the anxiety typically associated with productivity locks:

- **Primary Teal (`#0D9488`) & Light Mint Container (`#CCFBF1`)**: Anchors deep work, breathing sessions, and successful milestone screens. It evokes equilibrium, clarity, and mental spaciousness.
- **Secondary Warm Coral (`#F97316`) & Light Apricot (`#FFEDD5`)**: Delivers dynamic urgency for focus timers, streaks, urgent urge-surfing interventions, and active session tags.
- **Tertiary Mint Accent (`#14B8A6`)**: Handles secondary highlights, active chips, and progress bars.
- **Neutral Canvas (`#F8FAFC`) / Dark Mode Canvas (`#0F172A`)**: Ensures distraction-free contrast and high-efficiency OLED battery savings in dark mode during late-night study sessions.

Use semantic roles strictly: destructive or addiction-trigger alerts adopt warm amber/coral rather than harsh punitive red to avoid student guilt loops.

## Typography

Typography prioritizes native Vietnamese diacritics rendering without vertical clipping or awkward accent positioning.

- **Headlines & Metric Accents (`Plus Jakarta Sans`)**: Delivers rounded, approachable modern geometric forms that feel peer-to-peer and unpretentious for countdown clocks, session counters, and screen titles.
- **Body & Continuous Reading (`Be Vietnam Pro`)**: Specially crafted for Vietnamese typography, accommodating stacked accents (`ẩ`, `ễ`, `ặ`) cleanly within standard line bounds while retaining crisp legibility across high-density mobile screens.
- **Rules**: Numerical timers and countdown clocks must use proportional figures or tabular numbers to prevent text jitter during active Pomodoro/Deep Work cycles.

## Layout & Spacing

The layout is built for native Android single-hand ergonomical reachability (bottom 60% interaction zone), following Material 3 guidelines:

- **Mobile Viewport (up to 599dp)**: 4-column layout, 16dp outer canvas margins (`1.25rem`), and 16dp horizontal column gutters. Navigation is handled by a standard 80dp tall bottom Navigation Bar with 4 primary targets.
- **Expanded/Foldable Viewport (600dp+)**: Shifts from bottom navigation to an M3 Navigation Rail on the left side, scaling to an 8-column layout with 24dp margins.
- **Vertical Rhythm**: Follows an explicit 4dp/8dp base unit. Touch targets (buttons, quick tags, focus mode toggles) are kept to a strict minimum height of 48dp to ensure effortless thumb interaction.

## Elevation & Depth

Visual hierarchy abandons harsh drop shadows in favor of **Tonal Surface Layering** with soft, colored ambient glows:

- **Level 0 (Base Canvas)**: `#F8FAFC` (Light) / `#0F172A` (Dark).
- **Level 1 (Card Containers & Inactive Modules)**: Pure `#FFFFFF` (Light) or `#1E293B` (Dark) with a crisp 1px tint-border (`#E2E8F0` or `#334155`).
- **Level 2 (Active Floating Timers & Bottom Sheet Dialogs)**: Layered with a 12px blur ambient shadow tinted with primary teal: `0 8px 24px -4px rgba(13, 148, 136, 0.12)`.
- **Level 3 (Action Prompts & Urge Interventions)**: High-contrast warm coral glow: `0 12px 28px -6px rgba(249, 115, 22, 0.22)`, elevating mindful breathing modals above all background content.

## Shapes

The shape vocabulary uses pill forms and oversized curves (24dp to 32dp corner radii) to communicate friendliness, safety, and modern tactile comfort:

- **Primary Interactive Elements**: Fully rounded pills (`9999px`) for CTA buttons, status badges, and the active indicator pill on the bottom Navigation Bar.
- **Cards, Panels, and Bottom Sheets**: `24px` to `28px` corner radius (`rounded-2xl` to `rounded-3xl`), mirroring native Jetpack Compose `ShapeDefaults.Large` and `ShapeDefaults.ExtraLarge`.
- **Timer & Progress Containers**: Squircle-inspired circular tracks and rounded stroke endpoints that soften the visual intensity of ticking clocks.

## Components

### 1. Navigation Bar (Android M3 Shell)
- **Structure**: 4 tabs positioned at the screen bottom: *Tập trung* (Focus), *Tiến độ* (Progress), *Lộ trình* (Roadmap), *Hồ sơ* (Profile).
- **Height & Style**: 80dp container; active tab indicated by a mint pill background (`#CCFBF1`) enclosing the teal icon (`#0D9488`), with label text in `label-md` bold.

### 2. Buttons & Floating Actions
- **Primary CTA ("Bắt đầu tập trung")**: Large 56dp height, pill-shaped, solid primary teal `#0D9488` with white text. High-scale tactile press ripple (0.97 scale down animation).
- **Secondary / Emergency "Cứu nguy lướt video" (Urge Surfing)**: Outlined pill with warm coral border (`#F97316`) and light apricot surface (`#FFEDD5`) to trigger immediate deep breathing or mini-journaling prompts.

### 3. Focus Cards & Session Containers
- **Visuals**: Extra-large rounded cards (`rounded-3xl`, 28dp radius), filled with surface container white, bordered with 1px subtle slate.
- **Contents**: Centered countdown dial, quick-switch chips for subject matter (e.g., "Toán 12", "Luyện đề", "Đọc sách"), and a minimal mute toggle for app blockers.

### 4. Interactive Chips & Filters
- **Pill Chips**: 36dp height, `label-md`. Unselected chips use neutral slate outline with transparent fill; selected chips transform into tertiary mint `#CCFBF1` with dark teal text `#0F766E` and no harsh borders.

### 5. Lists & Progress Trackers
- **Daily Streak & Habit Rows**: Clean horizontal lists separated by 12dp spacing rather than horizontal line dividers. Progress indicators feature rounded caps with warm orange-to-teal gradient fills representing dopamine redirection.

### 6. Inputs & Modal Prompts
- **Text & Reflection Fields**: Full-width inputs with 16dp rounded corners, background `#F1F5F9`, zero underline, transitioning to a 2px primary teal focus ring upon keyboard activation.