$(document).ready(function(){
    $(".confirmar-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                confirmarOcupacion(idOcupacion);
                window.location.href = "/";
            }
        });


    });

    $(".anular-ocupacion").click(function (event) {
        let idOcupacion = $(this).data("idocupacion");
        showConfirmationModal(function (confirmed) {
            if (confirmed) {
                anularOcupacion(idOcupacion);
                window.location.href = "/";

            }
        });
    });

//    // Inicializar el carrusel
//    new bootstrap.Carousel(document.querySelector('#carrousel-lista-ocupaciones-pendientes'), {
//        interval: 5000, // Cambiar cada 5 segundos
//        wrap: true,     // Permitir ciclo continuo
//        touch: true     // Permitir control táctil
//    });
//
//    // Agregar animación a las tarjetas cuando se muestran
//    const carousel = document.querySelector('#carrousel-lista-ocupaciones-pendientes');
//    carousel.addEventListener('slide.bs.carousel', function () {
//        const cards = document.querySelectorAll('.card');
//        cards.forEach(card => {
//            card.classList.remove('animate__fadeIn');
//            void card.offsetWidth; // Forzar reflow
//            card.classList.add('animate__fadeIn');
//        });
//    });

    initializeGsapHomeAnimations();

});

function initializeGsapHomeAnimations() {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        document.body.style.opacity = "1";
        return;
    }

    if (!window.gsap) {
        document.body.style.opacity = "1";
        return;
    }

    const hero = document.querySelector(".js-gsap-hero");
    const heroImage = document.querySelector(".js-gsap-hero-image");
    const heroOverlay = document.querySelector(".js-gsap-hero-overlay");
    const heroLogo = document.querySelector(".js-gsap-hero-logo");
    const quickCards = gsap.utils.toArray(".js-gsap-quick-card");
    const sectionHeaders = gsap.utils.toArray(".js-gsap-section-header");
    const onboardingBanner = document.querySelector(".js-gsap-onboarding-banner");
    const pendingCards = gsap.utils.toArray(".js-gsap-pending-card");

    document.body.style.opacity = "1";

    if (window.ScrollTrigger) {
        gsap.registerPlugin(ScrollTrigger);
    }

    if (hero) {
        if (heroLogo) {
            gsap.set(heroLogo, {
                xPercent: -50,
                yPercent: -50,
                x: 0,
                y: 0,
                scale: 1,
                autoAlpha: 1,
                transformOrigin: "center center"
            });
        }

        const heroTimeline = gsap.timeline({ defaults: { ease: "sine.out" } });

        heroTimeline
            .from(hero, { autoAlpha: 0, y: 18, duration: 0.95 })
            .from(heroImage, { scale: 1.1, duration: 2.2, ease: "sine.out" }, 0)
            .from(heroOverlay, { opacity: 0, duration: 1.4, ease: "sine.out" }, 0.08)
            .fromTo(heroLogo,
                { autoAlpha: 0, y: 12, scale: 0.94, filter: "blur(10px) drop-shadow(0 4px 16px rgba(0,0,0,0.2))" },
                { autoAlpha: 1, y: 0, scale: 1, filter: "blur(0px) drop-shadow(0 12px 30px rgba(0,0,0,0.35))", duration: 1.25, ease: "power2.out", clearProps: "opacity,visibility" },
                0.28
            );

        const moveImageX = gsap.quickTo(heroImage, "x", { duration: 1.2, ease: "power3.out" });
        const moveImageY = gsap.quickTo(heroImage, "y", { duration: 1.2, ease: "power3.out" });
        const scaleImage = gsap.quickTo(heroImage, "scale", { duration: 1.6, ease: "power2.out" });
        const moveLogoX = gsap.quickTo(heroLogo, "x", { duration: 1.25, ease: "power3.out" });
        const moveLogoY = gsap.quickTo(heroLogo, "y", { duration: 1.25, ease: "power3.out" });

        hero.addEventListener("mousemove", function (event) {
            const bounds = hero.getBoundingClientRect();
            const progressX = (event.clientX - bounds.left) / bounds.width - 0.5;
            const progressY = (event.clientY - bounds.top) / bounds.height - 0.5;
            const x = progressX * 8;
            const y = progressY * 6;

            moveImageX(x);
            moveImageY(y);
            scaleImage(1.04);
            moveLogoX(-x * 0.12);
            moveLogoY(-y * 0.12);
        });

        hero.addEventListener("mouseleave", function () {
            moveImageX(0);
            moveImageY(0);
            scaleImage(1);
            moveLogoX(0);
            moveLogoY(0);
        });
    }

    if (sectionHeaders.length && window.ScrollTrigger) {
        sectionHeaders.forEach(function (header) {
            gsap.from(header, {
                autoAlpha: 0,
                y: 18,
                duration: 0.6,
                ease: "power2.out",
                scrollTrigger: {
                    trigger: header,
                    start: "top 88%"
                }
            });
        });
    }

    if (quickCards.length) {
        gsap.from(quickCards, {
            autoAlpha: 0,
            y: 24,
            duration: 0.65,
            stagger: 0.08,
            ease: "power2.out",
            delay: hero ? 0.2 : 0,
            scrollTrigger: window.ScrollTrigger ? {
                trigger: quickCards[0].closest(".row") || quickCards[0],
                start: "top 85%"
            } : undefined
        });
    }

    if (onboardingBanner) {
        gsap.from(onboardingBanner, {
            autoAlpha: 0,
            x: -28,
            duration: 0.75,
            ease: "power2.out",
            scrollTrigger: window.ScrollTrigger ? {
                trigger: onboardingBanner,
                start: "top 88%"
            } : undefined
        });
    }

    const pendingCarousel = document.querySelector("#carrousel-lista-ocupaciones-pendientes");
    if (pendingCarousel) {
        animatePendingCarouselContainer(pendingCarousel);
        animatePendingCarouselChrome(pendingCarousel);
        animatePendingCards(pendingCards, false, true);

        initializeGsapPendingCarousel(pendingCarousel);
    } else {
        animatePendingCards(pendingCards);
    }
}

function animatePendingCarouselContainer(carousel) {
    if (!window.gsap || !carousel || carousel.dataset.gsapContainerAnimated === "true") {
        return;
    }

    carousel.dataset.gsapContainerAnimated = "true";

    gsap.fromTo(carousel,
        {
            autoAlpha: 0,
            y: 24,
            scale: 0.985,
            transformOrigin: "50% 50%"
        },
        {
            autoAlpha: 1,
            y: 0,
            scale: 1,
            duration: 0.82,
            ease: "power2.out",
            overwrite: "auto",
            clearProps: "opacity,visibility,transform",
            scrollTrigger: window.ScrollTrigger ? {
                trigger: carousel,
                start: "top 88%",
                once: true
            } : undefined
        }
    );
}

function initializeGsapPendingCarousel(carousel) {
    if (!window.gsap || !window.bootstrap || !window.bootstrap.Carousel || !carousel) {
        return;
    }

    const carouselInner = carousel.querySelector(".carousel-inner");
    if (!carouselInner) {
        return;
    }

    const slides = Array.from(carousel.querySelectorAll(".carousel-item"));
    if (slides.length < 2) {
        return;
    }

    carousel.classList.add("carousel-gsap-enabled");

    const carouselInstance = window.bootstrap.Carousel.getOrCreateInstance(carousel);
    let isAnimating = false;
    let queuedDirection = null;

    carousel.addEventListener("slide.bs.carousel", function (event) {
        event.preventDefault();

        const outgoingSlide = carousel.querySelector(".carousel-item.active") || slides[0];
        const incomingSlide = getIncomingSlideFromEvent(event, slides, outgoingSlide);

        if (!outgoingSlide || !incomingSlide || outgoingSlide === incomingSlide) {
            return;
        }

        const direction = getCarouselDirection(event, slides, outgoingSlide, incomingSlide);

        if (isAnimating) {
            queuedDirection = direction;
            return;
        }

        isAnimating = true;
        const incomingCards = Array.from(incomingSlide.querySelectorAll(".js-gsap-pending-card"));
        const outgoingCards = Array.from(outgoingSlide.querySelectorAll(".js-gsap-pending-card"));

        const targetInnerHeight = Math.max(outgoingSlide.offsetHeight, incomingSlide.offsetHeight);
        if (targetInnerHeight) {
            gsap.set(carouselInner, { minHeight: targetInnerHeight + "px" });
        }

        gsap.killTweensOf([outgoingSlide, incomingSlide]);
        gsap.killTweensOf(outgoingCards);
        gsap.killTweensOf(incomingCards);

        gsap.set(carouselInner, { position: "relative", overflow: "hidden" });
        gsap.set([outgoingSlide, incomingSlide], {
            position: "absolute",
            inset: 0,
            width: "100%",
            display: "block"
        });

        gsap.set(outgoingSlide, { xPercent: 0, autoAlpha: 1, zIndex: 2 });
        gsap.set(incomingSlide, {
            xPercent: direction === "next" ? 12 : -12,
            autoAlpha: 0,
            zIndex: 3
        });

        if (outgoingCards.length) {
            gsap.set(outgoingCards, { x: 0, y: 0, scale: 1, autoAlpha: 1 });
        }

        if (incomingCards.length) {
            gsap.set(incomingCards, {
                x: direction === "next" ? 8 : -8,
                y: 6,
                scale: 0.992,
                autoAlpha: 0
            });
        }

        const timeline = gsap.timeline({
            defaults: { ease: "sine.out" },
            onComplete: function () {
                outgoingSlide.classList.remove("active");
                incomingSlide.classList.add("active");

                gsap.set([outgoingSlide, incomingSlide], {
                    clearProps: "position,inset,width,display,xPercent,zIndex"
                });
                gsap.set(outgoingSlide, { clearProps: "opacity,visibility" });
                gsap.set(incomingSlide, { clearProps: "opacity,visibility" });
                gsap.set(carouselInner, { clearProps: "minHeight" });
                if (outgoingCards.length) {
                    gsap.set(outgoingCards, { clearProps: "x,y,scale,opacity,visibility" });
                }
                if (incomingCards.length) {
                    gsap.set(incomingCards, { clearProps: "x,y,scale,opacity,visibility" });
                }

                animatePendingCards(incomingCards, true);

                isAnimating = false;
                if (queuedDirection) {
                    const nextDirection = queuedDirection;
                    queuedDirection = null;
                    if (nextDirection === "prev") {
                        carouselInstance.prev();
                        return;
                    }

                    carouselInstance.next();
                }
            }
        });

        timeline
            .to(outgoingSlide, {
                autoAlpha: 0.08,
                xPercent: direction === "next" ? -9 : 9,
                duration: 0.72,
                ease: "sine.inOut",
                overwrite: true
            }, 0)
            .fromTo(incomingSlide,
                {
                    autoAlpha: 0,
                    xPercent: direction === "next" ? 12 : -12
                },
                {
                    autoAlpha: 1,
                    xPercent: 0,
                    duration: 0.9,
                    ease: "power2.out",
                    overwrite: true
                },
                0.08
            );

        if (outgoingCards.length) {
            timeline.to(outgoingCards, {
                autoAlpha: 0.22,
                x: direction === "next" ? -5 : 5,
                y: -4,
                scale: 0.988,
                duration: 0.62,
                ease: "sine.inOut",
                stagger: 0.025,
                overwrite: true
            }, 0);
        }

        if (incomingCards.length) {
            timeline.to(incomingCards, {
                autoAlpha: 1,
                x: 0,
                y: 0,
                scale: 1,
                duration: 0.78,
                ease: "power2.out",
                stagger: 0.03,
                overwrite: true
            }, 0.16);
        }

    });
}

function getIncomingSlideFromEvent(event, slides, fallbackSlide) {
    if (event.relatedTarget) {
        return event.relatedTarget;
    }

    if (typeof event.to === "number" && slides[event.to]) {
        return slides[event.to];
    }

    return fallbackSlide;
}

function getCarouselDirection(event, slides, outgoingSlide, incomingSlide) {
    if (event.direction === "left") {
        return "next";
    }

    if (event.direction === "right") {
        return "prev";
    }

    const fromIndex = typeof event.from === "number" ? event.from : slides.indexOf(outgoingSlide);
    const toIndex = typeof event.to === "number" ? event.to : slides.indexOf(incomingSlide);

    if (fromIndex === -1 || toIndex === -1 || fromIndex === toIndex) {
        return "next";
    }

    if (fromIndex === slides.length - 1 && toIndex === 0) {
        return "next";
    }

    if (fromIndex === 0 && toIndex === slides.length - 1) {
        return "prev";
    }

    return toIndex > fromIndex ? "next" : "prev";
}

function animatePendingCarouselChrome(carousel) {
    if (!window.gsap || !carousel) {
        return;
    }

    const controls = carousel.querySelectorAll(".carousel-control-prev, .carousel-control-next");
    const header = carousel.closest(".row") ? carousel.closest(".row").querySelector(".js-gsap-section-header") : null;

    if (controls.length) {
        gsap.fromTo(controls,
            {
                autoAlpha: 0,
                scale: 0.92
            },
            {
                autoAlpha: 1,
                scale: 1,
                duration: 0.7,
                stagger: 0.08,
                ease: "power2.out",
                scrollTrigger: window.ScrollTrigger ? {
                    trigger: carousel,
                    start: "top 88%"
                } : undefined
            }
        );
    }

    if (header) {
        const badge = header.querySelector(".section-header__badge");
        if (badge) {
            gsap.fromTo(badge,
                {
                    scale: 0.9,
                    boxShadow: "0 0 0 0 rgba(231, 76, 60, 0)"
                },
                {
                    scale: 1,
                    boxShadow: "0 10px 24px rgba(231, 76, 60, 0.18)",
                    duration: 0.9,
                    ease: "power2.out",
                    scrollTrigger: window.ScrollTrigger ? {
                        trigger: carousel,
                        start: "top 88%"
                    } : undefined
                }
            );
        }
    }
}

function animatePendingCards(cards, force, isInitialRender) {
    if (!window.gsap || !cards || !cards.length) {
        return;
    }

    const cardsToAnimate = force
        ? cards
        : cards.filter(function (card) {
            return card.closest(".carousel-item") === null || card.closest(".carousel-item").classList.contains("active");
        });

    const shouldAnimateRightToLeft = Boolean(isInitialRender && !force);
    const animationOrder = shouldAnimateRightToLeft
        ? cardsToAnimate.slice().reverse()
        : cardsToAnimate;

    if (!cardsToAnimate.length) {
        return;
    }

    cardsToAnimate.forEach(function (card) {
        const header = card.querySelector(".ocp-card__header");
        const body = card.querySelector(".ocp-card__body");
        const footer = card.querySelector(".ocp-card__footer");

        gsap.set([header, body, footer].filter(Boolean), { clearProps: "all" });
    });

    gsap.fromTo(animationOrder,
        {
            autoAlpha: 0,
            y: 26,
            scale: 0.975,
            rotateX: force ? 0 : 3,
            transformOrigin: "50% 100%"
        },
        {
            autoAlpha: 1,
            y: 0,
            scale: 1,
            rotateX: 0,
            duration: force ? 0.62 : 0.72,
            stagger: 0.1,
            ease: "power3.out",
            overwrite: true,
            scrollTrigger: !force && window.ScrollTrigger ? {
                trigger: cardsToAnimate[0],
                start: "top 88%"
            } : undefined
        }
    );

    animationOrder.forEach(function (card, index) {
        const header = card.querySelector(".ocp-card__header");
        const body = card.querySelector(".ocp-card__body");
        const footer = card.querySelector(".ocp-card__footer");
        const timelineDelay = index * 0.08 + (force ? 0.06 : 0.14);

        if (!header && !body && !footer) {
            return;
        }

        gsap.timeline({ defaults: { ease: "sine.out" }, delay: timelineDelay })
            .from(header, { autoAlpha: 0, y: 10, duration: 0.28 }, 0)
            .from(body, { autoAlpha: 0, y: 12, duration: 0.34 }, 0.06)
            .from(footer, { autoAlpha: 0, y: 8, duration: 0.24 }, 0.14);
    });
}
