$(document).ready(function () {
    initDatePickers();
    initArtistaChoices();
    initializeGsapEventosAnimations();
});

function initDatePickers() {
    const desdeInput = document.querySelector("#idFechaDesde");
    const hastaInput = document.querySelector("#idFechaHasta");

    if (!desdeInput || !hastaInput) {
        return;
    }

    // Calculate max date (today + 45 days)
    const maxDate = new Date();
    maxDate.setDate(maxDate.getDate() + 45);

    const fechaHastaPicker = flatpickr(hastaInput, {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "Y-m-d",
        allowInput: false,
        defaultDate: hastaInput.value || null,
        minDate: desdeInput.value || null,
        maxDate: maxDate
    });

    const fechaDesdePicker = flatpickr(desdeInput, {
        disableMobile: true,
        locale: "es",
        altInput: true,
        altFormat: "j F, Y",
        dateFormat: "Y-m-d",
        allowInput: false,
        defaultDate: desdeInput.value || null,
        maxDate: maxDate,
        onChange: function (selectedDates) {
            if (!selectedDates || selectedDates.length === 0) {
                fechaHastaPicker.set("minDate", null);
                return;
            }
            const desdeDate = selectedDates[0];
            fechaHastaPicker.set("minDate", desdeDate);
            // Also update maxDate of hasta picker to not exceed 45 days from today
            if (desdeDate > maxDate) {
                fechaHastaPicker.set("maxDate", maxDate);
            } else {
                fechaHastaPicker.set("maxDate", maxDate);
            }
        }
    });
}

/**
 * Inicializa Choices.js solo para el selector de artista.
 * Los filtros de provincia/municipio se manejan en eventos-publicos-filter.js
 */
function initArtistaChoices() {
    const artistaEl = document.querySelector("#idArtista");

    if (!artistaEl || typeof Choices === "undefined") {
        return;
    }

    new Choices(artistaEl, {
        searchEnabled: true,
        shouldSort: false,
        placeholder: true,
        placeholderValue: 'Todos los artistas'
    });
}

/**
 * Initialize GSAP animations for /eventos page
 * Includes hero parallax, text reveal, and staggered card animations
 */
function initializeGsapEventosAnimations() {
    // Check if GSAP is available
    if (typeof gsap === 'undefined' || typeof ScrollTrigger === 'undefined') {
        console.warn('GSAP not loaded, content already visible');
        return;
    }

    // Register ScrollTrigger plugin
    gsap.registerPlugin(ScrollTrigger);
    
    // Mark GSAP as loaded - this will enable scroll animations
    document.body.classList.add('gsap-loaded');

    // Detect touch devices
    const isTouch = window.matchMedia('(pointer: coarse)').matches;

    // Animation timing constants - only for scroll animations
    const STAGGER_DELAY = isTouch ? 0.05 : 0.08;
    const CARD_DURATION = isTouch ? 0.45 : 0.65;

    // Hero Parallax only (Desktop Only) - no load animations
    if (!isTouch) {
        initHeroParallax(0.95);
    }

    // ScrollTrigger for Event Cards only
    initCardStaggerAnimation(STAGGER_DELAY, CARD_DURATION, isTouch);

    // Cleanup on page unload
    window.addEventListener('beforeunload', cleanupGsapAnimations);
}

/**
 * Initialize hero parallax mouse-follow effect
 * Image has 30px overflow on each side to prevent edge clipping
 * @param {number} duration - Animation duration
 */
function initHeroParallax(duration) {
    const heroImage = document.querySelector('.js-gsap-hero-image');
    const heroContainer = document.querySelector('.js-gsap-hero');

    if (!heroImage || !heroContainer) return;

    let rafId = null;
    let mouseX = 0;
    let mouseY = 0;
    let isActive = false;

    function updateParallax() {
        if (!isActive) return;

        // Calculate movement (max 15px for smoother, safer parallax)
        const moveX = mouseX * 15;
        const moveY = mouseY * 15;

        gsap.to(heroImage, {
            x: moveX,
            y: moveY,
            duration: duration,
            ease: "power2.out"
        });

        rafId = null;
    }

    heroContainer.addEventListener('mousemove', function(e) {
        const rect = heroContainer.getBoundingClientRect();
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;

        // Normalize to -1 to 1 range
        mouseX = (e.clientX - rect.left - centerX) / centerX;
        mouseY = (e.clientY - rect.top - centerY) / centerY;

        if (!rafId) {
            isActive = true;
            rafId = requestAnimationFrame(updateParallax);
        }
    });

    heroContainer.addEventListener('mouseleave', function() {
        isActive = false;
        gsap.to(heroImage, {
            x: 0,
            y: 0,
            duration: duration,
            ease: "power2.out"
        });
    });
}

/**
 * Initialize text reveal timeline
 * @param {number} delay1 - Delay for first title line
 * @param {number} delay2 - Delay for second title line
 * @param {number} subtitleDelay - Delay for subtitle
 */
function initTextRevealTimeline(delay1, delay2, subtitleDelay) {
    const heroLogo = document.querySelector('.hero-logo');
    const titleLine1 = document.querySelector('.gsap-title-line1');
    const titleLine2 = document.querySelector('.gsap-title-line2');
    const subtitle = document.querySelector('.hero-subtitle');

    if (!heroLogo && !titleLine1 && !titleLine2 && !subtitle) return;

    const tl = gsap.timeline({ defaults: { ease: "power3.out" } });

    // Animate logo first - same effect as main.js
    if (heroLogo) {
        tl.fromTo(heroLogo,
            { 
                opacity: 0, 
                y: 12, 
                scale: 0.94, 
                filter: "blur(10px) drop-shadow(0 4px 16px rgba(0,0,0,0.2))" 
            },
            { 
                opacity: 1, 
                y: 0, 
                scale: 1, 
                filter: "blur(0px) drop-shadow(0 12px 30px rgba(0,0,0,0.35))", 
                duration: 1.25, 
                ease: "power2.out"
            },
            0
        );
    }

    // Animate first title line
    if (titleLine1) {
        tl.to(titleLine1, {
            opacity: 1,
            scale: 1,
            filter: 'blur(0px)',
            duration: 0.8,
            delay: delay1
        }, 0);
    }

    // Animate second title line
    if (titleLine2) {
        tl.to(titleLine2, {
            opacity: 1,
            scale: 1,
            filter: 'blur(0px)',
            duration: 0.8,
            delay: delay2
        }, 0);
    }

    // Animate subtitle
    if (subtitle) {
        tl.to(subtitle, {
            opacity: 1,
            scale: 1,
            filter: 'blur(0px)',
            duration: 0.8,
            delay: subtitleDelay
        }, 0);
    }
}

/**
 * Initialize staggered card animation with ScrollTrigger
 * @param {number} staggerDelay - Delay between each card
 * @param {number} duration - Animation duration
 * @param {boolean} isTouch - Whether device is touch
 */
function initCardStaggerAnimation(staggerDelay, duration, isTouch) {
    const eventCards = document.querySelectorAll('.gsap-event-card');

    if (!eventCards.length) return;

    // Skip stagger on mobile if too many cards (performance)
    const shouldAnimate = !isTouch || eventCards.length <= 6;

    if (!shouldAnimate) {
        // Make cards visible immediately without animation
        gsap.set(eventCards, { opacity: 1, y: 0 });
        return;
    }

    // Create ScrollTrigger for each day's group
    const dayGroups = document.querySelectorAll('[data-animate="stagger"] > div');
    
    if (!dayGroups.length) {
        // If no groups found, animate all cards at once
        ScrollTrigger.create({
            trigger: ".eventos-grid",
            start: "top 85%",
            once: true,
            onEnter: () => {
                gsap.to(eventCards, {
                    opacity: 1,
                    y: 0,
                    duration: duration,
                    stagger: staggerDelay,
                    ease: "power2.out"
                });
            }
        });
        return;
    }
    
    dayGroups.forEach((group, groupIndex) => {
        const cards = group.querySelectorAll('.gsap-event-card');
        
        if (!cards.length) return;

        ScrollTrigger.create({
            trigger: group,
            start: "top 85%",
            once: true,
            onEnter: () => {
                gsap.to(cards, {
                    opacity: 1,
                    y: 0,
                    duration: duration,
                    stagger: staggerDelay,
                    ease: "power2.out"
                });
            }
        });
    });
}

/**
 * Initialize filter section entrance animation
 */
function initFilterSectionAnimation() {
    const filterSection = document.querySelector('.js-gsap-filter');

    if (!filterSection) return;

    // Animate from initial hidden state to visible
    gsap.to(filterSection, {
        opacity: 1,
        y: 0,
        duration: 0.6,
        delay: 0.5,
        ease: "power2.out"
    });
}

/**
 * Cleanup all GSAP animations and ScrollTriggers
 */
function cleanupGsapAnimations() {
    // Kill all ScrollTriggers
    if (typeof ScrollTrigger !== 'undefined') {
        ScrollTrigger.getAll().forEach(trigger => trigger.kill());
    }

    // Kill all GSAP animations
    if (typeof gsap !== 'undefined') {
        gsap.killTweensOf('.gsap-event-card, .js-gsap-hero-image');
    }
}
